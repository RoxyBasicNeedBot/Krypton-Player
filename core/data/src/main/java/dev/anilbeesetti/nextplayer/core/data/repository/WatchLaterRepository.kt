package dev.anilbeesetti.nextplayer.core.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager
import dev.anilbeesetti.nextplayer.core.database.dao.WatchLaterDao
import dev.anilbeesetti.nextplayer.core.database.entities.WatchLaterEntity
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.model.WatchLaterItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchLaterRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val watchLaterDao: WatchLaterDao,
    private val downloadManager: TelegramDownloadManager
) {
    companion object {
        private const val TAG = "WatchLaterRepository"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val queueFlow: Flow<List<WatchLaterItem>> = watchLaterDao.getAllQueueFlow().map { list ->
        list.map { it.asExternalModel() }
    }

    suspend fun addToQueue(
        fileId: String,
        chatId: Long,
        messageId: Long,
        title: String,
        subtitle: String = "",
        fileSize: Long = 0,
        durationSeconds: Long = 0,
        streamUrl: String,
        artworkUri: String? = null
    ): Long {
        val existing = watchLaterDao.getByFileId(fileId)
        if (existing != null) {
            Log.i(TAG, "Item already in watch later queue: $title")
            return existing.id
        }

        val entity = WatchLaterEntity(
            fileId = fileId,
            chatId = chatId,
            messageId = messageId,
            title = title,
            subtitle = subtitle,
            fileSize = fileSize,
            durationSeconds = durationSeconds,
            streamUrl = streamUrl,
            artworkUri = artworkUri,
            status = "PENDING",
            progress = 0f,
            addedAt = System.currentTimeMillis()
        )
        val id = watchLaterDao.insert(entity)
        Log.i(TAG, "[WATCH_LATER] Added to Watch Later Queue: $title (ID: $id)")
        return id
    }

    suspend fun removeFromQueue(id: Long) {
        watchLaterDao.delete(id)
    }

    suspend fun clearCompleted() {
        watchLaterDao.clearCompleted()
    }

    fun startDownload(item: WatchLaterItem) {
        scope.launch {
            try {
                watchLaterDao.updateStatus(item.id, "DOWNLOADING", 0f)

                val video = Video(
                    id = item.id,
                    path = "",
                    parentPath = "",
                    duration = item.durationSeconds * 1000L,
                    uriString = item.streamUrl,
                    nameWithExtension = item.title,
                    width = 0,
                    height = 0,
                    size = item.fileSize,
                    artworkUri = item.artworkUri,
                    externalId = item.fileId
                )

                downloadManager.downloadToWatch(video)

                // Track progress
                val videoKey = item.fileId
                downloadManager.activeDownloadTasks.collect { tasks ->
                    val task = tasks[videoKey]
                    if (task != null) {
                        val progressFraction = task.downloadedBytes.toFloat() / task.totalBytes.coerceAtLeast(1L).toFloat()
                        watchLaterDao.updateStatus(item.id, "DOWNLOADING", progressFraction.coerceIn(0f, 1f))
                    } else if (downloadManager.downloadedKeys.value.contains(videoKey)) {
                        watchLaterDao.markCompleted(
                            id = item.id,
                            status = "COMPLETED",
                            localPath = "offline_media",
                            downloadedAt = System.currentTimeMillis()
                        )
                        return@collect
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download queue item: ${item.title}", e)
                watchLaterDao.updateStatus(item.id, "FAILED", 0f)
            }
        }
    }

    fun downloadAllPending() {
        scope.launch {
            val pending = watchLaterDao.getPendingItems()
            pending.forEach { entity ->
                startDownload(entity.asExternalModel())
            }
        }
    }
}

fun WatchLaterEntity.asExternalModel(): WatchLaterItem = WatchLaterItem(
    id = id,
    fileId = fileId,
    chatId = chatId,
    messageId = messageId,
    title = title,
    subtitle = subtitle,
    fileSize = fileSize,
    durationSeconds = durationSeconds,
    streamUrl = streamUrl,
    artworkUri = artworkUri,
    status = status,
    progress = progress,
    addedAt = addedAt,
    downloadedAt = downloadedAt,
    localFilePath = localFilePath
)
