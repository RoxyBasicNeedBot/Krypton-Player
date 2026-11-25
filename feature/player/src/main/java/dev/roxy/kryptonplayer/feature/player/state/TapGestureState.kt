package dev.roxy.kryptonplayer.feature.player.state

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.model.DoubleTapGesture
import dev.roxy.kryptonplayer.feature.player.ui.SPEED_STEPS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberTapGestureState(
    player: PlaybackController,
    doubleTapGesture: DoubleTapGesture,
    seekIncrementMillis: Long,
    useLongPressGesture: Boolean,
    longPressSpeed: Float,
): TapGestureState {
    val coroutineScope = rememberCoroutineScope()
    return remember(
        player,
        doubleTapGesture,
        seekIncrementMillis,
        useLongPressGesture,
        longPressSpeed,
    ) {
        TapGestureState(
            player = player,
            doubleTapGesture = doubleTapGesture,
            seekIncrementMillis = seekIncrementMillis,
            useLongPressGesture = useLongPressGesture,
            longPressSpeed = longPressSpeed,
            coroutineScope = coroutineScope,
        )
    }
}

@Stable
class TapGestureState(
    private val player: PlaybackController,
    private val seekIncrementMillis: Long,
    private val useLongPressGesture: Boolean = true,
    private val coroutineScope: CoroutineScope,
    val longPressSpeed: Float = 2.0f,
    val doubleTapGesture: DoubleTapGesture,
    val interactionSource: MutableInteractionSource = MutableInteractionSource(),
) {
    var seekMillis by mutableLongStateOf(0L)
    var rippleTick by mutableIntStateOf(0)
    var isLongPressGestureInAction by mutableStateOf(false)
    var currentDynamicSpeed by mutableFloatStateOf(longPressSpeed)

    private var pendingDoubleTapTargetPosition: Long = -1L
    private var resetJob: Job? = null
    private var speedBeforeLongPress: Float = 1.0f
    private var currentSpeedIndex: Int = 4
    private var dragAccumulator: Float = 0f

    fun handleDoubleTap(offset: Offset, size: IntSize) {
        val action = when (doubleTapGesture) {
            DoubleTapGesture.FAST_FORWARD_AND_REWIND -> {
                if (offset.x < size.width / 2f) DoubleTapAction.SEEK_BACKWARD else DoubleTapAction.SEEK_FORWARD
            }
            DoubleTapGesture.BOTH -> {
                val ratio = offset.x / size.width
                when {
                    ratio < 0.42f -> DoubleTapAction.SEEK_BACKWARD
                    ratio > 0.58f -> DoubleTapAction.SEEK_FORWARD
                    else -> DoubleTapAction.PLAY_PAUSE
                }
            }
            DoubleTapGesture.PLAY_PAUSE -> DoubleTapAction.PLAY_PAUSE
            DoubleTapGesture.NONE -> return
        }

        val snap = player.state.value
        when (action) {
            DoubleTapAction.SEEK_BACKWARD -> {
                if (seekMillis >= 0L || pendingDoubleTapTargetPosition < 0L) {
                    pendingDoubleTapTargetPosition = snap.currentPositionMs
                    seekMillis = 0L
                }
                pendingDoubleTapTargetPosition = (pendingDoubleTapTargetPosition - seekIncrementMillis).coerceAtLeast(0L)
                player.seekTo(pendingDoubleTapTargetPosition, isUserSeek = true)
                seekMillis -= seekIncrementMillis
                rippleTick++
                val press = PressInteraction.Press(offset)
                coroutineScope.launch {
                    interactionSource.emit(press)
                    delay(250)
                    interactionSource.emit(PressInteraction.Release(press))
                }
            }
            DoubleTapAction.SEEK_FORWARD -> {
                val dur = if (snap.durationMs > 0L) snap.durationMs else Long.MAX_VALUE
                if (seekMillis <= 0L || pendingDoubleTapTargetPosition < 0L) {
                    pendingDoubleTapTargetPosition = snap.currentPositionMs
                    seekMillis = 0L
                }
                pendingDoubleTapTargetPosition = (pendingDoubleTapTargetPosition + seekIncrementMillis).coerceAtMost(dur)
                player.seekTo(pendingDoubleTapTargetPosition, isUserSeek = true)
                seekMillis += seekIncrementMillis
                rippleTick++
                val press = PressInteraction.Press(offset)
                coroutineScope.launch {
                    interactionSource.emit(press)
                    delay(250)
                    interactionSource.emit(PressInteraction.Release(press))
                }
            }
            DoubleTapAction.PLAY_PAUSE -> player.togglePlayPause()
        }
        resetDoubleTapSeekState()
    }

    fun handleLongPress(offset: Offset) {
        if (!useLongPressGesture) return
        val snap = player.state.value
        if (!snap.isPlaying) {
            player.play()
        }
        isLongPressGestureInAction = true
        speedBeforeLongPress = snap.playbackSpeed

        val closestIndex = SPEED_STEPS.indexOfFirst { it == longPressSpeed }.let { idx ->
            if (idx != -1) idx else {
                SPEED_STEPS.mapIndexed { i, v -> i to abs(v - longPressSpeed) }
                    .minByOrNull { it.second }?.first ?: 4
            }
        }
        currentSpeedIndex = closestIndex
        currentDynamicSpeed = SPEED_STEPS[closestIndex]
        dragAccumulator = 0f
        player.setPlaybackSpeed(currentDynamicSpeed)
    }

    fun triggerButtonSeek(deltaMillis: Long, seekPlayer: Boolean = false) {
        val snap = player.state.value
        if (seekPlayer) {
            val dur = if (snap.durationMs > 0L) snap.durationMs else Long.MAX_VALUE
            if (deltaMillis > 0L) {
                if (seekMillis <= 0L || pendingDoubleTapTargetPosition < 0L) {
                    pendingDoubleTapTargetPosition = snap.currentPositionMs
                    seekMillis = 0L
                }
                pendingDoubleTapTargetPosition = (pendingDoubleTapTargetPosition + deltaMillis).coerceAtMost(dur)
                player.seekTo(pendingDoubleTapTargetPosition, isUserSeek = true)
                seekMillis += deltaMillis
            } else {
                if (seekMillis >= 0L || pendingDoubleTapTargetPosition < 0L) {
                    pendingDoubleTapTargetPosition = snap.currentPositionMs
                    seekMillis = 0L
                }
                pendingDoubleTapTargetPosition = (pendingDoubleTapTargetPosition + deltaMillis).coerceAtLeast(0L)
                player.seekTo(pendingDoubleTapTargetPosition, isUserSeek = true)
                seekMillis += deltaMillis
            }
        } else {
            if (deltaMillis > 0L) {
                if (seekMillis < 0L) seekMillis = 0L
                seekMillis += deltaMillis
            } else {
                if (seekMillis > 0L) seekMillis = 0L
                seekMillis += deltaMillis
            }
        }
        rippleTick++
        resetDoubleTapSeekState()
    }

    fun onHorizontalDrag(dragAmount: Float, context: Context?) {
        if (!isLongPressGestureInAction) return
        dragAccumulator += dragAmount
        val density = context?.resources?.displayMetrics?.density ?: 2.5f
        val stepThresholdPx = 48f * density

        while (abs(dragAccumulator) >= stepThresholdPx) {
            val stepChange = if (dragAccumulator > 0f) 1 else -1
            val targetIndex = (currentSpeedIndex + stepChange).coerceIn(0, SPEED_STEPS.lastIndex)
            if (targetIndex == currentSpeedIndex) {
                dragAccumulator = 0f
                break
            }
            currentSpeedIndex = targetIndex
            currentDynamicSpeed = SPEED_STEPS[currentSpeedIndex]
            player.setPlaybackSpeed(currentDynamicSpeed)
            triggerHaptic(context)
            dragAccumulator -= stepChange * stepThresholdPx
        }
    }

    fun handleOnLongPressRelease() {
        if (isLongPressGestureInAction) {
            isLongPressGestureInAction = false
            player.setPlaybackSpeed(speedBeforeLongPress)
            dragAccumulator = 0f
        }
    }

    private fun triggerHaptic(context: Context?) {
        if (context == null) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30L)
            }
        } catch (_: Exception) {
        }
    }

    private fun resetDoubleTapSeekState() {
        resetJob?.cancel()
        resetJob = coroutineScope.launch {
            delay(750.milliseconds)
            seekMillis = 0L
            pendingDoubleTapTargetPosition = -1L
        }
    }
}

enum class DoubleTapAction {
    SEEK_BACKWARD,
    SEEK_FORWARD,
    PLAY_PAUSE,
}
