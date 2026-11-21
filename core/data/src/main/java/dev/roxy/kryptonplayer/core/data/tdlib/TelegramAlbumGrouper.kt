package dev.roxy.kryptonplayer.core.data.tdlib

import android.net.Uri
import dev.roxy.kryptonplayer.core.model.Video
import org.drinkless.tdlib.TdApi
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.pow

data class TelegramMediaAlbum(
    val albumId: Long,
    val channelId: Long,
    val title: String,
    val caption: String?,
    val messages: List<TdApi.Message>,
    val videos: List<Video>,
    val totalCount: Int,
    val totalSize: Long,
    val totalDurationMs: Long,
    val formattedTotalDuration: String,
    val formattedTotalSize: String,
    val coverVideo: Video?
)

sealed class TelegramGroupedItem {
    data class Single(val video: Video, val message: TdApi.Message? = null) : TelegramGroupedItem()
    data class Album(val album: TelegramMediaAlbum) : TelegramGroupedItem()
}

data class VideoAlbumGroup(
    val albumId: Long,
    val title: String,
    val videos: List<Video>,
    val coverVideo: Video?
)

/**
 * Groups Telegram media messages into cohesive albums and multi-part series playlists.
 *
 * Utilizes TDLib's native [TdApi.Message.mediaAlbumId] to automatically bundle
 * series episodes, multi-part video uploads, and album track collections into
 * continuous playlists.
 */
@Singleton
class TelegramAlbumGrouper @Inject constructor() {

    /**
     * Checks if a TDLib message belongs to a grouped media album.
     */
    fun isAlbumMessage(message: TdApi.Message): Boolean {
        return message.mediaAlbumId != 0L
    }

    /**
     * Extracts album ID from a domain [Video] URI string if previously tagged.
     */
    fun extractAlbumId(video: Video): Long? {
        return try {
            val uri = Uri.parse(video.uriString)
            uri.getQueryParameter("albumId")?.toLongOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Groups a collection of domain [Video] items by their album IDs.
     * Standalone videos remain in an album with albumId = 0L.
     */
    fun groupVideosByAlbum(videos: List<Video>): List<VideoAlbumGroup> {
        if (videos.isEmpty()) return emptyList()

        val groupedMap = LinkedHashMap<Long, MutableList<Video>>()
        val standaloneList = mutableListOf<Video>()

        for (v in videos) {
            val albumId = extractAlbumId(v)
            if (albumId != null && albumId != 0L) {
                groupedMap.getOrPut(albumId) { mutableListOf() }.add(v)
            } else {
                standaloneList.add(v)
            }
        }

        val result = mutableListOf<VideoAlbumGroup>()

        for ((albumId, albumVideos) in groupedMap) {
            val sorted = albumVideos.sortedBy { it.nameWithExtension }
            val first = sorted.firstOrNull()
            val title = first?.nameWithExtension?.substringBeforeLast(".") ?: "Album #$albumId"
            result.add(
                VideoAlbumGroup(
                    albumId = albumId,
                    title = title,
                    videos = sorted,
                    coverVideo = first
                )
            )
        }

        if (standaloneList.isNotEmpty()) {
            result.add(
                VideoAlbumGroup(
                    albumId = 0L,
                    title = "Individual Media",
                    videos = standaloneList,
                    coverVideo = standaloneList.firstOrNull()
                )
            )
        }

        return result
    }

    /**
     * Converts a raw list of [TdApi.Message] items and their corresponding mapped [Video] models
     * into structured [TelegramGroupedItem] entries (either Single or Album).
     */
    fun groupMessages(
        messages: List<TdApi.Message>,
        videoMapper: (TdApi.Message) -> Video?
    ): List<TelegramGroupedItem> {
        if (messages.isEmpty()) return emptyList()

        val result = mutableListOf<TelegramGroupedItem>()
        val pendingAlbumMessages = LinkedHashMap<Long, MutableList<TdApi.Message>>()
        val pendingAlbumVideos = LinkedHashMap<Long, MutableList<Video>>()

        for (msg in messages) {
            val albumId = msg.mediaAlbumId
            val video = videoMapper(msg)

            if (albumId != 0L) {
                pendingAlbumMessages.getOrPut(albumId) { mutableListOf() }.add(msg)
                if (video != null) {
                    pendingAlbumVideos.getOrPut(albumId) { mutableListOf() }.add(video)
                }
            } else {
                // If we encounter a standalone message, flush any pending album if desired, or keep collecting
                if (video != null) {
                    result.add(TelegramGroupedItem.Single(video, msg))
                }
            }
        }

        // Build album groups from collected mediaAlbumIds
        for ((albumId, albumMsgList) in pendingAlbumMessages) {
            val vList = pendingAlbumVideos[albumId] ?: emptyList()
            if (vList.isEmpty()) continue

            // Determine caption / title from the message containing text
            var albumCaption: String? = null
            for (m in albumMsgList) {
                val caption = when (val c = m.content) {
                    is TdApi.MessageVideo -> c.caption?.text
                    is TdApi.MessagePhoto -> c.caption?.text
                    is TdApi.MessageDocument -> c.caption?.text
                    is TdApi.MessageAnimation -> c.caption?.text
                    is TdApi.MessageAudio -> c.caption?.text
                    else -> null
                }
                if (!caption.isNullOrBlank()) {
                    albumCaption = caption
                    break
                }
            }

            val totalSize = vList.sumOf { it.size }
            val totalDuration = vList.sumOf { it.duration }
            val firstVideo = vList.firstOrNull()
            val title = albumCaption?.take(60)
                ?: firstVideo?.nameWithExtension?.substringBeforeLast(".")
                ?: "Album #$albumId"

            val album = TelegramMediaAlbum(
                albumId = albumId,
                channelId = albumMsgList.first().chatId,
                title = title,
                caption = albumCaption,
                messages = albumMsgList,
                videos = vList,
                totalCount = vList.size,
                totalSize = totalSize,
                totalDurationMs = totalDuration,
                formattedTotalDuration = formatDuration(totalDuration),
                formattedTotalSize = formatFileSize(totalSize),
                coverVideo = firstVideo
            )

            result.add(TelegramGroupedItem.Album(album))
        }

        return result
    }

    /**
     * Builds a continuous playback playlist from an album, sorted in chronological order.
     */
    fun createPlaylist(album: TelegramMediaAlbum): List<Video> {
        return album.videos.sortedWith(
            compareBy<Video> { it.nameWithExtension }.thenBy { it.id }
        )
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
}
