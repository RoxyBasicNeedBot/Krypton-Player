package dev.anilbeesetti.nextplayer.feature.player.state

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@androidx.media3.common.util.UnstableApi
class SeekbarThumbnailState(
    private val context: Context,
) {
    companion object {
        private val activeInstances = java.util.Collections.newSetFromMap(
            java.util.WeakHashMap<SeekbarThumbnailState, Boolean>()
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
    private var retriever: MediaMetadataRetriever? = null
    private var media3Extractor: dev.anilbeesetti.nextplayer.feature.player.thumbnail.Media3SessionExtractor? = null
    // Max 10MB memory cache for seekbar thumbnail frames with active recycling
    private val maxCacheSizeBytes = 10 * 1024 * 1024
    private val cache = object : LruCache<Long, Bitmap>(maxCacheSizeBytes) {
        override fun sizeOf(key: Long, value: Bitmap): Int {
            return value.byteCount
        }

        override fun entryRemoved(evicted: Boolean, key: Long, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && oldValue != thumbnail && !oldValue.isRecycled) {
                try {
                    oldValue.recycle()
                } catch (_: Exception) {}
            }
        }
    }
    private var currentUri: Uri? = null
    private var isInitialized = false
    private val mutex = Mutex()
    private var media3ExtractorFailed = false

    var thumbnail by mutableStateOf<Bitmap?>(null)
        private set

    suspend fun setVideoUri(uri: Uri?) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (uri == currentUri && isInitialized) return@withLock
            releaseInternal()
            currentUri = uri
            if (uri == null) return@withLock

            try {
                val mmr = MediaMetadataRetriever()
                val uriString = uri.toString()

                when {
                    // HTTP/HTTPS streaming URLs (Telegram Cloud, Watch Party, etc.)
                    uriString.startsWith("http://") || uriString.startsWith("https://") -> {
                        val headers = HashMap<String, String>()
                        if (uriString.contains("/proxy/stream")) {
                            headers["x-api-key"] = "ok223377"
                        }
                        headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        mmr.setDataSource(uriString, headers)
                    }
                    // Content URIs (local files via content provider)
                    uriString.startsWith("content://") -> {
                        mmr.setDataSource(context, uri)
                    }
                    // File URIs and direct paths
                    else -> {
                        mmr.setDataSource(context, uri)
                    }
                }

                // Verify it can actually extract frames by checking duration
                val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (durationStr != null) {
                    retriever = mmr
                    isInitialized = true
                } else {
                    mmr.release()
                }
            } catch (e: Exception) {
                android.util.Log.e("SeekbarThumbnailState", "Failed to initialize retriever for $uri", e)
                retriever = null
            }

            if (retriever == null) {
                try {
                    val extractor = dev.anilbeesetti.nextplayer.feature.player.thumbnail.Media3SessionExtractor(context, uri)
                    extractor.initialize()
                    media3Extractor = extractor
                    isInitialized = true
                } catch (e: Exception) {
                    android.util.Log.e("SeekbarThumbnailState", "Failed to initialize Media3SessionExtractor for $uri", e)
                    media3Extractor = null
                    isInitialized = false
                }
            }
        }
    }

    suspend fun requestThumbnail(positionMs: Long) {
        // Round to nearest second for cache efficiency
        val roundedSec = positionMs / 1000L

        // Check cache first (fast path)
        val cachedBitmap = cache.get(roundedSec)
        if (cachedBitmap != null) {
            thumbnail = cachedBitmap
            return
        }

        // Use mutex to prevent concurrent MediaMetadataRetriever/Media3Extractor access
        mutex.withLock {
            val ret = retriever
            var m3Ext = media3Extractor
            if (ret == null && m3Ext == null) return@withLock

            withContext(Dispatchers.IO) {
                try {
                    val timeUs = roundedSec * 1_000_000L
                    var frame = kotlinx.coroutines.withTimeoutOrNull(2000L) {
                        if (ret != null) {
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                                    try {
                                        ret.getScaledFrameAtTime(
                                            timeUs,
                                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                            320,
                                            180
                                        )
                                    } catch (_: Exception) {
                                        ret.getFrameAtTime(
                                            timeUs,
                                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                                        )
                                    }
                                } else {
                                    ret.getFrameAtTime(
                                        timeUs,
                                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                                    )
                                }
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }

                    if (frame == null) {
                        // If retriever failed to get a frame, release it and null it out to avoid using it again for this video
                        if (ret != null) {
                            try {
                                ret.release()
                            } catch (_: Exception) {}
                            if (retriever == ret) {
                                retriever = null
                            }
                        }

                        // Fallback to Media3SessionExtractor
                        if (m3Ext == null && currentUri != null && !media3ExtractorFailed) {
                            try {
                                val extractor = dev.anilbeesetti.nextplayer.feature.player.thumbnail.Media3SessionExtractor(context, currentUri!!)
                                extractor.initialize()
                                media3Extractor = extractor
                                m3Ext = extractor
                            } catch (e: Exception) {
                                android.util.Log.e("SeekbarThumbnailState", "Failed to initialize Media3SessionExtractor lazily for $currentUri", e)
                                media3ExtractorFailed = true
                            }
                        }

                        frame = m3Ext?.getFrameAtTime(positionMs)
                    }

                    if (frame == null) return@withContext

                    // Keep frame if already within 320x180, otherwise scale down
                    val scaled = if (frame.width <= 320 && frame.height <= 180) {
                        frame
                    } else {
                        val aspectRatio = frame.width.toFloat() / frame.height.toFloat()
                        val targetWidth: Int
                        val targetHeight: Int
                        if (aspectRatio >= 1f) {
                            targetWidth = 320
                            targetHeight = (320 / aspectRatio).toInt().coerceAtLeast(1)
                        } else {
                            targetHeight = 180
                            targetWidth = (180 * aspectRatio).toInt().coerceAtLeast(1)
                        }

                        val s = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true)
                        if (s != frame) frame.recycle()
                        s
                    }

                    cache.put(roundedSec, scaled)
                    
                    // State updates must be on Main Thread
                    withContext(Dispatchers.Main) {
                        thumbnail = scaled
                    }
                } catch (_: Exception) {
                    // Silently handle - some formats/streams may not support frame extraction
                }
            }
        }
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
        val oldMedia3Extractor = media3Extractor
        media3Extractor = null
        isInitialized = false
        cache.evictAll()
        thumbnail = null
        currentUri = null
        media3ExtractorFailed = false

        if (oldRetriever != null) {
            scope.launch {
                try {
                    oldRetriever.release()
                } catch (_: Exception) {}
            }
        }

        oldMedia3Extractor?.release()
    }

    fun release() {
        releaseInternal()
    }
}

@kotlin.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun rememberSeekbarThumbnailState(): SeekbarThumbnailState {
    val context = LocalContext.current
    val state = remember { SeekbarThumbnailState(context) }
    DisposableEffect(Unit) {
        onDispose { state.release() }
    }
    return state
}
