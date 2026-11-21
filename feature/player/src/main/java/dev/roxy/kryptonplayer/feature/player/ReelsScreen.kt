package dev.roxy.kryptonplayer.feature.player

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import dev.roxy.kryptonplayer.core.ui.liquidglass.liquidGlassBorderLaser
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.feature.player.model.ReelItem
import dev.roxy.kryptonplayer.core.ui.components.LottieHeartLikeButton
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramDelete
import dev.roxy.kryptonplayer.core.data.tdlib.SmartTelegramDataSource
import dev.roxy.kryptonplayer.core.data.tdlib.TdlibManager
import dev.roxy.kryptonplayer.core.data.tdlib.TdlibMediaResolver
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    playlist: List<ReelItem>,
    startIndex: Int,
    tdlibManager: TdlibManager? = null,
    tdlibMediaResolver: TdlibMediaResolver? = null,
    telegramRepository: dev.roxy.kryptonplayer.core.data.repository.TelegramRepository? = null,
    preferencesRepository: dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository? = null,
    telegramLikedRepository: dev.roxy.kryptonplayer.core.data.repository.TelegramLikedRepository? = null,
    onBackClick: () -> Unit,
    onRotateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPlaylist by remember(playlist) { mutableStateOf(playlist) }

    if (currentPlaylist.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No media to display in Reels", color = Color.White)
        }
        return
    }
    val context = LocalContext.current
    val playlistKey = remember { ReelsActivity.playlistKey }
    val sharedPrefs = remember { context.getSharedPreferences("reels_progress", Context.MODE_PRIVATE) }

    val savedIndexKey = remember(playlistKey) { "${playlistKey}_index" }
    val savedPositionKey = remember(playlistKey) { "${playlistKey}_position" }

    val savedIndex = remember(savedIndexKey) { sharedPrefs.getInt(savedIndexKey, -1) }
    val savedPosition = remember(savedPositionKey) { sharedPrefs.getLong(savedPositionKey, 0L) }

    var initialPositionToShow by remember { mutableStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, currentPlaylist.size - 1)
    ) {
        currentPlaylist.size
    }

    var isDialogActive by remember {
        mutableStateOf(
            playlistKey.isNotEmpty() &&
            savedIndex >= 0 &&
            savedIndex < currentPlaylist.size &&
            (savedIndex != 0 || savedPosition > 1000L)
        )
    }

    // Initialize ExoPlayer with SmartTelegramDataSource for Telegram Cloud MTProto + HTTP streaming
    val player = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = DataSource.Factory {
            val baseDataSource = DefaultDataSource(context.applicationContext, httpDataSourceFactory.createDataSource())
            if (tdlibManager != null && tdlibMediaResolver != null) {
                SmartTelegramDataSource(
                    context = context.applicationContext,
                    httpDelegate = baseDataSource,
                    tdlibManager = tdlibManager,
                    tdlibMediaResolver = tdlibMediaResolver,
                    preferencesRepository = preferencesRepository
                )
            } else {
                baseDataSource
            }
        }

        val extractorsFactory = dev.roxy.kryptonplayer.feature.player.extractor.ResilientExtractorsFactory()

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    DisposableEffect(playlistKey, isDialogActive) {
        onDispose {
            if (playlistKey.isNotEmpty() && !isDialogActive) {
                val currentPos = try { player.currentPosition } catch (e: Exception) { 0L }
                if (currentPos > 0L) {
                    sharedPrefs.edit()
                        .putInt(savedIndexKey, pagerState.currentPage)
                        .putLong(savedPositionKey, currentPos)
                        .apply()
                }
            }
        }
    }

    var isPlaying by remember { mutableStateOf(true) }

    // Sync player play/pause state back to UI
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    // Standard Android Lifecycle handling
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        player.pause()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isPlaying && !isDialogActive) {
            player.play()
        }
    }

    var currentProgress by remember { mutableStateOf(0f) }
    var currentPositionText by remember { mutableStateOf("00:00") }
    var durationText by remember { mutableStateOf("00:00") }
    var isBuffering by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableStateOf(0f) }

    // Monitor buffering state
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    // Monitor progress and save history periodically (handling both videos and photos)
    LaunchedEffect(player, isPlaying, pagerState.currentPage, isDialogActive, isSeeking) {
        var lastSaveTime = 0L
        val activeItem = if (playlist.isNotEmpty() && pagerState.currentPage in playlist.indices) playlist[pagerState.currentPage] else null
        
        if (activeItem != null && activeItem.isImage) {
            // 5-second slide timer for photos with smooth progress
            val totalPhotoDurationMs = 5000L
            var elapsedMs = 0L
            durationText = "00:05"
            while (elapsedMs <= totalPhotoDurationMs) {
                if (isPlaying && !isDialogActive && !isSeeking) {
                    currentProgress = elapsedMs.toFloat() / totalPhotoDurationMs
                    currentPositionText = formatTime(elapsedMs)
                    elapsedMs += 100L
                }
                delay(100)
            }
            if (isPlaying && !isDialogActive && pagerState.currentPage < playlist.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        } else {
            while (true) {
                val duration = player.duration
                if (duration > 0 && !isSeeking) {
                    val currentPos = player.currentPosition
                    currentProgress = currentPos.toFloat() / duration
                    currentPositionText = formatTime(currentPos)
                    durationText = formatTime(duration)

                    val now = System.currentTimeMillis()
                    if (isPlaying && playlistKey.isNotEmpty() && !isDialogActive && now - lastSaveTime >= 2000L) {
                        sharedPrefs.edit()
                            .putInt(savedIndexKey, pagerState.currentPage)
                            .putLong(savedPositionKey, currentPos)
                            .apply()
                        lastSaveTime = now
                    }
                }
                delay(200)
            }
        }
    }

    // Load active video when page changes
    LaunchedEffect(pagerState.currentPage, isDialogActive) {
        if (isDialogActive) {
            player.stop()
            return@LaunchedEffect
        }
        player.stop()
        val activeItem = if (currentPlaylist.isNotEmpty() && pagerState.currentPage in currentPlaylist.indices) currentPlaylist[pagerState.currentPage] else null
        if (activeItem != null && !activeItem.isImage) {
            val rawUri = activeItem.uri
            val resolvedUri = when {
                rawUri.startsWith("tdlib://") -> Uri.parse(rawUri)
                rawUri.startsWith("http://") || rawUri.startsWith("https://") || rawUri.startsWith("content://") || rawUri.startsWith("file://") -> Uri.parse(rawUri)
                else -> Uri.fromFile(java.io.File(rawUri))
            }
            val mediaItem = MediaItem.Builder()
                .setUri(resolvedUri)
                .setMediaId(rawUri)
                .apply {
                    if (rawUri.startsWith("tdlib://") || rawUri.contains("/api/stream/")) {
                        setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP4)
                    }
                }
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            if (initialPositionToShow > 0L) {
                player.seekTo(initialPositionToShow)
                initialPositionToShow = 0L // reset so we don't seek again on page swipes
            }
            player.playWhenReady = true
        }
    }

    if (isDialogActive) {
        AlertDialog(
            onDismissRequest = {
                isDialogActive = false
            },
            title = { Text("Continue Watching?") },
            text = { Text("Would you like to resume playing from where you left off?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        initialPositionToShow = savedPosition
                        isDialogActive = false
                        if (savedIndex != pagerState.currentPage) {
                            coroutineScope.launch {
                                try {
                                    pagerState.scrollToPage(savedIndex)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                ) {
                    Text("Resume")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Start over: clear saved preferences
                        sharedPrefs.edit()
                            .remove(savedIndexKey)
                            .remove(savedPositionKey)
                            .apply()
                        isDialogActive = false
                    }
                ) {
                    Text("Start Over")
                }
            }
        )
    }

    // Controls visibility overlay (taps show/hide titles, etc.)
    var showControlsOverlay by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = if (currentPlaylist.isNotEmpty() && page in currentPlaylist.indices) currentPlaylist[page] else null
            val isActive = page == pagerState.currentPage

            var showPlayPauseIcon by remember(page) { mutableStateOf<Boolean?>(null) }
            var iconTrigger by remember(page) { mutableStateOf(0) }

            LaunchedEffect(iconTrigger) {
                if (showPlayPauseIcon != null) {
                    delay(500)
                    showPlayPauseIcon = null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .let {
                        if (item != null && !item.isImage) {
                            it.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isActive) {
                                    if (player.isPlaying) {
                                        player.pause()
                                        showPlayPauseIcon = false
                                    } else {
                                        player.play()
                                        showPlayPauseIcon = true
                                    }
                                    iconTrigger++
                                }
                            }
                        } else it
                    }
            ) {
                if (item != null && item.isImage) {
                    val imageModel = remember(item) {
                        val u = item.uri
                        when {
                            u.startsWith("http://") || u.startsWith("https://") -> u
                            u.startsWith("file://") || u.startsWith("/") -> java.io.File(u.removePrefix("file://"))
                            u.startsWith("content://") -> Uri.parse(u)
                            u.startsWith("tdlib://") -> {
                                val parsed = Uri.parse(u)
                                parsed.getQueryParameter("fallback") ?: item.thumbnailUri ?: u
                            }
                            !item.thumbnailUri.isNullOrBlank() -> item.thumbnailUri
                            else -> u
                        }
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isPlaying = !isPlaying
                                showPlayPauseIcon = isPlaying
                                iconTrigger++
                            },
                        contentScale = ContentScale.Fit
                    )
                } else if (isActive) {
                    val presentationState = rememberPresentationState(player)
                    PlayerSurface(
                        player = player,
                        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                        modifier = Modifier
                            .fillMaxSize()
                            .resizeWithContentScale(
                                contentScale = ContentScale.Fit,
                                sourceSizeDp = presentationState.videoSizeDp?.let { size ->
                                    size.copy(
                                        width = with(LocalDensity.current) { size.width.toDp().value },
                                        height = with(LocalDensity.current) { size.height.toDp().value },
                                    )
                                }
                            )
                    )

                    // Loading indicator when buffering
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp),
                            color = Color.White
                        )
                    }
                } else {
                    // Show thumbnail/placeholder for inactive pages
                    if (item != null && !item.thumbnailUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = item.thumbnailUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    }
                }

                // Play / Pause Micro-animation Indicator
                AnimatedVisibility(
                    visible = showPlayPauseIcon != null,
                    enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.5f, animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 1.5f, animationSpec = tween(150)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (showPlayPauseIcon == true) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        // --- OVERLAYS ---

        val activeItem = if (currentPlaylist.isNotEmpty() && pagerState.currentPage in currentPlaylist.indices) currentPlaylist[pagerState.currentPage] else null

        // Top Header Control Bar
        AnimatedVisibility(
            visible = showControlsOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBackClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Reels",
                        tint = Color.White
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete from Cloud (Only for My Cloud items)
                    if (activeItem != null && activeItem.source == "cloud" && !activeItem.dbId.isNullOrBlank()) {
                        var showDeleteConfirmDialog by remember(activeItem.dbId) { mutableStateOf(false) }

                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.4f)
                            )
                        ) {
                            LottieTelegramDelete(
                                size = 24.dp,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        if (showDeleteConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmDialog = false },
                                title = { Text("Delete from Cloud?") },
                                text = { Text("Are you sure you want to permanently delete \"${activeItem?.title ?: "this item"}\" from your cloud database?") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val itemToDelete = activeItem
                                            showDeleteConfirmDialog = false
                                            if (itemToDelete != null) {
                                                coroutineScope.launch {
                                                    val token = preferencesRepository?.playerPreferences?.value?.telegramToken ?: ""
                                                    val dbId = itemToDelete.dbId
                                                    if (token.isNotEmpty() && !dbId.isNullOrBlank() && telegramRepository != null) {
                                                    val success = telegramRepository.deleteVideo(dbId, token)
                                                    if (success) {
                                                        Toast.makeText(context, "Deleted from Cloud", Toast.LENGTH_SHORT).show()
                                                        val currentIndex = pagerState.currentPage
                                                        val newPlaylist = currentPlaylist.toMutableList().apply {
                                                            if (currentIndex in indices) removeAt(currentIndex)
                                                        }
                                                        if (newPlaylist.isEmpty()) {
                                                            onBackClick()
                                                        } else {
                                                            currentPlaylist = newPlaylist
                                                            if (currentIndex >= newPlaylist.size) {
                                                                pagerState.scrollToPage(newPlaylist.size - 1)
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Failed to delete file from DB", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Error: Token or DB ID missing", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                    ) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }

                    // Rotate Button
                    IconButton(
                        onClick = onRotateClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ScreenRotation,
                            contentDescription = "Rotate Player",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Title and Progress Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Video Title
            Text(
                text = activeItem?.title ?: "", 
                style = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            if (activeItem != null && !activeItem.isImage) {
                Spacer(modifier = Modifier.height(12.dp))

                // Thin Custom Seekable Slider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .liquidGlassBorderLaser(
                            laserColor = Color(0xFFFFD54F),
                            coreColor = Color(0xFFFFFBEB),
                            haloColor = Color(0xFFF59E0B),
                            speedMillis = 3600,
                            strokeWidth = 1.2.dp,
                            glowWidth = 3.dp,
                            cornerRadius = 12.dp,
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Slider(
                    value = if (isSeeking) seekProgress else currentProgress,
                    onValueChange = {
                        isSeeking = true
                        seekProgress = it
                        val duration = player.duration
                        if (duration > 0) {
                            currentPositionText = formatTime((it * duration).toLong())
                        }
                    },
                    onValueChangeFinished = {
                        val duration = player.duration
                        if (duration > 0) {
                            val targetPosition = (seekProgress * duration).toLong()
                            player.seekTo(targetPosition)
                        }
                        isSeeking = false
                    },
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                        thumbColor = Color.White
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color.White, CircleShape)
                        )
                    },
                    track = { sliderState ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            val valueFraction = if (sliderState.valueRange.endInclusive > 0f) sliderState.value / sliderState.valueRange.endInclusive else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(valueFraction)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions (Time, Like and Download Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val likedKeys by (telegramLikedRepository?.likedKeys ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptySet()) }).collectAsState()
                var isItemLiked by remember(activeItem?.uri) { mutableStateOf(false) }
                val currentItem = activeItem
                val isLikedNow = remember(likedKeys, currentItem?.dbId, currentItem?.uri) {
                    telegramLikedRepository?.isLiked(currentItem?.dbId ?: currentItem?.uri) ?: isItemLiked
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentItem != null && !currentItem.isImage) {
                        Text(
                            text = "$currentPositionText / $durationText",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Photo",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Lottie Heart Like Button in Reels
                    if (currentItem != null && currentItem.source != "local") {
                        val activeVideo = remember(currentItem) {
                            dev.roxy.kryptonplayer.core.model.Video(
                                id = 0L,
                                path = currentItem.uri,
                                parentPath = currentItem.source,
                                duration = currentItem.duration,
                                uriString = currentItem.uri,
                                nameWithExtension = currentItem.title,
                                width = 0,
                                height = 0,
                                size = 0L,
                                formattedDuration = "",
                                formattedFileSize = "",
                                artworkUri = currentItem.thumbnailUri,
                                externalId = currentItem.dbId ?: currentItem.uri,
                                isImage = currentItem.isImage
                            )
                        }

                        LottieHeartLikeButton(
                            isLiked = isLikedNow,
                            onToggleLike = {
                                if (telegramLikedRepository != null) {
                                    val token = preferencesRepository?.playerPreferences?.value?.telegramToken ?: ""
                                    val newState = telegramLikedRepository.toggleLike(activeVideo, token)
                                    isItemLiked = newState
                                    if (newState) {
                                        Toast.makeText(context, "Added to Liked Posts", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Removed from Liked Posts", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    isItemLiked = !isItemLiked
                                }
                            },
                            size = 24.dp
                        )
                    }
                }

                // Download Button (Only for Telegram Cloud / Telegram Channels, not local)
                if (currentItem != null && currentItem.source != "local") {
                    IconButton(
                        onClick = {
                            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val resolvedUriString = when {
                                currentItem.uri.startsWith("tdlib://") -> {
                                    val parsed = Uri.parse(currentItem.uri)
                                    parsed.getQueryParameter("fallback") ?: currentItem.thumbnailUri ?: currentItem.uri
                                }
                                else -> currentItem.uri
                            }
                            val request = DownloadManager.Request(Uri.parse(resolvedUriString))
                                .setTitle(currentItem.title)
                                .setDescription(if (currentItem.isImage) "Downloading photo from Telegram Cloud" else "Downloading video from Telegram Cloud")
                                .setDestinationInExternalPublicDir(
                                    if (currentItem.isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES,
                                    "KryptonPlayer/${currentItem.title}"
                                )
                                .setAllowedOverMetered(true)
                                .setAllowedOverRoaming(true)
                            try {
                                downloadManager.enqueue(request)
                                Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = if (currentItem.isImage) "Download Image" else "Download Video",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
