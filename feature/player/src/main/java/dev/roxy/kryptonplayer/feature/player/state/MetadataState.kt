package dev.roxy.kryptonplayer.feature.player.state

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackStateSnapshot
import java.net.URLDecoder

@Composable
fun rememberMetadataState(player: PlaybackController): MetadataState {
    val metadataState = remember(player) { MetadataState(player) }
    LaunchedEffect(player) { metadataState.observe() }
    return metadataState
}

@Stable
class MetadataState(private val player: PlaybackController) {
    var title: String? by mutableStateOf(null)
        private set
    var artist: String? by mutableStateOf(null)
        private set
    var artworkUri: Uri? by mutableStateOf(null)
        private set

    suspend fun observe() {
        player.state.collect { snapshot ->
            update(snapshot)
        }
    }

    private fun update(snapshot: PlaybackStateSnapshot) {
        val item = snapshot.currentItem
        val uri = item?.uri

        val isGenericStreamName = { str: String? ->
            if (str.isNullOrBlank()) {
                true
            } else {
                val s = str.trim().lowercase()
                s == "stream" || s == "stream.m3u8" || s == "master.m3u8" ||
                    s == "playlist.m3u8" || s == "index.m3u8" || s == "manifest" ||
                    s == "video.mp4" || s == "video" || s == "media" || s == "hls" ||
                    s.startsWith("stream.") || s.startsWith("chunklist")
            }
        }

        val queryTitle = uri?.getQueryParameter("title")
            ?: uri?.getQueryParameter("caption")
            ?: uri?.getQueryParameter("name")

        val rawTitle = queryTitle?.takeIf { it.isNotBlank() }
            ?: item?.title?.takeIf { it.isNotBlank() && !isGenericStreamName(it) }
            ?: item?.title?.takeIf { it.isNotBlank() }
            ?: uri?.lastPathSegment
            ?: item?.mediaId

        title = rawTitle?.let {
            try {
                URLDecoder.decode(it, "UTF-8")
            } catch (_: Exception) {
                it
            }
        }
        artist = item?.artist?.takeIf { it.isNotBlank() }
        artworkUri = item?.artworkUri
    }
}
