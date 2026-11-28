package dev.roxy.kryptonplayer.core.data.repository.fake

import android.net.Uri
import dev.roxy.kryptonplayer.core.data.models.VideoState
import dev.roxy.kryptonplayer.core.data.repository.MediaRepository
import dev.roxy.kryptonplayer.core.model.Folder
import dev.roxy.kryptonplayer.core.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeMediaRepository : MediaRepository {

    val videos = mutableListOf<Video>()
    val directories = mutableListOf<Folder>()

    override fun getVideosFlow(): Flow<List<Video>> {
        return flowOf(videos)
    }

    override fun getVideosFlowFromFolderPath(folderPath: String): Flow<List<Video>> {
        return flowOf(videos)
    }

    override fun getFoldersFlow(): Flow<List<Folder>> {
        return flowOf(directories)
    }

    override fun getPlaybackProgressFlow(): Flow<Map<String, Pair<Long, Long>>> {
        return flowOf(emptyMap())
    }

    override suspend fun getVideoByUri(uri: String): Video? {
        return videos.find { it.path == uri }
    }

    override suspend fun getVideoState(uri: String): VideoState? {
        return null
    }

    override suspend fun updateMediumLastPlayedTime(uri: String, lastPlayedTime: Long) {
    }

    override suspend fun updateMediumPosition(uri: String, position: Long) {
    }

    override suspend fun addWatchDuration(uri: String, deltaMs: Long) {
    }

    override suspend fun updateMediumPlaybackSpeed(uri: String, playbackSpeed: Float) {
    }

    override suspend fun updateMediumAudioTrack(uri: String, audioTrackIndex: Int) {
    }

    override suspend fun updateMediumSubtitleTrack(uri: String, subtitleTrackIndex: Int) {
    }

    override suspend fun updateMediumZoom(uri: String, zoom: Float) {
    }

    override suspend fun addExternalSubtitleToMedium(uri: String, subtitleUri: Uri) {
    }

    override suspend fun updateSubtitleDelay(uri: String, delay: Long) {
    }

    override suspend fun updateSubtitleSpeed(uri: String, speed: Float) {
    }
}
