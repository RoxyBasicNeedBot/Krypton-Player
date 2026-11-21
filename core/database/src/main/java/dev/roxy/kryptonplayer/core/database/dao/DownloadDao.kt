package dev.roxy.kryptonplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.roxy.kryptonplayer.core.database.entities.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getDownloadHistory(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Int): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE videoPath = :path")
    suspend fun getByPath(path: String): DownloadEntity?

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
}
