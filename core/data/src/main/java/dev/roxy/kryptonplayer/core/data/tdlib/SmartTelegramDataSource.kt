package dev.roxy.kryptonplayer.core.data.tdlib

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.TransferListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager
import dev.roxy.kryptonplayer.core.data.network.models.ChannelPostDto
import dev.roxy.kryptonplayer.core.model.Video
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException

/**
 * Smart Streaming DataSource that prioritizes:
 * 1. Fully downloaded offline files (0 Server Bandwidth, instant start, zero buffer stalls).
 * 2. Direct MTProto progressive disk streaming via TDLib (0 Server Bandwidth).
 * 3. Automatic resilient fallback to server HTTP streaming if TDLib or local file encounters an error.
 */
@OptIn(UnstableApi::class)
class SmartTelegramDataSource(
    private val context: Context,
    private val httpDelegate: DataSource,
    private val tdlibManager: TdlibManager,
    private val tdlibMediaResolver: TdlibMediaResolver,
    private val preferencesRepository: dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository? = null
) : DataSource {

    companion object {
        private const val TAG = "SmartTelegramDataSource"
        private const val OFFLINE_FOLDER = "offline_media"

        /**
         * Tracks the canonical URI key of the video for which the Cloudflare-fallback
         * warning Toast was last shown. Using a class-level (companion) field ensures the
         * toast fires only ONCE per unique video even though ExoPlayer creates a new
         * SmartTelegramDataSource instance for every chunk via the DataSource.Factory lambda.
         */
        @Volatile
        private var lastWarnedVideoKey: String? = null

        private fun sanitizeFileName(name: String): String {
            return name.replace(Regex("[\\/:*?\"<>|\r\n\t]"), "_").trim()
        }
    }

    private var activeDataSource: DataSource? = null
    private var currentDataSpec: DataSpec? = null
    private var currentBytePosition: Long = 0L
    private var expectedTotalBytes: Long = -1L
    private var knownContentLength: Long = -1L

    override fun addTransferListener(transferListener: TransferListener) {
        httpDelegate.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        this.currentDataSpec = dataSpec
        this.currentBytePosition = dataSpec.position
        val uri = dataSpec.uri
        val uriString = uri.toString()

        val paramSize = try { uri.getQueryParameter("size")?.toLongOrNull() } catch (_: Exception) { null } ?: -1L
        if (paramSize > 0) {
            knownContentLength = paramSize
        }

        this.expectedTotalBytes = if (dataSpec.length != androidx.media3.common.C.LENGTH_UNSET.toLong()) {
            dataSpec.position + dataSpec.length
        } else if (knownContentLength > 0) {
            knownContentLength
        } else {
            -1L
        }

        val rawFileName = try {
            uri.getQueryParameter("fileName") ?: uri.getQueryParameter("name") ?: uri.lastPathSegment ?: ""
        } catch (_: Exception) {
            uri.lastPathSegment ?: ""
        }
        val fileName = try {
            if (rawFileName.contains("%")) java.net.URLDecoder.decode(rawFileName, "UTF-8") else rawFileName
        } catch (_: Exception) { rawFileName }
        val decodedFileName = try { java.net.URLDecoder.decode(fileName, "UTF-8") } catch (_: Exception) { fileName }
        var fileId = try { uri.getQueryParameter("fileId") ?: "" } catch (_: Exception) { "" }
        var chatId = try { uri.getQueryParameter("chatId")?.toLongOrNull() } catch (_: Exception) { null }
        var messageId = try { uri.getQueryParameter("messageId")?.toLongOrNull() } catch (_: Exception) { null }
        var fallbackUrl = try { uri.getQueryParameter("fallback") ?: "" } catch (_: Exception) { "" }
        val paramUsername = try { uri.getQueryParameter("username") } catch (_: Exception) { null }
            ?: (chatId?.let { tdlibManager.getRegisteredChannelUsername(it) })
        val effectiveToken = try {
            uri.getQueryParameter("token")?.takeIf { it.isNotBlank() }
                ?: preferencesRepository?.playerPreferences?.value?.telegramToken?.takeIf { it.isNotBlank() }
                ?: ""
        } catch (_: Exception) { "" }

        // Fast metadata resolution from cache or registered fallbacks if fileId/chatId missing or size unknown
        val cachedMetadata = if (chatId == null || messageId == null || fileId.isEmpty() || fallbackUrl.isEmpty() || knownContentLength <= 0) {
            findMetadataFromCache(fileName, decodedFileName, fileId)
        } else null

        if (cachedMetadata != null) {
            if (chatId == null) chatId = cachedMetadata.chatId
            if (messageId == null) messageId = cachedMetadata.messageId
            if (fileId.isEmpty()) fileId = cachedMetadata.fileId
            if (fallbackUrl.isEmpty()) fallbackUrl = cachedMetadata.fallbackUrl
            if (knownContentLength <= 0 && cachedMetadata.fileSize > 0) {
                knownContentLength = cachedMetadata.fileSize
                if (expectedTotalBytes <= 0) expectedTotalBytes = cachedMetadata.fileSize
            }
        }

        // =========================================================================
        // 1. FAST LOCAL OFFLINE CHECK: If video was downloaded, play directly!
        // =========================================================================
        val rawPath = if (uriString.startsWith("file://")) uri.path else null
        val decodedPath = try {
            if (rawPath != null) java.net.URLDecoder.decode(rawPath, "UTF-8") else null
        } catch (_: Exception) { rawPath }

        val offlineFile = decodedPath?.let { File(it) }?.takeIf { it.exists() }
            ?: rawPath?.let { File(it) }?.takeIf { it.exists() }
            ?: findOfflineFile(fileName, fileId, knownContentLength)
            ?: (if (decodedFileName != fileName) findOfflineFile(decodedFileName, fileId, knownContentLength) else null)

        val isFileInOfflineDir = offlineFile != null && offlineFile.absolutePath.contains(OFFLINE_FOLDER)

        // Check if offline file is corrupted, truncated, or incomplete
        val isCorruptedStub = offlineFile != null && (
            offlineFile.length() < TelegramDownloadManager.MIN_VALID_VIDEO_SIZE ||
            offlineFile.name.endsWith(".part") ||
            offlineFile.name.endsWith(".tmp")
        )

        if (offlineFile != null && isCorruptedStub) {
            Log.w(TAG, "Offline file is corrupted or incomplete stub (${offlineFile.name}, size=${offlineFile.length()} bytes, pos=$currentBytePosition, minValid=${TelegramDownloadManager.MIN_VALID_VIDEO_SIZE}). Falling back to stream...")
            if (isFileInOfflineDir && offlineFile.length() < TelegramDownloadManager.MIN_VALID_VIDEO_SIZE && !offlineFile.name.endsWith(".part")) {
                try { offlineFile.delete() } catch (_: Exception) {}
            }
            activeDataSource = null
        } else if (offlineFile != null && offlineFile.exists() && (if (isFileInOfflineDir) (offlineFile.length() >= TelegramDownloadManager.MIN_VALID_VIDEO_SIZE && !offlineFile.name.endsWith(".part")) else (offlineFile.length() > 0))) {
            if (currentBytePosition < offlineFile.length()) {
                try {
                    Log.i(TAG, "[OFFLINE] Playing fully downloaded offline file: ${offlineFile.name} (${offlineFile.length()} bytes)")
                    val fileDataSource = FileDataSource()
                    val fileSpec = dataSpec.buildUpon().setUri(Uri.fromFile(offlineFile)).build()
                    val bytesRemaining = fileDataSource.open(fileSpec)
                    activeDataSource = fileDataSource
                    if (bytesRemaining != androidx.media3.common.C.LENGTH_UNSET.toLong()) {
                        expectedTotalBytes = currentBytePosition + bytesRemaining
                        knownContentLength = expectedTotalBytes
                    }
                    return bytesRemaining
                } catch (e: Exception) {
                    Log.w(TAG, "Local offline file playback failed (${e.message}), deleting corrupted file and falling back...", e)
                    if (isFileInOfflineDir) {
                        try { offlineFile.delete() } catch (_: Exception) {}
                    }
                    activeDataSource = null
                }
            } else {
                Log.w(TAG, "Requested seek pos $currentBytePosition >= offline file length ${offlineFile.length()}, falling back to stream")
                activeDataSource = null
            }
        }

        // =========================================================================
        // 2. DIRECT TDLib MTPROTO STREAMING
        // =========================================================================
        val shouldAttemptTdlib = (uriString.startsWith("tdlib://stream") || activeDataSource == null) &&
            ((chatId != null && messageId != null) || fileId.isNotEmpty())

        if (shouldAttemptTdlib && ((chatId != null && messageId != null) || fileId.isNotEmpty()) && tdlibManager.isReady()) {
            Log.i(TAG, "[MTPROTO] Attempting Direct MTProto streaming for file=$fileId msg ($chatId, $messageId)...")
            tdlibManager.addLog(
                level = "STREAM",
                message = "Video Stream Requested: chatId=$chatId, messageId=$messageId, fileId=$fileId",
                tag = "STREAM"
            )

            try {
                val fileInfo = runBlocking {
                    tdlibMediaResolver.resolveFile(chatId, messageId, fileId, paramUsername, expectedFileSize = knownContentLength)
                }

                if (fileInfo != null) {
                    val effectiveSize = when {
                        fileInfo.fileSize > 65536L -> fileInfo.fileSize
                        knownContentLength > 65536L -> knownContentLength
                        fileInfo.fileSize > 0 -> fileInfo.fileSize
                        knownContentLength > 0 -> knownContentLength
                        else -> 0L
                    }

                    val tdLocal = File(fileInfo.localPath)
                    if (fileInfo.isCompleted && tdLocal.exists() && tdLocal.length() >= effectiveSize && tdLocal.length() >= TelegramDownloadManager.MIN_VALID_VIDEO_SIZE) {
                        try {
                            Log.i(TAG, "[MTPROTO] Playing completed TDLib cache file: ${tdLocal.absolutePath}")
                            val fileDataSource = FileDataSource()
                            val fileSpec = dataSpec.buildUpon().setUri(Uri.fromFile(tdLocal)).build()
                            val openRes = fileDataSource.open(fileSpec)
                            activeDataSource = fileDataSource
                            if (openRes != androidx.media3.common.C.LENGTH_UNSET.toLong()) {
                                expectedTotalBytes = currentBytePosition + openRes
                                knownContentLength = expectedTotalBytes
                            }
                            return openRes
                        } catch (e: Exception) {
                            Log.w(TAG, "TDLib local cache file open failed: ${e.message}")
                        }
                    }

                    Log.i(TAG, "[MTPROTO] Direct MTProto progressive stream active: fileId=${fileInfo.fileId}, localPath=${fileInfo.localPath}")
                    tdlibManager.addLog(
                        level = "SUCCESS",
                        message = "Direct MTProto 2.0 Stream Active: fileId=${fileInfo.fileId}, size=$effectiveSize bytes",
                        tag = "MTPROTO"
                    )
                    tdlibManager.updateStreamingStats(
                        fileName = fileName.ifEmpty { "Telegram Video (Msg #${messageId ?: fileId})" },
                        chatId = chatId ?: 0L,
                        messageId = messageId ?: 0L,
                        fileId = fileInfo.fileId.toString(),
                        totalBytes = effectiveSize,
                        downloadedBytes = 0L,
                        isDirectMtproto = true,
                        status = "MTProto Direct Stream"
                    )

                    val tdDataSource = TdlibDataSource(
                        tdlibManager = tdlibManager,
                        tdlibFileId = fileInfo.fileId,
                        totalFileSize = effectiveSize,
                        localFilePath = fileInfo.localPath
                    )
                    activeDataSource = tdDataSource
                    if (effectiveSize > 0) {
                        expectedTotalBytes = effectiveSize
                        knownContentLength = effectiveSize
                    }
                    return tdDataSource.open(dataSpec)
                } else {
                    Log.w(TAG, "TDLib media resolution returned null (fileId=$fileId, chatId=$chatId), bypassing to HTTP stream fallback")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "TDLib direct stream resolution error, falling back to HTTP stream", e)
            }
        }

        // =========================================================================
        // 3. FALLBACK TO HTTP STREAM URL (Cloudflare Worker / Registered Stream URL)
        // =========================================================================
        val rawFallback = when {
            fallbackUrl.isNotEmpty() -> fallbackUrl
            TelegramDownloadManager.getFallbackUrl(fileName)?.isNotEmpty() == true -> TelegramDownloadManager.getFallbackUrl(fileName)!!
            decodedFileName != fileName && TelegramDownloadManager.getFallbackUrl(decodedFileName)?.isNotEmpty() == true -> TelegramDownloadManager.getFallbackUrl(decodedFileName)!!
            fileId.isNotEmpty() && TelegramDownloadManager.getFallbackUrl(fileId)?.isNotEmpty() == true -> TelegramDownloadManager.getFallbackUrl(fileId)!!
            fileId.isNotEmpty() -> dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(fileId)
            else -> ""
        }

        val resolvedFallback = if (rawFallback.contains("workers.dev") && !rawFallback.contains("token=") && effectiveToken.isNotBlank()) {
            if (rawFallback.contains("?")) "$rawFallback&token=$effectiveToken" else "$rawFallback?token=$effectiveToken"
        } else {
            rawFallback
        }

        if (resolvedFallback.isNotEmpty()) {
            Log.i(TAG, "Using fallback HTTP stream: $resolvedFallback at pos $currentBytePosition")
            tdlibManager.addLog(
                level = "WARN",
                message = "Streaming via Cloudflare Fallback: $resolvedFallback",
                tag = "FALLBACK"
            )
            tdlibManager.updateStreamingStats(
                fileName = fileName.ifEmpty { "Telegram Video (Fallback)" },
                chatId = chatId ?: 0L,
                messageId = messageId ?: 0L,
                fileId = fileId,
                totalBytes = knownContentLength,
                downloadedBytes = 0L,
                isDirectMtproto = false,
                status = "Cloudflare Fallback Stream"
            )


            // One-time per-video warning: notify user that cloud fallback is being used.
            // Uses a class-level key because ExoPlayer creates a NEW SmartTelegramDataSource
            // instance for every chunk, so an instance-level flag would always be false.
            val videoKey = when {
                fileId.isNotEmpty() -> fileId
                chatId != null && messageId != null -> "$chatId:$messageId"
                else -> resolvedFallback
            }
            val shouldWarn = synchronized(SmartTelegramDataSource::class.java) {
                if (lastWarnedVideoKey != videoKey) {
                    lastWarnedVideoKey = videoKey
                    true
                } else false
            }
            if (shouldWarn) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        Toast.makeText(
                            context,
                            "Streaming via Cloud (slower). Add this channel to get faster direct streaming.",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) {}
                }
            }

            val fallbackDataSpec = dataSpec.buildUpon()
                .setUri(Uri.parse(resolvedFallback))
                .setPosition(currentBytePosition)
                .build()
            activeDataSource = httpDelegate
            try {
                val httpRes = httpDelegate.open(fallbackDataSpec)
                if (httpRes != androidx.media3.common.C.LENGTH_UNSET.toLong() && expectedTotalBytes <= 0) {
                    expectedTotalBytes = currentBytePosition + httpRes
                    knownContentLength = expectedTotalBytes
                }
                return httpRes
            } catch (httpErr: Exception) {
                Log.w(TAG, "Fallback HTTP stream open failed (${httpErr.message}), checking MTProto recovery...", httpErr)
                if (tdlibManager.isReady() && ((chatId != null && messageId != null) || fileId.isNotEmpty())) {
                    try {
                        Log.i(TAG, "Emergency re-attempting TDLib direct MTProto streaming...")
                        val retryInfo = runBlocking {
                            tdlibMediaResolver.resolveFile(chatId, messageId, fileId, paramUsername, expectedFileSize = knownContentLength)
                        }
                        if (retryInfo != null) {
                            val effectiveRetrySize = when {
                                retryInfo.fileSize > 65536L -> retryInfo.fileSize
                                knownContentLength > 65536L -> knownContentLength
                                retryInfo.fileSize > 0 -> retryInfo.fileSize
                                knownContentLength > 0 -> knownContentLength
                                else -> 0L
                            }
                            val tdDataSource = TdlibDataSource(
                                tdlibManager = tdlibManager,
                                tdlibFileId = retryInfo.fileId,
                                totalFileSize = effectiveRetrySize,
                                localFilePath = retryInfo.localPath
                            )
                            activeDataSource = tdDataSource
                            if (effectiveRetrySize > 0) {
                                expectedTotalBytes = effectiveRetrySize
                                knownContentLength = effectiveRetrySize
                            }
                            return tdDataSource.open(dataSpec)
                        }
                    } catch (retryErr: Exception) {
                        Log.e(TAG, "TDLib retry also failed: ${retryErr.message}")
                    }
                }
                throw httpErr
            }
        }

        // =========================================================================
        // 4. STANDARD HTTP / LOCAL PLAYBACK (WITH RESILIENT RECOVERY)
        // =========================================================================
        if (uriString.startsWith("file://")) {
            if (offlineFile != null && offlineFile.exists() && offlineFile.length() > 0 && !isFileInOfflineDir) {
                val fileDataSource = FileDataSource()
                val fileSpec = dataSpec.buildUpon().setUri(Uri.fromFile(offlineFile)).build()
                activeDataSource = fileDataSource
                return fileDataSource.open(fileSpec)
            }
            throw IOException("Local offline media file cannot be played (${offlineFile?.name ?: fileName}), and no remote streaming fallback was found.")
        }

        activeDataSource = httpDelegate
        try {
            val res = httpDelegate.open(dataSpec)
            if (res != androidx.media3.common.C.LENGTH_UNSET.toLong() && expectedTotalBytes <= 0) {
                expectedTotalBytes = currentBytePosition + res
                knownContentLength = expectedTotalBytes
            }
            return res
        } catch (e: Exception) {
            Log.w(TAG, "Standard playback failed for ${dataSpec.uri} (${e.message}), attempting emergency HTTP recovery...", e)
            val lateFallback = TelegramDownloadManager.getFallbackUrl(fileName)
                ?: (if (decodedFileName != fileName) TelegramDownloadManager.getFallbackUrl(decodedFileName) else null)
                ?: (if (fileId.isNotEmpty()) TelegramDownloadManager.getFallbackUrl(fileId) else null)
                ?: (if (fileId.isNotEmpty()) dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(fileId) else null)
            if (!lateFallback.isNullOrEmpty()) {
                Log.i(TAG, "Emergency recovery via fallback HTTP stream: $lateFallback")
                val emergencySpec = dataSpec.buildUpon()
                    .setUri(Uri.parse(lateFallback))
                    .setPosition(currentBytePosition)
                    .build()
                val emergencyRes = httpDelegate.open(emergencySpec)
                if (emergencyRes != androidx.media3.common.C.LENGTH_UNSET.toLong() && expectedTotalBytes <= 0) {
                    expectedTotalBytes = currentBytePosition + emergencyRes
                    knownContentLength = expectedTotalBytes
                }
                return emergencyRes
            }
            throw e
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length <= 0) return 0
        var current = activeDataSource ?: httpDelegate

        for (attempt in 0..3) {
            try {
                val bytesRead = current.read(buffer, offset, length)
                if (bytesRead > 0) {
                    // SNOOP FIRST BYTES: Protect ExoPlayer from JSON/HTML error bodies masquerading as video
                    if (currentBytePosition == 0L && bytesRead >= 2) {
                        val b0 = buffer[offset]
                        val b1 = buffer[offset + 1]
                        val isJsonOrHtml = (b0 == 123.toByte() && (b1 == 34.toByte() || b1 == 32.toByte())) ||
                                           (b0 == 60.toByte())
                        if (isJsonOrHtml) {
                            val sample = String(buffer, offset, minOf(bytesRead, 120))
                            Log.e(TAG, "Detected non-video payload (JSON/HTML) from stream: $sample")
                            throw IOException("Stream returned server error payload instead of media bytes: $sample")
                        }
                    }
                    currentBytePosition += bytesRead
                    return bytesRead
                } else if (bytesRead == androidx.media3.common.C.RESULT_END_OF_INPUT) {
                    val total = if (expectedTotalBytes > 0) expectedTotalBytes else knownContentLength
                    if (total > 0 && currentBytePosition < total) {
                        throw java.io.EOFException("Premature EOF: read returned -1 at pos $currentBytePosition / $total")
                    }
                    return androidx.media3.common.C.RESULT_END_OF_INPUT
                }
                return bytesRead
            } catch (e: Throwable) {
                val spec = currentDataSpec ?: throw if (e is IOException) e else IOException(e)
                val rawFileName = try {
                    spec.uri.getQueryParameter("fileName") ?: spec.uri.getQueryParameter("name") ?: spec.uri.lastPathSegment ?: ""
                } catch (_: Exception) { spec.uri.lastPathSegment ?: "" }
                val fileName = try {
                    if (rawFileName.contains("%")) java.net.URLDecoder.decode(rawFileName, "UTF-8") else rawFileName
                } catch (_: Exception) { rawFileName }
                val decodedFileName = try { java.net.URLDecoder.decode(fileName, "UTF-8") } catch (_: Exception) { fileName }
                val fileId = try { spec.uri.getQueryParameter("fileId") } catch (_: Exception) { null } ?: ""
                var fallbackUrl = try { spec.uri.getQueryParameter("fallback") } catch (_: Exception) { null }
                    ?: TelegramDownloadManager.getFallbackUrl(fileName)
                    ?: (if (decodedFileName != fileName) TelegramDownloadManager.getFallbackUrl(decodedFileName) else null)
                    ?: (if (fileId.isNotEmpty()) TelegramDownloadManager.getFallbackUrl(fileId) else null)
                    ?: (if (fileId.isNotEmpty()) dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(fileId) else "")

                if (fallbackUrl.isEmpty()) {
                    val cachedMeta = findMetadataFromCache(fileName, decodedFileName, fileId)
                    if (cachedMeta != null && cachedMeta.fallbackUrl.isNotEmpty()) {
                        fallbackUrl = cachedMeta.fallbackUrl
                    }
                }

                val effectiveToken = try {
                    spec.uri.getQueryParameter("token")?.takeIf { it.isNotBlank() }
                        ?: preferencesRepository?.playerPreferences?.value?.telegramToken?.takeIf { it.isNotBlank() }
                        ?: ""
                } catch (_: Exception) { "" }

                val finalFallback = if (fallbackUrl.contains("workers.dev") && !fallbackUrl.contains("token=") && effectiveToken.isNotBlank()) {
                    if (fallbackUrl.contains("?")) "$fallbackUrl&token=$effectiveToken" else "$fallbackUrl?token=$effectiveToken"
                } else {
                    fallbackUrl
                }

                if (finalFallback.isNotEmpty() && attempt < 3) {
                    val total = if (expectedTotalBytes > 0) expectedTotalBytes else knownContentLength
                    Log.w(TAG, "DataSource read error at pos $currentBytePosition / $total (${e.message}), gracefully resuming via HTTP Range (attempt ${attempt + 1}): $finalFallback")
                    try { current.close() } catch (_: Exception) {}
                    if (current is FileDataSource) {
                        try {
                            val f = findOfflineFile(fileName, fileId)
                            if (f != null && (total <= 0 || f.length() < total)) {
                                f.delete()
                            }
                        } catch (_: Exception) {}
                    }

                    val remainingBytes = if (total > currentBytePosition) {
                        total - currentBytePosition
                    } else {
                        androidx.media3.common.C.LENGTH_UNSET.toLong()
                    }

                    val fallbackSpec = spec.buildUpon()
                        .setUri(Uri.parse(finalFallback))
                        .setPosition(currentBytePosition)
                        .setLength(remainingBytes)
                        .build()

                    activeDataSource = httpDelegate
                    current = httpDelegate
                    try {
                        httpDelegate.open(fallbackSpec)
                    } catch (openErr: Exception) {
                        Log.w(TAG, "Failed to open fallback HTTP stream: ${openErr.message}")
                        if (attempt == 2) throw openErr
                        continue
                    }
                    continue
                }

                if (e is IOException) throw e else throw IOException(e)
            }
        }
        return androidx.media3.common.C.RESULT_END_OF_INPUT
    }

    override fun getUri(): Uri? {
        return activeDataSource?.uri ?: httpDelegate.uri
    }

    override fun close() {
        try {
            activeDataSource?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing active data source", e)
        } finally {
            activeDataSource = null
            currentDataSpec = null
            currentBytePosition = 0L
            expectedTotalBytes = -1L
        }
    }

    data class ResolvedStreamInfo(
        val chatId: Long?,
        val messageId: Long?,
        val fileId: String,
        val fileName: String,
        val fileSize: Long,
        val streamUrl: String,
        val fallbackUrl: String
    )

    private fun findMetadataFromCache(fileName: String, decodedFileName: String, fileId: String): ResolvedStreamInfo? {
        if (fileName.isBlank() && decodedFileName.isBlank() && fileId.isBlank()) return null
        return try {
            val cacheDir = File(context.filesDir, "telegram_cache")
            if (!cacheDir.exists()) return null
            val files = cacheDir.listFiles() ?: return null

            val targetNames = mutableSetOf<String>()
            if (fileName.isNotBlank()) {
                val fn = fileName.lowercase()
                targetNames.add(fn)
                targetNames.add(sanitizeFileName(fn))
                targetNames.add(fn.substringBeforeLast("."))
                targetNames.add(sanitizeFileName(fn).substringBeforeLast("."))
            }
            if (decodedFileName.isNotBlank()) {
                val dfn = decodedFileName.lowercase()
                targetNames.add(dfn)
                targetNames.add(sanitizeFileName(dfn))
                targetNames.add(dfn.substringBeforeLast("."))
                targetNames.add(sanitizeFileName(dfn).substringBeforeLast("."))
            }

            for (file in files) {
                if (!file.isFile || !file.name.endsWith(".json")) continue
                if (file.name.startsWith("cached_posts_")) {
                    val json = try { file.readText(Charsets.UTF_8) } catch (_: Exception) { continue }
                    val type = object : TypeToken<List<ChannelPostDto>>() {}.type
                    val posts: List<ChannelPostDto>? = try { Gson().fromJson(json, type) } catch (_: Exception) { null }
                    posts?.forEach { post ->
                        val postName = post.fileName?.lowercase()
                        val postNameNoExt = postName?.substringBeforeLast(".")
                        val postFileId = post.fileId ?: ""
                        val nameMatches = (postName != null && (targetNames.contains(postName) || (postNameNoExt != null && targetNames.contains(postNameNoExt)))) ||
                                (post.caption != null && targetNames.any { it.length > 3 && post.caption.lowercase().contains(it) })
                        if (nameMatches || (fileId.isNotEmpty() && postFileId == fileId)) {
                            val fId = post.fileId ?: ""
                            val safeName = try { java.net.URLEncoder.encode(post.fileName ?: "video.mp4", "UTF-8") } catch (_: Exception) { "video.mp4" }
                            val fallback = if (fId.isNotEmpty()) dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(fId) else ""
                            val stream = "tdlib://stream?chatId=${post.channelId}&messageId=${post.messageId}&fileId=$fId&size=${post.fileSize ?: 0L}&name=$safeName"
                            if (post.fileName != null) {
                                TelegramDownloadManager.registerFallbackUrl(post.fileName, stream)
                                if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(post.fileName, fallback)
                            }
                            if (fId.isNotEmpty()) {
                                TelegramDownloadManager.registerFallbackUrl(fId, stream)
                                if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(fId, fallback)
                            }
                            return ResolvedStreamInfo(
                                chatId = post.channelId,
                                messageId = post.messageId.toLong(),
                                fileId = fId,
                                fileName = post.fileName ?: fileName,
                                fileSize = post.fileSize ?: 0L,
                                streamUrl = stream,
                                fallbackUrl = fallback
                            )
                        }
                        post.mediaItems?.forEach { item ->
                            val itemName = item.fileName?.lowercase()
                            val itemNameNoExt = itemName?.substringBeforeLast(".")
                            val itemFileId = item.fileId
                            val itemMatches = (itemName != null && (targetNames.contains(itemName) || (itemNameNoExt != null && targetNames.contains(itemNameNoExt)))) ||
                                    (fileId.isNotEmpty() && itemFileId == fileId)
                            if (itemMatches) {
                                val fId = item.fileId
                                val msgId = (item.messageId ?: post.messageId).toLong()
                                val safeName = try { java.net.URLEncoder.encode(item.fileName ?: "video.mp4", "UTF-8") } catch (_: Exception) { "video.mp4" }
                                val fallback = if (fId.isNotEmpty()) dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(fId) else ""
                                val stream = "tdlib://stream?chatId=${post.channelId}&messageId=$msgId&fileId=$fId&size=${item.fileSize ?: 0L}&name=$safeName"
                                if (item.fileName != null) {
                                    TelegramDownloadManager.registerFallbackUrl(item.fileName, stream)
                                    if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(item.fileName, fallback)
                                }
                                if (fId.isNotEmpty()) {
                                    TelegramDownloadManager.registerFallbackUrl(fId, stream)
                                    if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(fId, fallback)
                                }
                                return ResolvedStreamInfo(
                                    chatId = post.channelId,
                                    messageId = msgId,
                                    fileId = fId,
                                    fileName = item.fileName ?: fileName,
                                    fileSize = item.fileSize ?: 0L,
                                    streamUrl = stream,
                                    fallbackUrl = fallback
                                )
                            }
                        }
                    }
                } else if (file.name.startsWith("cached_cloud_videos_")) {
                    val json = try { file.readText(Charsets.UTF_8) } catch (_: Exception) { continue }
                    val type = object : TypeToken<List<Video>>() {}.type
                    val videos: List<Video>? = try { Gson().fromJson(json, type) } catch (_: Exception) { null }
                    videos?.forEach { video ->
                        val vName = video.nameWithExtension.lowercase()
                        val vNameNoExt = vName.substringBeforeLast(".")
                        val vExtId = video.externalId ?: ""
                        if (targetNames.contains(vName) || targetNames.contains(vNameNoExt) || (fileId.isNotEmpty() && vExtId == fileId)) {
                            val stream = video.uriString.takeIf { it.startsWith("tdlib") || it.startsWith("http") }
                                ?: video.path.takeIf { it.startsWith("http") }
                                ?: ""
                            val fallback = if (vExtId.isNotEmpty()) dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(vExtId) else ""
                            val parsedUri = Uri.parse(video.uriString)
                            val cId = parsedUri.getQueryParameter("chatId")?.toLongOrNull()
                            val mId = parsedUri.getQueryParameter("messageId")?.toLongOrNull()
                            if (video.nameWithExtension.isNotEmpty()) {
                                TelegramDownloadManager.registerFallbackUrl(video.nameWithExtension, stream)
                                if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(video.nameWithExtension, fallback)
                            }
                            if (vExtId.isNotEmpty()) {
                                TelegramDownloadManager.registerFallbackUrl(vExtId, stream)
                                if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(vExtId, fallback)
                            }
                            return ResolvedStreamInfo(
                                chatId = cId,
                                messageId = mId,
                                fileId = vExtId,
                                fileName = video.nameWithExtension,
                                fileSize = video.size,
                                streamUrl = stream,
                                fallbackUrl = fallback
                            )
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "findMetadataFromCache failed: ${e.message}")
            null
        }
    }

    private fun findOfflineFile(fileName: String, fileId: String, expectedSize: Long = -1L): File? {
        val candidateNames = mutableListOf<String>()
        if (fileName.isNotBlank()) {
            val decoded = try { java.net.URLDecoder.decode(fileName, "UTF-8") } catch (_: Exception) { fileName }
            val safe = sanitizeFileName(decoded)
            candidateNames.add(safe)
            if (safe != decoded) candidateNames.add(decoded)
            if (fileName != decoded) candidateNames.add(fileName)
            val safeRaw = sanitizeFileName(fileName)
            if (safeRaw != safe) candidateNames.add(safeRaw)
        }
        if (fileId.isNotBlank()) {
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
            for (name in allCandidates) {
                val f = File(dir, name)
                if (f.exists() && f.isFile && !f.name.endsWith(".part") && !f.name.endsWith(".tmp")) {
                    if (f.length() < TelegramDownloadManager.MIN_VALID_VIDEO_SIZE) {
                        continue
                    }
                    return f
                }
            }
            val dirFiles = dir.listFiles() ?: continue
            for (file in dirFiles) {
                if (file.isFile && !file.name.endsWith(".part") && !file.name.endsWith(".tmp")) {
                    if (allCandidates.any { it.equals(file.name, ignoreCase = true) || it.removeSuffix(".mp4").equals(file.name.removeSuffix(".mp4"), ignoreCase = true) }) {
                        if (file.length() >= TelegramDownloadManager.MIN_VALID_VIDEO_SIZE) {
                            return file
                        }
                    }
                }
            }
        }
        return null
    }
}
