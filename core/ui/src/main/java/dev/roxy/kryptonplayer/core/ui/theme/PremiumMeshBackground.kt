package dev.roxy.kryptonplayer.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex

@Composable
fun PremiumMeshBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = LocalMeshBackgroundEnabled.current,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    if (!enabled) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(surfaceColor)
        ) {
            content()
        }
        return
    }

    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .align(Alignment.TopCenter)
                .zIndex(-1f)
                .drawWithCache {
                    val width = this.size.width
                    val height = this.size.height

                    // Five color blobs matching ArchiveTune's exact mesh gradient
                    val brush1 = Brush.radialGradient(
                        colors = listOf(
                            color1.copy(alpha = 0.38f),
                            color1.copy(alpha = 0.24f),
                            color1.copy(alpha = 0.14f),
                            color1.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.1f),
                        radius = width * 0.55f
                    )

                    val brush2 = Brush.radialGradient(
                        colors = listOf(
                            color2.copy(alpha = 0.34f),
                            color2.copy(alpha = 0.2f),
                            color2.copy(alpha = 0.11f),
                            color2.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.2f),
                        radius = width * 0.65f
                    )

                    val brush3 = Brush.radialGradient(
                        colors = listOf(
                            color3.copy(alpha = 0.3f),
                            color3.copy(alpha = 0.17f),
                            color3.copy(alpha = 0.09f),
                            color3.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.3f, height * 0.45f),
                        radius = width * 0.6f
                    )

                    val brush4 = Brush.radialGradient(
                        colors = listOf(
                            color4.copy(alpha = 0.26f),
                            color4.copy(alpha = 0.14f),
                            color4.copy(alpha = 0.08f),
                            color4.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.7f, height * 0.5f),
                        radius = width * 0.7f
                    )

                    val brush5 = Brush.radialGradient(
                        colors = listOf(
                            color5.copy(alpha = 0.22f),
                            color5.copy(alpha = 0.12f),
                            color5.copy(alpha = 0.06f),
                            color5.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.75f),
                        radius = width * 0.8f
                    )

                    // Vertical overlay brush to fade smoothly into surface color
                    val overlayBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            surfaceColor.copy(alpha = 0.22f),
                            surfaceColor.copy(alpha = 0.55f),
                            surfaceColor
                        ),
                        startY = height * 0.4f,
                        endY = height
                    )

                    onDrawBehind {
                        drawRect(brush = brush1)
                        drawRect(brush = brush2)
                        drawRect(brush = brush3)
                        drawRect(brush = brush4)
                        drawRect(brush = brush5)
                        drawRect(brush = overlayBrush)
                    }
                }
        )
        content()
    }
}
