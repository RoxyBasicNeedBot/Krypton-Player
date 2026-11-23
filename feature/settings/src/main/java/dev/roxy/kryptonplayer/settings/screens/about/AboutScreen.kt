package dev.roxy.kryptonplayer.settings.screens.about

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.GitHubCommitInfo
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.AppUpdateFormattedChangelog
import dev.roxy.kryptonplayer.core.ui.components.AppUpdateSheet
import dev.roxy.kryptonplayer.core.ui.components.LaserAppLogoBadge
import dev.roxy.kryptonplayer.core.ui.components.MochyMotion
import dev.roxy.kryptonplayer.core.ui.components.MochyScrollBlurHeader
import dev.roxy.kryptonplayer.core.ui.components.mochyItemPress
import dev.roxy.kryptonplayer.core.ui.components.mochyScrollFadeEdge
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.liquidglass.layerBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.rememberBackdrop
import dev.roxy.kryptonplayer.core.ui.preview.DayNightPreview
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private const val GITHUB_REPO_URL = "https://github.com/RoxyBasicNeedBot/Krypton-Player"
private const val GITHUB_CONTRIBUTORS_URL = "https://github.com/RoxyBasicNeedBot/Krypton-Player/graphs/contributors"
private const val GITHUB_ISSUES_URL = "https://github.com/RoxyBasicNeedBot/Krypton-Player/issues"
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

    val (currentVersionCode, currentVersionName) = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
            val name = pInfo.versionName ?: "1.0.0"
            Pair(code, name)
        } catch (_: Exception) {
            Pair(1, "1.0.0")
        }
    }

    val isAppDebuggable = remember {
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    val primaryAbi = remember {
        (Build.SUPPORTED_ABIS?.firstOrNull() ?: "universal").uppercase()
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumePendingInstall()
    }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateState.Available -> {
                pendingUpdateInfo = state.info
                showUpdateDialog = true
            }
            is UpdateState.Downloading -> {
                if (state.info != null) pendingUpdateInfo = state.info
            }
            is UpdateState.ReadyToInstall -> {
                if (state.info != null) pendingUpdateInfo = state.info
            }
            is UpdateState.UpToDate -> {
                showUpToDateDialog = true
            }
            is UpdateState.Error -> {
                if (state.info != null) {
                    pendingUpdateInfo = state.info
                    showUpdateDialog = true
                } else {
                    updateErrorMessage = state.message
                    showUpdateErrorDialog = true
                }
            }
            else -> Unit
        }
    }

    // Update Bottom Sheet
    if (showUpdateDialog && pendingUpdateInfo != null) {
        val info = pendingUpdateInfo!!
        val apkUri = (updateState as? UpdateState.ReadyToInstall)?.apkUri

        AppUpdateSheet(
            info = info,
            updateState = updateState,
            currentVersionName = currentVersionName,
            onDownload = { viewModel.downloadAndInstall(info) },
            onDownloadAsset = { asset -> viewModel.downloadAndInstall(info, asset) },
            onInstall = { viewModel.installApk(apkUri) },
            onCancelDownload = { viewModel.cancelDownload() },
            onRemindLater = {
                viewModel.snoozeUpdatePrompt()
                showUpdateDialog = false
            },
            onSkipVersion = {
                viewModel.skipVersion(info.tagName.ifBlank { info.versionName })
                showUpdateDialog = false
            },
            onOpenReleaseUrl = if (info.releaseUrl.isNotBlank()) {
                { uriHandler.openUriOrToast(info.releaseUrl, context) }
            } else null,
            onDismiss = { showUpdateDialog = false },
        )
    }

    // Up-to-Date Dialog
    if (showUpToDateDialog) {
        AlertDialog(
            onDismissRequest = { showUpToDateDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
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
                        text = "Krypton Player is running the latest release.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AboutMetadataBadge(text = "v$currentVersionName (#$currentVersionCode)")
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
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    }

    // Update Error Dialog
    if (showUpdateErrorDialog) {
        AlertDialog(
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
                            modifier = Modifier.size(26.dp),
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
                    text = updateErrorMessage.ifBlank { "Unable to reach update servers. Please check your connection." },
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
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    }

    // Changelog Modal Bottom Sheet
    if (showChangelogSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showChangelogSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
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

    Scaffold(
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
        ) {
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
                        .mochyScrollFadeEdge(topFadeHeight = 16.dp, bottomFadeHeight = 36.dp),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 74.dp,
                        bottom = 48.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item(key = "about_status_spacer") {
                        Spacer(modifier = Modifier.statusBarsPadding())
                    }

                    // 1. Mochi Brand & Build Identity Card
                    item(key = "about_identity") {
                        AboutIdentityCard(
                            appName = stringResource(id = R.string.app_name),
                            versionName = currentVersionName,
                            versionCode = currentVersionCode,
                            isDebug = isAppDebuggable,
                            abi = primaryAbi,
                            onOpenGitHub = { uriHandler.openUriOrToast(GITHUB_REPO_URL, context) },
                            onOpenTelegram = { uriHandler.openUriOrToast(TELEGRAM_URL, context) },
                            onOpenBoost = { uriHandler.openUriOrToast(TELEGRAM_BOOST_URL, context) },
                            onOpenChangelog = {
                                viewModel.loadChangelog(forceRefresh = false)
                                viewModel.loadCommits(forceRefresh = false)
                                showChangelogSheet = true
                            },
                        )
                    }

                    // 2. Lead Developer Card
                    item(key = "about_lead_dev") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AboutSectionHeader(title = "Lead Developer")
                            LeadDeveloperCard(
                                developerName = stringResource(id = R.string.app_developer),
                                onOpenTelegram = { uriHandler.openUriOrToast(TELEGRAM_URL, context) },
                                onOpenGitHub = { uriHandler.openUriOrToast(GITHUB_REPO_URL, context) },
                            )
                        }
                    }

                    // 3. Contributors & Acknowledgements
                    item(key = "about_contributors") {
                        AboutCardGroup(
                            title = "Contributors & Credits",
                            items = listOf(
                                AboutRowItem(
                                    title = "Community Contributors",
                                    subtitle = "Open-source PRs, localizations & bug fixes on GitHub",
                                    iconVector = Icons.Rounded.Groups,
                                    testTag = "about_contributors_github",
                                    onClick = { uriHandler.openUriOrToast(GITHUB_CONTRIBUTORS_URL, context) },
                                ),
                                AboutRowItem(
                                    title = "Open-Source Ecosystem",
                                    subtitle = "Built with Media3, FFmpeg, Next Player & Mochi UI craft",
                                    iconVector = Icons.Rounded.Code,
                                    testTag = "about_ecosystem_credits",
                                    onClick = onLibrariesClick,
                                ),
                            ),
                        )
                    }

                    // 4. Updates & Changelog
                    item(key = "about_updates") {
                        AboutCardGroup(
                            title = "Updates & Changelog",
                            items = listOf(
                                AboutRowItem(
                                    title = "Changelog",
                                    subtitle = "Release history & recent GitHub commits",
                                    iconRes = R.drawable.history,
                                    badge = "v$currentVersionName",
                                    testTag = "about_changelog",
                                    onClick = {
                                        viewModel.loadChangelog(forceRefresh = false)
                                        viewModel.loadCommits(forceRefresh = false)
                                        showChangelogSheet = true
                                    },
                                ),
                                AboutRowItem(
                                    title = "Check for updates",
                                    subtitle = updateSubtitle,
                                    iconVector = updateCardIcon,
                                    iconRotation = syncRotation,
                                    badge = if (isUpdateAvail) "NEW" else null,
                                    highlightBadge = isUpdateAvail,
                                    testTag = "about_check_updates",
                                    onClick = {
                                        when (val state = updateState) {
                                            is UpdateState.ReadyToInstall -> viewModel.installApk(state.apkUri)
                                            is UpdateState.Available -> {
                                                pendingUpdateInfo = state.info
                                                showUpdateDialog = true
                                            }
                                            is UpdateState.Downloading -> showUpdateDialog = true
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
                                ),
                            ),
                        )
                    }

                    // 5. Community & Support
                    item(key = "about_community") {
                        AboutCardGroup(
                            title = "Community & Support",
                            items = listOf(
                                AboutRowItem(
                                    title = "Telegram Community",
                                    subtitle = "Join @KryptonPlayer for support & discussion",
                                    iconRes = R.drawable.telegram,
                                    testTag = "about_telegram",
                                    onClick = { uriHandler.openUriOrToast(TELEGRAM_URL, context) },
                                ),
                                AboutRowItem(
                                    title = "Telegram Boost (/boost)",
                                    subtitle = "Support channel to unlock high-speed cloud limits",
                                    iconRes = R.drawable.bolt,
                                    testTag = "about_boost",
                                    onClick = { uriHandler.openUriOrToast(TELEGRAM_BOOST_URL, context) },
                                ),
                                AboutRowItem(
                                    title = "Guides & Bot (/learn)",
                                    subtitle = "Cloud streaming tutorial, bot commands & setup guide",
                                    iconRes = R.drawable.ic_telegram,
                                    testTag = "about_guides",
                                    onClick = { uriHandler.openUriOrToast(TELEGRAM_LEARN_URL, context) },
                                ),
                                AboutRowItem(
                                    title = "GitHub Repository & Issues",
                                    subtitle = "Source code, releases & bug reports",
                                    iconRes = R.drawable.ic_github,
                                    testTag = "about_github",
                                    onClick = { uriHandler.openUriOrToast(GITHUB_ISSUES_URL, context) },
                                ),
                            ),
                        )
                    }

                    // 6. Open-Source Licenses
                    item(key = "about_licenses") {
                        AboutCardGroup(
                            title = stringResource(id = R.string.libraries),
                            items = listOf(
                                AboutRowItem(
                                    title = stringResource(id = R.string.libraries),
                                    subtitle = "Open-source libraries & licenses used in Krypton Player",
                                    iconVector = NextIcons.Info,
                                    testTag = "about_libraries",
                                    onClick = onLibrariesClick,
                                ),
                            ),
                        )
                    }

                    item(key = "about_nav_spacer") {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }

            // Floating Mochi scroll-reactive frosted glass header
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                        border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
                    ) {
                        IconButton(
                            onClick = onNavigateUp,
                            modifier = Modifier.testTag("about_back_button"),
                        ) {
                            Icon(
                                imageVector = NextIcons.ArrowBack,
                                contentDescription = stringResource(id = R.string.navigate_up),
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Text(
                        text = stringResource(id = R.string.about_name),
                        fontSize = (24f - 4f * easedScroll).sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = (-0.4f + 0.15f * easedScroll).sp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutIdentityCard(
    appName: String,
    versionName: String,
    versionCode: Int,
    isDebug: Boolean,
    abi: String,
    onOpenGitHub: () -> Unit,
    onOpenTelegram: () -> Unit,
    onOpenBoost: () -> Unit,
    onOpenChangelog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LaserAppLogoBadge(
                containerSize = 80.dp,
                logoSize = 56.dp,
                cornerRadius = 22.dp,
                borderWidth = 1.dp,
                animateOnEntry = true,
                allowTapToReplay = true,
            )

            Text(
                text = appName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                ),
                color = colorScheme.onSurface,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AboutMetadataBadge(text = "v$versionName")
                if (versionCode > 0) AboutMetadataBadge(text = "#$versionCode")
                AboutMetadataBadge(text = if (isDebug) "Debug" else "Release")
                if (abi.isNotBlank()) AboutMetadataBadge(text = abi)
            }

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.65f),
                thickness = 0.6.dp,
                modifier = Modifier.padding(vertical = 2.dp),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MochyAboutChip(label = "GitHub", iconRes = R.drawable.ic_github, onClick = onOpenGitHub)
                MochyAboutChip(label = "Telegram", iconRes = R.drawable.ic_telegram, onClick = onOpenTelegram)
                MochyAboutChip(label = "Boost", iconRes = R.drawable.bolt, onClick = onOpenBoost)
                MochyAboutChip(label = "What's New", iconRes = R.drawable.history, onClick = onOpenChangelog)
            }
        }
    }
}

@Composable
private fun LeadDeveloperCard(
    developerName: String,
    onOpenTelegram: () -> Unit,
    onOpenGitHub: () -> Unit,
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
                AsyncImage(
                    model = R.drawable.dev_profile,
                    contentDescription = "Lead Developer Profile",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = colorScheme.outline,
                            shape = CircleShape,
                        ),
                    contentScale = ContentScale.Crop,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = developerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Lead Developer & Maintainer",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AboutMetadataBadge(text = "Core Architecture & UI")
                }
            }

            HorizontalDivider(
                thickness = 0.6.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.65f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MochyAboutChip(
                    label = "Telegram",
                    iconRes = R.drawable.ic_telegram,
                    onClick = onOpenTelegram,
                    modifier = Modifier.weight(1f),
                )
                MochyAboutChip(
                    label = "GitHub",
                    iconRes = R.drawable.ic_github,
                    onClick = onOpenGitHub,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MochyAboutChip(
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
            .heightIn(min = 38.dp)
            .mochyItemPress(interactionSource, pressedScale = 0.96f),
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.75.dp, colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = colorScheme.onSurface,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
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
private fun AboutMetadataBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.heightIn(min = 24.dp),
        shape = RoundedCornerShape(8.dp),
        color = colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.6.dp, colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun AboutSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
    )
}

private data class AboutRowItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int = 0,
    val iconVector: ImageVector? = null,
    val iconRotation: Float = 0f,
    val badge: String? = null,
    val highlightBadge: Boolean = false,
    val testTag: String = "",
    val onClick: () -> Unit,
)

@Composable
private fun AboutCardGroup(
    title: String,
    items: List<AboutRowItem>,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AboutSectionHeader(title = title)
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
                            .then(if (item.testTag.isNotBlank()) Modifier.testTag(item.testTag) else Modifier),
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
                                item.iconRes != 0 -> Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(19.dp)
                                        .graphicsLayer { rotationZ = item.iconRotation },
                                )
                                item.iconVector != null -> Icon(
                                    imageVector = item.iconVector,
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(19.dp)
                                        .graphicsLayer { rotationZ = item.iconRotation },
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
                            if (item.subtitle.isNotBlank()) {
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (item.badge != null) {
                            Surface(
                                shape = CircleShape,
                                color = if (item.highlightBadge) colorScheme.primary else colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = item.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.highlightBadge) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
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

/**
 * Compatibility helper retained for any callers referencing AboutSegmentedCard.
 */
@Composable
fun AboutSegmentedCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    iconDrawableRes: Int? = null,
    iconUrl: Any? = null,
    iconRotation: Float = 0f,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    badge: String? = null,
    showUpdateIndicator: Boolean = false,
    index: Int = 0,
    count: Int = 1,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .mochyItemPress(interactionSource, pressedScale = 0.985f),
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    iconUrl != null -> AsyncImage(
                        model = iconUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    icon != null -> Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colorScheme.onSurface,
                        modifier = Modifier
                            .size(19.dp)
                            .graphicsLayer { rotationZ = iconRotation },
                    )
                    iconDrawableRes != null -> Icon(
                        painter = painterResource(id = iconDrawableRes),
                        contentDescription = null,
                        tint = colorScheme.onSurface,
                        modifier = Modifier
                            .size(19.dp)
                            .graphicsLayer { rotationZ = iconRotation },
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = if (showUpdateIndicator) colorScheme.primary else colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (showUpdateIndicator) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

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
                AboutMetadataBadge(text = "v$currentVersionName")
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
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                            AboutMetadataBadge(text = "INSTALLED")
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
                                            AboutMetadataBadge(text = chipLabel)
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
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ) {
                                            Text(
                                                text = commit.shortSha,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
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
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.error_opening_link), Toast.LENGTH_SHORT).show()
    }
}

@DayNightPreview
@Composable
private fun AboutSegmentedCardPreview() {
    KryptonPlayerTheme {
        AboutSegmentedCard(
            title = "Source Code",
            subtitle = "GitHub Repository",
            onClick = {},
        )
    }
}
