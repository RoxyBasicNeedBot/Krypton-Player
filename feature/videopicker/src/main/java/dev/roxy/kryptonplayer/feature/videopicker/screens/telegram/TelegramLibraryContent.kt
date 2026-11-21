package dev.roxy.kryptonplayer.feature.videopicker.screens.telegram

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.material.icons.rounded.Download
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramDownload
import dev.roxy.kryptonplayer.core.data.download.DownloadErrorInfo
import dev.roxy.kryptonplayer.core.data.download.DownloadTask
import dev.roxy.kryptonplayer.core.model.Video
import dev.roxy.kryptonplayer.core.ui.components.LottieHeartLikeButton
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramDelete
// LottieTelegramDownload removed
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.theme.NextColors

enum class LibraryFilter {
    ALL,
    LIKED,
    DOWNLOADS,
    WATCH_LATER
}

enum class LibrarySortType {
    DATE_ADDED,
    SIZE,
    NAME
}

enum class LibraryViewMode {
    GRID,
    LIST
}

@Composable
fun TelegramLibraryContent(
    viewModel: TelegramViewModel,
    onPlayVideo: (android.net.Uri, String, String?, String?) -> Unit,
    onReelsClick: (List<Video>, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var sortType by remember { mutableStateOf(LibrarySortType.DATE_ADDED) }
    var sortDescending by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf(LibraryViewMode.GRID) }

    val likedVideos by viewModel.likedVideos.collectAsState()
    val watchLaterItems by viewModel.watchLaterQueue.collectAsState(initial = emptyList())
    val downloadedVideos by viewModel.downloadedVideos.collectAsState()
    val downloadedKeys by viewModel.downloadedKeys.collectAsState()
    val activeDownloadTasks by viewModel.activeDownloadTasks.collectAsState()
    val failedDownloads by viewModel.failedDownloads.collectAsState()

    val activeTasksList = remember(activeDownloadTasks) { activeDownloadTasks.values.toList() }
    val failedTasksList = remember(failedDownloads) { failedDownloads.values.toList() }
    var activeErrorDialogInfo by remember { mutableStateOf<DownloadErrorInfo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshDownloadedVideos()
    }

    // Filter displayed media: Library contains user's personal collections: Liked, Offline, Watch Later
    val rawVideos = remember(selectedFilter, likedVideos, downloadedVideos, watchLaterItems) {
        val watchLaterAsVideos = watchLaterItems.map { item ->
            Video(
                id = item.id,
                path = item.localFilePath ?: item.streamUrl,
                parentPath = "Watch Later",
                duration = item.durationSeconds * 1000L,
                uriString = item.streamUrl,
                nameWithExtension = item.title,
                width = 0,
                height = 0,
                size = item.fileSize,
                dateModified = item.addedAt,
                formattedDuration = "",
                formattedFileSize = "",
                artworkUri = item.artworkUri,
                externalId = item.fileId,
                isImage = false
            )
        }
        when (selectedFilter) {
            LibraryFilter.ALL -> (downloadedVideos + likedVideos + watchLaterAsVideos).distinctBy { it.externalId ?: it.path }
            LibraryFilter.LIKED -> likedVideos
            LibraryFilter.DOWNLOADS -> downloadedVideos.distinctBy { it.path }.distinctBy { it.id }
            LibraryFilter.WATCH_LATER -> watchLaterAsVideos
        }
    }

    // Sort displayed media (ArchiveTune parity: natural ascending sortedBy with .asReversed() when sortDescending)
    val displayedVideos = remember(rawVideos, sortType, sortDescending) {
        val sorted = when (sortType) {
            LibrarySortType.DATE_ADDED -> rawVideos.sortedBy { if (it.dateModified > 0) it.dateModified else it.id }
            LibrarySortType.SIZE -> rawVideos.sortedBy { it.size }
            LibrarySortType.NAME -> rawVideos.sortedBy { it.nameWithExtension.lowercase() }
        }
        if (sortDescending) sorted.asReversed() else sorted
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- TOP CHIPS BAR (ARCHIVETUNE STYLE) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == LibraryFilter.ALL,
                    onClick = { selectedFilter = LibraryFilter.ALL },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        val totalCount = remember(downloadedVideos, likedVideos, watchLaterItems) {
                            (downloadedVideos.map { it.path } + likedVideos.map { it.path } + watchLaterItems.map { it.localFilePath ?: it.streamUrl }).distinct().size
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("All ($totalCount)")
                            if (activeTasksList.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = if (selectedFilter == LibraryFilter.ALL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${activeTasksList.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = null
                )

                FilterChip(
                    selected = selectedFilter == LibraryFilter.LIKED,
                    onClick = { selectedFilter = if (selectedFilter == LibraryFilter.LIKED) LibraryFilter.ALL else LibraryFilter.LIKED },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Liked (${likedVideos.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF2D55),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = null
                )

                FilterChip(
                    selected = selectedFilter == LibraryFilter.DOWNLOADS,
                    onClick = { selectedFilter = if (selectedFilter == LibraryFilter.DOWNLOADS) LibraryFilter.ALL else LibraryFilter.DOWNLOADS },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Downloads (${downloadedVideos.size})")
                            if (activeTasksList.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = if (selectedFilter == LibraryFilter.DOWNLOADS) Color.White else Color(0xFF00B0FF)
                                    )
                                    Text(
                                        text = "${activeTasksList.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (failedTasksList.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = if (selectedFilter == LibraryFilter.DOWNLOADS) Color.White else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "${failedTasksList.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00B0FF),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = null
                )

                FilterChip(
                    selected = selectedFilter == LibraryFilter.WATCH_LATER,
                    onClick = { selectedFilter = if (selectedFilter == LibraryFilter.WATCH_LATER) LibraryFilter.ALL else LibraryFilter.WATCH_LATER },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Watch Later (${watchLaterItems.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF9800),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = null
                )
            }

            // --- MAIN LIBRARY LAZY VERTICAL GRID (SUPPORTS BOTH GRID & LIST VIEWS) ---
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // ARCHIVETUNE 2-COLUMN PINNED SHORTCUT GRID
                if (selectedFilter == LibraryFilter.ALL) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Row 1: Liked & Downloads
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                LibraryPinnedCollectionTile(
                                    title = "Liked Media",
                                    subtitle = "${likedVideos.size} items",
                                    icon = Icons.Default.Favorite,
                                    accentColor = Color(0xFFFF2D55),
                                    onClick = { selectedFilter = LibraryFilter.LIKED },
                                    modifier = Modifier.weight(1f)
                                )

                                val downloadSubtitle = if (activeTasksList.isNotEmpty()) {
                                    "${downloadedVideos.size} saved • ${activeTasksList.size} queued"
                                } else {
                                    "${downloadedVideos.size} saved"
                                }
                                LibraryPinnedCollectionTile(
                                    title = "Downloads",
                                    subtitle = downloadSubtitle,
                                    painter = painterResource(id = NextIcons.TelegramCloudDone),
                                    accentColor = Color(0xFF00E5FF),
                                    onClick = { selectedFilter = LibraryFilter.DOWNLOADS },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 2: Watch Later Queue
                            LibraryPinnedCollectionTile(
                                title = "Watch Later Queue",
                                subtitle = "${watchLaterItems.size} in queue",
                                icon = Icons.Default.Schedule,
                                accentColor = Color(0xFFFF9800),
                                onClick = { selectedFilter = LibraryFilter.WATCH_LATER },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ARCHIVETUNE CONTROLS ROW (SORT SPLIT BUTTON & VIEW MODE TOGGLE)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibraryControlCard(
                        sortType = sortType,
                        sortDescending = sortDescending,
                        viewMode = viewMode,
                        onSortTypeChange = { sortType = it },
                        onSortDescendingChange = { sortDescending = it },
                        onViewModeToggle = {
                            viewMode = if (viewMode == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
                        }
                    )
                }

                // ACTIVE & QUEUED DOWNLOADS SECTION
                if ((selectedFilter == LibraryFilter.DOWNLOADS || selectedFilter == LibraryFilter.ALL) && activeTasksList.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "Downloading & Queue (${activeTasksList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(activeTasksList, key = { index, task -> "active_${task.key}_$index" }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { _, task ->
                        LibraryDownloadTaskCard(
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

                // FAILED DOWNLOADS (with Diagnostic Logs dialog & Retry trigger)
                if ((selectedFilter == LibraryFilter.DOWNLOADS || selectedFilter == LibraryFilter.ALL) && failedTasksList.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Failed Downloads (${failedTasksList.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF3B30)
                            )
                        }
                    }

                    itemsIndexed(failedTasksList, key = { index, errInfo -> "failed_${errInfo.key}_$index" }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { _, errInfo ->
                        LibraryFailedDownloadCard(
                            errorInfo = errInfo,
                            onViewLogs = { activeErrorDialogInfo = errInfo },
                            onRetry = { viewModel.retryFailedDownload(errInfo.key) },
                            onDismiss = { viewModel.dismissFailedDownload(errInfo.key) }
                        )
                    }
                }

                // HERO GRADIENT BANNERS
                when (selectedFilter) {
                    LibraryFilter.ALL -> {
                        if (rawVideos.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ArchiveTuneHeroBanner(
                                    title = "Saved Collection",
                                    subtitle = "${rawVideos.size} saved & liked videos",
                                    gradient = listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
                                    icon = Icons.Default.VideoLibrary,
                                    onPlayAll = {
                                        val first = rawVideos.first()
                                        onPlayVideo(viewModel.getPlayableUri(first), first.nameWithExtension, null, null)
                                    },
                                    onReelsMode = {
                                        onReelsClick(rawVideos, 0, "Saved Collection")
                                    }
                                )
                            }
                        }
                    }
                    LibraryFilter.LIKED -> {
                        if (likedVideos.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ArchiveTuneHeroBanner(
                                    title = "Liked Posts & Media",
                                    subtitle = "${likedVideos.size} items in your favorite collection",
                                    gradient = listOf(Color(0xFFFF2D55), Color(0xFFFF5E3A)),
                                    icon = Icons.Default.Favorite,
                                    onPlayAll = {
                                        val first = likedVideos.first()
                                        onPlayVideo(viewModel.getPlayableUri(first), first.nameWithExtension, null, null)
                                    },
                                    onReelsMode = {
                                        onReelsClick(likedVideos, 0, "Liked Posts")
                                    }
                                )
                            }
                        }
                    }
                    LibraryFilter.DOWNLOADS -> {
                        if (downloadedVideos.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ArchiveTuneHeroBanner(
                                    title = "Downloads",
                                    subtitle = "${downloadedVideos.size} videos ready for instant playback",
                                    gradient = listOf(Color(0xFF6200EE), Color(0xFF00E5FF)),
                                    icon = Icons.Default.Download,
                                    onPlayAll = {
                                        val first = downloadedVideos.first()
                                        onPlayVideo(viewModel.getPlayableUri(first), first.nameWithExtension, null, null)
                                    },
                                    onReelsMode = {
                                        onReelsClick(downloadedVideos, 0, "Downloads")
                                    }
                                )
                            }
                        }
                    }
                    LibraryFilter.WATCH_LATER -> {
                        if (rawVideos.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ArchiveTuneHeroBanner(
                                    title = "Watch Later Queue",
                                    subtitle = "${rawVideos.size} videos queued for later playback",
                                    gradient = listOf(Color(0xFFE65100), Color(0xFFFFB74D)),
                                    icon = Icons.Default.Schedule,
                                    onPlayAll = {
                                        val first = rawVideos.first()
                                        onPlayVideo(viewModel.getPlayableUri(first), first.nameWithExtension, null, null)
                                    },
                                    onReelsMode = {
                                        onReelsClick(rawVideos, 0, "Watch Later")
                                    }
                                )
                            }
                        }
                    }
                }

                // EMPTY STATE
                if (displayedVideos.isEmpty() && (selectedFilter != LibraryFilter.DOWNLOADS || (activeTasksList.isEmpty() && failedTasksList.isEmpty()))) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = when (selectedFilter) {
                                        LibraryFilter.LIKED -> Icons.Default.Favorite
                                        LibraryFilter.DOWNLOADS -> Icons.Default.Download
                                        LibraryFilter.WATCH_LATER -> Icons.Default.Schedule
                                        else -> Icons.Outlined.FolderZip
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = when (selectedFilter) {
                                        LibraryFilter.LIKED -> "No liked posts yet"
                                        LibraryFilter.DOWNLOADS -> "No downloaded videos"
                                        LibraryFilter.WATCH_LATER -> "Watch later queue is empty"
                                        else -> "Library is empty"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = when (selectedFilter) {
                                        LibraryFilter.DOWNLOADS -> "Download any video from My Cloud or Channels to watch anytime!"
                                        else -> "Like videos in Reels or channels to see them here!"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // MEDIA ITEMS: GRID MODE VS LIST MODE
                if (viewMode == LibraryViewMode.GRID) {
                    itemsIndexed(displayedVideos, key = { index, video -> "${video.path}_${video.id}_$index" }) { _, video ->
                        val isDownloaded = viewModel.isVideoDownloaded(video, downloadedKeys)
                        ArchiveTuneMediaCard(
                            video = video,
                            isLiked = viewModel.isLiked(video.externalId ?: video.path),
                            isDownloaded = isDownloaded,
                            onToggleLike = { viewModel.toggleLike(video) },
                            onDeleteOffline = if (selectedFilter == LibraryFilter.DOWNLOADS || isDownloaded) {
                                { viewModel.deleteFromOffline(video) }
                            } else null,
                            onClick = {
                                onPlayVideo(viewModel.getPlayableUri(video), video.nameWithExtension, null, null)
                            }
                        )
                    }
                } else {
                    // LIST MODE (ARCHIVETUNE FEATURE CARD STYLE)
                    itemsIndexed(displayedVideos, key = { index, video -> "${video.path}_${video.id}_$index" }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { _, video ->
                        val isDownloaded = viewModel.isVideoDownloaded(video, downloadedKeys)
                        ArchiveTuneFeatureCard(
                            video = video,
                            isLiked = viewModel.isLiked(video.externalId ?: video.path),
                            isDownloaded = isDownloaded,
                            onToggleLike = { viewModel.toggleLike(video) },
                            onDeleteOffline = if (selectedFilter == LibraryFilter.DOWNLOADS || isDownloaded) {
                                { viewModel.deleteFromOffline(video) }
                            } else null,
                            onClick = {
                                onPlayVideo(viewModel.getPlayableUri(video), video.nameWithExtension, null, null)
                            }
                        )
                    }
                }
            }
        }

        if (activeErrorDialogInfo != null) {
            DownloadErrorDiagnosticsDialog(
                errorInfo = activeErrorDialogInfo!!,
                onDismiss = { activeErrorDialogInfo = null },
                onRetry = {
                    val key = activeErrorDialogInfo!!.key
                    activeErrorDialogInfo = null
                    viewModel.retryFailedDownload(key)
                }
            )
        }
    }
}

@Composable
private fun LibraryPinnedCollectionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .border(
                NextColors.cardBorder(accentColor.copy(alpha = 0.25f)),
                RoundedCornerShape(20.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.3f),
                        )
                    )
                )
        ) {
            Icon(
                painter = painterResource(id = NextIcons.TelegramPin),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(16.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                    shape = CircleShape,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        } else if (painter != null) {
                            Icon(
                                painter = painter,
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryControlCard(
    sortType: LibrarySortType,
    sortDescending: Boolean,
    viewMode: LibraryViewMode,
    onSortTypeChange: (LibrarySortType) -> Unit,
    onSortDescendingChange: (Boolean) -> Unit,
    onViewModeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val sortDirectionRotation by animateFloatAsState(
        targetValue = if (sortDescending) 0f else 180f,
        label = "LibrarySortDirection"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ArchiveTune-style Split Button Layout for Sort
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Leading split button: Sort dropdown trigger
                Surface(
                    onClick = { menuExpanded = true },
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = NextColors.cardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when (sortType) {
                                LibrarySortType.DATE_ADDED -> "Date Added"
                                LibrarySortType.SIZE -> "File Size"
                                LibrarySortType.NAME -> "Name"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Trailing split button: Sort direction toggle
                Surface(
                    onClick = { onSortDescendingChange(!sortDescending) },
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = NextColors.cardBorder()
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = if (sortDescending) "Descending" else "Ascending",
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(sortDirectionRotation),
                            tint = if (sortDescending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                LibrarySortType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (type) {
                                    LibrarySortType.DATE_ADDED -> "Date Added"
                                    LibrarySortType.SIZE -> "File Size"
                                    LibrarySortType.NAME -> "Name"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (sortType == type) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        trailingIcon = {
                            if (sortType == type) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            onSortTypeChange(type)
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        // View mode toggle button (Cards / List)
        FilledTonalIconButton(
            onClick = onViewModeToggle,
            shape = RoundedCornerShape(14.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = if (viewMode == LibraryViewMode.GRID) Icons.Default.ViewAgenda else Icons.Default.GridView,
                contentDescription = "Switch View Mode",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ArchiveTuneHeroBanner(
    title: String,
    subtitle: String,
    gradient: List<Color>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onPlayAll: () -> Unit,
    onReelsMode: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = gradient.first().copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onReelsMode,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black.copy(alpha = 0.35f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Reels Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveTuneMediaCard(
    video: Video,
    isLiked: Boolean,
    isDownloaded: Boolean,
    onToggleLike: () -> Unit,
    onDeleteOffline: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                NextColors.cardBorder(),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Artwork / Thumbnail with badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                val context = LocalContext.current
                val thumbModel = remember(video.artworkUri, video.path, video.uriString) {
                    val candidate: Any? = when {
                        !video.artworkUri.isNullOrBlank() -> video.artworkUri
                        !video.uriString.isNullOrBlank() && (video.uriString.startsWith("file://") || video.uriString.startsWith("content://")) -> video.uriString
                        !video.path.isNullOrBlank() && (video.path.startsWith("/") || video.path.startsWith("file://")) -> video.path
                        else -> null
                    }
                    if (candidate != null) {
                        val builder = ImageRequest.Builder(context)
                            .data(candidate)
                            .crossfade(true)
                        if (candidate is String && dev.roxy.kryptonplayer.core.data.network.NetworkConfig.isWorkerUrl(candidate)) {
                            builder.httpHeaders(
                                NetworkHeaders.Builder()
                                    .set("X-Arch-Client-Key", dev.roxy.kryptonplayer.core.data.network.NetworkConfig.CLIENT_KEY)
                                    .build()
                            )
                        }
                        builder.build()
                    } else null
                }

                if (thumbModel != null) {
                    AsyncImage(
                        model = thumbModel,
                        contentDescription = video.nameWithExtension,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Center Play Icon Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Top Right: Lottie Heart Like Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .padding(4.dp)
                ) {
                    LottieHeartLikeButton(
                        isLiked = isLiked,
                        onToggleLike = onToggleLike,
                        size = 20.dp
                    )
                }

                // Bottom Right: Duration Badge / Download Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isDownloaded) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = NextIcons.TelegramCloudDone),
                                contentDescription = "Saved",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    if (video.formattedDuration.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.formattedDuration,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Title and details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = video.nameWithExtension,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (video.formattedFileSize.isNotEmpty()) {
                        Text(
                            text = video.formattedFileSize,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    if (isDownloaded && onDeleteOffline != null) {
                        IconButton(
                            onClick = onDeleteOffline,
                            modifier = Modifier.size(24.dp)
                        ) {
                            LottieTelegramDelete(
                                size = 20.dp,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveTuneFeatureCard(
    video: Video,
    isLiked: Boolean,
    isDownloaded: Boolean,
    onToggleLike: () -> Unit,
    onDeleteOffline: (() -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .border(NextColors.cardBorder(), RoundedCornerShape(20.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Square Thumbnail with RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                val context = LocalContext.current
                val thumbModel = remember(video.artworkUri, video.path, video.uriString) {
                    val candidate: Any? = when {
                        !video.artworkUri.isNullOrBlank() -> video.artworkUri
                        !video.uriString.isNullOrBlank() && (video.uriString.startsWith("file://") || video.uriString.startsWith("content://")) -> video.uriString
                        !video.path.isNullOrBlank() && (video.path.startsWith("/") || video.path.startsWith("file://")) -> video.path
                        else -> null
                    }
                    if (candidate != null) {
                        val builder = ImageRequest.Builder(context)
                            .data(candidate)
                            .crossfade(true)
                        if (candidate is String && dev.roxy.kryptonplayer.core.data.network.NetworkConfig.isWorkerUrl(candidate)) {
                            builder.httpHeaders(
                                NetworkHeaders.Builder()
                                    .set("X-Arch-Client-Key", dev.roxy.kryptonplayer.core.data.network.NetworkConfig.CLIENT_KEY)
                                    .build()
                            )
                        }
                        builder.build()
                    } else null
                }

                if (thumbModel != null) {
                    AsyncImage(
                        model = thumbModel,
                        contentDescription = video.nameWithExtension,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Center Play Icon Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Duration badge at bottom
                if (video.formattedDuration.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.nameWithExtension,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isDownloaded) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(2.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = NextIcons.TelegramCloudDone),
                                contentDescription = "Saved",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    if (video.formattedFileSize.isNotEmpty()) {
                        Text(
                            text = video.formattedFileSize,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            // Trailing actions: Lottie Like & Delete Offline
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                LottieHeartLikeButton(
                    isLiked = isLiked,
                    onToggleLike = onToggleLike,
                    size = 22.dp
                )

                if (isDownloaded && onDeleteOffline != null) {
                    IconButton(
                        onClick = onDeleteOffline,
                        modifier = Modifier.size(32.dp)
                    ) {
                        LottieTelegramDelete(
                            size = 22.dp,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryDownloadTaskCard(
    task: DownloadTask,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .border(NextColors.cardBorder(), RoundedCornerShape(20.dp))
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
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background((if (task.isPaused) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isPaused) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        LottieTelegramDownload(
                            size = 24.dp,
                            isPlaying = true,
                            tint = Color(0xFF2AABEE)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val statusText = if (task.isPaused) {
                        "Paused"
                    } else if (task.status.contains("Queued")) {
                        "In Queue"
                    } else {
                        val downloadedStr = formatSize(task.downloadedBytes)
                        val totalStr = formatSize(task.totalBytes)
                        val speedStr = formatSpeed(task.speedBytesPerSec)
                        "$downloadedStr / $totalStr • $speedStr"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!task.isPaused) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2AABEE).copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
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
                                    .clip(RoundedCornerShape(4.dp))
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
                            text = statusText,
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
                    FilledTonalIconButton(
                        onClick = onPauseResume,
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (task.isPaused) Color(0xFFFF9800).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = if (task.isPaused) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (task.isPaused) "Resume Download" else "Pause Download",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Download",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (!task.status.contains("Queued")) {
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (task.isPaused) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 KB/s"
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB/s", mb)
        else -> String.format(java.util.Locale.US, "%.0f KB/s", kb)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

@Composable
private fun LibraryFailedDownloadCard(
    errorInfo: DownloadErrorInfo,
    onViewLogs: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFF3B30).copy(alpha = 0.08f),
        border = NextColors.cardBorder(Color(0xFFFF3B30).copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewLogs() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = errorInfo.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Failed: ${errorInfo.errorCode} (${errorInfo.errorMessage})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF3B30),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Advice snippet
            Text(
                text = errorInfo.diagnosticAdvice,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )

            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onViewLogs,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Logs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
