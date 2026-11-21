package dev.roxy.kryptonplayer.core.data.mappers

import dev.roxy.kryptonplayer.core.common.Utils
import dev.roxy.kryptonplayer.core.database.entities.AudioStreamInfoEntity
import dev.roxy.kryptonplayer.core.database.entities.SubtitleStreamInfoEntity
import dev.roxy.kryptonplayer.core.database.relations.MediumWithInfo
import dev.roxy.kryptonplayer.core.model.Video
import java.io.File
import java.util.Date

fun MediumWithInfo.toVideo(): Video {
    val resolvedSize = if (mediumEntity.size > 0L) {
        mediumEntity.size
    } else {
        try {
            val f = File(mediumEntity.path)
            if (f.exists()) f.length() else mediumEntity.size
        } catch (e: Exception) {
            mediumEntity.size
        }
    }

    return Video(
        id = mediumEntity.mediaStoreId,
        path = mediumEntity.path,
        parentPath = mediumEntity.parentPath,
        duration = mediumEntity.duration,
        uriString = mediumEntity.uriString,
        nameWithExtension = mediumEntity.name,
        width = mediumEntity.width,
        height = mediumEntity.height,
        size = resolvedSize,
        dateModified = mediumEntity.modified,
        format = mediumEntity.format,
        playbackPosition = mediumStateEntity?.playbackPosition ?: 0L,
        lastPlayedAt = mediumStateEntity?.lastPlayedTime?.let { Date(it) },
        formattedDuration = Utils.formatDurationMillis(mediumEntity.duration),
        formattedFileSize = Utils.formatFileSize(resolvedSize),
        videoStream = videoStreamInfo?.toVideoStreamInfo(),
        audioStreams = audioStreamsInfo.map(AudioStreamInfoEntity::toAudioStreamInfo),
        subtitleStreams = subtitleStreamsInfo.map(SubtitleStreamInfoEntity::toSubtitleStreamInfo),
        artworkUri = mediumEntity.thumbnailPath,
    )
}
