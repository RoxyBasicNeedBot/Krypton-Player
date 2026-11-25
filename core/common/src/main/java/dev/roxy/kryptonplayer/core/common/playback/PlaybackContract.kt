package dev.roxy.kryptonplayer.core.common.playback

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackLoopMode {
    OFF,
    ONE,
    ALL,
}

data class PlaybackTrack(
    val id: Int,
    val index: Int,
    val title: String,
    val language: String? = null,
    val codec: String? = null,
    val mimeType: String? = null,
    val isSelected: Boolean = false,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val isExternal: Boolean = false,
    val channels: Int = 0,
    val sampleRate: Int = 0,
    val bitrate: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Float = 0f,
)

data class PlaybackMediaItem(
    val mediaId: String,
    val uri: Uri,
    val title: String = "",
    val artist: String? = null,
    val artworkUri: Uri? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val videoScale: Float? = null,
    val playbackSpeed: Float? = null,
    val audioTrackIndex: Int? = null,
    val subtitleTrackIndex: Int? = null,
    val subtitleDelayMs: Long? = null,
    val subtitleSpeed: Float? = null,
    val externalSubtitles: List<Uri> = emptyList(),
)

data class PlaybackFailure(
    val message: String,
    val cause: String? = null,
    val errorCodeName: String = "MPV_PLAYBACK_ERROR",
    val httpStatusCode: Int? = null,
    val uri: String? = null,
)

data class PlaybackStateSnapshot(
    val isInitialized: Boolean = false,
    val isIdle: Boolean = true,
    val isLoading: Boolean = false,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playlist: List<PlaybackMediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: PlaybackLoopMode = PlaybackLoopMode.OFF,
    val shuffleEnabled: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoAspectRatio: Float = 0f,
    val containerFps: Float = 0f,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val audioChannels: Int = 0,
    val audioSampleRate: Int = 0,
    val videoBitrate: Int = 0,
    val audioBitrate: Int = 0,
    val hwdecCurrent: String? = null,
    val colorTransfer: String? = null,
    val videoTracks: List<PlaybackTrack> = emptyList(),
    val audioTracks: List<PlaybackTrack> = emptyList(),
    val subtitleTracks: List<PlaybackTrack> = emptyList(),
    val subtitleDelayMs: Long = 0L,
    val subtitleSpeed: Float = 1.0f,
    val skipSilenceEnabled: Boolean = false,
    val isLoudnessGainSupported: Boolean = true,
    val volumeBoostGainMb: Int = 0,
    val anime4kMode: String = "OFF",
    val hdrMode: String = "OFF",
    val error: PlaybackFailure? = null,
) {
    val currentItem: PlaybackMediaItem?
        get() = playlist.getOrNull(currentIndex)

    val hasNext: Boolean
        get() = playlist.isNotEmpty() && (currentIndex in 0 until playlist.lastIndex || repeatMode == PlaybackLoopMode.ALL)

    val hasPrevious: Boolean
        get() = playlist.isNotEmpty() && (currentIndex > 0 || repeatMode == PlaybackLoopMode.ALL)
}

sealed interface PlaybackEvent {
    data class IsPlayingChanged(val isPlaying: Boolean) : PlaybackEvent
    data class SeekCompleted(val positionMs: Long, val userInitiated: Boolean) : PlaybackEvent
    data class MediaItemTransition(val item: PlaybackMediaItem?, val index: Int) : PlaybackEvent
    data object TracksChanged : PlaybackEvent
    data object FirstFrameRendered : PlaybackEvent
    data class VideoSizeChanged(val width: Int, val height: Int) : PlaybackEvent
    data class PlaybackParametersChanged(val speed: Float) : PlaybackEvent
    data class Error(val failure: PlaybackFailure) : PlaybackEvent
    data object PlaybackEnded : PlaybackEvent
}

interface PlaybackController {
    val state: StateFlow<PlaybackStateSnapshot>
    val events: SharedFlow<PlaybackEvent>

    fun setPlaylist(
        items: List<PlaybackMediaItem>,
        startIndex: Int = 0,
        startPositionMs: Long = -1L,
        playWhenReady: Boolean = true,
    )

    fun addMediaItem(index: Int, item: PlaybackMediaItem)

    fun moveMediaItem(fromIndex: Int, toIndex: Int)

    fun removeMediaItem(index: Int)

    fun play()

    fun pause()

    fun togglePlayPause()

    fun stop(clearPlaylist: Boolean = true)

    fun seekTo(positionMs: Long, isUserSeek: Boolean = true)

    fun seekToItem(index: Int, positionMs: Long = 0L)

    fun seekToNext()

    fun seekToPrevious()

    fun setPlaybackSpeed(speed: Float)

    fun setRepeatMode(mode: PlaybackLoopMode)

    fun setShuffleEnabled(enabled: Boolean)

    fun selectAudioTrack(index: Int)

    fun selectSubtitleTrack(index: Int)

    fun addExternalSubtitle(uri: Uri, select: Boolean = true)

    fun setSubtitleDelayMs(delayMs: Long)

    fun setSubtitleSpeed(speed: Float)

    fun setSkipSilenceEnabled(enabled: Boolean)

    fun setLoudnessGainMb(gainMb: Int)

    fun setScrubbingMode(enabled: Boolean)

    fun updateCurrentItemMetadata(transform: (PlaybackMediaItem) -> PlaybackMediaItem)

    fun setAnime4KMode(mode: String)

    fun setHdrMode(mode: String)
}

interface FrameThumbnailExtractor {
    suspend fun extractFrame(
        uri: Uri,
        positionMs: Long,
        width: Int = 640,
        height: Int = 360,
    ): Bitmap?

    fun release()
}
