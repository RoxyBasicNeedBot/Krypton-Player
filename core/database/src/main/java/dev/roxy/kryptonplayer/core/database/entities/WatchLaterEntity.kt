package dev.roxy.kryptonplayer.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_later_queue")
data class WatchLaterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: String,
    val chatId: Long = 0,
    val messageId: Long = 0,
    val title: String,
    val subtitle: String = "",
    val fileSize: Long = 0,
    val durationSeconds: Long = 0,
    val streamUrl: String,
    val artworkUri: String? = null,
    val status: String = "PENDING", // PENDING, DOWNLOADING, COMPLETED, FAILED
    val progress: Float = 0f,
    val addedAt: Long = System.currentTimeMillis(),
    val downloadedAt: Long? = null,
    val localFilePath: String? = null
)
