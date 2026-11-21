package dev.roxy.kryptonplayer.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun SeekIndicatorOverlay(
    currentSeekPos: Long,
    duration: Long,
    seekDeltaSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val isForward = seekDeltaSeconds >= 0
    val accentColor = if (isForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val progressFraction = if (duration > 0L) (currentSeekPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val deltaLabel = if (isForward) "+${seekDeltaSeconds}s" else "${seekDeltaSeconds}s"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier
            .widthIn(min = 200.dp, max = 320.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.40f),
                    ),
                ),
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        // Direction chip + time row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Direction pill chip
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.18f))
                    .border(0.6.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = deltaLabel,
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                    )
                }
            }

            // Current / Total time
            Text(
                text = "${formatOverlayTime(currentSeekPos)}  /  ${formatOverlayTime(duration)}",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Seek progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.15f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.7f),
                                accentColor,
                            ),
                        ),
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

private fun formatOverlayTime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

// Backward-compatible overload
@Composable
fun SeekIndicatorOverlay(
    seekDeltaSeconds: Int,
    modifier: Modifier = Modifier,
) {
    SeekIndicatorOverlay(
        currentSeekPos = 0L,
        duration = 0L,
        seekDeltaSeconds = seekDeltaSeconds,
        modifier = modifier,
    )
}
