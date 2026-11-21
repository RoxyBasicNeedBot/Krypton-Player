package dev.roxy.kryptonplayer.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.ui.preview.DayNightPreview
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.core.ui.theme.JioHotstarFontFamily

/**
 * Exact matte charcoal monochrome design tokens for the onboarding texture & palette.
 */
internal object KryptonOnbColors {
    val OuterBlack = Color(0xFF000000)
    val Page = Color(0xFF1C1C1C)
    val Dot = Color(0xFF2F2F2F)
    val WordOutline = Color(0xFF343434)
    val Handle = Color(0xFF7D7D7D)
    val Card = Color(0xFF262626)
    val CardElevated = Color(0xFF2C2C2C)
    val CardPressed = Color(0xFF353535)
    val CardBorder = Color(0xFF3A3A3A)
    val Title = Color(0xFFFFFFFF)
    val WelcomeLead = Color(0xFFA7A7A7)
    val Subtitle = Color(0xFFBDBDBD)
    val ClosingLine = Color(0xFFD8D8D8)
    val Muted = Color(0xFF8C8C8C)
    val Faint = Color(0xFF6A6A6A)
    val ButtonPrimaryBg = Color(0xFFEBEBEB)
    val ButtonPrimaryText = Color(0xFF141414)
    val ButtonSecondaryBg = Color(0xFF323232)
    val ButtonSecondaryText = Color(0xFFD6D6D6)
    val BadgeBg = Color(0xFF2F2F2F)
    val BadgeContent = Color(0xFFE2E2E2)
    // Soft natural sage harmonized with the mascot's hoodie for granted/active states (zero neon)
    val SageAccent = Color(0xFF8FB396)
    val SageContainer = Color(0xFF243026)

    val MaxContentWidth = 560.dp
    val TopClearance = 44.dp
}

internal val KryptonTitleTracking = (-0.04).em
internal val KryptonSubtitleTracking = (-0.03).em

@Composable
internal fun rememberOnbSidePadding(maxWidth: Dp): Dp = remember(maxWidth) {
    if (maxWidth < 340.dp) 20.dp else 28.dp
}

/**
 * Recessed modal sheet shell:
 * Scales the underlying app subtly (0.92x, 32.dp corners) while the rounded charcoal onboarding sheet
 * sits over pure black, then smoothly expands the app back to 1.0x when onboarding finishes.
 */
@Composable
fun KryptonOnboardingSheetShell(
    showOnboarding: Boolean,
    onboardingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    mainContent: @Composable () -> Unit,
) {
    val overlayProgress = remember { Animatable(if (showOnboarding) 1f else 0f) }

    LaunchedEffect(showOnboarding) {
        overlayProgress.animateTo(
            targetValue = if (showOnboarding) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (showOnboarding) 420 else 360,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    val progress = overlayProgress.value
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(KryptonOnbColors.OuterBlack),
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // Underlying main application content recessed gently when onboarding is active
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = 1f - 0.08f * progress
                    scaleX = scale
                    scaleY = scale
                    translationY = (statusTop.toPx() * 0.45f) * progress
                    clip = progress > 0.001f
                    shape = RoundedCornerShape((32f * progress).dp)
                    alpha = 1f - 0.35f * progress
                },
        ) {
            mainContent()
        }

        // Elevated rounded-top matte charcoal onboarding sheet
        if (progress > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusTop + 10.dp)
                    .graphicsLayer {
                        translationY = screenHeightPx * (1f - progress)
                        clip = true
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    }
                    .background(KryptonOnbColors.Page),
            ) {
                onboardingContent()

                // Signature centered matte drag-handle pill at top of sheet
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .width(56.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(KryptonOnbColors.Handle.copy(alpha = 0.72f)),
                )
            }
        }
    }
}

/**
 * Shared onboarding page layout with top badge, tight-tracked display title,
 * scrollable content area, and pinned navigation-bar-safe footer.
 */
@Composable
internal fun KryptonOnboardingPage(
    badge: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    stepIndex: Int? = null,
    totalSteps: Int = 3,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(KryptonOnbColors.Page),
    ) {
        val side = rememberOnbSidePadding(maxWidth)
        KryptonWelcomeDots(spacing = 22.dp)

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = KryptonOnbColors.MaxContentWidth)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = side)
                    .padding(top = KryptonOnbColors.TopClearance + 14.dp, bottom = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    badge()
                    if (stepIndex != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(totalSteps) { index ->
                                val isCurrent = index == stepIndex
                                val isDone = index < stepIndex
                                Box(
                                    modifier = Modifier
                                        .width(if (isCurrent) 22.dp else 7.dp)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isCurrent -> KryptonOnbColors.Title
                                                isDone -> KryptonOnbColors.Subtitle.copy(alpha = 0.55f)
                                                else -> KryptonOnbColors.CardBorder
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = title,
                    fontFamily = JioHotstarFontFamily,
                    color = KryptonOnbColors.Title,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = KryptonTitleTracking,
                )

                if (subtitle != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = subtitle,
                        fontFamily = JioHotstarFontFamily,
                        color = KryptonOnbColors.Subtitle,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = KryptonSubtitleTracking,
                    )
                }

                Spacer(Modifier.height(24.dp))
                content()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = side)
                    .padding(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = footer,
            )
        }
    }
}

@Composable
internal fun KryptonOnbBadge(
    icon: ImageVector,
    contentDescription: String? = null,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(KryptonOnbColors.BadgeBg)
            .border(1.dp, KryptonOnbColors.CardBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = KryptonOnbColors.BadgeContent,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
internal fun KryptonOnbPrimaryButton(
    text: String,
    onClick: () -> Unit,
    testTag: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "onbPrimaryScale",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .minimumInteractiveComponentSize()
            .scale(scale)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) KryptonOnbColors.ButtonPrimaryBg else KryptonOnbColors.ButtonSecondaryBg,
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontFamily = JioHotstarFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02).em,
                color = if (enabled) KryptonOnbColors.ButtonPrimaryText else KryptonOnbColors.Muted,
            )
        }
    }
}

@Composable
internal fun KryptonOnbSecondaryButton(
    text: String,
    onClick: () -> Unit,
    testTag: String = "",
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "onbSecondaryScale",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .minimumInteractiveComponentSize()
            .scale(scale)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = KryptonOnbColors.ButtonSecondaryBg,
        border = BorderStroke(1.dp, KryptonOnbColors.CardBorder),
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontFamily = JioHotstarFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).em,
                color = KryptonOnbColors.ButtonSecondaryText,
            )
        }
    }
}

@Composable
internal fun KryptonOnbSwitchCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = "",
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "onbSwitchCardScale",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = if (isPressed) KryptonOnbColors.CardPressed else KryptonOnbColors.Card,
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) KryptonOnbColors.CardBorder else KryptonOnbColors.CardBorder.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KryptonOnbColors.CardElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) KryptonOnbColors.Title else KryptonOnbColors.Muted,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = JioHotstarFontFamily,
                    color = KryptonOnbColors.Title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.02).em,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    fontFamily = JioHotstarFontFamily,
                    color = KryptonOnbColors.Muted,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
            }

            Spacer(Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = KryptonOnbColors.ButtonPrimaryText,
                    checkedTrackColor = KryptonOnbColors.SageAccent,
                    uncheckedThumbColor = KryptonOnbColors.Subtitle,
                    uncheckedTrackColor = KryptonOnbColors.CardPressed,
                    uncheckedBorderColor = KryptonOnbColors.CardBorder,
                ),
            )
        }
    }
}

@DayNightPreview
@Composable
private fun KryptonOnbSwitchCardPreview() {
    KryptonPlayerTheme {
        KryptonOnbSwitchCard(
            icon = Icons.Rounded.Settings,
            title = "Preview Setting",
            description = "Quick toggle description for the preview panel",
            checked = true,
            onCheckedChange = {},
            testTag = "onboarding_preview_switch",
        )
    }
}
