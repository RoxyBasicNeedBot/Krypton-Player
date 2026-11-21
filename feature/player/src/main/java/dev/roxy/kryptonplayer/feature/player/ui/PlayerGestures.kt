package dev.roxy.kryptonplayer.feature.player.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import dev.roxy.kryptonplayer.feature.player.extensions.detectCustomHorizontalDragGestures
import dev.roxy.kryptonplayer.feature.player.extensions.detectCustomTransformGestures
import dev.roxy.kryptonplayer.feature.player.extensions.detectCustomVerticalDragGestures
import dev.roxy.kryptonplayer.feature.player.state.ControlsVisibilityState
import dev.roxy.kryptonplayer.feature.player.state.PictureInPictureState
import dev.roxy.kryptonplayer.feature.player.state.SeekGestureState
import dev.roxy.kryptonplayer.feature.player.state.TapGestureState
import dev.roxy.kryptonplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.roxy.kryptonplayer.feature.player.state.VolumeAndBrightnessGestureState

@Composable
fun PlayerGestures(
    modifier: Modifier = Modifier,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    pictureInPictureState: PictureInPictureState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    volumeAndBrightnessGestureState: VolumeAndBrightnessGestureState,
) {
    val context = LocalContext.current

    BoxWithConstraints {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(pictureInPictureState.isInPictureInPictureMode) {
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectTapGestures(
                        onTap = { offset ->
                            if (tapGestureState.seekMillis != 0L) {
                                val isLeftTap = offset.x < size.width * 0.45f
                                val isRightTap = offset.x > size.width * 0.55f
                                if ((tapGestureState.seekMillis < 0L && isLeftTap) || (tapGestureState.seekMillis > 0L && isRightTap)) {
                                    tapGestureState.handleDoubleTap(offset = offset, size = size)
                                }
                                return@detectTapGestures
                            }
                            controlsVisibilityState.toggleControlsVisibility()
                        },
                        onDoubleTap = {
                            if (controlsVisibilityState.controlsLocked) return@detectTapGestures
                            tapGestureState.handleDoubleTap(offset = it, size = size)
                        },
                        onPress = {
                            val released = tryAwaitRelease()
                            if (released || !tapGestureState.isLongPressGestureInAction) {
                                tapGestureState.handleOnLongPressRelease()
                            }
                        },
                        onLongPress = {
                            if (controlsVisibilityState.controlsLocked) return@detectTapGestures
                            tapGestureState.handleLongPress(offset = it)
                        },
                    )
                }
                .pointerInput(
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectCustomHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (!tapGestureState.isLongPressGestureInAction) {
                                seekGestureState.onDragStart(offset)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (tapGestureState.isLongPressGestureInAction) {
                                tapGestureState.onHorizontalDrag(dragAmount, context)
                            } else {
                                seekGestureState.onDrag(change, dragAmount)
                            }
                        },
                        onDragCancel = {
                            if (tapGestureState.isLongPressGestureInAction) {
                                tapGestureState.handleOnLongPressRelease()
                            } else {
                                seekGestureState.onDragEnd()
                            }
                        },
                        onDragEnd = {
                            if (tapGestureState.isLongPressGestureInAction) {
                                tapGestureState.handleOnLongPressRelease()
                            } else {
                                seekGestureState.onDragEnd()
                            }
                        },
                    )
                }
                .pointerInput(
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectCustomVerticalDragGestures(
                        onDragStart = {
                            if (!tapGestureState.isLongPressGestureInAction) {
                                volumeAndBrightnessGestureState.onDragStart(it, size)
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (!tapGestureState.isLongPressGestureInAction) {
                                volumeAndBrightnessGestureState.onDrag(change, dragAmount)
                            }
                        },
                        onDragCancel = {
                            if (tapGestureState.isLongPressGestureInAction) {
                                tapGestureState.handleOnLongPressRelease()
                            } else {
                                volumeAndBrightnessGestureState.onDragEnd()
                            }
                        },
                        onDragEnd = {
                            if (tapGestureState.isLongPressGestureInAction) {
                                tapGestureState.handleOnLongPressRelease()
                            } else {
                                volumeAndBrightnessGestureState.onDragEnd()
                            }
                        },
                    )
                }
                .pointerInput(
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectCustomTransformGestures(
                        onGesture = { _, panChange, zoomChange, _ ->
                            if (tapGestureState.isLongPressGestureInAction) return@detectCustomTransformGestures
                            videoZoomAndContentScaleState.onZoomPanGesture(
                                constraints = this@BoxWithConstraints.constraints,
                                panChange = panChange,
                                zoomChange = zoomChange,
                            )
                        },
                        onGestureEnd = {
                            videoZoomAndContentScaleState.onZoomPanGestureEnd()
                        },
                    )
                },
        )
    }
}
