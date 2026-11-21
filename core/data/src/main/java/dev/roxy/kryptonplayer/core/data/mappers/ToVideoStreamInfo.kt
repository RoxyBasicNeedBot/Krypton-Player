package dev.roxy.kryptonplayer.core.data.mappers

import dev.roxy.kryptonplayer.core.database.entities.VideoStreamInfoEntity
import dev.roxy.kryptonplayer.core.model.VideoStreamInfo

fun VideoStreamInfoEntity.toVideoStreamInfo() = VideoStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    frameRate = frameRate,
    frameWidth = frameWidth,
    frameHeight = frameHeight,
)
