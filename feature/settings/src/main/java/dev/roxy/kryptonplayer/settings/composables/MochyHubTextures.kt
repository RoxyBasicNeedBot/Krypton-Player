package dev.roxy.kryptonplayer.settings.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.MochyMotion
import dev.roxy.kryptonplayer.core.ui.components.press3D
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural surface textures rendered cleanly on Canvas (zero bitmap overhead)
 * to give Mochi Hub cards tactile depth on Pure Black AMOLED (#000000).
 */
enum class MochyCardTexture {
    DOT_MATRIX,
    DIAGONAL_GRAIN,
    CONCENTRIC_RADAR,
    AURORA_MESH,
}

/**
 * Staggered spring reveal used across Mochi Hub cards for fluid entrance choreography.
 */
@Composable
fun MochyStaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay((index * 42L).coerceAtMost(320L))
        progress.animateTo(
            targetValue = 1f,
            animationSpec = MochyMotion.SpringSmooth,
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            val t = progress.value
            alpha = t.coerceIn(0f, 1f)
            translationY = (1f - t) * 26.dp.toPx()
            val s = 0.965f + 0.035f * t
            scaleX = s
            scaleY = s
        },
    ) {
        content()
    }
}

/**
 * Signature Mochi Obsidian Glass Card with:
 * - Layered AMOLED obsidian vertical gradient
 * - Procedural micro-texture (dot matrix, diagonal etch, radar rings, or aurora mesh)
 * - Specular top-edge glass bevel border
 * - Optional animated ambient light sweep
 * - Interactive 3D perspective tilt ([press3D]) towards touch position
 */
@Composable
fun MochyGlassCard(
    modifier: Modifier = Modifier,
    texture: MochyCardTexture = MochyCardTexture.DOT_MATRIX,
    cornerRadius: Dp = 24.dp,
    enable3DTilt: Boolean = true,
    maxTilt: Float = 4.2f,
    shimmerSweep: Boolean = false,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val sheenPhase = if (shimmerSweep) {
        val infiniteTransition = rememberInfiniteTransition(label = "mochy_card_sheen")
        infiniteTransition.animateFloat(
            initialValue = -0.6f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "mochy_sheen_phase",
        ).value
    } else {
        -1f
    }

    val borderBoost by animateFloatAsState(
        targetValue = if (isPressed) 1.85f else 1f,
        animationSpec = MochyMotion.SpringSnappy,
        label = "mochy_card_border_boost",
    )

    val topSurface = when {
        isPressed && onClick != null -> Color(0xFF232530)
        elevated -> Color(0xFF191B22)
        else -> Color(0xFF13151B)
    }
    val midSurface = when {
        isPressed && onClick != null -> Color(0xFF1A1C24)
        elevated -> Color(0xFF111318)
        else -> Color(0xFF0D0E13)
    }
    val bottomSurface = if (isPressed && onClick != null) Color(0xFF121319) else Color(0xFF08090C)

    val topBorderAlpha = (0.24f * borderBoost).coerceAtMost(0.52f)
    val bottomBorderAlpha = (0.08f * borderBoost).coerceAtMost(0.24f)

    Box(
        modifier = modifier
            .then(
                if (enable3DTilt && onClick != null) {
                    Modifier.press3D(maxTilt = maxTilt, pressedScale = 0.978f)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(topSurface, midSurface, bottomSurface),
                ),
            )
            .drawBehind {
                // 1. Subtle procedural texture layer
                when (texture) {
                    MochyCardTexture.DOT_MATRIX -> {
                        val spacing = 14.dp.toPx()
                        val dotRadius = 0.85.dp.toPx()
                        var y = spacing * 0.5f
                        while (y < size.height) {
                            var x = spacing * 0.5f
                            val fadeY = (1f - (y / size.height) * 0.55f).coerceIn(0.15f, 1f)
                            while (x < size.width) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.045f * fadeY),
                                    radius = dotRadius,
                                    center = Offset(x, y),
                                )
                                x += spacing
                            }
                            y += spacing
                        }
                    }

                    MochyCardTexture.DIAGONAL_GRAIN -> {
                        val step = 12.dp.toPx()
                        val strokeW = 0.65.dp.toPx()
                        val maxSpan = size.width + size.height
                        var offset = -size.height
                        while (offset < maxSpan) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.032f),
                                start = Offset(offset, 0f),
                                end = Offset(offset + size.height, size.height),
                                strokeWidth = strokeW,
                            )
                            offset += step
                        }
                        // Soft top-right corner glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.07f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.92f, size.height * 0.12f),
                                radius = size.minDimension * 0.95f,
                            ),
                            center = Offset(size.width * 0.92f, size.height * 0.12f),
                            radius = size.minDimension * 0.95f,
                        )
                    }

                    MochyCardTexture.CONCENTRIC_RADAR -> {
                        val center = Offset(size.width * 0.88f, size.height * 0.50f)
                        val baseR = size.minDimension * 0.36f
                        for (ring in 1..4) {
                            val r = baseR * ring * 0.65f
                            drawCircle(
                                color = Color.White.copy(alpha = (0.075f - ring * 0.014f).coerceAtLeast(0.015f)),
                                radius = r,
                                center = center,
                                style = Stroke(width = 0.85.dp.toPx()),
                            )
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.09f),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = size.minDimension * 1.15f,
                            ),
                            center = center,
                            radius = size.minDimension * 1.15f,
                        )
                    }

                    MochyCardTexture.AURORA_MESH -> {
                        // Layered monochrome obsidian nebula bloom + micro-dots
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.11f),
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.50f, size.height * 0.12f),
                                radius = size.maxDimension * 0.72f,
                            ),
                            center = Offset(size.width * 0.50f, size.height * 0.12f),
                            radius = size.maxDimension * 0.72f,
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    colorScheme.primary.copy(alpha = 0.08f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.14f, size.height * 0.86f),
                                radius = size.maxDimension * 0.52f,
                            ),
                            center = Offset(size.width * 0.14f, size.height * 0.86f),
                            radius = size.maxDimension * 0.52f,
                        )
                        // Subtle halftone micro-grain
                        val spacing = 16.dp.toPx()
                        var y = spacing * 0.5f
                        while (y < size.height) {
                            var x = spacing * 0.5f
                            while (x < size.width) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.035f),
                                    radius = 0.75.dp.toPx(),
                                    center = Offset(x, y),
                                )
                                x += spacing
                            }
                            y += spacing
                        }
                    }
                }

                // 2. Top inner glass highlight strip
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.075f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = 28.dp.toPx(),
                    ),
                )

                // 3. Optional slow ambient specular light sweep
                if (shimmerSweep && sheenPhase >= -0.5f) {
                    val bandWidth = size.width * 0.45f
                    val sweepStart = size.width * sheenPhase - bandWidth
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.055f),
                                Color.Transparent,
                            ),
                            start = Offset(sweepStart, 0f),
                            end = Offset(sweepStart + bandWidth, size.height),
                        ),
                    )
                }
            }
            .border(
                width = 0.9.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = topBorderAlpha),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = bottomBorderAlpha),
                    ),
                ),
                shape = shape,
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        content = content,
    )
}

/**
 * Interactive floating Mochi mascot emblem with breathing float, orbital radar ring,
 * and tap-to-bounce spring reaction.
 */
@Composable
fun MochyFloatingMascotStage(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    onEasterEggTap: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var tapCount by remember { mutableIntStateOf(0) }
    val popScale = remember { Animatable(1f) }
    val popRotation = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "mochy_mascot_stage")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascot_float",
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mascot_orbit",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascot_pulse",
    )

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            launch {
                popScale.snapTo(0.84f)
                popScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
            launch {
                val targetSpin = if (tapCount % 2 == 0) 14f else -14f
                popRotation.animateTo(targetSpin, tween(110))
                popRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                tapCount += 1
                onEasterEggTap?.invoke()
            },
        contentAlignment = Alignment.Center,
    ) {
        // Rotating dashed orbital ring & orbiting starlight dot
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.minDimension * 0.47f
            val innerR = this.size.minDimension * 0.36f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = pulseAlpha),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = outerR * 1.15f,
                ),
                center = center,
                radius = outerR * 1.15f,
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = outerR,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), orbitAngle * 0.5f),
                ),
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = innerR,
                center = center,
                style = Stroke(width = 0.75.dp.toPx()),
            )

            val rad = Math.toRadians(orbitAngle.toDouble())
            val satX = center.x + (cos(rad) * outerR).toFloat()
            val satY = center.y + (sin(rad) * outerR).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 2.4.dp.toPx(),
                center = Offset(satX, satY),
            )
        }

        // Core obsidian glass emblem with floating Mochi mascot icon
        Box(
            modifier = Modifier
                .size(size * 0.68f)
                .graphicsLayer {
                    translationY = floatY.dp.toPx()
                    scaleX = popScale.value
                    scaleY = popScale.value
                    rotationZ = popRotation.value
                }
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF22242C),
                            Color(0xFF101116),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.34f),
                            Color.White.copy(alpha = 0.08f),
                        ),
                    ),
                    shape = RoundedCornerShape(20.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                contentDescription = "Krypton Hub Emblem",
                tint = Color.White,
                modifier = Modifier.size(size * 0.44f),
            )
        }
    }
}
