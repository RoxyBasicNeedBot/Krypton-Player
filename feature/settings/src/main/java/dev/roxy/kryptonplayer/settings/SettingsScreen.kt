package dev.roxy.kryptonplayer.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import dev.roxy.kryptonplayer.core.ui.components.MochyMotion
import dev.roxy.kryptonplayer.core.ui.components.NextDialog
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.settings.composables.AllSettingsContent
import dev.roxy.kryptonplayer.settings.composables.MochyHubContent

@OptIn(ExperimentalMaterial3Api::class)
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

    val userHubState by viewModel.userHubState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var showAllSettings by rememberSaveable { mutableStateOf(false) }
    var showUpdateSheet by remember { mutableStateOf(false) }
    var pendingUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isMoreMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showNetworkStreamDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showAllSettings) {
        showAllSettings = false
    }

    val openUriSafely: (String) -> Unit = { url ->
        runCatching { uriHandler.openUri(url) }.onFailure {
            Toast.makeText(context, context.getString(R.string.error_opening_link), Toast.LENGTH_SHORT).show()
        }
    }

    val playVideoUri: (Uri) -> Unit = { uri ->
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

    val selectVideoFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            playVideoUri(uri)
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
                { openUriSafely(info.releaseUrl) }
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

    val quickMenu: @Composable () -> Unit = {
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        AnimatedContent(
            targetState = showAllSettings,
            transitionSpec = {
                if (targetState) {
                    // Forward: 4th Profile/Stats Tab -> All Settings
                    (slideInHorizontally(tween(300, easing = MochyMotion.EaseOut)) { it / 3 } +
                        fadeIn(tween(240, easing = MochyMotion.EaseOut)))
                        .togetherWith(
                            scaleOut(tween(240, easing = MochyMotion.EaseInOut), targetScale = 0.95f) +
                                fadeOut(tween(180, easing = MochyMotion.EaseInOut)),
                        )
                } else {
                    // Back: All Settings -> 4th Profile/Stats Tab
                    (scaleIn(tween(280, easing = MochyMotion.EaseOut), initialScale = 0.95f) +
                        fadeIn(tween(220, easing = MochyMotion.EaseOut)))
                        .togetherWith(
                            slideOutHorizontally(tween(260, easing = MochyMotion.EaseInOut)) { it / 3 } +
                                fadeOut(tween(180, easing = MochyMotion.EaseInOut)),
                        )
                }
            },
            label = "mochy_profile_settings_transition",
        ) { inAllSettings ->
            if (!inAllSettings) {
                MochyHubContent(
                    userState = userHubState,
                    onOpenSettings = { showAllSettings = true },
                    onOpenDetailedStats = { onItemClick(Setting.STATS) },
                    onPlayVideo = { video ->
                        val parsed = runCatching { Uri.parse(video.uriString) }.getOrNull()
                        if (parsed != null && video.uriString.isNotBlank()) {
                            playVideoUri(parsed)
                        }
                    },
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToExplore = onNavigateToExplore,
                    onSearchClick = onSearchClick,
                )
            } else {
                AllSettingsContent(
                    appVersion = appVersion,
                    updateState = updateState,
                    onBackToHub = { showAllSettings = false },
                    onItemClick = onItemClick,
                    onUpdateAction = handleUpdateAction,
                    onMoreMenuClick = { isMoreMenuExpanded = true },
                    moreMenuContent = quickMenu,
                )
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
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
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
