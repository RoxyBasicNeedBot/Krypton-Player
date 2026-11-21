package dev.roxy.kryptonplayer.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.roxy.kryptonplayer.wear.ui.WearNowPlayingScreen

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearNowPlayingScreen()
        }
    }
}
