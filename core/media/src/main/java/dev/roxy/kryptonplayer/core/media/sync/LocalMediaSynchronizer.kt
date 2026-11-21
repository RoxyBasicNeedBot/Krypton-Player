package dev.roxy.kryptonplayer.core.media.sync

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil3.ImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.roxy.kryptonplayer.core.common.Dispatcher
import dev.roxy.kryptonplayer.core.common.NextDispatchers
import dev.roxy.kryptonplayer.core.common.di.ApplicationScope
import dev.roxy.kryptonplayer.core.common.extensions.VIDEO_COLLECTION_URI
import dev.roxy.kryptonplayer.core.common.extensions.getStorageVolumes
import dev.roxy.kryptonplayer.core.common.extensions.prettyName
import dev.roxy.kryptonplayer.core.common.extensions.scanPaths
import dev.roxy.kryptonplayer.core.common.extensions.scanStorage
import dev.roxy.kryptonplayer.core.database.converter.UriListConverter
import dev.roxy.kryptonplayer.core.database.dao.DirectoryDao
import dev.roxy.kryptonplayer.core.database.dao.MediumDao
import dev.roxy.kryptonplayer.core.database.dao.MediumStateDao
import dev.roxy.kryptonplayer.core.database.entities.DirectoryEntity
import dev.roxy.kryptonplayer.core.database.entities.MediumEntity
import dev.roxy.kryptonplayer.core.media.model.MediaVideo
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalMediaSynchronizer @Inject constructor(
    private val mediumDao: MediumDao,
    private val mediumStateDao: MediumStateDao,
    private val directoryDao: DirectoryDao,
    private val imageLoader: ImageLoader,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    @Dispatcher(NextDispatchers.IO) private val dispatcher: CoroutineDispatcher,
) : MediaSynchronizer {

    private var mediaSyncingJob: Job? = null

    override suspend fun refresh(path: String?): Boolean {
        val scanResult = path?.let { context.scanPaths(listOf(path)) }
            ?: context.getStorageVolumes().all { context.scanStorage(it.path) }
        withContext(dispatcher) {
            rescanAndSync()
        }
        return scanResult
    }

    override fun startSync() {
        if (mediaSyncingJob?.isActive == true) {
            applicationScope.launch(dispatcher) {
                rescanAndSync()
            }
            return
        }
        mediaSyncingJob?.cancel()
        mediaSyncingJob = getMediaVideosFlow().onEach { media ->
            applicationScope.launch { updateDirectories(media) }
            applicationScope.launch { updateMedia(media) }
        }.launchIn(applicationScope)
    }

    private suspend fun rescanAndSync() = withContext(dispatcher) {
        val media = getMediaVideo(null, null, "${MediaStore.Video.Media.DISPLAY_NAME} ASC")
        updateDirectories(media)
        updateMedia(media)
    }

    override fun stopSync() {
        mediaSyncingJob?.cancel()
    }

    private suspend fun updateDirectories(media: List<MediaVideo>) =
        withContext(Dispatchers.Default) {
            val directories = buildDirectoryEntities(
                storageVolumes = context.getStorageVolumes(),
                media = media,
            )
            directoryDao.upsertAll(directories)

            val currentDirectoryPaths = directories.map { it.path }

            val unwantedDirectories = directoryDao.getAll().first()
                .filterNot { it.path in currentDirectoryPaths }

            val unwantedDirectoriesPaths = unwantedDirectories.map { it.path }

            directoryDao.delete(unwantedDirectoriesPaths)
        }

    private fun buildDirectoryEntities(
        storageVolumes: List<File>,
        media: List<MediaVideo>,
    ): List<DirectoryEntity> {
        val normalizedRoots = (storageVolumes.map { it.path } +
            listOfNotNull(Environment.getExternalStorageDirectory()?.path))
            .map { it.trimEnd('/') }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedByDescending { it.length }

        val directoryMap = LinkedHashMap<String, DirectoryEntity>()

        for (video in media) {
            val videoFile = File(video.data)
            val parentDir = videoFile.parentFile ?: continue
            val matchingRootPath = normalizedRoots.firstOrNull { root ->
                video.data.startsWith("$root/")
            }

            var current: File? = parentDir
            while (current != null) {
                val currentPath = current.path.trimEnd('/')
                if (currentPath.isEmpty()) break

                val isRoot = matchingRootPath != null && currentPath == matchingRootPath
                val parentPath = if (isRoot) {
                    "/"
                } else {
                    current.parentFile?.path?.trimEnd('/')?.ifEmpty { "/" } ?: "/"
                }

                if (!directoryMap.containsKey(currentPath)) {
                    directoryMap[currentPath] = DirectoryEntity(
                        path = currentPath,
                        name = current.prettyName,
                        modified = current.lastModified().takeIf { it > 0L } ?: video.dateModified,
                        parentPath = parentPath,
                    )
                }

                if (isRoot || parentPath == "/") break
                current = current.parentFile
            }
        }

        return directoryMap.values.toList()
    }

    private suspend fun updateMedia(media: List<MediaVideo>) = withContext(Dispatchers.Default) {
        val mediumEntities = media.map {
            val file = File(it.data)
            val mediumEntity = mediumDao.get(it.uri.toString())
            val resolvedSize = if (it.size > 0L) it.size else if (file.exists()) file.length() else it.size
            val resolvedDuration = if (it.duration > 0L) it.duration else (mediumEntity?.duration ?: 0L)
            val parentPath = file.parent?.trimEnd('/')?.ifEmpty { "/" } ?: "/"
            mediumEntity?.copy(
                path = file.path,
                name = file.name,
                size = resolvedSize,
                width = it.width,
                height = it.height,
                duration = resolvedDuration,
                mediaStoreId = it.id,
                modified = it.dateModified,
                parentPath = parentPath,
            ) ?: MediumEntity(
                uriString = it.uri.toString(),
                path = it.data,
                name = file.name,
                parentPath = parentPath,
                modified = it.dateModified,
                size = resolvedSize,
                width = it.width,
                height = it.height,
                duration = it.duration,
                mediaStoreId = it.id,
            )
        }

        mediumDao.upsertAll(mediumEntities)

        val currentMediaUris = mediumEntities.map { it.uriString }

        val unwantedMedia = mediumDao.getAllWithInfo().first()
            .filterNot { it.mediumEntity.uriString in currentMediaUris }

        val unwantedMediaUris = unwantedMedia.map { it.mediumEntity.uriString }

        mediumDao.delete(unwantedMediaUris)
        mediumStateDao.delete(unwantedMediaUris)

        // Delete unwanted thumbnails
        unwantedMedia.forEach { media ->
            try {
                imageLoader.diskCache?.remove(media.mediumEntity.uriString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Release external subtitle uri permission if not used by any other media
        launch {
            val currentMediaExternalSubs = mediumEntities.flatMap {
                val mediaState = mediumStateDao.get(it.uriString) ?: return@flatMap emptyList<String>()
                UriListConverter.fromStringToList(mediaState.externalSubs)
            }.toSet()

            unwantedMedia.onEach { mediumWithInfo ->
                val mediumState = mediumWithInfo.mediumStateEntity ?: return@onEach
                for (sub in UriListConverter.fromStringToList(mediumState.externalSubs)) {
                    if (sub !in currentMediaExternalSubs) {
                        try {
                            context.contentResolver.releasePersistableUriPermission(sub, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun getMediaVideosFlow(
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = "${MediaStore.Video.Media.DISPLAY_NAME} ASC",
    ): Flow<List<MediaVideo>> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(getMediaVideo(selection, selectionArgs, sortOrder))
            }
        }
        context.contentResolver.registerContentObserver(VIDEO_COLLECTION_URI, true, observer)
        // initial value
        trySend(getMediaVideo(selection, selectionArgs, sortOrder))
        // close
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.flowOn(dispatcher).distinctUntilChanged()

    private fun getMediaVideo(
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): List<MediaVideo> {
        val mediaVideos = mutableListOf<MediaVideo>()
        context.contentResolver.query(
            VIDEO_COLLECTION_URI,
            VIDEO_PROJECTION,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID)
            val dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
            val displayNameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val relPathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val rawData = if (dataColumn != -1) cursor.getString(dataColumn) else null
                val displayName = if (displayNameColumn != -1) cursor.getString(displayNameColumn) else null
                val relativePath = if (relPathColumn != -1) cursor.getString(relPathColumn) else null

                val data = if (!rawData.isNullOrBlank()) {
                    rawData
                } else {
                    val fallbackName = displayName?.takeIf { it.isNotBlank() } ?: "video_$id.mp4"
                    val rel = relativePath?.trim('/') ?: ""
                    val primaryStorage = Environment.getExternalStorageDirectory()?.path ?: "/storage/emulated/0"
                    if (rel.isNotEmpty()) "$primaryStorage/$rel/$fallbackName" else "$primaryStorage/$fallbackName"
                }

                val rawSize = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                val resolvedSize = if (rawSize > 0L) {
                    rawSize
                } else {
                    try {
                        val f = File(data)
                        if (f.exists()) f.length() else rawSize
                    } catch (e: Exception) {
                        rawSize
                    }
                }
                mediaVideos.add(
                    MediaVideo(
                        id = id,
                        data = data,
                        duration = cursor.getLong(durationColumn),
                        uri = ContentUris.withAppendedId(VIDEO_COLLECTION_URI, id),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn),
                        size = resolvedSize,
                        dateModified = cursor.getLong(dateModifiedColumn),
                    ),
                )
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaVideos.filter { it.data.isNotBlank() }
        } else {
            mediaVideos.filter {
                it.data.isNotBlank() && try { File(it.data).exists() } catch (_: Exception) { true }
            }
        }
    }

    companion object {
        val VIDEO_PROJECTION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH,
            )
        } else {
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DISPLAY_NAME,
            )
        }
    }
}
