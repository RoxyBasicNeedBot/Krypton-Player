package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig
import dev.anilbeesetti.nextplayer.core.data.network.TelegramApi
import dev.anilbeesetti.nextplayer.core.data.network.TelegramAuthResponse
import dev.anilbeesetti.nextplayer.core.data.network.TelegramUserProfileResponse
import dev.anilbeesetti.nextplayer.core.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramRepository @Inject constructor(
    private val telegramApi: TelegramApi,
    private val telegramCacheManager: dev.anilbeesetti.nextplayer.core.data.cache.TelegramCacheManager
) {
    fun getTelegramVideosFlow(token: String): Flow<List<Video>> = flow {
        // 1. Emit cached videos immediately so user sees videos even if offline / server OFF
        val cachedVideos = telegramCacheManager.getCloudVideos(token)
        if (cachedVideos.isNotEmpty()) {
            emit(cachedVideos)
        }

        // 2. Fetch fresh videos from server
        try {
            val response = telegramApi.getVideos(token)
            val videos = response.videos.mapNotNull { dto ->
                if (dto.fileId == null) return@mapNotNull null
                
                // Ensure channel-originating videos do not leak into personal cloud
                val isChannelVideo = (dto.channelId != null && dto.channelId != 0L) ||
                    (dto.chatId != null && dto.chatId < 0 && dto.chatId != dto.userId)
                if (isChannelVideo) return@mapNotNull null
                
                val fallbackUrl = NetworkConfig.streamUrl(dto.fileId, token)
                val mimeType = dto.mimeType ?: ""
                val fileName = dto.fileName ?: "Unknown"
                val fileSize = dto.fileSize ?: 0L

                val isImage = mimeType.startsWith("image/", ignoreCase = true) ||
                    listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".tiff", ".svg", ".heic", ".heif")
                        .any { fileName.endsWith(it, ignoreCase = true) }

                val chatId = dto.chatId ?: dto.channelId ?: dto.userId
                val messageId = dto.messageId
                val encodedFallback = java.net.URLEncoder.encode(fallbackUrl, "UTF-8")
                val safeName = try { java.net.URLEncoder.encode(fileName, "UTF-8") } catch (_: Exception) { "video.mp4" }
                val effectiveThumbId = dto.thumbFileId?.takeIf { it.isNotBlank() } ?: dto.thumbUrl?.takeIf { it.isNotBlank() }
                val thumbParam = if (!effectiveThumbId.isNullOrEmpty() && !effectiveThumbId.startsWith("http") && !effectiveThumbId.startsWith("data:image")) {
                    "&thumbFileId=${java.net.URLEncoder.encode(effectiveThumbId, "UTF-8")}"
                } else ""
                val channelUser = if (chatId != null) telegramCacheManager.getChannelUsername(chatId, token) else null
                val userParam = if (!channelUser.isNullOrEmpty()) "&username=${java.net.URLEncoder.encode(channelUser, "UTF-8")}" else ""
                val streamUrl = if (chatId != null && messageId != null) {
                    "tdlib://stream?chatId=$chatId&messageId=$messageId&fileId=${dto.fileId}&size=$fileSize&name=$safeName$thumbParam$userParam&fallback=$encodedFallback"
                } else {
                    "tdlib://stream?fileId=${dto.fileId}&size=$fileSize&name=$safeName$thumbParam$userParam&fallback=$encodedFallback"
                }
                
                val durationMs = if (isImage) 0L else ((dto.duration ?: 0.0).toLong()) * 1000L
                val formattedDuration = if (isImage) "" else formatDuration(durationMs)

                val resolvedArtworkUri = when {
                    effectiveThumbId != null && (effectiveThumbId.startsWith("data:image") || effectiveThumbId.startsWith("http")) -> effectiveThumbId
                    !effectiveThumbId.isNullOrEmpty() -> NetworkConfig.thumbnailUrl(effectiveThumbId)
                    isImage -> fallbackUrl
                    else -> null
                }

                // Map to domain Video model
                Video(
                    id = dto.fileId.hashCode().toLong(),
                    path = fallbackUrl,
                    parentPath = "Telegram Cloud",
                    duration = durationMs,
                    uriString = streamUrl,
                    nameWithExtension = fileName,
                    width = 0,
                    height = 0,
                    size = fileSize,
                    formattedDuration = formattedDuration,
                    formattedFileSize = formatFileSize(fileSize),
                    artworkUri = resolvedArtworkUri,
                    externalId = dto.fileId ?: dto.id,
                    isImage = isImage
                )
            }
            if (videos.isNotEmpty()) {
                telegramCacheManager.saveCloudVideos(videos, token)
            }
            emit(videos)
        } catch (e: Exception) {
            e.printStackTrace()
            if (cachedVideos.isEmpty()) {
                emit(emptyList())
            }
        }
    }

    suspend fun deleteVideo(dbId: String, token: String): Boolean {
        return try {
            telegramApi.deleteVideo(dbId, token)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun verifyToken(token: String): TelegramAuthResponse? {
        return try {
            telegramApi.verifyToken(token)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getUserProfile(token: String): TelegramUserProfileResponse? {
        return try {
            telegramApi.getUserProfile(token)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createShareLink(token: String, fileId: String, durationHours: Int): String? {
        return try {
            val response = telegramApi.createShareLink(token, dev.anilbeesetti.nextplayer.core.data.network.CreateShareRequest(fileId, durationHours))
            response.shareUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun addChannel(username: String, token: String): dev.anilbeesetti.nextplayer.core.data.network.models.ChannelResponse? {
        return try {
            telegramApi.addChannel(token, dev.anilbeesetti.nextplayer.core.data.network.models.AddChannelRequest(username))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getChannels(token: String): List<dev.anilbeesetti.nextplayer.core.data.network.models.ChannelResponse> {
        return try {
            val channels = telegramApi.getChannels(token).channels
            if (channels.isNotEmpty()) {
                telegramCacheManager.saveChannels(channels, token)
            }
            channels
        } catch (e: Exception) {
            e.printStackTrace()
            telegramCacheManager.getChannels(token)
        }
    }

    suspend fun removeChannel(channelId: Long, token: String): Boolean {
        return try {
            telegramApi.removeChannel(channelId, token)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getChannelVideos(channelId: Long, token: String, skip: Int = 0, limit: Int = 50, query: String? = null): List<Video> {
        return try {
            val response = telegramApi.getChannelVideos(channelId, token, skip, limit, query)
            response.videos.mapNotNull { dto ->
                if (dto.fileId == null) return@mapNotNull null
                
                val fallbackUrl = NetworkConfig.streamUrl(dto.fileId, token)
                val mimeType = dto.mimeType ?: ""
                val fileName = dto.fileName ?: "Unknown"
                val fileSize = dto.fileSize ?: 0L

                val isImage = mimeType.startsWith("image/", ignoreCase = true) ||
                    listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".tiff", ".svg", ".heic", ".heif")
                        .any { fileName.endsWith(it, ignoreCase = true) }

                val chatId = dto.chatId ?: dto.channelId ?: channelId
                val messageId = dto.messageId
                val encodedFallback = java.net.URLEncoder.encode(fallbackUrl, "UTF-8")
                val safeName = try { java.net.URLEncoder.encode(fileName, "UTF-8") } catch (_: Exception) { "video.mp4" }
                val channelUser = telegramCacheManager.getChannelUsername(chatId, token)
                val userParam = if (!channelUser.isNullOrEmpty()) "&username=${java.net.URLEncoder.encode(channelUser, "UTF-8")}" else ""
                val streamUrl = if (messageId != null && !isImage) {
                    "tdlib://stream?chatId=$chatId&messageId=$messageId&fileId=${dto.fileId}&size=$fileSize&name=$safeName$userParam&fallback=$encodedFallback"
                } else {
                    "tdlib://stream?fileId=${dto.fileId}&size=$fileSize&name=$safeName$userParam&fallback=$encodedFallback"
                }
                
                val durationMs = if (isImage) 0L else ((dto.duration ?: 0.0).toLong()) * 1000L
                val formattedDuration = if (isImage) "" else formatDuration(durationMs)

                // Map to domain Video model
                Video(
                    id = dto.fileId.hashCode().toLong(),
                    path = fallbackUrl,
                    parentPath = "Channel Videos",
                    duration = durationMs,
                    uriString = streamUrl,
                    nameWithExtension = fileName,
                    width = 0,
                    height = 0,
                    size = fileSize,
                    formattedDuration = formattedDuration,
                    formattedFileSize = formatFileSize(fileSize),
                    artworkUri = dto.thumbUrl?.let { 
                        if (it.startsWith("data:image")) it else NetworkConfig.thumbnailUrl(it)
                    } ?: if (isImage) fallbackUrl else null,
                    externalId = dto.fileId ?: dto.id,
                    isImage = isImage
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getChannelPosts(
        channelId: Long,
        token: String,
        skip: Int = 0,
        limit: Int = 30,
        searchQuery: String? = null,
        postType: String? = null
    ): List<dev.anilbeesetti.nextplayer.core.data.network.models.ChannelPostDto> {
        return try {
            val response = telegramApi.getChannelPosts(channelId, token, skip, limit, searchQuery, postType)
            val posts = response.posts.map { post ->
                if (post.channelId == 0L) post.copy(channelId = channelId) else post
            }
            if (skip == 0 && searchQuery.isNullOrEmpty() && postType == null && posts.isNotEmpty()) {
                telegramCacheManager.saveChannelPosts(channelId, posts, token)
            }
            posts
        } catch (e: Exception) {
            e.printStackTrace()
            if (skip == 0 && searchQuery.isNullOrEmpty() && postType == null) {
                telegramCacheManager.getChannelPosts(channelId, token).map { post ->
                    if (post.channelId == 0L) post.copy(channelId = channelId) else post
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun getChannelStats(
        channelId: Long,
        token: String
    ): dev.anilbeesetti.nextplayer.core.data.network.models.ChannelStatsResponse? {
        return try {
            val stats = telegramApi.getChannelStats(channelId, token)
            telegramCacheManager.saveChannelStats(channelId, stats, token)
            stats
        } catch (e: Exception) {
            e.printStackTrace()
            telegramCacheManager.getChannelStats(channelId, token)
        }
    }

    fun clearCache() {
        telegramCacheManager.clearAll()
    }

    suspend fun refreshChannel(
        channelId: Long,
        token: String
    ): Boolean {
        return try {
            telegramApi.refreshChannel(channelId, token)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return ""
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
