package dev.roxy.kryptonplayer.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoTitle: String,
    val videoAuthor: String,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val videoPath: String,
    val extractor: String,
    val fileSize: Long,
    val duration: Long,
    val downloadedAt: Long,
)
