package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * DoubleTapRippleOverlay: YouTube-style double-tap seek animation with expanding ripple
 * and bouncy second counter (+10s / -10s).
 */
@Composable
fun DoubleTapRippleOverlay(
    isForward: Boolean,
    seekSeconds: Int,
    visible: Boolean,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rippleScale = remember { Animatable(0.4f) }
    val rippleAlpha = remember { Animatable(0.8f) }

    LaunchedEffect(visible) {
        if (visible) {
            rippleScale.snapTo(0.4f)
            rippleAlpha.snapTo(0.8f)
            rippleScale.animateTo(
                targetValue = 1.3f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
            )
            rippleAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
            )
            onAnimationEnd()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Ripple Circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = rippleScale.value
                        scaleY = rippleScale.value
                        alpha = rippleAlpha.value
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
            )

            // Center Pill with icon and text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${if (isForward) "+" else "-"}${seekSeconds}s",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
