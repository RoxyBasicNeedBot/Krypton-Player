@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.roxy.kryptonplayer.feature.videopicker.screens.telegram

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.CircularWavyProgressIndicator
import dev.roxy.kryptonplayer.core.ui.components.ChannelLoadingPlaceholder
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramBoost
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramDelete
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramDownload
import dev.roxy.kryptonplayer.core.ui.components.LottieVerifiedBadge
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.theme.NextColors
import dev.roxy.kryptonplayer.core.ui.R as UiR
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.data.network.models.ChannelPostDto
import dev.roxy.kryptonplayer.core.data.network.models.ChannelResponse
import dev.roxy.kryptonplayer.core.data.network.models.MediaItemDto
import dev.roxy.kryptonplayer.core.model.Video
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.*

private fun formatDuration(durationSec: Int): String {
    if (durationSec <= 0) return ""
    val hours = durationSec / 3600
    val minutes = (durationSec % 3600) / 60
    val seconds = durationSec % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

internal fun formatPostDate(dateString: String?): String {
    val dateStr = dateString ?: return ""
    return try {
        // e.g., "2026-06-13T12:00:00.000Z"
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(dateStr) ?: return ""
        val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        try {
            dateStr.take(10)
        } catch (ex: Exception) {
            ""
        }
    }
}

private fun formatTimeOnly(dateString: String?): String {
    val dateStr = dateString ?: return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(dateStr) ?: return ""
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun highlightSearchQuery(text: String, query: String): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(text)
    
    val spanStyle = SpanStyle(
        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
    
    return buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val index = text.indexOf(query, start, ignoreCase = true)
            if (index == -1) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            withStyle(spanStyle) {
                append(text.substring(index, index + query.length))
            }
            start = index + query.length
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChannelFeedContent(
    viewModel: TelegramViewModel,
    channel: ChannelResponse,
    onPlayVideo: (Uri, String) -> Unit,
    onImageClick: (List<Video>, Int) -> Unit,
    onSearchClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    scrollToPostId: String?,
    onScrollCompleted: () -> Unit,
    scrollToTopTrigger: Boolean = false,
    onScrollToTopCompleted: () -> Unit = {},
    searchQuery: String = "",
    modifier: Modifier = Modifier
) {
    val posts by viewModel.channelPosts.collectAsState()
    val distinctPosts = remember(posts, channel.channel_id) {
        val channelAbsId = kotlin.math.abs(channel.channel_id)
        val bareId = if (channelAbsId > 1000000000000L) channelAbsId - 1000000000000L else channelAbsId
        posts.map { post ->
            if (post.channelId == 0L) post.copy(channelId = channel.channel_id) else post
        }.filter { post ->
            val postAbs = kotlin.math.abs(post.channelId)
            val postBare = if (postAbs > 1000000000000L) postAbs - 1000000000000L else postAbs
            postBare == bareId
        }.distinctBy { it.postId }
    }
    val stats by viewModel.channelStats.collectAsState()
    val selectedFilter by viewModel.selectedPostType.collectAsState()
    val isChannelLoading by viewModel.isChannelLoading.collectAsState()
    val isChannelLoadingMore by viewModel.isChannelPostsLoadingMore.collectAsState()
    val isChannelPostsEndReached by viewModel.isChannelPostsEndReached.collectAsState()
    
    var selectedAlbumPost by remember { mutableStateOf<ChannelPostDto?>(null) }
    var showBoostSheet by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val initialScrollPos = remember(channel.channel_id) {
        viewModel.getChannelScrollPosition(channel.channel_id)
    }

    val lazyListState = remember(channel.channel_id) {
        LazyListState(
            firstVisibleItemIndex = initialScrollPos?.index ?: 0,
            firstVisibleItemScrollOffset = initialScrollPos?.offset ?: 0
        )
    }

    val playerPreferences by viewModel.playerPreferences.collectAsState()
    val userToken = remember(playerPreferences) { playerPreferences.telegramToken }
    val progressFlow = viewModel.playbackProgress.collectAsState()
    val downloadProgressMap by viewModel.downloadProgress.collectAsState()
    val activeDownloadTasks by viewModel.activeDownloadTasks.collectAsState()
    val downloadedKeys by viewModel.downloadedKeys.collectAsState()

    var hasRestoredScrollState by remember(channel.channel_id) {
        mutableStateOf(false)
    }
    var hasUserManuallyScrolled by remember(channel.channel_id) {
        mutableStateOf(false)
    }

    // Detect user manual interaction with the list
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .collect { inProgress ->
                if (inProgress) {
                    hasUserManuallyScrolled = true
                }
            }
    }

    // Perform exact scroll restoration to the last viewed post (keeps locked to target post even when network prepends/appends posts)
    LaunchedEffect(distinctPosts, hasUserManuallyScrolled) {
        if (!hasUserManuallyScrolled && distinctPosts.isNotEmpty()) {
            if (initialScrollPos == null) {
                hasRestoredScrollState = true
                return@LaunchedEffect
            }
            val targetPostId = initialScrollPos.postId
            val targetMsgId = initialScrollPos.messageId
            val targetIndex = when {
                !targetPostId.isNullOrEmpty() -> distinctPosts.indexOfFirst {
                    it.postId == targetPostId || it.postId.trim() == targetPostId.trim() || it.messageId.toString() == targetPostId || targetPostId.endsWith("_" + it.messageId)
                }
                targetMsgId > 0L -> distinctPosts.indexOfFirst { it.messageId.toLong() == targetMsgId }
                else -> -1
            }

            val restoreIndex = when {
                targetIndex >= 0 -> targetIndex
                initialScrollPos.postId.isNullOrEmpty() && initialScrollPos.index in distinctPosts.indices -> initialScrollPos.index
                else -> -1
            }

            if (restoreIndex >= 0) {
                if (lazyListState.firstVisibleItemIndex != restoreIndex || lazyListState.firstVisibleItemScrollOffset != initialScrollPos.offset) {
                    lazyListState.scrollToItem(restoreIndex, initialScrollPos.offset)
                }
                hasRestoredScrollState = true
            } else if (!isChannelLoading && !isChannelLoadingMore && distinctPosts.isNotEmpty()) {
                if (initialScrollPos.index in distinctPosts.indices) {
                    lazyListState.scrollToItem(initialScrollPos.index, initialScrollPos.offset)
                }
                hasRestoredScrollState = true
            }
        }
    }

    // Save scroll position as the user scrolls (debounced flow, only when scrolling stopped AND user actually scrolled)
    LaunchedEffect(lazyListState, hasUserManuallyScrolled, distinctPosts.isNotEmpty()) {
        if (!hasUserManuallyScrolled || distinctPosts.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val idx = lazyListState.firstVisibleItemIndex
            val off = lazyListState.firstVisibleItemScrollOffset
            val inProgress = lazyListState.isScrollInProgress
            Triple(idx, off, inProgress)
        }
        .distinctUntilChanged()
        .collectLatest { (idx, off, inProgress) ->
            if (!inProgress && hasUserManuallyScrolled && distinctPosts.isNotEmpty()) {
                delay(300)
                val visiblePost = distinctPosts.getOrNull(idx)
                if (visiblePost != null || idx > 0) {
                    val mid = visiblePost?.messageId?.toLong() ?: 0L
                    viewModel.saveChannelScrollState(
                        channelId = channel.channel_id,
                        index = idx,
                        offset = off,
                        postId = visiblePost?.postId,
                        messageId = mid
                    )
                }
            }
        }
    }

    // Save exact scroll position immediately on disposal (leaving channel or entering player) only if user actually interacted
    DisposableEffect(channel.channel_id) {
        onDispose {
            if (hasUserManuallyScrolled && distinctPosts.isNotEmpty()) {
                val idx = lazyListState.firstVisibleItemIndex
                val off = lazyListState.firstVisibleItemScrollOffset
                val visiblePost = distinctPosts.getOrNull(idx)
                if (visiblePost != null || idx > 0) {
                    val mid = visiblePost?.messageId?.toLong() ?: 0L
                    viewModel.saveChannelScrollState(
                        channelId = channel.channel_id,
                        index = idx,
                        offset = off,
                        postId = visiblePost?.postId,
                        messageId = mid
                    )
                }
            }
        }
    }

    val allChannelImages = remember(distinctPosts) {
        val list = mutableListOf<Video>()
        distinctPosts.forEach { post ->
            if (post.postType == "photo") {
                list.add(mapPostToVideo(post))
            } else if (post.postType == "album") {
                post.mediaItems?.forEach { item ->
                    if (item.type == "photo") {
                        list.add(mapMediaItemToVideo(item, post))
                    }
                }
            }
        }
        list
    }

    // Scroll to target match when navigating search results
    LaunchedEffect(scrollToPostId) {
        if (scrollToPostId != null) {
            val index = distinctPosts.indexOfFirst { it.postId == scrollToPostId }
            if (index >= 0) {
                lazyListState.animateScrollToItem(index)
                onScrollCompleted()
            }
        }
    }

    // Scroll to top (newest post) when triggered
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger) {
            lazyListState.animateScrollToItem(0)
            onScrollToTopCompleted()
        }
    }

    // Continuously detect when user scrolls near the bottom to load more posts
    LaunchedEffect(lazyListState, distinctPosts.size) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            totalItems > 0 && lastVisibleItem >= totalItems - 8
        }
        .distinctUntilChanged()
        .collect { shouldLoad ->
            if (shouldLoad) {
                viewModel.loadMoreChannelPosts()
            }
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // ArchiveTune Sleek Channel Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = NextColors.cardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onBackClick != null) {
                    FilledTonalIconButton(
                        onClick = onBackClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = "Back to Channels",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                val photoBytes = remember(channel.channel_photo) {
                    if (!channel.channel_photo.isNullOrEmpty()) {
                        try {
                            Base64.decode(channel.channel_photo, Base64.DEFAULT)
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }
                val bitmap = remember(photoBytes) {
                    if (photoBytes != null) {
                        try {
                            BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }

                // Channel Squircle Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .border(
                            NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Channel Avatar",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val fallbackTitle = channel.channel_title?.takeIf { it.isNotBlank() }
                            ?: channel.channel_username?.takeIf { it.isNotBlank() }
                            ?: "Channel"
                        Text(
                            text = fallbackTitle.trim().removePrefix("@").take(1).uppercase().ifEmpty { "C" },
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = channel.channel_title ?: channel.channel_username ?: "Channel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        LottieVerifiedBadge(size = 18.dp)
                    }
                    val formattedSubs = remember(channel.member_count) {
                        val count = channel.member_count
                        if (count >= 1000) {
                            String.format("%.1fK", count / 1000f).replace(".0", "")
                        } else {
                            count.toString()
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    ) {
                        Text(
                            text = "$formattedSubs subscribers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = { showBoostSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.15f),
                        contentColor = Color(0xFFFF9800)
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    LottieTelegramBoost(
                        size = 22.dp,
                        tint = Color(0xFFFF9800)
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/kryptonplayerbot?start=learn")).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = "Guides & Bot (/learn)",
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onSearchClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Channel",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }


        // Type Filter Chips Row
        TypeFilterChipsRow(
            selectedType = selectedFilter,
            stats = stats,
            onTypeSelected = { viewModel.setPostTypeFilter(it) }
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        // Post Feed or Empty State
        if (isChannelLoading && distinctPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ChannelLoadingPlaceholder(
                    lottieRes = UiR.raw.anim_duck_search,
                    title = "Loading Channel Feed...",
                    subtitle = "Fetching latest messages, videos, and media from Telegram"
                )
            }
        } else if (distinctPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ChannelLoadingPlaceholder(
                    lottieRes = if (selectedFilter != null)
                        UiR.raw.anim_duck_search
                    else
                        UiR.raw.anim_duck_empty,
                    title = if (selectedFilter != null) "No ${selectedFilter?.replaceFirstChar { it.uppercase() }} Posts" else "Channel is Empty",
                    subtitle = if (selectedFilter != null)
                        "No content matches the '$selectedFilter' filter in this channel."
                    else
                        "This channel hasn't shared any media or messages yet. Boost the channel to request posts!",
                    isLoading = false,
                    actionButtonText = if (selectedFilter != null) "Reset Filter" else "Boost Channel",
                    onActionClick = if (selectedFilter != null) {
                        { viewModel.setPostTypeFilter(null) }
                    } else {
                        { showBoostSheet = true }
                    },
                    showBoostIcon = (selectedFilter == null),
                    suggestions = if (selectedFilter == null) listOf("/boost", "/learn") else emptyList(),
                    onSuggestionClick = { suggestion ->
                        if (suggestion == "/boost") {
                            showBoostSheet = true
                        } else if (suggestion == "/learn") {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/kryptonplayerbot?start=learn")).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    itemsIndexed(
                        distinctPosts,
                        key = { index, post -> if (post.postId.isNotBlank()) "${post.postId}_$index" else "post_${post.channelId}_${post.messageId}_$index" }
                    ) { index, post ->
                        val dateFormatted = formatPostDate(post.date)
                        val isFirstInDateGroup = index == 0 || dateFormatted != formatPostDate(distinctPosts[index - 1].date)
                        if (isFirstInDateGroup) {
                            DateSeparator(date = dateFormatted)
                        }
                        
                        ChannelPostCard(
                            post = post,
                            userToken = userToken,
                            progressFlow = progressFlow,
                            downloadProgressMap = downloadProgressMap,
                            activeDownloadTasks = activeDownloadTasks,
                            downloadedKeys = downloadedKeys,
                            viewModel = viewModel,
                            onPlayVideo = { uri, title ->
                                val postIndex = distinctPosts.indexOfFirst { it.postId == post.postId }
                                val effectiveIndex = if (postIndex >= 0) postIndex else lazyListState.firstVisibleItemIndex
                                val effectiveOffset = if (postIndex >= 0) 0 else lazyListState.firstVisibleItemScrollOffset
                                val mid = post.messageId.toLong()
                                viewModel.saveChannelScrollState(
                                    channelId = channel.channel_id,
                                    index = effectiveIndex,
                                    offset = effectiveOffset,
                                    postId = post.postId,
                                    messageId = mid
                                )
                                onPlayVideo(uri, title)
                            },
                            onImageClick = { albumVideos, clickedIndex ->
                                val postIndex = distinctPosts.indexOfFirst { it.postId == post.postId }
                                val effectiveIndex = if (postIndex >= 0) postIndex else lazyListState.firstVisibleItemIndex
                                val effectiveOffset = if (postIndex >= 0) 0 else lazyListState.firstVisibleItemScrollOffset
                                val mid = post.messageId.toLong()
                                viewModel.saveChannelScrollState(
                                    channelId = channel.channel_id,
                                    index = effectiveIndex,
                                    offset = effectiveOffset,
                                    postId = post.postId,
                                    messageId = mid
                                )
                                val clickedVideo = albumVideos.getOrNull(clickedIndex)
                                if (clickedVideo != null) {
                                    val globalIndex = allChannelImages.indexOfFirst { it.externalId == clickedVideo.externalId }
                                    if (globalIndex >= 0) {
                                        onImageClick(allChannelImages, globalIndex)
                                    } else {
                                        val albumImagesOnly = albumVideos.filter { it.isImage }
                                        val localIndex = albumImagesOnly.indexOf(clickedVideo).coerceAtLeast(0)
                                        onImageClick(albumImagesOnly, localIndex)
                                    }
                                } else {
                                    onImageClick(albumVideos.filter { it.isImage }, 0)
                                }
                            },
                            onAlbumClick = {
                                val postIndex = distinctPosts.indexOfFirst { it.postId == post.postId }
                                val effectiveIndex = if (postIndex >= 0) postIndex else lazyListState.firstVisibleItemIndex
                                val effectiveOffset = if (postIndex >= 0) 0 else lazyListState.firstVisibleItemScrollOffset
                                val mid = post.messageId.toLong()
                                viewModel.saveChannelScrollState(
                                    channelId = channel.channel_id,
                                    index = effectiveIndex,
                                    offset = effectiveOffset,
                                    postId = post.postId,
                                    messageId = mid
                                )
                                selectedAlbumPost = it
                            },
                            searchQuery = searchQuery
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (isChannelLoadingMore) {
                        item(key = "channel_loading_more_indicator") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularWavyProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Loading older channel posts...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    } else if (isChannelPostsEndReached && distinctPosts.size >= 50) {
                        item(key = "channel_posts_end_reached_indicator") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = NextColors.cardBorder()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "All ${distinctPosts.size} channel posts loaded",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                // Nagram Signature Fast ScrollBar with Date Floating Bubble
                dev.roxy.kryptonplayer.core.ui.components.NagramFastScrollBar(
                    lazyListState = lazyListState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    sectionHeaderProvider = { index ->
                        val post = distinctPosts.getOrNull(index)
                        if (post != null) {
                            formatPostDate(post.date)
                        } else null
                    }
                )
            }
        }
    }

    selectedAlbumPost?.let { albumPost ->
        ChannelAlbumBottomSheet(
            post = albumPost,
            onDismissRequest = { selectedAlbumPost = null },
            onPlayVideo = { uri, name ->
                val postIndex = distinctPosts.indexOfFirst { it.postId == albumPost.postId }
                val effectiveIndex = if (postIndex >= 0) postIndex else lazyListState.firstVisibleItemIndex
                val effectiveOffset = if (postIndex >= 0) 0 else lazyListState.firstVisibleItemScrollOffset
                val mid = albumPost.messageId.toLong()
                viewModel.saveChannelScrollState(
                    channelId = channel.channel_id,
                    index = effectiveIndex,
                    offset = effectiveOffset,
                    postId = albumPost.postId,
                    messageId = mid
                )
                onPlayVideo(uri, name)
            },
            onImageClick = { videos, idx ->
                val postIndex = distinctPosts.indexOfFirst { it.postId == albumPost.postId }
                val effectiveIndex = if (postIndex >= 0) postIndex else lazyListState.firstVisibleItemIndex
                val effectiveOffset = if (postIndex >= 0) 0 else lazyListState.firstVisibleItemScrollOffset
                val mid = albumPost.messageId.toLong()
                viewModel.saveChannelScrollState(
                    channelId = channel.channel_id,
                    index = effectiveIndex,
                    offset = effectiveOffset,
                    postId = albumPost.postId,
                    messageId = mid
                )
                onImageClick(videos, idx)
            },
            onDownloadToWatch = { viewModel.downloadToWatch(it) },
            onSaveToGallery = { viewModel.saveToGallery(it) },
            downloadProgressMap = downloadProgressMap,
            onPauseDownload = { viewModel.pauseDownload(it) },
            onResumeDownload = { viewModel.resumeDownload(it) },
            onCancelDownload = { viewModel.cancelDownload(it) },
            isDownloadPaused = { key ->
                activeDownloadTasks[key]?.isPaused == true || viewModel.isDownloadPaused(key)
            }
        )
    }

    if (showBoostSheet) {
        TelegramBoostBottomSheet(
            channel = channel,
            onDismiss = { showBoostSheet = false }
        )
    }
}

@Composable
fun DateSeparator(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Text(
                text = date,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TypeFilterChipsRow(
    selectedType: String?,
    stats: dev.roxy.kryptonplayer.core.data.network.models.ChannelStatsResponse?,
    onTypeSelected: (String?) -> Unit
) {
    val filters = remember(stats) {
        listOf(
            Triple("All", null as String?, stats?.total ?: 0),
            Triple("Videos", "video", stats?.video ?: 0),
            Triple("Photos", "photo", stats?.photo ?: 0),
            Triple("Albums", "album", stats?.album ?: 0),
            Triple("Texts", "text", stats?.text ?: 0),
            Triple("Files", "document", stats?.document ?: 0),
            Triple("Audios", "audio", stats?.audio ?: 0)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (label, type, count) ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                shape = RoundedCornerShape(16.dp),
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = label, fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Medium)
                        if (count > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (selectedType == type)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.primaryContainer
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedType == type)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = null
            )
        }
    }
}

@Composable
fun ChannelPostCard(
    post: ChannelPostDto,
    userToken: String?,
    progressFlow: State<Map<String, Pair<Long, Long>>>,
    downloadProgressMap: Map<String, Float>,
    activeDownloadTasks: Map<String, dev.roxy.kryptonplayer.core.data.download.DownloadTask> = emptyMap(),
    downloadedKeys: Set<String>,
    viewModel: TelegramViewModel,
    onPlayVideo: (Uri, String) -> Unit,
    onImageClick: (List<Video>, Int) -> Unit,
    onAlbumClick: (ChannelPostDto) -> Unit,
    searchQuery: String = ""
) {
    val context = LocalContext.current
    var showPostOptions by remember { mutableStateOf(false) }
    val postVideo = remember(post, userToken) { mapPostToVideo(post, userToken) }
    val isDownloaded = remember(postVideo, downloadedKeys) { 
        val albumItems = post.mediaItems
        if (post.postType == "album" && !albumItems.isNullOrEmpty()) {
            albumItems.all { item ->
                val v = mapMediaItemToVideo(item, post, userToken)
                viewModel.isVideoDownloaded(v, downloadedKeys)
            }
        } else {
            downloadedKeys.contains(postVideo.nameWithExtension) || 
                downloadedKeys.contains(post.fileId ?: "") || 
                downloadedKeys.contains(postVideo.externalId ?: "") ||
                downloadedKeys.contains(postVideo.id.toString()) ||
                viewModel.isVideoDownloaded(postVideo, downloadedKeys)
        }
    }

    val currentDownloadProgress = downloadProgressMap[post.fileId ?: ""]
        ?: downloadProgressMap[postVideo.externalId ?: ""]
        ?: downloadProgressMap[postVideo.id.toString()]
        ?: downloadProgressMap[postVideo.nameWithExtension]

    val activeTask = activeDownloadTasks[post.fileId ?: ""]
        ?: activeDownloadTasks[postVideo.externalId ?: ""]
        ?: activeDownloadTasks[postVideo.id.toString()]
        ?: activeDownloadTasks[postVideo.nameWithExtension]
    val isPostPaused = activeTask?.isPaused == true || viewModel.isDownloadPaused(postVideo)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 5.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = NextColors.cardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            // Content according to post type
            when (post.postType) {
                "video" -> {
                    VideoPostLayout(
                        post = post,
                        progressFlow = progressFlow,
                        onPlayVideo = onPlayVideo,
                        onDownload = {
                            viewModel.downloadToWatch(postVideo)
                        },
                        downloadProgress = currentDownloadProgress,
                        isDownloaded = isDownloaded,
                        isPaused = isPostPaused,
                        viewModel = viewModel,
                        userToken = userToken
                    )
                }
                "photo" -> {
                    PhotoPostLayout(
                        post = post,
                        onImageClick = onImageClick,
                        viewModel = viewModel
                    )
                }
                "album" -> {
                    AlbumPostLayout(
                        post = post,
                        onAlbumClick = onAlbumClick,
                        viewModel = viewModel
                    )
                }
                "document" -> {
                    DocumentPostLayout(
                        post = post,
                        onDownload = {
                            viewModel.downloadToWatch(postVideo)
                        },
                        downloadProgress = currentDownloadProgress,
                        isDownloaded = isDownloaded,
                        isPaused = isPostPaused,
                        viewModel = viewModel
                    )
                }
                "audio" -> {
                    AudioPostLayout(
                        post = post,
                        onDownload = {
                            viewModel.downloadToWatch(postVideo)
                        },
                        downloadProgress = currentDownloadProgress,
                        isDownloaded = isDownloaded,
                        isPaused = isPostPaused,
                        viewModel = viewModel
                    )
                }
            }

            // Shared Caption & Text Section
            val hasMedia = post.postType != "text"
            val caption = post.caption
            if (!caption.isNullOrEmpty()) {
                SelectionContainer {
                    Text(
                        text = highlightSearchQuery(text = caption, query = searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = if (hasMedia) 8.dp else 12.dp,
                            bottom = 4.dp
                        )
                    )
                }
            }

            // Reactions Row (if any reactions exist)
            val reactions = post.reactions
            if (!reactions.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    reactions.forEach { (emoji, count) ->
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(
                                        NextColors.cardBorder(width = 0.5.dp),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = emoji, fontSize = 12.sp)
                                    Text(
                                        text = count.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer (Views, Forwards, Time)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Views",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = post.views?.toString() ?: "0",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                val forwards = post.forwards
                if (forwards != null && forwards > 0) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "Forwards",
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = forwards.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formatTimeOnly(post.date),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                if (post.postType != "text") {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showPostOptions = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Post Options",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    if (showPostOptions) {
        ModalBottomSheet(
            onDismissRequest = { showPostOptions = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header preview card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(NextColors.cardBorder(), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val effectiveThumbId = post.thumbFileId ?: post.fileId
                        TelegramAsyncImage(
                            thumbFileId = effectiveThumbId,
                            chatId = post.channelId,
                            messageId = post.messageId.toLong(),
                            fallbackUrl = if (!effectiveThumbId.isNullOrEmpty()) dev.roxy.kryptonplayer.core.data.network.NetworkConfig.thumbnailUrl(effectiveThumbId) else null,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            tdlibMediaResolver = viewModel.tdlibMediaResolver,
                            imageDiskCache = viewModel.imageDiskCache
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.fileName ?: "Telegram Post #${post.messageId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${postVideo.formattedFileSize}${if (postVideo.formattedDuration.isNotEmpty()) " • " + postVideo.formattedDuration else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions List
                NagramSubMenuItem(
                    iconContent = {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = if (post.postType == "album") "Download Album (${post.mediaItems?.size ?: 0} items)" else "Download to Watch",
                    subtitle = "Store in app sandbox for 100% instant playback",
                    onClick = {
                        showPostOptions = false
                        val albumItems = post.mediaItems
                        if (post.postType == "album" && !albumItems.isNullOrEmpty()) {
                            albumItems.forEach { item ->
                                viewModel.downloadToWatch(mapMediaItemToVideo(item, post, userToken))
                            }
                        } else {
                            viewModel.downloadToWatch(postVideo)
                        }
                    }
                )

                NagramSubMenuItem(
                    icon = Icons.Filled.PhotoLibrary,
                    iconTint = Color(0xFF4CAF50),
                    title = if (post.postType == "album") "Save Album to Gallery" else "Save to Gallery",
                    subtitle = "Export file directly to phone Gallery / Storage",
                    onClick = {
                        showPostOptions = false
                        val albumItems = post.mediaItems
                        if (post.postType == "album" && !albumItems.isNullOrEmpty()) {
                            albumItems.forEach { item ->
                                viewModel.saveToGallery(mapMediaItemToVideo(item, post, userToken))
                            }
                        } else {
                            viewModel.saveToGallery(postVideo)
                        }
                    }
                )

                if (post.postType == "video") {
                    val streamUri = viewModel.getPlayableUriForPost(post)
                    NagramSubMenuItem(
                        icon = Icons.Filled.PlayArrow,
                        iconTint = Color(0xFF2AABEE),
                        title = "Play in Video Player",
                        subtitle = "Direct streaming with zero buffer loss",
                        onClick = {
                            showPostOptions = false
                            onPlayVideo(streamUri, post.fileName ?: "video.mp4")
                        }
                    )
                }

                NagramSubMenuItem(
                    icon = Icons.Filled.Schedule,
                    iconTint = Color(0xFFFF9800),
                    title = "Add to Watch Later Queue",
                    subtitle = "Queue video for flexible background or night downloading",
                    onClick = {
                        showPostOptions = false
                        viewModel.addToWatchLater(
                            fileId = post.fileId ?: post.postId,
                            chatId = post.channelId,
                            messageId = post.messageId.toLong(),
                            title = post.fileName ?: post.caption ?: "Telegram Video #${post.messageId}",
                            subtitle = "Channel #${post.channelId}",
                            fileSize = post.fileSize ?: 0L,
                            durationSeconds = post.duration?.toLong() ?: 0L,
                            streamUrl = viewModel.getPlayableUriForPost(post).toString(),
                            artworkUri = (post.thumbFileId ?: post.fileId)?.let { dev.roxy.kryptonplayer.core.data.network.NetworkConfig.thumbnailUrl(it) }
                        )
                    }
                )

                if (isDownloaded) {
                    NagramSubMenuItem(
                        iconContent = {
                            LottieTelegramDelete(size = 22.dp, tint = MaterialTheme.colorScheme.error)
                        },
                        iconTint = MaterialTheme.colorScheme.error,
                        title = "Remove Download",
                        subtitle = "Delete downloaded file to free space",
                        isDestructive = true,
                        onClick = {
                            showPostOptions = false
                            viewModel.deleteFromOffline(postVideo)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPostLayout(
    post: ChannelPostDto,
    progressFlow: State<Map<String, Pair<Long, Long>>>,
    onPlayVideo: (Uri, String) -> Unit,
    onDownload: () -> Unit,
    downloadProgress: Float?,
    isDownloaded: Boolean = false,
    isPaused: Boolean = false,
    viewModel: TelegramViewModel? = null,
    userToken: String? = null
) {
    val effectiveToken = userToken?.takeIf { it.isNotBlank() } ?: viewModel?.userToken?.value ?: ""
    val tokenParam = if (!effectiveToken.isNullOrEmpty()) "?token=$effectiveToken" else ""
    val fallbackUrl = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(post.fileId ?: "", tokenParam)
    val encodedFallback = java.net.URLEncoder.encode(fallbackUrl, "UTF-8")
    val safeName = try { java.net.URLEncoder.encode(post.fileName ?: "video.mp4", "UTF-8") } catch (_: Exception) { "video.mp4" }
    val streamTokenParam = if (!effectiveToken.isNullOrEmpty()) "&token=$effectiveToken" else ""
    val effectiveChannelId = if (post.channelId != 0L) post.channelId else (viewModel?.selectedChannel?.value?.channel_id ?: 0L)
    val channelUser = viewModel?.channels?.value?.firstOrNull {
        val chAbs = kotlin.math.abs(it.channel_id)
        val postAbs = kotlin.math.abs(effectiveChannelId)
        val chBare = if (chAbs > 1000000000000L) chAbs - 1000000000000L else chAbs
        val postBare = if (postAbs > 1000000000000L) postAbs - 1000000000000L else postAbs
        chBare == postBare
    }?.channel_username
        ?: viewModel?.selectedChannel?.value?.channel_username
        ?: viewModel?.tdlibManager?.getRegisteredChannelUsername(effectiveChannelId)

    if (effectiveChannelId != 0L && !channelUser.isNullOrEmpty()) {
        viewModel?.tdlibManager?.registerChannelUsername(effectiveChannelId, channelUser)
    }
    val userParam = if (!channelUser.isNullOrEmpty()) "&username=${java.net.URLEncoder.encode(channelUser, "UTF-8")}" else ""
    val streamUrl = "tdlib://stream?chatId=${effectiveChannelId}&messageId=${post.messageId}&fileId=${post.fileId}&size=${post.fileSize ?: 0L}&name=$safeName$userParam&fallback=$encodedFallback$streamTokenParam"
    val progress = progressFlow.value[fallbackUrl] ?: progressFlow.value[streamUrl]
    val duration = post.duration
    val watchedPercentage = remember(progress, duration) {
        if (progress != null && duration != null && duration > 0) {
            val watchedMs = progress.first
            val totalMs = duration * 1000L
            (watchedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    val aspect = remember(post.width, post.height) {
        val w = post.width?.toFloat() ?: 0f
        val h = post.height?.toFloat() ?: 0f
        if (w > 0f && h > 0f) (w / h).coerceIn(0.5f, 2f) else 1.777f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color.Black)
            .clickable { onPlayVideo(viewModel?.getPlayableUriForPost(post) ?: streamUrl.toUri(), post.fileName ?: "video.mp4") }
    ) {
        // Thumbnail Image with Direct TDLib & Local Disk Cache (0 MB Render Bandwidth)
        val effectiveThumbId = post.thumbFileId ?: post.fileId
        if (!effectiveThumbId.isNullOrEmpty() || post.messageId > 0) {
            TelegramAsyncImage(
                thumbFileId = effectiveThumbId,
                chatId = post.channelId,
                messageId = post.messageId.toLong(),
                fallbackUrl = if (!effectiveThumbId.isNullOrEmpty()) dev.roxy.kryptonplayer.core.data.network.NetworkConfig.thumbnailUrl(effectiveThumbId) else null,
                contentDescription = "Video Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                tdlibMediaResolver = viewModel?.tdlibMediaResolver,
                imageDiskCache = viewModel?.imageDiskCache
            )
        }

        // Overlay Dark Gradient at the bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                        startY = 180f
                    )
                )
        )

        // Center Play Circle (Frosted Glassmorphic Glow)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Duration Badge
        if (duration != null && duration > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Icons (Download with interactive Pause/Resume & Cancel controls)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            if (isDownloaded && downloadProgress == null) {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        painter = painterResource(id = NextIcons.TelegramCloudDone),
                        contentDescription = "Downloaded",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (downloadProgress != null) {
                val videoKey = post.fileId ?: post.messageId.toString()
                val effectiveIsPaused = isPaused || viewModel?.isDownloadPaused(videoKey) == true
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pause / Resume Button with circular progress
                    Box(
                        modifier = Modifier.size(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (effectiveIsPaused) {
                            CircularProgressIndicator(
                                progress = { if (downloadProgress > 0f) downloadProgress else 0.5f },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 2.dp,
                                color = Color(0xFFFF9800),
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        } else {
                            CircularWavyProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (effectiveIsPaused) {
                                    viewModel?.resumeDownload(videoKey)
                                } else {
                                    viewModel?.pauseDownload(videoKey)
                                }
                            },
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                        ) {
                            Icon(
                                imageVector = if (effectiveIsPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (effectiveIsPaused) "Resume Download" else "Pause Download",
                                tint = if (effectiveIsPaused) Color(0xFFFF9800) else Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Full Cancel 'X' Button
                    IconButton(
                        onClick = { viewModel?.cancelDownload(videoKey) },
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel Download",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color(0xFF1E293B).copy(alpha = 0.65f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFF2AABEE).copy(alpha = 0.5f),
                                    Color.White.copy(alpha = 0.2f)
                                )
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Download to Watch",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Watched Progress Bar at very bottom of thumbnail
        if (watchedPercentage > 0.02f) {
            LinearProgressIndicator(
                progress = { watchedPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun PhotoPostLayout(
    post: ChannelPostDto,
    onImageClick: (List<Video>, Int) -> Unit,
    viewModel: TelegramViewModel? = null
) {
    val context = LocalContext.current
    val aspect = remember(post.width, post.height) {
        val w = post.width?.toFloat() ?: 0f
        val h = post.height?.toFloat() ?: 0f
        if (w > 0f && h > 0f) (w / h).coerceIn(0.5f, 2f) else 1.5f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable {
                val video = mapPostToVideo(post)
                onImageClick(listOf(video), 0)
            }
    ) {
        TelegramAsyncImage(
            thumbFileId = post.fileId,
            chatId = post.channelId,
            messageId = post.messageId.toLong(),
            fallbackUrl = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.thumbnailUrl(post.fileId ?: ""),
            contentDescription = "Photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            tdlibMediaResolver = viewModel?.tdlibMediaResolver,
            imageDiskCache = viewModel?.imageDiskCache
        )
    }
}

fun mapMediaItemToVideo(item: MediaItemDto, parentPost: ChannelPostDto, token: String? = null): Video {
    val isImage = item.type == "photo"
    val baseFallback = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(item.fileId)
    val fallbackUrl = if (!token.isNullOrEmpty()) "$baseFallback?token=$token" else baseFallback
    val msgId = item.messageId ?: parentPost.messageId
    val encodedFallback = java.net.URLEncoder.encode(fallbackUrl, "UTF-8")
    val safeName = try { java.net.URLEncoder.encode(item.fileName ?: "video.mp4", "UTF-8") } catch (_: Exception) { "video.mp4" }
    val tokenParam = if (!token.isNullOrEmpty()) "&token=$token" else ""
    val streamUrl = if (isImage) {
        fallbackUrl
    } else {
        "tdlib://stream?chatId=${parentPost.channelId}&messageId=$msgId&fileId=${item.fileId}&size=${item.fileSize ?: 0L}&name=$safeName&fallback=$encodedFallback$tokenParam"
    }

    // Register fallback URLs for seamless resilient player DataSource recovery
    item.fileName?.let {
        dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, streamUrl)
        dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, fallbackUrl)
    }
    item.fileId.let {
        dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, streamUrl)
        dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, fallbackUrl)
    }

    val durationMs = if (isImage) 0L else (item.duration ?: 0).toLong() * 1000L
    
    return Video(
        id = item.fileId.hashCode().toLong(),
        path = fallbackUrl,
        parentPath = "Channel Files",
        duration = durationMs,
        uriString = streamUrl,
        nameWithExtension = item.fileName ?: if (isImage) "photo_${msgId}.jpg" else "video_${msgId}.mp4",
        width = item.width ?: 0,
        height = item.height ?: 0,
        size = item.fileSize ?: 0L,
        formattedDuration = if (isImage) "" else formatDuration(item.duration ?: 0),
        formattedFileSize = formatTelegramFileSize(item.fileSize ?: 0L),
        artworkUri = item.thumbFileId?.let { dev.roxy.kryptonplayer.core.data.network.NetworkConfig.thumbnailUrl(it) } ?: (if (isImage) null else dev.roxy.kryptonplayer.core.data.network.NetworkConfig.thumbnailUrl(item.fileId)),
        externalId = item.fileId,
        isImage = isImage
    )
}

@Composable
fun AlbumPostLayout(
    post: ChannelPostDto,
    onAlbumClick: (ChannelPostDto) -> Unit,
    viewModel: TelegramViewModel? = null
) {
    val items = post.mediaItems ?: return
    if (items.isEmpty()) return

    val albumVideos = remember(items) {
        items.map { mapMediaItemToVideo(it, post) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        when (items.size) {
            1 -> {
                AlbumSingleItem(items[0], onAlbumClick, post, viewModel)
            }
            2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AlbumSingleItem(items[0], onAlbumClick, post, viewModel)
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AlbumSingleItem(items[1], onAlbumClick, post, viewModel)
                    }
                }
            }
            3 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AlbumSingleItem(items[0], onAlbumClick, post, viewModel)
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            AlbumSingleItem(items[1], onAlbumClick, post, viewModel)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            AlbumSingleItem(items[2], onAlbumClick, post, viewModel)
                        }
                    }
                }
            }
            else -> {
                // 4 or more items (2x2 grid)
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AlbumSingleItem(items[0], onAlbumClick, post, viewModel)
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AlbumSingleItem(items[1], onAlbumClick, post, viewModel)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AlbumSingleItem(items[2], onAlbumClick, post, viewModel)
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AlbumSingleItem(items[3], onAlbumClick, post, viewModel)
                            if (items.size > 4) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable {
                                            onAlbumClick(post)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${items.size - 3}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumSingleItem(
    item: MediaItemDto,
    onAlbumClick: (ChannelPostDto) -> Unit,
    post: ChannelPostDto,
    viewModel: TelegramViewModel? = null
) {
    val context = LocalContext.current
    val streamUrl = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(item.fileId)
    val isVideo = item.type == "video"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                onAlbumClick(post)
            }
    ) {
        val thumbId = if (isVideo && !item.thumbFileId.isNullOrEmpty()) item.thumbFileId else item.fileId
        val msgId = (item.messageId ?: post.messageId).toLong()

        TelegramAsyncImage(
            thumbFileId = thumbId,
            chatId = post.channelId,
            messageId = msgId,
            fallbackUrl = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.thumbnailUrl(thumbId ?: ""),
            contentDescription = "Album Item",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            tdlibMediaResolver = viewModel?.tdlibMediaResolver,
            imageDiskCache = viewModel?.imageDiskCache
        )

        if (isVideo) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            val duration = item.duration
            if (duration != null && duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = formatDuration(duration),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentPostLayout(
    post: ChannelPostDto,
    onDownload: () -> Unit,
    downloadProgress: Float?,
    isDownloaded: Boolean = false,
    isPaused: Boolean = false,
    viewModel: TelegramViewModel? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f))
            .border(
                NextColors.cardBorder(),
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                .border(NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Document",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = post.fileName ?: "file_${post.messageId}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatTelegramFileSize(post.fileSize ?: 0L),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isDownloaded && downloadProgress == null) {
            IconButton(onClick = onDownload) {
                Icon(
                    painter = painterResource(id = NextIcons.TelegramCloudDone),
                    contentDescription = "Downloaded",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (downloadProgress != null) {
            val videoKey = post.fileId ?: post.messageId.toString()
            val effectiveIsPaused = isPaused || viewModel?.isDownloadPaused(videoKey) == true
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pause / Resume Button with circular progress
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (effectiveIsPaused) {
                        CircularProgressIndicator(
                            progress = { if (downloadProgress > 0f) downloadProgress else 0.5f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFF9800),
                            trackColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    } else {
                        CircularWavyProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            if (effectiveIsPaused) {
                                viewModel?.resumeDownload(videoKey)
                            } else {
                                viewModel?.pauseDownload(videoKey)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (effectiveIsPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (effectiveIsPaused) "Resume Download" else "Pause Download",
                            tint = if (effectiveIsPaused) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Full Cancel 'X' Button
                IconButton(
                    onClick = { viewModel?.cancelDownload(videoKey) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel Download",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        } else {
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AudioPostLayout(
    post: ChannelPostDto,
    onDownload: () -> Unit,
    downloadProgress: Float?,
    isDownloaded: Boolean = false,
    isPaused: Boolean = false,
    viewModel: TelegramViewModel? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f))
            .border(
                NextColors.cardBorder(),
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                .border(NextColors.cardBorder(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Audio",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = post.fileName ?: "audio_${post.messageId}.mp3",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTelegramFileSize(post.fileSize ?: 0L),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val duration = post.duration
                if (duration != null && duration > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isDownloaded && downloadProgress == null) {
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    painter = painterResource(id = NextIcons.TelegramCloudDone),
                    contentDescription = "Downloaded",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else if (downloadProgress != null) {
            val videoKey = post.fileId ?: post.messageId.toString()
            val effectiveIsPaused = isPaused || viewModel?.isDownloadPaused(videoKey) == true
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pause / Resume Button with circular progress
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (effectiveIsPaused) {
                        CircularProgressIndicator(
                            progress = { if (downloadProgress > 0f) downloadProgress else 0.5f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFF9800),
                            trackColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    } else {
                        CircularWavyProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            if (effectiveIsPaused) {
                                viewModel?.resumeDownload(videoKey)
                            } else {
                                viewModel?.pauseDownload(videoKey)
                            }
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (effectiveIsPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (effectiveIsPaused) "Resume Download" else "Pause Download",
                            tint = if (effectiveIsPaused) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Full Cancel 'X' Button
                IconButton(
                    onClick = { viewModel?.cancelDownload(videoKey) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel Download",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun mapPostToVideo(post: ChannelPostDto, token: String? = null): Video {
    val isImage = post.postType == "photo"
    val baseFallback = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(post.fileId ?: "")
    val fallbackUrl = if (!token.isNullOrEmpty()) "$baseFallback?token=$token" else baseFallback
    val encodedFallback = java.net.URLEncoder.encode(fallbackUrl, "UTF-8")
    val safeName = try { java.net.URLEncoder.encode(post.fileName ?: "video.mp4", "UTF-8") } catch (_: Exception) { "video.mp4" }
    val tokenParam = if (!token.isNullOrEmpty()) "&token=$token" else ""
    val streamUrl = if (isImage) {
        fallbackUrl
    } else {
        "tdlib://stream?chatId=${post.channelId}&messageId=${post.messageId}&fileId=${post.fileId}&size=${post.fileSize ?: 0L}&name=$safeName&fallback=$encodedFallback$tokenParam"
    }

    // Register fallback URLs for seamless resilient player DataSource recovery
    post.fileName?.let {
        dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, streamUrl)
        dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, fallbackUrl)
    }
    post.fileId?.let {
        dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, streamUrl)
        dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, fallbackUrl)
    }

    val duration = post.duration
    val durationMs = if (isImage) 0L else (duration ?: 0).toLong() * 1000L
    
    return Video(
        id = post.fileId.hashCode().toLong(),
        path = fallbackUrl,
        parentPath = "Channel Files",
        duration = durationMs,
        uriString = streamUrl,
        nameWithExtension = post.fileName ?: if (isImage) "photo_${post.messageId}.jpg" else "video_${post.messageId}.mp4",
        width = post.width ?: 0,
        height = post.height ?: 0,
        size = post.fileSize ?: 0L,
        formattedDuration = if (isImage) "" else formatDuration(duration ?: 0),
        formattedFileSize = formatTelegramFileSize(post.fileSize ?: 0L),
        artworkUri = (post.thumbFileId ?: post.fileId)?.let { dev.roxy.kryptonplayer.core.data.network.NetworkConfig.thumbnailUrl(it) },
        externalId = post.fileId,
        isImage = isImage
    )
}

@Composable
fun NagramSubMenuItem(
    icon: ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    iconContent: (@Composable () -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = iconTint.copy(alpha = 0.12f),
    title: String,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else iconBackground),
                contentAlignment = Alignment.Center
            ) {
                if (iconContent != null) {
                    iconContent()
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isDestructive) MaterialTheme.colorScheme.error else iconTint
                    )
                } else if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isDestructive) MaterialTheme.colorScheme.error else iconTint
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

