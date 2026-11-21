package dev.roxy.kryptonplayer.feature.videopicker.composables

import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import dev.roxy.kryptonplayer.core.common.Utils
import dev.roxy.kryptonplayer.core.model.ApplicationPreferences
import dev.roxy.kryptonplayer.core.model.Folder
import dev.roxy.kryptonplayer.core.model.MediaLayoutMode
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.FolderShape
import dev.roxy.kryptonplayer.core.ui.components.tvFocusable
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme

@Composable
fun FolderItem(
    folder: Folder,
    isRecentlyPlayedFolder: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val focusableModifier = modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
    when (preferences.mediaLayoutMode) {
        MediaLayoutMode.LIST -> FolderListItem(
            folder = folder,
            isRecentlyPlayedFolder = isRecentlyPlayedFolder,
            preferences = preferences,
            modifier = focusableModifier,
            isFirstItem = isFirstItem,
            isLastItem = isLastItem,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick,
        )
        MediaLayoutMode.GRID -> FolderGridItem(
            folder = folder,
            isRecentlyPlayedFolder = isRecentlyPlayedFolder,
            preferences = preferences,
            modifier = focusableModifier,
            isFirstItem = isFirstItem,
            isLastItem = isLastItem,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FolderListItem(
    folder: Folder,
    isRecentlyPlayedFolder: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "FolderListItemScale"
    )

    val border = if (isRecentlyPlayedFolder && preferences.markLastPlayedMedia) {
        BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                )
            )
        )
    } else {
        BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.06f),
                )
            )
        )
    }

    val containerColor = if (selected) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
    } else if (isRecentlyPlayedFolder && preferences.markLastPlayedMedia) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder Single First-Video Preview inside FolderShape (Nosved Player style)
            FolderMediaPreview(
                folder = folder,
                isRecentlyPlayedFolder = isRecentlyPlayedFolder,
                preferences = preferences,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(min(115.dp, LocalConfiguration.current.screenWidthDp.dp * 0.32f))
            )

            // Info column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = folder.name,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    overflow = TextOverflow.Ellipsis,
                    color = if (isRecentlyPlayedFolder && preferences.markLastPlayedMedia) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (preferences.showPathField) {
                    Text(
                        text = folder.path.substringBeforeLast("/"),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (folder.mediaList.isNotEmpty()) {
                        InfoChip(
                            text = "${folder.mediaList.size} " +
                                    stringResource(id = R.string.video.takeIf { folder.mediaList.size == 1 } ?: R.string.videos),
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (folder.folderList.isNotEmpty()) {
                        InfoChip(
                            text = "${folder.folderList.size} " +
                                    stringResource(id = R.string.folder.takeIf { folder.folderList.size == 1 } ?: R.string.folders),
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (preferences.showSizeField) {
                        InfoChip(
                            text = Utils.formatFileSize(folder.mediaSize),
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FolderGridItem(
    folder: Folder,
    isRecentlyPlayedFolder: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "FolderGridItemScale"
    )

    val border = if (isRecentlyPlayedFolder && preferences.markLastPlayedMedia) {
        BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                )
            )
        )
    } else {
        BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.06f),
                )
            )
        )
    }

    val containerColor = if (selected) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
    } else if (isRecentlyPlayedFolder && preferences.markLastPlayedMedia) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f)
    }

    Card(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Folder Single First-Video Preview inside FolderShape (Nosved Player style)
            FolderMediaPreview(
                folder = folder,
                isRecentlyPlayedFolder = isRecentlyPlayedFolder,
                preferences = preferences,
                modifier = Modifier
                    .fillMaxWidth()
                    .width(min(140.dp, LocalConfiguration.current.screenWidthDp.dp * 0.42f))
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = folder.name,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    overflow = TextOverflow.Ellipsis,
                    color = if (isRecentlyPlayedFolder && preferences.markLastPlayedMedia) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                )

                val mediaCount = if (folder.mediaList.isNotEmpty()) {
                    "${folder.mediaList.size} " + stringResource(id = R.string.video.takeIf { folder.mediaList.size == 1 } ?: R.string.videos)
                } else {
                    null
                }
                val folderCount = if (folder.folderList.isNotEmpty()) {
                    "${folder.folderList.size} " + stringResource(id = R.string.folder.takeIf { folder.folderList.size == 1 } ?: R.string.folders)
                } else {
                    null
                }

                Text(
                    text = buildString {
                        mediaCount?.let {
                            append(it)
                            folderCount?.let {
                                append(", ")
                                append("\u00A0")
                            }
                        }
                        folderCount?.let {
                            append(it)
                        }
                    },
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FolderMediaPreview(
    folder: Folder,
    isRecentlyPlayedFolder: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
) {
    val firstVideo = remember(folder) { folder.allMediaList.firstOrNull() }
    val context = LocalContext.current
    val folderShape = remember { FolderShape(cornerRadiusDp = 10f) }

    Box(
        modifier = modifier
            .aspectRatio(16f / 11f)
            .clip(folderShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (firstVideo == null || !preferences.showThumbnailField) {
            // Sleek fallback folder presentation matching Nosved Player
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.folder_thumb),
                    contentDescription = null,
                    tint = if (isRecentlyPlayedFolder && preferences.markLastPlayedMedia) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f)
                    },
                    modifier = Modifier.size(36.dp),
                )
            }
        } else {
            // First video hero thumbnail (Nosved Player single-thumbnail concept)
            val thumbData = firstVideo.artworkUri ?: firstVideo.uriString
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbData)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Subtle layered bottom gradient vignette (Nosved Player style)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.50f),
                        )
                    ),
            )

            // Video count badge at top-left
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.folder_thumb),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(10.dp),
                    )
                    Text(
                        text = "${folder.allMediaList.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Duration chip at bottom-right
        if (preferences.showDurationField && folder.mediaDuration > 0) {
            InfoChip(
                text = Utils.formatDurationMillis(folder.mediaDuration),
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.BottomEnd),
                backgroundColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
                shape = MaterialTheme.shapes.extraSmall,
            )
        }
    }
}

@PreviewLightDark
@Composable
fun FolderItemRecentlyPlayedPreview() {
    KryptonPlayerTheme {
        FolderListItem(
            folder = Folder.sample,
            preferences = ApplicationPreferences(),
            isRecentlyPlayedFolder = true,
        )
    }
}

@PreviewLightDark
@Composable
fun FolderItemPreview() {
    KryptonPlayerTheme {
        FolderListItem(
            folder = Folder.sample.copy(folderList = listOf(Folder.sample)),
            preferences = ApplicationPreferences(),
            isRecentlyPlayedFolder = false,
        )
    }
}

@PreviewLightDark
@Composable
fun FolderGridViewPreview() {
    KryptonPlayerTheme {
        FolderGridItem(
            folder = Folder.sample.copy(folderList = listOf(Folder.sample)),
            preferences = ApplicationPreferences(),
            isRecentlyPlayedFolder = true,
        )
    }
}
