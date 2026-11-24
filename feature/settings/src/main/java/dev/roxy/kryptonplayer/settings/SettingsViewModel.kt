package dev.roxy.kryptonplayer.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.data.repository.DeviceAuthRepository
import dev.roxy.kryptonplayer.core.data.repository.MediaRepository
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.data.update.ApkAsset
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.AppUpdateManager
import dev.roxy.kryptonplayer.core.data.update.GitHubCommitInfo
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import dev.roxy.kryptonplayer.core.model.Video
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderStatItem(
    val name: String,
    val videoCount: Int,
    val totalBytes: Long,
    val shareFraction: Float,
)

data class UserHubUiState(
    val displayName: String = "Krypton User",
    val handleOrSubtitle: String = "Local Cinema Profile",
    val photoUrl: String? = null,
    val providerLabel: String = "Local",
    val isTelegramConnected: Boolean = false,
    val totalWatchTimeMs: Long = 0L,
    val totalLibraryDurationMs: Long = 0L,
    val totalVideos: Int = 0,
    val watchedVideos: Int = 0,
    val completedVideos: Int = 0,
    val inProgressVideos: Int = 0,
    val totalFolders: Int = 0,
    val totalStorageBytes: Long = 0L,
    val count4K: Int = 0,
    val countFullHd: Int = 0,
    val countHd: Int = 0,
    val countSd: Int = 0,
    val recentlyPlayed: List<Video> = emptyList(),
    val topFolders: List<FolderStatItem> = emptyList(),
) {
    val watchedFraction: Float
        get() = if (totalVideos > 0) (watchedVideos.toFloat() / totalVideos.toFloat()).coerceIn(0f, 1f) else 0f
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
    mediaRepository: MediaRepository,
    preferencesRepository: PreferencesRepository,
    private val deviceAuthRepository: DeviceAuthRepository,
) : ViewModel() {
    val updateState: StateFlow<UpdateState> = appUpdateManager.updateState
    val gitHubCommits: StateFlow<List<GitHubCommitInfo>> = appUpdateManager.gitHubCommits

    val userHubState: StateFlow<UserHubUiState> = combine(
        mediaRepository.getVideosFlow(),
        mediaRepository.getFoldersFlow(),
        preferencesRepository.playerPreferences,
    ) { videos, folders, playerPrefs ->
        val tgConnected = playerPrefs.telegramToken.isNotBlank()
        val authName = deviceAuthRepository.getUserName()?.takeIf { it.isNotBlank() }
        val authEmail = deviceAuthRepository.getUserEmail()?.takeIf { it.isNotBlank() }
        val authPhoto = deviceAuthRepository.getUserPhoto()?.takeIf { it.isNotBlank() }
        val authProvider = deviceAuthRepository.getAuthProvider()?.takeIf { it.isNotBlank() }

        val displayName = playerPrefs.telegramFirstName.takeIf { it.isNotBlank() }
            ?: authName
            ?: "Krypton User"

        val handleOrSubtitle = when {
            playerPrefs.telegramUsername.isNotBlank() -> "@${playerPrefs.telegramUsername}"
            !authEmail.isNullOrBlank() -> authEmail
            tgConnected -> "Telegram Cloud Connected"
            else -> "Local AMOLED Cinema"
        }

        val photoUrl = playerPrefs.telegramProfilePhoto.takeIf { it.isNotBlank() } ?: authPhoto

        val providerLabel = when {
            tgConnected -> "Telegram Cloud"
            authProvider.equals("google", ignoreCase = true) -> "Google"
            authProvider.equals("guest", ignoreCase = true) -> "Guest"
            else -> "Local"
        }

        val totalWatchMs = videos.sumOf { video ->
            val pos = video.playbackPosition.coerceAtLeast(0L)
            if (video.duration > 0L) pos.coerceAtMost(video.duration) else pos
        }
        val totalDurationMs = videos.sumOf { it.duration.coerceAtLeast(0L) }
        val totalBytes = videos.sumOf { it.size.coerceAtLeast(0L) }

        var watched = 0
        var completed = 0
        var inProgress = 0
        var count4K = 0
        var countFullHd = 0
        var countHd = 0
        var countSd = 0

        videos.forEach { video ->
            val hasWatched = video.lastPlayedAt != null || video.playbackPosition > 1_000L
            if (hasWatched) {
                watched++
                val isDone = video.duration > 0L && video.playedPercentage >= 0.90f
                if (isDone) completed++ else inProgress++
            }

            val maxDim = maxOf(video.width, video.height)
            val minDim = minOf(video.width, video.height)
            when {
                minDim >= 2160 || maxDim >= 3840 -> count4K++
                minDim >= 1080 || maxDim >= 1920 -> countFullHd++
                minDim >= 720 || maxDim >= 1280 -> countHd++
                else -> countSd++
            }
        }

        val recent = videos
            .filter { it.lastPlayedAt != null }
            .sortedByDescending { it.lastPlayedAt?.time ?: 0L }
            .take(4)

        val sortedFolders = folders
            .filter { it.allMediaList.isNotEmpty() }
            .sortedByDescending { it.allMediaList.size }
            .take(4)
        val maxFolderCount = sortedFolders.firstOrNull()?.allMediaList?.size?.coerceAtLeast(1) ?: 1
        val topFolders = sortedFolders.map { folder ->
            val count = folder.allMediaList.size
            FolderStatItem(
                name = folder.name.ifBlank { "Internal Storage" },
                videoCount = count,
                totalBytes = folder.mediaSize,
                shareFraction = (count.toFloat() / maxFolderCount.toFloat()).coerceIn(0.08f, 1f),
            )
        }

        UserHubUiState(
            displayName = displayName,
            handleOrSubtitle = handleOrSubtitle,
            photoUrl = photoUrl,
            providerLabel = providerLabel,
            isTelegramConnected = tgConnected,
            totalWatchTimeMs = totalWatchMs,
            totalLibraryDurationMs = totalDurationMs,
            totalVideos = videos.size,
            watchedVideos = watched,
            completedVideos = completed,
            inProgressVideos = inProgress,
            totalFolders = folders.size,
            totalStorageBytes = totalBytes,
            count4K = count4K,
            countFullHd = countFullHd,
            countHd = countHd,
            countSd = countSd,
            recentlyPlayed = recent,
            topFolders = topFolders,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = UserHubUiState(),
    )

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
