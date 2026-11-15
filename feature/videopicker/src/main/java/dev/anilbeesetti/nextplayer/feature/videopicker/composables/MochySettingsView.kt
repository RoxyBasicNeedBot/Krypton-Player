package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

private const val GITHUB_REPO_URL = "https://github.com/RoxyBasicNeedBot/Krypton-Player"
private const val TELEGRAM_URL = "https://t.me/KryptonPlayer"
private const val NEXT_PLAYER_REPO_URL = "https://github.com/anilbeesetti/nextplayer"
private const val GPL_V3_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val APP_VERSION_NAME = "v0.16.102"

/**
 * Pure Material 3 Settings View for Krypton Player.
 * Redesigned from the ground up:
 * - Clean Material 3 typography and dynamic theme color scheme
 * - Pure Material 3 cards grouping settings systematically
 * - Clear, high-usability list rows with leading tonal container icons, titles, subtitles, and interactive controls
 * - Full 110.dp bottom clearance ensuring floating navigation dock never obscures content
 */
@Composable
fun MochySettingsView(
    onSettingsClick: () -> Unit,
    onDownloadsClick: () -> Unit = {},
    onWatchlistClick: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier,
    userName: String = "Roxy",
    userPhone: String = "VIP Member",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("krypton_settings_prefs", Context.MODE_PRIVATE) }

    // Dialog States
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showNetworkStreamDialog by remember { mutableStateOf(false) }
    var networkStreamUrl by remember { mutableStateOf("") }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }

    // Playback Settings States
    var playbackSpeed by remember {
        mutableFloatStateOf(prefs.getFloat("playback_speed", 1.0f))
    }
    var resumePlayback by remember {
        mutableStateOf(prefs.getBoolean("resume_playback", true))
    }
    var backgroundPlay by remember {
        mutableStateOf(prefs.getBoolean("background_play", false))
    }
    var hwAcceleration by remember {
        mutableStateOf(prefs.getBoolean("hw_acceleration", true))
    }

    // Gestures & Controls States
    var volumeSwipe by remember {
        mutableStateOf(prefs.getBoolean("volume_swipe", true))
    }
    var brightnessSwipe by remember {
        mutableStateOf(prefs.getBoolean("brightness_swipe", true))
    }
    var doubleTapSeek by remember {
        mutableStateOf(prefs.getBoolean("double_tap_seek", true))
    }

    // Audio & Subtitles States
    var audioBoost by remember {
        mutableStateOf(prefs.getBoolean("audio_boost", true))
    }

    // Appearance States
    var darkTheme by remember {
        mutableStateOf(prefs.getBoolean("dark_theme", true))
    }
    var dynamicTheme by remember {
        mutableStateOf(prefs.getBoolean("dynamic_theme", true))
    }

    // Media Library States
    var autoRefresh by remember {
        mutableStateOf(prefs.getBoolean("auto_refresh", true))
    }
    var showHiddenFiles by remember {
        mutableStateOf(prefs.getBoolean("show_hidden_files", false))
    }

    // Cache Size State
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }

    fun refreshCacheSize() {
        scope.launch(Dispatchers.IO) {
            val size = try {
                context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } catch (_: Exception) {
                0L
            }
            withContext(Dispatchers.Main) {
                cacheSizeBytes = size
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshCacheSize()
    }

    fun openWebUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 110.dp) // Essential bottom clearance so floating dock never obscures content
    ) {
        // ==========================================
        // 1. Clean Material 3 Header
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Krypton Player $APP_VERSION_NAME",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onSettingsClick,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "All Settings",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // ==========================================
        // 2. Player & Playback Group
        // ==========================================
        SettingsGroup(title = "Player & Playback") {
            SettingsRowItem(
                title = "Playback speed",
                subtitle = "Default: ${playbackSpeed}x (Tap to change)",
                icon = Icons.Rounded.Speed,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                trailing = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                onClick = { showSpeedDialog = true }
            )

            SettingsItemDivider()

            SettingsSwitchItem(
                title = "Resume playback",
                subtitle = "Remember position and resume from where you left off",
                icon = Icons.Rounded.History,
                checked = resumePlayback,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onCheckedChange = { checked ->
                    resumePlayback = checked
                    prefs.edit().putBoolean("resume_playback", checked).apply()
                }
            )

            SettingsItemDivider()

            SettingsSwitchItem(
                title = "Background play",
                subtitle = "Continue playing audio when app is in the background",
                icon = Icons.Rounded.PlayCircle,
                checked = backgroundPlay,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onCheckedChange = { checked ->
                    backgroundPlay = checked
                    prefs.edit().putBoolean("background_play", checked).apply()
                }
            )

            SettingsItemDivider()

            SettingsSwitchItem(
                title = "Hardware acceleration",
                subtitle = "Use hardware decoder (HW+) for smooth, power-efficient playback",
                icon = Icons.Rounded.Bolt,
                checked = hwAcceleration,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onCheckedChange = { checked ->
                    hwAcceleration = checked
                    prefs.edit().putBoolean("hw_acceleration", checked).apply()
                }
            )
        }

        // ==========================================
        // 3. Gestures & Controls Group
        // ==========================================
        SettingsGroup(title = "Gestures & Controls") {
            SettingsSwitchItem(
                title = "Volume swipe",
                subtitle = "Swipe vertically on the right half of the screen",
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                checked = volumeSwipe,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onCheckedChange = { checked ->
                    volumeSwipe = checked
                    prefs.edit().putBoolean("volume_swipe", checked).apply()
                }
            )

            SettingsItemDivider()

            SettingsSwitchItem(
                title = "Brightness swipe",
                subtitle = "Swipe vertically on the left half of the screen",
                icon = Icons.Rounded.BrightnessMedium,
                checked = brightnessSwipe,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onCheckedChange = { checked ->
                    brightnessSwipe = checked
                    prefs.edit().putBoolean("brightness_swipe", checked).apply()
                }
            )

            SettingsItemDivider()

            SettingsSwitchItem(
                title = "Double tap to seek",
                subtitle = "Double tap edges to seek 10 seconds backward or forward",
                icon = Icons.Rounded.TouchApp,
                checked = doubleTapSeek,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onCheckedChange = { checked ->
                    doubleTapSeek = checked
                    prefs.edit().putBoolean("double_tap_seek", checked).apply()
                }
            )
        }

        // ==========================================
        // 4. Audio & Subtitles Group
        // ==========================================
        SettingsGroup(title = "Audio & Subtitles") {
            SettingsRowItem(
                title = "Equalizer",
                subtitle = "Audio presets, bass boost, and sound enhancements",
                icon = Icons.Rounded.GraphicEq,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onSettingsClick
            )

            SettingsItemDivider()

            SettingsSwitchItem(
                title = "Audio boost",
                subtitle = "Amplify volume up to 200% for quiet dialogue tracks",
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                checked = audioBoost,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onCheckedChange = { checked ->
                    audioBoost = checked
                    prefs.edit().putBoolean("audio_boost", checked).apply()
                }
            )

            SettingsItemDivider()

            SettingsRowItem(
                title = "Subtitle styling",
                subtitle = "Font family, text size, color, background, and encoding",
                icon = Icons.Rounded.Subtitles,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onSettingsClick
            )
        }

        // ==========================================
        // 5. Appearance Group
        // ==========================================
        SettingsGroup(title = "Appearance") {
            SettingsSwitchItem(
                title = "Dark theme",
                subtitle = "Pure black AMOLED styling for high contrast and battery efficiency",
                icon = Icons.Rounded.DarkMode,
                checked = darkTheme,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onCheckedChange = { checked ->
                    darkTheme = checked
                    prefs.edit().putBoolean("dark_theme", checked).apply()
                }
            )

            SettingsItemDivider()

            SettingsSwitchItem(
                title = "Dynamic theme",
                subtitle = "Material You dynamic colors generated from system wallpaper",
                icon = Icons.Rounded.Palette,
                checked = dynamicTheme,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onCheckedChange = { checked ->
                    dynamicTheme = checked
                    prefs.edit().putBoolean("dynamic_theme", checked).apply()
                }
            )
        }

        // ==========================================
        // 6. Media Library Group
        // ==========================================
        SettingsGroup(title = "Media Library") {
            SettingsSwitchItem(
                title = "Auto-refresh",
                subtitle = "Automatically detect and catalog newly added videos",
                icon = Icons.Rounded.Sync,
                checked = autoRefresh,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onCheckedChange = { checked ->
                    autoRefresh = checked
                    prefs.edit().putBoolean("auto_refresh", checked).apply()
                }
            )

            SettingsItemDivider()

            SettingsRowItem(
                title = "Scan folders",
                subtitle = "Select included and excluded media folders on device",
                icon = Icons.Rounded.Folder,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onSettingsClick
            )

            SettingsItemDivider()

            SettingsSwitchItem(
                title = "Show hidden files",
                subtitle = "Display hidden media and folders starting with dot or .nomedia",
                icon = Icons.Rounded.Visibility,
                checked = showHiddenFiles,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onCheckedChange = { checked ->
                    showHiddenFiles = checked
                    prefs.edit().putBoolean("show_hidden_files", checked).apply()
                }
            )
        }

        // ==========================================
        // 7. Cloud & Network Group
        // ==========================================
        SettingsGroup(title = "Cloud & Network") {
            SettingsRowItem(
                title = "Network streaming",
                subtitle = "Stream video directly from URL (HTTP, HLS, DASH, RTSP)",
                icon = Icons.Rounded.Cloud,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { showNetworkStreamDialog = true }
            )

            SettingsItemDivider()

            SettingsRowItem(
                title = "Cache size",
                subtitle = "${formatSize(cacheSizeBytes)} used • Tap to clear cache",
                icon = Icons.Rounded.Storage,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                trailing = {
                    FilledTonalButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    context.cacheDir.deleteRecursively()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                                        refreshCacheSize()
                                    }
                                } catch (_: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed to clear cache", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                },
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            context.cacheDir.deleteRecursively()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                                refreshCacheSize()
                            }
                        } catch (_: Exception) {}
                    }
                }
            )
        }

        // ==========================================
        // 8. About Krypton Player Group
        // ==========================================
        SettingsGroup(title = "About Krypton Player") {
            SettingsRowItem(
                title = "App version",
                subtitle = "$APP_VERSION_NAME • Stable (Latest build)",
                icon = Icons.Rounded.Info,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                trailing = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "Release",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                onClick = {
                    Toast.makeText(context, "Krypton Player $APP_VERSION_NAME is up to date", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsItemDivider()

            SettingsRowItem(
                title = "Open Source Licenses",
                subtitle = "Licensed under GNU General Public License v3.0 (GPLv3)",
                icon = Icons.Rounded.Description,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { showLicensesDialog = true }
            )

            SettingsItemDivider()

            SettingsRowItem(
                title = "GitHub Repository",
                subtitle = GITHUB_REPO_URL,
                icon = Icons.Rounded.Code,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Open in browser",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { openWebUrl(GITHUB_REPO_URL) }
            )

            SettingsItemDivider()

            SettingsRowItem(
                title = "Telegram Community",
                subtitle = "Join @KryptonPlayer for support & releases",
                icon = Icons.Rounded.Cloud,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Open in browser",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { openWebUrl(TELEGRAM_URL) }
            )

            SettingsItemDivider()

            SettingsRowItem(
                title = "Based on Next Player",
                subtitle = "Credits to Anil Beesetti and open-source contributors",
                icon = Icons.Rounded.Favorite,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { showCreditsDialog = true }
            )
        }
    }

    // ==========================================
    // Playback Speed Dialog
    // ==========================================
    if (showSpeedDialog) {
        val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = {
                Text(
                    text = "Default Playback Speed",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    speedOptions.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    playbackSpeed = speed
                                    prefs.edit().putFloat("playback_speed", speed).apply()
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${speed}x" + if (speed == 1.0f) " (Normal)" else "",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (speed == playbackSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (speed == playbackSpeed) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // ==========================================
    // Network Stream Dialog
    // ==========================================
    if (showNetworkStreamDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkStreamDialog = false },
            title = {
                Text(
                    text = "Network Streaming",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a direct video or stream URL to play (HTTP, HTTPS, HLS, DASH, RTSP):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = networkStreamUrl,
                        onValueChange = { networkStreamUrl = it },
                        label = { Text("Stream URL") },
                        placeholder = { Text("https://example.com/video.m3u8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNetworkStreamDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val trimmed = networkStreamUrl.trim()
                        if (trimmed.isNotBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(trimmed), "video/*")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                showNetworkStreamDialog = false
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not open stream", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Play Stream")
                }
            }
        )
    }

    // ==========================================
    // Open Source Licenses Dialog
    // ==========================================
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = {
                Text(
                    text = "Open Source Licenses",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Krypton Player is free and open-source software licensed under the GNU General Public License v3.0 (GPLv3).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "You are free to run, copy, distribute, study, change and improve this application under the conditions of the GPLv3 license.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { openWebUrl(GPL_V3_URL) }) {
                    Text("View License (Web)")
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // ==========================================
    // Credits Dialog
    // ==========================================
    if (showCreditsDialog) {
        AlertDialog(
            onDismissRequest = { showCreditsDialog = false },
            title = {
                Text(
                    text = "Credits & Acknowledgments",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Krypton Player is an enhanced open-source media player based on Next Player by Anil Beesetti.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Huge thanks to Anil Beesetti and all contributors to the open source community whose work made this player possible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { openWebUrl(NEXT_PLAYER_REPO_URL) }) {
                    Text("Next Player GitHub")
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreditsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Grouped Material 3 Card Container for settings sections.
 */
@Composable
private fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    }
}

/**
 * Clean Material 3 Setting Row with Leading Container Icon, Title, Subtitle, and Trailing Control.
 */
@Composable
private fun SettingsRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconContentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Clean Material 3 Setting Row with Switch Toggle.
 */
@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (onClick != null) {
                    onClick()
                } else {
                    onCheckedChange(!checked)
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconContentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Inset Divider separating items within a Material 3 Grouped Card.
 */
@Composable
private fun SettingsItemDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(start = 70.dp, end = 16.dp)
    )
}

/** Formats byte sizes in powers of 1024 */
private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    val number = if (value >= 10 || unit == 0) value.roundToInt().toString() else String.format(Locale.US, "%.1f", value)
    return "$number ${units[unit]}"
}
