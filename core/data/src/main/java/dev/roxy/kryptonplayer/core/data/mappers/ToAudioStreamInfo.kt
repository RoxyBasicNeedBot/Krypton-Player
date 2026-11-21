package dev.roxy.kryptonplayer.core.data.mappers

import dev.roxy.kryptonplayer.core.database.entities.AudioStreamInfoEntity
import dev.roxy.kryptonplayer.core.model.AudioStreamInfo

fun AudioStreamInfoEntity.toAudioStreamInfo() = AudioStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    sampleFormat = sampleFormat,
    sampleRate = sampleRate,
    channels = channels,
    channelLayout = channelLayout,
)
