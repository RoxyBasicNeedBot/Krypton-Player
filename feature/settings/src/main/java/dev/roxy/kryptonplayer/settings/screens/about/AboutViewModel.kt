package dev.roxy.kryptonplayer.settings.screens.about

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.data.update.ApkAsset
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.AppUpdateManager
import dev.roxy.kryptonplayer.core.data.update.GitHubCommitInfo
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import dev.roxy.kryptonplayer.settings.contributors.ContributorUiModel
import dev.roxy.kryptonplayer.settings.contributors.GitHubContributorsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class AboutViewModel @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
) : ViewModel() {
    val updateState: StateFlow<UpdateState> = appUpdateManager.updateState
    val changelogReleases: StateFlow<List<AppUpdateInfo>> = appUpdateManager.changelogReleases
    val gitHubCommits: StateFlow<List<GitHubCommitInfo>> = appUpdateManager.gitHubCommits

    private val _isLoadingChangelog = MutableStateFlow(false)
    val isLoadingChangelog: StateFlow<Boolean> = _isLoadingChangelog.asStateFlow()

    private val _isLoadingCommits = MutableStateFlow(false)
    val isLoadingCommits: StateFlow<Boolean> = _isLoadingCommits.asStateFlow()

    private val _contributors = MutableStateFlow(GitHubContributorsRepository.getCachedContributors())
    val contributors: StateFlow<List<ContributorUiModel>> = _contributors.asStateFlow()

    private val _isLoadingContributors = MutableStateFlow(false)
    val isLoadingContributors: StateFlow<Boolean> = _isLoadingContributors.asStateFlow()

    init {
        loadChangelog(forceRefresh = false)
        loadCommits(forceRefresh = false)
        loadContributors(forceRefresh = false)
    }

    fun loadChangelog(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingChangelog.value = true
            try {
                appUpdateManager.getChangelogReleases(forceRefresh)
            } catch (_: Exception) {
            } finally {
                _isLoadingChangelog.value = false
            }
        }
    }

    fun loadCommits(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingCommits.value = true
            try {
                appUpdateManager.getRecentCommits(forceRefresh)
            } catch (_: Exception) {
            } finally {
                _isLoadingCommits.value = false
            }
        }
    }

    fun loadContributors(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingContributors.value = true
            try {
                _contributors.value = GitHubContributorsRepository.fetchContributors(forceRefresh)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (_contributors.value.isEmpty()) {
                    _contributors.value = GitHubContributorsRepository.fallbackContributors()
                }
            } finally {
                _isLoadingContributors.value = false
            }
        }
    }

    suspend fun checkForUpdates(
        currentVersionCode: Int = 0,
        currentVersionName: String = "",
        manual: Boolean = false,
    ): AppUpdateInfo? {
        if (manual) {
            appUpdateManager.clearUpdatePromptSnooze()
        }
        val result = appUpdateManager.checkForUpdates(currentVersionCode, currentVersionName, manual)
        // Ensure commits are loaded
        if (gitHubCommits.value.isEmpty()) {
            loadCommits(forceRefresh = false)
        }
        return result
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

    fun skipVersion(tag: String) {
        appUpdateManager.skipVersion(tag)
    }

    fun snoozeUpdatePrompt() {
        appUpdateManager.snoozeUpdatePrompt()
    }

    fun cancelDownload() {
        appUpdateManager.cancelDownload()
    }

    fun resetState() {
        appUpdateManager.resetState()
    }
}
