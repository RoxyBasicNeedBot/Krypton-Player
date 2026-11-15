package dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker

import android.net.Uri
import dev.anilbeesetti.nextplayer.core.common.CrashBreadcrumbs
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.HorizontalDivider
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.FolderInfoDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import dev.anilbeesetti.nextplayer.core.ui.components.AnimatedAppLogo
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.NavigationBar
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.foundation.Image
import androidx.compose.material3.Surface

import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Scaffold

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import dev.anilbeesetti.nextplayer.core.common.MainTargetTab
import dev.anilbeesetti.nextplayer.core.common.MediaPickerTabController
import dev.anilbeesetti.nextplayer.core.common.storagePermission
import dev.anilbeesetti.nextplayer.core.media.services.MediaService
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.Folder
import dev.anilbeesetti.nextplayer.core.model.MediaLayoutMode
import dev.anilbeesetti.nextplayer.core.model.MediaViewMode
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.base.DataState
import dev.anilbeesetti.nextplayer.core.ui.components.CancelButton
import dev.anilbeesetti.nextplayer.core.ui.components.DoneButton
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.composables.PermissionMissingView
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.core.ui.preview.DayNightPreview
import dev.anilbeesetti.nextplayer.core.ui.preview.VideoPickerPreviewParameterProvider
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.core.ui.theme.PremiumMeshBackground
import dev.anilbeesetti.nextplayer.core.ui.theme.LocalMeshBackgroundEnabled
import dev.anilbeesetti.nextplayer.core.ui.theme.ForceDarkContent
import dev.anilbeesetti.nextplayer.core.ui.components.AmbientThemeGlow
import dev.anilbeesetti.nextplayer.core.ui.components.MochyNavigationDock
import dev.anilbeesetti.nextplayer.core.ui.components.SimpFloatingNavigationBar
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramLogo
import dev.anilbeesetti.nextplayer.core.ui.components.MainNavTab
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.PlatformBackdrop
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.MochySettingsView
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Search
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.rememberBackdrop
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.layerBackdrop
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import dev.anilbeesetti.nextplayer.core.ui.theme.LocalMediaController
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.rememberPlayerState
import androidx.media3.common.Player

import androidx.hilt.navigation.compose.hiltViewModel
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.CenterCircularProgressBar
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.MediaView
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.TmdbExploreView
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.NoVideosFound
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.MovieDetailDialog
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.search.TmdbSearchRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.ActorDetailDialog
import dev.anilbeesetti.nextplayer.core.ui.components.LiquidGlassIconButton
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.QuickSettingsDialog
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.RenameDialog
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.TextIconToggleButton
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.VideoInfoDialog
import dev.anilbeesetti.nextplayer.feature.videopicker.state.SelectedFolder
import dev.anilbeesetti.nextplayer.feature.videopicker.state.SelectedVideo
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.rememberFolderPaletteColors
import dev.anilbeesetti.nextplayer.core.ui.theme.toImmersiveBackground
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.border
import androidx.compose.material3.MenuDefaults
import dev.anilbeesetti.nextplayer.feature.videopicker.state.rememberSelectionManager
import android.content.Intent
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.rememberGraphicsLayer
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.rememberGlassLuminance
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.rememberGlassInteraction
import dev.anilbeesetti.nextplayer.core.ui.components.bouncyPress
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.drawInteractiveGlass
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.liquidGlassBorderLaser
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.RocketLaunch
import dev.anilbeesetti.nextplayer.core.data.update.AppUpdateInfo
import dev.anilbeesetti.nextplayer.core.data.update.UpdateState
import dev.anilbeesetti.nextplayer.core.ui.components.ExpressiveUpdateBottomSheet

private enum class FileOperationType {
    COPY,
    MOVE,
}

@Composable
fun MediaPickerRoute(
    viewModel: MediaPickerViewModel = hiltViewModel(),
    onPlayVideo: (uri: Uri) -> Unit,
    onPlayVideos: (uris: List<Uri>) -> Unit,
    onFolderClick: (folderPath: String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onNavigateUp: () -> Unit,
    onReelsClick: (videos: List<Video>, startIndex: Int, playlistKey: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val isUpdateSnoozed by viewModel.isUpdateSnoozed.collectAsStateWithLifecycle()

    MediaPickerScreen(
        uiState = uiState,
        updateState = updateState,
        isUpdateSnoozed = isUpdateSnoozed,
        onPlayVideo = onPlayVideo,
        onPlayVideos = onPlayVideos,
        onNavigateUp = onNavigateUp,
        onFolderClick = onFolderClick,
        onSettingsClick = onSettingsClick,
        onSearchClick = onSearchClick,
        onTelegramClick = onTelegramClick,
        onEvent = viewModel::onEvent,
        onReelsClick = onReelsClick,
        onSnoozeUpdate = viewModel::snoozeUpdatePrompt,
        onDownloadUpdate = { viewModel.downloadAndInstall(it) },
        onInstallUpdate = { viewModel.installApk(it) },
        onSkipUpdateVersion = { viewModel.skipVersion(it) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalPermissionsApi::class)
@Composable
internal fun MediaPickerScreen(
    uiState: MediaPickerUiState,
    updateState: UpdateState = UpdateState.Idle,
    isUpdateSnoozed: Boolean = false,
    onNavigateUp: () -> Unit = {},
    onPlayVideo: (Uri) -> Unit = {},
    onPlayVideos: (List<Uri>) -> Unit = {},
    onFolderClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onTelegramClick: () -> Unit = {},
    onEvent: (MediaPickerUiEvent) -> Unit = {},
    onReelsClick: (List<Video>, Int, String) -> Unit = { _, _, _ -> },
    onSnoozeUpdate: () -> Unit = {},
    onDownloadUpdate: (AppUpdateInfo) -> Unit = {},
    onInstallUpdate: (Uri?) -> Unit = {},
    onSkipUpdateVersion: (String) -> Unit = {},
) {
    val selectionManager = rememberSelectionManager()
    val mediaController = LocalMediaController.current
    val isMiniPlayerVisible = mediaController?.let { controller ->
        val playerState = rememberPlayerState(controller)
        playerState.currentMediaItem != null && playerState.playbackState != Player.STATE_IDLE
    } ?: false

    var showUpdateSheet by rememberSaveable { mutableStateOf(false) }
    var discoveredUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateState.Available -> discoveredUpdateInfo = state.info
            is UpdateState.Downloading -> if (state.info != null) discoveredUpdateInfo = state.info
            is UpdateState.ReadyToInstall -> if (state.info != null) discoveredUpdateInfo = state.info
            else -> {}
        }
    }

    val isUpdateAvailable = updateState is UpdateState.Available ||
            (updateState is UpdateState.Downloading && discoveredUpdateInfo != null) ||
            (updateState is UpdateState.ReadyToInstall && discoveredUpdateInfo != null)

    val shouldShowUpdatePill = isUpdateAvailable &&
            !isUpdateSnoozed &&
            !selectionManager.isInSelectionMode
    val permissionState = rememberPermissionState(permission = storagePermission)
    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            onEvent(MediaPickerUiEvent.Refresh)
        }
    }
    val homeLazyGridState = rememberLazyGridState()
    val folderLazyGridState = remember(uiState.folderName) { LazyGridState() }
    val lazyGridState = if (uiState.folderName == null) homeLazyGridState else folderLazyGridState

    val folderScrollOffset by remember(folderLazyGridState) {
        derivedStateOf {
            if (folderLazyGridState.firstVisibleItemIndex > 0) 1f
            else (folderLazyGridState.firstVisibleItemScrollOffset / 280f).coerceIn(0f, 1f)
        }
    }
    val selectVideoFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { it?.let { onPlayVideo(it) } },
    )

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingFileOperation by rememberSaveable { mutableStateOf<FileOperationType?>(null) }
    val copyMoveFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { treeUri ->
            val op = pendingFileOperation
            pendingFileOperation = null
            if (treeUri != null && op != null) {
                val selectedVideosToProcess = selectionManager.allSelectedVideos.toList()
                if (selectedVideosToProcess.isEmpty()) return@rememberLauncherForActivityResult

                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                            )
                        } catch (_: Exception) { }

                        val docUri = DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            DocumentsContract.getTreeDocumentId(treeUri),
                        )

                        var processedCount = 0
                        selectedVideosToProcess.forEach { video ->
                            val sourceUri = video.uriString.toUri()
                            val fileName = video.nameWithExtension.ifBlank { video.name }
                            val createdDocUri = DocumentsContract.createDocument(
                                context.contentResolver,
                                docUri,
                                "video/*",
                                fileName,
                            ) ?: throw IOException("Could not create destination file: $fileName")

                            context.contentResolver.openInputStream(sourceUri).use { input ->
                                if (input == null) throw IOException("Could not open source file: $fileName")
                                context.contentResolver.openOutputStream(createdDocUri).use { output ->
                                    if (output == null) throw IOException("Could not write destination file: $fileName")
                                    input.copyTo(output)
                                }
                            }

                            if (op == FileOperationType.MOVE) {
                                try {
                                    DocumentsContract.deleteDocument(context.contentResolver, sourceUri)
                                } catch (_: Exception) {
                                    context.contentResolver.delete(sourceUri, null, null)
                                }
                            }
                            processedCount++
                        }

                        withContext(Dispatchers.Main) {
                            val message = if (op == FileOperationType.COPY) {
                                "Successfully copied $processedCount video(s)"
                            } else {
                                "Successfully moved $processedCount video(s)"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            selectionManager.exitSelectionMode()
                            onEvent(MediaPickerUiEvent.Refresh)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Operation failed: ${e.localizedMessage ?: "Unknown error"}",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }
            }
        },
    )

    val backdrop = rememberBackdrop()

    // TMDB States
    val tmdbExploreViewModel: TmdbExploreViewModel = hiltViewModel()
    val tmdbExploreState by tmdbExploreViewModel.uiState.collectAsStateWithLifecycle()

    var detailBackStack by remember { mutableStateOf<List<TmdbDetailDestination>>(emptyList()) }
    var selectedTab by rememberSaveable { mutableStateOf(MediaPickerTab.HOME) }

    val profilePrefs = remember { context.getSharedPreferences("krypton_profile_prefs", Context.MODE_PRIVATE) }
    var profilePicUri by remember { mutableStateOf(profilePrefs.getString("profile_pic_uri", null)) }
    var currentUsername by remember {
        mutableStateOf(profilePrefs.getString("username", null)?.takeIf { it.isNotBlank() } ?: "Roxy")
    }

    DisposableEffect(profilePrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "profile_pic_uri") {
                profilePicUri = prefs.getString("profile_pic_uri", null)
            } else if (key == "username") {
                currentUsername = prefs.getString("username", null)?.takeIf { it.isNotBlank() } ?: "Roxy"
            }
        }
        profilePrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            profilePrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val target = MediaPickerTabController.targetTab.value
        if (target != null) {
            when (target) {
                MainTargetTab.HOME -> {
                    selectedTab = MediaPickerTab.HOME
                }
                MainTargetTab.EXPLORE -> {
                    detailBackStack = emptyList()
                    selectedTab = MediaPickerTab.SEARCH
                }
            }
            MediaPickerTabController.targetTab.value = null
        }
    }

    LaunchedEffect(Unit) {
        MediaPickerTabController.targetTab.collect { target ->
            if (target != null) {
                when (target) {
                    MainTargetTab.HOME -> {
                        selectedTab = MediaPickerTab.HOME
                    }
                    MainTargetTab.EXPLORE -> {
                        detailBackStack = emptyList()
                        selectedTab = MediaPickerTab.SEARCH
                    }
                }
                MediaPickerTabController.targetTab.value = null
            }
        }
    }

    val currentDestination = detailBackStack.lastOrNull()

    BackHandler(enabled = detailBackStack.isNotEmpty()) {
        detailBackStack = detailBackStack.dropLast(1)
    }

    BackHandler(enabled = selectedTab == MediaPickerTab.SEARCH && tmdbExploreState.activeSeeAllCategory != null && detailBackStack.isEmpty()) {
        tmdbExploreViewModel.clearSeeAll()
    }

    BackHandler(enabled = selectedTab != MediaPickerTab.HOME && tmdbExploreState.activeSeeAllCategory == null && detailBackStack.isEmpty()) {
        selectedTab = MediaPickerTab.HOME
    }

    var showMoreMenu by rememberSaveable { mutableStateOf(false) }
    var showQuickSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showUrlDialog by rememberSaveable { mutableStateOf(false) }
    var showFolderInfoDialog by rememberSaveable { mutableStateOf(false) }
    var isManualPullToRefresh by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.refreshing) {
        if (!uiState.refreshing) {
            isManualPullToRefresh = false
        }
    }

    // In-Folder search states (searches only within current folder)
    var isFolderSearching by rememberSaveable { mutableStateOf(false) }
    var folderSearchQuery by rememberSaveable { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.folderName) {
        isFolderSearching = false
        folderSearchQuery = ""
    }

    LaunchedEffect(isFolderSearching) {
        if (isFolderSearching) {
            kotlinx.coroutines.delay(120)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    var showRenameActionFor: Video? by rememberSaveable { mutableStateOf(null) }
    var showInfoActionFor: Video? by rememberSaveable { mutableStateOf(null) }
    var showDeleteVideosConfirmation by rememberSaveable { mutableStateOf(false) }

    val selectedItemsSize = selectionManager.selectedFolders.size + selectionManager.selectedVideos.size
    val totalItemsSize = (uiState.mediaDataState as? DataState.Success)?.value?.run { folderList.size + mediaList.size } ?: 0



    val rawFolderForBg = (uiState.mediaDataState as? DataState.Success)?.value
    val isInsideFolder = uiState.folderName != null
    val folderCoverModel = remember(rawFolderForBg, isInsideFolder) {
        if (!isInsideFolder || rawFolderForBg == null) null
        else {
            val targetVideo = rawFolderForBg.recentlyPlayedVideo ?: rawFolderForBg.firstVideo ?: rawFolderForBg.mediaList.firstOrNull() ?: rawFolderForBg.allMediaList.firstOrNull()
            targetVideo?.let { video ->
                when {
                    !video.artworkUri.isNullOrBlank() -> video.artworkUri
                    video.isImage -> video.uriString.ifEmpty { video.path }
                    video.uriString.isNotBlank() && !video.uriString.startsWith("http") -> video.uriString
                    video.path.isNotBlank() -> video.path
                    else -> null
                }
            }
        }
    }
    val (folderDominant, _, _) = rememberFolderPaletteColors(folderCoverModel)
    val folderBackgroundColor by animateColorAsState(
        targetValue = if (isInsideFolder && !isFolderSearching) folderDominant.toImmersiveBackground() else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "folderBackgroundColor",
    )

    PremiumMeshBackground(
        enabled = LocalMeshBackgroundEnabled.current && uiState.folderName == null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isInsideFolder && !isFolderSearching) Modifier.background(folderBackgroundColor)
                    else Modifier
                )
        ) {

                val isHomeRoot = selectedTab == MediaPickerTab.HOME && uiState.folderName == null && !selectionManager.isInSelectionMode
                val homeTopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

                Scaffold(
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                        if (isHomeRoot) {
                            Modifier.nestedScroll(homeTopAppBarScrollBehavior.nestedScrollConnection)
                        } else {
                            Modifier
                        }
                    ),
                floatingActionButton = {
                    val currentVideos = (uiState.mediaDataState as? DataState.Success)?.value?.mediaList ?: emptyList()
                    if (currentVideos.isNotEmpty() && !selectionManager.isInSelectionMode && selectedTab == MediaPickerTab.HOME) {
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        Box(
                            modifier = Modifier
                                .padding(bottom = if (isMiniPlayerVisible) 80.dp else 0.dp)
                                .size(56.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = CircleShape,
                                    spotColor = Color.Black.copy(alpha = 0.35f),
                                )
                                .clip(CircleShape)
                                .then(
                                    if (backdrop != null) {
                                        Modifier.drawInteractiveGlass(
                                            isDark = isDark,
                                            backdrop = backdrop,
                                            layer = null,
                                            luminance = { 0.5f },
                                            shape = CircleShape,
                                            interaction = null,
                                            minScrim = 0.12f,
                                            maxScrim = 0.35f,
                                        )
                                    } else {
                                        Modifier.background(
                                            if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF)
                                        )
                                    }
                                )
                                .liquidGlassBorderLaser(
                                    laserColor = Color(0xFFFFD54F),
                                    coreColor = Color(0xFFFFFBEB),
                                    haloColor = Color(0xFFF59E0B),
                                    speedMillis = 3600,
                                    strokeWidth = 1.3.dp,
                                    glowWidth = 3.5.dp,
                                )
                                .bouncyPress()
                                .clickable {
                                    CrashBreadcrumbs.recordClick("Reels FAB (Liquid Glass)", "MediaPickerScreen", "Opening Reels with ${currentVideos.size} videos")
                                    onReelsClick(currentVideos, 0, uiState.folderName ?: "All Videos")
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = NextIcons.Reel),
                                contentDescription = "Reels",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                },
                topBar = {
                    if (selectedTab == MediaPickerTab.HOME && uiState.folderName != null && isFolderSearching) {
                        NextTopAppBar(
                            title = {
                                OutlinedTextField(
                                    value = folderSearchQuery,
                                    onValueChange = { folderSearchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(searchFocusRequester),
                                    placeholder = {
                                        Text(
                                            text = "Search in ${uiState.folderName}...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    shape = CircleShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.Transparent,
                                    ),
                                    trailingIcon = {
                                        if (folderSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { folderSearchQuery = "" }) {
                                                Icon(
                                                    imageVector = NextIcons.Close,
                                                    contentDescription = "Clear search",
                                                )
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        keyboardController?.hide()
                                    }),
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    isFolderSearching = false
                                    folderSearchQuery = ""
                                }) {
                                    Icon(
                                        imageVector = NextIcons.ArrowBack,
                                        contentDescription = "Exit search",
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            ),
                        )
                    } else if (selectedTab == MediaPickerTab.HOME && uiState.folderName != null && !selectionManager.isInSelectionMode) {
                        // Mochy-style Material 3 Collapsing & Blending Top Bar
                        val folderTitle = uiState.folderName ?: ""
                        val folderVideos = (uiState.mediaDataState as? DataState.Success)?.value?.let {
                            if (it.allMediaList.isNotEmpty()) it.allMediaList else it.mediaList
                        } ?: emptyList()
                        val topBarBgColor = if (folderScrollOffset > 0.05f) {
                            folderDominant.toImmersiveBackground().copy(alpha = (0.78f + 0.22f * folderScrollOffset).coerceIn(0f, 0.98f))
                        } else {
                            Color.Transparent
                        }

                        NextTopAppBar(
                            title = {
                                Column(
                                    modifier = Modifier.graphicsLayer {
                                        alpha = folderScrollOffset
                                        translationY = (1f - folderScrollOffset) * 8f
                                    }
                                ) {
                                    Text(
                                        text = folderTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${folderVideos.size} ${if (folderVideos.size == 1) "video" else "videos"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1,
                                    )
                                }
                            },
                            navigationIcon = {
                                LiquidGlassIconButton(
                                    onClick = onNavigateUp,
                                    size = 40.dp,
                                    backdrop = backdrop,
                                ) {
                                    Icon(
                                        imageVector = NextIcons.ArrowBack,
                                        contentDescription = stringResource(id = R.string.navigate_up),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            actions = {
                                LiquidGlassIconButton(
                                    onClick = {
                                        isFolderSearching = true
                                    },
                                    size = 40.dp,
                                    backdrop = backdrop,
                                ) {
                                    Icon(
                                        imageVector = NextIcons.Search,
                                        contentDescription = "Search videos in folder",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Box {
                                    LiquidGlassIconButton(
                                        onClick = { showMoreMenu = true },
                                        size = 40.dp,
                                        backdrop = backdrop,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = "Folder options",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    val currentFolder = (uiState.mediaDataState as? DataState.Success)?.value
                                    val videoToPlay = currentFolder?.recentlyPlayedVideo ?: currentFolder?.firstVideo
                                    NavigationMoreMenuContent(
                                        expanded = showMoreMenu,
                                        onDismissRequest = { showMoreMenu = false },
                                        onOpenLocalVideo = {
                                            showMoreMenu = false
                                            selectVideoFileLauncher.launch("video/*")
                                        },
                                        onOpenNetworkStream = {
                                            showMoreMenu = false
                                            showUrlDialog = true
                                        },
                                        onPlayRecentlyPlayed = {
                                            showMoreMenu = false
                                            if (videoToPlay != null) {
                                                onPlayVideo(videoToPlay.uriString.toUri())
                                            }
                                        },
                                        recentlyPlayedEnabled = videoToPlay != null,
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = topBarBgColor,
                                scrolledContainerColor = folderDominant.toImmersiveBackground(),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else if (selectedTab == MediaPickerTab.SETTINGS) {
                        // MochySettingsView has its own full-bleed immersive dot-grid header and statusBarsPadding!
                    } else {
                        NextTopAppBar(
                            scrollBehavior = if (isHomeRoot) homeTopAppBarScrollBehavior else null,
                            title = {
                                if (selectedTab == MediaPickerTab.SEARCH) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = NextIcons.Tmdb),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = Color.Unspecified
                                        )
                                        Text(
                                            text = "Explore",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.2.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    val titleText = (uiState.folderName ?: stringResource(R.string.app_name)).takeIf { !selectionManager.isInSelectionMode } ?: ""
                                    if (titleText.isNotEmpty()) {
                                        if (uiState.folderName == null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                AnimatedAppLogo(
                                                    modifier = Modifier.size(26.dp),
                                                    useGradient = false,
                                                    singleColor = MaterialTheme.colorScheme.onSurface,
                                                    animateOnEntry = true,
                                                    allowTapToReplay = true,
                                                )
                                                Text(
                                                    text = titleText,
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        letterSpacing = 0.25.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = titleText,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    letterSpacing = 0.2.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            ),
                            navigationIcon = {
                                if (selectionManager.isInSelectionMode) {
                                    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .then(
                                                if (backdrop != null) {
                                                    Modifier.drawInteractiveGlass(
                                                        isDark = isDark,
                                                        backdrop = backdrop,
                                                        layer = null,
                                                        luminance = { 0.5f },
                                                        shape = CircleShape,
                                                        interaction = null,
                                                        minScrim = 0.16f,
                                                        maxScrim = 0.42f,
                                                    )
                                                } else {
                                                    Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f))
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.35f),
                                                        Color.White.copy(alpha = 0.08f),
                                                    ),
                                                ),
                                                shape = CircleShape,
                                            )
                                            .bouncyPress()
                                            .clickable { selectionManager.exitSelectionMode() }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = NextIcons.Close,
                                            contentDescription = stringResource(id = R.string.navigate_up),
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = stringResource(R.string.m_n_selected, selectedItemsSize, totalItemsSize),
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                } else if (selectedTab == MediaPickerTab.HOME && uiState.folderName != null) {
                                    FilledTonalIconButton(onClick = onNavigateUp) {
                                        Icon(
                                            imageVector = NextIcons.ArrowBack,
                                            contentDescription = stringResource(id = R.string.navigate_up),
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (selectionManager.isInSelectionMode) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(end = 4.dp),
                                    ) {
                                        LiquidGlassIconButton(
                                            onClick = {
                                                val uris = selectionManager.allSelectedVideos.map { it.uriString }
                                                if (uris.isNotEmpty()) {
                                                    onEvent(MediaPickerUiEvent.ShareVideos(uris))
                                                }
                                            },
                                            enabled = selectionManager.allSelectedVideos.isNotEmpty(),
                                            size = 38.dp,
                                            backdrop = backdrop,
                                        ) {
                                            Icon(
                                                imageVector = NextIcons.Share,
                                                contentDescription = stringResource(R.string.share),
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        LiquidGlassIconButton(
                                            onClick = {
                                                if (selectedItemsSize != totalItemsSize) {
                                                    (uiState.mediaDataState as? DataState.Success)?.value?.let { folder ->
                                                        folder.folderList.forEach { selectionManager.selectFolder(it) }
                                                        folder.mediaList.forEach { selectionManager.selectVideo(it) }
                                                    }
                                                } else {
                                                    selectionManager.clearSelection()
                                                }
                                            },
                                            size = 38.dp,
                                            backdrop = backdrop,
                                        ) {
                                            Icon(
                                                imageVector = if (selectedItemsSize != totalItemsSize) {
                                                    NextIcons.SelectAll
                                                } else {
                                                    NextIcons.DeselectAll
                                                },
                                                contentDescription = if (selectedItemsSize != totalItemsSize) {
                                                    stringResource(R.string.select_all)
                                                } else {
                                                    stringResource(R.string.deselect_all)
                                                },
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                } else if (selectedTab == MediaPickerTab.HOME || selectedTab == MediaPickerTab.LIBRARY) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(end = 4.dp),
                                    ) {
                                        LiquidGlassIconButton(
                                            onClick = {
                                                CrashBreadcrumbs.recordClick("Link / Network Stream Icon", "Home Top Bar", "Opening Network Stream Dialog")
                                                showUrlDialog = true
                                            },
                                            size = 38.dp,
                                            backdrop = backdrop,
                                        ) {
                                            Icon(
                                                imageVector = NextIcons.Link,
                                                contentDescription = stringResource(id = R.string.open_network_stream),
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        AnimatedTelegramButton(
                                            onClick = {
                                                CrashBreadcrumbs.recordClick("Telegram Cloud Icon", "Home Top Bar", "Navigating to Telegram Screen")
                                                onTelegramClick()
                                            },
                                            backdrop = backdrop,
                                        )
                                        LiquidGlassIconButton(
                                            onClick = {
                                                CrashBreadcrumbs.recordClick("Quick Settings Icon", "Home Top Bar", "Opening Quick Settings Dialog")
                                                showQuickSettingsDialog = true
                                            },
                                            size = 38.dp,
                                            backdrop = backdrop,
                                        ) {
                                            Icon(
                                                imageVector = NextIcons.DashBoard,
                                                contentDescription = stringResource(id = R.string.quick_settings),
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }

                                        // Mochy 1:1 Persistent Top Bar Profile Avatar (1:1 with HomeScreen.kt lines 1850-1885)
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    CrashBreadcrumbs.recordClick("Profile Avatar Icon", "Home Top Bar", "Navigating to Mochy Profile/Settings")
                                                    selectedTab = MediaPickerTab.SETTINGS
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (!profilePicUri.isNullOrBlank()) {
                                                val model = if (profilePicUri!!.contains("://")) profilePicUri else "file://$profilePicUri"
                                                AsyncImage(
                                                    model = model,
                                                    placeholder = painterResource(id = R.drawable.person),
                                                    error = painterResource(id = R.drawable.person),
                                                    contentDescription = "Profile",
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.person),
                                                        contentDescription = "Profile",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (selectedTab == MediaPickerTab.SEARCH) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(end = 4.dp),
                                    ) {
                                        IconButton(onClick = {
                                            detailBackStack = detailBackStack + TmdbDetailDestination.TmdbSearch
                                        }) {
                                            Icon(
                                                imageVector = NextIcons.Search,
                                                contentDescription = "Search TMDb Movies, Shows & Actors"
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    CrashBreadcrumbs.recordClick("Profile Avatar Icon", "Search Top Bar", "Navigating to Mochy Profile/Settings")
                                                    selectedTab = MediaPickerTab.SETTINGS
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (!profilePicUri.isNullOrBlank()) {
                                                val model = if (profilePicUri!!.contains("://")) profilePicUri else "file://$profilePicUri"
                                                AsyncImage(
                                                    model = model,
                                                    placeholder = painterResource(id = R.drawable.person),
                                                    error = painterResource(id = R.drawable.person),
                                                    contentDescription = "Profile",
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.person),
                                                        contentDescription = "Profile",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                            },
                        )
                    }
                },
                bottomBar = {
                    if (selectionManager.isInSelectionMode) {
                        FloatingSelectionBar(
                            show = selectionManager.allSelectedVideos.isNotEmpty() || selectionManager.selectedFolders.isNotEmpty(),
                            showRenameAction = selectionManager.isSingleVideoSelected,
                            showInfoAction = selectionManager.isSingleVideoSelected,
                            backdrop = backdrop,
                            onPlayAction = {
                                val videoUris = selectionManager.allSelectedVideos.map { it.uriString.toUri() }
                                onPlayVideos(videoUris)
                                selectionManager.exitSelectionMode()
                            },
                            onCopyAction = {
                                pendingFileOperation = FileOperationType.COPY
                                copyMoveFolderPickerLauncher.launch(null)
                            },
                            onMoveAction = {
                                pendingFileOperation = FileOperationType.MOVE
                                copyMoveFolderPickerLauncher.launch(null)
                            },
                            onRenameAction = {
                                val selectedVideo = selectionManager.selectedVideos.firstOrNull() ?: return@FloatingSelectionBar
                                val video = (uiState.mediaDataState as? DataState.Success)?.value?.mediaList
                                    ?.find { it.uriString == selectedVideo.uriString } ?: return@FloatingSelectionBar
                                showRenameActionFor = video
                            },
                            onInfoAction = {
                                val selectedVideo = selectionManager.selectedVideos.firstOrNull() ?: return@FloatingSelectionBar
                                val video = (uiState.mediaDataState as? DataState.Success)?.value?.mediaList
                                    ?.find { it.uriString == selectedVideo.uriString } ?: return@FloatingSelectionBar
                                showInfoActionFor = video
                                selectionManager.clearSelection()
                            },
                            onDeleteAction = {
                                if (MediaService.willSystemAsksForDeleteConfirmation()) {
                                    onEvent(MediaPickerUiEvent.DeleteVideos(selectionManager.allSelectedVideos.map { it.uriString }))
                                    selectionManager.exitSelectionMode()
                                } else {
                                    showDeleteVideosConfirmation = true
                                }
                            },
                        )
                    } else if (currentDestination == null && uiState.folderName == null) {
                        MediaNavigationBar(
                            selectedTab = selectedTab,
                            navigationBarStyle = uiState.preferences.navigationBarStyle,
                            onTabSelected = {
                                CrashBreadcrumbs.recordClick("Nav Tab: $it", "MediaNavigationBar")
                                selectedTab = it
                            },
                            onSearchClick = {
                                CrashBreadcrumbs.recordClick("Search Icon", "MediaNavigationBar", "Opening Search")
                                if (selectedTab == MediaPickerTab.SEARCH) {
                                    detailBackStack = detailBackStack + TmdbDetailDestination.TmdbSearch
                                } else {
                                    onSearchClick()
                                }
                            },
                            onSettingsClick = {
                                CrashBreadcrumbs.recordClick("Settings Icon", "MediaNavigationBar")
                                onSettingsClick()
                            },
                            onMoreClick = {
                                CrashBreadcrumbs.recordClick("More Menu Icon", "MediaNavigationBar")
                                showMoreMenu = !showMoreMenu
                            },
                            moreMenuContent = {
                                val folder = (uiState.mediaDataState as? DataState.Success)?.value
                                val videoToPlay = folder?.recentlyPlayedVideo ?: folder?.firstVideo
                                NavigationMoreMenuContent(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    onOpenLocalVideo = {
                                        CrashBreadcrumbs.recordClick("More Menu -> Open Local Video", "MediaPickerScreen")
                                        showMoreMenu = false
                                        selectVideoFileLauncher.launch("video/*")
                                    },
                                    onOpenNetworkStream = {
                                        CrashBreadcrumbs.recordClick("More Menu -> Open Network Stream (Link)", "MediaPickerScreen")
                                        showMoreMenu = false
                                        showUrlDialog = true
                                    },
                                    onPlayRecentlyPlayed = {
                                        CrashBreadcrumbs.recordClick("More Menu -> Play Recently Played", "MediaPickerScreen")
                                        showMoreMenu = false
                                        if (videoToPlay != null) {
                                            onPlayVideo(videoToPlay.uriString.toUri())
                                        }
                                    },
                                    recentlyPlayedEnabled = videoToPlay != null
                                )
                            },
                            backdrop = backdrop,
                        )
                    }
                },
            ) { scaffoldPadding ->
                val isFolderDetail = uiState.folderName != null && !isFolderSearching
                val updatedScaffoldPadding = if (isFolderDetail && !selectionManager.isInSelectionMode) {
                    PaddingValues(
                        start = 0.dp,
                        top = 0.dp,
                        end = 0.dp,
                        bottom = scaffoldPadding.calculateBottomPadding() +
                                (if (isMiniPlayerVisible) 80.dp else 0.dp),
                    )
                } else {
                    scaffoldPadding.copy(
                        start = 0.dp,
                        bottom = scaffoldPadding.calculateBottomPadding() +
                                (if (isMiniPlayerVisible) 80.dp else 0.dp),
                    )
                }

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState == MediaPickerTab.SEARCH || targetState == MediaPickerTab.LIBRARY || targetState == MediaPickerTab.SETTINGS) {
                            (slideInHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f)) { width -> width / 4 } + fadeIn(tween(250)))
                                .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f)) { width -> -width / 4 } + fadeOut(tween(250)))
                        } else {
                            (slideInHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f)) { width -> -width / 4 } + fadeIn(tween(250)))
                                .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f)) { width -> width / 4 } + fadeOut(tween(250)))
                        }
                    },
                    label = "MediaTabTransition",
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop),
                ) { currentTab ->
                    if (currentTab == MediaPickerTab.HOME || currentTab == MediaPickerTab.LIBRARY) {
                    when (uiState.mediaDataState) {
                        is DataState.Error -> {
                        }

                        is DataState.Loading -> {
                            CenterCircularProgressBar(modifier = Modifier.padding(scaffoldPadding))
                        }

                        is DataState.Success -> {
                            val content = @Composable {
                                PermissionMissingView(
                                    isGranted = permissionState.status.isGranted,
                                    showRationale = permissionState.status.shouldShowRationale,
                                    permission = permissionState.permission,
                                    launchPermissionRequest = { permissionState.launchPermissionRequest() },
                                ) {
                                    val rawFolder = uiState.mediaDataState.value
                                    if (rawFolder == null || (rawFolder.folderList.isEmpty() && rawFolder.mediaList.isEmpty())) {
                                        NoVideosFound(contentPadding = updatedScaffoldPadding)
                                        return@PermissionMissingView
                                    }

                                    val displayFolder = remember(rawFolder, isFolderSearching, folderSearchQuery) {
                                        if (isFolderSearching && folderSearchQuery.isNotBlank()) {
                                            val query = folderSearchQuery.trim()
                                            val poolOfVideos = if (rawFolder.folderList.isEmpty()) rawFolder.mediaList else rawFolder.allMediaList
                                            val matchingVideos = poolOfVideos.filter {
                                                it.displayName.contains(query, ignoreCase = true) ||
                                                it.nameWithExtension.contains(query, ignoreCase = true)
                                            }
                                            val matchingFolders = rawFolder.folderList.filter {
                                                it.name.contains(query, ignoreCase = true)
                                            }
                                            rawFolder.copy(
                                                mediaList = matchingVideos,
                                                folderList = matchingFolders,
                                            )
                                        } else {
                                            rawFolder
                                        }
                                    }

                                    if (isFolderSearching && folderSearchQuery.isNotBlank() && displayFolder.mediaList.isEmpty() && displayFolder.folderList.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(updatedScaffoldPadding),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(24.dp),
                                            ) {
                                                Icon(
                                                    imageVector = NextIcons.Search,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(56.dp),
                                                )
                                                Text(
                                                    text = "No results found for \"$folderSearchQuery\"",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                )
                                                Text(
                                                    text = "No videos or subfolders in \"${uiState.folderName}\" matched your query.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                        return@PermissionMissingView
                                    }

                                    val rootFolder = displayFolder ?: rawFolder
                                    if (rootFolder == null || rootFolder.folderList.isEmpty() && rootFolder.mediaList.isEmpty()) {
                                        NoVideosFound(contentPadding = updatedScaffoldPadding)
                                        return@PermissionMissingView
                                    }

                                    val mediaViewContent = @Composable {
                                        MediaView(
                                            rootFolder = rootFolder,
                                            isFolderDetail = isFolderDetail,
                                            preferences = uiState.preferences,
                                            recentVideos = if (currentTab == MediaPickerTab.HOME) uiState.recentVideos else emptyList(),
                                            onFolderClick = onFolderClick,
                                            onVideoClick = { onPlayVideo(it) },
                                            onPlayVideos = onPlayVideos,
                                            selectionManager = selectionManager,
                                            lazyGridState = lazyGridState,
                                            contentPadding = updatedScaffoldPadding,
                                            onVideoLoaded = { onEvent(MediaPickerUiEvent.AddToSync(it)) },
                                            onNavigateUp = onNavigateUp,
                                            onReelsClick = { videos ->
                                                onReelsClick(videos, 0, uiState.folderName ?: "All Videos")
                                            },
                                            onQuickSettingsClick = { showQuickSettingsDialog = true },
                                            onSearchClick = {
                                                if (uiState.folderName != null) {
                                                    isFolderSearching = true
                                                } else {
                                                    onSearchClick()
                                                }
                                            },
                                            onMoreClick = { showMoreMenu = !showMoreMenu },
                                            onPlayNext = { video ->
                                                mediaController?.let { controller ->
                                                    val mediaItem = androidx.media3.common.MediaItem.Builder()
                                                        .setUri(video.uriString.toUri())
                                                        .setMediaId(video.uriString)
                                                        .build()
                                                    val insertIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount)
                                                    controller.addMediaItem(insertIndex, mediaItem)
                                                } ?: onPlayVideo(video.uriString.toUri())
                                            },
                                            onShareVideo = { video ->
                                                onEvent(MediaPickerUiEvent.ShareVideos(listOf(video.uriString)))
                                            },
                                            onRenameVideo = { video ->
                                                showRenameActionFor = video
                                            },
                                            onInfoVideo = { video ->
                                                showInfoActionFor = video
                                            },
                                            onDeleteVideo = { video ->
                                                if (MediaService.willSystemAsksForDeleteConfirmation()) {
                                                    onEvent(MediaPickerUiEvent.DeleteVideos(listOf(video.uriString)))
                                                } else {
                                                    selectionManager.clearSelection()
                                                    selectionManager.selectVideo(video)
                                                    showDeleteVideosConfirmation = true
                                                }
                                            },
                                            moreMenuContent = {
                                                if (uiState.folderName != null) {
                                                    FolderMoreMenuContent(
                                                        expanded = showMoreMenu,
                                                        onDismissRequest = { showMoreMenu = false },
                                                        folder = rawFolder,
                                                        currentLayoutMode = uiState.preferences.mediaLayoutMode,
                                                        onPlayAll = {
                                                            showMoreMenu = false
                                                            val videos = rawFolder.allMediaList
                                                            if (videos.isNotEmpty()) {
                                                                onPlayVideos(videos.map { it.uriString.toUri() })
                                                            }
                                                        },
                                                        onShuffleAll = {
                                                            showMoreMenu = false
                                                            val videos = rawFolder.allMediaList.shuffled()
                                                            if (videos.isNotEmpty()) {
                                                                onPlayVideos(videos.map { it.uriString.toUri() })
                                                            }
                                                        },
                                                        onSelectVideos = {
                                                            showMoreMenu = false
                                                            selectionManager.enterSelectionMode()
                                                        },
                                                        onSortClick = {
                                                            showMoreMenu = false
                                                            showQuickSettingsDialog = true
                                                        },
                                                        onToggleLayoutMode = {
                                                            showMoreMenu = false
                                                            val newMode = if (uiState.preferences.mediaLayoutMode == MediaLayoutMode.GRID) {
                                                                MediaLayoutMode.LIST
                                                            } else {
                                                                MediaLayoutMode.GRID
                                                            }
                                                            onEvent(MediaPickerUiEvent.UpdateMenu(uiState.preferences.copy(mediaLayoutMode = newMode)))
                                                        },
                                                        onRefresh = {
                                                            showMoreMenu = false
                                                            onEvent(MediaPickerUiEvent.Refresh)
                                                        },
                                                        onShowFolderInfo = {
                                                            showMoreMenu = false
                                                            showFolderInfoDialog = true
                                                        },
                                                    )
                                                } else {
                                                    val folder = (uiState.mediaDataState as? DataState.Success)?.value
                                                    val videoToPlay = folder?.recentlyPlayedVideo ?: folder?.firstVideo
                                                    NavigationMoreMenuContent(
                                                        expanded = showMoreMenu,
                                                        onDismissRequest = { showMoreMenu = false },
                                                        onOpenLocalVideo = {
                                                            showMoreMenu = false
                                                            selectVideoFileLauncher.launch("video/*")
                                                        },
                                                        onOpenNetworkStream = {
                                                            showMoreMenu = false
                                                            showUrlDialog = true
                                                        },
                                                        onPlayRecentlyPlayed = {
                                                            showMoreMenu = false
                                                            if (videoToPlay != null) {
                                                                onPlayVideo(videoToPlay.uriString.toUri())
                                                            }
                                                        },
                                                        recentlyPlayedEnabled = videoToPlay != null,
                                                    )
                                                }
                                            },
                                        )
                                    }

                                    if (isFolderDetail) {
                                        ForceDarkContent {
                                            mediaViewContent()
                                        }
                                    } else {
                                        mediaViewContent()
                                    }
                                }
                            }

                            if (uiState.folderName == null) {
                                val pullToRefreshState = rememberPullToRefreshState()
                                val isExplicitRefreshing = uiState.refreshing && isManualPullToRefresh
                                PullToRefreshBox(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = scaffoldPadding.calculateStartPadding(LocalLayoutDirection.current)),
                                    isRefreshing = isExplicitRefreshing,
                                    state = pullToRefreshState,
                                    onRefresh = {
                                        isManualPullToRefresh = true
                                        onEvent(MediaPickerUiEvent.Refresh)
                                    },
                                    indicator = {
                                        PullToRefreshDefaults.IndicatorBox(
                                            state = pullToRefreshState,
                                            isRefreshing = isExplicitRefreshing,
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .size(56.dp),
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ) {
                                            LoadingIndicator(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(28.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    },
                                ) {
                                    content()
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = scaffoldPadding.calculateStartPadding(LocalLayoutDirection.current)),
                                ) {
                                    content()
                                }
                            }
                        }
                    }
                } else if (currentTab == MediaPickerTab.SEARCH) {
                    TmdbExploreView(
                        tmdbExploreState = tmdbExploreState,
                        onSeeAllCategoryClick = tmdbExploreViewModel::showSeeAll,
                        onClearSeeAll = tmdbExploreViewModel::clearSeeAll,
                        onTmdbMediaClick = { id, isTv ->
                            detailBackStack = detailBackStack + TmdbDetailDestination.MovieDetail(id, isTv)
                        },
                        onTmdbActorClick = { id ->
                            detailBackStack = detailBackStack + TmdbDetailDestination.ActorDetail(id)
                        },
                        onSearchClick = {
                            CrashBreadcrumbs.recordClick("Search Icon", "TmdbExploreView", "Opening TMDB Search")
                            detailBackStack = detailBackStack + TmdbDetailDestination.TmdbSearch
                        },
                        onLoadMore = tmdbExploreViewModel::loadNextPage,
                        contentPadding = updatedScaffoldPadding,
                        onRetry = tmdbExploreViewModel::fetchAllData
                    )
                } else {
                    MochySettingsView(
                        onSettingsClick = onSettingsClick,
                        onDownloadsClick = onTelegramClick,
                        onWatchlistClick = {
                            selectedTab = MediaPickerTab.SEARCH
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        val isBottomNavVisible = uiState.folderName == null && currentDestination == null && !selectionManager.isInSelectionMode
        val updatePillBottomPadding = (if (isBottomNavVisible) 88.dp else 12.dp) + (if (isMiniPlayerVisible) 76.dp else 0.dp)

        AnimatedVisibility(
            visible = shouldShowUpdatePill,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(animationSpec = tween(350)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250),
            ) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = updatePillBottomPadding, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
        ) {
            discoveredUpdateInfo?.let { updateInfo ->
                HomeScreenUpdatePill(
                    info = updateInfo,
                    updateState = updateState,
                    onClick = {
                        showUpdateSheet = true
                    },
                    onSnooze = onSnoozeUpdate,
                )
            }
        }
    }
}


    LaunchedEffect(lazyGridState.isScrollInProgress) {
        if (showMoreMenu && lazyGridState.isScrollInProgress) {
            showMoreMenu = false
        }
    }

    LaunchedEffect(selectionManager.isInSelectionMode) {
        if (selectionManager.isInSelectionMode) {
            showMoreMenu = false
        }
    }

    BackHandler(enabled = isFolderSearching) {
        isFolderSearching = false
        folderSearchQuery = ""
    }

    BackHandler(enabled = showFolderInfoDialog) {
        showFolderInfoDialog = false
    }

    BackHandler(enabled = showMoreMenu) {
        showMoreMenu = false
    }

    BackHandler(enabled = selectionManager.isInSelectionMode) {
        selectionManager.exitSelectionMode()
    }

    if (showQuickSettingsDialog) {
        QuickSettingsDialog(
            applicationPreferences = uiState.preferences,
            onDismiss = { showQuickSettingsDialog = false },
            updatePreferences = { onEvent(MediaPickerUiEvent.UpdateMenu(it)) },
            backdrop = backdrop,
        )
    }

    if (showUrlDialog) {
        NetworkUrlDialog(
            onDismiss = { showUrlDialog = false },
            onDone = { showUrlDialog = false; onPlayVideo(it.trim().toUri()) },
        )
    }

    if (showFolderInfoDialog) {
        (uiState.mediaDataState as? DataState.Success)?.value?.let { currentFolder ->
            FolderInfoDialog(
                folder = currentFolder,
                onDismiss = { showFolderInfoDialog = false },
            )
        }
    }

    showRenameActionFor?.let { video ->
        RenameDialog(
            name = video.displayName,
            onDismiss = { showRenameActionFor = null },
            onDone = {
                onEvent(MediaPickerUiEvent.RenameVideo(video.uriString.toUri(), it))
                showRenameActionFor = null
                selectionManager.clearSelection()
            },
        )
    }

    showInfoActionFor?.let { video ->
        VideoInfoDialog(
            video = video,
            onDismiss = { showInfoActionFor = null },
        )
    }

    if (showDeleteVideosConfirmation) {
        DeleteConfirmationDialog(
            selectedVideos = selectionManager.selectedVideos,
            selectedFolders = selectionManager.selectedFolders,
            onConfirm = {
                onEvent(MediaPickerUiEvent.DeleteVideos(selectionManager.allSelectedVideos.map { it.uriString }))
                selectionManager.clearSelection()
                showDeleteVideosConfirmation = false
            },
            onCancel = { showDeleteVideosConfirmation = false },
        )
    }



    currentDestination?.let { destination ->
        when (destination) {
            is TmdbDetailDestination.MovieDetail -> {
                MovieDetailDialog(
                    movieId = destination.id,
                    isTvShow = destination.isTv,
                    onDismiss = { detailBackStack = detailBackStack.dropLast(1) },
                    onPlayVideo = { video ->
                        detailBackStack = emptyList() // Clear backstack on playback
                        onPlayVideo(video.uriString.toUri())
                    },
                    onActorClick = { actorId ->
                        detailBackStack = detailBackStack + TmdbDetailDestination.ActorDetail(actorId)
                    },
                    onMovieClick = { mediaId, isTv ->
                        detailBackStack = detailBackStack + TmdbDetailDestination.MovieDetail(mediaId, isTv)
                    }
                )
            }
            is TmdbDetailDestination.ActorDetail -> {
                ActorDetailDialog(
                    actorId = destination.id,
                    onDismiss = { detailBackStack = detailBackStack.dropLast(1) },
                    onMediaClick = { mediaId, isTv ->
                        detailBackStack = detailBackStack + TmdbDetailDestination.MovieDetail(mediaId, isTv)
                    }
                )
            }
            is TmdbDetailDestination.TmdbSearch -> {
                TmdbSearchRoute(
                    onNavigateUp = { detailBackStack = detailBackStack.dropLast(1) },
                    onMediaClick = { mediaId, isTv ->
                        detailBackStack = detailBackStack + TmdbDetailDestination.MovieDetail(mediaId, isTv)
                    },
                    onActorClick = { actorId ->
                        detailBackStack = detailBackStack + TmdbDetailDestination.ActorDetail(actorId)
                    }
                )
            }
        }
    }

    if (showUpdateSheet && discoveredUpdateInfo != null) {
        val currentVersionName = remember {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "1.0.0"
            } catch (_: Exception) {
                "1.0.0"
            }
        }

        ExpressiveUpdateBottomSheet(
            info = discoveredUpdateInfo!!,
            updateState = updateState,
            currentVersionName = currentVersionName,
            onDownload = {
                onDownloadUpdate(discoveredUpdateInfo!!)
            },
            onInstall = {
                val uri = (updateState as? UpdateState.ReadyToInstall)?.apkUri
                onInstallUpdate(uri)
            },
            onDismiss = {
                showUpdateSheet = false
            },
            onRemindLater = {
                showUpdateSheet = false
                onSnoozeUpdate()
            },
            onSkipVersion = {
                showUpdateSheet = false
                onSkipUpdateVersion(discoveredUpdateInfo!!.tagName.ifBlank { discoveredUpdateInfo!!.versionName })
            },
            onOpenReleaseUrl = if (discoveredUpdateInfo!!.releaseUrl.isNotBlank()) {
                {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(discoveredUpdateInfo!!.releaseUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            } else null,
        )
    }
}

/**
 * Sleek, floating update pill / banner on MediaPickerScreen for Krypton Player.
 *
 * Implements non-intrusive auto-update discoverability:
 * - Positioned cleanly above the bottom navigation bar / mini player
 * - Extra-large rounded pill (`RoundedCornerShape(24.dp)`), Material 3 `primaryContainer` tonal surface with subtle border and elevation
 * - Leading circular badge with pulsing rocket/download icon
 * - Text: "Update Available" with target version badge (e.g. `v0.16.3`), and subtitle "Tap to review & install"
 * - Trailing close (✕) button that snoozes the prompt for 24 hours (saving snooze timestamp)
 * - Smooth animated entrance: `slideInVertically() + fadeIn()` when update is discovered
 * - Branded strictly for "Krypton Player"
 */
@Composable
private fun HomeScreenUpdatePill(
    info: AppUpdateInfo,
    updateState: UpdateState,
    onClick: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDownloading = updateState is UpdateState.Downloading
    val isReadyToInstall = updateState is UpdateState.ReadyToInstall

    val infiniteTransition = rememberInfiniteTransition(label = "krypton_pulse_rocket")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    val targetVersion = info.versionName.ifBlank { info.tagName.removePrefix("v").removePrefix("V") }.trim()
    val displayVersion = if (targetVersion.startsWith("v", ignoreCase = true)) {
        targetVersion
    } else {
        "v$targetVersion"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Leading Circular Badge with Pulsing Rocket / Download Icon
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Pulsing outer aura glow
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)),
                )
                // Solid primary inner badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isReadyToInstall) {
                            Icons.Rounded.Download
                        } else {
                            Icons.Rounded.RocketLaunch
                        },
                        contentDescription = "Krypton Player Update Available",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(20.dp)
                            .scale(pulseScale),
                    )
                }
            }

            // Central Info Column: Title + Version Badge + Subtitle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = when {
                            isDownloading -> "Downloading Update"
                            isReadyToInstall -> "Update Ready"
                            else -> "Update Available"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Target Version Badge (e.g. v0.16.3)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = displayVersion,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = when {
                        isDownloading -> {
                            val progress = (updateState as? UpdateState.Downloading)?.progress ?: 0
                            "$progress% • Tap to view progress"
                        }
                        isReadyToInstall -> "Tap to install now"
                        else -> "Tap to review & install"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Trailing Close (✕) Button that Snoozes for 24 Hours
            IconButton(
                onClick = onSnooze,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Snooze Krypton Player update for 24 hours",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    modifier: Modifier = Modifier,
    selectedVideos: Set<SelectedVideo>,
    selectedFolders: Set<SelectedFolder>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    NextDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = when {
                    selectedVideos.isEmpty() -> when (selectedFolders.size) {
                        1 -> stringResource(R.string.delete_one_folder)
                        else -> stringResource(R.string.delete_folders, selectedFolders.size)
                    }

                    selectedFolders.isEmpty() -> when (selectedVideos.size) {
                        1 -> stringResource(R.string.delete_one_video)
                        else -> stringResource(R.string.delete_videos, selectedVideos.size)
                    }

                    else -> stringResource(R.string.delete_items, selectedFolders.size + selectedVideos.size)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = modifier,
            ) {
                Text(text = stringResource(R.string.delete))
            }
        },
        dismissButton = { CancelButton(onClick = onCancel) },
        modifier = modifier,
        content = {
            Text(
                text = if ((selectedFolders.size + selectedVideos.size) == 1) {
                    stringResource(R.string.delete_item_info)
                } else {
                    stringResource(R.string.delete_items_info)
                },
                style = MaterialTheme.typography.titleSmall,
            )
        },
    )
}

@Composable
private fun NetworkUrlDialog(
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

@Composable
private fun FloatingSelectionBar(
    modifier: Modifier = Modifier,
    show: Boolean,
    showRenameAction: Boolean,
    showInfoAction: Boolean,
    backdrop: PlatformBackdrop? = null,
    onPlayAction: () -> Unit,
    onCopyAction: () -> Unit,
    onMoveAction: () -> Unit,
    onRenameAction: () -> Unit,
    onInfoAction: () -> Unit,
    onDeleteAction: () -> Unit,
) {
    AnimatedVisibility(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        visible = show,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
        ) + fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val shape = CircleShape
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            val layer = rememberGraphicsLayer()
            val luminance = rememberGlassLuminance(layer, enabled = backdrop != null)
            val interaction = rememberGlassInteraction()

            val glassModifier = if (backdrop != null) {
                Modifier.drawInteractiveGlass(
                    isDark = isDark,
                    backdrop = backdrop,
                    layer = layer,
                    luminance = { luminance.value },
                    shape = shape,
                    interaction = interaction,
                    pressedScale = 1.02f,
                    blurScale = 1.25f,
                    minScrim = 0.12f,
                    maxScrim = 0.40f,
                )
            } else {
                Modifier
                    .clip(shape)
                    .background(
                        color = if (isDark) Color(0xFF161616).copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
                        shape = shape,
                    )
            }

            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 16.dp,
                        shape = shape,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        ambientColor = Color.Black.copy(alpha = 0.5f),
                    )
                    .then(glassModifier)
                    .liquidGlassBorderLaser(
                        laserColor = Color(0xFFFFD54F),
                        speedMillis = 3600,
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FloatingBarIconButton(
                    icon = NextIcons.Play,
                    contentDescription = stringResource(R.string.play),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onPlayAction,
                )
                FloatingBarIconButton(
                    icon = NextIcons.Copy,
                    contentDescription = stringResource(R.string.copy),
                    onClick = onCopyAction,
                )
                FloatingBarIconButton(
                    icon = NextIcons.Move,
                    contentDescription = stringResource(R.string.move),
                    onClick = onMoveAction,
                )
                if (showRenameAction) {
                    FloatingBarIconButton(
                        icon = NextIcons.Edit,
                        contentDescription = stringResource(R.string.rename),
                        onClick = onRenameAction,
                    )
                }
                if (showInfoAction) {
                    FloatingBarIconButton(
                        icon = NextIcons.Info,
                        contentDescription = stringResource(id = R.string.info),
                        onClick = onInfoAction,
                    )
                }
                FloatingBarIconButton(
                    iconRes = NextIcons.DeleteCustom,
                    contentDescription = stringResource(id = R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteAction,
                )
            }
        }
    }
}

@Composable
private fun FloatingBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .bouncyPress()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
    }
}

@Composable
private fun FloatingBarIconButton(
    iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .bouncyPress()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
    }
}


@PreviewScreenSizes
@PreviewLightDark
@Composable
private fun MediaPickerScreenPreview(
    @PreviewParameter(VideoPickerPreviewParameterProvider::class)
    videos: List<Video>,
) {
    NextPlayerTheme {
        MediaPickerScreen(
            uiState = MediaPickerUiState(
                folderName = null,
                mediaDataState = DataState.Success(
                    value = Folder(
                        name = "Root Folder",
                        path = "/root",
                        dateModified = System.currentTimeMillis(),
                        folderList = listOf(
                            Folder(name = "Folder 1", path = "/root/folder1", dateModified = System.currentTimeMillis()),
                            Folder(name = "Folder 2", path = "/root/folder2", dateModified = System.currentTimeMillis()),
                        ),
                        mediaList = videos,
                    ),
                ),
                preferences = ApplicationPreferences().copy(
                    mediaViewMode = MediaViewMode.FOLDER_TREE,
                    mediaLayoutMode = MediaLayoutMode.GRID,
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun ButtonPreview() {
    Surface {
        TextIconToggleButton(
            text = "Title",
            icon = NextIcons.Title,
            onClick = {},
        )
    }
}

@DayNightPreview
@Composable
private fun MediaPickerNoVideosFoundPreview() {
    NextPlayerTheme {
        Surface {
            MediaPickerScreen(
                uiState = MediaPickerUiState(
                    folderName = null,
                    mediaDataState = DataState.Success(null),
                    preferences = ApplicationPreferences(),
                ),
            )
        }
    }
}

@DayNightPreview
@Composable
private fun MediaPickerLoadingPreview() {
    NextPlayerTheme {
        Surface {
            MediaPickerScreen(
                uiState = MediaPickerUiState(
                    folderName = null,
                    mediaDataState = DataState.Loading,
                    preferences = ApplicationPreferences(),
                ),
            )
        }
    }
}

@Composable
private fun FolderMoreMenuContent(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    folder: Folder,
    currentLayoutMode: MediaLayoutMode,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onSelectVideos: () -> Unit,
    onSortClick: () -> Unit,
    onToggleLayoutMode: () -> Unit,
    onRefresh: () -> Unit,
    onShowFolderInfo: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
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
            text = { Text("Play All (${folder.allMediaList.size})") },
            onClick = onPlayAll,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.primary,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.Play,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Shuffle All") },
            onClick = onShuffleAll,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.secondary,
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        )
        DropdownMenuItem(
            text = { Text("Select Videos") },
            onClick = onSelectVideos,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.SelectAll,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Sort By...") },
            onClick = onSortClick,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        )
        DropdownMenuItem(
            text = {
                Text(if (currentLayoutMode == MediaLayoutMode.GRID) "View as List" else "View as Grid")
            },
            onClick = onToggleLayoutMode,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = if (currentLayoutMode == MediaLayoutMode.GRID) {
                        Icons.AutoMirrored.Rounded.ViewList
                    } else {
                        Icons.Default.GridView
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        )
        DropdownMenuItem(
            text = { Text("Refresh Folder") },
            onClick = onRefresh,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Folder Info") },
            onClick = onShowFolderInfo,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        )
    }
}

@Composable
private fun NavigationMoreMenuContent(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onOpenLocalVideo: () -> Unit,
    onOpenNetworkStream: () -> Unit,
    onPlayRecentlyPlayed: () -> Unit,
    recentlyPlayedEnabled: Boolean,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f),
                    ),
                ),
                shape = RoundedCornerShape(20.dp),
            ),
        containerColor = Color(0xEE14151B),
        shadowElevation = 16.dp,
        tonalElevation = 6.dp,
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.open_local_video)) },
            onClick = onOpenLocalVideo,
            colors = MenuDefaults.itemColors(textColor = Color.White),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.FileOpen,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                )
            }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.open_network_stream)) },
            onClick = onOpenNetworkStream,
            colors = MenuDefaults.itemColors(textColor = Color.White),
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.Link,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                )
            }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.recently_played)) },
            enabled = recentlyPlayedEnabled,
            onClick = onPlayRecentlyPlayed,
            colors = MenuDefaults.itemColors(
                textColor = Color.White,
                disabledTextColor = Color.White.copy(alpha = 0.38f),
                leadingIconColor = MaterialTheme.colorScheme.primary,
                disabledLeadingIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            ),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = NextIcons.ContinuePlay),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        )
    }
}

@Composable
private fun MediaNavigationBar(
    selectedTab: MediaPickerTab,
    onTabSelected: (MediaPickerTab) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMoreClick: () -> Unit,
    moreMenuContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationBarStyle: String = "CURVED_DOCK",
    backdrop: PlatformBackdrop? = null,
) {
    val selectedIndex = when (selectedTab) {
        MediaPickerTab.HOME -> 0
        MediaPickerTab.SEARCH -> 1
        MediaPickerTab.LIBRARY -> 2
        MediaPickerTab.SETTINGS -> 3
    }

    MochyNavigationDock(
        selectedTabIndex = selectedIndex,
        onTabSelected = { index ->
            when (index) {
                0 -> onTabSelected(MediaPickerTab.HOME)
                1 -> onTabSelected(MediaPickerTab.SEARCH)
                2 -> onTabSelected(MediaPickerTab.LIBRARY)
                3 -> onTabSelected(MediaPickerTab.SETTINGS)
            }
        },
        modifier = modifier
    )
}



enum class MediaPickerTab {
    HOME,
    SEARCH,
    LIBRARY,
    SETTINGS
}

@Composable
private fun AnimatedTelegramButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: PlatformBackdrop? = null,
) {
    LiquidGlassIconButton(
        onClick = onClick,
        size = 38.dp,
        containerColor = Color.White.copy(alpha = 0.08f),
        borderBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.08f),
            ),
        ),
        backdrop = backdrop,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = NextIcons.TelegramMonochrome),
            contentDescription = "Telegram Cloud",
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}
