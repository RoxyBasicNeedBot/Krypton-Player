package dev.roxy.kryptonplayer.core.data.tdlib

import android.util.Log
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pure Kotlin random-access stream that reads video directly from Telegram Data Centers
 * using progressive disk-buffered chunk streaming via TDLib MTProto.
 *
 * Coordinates with TDLib's internal download engine:
 * 1. Progressive streaming window: Keeps offset constant during sequential playback and smoothly
 *    expands the limit forward without resetting the streaming offset, preventing TDLib from
 *    canceling active in-flight MTProto chunk downloads.
 * 2. On explicit seek (random access), shifts stream offset to requested position with priority 32.
 * 3. Respects active background downloads ([TdlibManager.isExplicitFullDownload]) so playback
 *    never cancels ongoing full downloads.
 * 4. RandomAccessFile disk reads directly from TDLib's native local storage for zero-copy reads.
 */
class TdlibRandomAccessStream(
    private val tdlibManager: TdlibManager,
    private val tdlibFileId: Int,
    private val totalFileSize: Long,
    private val localFilePath: String,
) : AutoCloseable {

    companion object {
        private const val TAG = "TdlibRandomAccessStream"
        private const val READ_WAIT_TIMEOUT_MS = 25_000L
        private const val POLL_INTERVAL_MS = 15L
        private const val PREBUFFER_TIMEOUT_MS = 15_000L
        private const val INITIAL_STREAM_WINDOW = 32 * 1024 * 1024L // 32 MB sliding window
        private const val WINDOW_EXTEND_STEP = 32 * 1024 * 1024L // 32 MB limit expansion step
        private const val PREFETCH_THRESHOLD = 16 * 1024 * 1024L // 16 MB prefetch ahead threshold
        const val LENGTH_UNSET = -1L
        const val END_OF_INPUT = -1
    }

    private val closed = AtomicBoolean(false)
    private val lock = Object()
    private val rafLock = Any()

    private var randomAccessFile: RandomAccessFile? = null
    private var currentPosition = 0L
    private var bytesRemaining = LENGTH_UNSET

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

    @Volatile
    private var activeFilePath: String = localFilePath

    @Volatile
    private var targetFile: File = if (localFilePath.isNotEmpty()) File(localFilePath) else File("")

    @Volatile
    private var effectiveTotalSize: Long = when {
        totalFileSize > 65536L -> totalFileSize
        totalFileSize > 0L -> totalFileSize
        else -> 0L
    }

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

    private val fileListener: (TdApi.File) -> Unit = { file ->
        if (file.id == tdlibFileId && !closed.get()) {
            synchronized(lock) {
                val path = file.local.path
                if (path.isNotEmpty() && (activeFilePath.isEmpty() || activeFilePath != path || !targetFile.exists())) {
                    activeFilePath = path
                    targetFile = File(path)
                    synchronized(rafLock) {
                        runCatching { randomAccessFile?.close() }
                        randomAccessFile = null
                    }
                }
                currentDownloadOffset = file.local.downloadOffset
                currentDownloadedPrefixSize = file.local.downloadedPrefixSize
                currentDownloadedSize = file.local.downloadedSize
                isDownloadingActive = file.local.isDownloadingActive
                isDownloadingCompleted = file.local.isDownloadingCompleted
                if (file.size > 0 && (effectiveTotalSize <= 0L || effectiveTotalSize == 65536L)) {
                    effectiveTotalSize = file.size
                }
                lastProgressTimeMs = System.currentTimeMillis()
                lock.notifyAll()
            }
            tdlibManager.updateStreamingProgress(
                downloadedBytes = file.local.downloadedSize,
                totalBytes = if (file.size > 0L) file.size else effectiveTotalSize,
            )
        }
    }

    /**
     * Calculates how many contiguous valid bytes starting at [pos] are physically ready on disk.
     * Returns 0 if [pos] is not currently ready to read.
     */
    private fun getAvailableBytesAt(pos: Long): Long {
        if (isDownloadingCompleted) {
            val total = if (effectiveTotalSize > 0L) effectiveTotalSize else targetFile.length()
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
            if (pos in offset until chunkEnd) {
                return chunkEnd - pos
            }
        }
        return 0L
    }

    private fun requestStreamWindow(offset: Long, limit: Long) {
        if (limit <= 0L || isDownloadingCompleted || closed.get()) return
        val now = System.currentTimeMillis()
        if (offset == lastRequestedOffset && limit == lastRequestedLimit && (now - lastRequestTimeMs) < 1500L) {
            return
        }
        if (isDownloadingActive &&
            currentDownloadOffset == offset &&
            currentDownloadedPrefixSize > 0L &&
            (now - lastProgressTimeMs) < 2000L &&
            limit <= lastRequestedLimit
        ) {
            return
        }
        lastRequestedOffset = offset
        lastRequestedLimit = limit
        lastRequestTimeMs = now
        tdlibManager.downloadFile(
            fileId = tdlibFileId,
            offset = offset,
            limit = limit,
            priority = 32,
        )
    }

    /**
     * Opens the TDLib stream at [offset] for up to [length] bytes (or [LENGTH_UNSET] until EOF).
     * Pre-buffers initial header/seek bytes before returning remaining byte count or [LENGTH_UNSET].
     */
    @Throws(IOException::class)
    fun open(offset: Long = 0L, length: Long = LENGTH_UNSET): Long {
        if (closed.get()) {
            throw IOException("[TDLib MTProto] Stream already closed")
        }
        if (offset < 0L) {
            throw IOException("[TDLib MTProto] Invalid negative offset: $offset")
        }

        currentPosition = offset
        bytesRemaining = when {
            length >= 0L -> length
            effectiveTotalSize > 0L -> maxOf(0L, effectiveTotalSize - currentPosition)
            else -> LENGTH_UNSET
        }

        Log.d(TAG, "open() position=$currentPosition, length=$bytesRemaining, fileId=$tdlibFileId")

        try {
            tdlibManager.keepStreamingDownload(tdlibFileId)
            tdlibManager.optimizeNetworkForDownload()
            tdlibManager.registerFileListener(tdlibFileId, fileListener)

            if (activeFilePath.isNotEmpty() && !targetFile.exists()) {
                try {
                    targetFile.parentFile?.mkdirs()
                    targetFile.createNewFile()
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating cache file: $activeFilePath", e)
                }
            }

            if (bytesRemaining == 0L) {
                return 0L
            }

            currentStreamOffset = currentPosition
            currentRequestedLimit = if (effectiveTotalSize > 0L) {
                minOf(INITIAL_STREAM_WINDOW, maxOf(0L, effectiveTotalSize - currentPosition))
            } else {
                INITIAL_STREAM_WINDOW
            }

            val isFullDownload = tdlibManager.isExplicitFullDownload(tdlibFileId)
            if (!(isFullDownload && currentPosition == 0L)) {
                requestStreamWindow(currentStreamOffset, currentRequestedLimit)
            }

            // Pre-buffer initial bytes so container probes start reliably
            val targetInitialBytes = if (currentPosition == 0L) {
                if (effectiveTotalSize > 0L) minOf(effectiveTotalSize, 131072L) else 131072L
            } else {
                if (bytesRemaining != LENGTH_UNSET) minOf(bytesRemaining, 32768L) else 32768L
            }
            val minRequired = if (currentPosition == 0L) minOf(targetInitialBytes, 8L) else 1L
            val prebufferDeadline = System.currentTimeMillis() + PREBUFFER_TIMEOUT_MS
            synchronized(lock) {
                while (!closed.get() &&
                    !isDownloadingCompleted &&
                    getAvailableBytesAt(currentPosition) < minRequired &&
                    System.currentTimeMillis() < prebufferDeadline
                ) {
                    try {
                        lock.wait(50L)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }

            if (closed.get()) {
                throw IOException("[TDLib MTProto] Stream closed while prebuffering")
            }

            Log.d(
                TAG,
                "Pre-buffer ready at $currentPosition: available=${getAvailableBytesAt(currentPosition)} bytes (min: $minRequired)",
            )

            if (getAvailableBytesAt(currentPosition) < minRequired && !isDownloadingCompleted) {
                throw IOException(
                    "[TDLib MTProto] Prebuffering timed out at pos $currentPosition (${getAvailableBytesAt(currentPosition)} < $minRequired bytes)",
                )
            }

            if (length < 0L && effectiveTotalSize > 0L) {
                bytesRemaining = maxOf(0L, effectiveTotalSize - currentPosition)
            }

            synchronized(rafLock) {
                if (activeFilePath.isNotEmpty() && targetFile.exists()) {
                    randomAccessFile = RandomAccessFile(activeFilePath, "r")
                }
            }

            return bytesRemaining
        } catch (t: Throwable) {
            close()
            throw if (t is IOException) t else IOException(t)
        }
    }

    /**
     * Reads up to [length] bytes into [buffer] starting at [offset].
     * Returns the number of bytes read, or [END_OF_INPUT] (`-1`) on EOF.
     */
    @Throws(IOException::class)
    fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (offset < 0 || length < 0 || offset + length > buffer.size) {
            throw IndexOutOfBoundsException("offset=$offset, length=$length, size=${buffer.size}")
        }
        if (length == 0) return 0
        if (closed.get()) {
            throw IOException("[TDLib MTProto] Stream closed")
        }
        if (bytesRemaining == 0L) return END_OF_INPUT

        val safeLength = if (bytesRemaining != LENGTH_UNSET) {
            if (bytesRemaining <= 0L) return END_OF_INPUT
            minOf(length.toLong(), bytesRemaining).toInt()
        } else {
            length
        }

        if (safeLength <= 0) return END_OF_INPUT

        val deadline = System.currentTimeMillis() + READ_WAIT_TIMEOUT_MS
        val minNeeded = if (currentPosition == 0L) {
            minOf(8L, if (effectiveTotalSize > 0L) effectiveTotalSize else 8L)
        } else {
            1L
        }

        synchronized(lock) {
            while (!isDownloadingCompleted && getAvailableBytesAt(currentPosition) < minNeeded) {
                if (closed.get()) {
                    throw IOException("[TDLib MTProto] Stream closed")
                }
                val knownTotal = getKnownTotalSize()
                if (knownTotal > 0L && currentPosition >= knownTotal) {
                    return END_OF_INPUT
                }
                val timeLeft = deadline - System.currentTimeMillis()
                if (timeLeft <= 0L) {
                    val avail = getAvailableBytesAt(currentPosition)
                    if (avail > 0L) break
                    if (knownTotal > 0L && currentPosition >= knownTotal) {
                        return END_OF_INPUT
                    }
                    if (isDownloadingCompleted) {
                        return END_OF_INPUT
                    }
                    throw IOException(
                        "[TDLib MTProto] Read timeout at pos $currentPosition (downloadOffset=$currentDownloadOffset, prefix=$currentDownloadedPrefixSize, disk=${targetFile.length()} bytes, total: $knownTotal)",
                    )
                }

                // If pos is outside the current stream window, seek TDLib to currentPosition
                if (currentPosition < currentStreamOffset ||
                    currentPosition >= (currentStreamOffset + currentRequestedLimit)
                ) {
                    currentStreamOffset = currentPosition
                    currentRequestedLimit = if (effectiveTotalSize > 0L) {
                        minOf(INITIAL_STREAM_WINDOW, maxOf(0L, effectiveTotalSize - currentPosition))
                    } else {
                        INITIAL_STREAM_WINDOW
                    }
                }

                requestStreamWindow(currentStreamOffset, currentRequestedLimit)

                try {
                    lock.wait(POLL_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("[TDLib MTProto] Read interrupted")
                }
            }
        }

        if (closed.get()) {
            throw IOException("[TDLib MTProto] Stream closed")
        }

        val knownTotal = getKnownTotalSize()
        val available = getAvailableBytesAt(currentPosition)
        if (available <= 0L) {
            if (knownTotal > 0L && currentPosition >= knownTotal) {
                return END_OF_INPUT
            }
            if (isDownloadingCompleted) {
                return END_OF_INPUT
            }
            throw IOException(
                "[TDLib MTProto] Buffer stall: read pos $currentPosition (avail=$available, total=$knownTotal)",
            )
        }

        // Seamless progressive window expansion without changing currentStreamOffset
        val remainingInWindow = (currentStreamOffset + currentRequestedLimit) - currentPosition
        if (remainingInWindow < PREFETCH_THRESHOLD && !isDownloadingCompleted) {
            val maxAllowed = if (effectiveTotalSize > 0L) {
                effectiveTotalSize - currentStreamOffset
            } else {
                Long.MAX_VALUE
            }
            if (currentRequestedLimit < maxAllowed) {
                currentRequestedLimit = minOf(currentRequestedLimit + WINDOW_EXTEND_STEP, maxAllowed)
                requestStreamWindow(currentStreamOffset, currentRequestedLimit)
            }
        }

        val bytesToRead = minOf(safeLength.toLong(), available).toInt()
        if (bytesToRead <= 0) {
            if (knownTotal > 0L && currentPosition >= knownTotal) {
                return END_OF_INPUT
            }
            if (isDownloadingCompleted) {
                return END_OF_INPUT
            }
            return 0
        }

        val readCount = synchronized(rafLock) {
            if (closed.get()) {
                throw IOException("[TDLib MTProto] Stream closed")
            }
            if (randomAccessFile == null && activeFilePath.isNotEmpty() && targetFile.exists()) {
                randomAccessFile = RandomAccessFile(activeFilePath, "r")
            }
            val raf = randomAccessFile
                ?: throw IOException("[TDLib MTProto] Cache file not available on disk: $activeFilePath")
            raf.seek(currentPosition)
            raf.read(buffer, offset, bytesToRead)
        }

        if (readCount > 0) {
            currentPosition += readCount
            if (bytesRemaining != LENGTH_UNSET) {
                bytesRemaining = maxOf(0L, bytesRemaining - readCount)
            }
            return readCount
        } else if (readCount < 0) {
            if (knownTotal > 0L && currentPosition >= knownTotal) {
                return END_OF_INPUT
            }
            if (isDownloadingCompleted) {
                return END_OF_INPUT
            }
            throw IOException(
                "[TDLib MTProto] Unexpected EOF from disk at pos $currentPosition / $knownTotal (avail: $available)",
            )
        }

        if (knownTotal > 0L && currentPosition >= knownTotal) {
            return END_OF_INPUT
        }
        if (isDownloadingCompleted) {
            return END_OF_INPUT
        }
        throw IOException(
            "[TDLib MTProto] Incomplete read at pos $currentPosition / $knownTotal (avail: $available)",
        )
    }

    private fun getKnownTotalSize(): Long = if (effectiveTotalSize > 0L) effectiveTotalSize else totalFileSize

    fun getCurrentPosition(): Long = currentPosition

    fun getTotalFileSize(): Long = getKnownTotalSize()

    fun getBytesRemaining(): Long = bytesRemaining

    fun asInputStream(): InputStream = object : InputStream() {
        private val singleByte = ByteArray(1)

        override fun read(): Int {
            while (true) {
                val n = this@TdlibRandomAccessStream.read(singleByte, 0, 1)
                if (n < 0) return -1
                if (n > 0) return singleByte[0].toInt() and 0xFF
            }
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            return this@TdlibRandomAccessStream.read(b, off, len)
        }

        override fun close() {
            this@TdlibRandomAccessStream.close()
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        Log.d(TAG, "close() fileId=$tdlibFileId")
        tdlibManager.unregisterFileListener(tdlibFileId, fileListener)
        if (tdlibManager.isExplicitFullDownload(tdlibFileId)) {
            tdlibManager.downloadFile(tdlibFileId, offset = 0, limit = 0, priority = 32)
        } else {
            tdlibManager.cancelStreamingDownload(tdlibFileId, 3500L)
        }
        synchronized(rafLock) {
            runCatching { randomAccessFile?.close() }
            randomAccessFile = null
        }
        synchronized(lock) {
            lock.notifyAll()
        }
    }
}
