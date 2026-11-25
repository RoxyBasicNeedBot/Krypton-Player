package dev.roxy.kryptonplayer.feature.player.extensions

import dev.roxy.kryptonplayer.core.common.playback.PlaybackController

fun PlaybackController.seekBack(incrementMs: Long = 10_000L) {
    val target = (state.value.currentPositionMs - incrementMs).coerceAtLeast(0L)
    seekTo(target, isUserSeek = true)
}

fun PlaybackController.seekForward(incrementMs: Long = 10_000L) {
    val dur = state.value.durationMs
    val maxPos = if (dur > 0L) dur else Long.MAX_VALUE
    val target = (state.value.currentPositionMs + incrementMs).coerceAtMost(maxPos)
    seekTo(target, isUserSeek = true)
}
