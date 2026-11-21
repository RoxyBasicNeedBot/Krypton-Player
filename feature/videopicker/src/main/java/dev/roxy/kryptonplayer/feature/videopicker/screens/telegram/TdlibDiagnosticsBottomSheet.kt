package dev.roxy.kryptonplayer.feature.videopicker.screens.telegram

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import dev.roxy.kryptonplayer.core.ui.components.LottieTelegramDelete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.data.tdlib.TdlibLogEntry
import dev.roxy.kryptonplayer.core.data.tdlib.TdlibManager
import dev.roxy.kryptonplayer.core.data.tdlib.TdlibStreamingStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TdlibDiagnosticsBottomSheet(
    viewModel: TelegramViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.tdlibAuthState.collectAsState()
    val logs by viewModel.tdlibLiveLogs.collectAsState()
    val streamingStats by viewModel.tdlibStreamingStats.collectAsState()
    val preferences by viewModel.playerPreferences.collectAsState()
    val logListState = rememberLazyListState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBox by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, selectedFilter, searchQuery) {
        logs.filter { entry ->
            val matchesFilter = when (selectedFilter) {
                "STREAM" -> entry.level == "STREAM" || entry.tag == "STREAM"
                "MTPROTO" -> entry.level == "SUCCESS" || entry.tag == "MTPROTO"
                "ERROR" -> entry.level == "ERROR" || entry.level == "WARN"
                "INFO" -> entry.level == "INFO"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                entry.message.contains(searchQuery, ignoreCase = true) ||
                entry.level.contains(searchQuery, ignoreCase = true) ||
                entry.tag.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            logListState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    val statusColor by animateColorAsState(
        targetValue = when (authState) {
            is TdlibManager.AuthState.Ready -> Color(0xFF00E676) // Bright Neon Green
            is TdlibManager.AuthState.Initializing -> Color(0xFFFFA000) // Amber
            is TdlibManager.AuthState.Error, is TdlibManager.AuthState.Idle -> Color(0xFFFF5252) // Red
        },
        label = "statusColor"
    )

    val statusTitle = when (authState) {
        is TdlibManager.AuthState.Ready -> "TDLib MTProto 2.0 (Active & Connected)"
        is TdlibManager.AuthState.Initializing -> "TDLib Initializing MTProto Session..."
        is TdlibManager.AuthState.Error -> "TDLib Connection Error"
        is TdlibManager.AuthState.Idle -> "TDLib Standby / Disconnected"
    }

    val statusDescription = when (authState) {
        is TdlibManager.AuthState.Ready -> "Direct peer streaming & chunk downloads from Telegram Data Centers (0 Server Bandwidth, High Speed)."
        is TdlibManager.AuthState.Initializing -> "Authenticating Bot session with MTProto Telegram servers..."
        is TdlibManager.AuthState.Error -> (authState as TdlibManager.AuthState.Error).message
        is TdlibManager.AuthState.Idle -> "Session not started. Check Bot Token credentials."
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==================== 1. HEADER ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "TDLib MTProto Monitor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Direct Telegram Peer-to-Peer Engine",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedPulseDot(color = statusColor)
                        Text(
                            text = when (authState) {
                                is TdlibManager.AuthState.Ready -> "CONNECTED"
                                is TdlibManager.AuthState.Initializing -> "CONNECTING"
                                is TdlibManager.AuthState.Error -> "ERROR"
                                is TdlibManager.AuthState.Idle -> "IDLE"
                            },
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // ==================== 2. ACTIVE LIVE STREAMING / FETCHING MONITOR CARD ====================
            if (streamingStats != null) {
                val stats = streamingStats!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A24)),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "LIVE VIDEO STREAMING ACTIVE",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (stats.isDirectMtproto) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFFFFA000).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (stats.isDirectMtproto) "MTProto 2.0" else "HTTP Proxy",
                                    color = if (stats.isDirectMtproto) Color(0xFF00E676) else Color(0xFFFFA000),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = stats.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val progressFraction = if (stats.totalBytes > 0) {
                            (stats.downloadedBytes.toFloat() / stats.totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF00E5FF),
                                trackColor = Color(0xFF1E2D3D)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${formatBytes(stats.downloadedBytes)} / ${if (stats.totalBytes > 0) formatBytes(stats.totalBytes) else "Streaming"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${(progressFraction * 100).toInt()}% • ${stats.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "0 MB Server Bandwidth (Peer-to-Peer)",
                                    color = Color(0xFF81C784),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (stats.chatId != 0L && stats.messageId != 0L) {
                                Text(
                                    text = "Chat #${stats.chatId} • Msg #${stats.messageId}",
                                    color = Color(0xFF78909C),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            } else {
                // Standby Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (authState) {
                                    is TdlibManager.AuthState.Ready -> Icons.Default.CheckCircle
                                    is TdlibManager.AuthState.Initializing -> Icons.Default.HourglassTop
                                    else -> Icons.Default.ErrorOutline
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = statusTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = statusDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Protocol", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Direct MTProto 2.0", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Bot Gateway", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (preferences.telegramBotToken.isNotEmpty()) "Configured" else "Auto-Link", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Cache Dir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("tdlib_stream/", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ==================== 3. LIVE DIAGNOSTIC LOGS SECTION ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "LIVE DIAGNOSTIC LOGS (${filteredLogs.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showSearchBox = !showSearchBox },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search Logs", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            val allLogsText = logs.joinToString("\n") { "[${it.timestamp}] [${it.level}] [${it.tag}] ${it.message}" }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("TDLib Diagnostic Logs", allLogsText))
                            Toast.makeText(context, "Logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Logs", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { viewModel.clearTdlibLogs() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        LottieTelegramDelete(
                            size = 18.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Bar (if opened)
            AnimatedVisibility(visible = showSearchBox) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter logs by text / fileId / messageId...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Filter Chips Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("ALL", "All", null),
                    Triple("STREAM", "Streams", Icons.Filled.Movie),
                    Triple("MTPROTO", "MTProto", Icons.Filled.Bolt),
                    Triple("INFO", "Info", Icons.Filled.Info),
                    Triple("ERROR", "Errors", Icons.Filled.ErrorOutline)
                ).forEach { (key, label, icon) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        leadingIcon = if (icon != null) {
                            {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        } else null,
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // High-Tech Terminal Console Log Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF090D12)),
                border = BorderStroke(1.dp, Color(0xFF1E2836))
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No logs matching '$searchQuery'" else "Waiting for TDLib events...",
                            color = Color(0xFF616161),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = logListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(filteredLogs) { entry ->
                            TdlibLogLine(entry)
                        }
                    }
                }
            }

            // Bottom Reconnect Action Button
            Button(
                onClick = {
                    viewModel.reconnectTdlib()
                    Toast.makeText(context, "Reconnecting TDLib MTProto Engine...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reconnect TDLib MTProto Engine", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TdlibLogLine(entry: TdlibLogEntry) {
    val levelColor = when (entry.level) {
        "SUCCESS" -> Color(0xFF00E676) // Neon Green
        "STREAM" -> Color(0xFFFF4081)  // Magenta
        "FETCH" -> Color(0xFF00E5FF)   // Neon Cyan
        "INFO" -> Color(0xFF64B5F6)    // Light Blue
        "WARN" -> Color(0xFFFFB74D)    // Amber
        "ERROR" -> Color(0xFFFF5252)   // Coral Red
        else -> Color.LightGray
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = entry.timestamp,
            color = Color(0xFF757575),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = levelColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = "[${entry.level}]",
                color = levelColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        Text(
            text = entry.message,
            color = Color(0xFFE8ECEF),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AnimatedPulseDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups])
}
