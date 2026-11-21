package dev.roxy.kryptonplayer.feature.player.buttons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import dev.roxy.kryptonplayer.core.ui.R as coreUiR
import dev.roxy.kryptonplayer.feature.player.LocalControlsVisibilityState

@OptIn(UnstableApi::class)
@Composable
internal fun PreviousButton(player: Player, modifier: Modifier = Modifier) {
    val state = rememberPreviousButtonState(player)
    val controlsVisibilityState = LocalControlsVisibilityState.current

    PlayerButton(
        modifier = modifier.size(46.dp),
        isEnabled = state.isEnabled,
        onClick = {
            state.onClick()
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
