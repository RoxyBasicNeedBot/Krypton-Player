package dev.roxy.kryptonplayer.core.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val DefaultMaxTilt = 5f
private const val DefaultPressedScale = 0.985f

private val TiltSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 420f,
)

/**
 * Mochy 3D interactive tilt modifier on touch/press (from Mochy's Press3D.kt).
 * Tilts towards touch position and squishes smoothly with spring recovery.
 */
fun Modifier.press3D(
    maxTilt: Float = DefaultMaxTilt,
    pressedScale: Float = DefaultPressedScale,
    enabled: Boolean = true,
    origin: TransformOrigin = TransformOrigin.Center,
): Modifier = composed {
    if (!enabled) return@composed this

    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }

    var normalized by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitPointerEvent(PointerEventPass.Initial)
                        .changes.firstOrNull { it.pressed } ?: continue

                    val bounds = this.size
                    if (bounds.width > 0 && bounds.height > 0) {
                        val p = down.position
                        normalized = Offset(
                            x = ((p.x / bounds.width) * 2f - 1f).coerceIn(-1f, 1f),
                            y = ((p.y / bounds.height) * 2f - 1f).coerceIn(-1f, 1f),
                        )
                        size = bounds
                    }
                    scope.launch { press.animateTo(1f, TiltSpring) }

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.none { it.pressed }) break
                    }
                    scope.launch { press.animateTo(0f, TiltSpring) }
                }
            }
        }
        .graphicsLayer {
            val t = press.value
            if (t > 0f) {
                val s = 1f - (1f - pressedScale) * t
                scaleX = s
                scaleY = s

                rotationX = -normalized.y * maxTilt * t
                rotationY = normalized.x * maxTilt * t

                cameraDistance = (size.width.takeIf { it > 0 }?.toFloat() ?: 1000f) * 3f
                transformOrigin = origin
            }
        }
}

/**
 * Mochy-inspired high-speed Frosted Glass & Progressive Blur Container.
 * Supports hardware RenderEffect on API 31+ with smooth gradient fallback.
 */
@Composable
fun MochyFrostedGlass(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 24.dp,
    tintColor: Color = Color(0x990A0E18),
    borderColor: Color = Color(0x22FFFFFF),
    cornerRadius: Dp = 22.dp,
    progressive: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .border(0.8.dp, borderColor, shape)
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val radiusPx = blurRadius.toPx()
                    if (radiusPx > 0f) {
                        renderEffect = RenderEffect
                            .createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                }
            }
            .background(
                if (progressive) {
                    Brush.verticalGradient(
                        colors = listOf(
                            tintColor.copy(alpha = (tintColor.alpha * 1.15f).coerceAtMost(1f)),
                            tintColor.copy(alpha = tintColor.alpha * 0.85f),
                            tintColor.copy(alpha = (tintColor.alpha * 1.05f).coerceAtMost(1f))
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(tintColor, tintColor))
                }
            ),
        content = content
    )
}
