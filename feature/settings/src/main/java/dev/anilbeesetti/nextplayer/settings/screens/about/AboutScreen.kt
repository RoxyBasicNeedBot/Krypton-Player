package dev.anilbeesetti.nextplayer.settings.screens.about

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.anilbeesetti.nextplayer.core.common.extensions.appIcon
import dev.anilbeesetti.nextplayer.core.data.update.AppUpdateInfo
import dev.anilbeesetti.nextplayer.core.data.update.GitHubCommitInfo
import dev.anilbeesetti.nextplayer.core.data.update.UpdateState
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.AppUpdateDialog
import dev.anilbeesetti.nextplayer.core.ui.components.AppUpdateFormattedChangelog
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private const val GITHUB_REPO_URL = "https://github.com/RoxyBasicNeedBot/Krypton-Player"
private const val TELEGRAM_URL = "https://t.me/KryptonPlayer"
private const val TELEGRAM_BOOST_URL = "https://t.me/boost/KryptonPlayer"
private const val TELEGRAM_LEARN_URL = "https://t.me/KryptonPlayer"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    onLibrariesClick: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val appIcon = remember { context.appIcon()?.asImageBitmap() }
    val (currentVersionCode, currentVersionName) = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                pInfo.versionCode
            }
            val name = pInfo.versionName ?: "1.0.0"
            Pair(code, name)
        } catch (_: Exception) {
            Pair(1, "1.0.0")
        }
    }

    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val changelogReleases by viewModel.changelogReleases.collectAsStateWithLifecycle()
    val isLoadingChangelog by viewModel.isLoadingChangelog.collectAsStateWithLifecycle()
    val gitHubCommits by viewModel.gitHubCommits.collectAsStateWithLifecycle()
    val isLoadingCommits by viewModel.isLoadingCommits.collectAsStateWithLifecycle()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var pendingUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showChangelogSheet by remember { mutableStateOf(false) }
    var showUpToDateDialog by remember { mutableStateOf(false) }
    var showUpdateErrorDialog by remember { mutableStateOf(false) }
    var updateErrorMessage by remember { mutableStateOf("") }

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
                }
            }
            is UpdateState.UpToDate -> {
                showUpToDateDialog = true
            }
            is UpdateState.Error -> {
                updateErrorMessage = state.message
                showUpdateErrorDialog = true
            }
            else -> Unit
        }
    }

    // App Update Dialog popup (ArchiveTune-style)
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
            currentVersionName = currentVersionName,
            fileSize = info.fileSize,
            releaseDate = info.releaseDate,
            downloadProgress = downloadProgress,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            isReadyToInstall = isReady,
            apkAssets = info.apkAssets,
            commits = gitHubCommits,
            isUpToDate = !info.isUpdateAvailable,
            onDownload = {
                viewModel.downloadAndInstall(info)
            },
            onDownloadAsset = { asset ->
                viewModel.downloadAndInstall(info, asset)
            },
            onInstall = {
                viewModel.installApk(apkUri)
            },
            onOpenReleaseUrl = if (info.releaseUrl.isNotBlank()) {
                { uriHandler.openUriOrToast(info.releaseUrl, context) }
            } else null,
            onDismiss = {
                showUpdateDialog = false
            },
        )
    }

    // ArchiveTune-style Up-to-Date Dialog
    if (showUpToDateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUpToDateDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "You're up to date",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Krypton Player is at the latest version available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = "v$currentVersionName (#$currentVersionCode)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (pendingUpdateInfo != null) {
                        TextButton(onClick = {
                            showUpToDateDialog = false
                            showUpdateDialog = true
                        }) {
                            Text("Download APK")
                        }
                    }
                    Button(onClick = { showUpToDateDialog = false }) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }

    // ArchiveTune-style Update Error Dialog
    if (showUpdateErrorDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUpdateErrorDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SyncProblem,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Update Check Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Text(
                    text = updateErrorMessage.ifBlank { "Unable to connect to update servers. Please check your internet connection." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateErrorDialog = false
                    scope.launch {
                        val info = viewModel.checkForUpdates(currentVersionCode, currentVersionName, manual = true)
                        if (info != null) {
                            pendingUpdateInfo = info
                            showUpdateDialog = true
                        } else {
                            updateErrorMessage = (viewModel.updateState.value as? UpdateState.Error)?.message
                                ?: "Unable to reach update servers. Please check your connection."
                            showUpdateErrorDialog = true
                        }
                    }
                }) {
                    Text("Retry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateErrorDialog = false }) {
                    Text("Dismiss")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }

    // ArchiveTune-style Changelog Modal Bottom Sheet
    if (showChangelogSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showChangelogSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            ChangelogSheetContent(
                releases = changelogReleases,
                commits = gitHubCommits,
                currentVersionName = currentVersionName,
                isLoading = isLoadingChangelog,
                isLoadingCommits = isLoadingCommits,
                onRefresh = { viewModel.loadChangelog(forceRefresh = true) },
                onRefreshCommits = { viewModel.loadCommits(forceRefresh = true) },
                onUpdateClick = { release ->
                    pendingUpdateInfo = release
                    showChangelogSheet = false
                    showUpdateDialog = true
                },
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showChangelogSheet = false
                    }
                },
            )
        }
    }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.about_name),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // 1. Hero Identity Card (ArchiveTune AboutIdentityCard)
            val isAppDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val primaryAbi = (Build.SUPPORTED_ABIS?.firstOrNull() ?: "universal").uppercase()

            AboutIdentityCard(
                appName = stringResource(id = R.string.app_name),
                versionName = currentVersionName,
                versionCode = currentVersionCode,
                isDebug = isAppDebuggable,
                abi = primaryAbi,
                appIcon = appIcon,
                onOpenGitHub = { uriHandler.openUriOrToast(GITHUB_REPO_URL, context) },
                onOpenTelegram = { uriHandler.openUriOrToast(TELEGRAM_URL, context) },
                onOpenBoost = { uriHandler.openUriOrToast(TELEGRAM_BOOST_URL, context) },
                onOpenChangelog = {
                    viewModel.loadChangelog(forceRefresh = false)
                    viewModel.loadCommits(forceRefresh = false)
                    showChangelogSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(24.dp))

            // 2. Section: Updates & Changelog
            AboutSectionHeader(
                title = "Updates & Changelog",
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))

            val infiniteTransition = rememberInfiniteTransition(label = "checkUpdateAnim")
            val checkingRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "syncRotation",
            )
            val isChecking = updateState is UpdateState.Checking
            val syncRotation = if (isChecking) checkingRotation else 0f

            val updateCardIcon = when (updateState) {
                is UpdateState.UpToDate -> Icons.Rounded.CheckCircle
                is UpdateState.Available -> Icons.Rounded.Download
                is UpdateState.Downloading -> Icons.Rounded.Download
                is UpdateState.ReadyToInstall -> Icons.Rounded.CheckCircle
                is UpdateState.Error -> Icons.Rounded.SyncProblem
                else -> NextIcons.Sync
            }

            val updateAccentColor = when (updateState) {
                is UpdateState.UpToDate -> Color(0xFF4CAF50)
                is UpdateState.ReadyToInstall -> Color(0xFF2E7D32)
                is UpdateState.Available -> MaterialTheme.colorScheme.tertiary
                is UpdateState.Error -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }

            val updateSubtitle = when (val state = updateState) {
                is UpdateState.Checking -> "Checking for updates..."
                is UpdateState.Available -> "Version ${state.info.versionName} available — Tap to download"
                is UpdateState.UpToDate -> "Krypton Player is up to date (v$currentVersionName)"
                is UpdateState.Downloading -> "Downloading update: ${state.progress}%"
                is UpdateState.ReadyToInstall -> "Update downloaded — Tap to install"
                is UpdateState.Error -> "Check failed: ${state.message} — Tap to retry"
                else -> "Tap to check for new release"
            }

            val isUpdateAvail = updateState is UpdateState.Available

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Changelog item
                AboutSegmentedCard(
                    title = "Changelog",
                    subtitle = "View release history & recent changes",
                    iconDrawableRes = R.drawable.history,
                    accentColor = MaterialTheme.colorScheme.primary,
                    index = 0,
                    count = 2,
                    badge = "v$currentVersionName",
                    onClick = {
                        viewModel.loadChangelog(forceRefresh = false)
                        showChangelogSheet = true
                    },
                )

                // Check for Updates item
                AboutSegmentedCard(
                    title = "Check for updates",
                    subtitle = updateSubtitle,
                    icon = updateCardIcon,
                    iconRotation = syncRotation,
                    accentColor = updateAccentColor,
                    badge = if (isUpdateAvail) "NEW" else null,
                    showUpdateIndicator = isUpdateAvail,
                    index = 1,
                    count = 2,
                    onClick = {
                        when (val state = updateState) {
                            is UpdateState.ReadyToInstall -> {
                                viewModel.installApk(state.apkUri)
                            }
                            is UpdateState.Available -> {
                                pendingUpdateInfo = state.info
                                showUpdateDialog = true
                            }
                            is UpdateState.Downloading -> {
                                showUpdateDialog = true
                            }
                            is UpdateState.Checking -> Unit
                            else -> {
                                scope.launch {
                                    val info = viewModel.checkForUpdates(currentVersionCode, currentVersionName, manual = true)
                                    if (info != null) {
                                        if (info.isUpdateAvailable) {
                                            pendingUpdateInfo = info
                                            showUpdateDialog = true
                                        } else {
                                            showUpToDateDialog = true
                                        }
                                    } else {
                                        val err = (viewModel.updateState.value as? UpdateState.Error)?.message
                                        if (!err.isNullOrBlank()) {
                                            updateErrorMessage = err
                                            showUpdateErrorDialog = true
                                        } else if (viewModel.updateState.value is UpdateState.UpToDate) {
                                            showUpToDateDialog = true
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }

            Spacer(Modifier.height(24.dp))

            // 3. Section: Project Information & Licenses
            AboutSectionHeader(
                title = stringResource(id = R.string.libraries),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))

            AboutSegmentedCard(
                title = stringResource(id = R.string.libraries),
                subtitle = "Open source libraries used in Krypton Player",
                icon = NextIcons.Info,
                accentColor = MaterialTheme.colorScheme.secondary,
                index = 0,
                count = 1,
                onClick = onLibrariesClick,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(24.dp))

            // 4. Section: Community & Support
            AboutSectionHeader(
                title = "Community & Support",
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AboutSegmentedCard(
                    title = "Telegram Community Group",
                    subtitle = "Join @KryptonPlayer for support & discussion",
                    iconDrawableRes = R.drawable.telegram,
                    accentColor = MaterialTheme.colorScheme.primary,
                    index = 0,
                    count = 4,
                    onClick = { uriHandler.openUriOrToast(TELEGRAM_URL, context) },
                )
                AboutSegmentedCard(
                    title = "Telegram Boost (/boost)",
                    subtitle = "Boost channel to unlock high-speed cloud limits",
                    iconDrawableRes = R.drawable.bolt,
                    accentColor = Color(0xFFFFB300),
                    index = 1,
                    count = 4,
                    onClick = { uriHandler.openUriOrToast(TELEGRAM_BOOST_URL, context) },
                )
                AboutSegmentedCard(
                    title = "Guides & Bot (/learn)",
                    subtitle = "Cloud streaming tutorial, bot commands & setup guide",
                    iconDrawableRes = R.drawable.ic_telegram,
                    accentColor = Color(0xFF0288D1),
                    index = 2,
                    count = 4,
                    onClick = { uriHandler.openUriOrToast(TELEGRAM_LEARN_URL, context) },
                )
                AboutSegmentedCard(
                    title = "GitHub Repository",
                    subtitle = "Source code, release notes, and issue tracker",
                    iconDrawableRes = R.drawable.ic_github,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    index = 3,
                    count = 4,
                    onClick = { uriHandler.openUriOrToast(GITHUB_REPO_URL, context) },
                )
            }

            Spacer(Modifier.height(24.dp))

            // 5. Section: Lead Developer Card (ArchiveTune Team Member style)
            AboutSectionHeader(
                title = "Developer",
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AsyncImage(
                        model = R.drawable.dev_profile,
                        contentDescription = "Developer Profile",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ),
                        contentScale = ContentScale.Crop,
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(id = R.string.app_developer),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Lead Developer & Maintainer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = { uriHandler.openUriOrToast(TELEGRAM_URL, context) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_telegram),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = { Text("Telegram") },
                            shape = RoundedCornerShape(12.dp),
                        )
                        AssistChip(
                            onClick = { uriHandler.openUriOrToast(GITHUB_REPO_URL, context) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_github),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = { Text("GitHub") },
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * ArchiveTune-style Hero Identity Card displaying app icon, dynamic versioning badges,
 * and quick link chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutIdentityCard(
    appName: String,
    versionName: String,
    versionCode: Int,
    isDebug: Boolean,
    abi: String,
    appIcon: androidx.compose.ui.graphics.ImageBitmap?,
    onOpenGitHub: () -> Unit,
    onOpenTelegram: () -> Unit,
    onOpenBoost: () -> Unit,
    onOpenChangelog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Animated Hero App Logo (3-Stage Magic Animation)
            dev.anilbeesetti.nextplayer.core.ui.components.LaserAppLogoBadge(
                containerSize = 86.dp,
                logoSize = 62.dp,
                cornerRadius = 24.dp,
                borderWidth = 1.dp,
                animateOnEntry = true,
                allowTapToReplay = true,
            )

            // App Name
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.25.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Dynamic Metadata Badges (ArchiveTune FlowRow)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AboutMetadataBadge(text = "v$versionName")
                if (versionCode > 0) {
                    AboutMetadataBadge(text = "#$versionCode")
                }
                AboutMetadataBadge(
                    text = if (isDebug) "Debug" else "Release",
                )
                if (abi.isNotBlank()) {
                    AboutMetadataBadge(text = abi)
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 4.dp),
            )

            // Primary Links AssistChips Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AssistChip(
                    onClick = onOpenGitHub,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text("GitHub") },
                    shape = RoundedCornerShape(12.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                )
                AssistChip(
                    onClick = onOpenTelegram,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_telegram),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text("Telegram") },
                    shape = RoundedCornerShape(12.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                )
                AssistChip(
                    onClick = onOpenBoost,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.bolt),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFB300),
                        )
                    },
                    label = { Text("Boost") },
                    shape = RoundedCornerShape(12.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                )
                AssistChip(
                    onClick = onOpenChangelog,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.history),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text("What's New") },
                    shape = RoundedCornerShape(12.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                )
            }
        }
    }
}

/**
 * ArchiveTune-style metadata badge pill.
 */
@Composable
private fun AboutMetadataBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 28.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * ArchiveTune-style Section Header.
 */
@Composable
private fun AboutSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.3f,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * ArchiveTune-style Segmented Item Card with custom curved shapes per index.
 */
@Composable
fun AboutSegmentedCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    iconDrawableRes: Int? = null,
    iconUrl: Any? = null,
    iconRotation: Float = 0f,
    accentColor: Color,
    badge: String? = null,
    showUpdateIndicator: Boolean = false,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = remember(index, count) { segmentedCardShape(index, count) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "aboutCardScale",
    )
    val iconContentColor = contentColorFor(accentColor).let {
        if (it == Color.Unspecified) Color.White else it
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .focusable()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                if (showUpdateIndicator) {
                    androidx.compose.material3.BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(9.dp),
                            )
                        },
                    ) {
                        RenderCardIcon(icon, iconDrawableRes, iconUrl, iconRotation, iconContentColor)
                    }
                } else {
                    RenderCardIcon(icon, iconDrawableRes, iconUrl, iconRotation, iconContentColor)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (badge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (showUpdateIndicator) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (showUpdateIndicator) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = NextIcons.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RenderCardIcon(
    icon: ImageVector?,
    iconDrawableRes: Int?,
    iconUrl: Any?,
    iconRotation: Float,
    iconContentColor: Color,
) {
    if (iconUrl != null) {
        AsyncImage(
            model = iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    } else if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconContentColor,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer { rotationZ = iconRotation },
        )
    } else if (iconDrawableRes != null) {
        Icon(
            painter = painterResource(id = iconDrawableRes),
            contentDescription = null,
            tint = iconContentColor,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer { rotationZ = iconRotation },
        )
    }
}

private fun segmentedCardShape(index: Int, count: Int): Shape {
    val large = 28.dp
    val small = 6.dp
    return when {
        count <= 1 -> RoundedCornerShape(large)
        index == 0 -> RoundedCornerShape(
            topStart = large,
            topEnd = large,
            bottomEnd = small,
            bottomStart = small,
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = small,
            topEnd = small,
            bottomEnd = large,
            bottomStart = large,
        )
        else -> RoundedCornerShape(small)
    }
}

/**
 * ArchiveTune-style Changelog Bottom Sheet Content.
 */
@Composable
private fun ChangelogSheetContent(
    releases: List<AppUpdateInfo>,
    commits: List<GitHubCommitInfo>,
    currentVersionName: String,
    isLoading: Boolean,
    isLoadingCommits: Boolean,
    onRefresh: () -> Unit,
    onRefreshCommits: () -> Unit,
    onUpdateClick: (AppUpdateInfo) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var selectedTab by remember { mutableStateOf(0) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 20.dp),
    ) {
        // Sheet Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Release Notes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "Current: v$currentVersionName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Segmented Tab Selector (Releases / Recent Commits)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Tab 0: Releases
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Releases",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (releases.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = "${releases.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // Tab 1: Recent Commits
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recent Commits",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (commits.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = "${commits.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // TAB 0: RELEASES
            if (isLoading && releases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (releases.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "No changelog entries found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(releases) { release ->
                        val formattedDate = remember(release.releaseDate) {
                            try {
                                if (release.releaseDate.length >= 10) {
                                    val d = dateFormat.parse(release.releaseDate.substring(0, 10))
                                    d?.let { displayDateFormat.format(it) } ?: release.releaseDate
                                } else {
                                    release.releaseDate
                                }
                            } catch (_: Exception) {
                                release.releaseDate
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "v${release.versionName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        if (release.versionName == currentVersionName) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                            ) {
                                                Text(
                                                    text = "INSTALLED",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                )
                                            }
                                        }
                                    }

                                    if (formattedDate.isNotBlank()) {
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                if (release.changelog.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AppUpdateFormattedChangelog(changelog = release.changelog)
                                }

                                if (release.apkAssets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        release.apkAssets.forEach { asset ->
                                            val chipLabel = when (asset.abi.lowercase()) {
                                                "arm64-v8a", "arm64" -> "ARM64"
                                                "armeabi-v7a", "armv7" -> "ARMv7"
                                                "universal" -> "Universal"
                                                else -> asset.abi.uppercase()
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (asset.isRecommended) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            ) {
                                                Text(
                                                    text = chipLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = if (asset.isRecommended) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (asset.isRecommended) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                if (release.isUpdateAvailable) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { onUpdateClick(release) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Update to v${release.versionName}")
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { onUpdateClick(release) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download APK (v${release.versionName})")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // TAB 1: RECENT COMMITS
            if (isLoadingCommits && commits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (commits.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "No recent commits found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRefreshCommits,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(commits) { commit ->
                        val formattedDate = remember(commit.date) {
                            try {
                                if (commit.date.length >= 10) {
                                    val d = dateFormat.parse(commit.date.substring(0, 10))
                                    d?.let { displayDateFormat.format(it) } ?: commit.date
                                } else {
                                    commit.date
                                }
                            } catch (_: Exception) {
                                commit.date
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false),
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                        ) {
                                            Text(
                                                text = commit.shortSha,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = commit.authorName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    if (formattedDate.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = commit.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(
                                        onClick = {
                                            if (commit.commitUrl.isNotBlank()) {
                                                uriHandler.openUriOrToast(commit.commitUrl, context)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.OpenInBrowser,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View on GitHub")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun UriHandler.openUriOrToast(uri: String, context: Context) {
    try {
        openUri(uri = uri)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.error_opening_link), Toast.LENGTH_SHORT).show()
    }
}
