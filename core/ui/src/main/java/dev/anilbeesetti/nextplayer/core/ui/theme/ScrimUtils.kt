package dev.anilbeesetti.nextplayer.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Vertical scrim from [from] to [to] that fades without showing an edge (SimpMusic Smoothstep easing).
 *
 * Uses Hermite interpolation: `t * t * (3f - 2f * t)` across 24 stops.
 * Stops are interpolated directly to avoid Skia un-premultiplied dirty grey bands.
 */
fun smoothScrimBrush(
    from: Color,
    to: Color,
    startFraction: Float = 0f,
    endFraction: Float = 1f,
    startY: Float = 0f,
    endY: Float = Float.POSITIVE_INFINITY,
    steps: Int = 24,
): Brush =
    Brush.verticalGradient(
        colorStops =
            Array(steps + 1) { i ->
                val t = i / steps.toFloat()
                val position = startFraction + (endFraction - startFraction) * t
                position to lerp(from, to, t * t * (3f - 2f * t))
            },
        startY = startY,
        endY = endY,
    )

/**
 * The bottom scrim that melts artwork into the page background by ramping [color]
 * smoothly from invisible to opaque.
 */
fun artworkScrimBrush(
    color: Color,
    steps: Int = 24,
): Brush = smoothScrimBrush(from = color.copy(alpha = 0f), to = color, steps = steps)

/**
 * Computes an adaptive immersive background color from a dominant swatch color.
 * Darkens lighter artwork more so text remains crisp and readable.
 */
fun Color.toImmersiveBackground(): Color {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    val darkenFactor = (0.35f + 0.45f * luminance).coerceIn(0.35f, 0.85f)
    return lerp(this, Color.Black, darkenFactor)
}
