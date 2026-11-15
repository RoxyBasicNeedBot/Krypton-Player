package dev.anilbeesetti.nextplayer.core.data.network.models

enum class StillSource(val displayName: String, val badgeText: String) {
    ALL("All Stills", "STILL"),
    TMDB("TMDB 4K", "4K UHT"),
    IMDB("IMDb", "IMDb STILL"),
    FANART("Fanart.tv", "FANART 1080p"),
    FANCAPS("Frame-by-Frame", "FANCAPS FRAME"),
    DUCKDUCKGO("DuckDuckGo", "HD PHOTO")
}

data class MediaStill(
    val id: String,
    val thumbnailUrl: String,
    val fullUrl: String,
    val source: StillSource,
    val width: Int = 0,
    val height: Int = 0,
    val caption: String? = null,
    val frameNumber: Int? = null
)

// Fanart.tv API models
data class FanartImage(
    val id: String,
    val url: String,
    val lang: String? = null,
    val likes: String? = null
)

data class FanartMovieResponse(
    val name: String? = null,
    val tmdb_id: String? = null,
    val imdb_id: String? = null,
    val moviebackground: List<FanartImage>? = null,
    val movie4kbackground: List<FanartImage>? = null
)

data class FanartTvResponse(
    val name: String? = null,
    val thetvdb_id: String? = null,
    val showbackground: List<FanartImage>? = null,
    val seasonposter: List<FanartImage>? = null
)

// Fancaps scraping models
data class FancapsFrame(
    val frameId: String,
    val thumbnailUrl: String,
    val fullUrl: String,
    val pageNumber: Int,
    val indexOnPage: Int
)

data class FancapsPageResult(
    val title: String,
    val mediaUrl: String,
    val currentPage: Int,
    val totalPages: Int,
    val frames: List<FancapsFrame>
)

data class FancapsScrubState(
    val totalPages: Int = 1,
    val totalEstimatedFrames: Int = 0,
    val currentPage: Int = 1,
    val scrubPercentage: Float = 0f,
    val previewFrame: MediaStill? = null,
    val loadedFrames: List<MediaStill> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
