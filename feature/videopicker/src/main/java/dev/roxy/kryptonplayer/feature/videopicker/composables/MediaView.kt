package dev.roxy.kryptonplayer.feature.videopicker.composables

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import dev.roxy.kryptonplayer.core.model.ApplicationPreferences
import dev.roxy.kryptonplayer.core.model.Folder
import dev.roxy.kryptonplayer.core.model.MediaLayoutMode
import dev.roxy.kryptonplayer.core.model.MediaViewMode
import dev.roxy.kryptonplayer.core.model.Video
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.ListSectionTitle
import dev.roxy.kryptonplayer.core.ui.extensions.plus
import dev.roxy.kryptonplayer.feature.videopicker.state.SelectionManager
import dev.roxy.kryptonplayer.feature.videopicker.state.rememberSelectionManager
import dev.roxy.kryptonplayer.core.ui.liquidglass.PlatformBackdrop
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun MediaView(
    rootFolder: Folder,
    isFolderDetail: Boolean = false,
    preferences: ApplicationPreferences,
    recentVideos: List<Video> = emptyList(),
    showHeaders: Boolean = preferences.mediaViewMode == MediaViewMode.FOLDER_TREE,
    contentPadding: PaddingValues = PaddingValues(),
    selectionManager: SelectionManager = rememberSelectionManager(),
    lazyGridState: LazyGridState = rememberLazyGridState(),
    onFolderClick: (String) -> Unit,
    onVideoClick: (Uri) -> Unit,
    onPlayVideos: (List<Uri>) -> Unit = {},
    onVideoLoaded: (Uri) -> Unit,
    onNavigateUp: (() -> Unit)? = null,
    onReelsClick: ((List<Video>) -> Unit)? = null,
    onQuickSettingsClick: (() -> Unit)? = null,
    onSearchClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    moreMenuContent: @Composable () -> Unit = {},
    onPlayNext: ((Video) -> Unit)? = null,
    onShareVideo: ((Video) -> Unit)? = null,
    onRenameVideo: ((Video) -> Unit)? = null,
    onInfoVideo: ((Video) -> Unit)? = null,
    onDeleteVideo: ((Video) -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current

    val folderMinWidth = 100.dp
    val videoMinWidth = 160.dp
    BoxWithConstraints {
        val layoutMode = preferences.mediaLayoutMode
        val displayPreferences = remember(preferences, layoutMode) {
            preferences.copy(mediaLayoutMode = layoutMode)
        }
        val contentHorizontalPadding = when (layoutMode) {
            MediaLayoutMode.LIST -> 8.dp
            MediaLayoutMode.GRID -> 8.dp
        }
        val itemSpacing = when (layoutMode) {
            MediaLayoutMode.LIST -> 2.dp
            MediaLayoutMode.GRID -> 2.dp
        }
        val maxWidth = (this.maxWidth - (contentHorizontalPadding * 2) - itemSpacing).coerceAtLeast(160.dp)
        val maxFolders = (maxWidth / folderMinWidth).toInt().coerceAtLeast(1)
        val maxVideos = (maxWidth / videoMinWidth).toInt().coerceAtLeast(1)
        val spans = when (layoutMode) {
            MediaLayoutMode.LIST -> 1
            MediaLayoutMode.GRID -> lcm(maxFolders, maxVideos)
        }

        val singleFolderSpan = when (layoutMode) {
            MediaLayoutMode.LIST -> 1
            MediaLayoutMode.GRID -> spans / maxFolders
        }
        val singleVideoSpan = when (layoutMode) {
            MediaLayoutMode.LIST -> 1
            MediaLayoutMode.GRID -> spans / maxVideos
        }

        val gridContentPadding = if (isFolderDetail) {
            PaddingValues(
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                top = 0.dp,
                end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
            )
        } else {
            contentPadding + PaddingValues(horizontal = contentHorizontalPadding, vertical = 8.dp)
        }

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = lazyGridState,
            columns = GridCells.Fixed(spans),
            contentPadding = gridContentPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            val carouselVideos = if (recentVideos.isNotEmpty()) recentVideos else if (rootFolder.allMediaList.isNotEmpty()) rootFolder.allMediaList else rootFolder.mediaList
            if (carouselVideos.isNotEmpty() && !isFolderDetail && !selectionManager.isInSelectionMode) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "recent_videos",
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        MochyHeroCarousel(
                            videos = carouselVideos,
                            onVideoClick = onVideoClick
                        )
                    }
                }
            }

            if (isFolderDetail && (rootFolder.allMediaList.isNotEmpty() || rootFolder.mediaList.isNotEmpty()) && !selectionManager.isInSelectionMode) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "folder_hero",
                ) {
                    FolderHeroHeader(
                        folder = rootFolder,
                        onPlayAllClick = { videos ->
                            onPlayVideos(videos.map { it.uriString.toUri() })
                        },
                        onShuffleClick = { shuffledList ->
                            onPlayVideos(shuffledList.map { it.uriString.toUri() })
                        },
                        onReelsClick = onReelsClick,
                        onBackClick = onNavigateUp,
                        onQuickSettingsClick = onQuickSettingsClick,
                        onSearchClick = onSearchClick,
                        onMoreClick = onMoreClick,
                        moreMenuContent = moreMenuContent,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            if (showHeaders && rootFolder.folderList.isNotEmpty()) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "section_header",
                ) {
                    ListSectionTitle(text = stringResource(id = R.string.folders) + " (${rootFolder.folderList.size})")
                }
            }
            itemsIndexed(
                items = rootFolder.folderList,
                key = { _, folder -> folder.path },
                span = { _, _ -> GridItemSpan(singleFolderSpan) },
                contentType = { _, _ -> "folder" },
            ) { index, folder ->
                val selected by remember { derivedStateOf { selectionManager.isFolderSelected(folder) } }
                FolderItem(
                    folder = folder,
                    isRecentlyPlayedFolder = rootFolder.isRecentlyPlayedVideo(folder.recentlyPlayedVideo),
                    preferences = displayPreferences,
                    selected = selected,
                    isFirstItem = index == 0,
                    isLastItem = index == rootFolder.folderList.lastIndex,
                    onClick = {
                        if (selectionManager.isInSelectionMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            selectionManager.toggleFolderSelection(folder)
                        } else {
                            onFolderClick(folder.path)
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectionManager.toggleFolderSelection(folder)
                    },
                )
            }

            if (showHeaders && rootFolder.mediaList.isNotEmpty() && !isFolderDetail) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "section_header",
                ) {
                    ListSectionTitle(
                        text = stringResource(id = R.string.videos) + " (${rootFolder.mediaList.size})",
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            itemsIndexed(
                items = rootFolder.mediaList,
                key = { _, video -> video.uriString },
                span = { _, _ -> GridItemSpan(singleVideoSpan) },
                contentType = { _, _ -> "video" },
            ) { index, video ->
                val selected by remember { derivedStateOf { selectionManager.isVideoSelected(video) } }
                VideoItem(
                    video = video,
                    preferences = displayPreferences,
                    isRecentlyPlayedVideo = rootFolder.isRecentlyPlayedVideo(video),
                    isFirstItem = index == 0,
                    isLastItem = index == rootFolder.mediaList.lastIndex,
                    selected = selected,
                    onClick = {
                        if (selectionManager.isInSelectionMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            selectionManager.toggleVideoSelection(video)
                        } else {
                            onVideoClick(video.uriString.toUri())
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectionManager.toggleVideoSelection(video)
                    },
                    onPlayNext = onPlayNext,
                    onShare = onShareVideo,
                    onRename = onRenameVideo,
                    onInfo = onInfoVideo,
                    onDelete = onDeleteVideo,
                    modifier = Modifier
                        .padding(horizontal = if (isFolderDetail) 8.dp else 0.dp)
                        .onFirstVisible { onVideoLoaded(video.uriString.toUri()) },
                )
            }
        }

        // Nagram Signature Fast ScrollBar
        dev.roxy.kryptonplayer.core.ui.components.NagramFastScrollBar(
            lazyGridState = lazyGridState,
            modifier = Modifier.align(Alignment.CenterEnd),
            topPadding = contentPadding.calculateTopPadding() + 8.dp,
            bottomPadding = contentPadding.calculateBottomPadding() + 8.dp,
            sectionHeaderProvider = { gridIndex ->
                var currentIndex = gridIndex
                if (recentVideos.isNotEmpty() && !selectionManager.isInSelectionMode) {
                    if (currentIndex == 0) return@NagramFastScrollBar "★"
                    currentIndex -= 1
                }
                if (showHeaders && rootFolder.folderList.isNotEmpty()) {
                    if (currentIndex == 0) return@NagramFastScrollBar "📁"
                    currentIndex -= 1
                }
                if (currentIndex < rootFolder.folderList.size) {
                    return@NagramFastScrollBar rootFolder.folderList.getOrNull(currentIndex)?.name?.take(1)?.uppercase()
                }
                currentIndex -= rootFolder.folderList.size
                if (rootFolder.mediaList.isNotEmpty() && !selectionManager.isInSelectionMode) {
                    if (currentIndex == 0) return@NagramFastScrollBar "▶"
                    currentIndex -= 1
                }
                if (showHeaders && rootFolder.mediaList.isNotEmpty()) {
                    if (currentIndex == 0) return@NagramFastScrollBar "🎬"
                    currentIndex -= 1
                }
                rootFolder.mediaList.getOrNull(currentIndex)?.displayName?.take(1)?.uppercase()
            }
        )
    }
}

@Composable
fun TmdbMediaCard(
    title: String,
    posterPath: String?,
    rating: Double = 0.0,
    releaseDate: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdropPath: String? = null,
    isHorizontal: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imagePath = posterPath ?: backdropPath
        val imageUrl = imagePath?.let {
            dev.roxy.kryptonplayer.core.data.network.NetworkConfig.tmdbImageUrl("w342", it)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        maxLines = 3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Movie / TV Show Title: Centered, clean adaptive text (max 2 lines)
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun TmdbActorCard(
    name: String,
    profilePath: String?,
    department: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageUrl = profilePath?.let { dev.roxy.kryptonplayer.core.data.network.NetworkConfig.tmdbImageUrl("w185", it) }
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(2),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (department != null) {
            Text(
                text = department,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun lcm(a: Int, b: Int): Int {
    return abs(a * b) / gcd(a, b)
}

fun gcd(a: Int, b: Int): Int {
    return if (b == 0) a else gcd(b, a % b)
}
