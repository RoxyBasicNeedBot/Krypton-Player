package dev.roxy.kryptonplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import java.net.URLDecoder

@Composable
fun rememberMetadataState(player: Player): MetadataState {
    val metadataState = remember { MetadataState(player) }
    LaunchedEffect(player) { metadataState.observe() }
    return metadataState
}

@Stable
class MetadataState(private val player: Player) {
    var title: String? by mutableStateOf(null)
        private set
    var artist: String? by mutableStateOf(null)
        private set
    var artworkUri: android.net.Uri? by mutableStateOf(null)
        private set

    suspend fun observe() {
        update()
        player.listen { events ->
            if (events.containsAny(
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED
                )
            ) {
                update()
            }
        }
    }

    private fun update() {
        val meta = player.mediaMetadata
        val item = player.currentMediaItem
        val uri = item?.localConfiguration?.uri

        val isGenericStreamName = { str: String? ->
            if (str.isNullOrBlank()) true
            else {
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
            ?: item?.mediaMetadata?.title?.toString()?.takeIf { it.isNotBlank() && !isGenericStreamName(it) }
            ?: meta.title?.toString()?.takeIf { it.isNotBlank() && !isGenericStreamName(it) }
            ?: item?.mediaMetadata?.title?.toString()?.takeIf { it.isNotBlank() }
            ?: meta.title?.toString()?.takeIf { it.isNotBlank() }
            ?: uri?.lastPathSegment
            ?: item?.mediaId

        title = rawTitle?.let {
            try {
                URLDecoder.decode(it, "UTF-8")
            } catch (_: Exception) { it }
        }
        artist = item?.mediaMetadata?.artist?.toString()?.takeIf { it.isNotBlank() }
            ?: meta.artist?.toString()?.takeIf { it.isNotBlank() }
        artworkUri = meta.artworkUri ?: item?.mediaMetadata?.artworkUri
    }
}
