package dev.roxy.kryptonplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackTrack

enum class PlayerTrackType {
    AUDIO,
    SUBTITLE,
    VIDEO,
}

@Composable
fun rememberTracksState(
    player: PlaybackController,
    trackType: PlayerTrackType,
): TracksState {
    val tracksState = remember(player, trackType) { TracksState(player, trackType) }
    LaunchedEffect(player, trackType) { tracksState.observe() }
    return tracksState
}

@Stable
class TracksState(
    private val player: PlaybackController,
    private val trackType: PlayerTrackType,
) {
    var tracks: List<PlaybackTrack> by mutableStateOf(emptyList())
        private set

    fun switchTrack(index: Int) {
        when (trackType) {
            PlayerTrackType.AUDIO -> player.selectAudioTrack(index)
            PlayerTrackType.SUBTITLE -> player.selectSubtitleTrack(index)
            PlayerTrackType.VIDEO -> Unit
        }
    }

    suspend fun observe() {
        player.state.collect { snapshot ->
            tracks = when (trackType) {
                PlayerTrackType.AUDIO -> snapshot.audioTracks
                PlayerTrackType.SUBTITLE -> snapshot.subtitleTracks
                PlayerTrackType.VIDEO -> snapshot.videoTracks
            }
        }
    }
}
