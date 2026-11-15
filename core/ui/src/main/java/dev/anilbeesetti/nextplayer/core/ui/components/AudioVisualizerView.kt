package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * AudioVisualizerView: 16-bar animated sound visualizer dancing to playback.
 */
@Composable
fun AudioVisualizerView(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 16,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 32.dp,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 3.dp,
) {
    val transition = rememberInfiniteTransition(label = "audioVisualizer")

    val animations = List(barCount) { index ->
        val duration = 300 + (index * 47) % 350
        val target = if (isPlaying) 0.25f + ((index * 37) % 75) / 100f else 0.1f

        val animatedHeight by transition.animateFloat(
            initialValue = 0.1f,
            targetValue = target,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar_$index",
        )
        animatedHeight
    }

    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        animations.forEach { barHeight ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(barHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(color),
            )
        }
    }
}
