package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * TV & Google TV focus modifier extension.
 * Smoothly scales the focused element (1.0f -> 1.04f) and renders an expressive
 * 2.dp primary outline border when focused via Android TV remote / D-pad navigation.
 */
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(12.dp),
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this

    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "TvFocusScale",
    )

    this
        .onFocusChanged { isFocused = it.isFocused }
        .focusable(enabled = enabled)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (isFocused) {
                Modifier.border(
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    shape = shape,
                )
            } else {
                Modifier
            }
        )
}
