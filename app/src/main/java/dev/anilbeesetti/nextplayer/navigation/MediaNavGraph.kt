package dev.anilbeesetti.nextplayer.navigation

import android.content.Context
import android.content.Intent
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import dev.anilbeesetti.nextplayer.feature.player.PlayerActivity
import dev.anilbeesetti.nextplayer.feature.player.ReelsActivity
import dev.anilbeesetti.nextplayer.feature.player.model.ReelItem
import dev.anilbeesetti.nextplayer.feature.player.utils.PlayerApi
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.MediaPickerRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.mediaPickerScreen
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.navigateToMediaPickerScreen
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.navigateToSearch
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.searchScreen
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.telegramCloudScreen
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.navigateToTelegramCloud
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.ImageGalleryItem
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.imageGalleryScreen
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.navigateToImageGallery
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data object MediaRootRoute

fun NavGraphBuilder.mediaNavGraph(
    context: Context,
    navController: NavHostController,
) {
    navigation<MediaRootRoute>(
        startDestination = MediaPickerRoute()
    ) {
        mediaPickerScreen(
            onNavigateUp = navController::navigateUp,
            onPlayVideo = { uri ->
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = uri
                    val queryTitle = uri.getQueryParameter("title")
                    if (!queryTitle.isNullOrBlank()) {
                        putExtra("title", queryTitle)
                    }
                }
                context.startActivity(intent)
            },
            onPlayVideos = { uris ->
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = uris.first()
                    putParcelableArrayListExtra(PlayerApi.API_PLAYLIST, ArrayList(uris))
                }
                context.startActivity(intent)
            },
            onFolderClick = navController::navigateToMediaPickerScreen,
            onSettingsClick = navController::navigateToSettings,
            onSearchClick = navController::navigateToSearch,
            onTelegramClick = navController::navigateToTelegramCloud,
            onReelsClick = { videos, startIndex, playlistKey ->
                val reelItems = videos.map { video ->
                    ReelItem(
                        uri = video.uriString,
                        title = video.nameWithExtension,
                        duration = video.duration,
                        thumbnailUri = video.artworkUri,
                        source = "local",
                        isImage = video.isImage
                    )
                }
                ReelsActivity.playlist = reelItems
                ReelsActivity.playlistKey = "local_$playlistKey"
                val intent = Intent(context, ReelsActivity::class.java).apply {
                    putExtra("reels_start_index", startIndex)
                    putExtra("reels_source", "local")
                }
                context.startActivity(intent)
            }
        )

        telegramCloudScreen(
            onNavigateUp = navController::navigateUp,
            onPlayVideo = { uri, title, roomId, token ->
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = uri
                    putExtra("title", title)
                    putExtra("is_telegram_cloud", true)
                    if (roomId != null && token != null) {
                        putExtra("watch_party_room_id", roomId)
                        putExtra("watch_party_token", token)
                    }
                }
                context.startActivity(intent)
            },
            onImageClick = { images, index ->

                val items = images.map { video ->
                    ImageGalleryItem(
                        url = video.uriString,
                        name = video.nameWithExtension,
                        externalId = video.externalId,
                        size = video.size
                    )
                }
                val itemsJson = Json.encodeToString(items)
                val base64Json = android.util.Base64.encodeToString(itemsJson.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                navController.navigateToImageGallery(base64Json, index)
            },
            onReelsClick = { videos, startIndex, playlistKey ->
                val reelItems = videos.map { video ->
                    val resolvedUri = if (video.uriString.isNotBlank()) video.uriString else video.path
                    val resolvedThumb = if (!video.artworkUri.isNullOrBlank()) video.artworkUri else video.path
                    val dbId = video.externalId ?: video.path.substringAfterLast("/").substringBefore("?")
                    ReelItem(
                        uri = resolvedUri,
                        title = video.nameWithExtension,
                        duration = video.duration,
                        thumbnailUri = resolvedThumb,
                        source = "cloud",
                        isImage = video.isImage,
                        dbId = dbId
                    )
                }
                ReelsActivity.playlist = reelItems
                ReelsActivity.playlistKey = "cloud_$playlistKey"
                val intent = Intent(context, ReelsActivity::class.java).apply {
                    putExtra("reels_start_index", startIndex)
                    putExtra("reels_source", "cloud")
                }
                context.startActivity(intent)
            }
        )

        imageGalleryScreen(
            onNavigateUp = navController::navigateUp,
            onDownload = { item ->
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                val request = android.app.DownloadManager.Request(android.net.Uri.parse(item.url))
                    .setTitle(item.name)
                    .setDescription("Downloading image from Telegram Cloud")
                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_PICTURES, "KryptonPlayer/${item.name}")
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                try {
                    downloadManager.enqueue(request)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )

        searchScreen(
            onNavigateUp = navController::navigateUp,
            onPlayVideo = { uri ->
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = uri
                }
                context.startActivity(intent)
            },
            onFolderClick = navController::navigateToMediaPickerScreen,
        )

    }
}
