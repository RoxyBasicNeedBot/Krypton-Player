package dev.roxy.kryptonplayer.feature.videopicker.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.roxy.kryptonplayer.core.model.ApplicationPreferences
import dev.roxy.kryptonplayer.core.model.MediaLayoutMode
import dev.roxy.kryptonplayer.core.model.MediaViewMode
import dev.roxy.kryptonplayer.core.model.Sort
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.bouncyPress
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.liquidglass.PlatformBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.liquidGlass
import dev.roxy.kryptonplayer.core.ui.liquidglass.liquidGlassBorderLaser
import dev.roxy.kryptonplayer.feature.videopicker.extensions.name

/**
 * Liquid Glass Quick Settings Dialog:
 * - Ultra-polished Kyant liquid glass backdrop refraction and specular border highlights
 * - Orbiting border laser accent
 * - Frosted capsule segmented controls for View Mode, Layout, and Sort Order
 * - Interactive glass filter chips for display fields
 * - Pinned glass header and footer actions
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSettingsDialog(
    applicationPreferences: ApplicationPreferences,
    onDismiss: () -> Unit,
    updatePreferences: (ApplicationPreferences) -> Unit,
    backdrop: PlatformBackdrop? = null,
) {
    var preferences by remember { mutableStateOf(applicationPreferences) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dialogShape = RoundedCornerShape(26.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .padding(vertical = 24.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = dialogShape,
                        spotColor = Color.Black.copy(alpha = 0.60f),
                    )
                    .clip(dialogShape)
                    .background(
                        color = Color(0xFF0C1019),
                        shape = dialogShape,
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = dialogShape,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}, // Prevent dismiss when tapping dialog contents
                    ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // --- 1. Dialog Header (Pinned) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.12f),
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = NextIcons.DashBoard,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.quick_settings),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.2).sp,
                                    ),
                                    color = Color.White,
                                )
                                Text(
                                    text = stringResource(R.string.media_view_mode) + " • " + stringResource(R.string.sort),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF828A9E),
                                    ),
                                )
                            }
                        }

                        // Close Button (Circular Pill)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = CircleShape,
                                )
                                .bouncyPress()
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = NextIcons.Close,
                                contentDescription = stringResource(R.string.cancel),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            )
                        }
                    }

                    HorizontalDivider(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 1.dp,
                    )

                    // --- 2. Scrollable Body ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                    ) {
                        // Media View Mode
                        DialogSectionTitle(title = stringResource(R.string.media_view_mode))
                        GlassSegmentedRow(
                            items = MediaViewMode.entries,
                            selectedItem = preferences.mediaViewMode,
                            onItemSelected = { preferences = preferences.copy(mediaViewMode = it) },
                            itemLabel = { it.name() },
                            itemIcon = { mode ->
                                when (mode) {
                                    MediaViewMode.FOLDERS -> NextIcons.Folder
                                    MediaViewMode.VIDEOS -> NextIcons.Video
                                    MediaViewMode.FOLDER_TREE -> NextIcons.VideoLibrary
                                }
                            },
                        )

                        // Media Layout
                        DialogSectionTitle(title = stringResource(R.string.media_layout))
                        GlassSegmentedRow(
                            items = MediaLayoutMode.entries,
                            selectedItem = preferences.mediaLayoutMode,
                            onItemSelected = { preferences = preferences.copy(mediaLayoutMode = it) },
                            itemLabel = { it.name() },
                            itemIcon = { layoutMode ->
                                when (layoutMode) {
                                    MediaLayoutMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
                                    MediaLayoutMode.GRID -> Icons.Default.GridView
                                }
                            },
                        )

                        GlassDivider()

                        // Sort Options
                        DialogSectionTitle(title = stringResource(R.string.sort))
                        SortOptions(
                            selectedSortBy = preferences.sortBy,
                            onOptionSelected = { preferences = preferences.copy(sortBy = it) },
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GlassSegmentedRow(
                            items = Sort.Order.entries,
                            selectedItem = preferences.sortOrder,
                            onItemSelected = { preferences = preferences.copy(sortOrder = it) },
                            itemLabel = { it.name(sortBy = preferences.sortBy) },
                            itemIcon = { order ->
                                if (order == Sort.Order.ASCENDING) NextIcons.ArrowUpward else NextIcons.ArrowDownward
                            },
                        )

                        GlassDivider()

                        // Display Fields
                        DialogSectionTitle(title = stringResource(R.string.fields))
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.Top),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FieldChip(
                                label = stringResource(id = R.string.duration),
                                selected = preferences.showDurationField,
                                onClick = { preferences = preferences.copy(showDurationField = !preferences.showDurationField) },
                            )
                            FieldChip(
                                label = stringResource(id = R.string.extension),
                                selected = preferences.showExtensionField,
                                onClick = { preferences = preferences.copy(showExtensionField = !preferences.showExtensionField) },
                            )
                            FieldChip(
                                label = stringResource(id = R.string.path),
                                selected = preferences.showPathField,
                                onClick = { preferences = preferences.copy(showPathField = !preferences.showPathField) },
                            )
                            FieldChip(
                                label = stringResource(id = R.string.played_progress),
                                selected = preferences.showPlayedProgress,
                                onClick = { preferences = preferences.copy(showPlayedProgress = !preferences.showPlayedProgress) },
                            )
                            FieldChip(
                                label = stringResource(id = R.string.resolution),
                                selected = preferences.showResolutionField,
                                onClick = { preferences = preferences.copy(showResolutionField = !preferences.showResolutionField) },
                            )
                            FieldChip(
                                label = stringResource(id = R.string.size),
                                selected = preferences.showSizeField,
                                onClick = { preferences = preferences.copy(showSizeField = !preferences.showSizeField) },
                            )
                            FieldChip(
                                label = stringResource(id = R.string.thumbnail),
                                selected = preferences.showThumbnailField,
                                onClick = { preferences = preferences.copy(showThumbnailField = !preferences.showThumbnailField) },
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    HorizontalDivider(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 1.dp,
                    )

                    // --- 3. Dialog Footer (Pinned) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Cancel Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .bouncyPress()
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                ),
                            )
                        }

                        // Done Button (JioHotstar Blue Action Button)
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0072D2))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF38B6FF).copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .bouncyPress()
                                .clickable {
                                    updatePreferences(preferences)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = NextIcons.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.done),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Frosted Glass Segmented Control with active luminous pill and bouncy press.
 */
@Composable
private fun <T> GlassSegmentedRow(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: @Composable (T) -> String,
    itemIcon: (@Composable (T) -> ImageVector?)? = null,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131722))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem

            val bg = if (isSelected) {
                Color(0xFF0072D2).copy(alpha = 0.35f)
            } else {
                Color.Transparent
            }
            val borderBrush = if (isSelected) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF38B6FF).copy(alpha = 0.65f),
                        Color(0xFF0072D2).copy(alpha = 0.35f),
                    ),
                )
            } else {
                null
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .then(
                        if (borderBrush != null) {
                            Modifier.border(1.dp, borderBrush, RoundedCornerShape(10.dp))
                        } else {
                            Modifier
                        },
                    )
                    .bouncyPress()
                    .clickable { onItemSelected(item) }
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val icon = itemIcon?.invoke(item)
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else Color(0xFF828A9E),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = itemLabel(item),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF828A9E),
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Frosted Glass Filter Chip for display field toggles.
 */
@Composable
fun FieldChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedIcon: ImageVector = NextIcons.CheckBox,
    unselectedIcon: ImageVector = NextIcons.CheckBoxOutline,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) Color(0xFF0072D2).copy(alpha = 0.30f) else Color(0xFF131722)
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF38B6FF).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
            )
            .bouncyPress()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (selected) Color(0xFF38B6FF) else Color(0xFF828A9E),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else Color(0xFF828A9E),
            ),
        )
    }
}

/**
 * Horizontal scrollable row of Frosted Glass Sort Option Pills.
 */
@Composable
private fun SortOptions(
    selectedSortBy: Sort.By,
    onOptionSelected: (Sort.By) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        GlassSortOptionChip(
            text = stringResource(id = R.string.title),
            icon = NextIcons.Title,
            isSelected = selectedSortBy == Sort.By.TITLE,
            onClick = { onOptionSelected(Sort.By.TITLE) },
        )
        GlassSortOptionChip(
            text = stringResource(id = R.string.duration),
            icon = NextIcons.Length,
            isSelected = selectedSortBy == Sort.By.LENGTH,
            onClick = { onOptionSelected(Sort.By.LENGTH) },
        )
        GlassSortOptionChip(
            text = stringResource(id = R.string.date),
            icon = NextIcons.Calendar,
            isSelected = selectedSortBy == Sort.By.DATE,
            onClick = { onOptionSelected(Sort.By.DATE) },
        )
        GlassSortOptionChip(
            text = stringResource(id = R.string.size),
            icon = NextIcons.Size,
            isSelected = selectedSortBy == Sort.By.SIZE,
            onClick = { onOptionSelected(Sort.By.SIZE) },
        )
        GlassSortOptionChip(
            text = stringResource(id = R.string.location),
            icon = NextIcons.Location,
            isSelected = selectedSortBy == Sort.By.PATH,
            onClick = { onOptionSelected(Sort.By.PATH) },
        )
    }
}

@Composable
private fun GlassSortOptionChip(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) Color(0xFF0072D2).copy(alpha = 0.35f) else Color(0xFF131722)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF38B6FF).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
            )
            .bouncyPress()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp),
            tint = if (isSelected) Color.White else Color(0xFF828A9E),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF828A9E),
            ),
        )
    }
}

@Composable
private fun DialogSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun GlassDivider(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    HorizontalDivider(
        modifier = modifier.padding(top = 16.dp),
        color = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        thickness = 1.dp,
    )
}

@Preview
@Composable
fun QuickSettingsPreview() {
    Surface {
        QuickSettingsDialog(
            applicationPreferences = ApplicationPreferences(),
            onDismiss = { },
            updatePreferences = {},
            backdrop = null,
        )
    }
}
