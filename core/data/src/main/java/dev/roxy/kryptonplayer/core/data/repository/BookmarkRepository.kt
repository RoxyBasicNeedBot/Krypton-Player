package dev.roxy.kryptonplayer.core.data.repository

import dev.roxy.kryptonplayer.core.database.dao.BookmarkDao
import dev.roxy.kryptonplayer.core.database.entities.BookmarkEntity
import dev.roxy.kryptonplayer.core.model.BookmarkItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

fun BookmarkEntity.toDomainModel() = BookmarkItem(
    id = id,
    mediaUri = mediaUri,
    title = title,
    positionMs = positionMs,
    formattedTime = formattedTime,
    thumbnailPath = thumbnailPath,
    createdAt = createdAt
)

fun BookmarkItem.toEntity() = BookmarkEntity(
    id = id,
    mediaUri = mediaUri,
    title = title,
    positionMs = positionMs,
    formattedTime = formattedTime,
    thumbnailPath = thumbnailPath,
    createdAt = createdAt
)

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao
) {

    fun getBookmarks(mediaUri: String): Flow<List<BookmarkItem>> {
        return bookmarkDao.getBookmarksForMedia(mediaUri).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun addBookmark(
        mediaUri: String,
        title: String,
        positionMs: Long,
        formattedTime: String,
        thumbnailPath: String? = null
    ): Long {
        val entity = BookmarkEntity(
            mediaUri = mediaUri,
            title = if (title.isBlank()) "Bookmark @ $formattedTime" else title,
            positionMs = positionMs,
            formattedTime = formattedTime,
            thumbnailPath = thumbnailPath,
            createdAt = System.currentTimeMillis()
        )
        return bookmarkDao.insertBookmark(entity)
    }

    suspend fun deleteBookmark(id: Long) {
        bookmarkDao.deleteBookmarkById(id)
    }

    suspend fun deleteAllBookmarks(mediaUri: String) {
        bookmarkDao.deleteAllBookmarksForMedia(mediaUri)
    }
}
