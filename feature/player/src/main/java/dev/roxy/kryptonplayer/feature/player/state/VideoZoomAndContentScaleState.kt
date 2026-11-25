package dev.roxy.kryptonplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Constraints
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackMediaItem
import dev.roxy.kryptonplayer.core.model.VideoContentScale
import dev.roxy.kryptonplayer.feature.player.extensions.next
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun rememberVideoZoomAndContentScaleState(
    player: PlaybackController,
    initialContentScale: VideoContentScale,
    enableZoomGesture: Boolean,
    enablePanGesture: Boolean,
    onEvent: (VideoZoomEvent) -> Unit = {},
): VideoZoomAndContentScaleState {
    val coroutineScope = rememberCoroutineScope()
    val videoZoomAndContentScaleState = remember(player, initialContentScale, enableZoomGesture, enablePanGesture) {
        VideoZoomAndContentScaleState(
            player = player,
            initialContentScale = initialContentScale,
            enableZoomGesture = enableZoomGesture,
            enablePanGesture = enablePanGesture,
            onEvent = onEvent,
            coroutineScope = coroutineScope,
        )
    }
    LaunchedEffect(player) { videoZoomAndContentScaleState.observe() }
    return videoZoomAndContentScaleState
}

@Stable
class VideoZoomAndContentScaleState(
    private val player: PlaybackController,
    initialContentScale: VideoContentScale,
    private val enableZoomGesture: Boolean = true,
    private val enablePanGesture: Boolean = true,
    private val onEvent: (VideoZoomEvent) -> Unit,
    private val coroutineScope: CoroutineScope,
) {
    companion object {
        private const val MIN_ZOOM = 0.25f
        private const val MAX_ZOOM = 4f
        private const val CONTENT_SCALE_INDICATOR_DURATION_MS = 1000L
    }

    var videoContentScale: VideoContentScale by mutableStateOf(initialContentScale)
        private set

    var zoom: Float by mutableFloatStateOf(1f)
        private set

    var offset: Offset by mutableStateOf(Offset.Zero)
        private set

    var isZooming: Boolean by mutableStateOf(false)
        private set

    var showContentScaleIndicator: Boolean by mutableStateOf(false)
        private set

    private var showContentScaleJob: Job? = null

    fun onVideoContentScaleChanged(newContentScale: VideoContentScale) {
        videoContentScale = newContentScale
        zoom = 1f
        offset = Offset.Zero
        onEvent(VideoZoomEvent.ContentScaleChanged(videoContentScale))
        updateVideoScaleMetadataAndSendEvent()
        showContentScaleIndicator()
    }

    private fun showContentScaleIndicator() {
        showContentScaleJob?.cancel()
        showContentScaleIndicator = true
        showContentScaleJob = coroutineScope.launch {
            delay(CONTENT_SCALE_INDICATOR_DURATION_MS)
            showContentScaleIndicator = false
            showContentScaleJob = null
        }
    }

    fun switchToNextVideoContentScale() {
        onVideoContentScaleChanged(videoContentScale.next())
    }

    fun onZoomPanGesture(constraints: Constraints, panChange: Offset, zoomChange: Float) {
        if (!enableZoomGesture) return
        if (player.state.value.durationMs <= 0L) return

        isZooming = true
        zoom = (zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)

        val extraWidth = (zoom - 1) * constraints.maxWidth
        val extraHeight = (zoom - 1) * constraints.maxHeight
        val maxX = abs(extraWidth / 2)
        val maxY = abs(extraHeight / 2)

        if (enablePanGesture) {
            offset = Offset(
                x = (offset.x + zoom * panChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + zoom * panChange.y).coerceIn(-maxY, maxY),
            )
        }
    }

    fun onZoomPanGestureEnd() {
        isZooming = false
        updateVideoScaleMetadataAndSendEvent()
    }

    suspend fun observe() {
        player.state.collect { snapshot ->
            val savedScale = snapshot.currentItem?.videoScale
            if (savedScale != null && !isZooming) {
                zoom = savedScale
            }
        }
    }

    private fun updateVideoScaleMetadataAndSendEvent(zoom: Float = this.zoom) {
        val currentMediaItem = player.state.value.currentItem ?: return
        val updated = currentMediaItem.copy(videoScale = zoom)
        player.updateCurrentItemMetadata { it.copy(videoScale = zoom) }
        onEvent(VideoZoomEvent.ZoomChanged(updated, zoom))
    }
}

sealed interface VideoZoomEvent {
    data class ContentScaleChanged(val contentScale: VideoContentScale) : VideoZoomEvent
    data class ZoomChanged(val mediaItem: PlaybackMediaItem, val zoom: Float) : VideoZoomEvent
}
