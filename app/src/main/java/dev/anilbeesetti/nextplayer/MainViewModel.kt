package dev.anilbeesetti.nextplayer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.update.ApkAsset
import dev.anilbeesetti.nextplayer.core.data.update.AppUpdateInfo
import dev.anilbeesetti.nextplayer.core.data.update.AppUpdateManager
import dev.anilbeesetti.nextplayer.core.data.update.GitHubCommitInfo
import dev.anilbeesetti.nextplayer.core.data.update.UpdateState
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val appUpdateManager: AppUpdateManager,
) : ViewModel() {

    val updateState: StateFlow<UpdateState> = appUpdateManager.updateState
    val gitHubCommits: StateFlow<List<GitHubCommitInfo>> = appUpdateManager.gitHubCommits

    init {
        viewModelScope.launch {
            delay(2500) // gentle initial delay for smooth app launch
            try {
                appUpdateManager.checkForUpdates(manual = false)
            } catch (_: Exception) {}
        }
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


    fun checkForUpdates(manual: Boolean = true) {
        viewModelScope.launch {
            appUpdateManager.checkForUpdates(manual = manual)
        }
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
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data class Success(
        val preferences: ApplicationPreferences,
        val playerPreferences: PlayerPreferences,
    ) : MainActivityUiState
}
