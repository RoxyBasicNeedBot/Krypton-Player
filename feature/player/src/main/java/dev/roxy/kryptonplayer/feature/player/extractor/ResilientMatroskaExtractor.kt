package dev.roxy.kryptonplayer.feature.player.extractor

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.TrackOutput
import android.util.Log
import androidx.media3.common.ParserException
import java.io.File
import java.io.RandomAccessFile

/**
 * ResilientMatroskaExtractor wraps MatroskaExtractor to provide seamless seeking support
 * for MKV / WebM files that lack Cues (unindexed streams, live captures, web rips, or HDTC downloads)
 * and resilient cluster-level error recovery against corrupted EBML elements (e.g. "Invalid integer size: 9121").
 *
 * In stock MatroskaExtractor, when Cues are absent or not front-loaded, an unseekable SeekMap
 * (SeekMap.Unseekable) is emitted, which causes ExoPlayer to disable seeking entirely (ignoring all seeks).
 * Furthermore, corrupted blocks or invalid integer sizes in EBML headers cause fatal ParserException crashes.
 *
 * ResilientMatroskaExtractor:
 * 1. Emits a seekable ResilientMatroskaSeekMap that maps seek time to proportional byte positions.
 * 2. Extracts duration from the local file's final cluster timecode (0xE7) if the container header omits it.
 * 3. On seek or on malformed EBML corruption (e.g. Invalid integer size), automatically scans forward
 *    to resynchronize ExtractorInput to the nearest Matroska Cluster signature (0x1F43B675), ensuring
 *    clean, crash-free, VLC-style playback through corrupted files.
 */
@OptIn(UnstableApi::class)
class ResilientMatroskaExtractor(
    private val delegate: Extractor,
    private val sourceUri: Uri? = null,
) : Extractor {

    private var extractorOutput: ExtractorOutput? = null
    private var totalFileLength: Long = C.LENGTH_UNSET.toLong()
    private var firstClusterPosition: Long = C.INDEX_UNSET.toLong()
    private var dataDurationUs: Long = C.TIME_UNSET
    private var needClusterResync: Boolean = false
    private var pendingSeekTimeUs: Long = 0L
    private var seekMapSent: Boolean = false
    private var tracksEnded: Boolean = false
    private var consecutiveErrorCount: Int = 0

    override fun sniff(input: ExtractorInput): Boolean {
        return delegate.sniff(input)
    }

    override fun init(output: ExtractorOutput) {
        extractorOutput = output
        delegate.init(object : ExtractorOutput {
            override fun track(id: Int, type: Int): TrackOutput {
                return output.track(id, type)
            }

            override fun endTracks() {
                tracksEnded = true
                output.endTracks()
            }

            override fun seekMap(seekMap: SeekMap) {
                if (seekMap.isSeekable) {
                    seekMapSent = true
                    output.seekMap(seekMap)
                    return
                }

                // If MatroskaExtractor produced SeekMap.Unseekable, intercept and supply a resilient seekable map!
                val duration = seekMap.durationUs
                if (duration != C.TIME_UNSET && duration > 0L) {
                    dataDurationUs = duration
                }
                provideSeekableMap(output)
            }
        })
    }

    private fun provideSeekableMap(output: ExtractorOutput) {
        var duration = dataDurationUs
        if (duration == C.TIME_UNSET || duration <= 0L) {
            val localDuration = tryReadDurationFromLocalFile(sourceUri)
            if (localDuration != C.TIME_UNSET && localDuration > 0L) {
                duration = localDuration
                dataDurationUs = localDuration
            } else if (totalFileLength > 0L) {
                // Estimated duration assuming average bitrate ~2.5 Mbps
                duration = (totalFileLength * 8L * 1_000_000L) / 2_500_000L
                dataDurationUs = duration
            }
        }

        val startPos = if (firstClusterPosition != C.INDEX_UNSET.toLong()) firstClusterPosition else 0L
        val endPos = if (totalFileLength > 0L) totalFileLength else C.LENGTH_UNSET.toLong()

        val resilientSeekMap = ResilientMatroskaSeekMap(
            durationUs = if (duration > 0L) duration else C.TIME_UNSET,
            dataStartPosition = startPos,
            dataEndPosition = endPos,
        )
        seekMapSent = true
        output.seekMap(resilientSeekMap)
    }

    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        if (totalFileLength <= 0L && input.length > 0L) {
            totalFileLength = input.length
            if (seekMapSent && extractorOutput != null) {
                provideSeekableMap(extractorOutput!!)
            }
        }

        if (firstClusterPosition == C.INDEX_UNSET.toLong() && input.position > 0L) {
            firstClusterPosition = input.position
        }

        if (needClusterResync) {
            needClusterResync = false
            if (input.position > 0L) {
                val clusterOffset = findNextClusterOffset(input, minSearchOffset = 0)
                if (clusterOffset > 0) {
                    if (clusterOffset <= 128 * 1024) {
                        try {
                            input.skipFully(clusterOffset)
                            delegate.seek(input.position, pendingSeekTimeUs)
                        } catch (_: Exception) {
                            return Extractor.RESULT_END_OF_INPUT
                        }
                    } else {
                        val targetPos = input.position + clusterOffset
                        seekPosition.position = targetPos
                        delegate.seek(targetPos, pendingSeekTimeUs)
                        return Extractor.RESULT_SEEK
                    }
                }
            }
        }

        try {
            val result = delegate.read(input, seekPosition)
            consecutiveErrorCount = 0
            return result
        } catch (e: Exception) {
            if (e !is ParserException &&
                e !is java.io.EOFException &&
                e.message?.contains("malformed", ignoreCase = true) != true &&
                e.message?.contains("Invalid integer size", ignoreCase = true) != true
            ) {
                throw e
            }

            consecutiveErrorCount++
            Log.w(
                TAG,
                "Caught malformed container exception (${e.message}) at position ${input.position}. Consecutive error count: $consecutiveErrorCount"
            )

            // If we are at or near EOF, cleanly finish playback instead of crashing
            if (input.length > 0L && input.position >= input.length - 2048L) {
                Log.i(TAG, "Near EOF reached after error, ending playback cleanly.")
                return Extractor.RESULT_END_OF_INPUT
            }

            // If too many consecutive recovery attempts failed without finding a playable cluster
            if (consecutiveErrorCount > 30) {
                if (tracksEnded || firstClusterPosition != C.INDEX_UNSET.toLong()) {
                    Log.w(TAG, "Exceeded max recovery retries ($consecutiveErrorCount), ending stream cleanly.")
                    return Extractor.RESULT_END_OF_INPUT
                }
                throw e
            }

            // Search forward for the next valid Cluster (or Tracks element if tracks not yet emitted)
            val clusterOffset = findNextClusterOffset(input, minSearchOffset = 1, maxSearchBytes = 16 * 1024 * 1024)
            if (clusterOffset > 0) {
                val targetPos = input.position + clusterOffset
                Log.i(TAG, "Resynchronizing to next cluster at $targetPos (offset +$clusterOffset)")
                if (clusterOffset <= 128 * 1024) {
                    try {
                        input.skipFully(clusterOffset)
                        delegate.seek(input.position, pendingSeekTimeUs)
                        return Extractor.RESULT_CONTINUE
                    } catch (_: Exception) {
                        return Extractor.RESULT_END_OF_INPUT
                    }
                } else {
                    seekPosition.position = targetPos
                    delegate.seek(targetPos, pendingSeekTimeUs)
                    return Extractor.RESULT_SEEK
                }
            } else {
                // No cluster found in the next 16MB
                if (input.length > 0L && input.position + (16 * 1024 * 1024L) >= input.length) {
                    Log.i(TAG, "No further clusters found before EOF, ending playback cleanly.")
                    return Extractor.RESULT_END_OF_INPUT
                }

                // Advance by 512KB to bypass the corrupted section and continue searching
                val skipBytes = 512 * 1024L
                val targetPos = input.position + skipBytes
                seekPosition.position = targetPos
                delegate.seek(targetPos, pendingSeekTimeUs)
                return Extractor.RESULT_SEEK
            }
        }
    }

    override fun seek(position: Long, timeUs: Long) {
        pendingSeekTimeUs = timeUs
        needClusterResync = true
        delegate.seek(position, timeUs)
    }

    override fun release() {
        delegate.release()
    }

    /**
     * Reads the final clusters from the end of a local file to extract the exact presentation timestamp (0xE7).
     */
    private fun tryReadDurationFromLocalFile(uri: Uri?): Long {
        if (uri == null) return C.TIME_UNSET
        val filePath = when (uri.scheme) {
            "file" -> uri.path
            null -> uri.toString()
            else -> if (uri.path?.startsWith("/storage") == true) uri.path else null
        } ?: return C.TIME_UNSET

        return try {
            val file = File(filePath)
            if (!file.exists() || file.length() < 1024) return C.TIME_UNSET
            val fileLength = file.length()
            val scanLength = minOf(fileLength, 512L * 1024L).toInt()
            val buffer = ByteArray(scanLength)
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(fileLength - scanLength)
                raf.readFully(buffer)
            }

            // Scan backwards for Matroska Cluster ID: 0x1F, 0x43, 0xB6, 0x75
            for (i in scanLength - 16 downTo 0) {
                if (buffer[i] == 0x1F.toByte() &&
                    buffer[i + 1] == 0x43.toByte() &&
                    buffer[i + 2] == 0xB6.toByte() &&
                    buffer[i + 3] == 0x75.toByte()
                ) {
                    val varintStart = i + 4
                    val timecodeMs = parseClusterTimecode(buffer, varintStart, scanLength)
                    if (timecodeMs > 0L) {
                        return timecodeMs * 1000L // convert ms to microseconds
                    }
                }
            }
            C.TIME_UNSET
        } catch (e: Exception) {
            C.TIME_UNSET
        }
    }

    /**
     * Parses the cluster Timecode element (0xE7) located right after the cluster size varint.
     */
    private fun parseClusterTimecode(buffer: ByteArray, varintStart: Int, limit: Int): Long {
        if (varintStart >= limit) return -1L
        val firstByte = buffer[varintStart].toInt() and 0xFF
        if (firstByte == 0) return -1L

        var mask = 0x80
        var varintLength = 1
        while ((firstByte and mask) == 0 && mask > 0) {
            mask = mask shr 1
            varintLength++
        }
        if (varintLength > 8) return -1L

        var idx = varintStart + varintLength
        val maxLookahead = minOf(limit, idx + 32)
        while (idx < maxLookahead - 2) {
            val id = buffer[idx].toInt() and 0xFF
            if (id == 0xE7) {
                val lenByte = buffer[idx + 1].toInt() and 0xFF
                val len = lenByte and 0x7F
                if (len in 1..8 && idx + 2 + len <= limit) {
                    var value = 0L
                    for (k in 0 until len) {
                        value = (value shl 8) or (buffer[idx + 2 + k].toLong() and 0xFF)
                    }
                    return value
                }
            }
            idx++
        }
        return -1L
    }

    /**
     * Scans forward from the current input position up to maxSearchBytes to locate the nearest Cluster header.
     * If minSearchOffset > 0, skips the first minSearchOffset bytes before searching to guarantee forward progress.
     */
    private fun findNextClusterOffset(
        input: ExtractorInput,
        minSearchOffset: Int = 0,
        maxSearchBytes: Int = 16 * 1024 * 1024,
    ): Int {
        input.resetPeekPosition()
        if (minSearchOffset > 0) {
            try {
                if (!input.advancePeekPosition(minSearchOffset, true)) {
                    input.resetPeekPosition()
                    return -1
                }
            } catch (_: Exception) {
                input.resetPeekPosition()
                return -1
            }
        }

        val buffer = ByteArray(32 * 1024)
        var bytesSearched = minSearchOffset

        while (bytesSearched < maxSearchBytes) {
            val bytesToRead = minOf(buffer.size, maxSearchBytes - bytesSearched)
            val peekSuccess = try {
                input.peekFully(buffer, 0, bytesToRead, true)
            } catch (_: Exception) {
                false
            }
            if (!peekSuccess) {
                break
            }

            for (i in 0 until bytesToRead - 4) {
                // Matroska Cluster ID: 0x1F, 0x43, 0xB6, 0x75
                if (buffer[i] == 0x1F.toByte() &&
                    buffer[i + 1] == 0x43.toByte() &&
                    buffer[i + 2] == 0xB6.toByte() &&
                    buffer[i + 3] == 0x75.toByte()
                ) {
                    val varintStart = i + 4
                    if (isValidClusterHeader(buffer, varintStart, bytesToRead)) {
                        input.resetPeekPosition()
                        return bytesSearched + i
                    }
                }

                // Matroska Tracks ID: 0x16, 0x54, 0xAE, 0x6B (if tracks not yet emitted)
                if (!tracksEnded &&
                    buffer[i] == 0x16.toByte() &&
                    buffer[i + 1] == 0x54.toByte() &&
                    buffer[i + 2] == 0xAE.toByte() &&
                    buffer[i + 3] == 0x6B.toByte()
                ) {
                    input.resetPeekPosition()
                    return bytesSearched + i
                }
            }

            val step = maxOf(1, bytesToRead - 3)
            bytesSearched += step
            input.resetPeekPosition()
            val advanceSuccess = try {
                input.advancePeekPosition(bytesSearched, true)
            } catch (_: Exception) {
                false
            }
            if (!advanceSuccess) {
                break
            }
        }
        input.resetPeekPosition()
        return -1
    }

    private fun isValidClusterHeader(buffer: ByteArray, varintStart: Int, limit: Int): Boolean {
        if (varintStart >= limit) return false
        val firstByte = buffer[varintStart].toInt() and 0xFF
        if (firstByte == 0) return false

        var mask = 0x80
        var varintLength = 1
        while ((firstByte and mask) == 0 && mask > 0) {
            mask = mask shr 1
            varintLength++
        }
        if (varintLength > 8) return false

        val childTagIndex = varintStart + varintLength
        if (childTagIndex >= limit) {
            return true
        }

        val childTag = buffer[childTagIndex].toInt() and 0xFF
        return childTag == 0xE7 || childTag == 0xAB || childTag == 0xA3 || childTag == 0xA0 ||
            childTag == 0x58 || childTag == 0x53 || childTag == 0xAF || childTag == 0x2B
    }

    companion object {
        private const val TAG = "ResilientMatroska"
    }
}

/**
 * SeekMap implementation for unindexed Matroska files that maps seek time to proportional byte positions.
 */
@OptIn(UnstableApi::class)
class ResilientMatroskaSeekMap(
    private val durationUs: Long,
    private val dataStartPosition: Long,
    private val dataEndPosition: Long,
) : SeekMap {

    override fun isSeekable(): Boolean = true

    override fun getDurationUs(): Long = if (durationUs > 0L) durationUs else C.TIME_UNSET

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        if (durationUs <= 0L || dataEndPosition <= dataStartPosition) {
            val pos = maxOf(0L, dataStartPosition)
            return SeekMap.SeekPoints(SeekPoint(timeUs, pos))
        }

        val clampedTimeUs = timeUs.coerceIn(0L, durationUs)
        val dataSize = dataEndPosition - dataStartPosition
        val targetByte = dataStartPosition + ((clampedTimeUs.toDouble() / durationUs.toDouble()) * dataSize).toLong()
        val boundedTargetByte = targetByte.coerceIn(dataStartPosition, maxOf(dataStartPosition, dataEndPosition - 1))
        val seekPoint = SeekPoint(clampedTimeUs, boundedTargetByte)
        return SeekMap.SeekPoints(seekPoint)
    }
}
