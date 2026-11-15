package dev.anilbeesetti.nextplayer.feature.player.extensions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Modifier.noRippleClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    enabled = enabled,
    onClick = onClick,
)

fun Float.roundToTwoDecimals(): Float {
    return (this * 100.0f).let { kotlin.math.round(it) / 100.0f }
}

fun Modifier.repeatingClickable(
    initialDelayMillis: Long = 400,
    delayMillis: Long = 80,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    if (!enabled) return@composed this
    val currentClickListener by rememberUpdatedState(onClick)
    val scope = rememberCoroutineScope()
    val localInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    this.pointerInput(initialDelayMillis, delayMillis, enabled) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var pressed = true

            val press = PressInteraction.Press(down.position)
            scope.launch {
                localInteractionSource.emit(press)
            }

            val job = scope.launch {
                currentClickListener()
                delay(initialDelayMillis)
                while (pressed) {
                    currentClickListener()
                    delay(delayMillis)
                }
            }

            val up = waitForUpOrCancellation()
            pressed = false
            job.cancel()

            val release = if (up != null) {
                PressInteraction.Release(press)
            } else {
                PressInteraction.Cancel(press)
            }
            scope.launch {
                localInteractionSource.emit(release)
            }
        }
    }
}
