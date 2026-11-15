package dev.anilbeesetti.nextplayer.feature.videopicker.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.telegram.TelegramRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.telegram.ImageGalleryScreen
import kotlinx.serialization.Serializable

import kotlinx.serialization.json.Json
import androidx.compose.runtime.remember
@Serializable
object TelegramCloudRoute

@Serializable
data class ImageGalleryRoute(
    val itemsJson: String,
    val initialIndex: Int
)

@Serializable
data class ImageGalleryItem(
    val url: String,
    val name: String,
    val externalId: String?,
    val size: Long
)

fun NavController.navigateToTelegramCloud(navOptions: NavOptions? = null) {
    if (navOptions != null) {
        this.navigate(TelegramCloudRoute, navOptions)
    } else {
        this.navigate(TelegramCloudRoute) {
            launchSingleTop = true
            restoreState = true
        }
    }
}

fun NavController.navigateToImageGallery(itemsJson: String, initialIndex: Int, navOptions: NavOptions? = null) {
    this.navigate(ImageGalleryRoute(itemsJson, initialIndex), navOptions)
}

fun NavGraphBuilder.telegramCloudScreen(
    onNavigateUp: () -> Unit,
    onPlayVideo: (uri: Uri, title: String, roomId: String?, token: String?) -> Unit,
    onImageClick: (images: List<dev.anilbeesetti.nextplayer.core.model.Video>, initialIndex: Int) -> Unit,
    onReelsClick: (videos: List<dev.anilbeesetti.nextplayer.core.model.Video>, startIndex: Int, playlistKey: String) -> Unit
) {
    composable<TelegramCloudRoute> {
        TelegramRoute(
            onNavigateUp = onNavigateUp,
            onPlayVideo = onPlayVideo,
            onImageClick = onImageClick,
            onReelsClick = onReelsClick
        )
    }
}


fun NavGraphBuilder.imageGalleryScreen(
    onNavigateUp: () -> Unit,
    onDownload: (ImageGalleryItem) -> Unit
) {
    composable<ImageGalleryRoute> { backStackEntry ->
        val route: ImageGalleryRoute = backStackEntry.toRoute()
        val items = remember(route.itemsJson) {
            try {
                val decodedJson = String(android.util.Base64.decode(route.itemsJson, android.util.Base64.URL_SAFE))
                Json.decodeFromString<List<ImageGalleryItem>>(decodedJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
        ImageGalleryScreen(
            items = items,
            initialIndex = route.initialIndex,
            onNavigateUp = onNavigateUp,
            onDownload = onDownload
        )
    }
}
