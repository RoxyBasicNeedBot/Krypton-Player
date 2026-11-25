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
internal fun NextButton(player: PlaybackController, modifier: Modifier = Modifier) {
    val state by player.state.collectAsState()
    val controlsVisibilityState = LocalControlsVisibilityState.current

    PlayerButton(
        modifier = modifier.size(46.dp),
        isEnabled = state.hasNext,
        onClick = {
            player.seekToNext()
            controlsVisibilityState?.showControls()
        },
    ) {
        Icon(
            painter = painterResource(coreUiR.drawable.ic_skip_next),
            contentDescription = stringResource(coreUiR.string.player_controls_next),
            modifier = Modifier.size(26.dp),
        )
    }
}
