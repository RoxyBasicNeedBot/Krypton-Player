@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package dev.roxy.kryptonplayer.feature.player.ui.lyrics

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.data.lyrics.LyricsLine
import dev.roxy.kryptonplayer.core.data.lyrics.LyricsParser
import dev.roxy.kryptonplayer.core.data.lyrics.LyricsRepository
import dev.roxy.kryptonplayer.core.data.lyrics.LyricsUiState
import dev.roxy.kryptonplayer.core.ui.components.LottieLyricsIcon
import dev.roxy.kryptonplayer.core.model.PlayerPreferences
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.feature.player.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun LiveLyricsBottomSheet(
    viewModel: PlayerViewModel,
    player: Player,
    videoTitle: String? = null,
    artist: String? = null,
    artworkUri: Uri? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val preferences by viewModel.playerPreferences.collectAsStateWithLifecycle()

    var currentPositionMs by remember { mutableLongStateOf(player.currentPosition) }
    var durationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }

    var customSearchQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }
    var showSyncControls by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (isActive) {
            currentPositionMs = player.currentPosition
            durationMs = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            delay(33L)
        }
    }

    LaunchedEffect(videoTitle, artist) {
        if (lyricsState is LyricsUiState.Idle && !videoTitle.isNullOrBlank()) {
            viewModel.checkAndLoadLyrics(
                context = context,
                videoUri = player.currentMediaItem?.localConfiguration?.uri,
                mediaTitle = videoTitle,
                artist = artist
            )
        }
    }

    val displayTitle = remember(videoTitle, lyricsState) {
        when (val s = lyricsState) {
            is LyricsUiState.Available -> s.lyrics.trackName.ifBlank { videoTitle ?: "Now Playing" }
            else -> videoTitle?.let { LyricsRepository.cleanVideoSongTitle(it) } ?: "Now Playing"
        }
    }

    val displayArtist = remember(artist, lyricsState) {
        when (val s = lyricsState) {
            is LyricsUiState.Available -> s.lyrics.artistName.ifBlank { artist ?: "ArchiveTune Synced Lyrics" }
            is LyricsUiState.Searching -> "Searching BetterLyrics & LRCLIB..."
            else -> artist ?: "Live Karaoke Synchronizer"
        }
    }

    val insets = WindowInsets.displayCutout.union(WindowInsets.systemBars)
    val leftPadding = if (isLandscape) insets.asPaddingValues().calculateLeftPadding(layoutDirection).coerceAtLeast(16.dp) else 20.dp
    val rightPadding = if (isLandscape) insets.asPaddingValues().calculateRightPadding(layoutDirection).coerceAtLeast(16.dp) else 20.dp
    val verticalPadding = if (isLandscape) 6.dp else 14.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.97f),
        dragHandle = null,
        modifier = modifier.fillMaxHeight(if (isLandscape) 0.98f else 0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = leftPadding, end = rightPadding, top = verticalPadding, bottom = verticalPadding)
        ) {
            if (!isLandscape) {
                // Drag grabber
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ArchiveTune Track Header (Compact in landscape)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media / Song Thumbnail Card
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 36.dp else 54.dp)
                        .clip(RoundedCornerShape(if (isLandscape) 10.dp else 14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(if (isLandscape) 10.dp else 14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (artworkUri != null) {
                        AsyncImage(
                            model = artworkUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LottieLyricsIcon(
                            size = if (isLandscape) 22.dp else 30.dp,
                            isPlaying = isPlaying
                        )
                    }
                }

                Spacer(modifier = Modifier.width(if (isLandscape) 10.dp else 14.dp))

                // Title and Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        style = if (isLandscape) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayArtist,
                        style = if (isLandscape) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Action Icons (Sync Offset, Search, Close)
                IconButton(
                    onClick = { showSyncControls = !showSyncControls },
                    modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                ) {
                    Icon(
                        imageVector = NextIcons.Tune,
                        contentDescription = "Sync Offset",
                        tint = if (showSyncControls || preferences.lyricsSyncOffset != 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp)
                    )
                }

                IconButton(
                    onClick = { showSearchField = !showSearchField },
                    modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                ) {
                    Icon(
                        imageVector = NextIcons.Search,
                        contentDescription = "Search Lyrics",
                        tint = if (showSearchField) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                ) {
                    Icon(
                        imageVector = NextIcons.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp)
                    )
                }
            }

            // Expandable Search Bar
            AnimatedVisibility(
                visible = showSearchField,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customSearchQuery,
                        onValueChange = { customSearchQuery = it },
                        placeholder = { Text("Search song or artist name...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = NextIcons.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (customSearchQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    viewModel.searchLyricsManually(customSearchQuery)
                                }) {
                                    Icon(
                                        imageVector = NextIcons.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.searchLyricsManually(customSearchQuery)
                        }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            // Quick Sync Offset Adjuster Bar
            AnimatedVisibility(
                visible = showSyncControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { viewModel.updateLyricsSyncOffset(-500) }) {
                            Text("-0.5s", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.updateLyricsSyncOffset(-100) }) {
                            Text("-100ms", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.resetLyricsSyncOffset() }) {
                            Text(
                                text = if (preferences.lyricsSyncOffset == 0) "Synced (0ms)" else "${preferences.lyricsSyncOffset} ms",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (preferences.lyricsSyncOffset != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(onClick = { viewModel.updateLyricsSyncOffset(100) }) {
                            Text("+100ms", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.updateLyricsSyncOffset(500) }) {
                            Text("+0.5s", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 12.dp))

            // Lyrics Main Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val s = lyricsState) {
                    is LyricsUiState.Searching -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Loading synced karaoke lyrics...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is LyricsUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                LottieLyricsIcon(
                                    size = 48.dp,
                                    isPlaying = false
                                )
                                Text(
                                    text = s.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(onClick = { showSearchField = true }) {
                                    Text("Search manually for lyrics")
                                }
                            }
                        }
                    }
                    is LyricsUiState.Available -> {
                        val isPlainMode = preferences.lyricsMode == "PLAIN"
                        if (!isPlainMode && s.lyrics.isSynced && s.lyrics.lines.isNotEmpty()) {
                            SyncedLyricsListView(
                                lines = s.lyrics.lines,
                                currentPositionMs = currentPositionMs + preferences.lyricsSyncOffset,
                                preferences = preferences,
                                onSeekTo = { timestamp ->
                                    if (preferences.lyricsClickToSeek) {
                                        player.seekTo(timestamp)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                isLandscape = isLandscape
                            )
                        } else {
                            val plain = s.lyrics.plainLyrics
                            if (!plain.isNullOrBlank()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 24.dp)
                                ) {
                                    item {
                                        Text(
                                            text = plain,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = preferences.lyricsTextSize.sp,
                                                lineHeight = (preferences.lyricsTextSize * preferences.lyricsLineSpacing).sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No lyrics content available.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    is LyricsUiState.Idle -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Play a video or song to view live synced lyrics",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Mini Playback Controls Bar
            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))
            Surface(
                shape = RoundedCornerShape(if (isLandscape) 12.dp else 18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isLandscape) 12.dp else 16.dp, vertical = if (isLandscape) 4.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Position and Duration
                    if (isLandscape) {
                        Text(
                            text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Column {
                            Text(
                                text = formatTime(currentPositionMs),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatTime(durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mini Controls (Rewind 10s, Play/Pause, Fast-Forward 10s)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 8.dp)
                    ) {
                        IconButton(
                            onClick = { player.seekTo((currentPositionMs - 10000L).coerceAtLeast(0L)) },
                            modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                        ) {
                            Icon(
                                imageVector = NextIcons.Replay,
                                contentDescription = "Rewind 10s",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                if (isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.size(if (isLandscape) 32.dp else 42.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) NextIcons.Pause else NextIcons.Play,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp)
                            )
                        }

                        IconButton(
                            onClick = { player.seekTo((currentPositionMs + 10000L).coerceAtMost(durationMs)) },
                            modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                        ) {
                            Icon(
                                imageVector = NextIcons.Fast,
                                contentDescription = "Forward 10s",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsListView(
    lines: List<LyricsLine>,
    currentPositionMs: Long,
    preferences: PlayerPreferences,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    val listState = rememberLazyListState()
    val activeIndex = remember(lines, currentPositionMs) {
        LyricsParser.findCurrentLineIndex(lines, currentPositionMs)
    }

    LaunchedEffect(activeIndex, preferences.lyricsAutoScroll) {
        if (preferences.lyricsAutoScroll && activeIndex in lines.indices) {
            val targetIndex = if (isLandscape) (activeIndex - 1).coerceAtLeast(0) else (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(
                index = targetIndex,
                scrollOffset = 0
            )
        }
    }

    val baseTextSize = preferences.lyricsTextSize
    val textSize = if (isLandscape) (baseTextSize * 0.82f).coerceIn(13f, 20f).sp else baseTextSize.sp
    val lineHeight = if (isLandscape) (textSize.value * 1.30f).sp else (baseTextSize * preferences.lyricsLineSpacing).sp

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = if (isLandscape) 16.dp else 80.dp),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 14.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            val isPassed = index < activeIndex

            val scale by animateFloatAsState(
                targetValue = if (isActive) (if (isLandscape) 1.02f else 1.05f) else 0.98f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "lyricScale"
            )

            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1.0f else if (isPassed) 0.50f else 0.38f,
                animationSpec = tween(300),
                label = "lyricAlpha"
            )

            val textColor by animateColorAsState(
                targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(300),
                label = "lyricTextColor"
            )

            val lineModifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onSeekTo(line.timeMs) }
                )
                .padding(horizontal = 8.dp, vertical = if (isLandscape) 3.dp else 6.dp)
                .scale(scale)
                .alpha(alpha)
                .then(
                    if (!isActive && preferences.lyricsLineBlur) {
                        Modifier.blur(2.dp)
                    } else {
                        Modifier
                    }
                )

            Box(
                modifier = lineModifier,
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (isActive) textSize else (textSize.value * 0.90f).sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                        lineHeight = lineHeight
                    ),
                    color = textColor
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
