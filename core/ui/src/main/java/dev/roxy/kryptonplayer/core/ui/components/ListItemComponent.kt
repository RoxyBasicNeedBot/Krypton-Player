package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.ui.theme.LocalIsDarkTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NextSegmentedListItem(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    colors: ListItemColors? = null,
    shapes: ListItemShapes = ListItemDefaults.shapes(),
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val itemShape = remember(isFirstItem, isLastItem, shapes) {
        val defaultBaseShape = shapes.shape
        if (defaultBaseShape is CornerBasedShape) {
            val corner24 = CornerSize(24.dp)
            defaultBaseShape.copy(
                topStart = if (isFirstItem) corner24 else defaultBaseShape.topStart,
                topEnd = if (isFirstItem) corner24 else defaultBaseShape.topEnd,
                bottomStart = if (isLastItem) corner24 else defaultBaseShape.bottomStart,
                bottomEnd = if (isLastItem) corner24 else defaultBaseShape.bottomEnd,
            )
        } else {
            shapes.shape
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val resolvedColors = colors ?: ListItemDefaults.segmentedColors(
        containerColor = colorScheme.surfaceContainerLow,
        disabledContainerColor = colorScheme.surfaceContainerLowest,
        contentColor = colorScheme.onSurface,
        supportingContentColor = colorScheme.onSurfaceVariant,
        disabledContentColor = colorScheme.onSurface.copy(alpha = 0.40f),
        disabledSupportingContentColor = colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
    )

    val borderColor = colorScheme.outlineVariant

    SegmentedListItem(
        modifier = modifier
            .bouncyPress()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = itemShape
            ),
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        verticalAlignment = Alignment.CenterVertically,
        shapes = shapes.copy(shape = itemShape),
        colors = resolvedColors,
        contentPadding = contentPadding,
        leadingContent = leadingContent,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        interactionSource = interactionSource,
        content = content,
    )
}


@Composable
fun ListSectionTitle(
    modifier: Modifier = Modifier,
    text: String,
    contentPadding: PaddingValues = PaddingValues(
        start = 12.dp,
        top = 20.dp,
        bottom = 10.dp,
    ),
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        modifier = modifier.padding(contentPadding),
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
    )
}
