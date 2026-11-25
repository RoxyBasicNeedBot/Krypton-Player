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
import dev.roxy.kryptonplayer.core.common.playback.PlaybackLoopMode
import dev.roxy.kryptonplayer.core.ui.R as coreUiR
import dev.roxy.kryptonplayer.feature.player.LocalControlsVisibilityState

@Composable
fun LoopButton(player: PlaybackController, modifier: Modifier = Modifier) {
    val state by player.state.collectAsState()
    val controlsVisibilityState = LocalControlsVisibilityState.current

    PlayerButton(
        modifier = modifier,
        isEnabled = true,
        onClick = {
            val nextMode = when (state.repeatMode) {
                PlaybackLoopMode.OFF -> PlaybackLoopMode.ONE
                PlaybackLoopMode.ONE -> PlaybackLoopMode.ALL
                PlaybackLoopMode.ALL -> PlaybackLoopMode.OFF
            }
            player.setRepeatMode(nextMode)
            controlsVisibilityState?.showControls()
        },
    ) {
        Icon(
            painter = repeatModeIconPainter(state.repeatMode),
            contentDescription = repeatModeContentDescription(state.repeatMode),
        )
    }
}

@Composable
private fun repeatModeIconPainter(repeatMode: PlaybackLoopMode): Painter {
    return when (repeatMode) {
        PlaybackLoopMode.OFF -> painterResource(coreUiR.drawable.ic_loop_off)
        PlaybackLoopMode.ONE -> painterResource(coreUiR.drawable.ic_loop_one)
        PlaybackLoopMode.ALL -> painterResource(coreUiR.drawable.ic_loop_all)
    }
}

@Composable
private fun repeatModeContentDescription(repeatMode: PlaybackLoopMode): String {
    return when (repeatMode) {
        PlaybackLoopMode.OFF -> stringResource(coreUiR.string.loop_mode_off)
        PlaybackLoopMode.ONE -> stringResource(coreUiR.string.loop_mode_one)
        PlaybackLoopMode.ALL -> stringResource(coreUiR.string.loop_mode_all)
    }
}
