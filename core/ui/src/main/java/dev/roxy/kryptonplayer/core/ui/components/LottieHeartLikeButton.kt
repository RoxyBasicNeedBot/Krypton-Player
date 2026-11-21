package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LottieHeartLikeButton(
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    activeColor: Color = Color(0xFFFF2D55),
    inactiveColor: Color = Color.White
) {
    val coroutineScope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1.0f) }
    val burstAnim = remember { Animatable(0f) }

    val burstColors = remember {
        listOf(
            Color(0xFFFF2D55),
            Color(0xFFFF9500),
            Color(0xFFFFCC00),
            Color(0xFFAF52DE),
            Color(0xFFFF2D55),
            Color(0xFF5856D6),
            Color(0xFFFF3B30),
            Color(0xFFFF9500)
        )
    }

    Box(
        modifier = modifier.size(size * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        // Sparkle Burst Particles (Native Canvas - Zero Surface Punch / Zero Black Screen)
        if (burstAnim.value > 0.01f && burstAnim.value < 0.99f) {
            val progress = burstAnim.value
            val maxDistance = with(androidx.compose.ui.platform.LocalDensity.current) { (size * 0.9f).toPx() }
            val currentDistance = maxDistance * progress
            val particleAlpha = (1f - progress).coerceIn(0f, 1f)
            val dotRadius = with(androidx.compose.ui.platform.LocalDensity.current) { (2.5.dp * (1f - progress * 0.5f)).toPx() }

            Canvas(modifier = Modifier.size(size * 1.5f)) {
                val center = Offset(size.toPx() * 0.75f, size.toPx() * 0.75f)
                val count = burstColors.size
                for (i in 0 until count) {
                    val angle = (i * (360.0 / count) * (Math.PI / 180.0)).toFloat()
                    val x = center.x + cos(angle) * currentDistance
                    val y = center.y + sin(angle) * currentDistance
                    drawCircle(
                        color = burstColors[i].copy(alpha = particleAlpha),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Heart Icon with Bouncy Spring Scale
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        val willBeLiked = !isLiked
                        coroutineScope.launch {
                            if (willBeLiked) {
                                launch {
                                    scaleAnim.snapTo(0.6f)
                                    scaleAnim.animateTo(
                                        targetValue = 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                                launch {
                                    burstAnim.snapTo(0f)
                                    burstAnim.animateTo(
                                        targetValue = 1.0f,
                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                    )
                                    burstAnim.snapTo(0f)
                                }
                            } else {
                                scaleAnim.snapTo(0.85f)
                                scaleAnim.animateTo(
                                    targetValue = 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                        onToggleLike()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) activeColor else inactiveColor,
                modifier = Modifier.size(size)
            )
        }
    }
}
