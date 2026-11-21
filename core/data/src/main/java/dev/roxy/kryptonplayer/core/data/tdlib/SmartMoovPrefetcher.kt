package dev.roxy.kryptonplayer.core.data.tdlib

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart prefetcher for MP4/MOV containers where supports_streaming == false.
 *
 * In non-faststart MP4 files, the crucial 'moov' atom (metadata, sample tables, audio/video track
 * indexing) is located at the trailing end of the file rather than the beginning. When ExoPlayer / Media3
 * attempts playback of such files, it reads the initial container header, detects no 'moov' atom, and
 * immediately seeks to the trailing megabytes of the file to parse the track headers.
 *
 * This prefetcher calculates the trailing tail range, pre-buffers the trailing atom chunks over TDLib
 * MTProto 2.0 with high priority (32), and ensures that by the time ExoPlayer requests the moov atom,
 * the bytes are already resident on disk or actively downloading—preventing playback stalls and timeouts.
 */
@Singleton
class SmartMoovPrefetcher @Inject constructor(
    private val tdlibManager: TdlibManager
) {
    companion object {
        private const val TAG = "SmartMoovPrefetcher"
        private const val MIN_MOOV_TAIL_SIZE = 1024 * 1024L // 1 MB minimum
        private const val DEFAULT_MOOV_TAIL_SIZE = 3 * 1024 * 1024L // 3 MB default
        private const val MAX_MOOV_TAIL_SIZE = 8 * 1024 * 1024L // 8 MB max for multi-GB movies
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activePrefetches = ConcurrentHashMap<Int, CompletableDeferred<Boolean>>()

    /**
     * Calculates the optimal byte offset and size of the trailing atom window based on total file size.
     */
    fun computeTailRange(fileSize: Long): Pair<Long, Long> {
        if (fileSize <= 0L) {
            return Pair(0L, DEFAULT_MOOV_TAIL_SIZE)
        }

        val tailSize = when {
            fileSize < 30 * 1024 * 1024L -> minOf(fileSize, MIN_MOOV_TAIL_SIZE)
            fileSize < 200 * 1024 * 1024L -> 2 * 1024 * 1024L
            fileSize < 800 * 1024 * 1024L -> DEFAULT_MOOV_TAIL_SIZE
            fileSize < 2048 * 1024 * 1024L -> 5 * 1024 * 1024L
            else -> MAX_MOOV_TAIL_SIZE
        }

        val tailOffset = maxOf(0L, fileSize - tailSize)
        return Pair(tailOffset, tailSize)
    }

    /**
     * Checks if a [TdApi.Video] requires trailing moov prefetching.
     */
    fun requiresMoovPrefetch(video: TdApi.Video): Boolean {
        // When supportsStreaming is true, the moov box is at the beginning; no tail prefetch required
        return !video.supportsStreaming && video.video.size > 0
    }

    /**
     * Initiates asynchronous prefetching of the trailing moov atom for [fileId].
     * Deduplicates in-flight calls and returns whether the tail was prefetched successfully.
     */
    suspend fun prefetchMoovAtom(
        fileId: Int,
        fileSize: Long,
        timeoutMs: Long = 6000L
    ): Boolean {
        if (fileSize <= 0L || !tdlibManager.isReady()) return false

        val (tailOffset, tailLimit) = computeTailRange(fileSize)

        val deferred = activePrefetches.computeIfAbsent(fileId) {
            val d = CompletableDeferred<Boolean>()

            tdlibManager.addLog(
                level = "STREAM",
                message = "Prefetching non-streaming moov atom: fileId=$fileId, offset=$tailOffset, limit=$tailLimit",
                tag = "MOOV"
            )
            Log.i(TAG, "Initiating smart moov prefetch: fileId=$fileId, offset=$tailOffset, limit=$tailLimit, total=$fileSize")

            // 1. Send TDLib chunk download request for trailing bytes with priority 32
            tdlibManager.downloadFile(
                fileId = fileId,
                offset = tailOffset,
                limit = tailLimit,
                priority = 32
            )

            // 2. Register file listener to complete once trailing bytes are resident
            val listener: (TdApi.File) -> Unit = { file ->
                if (file.id == fileId) {
                    val isDone = file.local.isDownloadingCompleted ||
                        (file.local.downloadOffset == tailOffset && file.local.downloadedPrefixSize > 0) ||
                        file.local.downloadedSize >= tailLimit

                    if (isDone && !d.isCompleted) {
                        d.complete(true)
                    }
                }
            }

            tdlibManager.registerFileListener(fileId, listener)

            d.invokeOnCompletion {
                tdlibManager.unregisterFileListener(fileId, listener)
                activePrefetches.remove(fileId)
            }

            d
        }

        return withTimeoutOrNull(timeoutMs) {
            try {
                deferred.await()
            } catch (_: Throwable) {
                false
            }
        } ?: false
    }

    /**
     * Proactively launches trailing moov prefetch in background if needed.
     */
    fun prefetchAsync(fileId: Int, fileSize: Long) {
        if (fileSize <= 0L) return
        scope.launch {
            prefetchMoovAtom(fileId, fileSize, timeoutMs = 8000L)
        }
    }
}
