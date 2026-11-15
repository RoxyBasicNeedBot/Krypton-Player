package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modifier that applies basicMarquee with a smooth gradient fade on the left and right edges.
 */
@Composable
fun Modifier.fadingMarquee(
    edgeFadeWidth: Dp = 16.dp,
    iterations: Int = Int.MAX_VALUE,
    animationMode: MarqueeAnimationMode = MarqueeAnimationMode.Immediately,
): Modifier {
    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()

            val fadePx = edgeFadeWidth.toPx()
            val width = size.width

            if (width > fadePx * 2) {
                // Left Fade
                val leftFadeBrush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = fadePx,
                )
                drawRect(
                    brush = leftFadeBrush,
                    size = androidx.compose.ui.geometry.Size(fadePx, size.height),
                    blendMode = BlendMode.DstIn,
                )

                // Right Fade
                val rightFadeBrush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = width - fadePx,
                    endX = width,
                )
                drawRect(
                    brush = rightFadeBrush,
                    topLeft = androidx.compose.ui.geometry.Offset(width - fadePx, 0f),
                    size = androidx.compose.ui.geometry.Size(fadePx, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
        .basicMarquee(
            iterations = iterations,
            animationMode = animationMode,
        )
}
