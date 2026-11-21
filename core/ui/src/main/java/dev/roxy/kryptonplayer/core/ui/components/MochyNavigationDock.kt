package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.ui.R
import kotlinx.coroutines.launch

/** Mochy-inspired Floating Navigation Bar Geometry */
internal val MochyNavBarHeight = 64.dp
internal val MochyNavBarShape = RoundedCornerShape(50)
private val NavHighlightInset = 6.dp
private val NavHighlightGap = 4.dp
private val NavRowPadding = NavHighlightInset

/** Asymmetric Spring Highlight Physics */
private val NavHighlightLead = spring<Float>(dampingRatio = 0.84f, stiffness = 680f)
private val NavHighlightTrail = spring<Float>(dampingRatio = 0.84f, stiffness = 340f)

private const val ChromeOutlineWidthPx = 2f
private val ChromeOutlineStroke = Stroke(ChromeOutlineWidthPx)
private const val UnlitIconAlpha = 0.72f

data class MochyNavTabItem(
    val title: String,
    val unlitIconRes: Int,
    val litIconRes: Int,
    val iconSize: Dp = 22.dp,
)

/**
 * Refined Floating Obsidian Glass Navigation Bar.
 * - 4 balanced tabs: Home, Search, Library, Krypton.
 * - Theme-aware obsidian surface with crisp chrome outline (no hardcoded red accents).
 * - Smooth sliding active highlight pill with asymmetric lead/trail springs.
 * - Responsive width cap (420.dp) for phones, foldables, and tablets with Android 14 edge-to-edge insets.
 * - Full accessibility semantics, 48dp+ touch targets, and crisp active icon + label contrast.
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
            MochyNavTabItem("Home", R.drawable.ic_nav_home_outline, R.drawable.ic_nav_home_filled, 21.dp),
            MochyNavTabItem("Search", R.drawable.ic_nav_search_outline, R.drawable.ic_nav_search_filled, 21.dp),
            MochyNavTabItem("Library", R.drawable.ic_nav_lib_outline, R.drawable.ic_nav_lib_filled, 22.dp),
            MochyNavTabItem("Krypton", R.drawable.ic_nav_krypton_outline, R.drawable.ic_nav_krypton_filled, 22.dp),
        )
    }

    val safeSelectedIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)
    val colorScheme = MaterialTheme.colorScheme
    val dockTopSurface = colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
    val dockBottomSurface = colorScheme.surfaceContainerLow.copy(alpha = 0.98f)
    val dockOutlineColor = colorScheme.onSurface.copy(alpha = 0.10f)
    val pillFillColor = colorScheme.primary.copy(alpha = 0.14f)
    val pillStrokeColor = colorScheme.primary.copy(alpha = 0.22f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .height(MochyNavBarHeight)
                .press3D(maxTilt = 2.5f, pressedScale = 0.99f)
                .shadow(
                    elevation = 14.dp,
                    shape = MochyNavBarShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.55f),
                )
                .clip(MochyNavBarShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(dockTopSurface, dockBottomSurface),
                    ),
                )
                .drawWithContent {
                    drawContent()
                    val half = ChromeOutlineWidthPx / 2f
                    drawRoundRect(
                        color = dockOutlineColor,
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
                        fillColor = pillFillColor,
                        strokeColor = pillStrokeColor,
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
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = 620f, dampingRatio = 0.72f),
        label = "pressBounce",
    )
    val lit by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(180),
        label = "tab_lit",
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(180),
        label = "tab_tint",
    )
    val dotWidth by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 0.dp,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.78f),
        label = "tab_dot",
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = tab.unlitIconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(tab.iconSize)
                        .graphicsLayer {
                            alpha = UnlitIconAlpha * (1f - lit)
                        },
                )
                Icon(
                    painter = painterResource(id = tab.litIconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(tab.iconSize)
                        .graphicsLayer {
                            alpha = lit
                        },
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = tab.title,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = iconTint.copy(alpha = if (isSelected) 0.96f else 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (dotWidth > 0.dp) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(width = dotWidth, height = 2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                )
            }
        }
    }
}

/**
 * Sliding Nav Highlight Pill: stretches smoothly with asymmetric lead/trail springs.
 */
@Composable
private fun Modifier.mochyNavHighlight(
    selectedIndex: Int,
    tabCount: Int,
    fillColor: Color,
    strokeColor: Color,
): Modifier {
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
        launch { shown.animateTo(1f, tween(180)) }
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
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(left, inset),
            size = Size(pillWidth, pillHeight),
            cornerRadius = radius,
            style = Stroke(width = 1.dp.toPx()),
            alpha = alpha * 0.7f,
        )
    }
}
