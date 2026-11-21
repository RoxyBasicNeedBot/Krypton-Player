package dev.roxy.kryptonplayer.core.data.mappers

import dev.roxy.kryptonplayer.core.data.models.VideoState
import dev.roxy.kryptonplayer.core.database.converter.UriListConverter
import dev.roxy.kryptonplayer.core.database.entities.MediumStateEntity

fun MediumStateEntity.toVideoState(): VideoState {
    return VideoState(
        path = uriString,
        position = playbackPosition.takeIf { it != 0L },
        audioTrackIndex = audioTrackIndex,
        subtitleTrackIndex = subtitleTrackIndex,
        playbackSpeed = playbackSpeed,
        externalSubs = UriListConverter.fromStringToList(externalSubs),
        videoScale = videoScale,
        subtitleDelayMilliseconds = subtitleDelayMilliseconds,
        subtitleSpeed = subtitleSpeed,
    )
}
