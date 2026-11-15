package dev.anilbeesetti.nextplayer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationPreferences(
    val sortBy: Sort.By = Sort.By.TITLE,
    val sortOrder: Sort.Order = Sort.Order.ASCENDING,
    val themeConfig: ThemeConfig = ThemeConfig.ON,
    val useHighContrastDarkTheme: Boolean = false,
    val useDynamicColors: Boolean = true,
    val markLastPlayedMedia: Boolean = true,
    val excludeFolders: List<String> = emptyList(),
    val mediaViewMode: MediaViewMode = MediaViewMode.FOLDERS,
    val mediaLayoutMode: MediaLayoutMode = MediaLayoutMode.LIST,

    // Fields
    val showDurationField: Boolean = true,
    val showExtensionField: Boolean = false,
    val showPathField: Boolean = true,
    val showResolutionField: Boolean = false,
    val showSizeField: Boolean = false,
    val showThumbnailField: Boolean = true,
    val showPlayedProgress: Boolean = true,

    // Thumbnail generation
    val thumbnailGenerationStrategy: ThumbnailGenerationStrategy = ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE,
    val thumbnailFramePosition: Float = DEFAULT_THUMBNAIL_FRAME_POSITION,

    // Dynamic theme & expressive styling preferences (from ArchiveTune)
    val customThemeValue: String = "",
    val randomThemeOnStartup: Boolean = false,
    val pureBlack: Boolean = true,
    val enableMeshBackground: Boolean = false,
    val disableBlur: Boolean = false,
    val blurRadius: Float = 36f,
    val useSystemFont: Boolean = false,
    val playerDesignStyle: String = "V4",
    val playerBackgroundStyle: String = "DEFAULT",
    val hidePlayerThumbnail: Boolean = false,
    val playerButtonsStyle: String = "DEFAULT",
    val playerSliderStyle: String = "STANDARD",
    val smartTrimmer: Boolean = false,
    val maxSongCacheSize: Int = 1024,
    val maxImageCacheSize: Int = 512,
    val downloadDirectory: String = "",
    val downloadExtractAudio: Boolean = false,
    val downloadVideoResolution: Int = 3,
    val downloadVideoFormat: Int = 0,
    val downloadSubtitles: Boolean = false,
    val downloadEmbedSubtitles: Boolean = true,
    val downloadSubtitleLanguage: String = "en",
    val downloadUseAria2c: Boolean = true,
    val downloadUseArchive: Boolean = true,
    val downloadProxyEnabled: Boolean = false,
    val downloadProxyUrl: String = "",
    val downloadAudioFormat: String = "mp3",
    val downloadAllowPlaylist: Boolean = false,
    val downloadEmbedThumbnail: Boolean = true,
    val downloadCustomArguments: String = "",
    val downloadMaxConcurrent: Int = 5,
    val downloadAria2cConnections: Int = 16,
    // "ANY", "WIFI", "MOBILE"
    val downloadNetworkRestriction: String = "ANY",
    val downloadSponsorBlockEnabled: Boolean = false,
    val downloadSleepInterval: Int = 0,
    val downloadPlayerClient: String = "default",
    val metadataProvider: String = "TMDB",
    val navigationBarStyle: String = "CURVED_DOCK",
) {

    companion object {
        const val DEFAULT_THUMBNAIL_FRAME_POSITION = 0.33f
    }
}
