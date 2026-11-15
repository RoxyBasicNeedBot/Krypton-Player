package dev.anilbeesetti.nextplayer.feature.player.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramDelete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.core.net.toUri
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerErrorDiagnosticsDialog(
    error: PlaybackException,
    player: Player,
    onDismiss: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var showDetailedLogs by remember { mutableStateOf(true) }

    val currentMediaItem = player.currentMediaItem
    val currentUri = currentMediaItem?.localConfiguration?.uri ?: Uri.EMPTY
    val uriString = currentUri.toString()
    val mediaTitle = currentMediaItem?.mediaMetadata?.title?.toString()
        ?: currentUri.lastPathSegment
        ?: "Current Video"

    val httpException = generateSequence(error.cause) { it.cause }
        .filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>()
        .firstOrNull()
    val httpResponseCode = httpException?.responseCode ?: 0
    val isHttpExpiredOrBlocked = httpResponseCode in listOf(401, 403, 404, 410, 502, 503, 504)

    var showEnterNewUrlDialog by remember { mutableStateOf(false) }
    var enteredStreamUrl by remember { mutableStateOf(if (isHttpExpiredOrBlocked) uriString else "") }

    val isContentUri = currentUri.scheme == "content"
    val isOfflineMedia = uriString.contains("offline_media") || currentUri.scheme == "file" || isContentUri

    val offlineFileSize = remember(currentUri) {
        if (isContentUri) {
            try {
                context.contentResolver.openFileDescriptor(currentUri, "r")?.use { pfd ->
                    val size = pfd.statSize
                    if (size > 0) "${size} bytes (${size / (1024 * 1024)} MB)" else "Local Device File"
                } ?: "Local Device File"
            } catch (_: Exception) {
                "Local Device File"
            }
        } else {
            val rawPath = currentUri.path
            val decodedPath = try {
                if (rawPath != null) java.net.URLDecoder.decode(rawPath, "UTF-8") else null
            } catch (_: Exception) { rawPath }
            val f = decodedPath?.let { File(it) }?.takeIf { it.exists() }
                ?: rawPath?.let { File(it) }?.takeIf { it.exists() }

            if (isOfflineMedia && f != null && f.exists()) {
                "${f.length()} bytes (${f.length() / (1024 * 1024)} MB)"
            } else if (isOfflineMedia) {
                "File Not Found on Disk"
            } else {
                "N/A (Streaming)"
            }
        }
    }

    val errorCodeName = error.errorCodeName
    val errorCodeNum = error.errorCode
    val errorMessage = error.message ?: "Unknown source error"
    val errorCause = error.cause?.toString() ?: "No root cause throwable"

    val timestamp = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }

    val diagnosticReport = remember(error, currentUri) {
        buildString {
            appendLine("=== KRYPTON PLAYER PLAYBACK DIAGNOSTICS ===")
            appendLine("Timestamp: $timestamp")
            appendLine("Error Code: $errorCodeName ($errorCodeNum)")
            appendLine("Message: $errorMessage")
            appendLine("Title: $mediaTitle")
            appendLine("URI: $uriString")
            appendLine("Is Local File / Cache: $isOfflineMedia")
            appendLine("Local File Size: $offlineFileSize")
            appendLine("Root Cause: $errorCause")
            appendLine("\n--- STACK TRACE ---")
            appendLine(error.stackTraceToString().take(2000))
        }
    }

    AlertDialog(
        onDismissRequest = { },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFF3B30).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Source Playback Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = errorCodeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF3B30)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isHttpExpiredOrBlocked) {
                    val statusReason = when (httpResponseCode) {
                        503 -> "HTTP 503 (Service Unavailable / Token Expired)"
                        403 -> "HTTP 403 (Forbidden / Expired Link)"
                        404 -> "HTTP 404 (Stream Link Not Found)"
                        410 -> "HTTP 410 (Stream Link Expired)"
                        else -> "HTTP $httpResponseCode (Server Error)"
                    }
                    Text(
                        text = "$statusReason: The streaming token in this URL has expired or the remote host is rejecting the connection. Please provide a fresh stream link or retry with browser headers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Player encountered an issue parsing or reading the video source. This usually happens if an offline cached file was incomplete or a stream connection was interrupted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Diagnostics Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Source:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isContentUri) "Local Device Storage" else if (isOfflineMedia) "In-App Cache File" else "Online Stream",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOfflineMedia) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                        }

                        if (isOfflineMedia) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isContentUri) "File Size:" else "Cached Size:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = offlineFileSize,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = "Cause: $errorMessage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2
                        )
                    }
                }

                // Expandable Diagnostics Log Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDetailedLogs = !showDetailedLogs }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Detailed Diagnostics Log",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (showDetailedLogs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = showDetailedLogs) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF141414),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        SelectionContainer {
                            Text(
                                text = diagnosticReport,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                ),
                                color = Color(0xFF00FF66),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }

                // Copy Diagnostic Logs Button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Krypton Player Diagnostics", diagnosticReport)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Diagnostics logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Diagnostic Logs", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isHttpExpiredOrBlocked) {
                    Button(
                        onClick = { showEnterNewUrlDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update / Enter Fresh Stream URL", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    // Primary Action: Retry with Online Stream (Auto-repair)
                    Button(
                        onClick = {
                            // Find fallback online stream URL
                            val fileName = currentUri.lastPathSegment ?: mediaTitle
                            val fileId = currentUri.getQueryParameter("fileId") ?: ""
                            val fallback = currentUri.getQueryParameter("fallback")
                                ?: TelegramDownloadManager.getFallbackUrl(fileName)
                                ?: TelegramDownloadManager.getFallbackUrl(fileId)
                                ?: (if (fileId.isNotEmpty()) dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.streamUrl(fileId) else null)

                            if (!fallback.isNullOrEmpty()) {
                                val newMediaItem = MediaItem.Builder()
                                    .setUri(fallback)
                                    .setMediaId(fallback)
                                    .setMimeType(MimeTypes.VIDEO_MP4)
                                    .setMediaMetadata(currentMediaItem?.mediaMetadata ?: androidx.media3.common.MediaMetadata.EMPTY)
                                    .build()
                                player.setMediaItem(newMediaItem)
                                player.prepare()
                                player.play()
                                Toast.makeText(context, "Switching to live stream...", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                player.prepare()
                                player.play()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry with Live Stream", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Resilient local retry option
                OutlinedButton(
                    onClick = {
                        player.prepare()
                        player.play()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Local Playback", fontSize = 12.sp)
                }

                if (isOfflineMedia) {
                    // Option to Delete In-App Cache
                    FilledTonalButton(
                        onClick = {
                            val rawPath = currentUri.path
                            val decodedPath = try {
                                if (rawPath != null) java.net.URLDecoder.decode(rawPath, "UTF-8") else null
                            } catch (_: Exception) { rawPath }
                            try {
                                decodedPath?.let { File(it) }?.takeIf { it.exists() }?.delete()
                                rawPath?.let { File(it) }?.takeIf { it.exists() }?.delete()
                                Toast.makeText(context, "Corrupt cache cleared", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                            player.prepare()
                            player.play()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        LottieTelegramDelete(size = 18.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Corrupt Cache & Reload", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (player.hasNextMediaItem()) {
                        TextButton(
                            onClick = {
                                onDismiss()
                                player.seekToNext()
                                player.play()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Next")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(
                        onClick = {
                            onDismiss()
                            onBackClick()
                        }
                    ) {
                        Text("Exit Player")
                    }
                }
            }
        }
    )

    if (showEnterNewUrlDialog) {
        AlertDialog(
            onDismissRequest = { showEnterNewUrlDialog = false },
            title = {
                Text(
                    text = "Update Stream URL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "The previous stream link expired or failed. Paste a fresh video stream URL to resume playback:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = enteredStreamUrl,
                        onValueChange = { enteredStreamUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://...", fontSize = 12.sp) },
                        singleLine = false,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUrl = enteredStreamUrl.trim()
                        if (cleanUrl.isNotEmpty()) {
                            try {
                                val newMediaItem = MediaItem.Builder()
                                    .setUri(cleanUrl)
                                    .setMediaId(cleanUrl)
                                    .setMimeType(MimeTypes.VIDEO_MP4)
                                    .setMediaMetadata(
                                        currentMediaItem?.mediaMetadata ?: androidx.media3.common.MediaMetadata.EMPTY
                                    )
                                    .build()
                                player.setMediaItem(newMediaItem)
                                player.prepare()
                                player.play()
                                Toast.makeText(context, "Playing new stream...", Toast.LENGTH_SHORT).show()
                                showEnterNewUrlDialog = false
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Invalid URL: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Play Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnterNewUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
