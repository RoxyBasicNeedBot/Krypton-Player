package dev.roxy.kryptonplayer.feature.player.buttons

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.feature.player.LocalControlsVisibilityState

@Composable
fun SeekBackButton(
    player: PlaybackController,
    modifier: Modifier = Modifier,
    seekDurationSeconds: Int = 10,
    onSeekAnimated: ((Long) -> Unit)? = null,
) {
    val controlsVisibilityState = LocalControlsVisibilityState.current
    val icon = when (seekDurationSeconds) {
        5 -> Icons.Rounded.Replay5
        30 -> Icons.Rounded.Replay30
        else -> Icons.Rounded.Replay10
    }

    var pendingTargetPosition by remember { mutableLongStateOf(-1L) }
    var lastClickTimestamp by remember { mutableLongStateOf(0L) }

    PlayerButton(
        modifier = modifier.size(50.dp),
        onClick = {
            val now = System.currentTimeMillis()
            val deltaMs = -seekDurationSeconds * 1000L
            val basePos = if (now - lastClickTimestamp < 1000L && pendingTargetPosition >= 0L) {
                pendingTargetPosition
            } else {
                player.state.value.currentPositionMs
            }
            val target = (basePos + deltaMs).coerceAtLeast(0L)
            pendingTargetPosition = target
            lastClickTimestamp = now
            player.seekTo(target, isUserSeek = true)
            onSeekAnimated?.invoke(deltaMs)
            controlsVisibilityState?.showControls()
        },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Rewind ${seekDurationSeconds}s",
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
fun SeekForwardButton(
    player: PlaybackController,
    modifier: Modifier = Modifier,
    seekDurationSeconds: Int = 10,
    onSeekAnimated: ((Long) -> Unit)? = null,
) {
    val controlsVisibilityState = LocalControlsVisibilityState.current
    val icon = when (seekDurationSeconds) {
        5 -> Icons.Rounded.Forward5
        30 -> Icons.Rounded.Forward30
        else -> Icons.Rounded.Forward10
    }

    var pendingTargetPosition by remember { mutableLongStateOf(-1L) }
    var lastClickTimestamp by remember { mutableLongStateOf(0L) }

    PlayerButton(
        modifier = modifier.size(50.dp),
        onClick = {
            val now = System.currentTimeMillis()
            val deltaMs = seekDurationSeconds * 1000L
            val snapshot = player.state.value
            val maxPos = if (snapshot.durationMs > 0L) snapshot.durationMs else Long.MAX_VALUE
            val basePos = if (now - lastClickTimestamp < 1000L && pendingTargetPosition >= 0L) {
                pendingTargetPosition
            } else {
                snapshot.currentPositionMs
            }
            val target = (basePos + deltaMs).coerceAtMost(maxPos)
            pendingTargetPosition = target
            lastClickTimestamp = now
            player.seekTo(target, isUserSeek = true)
            onSeekAnimated?.invoke(deltaMs)
            controlsVisibilityState?.showControls()
        },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Fast forward ${seekDurationSeconds}s",
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}
