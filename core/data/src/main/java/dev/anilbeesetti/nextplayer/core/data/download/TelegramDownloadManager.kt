package dev.anilbeesetti.nextplayer.core.data.download

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibManager
import dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibMediaResolver
import dev.anilbeesetti.nextplayer.core.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.drinkless.tdlib.TdApi
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import javax.inject.Singleton

class DownloadStalledException(message: String) : IOException(message)

data class DownloadErrorInfo(
    val key: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileName: String,
    val fileId: String?,
    val downloadUrl: String,
    val errorCode: String,
    val errorMessage: String,
    val rootCause: String,
    val stackTrace: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val diagnosticAdvice: String,
    val video: Video? = null
)

data class DownloadTask(
    val key: String,
    val title: String,
    val progress: Float,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val isPaused: Boolean = false,
    val status: String = "Downloading",
    val video: Video? = null,
    val error: DownloadErrorInfo? = null,
    val source: String = "Telegram Direct"
)

@Singleton
class TelegramDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tdlibManager: TdlibManager,
    private val tdlibMediaResolver: TdlibMediaResolver,
    private val preferencesRepository: PreferencesRepository
) {
    companion object {
        private const val TAG = "TelegramDownloadManager"
        private const val OFFLINE_FOLDER = "offline_media"
        private const val MAX_CONCURRENT_DOWNLOADS = 2
        const val MIN_VALID_VIDEO_SIZE = 1024L // 1 KB minimum valid video file size

        @Volatile
        private var instance: TelegramDownloadManager? = null

        fun getInstance(): TelegramDownloadManager? = instance

        private val fallbackUrls = ConcurrentHashMap<String, String>()

        fun registerFallbackUrl(nameOrKey: String, url: String) {
            if (nameOrKey.isNotBlank() && url.isNotBlank()) {
                fallbackUrls[nameOrKey] = url
                val safe = sanitizeFileName(nameOrKey)
                if (safe != nameOrKey) fallbackUrls[safe] = url
                val decoded = try { java.net.URLDecoder.decode(nameOrKey, "UTF-8") } catch (_: Exception) { nameOrKey }
                if (decoded != nameOrKey) {
                    fallbackUrls[decoded] = url
                    val safeDecoded = sanitizeFileName(decoded)
                    if (safeDecoded != decoded) fallbackUrls[safeDecoded] = url
                }
            }
        }

        fun getFallbackUrl(nameOrKey: String): String? {
            if (nameOrKey.isBlank()) return null
            fallbackUrls[nameOrKey]?.let { return it }
            val safe = sanitizeFileName(nameOrKey)
            fallbackUrls[safe]?.let { return it }
            val decoded = try { java.net.URLDecoder.decode(nameOrKey, "UTF-8") } catch (_: Exception) { nameOrKey }
            fallbackUrls[decoded]?.let { return it }
            val safeDecoded = sanitizeFileName(decoded)
            fallbackUrls[safeDecoded]?.let { return it }
            return null
        }

        fun sanitizeFileName(name: String): String {
            return name.replace(Regex("[\\/:*?\"<>|\r\n\t]"), "_").trim()
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _activeDownloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, Float>> = _activeDownloads.asStateFlow()

    private val _activeDownloadTasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val activeDownloadTasks: StateFlow<Map<String, DownloadTask>> = _activeDownloadTasks.asStateFlow()

    private val _downloadedKeys = MutableStateFlow<Set<String>>(emptySet())
    val downloadedKeys: StateFlow<Set<String>> = _downloadedKeys.asStateFlow()

    private val _failedDownloads = MutableStateFlow<Map<String, DownloadErrorInfo>>(emptyMap())
    val failedDownloads: StateFlow<Map<String, DownloadErrorInfo>> = _failedDownloads.asStateFlow()

    private val activeDownloadAliases = ConcurrentHashMap<String, Set<String>>()
    private val keyToFileMap = ConcurrentHashMap<String, String>()
    private val offlineFilesCache = ConcurrentHashMap<String, File>()

    private fun getAllVideoKeys(video: Video, extraFileId: String? = null): Set<String> {
        val keys = mutableSetOf<String>()
        video.externalId?.let { if (it.isNotEmpty()) keys.add(it) }
        keys.add(video.id.toString())
        if (video.nameWithExtension.isNotEmpty()) keys.add(video.nameWithExtension)
        val safeName = sanitizeFileName(video.nameWithExtension)
        if (safeName.isNotEmpty()) keys.add(safeName)
        extraFileId?.let { if (it.isNotEmpty()) keys.add(it) }
        if (video.uriString.startsWith("tdlib://")) {
            try {
                android.net.Uri.parse(video.uriString).getQueryParameter("fileId")?.let {
                    if (it.isNotEmpty()) keys.add(it)
                }
            } catch (_: Exception) {}
        }
        return keys
    }

    private fun registerActiveProgress(videoKey: String, aliases: Set<String>, progress: Float, task: DownloadTask) {
        activeDownloadAliases[videoKey] = aliases
        val updatedTasks = _activeDownloadTasks.value.toMutableMap()
        updatedTasks[videoKey] = task
        _activeDownloadTasks.value = updatedTasks

        val updatedDownloads = _activeDownloads.value.toMutableMap()
        for (k in aliases) {
            updatedDownloads[k] = progress
        }
        _activeDownloads.value = updatedDownloads
    }

    private fun removeActiveProgress(videoKey: String) {
        val aliases = activeDownloadAliases.remove(videoKey) ?: setOf(videoKey)
        val updatedTasks = _activeDownloadTasks.value.toMutableMap()
        updatedTasks.remove(videoKey)
        _activeDownloadTasks.value = updatedTasks

        val updatedDownloads = _activeDownloads.value.toMutableMap()
        for (k in aliases) {
            updatedDownloads.remove(k)
        }
        _activeDownloads.value = updatedDownloads
    }

    fun dismissFailedDownload(key: String) {
        _failedDownloads.value = _failedDownloads.value - key
        removeActiveProgress(key)
    }

    fun clearAllFailedDownloads() {
        _failedDownloads.value = emptyMap()
    }

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeTdlibFileIds = ConcurrentHashMap<String, Int>()
    private val downloadQueue = Collections.synchronizedList(mutableListOf<Video>())

    private val offlineDir: File by lazy {
        val external = context.getExternalFilesDir(OFFLINE_FOLDER)
        if (external != null) {
            external.mkdirs()
            external
        } else {
            val internal = File(context.filesDir, OFFLINE_FOLDER)
            internal.mkdirs()
            internal
        }
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val wakeLock = powerManager?.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "KryptonPlayer:TelegramDownloadManagerWakeLock"
    )?.apply {
        setReferenceCounted(false)
    }

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    @Suppress("DEPRECATION")
    private val wifiLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        wifiManager?.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "KryptonPlayer:TelegramDownloadManagerWifiLock"
        )
    } else {
        wifiManager?.createWifiLock(
            WifiManager.WIFI_MODE_FULL,
            "KryptonPlayer:TelegramDownloadManagerWifiLock"
        )
    }?.apply {
        setReferenceCounted(false)
    }

    fun hasActiveDownloads(): Boolean {
        val hasRunningTasks = _activeDownloadTasks.value.values.any { !it.isPaused }
        val hasJobs = activeJobs.isNotEmpty()
        val hasQueue = synchronized(downloadQueue) { downloadQueue.isNotEmpty() }
        return hasRunningTasks || hasJobs || hasQueue
    }

    private val autoSaveToGalleryKeys = ConcurrentHashMap.newKeySet<String>()
    private val galleryExportKeys = ConcurrentHashMap.newKeySet<String>()

    private fun galleryKeys(video: Video): Set<String> = buildSet {
        add(video.id.toString())
        video.externalId?.takeIf { it.isNotBlank() }?.let(::add)
        video.nameWithExtension.takeIf { it.isNotBlank() }?.let {
            add(it)
            add(sanitizeFileName(it))
        }
    }

    private fun markForGalleryExport(video: Video) {
        galleryExportKeys.addAll(galleryKeys(video))
    }

    private fun clearGalleryExport(video: Video) {
        galleryExportKeys.removeAll(galleryKeys(video))
    }

    private fun consumeGalleryExportRequest(video: Video): Boolean {
        val keys = galleryKeys(video)
        val requested = keys.any(autoSaveToGalleryKeys::contains)
        if (requested) autoSaveToGalleryKeys.removeAll(keys)
        return requested
    }

    fun isSavingToGallery(video: Video): Boolean = galleryKeys(video).any(galleryExportKeys::contains)

    private fun downloadStatus(video: Video, status: String): String {
        return if (isSavingToGallery(video)) "Saving to Gallery • $status" else status
    }

    private fun galleryMimeType(video: Video, name: String): String {
        if (video.isImage) return "image/*"
        return when (name.substringAfterLast('.', "").lowercase()) {
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            else -> "video/mp4"
        }
    }

    private fun publishGalleryProgress(
        video: Video,
        videoKey: String,
        copiedBytes: Long,
        totalBytes: Long,
        aliases: Set<String>,
    ) {
        val progress = if (totalBytes > 0L) {
            (copiedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.01f, 0.99f)
        } else {
            0.01f
        }
        registerActiveProgress(
            videoKey = videoKey,
            aliases = aliases,
            progress = progress,
            task = DownloadTask(
                key = videoKey,
                title = "Saving to Gallery",
                progress = progress,
                downloadedBytes = copiedBytes,
                totalBytes = totalBytes,
                status = "Saving to Gallery",
                video = video,
                source = "Device Gallery",
            ),
        )
    }

    private suspend fun copyFileToGallery(
        sourceFile: File,
        displayName: String,
        video: Video,
        videoKey: String,
        aliases: Set<String>,
    ) {
        if (!sourceFile.exists() || sourceFile.length() < MIN_VALID_VIDEO_SIZE) {
            showToast("Couldn't save to Gallery: file is not ready")
            clearGalleryExport(video)
            return
        }

        val safeName = sanitizeFileName(displayName.ifBlank { "video_${video.id}.mp4" })
        val totalBytes = sourceFile.length()
        var copiedBytes = 0L
        var lastProgressUpdate = 0L
        var pendingUri: Uri? = null

        fun reportProgress(force: Boolean = false) {
            val now = System.currentTimeMillis()
            if (force || now - lastProgressUpdate >= 180L) {
                lastProgressUpdate = now
                publishGalleryProgress(video, videoKey, copiedBytes, totalBytes, aliases)
            }
        }

        suspend fun writeTo(output: OutputStream) {
            val buffer = ByteArray(512 * 1024)
            val input = BufferedInputStream(FileInputStream(sourceFile), buffer.size)
            val bufferedOutput = BufferedOutputStream(output, buffer.size)
            try {
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read == -1) break
                    bufferedOutput.write(buffer, 0, read)
                    copiedBytes += read
                    reportProgress()
                }
                bufferedOutput.flush()
            } finally {
                runCatching { input.close() }
                runCatching { bufferedOutput.close() }
            }
        }

        markForGalleryExport(video)
        reportProgress(force = true)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, galleryMimeType(video, safeName))
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${if (video.isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES}/KryptonPlayer",
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = if (video.isImage) {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
                val resolver = context.contentResolver
                pendingUri = resolver.insert(collection, values)
                    ?: error("Couldn't create Gallery file")
                val output = resolver.openOutputStream(pendingUri!!, "w")
                    ?: error("Couldn't open Gallery file")
                writeTo(output)
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(pendingUri!!, values, null, null)
                Log.i(TAG, "Saved file to MediaStore: $pendingUri")
            } else {
                @Suppress("DEPRECATION")
                val publicFolder = File(
                    Environment.getExternalStoragePublicDirectory(
                        if (video.isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES,
                    ),
                    "KryptonPlayer",
                ).apply { mkdirs() }
                val destination = File(publicFolder, safeName)
                writeTo(FileOutputStream(destination))
                MediaScannerConnection.scanFile(context, arrayOf(destination.absolutePath), null, null)
                Log.i(TAG, "Saved file to Gallery: ${destination.absolutePath}")
            }
            copiedBytes = totalBytes
            reportProgress(force = true)
            showToast("Saved to Gallery: $safeName")
        } catch (e: Exception) {
            pendingUri?.let { uri ->
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
            Log.e(TAG, "Error saving $safeName to Gallery", e)
            showToast("Couldn't save to Gallery: ${e.message ?: "Unknown error"}")
        } finally {
            clearGalleryExport(video)
            removeActiveProgress(videoKey)
        }
    }

    @Synchronized
    fun updateLocksAndService() {
        val hasActive = hasActiveDownloads()
        if (hasActive) {
            acquireWakeLocks()
            TelegramDownloadService.start(context)
        } else {
            releaseWakeLocks()
            TelegramDownloadService.stop(context)
        }
    }

    private fun acquireWakeLocks() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(6 * 60 * 60 * 1000L)
                Log.i(TAG, "DownloadManager WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire DownloadManager WakeLock: ${e.message}")
        }
        try {
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
                Log.i(TAG, "DownloadManager WifiLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire DownloadManager WifiLock: ${e.message}")
        }
    }

    private fun releaseWakeLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(TAG, "DownloadManager WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release DownloadManager WakeLock: ${e.message}")
        }
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                Log.i(TAG, "DownloadManager WifiLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release DownloadManager WifiLock: ${e.message}")
        }
    }

    init {
        instance = this
        refreshDownloadedKeys()
    }

    fun refreshDownloadedKeys() {
        scope.launch {
            val existing = mutableSetOf<String>()
            val newCache = ConcurrentHashMap<String, File>()
            val activeKeys = _activeDownloads.value.keys
            listOfNotNull(
                context.getExternalFilesDir(OFFLINE_FOLDER),
                File(context.filesDir, OFFLINE_FOLDER),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "KryptonPlayer"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "KryptonPlayer"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ArchPlayer"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ArchPlayer")
            ).forEach { dir ->
                if (dir.exists()) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            if (file.name.endsWith(".tmp")) {
                                try { file.delete() } catch (_: Exception) {}
                            } else if (file.name.endsWith(".part")) {
                                val isActivelyDownloading = activeKeys.any { file.name.contains(it) }
                                if (!isActivelyDownloading && file.length() == 0L && System.currentTimeMillis() - file.lastModified() > 300_000L) {
                                    try { file.delete() } catch (_: Exception) {}
                                }
                            } else if (file.name.endsWith(".segments")) {
                                // Only delete orphan .segments file if its corresponding .part file is missing
                                val basePartName = file.name.removeSuffix(".segments")
                                val correspondingPart = File(file.parentFile, basePartName)
                                if (!correspondingPart.exists()) {
                                    try { file.delete() } catch (_: Exception) {}
                                }
                            } else if (file.name.endsWith(".thumb.jpg") || file.name.endsWith(".thumb.png")) {
                                // keep thumbnails
                            } else if (file.length() < MIN_VALID_VIDEO_SIZE) {
                                val isActivelyDownloading = activeKeys.any { file.name.contains(it) }
                                if (!isActivelyDownloading && System.currentTimeMillis() - file.lastModified() > 180_000L) {
                                    Log.w(TAG, "Cleaning up corrupt/partial file in offline storage: ${file.name} (${file.length()} bytes)")
                                    try { file.delete() } catch (_: Exception) {}
                                }
                            } else {
                                val cleanName = file.name.removeSuffix(".mp4")
                                existing.add(file.name)
                                existing.add(cleanName)
                                existing.add("${file.name}.mp4")
                                newCache[file.name] = file
                                newCache[cleanName] = file
                                newCache["${cleanName}.mp4"] = file
                            }
                        }
                    }
                }
            }
            // Retain mapped video IDs/keys if the corresponding file exists on disk
            for ((key, fileName) in keyToFileMap) {
                val cleanFileName = fileName.removeSuffix(".mp4")
                val matched = newCache[fileName] ?: newCache[cleanFileName] ?: newCache["$cleanFileName.mp4"]
                if (matched != null && matched.exists() && matched.length() >= MIN_VALID_VIDEO_SIZE) {
                    existing.add(key)
                    newCache[key] = matched
                } else {
                    keyToFileMap.remove(key)
                }
            }
            offlineFilesCache.clear()
            offlineFilesCache.putAll(newCache)
            _downloadedKeys.value = existing
        }
    }

    fun isDownloadedToWatch(video: Video): Boolean {
        // Fast in-memory check O(1) - never causes UI thread freezes
        val keys = getAllVideoKeys(video)
        val set = _downloadedKeys.value
        for (k in keys) {
            if (set.contains(k)) return true
            val cached = offlineFilesCache[k]
            if (cached != null && cached.exists() && cached.length() >= MIN_VALID_VIDEO_SIZE) {
                return true
            }
        }
        // STRICT UI THREAD PROTECTION: Never perform disk traversal on the Main Thread!
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return false
        }
        val file = getOfflineFile(video) ?: return false
        return file.length() >= MIN_VALID_VIDEO_SIZE
    }

    fun getOfflineFile(video: Video): File? {
        val expectedSize = video.size
        return getOfflineFile(video.nameWithExtension, video.externalId, expectedSize)
    }

    fun getOfflineFile(fileName: String?, fileId: String? = null, expectedSize: Long = -1L): File? {
        val candidateNames = mutableListOf<String>()
        if (!fileName.isNullOrBlank()) {
            val decoded = try { java.net.URLDecoder.decode(fileName, "UTF-8") } catch (_: Exception) { fileName }
            val safe = sanitizeFileName(decoded)
            candidateNames.add(safe)
            if (safe != decoded) candidateNames.add(decoded)
            if (fileName != decoded) candidateNames.add(fileName)
            val safeRaw = sanitizeFileName(fileName)
            if (safeRaw != safe) candidateNames.add(safeRaw)
        }
        if (!fileId.isNullOrBlank()) {
            candidateNames.add("$fileId.mp4")
            candidateNames.add(fileId)
        }

        val allCandidates = mutableSetOf<String>()
        for (name in candidateNames) {
            allCandidates.add(name)
            if (!name.endsWith(".mp4", ignoreCase = true)) {
                allCandidates.add("$name.mp4")
            }
        }

        // 1. Fast in-memory cache check
        for (candidate in allCandidates) {
            val cached = offlineFilesCache[candidate]
            if (cached != null && cached.exists() && cached.isFile && !cached.name.endsWith(".part") && cached.length() >= MIN_VALID_VIDEO_SIZE) {
                return cached
            }
        }

        // 2. Strict UI thread protection: return null instead of freezing Choreographer frame
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return null
        }

        val dirs = listOfNotNull(
            context.getExternalFilesDir(OFFLINE_FOLDER),
            File(context.filesDir, OFFLINE_FOLDER),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "KryptonPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "KryptonPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ArchPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ArchPlayer")
        )

        val activeKeys = _activeDownloads.value.keys

        for (dir in dirs) {
            if (!dir.exists()) continue
            for (name in allCandidates) {
                val f = File(dir, name)
                if (f.exists() && f.isFile && !f.name.endsWith(".part") && !f.name.endsWith(".tmp") && !f.name.endsWith(".segments")) {
                    val isActivelyDownloading = activeKeys.any { f.name.contains(it) }
                    if (f.length() < MIN_VALID_VIDEO_SIZE) {
                        if (!isActivelyDownloading) {
                            Log.w(TAG, "getOfflineFile found corrupt/truncated file (${f.name}, ${f.length()} bytes < $MIN_VALID_VIDEO_SIZE). Cleaning up...")
                            try { f.delete() } catch (_: Exception) {}
                        }
                        continue
                    }
                    allCandidates.forEach { offlineFilesCache[it] = f }
                    return f
                }
            }
            val dirFiles = dir.listFiles() ?: continue
            for (file in dirFiles) {
                if (file.isFile && !file.name.endsWith(".part") && !file.name.endsWith(".tmp") && !file.name.endsWith(".segments")) {
                    if (allCandidates.any { it.equals(file.name, ignoreCase = true) || it.removeSuffix(".mp4").equals(file.name.removeSuffix(".mp4"), ignoreCase = true) }) {
                        if (file.length() >= MIN_VALID_VIDEO_SIZE) {
                            allCandidates.forEach { offlineFilesCache[it] = file }
                            return file
                        }
                    }
                }
            }
        }
        return null
    }

    fun extractVideoThumbnail(videoFile: File, outputFile: File): Boolean {
        if (!videoFile.exists() || videoFile.length() <= 1024) return false
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime()
            if (bitmap != null) {
                FileOutputStream(outputFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract thumbnail for ${videoFile.name}: ${e.message}")
            false
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun getDownloadedVideos(knownVideos: List<Video> = emptyList()): List<Video> {
        val result = mutableListOf<Video>()
        val seenPaths = mutableSetOf<String>()

        val dirs = listOfNotNull(
            context.getExternalFilesDir(OFFLINE_FOLDER),
            File(context.filesDir, OFFLINE_FOLDER),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "KryptonPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "KryptonPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ArchPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ArchPlayer")
        )

        for (dir in dirs) {
            if (!dir.exists()) continue
            val files = dir.listFiles() ?: continue
            for (file in files) {
                if (!file.isFile || file.length() < MIN_VALID_VIDEO_SIZE ||
                    file.name.endsWith(".part") || file.name.endsWith(".tmp") || file.name.endsWith(".segments") ||
                    file.name.endsWith(".thumb.jpg") || file.name.endsWith(".thumb.png")) {
                    continue
                }
                if (!seenPaths.add(file.absolutePath)) continue

                val matchingVideo = knownVideos.firstOrNull { kv ->
                    val safeKv = sanitizeFileName(kv.nameWithExtension)
                    val extId = kv.externalId
                    val rawName = kv.nameWithExtension
                    val cleanFileName = file.name.removeSuffix(".mp4")
                    val cleanSafeKv = safeKv.removeSuffix(".mp4")
                    val cleanRawName = rawName.removeSuffix(".mp4")

                    rawName.equals(file.name, ignoreCase = true) ||
                    safeKv.equals(file.name, ignoreCase = true) ||
                    cleanFileName.equals(cleanRawName, ignoreCase = true) ||
                    cleanFileName.equals(cleanSafeKv, ignoreCase = true) ||
                    (!extId.isNullOrEmpty() && (file.name == "$extId.mp4" || file.name == extId || file.name.contains(extId))) ||
                    file.name.contains(kv.id.toString())
                }

                // Check or extract local permanent thumbnail asynchronously
                val thumbFile = File(file.parentFile, "${file.name}.thumb.jpg")
                if (!thumbFile.exists() || thumbFile.length() == 0L) {
                    scope.launch {
                        try {
                            extractVideoThumbnail(file, thumbFile)
                        } catch (_: Exception) {}
                    }
                }

                val resolvedArtworkUri = when {
                    thumbFile.exists() && thumbFile.length() > 0L -> Uri.fromFile(thumbFile).toString()
                    else -> Uri.fromFile(file).toString()
                }

                val modTime = if (file.lastModified() > 0L) file.lastModified() else System.currentTimeMillis()

                if (matchingVideo != null) {
                    keyToFileMap[matchingVideo.id.toString()] = file.name
                    matchingVideo.externalId?.let { keyToFileMap[it] = file.name }
                    result.add(
                        matchingVideo.copy(
                            path = file.absolutePath,
                            uriString = Uri.fromFile(file).toString(),
                            size = file.length(),
                            dateModified = modTime,
                            formattedFileSize = formatFileSize(file.length()),
                            artworkUri = resolvedArtworkUri
                        )
                    )
                } else {
                    val cleanTitle = file.nameWithoutExtension.replace('_', ' ')
                    result.add(
                        Video(
                            id = file.name.hashCode().toLong(),
                            path = file.absolutePath,
                            parentPath = "Offline Downloads",
                            duration = 0L,
                            uriString = Uri.fromFile(file).toString(),
                            nameWithExtension = file.name,
                            width = 0,
                            height = 0,
                            size = file.length(),
                            dateModified = modTime,
                            formattedDuration = "",
                            formattedFileSize = formatFileSize(file.length()),
                            artworkUri = resolvedArtworkUri,
                            externalId = file.name,
                            isImage = false
                        )
                    )
                }
            }
        }
        val seenIds = mutableSetOf<Long>()
        val seenNames = mutableSetOf<String>()
        val finalResult = mutableListOf<Video>()
        for (video in result) {
            val nameKey = video.nameWithExtension.lowercase().trim()
            if (seenIds.add(video.id) && seenNames.add(nameKey)) {
                finalResult.add(video)
            }
        }
        return finalResult
    }

    fun downloadToWatch(video: Video, prioritize: Boolean = false) {
        val videoKey = video.externalId ?: video.id.toString()
        val rawName = video.nameWithExtension.ifBlank { "video_${video.id}.mp4" }
        val safeName = sanitizeFileName(rawName)
        keyToFileMap[videoKey] = safeName
        keyToFileMap[video.id.toString()] = safeName
        video.externalId?.let { keyToFileMap[it] = safeName }
        _failedDownloads.value = _failedDownloads.value - videoKey

        // Cache fallback streaming URL
        val streamUrl = video.path.takeIf { it.startsWith("http") } ?: video.uriString
        registerFallbackUrl(rawName, streamUrl)
        video.externalId?.let { registerFallbackUrl(it, streamUrl) }

        if (isDownloadedToWatch(video)) {
            showToast("Already downloaded offline!")
            return
        }

        if (_activeDownloadTasks.value.containsKey(videoKey)) {
            showToast("Already in download queue...")
            return
        }

        val initialTask = DownloadTask(
            key = videoKey,
            title = sanitizeFileName(rawName),
            progress = 0f,
            totalBytes = video.size,
            status = downloadStatus(video, "Queued"),
            video = video
        )
        _activeDownloadTasks.value = _activeDownloadTasks.value + (videoKey to initialTask)
        _activeDownloads.value = _activeDownloads.value + (videoKey to 0f)

        synchronized(downloadQueue) {
            if (prioritize) downloadQueue.add(0, video) else downloadQueue.add(video)
        }
        dev.anilbeesetti.nextplayer.core.common.CrashBreadcrumbs.recordDownload(initialTask.title, "Queued")
        showToast("Added to Downloads: ${initialTask.title}")
        processQueue()
        updateLocksAndService()
    }

    private fun processQueue() {
        updateLocksAndService()
        scope.launch {
            synchronized(downloadQueue) {
                while (activeJobs.size < MAX_CONCURRENT_DOWNLOADS && downloadQueue.isNotEmpty()) {
                    val nextVideo = downloadQueue.removeAt(0)
                    val nextKey = nextVideo.externalId ?: nextVideo.id.toString()
                    startDownloadJob(nextVideo, nextKey)
                }
            }
            updateLocksAndService()
        }
    }

    private fun startDownloadJob(video: Video, videoKey: String) {
        dev.anilbeesetti.nextplayer.core.common.CrashBreadcrumbs.recordDownload(video.nameWithExtension, "Started")
        val uri = Uri.parse(video.uriString)
        if (uri.scheme == "tdlib") {
            downloadViaTdlib(video, videoKey, uri)
        } else {
            val fallback = if (video.path.startsWith("http")) video.path else uri.getQueryParameter("fallback") ?: video.path
            downloadViaHttp(video, videoKey, fallback)
        }
    }

    private val pausedDownloads = ConcurrentHashMap<String, Video>()

    private fun resolvePrimaryKey(key: String): String {
        if (_activeDownloadTasks.value.containsKey(key)) return key
        for ((pKey, aliases) in activeDownloadAliases) {
            if (pKey == key || aliases.contains(key)) return pKey
        }
        return key
    }

    fun pauseDownload(videoKey: String) {
        val primaryKey = resolvePrimaryKey(videoKey)
        var pausedVideo: Video? = null
        synchronized(downloadQueue) {
            val idx = downloadQueue.indexOfFirst {
                val k = it.externalId ?: it.id.toString()
                k == primaryKey || k == videoKey
            }
            if (idx != -1) {
                pausedVideo = downloadQueue.removeAt(idx)
            }
        }
        val currentTask = _activeDownloadTasks.value[primaryKey] ?: _activeDownloadTasks.value[videoKey]
        if (pausedVideo == null) {
            pausedVideo = currentTask?.video
        }

        val tdlibFileId = activeTdlibFileIds[primaryKey] ?: activeTdlibFileIds[videoKey]
        if (tdlibFileId != null) {
            tdlibManager.toggleDownloadIsPaused(tdlibFileId, true)
            tdlibManager.unmarkExplicitFullDownload(tdlibFileId)
        }

        // Store in pausedDownloads BEFORE cancelling active job so cancellation handlers preserve paused state
        if (pausedVideo != null) {
            pausedDownloads[primaryKey] = pausedVideo!!
            pausedDownloads[videoKey] = pausedVideo!!
        }

        activeJobs.remove(primaryKey)?.cancel()
        activeJobs.remove(videoKey)?.cancel()

        if (currentTask != null) {
            val pausedTask = currentTask.copy(
                isPaused = true,
                status = "Paused",
                speedBytesPerSec = 0L
            )
            val currentProgress = _activeDownloads.value[primaryKey] ?: _activeDownloads.value[videoKey] ?: currentTask.progress
            val aliases = (pausedVideo ?: currentTask.video)?.let { getAllVideoKeys(it, currentTask.key) } ?: setOf(primaryKey, videoKey)
            registerActiveProgress(primaryKey, aliases, currentProgress, pausedTask)
        }
        dev.anilbeesetti.nextplayer.core.common.CrashBreadcrumbs.recordDownload(pausedVideo?.nameWithExtension ?: videoKey, "Paused")
        processQueue()
        updateLocksAndService()
        showToast("Download paused")
    }

    fun resumeDownload(videoKey: String) {
        val primaryKey = resolvePrimaryKey(videoKey)
        val video = pausedDownloads.remove(primaryKey)
            ?: pausedDownloads.remove(videoKey)
            ?: _activeDownloadTasks.value[primaryKey]?.video
            ?: _activeDownloadTasks.value[videoKey]?.video
            ?: return

        dev.anilbeesetti.nextplayer.core.common.CrashBreadcrumbs.recordDownload(video.nameWithExtension, "Resumed")

        val tdlibFileId = activeTdlibFileIds[primaryKey] ?: activeTdlibFileIds[videoKey]
        if (tdlibFileId != null) {
            tdlibManager.toggleDownloadIsPaused(tdlibFileId, false)
        }

        val currentTask = _activeDownloadTasks.value[primaryKey] ?: _activeDownloadTasks.value[videoKey]
        if (currentTask != null) {
            val resumingTask = currentTask.copy(
                isPaused = false,
                status = "Resuming..."
            )
            val currentProgress = _activeDownloads.value[primaryKey] ?: _activeDownloads.value[videoKey] ?: currentTask.progress
            val aliases = getAllVideoKeys(video, primaryKey)
            registerActiveProgress(primaryKey, aliases, currentProgress, resumingTask)
        }

        synchronized(downloadQueue) {
            val existing = downloadQueue.any {
                val k = it.externalId ?: it.id.toString()
                k == primaryKey || k == videoKey
            }
            if (!existing) {
                downloadQueue.add(0, video)
            }
        }
        processQueue()
        updateLocksAndService()
        showToast("Resuming download")
    }

    fun resumeDownload(video: Video) {
        val key = video.externalId ?: video.id.toString()
        val primaryKey = resolvePrimaryKey(key)
        if (!pausedDownloads.containsKey(primaryKey)) {
            pausedDownloads[primaryKey] = video
        }
        if (!pausedDownloads.containsKey(key)) {
            pausedDownloads[key] = video
        }
        resumeDownload(primaryKey)
    }

    fun isDownloadPaused(videoKey: String): Boolean {
        val primaryKey = resolvePrimaryKey(videoKey)
        return _activeDownloadTasks.value[primaryKey]?.isPaused == true ||
               _activeDownloadTasks.value[videoKey]?.isPaused == true ||
               pausedDownloads.containsKey(primaryKey) ||
               pausedDownloads.containsKey(videoKey)
    }

    fun isDownloadPaused(video: Video): Boolean {
        val keys = getAllVideoKeys(video)
        return keys.any { isDownloadPaused(it) }
    }

    fun pauseAllDownloads() {
        val keys = _activeDownloadTasks.value.filter { !it.value.isPaused }.keys.toList()
        for (k in keys) {
            pauseDownload(k)
        }
        updateLocksAndService()
    }

    fun resumeAllDownloads() {
        val pausedKeys = (pausedDownloads.keys().toList() + _activeDownloadTasks.value.filter { it.value.isPaused }.keys).distinct()
        for (k in pausedKeys) {
            resumeDownload(k)
        }
        updateLocksAndService()
    }

    fun cancelAllDownloads() {
        val allKeys = (_activeDownloadTasks.value.keys + pausedDownloads.keys().toList()).distinct()
        for (k in allKeys) {
            cancelDownload(k)
        }
        updateLocksAndService()
    }

    fun cancelDownload(videoKey: String) {
        val primaryKey = resolvePrimaryKey(videoKey)
        pausedDownloads.remove(primaryKey)
        pausedDownloads.remove(videoKey)
        synchronized(downloadQueue) {
            downloadQueue.removeAll {
                val k = it.externalId ?: it.id.toString()
                k == primaryKey || k == videoKey
            }
        }
        val tdlibFileId = activeTdlibFileIds.remove(primaryKey) ?: activeTdlibFileIds.remove(videoKey)
        if (tdlibFileId != null) {
            tdlibManager.unmarkExplicitFullDownload(tdlibFileId)
            tdlibManager.cancelDownload(tdlibFileId)
            tdlibManager.unregisterFileListener(tdlibFileId)
        }
        activeJobs.remove(primaryKey)?.cancel()
        activeJobs.remove(videoKey)?.cancel()
        val currentTask = _activeDownloadTasks.value[primaryKey] ?: _activeDownloadTasks.value[videoKey]
        currentTask?.video?.let { video ->
            clearGalleryExport(video)
            autoSaveToGalleryKeys.removeAll(galleryKeys(video))
        }
        removeActiveProgress(primaryKey)
        removeActiveProgress(videoKey)
        try {
            val candidateNames = mutableSetOf(primaryKey, videoKey)
            currentTask?.title?.let { candidateNames.add(it) }
            currentTask?.video?.let { v ->
                candidateNames.addAll(getAllVideoKeys(v))
                candidateNames.add(sanitizeFileName(v.nameWithExtension))
            }
            val dirs = listOfNotNull(
                offlineDir,
                context.filesDir,
                context.getExternalFilesDir(OFFLINE_FOLDER)
            )
            for (dir in dirs) {
                dir.listFiles()?.filter { f ->
                    (f.name.endsWith(".part") || f.name.endsWith(".segments")) && candidateNames.any { cand ->
                        cand.isNotBlank() && (f.name == "$cand.part" || f.name == "$cand.part.segments" || f.name.contains(cand) || cand.contains(f.name.removeSuffix(".part").removeSuffix(".segments")))
                    }
                }?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
        processQueue()
        updateLocksAndService()
        showToast("Download cancelled")
    }

    private suspend fun handleSuccessfulDownload(
        srcFile: File,
        targetFile: File,
        partFile: File?,
        safeName: String,
        rawName: String,
        videoKey: String,
        video: Video,
        fileId: String,
        aliases: Set<String>
    ) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (!srcFile.exists() || srcFile.length() <= 0L) return@withContext
        val totalSize = srcFile.length()

        var success = false
        targetFile.parentFile?.mkdirs()

        if (srcFile.absolutePath == targetFile.absolutePath) {
            success = true
        } else {
            // Attempt fast atomic rename if on the same filesystem (0ms)
            try {
                if (targetFile.exists()) targetFile.delete()
                if (srcFile.renameTo(targetFile)) {
                    success = true
                    Log.i(TAG, "Atomic rename succeeded for $safeName")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Atomic rename failed, falling back to buffered stream copy: ${e.message}")
            }
        }

        // If cross-filesystem, copy via buffered stream with active progress updates so OS never kills service
        if (!success) {
            try {
                val buf = ByteArray(512 * 1024)
                var copied = 0L
                var lastUpdateMs = System.currentTimeMillis()

                srcFile.inputStream().buffered(512 * 1024).use { input ->
                    targetFile.outputStream().buffered(512 * 1024).use { output ->
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            output.write(buf, 0, n)
                            copied += n
                            val now = System.currentTimeMillis()
                            if (now - lastUpdateMs >= 400L) {
                                lastUpdateMs = now
                                val saveProgress = if (totalSize > 0) (copied.toFloat() / totalSize.toFloat()).coerceIn(0.95f, 0.99f) else 0.99f
                                val task = DownloadTask(
                                    key = videoKey,
                                    title = safeName,
                                    progress = saveProgress,
                                    downloadedBytes = copied,
                                    totalBytes = totalSize,
                                    speedBytesPerSec = 0L,
                                    status = "Saving: ${formatFileSize(copied)} / ${formatFileSize(totalSize)}",
                                    video = video,
                                    source = "Storage Finalizer"
                                )
                                registerActiveProgress(videoKey, aliases, saveProgress, task)
                            }
                        }
                        output.flush()
                    }
                }
                if (targetFile.exists() && targetFile.length() >= (totalSize * 0.95).toLong()) {
                    success = true
                    // Delete internal TDLib cache file now that it's safely copied, avoiding 2x disk usage
                    try { srcFile.delete() } catch (_: Exception) {}
                } else {
                    throw IOException("Target file incomplete after copy: target=${targetFile.length()} vs src=$totalSize")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error copying offline file to primary target, trying internal storage fallback", e)
                try {
                    val internalOffline = File(context.filesDir, OFFLINE_FOLDER).apply { mkdirs() }
                    val internalTarget = File(internalOffline, safeName)
                    if (srcFile.absolutePath != internalTarget.absolutePath) {
                        srcFile.copyTo(internalTarget, overwrite = true)
                    }
                    success = true
                } catch (e2: Exception) {
                    Log.e(TAG, "Error copying offline file to internal storage fallback", e2)
                }
            }
        }

        try { partFile?.delete() } catch (_: Exception) {}

        try {
            val effectiveTarget = if (targetFile.exists() && targetFile.length() > 0) targetFile else srcFile
            val thumbFile = File(effectiveTarget.parentFile, "$safeName.thumb.jpg")
            if (!thumbFile.exists() || thumbFile.length() == 0L) {
                extractVideoThumbnail(effectiveTarget, thumbFile)
            }
            MediaScannerConnection.scanFile(context, arrayOf(effectiveTarget.absolutePath), null, null)
        } catch (_: Exception) {}

        keyToFileMap[videoKey] = safeName
        keyToFileMap[video.id.toString()] = safeName
        video.externalId?.let { keyToFileMap[it] = safeName }
        if (fileId.isNotEmpty()) keyToFileMap[fileId] = safeName

        val newKeys = mutableSetOf(safeName, rawName, videoKey, video.id.toString())
        video.externalId?.let { newKeys.add(it) }
        if (fileId.isNotEmpty()) newKeys.add(fileId)
        newKeys.add(safeName.removeSuffix(".mp4"))
        newKeys.add("$safeName.mp4")
        newKeys.add(rawName.removeSuffix(".mp4"))
        newKeys.add("$rawName.mp4")
        _downloadedKeys.value = _downloadedKeys.value + newKeys
        _failedDownloads.value = _failedDownloads.value - videoKey

        showToast("Download Complete: $safeName")
        TelegramDownloadService.showCompletionNotification(context, safeName, videoKey)

        if (consumeGalleryExportRequest(video)) {
            val effectiveTarget = if (targetFile.exists()) targetFile else srcFile
            copyFileToGallery(effectiveTarget, safeName, video, videoKey, aliases)
        }
        refreshDownloadedKeys()
    }

    private fun downloadViaTdlib(video: Video, videoKey: String, uri: Uri) {
        val chatId = uri.getQueryParameter("chatId")?.toLongOrNull()
        val messageId = uri.getQueryParameter("messageId")?.toLongOrNull()
        val fileId = uri.getQueryParameter("fileId") ?: ""

        if ((chatId == null || messageId == null) && fileId.isEmpty()) {
            val fallbackUrl = uri.getQueryParameter("fallback") ?: video.path
            downloadViaHttp(video, videoKey, fallbackUrl)
            return
        }

        val rawName = video.nameWithExtension.ifBlank { "video_${video.id}.mp4" }
        val safeName = sanitizeFileName(rawName)
        val targetFile = File(offlineDir, safeName)
        val partFile = File(offlineDir, "$safeName.part")
        val aliases = getAllVideoKeys(video, fileId)

        val task = DownloadTask(
            key = videoKey,
            title = safeName,
            progress = 0.01f,
            totalBytes = if (video.size > 0) video.size else (uri.getQueryParameter("size")?.toLongOrNull() ?: 0L),
            status = downloadStatus(video, "Connecting MTProto..."),
            video = video,
            source = "TDLib MTProto"
        )
        registerActiveProgress(videoKey, aliases, 0.01f, task)

        var lastUpdateTime = System.currentTimeMillis()
        var lastDownloadedBytes = 0L

        val job = scope.launch {
            var activeTdlibId: Int? = null
            var watchdogJob: Job? = null
            var listenerRef: ((TdApi.File) -> Unit)? = null
            var shouldFallbackToHttp: String? = null

            try {
                // Ensure TDLib is ready, awaiting up to 5s if still initializing
                if (!tdlibManager.isReady()) {
                    withTimeoutOrNull(5000L) {
                        tdlibManager.authState.filter { it is TdlibManager.AuthState.Ready }.first()
                    }
                }
                if (!tdlibManager.isReady()) {
                    Log.w(TAG, "TDLib not ready, checking for HTTP fallback URL...")
                    val fallbackUrl = uri.getQueryParameter("fallback")
                        ?: getFallbackUrl(videoKey)
                        ?: getFallbackUrl(safeName)
                        ?: (if (video.path.startsWith("http")) video.path else null)

                    val isTelegramBotApi = fallbackUrl?.contains("api.telegram.org") == true
                    val canFallbackToHttp = !fallbackUrl.isNullOrBlank() &&
                        fallbackUrl.startsWith("http") &&
                        (!isTelegramBotApi || (video.size in 1..(20 * 1024 * 1024L)))

                    if (canFallbackToHttp) {
                        Log.i(TAG, "TDLib not ready - seamlessly falling back to HTTP download for $safeName: $fallbackUrl")
                        downloadViaHttp(video, videoKey, fallbackUrl)
                        return@launch
                    }
                    if (!fallbackUrl.isNullOrBlank() && fallbackUrl.startsWith("http")) {
                        downloadViaHttp(video, videoKey, fallbackUrl)
                        return@launch
                    }
                    throw IOException("TDLib is not ready and no HTTP streaming fallback available. Please check Telegram connection.")
                }

                // Apply peak MTProto download booster settings (Nagram/CherryGram optimization)
                tdlibManager.optimizeNetworkForDownload()
                tdlibManager.keepAliveForBackgroundDownload()
                updateLocksAndService()

                // Ensure chat is cached first if chatId is available
                if (chatId != null) {
                    tdlibManager.ensureChatSuspend(chatId)
                }

                val channelUsername = uri.getQueryParameter("user") ?: uri.getQueryParameter("username")
                val fileInfo = tdlibMediaResolver.resolveFile(
                    chatId = chatId,
                    messageId = messageId,
                    fileId = fileId,
                    channelUsername = channelUsername,
                    expectedFileSize = video.size
                )
                if (fileInfo == null) {
                    Log.w(TAG, "TDLib resolveFile returned null for $safeName")
                    val fallbackUrl = uri.getQueryParameter("fallback")
                        ?: getFallbackUrl(videoKey)
                        ?: getFallbackUrl(safeName)
                        ?: (if (video.path.startsWith("http")) video.path else null)

                    val isTelegramBotApi = fallbackUrl?.contains("api.telegram.org") == true
                    val canFallbackToHttp = !fallbackUrl.isNullOrBlank() &&
                        fallbackUrl.startsWith("http") &&
                        (!isTelegramBotApi || (video.size in 1..(20 * 1024 * 1024L)))

                    if (canFallbackToHttp) {
                        Log.i(TAG, "Falling back to HTTP download for $safeName: $fallbackUrl")
                        downloadViaHttp(video, videoKey, fallbackUrl)
                        return@launch
                    }

                    if (isTelegramBotApi && video.size > 20 * 1024 * 1024L) {
                        throw IOException("Telegram Bot API direct download limit is 20MB. Could not resolve file in Telegram.")
                    }
                    if (!fallbackUrl.isNullOrBlank() && fallbackUrl.startsWith("http")) {
                        downloadViaHttp(video, videoKey, fallbackUrl)
                        return@launch
                    }
                    val defaultFallback = uri.getQueryParameter("fallback") ?: video.path
                    if (defaultFallback.startsWith("http")) {
                        downloadViaHttp(video, videoKey, defaultFallback)
                        return@launch
                    }
                    throw IOException("Could not resolve file in Telegram and no HTTP streaming fallback available.")
                }

                if (fileInfo.isCompleted && fileInfo.localPath.isNotEmpty() && File(fileInfo.localPath).exists() && File(fileInfo.localPath).length() > 0) {
                    handleSuccessfulDownload(
                        srcFile = File(fileInfo.localPath),
                        targetFile = targetFile,
                        partFile = partFile,
                        safeName = safeName,
                        rawName = rawName,
                        videoKey = videoKey,
                        video = video,
                        fileId = fileId,
                        aliases = aliases
                    )
                    activeTdlibFileIds.remove(videoKey)
                    activeJobs.remove(videoKey)
                    removeActiveProgress(videoKey)
                    processQueue()
                    updateLocksAndService()
                    return@launch
                }

                // If TDLib already has partial bytes downloaded, initialize progress immediately so UI doesn't reset to 0%
                if (fileInfo.downloadedSize > 0L) {
                    val total = if (fileInfo.fileSize > 0) fileInfo.fileSize else video.size
                    val resumeProgress = if (total > 0) (fileInfo.downloadedSize.toFloat() / total.toFloat()).coerceIn(0.01f, 0.99f) else 0.01f
                    val resumeTask = task.copy(
                        downloadedBytes = fileInfo.downloadedSize,
                        totalBytes = total,
                        progress = resumeProgress,
                        status = downloadStatus(video, "Resuming MTProto (${(resumeProgress * 100).toInt()}%)...")
                    )
                    registerActiveProgress(videoKey, aliases, resumeProgress, resumeTask)
                    lastDownloadedBytes = fileInfo.downloadedSize
                }

                activeTdlibId = fileInfo.fileId
                activeTdlibFileIds[videoKey] = fileInfo.fileId

                var lastProgressTimestamp = System.currentTimeMillis()
                var lastProgressBytes = fileInfo.downloadedSize
                var lastPingTimestamp = 0L
                var lastAutoResumeTime = 0L
                var pingCount = 0
                var hasCompleted = false
                var completedPath: String? = null
                var smoothSpeed = 0L
                var isFileDownloadingActive = true

                // Keep coroutine active until download completes or is cancelled
                suspendCancellableCoroutine<Unit> { cont ->
                    val listener: (TdApi.File) -> Unit = { f ->
                        if (f.id == fileInfo.fileId && !hasCompleted) {
                            isFileDownloadingActive = f.local.isDownloadingActive
                            // Auto-resume if inactive and can be downloaded (debounced)
                            if (!f.local.isDownloadingCompleted && !f.local.isDownloadingActive && f.local.canBeDownloaded) {
                                val nowMs = System.currentTimeMillis()
                                if (nowMs - lastAutoResumeTime >= 2000L) {
                                    lastAutoResumeTime = nowMs
                                    Log.w(TAG, "TDLib file ${f.id} download inactive, immediately re-requesting downloadFile")
                                    tdlibManager.downloadFile(fileInfo.fileId, offset = 0, limit = 0, priority = 32)
                                    tdlibManager.addFileToDownloads(fileInfo.fileId, chatId ?: 0L, messageId ?: 0L, priority = 32)
                                }
                            }

                            val diskBytes = if (f.local.path.isNotEmpty()) {
                                try { File(f.local.path).length() } catch (_: Exception) { 0L }
                            } else 0L
                            val downloaded = maxOf(f.local.downloadedSize, f.local.downloadedPrefixSize, diskBytes)
                            val total = when {
                                f.size > 0 -> f.size
                                video.size > 0 -> video.size
                                fileInfo.fileSize > 65536L -> fileInfo.fileSize
                                f.expectedSize > 65536L -> f.expectedSize
                                else -> 0L
                            }

                            if (downloaded > lastProgressBytes) {
                                lastProgressBytes = downloaded
                                lastProgressTimestamp = System.currentTimeMillis()
                                pingCount = 0
                            }

                            val isDone = f.local.isDownloadingCompleted && f.local.path.isNotEmpty() && File(f.local.path).exists() && File(f.local.path).length() > 0
                            val progress = if (isDone) 1.0f else if (total > 0) (downloaded.toFloat() / total.toFloat()).coerceIn(0.01f, 0.99f) else 0.05f

                            val now = System.currentTimeMillis()
                            val timeDiff = (now - lastUpdateTime).coerceAtLeast(1L)

                            if (timeDiff >= 250 || isDone) {
                                val bytesDiff = (downloaded - lastDownloadedBytes).coerceAtLeast(0L)
                                val instantSpeed = if (timeDiff > 0) (bytesDiff * 1000L / timeDiff) else 0L
                                smoothSpeed = if (smoothSpeed == 0L) instantSpeed else ((smoothSpeed * 7 + instantSpeed * 3) / 10)
                                lastUpdateTime = now
                                lastDownloadedBytes = downloaded

                                val isNearCompletion = isDone || (total > 0 && downloaded >= total)
                                val status = if (isDone || isNearCompletion) "Finalizing..." else "Downloading (MTProto Turbo)"
                                registerActiveProgress(
                                    videoKey = videoKey,
                                    aliases = aliases,
                                    progress = progress,
                                    task = DownloadTask(
                                        key = videoKey,
                                        title = safeName,
                                        progress = progress,
                                        downloadedBytes = downloaded,
                                        totalBytes = total,
                                        speedBytesPerSec = if (isDone) 0L else smoothSpeed,
                                        status = downloadStatus(video, status),
                                        video = video,
                                        source = "TDLib MTProto"
                                    )
                                )
                            }

                            if (isDone) {
                                hasCompleted = true
                                completedPath = f.local.path
                                watchdogJob?.cancel()
                                listenerRef?.let { tdlibManager.unregisterFileListener(fileInfo.fileId, it) }
                                tdlibManager.unmarkExplicitFullDownload(fileInfo.fileId)
                                if (cont.isActive) cont.resume(Unit)
                            }
                        }
                    }
                    listenerRef = listener

                    tdlibManager.markExplicitFullDownload(fileInfo.fileId)
                    tdlibManager.registerFileListener(fileInfo.fileId, listener)
                    tdlibManager.addFileToDownloads(fileInfo.fileId, chatId ?: 0L, messageId ?: 0L, priority = 32)
                    tdlibManager.downloadFile(fileInfo.fileId, offset = 0, limit = 0, priority = 32)

                    // Launch watchdog coroutine to detect stalls and keep MTProto transfer active across screen lock
                    watchdogJob = launch {
                        while (isActive && !hasCompleted) {
                            delay(1000L)
                            if (hasCompleted) break

                            val now = System.currentTimeMillis()
                            val timeSinceProgress = now - lastProgressTimestamp

                            // Responsive ping: if no progress for 8s, ping TDLib and check active status
                            if (timeSinceProgress >= 8000L && (now - lastPingTimestamp >= 6000L)) {
                                lastPingTimestamp = now
                                pingCount++
                                Log.w(TAG, "Download silent for ${timeSinceProgress / 1000}s on $safeName (fileId=${fileInfo.fileId}, pingCount=$pingCount). Re-energizing MTProto transfer...")

                                tdlibManager.optimizeNetworkForDownload()
                                tdlibManager.keepAliveForBackgroundDownload()
                                tdlibManager.addFileToDownloads(fileInfo.fileId, chatId ?: 0L, messageId ?: 0L, priority = 32)
                                tdlibManager.downloadFile(fileInfo.fileId, offset = 0, limit = 0, priority = 32)
                                tdlibManager.getFile(fileInfo.fileId) { file ->
                                    if (file != null) {
                                        isFileDownloadingActive = file.local.isDownloadingActive
                                        if (file.local.isDownloadingCompleted && file.local.path.isNotEmpty() && File(file.local.path).exists()) {
                                            listener(file)
                                        } else if (!file.local.isDownloadingCompleted && !file.local.isDownloadingActive && file.local.canBeDownloaded) {
                                            Log.w(TAG, "Watchdog: File inactive for ${file.id}, re-triggering downloadFile and addFileToDownloads")
                                            tdlibManager.downloadFile(file.id, offset = 0, limit = 0, priority = 32)
                                            tdlibManager.addFileToDownloads(file.id, chatId ?: 0L, messageId ?: 0L, priority = 32)
                                        }
                                    }
                                }

                                val statusText = if (isFileDownloadingActive) {
                                    "Optimizing MTProto transfer..."
                                } else {
                                    "Reconnecting MTProto..."
                                }
                                val stalledTask = _activeDownloadTasks.value[videoKey]?.copy(
                                    speedBytesPerSec = 0L,
                                    status = statusText
                                )
                                if (stalledTask != null) {
                                    val currentProgress = _activeDownloads.value[videoKey] ?: 0f
                                    registerActiveProgress(videoKey, aliases, currentProgress, stalledTask)
                                }
                            }

                            // Strictly protect large videos / channel videos: NEVER fall back to HTTP for files >= 20MB!
                            // Telegram Bot API limit is 20MB, and Cloudflare Worker cannot handle multi-gigabyte video streams.
                            val isChannelOrLargeVideo = (video.size <= 0L || video.size >= 20 * 1024 * 1024L) || uri.scheme == "tdlib"
                            if (!isChannelOrLargeVideo && timeSinceProgress >= 90000L && !isFileDownloadingActive) {
                                val uriFallback = uri.getQueryParameter("fallback")?.let {
                                    try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
                                } ?: uri.getQueryParameter("fallback")

                                val extId = video.externalId?.takeIf { it.isNotBlank() } ?: fileId.takeIf { it.isNotBlank() }
                                val rawFallback = uriFallback
                                    ?: getFallbackUrl(videoKey)
                                    ?: getFallbackUrl(safeName)
                                    ?: getFallbackUrl(rawName)
                                    ?: (if (!extId.isNullOrEmpty()) NetworkConfig.streamUrl(extId) else null)
                                    ?: (if (video.path.startsWith("http")) video.path else null)

                                val canFallbackToHttp = !rawFallback.isNullOrBlank() &&
                                    rawFallback.startsWith("http") &&
                                    !rawFallback.contains("api.telegram.org") &&
                                    video.size in 1..(20 * 1024 * 1024L)

                                if (canFallbackToHttp && cont.isActive) {
                                    Log.w(TAG, "Seamlessly falling back to HTTP download for small file $safeName: $rawFallback")
                                    shouldFallbackToHttp = rawFallback
                                    cont.resume(Unit)
                                    break
                                }
                            }

                            if (timeSinceProgress >= 90000L) {
                                Log.e(TAG, "TDLib MTProto download completely stalled after 90s: $safeName")
                                cont.resumeWithException(DownloadStalledException("Download stalled: MTProto connection stopped receiving data. Please tap retry."))
                                break
                            }
                        }
                    }

                    cont.invokeOnCancellation {
                        watchdogJob?.cancel()
                        tdlibManager.unmarkExplicitFullDownload(fileInfo.fileId)
                        tdlibManager.unregisterFileListener(fileInfo.fileId, listener)
                        if (!isDownloadPaused(videoKey)) {
                            removeActiveProgress(videoKey)
                        }
                    }
                }

                // Safely execute file saving on Dispatchers.IO while service & job remain 100% active
                if (hasCompleted && !completedPath.isNullOrEmpty()) {
                    handleSuccessfulDownload(
                        srcFile = File(completedPath!!),
                        targetFile = targetFile,
                        partFile = partFile,
                        safeName = safeName,
                        rawName = rawName,
                        videoKey = videoKey,
                        video = video,
                        fileId = fileId,
                        aliases = aliases
                    )
                    activeTdlibFileIds.remove(videoKey)
                    activeJobs.remove(videoKey)
                    removeActiveProgress(videoKey)
                    processQueue()
                    updateLocksAndService()
                    return@launch
                }

                if (shouldFallbackToHttp != null) {
                    watchdogJob?.cancel()
                    listenerRef?.let { tdlibManager.unregisterFileListener(fileInfo.fileId, it) }
                    tdlibManager.unmarkExplicitFullDownload(fileInfo.fileId)
                    activeTdlibFileIds.remove(videoKey)
                    activeJobs.remove(videoKey)
                    removeActiveProgress(videoKey)
                    showToast("MTProto connection stalled. Switching to HTTP...")
                    downloadViaHttp(video, videoKey, shouldFallbackToHttp!!)
                    return@launch
                }
            } catch (e: Exception) {
                watchdogJob?.cancel()
                if (isDownloadPaused(videoKey)) {
                    return@launch
                }
                // DO NOT cancel TDLib download! TDLib preserves downloaded parts in local database
                activeTdlibId?.let {
                    tdlibManager.unmarkExplicitFullDownload(it)
                }
                if (e is kotlinx.coroutines.CancellationException) {
                    removeActiveProgress(videoKey)
                    updateLocksAndService()
                    return@launch
                }
                Log.e(TAG, "TDLib download error", e)
                val advice = if (e is DownloadStalledException) {
                    "Download Stalled: MTProto connection stopped receiving data for over 60s. Krypton Player cached downloaded parts in TDLib; tap Retry to resume downloading."
                } else {
                    "TDLib MTProto Error: Unable to download file via Telegram MTProto (${e.message}). Check internet connectivity or network restrictions."
                }
                val errorInfo = DownloadErrorInfo(
                    key = videoKey,
                    timestamp = System.currentTimeMillis(),
                    fileName = safeName,
                    fileId = video.externalId,
                    downloadUrl = uri.toString(),
                    errorCode = e.javaClass.simpleName,
                    errorMessage = e.message ?: "TDLib download failed",
                    rootCause = e.cause?.toString() ?: (e.message ?: "No cause"),
                    stackTrace = Log.getStackTraceString(e),
                    downloadedBytes = lastDownloadedBytes,
                    totalBytes = video.size,
                    diagnosticAdvice = advice,
                    video = video
                )
                _failedDownloads.value = _failedDownloads.value + (videoKey to errorInfo)
                clearGalleryExport(video)
                autoSaveToGalleryKeys.removeAll(galleryKeys(video))
                activeJobs.remove(videoKey)
                removeActiveProgress(videoKey)
                val toastMsg = if (e is DownloadStalledException) e.message ?: "Download stalled. Tap to retry" else "Download failed: ${e.message}"
                showToast(toastMsg)
                processQueue()
                updateLocksAndService()
            } finally {
                watchdogJob?.cancel()
                listenerRef?.let { activeTdlibId?.let { id -> tdlibManager.unregisterFileListener(id, it) } }
                updateLocksAndService()
            }
        }
        activeJobs[videoKey] = job
    }

    private fun loadSegmentProgress(stateFile: File, expectedTotalBytes: Long): Pair<Int, LongArray>? {
        if (!stateFile.exists()) return null
        return try {
            val lines = stateFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 3) return null
            val total = lines[0].toLongOrNull() ?: return null
            if (total != expectedTotalBytes) return null
            val count = lines[1].toIntOrNull() ?: return null
            if (count !in 2..8 || lines.size < 2 + count) return null
            val result = LongArray(count)
            for (i in 0 until count) {
                result[i] = lines[2 + i].toLongOrNull() ?: 0L
            }
            Pair(count, result)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveSegmentProgress(stateFile: File, totalBytes: Long, segmentProgress: AtomicLongArray) {
        try {
            val count = segmentProgress.length()
            val sb = StringBuilder()
            sb.append(totalBytes).append("\n")
            sb.append(count).append("\n")
            for (i in 0 until count) {
                sb.append(segmentProgress.get(i)).append("\n")
            }
            val tmp = File(stateFile.parentFile, "${stateFile.name}.tmp")
            tmp.writeText(sb.toString())
            if (tmp.exists()) {
                if (stateFile.exists()) stateFile.delete()
                tmp.renameTo(stateFile)
            }
        } catch (_: Exception) {}
    }

    private fun probeUrlAndRange(
        initialUrl: String
    ): Triple<String, Long, Boolean> {
        var currentUrl = initialUrl
        var redirectCount = 0
        var totalBytes = -1L
        var supportsRange = false

        while (redirectCount < 5) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(currentUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "KryptonPlayer-Downloader/3.0")
                    setRequestProperty("Accept-Encoding", "identity")
                    setRequestProperty("Range", "bytes=0-0")
                }
                conn.connect()
                val code = conn.responseCode
                if (code in 301..308) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!loc.isNullOrEmpty()) {
                        val nextUrl = try {
                            URL(URL(currentUrl), loc).toString()
                        } catch (_: Exception) {
                            loc
                        }
                        currentUrl = nextUrl
                        redirectCount++
                        continue
                    }
                }
                if (code == 206) {
                    supportsRange = true
                    val cr = conn.getHeaderField("Content-Range")
                    if (!cr.isNullOrEmpty()) {
                        val totalMatch = Regex("/(\\d+)").find(cr)
                        if (totalMatch != null) {
                            totalBytes = totalMatch.groupValues[1].toLongOrNull() ?: -1L
                        }
                    }
                } else if (code in 200..299) {
                    supportsRange = false
                    val len = conn.contentLengthLong
                    if (len > 0) totalBytes = len
                }
                try { conn.inputStream?.close() } catch (_: Exception) {}
                conn.disconnect()
                break
            } catch (_: Exception) {
                try { conn?.disconnect() } catch (_: Exception) {}
                break
            }
        }
        return Triple(currentUrl, totalBytes, supportsRange)
    }

    private suspend fun downloadSegmented(
        contentUrl: String,
        totalBytes: Long,
        partFile: File,
        stateFile: File,
        video: Video,
        videoKey: String,
        safeName: String,
        rawName: String,
        targetFile: File,
        aliases: Set<String>,
        currentSource: String
    ): Boolean = coroutineScope {
        val savedInfo = if (partFile.exists() && partFile.length() == totalBytes) {
            loadSegmentProgress(stateFile, totalBytes)
        } else null

        val numWorkers = savedInfo?.first ?: when {
            totalBytes >= 100 * 1024 * 1024L -> 4
            totalBytes >= 20 * 1024 * 1024L -> 3
            else -> 2
        }

        val segmentStarts = LongArray(numWorkers)
        val segmentEnds = LongArray(numWorkers)
        val segmentLengths = LongArray(numWorkers)
        val baseChunk = totalBytes / numWorkers

        for (i in 0 until numWorkers) {
            segmentStarts[i] = i * baseChunk
            segmentEnds[i] = if (i == numWorkers - 1) totalBytes - 1 else (i + 1) * baseChunk - 1
            segmentLengths[i] = segmentEnds[i] - segmentStarts[i] + 1
        }

        val segmentDownloaded = AtomicLongArray(numWorkers)
        var initialSum = 0L
        if (savedInfo != null) {
            for (i in 0 until numWorkers) {
                val downloaded = savedInfo.second[i].coerceAtMost(segmentLengths[i])
                segmentDownloaded.set(i, downloaded)
                initialSum += downloaded
            }
        } else {
            // Allocate full file space atomically
            RandomAccessFile(partFile, "rw").use { it.setLength(totalBytes) }
            saveSegmentProgress(stateFile, totalBytes, segmentDownloaded)
        }

        val totalDownloadedAtomic = AtomicLong(initialSum)

        val monitorJob = launch {
            var lastUpdateMs = System.currentTimeMillis()
            var lastSavedMs = System.currentTimeMillis()
            var lastBytes = totalDownloadedAtomic.get()
            var smoothSpeed = 0L

            while (isActive) {
                delay(250L)
                val now = System.currentTimeMillis()
                val currentBytes = totalDownloadedAtomic.get()
                val timeDiff = (now - lastUpdateMs).coerceAtLeast(1L)
                val bytesDiff = (currentBytes - lastBytes).coerceAtLeast(0L)
                val instantSpeed = if (timeDiff > 0) (bytesDiff * 1000L / timeDiff) else 0L
                smoothSpeed = if (smoothSpeed == 0L) instantSpeed else ((smoothSpeed * 7 + instantSpeed * 3) / 10)
                lastUpdateMs = now
                lastBytes = currentBytes

                val progress = if (totalBytes > 0) (currentBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.01f, 0.99f) else 0.5f

                registerActiveProgress(
                    videoKey = videoKey,
                    aliases = aliases,
                    progress = progress,
                    task = DownloadTask(
                        key = videoKey,
                        title = safeName,
                        progress = progress,
                        downloadedBytes = currentBytes,
                        totalBytes = totalBytes,
                        speedBytesPerSec = smoothSpeed,
                        status = downloadStatus(video, "Downloading (Parallel: $numWorkers streams)"),
                        video = video,
                        source = "$currentSource (Turbo)"
                    )
                )

                if (now - lastSavedMs >= 2000L) {
                    lastSavedMs = now
                    saveSegmentProgress(stateFile, totalBytes, segmentDownloaded)
                }
            }
        }

        var segmentSuccess = false
        try {
            val workerJobs = (0 until numWorkers).map { index ->
                async(Dispatchers.IO) {
                    val segStart = segmentStarts[index]
                    val segEnd = segmentEnds[index]
                    val segExpected = segmentLengths[index]

                    var segAttempt = 0
                    var segDone = segmentDownloaded.get(index) >= segExpected

                    while (isActive && segAttempt < 5 && !segDone) {
                        var segConn: HttpURLConnection? = null
                        var segRaf: RandomAccessFile? = null
                        var inStream: InputStream? = null
                        try {
                            val currentDownloaded = segmentDownloaded.get(index)
                            val currentSegStart = segStart + currentDownloaded
                            if (currentSegStart > segEnd) {
                                segDone = true
                                break
                            }

                            val conn = (URL(contentUrl).openConnection() as HttpURLConnection).apply {
                                requestMethod = "GET"
                                connectTimeout = 30000
                                readTimeout = 60000
                                instanceFollowRedirects = true
                                setRequestProperty("User-Agent", "KryptonPlayer-Downloader/3.0 (Segmented; +https://github.com/anilbeesetti/nextplayer)")
                                setRequestProperty("Accept-Encoding", "identity")
                                setRequestProperty("Range", "bytes=$currentSegStart-$segEnd")
                            }
                            segConn = conn
                            conn.connect()
                            val code = conn.responseCode
                            // STRICT CHECK: Segment range requests must return 206 Partial Content.
                            // If the server returns 200, it sent the whole file, which would corrupt offsets.
                            if (code != 206) {
                                throw IOException("Segment $index HTTP error $code (expected 206 Partial Content): ${conn.responseMessage}")
                            }

                            val raf = RandomAccessFile(partFile, "rw")
                            segRaf = raf
                            raf.seek(currentSegStart)
                            inStream = conn.inputStream
                            val buf = ByteArray(256 * 1024) // 256 KB worker buffer
                            var writePos = currentSegStart

                            while (isActive && writePos <= segEnd) {
                                val maxRead = minOf(buf.size.toLong(), segEnd - writePos + 1).toInt()
                                val count = inStream.read(buf, 0, maxRead)
                                if (count == -1) break
                                raf.write(buf, 0, count)
                                writePos += count
                                segmentDownloaded.addAndGet(index, count.toLong())
                                totalDownloadedAtomic.addAndGet(count.toLong())
                            }

                            if (segmentDownloaded.get(index) >= segExpected) {
                                segDone = true
                            } else if (!isActive) {
                                break
                            } else {
                                throw IOException("Segment $index ended prematurely at ${segmentDownloaded.get(index)}/$segExpected")
                            }
                        } catch (e: Exception) {
                            if (!isActive || e is kotlinx.coroutines.CancellationException) break
                            segAttempt++
                            Log.w(TAG, "Segment $index attempt $segAttempt failed: ${e.message}. Retrying from ${segmentDownloaded.get(index)} bytes...")
                            delay(1000L * segAttempt.coerceAtMost(4))
                        } finally {
                            try { inStream?.close() } catch (_: Exception) {}
                            try { segRaf?.close() } catch (_: Exception) {}
                            try { segConn?.disconnect() } catch (_: Exception) {}
                        }
                    }

                    if (!segDone && isActive) {
                        throw IOException("Segment $index failed after 5 attempts")
                    }
                }
            }

            workerJobs.awaitAll()
            monitorJob.cancel()

            val allDone = (0 until numWorkers).all { segmentDownloaded.get(it) >= segmentLengths[it] }
            if (allDone && partFile.exists() && partFile.length() >= totalBytes) {
                try { stateFile.delete() } catch (_: Exception) {}

                if (targetFile.exists()) targetFile.delete()
                val renamed = partFile.renameTo(targetFile)
                if (!renamed || !targetFile.exists() || targetFile.length() == 0L) {
                    partFile.copyTo(targetFile, overwrite = true)
                    partFile.delete()
                }

                val thumbFile = File(offlineDir, "$safeName.thumb.jpg")
                if (!thumbFile.exists() || thumbFile.length() == 0L) {
                    extractVideoThumbnail(targetFile, thumbFile)
                }
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)

                keyToFileMap[videoKey] = safeName
                keyToFileMap[video.id.toString()] = safeName
                video.externalId?.let { keyToFileMap[it] = safeName }

                val newKeys = mutableSetOf(safeName, rawName, videoKey, video.id.toString())
                video.externalId?.let { newKeys.add(it) }
                newKeys.add(safeName.removeSuffix(".mp4"))
                newKeys.add("$safeName.mp4")
                newKeys.add(rawName.removeSuffix(".mp4"))
                newKeys.add("$rawName.mp4")
                _downloadedKeys.value = _downloadedKeys.value + newKeys
                _failedDownloads.value = _failedDownloads.value - videoKey
                showToast("Download Complete: $safeName")
                TelegramDownloadService.showCompletionNotification(context, safeName, videoKey)
                if (consumeGalleryExportRequest(video)) {
                    copyFileToGallery(targetFile, safeName, video, videoKey, aliases)
                }
                refreshDownloadedKeys()
                segmentSuccess = true
            }
        } finally {
            monitorJob.cancel()
            if (!segmentSuccess) {
                saveSegmentProgress(stateFile, totalBytes, segmentDownloaded)
            }
            updateLocksAndService()
        }
        segmentSuccess
    }

    private suspend fun downloadSingleStream(
        finalUrl: String,
        totalBytes: Long,
        partFile: File,
        stateFile: File,
        targetFile: File,
        video: Video,
        videoKey: String,
        safeName: String,
        rawName: String,
        aliases: Set<String>,
        currentSource: String
    ) {
        // Clean up legacy segment state file if present, but NEVER purge partially downloaded partFile
        if (stateFile.exists()) {
            try { stateFile.delete() } catch (_: Exception) {}
        }
        if (totalBytes > 0 && partFile.exists() && partFile.length() >= totalBytes) {
            try { partFile.delete() } catch (_: Exception) {}
        }

        var effectiveTotalBytes = totalBytes
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        var currentDownloaded = if (partFile.exists()) partFile.length() else 0L

        var attempt = 0
        val maxRetries = 10
        var downloadSuccess = false

        try {
            while (attempt < maxRetries && kotlinx.coroutines.currentCoroutineContext().isActive && !downloadSuccess) {
                try {
                    var existingBytes = if (partFile.exists()) partFile.length() else 0L
                    if (effectiveTotalBytes > 0 && existingBytes >= effectiveTotalBytes) {
                        try { partFile.delete() } catch (_: Exception) {}
                        existingBytes = 0L
                    }
                    currentDownloaded = existingBytes

                    var currentStreamUrl = finalUrl
                    var redirectCount = 0
                    var responseCode = -1

                    while (redirectCount < 5) {
                        val url = URL(currentStreamUrl)
                        connection = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 30000
                            readTimeout = 60000
                            instanceFollowRedirects = false
                            setRequestProperty("User-Agent", "KryptonPlayer-Downloader/3.0")
                            setRequestProperty("Accept-Encoding", "identity")
                            if (existingBytes > 0) {
                                setRequestProperty("Range", "bytes=$existingBytes-")
                            }
                        }
                        connection.connect()
                        responseCode = connection.responseCode

                        if (responseCode in 301..308) {
                            val location = connection.getHeaderField("Location")
                            try { connection.disconnect() } catch (_: Exception) {}
                            if (!location.isNullOrEmpty()) {
                                val nextLoc = try {
                                    URL(URL(currentStreamUrl), location).toString()
                                } catch (_: Exception) {
                                    location
                                }
                                currentStreamUrl = nextLoc
                                redirectCount++
                                continue
                            }
                        }
                        break
                    }

                    val activeSource = if (currentStreamUrl.contains("api.telegram.org")) "Telegram Direct" else currentSource

                    // Handle HTTP 416 (Range Not Satisfiable): Reset partial file and restart cleanly from byte 0
                    if (responseCode == 416) {
                        Log.w(TAG, "HTTP 416 Range Not Satisfiable for existingBytes=$existingBytes. Resetting and restarting from byte 0.")
                        try { connection?.disconnect() } catch (_: Exception) {}
                        connection = null
                        try { partFile.delete() } catch (_: Exception) {}
                        existingBytes = 0L
                        attempt++
                        delay(1000L)
                        continue
                    }

                    val isResume = (responseCode == 206 && existingBytes > 0)
                    if (!isResume && responseCode != 200) {
                        throw IOException("HTTP error $responseCode: ${connection?.responseMessage}")
                    }

                    if (!isResume) {
                        existingBytes = 0L
                        val len = connection?.contentLengthLong ?: -1L
                        if (len > 0) effectiveTotalBytes = len
                        outputStream = FileOutputStream(partFile, false)
                    } else {
                        val len = connection?.contentLengthLong ?: -1L
                        if (len > 0) effectiveTotalBytes = existingBytes + len
                        outputStream = FileOutputStream(partFile, true)
                    }

                    inputStream = connection?.inputStream
                    val buffer = ByteArray(256 * 1024) // 256 KB buffer for peak single-stream throughput
                    var bytesRead: Int
                    var downloadedBytes = existingBytes
                    var lastUpdateTime = System.currentTimeMillis()
                    var lastDownloaded = existingBytes

                    while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                        val read = inputStream?.read(buffer) ?: -1
                        if (read == -1) break
                        bytesRead = read
                        outputStream?.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        currentDownloaded = downloadedBytes

                        val now = System.currentTimeMillis()
                        val timeDiff = (now - lastUpdateTime).coerceAtLeast(1L)
                        if (timeDiff > 300) {
                            val speed = (downloadedBytes - lastDownloaded) * 1000L / timeDiff
                            lastUpdateTime = now
                            lastDownloaded = downloadedBytes
                            val progress = if (effectiveTotalBytes > 0) (downloadedBytes.toFloat() / effectiveTotalBytes.toFloat()).coerceIn(0.01f, 0.99f) else 0.5f

                            registerActiveProgress(
                                videoKey = videoKey,
                                aliases = aliases,
                                progress = progress,
                                task = DownloadTask(
                                    key = videoKey,
                                    title = safeName,
                                    progress = progress,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = effectiveTotalBytes,
                                    speedBytesPerSec = speed,
                                    status = downloadStatus(video, if (attempt > 0) "Resuming ($attempt/$maxRetries)" else "Downloading"),
                                    video = video,
                                    source = activeSource
                                )
                            )
                        }
                    }

                    outputStream?.flush()
                    try { outputStream?.close() } catch (_: Exception) {}
                    outputStream = null
                    try { inputStream?.close() } catch (_: Exception) {}
                    inputStream = null
                    try { connection?.disconnect() } catch (_: Exception) {}
                    connection = null

                    if (partFile.exists()) {
                        if (partFile.length() < MIN_VALID_VIDEO_SIZE) {
                            throw IOException("Premature stream cutoff: downloaded only ${partFile.length()} bytes, minimum required $MIN_VALID_VIDEO_SIZE bytes")
                        }
                        if (effectiveTotalBytes > 0 && partFile.length() < (effectiveTotalBytes * 0.95).toLong()) {
                            throw IOException("Premature stream cutoff: expected $effectiveTotalBytes bytes, received ${partFile.length()}")
                        }

                        if (targetFile.exists()) targetFile.delete()
                        val renamed = partFile.renameTo(targetFile)
                        if (!renamed || !targetFile.exists() || targetFile.length() == 0L) {
                            partFile.copyTo(targetFile, overwrite = true)
                            partFile.delete()
                        }
                        val thumbFile = File(offlineDir, "$safeName.thumb.jpg")
                        if (!thumbFile.exists() || thumbFile.length() == 0L) {
                            extractVideoThumbnail(targetFile, thumbFile)
                        }
                        MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)

                        keyToFileMap[videoKey] = safeName
                        keyToFileMap[video.id.toString()] = safeName
                        video.externalId?.let { keyToFileMap[it] = safeName }

                        val newKeys = mutableSetOf(safeName, rawName, videoKey, video.id.toString())
                        video.externalId?.let { newKeys.add(it) }
                        newKeys.add(safeName.removeSuffix(".mp4"))
                        newKeys.add("$safeName.mp4")
                        newKeys.add(rawName.removeSuffix(".mp4"))
                        newKeys.add("$rawName.mp4")
                        _downloadedKeys.value = _downloadedKeys.value + newKeys
                        _failedDownloads.value = _failedDownloads.value - videoKey
                        showToast("Download Complete: $safeName")
                        TelegramDownloadService.showCompletionNotification(context, safeName, videoKey)
                        if (consumeGalleryExportRequest(video)) {
                            copyFileToGallery(targetFile, safeName, video, videoKey, aliases)
                        }
                        refreshDownloadedKeys()
                        downloadSuccess = true
                        break
                    } else {
                        throw IOException("Empty or invalid downloaded file")
                    }
                } catch (e: Exception) {
                    try { outputStream?.close() } catch (_: Exception) {}
                    outputStream = null
                    try { inputStream?.close() } catch (_: Exception) {}
                    inputStream = null
                    try { connection?.disconnect() } catch (_: Exception) {}
                    connection = null

                    if (e is kotlinx.coroutines.CancellationException) {
                        break
                    }

                    attempt++
                    Log.w(TAG, "Download attempt $attempt/$maxRetries failed for $safeName: ${e.message}. Retrying and resuming from ${partFile.length()} bytes...")

                    if (attempt >= maxRetries) {
                        Log.e(TAG, "Download permanently failed after $maxRetries retries for $safeName", e)
                        val advice = when {
                            e is java.net.SocketTimeoutException || e.cause is java.net.SocketTimeoutException || e.message?.contains("timeout", ignoreCase = true) == true ->
                                "Connection Stalled / Timeout: The stream stopped responding after $maxRetries retries (likely Telegram Bot API rate limit or Cloudflare Worker duration limit). Krypton Player has saved $currentDownloaded bytes on disk; tap Retry to continue right where it left off."
                            e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException ->
                                "DNS Resolution Failed: Unable to resolve server hostname (${e.message}). The host may be blocked by your ISP, mobile carrier DNS, or private DNS. Try switching to Wi-Fi, disabling VPN, or changing Android Private DNS to dns.google or 1.1.1.1."
                            e.message?.contains("403") == true ->
                                "HTTP 403 Forbidden: Telegram streaming authorization expired. Please send /token to your bot to refresh access token."
                            e.message?.contains("404") == true ->
                                "HTTP 404 Not Found: The requested video was not found or deleted on Telegram."
                            e.message?.contains("Premature") == true || e.message?.contains("cutoff") == true ->
                                "Connection Interrupted: The stream was cut off before finishing. Krypton Player saved downloaded progress; tap Retry to resume."
                            e.message?.contains("ENOSPC") == true || e.message?.contains("No space") == true ->
                                "Device Storage Full: Not enough disk space to store this video."
                            else ->
                                "Download Failed (${e.javaClass.simpleName}): ${e.message ?: "Unknown error"}"
                        }

                        val errorInfo = DownloadErrorInfo(
                            key = videoKey,
                            timestamp = System.currentTimeMillis(),
                            fileName = safeName,
                            fileId = video.externalId,
                            downloadUrl = finalUrl,
                            errorCode = e.javaClass.simpleName,
                            errorMessage = e.message ?: "Unknown download failure",
                            rootCause = e.cause?.toString() ?: (e.message ?: "No root cause throwable"),
                            stackTrace = Log.getStackTraceString(e),
                            downloadedBytes = currentDownloaded,
                            totalBytes = effectiveTotalBytes,
                            diagnosticAdvice = advice,
                            video = video
                        )
                        _failedDownloads.value = _failedDownloads.value + (videoKey to errorInfo)
                        clearGalleryExport(video)
                        autoSaveToGalleryKeys.removeAll(galleryKeys(video))

                        showToast("Download failed: ${e.javaClass.simpleName}. Tap to view logs.")
                    } else {
                        delay(1000L * attempt.coerceAtMost(5))
                    }
                }
            }
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            try { connection?.disconnect() } catch (_: Exception) {}
            updateLocksAndService()
        }
    }

    private fun downloadViaHttp(video: Video, videoKey: String, downloadUrl: String) {
        val rawName = video.nameWithExtension.ifBlank { "video_${video.id}.mp4" }
        val safeName = sanitizeFileName(rawName)
        val targetFile = File(offlineDir, safeName)
        val partFile = File(offlineDir, "$safeName.part")
        val stateFile = File(offlineDir, "$safeName.part.segments")
        val aliases = getAllVideoKeys(video)

        val task = DownloadTask(
            key = videoKey,
            title = safeName,
            progress = 0.01f,
            totalBytes = video.size,
            status = downloadStatus(video, "Downloading"),
            video = video
        )
        registerActiveProgress(videoKey, aliases, 0.01f, task)

        val job = scope.launch {
            val uriFileId = if (video.uriString.startsWith("tdlib://")) {
                try { Uri.parse(video.uriString).getQueryParameter("fileId") } catch (_: Exception) { null }
            } else null
            val extId = video.externalId?.takeIf { it.isNotBlank() } ?: uriFileId
            var finalUrl = if (downloadUrl.startsWith("http")) {
                downloadUrl
            } else if (!extId.isNullOrEmpty()) {
                NetworkConfig.streamUrl(extId)
            } else {
                video.path
            }
            var totalBytes = video.size
            val token = preferencesRepository.playerPreferences.value.telegramToken
            if (token.isNotEmpty() && !finalUrl.contains("token=")) {
                finalUrl = if (finalUrl.contains("?")) "$finalUrl&token=$token" else "$finalUrl?token=$token"
            }

            var effectiveUrl = finalUrl
            if (finalUrl.contains("/api/stream/") && !finalUrl.contains("direct=1")) {
                effectiveUrl = if (finalUrl.contains("?")) "$finalUrl&direct=1" else "$finalUrl?direct=1"
            }

            try {
                // Probe server for direct CDN location and Range request support (Nagram/CherryGram speed boost)
                val (resolvedUrl, probedBytes, supportsRange) = probeUrlAndRange(effectiveUrl)
                if (probedBytes > 0 && (totalBytes <= 0 || totalBytes < probedBytes)) {
                    totalBytes = probedBytes
                }
                val contentUrl = resolvedUrl.ifBlank { effectiveUrl }
                val currentSource = if (contentUrl.contains("api.telegram.org")) "Telegram Direct" else "Cloudflare Proxy"

                var downloadSuccess = false

                // Try Multi-Threaded Segmented Downloading (4-6 streams) if server supports Range requests and file is >= 5MB
                if (supportsRange && totalBytes >= 5 * 1024 * 1024L && isActive) {
                    try {
                        downloadSuccess = downloadSegmented(
                            contentUrl = contentUrl,
                            totalBytes = totalBytes,
                            partFile = partFile,
                            stateFile = stateFile,
                            video = video,
                            videoKey = videoKey,
                            safeName = safeName,
                            rawName = rawName,
                            targetFile = targetFile,
                            aliases = aliases,
                            currentSource = currentSource
                        )
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException || !isActive) {
                            return@launch
                        }
                        Log.w(TAG, "Multi-stream download interrupted or failed: ${e.message}. Preserving partial progress for resume.")
                    }
                }

                // If segmented download was not applicable or failed, run the single-stream engine
                if (!downloadSuccess && isActive) {
                    downloadSingleStream(
                        finalUrl = contentUrl,
                        totalBytes = totalBytes,
                        partFile = partFile,
                        stateFile = stateFile,
                        targetFile = targetFile,
                        video = video,
                        videoKey = videoKey,
                        safeName = safeName,
                        rawName = rawName,
                        aliases = aliases,
                        currentSource = currentSource
                    )
                }
            } finally {
                activeJobs.remove(videoKey)
                if (!isDownloadPaused(videoKey)) {
                    removeActiveProgress(videoKey)
                }
                processQueue()
                updateLocksAndService()
            }
        }
        activeJobs[videoKey] = job
    }

    fun saveToGallery(video: Video) {
        scope.launch {
            val offlineFile = getOfflineFile(video)
            val safeName = sanitizeFileName(video.nameWithExtension.ifBlank { "video_${video.id}.mp4" })
            val primaryKey = video.externalId ?: video.id.toString()
            val aliases = getAllVideoKeys(video)

            markForGalleryExport(video)

            if (offlineFile != null && offlineFile.exists() && offlineFile.length() >= MIN_VALID_VIDEO_SIZE) {
                copyFileToGallery(offlineFile, safeName, video, primaryKey, aliases)
            } else {
                autoSaveToGalleryKeys.add(primaryKey)
                autoSaveToGalleryKeys.add(safeName)
                autoSaveToGalleryKeys.addAll(galleryKeys(video))
                showToast("Preparing Gallery save: $safeName")
                downloadToWatch(video, prioritize = true)
            }
        }
    }

    fun deleteFromOffline(video: Video): Boolean {
        val safeName = sanitizeFileName(video.nameWithExtension)
        var deleted = false

        val dirs = listOfNotNull(
            offlineDir,
            File(context.filesDir, OFFLINE_FOLDER),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "KryptonPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "KryptonPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ArchPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ArchPlayer")
        )

        val targetNames = listOfNotNull(
            safeName,
            "$safeName.part",
            video.nameWithExtension,
            "${video.externalId}.mp4",
            "$safeName.part.segments",
            "$safeName.thumb.jpg",
            "${video.nameWithExtension}.thumb.jpg"
        )

        for (dir in dirs) {
            if (!dir.exists()) continue
            for (name in targetNames) {
                val f = File(dir, name)
                if (f.exists()) {
                    deleted = f.delete() || deleted
                }
            }
        }

        val key = video.externalId ?: video.id.toString()
        keyToFileMap.remove(key)
        keyToFileMap.remove(video.id.toString())
        video.externalId?.let { keyToFileMap.remove(it) }
        targetNames.forEach { offlineFilesCache.remove(it) }
        offlineFilesCache.remove(key)
        offlineFilesCache.remove(video.id.toString())
        video.externalId?.let { offlineFilesCache.remove(it) }
        _downloadedKeys.value = _downloadedKeys.value - safeName - video.nameWithExtension - key - "${video.externalId}.mp4"
        refreshDownloadedKeys()
        showToast("Removed from Offline Storage")
        return deleted
    }

    private fun showToast(message: String) {
        mainHandler.post {
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }
}
