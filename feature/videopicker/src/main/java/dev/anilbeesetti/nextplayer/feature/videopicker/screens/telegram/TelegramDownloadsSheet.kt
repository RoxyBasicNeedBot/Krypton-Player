@file:OptIn(ExperimentalMaterial3Api::class)

package dev.anilbeesetti.nextplayer.feature.videopicker.screens.telegram

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.core.data.download.DownloadTask
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.ui.components.bouncyPress
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramDownload
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.PlatformBackdrop
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.drawInteractiveGlass
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.liquidGlassBorderLaser
import dev.anilbeesetti.nextplayer.core.ui.theme.NextColors

/**
 * Downloads sheet modeled directly after YT-DLP-Nis (C:\Users\vip\OneDrive\Documents\ytdlnis)
 * rendered in premium Liquid Glass.
 *
 * Features:
 * - 3 Tabs: Processing (Running), In Queue, and Completed (Saved) with live count badges
 * - YT-DLP-Nis active download card: Large 16:9 thumbnail, progress tint and glowing linear bar over thumbnail
 * - High-contrast monospace telemetry: Exact downloaded/total bytes, percentage, speed, accurate ETA
 * - Liquid glass frosted surfaces, laser glows, and responsive bouncing controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramDownloadsSheet(
    viewModel: TelegramViewModel,
    backdrop: PlatformBackdrop? = null,
    onDismiss: () -> Unit,
    onPlayVideo: (Uri, String) -> Unit,
) {
    val activeTasksMap by viewModel.activeDownloadTasks.collectAsState()
    val allActiveTasks = remember(activeTasksMap) { activeTasksMap.values.distinctBy { it.key } }

    val processingTasks = remember(allActiveTasks) {
        allActiveTasks.filter { !it.status.contains("Queued", ignoreCase = true) }
    }
    val queuedTasks = remember(allActiveTasks) {
        allActiveTasks.filter { it.status.contains("Queued", ignoreCase = true) }
    }
    val completedVideos by viewModel.downloadedVideos.collectAsState()
    val distinctCompletedVideos = remember(completedVideos) {
        completedVideos.distinctBy { it.path }.distinctBy { it.id }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(if (allActiveTasks.isEmpty() && distinctCompletedVideos.isNotEmpty()) 2 else 0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasAnyProcessing = processingTasks.any { !it.isPaused }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFF111319),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Mass Actions in Liquid Glass
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0xFF2AABEE).copy(alpha = 0.4f))
                            .clip(CircleShape)
                            .background(Color(0xFF2AABEE).copy(alpha = 0.20f))
                            .liquidGlassBorderLaser(
                                laserColor = Color(0xFF2AABEE),
                                coreColor = Color(0xFFE0F7FA),
                                haloColor = Color(0xFF00B0FF),
                                speedMillis = 3600,
                                strokeWidth = 1.2.dp,
                                glowWidth = 3.2.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (processingTasks.isNotEmpty()) {
                            LottieTelegramDownload(
                                size = 22.dp,
                                isPlaying = processingTasks.any { !it.isPaused },
                                tint = Color(0xFF2AABEE)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Download Queue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (processingTasks.isNotEmpty()) {
                            val totalSpeed = processingTasks.sumOf { it.speedBytesPerSec }
                            Text(
                                text = if (processingTasks.all { it.isPaused }) "All downloads paused" else "${formatSpeed(totalSpeed)} transfer speed",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (processingTasks.all { it.isPaused }) Color(0xFFFF9800) else Color(0xFF2AABEE)
                            )
                        }
                    }
                }

                if (processingTasks.isNotEmpty() || queuedTasks.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (processingTasks.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    if (hasAnyProcessing) viewModel.pauseAllDownloads() else viewModel.resumeAllDownloads()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    contentColor = if (hasAnyProcessing) Color(0xFFFF9800) else Color(0xFF2AABEE)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.bouncyPress()
                            ) {
                                Icon(
                                    imageVector = if (hasAnyProcessing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (hasAnyProcessing) "Pause All" else "Resume All", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.cancelAllDownloads() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.bouncyPress()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel All", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Tab Selector in Liquid Glass Pill style (Running | In Queue | Completed)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF161824))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LiquidGlassTabChip(
                        title = "Running",
                        count = processingTasks.size,
                        isSelected = selectedTab == 0,
                        activeColor = Color(0xFF2AABEE),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    LiquidGlassTabChip(
                        title = "In Queue",
                        count = queuedTasks.size,
                        isSelected = selectedTab == 1,
                        activeColor = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                    LiquidGlassTabChip(
                        title = "Completed",
                        count = distinctCompletedVideos.size,
                        isSelected = selectedTab == 2,
                        activeColor = Color(0xFF00E676),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 2 }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Running / Processing Tab (YT-DLP-Nis Liquid Glass Card)
                    if (processingTasks.isEmpty()) {
                        EmptyDownloadState(
                            imageVector = Icons.Rounded.Download,
                            title = "No downloads running",
                            subtitle = "Downloads will appear here with live speed, exact ETA, and progress."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(processingTasks, key = { index, task -> "${task.key}_$index" }) { _, task ->
                                YtdlnisLiquidGlassDownloadCard(
                                    task = task,
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    isQueued = false,
                                    onPauseResume = {
                                        if (task.isPaused) viewModel.resumeDownload(task.key) else viewModel.pauseDownload(task.key)
                                    },
                                    onCancel = { viewModel.cancelDownload(task.key) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // In Queue Tab
                    if (queuedTasks.isEmpty()) {
                        EmptyDownloadState(
                            imageVector = Icons.Default.HourglassEmpty,
                            title = "No downloads queued",
                            subtitle = "Files waiting for download slot will appear here."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(queuedTasks, key = { index, task -> "${task.key}_$index" }) { _, task ->
                                YtdlnisLiquidGlassDownloadCard(
                                    task = task,
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    isQueued = true,
                                    onPauseResume = {},
                                    onCancel = { viewModel.cancelDownload(task.key) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Completed (Saved) Tab
                    if (distinctCompletedVideos.isEmpty()) {
                        EmptyDownloadState(
                            imageVector = Icons.Default.DownloadDone,
                            title = "No completed downloads",
                            subtitle = "Videos downloaded offline will be saved and accessible here."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(distinctCompletedVideos, key = { index, video -> "${video.id}_${video.path}_$index" }) { _, video ->
                                CompletedLiquidGlassDownloadRow(
                                    video = video,
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    onPlay = {
                                        val playUri = viewModel.getPlayableUri(video)
                                        onPlayVideo(playUri, video.nameWithExtension)
                                    },
                                    onDelete = {
                                        viewModel.deleteFromOffline(video)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassTabChip(
    title: String,
    count: Int,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = spring(stiffness = 520f),
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = 520f),
        label = "tabText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .bouncyPress()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 12.sp
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) activeColor else Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$count",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Download item card matching YT-DLP-Nis (C:\Users\vip\OneDrive\Documents\ytdlnis)
 * rendered in Liquid Glass:
 * - Large 16:9 thumbnail background
 * - Progress indicator & tint overlaid directly on the thumbnail
 * - Monospace output telemetry: accurate bytes, speed, ETA
 * - Frosted circular action buttons with bouncy feedback
 */
@Composable
private fun YtdlnisLiquidGlassDownloadCard(
    task: DownloadTask,
    viewModel: TelegramViewModel,
    backdrop: PlatformBackdrop? = null,
    isQueued: Boolean = false,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit
) {
    val effectiveTotalBytes = when {
        task.totalBytes > 0L -> task.totalBytes
        (task.video?.size ?: 0L) > 0L -> task.video!!.size
        else -> 0L
    }
    val effectiveDownloadedBytes = when {
        task.downloadedBytes > 0L -> task.downloadedBytes
        effectiveTotalBytes > 0L && task.progress > 0f -> (task.progress * effectiveTotalBytes).toLong()
        else -> 0L
    }

    val progressFraction = when {
        effectiveTotalBytes > 0L -> (effectiveDownloadedBytes.toFloat() / effectiveTotalBytes.toFloat()).coerceIn(0f, 1f)
        task.progress > 0f -> task.progress.coerceIn(0f, 1f)
        else -> 0f
    }
    val percentStr = String.format(java.util.Locale.US, "%.1f%%", progressFraction * 100f)

    val speed = task.speedBytesPerSec
    val etaStr = calculateEta(effectiveDownloadedBytes, effectiveTotalBytes, speed)
    val speedStr = formatSpeed(speed)

    val matchedVideo = task.video ?: remember(task.key, task.title) {
        val cloudVideos = (viewModel.uiState.value as? TelegramUiState.Success)?.videos ?: emptyList()
        val chVideos = viewModel.channelVideos.value
        (cloudVideos + chVideos).find {
            it.nameWithExtension == task.title ||
            it.id.toString() == task.key ||
            it.externalId == task.key ||
            it.nameWithExtension == task.video?.nameWithExtension
        }
    }
    val effectiveVideo = task.video ?: matchedVideo

    val streamUri = try { Uri.parse(effectiveVideo?.uriString ?: "") } catch (e: Exception) { null }
    val pathUri = try { Uri.parse(effectiveVideo?.path ?: "") } catch (e: Exception) { null }
    val chatId = streamUri?.getQueryParameter("chatId")?.toLongOrNull() ?: pathUri?.getQueryParameter("chatId")?.toLongOrNull()
    val messageId = streamUri?.getQueryParameter("messageId")?.toLongOrNull() ?: pathUri?.getQueryParameter("messageId")?.toLongOrNull()
    val thumbFileId = streamUri?.getQueryParameter("thumbFileId")
        ?: pathUri?.getQueryParameter("thumbFileId")
        ?: (effectiveVideo?.artworkUri?.substringAfterLast("/api/thumbnail/")?.takeIf { it.isNotEmpty() && !it.startsWith("http") })
    val fallbackUrl = effectiveVideo?.artworkUri ?: effectiveVideo?.path?.takeIf { it.startsWith("http") }



    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.38f),
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .background(Color(0xFF13151D))
    ) {
        // 0. Placeholder Background (Dark Movie Tile)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF181A22), Color(0xFF0F1017))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            )
        }

        // 1. Full-bleed Video Thumbnail
        TelegramAsyncImage(
            thumbFileId = thumbFileId,
            chatId = chatId,
            messageId = messageId,
            fallbackUrl = fallbackUrl,
            contentDescription = "Thumbnail",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            tdlibMediaResolver = viewModel.tdlibMediaResolver,
            imageDiskCache = viewModel.imageDiskCache
        )

        // 2. YT-DLP-Nis Progress Fill Tint directly across the Thumbnail
        if (!isQueued && progressFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFraction)
                    .background(
                        if (task.isPaused) Color(0x40FF9800) else Color(0x3B2AABEE)
                    )
            )
        }

        // 3. Top Gradient Scrim for Chips and Action Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF008090D),
                            Color(0xA008090D),
                            Color.Transparent
                        )
                    )
                )
        )

        // 4. Bottom Gradient Scrim for Title & Monospace Telemetry
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xB308090D),
                            Color(0xF808090D)
                        )
                    )
                )
        )

        // 5. Content Layout (Top Bar & Bottom Telemetry)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Chips (Left) & Frosted Action Buttons (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xEE161822))
                            .border(NextColors.cardBorder(Color.White.copy(alpha = 0.20f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Status Badge (Downloading / Paused)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xEE161822))
                            .border(NextColors.cardBorder(if (task.isPaused) Color(0xFFFF9800).copy(alpha = 0.40f) else Color(0xFF2AABEE).copy(alpha = 0.40f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (task.isPaused) Icons.Filled.Pause else Icons.Rounded.Download,
                                contentDescription = null,
                                tint = if (task.isPaused) Color(0xFFFF9800) else Color(0xFF2AABEE),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (task.isPaused) "Paused" else "Downloading",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (task.isPaused) Color(0xFFFF9800) else Color(0xFF2AABEE)
                            )
                        }
                    }

                    // File Size Chip
                    if (effectiveTotalBytes > 0L) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xEE161822))
                                .border(NextColors.cardBorder(Color.White.copy(alpha = 0.14f)), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formatBytes(effectiveTotalBytes),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }

                // Right Action Buttons in Liquid Glass
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isQueued) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (task.isPaused) Color(0xFFFF9800).copy(alpha = 0.28f) else Color(0xEE161822)
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.08f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onPauseResume
                                )
                                .bouncyPress(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (task.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (task.isPaused) "Resume" else "Pause",
                                tint = if (task.isPaused) Color(0xFFFF9800) else Color(0xFF2AABEE),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.38f))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.30f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCancel
                            )
                            .bouncyPress(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom Area: Title and Monospace Telemetry (YT-DLP-Nis Layout)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.90f),
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 3f
                        )
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Exact Telemetry Line (ytdlnis monospace layout)
                if (isQueued) {
                    Text(
                        text = "Waiting in queue... • ${formatBytes(effectiveTotalBytes)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.95f),
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                blurRadius = 3f
                            )
                        ),
                        color = Color(0xFFFF9800),
                        fontSize = 11.sp
                    )
                } else {
                    val telemetryText = when {
                        task.isPaused ->
                            "[$percentStr] ${formatBytes(effectiveDownloadedBytes)} / ${formatBytes(effectiveTotalBytes)} • Paused"
                        task.status.startsWith("Finalizing") || task.status.startsWith("Saving") ->
                            "[$percentStr] ${formatBytes(effectiveDownloadedBytes)} / ${formatBytes(effectiveTotalBytes)} • Finalizing..."
                        effectiveTotalBytes > 0L ->
                            "[$percentStr] ${formatBytes(effectiveDownloadedBytes)} / ${formatBytes(effectiveTotalBytes)} • $speedStr • $etaStr"
                        else ->
                            "${formatBytes(effectiveDownloadedBytes)} • $speedStr"
                    }
                    Text(
                        text = telemetryText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.95f),
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                blurRadius = 3f
                            )
                        ),
                        color = if (task.isPaused) Color(0xFFFF9800) else Color(0xFF64B5F6),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 6. Glowing Progress Indicator Bar along the very bottom edge
        if (!isQueued) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (task.isPaused) Color(0xFFFF9800) else Color(0xFF2AABEE),
                trackColor = Color.White.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

/**
 * Row for Completed / Offline downloaded videos in Tab 2 with Liquid Glass styling.
 */
@Composable
private fun CompletedLiquidGlassDownloadRow(
    video: Video,
    viewModel: TelegramViewModel,
    backdrop: PlatformBackdrop? = null,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val streamUri = try { Uri.parse(video.uriString) } catch (e: Exception) { null }
    val chatId = streamUri?.getQueryParameter("chatId")?.toLongOrNull()
    val messageId = streamUri?.getQueryParameter("messageId")?.toLongOrNull()
    val thumbFileId = streamUri?.getQueryParameter("thumbFileId")
        ?: (video.artworkUri?.substringAfterLast("/api/thumbnail/")?.takeIf { it.isNotEmpty() && !it.startsWith("http") })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF161824))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onPlay)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Video Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 88.dp, height = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF14161F))
            ) {
                TelegramAsyncImage(
                    thumbFileId = thumbFileId,
                    chatId = chatId,
                    messageId = messageId,
                    fallbackUrl = video.artworkUri,
                    contentDescription = "Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    tdlibMediaResolver = viewModel.tdlibMediaResolver,
                    imageDiskCache = viewModel.imageDiskCache
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Title & Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.nameWithExtension,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Downloaded",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = video.formattedFileSize.ifEmpty { formatBytes(video.size) },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Delete button in Liquid Glass
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDelete
                    )
                    .bouncyPress(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Offline File",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyDownloadState(
    title: String,
    subtitle: String,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    drawableRes: Int? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageVector != null) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                } else if (drawableRes != null) {
                    Icon(
                        painter = painterResource(id = drawableRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// Telemetry & Format Helpers
private fun calculateEta(downloadedBytes: Long, totalBytes: Long, speedBytesPerSec: Long): String {
    if (speedBytesPerSec <= 0L || totalBytes <= 0L || downloadedBytes >= totalBytes) {
        return "ETA --:--"
    }
    val remainingBytes = totalBytes - downloadedBytes
    val etaSeconds = remainingBytes / speedBytesPerSec
    return when {
        etaSeconds < 60 -> String.format(java.util.Locale.US, "ETA 00:%02d", etaSeconds)
        etaSeconds < 3600 -> {
            val mins = etaSeconds / 60
            val secs = etaSeconds % 60
            String.format(java.util.Locale.US, "ETA %02d:%02d", mins, secs)
        }
        else -> {
            val hours = etaSeconds / 3600
            val mins = (etaSeconds % 3600) / 60
            String.format(java.util.Locale.US, "ETA %dh %02dm", hours, mins)
        }
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 KB/s"
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB/s", gb)
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB/s", mb)
        else -> String.format(java.util.Locale.US, "%.0f KB/s", kb)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}
