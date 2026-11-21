package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Seekable wavy progress bar for M3 Expressive player aesthetic.
 *
 * - The wave amplitude animates while playing and flattens when paused or during scrubbing.
 * - The thumb morphs from a Ø14dp circle into a 6×22dp rounded scrub pill on user interaction.
 *
 * @param progressFraction current playback progress in 0..1 range.
 * @param onSeekProgress called with new progress in 0..1 range during tap/drag.
 * @param onSeekFinished called when user interaction finishes to commit the seek.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavySeekBar(
    progressFraction: Float,
    isPlaying: Boolean,
    onSeekProgress: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
    thumbColor: Color = MaterialTheme.colorScheme.primary,
) {
    var isInteracting by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }

    val displayedFraction = (if (isInteracting) dragFraction else progressFraction).coerceIn(0f, 1f)
    val targetAmplitude = if (isPlaying && !isInteracting) 1f else 0f

    val thumbMorph by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(250),
        label = "wavySeekBarThumbMorph",
    )

    fun fractionAt(x: Float): Float = if (widthPx <= 0) 0f else (x / widthPx).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = fractionAt(offset.x)
                    dragFraction = fraction
                    onSeekProgress(fraction)
                    onSeekFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isInteracting = true
                        val fraction = fractionAt(offset.x)
                        dragFraction = fraction
                        onSeekProgress(fraction)
                    },
                    onDragEnd = {
                        isInteracting = false
                        onSeekFinished()
                    },
                    onDragCancel = {
                        isInteracting = false
                        onSeekFinished()
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = fractionAt(change.position.x)
                        dragFraction = fraction
                        onSeekProgress(fraction)
                    },
                )
            },
    ) {
        LinearWavyProgressIndicator(
            progress = { displayedFraction },
            color = activeColor,
            trackColor = trackColor,
            amplitude = { p -> if (p > 0f) targetAmplitude else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
        )

        // Morphing thumb: Ø14dp circle to 6×22dp tall pill
        val thumbWidthDp = (14f + (6f - 14f) * thumbMorph).dp
        val thumbHeightDp = (14f + (22f - 14f) * thumbMorph).dp

        Box(
            modifier = Modifier
                .offset {
                    val x = (widthPx * displayedFraction - thumbWidthDp.toPx() / 2f).roundToInt()
                    IntOffset(x.coerceIn(0, (widthPx - thumbWidthDp.toPx()).roundToInt().coerceAtLeast(0)), 0)
                }
                .align(Alignment.CenterStart)
                .size(thumbWidthDp, thumbHeightDp)
                .clip(RoundedCornerShape(percent = 50))
                .background(thumbColor),
        )
    }
}
