package dev.roxy.kryptonplayer.feature.player.state

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.feature.player.extensions.PlayerVideoSize
import dev.roxy.kryptonplayer.feature.player.extensions.formatted
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberMediaPresentationState(player: PlaybackController): MediaPresentationState {
    val state = remember(player) { MediaPresentationState(player) }
    LaunchedEffect(player) { state.observe() }
    return state
}

@Stable
class MediaPresentationState(
    private val player: PlaybackController,
) {
    var position: Long by mutableLongStateOf(0L)
        private set

    var duration: Long by mutableLongStateOf(0L)
        private set

    var bufferedPosition: Long by mutableLongStateOf(0L)
        private set

    var isPlaying: Boolean by mutableStateOf(false)
        private set

    var isBuffering: Boolean by mutableStateOf(false)
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var isIdle: Boolean by mutableStateOf(true)
        private set

    var currentUri: Uri? by mutableStateOf(null)
        private set

    var videoSize: PlayerVideoSize by mutableStateOf(PlayerVideoSize.UNKNOWN)
        private set

    var playlistSize: Int by mutableIntStateOf(0)
        private set

    suspend fun observe() {
        player.state.collect { snapshot ->
            position = snapshot.currentPositionMs
            duration = snapshot.durationMs
            bufferedPosition = snapshot.bufferedPositionMs
            isPlaying = snapshot.isPlaying
            isBuffering = snapshot.isBuffering
            isLoading = snapshot.isLoading
            isIdle = snapshot.isIdle
            currentUri = snapshot.currentItem?.uri
            videoSize = PlayerVideoSize(snapshot.videoWidth, snapshot.videoHeight)
            playlistSize = snapshot.playlist.size
        }
    }
}

val MediaPresentationState.positionFormatted: String
    get() = position.milliseconds.formatted()

val MediaPresentationState.pendingPositionFormatted: String
    get() = (duration - position).coerceAtLeast(0L).milliseconds.formatted()

val MediaPresentationState.durationFormatted: String
    get() = duration.milliseconds.formatted()
