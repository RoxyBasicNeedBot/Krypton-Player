package dev.roxy.kryptonplayer.feature.player.buttons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.ui.R as coreUiR
import dev.roxy.kryptonplayer.feature.player.LocalControlsVisibilityState

@Composable
internal fun PreviousButton(player: PlaybackController, modifier: Modifier = Modifier) {
    val state by player.state.collectAsState()
    val controlsVisibilityState = LocalControlsVisibilityState.current

    PlayerButton(
        modifier = modifier.size(46.dp),
        isEnabled = state.hasPrevious,
        onClick = {
            player.seekToPrevious()
            controlsVisibilityState?.showControls()
        },
    ) {
        Icon(
            painter = painterResource(coreUiR.drawable.ic_skip_prev),
            contentDescription = stringResource(coreUiR.string.player_controls_previous),
            modifier = Modifier.size(26.dp),
        )
    }
}
