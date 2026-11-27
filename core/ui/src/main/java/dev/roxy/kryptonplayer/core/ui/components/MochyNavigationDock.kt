package dev.roxy.kryptonplayer.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.liquidglass.PlatformBackdrop
import kotlinx.coroutines.launch

/** Mochy-matched Floating Navigation Bar Geometry */
internal val MochyNavBarHeight = 64.dp
internal val MochyNavBarShape = RoundedCornerShape(50)
private val NavHighlightInset = 8.dp
private val NavHighlightGap = 4.dp
private val NavRowPadding = NavHighlightInset - NavHighlightGap / 2

/** Asymmetric Spring Highlight Physics (stretches toward destination before settling) */
private val NavHighlightLead = spring<Float>(dampingRatio = 0.82f, stiffness = 700f)
private val NavHighlightTrail = spring<Float>(dampingRatio = 0.82f, stiffness = 320f)

/** Mochy Chrome Surface Tokens (#131313 dark frosted glass, 2px hairline, soft pill highlight) */
private val ChromeBaseColor = Color(0xFF131313)
private const val FrostTintAlpha = 0.78f
private const val FallbackTintAlpha = 0.90f
private const val ChromeOutlineWidthPx = 2f
private val ChromeOutlineColor = Color.White.copy(alpha = 0.05f)
private val ChromeOutlineStroke = Stroke(ChromeOutlineWidthPx)
private val ChromeHighlightColor = Color.White.copy(alpha = 0.10f)
private val ChromeShadow = Shadow(
    radius = 6.3.dp,
    color = Color.Black.copy(alpha = 0.12f),
    spread = 2.dp,
)
private const val UnlitIconAlpha = 0.70f

/** Faint procedural grain to break up banding on dark frosted blurs. */
private const val FrostGrainAlpha = 0.035f
private const val FrostGrainTileSize = 64
private val FrostGrainBrush: ShaderBrush by lazy {
    val random = java.util.Random(0x6A55)
    val pixels = IntArray(FrostGrainTileSize * FrostGrainTileSize) {
        val alpha = random.nextInt(256) shl 24
        if (random.nextBoolean()) alpha or 0xFFFFFF else alpha
    }
    val tile = android.graphics.Bitmap.createBitmap(
        pixels,
        FrostGrainTileSize,
        FrostGrainTileSize,
        android.graphics.Bitmap.Config.ARGB_8888,
    )
    ShaderBrush(ImageShader(tile.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

/**
 * Remembers the last active dock index across screen transitions (e.g. MediaPickerScreen <-> Settings)
 * so the sliding pill glides smoothly even when another destination mounts its own dock instance.
 */
private object MochyNavTransitionState {
    var lastSettledIndex: Int = 0
}

data class MochyNavTabItem(
    val title: String,
    @DrawableRes val unlitIconRes: Int = 0,
    @DrawableRes val litIconRes: Int = 0,
    val iconSize: Dp = 22.dp,
    val unlitVector: ImageVector? = null,
    val litVector: ImageVector? = null,
)

/**
 * Floating Obsidian Frosted Chrome Navigation Bar modeled after Mochy:
 * - 4 balanced icon-only tabs: Home (0), Search (1), Explore (2), Settings (3).
 * - Fixed `#131313` frosted glass surface with subtle grain anti-banding, 2px hairline border, and soft ambient lift.
 * - Equal 8.dp inset borderless pill with asymmetric lead/trail springs + elastic tab pressBounce + 3D dock tilt.
 */
@Composable
fun MochyNavigationDock(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSearchDoubleTap: (() -> Unit)? = null,
    backdrop: PlatformBackdrop? = null,
) {
    val haptic = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val lastTap = remember { longArrayOf(0L) }

    val tabs = remember {
        listOf(
            MochyNavTabItem(
                title = "Home",
                unlitIconRes = R.drawable.ic_nav_home_outline,
                litIconRes = R.drawable.ic_nav_home_filled,
                iconSize = 22.dp,
            ),
            MochyNavTabItem(
                title = "Search",
                unlitIconRes = R.drawable.ic_nav_search_outline,
                litIconRes = R.drawable.ic_nav_search_filled,
                iconSize = 22.dp,
            ),
            MochyNavTabItem(
                title = "Explore",
                unlitVector = Icons.Outlined.Explore,
                litVector = Icons.Rounded.Explore,
                iconSize = 24.dp,
            ),
            MochyNavTabItem(
                title = "Settings",
                unlitIconRes = R.drawable.ic_nav_settings_outline,
                litIconRes = R.drawable.ic_nav_settings_filled,
                iconSize = 24.dp,
            ),
        )
    }

    val safeSelectedIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)

    val dockSurfaceModifier = if (backdrop != null) {
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { MochyNavBarShape },
                effects = {
                    runCatching {
                        vibrancy()
                        colorControls(
                            brightness = -0.02f,
                            contrast = 1.04f,
                            saturation = 1.15f,
                        )
                        blur(24.dp.toPx())
                    }
                },
                onDrawSurface = {
                    drawRect(ChromeBaseColor.copy(alpha = FrostTintAlpha))
                },
            )
            .drawWithContent {
                drawContent()
                drawRect(brush = FrostGrainBrush, alpha = FrostGrainAlpha)
            }
    } else {
        Modifier
            .background(ChromeBaseColor.copy(alpha = FallbackTintAlpha))
            .drawWithContent {
                drawContent()
                drawRect(brush = FrostGrainBrush, alpha = FrostGrainAlpha)
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .height(MochyNavBarHeight)
                .press3D(maxTilt = 4f, pressedScale = 0.985f)
                .dropShadow(MochyNavBarShape, ChromeShadow)
                .clip(MochyNavBarShape)
                .then(dockSurfaceModifier)
                .drawWithContent {
                    drawContent()
                    val half = ChromeOutlineWidthPx / 2f
                    drawRoundRect(
                        color = ChromeOutlineColor,
                        topLeft = Offset(half, half),
                        size = Size(
                            width = (size.width - ChromeOutlineWidthPx).coerceAtLeast(0f),
                            height = (size.height - ChromeOutlineWidthPx).coerceAtLeast(0f),
                        ),
                        cornerRadius = CornerRadius((size.height / 2f - half).coerceAtLeast(0f)),
                        style = ChromeOutlineStroke,
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NavRowPadding)
                    .mochyNavHighlight(
                        selectedIndex = safeSelectedIndex,
                        tabCount = tabs.size,
                        fillColor = ChromeHighlightColor,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    MochyTabSlot(
                        tab = tab,
                        isSelected = index == safeSelectedIndex,
                        onClick = {
                            val now = android.os.SystemClock.uptimeMillis()
                            if (tab.title == "Search" &&
                                onSearchDoubleTap != null &&
                                now - lastTap[0] < viewConfiguration.doubleTapTimeoutMillis
                            ) {
                                lastTap[0] = 0L
                                onSearchDoubleTap()
                            } else {
                                lastTap[0] = now
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(index)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.MochyTabSlot(
    tab: MochyNavTabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val lit = animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "nav_tab_lit",
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .pressBounce(interactionSource = interactionSource, pressedScale = 0.85f)
            .semantics {
                selected = isSelected
                contentDescription = tab.title
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .testTag("mochy_nav_tab_${tab.title.lowercase()}"),
        contentAlignment = Alignment.Center,
    ) {
        TabIcons(tab = tab) { lit.value }
    }
}

/** Smoothly crossfades unlit and lit tab icons with modulated alpha over dark glass. */
@Composable
private fun TabIcons(
    tab: MochyNavTabItem,
    lit: () -> Float,
) {
    Box(contentAlignment = Alignment.Center) {
        if (tab.unlitVector != null && tab.litVector != null) {
            Icon(
                imageVector = tab.unlitVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(tab.iconSize)
                    .graphicsLayer {
                        alpha = UnlitIconAlpha * (1f - lit())
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
            )
            Icon(
                imageVector = tab.litVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(tab.iconSize)
                    .graphicsLayer {
                        alpha = lit()
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
            )
        } else {
            Icon(
                painter = painterResource(id = tab.unlitIconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(tab.iconSize)
                    .graphicsLayer {
                        alpha = UnlitIconAlpha * (1f - lit())
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
            )
            Icon(
                painter = painterResource(id = tab.litIconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(tab.iconSize)
                    .graphicsLayer {
                        alpha = lit()
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
            )
        }
    }
}

/**
 * Sliding Nav Highlight Pill: borderless translucent capsule that stretches asymmetrically
 * toward the destination tab instead of moving as a rigid block.
 */
@Composable
private fun Modifier.mochyNavHighlight(
    selectedIndex: Int,
    tabCount: Int,
    fillColor: Color,
): Modifier {
    val initialIndex = if (selectedIndex >= 0) {
        MochyNavTransitionState.lastSettledIndex.coerceIn(0, (tabCount - 1).coerceAtLeast(0))
    } else {
        0
    }
    val start = remember { Animatable(initialIndex.toFloat()) }
    val end = remember { Animatable(initialIndex.toFloat()) }
    val shown = remember { Animatable(if (selectedIndex >= 0) 1f else 0f) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) {
            shown.animateTo(0f, tween(200))
            return@LaunchedEffect
        }
        val target = selectedIndex.toFloat()
        MochyNavTransitionState.lastSettledIndex = selectedIndex
        if (shown.value == 0f) {
            start.snapTo(target)
            end.snapTo(target)
        }
        launch { shown.animateTo(1f, tween(200)) }
        val movingRight = target > (start.value + end.value) / 2f
        launch { start.animateTo(target, if (movingRight) NavHighlightTrail else NavHighlightLead) }
        end.animateTo(target, if (movingRight) NavHighlightLead else NavHighlightTrail)
    }

    return drawBehind {
        val alpha = shown.value
        if (alpha <= 0f || tabCount <= 0) return@drawBehind
        val slot = size.width / tabCount
        val halfGap = NavHighlightGap.toPx() / 2f
        val inset = NavHighlightInset.toPx()
        val left = (start.value * slot + halfGap).coerceAtLeast(0f)
        val right = ((end.value + 1f) * slot - halfGap).coerceAtMost(size.width)
        val pillWidth = (right - left).coerceAtLeast(0f)
        val pillHeight = (size.height - inset * 2f).coerceAtLeast(0f)
        val radius = CornerRadius(pillHeight / 2f)

        drawRoundRect(
            color = fillColor,
            topLeft = Offset(left, inset),
            size = Size(pillWidth, pillHeight),
            cornerRadius = radius,
            alpha = alpha,
        )
    }
}
