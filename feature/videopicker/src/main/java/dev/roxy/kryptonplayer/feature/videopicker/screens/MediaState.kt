package dev.roxy.kryptonplayer.feature.videopicker.screens

import dev.roxy.kryptonplayer.core.model.Folder

sealed interface MediaState {
    data object Loading : MediaState
    data class Success(val data: Folder?) : MediaState
}
