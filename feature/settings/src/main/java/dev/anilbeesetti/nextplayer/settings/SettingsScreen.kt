package dev.anilbeesetti.nextplayer.settings

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Badge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.contentColorFor
import dev.anilbeesetti.nextplayer.core.data.update.AppUpdateInfo
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import dev.anilbeesetti.nextplayer.core.data.update.UpdateState
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.AppUpdateDialog
import dev.anilbeesetti.nextplayer.core.ui.components.CancelButton
import dev.anilbeesetti.nextplayer.core.ui.components.DoneButton
import dev.anilbeesetti.nextplayer.core.ui.components.EndOfPageFooter
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.UpdateDialog
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.components.MochyNavigationDock
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import dev.anilbeesetti.nextplayer.core.ui.components.SimpFloatingNavigationBar
import dev.anilbeesetti.nextplayer.core.ui.components.MainNavTab
import dev.anilbeesetti.nextplayer.core.ui.components.LiquidGlassIconButton
import dev.anilbeesetti.nextplayer.core.ui.components.bouncyPress
import dev.anilbeesetti.nextplayer.core.ui.theme.PremiumMeshBackground
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.rememberBackdrop
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.layerBackdrop
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.liquidGlassBorderLaser
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.settings.composables.AnimatedSettingsIcon
import dev.anilbeesetti.nextplayer.settings.composables.SettingsIconAnimation
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onItemClick: (Setting) -> Unit,
    onNavigateToHome: () -> Unit = onNavigateUp,
    onNavigateToExplore: () -> Unit = onNavigateUp,
    onSearchClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val appVersion = remember { context.appVersion() }
    val backdrop = rememberBackdrop()

    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val gitHubCommits by viewModel.gitHubCommits.collectAsStateWithLifecycle()
    var isUpdateBannerDismissed by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var pendingUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isMoreMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showNetworkStreamDialog by rememberSaveable { mutableStateOf(false) }

    val selectVideoFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage(context.packageName)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.resumePendingInstall()
    }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateState.Available -> {
                pendingUpdateInfo = state.info
                showUpdateDialog = true
            }
            is UpdateState.ReadyToInstall -> {
                if (state.info != null) {
                    pendingUpdateInfo = state.info
                    showUpdateDialog = true
                }
            }
            is UpdateState.UpToDate -> {
                android.widget.Toast.makeText(
                    context,
                    "App is up to date (${state.currentVersion.ifBlank { appVersion }})",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
            is UpdateState.Error -> {
                android.widget.Toast.makeText(
                    context,
                    state.message,
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
            else -> Unit
        }
    }

    val playbackGroup = remember {
        listOf(
            SettingRow.APPEARANCE,
            SettingRow.MEDIA_LIBRARY,
            SettingRow.PLAYER,
            SettingRow.WATCH_PARTY,
            SettingRow.LYRICS,
            SettingRow.GESTURES,
            SettingRow.SUBTITLE,
        )
    }

    val engineGroup = remember {
        listOf(
            SettingRow.DECODER,
            SettingRow.AUDIO,
        )
    }

    val systemGroup = remember {
        listOf(
            SettingRow.GENERAL,
            SettingRow.STATS,
            SettingRow.BACKUP_RESTORE,
            SettingRow.STORAGE,
            SettingRow.ABOUT,
        )
    }

    // Update Dialog display when triggered from banner or row
    if (showUpdateDialog && pendingUpdateInfo != null) {
        val info = pendingUpdateInfo!!
        val isDownloading = updateState is UpdateState.Downloading
        val downloadProgress = (updateState as? UpdateState.Downloading)?.progress ?: -1
        val bytesDownloaded = (updateState as? UpdateState.Downloading)?.bytesDownloaded ?: 0L
        val totalBytes = (updateState as? UpdateState.Downloading)?.totalBytes ?: info.fileSize
        val isReady = updateState is UpdateState.ReadyToInstall
        val apkUri = (updateState as? UpdateState.ReadyToInstall)?.apkUri

        AppUpdateDialog(
            versionName = info.versionName,
            changelog = info.changelog,
            currentVersionName = appVersion,
            fileSize = info.fileSize,
            releaseDate = info.releaseDate,
            downloadProgress = downloadProgress,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            isReadyToInstall = isReady,
            apkAssets = info.apkAssets,
            commits = gitHubCommits,
            isUpToDate = !info.isUpdateAvailable,
            onDownload = { viewModel.downloadAndInstall(info) },
            onDownloadAsset = { asset -> viewModel.downloadAndInstall(info, asset) },
            onInstall = { viewModel.installApk(apkUri) },
            onDismiss = {
                showUpdateDialog = false
                if (!isDownloading) {
                    viewModel.resetUpdateState()
                }
            },
            onOpenReleaseUrl = if (info.releaseUrl.isNotBlank()) {
                {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(info.releaseUrl),
                        ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            } else null,
        )
    }

    if (showNetworkStreamDialog) {
        SettingsNetworkUrlDialog(
            onDismiss = { showNetworkStreamDialog = false },
            onDone = { url ->
                showNetworkStreamDialog = false
                val trimmed = url.trim()
                if (trimmed.isNotEmpty()) {
                    val streamUri = Uri.parse(trimmed)
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(streamUri, "video/*")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            setPackage(context.packageName)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(streamUri, "video/*")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
            },
        )
    }

    var isDownloadsExpanded by remember { mutableStateOf(false) }
    var downloadOnlyWifi by remember { mutableStateOf(false) }
    var showAccountSettingsSheet by rememberSaveable { mutableStateOf(false) }

    if (showAccountSettingsSheet) {
        AccountSettingsBottomSheet(
            appVersion = appVersion,
            onDismiss = { showAccountSettingsSheet = false }
        )
    }

    Scaffold(
        containerColor = Color(0xFF07090E),
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        imageVector = NextIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Help & Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        bottomBar = {
            MochyNavigationDock(
                selectedTabIndex = 3,
                onTabSelected = { index ->
                    when (index) {
                        0 -> onNavigateToHome()
                        1 -> onNavigateToExplore()
                        2 -> onNavigateUp()
                        else -> Unit
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 0.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                end = 0.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            // Update Banner
            val bannerInfo = when (val state = updateState) {
                is UpdateState.Available -> state.info
                is UpdateState.ReadyToInstall -> state.info ?: pendingUpdateInfo
                is UpdateState.Downloading -> pendingUpdateInfo
                else -> null
            }
            if (bannerInfo != null && !isUpdateBannerDismissed) {
                item(key = "update_banner") {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val isReady = updateState is UpdateState.ReadyToInstall
                        val isDownloading = updateState is UpdateState.Downloading
                        val downloadProgress = (updateState as? UpdateState.Downloading)?.progress ?: -1
                        val apkUri = (updateState as? UpdateState.ReadyToInstall)?.apkUri
                        SettingsUpdateBanner(
                            info = bannerInfo,
                            isReadyToInstall = isReady,
                            isDownloading = isDownloading,
                            downloadProgress = downloadProgress,
                            onUpdateClick = {
                                if (isReady && apkUri != null) {
                                    viewModel.installApk(apkUri)
                                } else {
                                    pendingUpdateInfo = bannerInfo
                                    showUpdateDialog = true
                                }
                            },
                            onDismissClick = { isUpdateBannerDismissed = true },
                        )
                    }
                }
            }

            // 1. Account Settings
            item(key = "account_settings") {
                HotstarSettingRow(
                    title = "Account Settings",
                    subtitle = "Subscription Details & Device Manager",
                    icon = Icons.Rounded.AccountCircle,
                    onClick = { showAccountSettingsSheet = true }
                )
                HotstarDivider()
            }

            // 2. Downloads (Expandable like in screenshot!)
            item(key = "downloads_section") {
                Column {
                    HotstarSettingRow(
                        title = "Downloads",
                        subtitle = "Download quality, storage & more",
                        icon = Icons.Rounded.Download,
                        trailingContent = {
                            Icon(
                                imageVector = if (isDownloadsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF828A9E),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { isDownloadsExpanded = !isDownloadsExpanded }
                    )

                    AnimatedVisibility(visible = isDownloadsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0C0F17))
                                .padding(vertical = 4.dp)
                        ) {
                            // Sub-row 1: Download only on Wi-Fi switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 54.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Download only on Wi-Fi for this device",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = downloadOnlyWifi,
                                    onCheckedChange = { downloadOnlyWifi = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF0075FF),
                                        uncheckedThumbColor = Color(0xFF828A9E),
                                        uncheckedTrackColor = Color(0xFF1E2433)
                                    )
                                )
                            }

                            // Sub-row 2: Default Download Video Quality
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemClick(Setting.STORAGE) }
                                    .padding(start = 54.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Default Download Video Quality",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "HD",
                                        fontSize = 12.sp,
                                        color = Color(0xFF828A9E)
                                    )
                                }
                                Icon(
                                    imageVector = NextIcons.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF5C667A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    HotstarDivider()
                }
            }

            // 3. Media Library
            item(key = "media_library") {
                HotstarSettingRow(
                    title = "Media Library & Folders",
                    subtitle = "Included folders, scan media, hidden files",
                    icon = Icons.Rounded.Folder,
                    onClick = { onItemClick(Setting.MEDIA_LIBRARY) }
                )
                HotstarDivider()
            }

            // 4. Player Controls
            item(key = "player_controls") {
                HotstarSettingRow(
                    title = "Player Controls",
                    subtitle = "Seek gestures, double tap, zoom & brightness",
                    icon = Icons.Rounded.PlayCircle,
                    onClick = { onItemClick(Setting.PLAYER) }
                )
                HotstarDivider()
            }

            // 5. Audio & Decoders
            item(key = "audio_decoder") {
                HotstarSettingRow(
                    title = "Audio & Decoders",
                    subtitle = "Hardware acceleration (HW/SW), audio boost & track",
                    icon = NextIcons.GraphicEq,
                    onClick = { onItemClick(Setting.DECODER) }
                )
                HotstarDivider()
            }

            // 6. Subtitles
            item(key = "subtitles") {
                HotstarSettingRow(
                    title = "Subtitles",
                    subtitle = "Font size, styling, encoding & subtitles download",
                    icon = Icons.Rounded.Subtitles,
                    onClick = { onItemClick(Setting.SUBTITLE) }
                )
                HotstarDivider()
            }

            // 7. Lyrics
            item(key = "lyrics") {
                HotstarSettingRow(
                    title = "Lyrics",
                    subtitle = "Live synced lyrics, Romanization & styling",
                    icon = Icons.Rounded.MusicNote,
                    onClick = { onItemClick(Setting.LYRICS) }
                )
                HotstarDivider()
            }

            // 8. App Language
            item(key = "app_language") {
                HotstarSettingRow(
                    title = "App Language",
                    subtitle = "English",
                    icon = Icons.Rounded.Translate,
                    onClick = { onItemClick(Setting.GENERAL) }
                )
                HotstarDivider()
            }

            // 9. Parental Controls & Security
            item(key = "parental_controls") {
                HotstarSettingRow(
                    title = "Parental Controls",
                    subtitle = "Parental Lock & Watch Party",
                    icon = Icons.Rounded.Lock,
                    onClick = { onItemClick(Setting.WATCH_PARTY) }
                )
                HotstarDivider()
            }

            // 10. Backup & Restore
            item(key = "backup_restore") {
                HotstarSettingRow(
                    title = "Backup & Restore",
                    subtitle = "Export and import app settings & library data",
                    icon = NextIcons.Sync,
                    onClick = { onItemClick(Setting.BACKUP_RESTORE) }
                )
                HotstarDivider()
            }

            // 11. Help & Support
            item(key = "help_support") {
                HotstarSettingRow(
                    title = "Help & Support",
                    subtitle = "Help Centre, version info & developer",
                    icon = Icons.Rounded.HelpOutline,
                    onClick = { onItemClick(Setting.ABOUT) }
                )
                HotstarDivider()
            }

            // 12. Bottom Footer (matching screenshot)
            item(key = "hotstar_footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Check for Updates",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0075FF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.checkForUpdates(currentVersionName = appVersion, manual = true)
                                android.widget.Toast.makeText(context, "Checking for updates...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Privacy Policy   •   Subscriber Agreement",
                        fontSize = 12.sp,
                        color = Color(0xFF5A6275),
                        modifier = Modifier.clickable { onItemClick(Setting.ABOUT) }
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "App Version $appVersion",
                        fontSize = 12.sp,
                        color = Color(0xFF5A6275)
                    )
                }
            }
        }
    }
}

@Composable
private fun HotstarSettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFBAC5D6),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF828A9E)
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                imageVector = NextIcons.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF5C667A),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun HotstarDivider() {
    HorizontalDivider(
        color = Color(0xFF131724),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 18.dp)
    )
}

@Composable
private fun SettingsUpdateBanner(
    info: AppUpdateInfo,
    isReadyToInstall: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Int = -1,
    onUpdateClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "updateBannerScale",
    )

    val bannerTitle = when {
        isReadyToInstall -> "Update Ready to Install"
        isDownloading -> "Downloading Update"
        else -> "New Version Available"
    }

    val bannerSubtitle = when {
        isReadyToInstall -> "Update downloaded — Tap to install"
        isDownloading -> if (downloadProgress in 0..100) "Downloading package: $downloadProgress%" else "Downloading update package..."
        info.changelog.isNotBlank() -> info.changelog.take(65) + "…"
        else -> "Tap to view changelog & update"
    }

    val buttonText = when {
        isReadyToInstall -> "Install"
        isDownloading -> "Details"
        else -> "Update"
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val bannerShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = bannerShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            )
            .clip(bannerShape)
            .background(
                color = if (isDark) Color(0xA61B1E2E) else Color(0xEEF0F4FF),
                shape = bannerShape,
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.60f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    ),
                ),
                shape = bannerShape,
            )
            .liquidGlassBorderLaser(
                laserColor = MaterialTheme.colorScheme.primary,
                cornerRadius = 22.dp,
                strokeWidth = 1.2.dp,
                glowWidth = 3.6.dp,
            )
            .bouncyPress(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.update),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onUpdateClick,
                    ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bannerTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "v${info.versionName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bannerSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onUpdateClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            IconButton(
                onClick = onDismissClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    items: List<SettingRow>,
    onItemClick: (Setting) -> Unit,
    appVersion: String,
    isUpdateAvailable: Boolean,
    onUpdateClick: () -> Unit,
    groupIndex: Int,
    modifier: Modifier = Modifier,
) {
    val entranceAlpha = remember { Animatable(0f) }
    val entranceTranslateY = remember { Animatable(24f) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val groupShape = RoundedCornerShape(22.dp)

    LaunchedEffect(Unit) {
        delay(groupIndex * 70L)
        entranceAlpha.animateTo(
            1f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        )
    }
    LaunchedEffect(Unit) {
        delay(groupIndex * 70L)
        entranceTranslateY.animateTo(
            0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entranceAlpha.value
                translationY = entranceTranslateY.value
            },
    ) {
        SettingsSectionLabel(text = title)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = groupShape,
                    spotColor = Color.Black.copy(alpha = 0.35f),
                )
                .clip(groupShape)
                .background(
                    color = if (isDark) Color(0x99161824) else MaterialTheme.colorScheme.surfaceContainer,
                    shape = groupShape,
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.05f),
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            )
                        },
                    ),
                    shape = groupShape,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(groupShape),
            ) {
                items.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 74.dp),
                            color = if (isDark) Color.White.copy(alpha = 0.07f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        )
                    }
                    SettingsSegmentedItem(
                        row = item,
                        index = index,
                        count = items.size,
                        appVersion = appVersion,
                        isUpdateAvailable = isUpdateAvailable && item.setting == Setting.ABOUT,
                        onClick = {
                            onItemClick(item.setting)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSegmentedItem(
    row: SettingRow,
    index: Int,
    count: Int,
    appVersion: String,
    isUpdateAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "settingsRowScale",
    )
    val (iconBgColor, iconContentColor) = when (row.accentType) {
        AccentType.PRIMARY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        AccentType.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        AccentType.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
    }

    var tapCount by remember { mutableIntStateOf(0) }

    val title = if (row.titleResId != 0) {
        stringResource(id = row.titleResId)
    } else {
        row.title ?: ""
    }
    val description = if (row.descriptionResId != 0) {
        stringResource(id = row.descriptionResId)
    } else {
        row.description ?: ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .bouncyPress()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    tapCount++
                    onClick()
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconContentColor.copy(alpha = 0.14f))
                .border(
                    width = 1.dp,
                    color = iconContentColor.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isUpdateAvailable) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(8.dp),
                        )
                    },
                ) {
                    RenderRowIcon(row, tapCount, iconContentColor)
                }
            } else {
                RenderRowIcon(row, tapCount, iconContentColor)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (row.setting == Setting.ABOUT && appVersion.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isUpdateAvailable) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = if (isUpdateAvailable) "UPDATE" else "v$appVersion",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUpdateAvailable) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        Icon(
            imageVector = NextIcons.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RenderRowIcon(
    row: SettingRow,
    tapCount: Int,
    iconTint: Color,
) {
    if (row.drawableResId != 0) {
        Icon(
            painter = painterResource(id = row.drawableResId),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    } else {
        AnimatedSettingsIcon(
            icon = row.icon,
            animationType = row.animationType,
            tint = iconTint,
            triggerKey = tapCount,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FeaturedWatchPartyCard(
    row: SettingRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "featuredScale",
    )

    val entranceAlpha = remember { Animatable(0f) }
    val entranceTranslateY = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(
            1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        )
    }
    LaunchedEffect(Unit) {
        entranceTranslateY.animateTo(
            0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    val title = if (row.titleResId != 0) {
        stringResource(id = row.titleResId)
    } else {
        row.title ?: ""
    }
    val description = if (row.descriptionResId != 0) {
        stringResource(id = row.descriptionResId)
    } else {
        row.description ?: ""
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val featuredShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entranceAlpha.value
                translationY = entranceTranslateY.value
            }
            .shadow(
                elevation = 14.dp,
                shape = featuredShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            )
            .clip(featuredShape)
            .background(
                color = if (isDark) Color(0x99181B2A) else MaterialTheme.colorScheme.surfaceContainer,
                shape = featuredShape,
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f),
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                        )
                    },
                ),
                shape = featuredShape,
            )
            .liquidGlassBorderLaser(
                laserColor = MaterialTheme.colorScheme.primary,
                cornerRadius = 24.dp,
                strokeWidth = 1.2.dp,
                glowWidth = 3.6.dp,
            )
            .bouncyPress()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (row.drawableResId != 0) {
                    Icon(
                        painter = painterResource(id = row.drawableResId),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    Icon(
                        imageVector = row.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = NextIcons.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsAppIdentityCard(
    versionName: String,
    isUpdateAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = cardShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            )
            .clip(cardShape)
            .background(
                color = if (isDark) Color(0xA6181B28) else MaterialTheme.colorScheme.surfaceContainer,
                shape = cardShape,
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f),
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                        )
                    },
                ),
                shape = cardShape,
            )
            .liquidGlassBorderLaser(
                laserColor = MaterialTheme.colorScheme.primary,
                coreColor = Color.White,
                haloColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                cornerRadius = 24.dp,
                strokeWidth = 1.2.dp,
                glowWidth = 3.6.dp,
            )
            .bouncyPress()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Gradient app logo circle with monochrome mark
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                dev.anilbeesetti.nextplayer.core.ui.components.AnimatedAppLogo(
                    modifier = Modifier.size(32.dp),
                    useGradient = false,
                    singleColor = MaterialTheme.colorScheme.onPrimary,
                    animateOnEntry = true,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.25.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "v$versionName • Liquid Glass Edition",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isUpdateAvailable) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = if (isUpdateAvailable) "UPDATE" else "STABLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUpdateAvailable) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Icon(
                    imageVector = NextIcons.ArrowForward,
                    contentDescription = "View Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun Context.appVersion(): String {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: ""
    } catch (_: Exception) {
        ""
    }
}

enum class Setting {
    APPEARANCE,
    MEDIA_LIBRARY,
    PLAYER,
    GESTURES,
    DECODER,
    AUDIO,
    SUBTITLE,
    LYRICS,
    GENERAL,
    STATS,
    BACKUP_RESTORE,
    STORAGE,
    WATCH_PARTY,
    ABOUT,
}

private enum class AccentType { PRIMARY, SECONDARY, TERTIARY }

private enum class SettingRow(
    val titleResId: Int = 0,
    val descriptionResId: Int = 0,
    val title: String? = null,
    val description: String? = null,
    @DrawableRes val drawableResId: Int = 0,
    val icon: androidx.compose.ui.graphics.vector.ImageVector = NextIcons.Settings,
    val animationType: SettingsIconAnimation = SettingsIconAnimation.PULSE,
    val setting: Setting,
    val accentType: AccentType = AccentType.PRIMARY,
) {
    APPEARANCE(
        titleResId = R.string.appearance_name,
        descriptionResId = R.string.appearance_description,
        drawableResId = R.drawable.palette,
        setting = Setting.APPEARANCE,
        accentType = AccentType.SECONDARY,
    ),
    MEDIA_LIBRARY(
        titleResId = R.string.media_library,
        descriptionResId = R.string.media_library_description,
        drawableResId = R.drawable.history,
        setting = Setting.MEDIA_LIBRARY,
        accentType = AccentType.PRIMARY,
    ),
    PLAYER(
        titleResId = R.string.player_name,
        descriptionResId = R.string.player_description,
        drawableResId = R.drawable.tune,
        setting = Setting.PLAYER,
        accentType = AccentType.TERTIARY,
    ),
    LYRICS(
        title = "Lyrics",
        description = "Live synced lyrics, styling & providers",
        drawableResId = R.drawable.lyrics,
        setting = Setting.LYRICS,
        accentType = AccentType.SECONDARY,
    ),
    GESTURES(
        titleResId = R.string.gestures_name,
        descriptionResId = R.string.gestures_description,
        drawableResId = R.drawable.swipe,
        setting = Setting.GESTURES,
        accentType = AccentType.PRIMARY,
    ),
    DECODER(
        titleResId = R.string.decoder,
        descriptionResId = R.string.decoder_desc,
        drawableResId = R.drawable.sliders,
        setting = Setting.DECODER,
        accentType = AccentType.TERTIARY,
    ),
    AUDIO(
        titleResId = R.string.audio,
        descriptionResId = R.string.audio_desc,
        drawableResId = R.drawable.equalizer,
        setting = Setting.AUDIO,
        accentType = AccentType.SECONDARY,
    ),
    SUBTITLE(
        titleResId = R.string.subtitle,
        descriptionResId = R.string.subtitle_desc,
        drawableResId = R.drawable.language,
        setting = Setting.SUBTITLE,
        accentType = AccentType.PRIMARY,
    ),
    GENERAL(
        titleResId = R.string.general_name,
        descriptionResId = R.string.general_description,
        drawableResId = R.drawable.contrast,
        setting = Setting.GENERAL,
        accentType = AccentType.SECONDARY,
    ),
    STATS(
        title = "Statistics",
        description = "Watch statistics and folder distributions",
        drawableResId = R.drawable.stats,
        setting = Setting.STATS,
        accentType = AccentType.TERTIARY,
    ),
    BACKUP_RESTORE(
        title = "Backup & Restore",
        description = "Export and import app settings and library data",
        drawableResId = R.drawable.backup,
        setting = Setting.BACKUP_RESTORE,
        accentType = AccentType.PRIMARY,
    ),
    STORAGE(
        title = "Storage & Cache",
        description = "Manage media library cache and local storage",
        drawableResId = R.drawable.storage,
        setting = Setting.STORAGE,
        accentType = AccentType.TERTIARY,
    ),
    WATCH_PARTY(
        title = "Watch Party (Sync Play)",
        description = "Watch Telegram Cloud videos synchronously with friends",
        drawableResId = R.drawable.sync,
        setting = Setting.WATCH_PARTY,
        accentType = AccentType.PRIMARY,
    ),
    ABOUT(
        titleResId = R.string.about_name,
        descriptionResId = R.string.about_description,
        drawableResId = R.drawable.info,
        setting = Setting.ABOUT,
        accentType = AccentType.SECONDARY,
    );

    @Composable
    fun accentColor(): Color = when (accentType) {
        AccentType.PRIMARY -> MaterialTheme.colorScheme.primary
        AccentType.SECONDARY -> MaterialTheme.colorScheme.secondary
        AccentType.TERTIARY -> MaterialTheme.colorScheme.tertiary
    }
}

@Composable
private fun SettingsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onOpenLocalVideo: () -> Unit,
    onOpenNetworkStream: () -> Unit,
    onCheckForUpdates: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    if (isDark) {
                        listOf(
                            Color(0xF0181A22),
                            Color(0xF00D0E12),
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    },
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f),
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                        )
                    },
                ),
                shape = RoundedCornerShape(20.dp),
            ),
        containerColor = if (isDark) Color(0xEE14151B) else MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 16.dp,
        tonalElevation = 6.dp,
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.open_local_video)) },
            onClick = onOpenLocalVideo,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.FileOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.open_network_stream)) },
            onClick = onOpenNetworkStream,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        DropdownMenuItem(
            text = { Text(text = "Check for updates") },
            onClick = onCheckForUpdates,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.Update,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

@Composable
private fun SettingsNetworkUrlDialog(
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf("") }
    NextDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.network_stream)) },
        content = {
            Text(text = stringResource(R.string.enter_a_network_url))
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.example_url)) },
            )
        },
        confirmButton = {
            DoneButton(
                enabled = url.isNotBlank(),
                onClick = { onDone(url) },
            )
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSettingsBottomSheet(
    appVersion: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color(0xFF0C1019),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arch_player),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Account Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Subscription & Device Manager",
                            fontSize = 12.sp,
                            color = Color(0xFF828A9E)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = NextIcons.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // VIP Membership Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F1B2B),
                                Color(0xFF0B121E)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF38B6FF).copy(alpha = 0.40f),
                                Color(0xFF0072D2).copy(alpha = 0.20f)
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KRYPTON VIP ACCESS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color(0xFF38B6FF)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF00E676).copy(alpha = 0.18f),
                            border = BorderStroke(0.6.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Lifetime Free • 4K HDR • Zero Ads • Direct Cloud",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Text(
                        text = "• 4K UHD, HDR10+, Dolby Vision playback unlocked\n• High-speed Telegram Cloud streaming enabled\n• Zero ads and lifetime unrestricted access",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFBAC5D6)
                    )
                }
            }

            // Device Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF131722))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Connected Device",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF828A9E)
                    )
                    Text(
                        text = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Next Player v$appVersion • Architecture: ${(Build.SUPPORTED_ABIS?.firstOrNull() ?: "universal").uppercase()}",
                        fontSize = 12.sp,
                        color = Color(0xFF828A9E)
                    )
                }
            }

            // Manage VIP Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0072D2))
                    .border(1.dp, Color(0xFF38B6FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable {
                        android.widget.Toast.makeText(context, "VIP Pro features are 100% Free in Next Player!", android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VIP Membership Active",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
