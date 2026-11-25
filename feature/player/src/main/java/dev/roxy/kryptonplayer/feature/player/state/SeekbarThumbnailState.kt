package dev.roxy.kryptonplayer.feature.player.state

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.roxy.kryptonplayer.core.common.playback.FrameThumbnailExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.WeakHashMap

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface SeekbarThumbnailExtractorEntryPoint {
    fun frameThumbnailExtractor(): FrameThumbnailExtractor
}

class SeekbarThumbnailState(
    private val context: Context,
    private val frameExtractor: FrameThumbnailExtractor? = null,
) {
    companion object {
        private val activeInstances = Collections.newSetFromMap(
            WeakHashMap<SeekbarThumbnailState, Boolean>(),
        )

        fun clearAllCaches() {
            synchronized(activeInstances) {
                activeInstances.toList().forEach { it?.clearCache() }
            }
        }
    }

    init {
        synchronized(activeInstances) {
            activeInstances.add(this)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val maxCacheSizeBytes = 10 * 1024 * 1024
    private val cache = object : LruCache<Long, Bitmap>(maxCacheSizeBytes) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount

        override fun entryRemoved(evicted: Boolean, key: Long, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && oldValue != thumbnail && !oldValue.isRecycled) {
                runCatching { oldValue.recycle() }
            }
        }
    }
    private var retriever: MediaMetadataRetriever? = null
    private var currentUri: Uri? = null
    private val mutex = Mutex()

    var thumbnail by mutableStateOf<Bitmap?>(null)
        private set

    suspend fun setVideoUri(uri: Uri?) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (uri == currentUri && (retriever != null || frameExtractor != null)) return@withLock
            releaseInternal()
            currentUri = uri
            if (uri == null) return@withLock
            if (frameExtractor != null) return@withLock

            runCatching {
                val mmr = MediaMetadataRetriever()
                val uriString = uri.toString()
                if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                    mmr.setDataSource(uriString, emptyMap())
                } else {
                    mmr.setDataSource(context, uri)
                }
                if (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) != null) {
                    retriever = mmr
                } else {
                    mmr.release()
                }
            }
        }
    }

    suspend fun requestThumbnail(positionMs: Long) {
        val roundedSec = (positionMs / 1000L).coerceAtLeast(0L)
        cache.get(roundedSec)?.let { cached ->
            thumbnail = cached
            return
        }

        val activeUri = currentUri ?: return
        val extracted = frameExtractor?.extractFrame(
            uri = activeUri,
            positionMs = roundedSec * 1000L,
            width = 320,
            height = 180,
        ) ?: mutex.withLock {
            withContext(Dispatchers.IO) {
                val ret = retriever ?: return@withContext null
                val timeUs = roundedSec * 1_000_000L
                runCatching {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                        ret.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 180)
                    } else {
                        ret.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                }.getOrNull()
            }
        } ?: return

        val scaled = scaleBitmap(extracted)
        cache.put(roundedSec, scaled)
        withContext(Dispatchers.Main) {
            thumbnail = scaled
        }
    }

    private fun scaleBitmap(frame: Bitmap): Bitmap {
        if (frame.width <= 320 && frame.height <= 180) return frame
        val aspectRatio = frame.width.toFloat() / frame.height.toFloat().coerceAtLeast(1f)
        val (w, h) = if (aspectRatio >= 1f) {
            320 to (320 / aspectRatio).toInt().coerceAtLeast(1)
        } else {
            (180 * aspectRatio).toInt().coerceAtLeast(1) to 180
        }
        val scaled = Bitmap.createScaledBitmap(frame, w, h, true)
        if (scaled != frame) frame.recycle()
        return scaled
    }

    fun clearCache() {
        cache.evictAll()
        thumbnail = null
    }

    fun clearThumbnail() {
        thumbnail = null
    }

    private fun releaseInternal() {
        val oldRetriever = retriever
        retriever = null
        cache.evictAll()
        thumbnail = null
        currentUri = null
        if (oldRetriever != null) {
            scope.launch { runCatching { oldRetriever.release() } }
        }
    }

    fun release() {
        releaseInternal()
    }
}

@Composable
fun rememberSeekbarThumbnailState(): SeekbarThumbnailState {
    val context = LocalContext.current
    val extractor = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                SeekbarThumbnailExtractorEntryPoint::class.java,
            ).frameThumbnailExtractor()
        }.getOrNull()
    }
    val state = remember(context, extractor) { SeekbarThumbnailState(context, extractor) }
    DisposableEffect(state) {
        onDispose { state.release() }
    }
    return state
}
