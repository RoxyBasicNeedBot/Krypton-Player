package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GestureHudType {
    VOLUME,
    BRIGHTNESS,
    AUDIO_BOOST,
}

/**
 * LiquidGestureHUD: Frosted liquid glass pill overlay that displays live volume/brightness
 * percentages with smooth progress animation.
 */
@Composable
fun LiquidGestureHUD(
    visible: Boolean,
    type: GestureHudType,
    fraction: Float,
    percentageText: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + scaleIn(initialScale = 0.85f),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) + scaleOut(targetScale = 0.85f),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    ambientColor = Color.Black.copy(alpha = 0.6f),
                ),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.40f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    ),
                ),
            ),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Icon
                val icon: ImageVector = when (type) {
                    GestureHudType.VOLUME -> if (fraction <= 0f) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp
                    GestureHudType.BRIGHTNESS -> Icons.Rounded.BrightnessHigh
                    GestureHudType.AUDIO_BOOST -> Icons.Rounded.VolumeUp
                }

                val iconTint: Color = when (type) {
                    GestureHudType.VOLUME -> MaterialTheme.colorScheme.primary
                    GestureHudType.BRIGHTNESS -> Color(0xFFFFD54F)
                    GestureHudType.AUDIO_BOOST -> Color(0xFFFF5722)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Progress Bar
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(iconTint),
                    )
                }

                // Percentage Text
                Text(
                    text = percentageText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
