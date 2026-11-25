package dev.roxy.kryptonplayer.feature.player.state

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackEvent
import dev.roxy.kryptonplayer.feature.player.extensions.toggleSystemBars
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

@Composable
fun rememberControlsVisibilityState(
    player: PlaybackController,
    hideAfter: Duration,
): ControlsVisibilityState {
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()
    val controlsVisibilityState = remember(player, hideAfter) {
        ControlsVisibilityState(player, hideAfter, coroutineScope)
    }
    LaunchedEffect(player) { controlsVisibilityState.observe() }
    LaunchedEffect(controlsVisibilityState.controlsVisible, controlsVisibilityState.controlsLocked) {
        if (controlsVisibilityState.controlsLocked) {
            activity?.toggleSystemBars(showBars = false)
            return@LaunchedEffect
        }
        activity?.toggleSystemBars(showBars = controlsVisibilityState.controlsVisible)
    }
    return controlsVisibilityState
}

@Stable
class ControlsVisibilityState(
    private val player: PlaybackController,
    private val hideAfter: Duration,
    private val scope: CoroutineScope,
) {
    private var autoHideControlsJob: Job? = null

    var controlsVisible: Boolean by mutableStateOf(true)
        private set

    var controlsLocked: Boolean by mutableStateOf(false)
        private set

    fun showControls(duration: Duration = hideAfter) {
        controlsVisible = true
        autoHideControls(duration)
    }

    fun hideControls() {
        autoHideControlsJob?.cancel()
        controlsVisible = false
    }

    fun toggleControlsVisibility() {
        if (controlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    fun lockControls() {
        controlsLocked = true
    }

    fun unlockControls() {
        controlsLocked = false
        showControls()
    }

    suspend fun observe() {
        if (player.state.value.isPlaying) {
            autoHideControls()
        }
        player.events.collect { event ->
            if (event is PlaybackEvent.IsPlayingChanged && event.isPlaying) {
                autoHideControls()
            }
        }
    }

    private fun autoHideControls(duration: Duration = hideAfter) {
        autoHideControlsJob?.cancel()
        autoHideControlsJob = scope.launch {
            delay(duration)
            if (player.state.value.isPlaying) {
                controlsVisible = false
            }
        }
    }
}
