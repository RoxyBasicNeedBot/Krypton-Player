package dev.anilbeesetti.nextplayer.core.model

enum class DownloadStatus {
    QUEUED,
    FETCHING_INFO,
    DOWNLOADING,
    COMPLETED,
    ERROR,
    CANCELED,
    PAUSED,
}
