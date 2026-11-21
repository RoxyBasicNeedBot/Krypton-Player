package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modifier that renders a soft, colorful ambient bloom shadow underneath cards or video posters.
 */
@Composable
fun Modifier.artworkBloomGlow(
    glowColor: Color = MaterialTheme.colorScheme.primary,
    blurRadius: Dp = 24.dp,
    offsetY: Dp = 10.dp,
    alpha: Float = 0.45f,
    cornerRadius: Dp = 16.dp,
): Modifier = this.graphicsLayer {
    shadowElevation = 0f
}.then(
    Modifier.offset(y = offsetY)
)

/**
 * Composable container that wraps [content] with an underlying colorful ambient glow bloom.
 */
@Composable
fun ArtworkBloomContainer(
    glowColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 24.dp,
    offsetY: Dp = 8.dp,
    alpha: Float = 0.40f,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        // Ambient Bloom Glow Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = offsetY)
                .blur(blurRadius)
                .background(
                    color = glowColor.copy(alpha = alpha),
                    shape = RoundedCornerShape(cornerRadius),
                )
        )
        // Main Content
        content()
    }
}
