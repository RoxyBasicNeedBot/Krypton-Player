package dev.roxy.kryptonplayer.settings.screens.lyrics

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.data.lyrics.LyricsRepository
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.model.PlayerPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LyricsPreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val lyricsRepository: LyricsRepository,
) : ViewModel() {

    private val uiStateInternal = MutableStateFlow(
        LyricsPreferencesUiState(
            preferences = preferencesRepository.playerPreferences.value,
            cacheCount = lyricsRepository.cacheCount,
        ),
    )
    val uiState = uiStateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { preferences ->
                uiStateInternal.update { it.copy(preferences = preferences) }
            }
        }
    }

    fun onEvent(event: LyricsPreferencesUiEvent) {
        when (event) {
            is LyricsPreferencesUiEvent.ShowDialog -> showDialog(event.dialog)
            is LyricsPreferencesUiEvent.UpdateLyricsMode -> updateLyricsMode(event.mode)
            is LyricsPreferencesUiEvent.UpdateLyricsTextSize -> updateLyricsTextSize(event.size)
            is LyricsPreferencesUiEvent.UpdateLyricsLineSpacing -> updateLyricsLineSpacing(event.spacing)
            LyricsPreferencesUiEvent.ToggleLyricsLineBlur -> toggleLyricsLineBlur()
            LyricsPreferencesUiEvent.ToggleLyricsAutoScroll -> toggleLyricsAutoScroll()
            LyricsPreferencesUiEvent.ToggleLyricsClickToSeek -> toggleLyricsClickToSeek()
            LyricsPreferencesUiEvent.ToggleLyricsRomanize -> toggleLyricsRomanize()
            LyricsPreferencesUiEvent.ToggleBetterLyrics -> toggleBetterLyrics()
            LyricsPreferencesUiEvent.ToggleLrcLib -> toggleLrcLib()
            LyricsPreferencesUiEvent.ToggleKuGou -> toggleKuGou()
            LyricsPreferencesUiEvent.TogglePaxsenix -> togglePaxsenix()
            LyricsPreferencesUiEvent.ToggleNetEase -> toggleNetEase()
            LyricsPreferencesUiEvent.ClearLyricsCache -> clearLyricsCache()
        }
    }

    private fun showDialog(dialog: LyricsPreferenceDialog?) {
        uiStateInternal.update { it.copy(showDialog = dialog) }
    }

    private fun updateLyricsMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(lyricsMode = mode) }
        }
    }

    private fun updateLyricsTextSize(size: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(lyricsTextSize = size) }
        }
    }

    private fun updateLyricsLineSpacing(spacing: Float) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(lyricsLineSpacing = spacing) }
        }
    }

    private fun toggleLyricsLineBlur() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(lyricsLineBlur = !it.lyricsLineBlur) }
        }
    }

    private fun toggleLyricsAutoScroll() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(lyricsAutoScroll = !it.lyricsAutoScroll) }
        }
    }

    private fun toggleLyricsClickToSeek() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(lyricsClickToSeek = !it.lyricsClickToSeek) }
        }
    }

    private fun toggleLyricsRomanize() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(lyricsRomanize = !it.lyricsRomanize) }
        }
    }

    private fun toggleBetterLyrics() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(enableBetterLyrics = !it.enableBetterLyrics) }
        }
    }

    private fun toggleLrcLib() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(enableLrcLib = !it.enableLrcLib) }
        }
    }

    private fun toggleKuGou() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(enableKugou = !it.enableKugou) }
        }
    }

    private fun togglePaxsenix() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(enablePaxsenix = !it.enablePaxsenix) }
        }
    }

    private fun toggleNetEase() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(enableNetEase = !it.enableNetEase) }
        }
    }

    private fun clearLyricsCache() {
        lyricsRepository.clearCache()
        uiStateInternal.update { it.copy(cacheCount = 0) }
    }
}

@Stable
data class LyricsPreferencesUiState(
    val preferences: PlayerPreferences = PlayerPreferences(),
    val showDialog: LyricsPreferenceDialog? = null,
    val cacheCount: Int = 0,
)

sealed interface LyricsPreferencesUiEvent {
    data class ShowDialog(val dialog: LyricsPreferenceDialog?) : LyricsPreferencesUiEvent
    data class UpdateLyricsMode(val mode: String) : LyricsPreferencesUiEvent
    data class UpdateLyricsTextSize(val size: Int) : LyricsPreferencesUiEvent
    data class UpdateLyricsLineSpacing(val spacing: Float) : LyricsPreferencesUiEvent
    data object ToggleLyricsLineBlur : LyricsPreferencesUiEvent
    data object ToggleLyricsAutoScroll : LyricsPreferencesUiEvent
    data object ToggleLyricsClickToSeek : LyricsPreferencesUiEvent
    data object ToggleLyricsRomanize : LyricsPreferencesUiEvent
    data object ToggleBetterLyrics : LyricsPreferencesUiEvent
    data object ToggleLrcLib : LyricsPreferencesUiEvent
    data object ToggleKuGou : LyricsPreferencesUiEvent
    data object TogglePaxsenix : LyricsPreferencesUiEvent
    data object ToggleNetEase : LyricsPreferencesUiEvent
    data object ClearLyricsCache : LyricsPreferencesUiEvent
}

enum class LyricsPreferenceDialog {
    MODE,
    TEXT_SIZE,
    LINE_SPACING,
    CLEAR_CACHE,
}
