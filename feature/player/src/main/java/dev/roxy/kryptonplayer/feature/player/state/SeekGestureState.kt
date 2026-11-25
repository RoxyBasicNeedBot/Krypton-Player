package dev.roxy.kryptonplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.feature.player.extensions.formatted
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberSeekGestureState(
    player: PlaybackController,
    sensitivity: Float = 0.5f,
    enableSeekGesture: Boolean,
): SeekGestureState {
    return remember(player, sensitivity, enableSeekGesture) {
        SeekGestureState(
            player = player,
            sensitivity = sensitivity,
            enableSeekGesture = enableSeekGesture,
        )
    }
}

@Stable
class SeekGestureState(
    private val player: PlaybackController,
    private val enableSeekGesture: Boolean = true,
    private val sensitivity: Float = 0.5f,
) {
    var isSeeking: Boolean by mutableStateOf(false)
        private set

    var seekStartPosition: Long? by mutableStateOf(null)
        private set

    var seekAmount: Long? by mutableStateOf(null)
        private set

    private var seekStartX = 0f

    fun onSeek(value: Long) {
        val snap = player.state.value
        if (!isSeeking) {
            isSeeking = true
            seekStartPosition = snap.currentPositionMs
            player.setScrubbingMode(true)
        }
        val startPos = seekStartPosition ?: snap.currentPositionMs
        val dur = if (snap.durationMs > 0L) snap.durationMs else Long.MAX_VALUE
        seekAmount = (value - startPos).coerceIn(
            minimumValue = 0L - startPos,
            maximumValue = dur - startPos,
        )
        player.seekTo(value.coerceIn(0L, dur), isUserSeek = true)
    }

    fun onSeekEnd() {
        reset()
    }

    fun onDragStart(offset: Offset) {
        if (!enableSeekGesture) return
        val snap = player.state.value
        if (snap.durationMs <= 0L && snap.currentPositionMs <= 0L) return

        isSeeking = true
        seekStartX = offset.x
        seekStartPosition = snap.currentPositionMs
        player.setScrubbingMode(true)
    }

    fun onDrag(change: PointerInputChange, dragAmount: Float) {
        val startPos = seekStartPosition ?: return
        val snap = player.state.value
        if (snap.currentPositionMs <= 0L && dragAmount < 0) return
        val dur = if (snap.durationMs > 0L) snap.durationMs else Long.MAX_VALUE
        if (snap.currentPositionMs >= dur && dragAmount > 0) return

        val newPosition = startPos + ((change.position.x - seekStartX) * (sensitivity * 100)).toInt()
        seekAmount = (newPosition - startPos).coerceIn(
            minimumValue = 0L - startPos,
            maximumValue = dur - startPos,
        )
        player.seekTo(newPosition.coerceIn(0L, dur), isUserSeek = true)
    }

    fun onDragEnd() {
        reset()
    }

    private fun reset() {
        player.setScrubbingMode(false)
        isSeeking = false
        seekStartPosition = null
        seekAmount = null
        seekStartX = 0f
    }
}

val SeekGestureState.seekAmountFormatted: String
    get() {
        val seekAmount = seekAmount ?: return ""
        val sign = if (seekAmount < 0) "-" else "+"
        return sign + abs(seekAmount).milliseconds.formatted()
    }

val SeekGestureState.seekToPositionFormated: String
    get() {
        val position = seekStartPosition ?: return ""
        val seekAmount = seekAmount ?: return ""
        return (position + seekAmount).milliseconds.formatted()
    }
