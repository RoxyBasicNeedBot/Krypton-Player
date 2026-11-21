package dev.roxy.kryptonplayer.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.data.update.ApkAsset
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.AppUpdateManager
import dev.roxy.kryptonplayer.core.data.update.GitHubCommitInfo
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
) : ViewModel() {
    val updateState: StateFlow<UpdateState> = appUpdateManager.updateState
    val gitHubCommits: StateFlow<List<GitHubCommitInfo>> = appUpdateManager.gitHubCommits

    init {
        // App update checking temporarily disabled as requested
    }

    fun checkForUpdates(currentVersionCode: Int = 0, currentVersionName: String = "", manual: Boolean = false) {
        viewModelScope.launch {
            appUpdateManager.checkForUpdates(currentVersionCode, currentVersionName, manual)
        }
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
