package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.ui.R

@Composable
fun AnimatedStopwatchIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = Color.Unspecified,
    triggerKey: Any = Unit
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(triggerKey) {
        scale.snapTo(0.9f)
        scale.animateTo(
            1.18f,
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_stopwatch_timer),
            contentDescription = "Stopwatch",
            tint = if (tint != Color.Unspecified) tint else Color.White,
            modifier = Modifier.size(size)
        )
    }
}
