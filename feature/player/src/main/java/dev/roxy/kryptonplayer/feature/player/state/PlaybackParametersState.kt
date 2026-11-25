package dev.roxy.kryptonplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController

@Composable
fun rememberPlaybackParametersState(player: PlaybackController): PlaybackParametersState {
    val playbackParametersState = remember(player) { PlaybackParametersState(player) }
    LaunchedEffect(player) { playbackParametersState.observe() }
    return playbackParametersState
}

class PlaybackParametersState(
    private val player: PlaybackController,
) {
    var speed: Float by mutableFloatStateOf(player.state.value.playbackSpeed)
        private set

    var skipSilenceEnabled: Boolean by mutableStateOf(player.state.value.skipSilenceEnabled)
        private set

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun setIsSkipSilenceEnabled(enabled: Boolean) {
        player.setSkipSilenceEnabled(enabled)
    }

    suspend fun observe() {
        player.state.collect { snapshot ->
            speed = snapshot.playbackSpeed
            skipSilenceEnabled = snapshot.skipSilenceEnabled
        }
    }
}
