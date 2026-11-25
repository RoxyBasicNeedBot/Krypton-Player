package dev.roxy.kryptonplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackFailure

@Composable
fun rememberErrorState(player: PlaybackController): ErrorState {
    val errorState = remember(player) { ErrorState(player) }
    LaunchedEffect(player) { errorState.observe() }
    return errorState
}

class ErrorState(
    private val player: PlaybackController,
) {
    var error: PlaybackFailure? by mutableStateOf(null)
        private set

    fun dismiss() {
        error = null
    }

    suspend fun observe() {
        player.state.collect { snapshot ->
            error = snapshot.error
        }
    }
}
