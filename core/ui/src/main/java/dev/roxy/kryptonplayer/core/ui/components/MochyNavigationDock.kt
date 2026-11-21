package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.ui.R
import kotlinx.coroutines.launch

/** Mochy Navigation Bar Geometry (1:1 Parity with Mochy's AppNavigation.kt & FrostedGlass.kt) */
internal val MochyNavBarHeight = 64.dp
internal val MochyNavBarShape = RoundedCornerShape(50)
private val NavHighlightInset = 8.dp
private val NavHighlightGap = 4.dp
private val NavRowPadding = NavHighlightInset - NavHighlightGap / 2

/** Mochy Asymmetric Spring Highlight Physics */
private val NavHighlightLead = spring<Float>(dampingRatio = 0.82f, stiffness = 700f)
private val NavHighlightTrail = spring<Float>(dampingRatio = 0.82f, stiffness = 320f)

/** Mochy Chrome colors & outline */
private val ChromeHighlightColor = Color.White.copy(alpha = 0.10f)
private const val ChromeOutlineWidthPx = 2f
private val ChromeOutlineColor = Color.White.copy(alpha = 0.06f)
private val ChromeOutlineStroke = Stroke(ChromeOutlineWidthPx)
private const val UnlitIconAlpha = 0.70f

data class MochyNavTabItem(
    val title: String,
    val unlitIconRes: Int,
    val litIconRes: Int,
    val iconSize: Dp = 23.dp,
)

/**
 * Mochy Signature Floating Frosted Glass Navigation Bar (1:1 Parity with Mochy's AppNavigation.kt).
 * - Exact 4 Tabs: Home, Search, Library, Krypton.
 * - Stadium capsule shape with 64.dp height, floating margins.
 * - Crisp dark chrome glass surface (#131313 at ~95%) with Mochy chrome hairline outline (2px at 6%).
 * - Sibling layer architecture so the icons remain 100% crisp, sharp and never blurred by background render effects.
 * - Dynamic sliding highlight pill with asymmetric spring physics (lead = 700f, trail = 320f).
 * - 3D micro-tilt touch feedback on the capsule and pressBounce (scale to 0.85f) on individual tabs.
 * - Cross-fading unlit and lit vector icons with smooth alpha transitions.
 */
@Composable
fun MochyNavigationDock(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSearchDoubleTap: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val lastTap = remember { longArrayOf(0L) }

    val tabs = remember {
        listOf(
            MochyNavTabItem("Home", R.drawable.ic_nav_home_outline, R.drawable.ic_nav_home_filled, 22.dp),
            MochyNavTabItem("Search", R.drawable.ic_nav_search_outline, R.drawable.ic_nav_search_filled, 22.dp),
            MochyNavTabItem("Library", R.drawable.ic_nav_lib_outline, R.drawable.ic_nav_lib_filled, 24.dp),
            MochyNavTabItem("Krypton", R.drawable.ic_nav_krypton_outline, R.drawable.ic_nav_krypton_filled, 24.dp),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MochyNavBarHeight)
                .press3D(maxTilt = 4f, pressedScale = 0.985f)
                .shadow(
                    elevation = 12.dp,
                    shape = MochyNavBarShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
                .clip(MochyNavBarShape)
                .background(Color(0xF2131313)) // Mochy signature #131313 dark surface (~95% opacity)
                .drawWithContent {
                    drawContent()
                    val half = ChromeOutlineWidthPx / 2f
                    drawRoundRect(
                        color = ChromeOutlineColor,
                        topLeft = Offset(half, half),
                        size = Size(size.width - ChromeOutlineWidthPx, size.height - ChromeOutlineWidthPx),
                        cornerRadius = CornerRadius((size.height / 2f - half).coerceAtLeast(0f)),
                        style = ChromeOutlineStroke,
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NavRowPadding)
                    .mochyNavHighlight(selectedTabIndex, tabs.size),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    MochyTabSlot(
                        tab = tab,
                        isSelected = index == selectedTabIndex,
                        onClick = {
                            val now = android.os.SystemClock.uptimeMillis()
                            if (tab.title == "Search" && onSearchDoubleTap != null && now - lastTap[0] < viewConfiguration.doubleTapTimeoutMillis) {
                                lastTap[0] = 0L
                                onSearchDoubleTap()
                            } else {
                                lastTap[0] = now
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(index)
                            }
                        }
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
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.7f),
        label = "pressBounce"
    )
    val lit by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "tab_lit"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = tab.unlitIconRes),
                contentDescription = tab.title,
                tint = Color.White,
                modifier = Modifier
                    .size(tab.iconSize)
                    .graphicsLayer {
                        alpha = UnlitIconAlpha * (1f - lit)
                    }
            )
            Icon(
                painter = painterResource(id = tab.litIconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(tab.iconSize)
                    .graphicsLayer {
                        alpha = lit
                    }
            )
        }
    }
}

/**
 * Mochy Nav Highlight Pill: stretches smoothly with asymmetric lead/trail springs.
 */
@Composable
private fun Modifier.mochyNavHighlight(selectedIndex: Int, tabCount: Int): Modifier {
    val start = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val end = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val shown = remember { Animatable(if (selectedIndex >= 0) 1f else 0f) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) {
            shown.animateTo(0f, tween(200))
            return@LaunchedEffect
        }
        val target = selectedIndex.toFloat()
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
        if (alpha <= 0f || tabCount == 0) return@drawBehind
        val slot = size.width / tabCount
        val halfGap = NavHighlightGap.toPx() / 2f
        val inset = NavHighlightInset.toPx()
        val left = start.value * slot + halfGap
        val right = (end.value + 1f) * slot - halfGap
        val height = size.height - inset * 2f

        drawRoundRect(
            color = ChromeHighlightColor,
            topLeft = Offset(left, inset),
            size = Size(right - left, height),
            cornerRadius = CornerRadius(height / 2f),
            alpha = alpha,
        )
    }
}
