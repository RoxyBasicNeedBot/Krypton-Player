package dev.roxy.kryptonplayer.core.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import dev.roxy.kryptonplayer.core.database.entities.AudioStreamInfoEntity
import dev.roxy.kryptonplayer.core.database.entities.MediumEntity
import dev.roxy.kryptonplayer.core.database.entities.MediumStateEntity
import dev.roxy.kryptonplayer.core.database.entities.SubtitleStreamInfoEntity
import dev.roxy.kryptonplayer.core.database.entities.VideoStreamInfoEntity

data class MediumWithInfo(
    @Embedded val mediumEntity: MediumEntity,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
    )
    val mediumStateEntity: MediumStateEntity?,
    @Relation(
        parentColumn = "uri",
        entityColumn = "medium_uri",
    )
    val videoStreamInfo: VideoStreamInfoEntity?,
    @Relation(
        parentColumn = "uri",
        entityColumn = "medium_uri",
    )
    val audioStreamsInfo: List<AudioStreamInfoEntity>,
    @Relation(
        parentColumn = "uri",
        entityColumn = "medium_uri",
    )
    val subtitleStreamsInfo: List<SubtitleStreamInfoEntity>,
)
