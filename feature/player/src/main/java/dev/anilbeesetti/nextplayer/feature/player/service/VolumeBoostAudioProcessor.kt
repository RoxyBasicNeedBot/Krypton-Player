package dev.anilbeesetti.nextplayer.feature.player.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Crash-proof custom software volume amplification processor for Media3 / ExoPlayer.
 *
 * Replaces unreliable Android hardware LoudnessEnhancer (which causes AudioFlinger
 * crashes, AudioTrack dead object errors, and playback termination on volume change).
 *
 * Multiplies 16-bit PCM samples by a configurable volumeFactor with a soft-knee
 * compressor/limiter to eliminate harsh clipping and distortion.
 */
@OptIn(UnstableApi::class)
class VolumeBoostAudioProcessor : BaseAudioProcessor() {

    /**
     * Volume multiplier: 1.0f = 100% (passthrough), up to 3.5f = 350% loudness.
     */
    @Volatile
    var volumeFactor: Float = 1.0f

    @Volatile
    private var isConfigured = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            isConfigured = false
            return AudioProcessor.AudioFormat.NOT_SET
        }
        isConfigured = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return isConfigured
    }

    override fun onReset() {
        isConfigured = false
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val factor = volumeFactor
        if (factor <= 1.001f) {
            // Passthrough without modifying samples
            val outputBuffer = replaceOutputBuffer(remaining)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val outputBuffer = replaceOutputBuffer(remaining)
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            while (inputBuffer.hasRemaining()) {
                val sample = inputBuffer.short
                val amplified = sample * factor

                // Soft-knee limiting past 28000 to smoothly compress peaks without clipping
                val limited = if (amplified > 28000f) {
                    28000f + (amplified - 28000f) * 0.18f
                } else if (amplified < -28000f) {
                    -28000f + (amplified + 28000f) * 0.18f
                } else {
                    amplified
                }

                val clamped = limited.coerceIn(-32767f, 32767f).toInt().toShort()
                outputBuffer.putShort(clamped)
            }
        } else if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            while (inputBuffer.hasRemaining()) {
                val sample = inputBuffer.float
                val amplified = sample * factor

                // Soft-knee limiting past 0.85f for float PCM
                val limited = if (amplified > 0.85f) {
                    0.85f + (amplified - 0.85f) * 0.18f
                } else if (amplified < -0.85f) {
                    -0.85f + (amplified + 0.85f) * 0.18f
                } else {
                    amplified
                }

                val clamped = limited.coerceIn(-1.0f, 1.0f)
                outputBuffer.putFloat(clamped)
            }
        } else {
            outputBuffer.put(inputBuffer)
        }
        outputBuffer.flip()
    }
}
