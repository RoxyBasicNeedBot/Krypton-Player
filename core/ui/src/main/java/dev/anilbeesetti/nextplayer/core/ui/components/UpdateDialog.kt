package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.runtime.Composable

@Composable
fun UpdateDialog(
    versionName: String,
    changelog: String,
    currentVersionName: String = "",
    fileSize: Long = 0L,
    releaseDate: String = "",
    downloadProgress: Int = -1,
    bytesDownloaded: Long = 0L,
    totalBytes: Long = 0L,
    isReadyToInstall: Boolean = false,
    onDownload: () -> Unit,
    onInstall: () -> Unit = {},
    onDismiss: () -> Unit,
    onOpenReleaseUrl: (() -> Unit)? = null,
) {
    AppUpdateDialog(
        versionName = versionName,
        changelog = changelog,
        currentVersionName = currentVersionName,
        fileSize = fileSize,
        releaseDate = releaseDate,
        downloadProgress = downloadProgress,
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
        isReadyToInstall = isReadyToInstall,
        onDownload = onDownload,
        onInstall = onInstall,
        onDismiss = onDismiss,
        onOpenReleaseUrl = onOpenReleaseUrl,
    )
}

@Composable
fun UpdateDialog(
    versionName: String,
    changelog: String,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppUpdateDialog(
        versionName = versionName,
        changelog = changelog,
        currentVersionName = "",
        onDownload = onDownload,
        onDismiss = onDismiss,
    )
}
