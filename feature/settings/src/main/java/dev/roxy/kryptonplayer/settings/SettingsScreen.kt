package dev.roxy.kryptonplayer.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.AppUpdateSheet
import dev.roxy.kryptonplayer.core.ui.components.CancelButton
import dev.roxy.kryptonplayer.core.ui.components.DoneButton
import dev.roxy.kryptonplayer.core.ui.components.MochyBottomBlurScrim
import dev.roxy.kryptonplayer.core.ui.components.MochyMotion
import dev.roxy.kryptonplayer.core.ui.components.MochyNavigationDock
import dev.roxy.kryptonplayer.core.ui.components.MochyScrollBlurHeader
import dev.roxy.kryptonplayer.core.ui.components.NextDialog
import dev.roxy.kryptonplayer.core.ui.components.mochyItemPress
import dev.roxy.kryptonplayer.core.ui.components.mochyScrollFadeEdge
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.liquidglass.layerBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.rememberBackdrop

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
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val appVersion = remember { context.appVersion().ifBlank { "0.16" } }
    val primaryAbi = remember {
        when ((Build.SUPPORTED_ABIS?.firstOrNull() ?: "universal").lowercase()) {
            "arm64-v8a" -> "ARM64"
            "armeabi-v7a" -> "ARMv7"
            else -> "UNIVERSAL"
        }
    }

    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var showUpdateSheet by remember { mutableStateOf(false) }
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
                val fallback = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(fallback) }
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumePendingInstall()
    }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateState.Available -> {
                pendingUpdateInfo = state.info
                showUpdateSheet = true
            }
            is UpdateState.Downloading -> {
                if (state.info != null) pendingUpdateInfo = state.info
            }
            is UpdateState.ReadyToInstall -> {
                if (state.info != null) pendingUpdateInfo = state.info
            }
            is UpdateState.UpToDate -> {
                Toast.makeText(context, "You're on the latest version (v$appVersion)", Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateState()
            }
            is UpdateState.Error -> {
                if (state.info != null) {
                    pendingUpdateInfo = state.info
                    showUpdateSheet = true
                } else {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetUpdateState()
                }
            }
            else -> Unit
        }
    }

    val handleUpdateAction: () -> Unit = {
        when (val state = updateState) {
            is UpdateState.Available -> {
                pendingUpdateInfo = state.info
                showUpdateSheet = true
            }
            is UpdateState.Downloading -> showUpdateSheet = true
            is UpdateState.ReadyToInstall -> viewModel.installApk(state.apkUri)
            is UpdateState.Checking -> Unit
            else -> viewModel.checkForUpdates(manual = true)
        }
    }

    if (showUpdateSheet && pendingUpdateInfo != null) {
        val info = pendingUpdateInfo!!
        val apkUri = (updateState as? UpdateState.ReadyToInstall)?.apkUri

        AppUpdateSheet(
            info = info,
            updateState = updateState,
            currentVersionName = appVersion,
            onDownload = { viewModel.downloadAndInstall(info) },
            onDownloadAsset = { asset -> viewModel.downloadAndInstall(info, asset) },
            onInstall = { viewModel.installApk(apkUri) },
            onCancelDownload = { viewModel.cancelDownload() },
            onRemindLater = {
                viewModel.snoozeUpdatePrompt()
                showUpdateSheet = false
            },
            onSkipVersion = {
                viewModel.skipVersion(info.tagName.ifBlank { info.versionName })
                showUpdateSheet = false
            },
            onOpenReleaseUrl = if (info.releaseUrl.isNotBlank()) {
                {
                    runCatching { uriHandler.openUri(info.releaseUrl) }
                }
            } else {
                null
            },
            onDismiss = { showUpdateSheet = false },
        )
    }

    if (showNetworkStreamDialog) {
        SettingsNetworkUrlDialog(
            onDismiss = { showNetworkStreamDialog = false },
            onDone = { url ->
                showNetworkStreamDialog = false
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        setPackage(context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }.onFailure {
                    Toast.makeText(context, "Unable to open stream URL", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    val colorScheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val backdrop = rememberBackdrop(colorScheme.background)

    val scrollFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 110f).coerceIn(0f, 1f)
            }
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
        ) {
            // Live backdrop source layer: scrollable Mochi Hub & Settings list with GPU edge fade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .background(colorScheme.background),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .mochyScrollFadeEdge(topFadeHeight = 18.dp, bottomFadeHeight = 52.dp),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 74.dp,
                        bottom = 128.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item(key = "status_bar_spacer") {
                        Spacer(modifier = Modifier.statusBarsPadding())
                    }

                    // 1. Compact Mochi Brand & Build Hub Card
                    item(key = "mochy_hub_identity_card") {
                        MochyHubIdentityCard(
                            appName = stringResource(id = R.string.app_name),
                            versionName = appVersion,
                            abi = primaryAbi,
                            updateState = updateState,
                            onUpdateClick = handleUpdateAction,
                            onAboutClick = { onItemClick(Setting.ABOUT) },
                            onAppearanceClick = { onItemClick(Setting.APPEARANCE) },
                        )
                    }

                    // Active inline update strip when an update is available / downloading / ready
                    item(key = "update_alert_strip") {
                        val activeUpdateInfo = when (val state = updateState) {
                            is UpdateState.Available -> state.info
                            is UpdateState.Downloading -> state.info
                            is UpdateState.ReadyToInstall -> state.info
                            else -> null
                        }
                        AnimatedVisibility(
                            visible = activeUpdateInfo != null,
                            enter = fadeIn(tween(240, easing = MochyMotion.EaseOut)) +
                                expandVertically(tween(260, easing = MochyMotion.EaseOut)),
                            exit = fadeOut(tween(180, easing = MochyMotion.EaseInOut)) +
                                shrinkVertically(tween(220, easing = MochyMotion.EaseInOut)),
                        ) {
                            activeUpdateInfo?.let { info ->
                                ActiveUpdateStrip(
                                    info = info,
                                    updateState = updateState,
                                    onClick = {
                                        pendingUpdateInfo = info
                                        showUpdateSheet = true
                                    },
                                )
                            }
                        }
                    }

                    // 2. Customisation & Interface
                    item(key = "group_customisation") {
                        SettingsCardGroup(
                            title = "Customisation & Interface",
                            items = listOf(
                                SettingsRowItem(
                                    title = stringResource(id = R.string.appearance_name),
                                    subtitle = "AMOLED black, color palettes, dock & seekbar",
                                    iconRes = R.drawable.palette,
                                    testTag = "setting_appearance",
                                    onClick = { onItemClick(Setting.APPEARANCE) },
                                ),
                                SettingsRowItem(
                                    title = stringResource(id = R.string.media_library),
                                    subtitle = stringResource(id = R.string.media_library_description),
                                    iconRes = R.drawable.history,
                                    testTag = "setting_media_library",
                                    onClick = { onItemClick(Setting.MEDIA_LIBRARY) },
                                ),
                                SettingsRowItem(
                                    title = "Storage & Cache",
                                    subtitle = "Manage downloads, cache & local storage",
                                    iconRes = R.drawable.storage,
                                    testTag = "setting_storage",
                                    onClick = { onItemClick(Setting.STORAGE) },
                                ),
                            ),
                        )
                    }

                    // 3. Playback & App Behaviour
                    item(key = "group_playback") {
                        SettingsCardGroup(
                            title = "Playback & App Behaviour",
                            items = listOf(
                                SettingsRowItem(
                                    title = stringResource(id = R.string.player_name),
                                    subtitle = stringResource(id = R.string.player_description),
                                    iconRes = R.drawable.tune,
                                    testTag = "setting_player",
                                    onClick = { onItemClick(Setting.PLAYER) },
                                ),
                                SettingsRowItem(
                                    title = stringResource(id = R.string.gestures_name),
                                    subtitle = stringResource(id = R.string.gestures_description),
                                    iconRes = R.drawable.swipe,
                                    testTag = "setting_gestures",
                                    onClick = { onItemClick(Setting.GESTURES) },
                                ),
                                SettingsRowItem(
                                    title = stringResource(id = R.string.audio),
                                    subtitle = stringResource(id = R.string.audio_desc),
                                    iconRes = R.drawable.equalizer,
                                    testTag = "setting_audio",
                                    onClick = { onItemClick(Setting.AUDIO) },
                                ),
                                SettingsRowItem(
                                    title = stringResource(id = R.string.decoder),
                                    subtitle = stringResource(id = R.string.decoder_desc),
                                    iconRes = R.drawable.sliders,
                                    testTag = "setting_decoder",
                                    onClick = { onItemClick(Setting.DECODER) },
                                ),
                                SettingsRowItem(
                                    title = stringResource(id = R.string.subtitle),
                                    subtitle = stringResource(id = R.string.subtitle_desc),
                                    iconRes = R.drawable.language,
                                    testTag = "setting_subtitle",
                                    onClick = { onItemClick(Setting.SUBTITLE) },
                                ),
                                SettingsRowItem(
                                    title = "Lyrics",
                                    subtitle = "Synced glow lyrics, style & providers",
                                    iconRes = R.drawable.lyrics,
                                    testTag = "setting_lyrics",
                                    onClick = { onItemClick(Setting.LYRICS) },
                                ),
                                SettingsRowItem(
                                    title = "Watch Party",
                                    subtitle = "Telegram Cloud & synchronized playback",
                                    iconRes = R.drawable.sync,
                                    testTag = "setting_watch_party",
                                    onClick = { onItemClick(Setting.WATCH_PARTY) },
                                ),
                            ),
                        )
                    }

                    // 4. System & Data
                    item(key = "group_system") {
                        SettingsCardGroup(
                            title = "System & Data",
                            items = listOf(
                                SettingsRowItem(
                                    title = stringResource(id = R.string.general_name),
                                    subtitle = stringResource(id = R.string.general_description),
                                    iconRes = R.drawable.contrast,
                                    testTag = "setting_general",
                                    onClick = { onItemClick(Setting.GENERAL) },
                                ),
                                SettingsRowItem(
                                    title = "Statistics",
                                    subtitle = "Watch history & folder breakdown",
                                    iconRes = R.drawable.stats,
                                    testTag = "setting_stats",
                                    onClick = { onItemClick(Setting.STATS) },
                                ),
                                SettingsRowItem(
                                    title = "Backup & Restore",
                                    subtitle = "Export, import & migrate preferences",
                                    iconRes = R.drawable.backup,
                                    testTag = "setting_backup_restore",
                                    onClick = { onItemClick(Setting.BACKUP_RESTORE) },
                                ),
                            ),
                        )
                    }

                    // 5. Credits, Support & About
                    item(key = "group_credits_about") {
                        val isChecking = updateState is UpdateState.Checking
                        val updateSubtitle = when (val state = updateState) {
                            is UpdateState.Checking -> "Checking for updates..."
                            is UpdateState.Available -> "Update v${state.info.versionName} available"
                            is UpdateState.Downloading -> {
                                if (state.progress in 0..100) "Downloading ${state.progress}%" else "Downloading update..."
                            }
                            is UpdateState.ReadyToInstall -> "Tap to install downloaded update"
                            else -> "Current version v$appVersion"
                        }

                        SettingsCardGroup(
                            title = "Credits, Support & About",
                            items = listOf(
                                SettingsRowItem(
                                    title = "Updates",
                                    subtitle = updateSubtitle,
                                    iconVector = Icons.Rounded.SystemUpdate,
                                    isLoading = isChecking,
                                    testTag = "setting_check_updates",
                                    onClick = handleUpdateAction,
                                ),
                                SettingsRowItem(
                                    title = stringResource(id = R.string.about_name),
                                    subtitle = "Lead developer, contributors, support & licenses",
                                    iconRes = R.drawable.info,
                                    testTag = "setting_about",
                                    onClick = { onItemClick(Setting.ABOUT) },
                                ),
                            ),
                        )
                    }
                }
            }

            // Floating scroll-reactive Mochi frosted glass top header
            MochyScrollBlurHeader(
                scrollFraction = scrollFraction,
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                val easedScroll = MochyMotion.EaseOut.transform(scrollFraction)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .align(Alignment.Center)
                        .height(64.dp)
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Settings",
                        fontSize = (28f - 6f * easedScroll).sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = (-0.5f + 0.2f * easedScroll).sp,
                        modifier = Modifier.graphicsLayer {
                            translationY = (1f - easedScroll) * 2f
                        },
                    )

                    Box {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                            border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
                        ) {
                            IconButton(
                                onClick = { isMoreMenuExpanded = true },
                                modifier = Modifier.testTag("settings_more_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Quick actions",
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        SettingsQuickMenu(
                            expanded = isMoreMenuExpanded,
                            onDismissRequest = { isMoreMenuExpanded = false },
                            onOpenLocalVideo = {
                                isMoreMenuExpanded = false
                                selectVideoFileLauncher.launch("video/*")
                            },
                            onOpenNetworkStream = {
                                isMoreMenuExpanded = false
                                showNetworkStreamDialog = true
                            },
                            onCheckForUpdates = {
                                isMoreMenuExpanded = false
                                viewModel.checkForUpdates(manual = true)
                            },
                        )
                    }
                }
            }

            // Progressive bottom depth scrim + real GPU-blurred floating Mochi navigation dock
            MochyBottomBlurScrim(
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            MochyNavigationDock(
                selectedTabIndex = 3,
                onTabSelected = { index ->
                    when (index) {
                        0 -> onNavigateToHome()
                        1 -> onSearchClick()
                        2 -> onNavigateToExplore()
                        3 -> Unit
                    }
                },
                onSearchDoubleTap = onSearchClick,
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun MochyHubIdentityCard(
    appName: String,
    versionName: String,
    abi: String,
    updateState: UpdateState,
    onUpdateClick: () -> Unit,
    onAboutClick: () -> Unit,
    onAppearanceClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isChecking = updateState is UpdateState.Checking
    val updatePillLabel = when (val state = updateState) {
        is UpdateState.Checking -> "Checking"
        is UpdateState.Available -> "v${state.info.versionName}"
        is UpdateState.Downloading -> if (state.progress in 0..100) "${state.progress}%" else "Updating"
        is UpdateState.ReadyToInstall -> "Install"
        else -> "Updates"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mochy_hub_identity_card"),
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = colorScheme.surfaceContainerHigh,
                    border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_settings_filled),
                            contentDescription = appName,
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(25.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MochyHubMetaPill(text = "v$versionName")
                        MochyHubMetaPill(text = abi)
                        MochyHubMetaPill(text = "AMOLED")
                    }
                }

                val updateInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = onUpdateClick,
                    interactionSource = updateInteraction,
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .mochyItemPress(updateInteraction, pressedScale = 0.96f)
                        .testTag("mochy_hub_update_pill"),
                    shape = CircleShape,
                    color = if (updateState is UpdateState.Available || updateState is UpdateState.ReadyToInstall) {
                        colorScheme.primary
                    } else {
                        colorScheme.surfaceContainerHigh
                    },
                    border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val onPill = if (updateState is UpdateState.Available || updateState is UpdateState.ReadyToInstall) {
                            colorScheme.onPrimary
                        } else {
                            colorScheme.onSurface
                        }
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 1.8.dp,
                                color = onPill,
                            )
                        } else {
                            Icon(
                                imageVector = if (updateState is UpdateState.ReadyToInstall) {
                                    Icons.Rounded.CheckCircle
                                } else {
                                    Icons.Rounded.SystemUpdate
                                },
                                contentDescription = null,
                                tint = onPill,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            text = updatePillLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onPill,
                            maxLines = 1,
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.6.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.7f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MochyHubQuickPill(
                    label = "Theme & Look",
                    iconRes = R.drawable.palette,
                    onClick = onAppearanceClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mochy_hub_quick_theme"),
                )
                MochyHubQuickPill(
                    label = "Credits & Support",
                    iconRes = R.drawable.info,
                    onClick = onAboutClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mochy_hub_quick_credits"),
                )
            }
        }
    }
}

@Composable
private fun MochyHubMetaPill(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun MochyHubQuickPill(
    label: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = 42.dp)
            .mochyItemPress(interactionSource, pressedScale = 0.97f),
        shape = RoundedCornerShape(14.dp),
        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
        border = BorderStroke(0.75.dp, colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActiveUpdateStrip(
    info: AppUpdateInfo,
    updateState: UpdateState,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val subtitle = when (updateState) {
        is UpdateState.Downloading -> {
            if (updateState.progress in 0..100) "Downloading (${updateState.progress}%)" else "Downloading update..."
        }
        is UpdateState.ReadyToInstall -> "Verified & ready to install"
        else -> "Tap to view changelog and install"
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .mochyItemPress(interactionSource, pressedScale = 0.98f)
            .testTag("settings_update_banner"),
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.9.dp, colorScheme.primary.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (updateState is UpdateState.ReadyToInstall) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.SystemUpdate
                },
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(21.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Update v${info.versionName} Available",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private data class SettingsRowItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int = 0,
    val iconVector: ImageVector? = null,
    val isLoading: Boolean = false,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
private fun SettingsCardGroup(
    title: String,
    items: List<SettingsRowItem>,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = colorScheme.surfaceContainerLow,
            border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .mochyItemPress(interactionSource, pressedScale = 0.985f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = item.onClick,
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag(item.testTag),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                item.isLoading -> CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colorScheme.primary,
                                )
                                item.iconRes != 0 -> Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp),
                                )
                                item.iconVector != null -> Icon(
                                    imageVector = item.iconVector,
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                            thickness = 0.6.dp,
                            color = colorScheme.outlineVariant.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsQuickMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onOpenLocalVideo: () -> Unit,
    onOpenNetworkStream: () -> Unit,
    onCheckForUpdates: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.open_local_video)) },
            onClick = onOpenLocalVideo,
            colors = MenuDefaults.itemColors(
                textColor = colorScheme.onSurface,
                leadingIconColor = colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.FileOpen,
                    contentDescription = null,
                )
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.open_network_stream)) },
            onClick = onOpenNetworkStream,
            colors = MenuDefaults.itemColors(
                textColor = colorScheme.onSurface,
                leadingIconColor = colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.Link,
                    contentDescription = null,
                )
            },
        )
        DropdownMenuItem(
            text = { Text(text = "Check for updates") },
            onClick = onCheckForUpdates,
            colors = MenuDefaults.itemColors(
                textColor = colorScheme.onSurface,
                leadingIconColor = colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.Update,
                    contentDescription = null,
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
