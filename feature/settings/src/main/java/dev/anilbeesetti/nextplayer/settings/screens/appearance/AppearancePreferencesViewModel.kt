package dev.anilbeesetti.nextplayer.settings.screens.appearance

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.ThemeConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppearancePreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val uiStateInternal = MutableStateFlow(
        AppearancePreferencesUiState(
            preferences = preferencesRepository.applicationPreferences.value,
        ),
    )
    val uiState = uiStateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect { preferences ->
                uiStateInternal.update { it.copy(preferences = preferences) }
            }
        }
    }

    fun onEvent(event: AppearancePreferencesEvent) {
        when (event) {
            is AppearancePreferencesEvent.ShowDialog -> showDialog(event.value)
            AppearancePreferencesEvent.ToggleDarkTheme -> toggleDarkTheme()
            is AppearancePreferencesEvent.UpdateThemeConfig -> updateThemeConfig(event.themeConfig)
            AppearancePreferencesEvent.ToggleUseDynamicColors -> toggleUseDynamicColors()
            AppearancePreferencesEvent.ToggleUseHighContrastDarkTheme -> toggleUseHighContrastDarkTheme()
            is AppearancePreferencesEvent.UpdatePlayerSliderStyle -> updatePlayerSliderStyle(event.style)
            AppearancePreferencesEvent.TogglePureBlack -> togglePureBlack()
            AppearancePreferencesEvent.ToggleEnableMeshBackground -> toggleEnableMeshBackground()
            is AppearancePreferencesEvent.UpdateNavigationBarStyle -> updateNavigationBarStyle(event.style)
        }
    }

    private fun showDialog(value: AppearancePreferenceDialog?) {
        uiStateInternal.update {
            it.copy(showDialog = value)
        }
    }

    private fun toggleDarkTheme() {
        // Krypton Player is locked permanently to Dark Mode
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(themeConfig = ThemeConfig.ON)
            }
        }
    }

    private fun updateThemeConfig(themeConfig: ThemeConfig) {
        // Krypton Player is locked permanently to Dark Mode
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(themeConfig = ThemeConfig.ON)
            }
        }
    }

    private fun toggleUseDynamicColors() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(useDynamicColors = !it.useDynamicColors)
            }
        }
    }

    private fun toggleUseHighContrastDarkTheme() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(useHighContrastDarkTheme = !it.useHighContrastDarkTheme)
            }
        }
    }

    private fun togglePureBlack() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(pureBlack = !it.pureBlack)
            }
        }
    }

    private fun toggleEnableMeshBackground() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(enableMeshBackground = !it.enableMeshBackground)
            }
        }
    }

    private fun updatePlayerSliderStyle(style: String) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(playerSliderStyle = style)
            }
        }
    }

    private fun updateNavigationBarStyle(style: String) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(navigationBarStyle = style)
            }
        }
    }
}

@Stable
data class AppearancePreferencesUiState(
    val showDialog: AppearancePreferenceDialog? = null,
    val preferences: ApplicationPreferences = ApplicationPreferences(),
)

sealed interface AppearancePreferencesEvent {
    data class ShowDialog(val value: AppearancePreferenceDialog?) : AppearancePreferencesEvent
    data object ToggleDarkTheme : AppearancePreferencesEvent
    data class UpdateThemeConfig(val themeConfig: ThemeConfig) : AppearancePreferencesEvent
    data object ToggleUseDynamicColors : AppearancePreferencesEvent
    data object ToggleUseHighContrastDarkTheme : AppearancePreferencesEvent
    data class UpdatePlayerSliderStyle(val style: String) : AppearancePreferencesEvent
    data object TogglePureBlack : AppearancePreferencesEvent
    data object ToggleEnableMeshBackground : AppearancePreferencesEvent
    data class UpdateNavigationBarStyle(val style: String) : AppearancePreferencesEvent
}

sealed interface AppearancePreferenceDialog {
    data object Theme : AppearancePreferenceDialog
    data object SliderStyle : AppearancePreferenceDialog
    data object NavigationBarStyle : AppearancePreferenceDialog
}
