@file:OptIn(ExperimentalMaterial3Api::class)

package dev.anilbeesetti.nextplayer.feature.videopicker.screens.telegram

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.ui.res.painterResource
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramDownload
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import dev.anilbeesetti.nextplayer.core.ui.theme.NextColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.PlatformBackdrop
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.drawInteractiveGlass
import dev.anilbeesetti.nextplayer.core.data.download.DownloadTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingDownloadBar(
    viewModel: TelegramViewModel,
    modifier: Modifier = Modifier,
    backdrop: PlatformBackdrop? = null,
) {
    val activeTasksMap by viewModel.activeDownloadTasks.collectAsState()
    val activeTasks = remember(activeTasksMap) { activeTasksMap.values.toList() }
    var showDetailsSheet by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = activeTasks.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (activeTasks.isEmpty()) return@AnimatedVisibility

        val firstTask = activeTasks.first()
        val totalSpeed = activeTasks.sumOf { it.speedBytesPerSec }
        val avgProgress = if (activeTasks.isNotEmpty()) {
            activeTasks.map { it.progress }.average().toFloat().coerceIn(0.01f, 1f)
        } else 0.01f

        val hasAnyActive = activeTasks.any { !it.isPaused }
        val allPaused = activeTasks.all { it.isPaused }

        val isSavingToGallery = firstTask.status.startsWith("Saving to Gallery")
        val titleText = if (isSavingToGallery) {
            "Saving to Gallery"
        } else if (activeTasks.size == 1) {
            firstTask.title
        } else if (allPaused) {
            "Paused: ${activeTasks.size} downloads"
        } else {
            "Downloading ${activeTasks.size} files (${(avgProgress * 100).toInt()}%)"
        }

        val speedText = if (allPaused) "Paused" else formatDownloadSpeed(totalSpeed)
        val progressPercentText = if (allPaused) "PAUSED" else "${(avgProgress * 100).toInt()}%"
        val detailText = when {
            allPaused && activeTasks.size == 1 && firstTask.totalBytes > 0 ->
                "${formatDownloadBytes(firstTask.downloadedBytes)} / ${formatDownloadBytes(firstTask.totalBytes)} • Paused"
            allPaused -> "All downloads paused • Tap to manage"
            isSavingToGallery && firstTask.totalBytes > 0 ->
                "${formatDownloadBytes(firstTask.downloadedBytes)} / ${formatDownloadBytes(firstTask.totalBytes)} • ${if (firstTask.source == "Device Gallery") "Saving on device" else "Downloading for Gallery"}"
            activeTasks.size == 1 && firstTask.totalBytes > 0 ->
                "${formatDownloadBytes(firstTask.downloadedBytes)} / ${formatDownloadBytes(firstTask.totalBytes)} • $speedText"
            else -> "$speedText • Tap to manage queue"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .then(
                    if (backdrop != null) {
                        Modifier.drawInteractiveGlass(
                            isDark = true,
                            backdrop = backdrop,
                            layer = null,
                            luminance = { 0.5f },
                            shape = RoundedCornerShape(18.dp),
                            interaction = null,
                        )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xEE161822),
                                        Color(0xFA0F1017),
                                    ),
                                ),
                            )
                    }
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                    shape = RoundedCornerShape(18.dp),
                )
                .clickable { showDetailsSheet = true }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { avgProgress },
                            modifier = Modifier.size(34.dp),
                            strokeWidth = 2.5.dp,
                            color = if (allPaused) Color(0xFFFF9800) else Color(0xFF2AABEE),
                            trackColor = (if (allPaused) Color(0xFFFF9800) else Color(0xFF2AABEE)).copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                        if (allPaused) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = "Paused",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            LottieTelegramDownload(
                                size = 20.dp,
                                isPlaying = true,
                                tint = Color(0xFF2AABEE)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = progressPercentText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (allPaused) Color(0xFFFF9800) else Color(0xFF2AABEE)
                            )
                        }

                        Text(
                            text = detailText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                if (hasAnyActive) {
                                    viewModel.pauseAllDownloads()
                                } else {
                                    viewModel.resumeAllDownloads()
                                }
                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (hasAnyActive) MaterialTheme.colorScheme.surfaceContainerHighest else Color(0xFFFF9800).copy(alpha = 0.15f),
                                contentColor = if (hasAnyActive) Color(0xFF2AABEE) else Color(0xFFFF9800)
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (hasAnyActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (hasAnyActive) "Pause All" else "Resume All",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { showDetailsSheet = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Expand Downloads",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { avgProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = if (allPaused) Color(0xFFFF9800) else Color(0xFF2AABEE),
                    trackColor = (if (allPaused) Color(0xFFFF9800) else Color(0xFF2AABEE)).copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }

    // Detailed Active Downloads Bottom Sheet
    if (showDetailsSheet) {
        val hasAnyActive = activeTasks.any { !it.isPaused }
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = Color(0xFA14151D),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Active Downloads (${activeTasks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (activeTasks.isNotEmpty()) {
                            Text(
                                text = if (activeTasks.all { it.isPaused }) "All downloads paused" else formatDownloadSpeed(activeTasks.sumOf { it.speedBytesPerSec }),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTasks.all { it.isPaused }) Color(0xFFFF9800) else Color(0xFF2AABEE)
                            )
                        }
                    }

                    if (activeTasks.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (hasAnyActive) viewModel.pauseAllDownloads() else viewModel.resumeAllDownloads()
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasAnyActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (hasAnyActive) "Pause All" else "Resume All", fontSize = 11.5.sp)
                            }

                            FilledTonalButton(
                                onClick = { viewModel.cancelAllDownloads() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel All", fontSize = 11.5.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                if (activeTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All downloads completed!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val distinctTasks = remember(activeTasks) { activeTasks.distinctBy { it.key } }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(distinctTasks, key = { index, task -> "${task.key}_$index" }) { _, task ->
                            CardDownloadTaskRow(
                                task = task,
                                onPauseResume = {
                                    if (task.isPaused) {
                                        viewModel.resumeDownload(task.key)
                                    } else {
                                        viewModel.pauseDownload(task.key)
                                    }
                                },
                                onCancel = { viewModel.cancelDownload(task.key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardDownloadTaskRow(
    task: DownloadTask,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = NextColors.cardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background((if (task.isPaused) Color(0xFFFF9800) else Color(0xFF2AABEE)).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.isPaused) Icons.Filled.Pause else Icons.Default.Movie,
                        contentDescription = null,
                        tint = if (task.isPaused) Color(0xFFFF9800) else Color(0xFF2AABEE),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!task.isPaused) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2AABEE).copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF2AABEE),
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    text = "Downloading",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2AABEE)
                                )
                            }
                        }
                        if (task.isPaused) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Pause,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    text = "Paused",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                        Text(
                            text = if (task.isPaused) {
                                "${formatDownloadBytes(task.downloadedBytes)} / ${formatDownloadBytes(task.totalBytes)}"
                            } else {
                                "${formatDownloadBytes(task.downloadedBytes)} / ${formatDownloadBytes(task.totalBytes)} • ${formatDownloadSpeed(task.speedBytesPerSec)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pause / Resume Button
                    FilledTonalIconButton(
                        onClick = onPauseResume,
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (task.isPaused) Color(0xFFFF9800).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (task.isPaused) Color(0xFFFF9800) else Color(0xFF2AABEE)
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (task.isPaused) "Resume Download" else "Pause Download",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Cancel Button ('X')
                    FilledTonalIconButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Download",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (task.isPaused) Color(0xFFFF9800) else Color(0xFF2AABEE),
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

private fun formatDownloadSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 KB/s"
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.1f MB/s", mb)
        else -> String.format("%.0f KB/s", kb)
    }
}

private fun formatDownloadBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}
