package dev.anilbeesetti.nextplayer.feature.player.service

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION
import androidx.media3.common.Player.DISCONTINUITY_REASON_REMOVE
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.CommandButton
import androidx.media3.session.CommandButton.ICON_UNDEFINED
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.anilbeesetti.nextplayer.core.common.extensions.deleteFiles
import dev.anilbeesetti.nextplayer.core.common.extensions.getFilenameFromUri
import dev.anilbeesetti.nextplayer.core.common.extensions.getLocalSubtitles
import dev.anilbeesetti.nextplayer.core.common.extensions.getPath
import dev.anilbeesetti.nextplayer.core.common.extensions.subtitleCacheDir
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.DecoderPriority
import dev.anilbeesetti.nextplayer.core.model.LoopMode
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.model.Resume
import dev.anilbeesetti.nextplayer.core.ui.R as coreUiR
import dev.anilbeesetti.nextplayer.feature.player.PlayerActivity
import dev.anilbeesetti.nextplayer.feature.player.R
import dev.anilbeesetti.nextplayer.feature.player.extensions.addAdditionalSubtitleConfiguration
import dev.anilbeesetti.nextplayer.feature.player.extensions.audioTrackIndex
import dev.anilbeesetti.nextplayer.feature.player.extensions.copy
import dev.anilbeesetti.nextplayer.feature.player.extensions.getManuallySelectedTrackIndex
import dev.anilbeesetti.nextplayer.feature.player.extensions.playbackSpeed
import dev.anilbeesetti.nextplayer.feature.player.extensions.positionMs
import dev.anilbeesetti.nextplayer.feature.player.extensions.setExtras
import dev.anilbeesetti.nextplayer.feature.player.extensions.setIsScrubbingModeEnabled
import dev.anilbeesetti.nextplayer.feature.player.extensions.subtitleDelayMilliseconds
import dev.anilbeesetti.nextplayer.feature.player.extensions.subtitleSpeed
import dev.anilbeesetti.nextplayer.feature.player.extensions.subtitleTrackIndex
import dev.anilbeesetti.nextplayer.feature.player.extensions.switchTrack
import dev.anilbeesetti.nextplayer.feature.player.extensions.uriToSubtitleConfiguration
import dev.anilbeesetti.nextplayer.feature.player.extensions.videoZoom
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleDelayMilliseconds
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleSpeed
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlayerService : MediaSessionService() {

    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaSession: MediaSession? = null
    private var artworkLoadJob: Job? = null

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var mediaRepository: MediaRepository

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var tdlibManager: dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibManager

    @Inject
    lateinit var tdlibMediaResolver: dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibMediaResolver

    private val playerPreferences: PlayerPreferences
        get() = preferencesRepository.playerPreferences.value

    private val customCommands = CustomCommands.asSessionCommands()

    private var isMediaItemReady = false

    private val volumeBoostAudioProcessor = VolumeBoostAudioProcessor()
    private var currentVolumeGain: Int = 0

    private val playbackStateListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) return
            isMediaItemReady = false
            loadArtworkForCurrentMediaItem()
            setEnhancerTargetGain(currentVolumeGain)
            mediaItem?.mediaMetadata?.let { metadata ->
                mediaSession?.player?.run {
                    setPlaybackSpeed(metadata.playbackSpeed ?: playerPreferences.defaultPlaybackSpeed)
                    playerSpecificSubtitleDelayMilliseconds = metadata.subtitleDelayMilliseconds ?: 0L
                    playerSpecificSubtitleSpeed = metadata.subtitleSpeed ?: 1f
                }

                metadata.positionMs?.takeIf { playerPreferences.resume == Resume.YES }?.let {
                    mediaSession?.player?.seekTo(it)
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            val oldMediaItem = oldPosition.mediaItem ?: return

            when (reason) {
                DISCONTINUITY_REASON_SEEK,
                DISCONTINUITY_REASON_AUTO_TRANSITION,
                -> {
                    if (newPosition.mediaItem == null || oldMediaItem == newPosition.mediaItem) return

                    val updatedPosition = oldPosition.positionMs.takeIf { reason == DISCONTINUITY_REASON_SEEK } ?: C.TIME_UNSET
                    mediaSession?.player?.replaceMediaItem(
                        oldPosition.mediaItemIndex,
                        oldMediaItem.copy(positionMs = updatedPosition),
                    )
                    serviceScope.launch {
                        mediaRepository.updateMediumPosition(
                            uri = oldMediaItem.mediaId,
                            position = updatedPosition,
                        )
                    }
                }

                DISCONTINUITY_REASON_REMOVE -> {
                    serviceScope.launch {
                        val durationMs = oldMediaItem.mediaMetadata.durationMs
                        val isAtEnd = durationMs != null && oldPosition.positionMs >= durationMs - 1000
                        mediaRepository.updateMediumPosition(
                            uri = oldMediaItem.mediaId,
                            position = if (isAtEnd) C.TIME_UNSET else oldPosition.positionMs,
                        )
                    }
                }

                else -> return
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            super.onTracksChanged(tracks)
            if (!isMediaItemReady && tracks.groups.isNotEmpty()) {
                isMediaItemReady = true

                if (!playerPreferences.rememberSelections) return
                mediaSession?.player?.mediaMetadata?.audioTrackIndex?.let {
                    mediaSession?.player?.switchTrack(C.TRACK_TYPE_AUDIO, it)
                }
                mediaSession?.player?.mediaMetadata?.subtitleTrackIndex?.let {
                    mediaSession?.player?.switchTrack(C.TRACK_TYPE_TEXT, it)
                }
            }
        }

        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
            super.onTrackSelectionParametersChanged(parameters)
            val player = mediaSession?.player ?: return
            val currentMediaItem = player.currentMediaItem ?: return

            val audioTrackIndex = player.getManuallySelectedTrackIndex(C.TRACK_TYPE_AUDIO)
            val subtitleTrackIndex = player.getManuallySelectedTrackIndex(C.TRACK_TYPE_TEXT)

            if (audioTrackIndex != null) {
                serviceScope.launch {
                    mediaRepository.updateMediumAudioTrack(
                        uri = currentMediaItem.mediaId,
                        audioTrackIndex = audioTrackIndex,
                    )
                }
            }

            if (subtitleTrackIndex != null) {
                serviceScope.launch {
                    mediaRepository.updateMediumSubtitleTrack(
                        uri = currentMediaItem.mediaId,
                        subtitleTrackIndex = subtitleTrackIndex,
                    )
                }
            }

            player.replaceMediaItem(
                player.currentMediaItemIndex,
                currentMediaItem.copy(
                    audioTrackIndex = audioTrackIndex,
                    subtitleTrackIndex = subtitleTrackIndex,
                ),
            )
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            super.onPlaybackParametersChanged(playbackParameters)
            val player = mediaSession?.player ?: return
            val currentMediaItem = player.currentMediaItem ?: return
            val playbackSpeed = playbackParameters.speed

            serviceScope.launch {
                mediaRepository.updateMediumPlaybackSpeed(
                    uri = currentMediaItem.mediaId,
                    playbackSpeed = playbackSpeed,
                )
            }
            player.replaceMediaItem(
                player.currentMediaItemIndex,
                currentMediaItem.copy(playbackSpeed = playbackSpeed),
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                mediaSession?.player?.trackSelectionParameters = TrackSelectionParameters.DEFAULT
                mediaSession?.player?.setPlaybackSpeed(playerPreferences.defaultPlaybackSpeed)
            }

            if (playbackState == Player.STATE_READY) {
                mediaSession?.player?.let {
                    serviceScope.launch {
                        mediaRepository.updateMediumLastPlayedTime(
                            uri = it.currentMediaItem?.mediaId ?: return@launch,
                            lastPlayedTime = System.currentTimeMillis(),
                        )
                    }
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)

            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                if (mediaSession?.player?.repeatMode != Player.REPEAT_MODE_OFF) {
                    mediaSession?.player?.seekTo(0)
                    mediaSession?.player?.play()
                    return
                }
                mediaSession?.run {
                    player.clearMediaItems()
                    player.stop()
                }
                stopSelf()
            }
        }

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            val player = mediaSession?.player ?: return
            val currentMediaItem = player.currentMediaItem ?: return
            // Update the media metadata duration so that it will be used later in position discontinuity handling
            player.replaceMediaItem(
                player.currentMediaItemIndex,
                currentMediaItem.copy(durationMs = player.duration.coerceAtLeast(0))
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            mediaSession?.run {
                serviceScope.launch {
                    mediaRepository.updateMediumPosition(
                        uri = player.currentMediaItem?.mediaId ?: return@launch,
                        position = player.currentPosition,
                    )
                }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            serviceScope.launch {
                preferencesRepository.updatePlayerPreferences {
                    it.copy(
                        loopMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> LoopMode.OFF
                            Player.REPEAT_MODE_ONE -> LoopMode.ONE
                            Player.REPEAT_MODE_ALL -> LoopMode.ALL
                            else -> LoopMode.OFF
                        },
                    )
                }
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            super.onAudioSessionIdChanged(audioSessionId)
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            android.util.Log.e(
                "PlayerService",
                "Player error occurred: errorCode=${error.errorCodeName} (${error.errorCode}), message=${error.message}",
                error
            )
        }
    }

    private fun setEnhancerTargetGain(gain: Int) {
        currentVolumeGain = gain
        if (gain <= 0 || !playerPreferences.enableVolumeBoost) {
            volumeBoostAudioProcessor.volumeFactor = 1.0f
        } else {
            // Map gain millibels (0..2000 mB) to 1.0x .. 3.5x factor smoothly
            val boostFraction = (gain.toFloat() / 2000f).coerceIn(0f, 1f)
            volumeBoostAudioProcessor.volumeFactor = 1.0f + boostFraction * 2.5f
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            return MediaSession.ConnectionResult.accept(
                connectionResult.availableSessionCommands
                    .buildUpon()
                    .addSessionCommands(customCommands)
                    .build(),
                connectionResult.availablePlayerCommands,
            )
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = serviceScope.future(Dispatchers.Default) {
            val updatedMediaItems = updatedMediaItemsWithMetadata(mediaItems)
            val resolvedStartPositionMs = if (startPositionMs == C.TIME_UNSET && playerPreferences.resume == Resume.YES) {
                updatedMediaItems.getOrNull(startIndex)?.mediaMetadata?.positionMs ?: C.TIME_UNSET
            } else {
                startPositionMs
            }
            return@future MediaSession.MediaItemsWithStartPosition(updatedMediaItems, startIndex, resolvedStartPositionMs)
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = serviceScope.future(Dispatchers.Default) {
            val updatedMediaItems = updatedMediaItemsWithMetadata(mediaItems)
            return@future updatedMediaItems.toMutableList()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> = serviceScope.future {
            val command = CustomCommands.fromSessionCommand(customCommand)
                ?: return@future SessionResult(SessionError.ERROR_BAD_VALUE)

            when (command) {
                CustomCommands.ADD_SUBTITLE_TRACK -> {
                    val subtitleUri = args.getString(CustomCommands.SUBTITLE_TRACK_URI_KEY)?.toUri()
                        ?: return@future SessionResult(SessionError.ERROR_BAD_VALUE)

                    try {
                        val newSubConfiguration = uriToSubtitleConfiguration(
                            uri = subtitleUri,
                            subtitleEncoding = playerPreferences.subtitleTextEncoding,
                            isSelected = true,
                        )
                        mediaSession?.player?.let { player ->
                            val currentMediaItem = player.currentMediaItem ?: return@let
                            val textTracks = player.currentTracks.groups.filter {
                                it.type == C.TRACK_TYPE_TEXT && it.isSupported
                            }

                            try {
                                mediaRepository.updateMediumPosition(
                                    uri = currentMediaItem.mediaId,
                                    position = player.currentPosition,
                                )
                                mediaRepository.updateMediumSubtitleTrack(
                                    uri = currentMediaItem.mediaId,
                                    subtitleTrackIndex = textTracks.size,
                                )
                                mediaRepository.addExternalSubtitleToMedium(
                                    uri = currentMediaItem.mediaId,
                                    subtitleUri = subtitleUri,
                                )
                            } catch (dbEx: Exception) {
                                android.util.Log.w("PlayerService", "Non-local media DB update skipped: ${dbEx.message}")
                            }

                            player.addAdditionalSubtitleConfiguration(newSubConfiguration)
                        }
                        return@future SessionResult(SessionResult.RESULT_SUCCESS)
                    } catch (e: Exception) {
                        android.util.Log.e("PlayerService", "Failed to apply subtitle track", e)
                        return@future SessionResult(SessionError.ERROR_UNKNOWN)
                    }
                }

                CustomCommands.SET_SKIP_SILENCE_ENABLED -> {
                    val enabled = args.getBoolean(CustomCommands.SKIP_SILENCE_ENABLED_KEY)
                    mediaSession?.player?.playerSpecificSkipSilenceEnabled = enabled
                    mediaSession?.sessionExtras = Bundle().apply {
                        putBoolean(CustomCommands.SKIP_SILENCE_ENABLED_KEY, enabled)
                    }
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_SKIP_SILENCE_ENABLED -> {
                    val enabled = mediaSession?.player?.playerSpecificSkipSilenceEnabled ?: false
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putBoolean(CustomCommands.SKIP_SILENCE_ENABLED_KEY, enabled)
                        },
                    )
                }

                CustomCommands.SET_IS_SCRUBBING_MODE_ENABLED -> {
                    val enabled = args.getBoolean(CustomCommands.IS_SCRUBBING_MODE_ENABLED_KEY)
                    mediaSession?.player?.setIsScrubbingModeEnabled(enabled)
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.IS_LOUDNESS_GAIN_SUPPORTED -> {
                    val isSupported = playerPreferences.enableVolumeBoost
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putBoolean(CustomCommands.IS_LOUDNESS_GAIN_SUPPORTED_KEY, isSupported)
                        },
                    )
                }

                CustomCommands.SET_LOUDNESS_GAIN -> {
                    val gain = args.getInt(CustomCommands.LOUDNESS_GAIN_KEY, 0)
                    setEnhancerTargetGain(gain)
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_LOUDNESS_GAIN -> {
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putInt(CustomCommands.LOUDNESS_GAIN_KEY, currentVolumeGain)
                        },
                    )
                }

                CustomCommands.GET_SUBTITLE_DELAY -> {
                    val subtitleDelay = mediaSession?.player?.playerSpecificSubtitleDelayMilliseconds ?: 0
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putLong(CustomCommands.SUBTITLE_DELAY_KEY, subtitleDelay)
                        },
                    )
                }

                CustomCommands.SET_SUBTITLE_DELAY -> {
                    val subtitleDelay = args.getLong(CustomCommands.SUBTITLE_DELAY_KEY)
                    mediaSession?.player?.playerSpecificSubtitleDelayMilliseconds = subtitleDelay
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_SUBTITLE_SPEED -> {
                    val subtitleSpeed = mediaSession?.player?.playerSpecificSubtitleSpeed ?: 0f
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putFloat(CustomCommands.SUBTITLE_SPEED_KEY, subtitleSpeed)
                        },
                    )
                }

                CustomCommands.SET_SUBTITLE_SPEED -> {
                    val subtitleSpeed = args.getFloat(CustomCommands.SUBTITLE_SPEED_KEY)
                    mediaSession?.player?.playerSpecificSubtitleSpeed = subtitleSpeed
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.STOP_PLAYER_SESSION -> {
                    mediaSession?.run {
                        val currentMediaItem = player.currentMediaItem
                        if (currentMediaItem != null) {
                            mediaRepository.updateMediumPosition(
                                uri = currentMediaItem.mediaId,
                                position = player.currentPosition,
                            )
                        }
                    }
                    mediaSession?.run {
                        player.clearMediaItems()
                        player.stop()
                    }
                    stopSelf()
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onCreate() {
        super.onCreate()
        val renderersFactory = object : NextRenderersFactory(applicationContext) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink? {
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(volumeBoostAudioProcessor))
                    .build()
            }
        }
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                when (playerPreferences.decoderPriority) {
                    DecoderPriority.DEVICE_ONLY -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    DecoderPriority.PREFER_DEVICE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                    DecoderPriority.PREFER_APP -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                },
            )

        val trackSelector = DefaultTrackSelector(applicationContext).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage(playerPreferences.preferredAudioLanguage)
                    .setPreferredTextLanguage(playerPreferences.preferredSubtitleLanguage),
            )
        }

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setAllocator(androidx.media3.exoplayer.upstream.DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 250,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setTargetBufferBytes(25 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(/* backBufferDurationMs = */ 10_000, /* retainBackBufferFromKeyframe = */ true)
            .build()

        val defaultUserAgent = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(defaultUserAgent)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to defaultUserAgent,
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Sec-Fetch-Mode" to "no-cors",
                    "Sec-Fetch-Dest" to "video"
                )
            )

        val dataSourceFactory = DataSource.Factory {
            val baseDataSource = DefaultDataSource(applicationContext, httpDataSourceFactory.createDataSource())
            val proxyDataSource = ProxyHeaderDataSource(baseDataSource)
            dev.anilbeesetti.nextplayer.core.data.tdlib.SmartTelegramDataSource(
                context = applicationContext,
                httpDelegate = proxyDataSource,
                tdlibManager = tdlibManager,
                tdlibMediaResolver = tdlibMediaResolver,
                preferencesRepository = preferencesRepository
            )
        }
        
        val extractorsFactory = dev.anilbeesetti.nextplayer.feature.player.extractor.ResilientExtractorsFactory()

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        val player = ExoPlayer.Builder(applicationContext)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                playerPreferences.requireAudioFocus,
            )
            .setHandleAudioBecomingNoisy(playerPreferences.pauseOnHeadsetDisconnect)
            .build()
            .also {
                it.addListener(playbackStateListener)
                it.pauseAtEndOfMediaItems = !playerPreferences.autoplay
                it.repeatMode = when (playerPreferences.loopMode) {
                    LoopMode.OFF -> Player.REPEAT_MODE_OFF
                    LoopMode.ONE -> Player.REPEAT_MODE_ONE
                    LoopMode.ALL -> Player.REPEAT_MODE_ALL
                }
            }

        try {
            mediaSession = MediaSession.Builder(this, player).apply {
                setSessionActivity(
                    PendingIntent.getActivity(
                        this@PlayerService,
                        0,
                        Intent(this@PlayerService, PlayerActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                setCallback(mediaSessionCallback)
                setCustomLayout(
                    listOf(
                        CommandButton.Builder(ICON_UNDEFINED)
                            .setCustomIconResId(coreUiR.drawable.ic_close)
                            .setDisplayName(getString(coreUiR.string.stop_player_session))
                            .setSessionCommand(CustomCommands.STOP_PLAYER_SESSION.sessionCommand)
                            .setEnabled(true)
                            .build(),
                    ),
                )
            }.build()
            setMediaNotificationProvider(
                androidx.media3.session.DefaultMediaNotificationProvider(this).apply {
                    setSmallIcon(coreUiR.drawable.ic_launcher_monochrome)
                }
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player!!
        if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        artworkLoadJob?.cancel()
        mediaSession?.run {
            player.clearMediaItems()
            player.stop()
            player.removeListener(playbackStateListener)
            player.release()
            release()
            mediaSession = null
        }
        subtitleCacheDir.deleteFiles()
        serviceScope.cancel()
    }

    private suspend fun updatedMediaItemsWithMetadata(
        mediaItems: List<MediaItem>,
    ): List<MediaItem> = supervisorScope {
        mediaItems.map { mediaItem ->
            async {
                val uri = mediaItem.mediaId.toUri()
                val video = mediaRepository.getVideoByUri(uri = mediaItem.mediaId)
                val videoState = mediaRepository.getVideoState(uri = mediaItem.mediaId)

                val externalSubs = videoState?.externalSubs ?: emptyList()
                val localSubs = (videoState?.path ?: getPath(uri))?.let {
                    File(it).getLocalSubtitles(
                        context = this@PlayerService,
                        excludeSubsList = externalSubs,
                    )
                } ?: emptyList()

                val existingSubConfigurations = mediaItem.localConfiguration?.subtitleConfigurations ?: emptyList()
                val subConfigurations = (localSubs + externalSubs).map { subtitleUri ->
                    uriToSubtitleConfiguration(
                        uri = subtitleUri,
                        subtitleEncoding = playerPreferences.subtitleTextEncoding,
                    )
                }

                // Use placeholder artwork initially - actual artwork will be loaded in background
                val artworkUri = getDefaultArtworkUri()

                val queryTitle = uri.getQueryParameter("title")
                    ?: uri.getQueryParameter("caption")
                    ?: uri.getQueryParameter("name")
                val title = queryTitle?.takeIf { it.isNotBlank() }
                    ?: mediaItem.mediaMetadata.title?.takeIf { it.isNotBlank() && it.toString().lowercase() != "stream" && it.toString().lowercase() != "stream.m3u8" }
                    ?: video?.nameWithExtension
                    ?: getFilenameFromUri(uri)
                val positionMs = mediaItem.mediaMetadata.positionMs ?: videoState?.position
                val videoScale = mediaItem.mediaMetadata.videoZoom ?: videoState?.videoScale
                val playbackSpeed = mediaItem.mediaMetadata.playbackSpeed ?: videoState?.playbackSpeed
                val audioTrackIndex = mediaItem.mediaMetadata.audioTrackIndex ?: videoState?.audioTrackIndex
                val subtitleTrackIndex = mediaItem.mediaMetadata.subtitleTrackIndex ?: videoState?.subtitleTrackIndex
                val subtitleDelay = mediaItem.mediaMetadata.subtitleDelayMilliseconds ?: videoState?.subtitleDelayMilliseconds
                val subtitleSpeed = mediaItem.mediaMetadata.subtitleSpeed ?: videoState?.subtitleSpeed

                mediaItem.buildUpon().apply {
                    if (mediaItem.mediaId.startsWith("tdlib://") || mediaItem.mediaId.contains("/api/stream/")) {
                        setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP4)
                    }
                    setSubtitleConfigurations(existingSubConfigurations + subConfigurations)
                    setMediaMetadata(
                        MediaMetadata.Builder().apply {
                            setTitle(title)
                            setArtworkUri(artworkUri)
                            setExtras(
                                positionMs = positionMs,
                                videoScale = videoScale,
                                playbackSpeed = playbackSpeed,
                                audioTrackIndex = audioTrackIndex,
                                subtitleTrackIndex = subtitleTrackIndex,
                                subtitleDelayMilliseconds = subtitleDelay,
                                subtitleSpeed = subtitleSpeed,
                            )
                        }.build(),
                    )
                }.build()
            }
        }.awaitAll()
    }
    
    private fun getDefaultArtworkUri(): Uri = Uri.Builder().apply {
        val defaultArtwork = R.drawable.artwork_default
        scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        authority(resources.getResourcePackageName(defaultArtwork))
        appendPath(resources.getResourceTypeName(defaultArtwork))
        appendPath(resources.getResourceEntryName(defaultArtwork))
    }.build()

    private fun loadArtworkForCurrentMediaItem() {
        artworkLoadJob?.cancel()
        artworkLoadJob = serviceScope.launch(Dispatchers.Main) {
            val player = mediaSession?.player ?: return@launch
            val currentMediaItem = player.currentMediaItem ?: return@launch
            if (currentMediaItem.mediaMetadata.artworkData != null) return@launch

            val artworkUri = loadArtworkForMediaItem(currentMediaItem) ?: return@launch

            val updatedPlayer = mediaSession?.player ?: return@launch
            val updatedMediaItem = updatedPlayer.currentMediaItem ?: return@launch
            if (updatedMediaItem.mediaId != currentMediaItem.mediaId) return@launch

            updatedPlayer.replaceMediaItem(
                updatedPlayer.currentMediaItemIndex,
                updatedMediaItem.withArtwork(artworkUri),
            )
        }
    }
    private suspend fun loadArtworkForMediaItem(mediaItem: MediaItem): Uri? = withContext(Dispatchers.IO) {
        val uriString = mediaItem.mediaId
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            val video = mediaRepository.getVideoByUri(uri = uriString)
            val artworkUrl = video?.artworkUri
            if (!artworkUrl.isNullOrEmpty()) {
                return@withContext try {
                    val request = ImageRequest.Builder(this@PlayerService)
                        .data(artworkUrl)
                        .size(512, 512)
                        .build()
                    imageLoader.execute(request)
                    val diskCache = imageLoader.diskCache ?: return@withContext null
                    diskCache.openSnapshot(artworkUrl)?.use { snapshot ->
                        snapshot.data.toFile().toUri()
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("PlayerService", "Failed to load remote artwork for $artworkUrl: ${e.localizedMessage}", e)
                    null
                }
            }
            return@withContext null
        }
        val uri = uriString.toUri()
        return@withContext try {
            val request = ImageRequest.Builder(this@PlayerService)
                .data(uri)
                .size(512, 512)
                .build()
            imageLoader.execute(request)
            val diskCache = imageLoader.diskCache ?: return@withContext null
            return@withContext diskCache.openSnapshot(uri.toString())?.use { snapshot ->
                snapshot.data.toFile().toUri()
            }
        } catch (e: Throwable) {
            android.util.Log.e("PlayerService", "Failed to extract local video frame for $uriString: ${e.localizedMessage}", e)
            null
        }
    }
    private fun MediaItem.withArtwork(uri: Uri): MediaItem = buildUpon()
        .setMediaMetadata(
            mediaMetadata.buildUpon()
                .setArtworkUri(uri)
                .build(),
        )
        .build()
}

@get:UnstableApi
@set:UnstableApi
private var Player.playerSpecificSkipSilenceEnabled: Boolean
    @OptIn(UnstableApi::class)
    get() = when (this) {
        is ExoPlayer -> this.skipSilenceEnabled
        else -> false
    }
    set(value) {
        when (this) {
            is ExoPlayer -> this.skipSilenceEnabled = value
        }
    }

@get:UnstableApi
@set:UnstableApi
private var Player.playerSpecificSubtitleDelayMilliseconds: Long
    @OptIn(UnstableApi::class)
    get() = when (this) {
        is ExoPlayer -> this.subtitleDelayMilliseconds
        else -> 0L
    }
    set(value) {
        when (this) {
            is ExoPlayer -> this.subtitleDelayMilliseconds = value
        }
    }

@get:UnstableApi
@set:UnstableApi
private var Player.playerSpecificSubtitleSpeed: Float
    @OptIn(UnstableApi::class)
    get() = when (this) {
        is ExoPlayer -> this.subtitleSpeed
        else -> 0f
    }
    set(value) {
        when (this) {
            is ExoPlayer -> this.subtitleSpeed = value
        }
    }

@UnstableApi
private class ProxyHeaderDataSource(
    private val delegate: DataSource
) : DataSource {
    override fun addTransferListener(transferListener: TransferListener) {
        delegate.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        val uriString = uri.toString()
        val headers = dataSpec.httpRequestHeaders.toMutableMap()

        if (uriString.contains("/proxy/stream")) {
            headers["x-api-key"] = "ok223377"
        }

        if (uri.scheme == "http" || uri.scheme == "https") {
            if (!headers.containsKey("User-Agent")) {
                headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
            }
            if (!headers.containsKey("Accept")) {
                headers["Accept"] = "*/*"
            }
            if (!headers.containsKey("Accept-Language")) {
                headers["Accept-Language"] = "en-US,en;q=0.9"
            }
            if (!headers.containsKey("Sec-Fetch-Mode")) {
                headers["Sec-Fetch-Mode"] = "no-cors"
            }
            if (!headers.containsKey("Sec-Fetch-Dest")) {
                headers["Sec-Fetch-Dest"] = "video"
            }

            if (!headers.containsKey("Referer")) {
                val host = uri.host ?: ""
                when {
                    host.contains("scdn.st") || host.contains("streamtape") -> {
                        headers["Referer"] = "https://streamtape.com/"
                    }
                    host.isNotEmpty() -> {
                        headers["Referer"] = "${uri.scheme}://$host/"
                    }
                }
            }
        }

        val updatedDataSpec = dataSpec.buildUpon()
            .setHttpRequestHeaders(headers)
            .build()

        return try {
            delegate.open(updatedDataSpec)
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            if (e.responseCode == 503 || e.responseCode == 403) {
                val fallbackHeaders = headers.toMutableMap()
                fallbackHeaders["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
                val host = uri.host ?: ""
                if (host.isNotEmpty()) {
                    fallbackHeaders["Origin"] = "${uri.scheme}://$host"
                    if (host.contains("scdn.st") || host.contains("streamtape")) {
                        fallbackHeaders["Referer"] = "https://streamtape.com/"
                    } else {
                        fallbackHeaders["Referer"] = "${uri.scheme}://$host/"
                    }
                }
                val retryDataSpec = dataSpec.buildUpon()
                    .setHttpRequestHeaders(fallbackHeaders)
                    .build()
                delegate.open(retryDataSpec)
            } else {
                throw e
            }
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return delegate.read(buffer, offset, length)
    }

    override fun getUri(): Uri? {
        return delegate.getUri()
    }

    override fun close() {
        delegate.close()
    }
}
