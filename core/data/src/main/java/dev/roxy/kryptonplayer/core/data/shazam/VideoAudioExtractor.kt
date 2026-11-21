package dev.roxy.kryptonplayer.core.data.shazam

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoAudioExtractor @Inject constructor() {
    companion object {
        private const val TAG = "VideoAudioExtractor"
        private const val TARGET_SAMPLE_RATE = 16000
        private const val TARGET_DURATION_SECONDS = 4.5
    }

    /**
     * Extracts 4.5 seconds of decoded PCM 16-bit Mono (16kHz) audio directly from a video URI starting from startPositionMs.
     */
    suspend fun extractPcmFromVideo(
        context: Context,
        videoUri: Uri,
        startPositionMs: Long
    ): ShortArray? = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, videoUri, null)

            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || inputFormat == null) {
                Log.w(TAG, "No audio track found in video")
                return@withContext null
            }

            extractor.selectTrack(audioTrackIndex)
            val seekUs = (startPositionMs.coerceAtLeast(0) * 1000L)
            extractor.seekTo(seekUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext null
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val srcSampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            val srcChannelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val targetSamplesNeeded = (TARGET_SAMPLE_RATE * TARGET_DURATION_SECONDS).toInt()
            val rawPcmSamples = mutableListOf<Short>()

            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false
            val timeoutUs = 10000L

            val startTimeMs = System.currentTimeMillis()
            val maxExtractionTimeMs = 5000L // 5s timeout

            val maxRawSamplesToRead = (targetSamplesNeeded * (srcSampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()) * srcChannelCount * 1.5).toInt()

            while (!isEos && rawPcmSamples.size < maxRawSamplesToRead) {
                if (System.currentTimeMillis() - startTimeMs > maxExtractionTimeMs) {
                    break
                }

                // Feed input to decoder
                val inIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    val inBuffer = codec.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        val sampleSize = extractor.readSampleData(inBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEos = true
                        } else {
                            val sampleTime = extractor.sampleTime
                            codec.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // Retrieve output from decoder
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outBuffer.order(ByteOrder.LITTLE_ENDIAN)

                        while (outBuffer.remaining() >= 2) {
                            rawPcmSamples.add(outBuffer.short)
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                }
            }

            if (rawPcmSamples.isEmpty()) {
                return@withContext null
            }

            // Downmix multi-channel to mono & resample to 16000Hz
            return@withContext downmixAndResample(
                rawSamples = rawPcmSamples.toShortArray(),
                srcSampleRate = srcSampleRate,
                srcChannels = srcChannelCount,
                targetSampleRate = TARGET_SAMPLE_RATE,
                maxOutputSamples = targetSamplesNeeded
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed extracting audio from video", e)
            null
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Exception) {}
            try {
                extractor?.release()
            } catch (_: Exception) {}
        }
    }

    private fun downmixAndResample(
        rawSamples: ShortArray,
        srcSampleRate: Int,
        srcChannels: Int,
        targetSampleRate: Int,
        maxOutputSamples: Int
    ): ShortArray {
        val channels = srcChannels.coerceAtLeast(1)
        val monoLength = rawSamples.size / channels
        val mono = ShortArray(monoLength)

        for (i in 0 until monoLength) {
            var sum = 0
            for (ch in 0 until channels) {
                sum += rawSamples[i * channels + ch]
            }
            mono[i] = (sum / channels).toShort()
        }

        if (srcSampleRate == targetSampleRate) {
            return if (mono.size > maxOutputSamples) mono.copyOf(maxOutputSamples) else mono
        }

        val ratio = srcSampleRate.toDouble() / targetSampleRate.toDouble()
        val outLength = ((mono.size / ratio).toInt()).coerceAtMost(maxOutputSamples)
        val resampled = ShortArray(outLength)

        for (i in 0 until outLength) {
            val srcIdx = (i * ratio).toInt().coerceIn(0, mono.size - 1)
            resampled[i] = mono[srcIdx]
        }

        return resampled
    }
}
