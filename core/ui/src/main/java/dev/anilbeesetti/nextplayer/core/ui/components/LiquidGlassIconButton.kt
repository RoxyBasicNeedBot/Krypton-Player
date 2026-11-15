package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.PlatformBackdrop
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.liquidGlass
import dev.anilbeesetti.nextplayer.core.ui.theme.LocalIsDarkTheme

/**
 * Liquid Glass circular icon button with native Kyant backdrop sampling,
 * specular vertical border highlight, and elastic bouncy spring press.
 */
@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    borderBrush: Brush? = null,
    backdrop: PlatformBackdrop? = null,
    content: @Composable () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val resolvedContainerColor = when {
        containerColor != null -> containerColor
        backdrop != null -> Color.Transparent // Kyant backdrop shader renders the frosted glass surface
        isDark -> Color.White.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
    }

    val resolvedBorderBrush = borderBrush ?: Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.08f),
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.80f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
            )
        },
    )

    val resolvedContentColor = contentColor ?: if (isDark) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val glassModifier = if (backdrop != null) {
        Modifier.liquidGlass(
            backdrop = backdrop,
            shape = CircleShape,
            interactive = false,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(glassModifier)
            .then(
                if (resolvedContainerColor != Color.Transparent) {
                    Modifier.background(resolvedContainerColor, CircleShape)
                } else {
                    Modifier
                }
            )
            .border(
                width = 1.dp,
                brush = resolvedBorderBrush,
                shape = CircleShape,
            )
            .bouncyPress()
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
            content()
        }
    }
}
