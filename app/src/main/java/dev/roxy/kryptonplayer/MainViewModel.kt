package dev.roxy.kryptonplayer

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.data.update.ApkAsset
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.AppUpdateManager
import dev.roxy.kryptonplayer.core.data.update.GitHubCommitInfo
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import dev.roxy.kryptonplayer.core.model.ApplicationPreferences
import dev.roxy.kryptonplayer.core.model.PlayerPreferences
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val appUpdateManager: AppUpdateManager,
) : ViewModel() {

    private val onboardingPrefs: SharedPreferences =
        context.getSharedPreferences(ONBOARDING_PREFS_NAME, Context.MODE_PRIVATE)

    private val _isOnboardingCompleted = MutableStateFlow(
        onboardingPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false),
    )
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val onboardingPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == KEY_ONBOARDING_COMPLETED) {
            _isOnboardingCompleted.value = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        }
    }

    val updateState: StateFlow<UpdateState> = appUpdateManager.updateState
    val gitHubCommits: StateFlow<List<GitHubCommitInfo>> = appUpdateManager.gitHubCommits

    init {
        onboardingPrefs.registerOnSharedPreferenceChangeListener(onboardingPrefsListener)

        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { prefs ->
                if (prefs.onboardingCompleted && !_isOnboardingCompleted.value) {
                    onboardingPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
                    _isOnboardingCompleted.value = true
                }
            }
        }

        viewModelScope.launch {
            delay(2500) // gentle initial delay for smooth app launch
            try {
                appUpdateManager.checkForUpdates(manual = false)
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        onboardingPrefs.unregisterOnSharedPreferenceChangeListener(onboardingPrefsListener)
        super.onCleared()
    }

    val uiState = combine(
        preferencesRepository.applicationPreferences,
        preferencesRepository.playerPreferences
    ) { appPrefs, playerPrefs ->
        MainActivityUiState.Success(
            preferences = appPrefs,
            playerPreferences = playerPrefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MainActivityUiState.Success(
            preferences = preferencesRepository.applicationPreferences.value,
            playerPreferences = preferencesRepository.playerPreferences.value
        ),
    )

    fun completeOnboarding() {
        onboardingPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
        _isOnboardingCompleted.value = true
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(onboardingCompleted = true)
            }
        }
    }

    fun setGestureControlsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(
                    enableVolumeSwipeGesture = enabled,
                    enableBrightnessSwipeGesture = enabled,
                    useLongPressControls = enabled,
                )
            }
        }
    }

    fun setLyricsBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(lyricsLineBlur = enabled)
            }
        }
    }

    fun setAutoPipEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(autoPip = enabled)
            }
        }
    }

    fun checkForUpdates(manual: Boolean = true) {
        viewModelScope.launch {
            if (manual) {
                appUpdateManager.clearUpdatePromptSnooze()
            }
            appUpdateManager.checkForUpdates(manual = manual)
        }
    }

    fun snoozeUpdatePrompt() {
        appUpdateManager.snoozeUpdatePrompt()
    }

    fun isUpdatePromptSnoozed(): Boolean {
        return appUpdateManager.isUpdatePromptSnoozed()
    }

    fun skipVersion(tag: String) {
        appUpdateManager.skipVersion(tag)
    }

    fun cancelDownload() {
        appUpdateManager.cancelDownload()
    }

    fun downloadAndInstall(info: AppUpdateInfo, asset: ApkAsset? = null) {
        appUpdateManager.downloadAndInstall(info, asset)
    }

    fun installApk(uri: Uri? = null) {
        appUpdateManager.installApk(uri)
    }

    fun resumePendingInstall(): Boolean {
        return appUpdateManager.resumePendingInstall()
    }

    fun resetUpdateState() {
        appUpdateManager.resetState()
    }

    companion object {
        const val ONBOARDING_PREFS_NAME = "krypton_onboarding_prefs"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data class Success(
        val preferences: ApplicationPreferences,
        val playerPreferences: PlayerPreferences,
    ) : MainActivityUiState
}
