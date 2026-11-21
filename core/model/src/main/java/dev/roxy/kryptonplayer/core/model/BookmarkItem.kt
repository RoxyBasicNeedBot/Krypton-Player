package dev.roxy.kryptonplayer.core.model

data class BookmarkItem(
    val id: Long = 0,
    val mediaUri: String,
    val title: String,
    val positionMs: Long,
    val formattedTime: String,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
