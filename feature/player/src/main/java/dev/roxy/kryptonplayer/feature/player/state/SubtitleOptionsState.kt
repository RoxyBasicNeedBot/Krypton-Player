package dev.roxy.kryptonplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackMediaItem

@Composable
fun rememberSubtitleOptionsState(
    player: PlaybackController,
    onEvent: (SubtitleOptionsEvent) -> Unit = {},
): SubtitleOptionsState {
    val state = remember(player) { SubtitleOptionsState(player, onEvent) }
    LaunchedEffect(player) { state.observe() }
    return state
}

@Stable
class SubtitleOptionsState(
    private val player: PlaybackController,
    private val onEvent: (SubtitleOptionsEvent) -> Unit,
) {
    var delayMilliseconds: Long by mutableLongStateOf(0L)
        private set

    var speedMultiplier: Float by mutableFloatStateOf(1f)
        private set

    fun setDelay(delayMs: Long) {
        delayMilliseconds = delayMs
        player.setSubtitleDelayMs(delayMs)
        player.state.value.currentItem?.let { item ->
            onEvent(SubtitleOptionsEvent.DelayChanged(item, delayMs))
        }
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        speedMultiplier = clamped
        player.setSubtitleSpeed(clamped)
        player.state.value.currentItem?.let { item ->
            onEvent(SubtitleOptionsEvent.SpeedChanged(item, clamped))
        }
    }

    suspend fun observe() {
        player.state.collect { snapshot ->
            delayMilliseconds = snapshot.subtitleDelayMs
            speedMultiplier = snapshot.subtitleSpeed
        }
    }
}

sealed interface SubtitleOptionsEvent {
    data class DelayChanged(val mediaItem: PlaybackMediaItem, val delay: Long) : SubtitleOptionsEvent
    data class SpeedChanged(val mediaItem: PlaybackMediaItem, val speed: Float) : SubtitleOptionsEvent
}
