package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.core.ui.theme.PlayerSliderColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferenceSlider(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
) {
    NextSegmentedListItem(
        modifier = modifier,
        onClick = {},
        onLongClick = null,
        enabled = enabled,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        leadingContent = icon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        supportingContent = {
            Column {
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.applyAlpha(enabled),
                    )
                }
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    value = value,
                    valueRange = valueRange,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    colors = PlayerSliderColors.standardSliderColors(MaterialTheme.colorScheme.primary),
                )
            }
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.applyAlpha(enabled),
            )
        },
        trailingContent = trailingContent,
    )
}

@PreviewLightDark
@Composable
private fun PreferenceSliderPreview() {
    KryptonPlayerTheme {
        PreferenceSlider(
            title = "Text Size",
            description = "24 sp",
            value = 24f,
            valueRange = 12f..36f,
            onValueChange = {},
        )
    }
}
