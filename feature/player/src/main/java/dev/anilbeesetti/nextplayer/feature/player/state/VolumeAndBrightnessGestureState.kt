package dev.anilbeesetti.nextplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.IntSize
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberVolumeAndBrightnessGestureState(
    volumeState: VolumeState,
    brightnessState: BrightnessState,
    enableVolumeGesture: Boolean,
    enableBrightnessGesture: Boolean,
    volumeGestureSensitivity: Float,
    brightnessGestureSensitivity: Float,
): VolumeAndBrightnessGestureState {
    val coroutineScope = rememberCoroutineScope()
    val volumeAndBrightnessGestureState = remember(
        volumeState,
        brightnessState,
        enableVolumeGesture,
        enableBrightnessGesture,
        volumeGestureSensitivity,
        brightnessGestureSensitivity,
    ) {
        VolumeAndBrightnessGestureState(
            volumeState = volumeState,
            brightnessState = brightnessState,
            enableVolumeGesture = enableVolumeGesture,
            enableBrightnessGesture = enableBrightnessGesture,
            volumeGestureSensitivity = volumeGestureSensitivity,
            brightnessGestureSensitivity = brightnessGestureSensitivity,
            coroutineScope = coroutineScope,
        )
    }
    return volumeAndBrightnessGestureState
}

@Stable
class VolumeAndBrightnessGestureState(
    private val volumeState: VolumeState,
    private val brightnessState: BrightnessState,
    private val enableVolumeGesture: Boolean = true,
    private val enableBrightnessGesture: Boolean = true,
    private val volumeGestureSensitivity: Float,
    private val brightnessGestureSensitivity: Float,
    private val coroutineScope: CoroutineScope,
) {
    var activeGesture: VerticalGesture? by mutableStateOf(null)
        private set

    var displayVolumePercentage: Int by mutableIntStateOf(volumeState.volumePercentage)
        private set

    var displayBrightnessPercentage: Int by mutableIntStateOf(brightnessState.brightnessPercentage)
        private set

    var volumeChangePercentage: Int by mutableIntStateOf(0)
        private set

    var brightnessChangePercentage: Int by mutableIntStateOf(0)
        private set

    private var currentVolumeFloat = 0f
    private var currentBrightnessFloat = 0f
    private var job: Job? = null

    fun onDragStart(offset: Offset, size: IntSize) {
        val viewCenterX = size.width / 2
        job?.cancel()
        activeGesture = when {
            offset.x < viewCenterX -> VerticalGesture.BRIGHTNESS.takeIf { enableBrightnessGesture }
            else -> VerticalGesture.VOLUME.takeIf { enableVolumeGesture }
        }
        currentVolumeFloat = volumeState.volumePercentage.toFloat()
        currentBrightnessFloat = brightnessState.brightnessPercentage.toFloat()
        displayVolumePercentage = volumeState.volumePercentage
        displayBrightnessPercentage = brightnessState.brightnessPercentage
    }

    fun onDrag(change: PointerInputChange, dragAmount: Float) {
        val activeGesture = activeGesture ?: return

        when (activeGesture) {
            VerticalGesture.VOLUME -> {
                val maxVolume = volumeState.maxVolumePercentage.toFloat()
                // Convert vertical movement (-dragAmount: up is positive) smoothly
                val delta = -dragAmount * (volumeGestureSensitivity * 0.22f)
                currentVolumeFloat = (currentVolumeFloat + delta).coerceIn(0f, maxVolume)
                val newVolume = kotlin.math.round(currentVolumeFloat).toInt()
                displayVolumePercentage = newVolume
                volumeChangePercentage = newVolume - volumeState.volumePercentage
                brightnessChangePercentage = 0
                volumeState.updateVolumePercentage(newVolume)
            }

            VerticalGesture.BRIGHTNESS -> {
                val maxBrightness = MAX_BRIGHTNESS_PERCENTAGE.toFloat()
                val delta = -dragAmount * (brightnessGestureSensitivity * 0.22f)
                currentBrightnessFloat = (currentBrightnessFloat + delta).coerceIn(0f, maxBrightness)
                val newBrightness = kotlin.math.round(currentBrightnessFloat).toInt()
                displayBrightnessPercentage = newBrightness
                brightnessChangePercentage = newBrightness - brightnessState.brightnessPercentage
                volumeChangePercentage = 0
                brightnessState.updateBrightnessPercentage(newBrightness)
            }
        }
    }

    fun onDragEnd() {
        job?.cancel()
        job = coroutineScope.launch {
            delay(1000L)
            activeGesture = null
            volumeChangePercentage = 0
            brightnessChangePercentage = 0
        }
    }

    companion object {
        private const val MAX_BRIGHTNESS_PERCENTAGE = 100
    }
}

enum class VerticalGesture {
    VOLUME,
    BRIGHTNESS,
}
