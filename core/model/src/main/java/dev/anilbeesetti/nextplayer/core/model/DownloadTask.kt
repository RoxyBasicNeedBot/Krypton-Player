package dev.anilbeesetti.nextplayer.core.model

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    // milliseconds
    val duration: Long,
    // bytes
    val fileSize: Long,
    // 0.0 to 1.0
    val progress: Float,
    val status: DownloadStatus,
    val outputPath: String?,
    val extractor: String,
    val createdAt: Long,
    val speed: String? = null,
    val eta: String? = null,
    val progressText: String? = null,
    val downloadedSize: Long = 0L,
    val totalSize: Long = 0L,
    val errorMessage: String? = null,
    val formatId: String? = null,
    val clipStartSec: Int? = null,
    val clipEndSec: Int? = null,
    val splitByChapters: Boolean = false,
    val downloadSubtitles: Boolean = false,
)
