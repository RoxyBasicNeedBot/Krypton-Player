package dev.anilbeesetti.nextplayer.core.model

/**
 * Represents an entertainment highlight, movie buzz, celebrity update,
 * or breaking scoop in the JioHotstar-style Explore experience.
 */
data class EntertainmentHighlight(
    val id: String,
    val title: String,
    val summary: String,
    val fullStory: String,
    val category: HighlightCategory,
    val bannerUrl: String?,
    val actorName: String? = null,
    val actorAvatarUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = "Just now",
    val trailerUrl: String? = null,
    val rating: Float? = null,
    val tags: List<String> = emptyList(),
    val viewsCount: String = "124K views"
)

enum class HighlightCategory(val label: String, val badgeColorHex: Long) {
    TRENDING("✦ TRENDING", 0xFF00ED82),
    EXCLUSIVE("🔥 EXCLUSIVE", 0xFFFF3B30),
    STAR_SPOTLIGHT("🌟 SPOTLIGHT", 0xFF0075FF),
    FIRST_LOOK("🎬 FIRST LOOK", 0xFFFF9500),
    BREAKING("⚡ BREAKING", 0xFFAF52DE),
    BOX_OFFICE("🍿 BOX OFFICE", 0xFFFFCC00)
}

/**
 * Represents a featured celebrity/actor spotlight in the story circle row.
 */
data class CelebritySpotlight(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val knownFor: String,
    val latestBuzz: String,
    val isVerified: Boolean = true
)
