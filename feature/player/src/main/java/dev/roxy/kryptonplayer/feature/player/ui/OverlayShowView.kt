package dev.roxy.kryptonplayer.feature.player.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.model.ApplicationPreferences
import dev.roxy.kryptonplayer.core.model.PlayerPreferences
import dev.roxy.kryptonplayer.core.model.VideoContentScale
import dev.roxy.kryptonplayer.feature.player.extensions.noRippleClickable
import dev.roxy.kryptonplayer.feature.player.state.SubtitleOptionsEvent

@Composable
fun BoxScope.OverlayShowView(
    player: PlaybackController,
    overlayView: OverlayView?,
    videoContentScale: VideoContentScale,
    playerPreferences: PlayerPreferences = PlayerPreferences(),
    appPreferences: ApplicationPreferences = ApplicationPreferences(),
    onUpdatePlayerPreferences: ((PlayerPreferences) -> PlayerPreferences) -> Unit = {},
    onUpdateApplicationPreferences: ((ApplicationPreferences) -> ApplicationPreferences) -> Unit = {},
    qualityUrls: List<String> = emptyList(),
    currentQualityUrl: String = "",
    onDismiss: () -> Unit = {},
    onSelectSubtitleClick: () -> Unit = {},
    onDownloadOnlineSubtitleClick: () -> Unit = {},
    onSubtitleOptionEvent: (SubtitleOptionsEvent) -> Unit = {},
    onVideoContentScaleChanged: (VideoContentScale) -> Unit = {},
    onLockControlsClick: () -> Unit = {},
    onQualitySelected: (String) -> Unit = {},
    onRingtoneMakerClick: () -> Unit = {},
    onClipMakerClick: () -> Unit = {},
    onLyricsClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onXRayClick: () -> Unit = {},
    onSoundtrackClick: () -> Unit = {},
    onPlaybackSpeedClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    isProHudVisible: Boolean = false,
    onProHudToggle: () -> Unit = {},
    onUsbCastClick: () -> Unit = {},
    onQualityClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                if (overlayView != null) {
                    Modifier.noRippleClickable(onClick = onDismiss)
                } else {
                    Modifier
                },
            ),
    )

    AudioTrackSelectorView(
        show = overlayView == OverlayView.AUDIO_SELECTOR,
        player = player,
        playerPreferences = playerPreferences,
        onUpdatePlayerPreferences = onUpdatePlayerPreferences,
        onDismiss = onDismiss,
    )

    SubtitleSelectorView(
        show = overlayView == OverlayView.SUBTITLE_SELECTOR,
        player = player,
        onSelectSubtitleClick = onSelectSubtitleClick,
        onDownloadOnlineSubtitleClick = onDownloadOnlineSubtitleClick,
        onEvent = onSubtitleOptionEvent,
        playerPreferences = playerPreferences,
        onUpdatePlayerPreferences = onUpdatePlayerPreferences,
        onDismiss = onDismiss,
    )

    PlaybackSpeedSelectorView(
        show = overlayView == OverlayView.PLAYBACK_SPEED,
        player = player,
    )

    VideoContentScaleSelectorView(
        show = overlayView == OverlayView.VIDEO_CONTENT_SCALE,
        videoContentScale = videoContentScale,
        onVideoContentScaleChanged = onVideoContentScaleChanged,
        onDismiss = onDismiss,
    )

    PlaylistView(
        show = overlayView == OverlayView.PLAYLIST,
        player = player,
    )

    VideoQualitySelectorView(
        show = overlayView == OverlayView.VIDEO_QUALITY,
        qualities = qualityUrls,
        currentQualityUrl = currentQualityUrl,
        onQualitySelected = onQualitySelected,
        onDismiss = onDismiss,
    )

    QuickToolsSelectorView(
        show = overlayView == OverlayView.QUICK_TOOLS,
        player = player,
        playerPreferences = playerPreferences,
        appPreferences = appPreferences,
        onUpdatePlayerPreferences = onUpdatePlayerPreferences,
        onUpdateApplicationPreferences = onUpdateApplicationPreferences,
        currentVideoScale = videoContentScale,
        onVideoContentScaleChanged = onVideoContentScaleChanged,
        onLockControlsClick = onLockControlsClick,
        onDismiss = onDismiss,
        onRingtoneMakerClick = onRingtoneMakerClick,
        onClipMakerClick = onClipMakerClick,
        onLyricsClick = onLyricsClick,
        onBookmarkClick = onBookmarkClick,
        onXRayClick = onXRayClick,
        onSoundtrackClick = onSoundtrackClick,
        onPlaybackSpeedClick = onPlaybackSpeedClick,
        onPlaylistClick = onPlaylistClick,
        isProHudVisible = isProHudVisible,
        onProHudToggle = onProHudToggle,
        onUsbCastClick = onUsbCastClick,
        onQualityClick = onQualityClick
    )
}

val Configuration.isPortrait: Boolean
    get() = orientation == Configuration.ORIENTATION_PORTRAIT

enum class OverlayView {
    AUDIO_SELECTOR,
    SUBTITLE_SELECTOR,
    PLAYBACK_SPEED,
    VIDEO_CONTENT_SCALE,
    PLAYLIST,
    VIDEO_QUALITY,
    QUICK_TOOLS,
}
