package dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker

sealed interface TmdbDetailDestination {
    data class MovieDetail(val id: Int, val isTv: Boolean) : TmdbDetailDestination
    data class ActorDetail(val id: Int) : TmdbDetailDestination
    data object TmdbSearch : TmdbDetailDestination
}
