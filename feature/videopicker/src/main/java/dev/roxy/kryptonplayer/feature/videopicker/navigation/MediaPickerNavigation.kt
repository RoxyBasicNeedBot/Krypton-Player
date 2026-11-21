package dev.roxy.kryptonplayer.feature.videopicker.navigation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker.MediaPickerRoute
import kotlinx.serialization.Serializable

internal const val folderIdArg = "folderId"

internal class FolderArgs(val folderId: String?) {
    constructor(savedStateHandle: SavedStateHandle) :
        this(savedStateHandle.get<String>(folderIdArg)?.let { Uri.decode(it) })
}

@Serializable
data class MediaPickerRoute(
    val folderId: String? = null,
)

fun NavController.navigateToMediaPickerScreen(
    folderId: String,
    navOptions: NavOptions? = null,
) {
    val encodedFolderId = Uri.encode(folderId)
    this.navigate(MediaPickerRoute(encodedFolderId), navOptions)
}

fun NavGraphBuilder.mediaPickerScreen(
    onNavigateUp: () -> Unit,
    onPlayVideo: (uri: Uri) -> Unit,
    onPlayVideos: (uris: List<Uri>) -> Unit,
    onFolderClick: (folderPath: String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onReelsClick: (videos: List<dev.roxy.kryptonplayer.core.model.Video>, startIndex: Int, playlistKey: String) -> Unit,
) {
    composable<MediaPickerRoute> {
        MediaPickerRoute(
            onPlayVideo = onPlayVideo,
            onPlayVideos = onPlayVideos,
            onNavigateUp = onNavigateUp,
            onFolderClick = onFolderClick,
            onSettingsClick = onSettingsClick,
            onSearchClick = onSearchClick,
            onTelegramClick = onTelegramClick,
            onReelsClick = onReelsClick,
        )
    }
}
