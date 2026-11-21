package dev.roxy.kryptonplayer.feature.player.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.feature.player.LocalUseMaterialYouControls
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    size: Dp = 40.dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    glassStyle: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current

    if (onLongClick != null) {
        LaunchedEffect(interactionSource) {
            var isLongPressClicked = false
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        isLongPressClicked = false
                        delay(viewConfiguration.longPressTimeoutMillis)
                        isLongPressClicked = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick.invoke()
                    }

                    is PressInteraction.Release -> {
                        if (!isLongPressClicked) {
                            onClick()
                        }
                    }
                }
            }
        }
    }

    val directOnClick = if (onLongClick == null) onClick else ({})

    if (LocalUseMaterialYouControls.current) {
        FilledTonalIconButton(
            onClick = directOnClick,
            enabled = isEnabled,
            modifier = modifier.size(size),
            interactionSource = interactionSource,
            content = content
        )
    } else {
        val effectiveBg = when {
            containerColor != Color.Transparent -> containerColor
            glassStyle -> Color.White.copy(alpha = 0.12f)
            else -> Color.Transparent
        }
        val effectiveBorder = when {
            glassStyle -> Modifier.border(0.8.dp, Color.White.copy(alpha = 0.20f), CircleShape)
            containerColor != Color.Transparent -> Modifier.border(0.8.dp, containerColor.copy(alpha = 0.3f), CircleShape)
            else -> Modifier
        }

        CompositionLocalProvider(
            LocalContentColor provides Color.White.copy(alpha = if (isEnabled) 0.95f else 0.4f),
            LocalRippleConfiguration provides RippleConfiguration(
                color = Color.White,
                rippleAlpha = RippleAlpha(
                    pressedAlpha = 0.35f,
                    focusedAlpha = 0.35f,
                    draggedAlpha = 0.35f,
                    hoveredAlpha = 0.35f
                )
            )
        ) {
            IconButton(
                onClick = directOnClick,
                enabled = isEnabled,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(effectiveBg, CircleShape)
                    .then(effectiveBorder),
                interactionSource = interactionSource,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White.copy(alpha = if (isEnabled) 0.95f else 0.4f),
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.White.copy(alpha = 0.3f),
                ),
                content = content,
            )
        }
    }
}
