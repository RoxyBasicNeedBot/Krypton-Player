package dev.roxy.kryptonplayer.feature.player.ui.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.roxy.kryptonplayer.core.model.VideoContentScale
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.extensions.copy
import dev.roxy.kryptonplayer.feature.player.LocalUseMaterialYouControls
import dev.roxy.kryptonplayer.feature.player.buttons.LoopButton
import dev.roxy.kryptonplayer.feature.player.buttons.PlayerButton
import dev.roxy.kryptonplayer.core.ui.components.LottieHeartLikeButton
import dev.roxy.kryptonplayer.feature.player.buttons.ShuffleButton
import dev.roxy.kryptonplayer.feature.player.extensions.drawableRes
import dev.roxy.kryptonplayer.feature.player.extensions.noRippleClickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextOverflow
import dev.roxy.kryptonplayer.feature.player.state.getAudioBrandIconRes
import kotlinx.coroutines.delay
import dev.roxy.kryptonplayer.feature.player.state.AudioDeviceType
import dev.roxy.kryptonplayer.feature.player.state.AudioOutputDevice
import dev.roxy.kryptonplayer.feature.player.state.MediaPresentationState
import dev.roxy.kryptonplayer.feature.player.state.SeekbarThumbnailState
import dev.roxy.kryptonplayer.feature.player.state.durationFormatted
import dev.roxy.kryptonplayer.feature.player.state.pendingPositionFormatted
import dev.roxy.kryptonplayer.feature.player.state.positionFormatted
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun ControlsBottomView(
    modifier: Modifier = Modifier,
    player: Player,
    mediaPresentationState: MediaPresentationState,
    controlsAlignment: Alignment.Horizontal,
    videoContentScale: VideoContentScale,
    isPipSupported: Boolean,
    onVideoContentScaleClick: () -> Unit,
    onVideoContentScaleLongClick: () -> Unit,
    onLockControlsClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onRotateClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onUsbCastClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    showLikeButton: Boolean = false,
    isLiked: Boolean = false,
    onToggleLike: () -> Unit = {},
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
    sliderStyle: String = "STANDARD",
    showThumbnails: Boolean = false,
    thumbnailState: SeekbarThumbnailState? = null,
    audioOutputDevice: AudioOutputDevice? = null,
    onAudioOutputClick: () -> Unit = {},
) {

    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    Column(
        modifier = modifier
            .padding(systemBarsPadding.copy(top = 0.dp))
            .padding(horizontal = 12.dp)
            .padding(top = 16.dp)
            .padding(bottom = 16.dp.takeIf { systemBarsPadding.calculateBottomPadding() == 0.dp } ?: 0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Timers & Audio Device Output Row (Above Seekbar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var showPendingPosition by rememberSaveable { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.noRippleClickable {
                    showPendingPosition = !showPendingPosition
                },
            ) {
                Text(
                    text = when (showPendingPosition) {
                        true -> "-${mediaPresentationState.pendingPositionFormatted}"
                        false -> mediaPresentationState.positionFormatted
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                )
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = mediaPresentationState.durationFormatted,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                )
            }

            // Audio Device Output Switcher Button (Just above rotate button and seekbar)
            AudioDeviceOutputButton(
                currentDevice = audioOutputDevice,
                onClick = onAudioOutputClick
            )
        }

        // Seekbar
        PlayerSeekbar(
            position = mediaPresentationState.position.toFloat(),
            duration = mediaPresentationState.duration.toFloat(),
            onSeek = { onSeek(it.toLong()) },
            onSeekFinished = { onSeekEnd() },
            sliderStyle = sliderStyle,
            isPlaying = mediaPresentationState.isPlaying,
            showThumbnails = showThumbnails,
            thumbnailState = thumbnailState,
        )

        // Bottom Actions Row (Left & Right clusters matching Nosved Player 1:1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left Group: Lock, Video Scale, PiP, Headset (Background Play)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlayerButton(onClick = onLockControlsClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_open),
                        contentDescription = "Lock Controls",
                        modifier = Modifier.size(20.dp)
                    )
                }
                PlayerButton(
                    onClick = onVideoContentScaleClick,
                    onLongClick = onVideoContentScaleLongClick,
                ) {
                    Icon(
                        painter = painterResource(videoContentScale.drawableRes()),
                        contentDescription = "Aspect Ratio",
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (isPipSupported) {
                    PlayerButton(onClick = onPictureInPictureClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pip),
                            contentDescription = "Picture in Picture",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                PlayerButton(onClick = onPlayInBackgroundClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_headset),
                        contentDescription = "Play in Background",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Right Group: Up Next Queue, USB Cast, Screen Rotation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlayerButton(onClick = onPlaylistClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = "Up Next Queue",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
                PlayerButton(onClick = onUsbCastClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_usb_cast_custom),
                        contentDescription = "USB Stream to PC",
                        modifier = Modifier.size(20.dp)
                    )
                }
                PlayerButton(onClick = onRotateClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_screen_rotation),
                        contentDescription = "Rotate Screen",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSeekbar(
    modifier: Modifier = Modifier,
    position: Float,
    duration: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    sliderStyle: String,
    isPlaying: Boolean,
    showThumbnails: Boolean = false,
    thumbnailState: SeekbarThumbnailState? = null,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var sliderSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Request thumbnail when scrubbing
    LaunchedEffect(isScrubbing, scrubPosition) {
        if (isScrubbing && showThumbnails && thumbnailState != null && duration > 0f) {
            // Debounce seek requests by 150ms to prevent overwhelming the player decoder while dragging rapidly
            kotlinx.coroutines.delay(150L)
            thumbnailState.requestThumbnail(scrubPosition.toLong())
        }
    }

    Box(modifier = modifier) {
        // The actual slider — rendered first so sliderSize is measured
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { sliderSize = it.size }
                ) {
                val wrappedOnValueChange: (Float) -> Unit = { value ->
                    isScrubbing = true
                    scrubPosition = value
                    onSeek(value)
                }
                val wrappedOnFinished: () -> Unit = {
                    isScrubbing = false
                    thumbnailState?.clearThumbnail()
                    onSeekFinished()
                }

                when (sliderStyle.uppercase()) {
                    "WAVY" -> {
                        WavySlider(
                            modifier = Modifier.fillMaxWidth(),
                            value = position,
                            valueRange = 0f..duration,
                            onValueChange = wrappedOnValueChange,
                            onValueChangeFinished = wrappedOnFinished,
                            isPlaying = isPlaying,
                        )
                    }
                    "THICK" -> {
                        ThickSlider(
                            modifier = Modifier.fillMaxWidth(),
                            value = position,
                            valueRange = 0f..duration,
                            onValueChange = wrappedOnValueChange,
                            onValueChangeFinished = wrappedOnFinished,
                        )
                    }
                    "SIMPLE" -> {
                        SimpleSlider(
                            modifier = Modifier.fillMaxWidth(),
                            value = position,
                            valueRange = 0f..duration,
                            onValueChange = wrappedOnValueChange,
                            onValueChangeFinished = wrappedOnFinished,
                        )
                    }
                    else -> {
                        if (LocalUseMaterialYouControls.current) {
                            MaterialYouSlider(
                                modifier = Modifier.fillMaxWidth(),
                                value = position,
                                valueRange = 0f..duration,
                                onValueChange = wrappedOnValueChange,
                                onValueChangeFinished = wrappedOnFinished,
                            )
                        } else {
                            SimpleSlider(
                                modifier = Modifier.fillMaxWidth(),
                                value = position,
                                valueRange = 0f..duration,
                                onValueChange = wrappedOnValueChange,
                                onValueChangeFinished = wrappedOnFinished,
                            )
                        }
                    }
                }
                }
            }
        }

        // Thumbnail popup overlay — rendered AFTER slider so sliderSize is available
        if (showThumbnails && thumbnailState != null && isScrubbing && thumbnailState.thumbnail != null && sliderSize.width > 0) {
            val thumbFraction = if (duration > 0f) (scrubPosition / duration).coerceIn(0f, 1f) else 0f
            val thumbPopupWidthDp = 160.dp
            val thumbPopupWidthPx = with(density) { thumbPopupWidthDp.toPx() }
            val paddingOffsetPx = with(density) { 8.dp.toPx() }
            val offsetX = (paddingOffsetPx + sliderSize.width * thumbFraction - thumbPopupWidthPx / 2f)
                .coerceIn(0f, (paddingOffsetPx * 2f + sliderSize.width - thumbPopupWidthPx).coerceAtLeast(0f))

            // Zero-size Box container to prevent expanding layout bounds and shifting the seek bar
            Box(
                modifier = Modifier
                    .size(0.dp)
                    .offset { IntOffset(offsetX.toInt(), with(density) { (-120).dp.roundToPx() }) }
            ) {
                Box(
                    modifier = Modifier.width(thumbPopupWidthDp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .background(
                                Color.Black.copy(alpha = 0.85f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        thumbnailState.thumbnail?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .width(152.dp)
                                    .height(86.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        val timeMs = scrubPosition.toLong()
                        val totalSec = timeMs / 1000
                        val h = totalSec / 3600
                        val m = (totalSec % 3600) / 60
                        val s = totalSec % 60
                        val timeText = if (h > 0) String.format("%d:%02d:%02d", h, m, s)
                                       else String.format("%d:%02d", m, s)
                        Text(
                            text = timeText,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialYouSlider(
    modifier: Modifier = Modifier,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val trackHeight = 8.dp
    val thumbWidth = 4.dp
    val trackThumbGapWidth = 12.dp

    Slider(
        value = value,
        valueRange = valueRange,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        modifier = modifier.size(24.dp),
        track = { sliderState ->
            val disabledAlpha = 0.4f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight),
            ) {
                val min = sliderState.valueRange.start
                val max = sliderState.valueRange.endInclusive
                val range = (max - min).takeIf { it > 0f } ?: 1f
                val playedFraction = ((sliderState.value - min) / range).coerceIn(0f, 1f)
                val playedPixels = size.width * playedFraction

                val endCornerRadius = size.height / 2f
                val insideCornerRadius = 2.dp.toPx()
                val gapHalf = trackThumbGapWidth.toPx() / 2f
                val leftEnd = (playedPixels - gapHalf).coerceIn(0f, size.width)
                val rightStart = (playedPixels + gapHalf).coerceIn(0f, size.width)

                // Inactive track left side
                if (leftEnd > 0f) {
                    drawRoundedRect(
                        offset = Offset(0f, 0f),
                        size = Size(leftEnd, size.height),
                        color = primaryColor.copy(alpha = disabledAlpha),
                        startCornerRadius = endCornerRadius,
                        endCornerRadius = insideCornerRadius,
                    )
                }

                // Inactive track right side
                if (rightStart < size.width) {
                    drawRoundedRect(
                        offset = Offset(rightStart, 0f),
                        size = Size(size.width - rightStart, size.height),
                        color = primaryColor.copy(alpha = disabledAlpha),
                        startCornerRadius = insideCornerRadius,
                        endCornerRadius = endCornerRadius,
                    )
                }

                // Active track
                if (leftEnd > 0f) {
                    drawRoundedRect(
                        offset = Offset(0f, 0f),
                        size = Size(leftEnd, size.height),
                        color = primaryColor,
                        startCornerRadius = endCornerRadius,
                        endCornerRadius = insideCornerRadius,
                    )
                }
            }
        },
        thumb = {
            Box(
                modifier = Modifier
                    .width(thumbWidth)
                    .height(20.dp)
                    .background(primaryColor, CircleShape),
            )
        },
    )
}

private fun DrawScope.drawRoundedRect(
    offset: Offset,
    size: Size,
    color: Color,
    startCornerRadius: Float,
    endCornerRadius: Float,
) {
    val startCorner = CornerRadius(startCornerRadius, startCornerRadius)
    val endCorner = CornerRadius(endCornerRadius, endCornerRadius)
    val track = RoundRect(
        rect = Rect(Offset(offset.x, 0f), size = Size(size.width, size.height)),
        topLeft = startCorner,
        topRight = endCorner,
        bottomRight = endCorner,
        bottomLeft = startCorner,
    )
    drawPath(
        path = Path().apply {
            addRoundRect(track)
        },
        color = color,
    )
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleSlider(
    modifier: Modifier = Modifier,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteracting = isDragged || isPressed

    val animatedTrackHeight by animateDpAsState(
        targetValue = if (isInteracting) 6.dp else 3.5.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "mochySeekTrackH"
    )
    val animatedThumbSize by animateDpAsState(
        targetValue = if (isInteracting) 16.dp else 11.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f),
        label = "mochySeekThumbS"
    )

    Slider(
        value = value,
        valueRange = valueRange,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        modifier = modifier.height(30.dp),
        thumb = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(animatedThumbSize)
                        .shadow(if (isInteracting) 6.dp else 2.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .then(
                            if (isInteracting) {
                                Modifier.border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        },
        track = {
            val progressFraction = if (valueRange.endInclusive > 0f) {
                (value / valueRange.endInclusive).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedTrackHeight)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.24f))
            ) {
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(animatedTrackHeight)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    )
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WavySlider(
    modifier: Modifier = Modifier,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    isPlaying: Boolean,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteracting = isDragged || isPressed

    val phaseAnimation = rememberInfiniteTransition(label = "phase")
    val phase by phaseAnimation.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val activePhase = if (isPlaying && !isInteracting) phase else 0f
    val waveAmplitude = if (isPlaying && !isInteracting) 4.dp else 0.dp

    val thumbMorph by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(250),
        label = "thumbMorph"
    )

    Slider(
        value = value,
        valueRange = valueRange,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        modifier = modifier.height(36.dp),
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
            ) {
                val min = sliderState.valueRange.start
                val max = sliderState.valueRange.endInclusive
                val range = (max - min).takeIf { it > 0f } ?: 1f
                val playedFraction = ((sliderState.value - min) / range).coerceIn(0f, 1f)
                val playedPixels = size.width * playedFraction

                val waveLength = 24.dp.toPx()
                val amplitude = waveAmplitude.toPx()
                val centerY = center.y

                val activePath = Path()
                activePath.moveTo(0f, centerY)
                val steps = playedPixels.toInt()
                for (x in 0..steps) {
                    val radians = (x / waveLength) * (2 * Math.PI).toFloat() - activePhase
                    val y = centerY + kotlin.math.sin(radians) * amplitude
                    activePath.lineTo(x.toFloat(), y)
                }

                val inactivePath = Path()
                inactivePath.moveTo(playedPixels, centerY)
                inactivePath.lineTo(size.width, centerY)

                drawPath(
                    path = activePath,
                    color = primaryColor,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawPath(
                    path = inactivePath,
                    color = primaryColor.copy(alpha = 0.3f),
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        },
        thumb = {
            val thumbWidth = (14f + (6f - 14f) * thumbMorph).dp
            val thumbHeight = (14f + (22f - 14f) * thumbMorph).dp
            Box(
                modifier = Modifier
                    .size(thumbWidth, thumbHeight)
                    .shadow(4.dp, RoundedCornerShape(percent = 50))
                    .background(Color.White, RoundedCornerShape(percent = 50))
            )
        }
    )
}


@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThickSlider(
    modifier: Modifier = Modifier,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Slider(
        value = value,
        valueRange = valueRange,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        modifier = modifier.height(24.dp),
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
            ) {
                val min = sliderState.valueRange.start
                val max = sliderState.valueRange.endInclusive
                val range = (max - min).takeIf { it > 0f } ?: 1f
                val playedFraction = ((sliderState.value - min) / range).coerceIn(0f, 1f)
                val playedPixels = size.width * playedFraction

                val trackHeightPx = 8.dp.toPx()
                val centerY = center.y

                // Inactive track
                drawLine(
                    color = primaryColor.copy(alpha = 0.3f),
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round
                )

                // Active track
                if (playedPixels > 0f) {
                    drawLine(
                        color = primaryColor,
                        start = Offset(0f, centerY),
                        end = Offset(playedPixels, centerY),
                        strokeWidth = trackHeightPx,
                        cap = StrokeCap.Round
                    )
                }
            }
        },
        thumb = {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
            )
        }
    )
}

@Composable
fun AudioDeviceBrandIcon(
    device: AudioOutputDevice,
    modifier: Modifier = Modifier
) {
    when (device.type) {
        AudioDeviceType.SPEAKER -> {
            Icon(
                painter = painterResource(R.drawable.ic_audio_speaker_clean),
                contentDescription = "Speaker",
                tint = Color.White,
                modifier = modifier
            )
        }
        AudioDeviceType.EARBUDS_BLUETOOTH -> {
            val iconRes = getAudioBrandIconRes(device.brand)
            Icon(
                painter = painterResource(iconRes),
                contentDescription = device.name,
                tint = Color.Unspecified,
                modifier = modifier
            )
        }
        AudioDeviceType.HEADPHONES_WIRED -> {
            Icon(
                painter = painterResource(R.drawable.ic_headset),
                contentDescription = "Headphones",
                tint = Color(0xFF34D399),
                modifier = modifier
            )
        }
        AudioDeviceType.USB -> {
            Icon(
                painter = painterResource(R.drawable.ic_usb_cast_custom),
                contentDescription = "USB Audio",
                tint = Color(0xFFA78BFA),
                modifier = modifier
            )
        }
        AudioDeviceType.OTHER -> {
            Icon(
                painter = painterResource(R.drawable.ic_audio_speaker_clean),
                contentDescription = "Audio Device",
                tint = Color.White,
                modifier = modifier
            )
        }
    }
}

@Composable
fun AudioDeviceOutputButton(
    modifier: Modifier = Modifier,
    currentDevice: AudioOutputDevice?,
    onClick: () -> Unit
) {
    val device = currentDevice ?: AudioOutputDevice(
        id = "speaker",
        name = "Phone Speaker",
        type = AudioDeviceType.SPEAKER,
        isSelected = true
    )

    var isExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(device.id, device.name) {
        isExpanded = true
        delay(3000L)
        isExpanded = false
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable {
                isExpanded = true
                onClick()
            },
        color = Color(0x38000000),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
        shape = RoundedCornerShape(percent = 50)
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(animationSpec = tween(350, easing = FastOutSlowInEasing))
                .padding(horizontal = if (isExpanded) 10.dp else 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AudioDeviceBrandIcon(
                device = device,
                modifier = Modifier.size(17.dp)
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(250)) + expandHorizontally(tween(350)),
                exit = fadeOut(tween(200)) + shrinkHorizontally(tween(350))
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
