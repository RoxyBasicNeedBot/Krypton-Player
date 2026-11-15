package dev.anilbeesetti.nextplayer.core.data.tdlib

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Nagram-Grade ExoPlayer / Media3 DataSource that streams video directly from Telegram Data Centers
 * using progressive disk-buffered chunk streaming via TDLib MTProto.
 *
 * Coordinates cleanly with TDLib's internal download engine (FileManager.cpp:2978-2987 & PartsManager.cpp):
 * 1. Progressive streaming window: Keeps offset constant during sequential playback and smoothly
 *    expands the limit forward without resetting the streaming offset. This prevents TDLib from
 *    canceling active in-flight MTProto chunk downloads with "Canceled by another downloadFile request".
 * 2. On explicit seek (random access), shifts stream offset to requested position with priority 32.
 * 3. Respects active background downloads (isExplicitFullDownload) so playback never cancels ongoing downloads.
 * 4. RandomAccessFile disk reads directly from TDLib's native local storage for zero-copy performance.
 */
@OptIn(UnstableApi::class)
class TdlibDataSource(
    private val tdlibManager: TdlibManager,
    private val tdlibFileId: Int,
    private val totalFileSize: Long,
    private val localFilePath: String
) : DataSource {

    companion object {
        private const val TAG = "TdlibDataSource"
        private const val READ_WAIT_TIMEOUT_MS = 25_000L
        private const val POLL_INTERVAL_MS = 15L
        private const val INITIAL_STREAM_WINDOW = 32 * 1024 * 1024L // 32 MB initial progressive window (boosted)
        private const val WINDOW_EXTEND_STEP = 32 * 1024 * 1024L // 32 MB limit expansion step
        private const val PREFETCH_THRESHOLD = 16 * 1024 * 1024L // 16 MB buffer ahead threshold to expand limit
    }

    private var dataSpec: DataSpec? = null
    private var randomAccessFile: RandomAccessFile? = null
    private var currentPosition = 0L
    private var bytesRemaining = 0L

    @Volatile
    private var currentDownloadOffset = 0L
    @Volatile
    private var currentDownloadedPrefixSize = 0L
    @Volatile
    private var currentDownloadedSize = 0L
    @Volatile
    private var isDownloadingActive = true
    @Volatile
    private var isDownloadingCompleted = false
    @Volatile
    private var lastProgressTimeMs = 0L

    private val lock = Object()
    private var activeFilePath: String = localFilePath
    private var targetFile: File = if (localFilePath.isNotEmpty()) File(localFilePath) else File("")
    @Volatile
    private var effectiveTotalSize: Long = if (totalFileSize > 65536L) totalFileSize else if (totalFileSize > 0) totalFileSize else 0L

    @Volatile
    private var currentStreamOffset = 0L
    @Volatile
    private var currentRequestedLimit = 0L
    @Volatile
    private var lastRequestedOffset = -1L
    @Volatile
    private var lastRequestedLimit = -1L
    @Volatile
    private var lastRequestTimeMs = 0L

    /**
     * Calculates how many contiguous valid bytes starting at [pos] are physically ready on disk.
     * Returns 0 if [pos] is not currently ready to read.
     */
    private fun getAvailableBytesAt(pos: Long): Long {
        if (isDownloadingCompleted) {
            val total = if (effectiveTotalSize > 0) effectiveTotalSize else targetFile.length()
            return maxOf(0L, total - pos)
        }
        val offset = currentDownloadOffset
        val prefix = currentDownloadedPrefixSize
        if (offset == 0L) {
            if (pos < prefix) {
                return prefix - pos
            }
        } else {
            val chunkEnd = offset + prefix
            if (pos >= offset && pos < chunkEnd) {
                return chunkEnd - pos
            }
        }
        return 0L
    }

    private fun requestStreamWindow(offset: Long, limit: Long) {
        if (limit <= 0L || isDownloadingCompleted) return
        val now = System.currentTimeMillis()
        if (offset == lastRequestedOffset && limit == lastRequestedLimit && (now - lastRequestTimeMs) < 1500L) {
            return
        }
        if (isDownloadingActive && currentDownloadOffset == offset && currentDownloadedPrefixSize > 0 && (now - lastProgressTimeMs) < 2000L && limit <= lastRequestedLimit) {
            return
        }
        lastRequestedOffset = offset
        lastRequestedLimit = limit
        lastRequestTimeMs = now
        tdlibManager.downloadFile(
            fileId = tdlibFileId,
            offset = offset,
            limit = limit,
            priority = 32
        )
    }

    private val fileListener: (TdApi.File) -> Unit = { file ->
        if (file.id == tdlibFileId) {
            synchronized(lock) {
                val path = file.local.path
                if (path.isNotEmpty() && (activeFilePath.isEmpty() || activeFilePath != path || !targetFile.exists())) {
                    activeFilePath = path
                    targetFile = File(path)
                    try { randomAccessFile?.close() } catch (_: Exception) {}
                    randomAccessFile = null
                }
                currentDownloadOffset = file.local.downloadOffset
                currentDownloadedPrefixSize = file.local.downloadedPrefixSize
                currentDownloadedSize = file.local.downloadedSize
                isDownloadingActive = file.local.isDownloadingActive
                isDownloadingCompleted = file.local.isDownloadingCompleted
                if (file.size > 0 && (effectiveTotalSize <= 0 || effectiveTotalSize == 65536L)) {
                    effectiveTotalSize = file.size
                }
                lastProgressTimeMs = System.currentTimeMillis()
                lock.notifyAll()
            }
            tdlibManager.updateStreamingProgress(
                downloadedBytes = file.local.downloadedSize,
                totalBytes = if (file.size > 0) file.size else effectiveTotalSize
            )
        }
    }

    override fun addTransferListener(transferListener: TransferListener) {
        // Handled directly via TDLib MTProto
    }

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.currentPosition = dataSpec.position

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            if (effectiveTotalSize > 0) effectiveTotalSize - currentPosition else C.LENGTH_UNSET.toLong()
        }

        Log.d(TAG, "open() position=$currentPosition, length=$bytesRemaining, fileId=$tdlibFileId")

        tdlibManager.keepStreamingDownload(tdlibFileId)
        tdlibManager.optimizeNetworkForDownload()
        tdlibManager.registerFileListener(tdlibFileId, fileListener)

        // Ensure parent directory and target file exist if path is already known
        if (activeFilePath.isNotEmpty() && !targetFile.exists()) {
            try {
                targetFile.parentFile?.mkdirs()
                targetFile.createNewFile()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating cache file: $activeFilePath", e)
            }
        }

        currentStreamOffset = currentPosition
        currentRequestedLimit = if (effectiveTotalSize > 0) {
            minOf(INITIAL_STREAM_WINDOW, maxOf(0L, effectiveTotalSize - currentPosition))
        } else {
            INITIAL_STREAM_WINDOW
        }

        val isFullDownload = tdlibManager.isExplicitFullDownload(tdlibFileId)
        // If full download is active and current position is already covered by downloaded prefix, don't interfere
        if (isFullDownload && currentPosition == 0L) {
            // Already downloading full file sequentially from 0
        } else {
            requestStreamWindow(currentStreamOffset, currentRequestedLimit)
        }

        // Pre-buffer bytes so ExoPlayer extractors sniff container format reliably (boosted for rapid playback start)
        val targetInitialBytes = if (currentPosition == 0L) {
            if (effectiveTotalSize > 0) minOf(effectiveTotalSize, 131072L) else 131072L
        } else {
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) minOf(bytesRemaining, 32768L) else 32768L
        }
        val minRequired = if (currentPosition == 0L) minOf(targetInitialBytes, 8L) else 1L
        val prebufferDeadline = System.currentTimeMillis() + 15000L
        synchronized(lock) {
            while (!isDownloadingCompleted && getAvailableBytesAt(currentPosition) < minRequired && System.currentTimeMillis() < prebufferDeadline) {
                try {
                    lock.wait(50L)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
        Log.d(TAG, "Pre-buffer ready at $currentPosition: available=${getAvailableBytesAt(currentPosition)} bytes (min: $minRequired)")

        if (getAvailableBytesAt(currentPosition) < minRequired && !isDownloadingCompleted) {
            throw IOException("[TDLib MTProto] Prebuffering timed out at pos $currentPosition (${getAvailableBytesAt(currentPosition)} < $minRequired bytes)")
        }

        if (activeFilePath.isNotEmpty() && targetFile.exists()) {
            randomAccessFile = RandomAccessFile(activeFilePath, "r")
        }
        return if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining else C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length <= 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val safeLength = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            if (bytesRemaining <= 0L) return C.RESULT_END_OF_INPUT
            minOf(length.toLong(), bytesRemaining).toInt()
        } else {
            length
        }

        if (safeLength <= 0) return C.RESULT_END_OF_INPUT

        val deadline = System.currentTimeMillis() + READ_WAIT_TIMEOUT_MS
        val minNeeded = if (currentPosition == 0L) minOf(8L, if (effectiveTotalSize > 0) effectiveTotalSize else 8L) else 1L

        // Wait strictly until required bytes AT currentPosition are physically available
        synchronized(lock) {
            while (!isDownloadingCompleted && getAvailableBytesAt(currentPosition) < minNeeded) {
                if (effectiveTotalSize > 0 && currentPosition >= effectiveTotalSize) {
                    return C.RESULT_END_OF_INPUT
                }
                val timeLeft = deadline - System.currentTimeMillis()
                if (timeLeft <= 0) {
                    val avail = getAvailableBytesAt(currentPosition)
                    if (avail > 0) break
                    if (totalFileSize > 0 && currentPosition >= totalFileSize) {
                        return C.RESULT_END_OF_INPUT
                    }
                    if (isDownloadingCompleted) {
                        return C.RESULT_END_OF_INPUT
                    }
                    throw IOException("[TDLib MTProto] Read timeout at pos $currentPosition (downloadOffset=$currentDownloadOffset, prefix=$currentDownloadedPrefixSize, disk=${targetFile.length()} bytes, total: $totalFileSize)")
                }

                // If pos is outside the current stream window, seek TDLib to currentPosition
                if (currentPosition < currentStreamOffset || currentPosition >= (currentStreamOffset + currentRequestedLimit)) {
                    currentStreamOffset = currentPosition
                    currentRequestedLimit = if (effectiveTotalSize > 0) {
                        minOf(INITIAL_STREAM_WINDOW, maxOf(0L, effectiveTotalSize - currentPosition))
                    } else INITIAL_STREAM_WINDOW
                }

                requestStreamWindow(currentStreamOffset, currentRequestedLimit)

                try {
                    lock.wait(POLL_INTERVAL_MS)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("[TDLib MTProto] Read interrupted")
                }
            }
        }

        val available = getAvailableBytesAt(currentPosition)
        if (available <= 0L) {
            if (totalFileSize > 0 && currentPosition >= totalFileSize) {
                return C.RESULT_END_OF_INPUT
            }
            if (isDownloadingCompleted) {
                return C.RESULT_END_OF_INPUT
            }
            throw IOException("[TDLib MTProto] Buffer stall: read pos $currentPosition (avail=$available, total=$totalFileSize)")
        }

        // Seamless progressive window expansion:
        // When remaining unread buffer inside requested limit window is below PREFETCH_THRESHOLD,
        // EXPAND the limit from the current offset WITHOUT changing currentStreamOffset!
        // In TDLib (FileDownloader.cpp:473), keeping offset unchanged updates streaming_limit
        // WITHOUT cancelling any active MTProto download parts!
        val remainingInWindow = (currentStreamOffset + currentRequestedLimit) - currentPosition
        if (remainingInWindow < PREFETCH_THRESHOLD && !isDownloadingCompleted) {
            val maxAllowed = if (effectiveTotalSize > 0) (effectiveTotalSize - currentStreamOffset) else Long.MAX_VALUE
            if (currentRequestedLimit < maxAllowed) {
                currentRequestedLimit = minOf(currentRequestedLimit + WINDOW_EXTEND_STEP, maxAllowed)
                requestStreamWindow(currentStreamOffset, currentRequestedLimit)
            }
        }

        val bytesToRead = minOf(safeLength.toLong(), available).toInt()
        if (bytesToRead <= 0) {
            if (totalFileSize > 0 && currentPosition >= totalFileSize) {
                return C.RESULT_END_OF_INPUT
            }
            if (isDownloadingCompleted) {
                return C.RESULT_END_OF_INPUT
            }
            return 0
        }

        if (randomAccessFile == null && activeFilePath.isNotEmpty() && targetFile.exists()) {
            randomAccessFile = RandomAccessFile(activeFilePath, "r")
        }
        val raf = randomAccessFile ?: throw IOException("[TDLib MTProto] Cache file not available on disk: $activeFilePath")
        raf.seek(currentPosition)

        val readCount = raf.read(buffer, offset, bytesToRead)
        if (readCount > 0) {
            currentPosition += readCount
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                bytesRemaining = maxOf(0L, bytesRemaining - readCount)
            }
            return readCount
        } else if (readCount < 0) {
            if (totalFileSize > 0 && currentPosition >= totalFileSize) {
                return C.RESULT_END_OF_INPUT
            }
            if (isDownloadingCompleted) {
                return C.RESULT_END_OF_INPUT
            }
            throw IOException("[TDLib MTProto] Unexpected EOF from disk at pos $currentPosition / $totalFileSize (avail: $available)")
        }

        if (totalFileSize > 0 && currentPosition >= totalFileSize) {
            return C.RESULT_END_OF_INPUT
        }
        if (isDownloadingCompleted) {
            return C.RESULT_END_OF_INPUT
        }
        throw IOException("[TDLib MTProto] Incomplete read at pos $currentPosition / $totalFileSize (avail: $available)")
    }

    fun getCurrentPosition(): Long = currentPosition
    fun getTotalFileSize(): Long = totalFileSize

    override fun getUri(): Uri? = dataSpec?.uri

    override fun close() {
        Log.d(TAG, "close() fileId=$tdlibFileId")
        tdlibManager.unregisterFileListener(tdlibFileId, fileListener)
        // If an explicit background download is active, restore full file download from 0 to EOF
        if (tdlibManager.isExplicitFullDownload(tdlibFileId)) {
            tdlibManager.downloadFile(tdlibFileId, offset = 0, limit = 0, priority = 32)
        } else {
            // Cancel chunk streaming download with debounce
            tdlibManager.cancelStreamingDownload(tdlibFileId, 3500L)
        }
        try {
            randomAccessFile?.close()
        } catch (_: Exception) {}
        randomAccessFile = null
        dataSpec = null
    }
}
