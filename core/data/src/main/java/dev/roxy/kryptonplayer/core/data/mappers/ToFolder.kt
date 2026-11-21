package dev.roxy.kryptonplayer.core.data.mappers

import dev.roxy.kryptonplayer.core.common.Utils
import dev.roxy.kryptonplayer.core.database.relations.DirectoryWithMedia
import dev.roxy.kryptonplayer.core.database.relations.MediumWithInfo
import dev.roxy.kryptonplayer.core.model.Folder

fun DirectoryWithMedia.toFolder() = Folder(
    name = directory.name,
    path = directory.path,
    dateModified = directory.modified,
    parentPath = directory.parentPath,
    formattedMediaSize = Utils.formatFileSize(media.sumOf { it.mediumEntity.size }),
    mediaList = media.map(MediumWithInfo::toVideo),
)
