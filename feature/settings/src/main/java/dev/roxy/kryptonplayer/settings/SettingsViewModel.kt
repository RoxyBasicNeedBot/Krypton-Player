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
import dev.roxy.kryptonplayer.core.database.dao.MediumDao
import dev.roxy.kryptonplayer.core.database.dao.MediumStateDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HubUserStatsSummary(
    val totalWatchTimeMs: Long = 0L,
    val watchedVideosCount: Int = 0,
    val totalVideosInLibrary: Int = 0,
    val uniqueFoldersCount: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
    mediumDao: MediumDao,
    mediumStateDao: MediumStateDao,
) : ViewModel() {
    val updateState: StateFlow<UpdateState> = appUpdateManager.updateState
    val gitHubCommits: StateFlow<List<GitHubCommitInfo>> = appUpdateManager.gitHubCommits

    val userStats: StateFlow<HubUserStatsSummary> = combine(
        mediumDao.getAll(),
        mediumStateDao.getAll(),
    ) { mediaList, stateList ->
        val totalWatchTimeMs = stateList.sumOf { it.effectiveWatchDurationMs() }
        val watchedCount = stateList.count {
            it.lastPlayedTime != null || it.watchDurationMs > 0L || it.playbackPosition > 0L
        }
        val totalVideos = mediaList.size
        val foldersCount = mediaList.map { it.parentPath }.distinct().size
        HubUserStatsSummary(
            totalWatchTimeMs = totalWatchTimeMs,
            watchedVideosCount = watchedCount,
            totalVideosInLibrary = totalVideos,
            uniqueFoldersCount = foldersCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HubUserStatsSummary(),
    )

    init {
        // App update checking temporarily disabled as requested
    }

    fun checkForUpdates(currentVersionCode: Int = 0, currentVersionName: String = "", manual: Boolean = false) {
        viewModelScope.launch {
            if (manual) {
                appUpdateManager.clearUpdatePromptSnooze()
            }
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

    fun cancelDownload() {
        appUpdateManager.cancelDownload()
    }

    fun skipVersion(tag: String) {
        appUpdateManager.skipVersion(tag)
    }

    fun snoozeUpdatePrompt() {
        appUpdateManager.snoozeUpdatePrompt()
    }

    fun resetUpdateState() {
        appUpdateManager.resetState()
    }
}
