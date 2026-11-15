package dev.anilbeesetti.nextplayer.feature.videopicker.screens.telegram

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.anilbeesetti.nextplayer.core.data.cache.TelegramImageDiskCache
import dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibMediaResolver
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import dev.anilbeesetti.nextplayer.core.data.tdlib.MinithumbnailDecoder
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TelegramAsyncImage(
    thumbFileId: String? = null,
    chatId: Long? = null,
    messageId: Long? = null,
    fallbackUrl: String? = null,
    minithumbnailBytes: ByteArray? = null,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    tdlibMediaResolver: TdlibMediaResolver? = null,
    imageDiskCache: TelegramImageDiskCache? = null
) {
    val context = LocalContext.current
    val msgKey = if (chatId != null && messageId != null) "thumb_${chatId}_$messageId" else null
    val fileKey = if (!thumbFileId.isNullOrEmpty()) "thumb_$thumbFileId" else null

    var localPath by remember(chatId, messageId, thumbFileId) {
        val cached = (msgKey?.let { imageDiskCache?.getCachedFile(it) })
            ?: (fileKey?.let { imageDiskCache?.getCachedFile(it) })
        mutableStateOf(cached?.absolutePath)
    }
    var isLocalFailed by remember(localPath) { mutableStateOf(false) }

    LaunchedEffect(chatId, messageId, thumbFileId) {
        if (localPath != null && !isLocalFailed) return@LaunchedEffect

        // Attempt 1: Direct In-App TDLib MTProto resolution (0 MB Render Bandwidth)
        if (tdlibMediaResolver != null) {
            val resolved = withContext(Dispatchers.IO) {
                tdlibMediaResolver.resolveThumbnail(chatId, messageId, thumbFileId)
            }
            if (resolved != null) {
                val file = File(resolved)
                if (file.exists() && file.length() > 0) {
                    localPath = resolved
                    isLocalFailed = false
                    return@LaunchedEffect
                }
            }
        }
    }

    val effectiveLocalFile = remember(localPath, isLocalFailed) {
        if (!isLocalFailed && !localPath.isNullOrEmpty()) {
            val file = File(localPath!!)
            if (file.exists() && file.length() > 0) file else null
        } else null
    }

    val model: Any? = remember(effectiveLocalFile, thumbFileId, fallbackUrl) {
        when {
            effectiveLocalFile != null -> effectiveLocalFile
            fallbackUrl != null && (fallbackUrl.startsWith("/") || fallbackUrl.startsWith("file://")) -> {
                val file = File(fallbackUrl.removePrefix("file://"))
                if (file.exists() && file.length() > 0) file else null
            }
            thumbFileId != null && (thumbFileId.startsWith("/") || thumbFileId.startsWith("file://")) -> {
                val file = File(thumbFileId.removePrefix("file://"))
                if (file.exists() && file.length() > 0) file else null
            }
            fallbackUrl != null && fallbackUrl.startsWith("data:image/") -> {
                val base64String = fallbackUrl.substringAfter("base64,")
                try {
                    Base64.decode(base64String, Base64.DEFAULT)
                } catch (e: Exception) {
                    fallbackUrl
                }
            }
            !fallbackUrl.isNullOrEmpty() -> fallbackUrl
            !thumbFileId.isNullOrEmpty() -> {
                if (thumbFileId.startsWith("http") || thumbFileId.startsWith("data:image")) {
                    thumbFileId
                } else if (thumbFileId.length > 28 || !thumbFileId.matches(Regex("^[0-9a-fA-F]{24}$"))) {
                    // Valid Telegram file_id (not MongoDB hex ID)
                    dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.thumbnailUrl(thumbFileId)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    val request = remember(model) {
        if (model == null) null
        else {
            val builder = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
            if (model is String && dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.isWorkerUrl(model)) {
                builder.httpHeaders(
                    NetworkHeaders.Builder()
                        .set("X-Arch-Client-Key", dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.CLIENT_KEY)
                        .build()
                )
            }
            builder.build()
        }
    }

    val placeholderBitmap = remember(minithumbnailBytes, msgKey, fileKey) {
        when {
            minithumbnailBytes != null -> MinithumbnailDecoder.decodeToBitmap(minithumbnailBytes, msgKey ?: fileKey)
            msgKey != null -> MinithumbnailDecoder.getCachedBitmap(msgKey)
            fileKey != null -> MinithumbnailDecoder.getCachedBitmap(fileKey)
            else -> null
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (placeholderBitmap != null) {
            Image(
                bitmap = placeholderBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }

        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = {
                    if (effectiveLocalFile != null) {
                        isLocalFailed = true
                    }
                }
            )
        } else if (placeholderBitmap == null) {
            Icon(
                imageVector = NextIcons.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
