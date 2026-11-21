package dev.roxy.kryptonplayer.core.data.mappers

import dev.roxy.kryptonplayer.core.database.entities.SubtitleStreamInfoEntity
import dev.roxy.kryptonplayer.core.model.SubtitleStreamInfo

fun SubtitleStreamInfoEntity.toSubtitleStreamInfo() = SubtitleStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
)
