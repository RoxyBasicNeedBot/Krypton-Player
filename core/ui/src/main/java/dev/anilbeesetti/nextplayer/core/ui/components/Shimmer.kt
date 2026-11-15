package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.theme.LocalAppColors

/**
 * Modifier that renders an animated linear gradient light sweep (25° angle) across the composable.
 */
@Composable
fun Modifier.shimmer(
    shimmerColor: Color = LocalAppColors.current.shimmerLine,
    backgroundColor: Color = LocalAppColors.current.shimmerBackground,
    durationMs: Int = 1300,
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    return this.drawWithCache {
        val width = size.width
        val height = size.height

        val brush = Brush.linearGradient(
            colors = listOf(
                backgroundColor,
                shimmerColor.copy(alpha = 0.65f),
                backgroundColor,
            ),
            start = Offset(width * translateAnim, 0f),
            end = Offset(width * (translateAnim + 0.6f), height),
        )

        onDrawBehind {
            drawRect(brush = brush)
        }
    }
}

/**
 * Skeleton loader for Video/Media list items.
 */
@Composable
fun VideoItemSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(110.dp)
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(12.dp))
                .shimmer(),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer(),
            )
        }
    }
}

/**
 * Skeleton loader for TMDb Explore grid cards.
 */
@Composable
fun ExploreGridSkeleton(
    modifier: Modifier = Modifier,
    cardWidth: Dp = 130.dp,
) {
    Column(modifier = modifier.padding(8.dp)) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmer(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            userScrollEnabled = false,
        ) {
            items(5) {
                Column(
                    modifier = Modifier.width(cardWidth),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmer(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmer(),
                    )
                }
            }
        }
    }
}
