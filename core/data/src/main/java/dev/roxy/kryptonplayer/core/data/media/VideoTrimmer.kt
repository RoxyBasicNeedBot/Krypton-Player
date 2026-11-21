package dev.roxy.kryptonplayer.core.data.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VideoTrimmer {
    private const val TAG = "VideoTrimmer"
    private const val DEFAULT_BUFFER_SIZE = 1024 * 1024 // 1MB buffer

    suspend fun trimVideoLossless(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, sourceUri, null)
            } catch (e: Exception) {
                extractor.setDataSource(sourceUri.toString())
            }

            val trackCount = extractor.trackCount
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.cacheDir, "ArchClips").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(outputDir, "ArchStatus_${timeStamp}.mp4")

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val indexMap = HashMap<Int, Int>(trackCount)
            val buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val muxerTrackIndex = muxer.addTrack(format)
                    indexMap[i] = muxerTrackIndex
                }
            }

            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.presentationTimeUs = (sampleTime - startUs).coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlags
                bufferInfo.offset = 0

                val muxerTrack = indexMap[trackIndex]
                if (muxerTrack != null) {
                    muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                }

                extractor.advance()
            }

            try {
                muxer.stop()
                muxer.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping muxer: ${e.message}")
            }
            extractor.release()

            outputFile
        }
    }
}
