@file:OptIn(ExperimentalMaterial3Api::class)

package dev.anilbeesetti.nextplayer.feature.videopicker.screens.telegram

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.core.data.cache.CachedMediaItem
import dev.anilbeesetti.nextplayer.core.data.cache.CachedMediaType
import dev.anilbeesetti.nextplayer.core.data.cache.TelegramCacheManager
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramDelete
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageControlBottomSheet(
    viewModel: TelegramViewModel,
    telegramCacheManager: TelegramCacheManager,
    onPlayMedia: ((java.io.File, String) -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences by viewModel.playerPreferences.collectAsState()
    val tdlibStats by viewModel.tdlibStorageStats.collectAsState()

    var videosCacheSize by remember { mutableLongStateOf(0L) }
    var thumbnailsCacheSize by remember { mutableLongStateOf(0L) }
    var audioCacheSize by remember { mutableLongStateOf(0L) }
    var documentsCacheSize by remember { mutableLongStateOf(0L) }
    var metadataCacheSize by remember { mutableLongStateOf(0L) }
    var totalCacheSize by remember { mutableLongStateOf(0L) }
    var deviceFreeSpace by remember { mutableLongStateOf(0L) }
    var deviceTotalSpace by remember { mutableLongStateOf(0L) }
    var cachedItems by remember { mutableStateOf<List<CachedMediaItem>>(emptyList()) }

    var selectedCategoryFilter by remember { mutableStateOf<CachedMediaType?>(null) }
    var showClearSubDialog by remember { mutableStateOf(false) }

    fun refreshSizes() {
        viewModel.refreshTdlibStorageStats()
        coroutineScope.launch(Dispatchers.IO) {
            val items = telegramCacheManager.getAllCachedMediaItems()
            videosCacheSize = telegramCacheManager.getTelegramVideosCacheSize()
            thumbnailsCacheSize = telegramCacheManager.getThumbnailsCacheSize()
            audioCacheSize = telegramCacheManager.getAudioCacheSize()
            documentsCacheSize = telegramCacheManager.getDocumentsCacheSize()
            metadataCacheSize = telegramCacheManager.getMetadataCacheSize()
            totalCacheSize = telegramCacheManager.getTotalCacheSize()
            deviceFreeSpace = telegramCacheManager.getDeviceFreeSpace()
            deviceTotalSpace = telegramCacheManager.getDeviceTotalSpace()
            withContext(Dispatchers.Main) {
                cachedItems = items
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshSizes()
    }

    val keepMediaLabels = listOf("3 days", "1 week", "1 month", "Forever")
    val maxCacheLabels = listOf("2 GB", "5 GB", "10 GB", "50 GB", "No limit")

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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Data and Storage",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )

            // ==================== 1. STORAGE DIAGRAM CARD (NAGRAM STYLE) ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Custom Donut Diagram with all 5 categories
                    StorageSheetDonutDiagram(
                        videosBytes = videosCacheSize,
                        thumbnailsBytes = thumbnailsCacheSize,
                        audioBytes = audioCacheSize,
                        documentsBytes = documentsCacheSize,
                        metadataBytes = metadataCacheSize,
                        totalCacheBytes = totalCacheSize,
                        freeBytes = deviceFreeSpace,
                        totalDeviceBytes = deviceTotalSpace,
                        modifier = Modifier.size(150.dp)
                    )

                    // Storage Legend Breakdown Pills
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StorageLegendPill(color = Color(0xFF2481CC), label = "Videos", size = formatBytes(videosCacheSize))
                            StorageLegendPill(color = Color(0xFF4BB34B), label = "Photos", size = formatBytes(thumbnailsCacheSize))
                            StorageLegendPill(color = Color(0xFF9C27B0), label = "Audio", size = formatBytes(audioCacheSize))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StorageLegendPill(color = Color(0xFFFFA000), label = "Files", size = formatBytes(documentsCacheSize))
                            StorageLegendPill(color = Color(0xFF009688), label = "Channels/Meta", size = formatBytes(metadataCacheSize))
                        }
                    }

                    // Prominent Clear Cache Button
                    Button(
                        onClick = { showClearSubDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (totalCacheSize > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (totalCacheSize > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        LottieTelegramDelete(
                            size = 20.dp,
                            tint = if (totalCacheSize > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (totalCacheSize > 0) "Clear Cache (${formatBytes(totalCacheSize)})" else "Cache is Empty",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ==================== TDLib MTProto Stream Cache Section ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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
                        Column {
                            Text(
                                text = "Stream Cache",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Temporary stream chunk & thumbnail cache",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = tdlibStats.formattedFilesSize,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearTdlibStreamCache(onComplete = { ok ->
                                    Toast.makeText(context, if (ok) "Stream cache cleared" else "Failed to clear cache", Toast.LENGTH_SHORT).show()
                                    refreshSizes()
                                })
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Stream Cache")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = NextIcons.TelegramLockClose),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Downloads in offline_media are 100% protected and will not be deleted by Storage Cleanup",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ==================== 2. DOWNLOADED MEDIA & CACHED FILES (NAGRAM EXPLORER) ====================
            NagramSectionTitle("DOWNLOADED & CACHED MEDIA (${cachedItems.size})")

            // Category Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("All (${cachedItems.size})", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
                val videoCount = cachedItems.count { it.type == CachedMediaType.VIDEO }
                if (videoCount > 0) {
                    FilterChip(
                        selected = selectedCategoryFilter == CachedMediaType.VIDEO,
                        onClick = { selectedCategoryFilter = CachedMediaType.VIDEO },
                        label = { Text("Videos ($videoCount)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                val photoCount = cachedItems.count { it.type == CachedMediaType.PHOTO }
                if (photoCount > 0) {
                    FilterChip(
                        selected = selectedCategoryFilter == CachedMediaType.PHOTO,
                        onClick = { selectedCategoryFilter = CachedMediaType.PHOTO },
                        label = { Text("Photos ($photoCount)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                val audioCount = cachedItems.count { it.type == CachedMediaType.AUDIO }
                if (audioCount > 0) {
                    FilterChip(
                        selected = selectedCategoryFilter == CachedMediaType.AUDIO,
                        onClick = { selectedCategoryFilter = CachedMediaType.AUDIO },
                        label = { Text("Audio ($audioCount)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                val docCount = cachedItems.count { it.type == CachedMediaType.DOCUMENT }
                if (docCount > 0) {
                    FilterChip(
                        selected = selectedCategoryFilter == CachedMediaType.DOCUMENT,
                        onClick = { selectedCategoryFilter = CachedMediaType.DOCUMENT },
                        label = { Text("Files ($docCount)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Cached Files List Card
            val displayedItems = remember(cachedItems, selectedCategoryFilter) {
                if (selectedCategoryFilter == null) cachedItems else cachedItems.filter { it.type == selectedCategoryFilter }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                if (displayedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No downloaded media files found.\nUse 'Download to Watch' on channels or cloud posts to store here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        fun openOrPlayItem(item: CachedMediaItem) {
                            if (item.type == CachedMediaType.VIDEO || item.type == CachedMediaType.AUDIO) {
                                onPlayMedia?.invoke(item.file, item.name)
                            } else {
                                try {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        item.file
                                    )
                                    val mimeType = when (item.type) {
                                        CachedMediaType.PHOTO -> "image/*"
                                        CachedMediaType.AUDIO -> "audio/*"
                                        else -> "*/*"
                                    }
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, mimeType)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    onPlayMedia?.invoke(item.file, item.name)
                                }
                            }
                        }

                        displayedItems.forEachIndexed { index, item ->
                            val itemColor = when (item.type) {
                                CachedMediaType.VIDEO -> Color(0xFF2481CC)
                                CachedMediaType.PHOTO -> Color(0xFF4BB34B)
                                CachedMediaType.AUDIO -> Color(0xFF9C27B0)
                                CachedMediaType.DOCUMENT -> Color(0xFFFFA000)
                                CachedMediaType.CHANNEL_DATA -> Color(0xFF009688)
                            }
                            val itemIcon = when (item.type) {
                                CachedMediaType.VIDEO -> Icons.Default.Movie
                                CachedMediaType.PHOTO -> Icons.Default.Image
                                CachedMediaType.AUDIO -> Icons.Default.MusicNote
                                CachedMediaType.DOCUMENT -> Icons.Default.InsertDriveFile
                                CachedMediaType.CHANNEL_DATA -> Icons.Default.CloudQueue
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { openOrPlayItem(item) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Large 16:9 Cinematic Thumbnail (Prominent & High-Res)
                                Box(
                                    modifier = Modifier
                                        .width(168.dp)
                                        .height(106.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(itemColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.type == CachedMediaType.VIDEO || item.type == CachedMediaType.PHOTO) {
                                        coil3.compose.AsyncImage(
                                            model = item.file,
                                            contentDescription = "Media Thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        // Gradient overlay & play icon for videos
                                        if (item.type == CachedMediaType.VIDEO) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                                            listOf(
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = 0.6f)
                                                            )
                                                        )
                                                    )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.65f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        // Format / Type Pill at bottom right
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.75f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (item.type == CachedMediaType.VIDEO) item.name.substringAfterLast(".", "VIDEO").uppercase() else "PHOTO",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = itemIcon,
                                                contentDescription = null,
                                                tint = itemColor,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.name.substringAfterLast(".", "FILE").uppercase(),
                                                color = itemColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(itemColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = formatBytes(item.size),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = itemColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                        Text(
                                            text = "• ${item.source}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { openOrPlayItem(item) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.type == CachedMediaType.VIDEO || item.type == CachedMediaType.AUDIO) Icons.Default.PlayCircleFilled else Icons.Default.OpenInNew,
                                            contentDescription = "Play",
                                            tint = itemColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                telegramCacheManager.deleteCachedItem(item)
                                                withContext(Dispatchers.Main) {
                                                    refreshSizes()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        LottieTelegramDelete(
                                            size = 20.dp,
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                            if (index < displayedItems.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }

            // ==================== 3. AUTO-DELETE CACHED MEDIA (KEEP MEDIA SLIDER) ====================
            NagramSectionTitle("AUTO-DELETE CACHED MEDIA")
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keep Media",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = keepMediaLabels.getOrElse(preferences.keepMediaIndex) { "Forever" },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = preferences.keepMediaIndex.toFloat(),
                        onValueChange = { viewModel.updateKeepMediaIndex(it.toInt()) },
                        valueRange = 0f..3f,
                        steps = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        keepMediaLabels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Photos, videos and other files from cloud chats that you have not accessed during this period will be removed from this device to save disk space.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ==================== 4. MAXIMUM CACHE SIZE SLIDER ====================
            NagramSectionTitle("MAXIMUM CACHE SIZE")
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Maximum Cache Size",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = maxCacheLabels.getOrElse(preferences.maxCacheSizeIndex) { "No limit" },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = preferences.maxCacheSizeIndex.toFloat(),
                        onValueChange = { viewModel.updateMaxCacheSizeIndex(it.toInt()) },
                        valueRange = 0f..4f,
                        steps = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        maxCacheLabels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "If your cache size exceeds this limit, the oldest unused media will be removed from the device automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Selective Clear Sub-Dialog
    if (showClearSubDialog) {
        var checkVideos by remember { mutableStateOf(true) }
        var checkPhotos by remember { mutableStateOf(true) }
        var checkAudio by remember { mutableStateOf(true) }
        var checkDocuments by remember { mutableStateOf(true) }
        var checkMetadata by remember { mutableStateOf(false) }

        val selectedBytes = (if (checkVideos) videosCacheSize else 0L) +
                (if (checkPhotos) thumbnailsCacheSize else 0L) +
                (if (checkAudio) audioCacheSize else 0L) +
                (if (checkDocuments) documentsCacheSize else 0L) +
                (if (checkMetadata) metadataCacheSize else 0L)

        AlertDialog(
            onDismissRequest = { showClearSubDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Clear Cache", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select media categories to remove from local storage:", style = MaterialTheme.typography.bodySmall)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkVideos = !checkVideos }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Videos (${formatBytes(videosCacheSize)})", style = MaterialTheme.typography.bodyMedium)
                                Checkbox(checked = checkVideos, onCheckedChange = { checkVideos = it })
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkPhotos = !checkPhotos }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Photos (${formatBytes(thumbnailsCacheSize)})", style = MaterialTheme.typography.bodyMedium)
                                Checkbox(checked = checkPhotos, onCheckedChange = { checkPhotos = it })
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkAudio = !checkAudio }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Audio (${formatBytes(audioCacheSize)})", style = MaterialTheme.typography.bodyMedium)
                                Checkbox(checked = checkAudio, onCheckedChange = { checkAudio = it })
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkDocuments = !checkDocuments }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Files & Documents (${formatBytes(documentsCacheSize)})", style = MaterialTheme.typography.bodyMedium)
                                Checkbox(checked = checkDocuments, onCheckedChange = { checkDocuments = it })
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkMetadata = !checkMetadata }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Channel Metadata (${formatBytes(metadataCacheSize)})", style = MaterialTheme.typography.bodyMedium)
                                Checkbox(checked = checkMetadata, onCheckedChange = { checkMetadata = it })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            if (checkVideos) telegramCacheManager.clearVideosCache()
                            if (checkPhotos) telegramCacheManager.clearPhotosCache()
                            if (checkAudio) telegramCacheManager.clearAudioCache()
                            if (checkDocuments) telegramCacheManager.clearDocumentsCache()
                            if (checkMetadata) telegramCacheManager.clearMetadataCache()
                            viewModel.clearTdlibStreamCache(
                                clearVideos = checkVideos,
                                clearThumbnails = checkPhotos,
                                clearAudio = checkAudio
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Selected Cache Cleared", Toast.LENGTH_SHORT).show()
                                refreshSizes()
                                showClearSubDialog = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear ${formatBytes(selectedBytes)}")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClearSubDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StorageSheetDonutDiagram(
    videosBytes: Long,
    thumbnailsBytes: Long,
    audioBytes: Long,
    documentsBytes: Long,
    metadataBytes: Long,
    totalCacheBytes: Long,
    freeBytes: Long,
    totalDeviceBytes: Long,
    modifier: Modifier = Modifier
) {
    val total = if (totalCacheBytes > 0) totalCacheBytes.toFloat() else 1f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 22f
            val diameter = size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background Circle (Device Free Space)
            drawArc(
                color = Color(0x33888888),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (totalCacheBytes > 0) {
                var currentAngle = -90f

                // Videos Arc (Blue)
                val videoFrac = (videosBytes.toFloat() / total)
                if (videoFrac > 0.01f) {
                    val sweepVideo = (360f * videoFrac) - 3f
                    drawArc(
                        color = Color(0xFF2481CC),
                        startAngle = currentAngle,
                        sweepAngle = sweepVideo.coerceAtLeast(4f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += sweepVideo + 3f
                }

                // Photos Arc (Green)
                val thumbFrac = (thumbnailsBytes.toFloat() / total)
                if (thumbFrac > 0.01f) {
                    val sweepThumb = (360f * thumbFrac) - 3f
                    drawArc(
                        color = Color(0xFF4BB34B),
                        startAngle = currentAngle,
                        sweepAngle = sweepThumb.coerceAtLeast(4f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += sweepThumb + 3f
                }

                // Audio Arc (Purple)
                val audioFrac = (audioBytes.toFloat() / total)
                if (audioFrac > 0.01f) {
                    val sweepAudio = (360f * audioFrac) - 3f
                    drawArc(
                        color = Color(0xFF9C27B0),
                        startAngle = currentAngle,
                        sweepAngle = sweepAudio.coerceAtLeast(4f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += sweepAudio + 3f
                }

                // Documents Arc (Amber)
                val docFrac = (documentsBytes.toFloat() / total)
                if (docFrac > 0.01f) {
                    val sweepDoc = (360f * docFrac) - 3f
                    drawArc(
                        color = Color(0xFFFFA000),
                        startAngle = currentAngle,
                        sweepAngle = sweepDoc.coerceAtLeast(4f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += sweepDoc + 3f
                }

                // Metadata Arc (Teal)
                val metaFrac = (metadataBytes.toFloat() / total)
                if (metaFrac > 0.01f) {
                    val sweepMeta = (360f * metaFrac) - 3f
                    drawArc(
                        color = Color(0xFF009688),
                        startAngle = currentAngle,
                        sweepAngle = sweepMeta.coerceAtLeast(4f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatBytes(totalCacheBytes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Used Cache",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageLegendPill(
    color: Color,
    label: String,
    size: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = size,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NagramSheetSwitchRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun NagramSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}
