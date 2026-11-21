package dev.roxy.kryptonplayer.core.ui.liquidglass

import android.graphics.Matrix as AndroidMatrix
import android.graphics.SweepGradient as AndroidSweepGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated moving beam of light (laser orbit) circling around a capsule border.
 * Reusable across MiniPlayer, Floating Navigation Bar, and Selection Bar.
 */
@Composable
fun Modifier.liquidGlassBorderLaser(
    laserColor: Color = Color(0xFFFFD54F), // Amber gold
    coreColor: Color = Color(0xFFFFFBEB),  // Specular apex highlight
    haloColor: Color = Color(0xFFF59E0B),  // Warm atmospheric glow
    speedMillis: Int = 3600,
    strokeWidth: Dp = 1.4.dp,
    glowWidth: Dp = 4.2.dp,
    cornerRadius: Dp? = null,
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "liquidGlassBorderLaser")
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = speedMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "borderLaserAngle",
    )

    return this.drawWithCache {
        val width = size.width
        val height = size.height
        val resolvedCornerRadius = cornerRadius?.toPx() ?: (minOf(width, height) / 2f)
        val strokeWidthPx = strokeWidth.toPx()
        val glowWidthPx = glowWidth.toPx()

        val centerX = width / 2f
        val centerY = height / 2f

        // Beam colors: intense white-gold core, rich gold body, warm amber halo, fading tail
        val shaderColors = intArrayOf(
            android.graphics.Color.TRANSPARENT,
            haloColor.copy(alpha = 0.12f).toArgb(),
            laserColor.copy(alpha = 0.70f).toArgb(),
            coreColor.copy(alpha = 1.0f).toArgb(), // Apex highlight
            laserColor.copy(alpha = 0.85f).toArgb(),
            haloColor.copy(alpha = 0.45f).toArgb(),
            haloColor.copy(alpha = 0.10f).toArgb(),
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT,
        )

        val shaderPositions = floatArrayOf(
            0.00f,
            0.02f,
            0.06f,
            0.09f, // Apex
            0.12f,
            0.17f,
            0.22f,
            0.26f,
            1.00f,
        )

        val sweepShader = AndroidSweepGradient(
            centerX,
            centerY,
            shaderColors,
            shaderPositions,
        )

        val matrix = AndroidMatrix()
        // Equalize coordinate scaling so sweep gradient travels with uniform velocity around elongated shapes
        val scaleX = if (height > width && width > 0f) width / height else 1f
        val scaleY = if (width > height && height > 0f) height / width else 1f
        matrix.setScale(scaleX, scaleY, centerX, centerY)
        matrix.postRotate(animatedAngle, centerX, centerY)
        sweepShader.setLocalMatrix(matrix)

        val beamBrush = ShaderBrush(sweepShader)

        // Specular sheen for curved liquid glass surface
        val glassBgBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.09f),
                Color.White.copy(alpha = 0.02f),
                Color.Black.copy(alpha = 0.28f),
            ),
        )

        // Subtle static delicate frosted rim
        val staticRimBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.12f),
            ),
        )

        val innerRadius = (resolvedCornerRadius - strokeWidthPx / 2f).coerceAtLeast(0f)

        onDrawWithContent {
            // 1. Draw outer ambient bloom halo around capsule where the beam is currently shining
            drawRoundRect(
                brush = beamBrush,
                topLeft = Offset(0f, 0f),
                size = size,
                cornerRadius = CornerRadius(resolvedCornerRadius, resolvedCornerRadius),
                style = Stroke(width = glowWidthPx),
                alpha = 0.38f,
            )

            // 2. Draw content (children: artwork, text, buttons, progress)
            drawContent()

            // 3. Draw specular surface sheen overlay
            drawRoundRect(
                brush = glassBgBrush,
                topLeft = Offset(0f, 0f),
                size = size,
                cornerRadius = CornerRadius(resolvedCornerRadius, resolvedCornerRadius),
                blendMode = BlendMode.SrcOver,
            )

            // 4. Draw static delicate glass border rim
            drawRoundRect(
                brush = staticRimBrush,
                topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                size = Size(width - strokeWidthPx, height - strokeWidthPx),
                cornerRadius = CornerRadius(innerRadius, innerRadius),
                style = Stroke(width = 1.dp.toPx()),
            )

            // 5. Draw the animated travelling light beam right on the border
            drawRoundRect(
                brush = beamBrush,
                topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                size = Size(width - strokeWidthPx, height - strokeWidthPx),
                cornerRadius = CornerRadius(innerRadius, innerRadius),
                style = Stroke(width = strokeWidthPx),
                blendMode = BlendMode.SrcOver,
            )
        }
    }
}
