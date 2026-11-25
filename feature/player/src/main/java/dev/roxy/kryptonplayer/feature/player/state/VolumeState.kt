package dev.roxy.kryptonplayer.feature.player.state

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import kotlin.math.roundToInt

@Composable
fun rememberVolumeState(
    player: PlaybackController? = null,
    showVolumePanelIfHeadsetIsOn: Boolean = false,
    enableVolumeBoost: Boolean = true,
): VolumeState {
    val context = LocalContext.current
    val volumeState = remember(context, player, showVolumePanelIfHeadsetIsOn, enableVolumeBoost) {
        VolumeState(
            context = context,
            player = player,
            showVolumePanelIfHeadsetIsOn = showVolumePanelIfHeadsetIsOn,
            enableVolumeBoost = enableVolumeBoost,
        )
    }
    LaunchedEffect(player) {
        player?.state?.collect { snap ->
            volumeState.syncLoudnessGain(snap.volumeBoostGainMb)
        }
    }
    return volumeState
}

@Stable
class VolumeState(
    private val context: Context,
    private val player: PlaybackController?,
    private val showVolumePanelIfHeadsetIsOn: Boolean,
    private val enableVolumeBoost: Boolean,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxSystemVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    val maxVolumePercentage: Int = if (enableVolumeBoost) 200 else 100

    var volumePercentage: Int by mutableIntStateOf(readCurrentPercentage())
        private set

    fun updateVolumePercentage(percentage: Int) {
        val clamped = percentage.coerceIn(0, maxVolumePercentage)
        volumePercentage = clamped
        val systemRatio = (clamped.coerceAtMost(100) / 100f)
        val targetStreamVol = (systemRatio * maxSystemVolume).roundToInt().coerceIn(0, maxSystemVolume)
        val flags = if (showVolumePanelIfHeadsetIsOn && audioManager.isWiredHeadsetOn) AudioManager.FLAG_SHOW_UI else 0
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStreamVol, flags)

        val gainMb = if (enableVolumeBoost && clamped > 100) {
            ((clamped - 100) * 20).coerceIn(0, 2000)
        } else {
            0
        }
        player?.setLoudnessGainMb(gainMb)
    }

    fun increaseVolume(delta: Int = 5) {
        updateVolumePercentage(volumePercentage + delta)
    }

    fun decreaseVolume(delta: Int = 5) {
        updateVolumePercentage(volumePercentage - delta)
    }

    internal fun syncLoudnessGain(gainMb: Int) {
        val boostPct = if (enableVolumeBoost && gainMb > 0) (gainMb / 20).coerceIn(0, 100) else 0
        val basePct = readSystemPercentage()
        volumePercentage = if (boostPct > 0) 100 + boostPct else basePct
    }

    private fun readSystemPercentage(): Int {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((current.toFloat() / maxSystemVolume) * 100f).roundToInt().coerceIn(0, 100)
    }

    private fun readCurrentPercentage(): Int = readSystemPercentage()
}
