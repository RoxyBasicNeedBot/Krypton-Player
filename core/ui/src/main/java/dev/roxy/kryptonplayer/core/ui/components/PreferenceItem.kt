package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferenceItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    trailingContent: @Composable () -> Unit = {},
) {
    NextSegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        leadingContent = icon?.let {
            {
                val iconBg = if (enabled) {
                    Color.White.copy(alpha = 0.08f)
                } else {
                    Color.White.copy(alpha = 0.04f)
                }
                val iconTint = if (enabled) {
                    Color.White
                } else {
                    Color(0xFF828A9E).copy(alpha = 0.40f)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = iconTint,
                    )
                }
            }
        },
        supportingContent = description?.let {
            {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f)
                    },
                )
            }
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                },
            )
        },
        trailingContent = trailingContent,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectablePreference(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
) {
    NextSegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        content = {
            Text(
                text = title,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium.copy(
                    textDecoration = if (selected) TextDecoration.LineThrough else TextDecoration.None,
                ),
            )
        },
        supportingContent = {
            description?.let {
                Text(
                    text = it,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (selected) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
            }
        },
        trailingContent = {
            Checkbox(
                modifier = Modifier.semantics { contentDescription = title },
                checked = selected,
                onCheckedChange = null,
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SingleSelectablePreference(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
) {
    NextSegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        content = {
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        supportingContent = {
            description?.let {
                Text(
                    text = it,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
fun PreferenceItemPreview() {
    PreferenceItem(
        title = "Title",
        description = "Description of the preference item goes here.",
        icon = NextIcons.DoubleTap,
        enabled = true,
    )
}

@Preview(showBackground = true)
@Composable
fun SelectablePreferencePreview() {
    SelectablePreference(
        title = "Title",
        description = "Description of the preference item goes here.",
    )
}

internal fun Color.applyAlpha(enabled: Boolean): Color {
    return if (enabled) this else this.copy(alpha = 0.6f)
}
