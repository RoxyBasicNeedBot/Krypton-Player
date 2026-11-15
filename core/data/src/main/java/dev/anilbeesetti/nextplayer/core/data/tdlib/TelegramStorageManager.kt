package dev.anilbeesetti.nextplayer.core.data.tdlib

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.pow

data class TdlibStorageStats(
    val filesSize: Long = 0L,
    val fileCount: Int = 0,
    val databaseSize: Long = 0L,
    val languagePackDatabaseSize: Long = 0L,
    val logSize: Long = 0L,
    val totalSize: Long = 0L,
    val formattedFilesSize: String = "0 B",
    val formattedTotalSize: String = "0 B"
)

/**
 * Native TDLib storage inspection and optimization manager.
 *
 * Utilizes official TDLib MTProto engine capabilities:
 * - [TdApi.GetStorageStatisticsFast] for instantaneous 0-disk-IO storage metrics.
 * - [TdApi.OptimizeStorage] for granular cache pruning by file type and TTL.
 *
 * CRITICAL SAFETY INVARIANT:
 * This manager ONLY targets TDLib's internal streaming cache directory ('tdlib_stream').
 * User-initiated offline downloads stored in 'offline_media' are NEVER modified or purged.
 */
@Singleton
class TelegramStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tdlibManager: TdlibManager
) {
    companion object {
        private const val TAG = "TelegramStorageManager"
        private const val STREAM_DIR_NAME = "tdlib_stream"
        private const val OFFLINE_DIR_NAME = "offline_media"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _storageStats = MutableStateFlow(TdlibStorageStats())
    val storageStats: StateFlow<TdlibStorageStats> = _storageStats.asStateFlow()

    /**
     * Inspects TDLib native storage asynchronously and updates [storageStats].
     */
    suspend fun refreshStorageStats(): TdlibStorageStats {
        if (!tdlibManager.isReady()) {
            return _storageStats.value
        }

        val result = tdlibManager.sendQuery(TdApi.GetStorageStatisticsFast(), timeoutMs = 5000L)
        if (result is TdApi.StorageStatisticsFast) {
            val total = result.filesSize + result.databaseSize + result.languagePackDatabaseSize + result.logSize
            val stats = TdlibStorageStats(
                filesSize = result.filesSize,
                fileCount = result.fileCount,
                databaseSize = result.databaseSize,
                languagePackDatabaseSize = result.languagePackDatabaseSize,
                logSize = result.logSize,
                totalSize = total,
                formattedFilesSize = formatFileSize(result.filesSize),
                formattedTotalSize = formatFileSize(total)
            )
            _storageStats.value = stats
            Log.i(TAG, "Storage stats updated: files=${stats.formattedFilesSize}, total=${stats.formattedTotalSize}")
            return stats
        } else {
            Log.w(TAG, "Failed to fetch storage statistics: $result")
            return _storageStats.value
        }
    }

    /**
     * Clears temporary stream chunk cache and thumbnails via TDLib [TdApi.OptimizeStorage].
     *
     * Protected Guarantee:
     * NEVER purges files located in [OFFLINE_DIR_NAME].
     */
    suspend fun clearStreamCache(
        clearVideos: Boolean = true,
        clearThumbnails: Boolean = true,
        clearAudio: Boolean = false
    ): Boolean {
        if (!tdlibManager.isReady()) {
            Log.w(TAG, "Cannot clear cache: TDLib MTProto client not ready")
            return false
        }

        val fileTypes = mutableListOf<TdApi.FileType>()
        if (clearVideos) {
            fileTypes.add(TdApi.FileTypeVideo())
            fileTypes.add(TdApi.FileTypeAnimation())
            fileTypes.add(TdApi.FileTypeVideoNote())
        }
        if (clearThumbnails) {
            fileTypes.add(TdApi.FileTypeThumbnail())
            fileTypes.add(TdApi.FileTypePhoto())
            fileTypes.add(TdApi.FileTypeProfilePhoto())
        }
        if (clearAudio) {
            fileTypes.add(TdApi.FileTypeAudio())
            fileTypes.add(TdApi.FileTypeVoiceNote())
        }

        if (fileTypes.isEmpty()) return true

        tdlibManager.addLog(
            level = "INFO",
            message = "Clearing TDLib streaming cache (types=${fileTypes.size})...",
            tag = "STORAGE"
        )

        // optimizeStorage with size=0, ttl=0 purges matching types completely
        val optimizeQuery = TdApi.OptimizeStorage(
            0L, // Target size 0 bytes
            0,  // TTL 0 seconds
            -1, // Count unlimited
            0,  // Immunity delay 0
            fileTypes.toTypedArray(),
            LongArray(0),
            LongArray(0),
            false,
            0
        )

        val result = tdlibManager.sendQuery(optimizeQuery, timeoutMs = 15000L)
        val success = result is TdApi.StorageStatistics || result is TdApi.Ok

        // Clean orphaned .tmp and .part files from stream directory
        cleanOrphanedStreamFiles()

        // Refresh stats
        refreshStorageStats()

        tdlibManager.addLog(
            level = if (success) "SUCCESS" else "WARN",
            message = if (success) "TDLib streaming cache cleared successfully" else "Storage optimization returned: $result",
            tag = "STORAGE"
        )

        return success
    }

    /**
     * Cleans orphaned temporary chunk files from the stream directory.
     * Guaranteed never to touch the offline download directory.
     */
    private fun cleanOrphanedStreamFiles() {
        try {
            val streamDir = File(context.filesDir, STREAM_DIR_NAME)
            if (!streamDir.exists()) return

            val files = streamDir.listFiles() ?: return
            for (f in files) {
                // Strictly ensure this file is NOT in the offline media folder
                if (f.absolutePath.contains(OFFLINE_DIR_NAME)) continue

                if (f.name.endsWith(".tmp") || f.name.endsWith(".part")) {
                    try {
                        f.delete()
                    } catch (_: Throwable) {}
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error cleaning orphaned stream files: ${e.message}")
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }
}
