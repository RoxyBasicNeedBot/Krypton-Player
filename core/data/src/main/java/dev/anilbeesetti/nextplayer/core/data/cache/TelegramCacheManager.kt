package dev.anilbeesetti.nextplayer.core.data.cache

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.data.network.models.ChannelPostDto
import dev.anilbeesetti.nextplayer.core.data.network.models.ChannelResponse
import dev.anilbeesetti.nextplayer.core.data.network.models.ChannelStatsResponse
import dev.anilbeesetti.nextplayer.core.model.Video
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class CachedMediaType {
    VIDEO,
    PHOTO,
    AUDIO,
    DOCUMENT,
    CHANNEL_DATA
}

data class CachedMediaItem(
    val file: File,
    val name: String,
    val size: Long,
    val type: CachedMediaType,
    val lastModified: Long,
    val source: String = "Downloaded Media"
)

@Singleton
class TelegramCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = Gson()

    companion object {
        private const val TAG = "TelegramCacheManager"
        private const val CACHE_DIR = "telegram_cache"
        private const val CLOUD_VIDEOS_FILE = "cached_cloud_videos.json"
        private const val CHANNELS_FILE = "cached_channels.json"
    }

    private val cacheDir: File by lazy {
        File(context.filesDir, CACHE_DIR).apply { mkdirs() }
    }

    private val memoryChannelUsernames = java.util.concurrent.ConcurrentHashMap<Long, String>()

    private fun registerMemoryUsername(channelId: Long, username: String) {
        val clean = username.removePrefix("@").removePrefix("https://t.me/").trim()
        if (clean.isNotEmpty()) {
            val absId = kotlin.math.abs(channelId)
            val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
            val fullId = 1000000000000L + bareId
            memoryChannelUsernames[bareId] = clean
            memoryChannelUsernames[-bareId] = clean
            memoryChannelUsernames[fullId] = clean
            memoryChannelUsernames[-fullId] = clean
            memoryChannelUsernames[channelId] = clean
            memoryChannelUsernames[-channelId] = clean
        }
    }

    private fun getUserSuffix(userKey: String?): String {
        if (userKey.isNullOrBlank()) return "default"
        return userKey.trim().takeLast(12).replace(Regex("[^a-zA-Z0-9]"), "_")
    }

    fun getCloudVideos(userKey: String? = null): List<Video> {
        return try {
            val suffix = getUserSuffix(userKey)
            val file = File(cacheDir, "cached_cloud_videos_${suffix}.json")
            if (!file.exists()) return emptyList()
            val json = file.readText(Charsets.UTF_8)
            val type = object : TypeToken<List<Video>>() {}.type
            gson.fromJson<List<Video>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached cloud videos", e)
            emptyList()
        }
    }

    fun saveCloudVideos(videos: List<Video>, userKey: String? = null) {
        try {
            val suffix = getUserSuffix(userKey)
            val file = File(cacheDir, "cached_cloud_videos_${suffix}.json")
            val json = gson.toJson(videos)
            file.writeText(json, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cloud videos to cache", e)
        }
    }

    fun getChannels(userKey: String? = null): List<ChannelResponse> {
        return try {
            val suffix = getUserSuffix(userKey)
            val targetFile = File(cacheDir, "cached_channels_${suffix}.json")
            val fileToRead = if (targetFile.exists()) {
                targetFile
            } else if (userKey.isNullOrBlank()) {
                // If userKey is null/blank and default doesn't exist, pick the most recent cached_channels_*.json
                cacheDir.listFiles { _, name -> name.startsWith("cached_channels_") && name.endsWith(".json") }
                    ?.maxByOrNull { it.lastModified() }
            } else null

            if (fileToRead == null || !fileToRead.exists()) return emptyList()
            val json = fileToRead.readText(Charsets.UTF_8)
            val type = object : TypeToken<List<ChannelResponse>>() {}.type
            val channels = gson.fromJson<List<ChannelResponse>>(json, type) ?: emptyList()
            channels.forEach { ch ->
                val u = ch.channel_username
                if (!u.isNullOrEmpty()) {
                    registerMemoryUsername(ch.channel_id, u)
                }
            }
            channels
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached channels", e)
            emptyList()
        }
    }

    fun saveChannels(channels: List<ChannelResponse>, userKey: String? = null) {
        try {
            channels.forEach { ch ->
                val u = ch.channel_username
                if (!u.isNullOrEmpty()) {
                    registerMemoryUsername(ch.channel_id, u)
                }
            }
            val suffix = getUserSuffix(userKey)
            val file = File(cacheDir, "cached_channels_${suffix}.json")
            val json = gson.toJson(channels)
            file.writeText(json, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving channels to cache", e)
        }
    }

    fun getChannelUsername(channelId: Long, userKey: String? = null): String? {
        val absId = kotlin.math.abs(channelId)
        val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
        val fullId = 1000000000000L + bareId

        memoryChannelUsernames[bareId]?.let { return it }
        memoryChannelUsernames[fullId]?.let { return it }
        memoryChannelUsernames[channelId]?.let { return it }
        memoryChannelUsernames[-fullId]?.let { return it }

        val channels = getChannels(userKey)
        val found = channels.firstOrNull { ch ->
            val chAbs = kotlin.math.abs(ch.channel_id)
            val chBare = if (chAbs > 1000000000000L) chAbs - 1000000000000L else chAbs
            chBare == bareId
        }?.channel_username?.takeIf { it.isNotBlank() }

        if (found != null) {
            registerMemoryUsername(channelId, found)
            return found
        }

        // If not found and userKey was specified, try searching any channel cache
        if (userKey != null) {
            val fallbackChannels = getChannels(null)
            val fallbackFound = fallbackChannels.firstOrNull { ch ->
                val chAbs = kotlin.math.abs(ch.channel_id)
                val chBare = if (chAbs > 1000000000000L) chAbs - 1000000000000L else chAbs
                chBare == bareId
            }?.channel_username?.takeIf { it.isNotBlank() }
            if (fallbackFound != null) {
                registerMemoryUsername(channelId, fallbackFound)
                return fallbackFound
            }
        }

        return null
    }

    fun getChannelPosts(channelId: Long, userKey: String? = null): List<ChannelPostDto> {
        return try {
            val suffix = getUserSuffix(userKey)
            val file = File(cacheDir, "cached_posts_${suffix}_${channelId}.json")
            if (!file.exists()) return emptyList()
            val json = file.readText(Charsets.UTF_8)
            val type = object : TypeToken<List<ChannelPostDto>>() {}.type
            val raw = gson.fromJson<List<ChannelPostDto>>(json, type) ?: emptyList()
            raw.map { if (it.channelId == 0L) it.copy(channelId = channelId) else it }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached posts for channel $channelId", e)
            emptyList()
        }
    }

    fun saveChannelPosts(channelId: Long, posts: List<ChannelPostDto>, userKey: String? = null) {
        try {
            val normalized = posts.map { if (it.channelId == 0L) it.copy(channelId = channelId) else it }
            val suffix = getUserSuffix(userKey)
            val file = File(cacheDir, "cached_posts_${suffix}_${channelId}.json")
            val json = gson.toJson(normalized)
            file.writeText(json, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving posts for channel $channelId to cache", e)
        }
    }

    fun getChannelStats(channelId: Long, userKey: String? = null): ChannelStatsResponse? {
        return try {
            val suffix = getUserSuffix(userKey)
            val file = File(cacheDir, "cached_stats_${suffix}_${channelId}.json")
            if (!file.exists()) return null
            val json = file.readText(Charsets.UTF_8)
            gson.fromJson(json, ChannelStatsResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached stats for channel $channelId", e)
            null
        }
    }

    fun saveChannelStats(channelId: Long, stats: ChannelStatsResponse, userKey: String? = null) {
        try {
            val suffix = getUserSuffix(userKey)
            val file = File(cacheDir, "cached_stats_${suffix}_${channelId}.json")
            val json = gson.toJson(stats)
            file.writeText(json, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stats for channel $channelId to cache", e)
        }
    }

    fun clearUserData(userKey: String?) {
        try {
            val suffix = getUserSuffix(userKey)
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.contains("_${suffix}") || file.name.contains("_${suffix}.")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user cache for $userKey", e)
        }
    }

    fun getAllCachedMediaItems(): List<CachedMediaItem> {
        val items = mutableListOf<CachedMediaItem>()

        // 1. External & Internal Offline Downloads
        listOfNotNull(
            context.getExternalFilesDir("offline_media"),
            File(context.filesDir, "offline_media")
        ).forEach { dir ->
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && !file.name.endsWith(".part") && !file.name.endsWith(".tmp")) {
                        val type = detectMediaType(file.name)
                        val minSize = if (type == CachedMediaType.VIDEO) dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.MIN_VALID_VIDEO_SIZE else 1024L
                        if (file.length() >= minSize) {
                            items.add(
                                CachedMediaItem(
                                    file = file,
                                    name = file.name,
                                    size = file.length(),
                                    type = type,
                                    lastModified = file.lastModified(),
                                    source = "Downloaded to Watch"
                                )
                            )
                        } else if (type == CachedMediaType.VIDEO && !file.name.endsWith(".thumb.jpg") && !file.name.endsWith(".thumb.png")) {
                            // Purge corrupt stub from offline storage
                            Log.w(TAG, "Purging corrupt video stub from offline directory: ${file.name} (${file.length()} bytes)")
                            try { file.delete() } catch (_: Exception) {}
                        }
                    }
                }
            }
        }

        // 2. TDLib Stream Cache
        val tdDir = File(context.filesDir, "tdlib_stream")
        if (tdDir.exists()) {
            tdDir.listFiles()?.forEach { file ->
                if (file.isFile && file.length() > 0 && !file.name.endsWith(".binlog") && !file.name.endsWith(".db")) {
                    val type = detectMediaType(file.name)
                    items.add(
                        CachedMediaItem(
                            file = file,
                            name = file.name,
                            size = file.length(),
                            type = type,
                            lastModified = file.lastModified(),
                            source = "TDLib MTProto Cache"
                        )
                    )
                }
            }
        }

        return items.sortedByDescending { it.lastModified }
    }

    fun getTelegramVideosCacheSize(): Long {
        return getAllCachedMediaItems().filter { it.type == CachedMediaType.VIDEO }.sumOf { it.size }
    }

    fun getThumbnailsCacheSize(): Long {
        var size = getAllCachedMediaItems().filter { it.type == CachedMediaType.PHOTO }.sumOf { it.size }
        val coilCache = File(context.cacheDir, "image_cache")
        if (coilCache.exists()) size += getFolderSize(coilCache)
        val imageLoaderCache = File(context.cacheDir, "image_loader")
        if (imageLoaderCache.exists()) size += getFolderSize(imageLoaderCache)
        return size
    }

    fun getAudioCacheSize(): Long {
        return getAllCachedMediaItems().filter { it.type == CachedMediaType.AUDIO }.sumOf { it.size }
    }

    fun getDocumentsCacheSize(): Long {
        return getAllCachedMediaItems().filter { it.type == CachedMediaType.DOCUMENT }.sumOf { it.size }
    }

    fun getMetadataCacheSize(): Long {
        var size = 0L
        if (cacheDir.exists()) size += getFolderSize(cacheDir)
        val tdDir = File(context.filesDir, "tdlib_stream")
        if (tdDir.exists()) {
            tdDir.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.endsWith(".binlog") || file.name.endsWith(".db"))) {
                    size += file.length()
                }
            }
        }
        val dbFile = context.getDatabasePath("media.db")
        if (dbFile.exists()) size += dbFile.length()
        return size
    }

    fun getTotalCacheSize(): Long {
        return getTelegramVideosCacheSize() + getThumbnailsCacheSize() + getAudioCacheSize() + getDocumentsCacheSize() + getMetadataCacheSize()
    }

    fun deleteCachedItem(item: CachedMediaItem): Boolean {
        return try {
            if (item.file.exists()) {
                item.file.delete()
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting item: ${item.name}", e)
            false
        }
    }

    fun clearVideosCache() {
        try {
            getAllCachedMediaItems().filter { it.type == CachedMediaType.VIDEO }.forEach {
                it.file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing videos cache", e)
        }
    }

    fun clearPhotosCache() {
        try {
            getAllCachedMediaItems().filter { it.type == CachedMediaType.PHOTO }.forEach {
                it.file.delete()
            }
            clearThumbnailsCache()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing photos cache", e)
        }
    }

    fun clearAudioCache() {
        try {
            getAllCachedMediaItems().filter { it.type == CachedMediaType.AUDIO }.forEach {
                it.file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing audio cache", e)
        }
    }

    fun clearDocumentsCache() {
        try {
            getAllCachedMediaItems().filter { it.type == CachedMediaType.DOCUMENT }.forEach {
                it.file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing documents cache", e)
        }
    }

    fun clearThumbnailsCache() {
        try {
            val coilCache = File(context.cacheDir, "image_cache")
            deleteFolderContents(coilCache)
            val imageLoaderCache = File(context.cacheDir, "image_loader")
            deleteFolderContents(imageLoaderCache)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing thumbnails cache", e)
        }
    }

    fun clearMetadataCache() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing metadata cache", e)
        }
    }

    fun clearAll() {
        try {
            clearVideosCache()
            clearPhotosCache()
            clearAudioCache()
            clearDocumentsCache()
            clearThumbnailsCache()
            clearMetadataCache()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing telegram cache", e)
        }
    }

    fun getDeviceFreeSpace(): Long {
        return try {
            val statFs = android.os.StatFs(context.filesDir.absolutePath)
            statFs.availableBlocksLong * statFs.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    fun getDeviceTotalSpace(): Long {
        return try {
            val statFs = android.os.StatFs(context.filesDir.absolutePath)
            statFs.blockCountLong * statFs.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    private fun detectMediaType(fileName: String): CachedMediaType {
        val lower = fileName.removeSuffix(".part").removeSuffix(".tmp").lowercase()
        return when {
            lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov") ||
            lower.endsWith(".webm") || lower.endsWith(".avi") || lower.endsWith(".flv") ||
            lower.endsWith(".3gp") || lower.endsWith(".ts") || lower.endsWith(".m4v") -> CachedMediaType.VIDEO

            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
            lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp") ||
            lower.endsWith(".heic") || lower.endsWith(".heif") -> CachedMediaType.PHOTO

            lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") ||
            lower.endsWith(".flac") || lower.endsWith(".opus") || lower.endsWith(".ogg") ||
            lower.endsWith(".wav") -> CachedMediaType.AUDIO

            else -> CachedMediaType.DOCUMENT
        }
    }

    private fun getFolderSize(file: File): Long {
        var size = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                size += getFolderSize(child)
            }
        } else {
            size = file.length()
        }
        return size
    }

    private fun deleteFolderContents(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteFolderContents(child)
                child.delete()
            }
        } else {
            file.delete()
        }
    }
}
