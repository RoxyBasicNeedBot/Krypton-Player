package dev.roxy.kryptonplayer.feature.player.state

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackEvent
import dev.roxy.kryptonplayer.core.model.ScreenOrientation

@Composable
fun rememberRotationState(
    player: PlaybackController,
    screenOrientation: ScreenOrientation,
): RotationState {
    val activity = LocalActivity.current as ComponentActivity
    val rotationState = remember(activity, player, screenOrientation) {
        RotationState(
            activity = activity,
            player = player,
            screenOrientation = screenOrientation,
        )
    }
    DisposableEffect(activity) {
        rotationState.handleListeners(this)
    }
    LaunchedEffect(player, screenOrientation) { rotationState.observe() }
    return rotationState
}

@Stable
class RotationState(
    private val activity: ComponentActivity,
    private val player: PlaybackController,
    private val screenOrientation: ScreenOrientation,
) {
    var currentRequestedOrientation: Int by mutableIntStateOf(activity.requestedOrientation)
        private set

    fun rotate() {
        activity.requestedOrientation = when (activity.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    fun handleListeners(disposableEffectScope: DisposableEffectScope): DisposableEffectResult = with(disposableEffectScope) {
        val configurationChangedListener: Consumer<Configuration> = Consumer {
            currentRequestedOrientation = activity.requestedOrientation
        }
        activity.addOnConfigurationChangedListener(configurationChangedListener)
        onDispose {
            activity.removeOnConfigurationChangedListener(configurationChangedListener)
        }
    }

    suspend fun observe() {
        setOrientation()
        player.events.collect { event ->
            if (event is PlaybackEvent.VideoSizeChanged && screenOrientation == ScreenOrientation.VIDEO_ORIENTATION) {
                activity.requestedOrientation = getVideoBasedOrientation(event.width, event.height)
            }
        }
    }

    private fun setOrientation() {
        if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            val snap = player.state.value
            activity.requestedOrientation = when (screenOrientation) {
                ScreenOrientation.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ScreenOrientation.LANDSCAPE_REVERSE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                ScreenOrientation.LANDSCAPE_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                ScreenOrientation.VIDEO_ORIENTATION -> getVideoBasedOrientation(snap.videoWidth, snap.videoHeight)
            }
        }
    }

    private fun getVideoBasedOrientation(width: Int, height: Int) = when {
        width == 0 || height == 0 -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        height > width -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}
