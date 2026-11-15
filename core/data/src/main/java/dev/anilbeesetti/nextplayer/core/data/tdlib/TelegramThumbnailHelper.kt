package dev.anilbeesetti.nextplayer.core.data.tdlib

import android.graphics.Bitmap
import dev.anilbeesetti.nextplayer.core.data.cache.TelegramImageDiskCache
import org.drinkless.tdlib.TdApi
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper to coordinate instant MiniThumbnail placeholders and high-resolution thumbnail caching.
 * Prevents blank card rendering across all Telegram video and channel list views.
 */
@Singleton
class TelegramThumbnailHelper @Inject constructor(
    private val imageDiskCache: TelegramImageDiskCache
) {
    /**
     * Extracts [TdApi.Minithumbnail] from any TDLib message content.
     */
    fun extractMinithumbnail(message: TdApi.Message): TdApi.Minithumbnail? {
        return extractMinithumbnail(message.content)
    }

    /**
     * Extracts [TdApi.Minithumbnail] from any TDLib [TdApi.MessageContent].
     */
    fun extractMinithumbnail(content: TdApi.MessageContent): TdApi.Minithumbnail? {
        return when (content) {
            is TdApi.MessageVideo -> content.video.minithumbnail
            is TdApi.MessagePhoto -> content.photo.minithumbnail
            is TdApi.MessageDocument -> content.document.minithumbnail
            is TdApi.MessageAnimation -> content.animation.minithumbnail
            is TdApi.MessageAudio -> content.audio.albumCoverMinithumbnail
            is TdApi.MessageVideoNote -> content.videoNote.minithumbnail
            else -> null
        }
    }

    /**
     * Extracts high-resolution [TdApi.Thumbnail] from any message.
     */
    fun extractThumbnail(message: TdApi.Message): TdApi.Thumbnail? {
        return when (val content = message.content) {
            is TdApi.MessageVideo -> content.video.thumbnail
            is TdApi.MessageDocument -> content.document.thumbnail
            is TdApi.MessageAnimation -> content.animation.thumbnail
            is TdApi.MessageAudio -> content.audio.albumCoverThumbnail
            is TdApi.MessageVideoNote -> content.videoNote.thumbnail
            else -> null
        }
    }

    /**
     * Obtains an instant decoded [Bitmap] placeholder from a [TdApi.Minithumbnail]
     * using the 0ms [MinithumbnailDecoder].
     */
    fun getPlaceholderBitmap(minithumbnail: TdApi.Minithumbnail?, cacheKey: String? = null): Bitmap? {
        if (minithumbnail == null) return null
        return MinithumbnailDecoder.decode(minithumbnail, cacheKey)
    }

    /**
     * Obtains an instant decoded [Bitmap] placeholder from message content.
     */
    fun getPlaceholderBitmap(message: TdApi.Message, cacheKey: String? = null): Bitmap? {
        val mini = extractMinithumbnail(message) ?: return null
        val key = cacheKey ?: "mini_${message.chatId}_${message.id}"
        return MinithumbnailDecoder.decode(mini, key)
    }

    /**
     * Saves minithumbnail JPEG bytes into [imageDiskCache] for offline or zero-network recovery.
     */
    fun saveMinithumbnailToDisk(cacheKey: String, minithumbnail: TdApi.Minithumbnail): File? {
        val jpegBytes = MinithumbnailDecoder.toJpegBytes(minithumbnail.data) ?: return null
        return try {
            imageDiskCache.putBytes(cacheKey, jpegBytes)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Checks if a thumbnail is already available on disk.
     */
    fun hasDiskThumbnail(cacheKey: String): Boolean {
        val f = imageDiskCache.getCachedFile(cacheKey)
        return f != null && f.exists() && f.length() > 0
    }
}
