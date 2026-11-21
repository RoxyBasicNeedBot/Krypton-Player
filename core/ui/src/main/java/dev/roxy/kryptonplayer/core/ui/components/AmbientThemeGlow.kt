package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Height of the ambient top glow layer.
 */
val AmbientGlowHeight: Dp = 360.dp

/**
 * Multiply RGB channels by [factor] while preserving alpha.
 */
fun Color.rgbFactor(factor: Float): Color = Color(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
    alpha = alpha,
)

/**
 * 24-step Hermite polynomial smooth scrim brush S(t) = t^2 * (3 - 2t).
 * Eliminates gradient banding and dirty grey halos over video posters/artwork.
 */
fun smoothScrimBrush(
    color: Color,
    numStops: Int = 24,
    startY: Float = 0f,
    endY: Float = Float.POSITIVE_INFINITY,
): Brush {
    val stops = Array(numStops) { i ->
        val t = i.toFloat() / (numStops - 1)
        val smoothT = t * t * (3f - 2f * t)
        val alpha = (1f - smoothT).coerceIn(0f, 1f)
        Pair(t, color.copy(alpha = alpha))
    }
    return Brush.verticalGradient(
        colorStops = stops,
        startY = startY,
        endY = endY,
    )
}

/**
 * Modifier that applies an angled linear gradient across any composable surface.
 */
fun Modifier.angledGradientBackground(colors: List<Color>, angleDegrees: Float = 25f): Modifier =
    this.drawWithCache {
        val angleRad = (angleDegrees % 360f) * (PI.toFloat() / 180f)
        val width = size.width
        val height = size.height

        val xOffset = cos(angleRad) * width
        val yOffset = sin(angleRad) * height

        val start = Offset((width - xOffset) / 2f, (height - yOffset) / 2f)
        val end = Offset((width + xOffset) / 2f, (height + yOffset) / 2f)

        val brush = Brush.linearGradient(
            colors = colors,
            start = start,
            end = end,
        )

        onDrawBehind {
            drawRect(brush = brush)
        }
    }

/**
 * AmbientThemeGlow: Dynamic top ambient glow layer that extracts/receives a dominant
 * tint (from video thumbnail, channel banner, or TMDb backdrop) and breathes smoothly
 * into the AMOLED pure black background across the top of the screen.
 */
@Composable
fun AmbientThemeGlow(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    height: Dp = AmbientGlowHeight,
    glowHeight: Dp = height,
) {
    val effectiveHeight = if (glowHeight != AmbientGlowHeight && height == AmbientGlowHeight) glowHeight else height
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f


    // Animate glow color transitions smoothly (500ms) on track/channel change
    val glowColor by animateColorAsState(
        targetValue = when {
            tint == null -> backgroundColor
            isLightTheme -> lerp(tint, Color.White, 0.85f)
            else -> tint.rgbFactor(0.55f)
        },
        animationSpec = tween(500),
        label = "ambientGlowColor",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(effectiveHeight)
            .angledGradientBackground(listOf(glowColor, backgroundColor), 25f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(effectiveHeight / 2)
                .align(Alignment.BottomCenter)
                .background(smoothScrimBrush(backgroundColor))
        )
    }
}

