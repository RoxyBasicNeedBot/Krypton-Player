package dev.anilbeesetti.nextplayer.settings.screens.general

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.cache.TelegramCacheManager
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.theme.PremiumMeshBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val telegramCacheManager: TelegramCacheManager,
) : ViewModel() {

    val playerPreferences = preferencesRepository.playerPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerPreferences()
        )

    fun updateKeepMediaIndex(index: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(keepMediaIndex = index)
            }
        }
    }

    fun updateMaxCacheSizeIndex(index: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(maxCacheSizeIndex = index)
            }
        }
    }

    fun updateAutoSaveVideos(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(autoSaveVideosToGallery = enabled)
            }
        }
    }

    fun updateAutoSavePhotos(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(autoSavePhotosToGallery = enabled)
            }
        }
    }

    fun updateAutoDownloadMobile(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(autoDownloadMobileData = enabled)
            }
        }
    }

    fun updateAutoDownloadWifi(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(autoDownloadWifi = enabled)
            }
        }
    }

    fun getVideosCacheSize(): Long = telegramCacheManager.getTelegramVideosCacheSize()
    fun getThumbnailsCacheSize(): Long = telegramCacheManager.getThumbnailsCacheSize()
    fun getMetadataCacheSize(): Long = telegramCacheManager.getMetadataCacheSize()
    fun getTotalCacheSize(): Long = telegramCacheManager.getTotalCacheSize()
    fun getDeviceFreeSpace(): Long = telegramCacheManager.getDeviceFreeSpace()
    fun getDeviceTotalSpace(): Long = telegramCacheManager.getDeviceTotalSpace()

    fun clearVideosCache(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            telegramCacheManager.clearVideosCache()
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun clearThumbnailsCache(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            telegramCacheManager.clearThumbnailsCache()
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun clearAllCache(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            telegramCacheManager.clearAll()
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StorageSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: StorageSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences by viewModel.playerPreferences.collectAsState()

    var videosCacheSize by remember { mutableLongStateOf(0L) }
    var thumbnailsCacheSize by remember { mutableLongStateOf(0L) }
    var metadataCacheSize by remember { mutableLongStateOf(0L) }
    var totalCacheSize by remember { mutableLongStateOf(0L) }
    var deviceFreeSpace by remember { mutableLongStateOf(0L) }
    var deviceTotalSpace by remember { mutableLongStateOf(0L) }

    var showClearBottomSheet by remember { mutableStateOf(false) }

    fun refreshSizes() {
        coroutineScope.launch(Dispatchers.IO) {
            videosCacheSize = viewModel.getVideosCacheSize()
            thumbnailsCacheSize = viewModel.getThumbnailsCacheSize()
            metadataCacheSize = viewModel.getMetadataCacheSize()
            totalCacheSize = viewModel.getTotalCacheSize()
            deviceFreeSpace = viewModel.getDeviceFreeSpace()
            deviceTotalSpace = viewModel.getDeviceTotalSpace()
        }
    }

    LaunchedEffect(Unit) {
        refreshSizes()
    }

    val keepMediaLabels = listOf("3 days", "1 week", "1 month", "Forever")
    val maxCacheLabels = listOf("2 GB", "5 GB", "10 GB", "50 GB", "No limit")

    PremiumMeshBackground {
        Scaffold(
            topBar = {
                NextTopAppBar(
                    title = "Data and Storage",
                    navigationIcon = {
                        FilledTonalIconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = NextIcons.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ==================== 1. STORAGE DIAGRAM CARD (NAGRAM STYLE) ====================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Storage Usage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Custom Donut Diagram
                        StorageDonutDiagram(
                            videosBytes = videosCacheSize,
                            thumbnailsBytes = thumbnailsCacheSize,
                            metadataBytes = metadataCacheSize,
                            totalCacheBytes = totalCacheSize,
                            freeBytes = deviceFreeSpace,
                            totalDeviceBytes = deviceTotalSpace,
                            modifier = Modifier.size(160.dp)
                        )

                        // Storage Legend Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StorageLegendItem(color = Color(0xFF2481CC), label = "Videos", size = formatBytes(videosCacheSize))
                            StorageLegendItem(color = Color(0xFF4BB34B), label = "Photos", size = formatBytes(thumbnailsCacheSize))
                            StorageLegendItem(color = Color(0xFFFFA000), label = "Metadata", size = formatBytes(metadataCacheSize))
                        }

                        // Prominent Clear Cache Button
                        Button(
                            onClick = { showClearBottomSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (totalCacheSize > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (totalCacheSize > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
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

                // ==================== 2. AUTO-DELETE CACHED MEDIA (KEEP MEDIA SLIDER) ====================
                NagramSettingsSectionHeader("AUTO-DELETE CACHED MEDIA")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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

                // ==================== 3. MAXIMUM CACHE SIZE SLIDER ====================
                NagramSettingsSectionHeader("MAXIMUM CACHE SIZE")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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



                // ==================== 5. SAVE TO GALLERY ====================
                NagramSettingsSectionHeader("SAVE TO GALLERY")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column {
                        NagramSwitchRow(
                            icon = Icons.Default.Movie,
                            iconColor = Color(0xFFE91E63),
                            title = "Save Videos to Gallery",
                            subtitle = "Automatically copy downloaded videos to Movies/NextPlayer",
                            checked = preferences.autoSaveVideosToGallery,
                            onCheckedChange = { viewModel.updateAutoSaveVideos(it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        NagramSwitchRow(
                            icon = Icons.Default.PhotoLibrary,
                            iconColor = Color(0xFFFF9800),
                            title = "Save Photos to Gallery",
                            subtitle = "Automatically copy downloaded photos to Pictures/NextPlayer",
                            checked = preferences.autoSavePhotosToGallery,
                            onCheckedChange = { viewModel.updateAutoSavePhotos(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ==================== CLEAR CACHE BOTTOM SHEET ====================
    if (showClearBottomSheet) {
        ClearCacheBottomSheet(
            videosSize = videosCacheSize,
            thumbnailsSize = thumbnailsCacheSize,
            metadataSize = metadataCacheSize,
            onDismiss = { showClearBottomSheet = false },
            onClearSelected = { clearVideos, clearPhotos, clearMetadata ->
                coroutineScope.launch(Dispatchers.IO) {
                    if (clearVideos) viewModel.clearVideosCache {}
                    if (clearPhotos) viewModel.clearThumbnailsCache {}
                    if (clearMetadata) viewModel.clearAllCache {}
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Selected Cache Cleared", Toast.LENGTH_SHORT).show()
                        refreshSizes()
                        showClearBottomSheet = false
                    }
                }
            }
        )
    }
}

@Composable
private fun StorageDonutDiagram(
    videosBytes: Long,
    thumbnailsBytes: Long,
    metadataBytes: Long,
    totalCacheBytes: Long,
    freeBytes: Long,
    totalDeviceBytes: Long,
    modifier: Modifier = Modifier
) {
    val total = if (totalDeviceBytes > 0) totalDeviceBytes.toFloat() else 1f
    val videoPercent = (videosBytes.toFloat() / total).coerceIn(0.01f, 0.4f)
    val thumbPercent = (thumbnailsBytes.toFloat() / total).coerceIn(0.01f, 0.2f)
    val metaPercent = (metadataBytes.toFloat() / total).coerceIn(0.005f, 0.1f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 24f
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
                val sweepVideo = 360f * (videoPercent / (videoPercent + thumbPercent + metaPercent)).coerceIn(0.1f, 0.7f)
                drawArc(
                    color = Color(0xFF2481CC),
                    startAngle = currentAngle,
                    sweepAngle = sweepVideo,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                currentAngle += sweepVideo + 4f

                // Photos Arc (Green)
                val sweepThumb = 360f * (thumbPercent / (videoPercent + thumbPercent + metaPercent)).coerceIn(0.05f, 0.5f)
                drawArc(
                    color = Color(0xFF4BB34B),
                    startAngle = currentAngle,
                    sweepAngle = sweepThumb,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                currentAngle += sweepThumb + 4f

                // Metadata Arc (Orange)
                val sweepMeta = 360f * (metaPercent / (videoPercent + thumbPercent + metaPercent)).coerceIn(0.05f, 0.3f)
                drawArc(
                    color = Color(0xFFFFA000),
                    startAngle = currentAngle,
                    sweepAngle = sweepMeta,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatBytes(totalCacheBytes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total Cache",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageLegendItem(color: Color, label: String, size: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Text(text = size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NagramSettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun NagramSwitchRow(
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClearCacheBottomSheet(
    videosSize: Long,
    thumbnailsSize: Long,
    metadataSize: Long,
    onDismiss: () -> Unit,
    onClearSelected: (clearVideos: Boolean, clearPhotos: Boolean, clearMetadata: Boolean) -> Unit
) {
    var checkVideos by remember { mutableStateOf(true) }
    var checkPhotos by remember { mutableStateOf(true) }
    var checkMetadata by remember { mutableStateOf(false) }

    val selectedBytes = (if (checkVideos) videosSize else 0L) +
            (if (checkPhotos) thumbnailsSize else 0L) +
            (if (checkMetadata) metadataSize else 0L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Clear Cache",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Select items to remove from local cache. This will not delete any files from Telegram Cloud.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column {
                    ClearCheckboxRow(
                        title = "Telegram Videos Cache",
                        size = formatBytes(videosSize),
                        checked = checkVideos,
                        onCheckedChange = { checkVideos = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ClearCheckboxRow(
                        title = "Thumbnails & Photos Cache",
                        size = formatBytes(thumbnailsSize),
                        checked = checkPhotos,
                        onCheckedChange = { checkPhotos = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ClearCheckboxRow(
                        title = "Channels & Posts Metadata Cache",
                        size = formatBytes(metadataSize),
                        checked = checkMetadata,
                        onCheckedChange = { checkMetadata = it }
                    )
                }
            }

            Button(
                onClick = { onClearSelected(checkVideos, checkPhotos, checkMetadata) },
                enabled = selectedBytes > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "Clear ${formatBytes(selectedBytes)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

@Composable
private fun ClearCheckboxRow(
    title: String,
    size: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
