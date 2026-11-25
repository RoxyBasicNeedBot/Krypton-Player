package dev.roxy.kryptonplayer.feature.player.buttons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.ui.R as coreUiR
import dev.roxy.kryptonplayer.feature.player.LocalControlsVisibilityState

@Composable
fun ShuffleButton(player: PlaybackController, modifier: Modifier = Modifier) {
    val state by player.state.collectAsState()
    val controlsVisibilityState = LocalControlsVisibilityState.current

    PlayerButton(
        modifier = modifier,
        isEnabled = state.playlist.size > 1,
        onClick = {
            player.setShuffleEnabled(!state.shuffleEnabled)
            controlsVisibilityState?.showControls()
        },
    ) {
        Icon(
            painter = shuffleModeIconPainter(state.shuffleEnabled),
            contentDescription = shuffleContentDescription(state.shuffleEnabled),
        )
    }
}

@Composable
private fun shuffleModeIconPainter(shuffleOn: Boolean): Painter {
    return if (shuffleOn) painterResource(coreUiR.drawable.ic_shuffle_on) else painterResource(coreUiR.drawable.ic_shuffle)
}

@Composable
private fun shuffleContentDescription(shuffleOn: Boolean): String {
    return if (shuffleOn) stringResource(coreUiR.string.shuffle_on) else stringResource(coreUiR.string.shuffle_off)
}
