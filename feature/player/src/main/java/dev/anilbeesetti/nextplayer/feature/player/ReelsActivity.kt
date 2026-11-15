package dev.anilbeesetti.nextplayer.feature.player

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import dagger.hilt.android.AndroidEntryPoint
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.model.ReelItem
import kotlinx.serialization.json.Json

import javax.inject.Inject
import dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibManager
import dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibMediaResolver

@AndroidEntryPoint
class ReelsActivity : ComponentActivity() {

    @Inject
    lateinit var tdlibManager: TdlibManager

    @Inject
    lateinit var tdlibMediaResolver: TdlibMediaResolver

    @Inject
    lateinit var telegramRepository: dev.anilbeesetti.nextplayer.core.data.repository.TelegramRepository

    @Inject
    lateinit var preferencesRepository: dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository

    @Inject
    lateinit var telegramLikedRepository: dev.anilbeesetti.nextplayer.core.data.repository.TelegramLikedRepository

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
            NextPlayerTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ComposeColor.Black
                ) {
                    ReelsScreen(
                        playlist = playlist,
                        startIndex = startIndex,
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

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Companion.playlist = emptyList()
            Companion.playlistKey = ""
        }
    }
}
