package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.feature.player.state.TapGestureState
import kotlin.math.abs

@Composable
fun DoubleTapIndicator(
    modifier: Modifier = Modifier,
    tapGestureState: TapGestureState,
) {
    val seekMillis = tapGestureState.seekMillis
    val rippleTick = tapGestureState.rippleTick

    AnimatedVisibility(
        visible = seekMillis != 0L,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(200)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (seekMillis < 0L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AccumulatingSeekRipple(
                        isRightSide = false,
                        accumulatedMs = abs(seekMillis),
                        rippleTick = rippleTick,
                    )
                }
            } else if (seekMillis > 0L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    AccumulatingSeekRipple(
                        isRightSide = true,
                        accumulatedMs = abs(seekMillis),
                        rippleTick = rippleTick,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccumulatingSeekRipple(
    isRightSide: Boolean,
    accumulatedMs: Long,
    rippleTick: Int,
) {
    val secs = (accumulatedMs / 1000L).coerceAtLeast(1L)
    val label = if (isRightSide) "+${secs}s" else "-${secs}s"
    val arcProgress = remember { Animatable(0f) }
    LaunchedEffect(rippleTick) {
        if (rippleTick > 0) {
            arcProgress.snapTo(0f)
            arcProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            )
        }
    }
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(rippleTick) {
        if (rippleTick > 0) {
            flashAlpha.snapTo(0.22f)
            flashAlpha.animateTo(0f, animationSpec = tween(500))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(150.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isRightSide) {
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.10f + flashAlpha.value))
                    } else {
                        listOf(Color.White.copy(alpha = 0.10f + flashAlpha.value), Color.Transparent)
                    },
                ),
                shape = if (isRightSide) {
                    RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                } else {
                    RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Canvas(modifier = Modifier.size(58.dp)) {
                val stroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                val sweep = 250f * arcProgress.value
                val bgStartAngle = if (isRightSide) 140f else -70f
                val bgSweep = if (isRightSide) 250f else -250f
                drawArc(
                    color = Color.White.copy(alpha = 0.18f),
                    startAngle = bgStartAngle,
                    sweepAngle = bgSweep,
                    useCenter = false,
                    style = stroke,
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.90f),
                    startAngle = bgStartAngle,
                    sweepAngle = if (isRightSide) sweep else -sweep,
                    useCenter = false,
                    style = stroke,
                )
                val cx = size.width / 2f
                val cy = size.height / 2f
                val aW = 7.dp.toPx()
                val aH = 10.dp.toPx()
                val gap = 5.dp.toPx()
                val arrowColor = Color.White
                val sw = 2.6.dp.toPx()
                if (isRightSide) {
                    drawLine(arrowColor, Offset(cx - aW - gap, cy - aH / 2), Offset(cx - gap, cy), strokeWidth = sw, cap = StrokeCap.Round)
                    drawLine(arrowColor, Offset(cx - aW - gap, cy + aH / 2), Offset(cx - gap, cy), strokeWidth = sw, cap = StrokeCap.Round)
                    drawLine(arrowColor, Offset(cx - gap, cy - aH / 2), Offset(cx + aW - gap, cy), strokeWidth = sw, cap = StrokeCap.Round)
                    drawLine(arrowColor, Offset(cx - gap, cy + aH / 2), Offset(cx + aW - gap, cy), strokeWidth = sw, cap = StrokeCap.Round)
                } else {
                    drawLine(arrowColor, Offset(cx + aW + gap, cy - aH / 2), Offset(cx + gap, cy), strokeWidth = sw, cap = StrokeCap.Round)
                    drawLine(arrowColor, Offset(cx + aW + gap, cy + aH / 2), Offset(cx + gap, cy), strokeWidth = sw, cap = StrokeCap.Round)
                    drawLine(arrowColor, Offset(cx + gap, cy - aH / 2), Offset(cx - aW + gap, cy), strokeWidth = sw, cap = StrokeCap.Round)
                    drawLine(arrowColor, Offset(cx + gap, cy + aH / 2), Offset(cx - aW + gap, cy), strokeWidth = sw, cap = StrokeCap.Round)
                }
            }
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (isRightSide) "Forward" else "Rewind",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
