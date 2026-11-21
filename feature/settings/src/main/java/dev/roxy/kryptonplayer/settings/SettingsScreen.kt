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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Verified
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import dev.roxy.kryptonplayer.core.ui.components.MochyNavigationDock
import dev.roxy.kryptonplayer.core.ui.components.NextDialog
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons

private const val GITHUB_REPO_URL = "https://github.com/RoxyBasicNeedBot/Krypton-Player"
private const val GITHUB_PROFILE_URL = "https://github.com/RoxyBasicNeedBot"
private const val GITHUB_ISSUES_URL = "https://github.com/RoxyBasicNeedBot/Krypton-Player/issues"
private const val GITHUB_CONTRIBUTORS_URL = "https://github.com/RoxyBasicNeedBot/Krypton-Player/graphs/contributors"
private const val UPSTREAM_NEXTPLAYER_URL = "https://github.com/anilbeesetti/nextplayer"
private const val MEDIA3_URL = "https://github.com/androidx/media"
private const val TELEGRAM_COMMUNITY_URL = "https://t.me/KryptonPlayer"
private const val TELEGRAM_BOOST_URL = "https://t.me/boost/KryptonPlayer"

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
    val primaryAbi = remember { (Build.SUPPORTED_ABIS?.firstOrNull() ?: "universal").uppercase() }

    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var showUpdateSheet by remember { mutableStateOf(false) }
    var pendingUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isMoreMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showNetworkStreamDialog by rememberSaveable { mutableStateOf(false) }

    fun openExternalUrl(url: String) {
        runCatching {
            uriHandler.openUri(url)
        }.onFailure {
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

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
                { openExternalUrl(info.releaseUrl) }
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

    Scaffold(
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
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
            )
        },
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 8.dp,
                    bottom = 124.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // 1. Minimalist Obsidian Top Bar
                item(key = "settings_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Krypton",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface,
                                letterSpacing = (-0.5).sp,
                            )
                            Text(
                                text = "Settings, Engine & Credits",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.surfaceContainer,
                                border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
                            ) {
                                IconButton(
                                    onClick = onSearchClick,
                                    modifier = Modifier.testTag("settings_search_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Box {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.surfaceContainer,
                                    border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
                                ) {
                                    IconButton(
                                        onClick = { isMoreMenuExpanded = true },
                                        modifier = Modifier.testTag("settings_more_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = "More actions",
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
                }

                // 2. Krypton Identity & Update Hero Card
                item(key = "krypton_hero_card") {
                    KryptonHeroCard(
                        appVersion = appVersion,
                        primaryAbi = primaryAbi,
                        updateState = updateState,
                        onCheckUpdateClick = {
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
                        },
                        onAboutClick = { onItemClick(Setting.ABOUT) },
                    )
                }

                // 3. Active Update Banner (shown when update is available / downloading / ready)
                item(key = "update_status_banner") {
                    val activeUpdateInfo = when (val state = updateState) {
                        is UpdateState.Available -> state.info
                        is UpdateState.Downloading -> state.info
                        is UpdateState.ReadyToInstall -> state.info
                        else -> null
                    }
                    AnimatedVisibility(
                        visible = activeUpdateInfo != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
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

                // 4. Personalization & Library
                item(key = "group_personalization") {
                    MochiSettingsSection(
                        title = "Personalization & Library",
                        items = listOf(
                            MochiSettingEntry(
                                title = stringResource(id = R.string.appearance_name),
                                subtitle = "Obsidian theme, palettes, nav bar & sliders",
                                iconRes = R.drawable.palette,
                                testTag = "setting_appearance",
                                onClick = { onItemClick(Setting.APPEARANCE) },
                            ),
                            MochiSettingEntry(
                                title = stringResource(id = R.string.media_library),
                                subtitle = stringResource(id = R.string.media_library_description),
                                iconRes = R.drawable.history,
                                testTag = "setting_media_library",
                                onClick = { onItemClick(Setting.MEDIA_LIBRARY) },
                            ),
                            MochiSettingEntry(
                                title = "Storage & Cache",
                                subtitle = "Manage downloads, thumbnail cache & local files",
                                iconRes = R.drawable.storage,
                                testTag = "setting_storage",
                                onClick = { onItemClick(Setting.STORAGE) },
                            ),
                        ),
                    )
                }

                // 5. Playback & Media Engine
                item(key = "group_playback") {
                    MochiSettingsSection(
                        title = "Playback & Media Engine",
                        items = listOf(
                            MochiSettingEntry(
                                title = stringResource(id = R.string.player_name),
                                subtitle = stringResource(id = R.string.player_description),
                                iconRes = R.drawable.tune,
                                testTag = "setting_player",
                                onClick = { onItemClick(Setting.PLAYER) },
                            ),
                            MochiSettingEntry(
                                title = stringResource(id = R.string.gestures_name),
                                subtitle = stringResource(id = R.string.gestures_description),
                                iconRes = R.drawable.swipe,
                                testTag = "setting_gestures",
                                onClick = { onItemClick(Setting.GESTURES) },
                            ),
                            MochiSettingEntry(
                                title = stringResource(id = R.string.audio),
                                subtitle = stringResource(id = R.string.audio_desc),
                                iconRes = R.drawable.equalizer,
                                testTag = "setting_audio",
                                onClick = { onItemClick(Setting.AUDIO) },
                            ),
                            MochiSettingEntry(
                                title = stringResource(id = R.string.decoder),
                                subtitle = stringResource(id = R.string.decoder_desc),
                                iconRes = R.drawable.sliders,
                                testTag = "setting_decoder",
                                onClick = { onItemClick(Setting.DECODER) },
                            ),
                            MochiSettingEntry(
                                title = stringResource(id = R.string.subtitle),
                                subtitle = stringResource(id = R.string.subtitle_desc),
                                iconRes = R.drawable.language,
                                testTag = "setting_subtitle",
                                onClick = { onItemClick(Setting.SUBTITLE) },
                            ),
                            MochiSettingEntry(
                                title = "Synced Lyrics",
                                subtitle = "Live time-synced lyrics, styling & providers",
                                iconRes = R.drawable.lyrics,
                                testTag = "setting_lyrics",
                                onClick = { onItemClick(Setting.LYRICS) },
                            ),
                            MochiSettingEntry(
                                title = "Watch Party & Cloud",
                                subtitle = "Telegram MTProto streaming & synchronized playback",
                                iconRes = R.drawable.sync,
                                testTag = "setting_watch_party",
                                onClick = { onItemClick(Setting.WATCH_PARTY) },
                            ),
                        ),
                    )
                }

                // 6. System & Data
                item(key = "group_system") {
                    MochiSettingsSection(
                        title = "System & Data",
                        items = listOf(
                            MochiSettingEntry(
                                title = stringResource(id = R.string.general_name),
                                subtitle = stringResource(id = R.string.general_description),
                                iconRes = R.drawable.contrast,
                                testTag = "setting_general",
                                onClick = { onItemClick(Setting.GENERAL) },
                            ),
                            MochiSettingEntry(
                                title = "Watch Statistics",
                                subtitle = "Playback history insights & folder breakdown",
                                iconRes = R.drawable.stats,
                                testTag = "setting_stats",
                                onClick = { onItemClick(Setting.STATS) },
                            ),
                            MochiSettingEntry(
                                title = "Backup & Restore",
                                subtitle = "Export or import app preferences & library state",
                                iconRes = R.drawable.backup,
                                testTag = "setting_backup_restore",
                                onClick = { onItemClick(Setting.BACKUP_RESTORE) },
                            ),
                        ),
                    )
                }

                // 7. Lead Developer Spotlight
                item(key = "lead_developer_card") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MochiSectionTitle("Lead Developer")
                        LeadDeveloperCard(
                            developerName = stringResource(id = R.string.app_developer),
                            onGitHubClick = { openExternalUrl(GITHUB_PROFILE_URL) },
                            onTelegramClick = { openExternalUrl(TELEGRAM_COMMUNITY_URL) },
                        )
                    }
                }

                // 8. Developers & Contributors
                item(key = "contributors_card") {
                    MochiContributorsSection(
                        onOpenUrl = ::openExternalUrl,
                    )
                }

                // 9. Community, Support & About
                item(key = "support_card") {
                    MochiSettingsSection(
                        title = "Support & Community",
                        items = listOf(
                            MochiSettingEntry(
                                title = "Telegram Support Community",
                                subtitle = "Join @KryptonPlayer for support, builds & feedback",
                                iconRes = R.drawable.ic_telegram,
                                trailingExternal = true,
                                testTag = "support_telegram",
                                onClick = { openExternalUrl(TELEGRAM_COMMUNITY_URL) },
                            ),
                            MochiSettingEntry(
                                title = "Boost Channel",
                                subtitle = "Support Krypton Player on Telegram (/boost)",
                                iconRes = R.drawable.bolt,
                                trailingExternal = true,
                                testTag = "support_boost",
                                onClick = { openExternalUrl(TELEGRAM_BOOST_URL) },
                            ),
                            MochiSettingEntry(
                                title = "Report a Bug or Issue",
                                subtitle = "Track bugs, crashes & feature requests on GitHub",
                                iconVector = Icons.Rounded.BugReport,
                                trailingExternal = true,
                                testTag = "support_github_issues",
                                onClick = { openExternalUrl(GITHUB_ISSUES_URL) },
                            ),
                            MochiSettingEntry(
                                title = "Source Code (GPL-3.0)",
                                subtitle = "RoxyBasicNeedBot/Krypton-Player on GitHub",
                                iconRes = R.drawable.ic_github,
                                trailingExternal = true,
                                testTag = "support_github_repo",
                                onClick = { openExternalUrl(GITHUB_REPO_URL) },
                            ),
                            MochiSettingEntry(
                                title = stringResource(id = R.string.about_name),
                                subtitle = "Changelog, commit history & open-source licenses",
                                iconRes = R.drawable.info,
                                testTag = "setting_about",
                                onClick = { onItemClick(Setting.ABOUT) },
                            ),
                        ),
                    )
                }

                // 10. Clean Footer
                item(key = "krypton_footer") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Krypton Player v$appVersion",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Crafted by ${stringResource(id = R.string.app_developer)} • Android ${Build.VERSION.RELEASE} • $primaryAbi",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KryptonHeroCard(
    appVersion: String,
    primaryAbi: String,
    updateState: UpdateState,
    onCheckUpdateClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isChecking = updateState is UpdateState.Checking
    val updateLabel = when (updateState) {
        is UpdateState.Checking -> "Checking..."
        is UpdateState.Available -> "Update v${updateState.info.versionName}"
        is UpdateState.Downloading -> {
            if (updateState.progress in 0..100) "Downloading ${updateState.progress}%" else "Downloading..."
        }
        is UpdateState.ReadyToInstall -> "Install Update"
        else -> "Check Updates"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                            colorScheme.surfaceContainerLow,
                        ),
                    ),
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceContainerHighest)
                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arch_player),
                        contentDescription = stringResource(id = R.string.app_name),
                        tint = colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "v$appVersion",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Text(
                        text = "Android ${Build.VERSION.RELEASE} • $primaryAbi • Native TDLib",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = onCheckUpdateClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("settings_check_updates_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = updateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    onClick = onAboutClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("settings_changelog_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.surfaceContainerHigh,
                    contentColor = colorScheme.onSurface,
                    border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.info),
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
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
    val subtitle = when (updateState) {
        is UpdateState.Downloading -> {
            if (updateState.progress in 0..100) "Downloading update (${updateState.progress}%)" else "Downloading update..."
        }
        is UpdateState.ReadyToInstall -> "Verified & ready to install"
        else -> "Tap to view changelog and install"
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_update_banner"),
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(0.9.dp, colorScheme.tertiary.copy(alpha = 0.35f)),
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
                tint = colorScheme.tertiary,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Krypton v${info.versionName} Available",
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

@Composable
private fun LeadDeveloperCard(
    developerName: String,
    onGitHubClick: () -> Unit,
    onTelegramClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dev_profile),
                    contentDescription = "$developerName avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, colorScheme.primary.copy(alpha = 0.65f), CircleShape),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = developerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                        )
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified lead developer",
                            tint = colorScheme.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = "Lead Developer & Creator",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        text = "@RoxyBasicNeedBot",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "Lead architect and maintainer behind Krypton Player — building the native TDLib MTProto 2.0 streaming engine, Media3 HDR playback pipeline, and obsidian interface.",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = onGitHubClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("lead_dev_github_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.surfaceContainerHigh,
                    border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = null,
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GitHub",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Surface(
                    onClick = onTelegramClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("lead_dev_telegram_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.surfaceContainerHigh,
                    border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_telegram),
                            contentDescription = null,
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Telegram",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

private data class ContributorEntry(
    val name: String,
    val role: String,
    val badge: String,
    val url: String,
    val icon: ImageVector,
)

@Composable
private fun MochiContributorsSection(
    onOpenUrl: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val contributors = remember {
        listOf(
            ContributorEntry(
                name = "ROXY (@RoxyBasicNeedBot)",
                role = "Lead Developer, UI Architecture & TDLib Core",
                badge = "Lead",
                url = GITHUB_PROFILE_URL,
                icon = Icons.Rounded.Verified,
            ),
            ContributorEntry(
                name = "Anil Beesetti",
                role = "Upstream Next Player & nextlib FFmpeg Decoders",
                badge = "Core Base",
                url = UPSTREAM_NEXTPLAYER_URL,
                icon = Icons.Rounded.Code,
            ),
            ContributorEntry(
                name = "AndroidX Media3 Team",
                role = "ExoPlayer Adaptive Playback & HDR Engine",
                badge = "Engine",
                url = MEDIA3_URL,
                icon = Icons.Rounded.Code,
            ),
            ContributorEntry(
                name = "Open-Source Community",
                role = "Translations, Bug Reports, Testing & Pull Requests",
                badge = "Contributors",
                url = GITHUB_CONTRIBUTORS_URL,
                icon = Icons.Rounded.Groups,
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MochiSectionTitle("Developers & Contributors")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = colorScheme.surfaceContainerLow,
            border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                contributors.forEachIndexed { index, contributor ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clickable { onOpenUrl(contributor.url) }
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                            .testTag("contributor_item_$index"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = contributor.icon,
                                contentDescription = null,
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(19.dp),
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = contributor.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colorScheme.surfaceContainerHighest,
                                ) {
                                    Text(
                                        text = contributor.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(
                                text = contributor.role,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = "Open link",
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    if (index < contributors.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 70.dp, end = 16.dp),
                            thickness = 0.6.dp,
                            color = colorScheme.outlineVariant.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }
    }
}

private data class MochiSettingEntry(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int = 0,
    val iconVector: ImageVector? = null,
    val trailingExternal: Boolean = false,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
private fun MochiSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 6.dp),
    )
}

@Composable
private fun MochiSettingsSection(
    title: String,
    items: List<MochiSettingEntry>,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MochiSectionTitle(title)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = colorScheme.surfaceContainerLow,
            border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 62.dp)
                            .clickable(onClick = item.onClick)
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                            .testTag(item.testTag),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                item.iconRes != 0 -> Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                                item.iconVector != null -> Icon(
                                    imageVector = item.iconVector,
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
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
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Icon(
                            imageVector = if (item.trailingExternal) {
                                Icons.Rounded.OpenInNew
                            } else {
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight
                            },
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.size(if (item.trailingExternal) 16.dp else 20.dp),
                        )
                    }

                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 70.dp, end = 16.dp),
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
