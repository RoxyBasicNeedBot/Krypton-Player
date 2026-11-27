package dev.roxy.kryptonplayer.feature.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.roxy.kryptonplayer.core.common.extensions.getMediaContentUri
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackEvent
import dev.roxy.kryptonplayer.core.common.playback.PlaybackLoopMode
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.core.ui.theme.LocalPlaybackController
import dev.roxy.kryptonplayer.feature.player.extensions.registerForSuspendActivityResult
import dev.roxy.kryptonplayer.feature.player.extensions.seekBack
import dev.roxy.kryptonplayer.feature.player.extensions.seekForward
import dev.roxy.kryptonplayer.feature.player.utils.PlayerApi
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalUseMaterialYouControls = compositionLocalOf { false }

@SuppressLint("UnsafeOptInUsageError")
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    val playerPreferences get() = viewModel.uiState.value.playerPreferences

    private val onWindowAttributesChangedListener = CopyOnWriteArrayList<Consumer<WindowManager.LayoutParams?>>()

    private var isPlaybackFinished = false
    private var playInBackground: Boolean = false
    private var isIntentNew: Boolean = true
    private var isStarted: Boolean = false
    private var handledNewIntentInBackground: Boolean = false
    private var playVideoJob: Job? = null

    /**
     * Player
     */
    @Inject
    lateinit var playbackController: PlaybackController

    private var eventsJob: Job? = null
    private lateinit var playerApi: PlayerApi

    private val subtitleFileSuspendLauncher = registerForSuspendActivityResult(OpenDocument())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerApi = PlayerApi(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val appPreferences = uiState.applicationPreferences ?: dev.roxy.kryptonplayer.core.model.ApplicationPreferences()
            CompositionLocalProvider(
                LocalUseMaterialYouControls provides (uiState.playerPreferences?.useMaterialYouControls == true),
                LocalPlaybackController provides playbackController,
            ) {
                KryptonPlayerTheme(
                    darkTheme = true,
                    highContrastDarkTheme = appPreferences.useHighContrastDarkTheme,
                    dynamicColor = appPreferences.useDynamicColors,
                    customThemeValue = appPreferences.customThemeValue,
                    pureBlack = appPreferences.pureBlack,
                ) {
                    MediaPlayerScreen(
                        player = playbackController,
                        viewModel = viewModel,
                        playerPreferences = uiState.playerPreferences ?: return@KryptonPlayerTheme,
                        appPreferences = appPreferences,
                        onSelectSubtitleClick = {
                            lifecycleScope.launch {
                                val uri = subtitleFileSuspendLauncher.launch(
                                    arrayOf(
                                        "application/x-subrip",
                                        "application/ttml+xml",
                                        "text/vtt",
                                        "text/x-ssa",
                                        "application/octet-stream",
                                        "text/*",
                                    ),
                                ) ?: return@launch
                                runCatching {
                                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                playbackController.addExternalSubtitle(uri, select = true)
                            }
                        },
                        onOnlineSubtitleSelected = { file ->
                            playbackController.addExternalSubtitle(Uri.fromFile(file), select = true)
                        },
                        onBackClick = { finishAndStopPlayerSession() },
                        onPlayInBackgroundClick = {
                            playInBackground = true
                            finish()
                        },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isStarted = true
        eventsJob?.cancel()
        eventsJob = lifecycleScope.launch {
            playbackController.events.collect { event ->
                when (event) {
                    is PlaybackEvent.MediaItemTransition -> {
                        intent.data = event.item?.uri
                    }
                    is PlaybackEvent.IsPlayingChanged -> {
                        updateKeepScreenOnFlag()
                    }
                    is PlaybackEvent.PlaybackEnded -> {
                        if (playbackController.state.value.repeatMode == PlaybackLoopMode.OFF) {
                            isPlaybackFinished = true
                            finishAndStopPlayerSession()
                        }
                    }
                    else -> Unit
                }
            }
        }
        updateKeepScreenOnFlag()
        if (handledNewIntentInBackground) {
            handledNewIntentInBackground = false
        } else {
            startPlayback()
        }
    }

    override fun onStop() {
        isStarted = false
        val snap = playbackController.state.value
        viewModel.playWhenReady = snap.isPlaying || snap.isLoading
        eventsJob?.cancel()
        eventsJob = null

        val shouldPlayInBackground = playInBackground || playerPreferences?.autoBackgroundPlay == true
        if (subtitleFileSuspendLauncher.isAwaitingResult || !shouldPlayInBackground) {
            playbackController.pause()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            finish()
            if (!shouldPlayInBackground) {
                playbackController.stop(clearPlaylist = true)
            }
        }
        super.onStop()
    }

    private fun startPlayback() {
        val uri = intent.data ?: return

        val isCustomScheme = (uri.scheme == "kryptonplayer" || uri.scheme == "nextplayer") && uri.host == "sync" && uri.pathSegments.firstOrNull() == "join"
        val isHttpsScheme = (uri.scheme == "http" || uri.scheme == "https") && dev.roxy.kryptonplayer.core.data.network.NetworkConfig.isWorkerHost(uri.host) && uri.path?.contains("/sync/join/") == true

        if (isCustomScheme || isHttpsScheme) {
            val roomId = uri.lastPathSegment
            if (roomId != null) {
                joinWatchPartyFromDeepLink(roomId)
                return
            }
        }

        val qualityUrls = intent.getStringArrayListExtra("quality_urls")
        if (qualityUrls != null) {
            viewModel.setQualityUrls(qualityUrls)
        }

        val snap = playbackController.state.value
        val currentItem = snap.currentItem
        val isSessionActive = !snap.isEnded && !snap.isIdle && snap.error == null && snap.playlist.isNotEmpty()
        val returningFromBackground = !isIntentNew && currentItem != null && isSessionActive
        val isNewUriTheCurrentMediaItem = currentItem?.uri?.toString() == uri.toString() && isSessionActive

        if (returningFromBackground || isNewUriTheCurrentMediaItem) {
            isIntentNew = false
            if (viewModel.playWhenReady) {
                playbackController.play()
            } else {
                playbackController.pause()
            }
            return
        }

        isIntentNew = false

        playVideoJob?.cancel()
        playVideoJob = lifecycleScope.launch {
            playVideo(uri)
        }
    }

    private fun joinWatchPartyFromDeepLink(roomId: String) {
        viewModel.joinWatchPartyFromDeepLink(roomId) { streamUrl, videoName, err ->
            if (err != null) {
                android.widget.Toast.makeText(this, err, android.widget.Toast.LENGTH_LONG).show()
                finish()
            } else if (streamUrl != null) {
                intent.data = Uri.parse(streamUrl)
                intent.putExtra("watch_party_room_id", roomId)
                isIntentNew = true
                startPlayback()
            }
        }
    }

    private suspend fun playVideo(uri: Uri) = withContext(Dispatchers.Default) {
        val isTelegramMedia = intent.getBooleanExtra("is_telegram_cloud", false) ||
            uri.scheme == "tdlib" ||
            uri.path?.contains("offline_media") == true ||
            (uri.scheme == "file" && (uri.path?.contains("/files/") == true || uri.path?.contains("offline_media") == true))

        val mediaContentUri = if (isTelegramMedia) null else getMediaContentUri(uri)
        val playlist = playerApi.getPlaylist().takeIf { it.isNotEmpty() }
            ?: mediaContentUri?.let { mediaUri ->
                viewModel.getPlaylistFromUri(mediaUri)
                    .map { it.uriString }
                    .toMutableList()
                    .apply {
                        if (!contains(mediaUri.toString())) {
                            add(index = 0, element = mediaUri.toString())
                        }
                    }
            } ?: listOf(uri.toString())

        val mediaItemIndexToPlay = playlist.indexOfFirst {
            it == (mediaContentUri ?: uri).toString()
        }.takeIf { it >= 0 } ?: 0

        val mediaItems = playlist.mapIndexed { index, itemUriStr ->
            val parsedUri = itemUriStr.toUri()
            val streamTitle = if (index == mediaItemIndexToPlay) {
                playerApi.title
                    ?: parsedUri.getQueryParameter("title")
                    ?: parsedUri.getQueryParameter("caption")
                    ?: parsedUri.lastPathSegment
                    ?: ""
            } else {
                parsedUri.lastPathSegment ?: ""
            }
            val apiSubs = if (index == mediaItemIndexToPlay) {
                playerApi.getSubs().map { it.uri }
            } else {
                emptyList()
            }
            dev.roxy.kryptonplayer.core.common.playback.PlaybackMediaItem(
                mediaId = itemUriStr,
                uri = parsedUri,
                title = streamTitle,
                positionMs = if (index == mediaItemIndexToPlay) playerApi.position?.toLong() ?: 0L else 0L,
                externalSubtitles = apiSubs,
            )
        }

        withContext(Dispatchers.Main) {
            playbackController.setPlaylist(
                items = mediaItems,
                startIndex = mediaItemIndexToPlay,
                startPositionMs = playerApi.position?.toLong() ?: -1L,
                playWhenReady = viewModel.playWhenReady,
            )
        }
    }

    override fun finish() {
        if (::playerApi.isInitialized && playerApi.shouldReturnResult) {
            val snap = playbackController.state.value
            val result = playerApi.getResult(
                isPlaybackFinished = isPlaybackFinished,
                duration = snap.durationMs.takeIf { it > 0L } ?: -1L,
                position = snap.currentPositionMs,
            )
            setResult(RESULT_OK, result)
        }
        super.finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data != null) {
            setIntent(intent)
            isIntentNew = true
            handledNewIntentInBackground = !isStarted
            startPlayback()
        }
    }

    private fun updateKeepScreenOnFlag() {
        if (playbackController.state.value.isPlaying) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun finishAndStopPlayerSession() {
        finish()
        playbackController.stop(clearPlaylist = true)
    }

    override fun onWindowAttributesChanged(params: WindowManager.LayoutParams?) {
        super.onWindowAttributesChanged(params)
        for (listener in onWindowAttributesChangedListener) {
            listener.accept(params)
        }
    }

    fun addOnWindowAttributesChangedListener(listener: Consumer<WindowManager.LayoutParams?>) {
        onWindowAttributesChangedListener.add(listener)
    }

    fun removeOnWindowAttributesChangedListener(listener: Consumer<WindowManager.LayoutParams?>) {
        onWindowAttributesChangedListener.remove(listener)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            dev.roxy.kryptonplayer.feature.player.state.SeekbarThumbnailState.clearAllCaches()
            System.gc()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                playbackController.togglePlayPause()
                true
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                playbackController.seekBack()
                true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                playbackController.seekForward()
                true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                playbackController.togglePlayPause()
                true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                playbackController.play()
                true
            }

            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                playbackController.pause()
                true
            }

            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                playbackController.seekToNext()
                true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                playbackController.seekToPrevious()
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }
}
