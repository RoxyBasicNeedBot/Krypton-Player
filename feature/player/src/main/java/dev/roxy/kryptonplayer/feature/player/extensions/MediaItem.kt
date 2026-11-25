package dev.roxy.kryptonplayer.feature.player.extensions

import dev.roxy.kryptonplayer.core.common.playback.PlaybackMediaItem
import dev.roxy.kryptonplayer.core.common.playback.PlaybackTrack

val PlaybackMediaItem.positionMsOrNull: Long?
    get() = positionMs.takeIf { it > 0L }

val PlaybackMediaItem.videoZoom: Float?
    get() = videoScale

fun PlaybackTrack.displayName(index: Int): String = title.ifBlank {
    val langPart = language?.takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
    if (langPart != null) "Track ${index + 1} ($langPart)" else "Track ${index + 1}"
}
