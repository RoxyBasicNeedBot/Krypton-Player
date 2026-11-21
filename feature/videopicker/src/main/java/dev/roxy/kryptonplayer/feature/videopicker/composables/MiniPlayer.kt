package dev.roxy.kryptonplayer.feature.videopicker.composables

import dev.roxy.kryptonplayer.core.ui.liquidglass.liquidGlassBorderLaser
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.ui.liquidglass.PlatformBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.liquidGlass
import dev.roxy.kryptonplayer.core.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

data class PlayerState(
    val isPlaying: Boolean,
    val playbackState: Int,
    val currentMediaItem: MediaItem?,
    val position: Long,
    val duration: Long,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
)

@Composable
fun rememberPlayerState(player: Player?): PlayerState {
    var isPlaying by remember(player) { mutableStateOf(player?.isPlaying == true) }
    var playbackState by remember(player) { mutableStateOf(player?.playbackState ?: Player.STATE_IDLE) }
    var currentMediaItem by remember(player) { mutableStateOf(player?.currentMediaItem) }
    var position by remember(player) { mutableStateOf(player?.currentPosition ?: 0L) }
    var duration by remember(player) { mutableStateOf(player?.duration ?: 0L) }
    var hasNext by remember(player) { mutableStateOf(player?.hasNextMediaItem() == true) }
    var hasPrevious by remember(player) { mutableStateOf(player?.hasPreviousMediaItem() == true) }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                duration = player.duration
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaItem = mediaItem
                duration = player.duration
                hasNext = player.hasNextMediaItem()
                hasPrevious = player.hasPreviousMediaItem()
            }
            override fun onEvents(playerEvents: Player, events: Player.Events) {
                hasNext = playerEvents.hasNextMediaItem()
                hasPrevious = playerEvents.hasPreviousMediaItem()
            }
        }
        player.addListener(listener)
        isPlaying = player.isPlaying
        playbackState = player.playbackState
        currentMediaItem = player.currentMediaItem
        duration = player.duration
        hasNext = player.hasNextMediaItem()
        hasPrevious = player.hasPreviousMediaItem()

        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(player, isPlaying) {
        if (player != null && isPlaying) {
            while (true) {
                position = player.currentPosition
                delay(250L)
            }
        }
    }

    return PlayerState(isPlaying, playbackState, currentMediaItem, position, duration, hasNext, hasPrevious)
}

@Composable
fun MiniPlayer(
    player: Player,
    onMiniPlayerClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: PlatformBackdrop? = null,
) {
    val playerState = rememberPlayerState(player)
    if (playerState.currentMediaItem == null) return

    val offsetXAnimatable = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val capsuleShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pointerInput(player) {
                detectHorizontalDragGestures(
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val adjustedDrag = if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                        val canSkipPrevious = playerState.hasPrevious
                        val canSkipNext = playerState.hasNext
                        val allowLeft = adjustedDrag < 0 && canSkipNext
                        val allowRight = adjustedDrag > 0 && canSkipPrevious
                        if (allowLeft || allowRight) {
                            coroutineScope.launch {
                                offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDrag)
                            }
                        }
                    },
                    onDragEnd = {
                        val currentOffset = offsetXAnimatable.value
                        val threshold = 150f
                        if (currentOffset > threshold && playerState.hasPrevious) {
                            player.seekToPreviousMediaItem()
                        } else if (currentOffset < -threshold && playerState.hasNext) {
                            player.seekToNextMediaItem()
                        }
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
                        }
                    }
                )
            }
    ) {
        // Swipe indicators
        if (offsetXAnimatable.value.absoluteValue > 30f) {
            Icon(
                imageVector = if (offsetXAnimatable.value > 0) Icons.Rounded.SkipPrevious else Icons.Rounded.SkipNext,
                contentDescription = null,
                tint = Color(0xFFFFD54F).copy(
                    alpha = (offsetXAnimatable.value.absoluteValue / 150f).coerceIn(0f, 1f)
                ),
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 8.dp)
                    .size(24.dp)
            )
        }

        val isDark = LocalIsDarkTheme.current
        val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        val subtitleColor = if (isDark) Color.White.copy(alpha = 0.60f) else MaterialTheme.colorScheme.onSurfaceVariant
        val iconTintColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        val playBtnBg = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        val playBtnBorder = if (isDark) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        val playBtnIconTint = if (isDark) Color.White else MaterialTheme.colorScheme.primary

        // MiniPlayer Liquid Glass Capsule Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .shadow(
                    elevation = 12.dp,
                    shape = capsuleShape,
                )
                .then(
                    if (backdrop != null) {
                        Modifier.liquidGlass(
                            backdrop = backdrop,
                            shape = capsuleShape,
                            interactive = false,
                        )
                    } else {
                        Modifier.background(
                            color = if (isDark) Color(0xEB131418) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                            shape = capsuleShape,
                        )
                    }
                )
                .liquidGlassBorderLaser(
                    laserColor = if (isDark) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
                    coreColor = if (isDark) Color(0xFFFFFBEB) else Color.White,
                    haloColor = if (isDark) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                )
                .clickable(onClick = onMiniPlayerClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                // 1. Cover Artwork (Circle ~44dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp)
                ) {
                    val isBuffering = playerState.playbackState == Player.STATE_BUFFERING
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(44.dp),
                            color = if (isDark) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            trackColor = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E2026) else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.20f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                                shape = CircleShape
                            )
                    ) {
                        val artworkUri = playerState.currentMediaItem?.mediaMetadata?.artworkUri
                        val videoUri = playerState.currentMediaItem?.localConfiguration?.uri
                        if (artworkUri != null && !artworkUri.toString().contains("artwork_default")) {
                            AsyncImage(
                                model = artworkUri,
                                contentDescription = "Artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (videoUri != null && !videoUri.toString().startsWith("http://") && !videoUri.toString().startsWith("https://")) {
                            AsyncImage(
                                model = videoUri,
                                contentDescription = "Video Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Audiotrack,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                tint = if (isDark) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 2. Title & Artist Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    val title = playerState.currentMediaItem?.mediaMetadata?.title?.toString()
                        ?: "Unknown Track"
                    val artist = playerState.currentMediaItem?.mediaMetadata?.artist?.toString()
                        ?: playerState.currentMediaItem?.mediaMetadata?.albumArtist?.toString()

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                        ),
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )

                    if (!artist.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                            ),
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 3. Like (Heart / Favorite) Button
                var isLiked by remember(playerState.currentMediaItem) { mutableStateOf(false) }
                val heartScale by animateFloatAsState(
                    targetValue = if (isLiked) 1.15f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "miniPlayerHeartScale"
                )
                IconButton(
                    onClick = { isLiked = !isLiked },
                    modifier = Modifier
                        .size(32.dp)
                        .scale(heartScale)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isLiked) Color(0xFFFF4B6E) else iconTintColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // 4. Previous Button (if track has previous)
                if (playerState.hasPrevious) {
                    IconButton(
                        onClick = { player.seekToPreviousMediaItem() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = iconTintColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // 5. Play / Pause Button (Modern circular highlighted button)
                IconButton(
                    onClick = {
                        if (playerState.isPlaying) player.pause() else player.play()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(playBtnBg)
                        .border(1.dp, playBtnBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = playBtnIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 6. Next Button
                IconButton(
                    onClick = { player.seekToNextMediaItem() },
                    enabled = playerState.hasNext,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = if (playerState.hasNext) iconTintColor.copy(alpha = 0.85f) else iconTintColor.copy(alpha = 0.28f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 7. Close (Cross) Button
                IconButton(
                    onClick = {
                        player.stop()
                        player.clearMediaItems()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 8. Sleek Continuous Progress Line at Bottom of Capsule
            val progressFraction = if (playerState.duration > 0) {
                (playerState.position.toFloat() / playerState.duration).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFF59E0B),
                                    Color(0xFFFFD54F),
                                )
                            )
                        )
                )
            }
        }
    }
}
