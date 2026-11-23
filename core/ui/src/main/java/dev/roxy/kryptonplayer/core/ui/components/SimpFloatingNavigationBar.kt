package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import dev.roxy.kryptonplayer.core.ui.liquidglass.DampedDragAnimation
import dev.roxy.kryptonplayer.core.ui.liquidglass.PlatformBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.drawInteractiveGlass
import dev.roxy.kryptonplayer.core.ui.liquidglass.rememberGlassInteraction
import dev.roxy.kryptonplayer.core.ui.liquidglass.rememberGlassLuminance
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Tab definitions for the main media screen.
 */
enum class MainNavTab {
    HOME,
    EXPLORE,
    SETTINGS,
}

private val CapsuleShape = RoundedCornerShape(percent = 50)
private val BarHeight = 64.dp
private val BlobHeight = 56.dp
private val BarInset = 6.dp

/**
 * SimpFloatingNavigationBar: Floating Liquid Glass pill toolbar with native backdrop sampling,
 * fluid sliding frosted blob indicator (with squash & stretch drag physics), and right-anchored
 * circular Search FAB inspired by SimpMusic / Apple Music.
 */
@Composable
fun SimpFloatingNavigationBar(
    selectedTab: MainNavTab,
    onTabSelected: (MainNavTab) -> Unit,
    onSearchClick: () -> Unit,
    onMoreClick: () -> Unit,
    moreMenuContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: PlatformBackdrop? = null,
) {
    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    val layer = rememberGraphicsLayer()
    val luminance = rememberGlassLuminance(layer, enabled = backdrop != null)
    val barInteraction = rememberGlassInteraction()
    val searchFabInteraction = rememberGlassInteraction()

    val tabCount = 4
    val selectedIndex = when (selectedTab) {
        MainNavTab.HOME -> 0
        MainNavTab.EXPLORE -> 1
        MainNavTab.SETTINGS -> 2
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val draggedFlag = remember { booleanArrayOf(false) }

    // Reference to tab width in pixels for drag calculations
    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    val currentTabWidthPx = rememberUpdatedState(tabWidthPx)

    val dampedDrag = remember(animationScope, tabCount) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 76f / 56f,
            onDragStarted = { draggedFlag[0] = false },
            onDragStopped = {
                if (draggedFlag[0]) {
                    val target = targetValue.roundToInt().coerceIn(0, tabCount - 1)
                    if (target in 0..2) {
                        val nextTab = when (target) {
                            0 -> MainNavTab.HOME
                            1 -> MainNavTab.EXPLORE
                            else -> MainNavTab.SETTINGS
                        }
                        currentIndex = target
                        onTabSelected(nextTab)
                    }
                    animateToValue(target.toFloat())
                }
            },
            onDrag = { _, dragAmount ->
                if (dragAmount.x != 0f) draggedFlag[0] = true
                val curPx = currentTabWidthPx.value
                if (curPx > 0f) {
                    val delta = dragAmount.x / curPx * if (isLtr) 1f else -1f
                    updateValue((targetValue + delta).coerceIn(0f, (tabCount - 1).toFloat()))
                }
            },
        )
    }

    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
        }
        dampedDrag.animateToValue(selectedIndex.toFloat())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            // 1) Main Centered Liquid Glass Pill Capsule
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(BarHeight)
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .shadow(
                                    elevation = 16.dp,
                                    shape = CapsuleShape,
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    ambientColor = Color.Black.copy(alpha = 0.5f),
                                )
                                .drawInteractiveGlass(
                                    isDark = isDark,
                                    backdrop = backdrop,
                                    layer = layer,
                                    luminance = { luminance.value },
                                    shape = CapsuleShape,
                                    interaction = barInteraction,
                                    minScrim = 0.08f,
                                    maxScrim = 0.28f,
                                )
                        } else {
                            Modifier
                                .shadow(
                                    elevation = 16.dp,
                                    shape = CapsuleShape,
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    ambientColor = Color.Black.copy(alpha = 0.5f),
                                )
                                .clip(CapsuleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = if (isDark) {
                                            listOf(
                                                Color(0xCC0A0A0D),
                                                Color(0xE6050507),
                                            )
                                        } else {
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.78f),
                                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                                            )
                                        },
                                    ),
                                )
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.06f),
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                                )
                            },
                        ),
                        shape = CapsuleShape,
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = BarInset, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val tabWidth = maxWidth / tabCount
                    val tabWidthPxVal = with(density) { tabWidth.toPx() }
                    LaunchedEffect(tabWidthPxVal) {
                        tabWidthPx = tabWidthPxVal
                    }

                    // 2) Sliding Frosted Liquid Blob (Selection Indicator)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = (if (isLtr) dampedDrag.value else (tabCount - 1) - dampedDrag.value) * tabWidthPxVal
                                scaleX = dampedDrag.scaleX
                                scaleY = dampedDrag.scaleY
                                val velocity = dampedDrag.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            }
                            .width(tabWidth)
                            .height(BlobHeight)
                            .then(
                                if (backdrop != null) {
                                    Modifier.drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { CapsuleShape },
                                        effects = {
                                            runCatching {
                                                val l = (luminance.value * 2f - 1f).let { sign(it) * it * it }
                                                val progress = dampedDrag.pressProgress
                                                vibrancy()
                                                colorControls(
                                                    brightness = 0.05f,
                                                    contrast = 1f,
                                                    saturation = 1.5f,
                                                )
                                                blur(
                                                    (if (l > 0f) lerp(8f.dp.toPx(), 16f.dp.toPx(), l) else lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)) +
                                                        20f.dp.toPx(),
                                                )
                                                lens(10f.dp.toPx() * progress, 14f.dp.toPx() * progress, chromaticAberration = true)
                                            }
                                        },
                                        highlight = { Highlight.Default.copy(alpha = 0.6f) },
                                        shadow = { Shadow(radius = 4f.dp, alpha = 0.4f) },
                                        innerShadow = {
                                            val progress = dampedDrag.pressProgress
                                            InnerShadow(radius = 8f.dp * progress, alpha = progress)
                                        },
                                        onDrawSurface = {
                                            val lumNorm = ((luminance.value - 0.3f) / 0.5f).coerceIn(0f, 1f)
                                            val darken = if (isDark) lerp(0.22f, 0.55f, lumNorm) else lerp(0.06f, 0.14f, lumNorm)
                                            drawRect(Color.Black.copy(alpha = darken))
                                            drawRect(primaryColor.copy(alpha = 0.12f))
                                        },
                                    )
                                } else {
                                    Modifier
                                        .clip(CapsuleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = if (isDark) {
                                                    listOf(
                                                        Color.White.copy(alpha = 0.18f),
                                                        Color.White.copy(alpha = 0.08f),
                                                    )
                                                } else {
                                                    listOf(
                                                        Color.White.copy(alpha = 0.85f),
                                                        Color.White.copy(alpha = 0.50f),
                                                    )
                                                },
                                            ),
                                        )
                                }
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = if (isDark) {
                                        listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            Color.White.copy(alpha = 0.10f),
                                        )
                                    } else {
                                        listOf(
                                            Color.White.copy(alpha = 0.85f),
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                        )
                                    },
                                ),
                                shape = CapsuleShape,
                            ),
                    )

                    // 3) Crisp Icons and Labels on top, carrying drag gesture
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(dampedDrag.modifier),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 1. Home Tab
                        val isHome = selectedTab == MainNavTab.HOME
                        SimpNavPillItem(
                            selected = isHome,
                            onClick = {
                                currentIndex = 0
                                onTabSelected(MainNavTab.HOME)
                            },
                            icon = {
                                AnimatedContent(
                                    targetState = isHome,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "HomeNavIcon",
                                ) { selected ->
                                    Icon(
                                        imageVector = if (selected) Icons.Rounded.Home else Icons.Outlined.Home,
                                        contentDescription = "Home",
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            },
                            label = "Home",
                            modifier = Modifier.weight(1f),
                        )

                        // 2. Explore Tab
                        val isExplore = selectedTab == MainNavTab.EXPLORE
                        SimpNavPillItem(
                            selected = isExplore,
                            onClick = {
                                currentIndex = 1
                                onTabSelected(MainNavTab.EXPLORE)
                            },
                            icon = {
                                AnimatedContent(
                                    targetState = isExplore,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "ExploreNavIcon",
                                ) { selected ->
                                    Icon(
                                        imageVector = if (selected) Icons.Rounded.Explore else Icons.Outlined.Explore,
                                        contentDescription = "Explore",
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            },
                            label = "Explore",
                            modifier = Modifier.weight(1f),
                        )

                        // 3. Settings Tab (with rotation micro-interaction)
                        val isSettings = selectedTab == MainNavTab.SETTINGS
                        var settingsRotationTarget by remember { mutableFloatStateOf(0f) }
                        val animatedSettingsRotation by animateFloatAsState(
                            targetValue = settingsRotationTarget,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                            label = "SettingsNavRotation",
                        )

                        SimpNavPillItem(
                            selected = isSettings,
                            onClick = {
                                settingsRotationTarget += 180f
                                currentIndex = 2
                                onTabSelected(MainNavTab.SETTINGS)
                            },
                            icon = {
                                AnimatedContent(
                                    targetState = isSettings,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "SettingsNavIcon",
                                ) { selected ->
                                    Icon(
                                        imageVector = if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings",
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer {
                                                rotationZ = animatedSettingsRotation
                                            },
                                    )
                                }
                            },
                            label = "Settings",
                            modifier = Modifier.weight(1f),
                        )

                        // 4. More Popup Tab
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            SimpNavPillItem(
                                selected = false,
                                onClick = onMoreClick,
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Widgets,
                                        contentDescription = "More",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                label = "More",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            moreMenuContent()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 4) Right-Anchored Floating Circular Search FAB (SimpMusic Liquid Glass Style)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .then(
                        if (backdrop != null) {
                            Modifier.drawInteractiveGlass(
                                isDark = isDark,
                                backdrop = backdrop,
                                layer = null,
                                luminance = { luminance.value },
                                shape = CircleShape,
                                interaction = searchFabInteraction,
                                minScrim = 0.08f,
                                maxScrim = 0.28f,
                            )
                        } else {
                            Modifier
                                .shadow(
                                    elevation = 16.dp,
                                    shape = CircleShape,
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    ambientColor = Color.Black.copy(alpha = 0.5f),
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = if (isDark) {
                                            listOf(
                                                Color(0xCC0A0A0D),
                                                Color(0xE6050507),
                                            )
                                        } else {
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.78f),
                                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                                            )
                                        },
                                    ),
                                )
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.06f),
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                                )
                            },
                        ),
                        shape = CircleShape,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSearchClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun SimpNavPillItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.92f, stiffness = 520f),
        label = "pillContentColor",
    )
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "pillItemScale",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = selectedScale
                scaleY = selectedScale
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.3.sp,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}
