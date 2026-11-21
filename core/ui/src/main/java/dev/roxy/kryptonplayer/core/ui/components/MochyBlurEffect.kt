package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import dev.roxy.kryptonplayer.core.ui.liquidglass.PlatformBackdrop
import kotlinx.coroutines.launch

/**
 * Shared Mochi-inspired motion & physics tokens for buttery-smooth transitions,
 * scroll blurs, and tactile spring micro-interactions across Krypton Player.
 */
object MochyMotion {
    /** Silky exponential ease-out curve (snappy departure, ultra-smooth settle). */
    val EaseOut: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** Smooth symmetric ease for cross-fades and collapsing chrome. */
    val EaseInOut: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** Snappy tactile spring for buttons, tabs, and icon pops. */
    val SpringSnappy = spring<Float>(
        dampingRatio = 0.80f,
        stiffness = 580f,
    )

    /** Fluid spring for pills, scroll headers, and surface depth changes. */
    val SpringSmooth = spring<Float>(
        dampingRatio = 0.88f,
        stiffness = 380f,
    )

    /** Gentle no-bounce spring for subtle press and 3D tilt recovery. */
    val SpringGentle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 420f,
    )
}

private const val DefaultMaxTilt = 5f
private const val DefaultPressedScale = 0.985f

/**
 * Mochy 3D interactive tilt modifier on touch/press.
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
                    scope.launch { press.animateTo(1f, MochyMotion.SpringGentle) }

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.none { it.pressed }) break
                    }
                    scope.launch { press.animateTo(0f, MochyMotion.SpringGentle) }
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
 * Silky Mochi spring press feedback for cards and list rows without consuming gestures.
 */
fun Modifier.mochyItemPress(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.978f,
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = MochyMotion.SpringSnappy,
        label = "mochy_item_press",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Smooth GPU offscreen alpha fade on the top and/or bottom edges of a scrollable list,
 * so items melt smoothly beneath the status bar and floating glass navigation dock.
 */
fun Modifier.mochyScrollFadeEdge(
    topFadeHeight: Dp = 20.dp,
    bottomFadeHeight: Dp = 44.dp,
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val topPx = topFadeHeight.toPx().coerceAtMost(size.height * 0.25f)
        val bottomPx = bottomFadeHeight.toPx().coerceAtMost(size.height * 0.35f)
        if (topPx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = topPx,
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, topPx),
                blendMode = BlendMode.DstIn,
            )
        }
        if (bottomPx > 0f) {
            val startY = (size.height - bottomPx).coerceAtLeast(0f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = startY,
                    endY = size.height,
                ),
                topLeft = Offset(0f, startY),
                size = Size(size.width, bottomPx),
                blendMode = BlendMode.DstIn,
            )
        }
    }

/**
 * Mochy-inspired Frosted Obsidian Glass surface.
 * When a [backdrop] is provided, renders real GPU backdrop blur behind the surface
 * while keeping foreground content razor-sharp; otherwise renders a clean layered
 * translucent gradient fallback.
 */
@Composable
fun MochyFrostedGlass(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 22.dp,
    tintColor: Color = Color(0xAA0C0D12),
    borderColor: Color = Color(0x20FFFFFF),
    cornerRadius: Dp = 22.dp,
    progressive: Boolean = true,
    backdrop: PlatformBackdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape: Shape = RoundedCornerShape(cornerRadius)

    val surfaceModifier = if (backdrop != null) {
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    runCatching {
                        vibrancy()
                        colorControls(
                            brightness = 0.02f,
                            contrast = 1.03f,
                            saturation = 1.2f,
                        )
                        val radiusPx = blurRadius.toPx()
                        if (radiusPx > 0f) {
                            blur(radiusPx)
                        }
                    }
                },
                onDrawSurface = {
                    drawRect(tintColor)
                },
            )
            .border(0.8.dp, borderColor, shape)
    } else {
        Modifier
            .clip(shape)
            .background(
                if (progressive) {
                    Brush.verticalGradient(
                        colors = listOf(
                            tintColor.copy(alpha = (tintColor.alpha * 1.15f).coerceAtMost(0.96f)),
                            tintColor.copy(alpha = (tintColor.alpha * 0.88f).coerceAtMost(0.92f)),
                            tintColor.copy(alpha = (tintColor.alpha * 1.08f).coerceAtMost(0.96f)),
                        ),
                    )
                } else {
                    Brush.linearGradient(listOf(tintColor, tintColor))
                },
            )
            .border(0.8.dp, borderColor, shape)
    }

    Box(
        modifier = modifier.then(surfaceModifier),
        content = content,
    )
}

/**
 * Progressive scroll-reactive frosted glass top bar.
 * Stays invisible at scroll = 0 and smoothly blooms into a live blurred obsidian glass
 * bar with a crisp hairline bottom border as [scrollFraction] moves from 0f to 1f.
 */
@Composable
fun MochyScrollBlurHeader(
    scrollFraction: Float,
    modifier: Modifier = Modifier,
    backdrop: PlatformBackdrop? = null,
    maxBlurRadius: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val clamped = scrollFraction.coerceIn(0f, 1f)
    val smoothFraction by animateFloatAsState(
        targetValue = clamped,
        animationSpec = MochyMotion.SpringSmooth,
        label = "mochy_scroll_header_fraction",
    )

    val scrimColor = colorScheme.background.copy(
        alpha = if (backdrop != null) {
            (0.76f * smoothFraction).coerceIn(0f, 0.82f)
        } else {
            (0.92f * smoothFraction).coerceIn(0f, 0.94f)
        },
    )
    val hairlineColor = colorScheme.onSurface.copy(
        alpha = (if (isDark) 0.10f else 0.08f) * smoothFraction,
    )

    val blurModifier = if (backdrop != null && smoothFraction > 0.01f) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RectangleShape },
            effects = {
                runCatching {
                    vibrancy()
                    colorControls(
                        brightness = if (isDark) 0.01f else 0.03f,
                        contrast = 1.02f,
                        saturation = 1.15f,
                    )
                    val radiusPx = (maxBlurRadius * smoothFraction).toPx()
                    if (radiusPx > 0.5f) {
                        blur(radiusPx)
                    }
                }
            },
            onDrawSurface = {
                drawRect(scrimColor)
            },
        )
    } else {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    scrimColor,
                    scrimColor.copy(alpha = scrimColor.alpha * 0.85f),
                ),
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(blurModifier)
            .drawWithContent {
                drawContent()
                if (smoothFraction > 0.02f) {
                    val strokePx = 0.75.dp.toPx()
                    drawLine(
                        color = hairlineColor,
                        start = Offset(0f, size.height - strokePx / 2f),
                        end = Offset(size.width, size.height - strokePx / 2f),
                        strokeWidth = strokePx,
                    )
                }
            }
            .statusBarsPadding(),
        content = content,
    )
}

/**
 * Progressive bottom vignette + subtle glass scrim behind the floating navigation dock,
 * giving scrolled content a soft Mochi depth fade beneath the floating pill.
 */
@Composable
fun MochyBottomBlurScrim(
    modifier: Modifier = Modifier,
    scrimHeight: Dp = 112.dp,
) {
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(scrimHeight)
            .background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.42f to bg.copy(alpha = 0.45f),
                    0.78f to bg.copy(alpha = 0.82f),
                    1.0f to bg.copy(alpha = 0.96f),
                ),
            ),
    )
}
