package dev.anilbeesetti.nextplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.anilbeesetti.nextplayer.core.database.entities.WatchLaterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchLaterDao {

    @Query("SELECT * FROM watch_later_queue ORDER BY id DESC")
    fun getAllQueueFlow(): Flow<List<WatchLaterEntity>>

    @Query("SELECT * FROM watch_later_queue WHERE status = 'PENDING' ORDER BY id ASC")
    suspend fun getPendingItems(): List<WatchLaterEntity>

    @Query("SELECT * FROM watch_later_queue WHERE fileId = :fileId LIMIT 1")
    suspend fun getByFileId(fileId: String): WatchLaterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchLaterEntity): Long

    @Update
    suspend fun update(item: WatchLaterEntity)

    @Query("UPDATE watch_later_queue SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, progress: Float)

    @Query("UPDATE watch_later_queue SET status = :status, localFilePath = :localPath, downloadedAt = :downloadedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, status: String, localPath: String, downloadedAt: Long)

    @Query("DELETE FROM watch_later_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM watch_later_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()
}
