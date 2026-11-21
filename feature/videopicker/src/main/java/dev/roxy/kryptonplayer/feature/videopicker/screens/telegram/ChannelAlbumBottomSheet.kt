package dev.roxy.kryptonplayer.feature.videopicker.screens.telegram
import androidx.compose.material3.Icon

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.data.network.models.ChannelPostDto
import dev.roxy.kryptonplayer.core.data.network.models.MediaItemDto
import dev.roxy.kryptonplayer.core.model.Video
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelAlbumBottomSheet(
    post: ChannelPostDto,
    onDismissRequest: () -> Unit,
    onPlayVideo: (Uri, String) -> Unit,
    onImageClick: (List<Video>, Int) -> Unit,
    onDownloadToWatch: (Video) -> Unit = {},
    onSaveToGallery: (Video) -> Unit = {},
    downloadProgressMap: Map<String, Float> = emptyMap(),
    onPauseDownload: ((String) -> Unit)? = null,
    onResumeDownload: ((String) -> Unit)? = null,
    onCancelDownload: ((String) -> Unit)? = null,
    isDownloadPaused: ((String) -> Boolean)? = null
) {
    val items = post.mediaItems ?: return
    val context = LocalContext.current
    
    val albumVideos = items.mapIndexed { index, item ->
        val fallbackUrl = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(item.fileId)
        val msgId = item.messageId ?: post.messageId
        val uri = if (item.type == "video") {
            val encodedFallback = java.net.URLEncoder.encode(fallbackUrl, "UTF-8")
            val safeName = try { java.net.URLEncoder.encode(item.fileName ?: "video.mp4", "UTF-8") } catch (_: Exception) { "video.mp4" }
            "tdlib://stream?chatId=${post.channelId}&messageId=$msgId&fileId=${item.fileId}&size=${item.fileSize ?: 0L}&name=$safeName&fallback=$encodedFallback"
        } else {
            fallbackUrl
        }
        Video(
            id = item.fileId.hashCode().toLong(),
            uriString = uri,
            nameWithExtension = item.fileName ?: "album_item_${index}.${if (item.type == "video") "mp4" else "jpg"}",
            size = item.fileSize ?: 0L,
            duration = item.duration?.toLong() ?: 0L,
            path = fallbackUrl,
            parentPath = "Channel Files",
            width = item.width ?: 0,
            height = item.height ?: 0,
            externalId = item.fileId,
            isImage = item.type == "photo"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Album (${items.size} items)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            albumVideos.forEach { onDownloadToWatch(it) }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download All", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            albumVideos.forEach { onSaveToGallery(it) }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save All", fontSize = 12.sp)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(items) { index, item ->
                    val video = albumVideos.getOrNull(index)
                    val videoKey = video?.externalId ?: video?.id?.toString() ?: item.fileId
                    val progress = downloadProgressMap[videoKey]
                        ?: downloadProgressMap[item.fileId]
                        ?: downloadProgressMap[video?.nameWithExtension ?: ""]
                    val isPaused = isDownloadPaused?.invoke(videoKey) ?: false
                    AlbumGridItem(
                        item = item,
                        index = index,
                        postChannelId = post.channelId,
                        postMessageId = post.messageId,
                        albumVideos = albumVideos,
                        video = video,
                        downloadProgress = progress,
                        isPaused = isPaused,
                        onPlayVideo = onPlayVideo,
                        onImageClick = onImageClick,
                        onDownloadToWatch = onDownloadToWatch,
                        onPauseDownload = onPauseDownload?.let { { it(videoKey) } },
                        onResumeDownload = onResumeDownload?.let { { it(videoKey) } },
                        onCancelDownload = onCancelDownload?.let { { it(videoKey) } },
                        onDismissSheet = onDismissRequest
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumGridItem(
    item: MediaItemDto,
    index: Int,
    postChannelId: Long,
    postMessageId: Int,
    albumVideos: List<Video>,
    video: Video?,
    downloadProgress: Float? = null,
    isPaused: Boolean = false,
    onPlayVideo: (Uri, String) -> Unit,
    onImageClick: (List<Video>, Int) -> Unit,
    onDownloadToWatch: (Video) -> Unit,
    onPauseDownload: (() -> Unit)? = null,
    onResumeDownload: (() -> Unit)? = null,
    onCancelDownload: (() -> Unit)? = null,
    onDismissSheet: () -> Unit
) {
    val context = LocalContext.current
    val fallbackUrl = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(item.fileId)
    val isVideo = item.type == "video"
    val msgId = item.messageId ?: postMessageId
    val streamUrl = if (isVideo) {
        val encodedFallback = java.net.URLEncoder.encode(fallbackUrl, "UTF-8")
        val safeName = try { java.net.URLEncoder.encode(item.fileName ?: "video.mp4", "UTF-8") } catch (_: Exception) { "video.mp4" }
        "tdlib://stream?chatId=$postChannelId&messageId=$msgId&fileId=${item.fileId}&size=${item.fileSize ?: 0L}&name=$safeName&fallback=$encodedFallback"
    } else {
        fallbackUrl
    }
    
    val thumbId = item.thumbFileId
    val imageUrl = if (isVideo && !thumbId.isNullOrEmpty()) {
        dev.roxy.kryptonplayer.core.data.network.NetworkConfig.thumbnailUrl(thumbId)
    } else {
        dev.roxy.kryptonplayer.core.data.network.NetworkConfig.thumbnailUrl(item.fileId)
    }

    val offlineFile by produceState<java.io.File?>(initialValue = null, key1 = video?.nameWithExtension) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val fileName = video?.nameWithExtension ?: "album_item_${index}.jpg"
            val internalFile = java.io.File(context.filesDir, "offline_media/$fileName")
            val externalOffline = context.getExternalFilesDir("offline_media")?.let { java.io.File(it, fileName) }
            val dir = if (isVideo) {
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
            } else {
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
            }
            val primaryPublic = java.io.File(dir, "KryptonPlayer/$fileName")
            val legacyPublic = java.io.File(dir, "ArchPlayer/$fileName")
            val publicFile = if (primaryPublic.exists()) primaryPublic else legacyPublic
            val expectedSize = item.fileSize ?: video?.size ?: -1L
            val minSize = if (isVideo) dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.MIN_VALID_VIDEO_SIZE else 1024L
            val candidate = when {
                internalFile.exists() && internalFile.length() >= minSize -> internalFile
                externalOffline != null && externalOffline.exists() && externalOffline.length() >= minSize -> externalOffline
                publicFile.exists() && publicFile.length() >= minSize -> publicFile
                else -> null
            }
            if (candidate != null && expectedSize > 0L && candidate.length() < (expectedSize * 0.95).toLong()) {
                null
            } else {
                candidate
            }
        }
    }
    val isDownloaded = offlineFile != null
    val playUri = if (offlineFile != null) {
        val fbUrl = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(item.fileId)
        video?.nameWithExtension?.let {
            dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, streamUrl)
            dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, fbUrl)
        }
        item.fileId.let {
            dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, streamUrl)
            dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, fbUrl)
        }
        Uri.fromFile(offlineFile)
    } else streamUrl.toUri()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable {
                onDismissSheet()
                if (isVideo) {
                    onPlayVideo(playUri, item.fileName ?: "video.mp4")
                } else {
                    onImageClick(albumVideos, index)
                }
            }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Album item ${index + 1}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (isVideo) {
            // Play Button Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Duration Badge
            val duration = item.duration
            if (duration != null && duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(duration),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top-End Download / Downloaded Status Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            if (isDownloaded && downloadProgress == null) {
                Icon(
                    painter = painterResource(id = NextIcons.TelegramCloudDone),
                    contentDescription = "Downloaded",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                )
            } else if (downloadProgress != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Pause / Resume Button with Circular Progress
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { if (isPaused) (if (downloadProgress > 0f) downloadProgress else 0.5f) else downloadProgress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 2.dp,
                            color = if (isPaused) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                        IconButton(
                            onClick = {
                                if (isPaused) onResumeDownload?.invoke() else onPauseDownload?.invoke()
                            },
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                tint = if (isPaused) Color(0xFFFF9800) else Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    // Full Cancel 'X' Button
                    IconButton(
                        onClick = { onCancelDownload?.invoke() },
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel Download",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else if (video != null) {
                IconButton(
                    onClick = { onDownloadToWatch(video) },
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationSec: Int): String {
    val h = durationSec / 3600
    val m = (durationSec % 3600) / 60
    val s = durationSec % 60
    return if (h > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(java.util.Locale.US, "%d:%02d", m, s)
    }
}
