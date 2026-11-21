package dev.roxy.kryptonplayer.core.model

/**
 * Represents a giant billboard hero item in the Google TV-style "For You" carousel.
 */
data class ForYouFeaturedItem(
    val id: String,
    val title: String,
    val tagline: String,
    val overview: String,
    val backdropUrl: String,
    val posterUrl: String? = null,
    val rating: Float = 8.5f,
    val matchScore: Int = 98, // e.g., 98% Match
    val releaseYear: String = "2026",
    val ageRating: String = "U/A 16+",
    val qualityBadges: List<String> = listOf("4K UHD", "Dolby Atmos", "HDR10+"),
    val genres: List<String> = listOf("Action", "Thriller"),
    val trailerUrl: String? = null,
    val streamUri: String? = null,
    val isHotstarSpecial: Boolean = true
)
