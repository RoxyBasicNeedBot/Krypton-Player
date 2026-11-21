package dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.common.extensions.prettyName
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.data.update.ApkAsset
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.AppUpdateManager
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import dev.roxy.kryptonplayer.core.domain.GetRecentVideosUseCase
import dev.roxy.kryptonplayer.core.domain.GetSortedMediaUseCase
import dev.roxy.kryptonplayer.core.media.services.MediaService
import dev.roxy.kryptonplayer.core.media.sync.MediaInfoSynchronizer
import dev.roxy.kryptonplayer.core.media.sync.MediaSynchronizer
import dev.roxy.kryptonplayer.core.model.ApplicationPreferences
import dev.roxy.kryptonplayer.core.model.Folder
import dev.roxy.kryptonplayer.core.model.Video
import dev.roxy.kryptonplayer.core.ui.base.DataState
import dev.roxy.kryptonplayer.feature.videopicker.navigation.FolderArgs
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    getSortedMediaUseCase: GetSortedMediaUseCase,
    private val getRecentVideosUseCase: GetRecentVideosUseCase,
    savedStateHandle: SavedStateHandle,
    private val mediaService: MediaService,
    private val preferencesRepository: PreferencesRepository,
    private val mediaInfoSynchronizer: MediaInfoSynchronizer,
    private val mediaSynchronizer: MediaSynchronizer,
    val appUpdateManager: AppUpdateManager,
) : ViewModel() {

    private val folderArgs = FolderArgs(savedStateHandle)

    val folderPath = folderArgs.folderId

    val updateState: StateFlow<UpdateState> = appUpdateManager.updateState

    private val _isUpdateSnoozed = MutableStateFlow(appUpdateManager.isUpdatePromptSnoozed())
    val isUpdateSnoozed: StateFlow<Boolean> = _isUpdateSnoozed.asStateFlow()

    private val uiStateInternal = MutableStateFlow(
        MediaPickerUiState(
            folderName = folderPath?.let { File(folderPath).prettyName },
            preferences = preferencesRepository.applicationPreferences.value,
        ),
    )
    val uiState = uiStateInternal.asStateFlow()

    init {
        mediaSynchronizer.startSync()

        viewModelScope.launch {
            getSortedMediaUseCase.invoke(folderPath).collect {
                uiStateInternal.update { currentState ->
                    currentState.copy(
                        mediaDataState = DataState.Success(it),
                    )
                }
            }
        }

        if (folderPath == null) {
            viewModelScope.launch {
                getRecentVideosUseCase(limit = 20).collect { videos ->
                    uiStateInternal.update { currentState ->
                        currentState.copy(
                            recentVideos = videos,
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect {
                uiStateInternal.update { currentState ->
                    currentState.copy(
                        preferences = it,
                    )
                }
            }
        }
    }

    fun onEvent(event: MediaPickerUiEvent) {
        when (event) {
            is MediaPickerUiEvent.DeleteFolders -> deleteFolders(event.folders)
            is MediaPickerUiEvent.DeleteVideos -> deleteVideos(event.videos)
            is MediaPickerUiEvent.ShareVideos -> shareVideos(event.videos)
            is MediaPickerUiEvent.Refresh -> refresh()
            is MediaPickerUiEvent.RenameVideo -> renameVideo(event.uri, event.to)
            is MediaPickerUiEvent.AddToSync -> addToMediaInfoSynchronizer(event.uri)
            is MediaPickerUiEvent.UpdateMenu -> updateMenu(event.preferences)
            is MediaPickerUiEvent.SnoozeUpdatePrompt -> snoozeUpdatePrompt()
        }
    }

    fun snoozeUpdatePrompt() {
        appUpdateManager.snoozeUpdatePrompt()
        _isUpdateSnoozed.value = true
    }

    fun downloadAndInstall(info: AppUpdateInfo, asset: ApkAsset? = null) {
        appUpdateManager.downloadAndInstall(info, asset)
    }

    fun installApk(uri: Uri? = null) {
        appUpdateManager.installApk(uri)
    }

    fun skipVersion(tag: String) {
        appUpdateManager.skipVersion(tag)
        _isUpdateSnoozed.value = true
    }

    fun cancelDownload() {
        appUpdateManager.cancelDownload()
    }

    private fun deleteFolders(folders: List<Folder>) {
        viewModelScope.launch {
            val uris = folders.flatMap { folder ->
                folder.allMediaList.map { video ->
                    video.uriString.toUri()
                }
            }
            mediaService.deleteMedia(uris)
        }
    }

    private fun deleteVideos(uris: List<String>) {
        viewModelScope.launch {
            mediaService.deleteMedia(uris.map { it.toUri() })
        }
    }

    private fun shareVideos(uris: List<String>) {
        viewModelScope.launch {
            mediaService.shareMedia(uris.map { it.toUri() })
        }
    }

    private fun addToMediaInfoSynchronizer(uri: Uri) {
        viewModelScope.launch {
            mediaInfoSynchronizer.sync(uri)
        }
    }

    private fun renameVideo(uri: Uri, to: String) {
        viewModelScope.launch {
            mediaService.renameMedia(uri, to)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            uiStateInternal.update { it.copy(refreshing = true) }
            mediaSynchronizer.startSync()
            mediaSynchronizer.refresh()
            uiStateInternal.update { it.copy(refreshing = false) }
        }
    }

    private fun updateMenu(preferences: ApplicationPreferences) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { preferences }
        }
    }
}

@Stable
data class MediaPickerUiState(
    val folderName: String?,
    val mediaDataState: DataState<Folder?> = DataState.Loading,
    val refreshing: Boolean = false,
    val preferences: ApplicationPreferences = ApplicationPreferences(),
    val recentVideos: List<Video> = emptyList(),
)

sealed interface MediaPickerUiEvent {
    data class DeleteVideos(val videos: List<String>) : MediaPickerUiEvent
    data class DeleteFolders(val folders: List<Folder>) : MediaPickerUiEvent
    data class ShareVideos(val videos: List<String>) : MediaPickerUiEvent
    data object Refresh : MediaPickerUiEvent
    data class RenameVideo(val uri: Uri, val to: String) : MediaPickerUiEvent
    data class AddToSync(val uri: Uri) : MediaPickerUiEvent
    data class UpdateMenu(val preferences: ApplicationPreferences) : MediaPickerUiEvent
    data object SnoozeUpdatePrompt : MediaPickerUiEvent
}
