@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package dev.roxy.kryptonplayer.feature.player.ui.ringtone

import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import dev.roxy.kryptonplayer.core.data.media.AudioRingtoneExtractor
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramBell
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramMusic
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramPhone
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RingtoneMakerBottomSheet(
    sourceUri: Uri,
    currentPositionMs: Long,
    durationMs: Long,
    mediaTitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedDurationSeconds by remember { mutableIntStateOf(30) }
    var isExtracting by remember { mutableStateOf(false) }
    var extractedAudioFile by remember { mutableStateOf<File?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val startMs = (currentPositionMs - (selectedDurationSeconds * 1000L / 2)).coerceAtLeast(0L)
    val endMs = (startMs + selectedDurationSeconds * 1000L).coerceAtMost(if (durationMs > 0) durationMs else Long.MAX_VALUE)

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ringtone),
                            contentDescription = "Ringtone Maker",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Ringtone & Audio Extractor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Extract HQ audio & set as Ringtone / Alarm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = NextIcons.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Time Window Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AUDIO EXTRACTION RANGE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatTime(startMs),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = NextIcons.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = formatTime(endMs),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Lossless HQ Audio (${selectedDurationSeconds}s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Duration Selector Chips
            Text(
                text = "Ringtone Length:",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair(15, "15s Tone"),
                    Pair(30, "30s Ringtone"),
                    Pair(45, "45s Extended"),
                    Pair(60, "60s Full")
                ).forEach { (seconds, label) ->
                    val isSelected = selectedDurationSeconds == seconds
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedDurationSeconds = seconds
                            extractedAudioFile = null
                            mediaPlayer?.stop()
                            isPreviewPlaying = false
                        },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isExtracting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFB300),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Extracting lossless audio...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (extractedAudioFile != null) {
                val file = extractedAudioFile!!
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Preview Button
                    OutlinedButton(
                        onClick = {
                            if (isPreviewPlaying) {
                                mediaPlayer?.stop()
                                isPreviewPlaying = false
                            } else {
                                mediaPlayer?.release()
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(file.absolutePath)
                                    prepare()
                                    setOnCompletionListener {
                                        isPreviewPlaying = false
                                    }
                                    start()
                                }
                                isPreviewPlaying = true
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) NextIcons.Pause else NextIcons.Play,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(if (isPreviewPlaying) "Stop Audio Preview" else "Listen to Extracted Audio")
                        }
                    }

                    // Set as Phone Ringtone
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                                Toast.makeText(context, "Please allow Write System Settings to set Ringtone", Toast.LENGTH_LONG).show()
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                                return@Button
                            }

                            coroutineScope.launch {
                                AudioRingtoneExtractor.saveAndSetRingtone(
                                    context = context,
                                    audioFile = file,
                                    title = dev.roxy.kryptonplayer.core.common.Utils.cleanMediaTitle(mediaTitle).first,
                                    type = RingtoneManager.TYPE_RINGTONE
                                ).onSuccess {
                                    statusMessage = "Successfully set as Phone Ringtone!"
                                    Toast.makeText(context, "Phone Ringtone updated!", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    statusMessage = "Error: ${err.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LottieTelegramPhone(size = 24.dp, tint = Color.Black)
                            Text(
                                text = "Set as Phone Ringtone",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }
                    }

                    // Set as Notification Sound
                    FilledTonalButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                                Toast.makeText(context, "Please allow Write System Settings to set Notification Tone", Toast.LENGTH_LONG).show()
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                                return@FilledTonalButton
                            }

                            coroutineScope.launch {
                                AudioRingtoneExtractor.saveAndSetRingtone(
                                    context = context,
                                    audioFile = file,
                                    title = dev.roxy.kryptonplayer.core.common.Utils.cleanMediaTitle(mediaTitle).first,
                                    type = RingtoneManager.TYPE_NOTIFICATION
                                ).onSuccess {
                                    statusMessage = "Set as Notification Tone!"
                                    Toast.makeText(context, "Notification sound updated!", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    statusMessage = "Error: ${err.message}"
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LottieTelegramBell(size = 24.dp)
                            Text("Set as Notification Sound")
                        }
                    }

                    // Share Audio
                    OutlinedButton(
                        onClick = {
                            shareAudio(context, file)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = NextIcons.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Share Audio File (WhatsApp / Files)")
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        isExtracting = true
                        statusMessage = null
                        coroutineScope.launch {
                            AudioRingtoneExtractor.extractAudio(
                                context = context,
                                sourceUri = sourceUri,
                                startMs = startMs,
                                endMs = endMs,
                                trackTitle = mediaTitle
                            ).onSuccess { file ->
                                isExtracting = false
                                extractedAudioFile = file
                                Toast.makeText(context, "Audio Extracted! (${file.length() / 1024} KB)", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                isExtracting = false
                                statusMessage = err.message ?: "Failed to extract audio"
                                Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LottieTelegramMusic(size = 24.dp)
                        Text(
                            text = "Extract ${selectedDurationSeconds}s HQ Audio",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = statusMessage!!,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

private fun shareAudio(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Ringtone Audio"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
