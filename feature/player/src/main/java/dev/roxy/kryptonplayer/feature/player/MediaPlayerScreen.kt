package dev.roxy.kryptonplayer.feature.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import dev.roxy.kryptonplayer.feature.player.ui.ringtone.RingtoneMakerBottomSheet
import dev.roxy.kryptonplayer.feature.player.ui.clipmaker.VideoClipMakerBottomSheet
import dev.roxy.kryptonplayer.feature.player.ui.lyrics.LiveLyricsBottomSheet
import dev.roxy.kryptonplayer.feature.player.state.rememberAudioOutputState
import dev.roxy.kryptonplayer.feature.player.ui.AudioOutputBottomSheet
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackEvent
import dev.roxy.kryptonplayer.core.model.ApplicationPreferences
import dev.roxy.kryptonplayer.core.model.ControlButtonsPosition
import dev.roxy.kryptonplayer.core.model.PlayerPreferences
import dev.roxy.kryptonplayer.core.ui.R as coreUiR
import dev.roxy.kryptonplayer.core.ui.extensions.copy
import dev.roxy.kryptonplayer.feature.player.buttons.NextButton
import dev.roxy.kryptonplayer.feature.player.buttons.PlayPauseButton
import dev.roxy.kryptonplayer.feature.player.buttons.PlayerButton
import dev.roxy.kryptonplayer.feature.player.buttons.PreviousButton
import dev.roxy.kryptonplayer.feature.player.buttons.SeekBackButton
import dev.roxy.kryptonplayer.feature.player.buttons.SeekForwardButton
import dev.roxy.kryptonplayer.feature.player.state.ControlsVisibilityState
import dev.roxy.kryptonplayer.feature.player.state.VerticalGesture
import dev.roxy.kryptonplayer.feature.player.state.rememberBrightnessState
import dev.roxy.kryptonplayer.feature.player.state.rememberControlsVisibilityState
import dev.roxy.kryptonplayer.feature.player.state.rememberErrorState
import dev.roxy.kryptonplayer.feature.player.state.rememberMediaPresentationState
import dev.roxy.kryptonplayer.feature.player.state.rememberMetadataState
import dev.roxy.kryptonplayer.feature.player.state.rememberSeekbarThumbnailState
import dev.roxy.kryptonplayer.feature.player.state.rememberPictureInPictureState
import dev.roxy.kryptonplayer.feature.player.state.rememberRotationState
import dev.roxy.kryptonplayer.feature.player.state.rememberSeekGestureState
import dev.roxy.kryptonplayer.feature.player.state.rememberTapGestureState
import dev.roxy.kryptonplayer.feature.player.state.rememberVideoZoomAndContentScaleState
import dev.roxy.kryptonplayer.feature.player.state.rememberVolumeAndBrightnessGestureState
import dev.roxy.kryptonplayer.feature.player.state.rememberVolumeState
import dev.roxy.kryptonplayer.feature.player.extensions.nameRes
import dev.roxy.kryptonplayer.feature.player.state.seekAmountFormatted
import dev.roxy.kryptonplayer.feature.player.state.seekToPositionFormated
import dev.roxy.kryptonplayer.feature.player.ui.DoubleTapIndicator
import dev.roxy.kryptonplayer.feature.player.ui.OverlayShowView
import dev.roxy.kryptonplayer.feature.player.ui.OverlayView
import dev.roxy.kryptonplayer.feature.player.ui.SeekIndicatorOverlay
import dev.roxy.kryptonplayer.feature.player.ui.SpeedSliderHUD
import dev.roxy.kryptonplayer.feature.player.ui.VerticalProgressView
import dev.roxy.kryptonplayer.feature.player.ui.controls.ControlsBottomView
import dev.roxy.kryptonplayer.feature.player.ui.controls.ControlsTopView
import kotlin.time.Duration.Companion.seconds
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.clickable

import kotlinx.coroutines.delay

val LocalControlsVisibilityState = compositionLocalOf<ControlsVisibilityState?> { null }

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaPlayerScreen(
    player: PlaybackController?,
    viewModel: PlayerViewModel,
    playerPreferences: PlayerPreferences,
    appPreferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    onSelectSubtitleClick: () -> Unit,
    onOnlineSubtitleSelected: (java.io.File) -> Unit = {},
    onBackClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showOnlineSubtitlesSheet by remember { mutableStateOf(false) }
    val volumeState = rememberVolumeState(
        player = player,
        showVolumePanelIfHeadsetIsOn = playerPreferences.showSystemVolumePanel,
        enableVolumeBoost = playerPreferences.enableVolumeBoost,
    )
    player ?: return
    val metadataState = rememberMetadataState(player)
    val mediaPresentationState = rememberMediaPresentationState(player)
    val seekbarThumbnailState = rememberSeekbarThumbnailState()
    // Set video URI for thumbnail extraction — keyed on currentUri state which updates on media item transition
    LaunchedEffect(mediaPresentationState.currentUri) {
        seekbarThumbnailState.setVideoUri(mediaPresentationState.currentUri)
    }
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        hideAfter = playerPreferences.controllerAutoHideTimeout.seconds,
    )
    val tapGestureState = rememberTapGestureState(
        player = player,
        doubleTapGesture = playerPreferences.doubleTapGesture,
        seekIncrementMillis = playerPreferences.seekIncrement.seconds.inWholeMilliseconds,
        useLongPressGesture = playerPreferences.useLongPressControls,
        longPressSpeed = playerPreferences.longPressControlsSpeed,
    )
    val seekGestureState = rememberSeekGestureState(
        player = player,
        sensitivity = playerPreferences.seekSensitivity,
        enableSeekGesture = playerPreferences.useSeekControls,
    )
    val pictureInPictureState = rememberPictureInPictureState(
        player = player,
        autoEnter = playerPreferences.autoPip,
    )
    val videoZoomAndContentScaleState = rememberVideoZoomAndContentScaleState(
        player = player,
        initialContentScale = playerPreferences.playerVideoZoom,
        enableZoomGesture = playerPreferences.useZoomControls,
        enablePanGesture = playerPreferences.enablePanGesture,
        onEvent = viewModel::onVideoZoomEvent,
    )
    val brightnessState = rememberBrightnessState()
    val volumeAndBrightnessGestureState = rememberVolumeAndBrightnessGestureState(
        volumeState = volumeState,
        brightnessState = brightnessState,
        enableVolumeGesture = playerPreferences.enableVolumeSwipeGesture,
        enableBrightnessGesture = playerPreferences.enableBrightnessSwipeGesture,
        volumeGestureSensitivity = playerPreferences.volumeGestureSensitivity,
        brightnessGestureSensitivity = playerPreferences.brightnessGestureSensitivity,
    )
    val rotationState = rememberRotationState(
        player = player,
        screenOrientation = playerPreferences.playerScreenOrientation,
    )
    val errorState = rememberErrorState(player = player)


    var showChatPanel by remember { mutableStateOf(false) }
    var showWatchPartyInfoDialog by remember { mutableStateOf(false) }
    var showUsbCastSheet by remember { mutableStateOf(false) }
    var showBookmarkSheet by remember { mutableStateOf(false) }
    var showXRaySheet by remember { mutableStateOf(false) }
    var showSoundtrackSheet by remember { mutableStateOf(false) }
    var showProHud by remember { mutableStateOf(false) }
    var showLiveLyricsSheet by rememberSaveable { mutableStateOf(false) }
    var showClipMakerSheet by rememberSaveable { mutableStateOf(false) }
    var showRingtoneMakerSheet by rememberSaveable { mutableStateOf(false) }
    val audioOutputState = rememberAudioOutputState()
    var showAudioOutputSheet by rememberSaveable { mutableStateOf(false) }
    val hasLyrics by viewModel.hasLyrics.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(mediaPresentationState.currentUri, metadataState.title, metadataState.artist) {
        val currentTitle = metadataState.title
        if (!currentTitle.isNullOrBlank()) {
            viewModel.checkAndLoadLyrics(
                context = context,
                videoUri = mediaPresentationState.currentUri,
                mediaTitle = currentTitle,
                artist = metadataState.artist,
                mediaUri = mediaPresentationState.currentUri?.toString()
            )
        }
    }
    var qrCodeBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(uiState.watchPartyRoomId, showWatchPartyInfoDialog) {
        val roomId = uiState.watchPartyRoomId
        if (roomId != null && showWatchPartyInfoDialog) {
            val inviteLink = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.syncJoinUrl(roomId)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    qrCodeBitmap = dev.roxy.kryptonplayer.core.ui.utils.QrCodeGenerator.generate(inviteLink, 400)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            qrCodeBitmap = null
        }
    }

    // Track user-initiated actions vs remote sync actions
    LaunchedEffect(player, uiState.isConnectedToWatchParty) {
        if (uiState.isConnectedToWatchParty) {
            player.events.collect { event ->
                if (viewModel.isSyncGuardActive) return@collect
                val timeSinceRemote = System.currentTimeMillis() - viewModel.lastRemoteEventTime
                if (timeSinceRemote < 3000L) return@collect
                val snap = player.state.value
                when (event) {
                    is PlaybackEvent.IsPlayingChanged -> {
                        if (event.isPlaying) {
                            viewModel.sendPlayToWatchParty(snap.currentPositionMs)
                        } else if (!snap.isBuffering) {
                            viewModel.sendPauseToWatchParty(snap.currentPositionMs)
                        }
                    }
                    is PlaybackEvent.SeekCompleted -> {
                        if (event.userInitiated) {
                            viewModel.sendSeekToWatchParty(event.positionMs)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    LaunchedEffect(player, uiState.isConnectedToWatchParty) {
        if (uiState.isConnectedToWatchParty) {
            while (true) {
                kotlinx.coroutines.delay(500L)
                val pos = player.state.value.currentPositionMs
                if (pos >= 0L) {
                    viewModel.updatePlayerPositionForSync(pos)
                }
            }
        }
    }

    // Handle incoming Watch Party commands
    LaunchedEffect(player, uiState.isConnectedToWatchParty) {
        if (uiState.isConnectedToWatchParty) {
            viewModel.watchPartyCommands.collect { command ->
                // Increment sync guard BEFORE touching the player
                viewModel.incrementSyncGuard()
                try {
                    when (command) {
                        is WatchPartyPlayerCommand.SeekAndPlay -> {
                            player.seekTo(command.positionMs, isUserSeek = false)
                            player.play()
                        }
                        is WatchPartyPlayerCommand.SeekAndPause -> {
                            player.seekTo(command.positionMs, isUserSeek = false)
                            player.pause()
                        }
                        is WatchPartyPlayerCommand.Seek -> {
                            player.seekTo(command.positionMs, isUserSeek = false)
                        }
                        is WatchPartyPlayerCommand.HardSync -> {
                            player.seekTo(command.positionMs, isUserSeek = false)
                        }
                        is WatchPartyPlayerCommand.AdjustSpeed -> {
                            if (player.state.value.playbackSpeed != command.speed) {
                                player.setPlaybackSpeed(command.speed)
                            }
                        }
                    }
                    kotlinx.coroutines.delay(1500)
                } finally {
                    viewModel.decrementSyncGuard()
                }
            }
        }
    }

    // Periodic position reporter for drift correction
    LaunchedEffect(player, uiState.isConnectedToWatchParty) {
        if (uiState.isConnectedToWatchParty) {
            while (true) {
                kotlinx.coroutines.delay(2000)
                val snap = player.state.value
                viewModel.reportLocalPosition(snap.currentPositionMs, snap.isPlaying)
            }
        }
    }

    LaunchedEffect(pictureInPictureState.isInPictureInPictureMode) {
        if (pictureInPictureState.isInPictureInPictureMode) {
            controlsVisibilityState.hideControls()
        }
    }


    LaunchedEffect(tapGestureState.isLongPressGestureInAction) {
        if (tapGestureState.isLongPressGestureInAction) {
            controlsVisibilityState.hideControls()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (playerPreferences.rememberPlayerBrightness) {
            brightnessState.setBrightness(playerPreferences.playerBrightness)
        }
    }

    LaunchedEffect(brightnessState.currentBrightness) {
        if (playerPreferences.rememberPlayerBrightness) {
            viewModel.updatePlayerBrightness(brightnessState.currentBrightness)
        }
    }
    val currentMediaUriStr = mediaPresentationState.currentUri?.toString()
        ?: player.state.value.currentItem?.uri?.toString()
        ?: player.state.value.currentItem?.mediaId
        ?: (context as? android.app.Activity)?.intent?.data?.toString()
        ?: ""
    val isTelegramMedia = currentMediaUriStr.startsWith("tdlib://") ||
                          currentMediaUriStr.contains("/api/stream/") ||
                          currentMediaUriStr.contains("arch-core") ||
                          currentMediaUriStr.contains("arch-stream") ||
                          currentMediaUriStr.contains("workers.dev") ||
                          (context as? android.app.Activity)?.intent?.getBooleanExtra("is_telegram_cloud", false) == true

    val likedKeys by viewModel.likedKeys.collectAsStateWithLifecycle()
    // Optimistic local toggle so the heart immediately stays red/unliked
    var localLikeOverride by remember { mutableStateOf<Boolean?>(null) }
    // Reset the local override whenever the backing StateFlow catches up
    val repoLiked by remember {
        derivedStateOf { viewModel.isMediaLiked(currentMediaUriStr, metadataState.title) }
    }
    LaunchedEffect(repoLiked) { localLikeOverride = null }
    val isCurrentMediaLiked = localLikeOverride ?: repoLiked

    var overlayView by remember { mutableStateOf<OverlayView?>(null) }
    val subtitleOptionsState = dev.roxy.kryptonplayer.feature.player.state.rememberSubtitleOptionsState(
        player = player,
        onEvent = viewModel::onSubtitleOptionEvent,
    )

    CompositionLocalProvider(LocalControlsVisibilityState provides controlsVisibilityState) {
        Box {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                PlayerContentFrame(
                    player = player,
                    playerPreferences = playerPreferences,
                    pictureInPictureState = pictureInPictureState,
                    controlsVisibilityState = controlsVisibilityState,
                    tapGestureState = tapGestureState,
                    seekGestureState = seekGestureState,
                    videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                    volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
                    subtitleOptionsState = subtitleOptionsState,
                )

                AnimatedVisibility(
                    visible = controlsVisibilityState.controlsVisible && !controlsVisibilityState.controlsLocked,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                    )
                }

                // In-video Buffering & Initial Loading overlay
                val isInitialLoading = (mediaPresentationState.isLoading || mediaPresentationState.isIdle) &&
                    mediaPresentationState.position == 0L &&
                    !mediaPresentationState.isPlaying

                if (isInitialLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                        Color.Black
                                    ),
                                    radius = 2200f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                } else if (mediaPresentationState.isBuffering) {
                    // Separate Buffering overlay if video stalls during playback (matching Nosved Player 1:1)
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.Center)
                    )
                }

                AnimatedVisibility(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(Alignment.TopCenter),
                    visible = tapGestureState.isLongPressGestureInAction,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
                ) {
                    SpeedSliderHUD(
                        playbackSpeed = tapGestureState.currentDynamicSpeed,
                    )
                }

                // Horizontal Swipe Seek Indicator (TopCenter with 50dp padding matching Nosved Player 1:1)
                AnimatedVisibility(
                    visible = seekGestureState.isSeeking && seekGestureState.seekAmount != null,
                    enter = fadeIn(animationSpec = tween(120)) + slideInVertically(
                        initialOffsetY = { -it / 3 },
                        animationSpec = tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    ),
                    exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(
                        targetOffsetY = { -it / 3 },
                        animationSpec = tween(150),
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 50.dp),
                ) {
                    val currentPos = mediaPresentationState.position.coerceAtLeast(0L)
                    val totalDur = mediaPresentationState.duration.coerceAtLeast(0L)
                    val deltaSec = ((seekGestureState.seekAmount ?: 0L) / 1000).toInt()
                    SeekIndicatorOverlay(
                        currentSeekPos = currentPos,
                        duration = totalDur,
                        seekDeltaSeconds = deltaSec,
                    )
                }

                if (controlsVisibilityState.controlsVisible && controlsVisibilityState.controlsLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(top = 24.dp),
                    ) {
                        PlayerButton(
                            containerColor = Color.Black.copy(0.5f),
                            onClick = { controlsVisibilityState.unlockControls() }
                        ) {
                            Icon(
                                painter = painterResource(coreUiR.drawable.ic_lock),
                                contentDescription = stringResource(coreUiR.string.controls_unlock),
                            )
                        }
                    }
                } else {
                    PlayerControlsView(
                        topView = {
                            AnimatedVisibility(
                                visible = controlsVisibilityState.controlsVisible,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                ControlsTopView(
                                    title = metadataState.title ?: "",
                                    onAudioClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.AUDIO_SELECTOR
                                    },
                                    onSubtitleClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.SUBTITLE_SELECTOR
                                    },
                                    onBackClick = onBackClick,
                                    onMoreOptionsClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.QUICK_TOOLS
                                    },
                                    decoderText = "HW",
                                    onDecoderClick = {
                                        Toast.makeText(context, "Hardware Decoder (HW)", Toast.LENGTH_SHORT).show()
                                    },
                                    watchPartyRoomId = uiState.watchPartyRoomId,
                                    watchPartyMemberCount = uiState.watchPartyMemberCount,
                                    onWatchPartyChatClick = {
                                        showChatPanel = !showChatPanel
                                    },
                                    onWatchPartyInfoClick = {
                                        showWatchPartyInfoDialog = true
                                    },
                                    hasLyrics = hasLyrics,
                                    onLyricsClick = {
                                        showLiveLyricsSheet = true
                                    },
                                    showLikeButton = isTelegramMedia,
                                    isLiked = isCurrentMediaLiked,
                                    onToggleLike = {
                                        localLikeOverride = !isCurrentMediaLiked
                                        val key = currentMediaUriStr.ifEmpty { metadataState.title ?: "Playing Video" }
                                        viewModel.toggleLikeMedia(metadataState.title ?: "Playing Video", key)
                                    }
                                )

                            }
                        },
                        middleView = {
                            when {
                                videoZoomAndContentScaleState.isZooming -> InfoView(info = "${(videoZoomAndContentScaleState.zoom * 100).toInt()}%")
                                videoZoomAndContentScaleState.showContentScaleIndicator -> InfoView(info = stringResource(videoZoomAndContentScaleState.videoContentScale.nameRes()))
                                controlsVisibilityState.controlsVisible -> ControlsMiddleView(
                                    player = player,
                                    seekDurationSeconds = playerPreferences.seekIncrement,
                                )
                                else -> Unit
                            }
                        },
                        bottomView = {
                            AnimatedVisibility(
                                visible = controlsVisibilityState.controlsVisible && !controlsVisibilityState.controlsLocked,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                ControlsBottomView(
                                    player = player,
                                    mediaPresentationState = mediaPresentationState,
                                    controlsAlignment = when (playerPreferences.controlButtonsPosition) {
                                        ControlButtonsPosition.LEFT -> Alignment.Start
                                        ControlButtonsPosition.RIGHT -> Alignment.End
                                    },
                                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                                    isPipSupported = pictureInPictureState.isPipSupported,
                                    onSeek = seekGestureState::onSeek,
                                    onSeekEnd = seekGestureState::onSeekEnd,
                                    onRotateClick = rotationState::rotate,
                                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                                    onPlaylistClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.PLAYLIST
                                    },
                                    showLikeButton = isTelegramMedia,
                                    isLiked = isCurrentMediaLiked,
                                    onToggleLike = {
                                        localLikeOverride = !isCurrentMediaLiked
                                        val key = currentMediaUriStr.ifEmpty { metadataState.title ?: "Playing Video" }
                                        viewModel.toggleLikeMedia(metadataState.title ?: "Playing Video", key)
                                    },
                                    onUsbCastClick = {
                                        showUsbCastSheet = true
                                    },
                                    onLockControlsClick = {

                                        controlsVisibilityState.showControls()
                                        controlsVisibilityState.lockControls()
                                    },
                                    onVideoContentScaleClick = {
                                        controlsVisibilityState.showControls()
                                        videoZoomAndContentScaleState.switchToNextVideoContentScale()
                                    },
                                    onVideoContentScaleLongClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.VIDEO_CONTENT_SCALE
                                    },
                                    onPictureInPictureClick = {
                                        if (!pictureInPictureState.hasPipPermission()) {
                                            Toast.makeText(context, coreUiR.string.enable_pip_from_settings, Toast.LENGTH_SHORT).show()
                                            pictureInPictureState.openPictureInPictureSettings()
                                        } else {
                                            pictureInPictureState.enterPictureInPictureMode()
                                        }
                                    },
                                    sliderStyle = appPreferences.playerSliderStyle,
                                    showThumbnails = playerPreferences.showSeekbarThumbnails,
                                    thumbnailState = seekbarThumbnailState,
                                    audioOutputDevice = audioOutputState.currentDevice,
                                    onAudioOutputClick = {
                                        showAudioOutputSheet = true
                                    },
                                )
                            }
                        },
                    )
                }

                DoubleTapIndicator(tapGestureState = tapGestureState)

                val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .displayCutoutPadding()
                        .padding(systemBarsPadding.copy(top = 0.dp, bottom = 0.dp))
                        .padding(24.dp),
                ) {
                    AnimatedVisibility(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp),
                        visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.BRIGHTNESS,
                        enter = fadeIn(tween(durationMillis = 250, easing = LinearOutSlowInEasing)) +
                            slideInHorizontally(tween(durationMillis = 250, easing = LinearOutSlowInEasing), initialOffsetX = { -it }),
                        exit = fadeOut(tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(tween(durationMillis = 300, easing = FastOutSlowInEasing), targetOffsetX = { -it }),
                    ) {
                        VerticalProgressView(
                            value = volumeAndBrightnessGestureState.displayBrightnessPercentage,
                            isVolume = false,
                        )
                    }

                    AnimatedVisibility(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                        visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.VOLUME,
                        enter = fadeIn(tween(durationMillis = 250, easing = LinearOutSlowInEasing)) +
                            slideInHorizontally(tween(durationMillis = 250, easing = LinearOutSlowInEasing), initialOffsetX = { it }),
                        exit = fadeOut(tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(tween(durationMillis = 300, easing = FastOutSlowInEasing), targetOffsetX = { it }),
                    ) {
                        VerticalProgressView(
                            value = volumeAndBrightnessGestureState.displayVolumePercentage,
                            maxValue = volumeState.maxVolumePercentage,
                            isVolume = true,
                        )
                    }
                }
            }

            OverlayShowView(
                player = player,
                overlayView = overlayView,
                videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                playerPreferences = playerPreferences,
                appPreferences = appPreferences,
                onUpdatePlayerPreferences = { viewModel.updatePlayerPreferences(it) },
                onUpdateApplicationPreferences = { viewModel.updateApplicationPreferences(it) },
                qualityUrls = uiState.qualityUrls,
                currentQualityUrl = mediaPresentationState.currentUri?.toString() ?: "",
                onDismiss = { overlayView = null },
                onSelectSubtitleClick = onSelectSubtitleClick,
                onDownloadOnlineSubtitleClick = { showOnlineSubtitlesSheet = true },
                onSubtitleOptionEvent = viewModel::onSubtitleOptionEvent,
                onVideoContentScaleChanged = { videoZoomAndContentScaleState.onVideoContentScaleChanged(it) },
                onLockControlsClick = {
                    controlsVisibilityState.lockControls()
                },
                onQualitySelected = { url ->
                    val snap = player.state.value
                    val position = snap.currentPositionMs
                    val isPlaying = snap.isPlaying
                    val currentItem = snap.currentItem
                    val updatedItem = dev.roxy.kryptonplayer.core.common.playback.PlaybackMediaItem(
                        mediaId = url,
                        uri = android.net.Uri.parse(url),
                        title = currentItem?.title ?: metadataState.title ?: "",
                        artist = currentItem?.artist,
                        artworkUri = currentItem?.artworkUri,
                    )
                    player.setPlaylist(
                        items = listOf(updatedItem),
                        startIndex = 0,
                        startPositionMs = position,
                        playWhenReady = isPlaying,
                    )
                },
                onRingtoneMakerClick = { showRingtoneMakerSheet = true },
                onClipMakerClick = { showClipMakerSheet = true },
                onLyricsClick = { showLiveLyricsSheet = true },
                onBookmarkClick = { showBookmarkSheet = true },
                onXRayClick = { showXRaySheet = true },
                onSoundtrackClick = { showSoundtrackSheet = true },
                onPlaybackSpeedClick = {
                    overlayView = OverlayView.PLAYBACK_SPEED
                },
                onPlaylistClick = {
                    overlayView = OverlayView.PLAYLIST
                },
                isProHudVisible = showProHud,
                onProHudToggle = { showProHud = !showProHud },
                onUsbCastClick = { showUsbCastSheet = true },
                onQualityClick = if (uiState.qualityUrls.size > 1) {
                    { overlayView = OverlayView.VIDEO_QUALITY }
                } else null
            )

            if (showOnlineSubtitlesSheet) {
                val currentTitle = metadataState.title?.takeIf { it.isNotBlank() }
                    ?: player.state.value.currentItem?.title?.takeIf { it.isNotBlank() }
                    ?: "Movie"
                dev.roxy.kryptonplayer.feature.player.ui.OnlineSubtitleBottomSheet(
                    viewModel = viewModel,
                    initialTitle = currentTitle,
                    onSubtitleSelected = { file ->
                        onOnlineSubtitleSelected(file)
                    },
                    onDismiss = { showOnlineSubtitlesSheet = false }
                )
            }

            if (showUsbCastSheet) {
                val currentTitle = metadataState.title?.takeIf { it.isNotBlank() }
                    ?: player.state.value.currentItem?.title?.takeIf { it.isNotBlank() }
                    ?: "Krypton Player Video"
                dev.roxy.kryptonplayer.feature.player.ui.UsbCastBottomSheet(
                    viewModel = viewModel,
                    player = player,
                    videoTitle = currentTitle,
                    onDismiss = { showUsbCastSheet = false }
                )
            }

            if (showAudioOutputSheet) {
                AudioOutputBottomSheet(
                    audioOutputState = audioOutputState,
                    onDismiss = { showAudioOutputSheet = false }
                )
            }

            if (showBookmarkSheet) {
                val currentMediaUri = mediaPresentationState.currentUri?.toString()?.takeIf { it.isNotBlank() }
                    ?: player.state.value.currentItem?.mediaId?.takeIf { it.isNotBlank() }
                    ?: metadataState.title?.takeIf { it.isNotBlank() }
                    ?: "current_media"
                dev.roxy.kryptonplayer.feature.player.ui.BookmarkBottomSheet(
                    viewModel = viewModel,
                    player = player,
                    mediaUri = currentMediaUri,
                    onSeekTo = { player.seekTo(it) },
                    onDismiss = { showBookmarkSheet = false }
                )
            }

            if (showXRaySheet) {
                val currentTitle = metadataState.title?.takeIf { it.isNotBlank() }
                    ?: player.state.value.currentItem?.title?.takeIf { it.isNotBlank() }
                    ?: "Movie"
                dev.roxy.kryptonplayer.feature.player.ui.xray.XRayCastBottomSheet(
                    player = player,
                    viewModel = viewModel,
                    videoTitle = currentTitle,
                    onDismiss = { showXRaySheet = false }
                )
            }

            if (showRingtoneMakerSheet) {
        val uri = mediaPresentationState.currentUri
        if (uri != null) {
            RingtoneMakerBottomSheet(
                sourceUri = uri,
                currentPositionMs = mediaPresentationState.position,
                durationMs = mediaPresentationState.duration,
                mediaTitle = metadataState.title ?: "Ringtone",
                onDismiss = { showRingtoneMakerSheet = false }
            )
        }
    }

    if (showClipMakerSheet) {
        val uri = mediaPresentationState.currentUri
        if (uri != null) {
            VideoClipMakerBottomSheet(
                sourceUri = uri,
                currentPositionMs = mediaPresentationState.position,
                durationMs = mediaPresentationState.duration,
                onDismiss = { showClipMakerSheet = false }
            )
        }
    }

    if (showLiveLyricsSheet) {
        LiveLyricsBottomSheet(
            viewModel = viewModel,
            player = player,
            videoTitle = metadataState.title,
            artist = metadataState.artist,
            artworkUri = metadataState.artworkUri,
            onDismiss = { showLiveLyricsSheet = false }
        )
    }

    if (showSoundtrackSheet) {
                val currentTitle = metadataState.title?.takeIf { it.isNotBlank() }
                    ?: player.state.value.currentItem?.title?.takeIf { it.isNotBlank() }
                    ?: "Movie"
                dev.roxy.kryptonplayer.feature.player.ui.soundtrack.SoundtrackFinderBottomSheet(
                    viewModel = viewModel,
                    videoTitle = currentTitle,
                    videoUri = mediaPresentationState.currentUri,
                    currentPositionMs = mediaPresentationState.position,
                    onDismiss = { showSoundtrackSheet = false }
                )
            }

            if (showProHud) {
                dev.roxy.kryptonplayer.feature.player.ui.hud.ProMediaHudOverlay(
                    player = player,
                    onClose = { showProHud = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 70.dp, start = 16.dp)
                )
            }
            
            if (showChatPanel && uiState.isConnectedToWatchParty) {
                WatchPartyChatOverlay(
                    messages = uiState.watchPartyMessages,
                    onSendMessage = { viewModel.sendChatMessage(it) },
                    onClose = { showChatPanel = false },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            if (showWatchPartyInfoDialog && uiState.watchPartyRoomId != null) {
                val roomId = uiState.watchPartyRoomId!!
                val inviteLink = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.syncJoinUrl(roomId)
                val clipboardManager = LocalClipboardManager.current
                val context = LocalContext.current

                AlertDialog(
                    onDismissRequest = { showWatchPartyInfoDialog = false },
                    title = {
                        Text(
                            text = "Watch Party Info",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Room Code Display
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Room Code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = roomId,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // QR Code Image
                            if (qrCodeBitmap != null) {
                                Image(
                                    bitmap = qrCodeBitmap!!.asImageBitmap(),
                                    contentDescription = "Watch Party QR Code",
                                    modifier = Modifier
                                        .size(180.dp)
                                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Invite Link Display & Copy Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inviteLink,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(inviteLink))
                                        Toast.makeText(context, "Invite link copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                                        contentDescription = "Copy Link",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Connected Members
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Members (${uiState.watchPartyMembers.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                ) {
                                    items(uiState.watchPartyMembers) { member ->
                                        val isHost = member.username == uiState.watchPartyHostUsername
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = member.username,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isHost) {
                                                Text(
                                                    text = "HOST",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showWatchPartyInfoDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            if (uiState.isJoiningFromDeepLink) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LoadingIndicator(
                            modifier = Modifier.size(56.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Joining Watch Party...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }


    errorState.error?.let { error ->
        dev.roxy.kryptonplayer.feature.player.ui.PlayerErrorDiagnosticsDialog(
            error = error,
            player = player,
            onDismiss = { errorState.dismiss() },
            onBackClick = {
                errorState.dismiss()
                onBackClick()
            }
        )
    }

    BackHandler {
        if (showWatchPartyInfoDialog) {
            showWatchPartyInfoDialog = false
        } else if (showChatPanel) {
            showChatPanel = false
        } else if (overlayView != null) {
            overlayView = null
        } else {
            onBackClick()
        }
    }
}

@Composable
fun WatchPartyChatOverlay(
    messages: List<WatchPartyMessage>,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {


    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 320.dp)
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Watch Party Chat",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close chat",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    if (msg.isSystem) {
                        Text(
                            text = msg.message,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = msg.sender,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = msg.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type a message...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSendMessage(text)
                            text = ""
                        }
                    },
                    enabled = text.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}


@Composable
fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = info,
            style = textStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ControlsMiddleView(
    modifier: Modifier = Modifier,
    player: PlaybackController,
    seekDurationSeconds: Int = 10,
    onSeekAnimated: ((Long) -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviousButton(player = player, modifier = Modifier.size(46.dp))
        SeekBackButton(player = player, modifier = Modifier.size(50.dp), seekDurationSeconds = seekDurationSeconds, onSeekAnimated = onSeekAnimated)
        PlayPauseButton(player = player)
        SeekForwardButton(player = player, modifier = Modifier.size(50.dp), seekDurationSeconds = seekDurationSeconds, onSeekAnimated = onSeekAnimated)
        NextButton(player = player, modifier = Modifier.size(46.dp))
    }
}

@Composable
fun PlayerControlsView(
    modifier: Modifier = Modifier,
    topView: @Composable () -> Unit,
    middleView: @Composable BoxScope.() -> Unit,
    bottomView: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            topView()
            Spacer(modifier = Modifier.weight(1f))
            bottomView()
        }

        middleView()
    }
}
