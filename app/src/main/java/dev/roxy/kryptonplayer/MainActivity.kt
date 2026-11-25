@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.roxy.kryptonplayer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import dagger.hilt.android.AndroidEntryPoint
import dev.roxy.kryptonplayer.core.common.hasMediaAccessPermission
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.storagePermissions
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.AppUpdateManager
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import dev.roxy.kryptonplayer.core.media.services.MediaService
import dev.roxy.kryptonplayer.core.media.sync.MediaSynchronizer
import dev.roxy.kryptonplayer.core.ui.components.AppPermissionBottomSheet
import dev.roxy.kryptonplayer.core.ui.components.AppPermissionType
import dev.roxy.kryptonplayer.core.ui.components.AppUpdateSheet
import dev.roxy.kryptonplayer.core.ui.components.MochyMotion
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.core.ui.theme.LocalPlaybackController
import dev.roxy.kryptonplayer.crash.CrashBreadcrumbs
import dev.roxy.kryptonplayer.feature.player.PlayerActivity
import dev.roxy.kryptonplayer.feature.videopicker.composables.MiniPlayer
import dev.roxy.kryptonplayer.navigation.MediaRootRoute
import dev.roxy.kryptonplayer.navigation.mediaNavGraph
import dev.roxy.kryptonplayer.navigation.settingsNavGraph
import dev.roxy.kryptonplayer.onboarding.KryptonOnboardingFlow
import dev.roxy.kryptonplayer.onboarding.KryptonOnboardingSheetShell
import dev.roxy.kryptonplayer.settings.navigation.settingsNavigationRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var synchronizer: MediaSynchronizer

    @Inject
    lateinit var mediaService: MediaService

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    @Inject
    lateinit var playbackController: PlaybackController

    private val viewModel: MainViewModel by viewModels()

    private val sharedUrlFlow = MutableStateFlow<String?>(null)

    private fun extractUrl(text: String?): String? {
        if (text == null) return null
        val regex = "(https?://[^\\s]+)".toRegex()
        val matchResult = regex.find(text)
        return matchResult?.value
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val url = extractUrl(sharedText)
            if (url != null) {
                sharedUrlFlow.value = url
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (hasMediaAccessPermission()) {
            synchronizer.startSync()
        }
        if (appUpdateManager.canRequestPackageInstalls() && appUpdateManager.hasPendingInstall()) {
            appUpdateManager.resumePendingInstall()
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaService.initialize(this@MainActivity)
        handleIntent(intent)

        setContent {
            val appUiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
            val shouldUseDarkTheme = shouldUseDarkTheme(uiState = appUiState)

            val updateState by viewModel.updateState.collectAsStateWithLifecycle()
            var showStartupUpdateDialog by remember { mutableStateOf(false) }
            var pendingStartupUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
            var dismissedVersion by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(updateState, isOnboardingCompleted) {
                when (val state = updateState) {
                    is UpdateState.Available -> {
                        pendingStartupUpdateInfo = state.info
                        if (isOnboardingCompleted &&
                            !viewModel.isUpdatePromptSnoozed() &&
                            dismissedVersion != state.info.versionName
                        ) {
                            showStartupUpdateDialog = true
                        }
                    }
                    is UpdateState.Downloading -> {
                        if (state.info != null) {
                            pendingStartupUpdateInfo = state.info
                        }
                        showStartupUpdateDialog = true
                    }
                    is UpdateState.ReadyToInstall -> {
                        if (state.info != null) {
                            pendingStartupUpdateInfo = state.info
                        }
                        showStartupUpdateDialog = true
                    }
                    is UpdateState.Verifying -> {
                        showStartupUpdateDialog = true
                    }
                    is UpdateState.Error -> {
                        if (state.info != null) {
                            pendingStartupUpdateInfo = state.info
                            showStartupUpdateDialog = true
                        }
                    }
                    is UpdateState.Idle, is UpdateState.UpToDate, is UpdateState.Checking -> {
                        // Keep sheet closed
                    }
                }
            }

            LaunchedEffect(shouldUseDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { shouldUseDarkTheme },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { shouldUseDarkTheme },
                    ),
                )
            }

            KryptonPlayerTheme(
                darkTheme = shouldUseDarkTheme,
                highContrastDarkTheme = shouldUseHighContrastDarkTheme(uiState = appUiState),
                dynamicColor = shouldUseDynamicTheming(uiState = appUiState),
                customThemeValue = customThemeValue(uiState = appUiState),
                pureBlack = shouldUsePureBlack(uiState = appUiState),
                useSystemFont = shouldUseSystemFont(uiState = appUiState),
                enableMeshBackground = shouldEnableMeshBackground(uiState = appUiState),
            ) {
                // Expressive Material 3 update bottom sheet
                if (showStartupUpdateDialog && pendingStartupUpdateInfo != null) {
                    val info = pendingStartupUpdateInfo!!
                    val apkUri = (updateState as? UpdateState.ReadyToInstall)?.apkUri

                    val currentVersionName = remember {
                        try {
                            val pInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
                            pInfo.versionName ?: "1.0.0"
                        } catch (_: Exception) {
                            "1.0.0"
                        }
                    }

                    AppUpdateSheet(
                        info = info,
                        updateState = updateState,
                        currentVersionName = currentVersionName,
                        onDownload = {
                            viewModel.downloadAndInstall(info)
                        },
                        onDownloadAsset = { asset ->
                            viewModel.downloadAndInstall(info, asset)
                        },
                        onInstall = {
                            viewModel.installApk(apkUri)
                        },
                        onDismiss = {
                            dismissedVersion = info.versionName
                            showStartupUpdateDialog = false
                        },
                        onRemindLater = {
                            viewModel.snoozeUpdatePrompt()
                            dismissedVersion = info.versionName
                            showStartupUpdateDialog = false
                        },
                        onSkipVersion = {
                            viewModel.skipVersion(info.tagName.ifBlank { info.versionName })
                            dismissedVersion = info.versionName
                            showStartupUpdateDialog = false
                        },
                        onCancelDownload = {
                            viewModel.cancelDownload()
                        },
                        onOpenReleaseUrl = if (info.releaseUrl.isNotBlank()) {
                            {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        } else null,
                    )
                }

                CompositionLocalProvider(LocalPlaybackController provides playbackController) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        val permPrefs = remember { getSharedPreferences("krypton_permission_prefs", Context.MODE_PRIVATE) }
                        var activePermissionSheetType by remember { mutableStateOf<AppPermissionType?>(null) }
                        var isCurrentSheetRePermission by remember { mutableStateOf(false) }

                        val storagePermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestMultiplePermissions()
                        ) { _ ->
                            if (hasMediaAccessPermission()) {
                                synchronizer.startSync()
                                lifecycleScope.launch(Dispatchers.IO) {
                                    synchronizer.refresh()
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val notifGranted = ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!notifGranted) {
                                        val requestedNotif = permPrefs.getBoolean("has_requested_notifications", false)
                                        val isRePerm = requestedNotif && !ActivityCompat.shouldShowRequestPermissionRationale(
                                            this@MainActivity,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                        isCurrentSheetRePermission = isRePerm
                                        activePermissionSheetType = AppPermissionType.NOTIFICATION
                                    }
                                }
                            }
                        }

                        val notificationPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { _ ->
                            activePermissionSheetType = null
                        }

                        fun checkPermissionsState() {
                            if (!viewModel.isOnboardingCompleted.value) {
                                activePermissionSheetType = null
                                return
                            }
                            val storageGranted = hasMediaAccessPermission()

                            if (!storageGranted) {
                                val requestedStorage = permPrefs.getBoolean("has_requested_storage", false)
                                val isRePerm = requestedStorage && storagePermissions.none { perm ->
                                    ActivityCompat.shouldShowRequestPermissionRationale(
                                        this@MainActivity,
                                        perm
                                    )
                                }
                                isCurrentSheetRePermission = isRePerm
                                activePermissionSheetType = AppPermissionType.STORAGE
                            } else {
                                synchronizer.startSync()
                                lifecycleScope.launch(Dispatchers.IO) {
                                    synchronizer.refresh()
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val notifGranted = ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!notifGranted) {
                                        val requestedNotif = permPrefs.getBoolean("has_requested_notifications", false)
                                        val isRePerm = requestedNotif && !ActivityCompat.shouldShowRequestPermissionRationale(
                                            this@MainActivity,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                        isCurrentSheetRePermission = isRePerm
                                        activePermissionSheetType = AppPermissionType.NOTIFICATION
                                    } else {
                                        activePermissionSheetType = null
                                    }
                                } else {
                                    activePermissionSheetType = null
                                }
                            }
                        }

                        LifecycleEventEffect(event = Lifecycle.Event.ON_START) {
                            checkPermissionsState()
                        }

                        if (isOnboardingCompleted) {
                            activePermissionSheetType?.let { sheetType ->
                                AppPermissionBottomSheet(
                                    type = sheetType,
                                    isRePermission = isCurrentSheetRePermission,
                                    onGrant = {
                                        activePermissionSheetType = null
                                        if (sheetType == AppPermissionType.STORAGE) {
                                            permPrefs.edit().putBoolean("has_requested_storage", true).apply()
                                            storagePermissionLauncher.launch(storagePermissions)
                                        } else {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                permPrefs.edit().putBoolean("has_requested_notifications", true).apply()
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                    },
                                    onDismiss = {
                                        activePermissionSheetType = null
                                    }
                                )
                            }
                        }

                        val mainNavController = rememberNavController()

                        DisposableEffect(mainNavController) {
                            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                                val route = destination.route?.substringAfterLast('.') ?: destination.displayName
                                CrashBreadcrumbs.recordNav(route)
                            }
                            mainNavController.addOnDestinationChangedListener(listener)
                            onDispose {
                                mainNavController.removeOnDestinationChangedListener(listener)
                            }
                        }

                        // Shared URL handling disabled since yt-dlp downloader is removed

                        val playerPrefs = (appUiState as? MainActivityUiState.Success)?.playerPreferences

                        KryptonOnboardingSheetShell(
                            showOnboarding = !isOnboardingCompleted,
                            onboardingContent = {
                                KryptonOnboardingFlow(
                                    gesturesEnabled = (playerPrefs?.enableVolumeSwipeGesture ?: true) &&
                                        (playerPrefs?.enableBrightnessSwipeGesture ?: true),
                                    lyricsBlurEnabled = playerPrefs?.lyricsLineBlur ?: true,
                                    autoPipEnabled = playerPrefs?.autoPip ?: true,
                                    onGesturesChanged = viewModel::setGestureControlsEnabled,
                                    onLyricsBlurChanged = viewModel::setLyricsBlurEnabled,
                                    onAutoPipChanged = viewModel::setAutoPipEnabled,
                                    onStorageGranted = {
                                        synchronizer.startSync()
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            synchronizer.refresh()
                                        }
                                    },
                                    onComplete = {
                                        viewModel.completeOnboarding()
                                    },
                                )
                            },
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                            NavHost(
                                navController = mainNavController,
                                startDestination = MediaRootRoute,
                                enterTransition = {
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec = tween(
                                            durationMillis = 280,
                                            easing = MochyMotion.EaseOut,
                                        ),
                                        initialOffset = { fullOffset -> (fullOffset * 0.22f).toInt() },
                                    ) + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 220,
                                            easing = MochyMotion.EaseOut,
                                        ),
                                    ) + scaleIn(
                                        initialScale = 0.985f,
                                        animationSpec = tween(
                                            durationMillis = 280,
                                            easing = MochyMotion.EaseOut,
                                        ),
                                    )
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec = tween(
                                            durationMillis = 260,
                                            easing = MochyMotion.EaseOut,
                                        ),
                                        targetOffset = { fullOffset -> (fullOffset * 0.14f).toInt() },
                                    ) + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = 200,
                                            easing = MochyMotion.EaseInOut,
                                        ),
                                        targetAlpha = 0.82f,
                                    ) + scaleOut(
                                        targetScale = 0.975f,
                                        animationSpec = tween(
                                            durationMillis = 260,
                                            easing = MochyMotion.EaseOut,
                                        ),
                                    )
                                },
                                popEnterTransition = {
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = tween(
                                            durationMillis = 260,
                                            easing = MochyMotion.EaseOut,
                                        ),
                                        initialOffset = { fullOffset -> (fullOffset * 0.16f).toInt() },
                                    ) + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 220,
                                            easing = MochyMotion.EaseOut,
                                        ),
                                        initialAlpha = 0.85f,
                                    ) + scaleIn(
                                        initialScale = 0.98f,
                                        animationSpec = tween(
                                            durationMillis = 260,
                                            easing = MochyMotion.EaseOut,
                                        ),
                                    )
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = tween(
                                            durationMillis = 260,
                                            easing = MochyMotion.EaseOut,
                                        ),
                                        targetOffset = { fullOffset -> (fullOffset * 0.24f).toInt() },
                                    ) + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = 190,
                                            easing = MochyMotion.EaseInOut,
                                        ),
                                    )
                                },
                            ) {
                                mediaNavGraph(
                                    context = this@MainActivity,
                                    navController = mainNavController,
                                )
                                settingsNavGraph(navController = mainNavController)
                            }

                            val playbackState by playbackController.state.collectAsStateWithLifecycle()
                            val showMiniPlayer = playbackState.currentItem != null && !playbackState.isIdle
                            if (showMiniPlayer) {
                                val currentBackStackEntry by mainNavController.currentBackStackEntryAsState()
                                val currentRoute = currentBackStackEntry?.destination?.route
                                val isSettingsRoot = currentRoute == settingsNavigationRoute
                                val isBottomNavVisible = currentRoute == null ||
                                    isSettingsRoot ||
                                    (!currentRoute.contains("Preferences", ignoreCase = true) &&
                                        !currentRoute.contains("Stats", ignoreCase = true) &&
                                        !currentRoute.contains("Backup", ignoreCase = true) &&
                                        !currentRoute.contains("Storage", ignoreCase = true))

                                MiniPlayer(
                                    playbackController = playbackController,
                                    playbackState = playbackState,
                                    onMiniPlayerClick = {
                                        val context = this@MainActivity
                                        val intent = Intent(context, PlayerActivity::class.java).apply {
                                            action = Intent.ACTION_VIEW
                                            data = playbackState.currentItem?.uri
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                        .padding(bottom = if (isBottomNavVisible) 88.dp else 12.dp),
                                )
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns `true` if dark theme should be used, as a function of the [uiState] and the
 * current system context.
 */
@Composable
fun shouldUseDarkTheme(
    uiState: MainActivityUiState,
): Boolean = true

@Composable
fun shouldUseHighContrastDarkTheme(
    uiState: MainActivityUiState,
): Boolean = when (uiState) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> uiState.preferences.useHighContrastDarkTheme
}

@Composable
fun shouldUseDynamicTheming(uiState: MainActivityUiState): Boolean = when (uiState) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> uiState.preferences.useDynamicColors
}

@Composable
fun shouldUsePureBlack(uiState: MainActivityUiState): Boolean = when (uiState) {
    MainActivityUiState.Loading -> true
    is MainActivityUiState.Success -> uiState.preferences.pureBlack
}

@Composable
fun shouldUseSystemFont(uiState: MainActivityUiState): Boolean = when (uiState) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> uiState.preferences.useSystemFont
}

@Composable
fun customThemeValue(uiState: MainActivityUiState): String = when (uiState) {
    MainActivityUiState.Loading -> ""
    is MainActivityUiState.Success -> uiState.preferences.customThemeValue
}

@Composable
fun shouldEnableMeshBackground(uiState: MainActivityUiState): Boolean = when (uiState) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> uiState.preferences.enableMeshBackground
}
