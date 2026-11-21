package dev.roxy.kryptonplayer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerPreferences(
    val resume: Resume = Resume.YES,
    val rememberPlayerBrightness: Boolean = false,
    val playerBrightness: Float = 0.5f,
    val minDurationForFastSeek: Long = 120000L,
    val rememberSelections: Boolean = true,
    val playerScreenOrientation: ScreenOrientation = ScreenOrientation.PORTRAIT,
    val playerVideoZoom: VideoContentScale = VideoContentScale.BEST_FIT,
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoplay: Boolean = true,
    val autoPip: Boolean = true,
    val autoBackgroundPlay: Boolean = false,
    val loopMode: LoopMode = LoopMode.OFF,

    // Controls (Gestures)
    @Deprecated(message = "Use individual enableVolumeSwipeGesture and enableBrightnessSwipeGesture instead")
    val useSwipeControls: Boolean = true,
    val enableVolumeSwipeGesture: Boolean = true,
    val enableBrightnessSwipeGesture: Boolean = true,
    val useSeekControls: Boolean = true,
    val useZoomControls: Boolean = true,
    val enablePanGesture: Boolean = false,
    val doubleTapGesture: DoubleTapGesture = DoubleTapGesture.BOTH,
    val useLongPressControls: Boolean = true,
    val longPressControlsSpeed: Float = 2.0f,
    val seekIncrement: Int = DEFAULT_SEEK_INCREMENT,
    val seekSensitivity: Float = DEFAULT_SEEK_SENSITIVITY,
    val volumeGestureSensitivity: Float = DEFAULT_VOLUME_GESTURE_SENSITIVITY,
    val brightnessGestureSensitivity: Float = DEFAULT_BRIGHTNESS_GESTURE_SENSITIVITY,

    // Player Interface
    val controllerAutoHideTimeout: Int = DEFAULT_CONTROLLER_AUTO_HIDE_TIMEOUT,
    val controlButtonsPosition: ControlButtonsPosition = ControlButtonsPosition.LEFT,
    val hidePlayerButtonsBackground: Boolean = false,
    val useMaterialYouControls: Boolean = false,
    val showSeekbarThumbnails: Boolean = true,

    // Audio Preferences
    val preferredAudioLanguage: String = "",
    val pauseOnHeadsetDisconnect: Boolean = true,
    val requireAudioFocus: Boolean = true,
    val showSystemVolumePanel: Boolean = true,
    val enableVolumeBoost: Boolean = true,

    // Subtitle Preferences
    val useSystemCaptionStyle: Boolean = false,
    val preferredSubtitleLanguage: String = "",
    val subtitleTextEncoding: String = "",
    val subtitleTextSize: Int = DEFAULT_SUBTITLE_TEXT_SIZE,
    val subtitleBackground: Boolean = false,
    val subtitleFont: Font = Font.DEFAULT,
    val subtitleTextBold: Boolean = true,
    val applyEmbeddedStyles: Boolean = true,
    val autoApplyDownloadedSubtitles: Boolean = true,
    val autoSearchOnlineSubtitles: Boolean = true,
    val subtitleTranslationTargetLang: String = "en",
    val subtitleTranslationEngine: String = "auto",

    // Lyrics Preferences (ArchiveTune Style)
    val lyricsMode: String = "ENHANCED",
    val lyricsTextSize: Int = DEFAULT_LYRICS_TEXT_SIZE,
    val lyricsLineSpacing: Float = 1.3f,
    val lyricsLineBlur: Boolean = true,
    val lyricsAutoScroll: Boolean = true,
    val lyricsClickToSeek: Boolean = true,
    val lyricsSyncOffset: Int = 0,
    val lyricsRomanize: Boolean = true,
    val enableLrcLib: Boolean = true,
    val enableKugou: Boolean = true,
    val enableBetterLyrics: Boolean = true,
    val enablePaxsenix: Boolean = true,
    val enableNetEase: Boolean = true,

    // Decoder Preferences
    val decoderPriority: DecoderPriority = DecoderPriority.PREFER_DEVICE,

    // Onboarding
    val onboardingCompleted: Boolean = false,

    // Telegram Cloud Multi-User & Multi-Account Switcher
    val telegramToken: String = "",
    val telegramUserId: Long = 0L,
    val telegramUsername: String = "",
    val telegramFirstName: String = "",
    val telegramProfilePhoto: String = "",
    val telegramBotToken: String = "",
    val telegramApiId: Int = 0,
    val telegramApiHash: String = "",
    val telegramChannelScrollPositions: Map<String, String> = emptyMap(),
    val savedTelegramAccounts: List<TelegramAccount> = emptyList(),

    // Telegram Storage & Cache Settings
    val keepMediaIndex: Int = 3, // 0: 3 days, 1: 1 week, 2: 1 month, 3: Forever
    val maxCacheSizeIndex: Int = 4, // 0: 2 GB, 1: 5 GB, 2: 10 GB, 3: 50 GB, 4: No limit / Unlimited
    val autoSaveVideosToGallery: Boolean = false,
    val autoSavePhotosToGallery: Boolean = false,
    val autoDownloadMobileData: Boolean = false,
    val autoDownloadWifi: Boolean = false,
) {

    companion object {
        const val DEFAULT_SEEK_INCREMENT = 10
        const val DEFAULT_SEEK_SENSITIVITY = 0.50f
        const val DEFAULT_VOLUME_GESTURE_SENSITIVITY = 0.50f
        const val DEFAULT_BRIGHTNESS_GESTURE_SENSITIVITY = 0.50f
        const val DEFAULT_SUBTITLE_TEXT_SIZE = 20
        const val DEFAULT_LYRICS_TEXT_SIZE = 24
        const val DEFAULT_CONTROLLER_AUTO_HIDE_TIMEOUT = 4
    }
}
