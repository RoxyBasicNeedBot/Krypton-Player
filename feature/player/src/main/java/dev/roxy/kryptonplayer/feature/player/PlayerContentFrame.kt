package dev.roxy.kryptonplayer.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.viewinterop.AndroidView
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.model.PlayerPreferences
import dev.roxy.kryptonplayer.core.model.VideoContentScale
import dev.roxy.kryptonplayer.feature.player.mpv.Anime4KManager
import dev.roxy.kryptonplayer.feature.player.mpv.HdrScreenMode
import dev.roxy.kryptonplayer.feature.player.mpv.HdrToysProfile
import dev.roxy.kryptonplayer.feature.player.mpv.MPVView
import dev.roxy.kryptonplayer.feature.player.mpv.PlaybackSession
import dev.roxy.kryptonplayer.feature.player.state.ControlsVisibilityState
import dev.roxy.kryptonplayer.feature.player.state.PictureInPictureState
import dev.roxy.kryptonplayer.feature.player.state.SeekGestureState
import dev.roxy.kryptonplayer.feature.player.state.SubtitleOptionsState
import dev.roxy.kryptonplayer.feature.player.state.TapGestureState
import dev.roxy.kryptonplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.roxy.kryptonplayer.feature.player.state.VolumeAndBrightnessGestureState
import dev.roxy.kryptonplayer.feature.player.ui.PlayerGestures
import dev.roxy.kryptonplayer.feature.player.ui.ShutterView
import kotlin.math.ln

@Composable
fun PlayerContentFrame(
    modifier: Modifier = Modifier,
    player: PlaybackController,
    playerPreferences: PlayerPreferences,
    pictureInPictureState: PictureInPictureState,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    volumeAndBrightnessGestureState: VolumeAndBrightnessGestureState,
    subtitleOptionsState: SubtitleOptionsState,
) {
    val snapshot by player.state.collectAsState()
    var mpvViewRef by remember { mutableStateOf<MPVView?>(null) }
    var viewWidthPx by remember { mutableStateOf(1) }
    var viewHeightPx by remember { mutableStateOf(1) }

    LaunchedEffect(videoZoomAndContentScaleState.videoContentScale) {
        if (!PlaybackSession.isInitialized) return@LaunchedEffect
        when (videoZoomAndContentScaleState.videoContentScale) {
            VideoContentScale.BEST_FIT -> {
                PlaybackSession.setPropertyString("keepaspect", "yes")
                PlaybackSession.setPropertyDouble("panscan", 0.0)
                PlaybackSession.setPropertyString("video-unscaled", "no")
            }
            VideoContentScale.STRETCH -> {
                PlaybackSession.setPropertyString("keepaspect", "no")
                PlaybackSession.setPropertyDouble("panscan", 0.0)
                PlaybackSession.setPropertyString("video-unscaled", "no")
            }
            VideoContentScale.CROP -> {
                PlaybackSession.setPropertyString("keepaspect", "yes")
                PlaybackSession.setPropertyDouble("panscan", 1.0)
                PlaybackSession.setPropertyString("video-unscaled", "no")
            }
            VideoContentScale.HUNDRED_PERCENT -> {
                PlaybackSession.setPropertyString("keepaspect", "yes")
                PlaybackSession.setPropertyDouble("panscan", 0.0)
                PlaybackSession.setPropertyString("video-unscaled", "yes")
            }
        }
    }

    LaunchedEffect(
        videoZoomAndContentScaleState.zoom,
        videoZoomAndContentScaleState.offset,
        viewWidthPx,
        viewHeightPx,
    ) {
        if (!PlaybackSession.isInitialized) return@LaunchedEffect
        val zoom = videoZoomAndContentScaleState.zoom.coerceIn(0.25f, 4.0f)
        val log2Zoom = ln(zoom.toDouble()) / ln(2.0)
        PlaybackSession.setPropertyDouble("video-zoom", log2Zoom)
        val panX = (videoZoomAndContentScaleState.offset.x / viewWidthPx.coerceAtLeast(1)).toDouble().coerceIn(-1.0, 1.0)
        val panY = (videoZoomAndContentScaleState.offset.y / viewHeightPx.coerceAtLeast(1)).toDouble().coerceIn(-1.0, 1.0)
        PlaybackSession.setPropertyDouble("video-pan-x", panX)
        PlaybackSession.setPropertyDouble("video-pan-y", panY)
    }

    LaunchedEffect(snapshot.anime4kMode) {
        val mode = Anime4KManager.Mode.entries.firstOrNull {
            it.name.equals(snapshot.anime4kMode, ignoreCase = true)
        } ?: Anime4KManager.Mode.OFF
        mpvViewRef?.applyAnime4KMode(mode)
    }

    LaunchedEffect(snapshot.hdrMode) {
        val screenMode = HdrScreenMode.entries.firstOrNull {
            it.name.equals(snapshot.hdrMode, ignoreCase = true)
        } ?: HdrScreenMode.OFF
        val toysProfile = HdrToysProfile.entries.firstOrNull {
            it.name.equals(snapshot.hdrMode, ignoreCase = true)
        } ?: HdrToysProfile.OFF
        mpvViewRef?.applyHdrMode(screenMode, toysProfile)
    }

    DisposableEffect(mpvViewRef) {
        onDispose {
            mpvViewRef?.releaseSurface()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    viewWidthPx = coordinates.size.width.coerceAtLeast(1)
                    viewHeightPx = coordinates.size.height.coerceAtLeast(1)
                    val rect = coordinates.boundsInWindow()
                    pictureInPictureState.setVideoViewRect(
                        android.graphics.Rect(
                            rect.left.toInt(),
                            rect.top.toInt(),
                            rect.right.toInt(),
                            rect.bottom.toInt(),
                        ),
                    )
                },
            factory = { ctx ->
                MPVView.create(ctx).apply {
                    mpvViewRef = this
                    initializeSession(playerPreferences)
                }
            },
            update = { view ->
                mpvViewRef = view
                if (!view.isSurfaceReady) {
                    view.rebindCurrentSurface()
                }
            },
        )

        if (!snapshot.isInitialized || (snapshot.isLoading && snapshot.currentPositionMs == 0L && !snapshot.isPlaying)) {
            ShutterView()
        }

        PlayerGestures(
            controlsVisibilityState = controlsVisibilityState,
            tapGestureState = tapGestureState,
            seekGestureState = seekGestureState,
            videoZoomAndContentScaleState = videoZoomAndContentScaleState,
            volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
            pictureInPictureState = pictureInPictureState,
        )
    }
}
