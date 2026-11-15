package dev.anilbeesetti.nextplayer.settings.screens.subtitle

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import dev.anilbeesetti.nextplayer.core.data.repository.OnlineSubtitleRepository
import dev.anilbeesetti.nextplayer.core.data.subtitles.SubtitleTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class SubtitlePreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val onlineSubtitleRepository: OnlineSubtitleRepository,
    private val subtitleTranslator: SubtitleTranslator,
) : ViewModel() {

    private val uiStateInternal = MutableStateFlow(
        SubtitlePreferencesUiState(
            preferences = preferencesRepository.playerPreferences.value,
        ),
    )
    val uiState = uiStateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { preferences ->
                uiStateInternal.update { currentState ->
                    currentState.copy(preferences = preferences)
                }
            }
        }
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val bytes = onlineSubtitleRepository.getCacheSizeBytes() + subtitleTranslator.getCacheSizeBytes()
            val formatted = formatBytes(bytes)
            uiStateInternal.update { it.copy(cacheSizeFormatted = formatted) }
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    fun onEvent(event: SubtitlePreferencesUiEvent) {
        when (event) {
            is SubtitlePreferencesUiEvent.ShowDialog -> showDialog(event.value)
            is SubtitlePreferencesUiEvent.UpdateSubtitleLanguage -> updateSubtitleLanguage(event.value)
            is SubtitlePreferencesUiEvent.UpdateSubtitleFont -> updateSubtitleFont(event.value)
            SubtitlePreferencesUiEvent.ToggleSubtitleTextBold -> toggleSubtitleTextBold()
            is SubtitlePreferencesUiEvent.UpdateSubtitleFontSize -> updateSubtitleFontSize(event.value)
            SubtitlePreferencesUiEvent.ToggleSubtitleBackground -> toggleSubtitleBackground()
            SubtitlePreferencesUiEvent.ToggleApplyEmbeddedStyles -> toggleApplyEmbeddedStyles()
            is SubtitlePreferencesUiEvent.UpdateSubtitleEncoding -> updateSubtitleEncoding(event.value)
            SubtitlePreferencesUiEvent.ToggleUseSystemCaptionStyle -> toggleUseSystemCaptionStyle()
            SubtitlePreferencesUiEvent.ToggleAutoApplyDownloadedSubtitles -> toggleAutoApplyDownloadedSubtitles()
            SubtitlePreferencesUiEvent.ToggleAutoSearchOnlineSubtitles -> toggleAutoSearchOnlineSubtitles()
            is SubtitlePreferencesUiEvent.UpdateSubtitleTranslationTargetLang -> updateSubtitleTranslationTargetLang(event.value)
            is SubtitlePreferencesUiEvent.UpdateSubtitleTranslationEngine -> updateSubtitleTranslationEngine(event.value)
            SubtitlePreferencesUiEvent.ClearSubtitleCache -> clearSubtitleCache()
        }
    }

    private fun showDialog(value: SubtitlePreferenceDialog?) {
        uiStateInternal.update {
            it.copy(showDialog = value)
        }
    }

    private fun updateSubtitleLanguage(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(preferredSubtitleLanguage = value)
            }
        }
    }

    private fun updateSubtitleFont(value: Font) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleFont = value)
            }
        }
    }

    private fun toggleSubtitleTextBold() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleTextBold = !it.subtitleTextBold)
            }
        }
    }

    private fun updateSubtitleFontSize(value: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleTextSize = value)
            }
        }
    }

    private fun toggleSubtitleBackground() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleBackground = !it.subtitleBackground)
            }
        }
    }

    private fun toggleApplyEmbeddedStyles() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(applyEmbeddedStyles = !it.applyEmbeddedStyles)
            }
        }
    }

    private fun updateSubtitleEncoding(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(subtitleTextEncoding = value) }
        }
    }

    private fun toggleUseSystemCaptionStyle() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(useSystemCaptionStyle = !it.useSystemCaptionStyle) }
        }
    }

    private fun toggleAutoApplyDownloadedSubtitles() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(autoApplyDownloadedSubtitles = !it.autoApplyDownloadedSubtitles) }
        }
    }

    private fun toggleAutoSearchOnlineSubtitles() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(autoSearchOnlineSubtitles = !it.autoSearchOnlineSubtitles) }
        }
    }

    private fun updateSubtitleTranslationTargetLang(lang: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(subtitleTranslationTargetLang = lang) }
        }
    }

    private fun updateSubtitleTranslationEngine(engine: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(subtitleTranslationEngine = engine) }
        }
    }

    private fun clearSubtitleCache() {
        viewModelScope.launch(Dispatchers.IO) {
            onlineSubtitleRepository.clearCache()
            subtitleTranslator.clearCache()
            refreshCacheSize()
        }
    }
}

@Stable
data class SubtitlePreferencesUiState(
    val showDialog: SubtitlePreferenceDialog? = null,
    val preferences: PlayerPreferences = PlayerPreferences(),
    val cacheSizeFormatted: String = "0 B",
)

sealed interface SubtitlePreferenceDialog {
    data object SubtitleLanguageDialog : SubtitlePreferenceDialog
    data object SubtitleFontDialog : SubtitlePreferenceDialog
    data object SubtitleEncodingDialog : SubtitlePreferenceDialog
    data object SubtitleTranslationLanguageDialog : SubtitlePreferenceDialog
    data object SubtitleTranslationEngineDialog : SubtitlePreferenceDialog
}

sealed interface SubtitlePreferencesUiEvent {
    data class ShowDialog(val value: SubtitlePreferenceDialog?) : SubtitlePreferencesUiEvent
    data class UpdateSubtitleLanguage(val value: String) : SubtitlePreferencesUiEvent
    data class UpdateSubtitleFont(val value: Font) : SubtitlePreferencesUiEvent
    data object ToggleSubtitleTextBold : SubtitlePreferencesUiEvent
    data class UpdateSubtitleFontSize(val value: Int) : SubtitlePreferencesUiEvent
    data object ToggleSubtitleBackground : SubtitlePreferencesUiEvent
    data object ToggleApplyEmbeddedStyles : SubtitlePreferencesUiEvent
    data class UpdateSubtitleEncoding(val value: String) : SubtitlePreferencesUiEvent
    data object ToggleUseSystemCaptionStyle : SubtitlePreferencesUiEvent
    data object ToggleAutoApplyDownloadedSubtitles : SubtitlePreferencesUiEvent
    data object ToggleAutoSearchOnlineSubtitles : SubtitlePreferencesUiEvent
    data class UpdateSubtitleTranslationTargetLang(val value: String) : SubtitlePreferencesUiEvent
    data class UpdateSubtitleTranslationEngine(val value: String) : SubtitlePreferencesUiEvent
    data object ClearSubtitleCache : SubtitlePreferencesUiEvent
}
