package dev.roxy.kryptonplayer.feature.videopicker.composables

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.AppUpdateManager
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import dev.roxy.kryptonplayer.core.ui.components.UpdateBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

private const val GITHUB_REPO_URL = "https://github.com/RoxyBasicNeedBot/Krypton-Player"
private const val TELEGRAM_URL = "https://t.me/KryptonPlayer"
private const val NEXT_PLAYER_REPO_URL = "https://github.com/anilbeesetti/nextplayer"
private const val GPL_V3_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MochySettingsEntryPoint {
    fun appUpdateManager(): AppUpdateManager
}

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
    appUpdateManager: AppUpdateManager? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("krypton_settings_prefs", Context.MODE_PRIVATE) }

    // Dynamically resolve real installed version name with safe fallback
    val rawVersionName = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
    val displayVersionName = remember(rawVersionName) {
        if (rawVersionName.startsWith("v", ignoreCase = true)) rawVersionName else "v$rawVersionName"
    }

    // Connect with AppUpdateManager (via passed instance, Hilt EntryPoint, or safe fallback)
    val resolvedUpdateManager = remember(appUpdateManager, context) {
        appUpdateManager ?: try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                MochySettingsEntryPoint::class.java
            ).appUpdateManager()
        } catch (_: Throwable) {
            AppUpdateManager(context.applicationContext)
        }
    }
    val updateState by resolvedUpdateManager.updateState.collectAsStateWithLifecycle()

    var activeUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateState.Available -> activeUpdateInfo = state.info
            is UpdateState.Downloading -> state.info?.let { activeUpdateInfo = it }
            is UpdateState.ReadyToInstall -> state.info?.let { activeUpdateInfo = it }
            else -> Unit
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "updateSpinTransition")
    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinRotation"
    )

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
                    text = "Krypton Player $displayVersionName",
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
            val versionSubtitle = when (val state = updateState) {
                is UpdateState.Checking -> "Checking for updates..."
                is UpdateState.Available -> {
                    val remoteVer = if (state.info.versionName.startsWith("v", ignoreCase = true)) {
                        state.info.versionName
                    } else {
                        "v${state.info.versionName}"
                    }
                    "$displayVersionName • Update available: $remoteVer"
                }
                is UpdateState.Downloading -> {
                    if (state.progress >= 0) "Downloading update (${state.progress}%)..." else "Downloading update..."
                }
                is UpdateState.Verifying -> "Verifying update package..."
                is UpdateState.ReadyToInstall -> "Update downloaded • Tap to install"
                is UpdateState.Error -> "Update check failed • Tap to retry"
                is UpdateState.UpToDate, is UpdateState.Idle -> "$displayVersionName • Stable (Latest build)"
            }

            val rowIcon = when (updateState) {
                is UpdateState.Available, is UpdateState.Downloading -> Icons.Rounded.Download
                is UpdateState.ReadyToInstall -> Icons.Rounded.CheckCircle
                else -> Icons.Rounded.Info
            }

            val rowIconContainerColor = when (updateState) {
                is UpdateState.Available -> MaterialTheme.colorScheme.primary
                is UpdateState.ReadyToInstall -> Color(0xFF10B981)
                else -> MaterialTheme.colorScheme.primaryContainer
            }

            val rowIconContentColor = when (updateState) {
                is UpdateState.Available -> MaterialTheme.colorScheme.onPrimary
                is UpdateState.ReadyToInstall -> Color.White
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }

            SettingsRowItem(
                title = "App version",
                subtitle = versionSubtitle,
                icon = rowIcon,
                iconContainerColor = rowIconContainerColor,
                iconContentColor = rowIconContentColor,
                trailing = {
                    when (val state = updateState) {
                        is UpdateState.Checking -> {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Checking for updates",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(spinRotation)
                            )
                        }
                        is UpdateState.Available -> {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                                    )
                                    Text(
                                        text = "UPDATE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.6.sp,
                                            fontSize = 10.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        is UpdateState.Downloading -> {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = if (state.progress >= 0) "${state.progress}%" else "...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        is UpdateState.ReadyToInstall -> {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "INSTALL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp,
                                        fontSize = 10.sp
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                                )
                            }
                        }
                        is UpdateState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "Retry",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        else -> {
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
                        }
                    }
                },
                onClick = {
                    when (val state = updateState) {
                        is UpdateState.Available -> {
                            activeUpdateInfo = state.info
                            showUpdateSheet = true
                        }
                        is UpdateState.Downloading -> {
                            state.info?.let { activeUpdateInfo = it }
                            showUpdateSheet = true
                        }
                        is UpdateState.Verifying -> {
                            showUpdateSheet = true
                        }
                        is UpdateState.ReadyToInstall -> {
                            state.info?.let { activeUpdateInfo = it }
                            showUpdateSheet = true
                        }
                        is UpdateState.UpToDate -> {
                            Toast.makeText(context, "Krypton Player $displayVersionName is up to date!", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                val res = resolvedUpdateManager.checkForUpdates(manual = true)
                                if (res?.isUpdateAvailable == true) {
                                    activeUpdateInfo = res
                                    showUpdateSheet = true
                                }
                            }
                        }
                        is UpdateState.Checking -> {
                            // Already in progress
                        }
                        is UpdateState.Idle, is UpdateState.Error -> {
                            scope.launch {
                                val res = resolvedUpdateManager.checkForUpdates(manual = true)
                                if (res?.isUpdateAvailable == true) {
                                    activeUpdateInfo = res
                                    showUpdateSheet = true
                                } else if (resolvedUpdateManager.updateState.value is UpdateState.UpToDate) {
                                    Toast.makeText(context, "Krypton Player $displayVersionName is up to date!", Toast.LENGTH_SHORT).show()
                                } else if (resolvedUpdateManager.updateState.value is UpdateState.Error) {
                                    val errMsg = (resolvedUpdateManager.updateState.value as UpdateState.Error).message
                                    Toast.makeText(context, errMsg.ifBlank { "Could not check for updates" }, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
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

    // ==========================================
    // Liquid Glass App Update Sheet
    // ==========================================
    val effectiveUpdateInfo = (updateState as? UpdateState.Available)?.info
        ?: (updateState as? UpdateState.Downloading)?.info
        ?: (updateState as? UpdateState.ReadyToInstall)?.info
        ?: activeUpdateInfo
        ?: AppUpdateInfo(
            versionName = "Latest",
            versionCode = 0,
            changelog = "",
            downloadUrl = "",
            fileName = "Krypton-Player.apk",
            fileSize = 0L,
            isUpdateAvailable = true,
        )

    if (showUpdateSheet) {
        UpdateBottomSheet(
            info = effectiveUpdateInfo,
            updateState = updateState,
            currentVersionName = displayVersionName,
            onDownload = {
                resolvedUpdateManager.downloadAndInstall(effectiveUpdateInfo)
            },
            onInstall = {
                val apkUri = (updateState as? UpdateState.ReadyToInstall)?.apkUri
                resolvedUpdateManager.installApk(apkUri)
            },
            onDismiss = {
                showUpdateSheet = false
            },
            onSkipVersion = {
                resolvedUpdateManager.skipVersion(effectiveUpdateInfo.tagName)
                showUpdateSheet = false
            },
            onCancelDownload = {
                resolvedUpdateManager.cancelDownload()
            },
            onOpenReleaseUrl = {
                val url = effectiveUpdateInfo.releaseUrl.ifBlank { GITHUB_REPO_URL }
                openWebUrl(url)
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
