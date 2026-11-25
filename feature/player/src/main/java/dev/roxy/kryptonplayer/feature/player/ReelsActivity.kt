package dev.roxy.kryptonplayer.feature.player

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.feature.player.model.ReelItem
import kotlinx.serialization.json.Json

import javax.inject.Inject
import dev.roxy.kryptonplayer.core.data.tdlib.TdlibManager
import dev.roxy.kryptonplayer.core.data.tdlib.TdlibMediaResolver

@AndroidEntryPoint
class ReelsActivity : ComponentActivity() {

    @Inject
    lateinit var playbackController: PlaybackController

    @Inject
    lateinit var tdlibManager: TdlibManager

    @Inject
    lateinit var tdlibMediaResolver: TdlibMediaResolver

    @Inject
    lateinit var telegramRepository: dev.roxy.kryptonplayer.core.data.repository.TelegramRepository

    @Inject
    lateinit var preferencesRepository: dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository

    @Inject
    lateinit var telegramLikedRepository: dev.roxy.kryptonplayer.core.data.repository.TelegramLikedRepository

    companion object {
        var playlist: List<ReelItem> = emptyList()
        var playlistKey: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val startIndex = intent.getIntExtra("reels_start_index", 0)

        val playlist = if (Companion.playlist.isNotEmpty()) {
            Companion.playlist
        } else {
            val playlistJson = intent.getStringExtra("reels_playlist") ?: "[]"
            try {
                Json.decodeFromString<List<ReelItem>>(playlistJson)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

        setContent {
            KryptonPlayerTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ComposeColor.Black
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val reelModifier = if (maxWidth > 600.dp) {
                            Modifier
                                .fillMaxHeight()
                                .aspectRatio(9f / 16f)
                        } else {
                            Modifier.fillMaxSize()
                        }
                        Box(
                            modifier = reelModifier,
                            contentAlignment = Alignment.Center
                        ) {
                            ReelsScreen(
                                playlist = playlist,
                                startIndex = startIndex,
                                playbackController = playbackController,
                                tdlibManager = tdlibManager,
                                tdlibMediaResolver = tdlibMediaResolver,
                                telegramRepository = telegramRepository,
                                preferencesRepository = preferencesRepository,
                                telegramLikedRepository = telegramLikedRepository,
                                onBackClick = { finish() },
                                onRotateClick = {
                                    requestedOrientation = if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Companion.playlist = emptyList()
            Companion.playlistKey = ""
        }
    }
}
