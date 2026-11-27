package dev.roxy.kryptonplayer.feature.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import dagger.hilt.android.AndroidEntryPoint
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackLoopMode
import dev.roxy.kryptonplayer.core.common.playback.PlaybackMediaItem
import dev.roxy.kryptonplayer.core.common.playback.PlaybackStateSnapshot
import dev.roxy.kryptonplayer.core.data.repository.MediaRepository
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.ui.R as coreUiR
import dev.roxy.kryptonplayer.feature.player.PlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PlayerService : MediaBrowserServiceCompat() {
    @Inject
    lateinit var playbackController: PlaybackController

    @Inject
    lateinit var mediaRepository: MediaRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var transientAudioFocusLoss = false
    private var resumeOnAudioFocusGain = false

    private data class NotificationKey(
        val mediaId: String?,
        val title: String?,
        val artist: String?,
        val isPlaying: Boolean,
        val isLoading: Boolean,
        val isBuffering: Boolean,
        val isIdle: Boolean,
        val playlistSize: Int,
        val errorMessage: String?,
    )

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (preferencesRepository.playerPreferences.value.pauseOnHeadsetDisconnect) {
                    playbackController.pause()
                }
            }
        }
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                transientAudioFocusLoss = false
                resumeOnAudioFocusGain = false
                abandonAudioFocus()
                if (preferencesRepository.playerPreferences.value.requireAudioFocus) {
                    playbackController.pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                transientAudioFocusLoss = true
                resumeOnAudioFocusGain = playbackController.state.value.isPlaying
                hasAudioFocus = false
                if (preferencesRepository.playerPreferences.value.requireAudioFocus) {
                    playbackController.pause()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                val shouldResume = transientAudioFocusLoss && resumeOnAudioFocusGain
                transientAudioFocusLoss = false
                resumeOnAudioFocusGain = false
                if (shouldResume && preferencesRepository.playerPreferences.value.requireAudioFocus) {
                    playbackController.play()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel(this)

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setCallback(sessionCallback)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        ContextCompat.registerReceiver(this, noisyReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        serviceScope.launch {
            var lastNotificationKey: NotificationKey? = null
            playbackController.state.collect { snapshot ->
                updateAudioFocus(snapshot)
                updateMediaSessionState(snapshot)
                val key = NotificationKey(
                    mediaId = snapshot.currentItem?.mediaId,
                    title = snapshot.currentItem?.title,
                    artist = snapshot.currentItem?.artist,
                    isPlaying = snapshot.isPlaying,
                    isLoading = snapshot.isLoading,
                    isBuffering = snapshot.isBuffering,
                    isIdle = snapshot.isIdle,
                    playlistSize = snapshot.playlist.size,
                    errorMessage = snapshot.error?.message,
                )
                if (key != lastNotificationKey) {
                    lastNotificationKey = key
                    updateNotification(snapshot)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> playbackController.togglePlayPause()
            ACTION_NEXT -> playbackController.seekToNext()
            ACTION_PREVIOUS -> playbackController.seekToPrevious()
            ACTION_STOP -> {
                startForegroundWithType(buildNotification(playbackController.state.value))
                playbackController.stop(clearPlaylist = true)
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
        }
        startForegroundWithType(buildNotification(playbackController.state.value))
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!playbackController.state.value.isPlaying) {
            playbackController.stop(clearPlaylist = true)
            stopForegroundAndSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(noisyReceiver) }
        abandonAudioFocus()
        mediaSession.isActive = false
        mediaSession.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot = BrowserRoot(ROOT_ID, null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        result.detach()
        serviceScope.launch(Dispatchers.IO) {
            val items = when (parentId) {
                ROOT_ID -> mutableListOf(
                    browsableItem(RECENT_ID, getString(coreUiR.string.recently_played)),
                    browsableItem(ALL_MEDIA_ID, "All Media"),
                )
                RECENT_ID -> {
                    val videos = mediaRepository.getVideosFlow().firstOrNull().orEmpty()
                    videos
                        .filter { it.lastPlayedAt != null }
                        .sortedByDescending { it.lastPlayedAt?.time ?: 0L }
                        .ifEmpty { videos.sortedByDescending { it.dateModified } }
                        .map { video -> playableBrowserItem(video) }
                        .toMutableList()
                }
                ALL_MEDIA_ID -> {
                    mediaRepository.getVideosFlow().firstOrNull().orEmpty()
                        .sortedBy { it.displayName }
                        .map { video -> playableBrowserItem(video) }
                        .toMutableList()
                }
                else -> mutableListOf()
            }
            result.sendResult(items)
        }
    }

    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() = playbackController.play()
        override fun onPause() = playbackController.pause()
        override fun onStop() {
            playbackController.stop(clearPlaylist = true)
            stopForegroundAndSelf()
        }

        override fun onSeekTo(pos: Long) = playbackController.seekTo(pos, isUserSeek = true)
        override fun onSkipToNext() = playbackController.seekToNext()
        override fun onSkipToPrevious() = playbackController.seekToPrevious()
        override fun onSetPlaybackSpeed(speed: Float) = playbackController.setPlaybackSpeed(speed)

        override fun onSetRepeatMode(repeatMode: Int) {
            val mode = when (repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> PlaybackLoopMode.ONE
                PlaybackStateCompat.REPEAT_MODE_ALL -> PlaybackLoopMode.ALL
                else -> PlaybackLoopMode.OFF
            }
            playbackController.setRepeatMode(mode)
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            playbackController.setShuffleEnabled(shuffleMode != PlaybackStateCompat.SHUFFLE_MODE_NONE)
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            if (mediaId.isNullOrBlank()) return
            serviceScope.launch(Dispatchers.IO) {
                val items = when (mediaId) {
                    ROOT_ID, ALL_MEDIA_ID -> mediaRepository.getVideosFlow().firstOrNull().orEmpty()
                        .sortedBy { it.displayName }
                        .map { it.toPlaybackMediaItem() }
                    RECENT_ID -> {
                        val all = mediaRepository.getVideosFlow().firstOrNull().orEmpty()
                        all.filter { it.lastPlayedAt != null }
                            .sortedByDescending { it.lastPlayedAt?.time ?: 0L }
                            .ifEmpty { all.sortedByDescending { it.dateModified } }
                            .map { it.toPlaybackMediaItem() }
                    }
                    else -> {
                        val video = mediaRepository.getVideoByUri(mediaId)
                        listOf(
                            video?.toPlaybackMediaItem() ?: PlaybackMediaItem(
                                mediaId = mediaId,
                                uri = mediaId.toUri(),
                                title = mediaId.toUri().lastPathSegment ?: mediaId,
                            ),
                        )
                    }
                }
                if (items.isNotEmpty()) {
                    serviceScope.launch(Dispatchers.Main.immediate) {
                        playbackController.setPlaylist(items, startIndex = 0, playWhenReady = true)
                    }
                }
            }
        }
    }

    private fun updateMediaSessionState(snapshot: PlaybackStateSnapshot) {
        val currentItem = snapshot.currentItem
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentItem?.title?.ifBlank { "Krypton Player" } ?: "Krypton Player")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentItem?.artist ?: "Krypton Player")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, snapshot.durationMs)
            .build()
        mediaSession.setMetadata(metadata)

        val pbState = when {
            snapshot.isLoading || snapshot.isBuffering -> PlaybackStateCompat.STATE_BUFFERING
            snapshot.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            snapshot.error != null -> PlaybackStateCompat.STATE_ERROR
            snapshot.isIdle && snapshot.playlist.isEmpty() -> PlaybackStateCompat.STATE_STOPPED
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED or
                    PlaybackStateCompat.ACTION_SET_REPEAT_MODE or
                    PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE,
            )
            .setState(pbState, snapshot.currentPositionMs, snapshot.playbackSpeed)
        snapshot.error?.let { err ->
            stateBuilder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, err.message)
        }
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun updateNotification(snapshot: PlaybackStateSnapshot) {
        if (snapshot.isIdle && snapshot.playlist.isEmpty()) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(snapshot))
    }

    private fun buildNotification(snapshot: PlaybackStateSnapshot): Notification {
        val item = snapshot.currentItem
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, PlayerActivity::class.java).apply {
                item?.uri?.let { data = it }
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val playPauseAction = if (snapshot.isPlaying) {
            NotificationCompat.Action(
                coreUiR.drawable.ic_pause,
                "Pause",
                serviceActionIntent(ACTION_PLAY_PAUSE, 1),
            )
        } else {
            NotificationCompat.Action(
                coreUiR.drawable.ic_play,
                "Play",
                serviceActionIntent(ACTION_PLAY_PAUSE, 1),
            )
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(coreUiR.drawable.ic_launcher_monochrome)
            .setContentTitle(item?.title?.ifBlank { "Krypton Player" } ?: "Krypton Player")
            .setContentText(item?.artist ?: "libmpv Playback")
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(snapshot.isPlaying)
            .addAction(coreUiR.drawable.ic_skip_prev, "Previous", serviceActionIntent(ACTION_PREVIOUS, 2))
            .addAction(playPauseAction)
            .addAction(coreUiR.drawable.ic_skip_next, "Next", serviceActionIntent(ACTION_NEXT, 3))
            .addAction(coreUiR.drawable.ic_close, "Stop", serviceActionIntent(ACTION_STOP, 4))
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .build()
    }

    private fun serviceActionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, PlayerService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun startForegroundWithType(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun stopForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateAudioFocus(snapshot: PlaybackStateSnapshot) {
        if (!preferencesRepository.playerPreferences.value.requireAudioFocus) {
            if (hasAudioFocus || transientAudioFocusLoss) abandonAudioFocus()
            return
        }
        if (snapshot.isIdle && snapshot.playlist.isEmpty()) {
            if (hasAudioFocus || transientAudioFocusLoss) abandonAudioFocus()
            return
        }
        if (snapshot.isPlaying && !hasAudioFocus) {
            transientAudioFocusLoss = false
            resumeOnAudioFocusGain = false
            requestAudioFocus()
        } else if (!snapshot.isPlaying && hasAudioFocus && !transientAudioFocusLoss) {
            abandonAudioFocus()
        }
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocus() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build(),
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
                .also { audioFocusRequest = it }
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        hasAudioFocus = granted
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            audioManager.abandonAudioFocus(audioFocusListener)
        }
        hasAudioFocus = false
        transientAudioFocusLoss = false
        resumeOnAudioFocusGain = false
    }

    private fun browsableItem(id: String, title: String): MediaBrowserCompat.MediaItem {
        val desc = MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .build()
        return MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    private fun playableBrowserItem(video: dev.roxy.kryptonplayer.core.model.Video): MediaBrowserCompat.MediaItem {
        val uri = if (video.uriString.isNotEmpty()) video.uriString.toUri() else File(video.path).toUri()
        val id = if (video.uriString.isNotEmpty()) video.uriString else video.path
        val desc = MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(video.displayName)
            .setMediaUri(uri)
            .build()
        return MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    private fun dev.roxy.kryptonplayer.core.model.Video.toPlaybackMediaItem(): PlaybackMediaItem {
        val uri = if (uriString.isNotEmpty()) uriString.toUri() else File(path).toUri()
        val id = if (uriString.isNotEmpty()) uriString else path
        return PlaybackMediaItem(
            mediaId = id,
            uri = uri,
            title = displayName,
            durationMs = duration,
            positionMs = playbackPosition,
        )
    }

    companion object {
        private const val TAG = "PlayerService"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "krypton_playback_channel"
        private const val ROOT_ID = "krypton_root"
        private const val RECENT_ID = "recent"
        private const val ALL_MEDIA_ID = "all_media"
        private const val ACTION_PLAY_PAUSE = "dev.roxy.kryptonplayer.action.PLAY_PAUSE"
        private const val ACTION_NEXT = "dev.roxy.kryptonplayer.action.NEXT"
        private const val ACTION_PREVIOUS = "dev.roxy.kryptonplayer.action.PREVIOUS"
        private const val ACTION_STOP = "dev.roxy.kryptonplayer.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, PlayerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlayerService::class.java))
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Active media playback controls"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
