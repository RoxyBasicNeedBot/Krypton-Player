package dev.roxy.kryptonplayer.feature.player.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

@UnstableApi
class Media3FrameExtractor(private val context: Context) {

    suspend fun extractFrame(
        uri: Uri,
        positionMs: Long,
        width: Int = 640,
        height: Int = 360
    ): Bitmap? {
        if (Build.VERSION.SDK_INT < 26) return null

        return withContext(Dispatchers.Main) {
            var player: ExoPlayer? = null
            var surfaceTexture: SurfaceTexture? = null
            var surface: Surface? = null
            var handlerThread: HandlerThread? = null

            try {
                // Initialize offscreen SurfaceTexture without GL context
                surfaceTexture = SurfaceTexture(false).apply {
                    setDefaultBufferSize(width, height)
                }
                surface = Surface(surfaceTexture)

                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        /* minBufferMs = */ 500,
                        /* maxBufferMs = */ 1500,
                        /* bufferForPlaybackMs = */ 250,
                        /* bufferForPlaybackAfterRebufferMs = */ 500
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()

                player = ExoPlayer.Builder(context)
                    .setLoadControl(loadControl)
                    .build().apply {
                        setVideoSurface(surface)
                        playWhenReady = false
                        setMediaItem(MediaItem.fromUri(uri))
                        prepare()
                        seekTo(positionMs)
                    }

                val frameRendered = AtomicBoolean(false)
                val frameSignal = kotlinx.coroutines.sync.Mutex(locked = true)

                surfaceTexture.setOnFrameAvailableListener {
                    if (frameRendered.compareAndSet(false, true)) {
                        if (frameSignal.isLocked) {
                            frameSignal.unlock()
                        }
                    }
                }

                val playerListener = object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        if (frameRendered.compareAndSet(false, true)) {
                            if (frameSignal.isLocked) {
                                frameSignal.unlock()
                            }
                        }
                    }
                }
                player.addListener(playerListener)

                // Wait up to 4 seconds for frame rendering
                withTimeoutOrNull(4000L) {
                    frameSignal.lock()
                }

                if (!frameRendered.get()) {
                    return@withContext null
                }

                // Grab the bitmap using PixelCopy (RGB_565 halves memory footprint)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                handlerThread = HandlerThread("PixelCopyThread").apply { start() }
                val handler = Handler(handlerThread.looper)

                val copyResult = suspendCancellableCoroutine<Int> { cont ->
                    try {
                        PixelCopy.request(surface, bitmap, { result ->
                            if (cont.isActive) cont.resume(result)
                        }, handler)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume(PixelCopy.ERROR_UNKNOWN)
                    }
                }

                if (copyResult == PixelCopy.SUCCESS) {
                    bitmap
                } else {
                    bitmap.recycle()
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("Media3FrameExtractor", "Frame extraction failed", e)
                null
            } finally {
                try {
                    player?.release()
                } catch (_: Exception) {}
                try {
                    surface?.release()
                } catch (_: Exception) {}
                try {
                    surfaceTexture?.release()
                } catch (_: Exception) {}
                try {
                    handlerThread?.quitSafely()
                } catch (_: Exception) {}
            }
        }
    }
}

@UnstableApi
class Media3SessionExtractor(
    private val context: Context,
    private val uri: Uri,
    private val width: Int = 320,
    private val height: Int = 180
) {
    private var player: ExoPlayer? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var frameSignal = kotlinx.coroutines.sync.Mutex(locked = false)
    private val frameRendered = AtomicBoolean(false)

    suspend fun initialize() = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT < 26) return@withContext
        try {
            surfaceTexture = SurfaceTexture(false).apply {
                setDefaultBufferSize(width, height)
            }
            surface = Surface(surfaceTexture)

            handlerThread = HandlerThread("Media3SessionCopy").apply { start() }
            handler = Handler(handlerThread!!.looper)

            surfaceTexture!!.setOnFrameAvailableListener {
                if (frameRendered.compareAndSet(false, true)) {
                    if (frameSignal.isLocked) {
                        frameSignal.unlock()
                    }
                }
            }

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 500,
                    /* maxBufferMs = */ 1500,
                    /* bufferForPlaybackMs = */ 250,
                    /* bufferForPlaybackAfterRebufferMs = */ 500
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            player = ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build().apply {
                    setVideoSurface(surface)
                    playWhenReady = false
                    setMediaItem(MediaItem.fromUri(uri))
                    prepare()
                }

            player!!.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (frameRendered.compareAndSet(false, true)) {
                        if (frameSignal.isLocked) {
                            frameSignal.unlock()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("Media3SessionExtractor", "Initialization failed", e)
            release()
        }
    }

    suspend fun getFrameAtTime(positionMs: Long): Bitmap? = withContext(Dispatchers.Main) {
        val p = player ?: return@withContext null
        val s = surface ?: return@withContext null
        val h = handler ?: return@withContext null

        try {
            frameRendered.set(false)
            if (frameSignal.isLocked) {
                frameSignal.unlock()
            }
            frameSignal = kotlinx.coroutines.sync.Mutex(locked = true)

            p.seekTo(positionMs)

            // Wait for frame available up to 2 seconds
            withTimeoutOrNull(2000L) {
                frameSignal.lock()
            }

            if (!frameRendered.get()) {
                return@withContext null
            }

            // RGB_565 reduces memory by 50% compared to ARGB_8888 for seekbar thumbnails
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val copyResult = suspendCancellableCoroutine<Int> { cont ->
                try {
                    PixelCopy.request(s, bitmap, { result ->
                        if (cont.isActive) cont.resume(result)
                    }, h)
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(PixelCopy.ERROR_UNKNOWN)
                }
            }

            if (copyResult == PixelCopy.SUCCESS) {
                bitmap
            } else {
                bitmap.recycle()
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("Media3SessionExtractor", "Frame extraction failed at $positionMs", e)
            null
        }
    }

    fun release() {
        val p = player
        player = null
        val s = surface
        surface = null
        val st = surfaceTexture
        surfaceTexture = null
        val ht = handlerThread
        handlerThread = null
        handler = null

        if (p != null || s != null || st != null || ht != null) {
            val mainHandler = Handler(context.mainLooper)
            mainHandler.post {
                try {
                    p?.release()
                } catch (_: Exception) {}
                try {
                    s?.release()
                } catch (_: Exception) {}
                try {
                    st?.release()
                } catch (_: Exception) {}
                try {
                    ht?.quitSafely()
                } catch (_: Exception) {}
            }
        }
    }
}
