package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaUri: String,
    val title: String,
    val positionMs: Long,
    val formattedTime: String,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
