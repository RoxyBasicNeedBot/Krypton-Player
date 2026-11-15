package dev.anilbeesetti.nextplayer.core.data.tdlib

import android.util.Log
import dev.anilbeesetti.nextplayer.core.data.cache.TelegramCacheManager
import dev.anilbeesetti.nextplayer.core.model.Video
import org.drinkless.tdlib.TdApi
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

data class TelegramServerSearchResult(
    val videos: List<Video>,
    val totalCount: Int,
    val nextFromMessageId: Long,
    val hasMore: Boolean
)

/**
 * Server-side channel media search manager using official TDLib MTProto engine.
 *
 * Executes remote server search directly across Telegram channel message histories
 * using [TdApi.SearchChatMessages] with [TdApi.SearchMessagesFilterVideo] and [TdApi.SearchMessagesFilterAudio].
 * Delivers instantaneous sub-second media lookup without relying on secondary web crawlers.
 */
@Singleton
class TelegramServerSearchManager @Inject constructor(
    private val tdlibManager: TdlibManager,
    private val telegramCacheManager: TelegramCacheManager,
    private val thumbnailHelper: TelegramThumbnailHelper
) {
    companion object {
        private const val TAG = "ServerSearchManager"
        private const val DEFAULT_SEARCH_LIMIT = 50
    }

    /**
     * Searches videos on the Telegram server within a specific channel or supergroup.
     */
    suspend fun searchVideos(
        channelId: Long,
        query: String = "",
        fromMessageId: Long = 0L,
        limit: Int = DEFAULT_SEARCH_LIMIT,
        channelUsername: String? = null
    ): TelegramServerSearchResult {
        return searchChannelMedia(
            channelId = channelId,
            query = query,
            fromMessageId = fromMessageId,
            limit = limit,
            channelUsername = channelUsername,
            filter = TdApi.SearchMessagesFilterVideo(),
            isAudio = false
        )
    }

    /**
     * Searches audios/podcasts/soundtracks on the Telegram server within a channel.
     */
    suspend fun searchAudios(
        channelId: Long,
        query: String = "",
        fromMessageId: Long = 0L,
        limit: Int = DEFAULT_SEARCH_LIMIT,
        channelUsername: String? = null
    ): TelegramServerSearchResult {
        return searchChannelMedia(
            channelId = channelId,
            query = query,
            fromMessageId = fromMessageId,
            limit = limit,
            channelUsername = channelUsername,
            filter = TdApi.SearchMessagesFilterAudio(),
            isAudio = true
        )
    }

    /**
     * Low-level search execution against TDLib MTProto [TdApi.SearchChatMessages].
     */
    private suspend fun searchChannelMedia(
        channelId: Long,
        query: String,
        fromMessageId: Long,
        limit: Int,
        channelUsername: String?,
        filter: TdApi.SearchMessagesFilter,
        isAudio: Boolean
    ): TelegramServerSearchResult {
        if (!tdlibManager.isReady()) {
            Log.w(TAG, "Cannot search: TDLib MTProto is not ready")
            return TelegramServerSearchResult(emptyList(), 0, 0L, false)
        }

        val effectiveChatId = resolveEffectiveChatId(channelId)
        val resolvedUsername = channelUsername
            ?: tdlibManager.getRegisteredChannelUsername(effectiveChatId)
            ?: tdlibManager.getRegisteredChannelUsername(channelId)
            ?: telegramCacheManager.getChannelUsername(channelId)

        if (!resolvedUsername.isNullOrEmpty()) {
            tdlibManager.registerChannelUsername(effectiveChatId, resolvedUsername)
            if (tdlibManager.getResolvedChat(resolvedUsername) == null && !tdlibManager.isChannelResolved(effectiveChatId)) {
                tdlibManager.searchPublicChatSuspend(resolvedUsername)
            }
        }

        val searchFunction = TdApi.SearchChatMessages(
            effectiveChatId,
            null,
            query.trim(),
            null,
            fromMessageId,
            0,
            limit,
            filter
        )

        tdlibManager.addLog(
            level = "FETCH",
            message = "Server media search: chat=$effectiveChatId, query='$query', filter=${filter::class.java.simpleName}",
            tag = "SEARCH"
        )

        var response = tdlibManager.sendQuery(searchFunction)

        // If chat is not found in local TDLib cache, ensure chat and retry once
        if (response is TdApi.Error && (response.message.contains("not found", ignoreCase = true) || response.message.contains("CHANNEL_INVALID", ignoreCase = true))) {
            Log.d(TAG, "SearchChatMessages returned error: ${response.message}. Resolving channel chat...")
            if (!resolvedUsername.isNullOrEmpty()) {
                tdlibManager.searchPublicChatSuspend(resolvedUsername, forceRefresh = true)
            }
            response = tdlibManager.sendQuery(searchFunction)
        }

        return when (response) {
            is TdApi.FoundChatMessages -> {
                val videos = response.messages.mapNotNull { msg ->
                    mapMessageToVideo(msg, effectiveChatId, resolvedUsername, isAudio)
                }
                val nextId = response.nextFromMessageId
                val hasMore = nextId != 0L && response.messages.isNotEmpty()

                tdlibManager.addLog(
                    level = "SUCCESS",
                    message = "Server search returned ${videos.size} items (totalCount=${response.totalCount})",
                    tag = "SEARCH"
                )

                TelegramServerSearchResult(
                    videos = videos,
                    totalCount = response.totalCount,
                    nextFromMessageId = nextId,
                    hasMore = hasMore
                )
            }
            is TdApi.Error -> {
                Log.w(TAG, "SearchChatMessages error (${response.code}): ${response.message}")
                tdlibManager.addLog(
                    level = "WARN",
                    message = "Server search error: ${response.message}",
                    tag = "SEARCH"
                )
                TelegramServerSearchResult(emptyList(), 0, 0L, false)
            }
            else -> {
                TelegramServerSearchResult(emptyList(), 0, 0L, false)
            }
        }
    }

    /**
     * Converts a raw [TdApi.Message] containing video, document, or audio into domain [Video].
     */
    private fun mapMessageToVideo(
        message: TdApi.Message,
        chatId: Long,
        channelUsername: String?,
        isAudio: Boolean
    ): Video? {
        val (file, fileName, durationSec, mimeType, width, height, supportsStreaming) = when (val c = message.content) {
            is TdApi.MessageVideo -> {
                val v = c.video
                ExtractedMedia(
                    file = v.video,
                    fileName = v.fileName.ifEmpty { "video_${message.id}.mp4" },
                    durationSec = v.duration,
                    mimeType = v.mimeType.ifEmpty { "video/mp4" },
                    width = v.width,
                    height = v.height,
                    supportsStreaming = v.supportsStreaming
                )
            }
            is TdApi.MessageDocument -> {
                val d = c.document
                val mime = d.mimeType.lowercase(Locale.US)
                val isDocVideo = mime.startsWith("video/") || listOf(".mp4", ".mkv", ".webm", ".avi", ".mov", ".ts", ".m4v").any { d.fileName.endsWith(it, ignoreCase = true) }
                val isDocAudio = mime.startsWith("audio/") || listOf(".mp3", ".m4a", ".aac", ".flac", ".ogg", ".wav").any { d.fileName.endsWith(it, ignoreCase = true) }

                if (!isDocVideo && !isDocAudio && !isAudio) return null

                ExtractedMedia(
                    file = d.document,
                    fileName = d.fileName.ifEmpty { "document_${message.id}" },
                    durationSec = 0,
                    mimeType = d.mimeType,
                    width = 0,
                    height = 0,
                    supportsStreaming = false
                )
            }
            is TdApi.MessageAudio -> {
                val a = c.audio
                val title = if (a.title.isNotBlank()) {
                    if (a.performer.isNotBlank()) "${a.performer} - ${a.title}" else a.title
                } else {
                    a.fileName.ifEmpty { "audio_${message.id}.mp3" }
                }
                ExtractedMedia(
                    file = a.audio,
                    fileName = title,
                    durationSec = a.duration,
                    mimeType = a.mimeType.ifEmpty { "audio/mpeg" },
                    width = 0,
                    height = 0,
                    supportsStreaming = true
                )
            }
            else -> return null
        }

        val fileSize = file.size
        val durationMs = durationSec.toLong() * 1000L
        val safeName = try { URLEncoder.encode(fileName, "UTF-8") } catch (_: Throwable) { "media" }
        val userParam = if (!channelUsername.isNullOrEmpty()) "&username=${URLEncoder.encode(channelUsername, "UTF-8")}" else ""
        val streamingParam = if (!supportsStreaming) "&streaming=0" else ""
        val albumParam = if (message.mediaAlbumId != 0L) "&albumId=${message.mediaAlbumId}" else ""

        val streamUrl = "tdlib://stream?chatId=$chatId&messageId=${message.id}&fileId=${file.id}&size=$fileSize&name=$safeName$userParam$streamingParam$albumParam"
        val fallbackUrl = dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.streamUrl(file.id.toString())

        // Cache minithumbnail if present to provide instant placeholder rendering
        val mini = thumbnailHelper.extractMinithumbnail(message)
        val cacheKey = "thumb_${chatId}_${message.id}"
        if (mini != null) {
            thumbnailHelper.getPlaceholderBitmap(mini, cacheKey)
            thumbnailHelper.saveMinithumbnailToDisk(cacheKey, mini)
        }

        val thumbFile = thumbnailHelper.extractThumbnail(message)?.file
        val artworkUri = thumbFile?.local?.path?.takeIf { it.isNotEmpty() }
            ?: dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.thumbnailUrl(file.id.toString())

        return Video(
            id = message.id,
            path = fallbackUrl,
            parentPath = if (isAudio) "Channel Audio" else "Channel Videos",
            duration = durationMs,
            uriString = streamUrl,
            nameWithExtension = fileName,
            width = width,
            height = height,
            size = fileSize,
            formattedDuration = if (durationMs > 0) formatDuration(durationMs) else "",
            formattedFileSize = formatFileSize(fileSize),
            artworkUri = artworkUri,
            externalId = message.id.toString(),
            isImage = false
        )
    }

    private fun resolveEffectiveChatId(channelId: Long): Long {
        val absId = abs(channelId)
        val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
        return -(1000000000000L + bareId)
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val remainingSeconds = totalSeconds % 60
        val hours = minutes / 60
        val remMinutes = minutes % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, remMinutes, remainingSeconds)
        } else {
            String.format(Locale.US, "%02d:%02d", remMinutes, remainingSeconds)
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }

    private data class ExtractedMedia(
        val file: TdApi.File,
        val fileName: String,
        val durationSec: Int,
        val mimeType: String,
        val width: Int,
        val height: Int,
        val supportsStreaming: Boolean
    )
}
