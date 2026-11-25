package dev.roxy.kryptonplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackMediaItem

@Composable
fun rememberPlaylistState(player: PlaybackController): PlaylistState {
    val playlistState = remember(player) { PlaylistState(player) }
    LaunchedEffect(player) { playlistState.observe() }
    return playlistState
}

class PlaylistState(
    private val player: PlaybackController,
) {
    var playlist: List<PlaybackMediaItem> by mutableStateOf(emptyList())
        private set

    var currentMediaItemIndex: Int by mutableIntStateOf(0)
        private set

    suspend fun observe() {
        player.state.collect { snapshot ->
            playlist = snapshot.playlist
            currentMediaItemIndex = snapshot.currentIndex
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in playlist.indices || toIndex !in playlist.indices) return
        player.moveMediaItem(fromIndex, toIndex)
    }

    fun removeItem(index: Int) {
        if (index !in playlist.indices) return
        if (playlist.size <= 1) return
        player.removeMediaItem(index)
    }

    fun seekToItem(index: Int) {
        if (index !in playlist.indices) return
        if (currentMediaItemIndex == index) return
        player.seekToItem(index, 0L)
    }
}
