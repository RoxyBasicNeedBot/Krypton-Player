package dev.roxy.kryptonplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.roxy.kryptonplayer.core.database.entities.DirectoryEntity
import dev.roxy.kryptonplayer.core.database.relations.DirectoryWithMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectoryDao {

    @Upsert
    suspend fun upsert(directory: DirectoryEntity)

    @Upsert
    suspend fun upsertAll(directories: List<DirectoryEntity>)

    @Query("SELECT * FROM directories")
    fun getAll(): Flow<List<DirectoryEntity>>

    @Transaction
    @Query("SELECT * FROM directories")
    fun getAllWithMedia(): Flow<List<DirectoryWithMedia>>

    @Query("DELETE FROM directories WHERE path in (:paths)")
    suspend fun delete(paths: List<String>)
}
