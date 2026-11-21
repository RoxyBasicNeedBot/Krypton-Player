package dev.roxy.kryptonplayer.feature.player.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

val SPEED_STEPS = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f)

@Composable
fun SpeedSliderHUD(
    playbackSpeed: Float,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val activeIndex = remember(playbackSpeed) {
        val exactIndex = SPEED_STEPS.indexOfFirst { it == playbackSpeed }
        if (exactIndex != -1) {
            exactIndex
        } else {
            SPEED_STEPS.mapIndexed { idx, v -> idx to abs(v - playbackSpeed) }
                .minByOrNull { it.second }?.first ?: 4
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "speedPulse")
    val c1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 0, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "c1",
    )
    val c2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 140, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "c2",
    )
    val c3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "c3",
    )

    Box(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 400.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                val density = LocalDensity.current
                val startPadding = 20.dp
                val endPadding = 20.dp
                val startPaddingPx = with(density) { startPadding.toPx() }
                val endPaddingPx = with(density) { endPadding.toPx() }
                val widthPx = constraints.maxWidth.toFloat()
                val trackWidthPx = widthPx - startPaddingPx - endPaddingPx

                // 1. Draw track line behind ticks
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                ) {
                    val centerY = size.height / 2f
                    // Draw inactive track (entire line)
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(startPaddingPx, centerY),
                        end = Offset(widthPx - endPaddingPx, centerY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )

                    // Draw active track (from left to active index)
                    val activeThumbX = startPaddingPx + trackWidthPx * (activeIndex.toFloat() / 8f)
                    drawLine(
                        color = activeColor,
                        start = Offset(startPaddingPx, centerY),
                        end = Offset(activeThumbX, centerY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                // 2. Draw labels and tick dots
                SPEED_STEPS.forEachIndexed { index, step ->
                    val fraction = index.toFloat() / 8f
                    val stepXPx = startPaddingPx + trackWidthPx * fraction
                    val stepXDp = with(density) { stepXPx.toDp() }
                    val isActive = index == activeIndex

                    // Label above the track
                    Box(
                        modifier = Modifier
                            .absoluteOffset(x = stepXDp - 20.dp, y = 2.dp)
                            .width(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2f", step).removeSuffix("0").removeSuffix(".0") + "x",
                            color = if (isActive) activeColor else Color.White.copy(alpha = 0.6f),
                            fontSize = if (isActive) 11.sp else 9.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }

                    // Tick dot or Active Thumb
                    val dotColor = if (isActive) activeColor else Color.White
                    val dotRadius = if (isActive) 5.dp else 2.5.dp
                    Box(
                        modifier = Modifier
                            .absoluteOffset(x = stepXDp - dotRadius, y = 22.dp - dotRadius)
                            .size(dotRadius * 2)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = if (isActive) 1.5.dp else 0.dp,
                                color = if (isActive) Color.White else Color.Transparent,
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Current Speed Playing Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-4).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = c1),
                        modifier = Modifier.size(14.dp),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = c2),
                        modifier = Modifier.size(16.dp),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = c3),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format(Locale.US, "%.2fx Speed Playing", playbackSpeed),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                )
            }
        }
    }
}
