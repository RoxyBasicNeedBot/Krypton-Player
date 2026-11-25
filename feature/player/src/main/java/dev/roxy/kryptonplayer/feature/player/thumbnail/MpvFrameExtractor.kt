package dev.roxy.kryptonplayer.feature.player.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.roxy.kryptonplayer.core.common.playback.FrameThumbnailExtractor
import dev.roxy.kryptonplayer.core.data.tdlib.TelegramStreamingProxy
import `is`.xyz.mpv.FastThumbnails
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class MpvFrameExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telegramStreamingProxy: TelegramStreamingProxy,
) : FrameThumbnailExtractor {
    private val mutex = Mutex()

    @Volatile
    private var fastThumbnailsInitialized = false

    override suspend fun extractFrame(
        uri: Uri,
        positionMs: Long,
        width: Int,
        height: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val maxDim = max(width, height).coerceIn(64, 1280)
        val posSec = (positionMs.coerceAtLeast(0L) / 1000.0)
        val shouldProxy = telegramStreamingProxy.shouldProxy(uri)
        val proxyUrl = if (shouldProxy) telegramStreamingProxy.registerUri(uri) else null
        try {
            val targetPath = proxyUrl ?: resolvePathForThumbnailer(uri) ?: return@withContext null
            extractWithMpvOrRetriever(targetPath, uri, posSec, positionMs, maxDim)
        } finally {
            if (proxyUrl != null) {
                runCatching { telegramStreamingProxy.unregisterUrl(proxyUrl) }
            }
        }
    }

    private suspend fun extractWithMpvOrRetriever(
        path: String,
        originalUri: Uri,
        posSec: Double,
        positionMs: Long,
        maxDim: Int,
    ): Bitmap? = mutex.withLock {
        ensureInitializedLocked()
        val fastBitmap = runCatching {
            FastThumbnails.generate(path, posSec, maxDim, false)
        }.getOrNull()
        if (fastBitmap != null) return@withLock fastBitmap

        val mpvBitmap = runCatching {
            MPVLib.grabThumbnailFast(path, posSec, maxDim, true)
        }.getOrNull()
        if (mpvBitmap != null) return@withLock mpvBitmap

        if (!path.startsWith("http://", ignoreCase = true) && !path.startsWith("https://", ignoreCase = true)) {
            return@withLock extractViaMediaMetadataRetriever(originalUri, positionMs)
        }
        null
    }

    private fun ensureInitializedLocked() {
        if (fastThumbnailsInitialized) return
        runCatching {
            Utils.copyAssets(context)
            MPVLib.setThumbnailJavaVM(context)
            FastThumbnails.initialize(context)
        }
        fastThumbnailsInitialized = true
    }

    private fun resolvePathForThumbnailer(uri: Uri): String? {
        return when (uri.scheme?.lowercase()) {
            null, "file" -> uri.path ?: uri.toString()
            "http", "https" -> uri.toString()
            "content" -> runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    Utils.findRealPath(pfd.fd) ?: "fd://${pfd.detachFd()}"
                }
            }.getOrNull() ?: uri.toString()
            else -> uri.toString()
        }
    }

    private fun extractViaMediaMetadataRetriever(
        uri: Uri,
        positionMs: Long,
    ): Bitmap? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            mmr.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { mmr.release() }
        }
    }

    override fun release() {
        runCatching { FastThumbnails.clearCache() }
    }
}
