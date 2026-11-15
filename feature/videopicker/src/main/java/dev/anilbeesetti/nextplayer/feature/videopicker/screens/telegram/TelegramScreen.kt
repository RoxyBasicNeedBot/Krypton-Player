@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.anilbeesetti.nextplayer.feature.videopicker.screens.telegram

import android.net.Uri
import dev.anilbeesetti.nextplayer.core.ui.theme.LocalMediaController
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.rememberPlayerState
import androidx.media3.common.Player
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.*
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreVert
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.hilt.navigation.compose.hiltViewModel
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Image
import coil3.compose.AsyncImage
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramLogo
import dev.anilbeesetti.nextplayer.core.ui.components.LottieChannelLogo
import dev.anilbeesetti.nextplayer.core.ui.components.LottieLibraryLogo
import dev.anilbeesetti.nextplayer.core.ui.components.LottieVerifiedBadge
import dev.anilbeesetti.nextplayer.core.ui.components.LottieHeartLikeButton
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramBoost
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramDelete
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramDownload
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramCheck
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.PremiumMeshBackground
import dev.anilbeesetti.nextplayer.core.ui.theme.NextColors
import dev.anilbeesetti.nextplayer.core.ui.components.AmbientThemeGlow
import dev.anilbeesetti.nextplayer.core.ui.components.AnimatedStickerImage
import dev.anilbeesetti.nextplayer.core.ui.components.LottieStickerImage
import dev.anilbeesetti.nextplayer.core.ui.components.ChannelLoadingPlaceholder
import dev.anilbeesetti.nextplayer.core.ui.R as UiR

import dev.anilbeesetti.nextplayer.core.ui.liquidglass.PlatformBackdrop
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.drawInteractiveGlass
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.layerBackdrop
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.rememberBackdrop
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.rememberGlassInteraction
import dev.anilbeesetti.nextplayer.core.ui.liquidglass.liquidGlassBorderLaser
import dev.anilbeesetti.nextplayer.core.ui.components.bouncyPress
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle

import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.LiveTv
import kotlinx.coroutines.launch

enum class SearchUiMode {
    NONE,
    LIST,
    CHAT
}

@Composable
fun TelegramRoute(
    viewModel: TelegramViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onPlayVideo: (Uri, String, String?, String?) -> Unit,
    onImageClick: (List<Video>, Int) -> Unit,
    onReelsClick: (List<Video>, Int, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadedKeys by viewModel.downloadedKeys.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showDownloadedOnly by viewModel.showDownloadedOnly.collectAsState()
    val showStats by viewModel.showStats.collectAsState()
    val playerPreferences by viewModel.playerPreferences.collectAsState()
    val isLinking by viewModel.isLinking.collectAsState()
    val linkError by viewModel.linkError.collectAsState()
    val context = LocalContext.current

    // Watch Party UI states
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinRoomCode by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }
    var isJoiningRoom by remember { mutableStateOf(false) }

    var showCreatedDialog by remember { mutableStateOf(false) }
    var createdRoomCode by remember { mutableStateOf<String?>(null) }
    var createdVideo by remember { mutableStateOf<Video?>(null) }
    var activeVideoOptions by remember { mutableStateOf<Video?>(null) }
    val channels by viewModel.filteredChannels.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val channelVideos by viewModel.filteredChannelVideos.collectAsState()
    val isChannelLoading by viewModel.isChannelLoading.collectAsState()
    val continueWatchingCloudVideo by viewModel.continueWatchingCloudVideo.collectAsState()
    val continueWatchingChannelVideo by viewModel.continueWatchingChannelVideo.collectAsState()

    var showShareDialog by remember { mutableStateOf<Video?>(null) }
    var showStorageSettings by remember { mutableStateOf(false) }
    var showTdlibDiagnostics by remember { mutableStateOf(false) }

    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(createdRoomCode, showCreatedDialog) {
        val code = createdRoomCode
        if (code != null && showCreatedDialog) {
            val inviteLink = dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.syncJoinUrl(code)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    qrCodeBitmap = dev.anilbeesetti.nextplayer.core.ui.utils.QrCodeGenerator.generate(inviteLink, 400)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            qrCodeBitmap = null
        }
    }

    if (playerPreferences.telegramToken.isEmpty()) {
        TelegramConnectScreen(
            isLinking = isLinking,
            linkError = linkError,
            onLinkClick = { code ->
                viewModel.linkAccount(code) { _ -> }
            },
            onNavigateUp = onNavigateUp
        )
    } else {
        TelegramScreen(
            uiState = uiState,
            downloadProgressMap = downloadProgress,
            searchQuery = searchQuery,
            sortOrder = sortOrder,
            showDownloadedOnly = showDownloadedOnly,
            showStats = showStats,
            telegramFirstName = playerPreferences.telegramFirstName,
            telegramUsername = playerPreferences.telegramUsername,
            telegramProfilePhoto = playerPreferences.telegramProfilePhoto,
            channels = channels,
            selectedChannel = selectedChannel,
            channelVideos = channelVideos,
            isChannelLoading = isChannelLoading,
            continueWatchingCloudVideo = continueWatchingCloudVideo,
            continueWatchingChannelVideo = continueWatchingChannelVideo,
            onLoadMoreChannelVideos = viewModel::loadMoreChannelVideos,
            onSearchQueryChange = viewModel::setSearchQuery,
            onSortOrderChange = viewModel::setSortOrder,
            onShowDownloadedOnlyChange = viewModel::setShowDownloadedOnly,
            onToggleStats = viewModel::toggleStats,
            onDisconnect = viewModel::disconnectAccount,
            onNavigateUp = onNavigateUp,
            onPlayVideo = { uri, title -> onPlayVideo(uri, title, null, null) },
            onImageClick = onImageClick,
            onReelsClick = onReelsClick,
            onRefresh = { 
                viewModel.fetchVideos()
                viewModel.fetchChannels()
                viewModel.selectedChannel.value?.let { viewModel.selectChannel(it) }
                viewModel.refreshUserProfile()
            },
            onDelete = { video -> video.externalId?.let { viewModel.deleteVideo(it) } },
            onDownload = { video -> viewModel.downloadToWatch(video) },
            onItemLongClick = { video -> activeVideoOptions = video },
            onJoinWatchPartyClick = { showJoinDialog = true },
            onOpenStorageSettings = { showStorageSettings = true },
            onOpenTdlibDiagnostics = { showTdlibDiagnostics = true },
            onAddChannel = { username -> 
                viewModel.addChannel(username) { success, msg -> 
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onRemoveChannel = viewModel::removeChannel,
            onSelectChannel = viewModel::selectChannel,
            viewModel = viewModel
        )
    }

    if (showStorageSettings) {
        StorageControlBottomSheet(
            viewModel = viewModel,
            telegramCacheManager = viewModel.telegramCacheManager,
            onPlayMedia = { file, name ->
                showStorageSettings = false
                onPlayVideo(android.net.Uri.fromFile(file), name, null, null)
            },
            onDismissRequest = { showStorageSettings = false }
        )
    }

    // TDLib engine diagnostics UI disabled

    // Video Options Dialog (Two-Tier Download / Watch Party / Delete)
    if (activeVideoOptions != null) {
        val video = activeVideoOptions!!
        val isOffline = remember(video, downloadedKeys) { viewModel.isVideoDownloaded(video, downloadedKeys) }

        AlertDialog(
            onDismissRequest = { activeVideoOptions = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(video.nameWithExtension, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isOffline) {
                        ListItem(
                            headlineContent = { Text("Play") },
                            supportingContent = { Text("Instant 0-buffer playback from in-app cache") },
                            leadingContent = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                activeVideoOptions = null
                                onPlayVideo(viewModel.getPlayableUri(video), video.nameWithExtension, null, null)
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Remove from In-App Cache") },
                            supportingContent = { Text("Delete local copy to free phone storage") },
                            leadingContent = { LottieTelegramDelete(size = 24.dp, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                activeVideoOptions = null
                                viewModel.deleteFromOffline(video)
                            }
                        )
                    } else {
                        ListItem(
                            headlineContent = { Text("Download to In-App Cache") },
                            supportingContent = { Text("Save inside app for 100% offline instant playback") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                activeVideoOptions = null
                                viewModel.downloadToWatch(video)
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text("Save to Gallery") },
                        supportingContent = { Text("Export to Movies/KryptonPlayer on device storage") },
                        leadingContent = { Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color(0xFF4CAF50)) },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                            activeVideoOptions = null
                            viewModel.saveToGallery(video)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Start Watch Party") },
                        supportingContent = { Text("Watch synchronously with friends and chat") },
                        leadingContent = { Icon(imageVector = Icons.Filled.LiveTv, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                            activeVideoOptions = null
                            val fileId = video.uriString.substringAfterLast("/")
                            viewModel.createSyncRoom(fileId) { roomId, err ->
                                if (roomId != null) {
                                    createdRoomCode = roomId
                                    createdVideo = video
                                    showCreatedDialog = true
                                } else {
                                    Toast.makeText(context, err ?: "Failed to create Watch Party", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Share Link") },
                        supportingContent = { Text("Generate a temporary public web link") },
                        leadingContent = { Icon(imageVector = Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                            showShareDialog = video
                            activeVideoOptions = null
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Delete from Cloud", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { LottieTelegramDelete(size = 24.dp, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                            video.externalId?.let { viewModel.deleteVideo(it) }
                            activeVideoOptions = null
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeVideoOptions = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Share Link Dialog
    if (showShareDialog != null) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        AlertDialog(
            onDismissRequest = { showShareDialog = null },
            title = { Text("Share Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Generate a temporary link for ${showShareDialog!!.nameWithExtension}. Anyone with this link can stream or download the file from a browser.")
                    Text("Select Expiry:", style = MaterialTheme.typography.labelLarge)
                    
                    val options = listOf(
                        "1 Hour" to 1,
                        "24 Hours" to 24,
                        "7 Days" to 168
                    )
                    
                    options.forEach { (label, hours) ->
                        OutlinedButton(
                            onClick = {
                                val video = showShareDialog!!
                                val fileId = video.uriString.substringAfterLast("/")
                                showShareDialog = null
                                viewModel.createShareLink(fileId, hours) { url ->
                                    if (url != null) {
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Share Link", url))
                                        Toast.makeText(context, "Link copied! Expires in $label", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to generate link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Join Watch Party Dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showJoinDialog = false 
                joinError = null
            },
            title = { Text("Join Watch Party") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the Room Code shared by your friend to watch synchronously.")
                    OutlinedTextField(
                        value = joinRoomCode,
                        onValueChange = { 
                            joinRoomCode = it.uppercase()
                            joinError = null
                        },
                        label = { Text("Room Code") },
                        placeholder = { Text("ROXY-XXXX") },
                        singleLine = true,
                        isError = joinError != null,
                        supportingText = {
                            if (joinError != null) {
                                Text(joinError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinRoomCode.isBlank()) {
                            joinError = "Code cannot be empty"
                            return@Button
                        }
                        isJoiningRoom = true
                        viewModel.joinSyncRoom(joinRoomCode) { streamUrl, videoName, err ->
                            isJoiningRoom = false
                            if (err != null) {
                                joinError = err
                            } else if (streamUrl != null) {
                                showJoinDialog = false
                                onPlayVideo(
                                    Uri.parse(streamUrl),
                                    videoName ?: "Watch Party",
                                    joinRoomCode,
                                    playerPreferences.telegramToken
                                )
                            }
                        }
                    },
                    enabled = !isJoiningRoom && joinRoomCode.isNotBlank()
                ) {
                    if (isJoiningRoom) {
                        LoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.secondary)
                    } else {
                        Text("Join")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showJoinDialog = false 
                    joinError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Room Created Dialog
    if (showCreatedDialog && createdRoomCode != null && createdVideo != null) {
        val roomId = createdRoomCode!!
        val inviteLink = dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.syncJoinUrl(roomId)

        AlertDialog(
            onDismissRequest = { showCreatedDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Watch Party Created") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Room Code Display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Room Code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Watch Party Room Code", roomId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = roomId,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // QR Code Image
                    if (qrCodeBitmap != null) {
                        Image(
                            bitmap = qrCodeBitmap!!.asImageBitmap(),
                            contentDescription = "Watch Party QR Code",
                            modifier = Modifier
                                .size(160.dp)
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Invite Link Display & Copy Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = inviteLink,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Watch Party Invite Link", inviteLink)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                                contentDescription = "Copy Link",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Share the room code or invite link. Friends can join using the link or by entering the code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreatedDialog = false
                        onPlayVideo(
                            Uri.parse(createdVideo!!.uriString),
                            createdVideo!!.nameWithExtension,
                            createdRoomCode,
                            playerPreferences.telegramToken
                        )
                    }
                ) {
                    Text("Start Watch Party")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TelegramScreen(
    uiState: TelegramUiState,
    downloadProgressMap: Map<String, Float>,
    searchQuery: String,
    sortOrder: TelegramSortOrder,
    showDownloadedOnly: Boolean,
    showStats: Boolean,
    telegramFirstName: String,
    telegramUsername: String,
    telegramProfilePhoto: String,
    channels: List<dev.anilbeesetti.nextplayer.core.data.network.models.ChannelResponse>,
    selectedChannel: dev.anilbeesetti.nextplayer.core.data.network.models.ChannelResponse?,
    channelVideos: List<Video>,
    isChannelLoading: Boolean,
    continueWatchingCloudVideo: Video?,
    continueWatchingChannelVideo: Video?,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (TelegramSortOrder) -> Unit,
    onShowDownloadedOnlyChange: (Boolean) -> Unit,
    onToggleStats: () -> Unit,
    onDisconnect: () -> Unit,
    onNavigateUp: () -> Unit,
    onPlayVideo: (Uri, String) -> Unit,
    onImageClick: (List<Video>, Int) -> Unit,
    onReelsClick: (List<Video>, Int, String) -> Unit,
    onRefresh: () -> Unit,
    onDelete: (Video) -> Unit,
    onDownload: (Video) -> Unit,
    onItemLongClick: (Video) -> Unit,
    onJoinWatchPartyClick: () -> Unit,
    onOpenStorageSettings: () -> Unit = {},
    onOpenTdlibDiagnostics: () -> Unit = {},
    onAddChannel: (String) -> Unit,
    onRemoveChannel: (Long) -> Unit,
    onSelectChannel: (dev.anilbeesetti.nextplayer.core.data.network.models.ChannelResponse?) -> Unit,
    onLoadMoreChannelVideos: () -> Unit = {},
    viewModel: TelegramViewModel,
) {
    // 1. FLAG_SECURE ONLY on Telegram cloud screen (Release hygiene)
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // 2. Security verdict re-checked on every resume
    var securityVerdict by remember { 
        mutableStateOf(dev.anilbeesetti.nextplayer.core.common.AppSecurityManager.verifyTelegramCloudAccess(context)) 
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                securityVerdict = dev.anilbeesetti.nextplayer.core.common.AppSecurityManager.verifyTelegramCloudAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (securityVerdict is dev.anilbeesetti.nextplayer.core.common.SecurityVerdict.Blocked) {
        val blocked = securityVerdict as dev.anilbeesetti.nextplayer.core.common.SecurityVerdict.Blocked
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Security Notice",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = blocked.reason,
                    fontSize = 14.sp,
                    color = Color(0xFFA1A1AA),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (blocked.openSettingsAction) {
                    Button(
                        onClick = {
                            try {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                                } catch (_: Exception) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Developer Settings")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                OutlinedButton(
                    onClick = onNavigateUp,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF2E2E32)),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val backdrop = rememberBackdrop()
    val continueFabInteraction = rememberGlassInteraction()
    val pillInteraction = rememberGlassInteraction()
    val reelsFabInteraction = rememberGlassInteraction()
    val topBarInteraction = rememberGlassInteraction()

    LaunchedEffect(selectedTabIndex) {
        val tabName = when (selectedTabIndex) {
            0 -> "My Cloud"
            1 -> "Channels Feed"
            2 -> "Library / Downloads"
            else -> "Tab $selectedTabIndex"
        }
        dev.anilbeesetti.nextplayer.core.common.CrashBreadcrumbs.recordNav("Telegram -> $tabName")
    }

    val downloadedKeys by viewModel.downloadedKeys.collectAsState()
    val activeDownloadTasks by viewModel.activeDownloadTasks.collectAsState()
    // Keep the last cards above the download bar and floating navigation.
    val listBottomPadding = 24.dp
    val cloudLazyListState = rememberLazyListState()
    val channelLazyListState = rememberLazyListState()
    val mediaController = LocalMediaController.current
    val isMiniPlayerVisible = mediaController?.let { controller ->
        val playerState = rememberPlayerState(controller)
        playerState.currentMediaItem != null && playerState.playbackState != Player.STATE_IDLE
    } ?: false

    // Search and navigation states
    var searchUiMode by rememberSaveable { mutableStateOf(SearchUiMode.NONE) }
    var typedQuery by rememberSaveable { mutableStateOf("") }
    var debouncedTypedQuery by rememberSaveable { mutableStateOf("") }
    var channelSearchFilter by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(typedQuery) {
        kotlinx.coroutines.delay(300)
        debouncedTypedQuery = typedQuery
    }

    val allChannelPosts by viewModel.channelPosts.collectAsState()
    var matchingPosts by remember { mutableStateOf(emptyList<dev.anilbeesetti.nextplayer.core.data.network.models.ChannelPostDto>()) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedChannel) {
        typedQuery = ""
        debouncedTypedQuery = ""
        searchUiMode = SearchUiMode.NONE
    }

    LaunchedEffect(debouncedTypedQuery, allChannelPosts, selectedChannel?.channel_id) {
        val q = debouncedTypedQuery.trim().lowercase()
        val curCh = selectedChannel
        val channelAbsId = curCh?.channel_id?.let { kotlin.math.abs(it) } ?: 0L
        val bareId = if (channelAbsId > 1000000000000L) channelAbsId - 1000000000000L else channelAbsId

        matchingPosts = if (q.isBlank()) {
            emptyList()
        } else {
            allChannelPosts.map { post ->
                if (curCh != null && post.channelId == 0L) post.copy(channelId = curCh.channel_id) else post
            }.filter { post ->
                val postMatchesChannel = if (curCh == null) true else {
                    val postAbs = kotlin.math.abs(post.channelId)
                    val postBare = if (postAbs > 1000000000000L) postAbs - 1000000000000L else postAbs
                    postBare == bareId
                }
                if (!postMatchesChannel) return@filter false

                val captionMatch = post.caption?.lowercase()?.contains(q) == true
                val fileNameMatch = post.fileName?.lowercase()?.contains(q) == true
                val albumItemMatch = post.mediaItems?.any { item ->
                    (item.fileName ?: "").lowercase().contains(q)
                } == true
                val messageIdMatch = post.messageId.toString().contains(q)
                captionMatch || fileNameMatch || albumItemMatch || messageIdMatch
            }.distinctBy { it.postId }
        }
        if (currentMatchIndex >= matchingPosts.size) {
            currentMatchIndex = 0
        }
    }


    var scrollToPostId by remember { mutableStateOf<String?>(null) }
    var scrollToTopTrigger by remember { mutableStateOf(false) }
    var scrollToVideoId by remember { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentMatchIndex, matchingPosts) {
        if (searchUiMode == SearchUiMode.CHAT && matchingPosts.isNotEmpty()) {
            scrollToPostId = matchingPosts[currentMatchIndex].postId
        }
    }

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var cloudTypedQuery by rememberSaveable { mutableStateOf(searchQuery) }
    LaunchedEffect(searchQuery) {
        if (cloudTypedQuery != searchQuery) {
            cloudTypedQuery = searchQuery
        }
    }
    
    LaunchedEffect(cloudTypedQuery) {
        kotlinx.coroutines.delay(500)
        if (cloudTypedQuery != searchQuery) {
            onSearchQueryChange(cloudTypedQuery)
        }
    }

    // Scroll to target match in Cloud search results
    LaunchedEffect(scrollToVideoId, uiState) {
        val id = scrollToVideoId
        if (id != null && uiState is TelegramUiState.Success) {
            val distinctVideos = uiState.videos.distinctBy { it.id }
            val index = distinctVideos.indexOfFirst { it.id == id }
            if (index >= 0) {
                cloudLazyListState.animateScrollToItem(index)
                scrollToVideoId = null
            }
        }
    }

    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showDownloadsSheet by rememberSaveable { mutableStateOf(false) }
    var showCloudSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showAddChannelDialog by remember { mutableStateOf(false) }
    var showAccountSwitcherSheet by remember { mutableStateOf(false) }
    val playerPreferences by viewModel.playerPreferences.collectAsState()
    val savedAccounts by viewModel.savedAccounts.collectAsState()
    val tdlibLiveStats by viewModel.tdlibStreamingStats.collectAsState()
    val tdlibAuth by viewModel.tdlibAuthState.collectAsState()
    val statusColor = when {
        tdlibLiveStats != null -> Color(0xFF00E5FF)
        tdlibAuth is dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibManager.AuthState.Ready -> Color(0xFF00E676)
        tdlibAuth is dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibManager.AuthState.Initializing -> Color(0xFFFFA000)
        else -> Color(0xFFFF7043)
    }
    var showSwitchChannelSheet by remember { mutableStateOf(false) }
    var showWatchLaterSheet by remember { mutableStateOf(false) }
    var channelToRemoveConfirm by remember { mutableStateOf<dev.anilbeesetti.nextplayer.core.data.network.models.ChannelResponse?>(null) }
    var boostChannelTarget by remember { mutableStateOf<dev.anilbeesetti.nextplayer.core.data.network.models.ChannelResponse?>(null) }
    var isTurboBoostActive by rememberSaveable { mutableStateOf(false) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    // Batch selection state
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    // Exit selection/search mode on back press
    if (isSelectionMode) {
        androidx.activity.compose.BackHandler {
            isSelectionMode = false
            selectedIds.clear()
        }
    }
    if (searchUiMode != SearchUiMode.NONE) {
        androidx.activity.compose.BackHandler {
            if (searchUiMode == SearchUiMode.CHAT) {
                searchUiMode = SearchUiMode.LIST
            } else {
                searchUiMode = SearchUiMode.NONE
                typedQuery = ""
                viewModel.setSearchQuery("")
            }
        }
    }
    if (selectedChannel != null && !isSelectionMode && searchUiMode == SearchUiMode.NONE && !isSearchActive) {
        androidx.activity.compose.BackHandler {
            onSelectChannel(null)
        }
    }
    if (selectedTabIndex == 1 && selectedChannel == null && channelSearchFilter.isNotEmpty() && !isSelectionMode && searchUiMode == SearchUiMode.NONE && !isSearchActive) {
        androidx.activity.compose.BackHandler {
            channelSearchFilter = ""
        }
    }
    if (selectedTabIndex != 0 && selectedChannel == null && channelSearchFilter.isEmpty() && !isSelectionMode && searchUiMode == SearchUiMode.NONE && !isSearchActive) {
        androidx.activity.compose.BackHandler {
            selectedTabIndex = 0
        }
    }
    if (isSearchActive) {
        androidx.activity.compose.BackHandler {
            isSearchActive = false
            cloudTypedQuery = ""
            onSearchQueryChange("")
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(androidx.compose.material3.rememberTopAppBarState())
    PremiumMeshBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = Color.Transparent,
                topBar = {
                    if (isSelectionMode) {
                        // Selection mode top bar
                        val currentVideos = (uiState as? TelegramUiState.Success)?.videos ?: emptyList()
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    border = NextColors.cardBorder(),
                                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                                )
                                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        ) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "${selectedIds.size} selected",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        isSelectionMode = false
                                        selectedIds.clear()
                                    }) {
                                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Cancel")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = {
                                        if (selectedIds.size == currentVideos.size) {
                                            selectedIds.clear()
                                        } else {
                                            selectedIds.clear()
                                            selectedIds.addAll(currentVideos.map { it.id })
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.SelectAll,
                                            contentDescription = "Select All",
                                            tint = if (selectedIds.size == currentVideos.size && currentVideos.isNotEmpty())
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            currentVideos.filter { it.id in selectedIds }.forEach { onDownload(it) }
                                            isSelectionMode = false
                                            selectedIds.clear()
                                        },
                                        enabled = selectedIds.isNotEmpty()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = "Download Selected",
                                            tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            currentVideos.filter { it.id in selectedIds }.forEach { onDelete(it) }
                                            isSelectionMode = false
                                            selectedIds.clear()
                                        },
                                        enabled = selectedIds.isNotEmpty()
                                    ) {
                                        LottieTelegramDelete(
                                            size = 22.dp,
                                            isPlaying = selectedIds.isNotEmpty(),
                                            tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    scrolledContainerColor = Color.Transparent
                                )
                            )
                        }
                    } else if (isSearchActive) {
                        // Cloud search active top bar
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = NextColors.cardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    cloudTypedQuery = ""
                                    onSearchQueryChange("")
                                }) {
                                    Icon(imageVector = NextIcons.ArrowBack, contentDescription = "Back")
                                }
                                OutlinedTextField(
                                    value = cloudTypedQuery,
                                    onValueChange = {
                                        cloudTypedQuery = it
                                        // onSearchQueryChange is handled by LaunchedEffect debounce
                                    },
                                    placeholder = { Text("Search videos...", style = MaterialTheme.typography.bodyMedium) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            if (cloudTypedQuery.isNotEmpty()) {
                                                cloudTypedQuery = ""
                                                onSearchQueryChange("")
                                            } else {
                                                isSearchActive = false
                                                onSearchQueryChange("")
                                            }
                                        }) {
                                            Icon(imageVector = NextIcons.Close, contentDescription = "Close search")
                                        }
                                    }
                                )
                            }
                        }
                    } else if (selectedTabIndex == 1 && searchUiMode != SearchUiMode.NONE) {
                        // Channel search active top bar
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = NextColors.cardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    if (searchUiMode == SearchUiMode.CHAT) {
                                        searchUiMode = SearchUiMode.LIST
                                    } else {
                                        searchUiMode = SearchUiMode.NONE
                                        typedQuery = ""
                                        viewModel.setSearchQuery("")
                                    }
                                }) {
                                    Icon(imageVector = NextIcons.ArrowBack, contentDescription = "Back")
                                }
                                OutlinedTextField(
                                    value = typedQuery,
                                    onValueChange = {
                                        typedQuery = it
                                        viewModel.setSearchQuery(it)
                                    },
                                    placeholder = { Text("Search channel messages...", style = MaterialTheme.typography.bodyMedium) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            if (typedQuery.isNotEmpty()) {
                                                typedQuery = ""
                                                viewModel.setSearchQuery("")
                                            } else {
                                                searchUiMode = SearchUiMode.NONE
                                                viewModel.setSearchQuery("")
                                            }
                                        }) {
                                            Icon(imageVector = NextIcons.Close, contentDescription = "Close search")
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        // Normal top bar (frosted liquid glass)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (backdrop != null) {
                                        Modifier.drawInteractiveGlass(
                                            isDark = true,
                                            backdrop = backdrop,
                                            layer = null,
                                            luminance = { 0.5f },
                                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                                            interaction = topBarInteraction,
                                            minScrim = 0.08f,
                                            maxScrim = 0.28f,
                                        )
                                    } else {
                                        Modifier
                                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0x5512131A),
                                                        Color(0x7712131A),
                                                    ),
                                                ),
                                            )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.25f),
                                            Color.White.copy(alpha = 0.06f),
                                        ),
                                    ),
                                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                                )
                                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        ) {
                            TopAppBar(
                                title = {
                                    if (selectedTabIndex == 0) {
                                        // Profile / User header with Nagram Account Switcher Trigger
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { showAccountSwitcherSheet = true }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            val bitmap = remember(telegramProfilePhoto) {
                                                if (telegramProfilePhoto.isNotEmpty()) {
                                                    try {
                                                        val byteArray = Base64.decode(telegramProfilePhoto, Base64.DEFAULT)
                                                        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                                                    } catch (e: Exception) { null }
                                                } else null
                                            }
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Profile Photo",
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            brush = Brush.linearGradient(
                                                                colors = listOf(
                                                                    MaterialTheme.colorScheme.primary,
                                                                    MaterialTheme.colorScheme.secondary
                                                                )
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val initial = remember(telegramFirstName, telegramUsername) {
                                                        val name = telegramFirstName.ifEmpty { telegramUsername }
                                                        if (name.isNotEmpty()) name.take(1).uppercase() else "?"
                                                    }
                                                    Text(
                                                        text = initial,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Column {
                                                val displayName = remember(telegramFirstName, telegramUsername) {
                                                    telegramFirstName.ifEmpty { telegramUsername }
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = displayName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    LottieVerifiedBadge(size = 18.dp)
                                                }
                                                Text(
                                                    text = if (telegramUsername.isNotEmpty() && telegramUsername != "Unknown") "@$telegramUsername" else "Telegram Cloud",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else if (selectedTabIndex == 1) {
                                        // Channels tab
                                        if (selectedChannel != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(11.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(11.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (!selectedChannel.channel_photo.isNullOrEmpty()) {
                                                        val photoBytes = remember(selectedChannel.channel_photo) {
                                                            try {
                                                                Base64.decode(selectedChannel.channel_photo, Base64.DEFAULT)
                                                            } catch (e: Exception) { null }
                                                        }
                                                        if (photoBytes != null) {
                                                            val bitmap = remember(photoBytes) {
                                                                BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                                                            }
                                                            if (bitmap != null) {
                                                                Image(
                                                                    bitmap = bitmap.asImageBitmap(),
                                                                    contentDescription = "Channel Avatar",
                                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp)),
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        val fallbackTitle = selectedChannel.channel_title?.takeIf { it.isNotBlank() } ?: selectedChannel.channel_username?.takeIf { it.isNotBlank() } ?: "Channel"
                                                        Text(
                                                            text = fallbackTitle.trim().take(1).uppercase().ifEmpty { "C" },
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.titleSmall
                                                        )
                                                    }
                                                }
                                                Column {
                                                    Text(
                                                        text = selectedChannel.channel_title ?: selectedChannel.channel_username ?: "Channel",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val formattedSubs = remember(selectedChannel.member_count) {
                                                        val count = selectedChannel.member_count
                                                        if (count >= 1000) {
                                                            String.format("%.1fK", count / 1000f).replace(".0", "")
                                                        } else {
                                                            count.toString()
                                                        }
                                                    }
                                                    Text(
                                                        text = "$formattedSubs subscribers",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        } else {
                                            Column {
                                                Text(
                                                    text = "Channels",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        // Library tab (SimpMusic Style)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "Telegram Library",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        if (selectedTabIndex == 1 && selectedChannel != null) {
                                            onSelectChannel(null)
                                        } else if (selectedTabIndex != 0) {
                                            selectedTabIndex = 0
                                        } else {
                                            onNavigateUp()
                                        }
                                    }) {
                                        Icon(imageVector = NextIcons.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                actions = {
                                    if (selectedTabIndex == 0) {
                                        IconButton(onClick = { isSearchActive = true }) {
                                            Icon(
                                                painter = painterResource(id = NextIcons.TelegramNavSearch),
                                                contentDescription = "Search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { showDownloadsSheet = true }) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (activeDownloadTasks.isNotEmpty()) {
                                                    val isDownloading = activeDownloadTasks.values.any { !it.isPaused }
                                                    LottieTelegramDownload(
                                                        size = 24.dp,
                                                        isPlaying = isDownloading,
                                                        tint = Color(0xFF2AABEE)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = 6.dp, y = (-4).dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                Brush.linearGradient(
                                                                    listOf(Color(0xFF2AABEE), Color(0xFF229ED9))
                                                                )
                                                            )
                                                            .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "${activeDownloadTasks.size}",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Download,
                                                        contentDescription = "Downloads",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Box {
                                            var menuExpanded by remember { mutableStateOf(false) }
                                            IconButton(onClick = { menuExpanded = true }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MoreVert,
                                                    contentDescription = "More options",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = menuExpanded,
                                                onDismissRequest = { menuExpanded = false },
                                                shape = RoundedCornerShape(20.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            ) {
                                                NagramSubMenuItem(
                                                    icon = Icons.Default.Settings,
                                                    iconTint = Color(0xFF2AABEE),
                                                    title = "Telegram Cloud Settings",
                                                    subtitle = "Auto-downloads, storage & playback options",
                                                    onClick = {
                                                        menuExpanded = false
                                                        showCloudSettingsSheet = true
                                                    }
                                                )

                                                NagramSubMenuItem(
                                                    icon = Icons.Default.Storage,
                                                    iconTint = MaterialTheme.colorScheme.primary,
                                                    title = "Data and Storage",
                                                    subtitle = "Manage disk cache & auto-download",
                                                    onClick = {
                                                        menuExpanded = false
                                                        onOpenStorageSettings()
                                                    }
                                                )
                                                NagramSubMenuItem(
                                                    painter = painterResource(id = NextIcons.TelegramNavSort),
                                                    iconTint = Color(0xFF2AABEE),
                                                    title = "Sort & Filter",
                                                    subtitle = "Order by date, size, or name",
                                                    onClick = {
                                                        menuExpanded = false
                                                        showSortDialog = true
                                                    }
                                                )
                                                NagramSubMenuItem(
                                                    painter = painterResource(id = NextIcons.TelegramNavStats),
                                                    iconTint = Color(0xFF9C27B0),
                                                    title = if (showStats) "Hide Cloud Stats" else "Show Cloud Stats",
                                                    onClick = {
                                                        menuExpanded = false
                                                        onToggleStats()
                                                    }
                                                )
                                                 NagramSubMenuItem(
                                                    painter = painterResource(id = NextIcons.TelegramNavRefresh),
                                                    iconTint = Color(0xFF4CAF50),
                                                    title = "Refresh",
                                                    onClick = {
                                                        menuExpanded = false
                                                        onRefresh()
                                                    }
                                                )
                                                NagramSubMenuItem(
                                                    icon = Icons.Filled.Schedule,
                                                    iconTint = Color(0xFFFF9800),
                                                    title = "Watch Later Queue",
                                                    onClick = {
                                                        menuExpanded = false
                                                        showWatchLaterSheet = true
                                                    }
                                                )
                                                NagramSubMenuItem(
                                                    iconContent = {
                                                        LottieTelegramBoost(
                                                            size = 22.dp,
                                                            tint = Color(0xFFFF9800)
                                                        )
                                                    },
                                                    iconTint = Color(0xFFFF9800),
                                                    title = "Turbo & Channel Boost (/boost)",
                                                    subtitle = "Speed booster & telegram boost link",
                                                    onClick = {
                                                        menuExpanded = false
                                                        if (selectedChannel != null) {
                                                            boostChannelTarget = selectedChannel
                                                        } else {
                                                            isTurboBoostActive = !isTurboBoostActive
                                                        }
                                                    }
                                                )
                                                NagramSubMenuItem(
                                                    icon = Icons.Rounded.School,
                                                    iconTint = Color(0xFF2196F3),
                                                    title = "Guides & Bot (/learn)",
                                                    subtitle = "Telegram bot tutorials & commands",
                                                    onClick = {
                                                        menuExpanded = false
                                                        uriHandler.openUri("https://t.me/kryptonplayerbot?start=learn")
                                                    }
                                                )
                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                NagramSubMenuItem(
                                                    painter = painterResource(id = NextIcons.TelegramLinkBroken),
                                                    iconTint = MaterialTheme.colorScheme.error,
                                                    title = "Disconnect Account",
                                                    isDestructive = true,
                                                    onClick = {
                                                        menuExpanded = false
                                                        onDisconnect()
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        // Channels Tab Actions
                                        if (selectedChannel != null) {
                                            IconButton(onClick = { searchUiMode = SearchUiMode.LIST }) {
                                                Icon(
                                                    painter = painterResource(id = NextIcons.TelegramNavSearch),
                                                    contentDescription = "Search",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            IconButton(onClick = { showAddChannelDialog = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add Channel",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        IconButton(onClick = { showDownloadsSheet = true }) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (activeDownloadTasks.isNotEmpty()) {
                                                    val isDownloading = activeDownloadTasks.values.any { !it.isPaused }
                                                    LottieTelegramDownload(
                                                        size = 24.dp,
                                                        isPlaying = isDownloading,
                                                        tint = Color(0xFF2AABEE)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = 6.dp, y = (-4).dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                Brush.linearGradient(
                                                                    listOf(Color(0xFF2AABEE), Color(0xFF229ED9))
                                                                )
                                                            )
                                                            .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "${activeDownloadTasks.size}",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Download,
                                                        contentDescription = "Downloads",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Box {
                                            var channelMenuExpanded by remember { mutableStateOf(false) }
                                            IconButton(onClick = { channelMenuExpanded = true }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MoreVert,
                                                    contentDescription = "More options",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = channelMenuExpanded,
                                                onDismissRequest = { channelMenuExpanded = false },
                                                shape = RoundedCornerShape(20.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            ) {
                                                NagramSubMenuItem(
                                                    icon = Icons.Default.Settings,
                                                    iconTint = Color(0xFF2AABEE),
                                                    title = "Telegram Cloud Settings",
                                                    subtitle = "Auto-downloads, storage & playback options",
                                                    onClick = {
                                                        channelMenuExpanded = false
                                                        showCloudSettingsSheet = true
                                                    }
                                                )

                                                NagramSubMenuItem(
                                                    icon = Icons.Default.Storage,
                                                    iconTint = MaterialTheme.colorScheme.primary,
                                                    title = "Data and Storage",
                                                    subtitle = "Manage disk cache & auto-download",
                                                    onClick = {
                                                        channelMenuExpanded = false
                                                        onOpenStorageSettings()
                                                    }
                                                )
                                                NagramSubMenuItem(
                                                    icon = Icons.Filled.Schedule,
                                                    iconTint = Color(0xFFFF9800),
                                                    title = "Watch Later Queue",
                                                    subtitle = "Manage background & offline downloads",
                                                    onClick = {
                                                        channelMenuExpanded = false
                                                        showWatchLaterSheet = true
                                                    }
                                                )
                                                if (selectedChannel != null) {
                                                    NagramSubMenuItem(
                                                        icon = NextIcons.ArrowUpward,
                                                        iconTint = Color(0xFF2AABEE),
                                                        title = "Go to Top",
                                                        onClick = {
                                                            channelMenuExpanded = false
                                                            scrollToTopTrigger = true
                                                        }
                                                    )
                                                }
                                                NagramSubMenuItem(
                                                    painter = painterResource(id = NextIcons.TelegramNavRefresh),
                                                    iconTint = Color(0xFF4CAF50),
                                                    title = "Refresh",
                                                    onClick = {
                                                        channelMenuExpanded = false
                                                        onRefresh()
                                                    }
                                                )
                                                NagramSubMenuItem(
                                                    icon = Icons.Default.Add,
                                                    iconTint = Color(0xFF009688),
                                                    title = "Add Channel",
                                                    onClick = {
                                                        channelMenuExpanded = false
                                                        showAddChannelDialog = true
                                                    }
                                                )
                                                if (channels.isNotEmpty()) {
                                                    NagramSubMenuItem(
                                                        icon = Icons.Default.Chat,
                                                        iconTint = Color(0xFFFF9800),
                                                        title = "Switch Channel",
                                                        onClick = {
                                                            channelMenuExpanded = false
                                                            showSwitchChannelSheet = true
                                                        }
                                                    )
                                                }
                                                if (selectedChannel != null) {
                                                    NagramSubMenuItem(
                                                        iconContent = {
                                                            LottieTelegramBoost(
                                                                size = 22.dp,
                                                                tint = Color(0xFFFF9800)
                                                            )
                                                        },
                                                        iconTint = Color(0xFFFF9800),
                                                        title = "Boost Channel (/boost)",
                                                        subtitle = "Open official Telegram boost link",
                                                        onClick = {
                                                            channelMenuExpanded = false
                                                            boostChannelTarget = selectedChannel
                                                        }
                                                    )
                                                    NagramSubMenuItem(
                                                        icon = Icons.Rounded.School,
                                                        iconTint = Color(0xFF2196F3),
                                                        title = "Guides & Bot (/learn)",
                                                        subtitle = "Telegram bot tutorials & commands",
                                                        onClick = {
                                                            channelMenuExpanded = false
                                                            uriHandler.openUri("https://t.me/kryptonplayerbot?start=learn")
                                                        }
                                                    )
                                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                    NagramSubMenuItem(
                                                        iconContent = {
                                                            LottieTelegramDelete(
                                                                size = 20.dp,
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        },
                                                        iconTint = MaterialTheme.colorScheme.error,
                                                        title = "Remove Current Channel",
                                                        isDestructive = true,
                                                        onClick = {
                                                            channelMenuExpanded = false
                                                            channelToRemoveConfirm = selectedChannel
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        }
                                    },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            ),
                            scrollBehavior = scrollBehavior
                        )
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Floating Bottom Navigation Bar + side action circles
        if (!isSearchActive && !isSelectionMode && searchUiMode == SearchUiMode.NONE) {
            val currentVideos2 = (uiState as? TelegramUiState.Success)?.videos ?: emptyList()
            val hasVideos2 = if (selectedTabIndex == 0) {
                currentVideos2.isNotEmpty()
            } else if (selectedTabIndex == 1) {
                selectedChannel != null && channelVideos.isNotEmpty()
            } else {
                viewModel.likedVideos.collectAsState().value.isNotEmpty() || currentVideos2.isNotEmpty()
            }
            val continueWatchingVideo2 = if (selectedTabIndex == 0) {
                continueWatchingCloudVideo ?: currentVideos2.firstOrNull()
            } else if (selectedTabIndex == 1) {
                continueWatchingChannelVideo ?: channelVideos.firstOrNull()
            } else {
                continueWatchingCloudVideo ?: currentVideos2.firstOrNull()
            }

            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp + if (isMiniPlayerVisible) 80.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left circle: Continue Watching in Liquid Glass
                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                if (continueWatchingVideo2 != null) {
                    Box(
                        modifier = Modifier
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
                                        interaction = continueFabInteraction,
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
                                laserColor = Color(0xFF00E5FF),
                                coreColor = Color(0xFFE0F7FA),
                                haloColor = Color(0xFF00B0FF),
                                speedMillis = 3600,
                                strokeWidth = 1.3.dp,
                                glowWidth = 3.5.dp,
                            )
                            .bouncyPress()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    onPlayVideo(
                                        viewModel.getPlayableUri(continueWatchingVideo2),
                                        continueWatchingVideo2.nameWithExtension
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = NextIcons.ContinuePlay),
                            contentDescription = "Continue Playing",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(56.dp))
                }

                // Centre: My Cloud | Channels | Library nav pill (ArchiveTune Expressive Style)
                BoxWithConstraints(
                    modifier = Modifier
                        .width(296.dp)
                        .height(56.dp)
                        .drawInteractiveGlass(
                            isDark = true,
                            backdrop = backdrop,
                            layer = null,
                            luminance = { 0.5f },
                            shape = RoundedCornerShape(28.dp),
                            interaction = pillInteraction,
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.08f),
                                ),
                            ),
                            shape = RoundedCornerShape(28.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val tabWidth = (maxWidth - 2.dp) / 3
                    val indicatorOffset by animateDpAsState(
                        targetValue = (tabWidth + 1.dp) * selectedTabIndex,
                        animationSpec = spring(
                            dampingRatio = 0.86f,
                            stiffness = 480f,
                        ),
                        label = "cloudTabIndicatorOffset",
                    )

                    Box(
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight()
                            .graphicsLayer {
                                translationX = indicatorOffset.toPx()
                            }
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color.White.copy(alpha = 0.08f),
                                    ),
                                ),
                            )
                            .border(
                                width = 0.75.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color.White.copy(alpha = 0.10f),
                                    ),
                                ),
                                shape = RoundedCornerShape(22.dp),
                            ),
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // My Cloud Button
                        val cloudColor by animateColorAsState(
                            targetValue = if (selectedTabIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(stiffness = 520f),
                            label = "cloudTabColor",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedTabIndex = 0 }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                LottieTelegramLogo(
                                    size = 22.dp,
                                    isPlaying = selectedTabIndex == 0
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "My Cloud",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = cloudColor,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
                        )

                        // Channels Button
                        val channelsColor by animateColorAsState(
                            targetValue = if (selectedTabIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(stiffness = 520f),
                            label = "channelsTabColor",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (selectedTabIndex == 1) {
                                            if (selectedChannel != null) {
                                                onSelectChannel(null)
                                            } else {
                                                coroutineScope.launch {
                                                    channelLazyListState.animateScrollToItem(0)
                                                }
                                            }
                                        } else {
                                            selectedTabIndex = 1
                                            onSelectChannel(null)
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                LottieChannelLogo(
                                    size = 22.dp,
                                    isPlaying = selectedTabIndex == 1,
                                    tint = channelsColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Channels",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = channelsColor,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
                        )

                        // Library Button (SimpMusic Style)
                        val libraryColor by animateColorAsState(
                            targetValue = if (selectedTabIndex == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(stiffness = 520f),
                            label = "libraryTabColor",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedTabIndex = 2 }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                LottieLibraryLogo(
                                    size = 22.dp,
                                    isPlaying = selectedTabIndex == 2,
                                    tint = libraryColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Library",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Medium,
                                    color = libraryColor,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // Right circle: Reels in Liquid Glass (matching MediaPickerScreen)
                if (hasVideos2) {
                    Box(
                        modifier = Modifier
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
                                        interaction = reelsFabInteraction,
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
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (selectedTabIndex == 0) {
                                        onReelsClick(currentVideos2, 0, "Telegram Cloud")
                                    } else if (selectedTabIndex == 1) {
                                        onReelsClick(channelVideos, 0, selectedChannel?.channel_title ?: "Channel Videos")
                                    } else {
                                        val liked = viewModel.likedVideos.value
                                        onReelsClick(if (liked.isNotEmpty()) liked else currentVideos2, 0, "Liked Reels")
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = NextIcons.Reel),
                            contentDescription = "Reels",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }
        }

                    }
                },
            ) { paddingValues ->
                val scaffoldBottomInset = paddingValues.calculateBottomPadding() + if (isMiniPlayerVisible) 80.dp else 0.dp
                val dynamicListBottomPadding = listBottomPadding + scaffoldBottomInset
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop)
                ) {
                    if (selectedTabIndex == 0) {

                        val isRefreshing = (uiState as? TelegramUiState.Success)?.isRefreshing == true
                        val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
                        
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = onRefresh,
                            modifier = Modifier.fillMaxSize(),
                            state = pullToRefreshState,
                            indicator = {
                                PullToRefreshDefaults.IndicatorBox(
                                    state = pullToRefreshState,
                                    isRefreshing = isRefreshing,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .size(56.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                ) {
                                    LoadingIndicator(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(28.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        ) {
                            when (uiState) {
                                is TelegramUiState.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = paddingValues.calculateTopPadding()),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            LoadingIndicator(
                                                modifier = Modifier.size(56.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                            Text(
                                                text = "Loading...",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                is TelegramUiState.Error -> {
                                    Text(
                                        text = "Error: ${uiState.message}",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                                is TelegramUiState.Success -> {
                                    val distinctVideos = remember(uiState.videos) { uiState.videos.distinctBy { it.id } }
                                    val filteredVideos = remember(distinctVideos, showDownloadedOnly, downloadedKeys) {
                                        if (showDownloadedOnly) {
                                            distinctVideos.filter { video ->
                                                viewModel.isVideoDownloaded(video, downloadedKeys)
                                            }
                                        } else {
                                            distinctVideos
                                        }
                                    }
                                    val totalFiles = distinctVideos.size
                                    val totalSizeBytes = distinctVideos.sumOf { it.size }
                                    val totalSizeFormatted = formatTelegramFileSize(totalSizeBytes)
                                    val downloadedCount = remember(distinctVideos, downloadedKeys) {
                                        distinctVideos.count { viewModel.isVideoDownloaded(it, downloadedKeys) }
                                    }

                                    LazyColumn(
                                        state = cloudLazyListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = paddingValues.calculateTopPadding()),
                                        contentPadding = PaddingValues(bottom = dynamicListBottomPadding)
                                    ) {
                                        item {
                                            TelegramCloudHeroBanner(
                                                title = "Telegram Cloud",
                                                subtitle = "$totalFiles items • $totalSizeFormatted storage",
                                                onPlayAll = {
                                                    if (filteredVideos.isNotEmpty()) {
                                                        val first = filteredVideos.first()
                                                        onPlayVideo(viewModel.getPlayableUri(first), first.nameWithExtension)
                                                    }
                                                },
                                                onReelsMode = {
                                                    if (filteredVideos.isNotEmpty()) {
                                                        onReelsClick(filteredVideos, 0, "Cloud Reels")
                                                    }
                                                },
                                                onSyncPlay = onJoinWatchPartyClick,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                            )
                                        }

                                        // ArchiveTune Sleek Filter Chips Row
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                FilterChip(
                                                    selected = !showDownloadedOnly,
                                                    onClick = { if (showDownloadedOnly) onShowDownloadedOnlyChange(false) },
                                                    label = { Text("All ($totalFiles)") },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                                                        labelColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    border = null
                                                )

                                                FilterChip(
                                                    selected = showDownloadedOnly,
                                                    onClick = { onShowDownloadedOnlyChange(!showDownloadedOnly) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Bolt,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = if (showDownloadedOnly) MaterialTheme.colorScheme.onPrimary else Color(0xFF00E5FF)
                                                        )
                                                    },
                                                    label = { Text("Downloaded ($downloadedCount)") },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                                                        labelColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    border = null
                                                )

                                                FilterChip(
                                                    selected = false,
                                                    onClick = { showSortDialog = true },
                                                    leadingIcon = {
                                                        Icon(
                                                            painter = painterResource(id = NextIcons.TelegramNavSort),
                                                            contentDescription = "Sort",
                                                            modifier = Modifier.size(15.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    },
                                                    label = { Text("Sort") },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                                                        labelColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    border = null
                                                )

                                                FilterChip(
                                                    selected = isTurboBoostActive,
                                                    onClick = {
                                                        isTurboBoostActive = !isTurboBoostActive
                                                        if (isTurboBoostActive) {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Turbo Boost Enabled: High-speed connection active!",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    },
                                                    leadingIcon = {
                                                        LottieTelegramBoost(
                                                            size = 16.dp,
                                                            isPlaying = isTurboBoostActive,
                                                            tint = if (isTurboBoostActive) MaterialTheme.colorScheme.onPrimary else Color(0xFFFF9800)
                                                        )
                                                    },
                                                    label = {
                                                        Text(
                                                            text = if (isTurboBoostActive) "Turbo 32X" else "Boost",
                                                            fontWeight = if (isTurboBoostActive) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color(0xFFFF9800),
                                                        selectedLabelColor = Color.White,
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                                                        labelColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    border = null
                                                )

                                                FilterChip(
                                                    selected = showStats,
                                                    onClick = onToggleStats,
                                                    leadingIcon = {
                                                        Icon(
                                                            painter = painterResource(id = NextIcons.TelegramNavStats),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(15.dp),
                                                            tint = if (showStats) MaterialTheme.colorScheme.onPrimary else Color(0xFFAB47BC)
                                                        )
                                                    },
                                                    label = { Text("Stats") },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                                                        labelColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    border = null
                                                )
                                            }
                                        }

                                        if (isTurboBoostActive) {
                                            item {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = Color(0xFFFF9800).copy(alpha = 0.12f),
                                                    border = NextColors.cardBorder(Color(0xFFFF9800).copy(alpha = 0.35f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.RocketLaunch,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFF9800),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(
                                                            text = "Turbo Boost Active • High speed connection & instant buffer cache",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontWeight = FontWeight.Medium,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        IconButton(
                                                            onClick = { isTurboBoostActive = false },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Dismiss",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (showStats && totalFiles > 0) {
                                            item {
                                                CloudStatsCard(
                                                    totalFiles = totalFiles,
                                                    totalSizeFormatted = totalSizeFormatted,
                                                    onDismiss = onToggleStats
                                                )
                                            }
                                        }

                                        if (filteredVideos.isEmpty()) {
                                            item {
                                                val isBoostCloud = cloudTypedQuery.trim().equals("/boost", ignoreCase = true) || cloudTypedQuery.contains("boost", ignoreCase = true)
                                                val isLearnCloud = cloudTypedQuery.trim().equals("/learn", ignoreCase = true) || cloudTypedQuery.contains("learn", ignoreCase = true)
                                                ArchiveTuneEmptyPlaceholder(
                                                    icon = if (showDownloadedOnly) Icons.Default.CloudDownload else Icons.Default.CloudQueue,
                                                    title = if (cloudTypedQuery.isNotEmpty()) {
                                                        if (isBoostCloud) "Boost Telegram Cloud"
                                                        else if (isLearnCloud) "Bot Guides & Tutorials (/learn)"
                                                        else "No matching videos"
                                                    } else if (showDownloadedOnly) "No offline videos" else "Telegram Cloud is empty",
                                                    subtitle = if (showDownloadedOnly) "Download videos from your cloud to watch without internet"
                                                    else if (isBoostCloud) "Boost your cloud downloads and streaming speeds with multi-socket acceleration!"
                                                    else if (isLearnCloud) "Learn how to save, link, and stream videos directly from Telegram Cloud."
                                                    else "Videos and documents sent to your saved messages or bot will appear here",
                                                    actionButtonText = if (showDownloadedOnly) "Show All Videos"
                                                    else if (isBoostCloud) "Boost Speed"
                                                    else if (isLearnCloud) "Open Guides (/learn)"
                                                    else null,
                                                    onActionClick = if (showDownloadedOnly) ({ onShowDownloadedOnlyChange(false) })
                                                    else if (isBoostCloud) ({ onToggleStats() })
                                                    else if (isLearnCloud) ({ uriHandler.openUri("https://t.me/kryptonplayerbot?start=learn") })
                                                    else null,
                                                    showBoostIcon = isBoostCloud,
                                                    showLearnIcon = isLearnCloud,
                                                    lottieRes = if (cloudTypedQuery.isNotEmpty()) {
                                                        if (isBoostCloud) UiR.raw.anim_duck_boost
                                                        else if (isLearnCloud) UiR.raw.anim_duck_learn
                                                        else UiR.raw.anim_duck_search
                                                    } else UiR.raw.anim_duck_empty
                                                )
                                            }
                                        } else {
                                            itemsIndexed(filteredVideos, key = { index, video -> "${video.id}_${video.path}_$index" }) { _, video ->
                                                val progress = downloadProgressMap[video.externalId ?: ""]
                                                    ?: downloadProgressMap[video.id.toString()]
                                                    ?: downloadProgressMap[video.nameWithExtension]
                                                val isSelected = video.id in selectedIds
                                                TelegramVideoItem(
                                                    video = video,
                                                    downloadProgress = progress,
                                                    isSelected = isSelected,
                                                    isSelectionMode = isSelectionMode,
                                                    viewModel = viewModel,
                                                    onClick = {
                                                        if (isSelectionMode) {
                                                            if (isSelected) selectedIds.remove(video.id)
                                                            else selectedIds.add(video.id)
                                                            if (selectedIds.isEmpty()) isSelectionMode = false
                                                        } else {
                                                            if (isSearchActive) {
                                                                scrollToVideoId = video.id
                                                                isSearchActive = false
                                                                cloudTypedQuery = ""
                                                                onSearchQueryChange("")
                                                            } else {
                                                                if (video.isImage) {
                                                                    val images = uiState.videos.filter { it.isImage }
                                                                    val index = images.indexOf(video)
                                                                    onImageClick(images, index)
                                                                } else {
                                                                    onPlayVideo(viewModel.getPlayableUri(video), video.nameWithExtension)
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onMoreOptionsClick = { onItemLongClick(video) },
                                                    onDownload = { onDownload(video) },
                                                    onLongClick = {
                                                        if (!isSelectionMode) {
                                                            isSelectionMode = true
                                                            selectedIds.clear()
                                                            selectedIds.add(video.id)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTabIndex == 1) {
                        if (selectedChannel == null) {
                            val distinctChannels = remember(channels) { channels.distinctBy { it.channel_id } }
                            val filteredChannels = remember(distinctChannels, channelSearchFilter) {
                                if (channelSearchFilter.isBlank()) {
                                    distinctChannels
                                } else {
                                    val q = channelSearchFilter.trim().lowercase().removePrefix("@")
                                    distinctChannels.filter {
                                        (it.channel_title?.lowercase()?.contains(q) == true) ||
                                        (it.channel_username?.lowercase()?.removePrefix("@")?.contains(q) == true)
                                    }
                                }
                            }
                            val channelsPullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
                            PullToRefreshBox(
                                isRefreshing = isChannelLoading,
                                onRefresh = onRefresh,
                                state = channelsPullToRefreshState,
                                modifier = Modifier.fillMaxSize(),
                                indicator = {
                                    PullToRefreshDefaults.IndicatorBox(
                                        state = channelsPullToRefreshState,
                                        isRefreshing = isChannelLoading,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = paddingValues.calculateTopPadding())
                                            .size(56.dp),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                    ) {
                                        LoadingIndicator(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(28.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            ) {
                                LazyColumn(
                                    state = channelLazyListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = paddingValues.calculateTopPadding()),
                                    contentPadding = PaddingValues(bottom = dynamicListBottomPadding)
                                ) {
                                    if (distinctChannels.isNotEmpty()) {
                                        item {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                shape = RoundedCornerShape(24.dp),
                                                border = NextColors.cardBorder(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = "Search",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(start = 8.dp).size(20.dp)
                                                    )
                                                    OutlinedTextField(
                                                        value = channelSearchFilter,
                                                        onValueChange = { channelSearchFilter = it },
                                                        placeholder = {
                                                            Text(
                                                                "Search channels or @username...",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                                            )
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(horizontal = 4.dp),
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedContainerColor = Color.Transparent,
                                                            unfocusedContainerColor = Color.Transparent,
                                                            disabledContainerColor = Color.Transparent,
                                                            focusedBorderColor = Color.Transparent,
                                                            unfocusedBorderColor = Color.Transparent
                                                        ),
                                                        textStyle = MaterialTheme.typography.bodyMedium,
                                                        trailingIcon = {
                                                            if (channelSearchFilter.isNotEmpty()) {
                                                                IconButton(onClick = { channelSearchFilter = "" }) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Close,
                                                                        contentDescription = "Clear search",
                                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        if (channelSearchFilter.isEmpty()) {
                                            item {
                                                TelegramChannelsHeroBanner(
                                                    title = "Subscribed Channels",
                                                    subtitle = "${distinctChannels.size} channels & groups connected",
                                                    onAddChannel = { showAddChannelDialog = true },
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (distinctChannels.isEmpty()) {
                                        item {
                                            if (isChannelLoading) {
                                                ChannelLoadingPlaceholder(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 40.dp, bottom = 40.dp),
                                                    lottieRes = UiR.raw.anim_duck_search,
                                                    title = "Loading Channels...",
                                                    subtitle = "Syncing your subscribed Telegram channels"
                                                )
                                            } else {
                                                ChannelLoadingPlaceholder(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 20.dp, bottom = 40.dp),
                                                    lottieRes = UiR.raw.anim_duck_empty,
                                                    title = "No Channels Added",
                                                    subtitle = "Add Telegram public or private channels to stream and download videos directly with zero cloud fees.",
                                                    isLoading = false,
                                                    suggestions = listOf("/boost", "/learn"),
                                                    onSuggestionClick = { suggestion ->
                                                        if (suggestion == "/boost") {
                                                            showAddChannelDialog = true
                                                        } else if (suggestion == "/learn") {
                                                            uriHandler.openUri("https://t.me/kryptonplayerbot?start=learn")
                                                        }
                                                    },
                                                    actionButtonText = "Add Channel",
                                                    onActionClick = { showAddChannelDialog = true }
                                                )
                                            }
                                        }
                                    } else if (filteredChannels.isEmpty()) {
                                        item {
                                            val isBoostFilter = channelSearchFilter.trim().equals("/boost", ignoreCase = true) || channelSearchFilter.contains("boost", ignoreCase = true)
                                            val isLearnFilter = channelSearchFilter.trim().equals("/learn", ignoreCase = true) || channelSearchFilter.contains("learn", ignoreCase = true)
                                            ChannelLoadingPlaceholder(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 20.dp, bottom = 40.dp),
                                                lottieRes = if (isBoostFilter) UiR.raw.anim_duck_boost else if (isLearnFilter) UiR.raw.anim_duck_learn else UiR.raw.anim_duck_search,
                                                title = if (isBoostFilter) "Boost Channel" else if (isLearnFilter) "Bot Guides & Tutorials (/learn)" else "No Matching Channels",
                                                subtitle = if (isBoostFilter) "Open any channel to boost it, or add a new channel below." else if (isLearnFilter) "Learn how to link bots, forward media, and stream cloud content with zero fees." else "No channels match \"$channelSearchFilter\".",
                                                isLoading = false,
                                                suggestions = if (isBoostFilter) listOf("/learn") else if (isLearnFilter) listOf("/boost") else listOf("/boost", "/learn"),
                                                onSuggestionClick = { suggestion ->
                                                    channelSearchFilter = suggestion
                                                },
                                                actionButtonText = if (isBoostFilter) "Add Channel" else if (isLearnFilter) "Open Guides (/learn)" else "Clear Search",
                                                onActionClick = if (isBoostFilter) { { showAddChannelDialog = true } } else if (isLearnFilter) { { uriHandler.openUri("https://t.me/kryptonplayerbot?start=learn") } } else { { channelSearchFilter = "" } },
                                                showBoostIcon = isBoostFilter,
                                                showLearnIcon = isLearnFilter
                                            )
                                        }
                                    } else {
                                        itemsIndexed(filteredChannels, key = { index, channel -> "${channel.channel_id}_$index" }) { _, channel ->
                                            ArchiveTuneChannelCard(
                                                channel = channel,
                                                onClick = {
                                                    channelSearchFilter = ""
                                                    onSelectChannel(channel)
                                                },
                                                onRemoveClick = { channelToRemoveConfirm = channel },
                                                onBoostClick = { boostChannelTarget = channel }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Channel selected: either LIST search results or normal CHAT view
                            val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
                            
                            PullToRefreshBox(
                                isRefreshing = isChannelLoading,
                                onRefresh = onRefresh,
                                state = pullToRefreshState,
                                modifier = Modifier.fillMaxSize(),
                                indicator = {
                                    PullToRefreshDefaults.IndicatorBox(
                                        state = pullToRefreshState,
                                        isRefreshing = isChannelLoading,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .size(56.dp),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                    ) {
                                        LoadingIndicator(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(28.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = paddingValues.calculateTopPadding())
                                ) {
                                    // ArchiveTune Top Channel Switcher Shelf
                                    val distinctChannels = remember(channels) { channels.distinctBy { it.channel_id } }
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // "All Channels" button to easily return to the channels hub
                                        item {
                                            Surface(
                                                onClick = { onSelectChannel(null) },
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                                                border = NextColors.cardBorder()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = NextIcons.ArrowBack,
                                                        contentDescription = "All Channels",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "All Channels",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        // Channel quick-switch chips
                                        itemsIndexed(distinctChannels, key = { index, ch -> "${ch.channel_id}_$index" }) { _, ch ->
                                            val isCurrent = ch.channel_id == selectedChannel?.channel_id
                                            Surface(
                                                onClick = { if (!isCurrent) onSelectChannel(ch) },
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                                       else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                                                border = if (isCurrent) NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                                       else NextColors.cardBorder()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        val name = ch.channel_title?.takeIf { it.isNotBlank() } ?: ch.channel_username?.takeIf { it.isNotBlank() } ?: "C"
                                                        Text(
                                                            text = name.trim().take(1).uppercase().ifEmpty { "C" },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Text(
                                                        text = ch.channel_title ?: ch.channel_username ?: "Channel",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        // Boost Channel Button in Shelf
                                        item {
                                            Surface(
                                                onClick = { boostChannelTarget = selectedChannel },
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color(0xFFFF9800).copy(alpha = 0.15f),
                                                border = NextColors.cardBorder(Color(0xFFFF9800).copy(alpha = 0.35f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.RocketLaunch,
                                                        contentDescription = "Boost Channel",
                                                        tint = Color(0xFFFF9800),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "Boost",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFF9800)
                                                    )
                                                }
                                            }
                                        }

                                        // Add Channel Button
                                        item {
                                            Surface(
                                                onClick = { showAddChannelDialog = true },
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                border = NextColors.cardBorder()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add Channel",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "Add",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (searchUiMode == SearchUiMode.LIST) {
                                        // Custom search list view
                                        if (matchingPosts.isEmpty()) {
                                            val isBoostQuery = debouncedTypedQuery.trim().equals("/boost", ignoreCase = true) || debouncedTypedQuery.contains("boost", ignoreCase = true)
                                            val isLearnQuery = debouncedTypedQuery.trim().equals("/learn", ignoreCase = true) || debouncedTypedQuery.contains("learn", ignoreCase = true)
                                            val channelDisplayName = selectedChannel.channel_title?.takeIf { it.isNotBlank() } ?: selectedChannel.channel_username ?: "Channel"
                                            ChannelLoadingPlaceholder(
                                                lottieRes = if (debouncedTypedQuery.isEmpty()) {
                                                    UiR.raw.anim_duck_empty
                                                } else if (isBoostQuery) {
                                                    UiR.raw.anim_duck_boost
                                                } else if (isLearnQuery) {
                                                    UiR.raw.anim_duck_learn
                                                } else {
                                                    UiR.raw.anim_duck_search
                                                },
                                                title = if (debouncedTypedQuery.isEmpty()) {
                                                    "Search Channel Messages"
                                                } else if (isBoostQuery) {
                                                    "Boost $channelDisplayName"
                                                } else if (isLearnQuery) {
                                                    "Bot Guides & Tutorials (/learn)"
                                                } else {
                                                    "No Matching Posts"
                                                },
                                                subtitle = if (debouncedTypedQuery.isEmpty()) {
                                                    "Type keywords or tap a suggestion below to search videos, files, and captions in $channelDisplayName."
                                                } else if (isBoostQuery) {
                                                    "Boost $channelDisplayName to unlock higher speeds, exclusive perks, and continuous streaming updates!"
                                                } else if (isLearnQuery) {
                                                    "Learn how to link bots, forward media, and stream cloud content in $channelDisplayName."
                                                } else {
                                                    "No posts found matching \"$debouncedTypedQuery\". Try another keyword or boost the channel!"
                                                },
                                                isLoading = false,
                                                suggestions = if (debouncedTypedQuery.isEmpty()) {
                                                    listOf("/boost", "/learn", "#video", "#movie", "1080p", "4K")
                                                } else if (isBoostQuery) {
                                                    listOf("/learn", "#video", "#movie", "1080p")
                                                } else if (isLearnQuery) {
                                                    listOf("/boost", "#video", "#movie")
                                                } else {
                                                    listOf("/boost", "/learn", "#video", "#movie")
                                                },
                                                onSuggestionClick = { suggestion ->
                                                    if (suggestion == "/boost") {
                                                        boostChannelTarget = selectedChannel
                                                    } else if (suggestion == "/learn") {
                                                        uriHandler.openUri("https://t.me/kryptonplayerbot?start=learn")
                                                    } else {
                                                        typedQuery = suggestion
                                                        viewModel.setSearchQuery(suggestion)
                                                    }
                                                },
                                                actionButtonText = if (isBoostQuery) "Boost Now" else if (isLearnQuery) "Open Guides (/learn)" else "Boost Channel",
                                                onActionClick = if (isLearnQuery) { { uriHandler.openUri("https://t.me/kryptonplayerbot?start=learn") } } else { { boostChannelTarget = selectedChannel } },
                                                showBoostIcon = isBoostQuery,
                                                showLearnIcon = isLearnQuery,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                            )
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                contentPadding = PaddingValues(bottom = 16.dp, start = 12.dp, end = 12.dp, top = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                itemsIndexed(matchingPosts, key = { index, post -> "${post.postId}_$index" }) { _, post ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                currentMatchIndex = matchingPosts.indexOf(post)
                                                                scrollToPostId = post.postId
                                                                searchUiMode = SearchUiMode.CHAT
                                                            },
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                                        ),
                                                        border = NextColors.cardBorder()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(42.dp)
                                                                        .clip(RoundedCornerShape(12.dp))
                                                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                                        .border(NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)), RoundedCornerShape(12.dp)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    val chPhoto = selectedChannel?.channel_photo
                                                                    if (!chPhoto.isNullOrEmpty()) {
                                                                        val photoBytes = remember(chPhoto) {
                                                                            try {
                                                                                Base64.decode(chPhoto, Base64.DEFAULT)
                                                                            } catch (e: Exception) { null }
                                                                        }
                                                                        if (photoBytes != null) {
                                                                            val bitmap = remember(photoBytes) {
                                                                                BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                                                                            }
                                                                            if (bitmap != null) {
                                                                                Image(
                                                                                    bitmap = bitmap.asImageBitmap(),
                                                                                    contentDescription = "Channel Avatar",
                                                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                                                                    contentScale = ContentScale.Crop
                                                                                )
                                                                            }
                                                                        }
                                                                    } else {
                                                                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                                    }
                                                                }
                                                            
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(
                                                                        text = selectedChannel.channel_title ?: "Channel Message",
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.onSurface,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Text(
                                                                        text = formatPostDate(post.date),
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                val captionText = post.caption ?: post.fileName ?: "Message #${post.messageId}"
                                                                val snippet = captionText.take(100) + if (captionText.length > 100) "..." else ""
                                                                Text(
                                                                    text = highlightSearchQuery(text = snippet, query = debouncedTypedQuery),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    maxLines = 2,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Glassmorphic results bottom bar
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    NextColors.cardBorder(),
                                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                                )
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = "${matchingPosts.size} results",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Button(
                                                    onClick = {
                                                        if (matchingPosts.isNotEmpty()) {
                                                            searchUiMode = SearchUiMode.CHAT
                                                        }
                                                    },
                                                    enabled = matchingPosts.isNotEmpty(),
                                                    shape = RoundedCornerShape(20.dp)
                                                ) {
                                                    Text("Show as Chat", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        // Chat view (SearchUiMode.CHAT or NONE)
                                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                            key(selectedChannel.channel_id) {
                                                ChannelFeedContent(
                                                    viewModel = viewModel,
                                                    channel = selectedChannel,
                                                    onPlayVideo = onPlayVideo,
                                                    onImageClick = onImageClick,
                                                    onSearchClick = { searchUiMode = SearchUiMode.LIST },
                                                    onBackClick = { onSelectChannel(null) },
                                                    scrollToPostId = scrollToPostId,
                                                    onScrollCompleted = { scrollToPostId = null },
                                                    scrollToTopTrigger = scrollToTopTrigger,
                                                    onScrollToTopCompleted = { scrollToTopTrigger = false },
                                                    searchQuery = debouncedTypedQuery,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            
                                            // Floating Up/Down navigation chevrons for matches in CHAT mode
                                            if (searchUiMode == SearchUiMode.CHAT && matchingPosts.isNotEmpty()) {
                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(end = 16.dp, bottom = 16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    FloatingActionButton(
                                                        onClick = {
                                                            if (currentMatchIndex > 0) {
                                                                currentMatchIndex--
                                                            } else {
                                                                currentMatchIndex = matchingPosts.size - 1
                                                            }
                                                        },
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                                        contentColor = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(48.dp).border(NextColors.cardBorder(), CircleShape)
                                                    ) {
                                                        Icon(imageVector = NextIcons.ArrowUpward, contentDescription = "Previous Match")
                                                    }
                                                    FloatingActionButton(
                                                        onClick = {
                                                            if (currentMatchIndex < matchingPosts.size - 1) {
                                                                currentMatchIndex++
                                                            } else {
                                                                currentMatchIndex = 0
                                                            }
                                                        },
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                                        contentColor = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(48.dp).border(NextColors.cardBorder(), CircleShape)
                                                    ) {
                                                        Icon(imageVector = NextIcons.ArrowDownward, contentDescription = "Next Match")
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Bottom match count bar in CHAT search mode
                                        if (searchUiMode == SearchUiMode.CHAT && matchingPosts.isNotEmpty()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        NextColors.cardBorder(),
                                                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                                    )
                                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Search,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Text(
                                                            text = "${currentMatchIndex + 1} of ${matchingPosts.size}",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { searchUiMode = SearchUiMode.LIST },
                                                        shape = RoundedCornerShape(20.dp)
                                                    ) {
                                                        Text("Show as List", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTabIndex == 2) {
                        TelegramLibraryContent(
                            viewModel = viewModel,
                            onPlayVideo = { uri, title, _, _ -> onPlayVideo(uri, title) },
                            onReelsClick = { videos, idx, title ->
                                onReelsClick(videos, idx, title)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = paddingValues.calculateTopPadding())
                        )
                    }
                }
            }
        }

    if (showDownloadsSheet) {
        TelegramDownloadsSheet(
            viewModel = viewModel,
            backdrop = backdrop,
            onDismiss = { showDownloadsSheet = false },
            onPlayVideo = { uri, title -> onPlayVideo(uri, title) }
        )
    }

    if (showCloudSettingsSheet) {
        TelegramCloudSettingsSheet(
            viewModel = viewModel,
            backdrop = backdrop,
            onOpenStorageSettings = onOpenStorageSettings,
            onOpenTdlibDiagnostics = onOpenTdlibDiagnostics,
            onDismiss = { showCloudSettingsSheet = false }
        )
    }

    if (showSortDialog) {
        TelegramSortFilterBottomSheet(
            currentSort = sortOrder,
            currentFilterOnlyDownloaded = showDownloadedOnly,
            onSortSelected = {
                onSortOrderChange(it)
                showSortDialog = false
            },
            onFilterChanged = onShowDownloadedOnlyChange,
            onDismiss = { showSortDialog = false }
        )
    }

    if (showAddChannelDialog) {
        var newChannelUsername by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showAddChannelDialog = false },
            title = { Text("Add Channel / Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter the public channel username (e.g. @MoviesHD), link, or channel ID (-100...).\n\nNote: For private channels (+invite link), add your Telegram Bot as Admin to the channel first.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = newChannelUsername,
                        onValueChange = {
                            newChannelUsername = it
                            inputError = null
                        },
                        label = { Text("Username, Link, or Channel ID") },
                        singleLine = true,
                        isError = inputError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (inputError != null) {
                        Text(
                            text = inputError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val raw = newChannelUsername.trim()
                    if (raw.isNotBlank()) {
                        val formatted = when {
                            raw.startsWith("-100") || raw.toLongOrNull() != null -> raw
                            raw.startsWith("https://t.me/+") || raw.startsWith("t.me/+") || raw.startsWith("+") -> {
                                // Private invite link
                                raw
                            }
                            raw.startsWith("https://t.me/joinchat/") || raw.startsWith("t.me/joinchat/") -> {
                                raw
                            }
                            raw.startsWith("@") -> raw
                            raw.startsWith("https://t.me/") -> "@" + raw.substringAfter("https://t.me/").removePrefix("@")
                            raw.startsWith("t.me/") -> "@" + raw.substringAfter("t.me/").removePrefix("@")
                            else -> "@$raw"
                        }
                        onAddChannel(formatted)
                        showAddChannelDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChannelDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAccountSwitcherSheet) {
        AccountSwitcherBottomSheet(
            activeToken = playerPreferences.telegramToken,
            activeUserId = playerPreferences.telegramUserId,
            accounts = savedAccounts,
            onDismiss = { showAccountSwitcherSheet = false },
            onSwitchAccount = { account ->
                viewModel.switchAccount(account)
            },
            onAddAccount = { token, onResult ->
                viewModel.addAccount(token, onResult)
            },
            onRemoveAccount = { account ->
                viewModel.removeAccount(account)
            },
            onLogoutActive = {
                onDisconnect()
            }
        )
    }

    if (showSwitchChannelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSwitchChannelSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .border(
                    NextColors.cardBorder(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Switch Channel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val distinctChannels = remember(channels) { channels.distinctBy { it.channel_id } }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(distinctChannels, key = { index, channel -> "${channel.channel_id}_$index" }) { _, channel ->
                        val isSelected = selectedChannel?.channel_id == channel.channel_id
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onSelectChannel(channel)
                                    showSwitchChannelSheet = false
                                }
                                .border(
                                    NextColors.cardBorder(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 52dp Nagram Squircle Avatar with Gradients
                            val gradientColors = remember(channel.channel_id) {
                                val gradients = listOf(
                                    listOf(Color(0xFFE53935), Color(0xFFFF7043)),
                                    listOf(Color(0xFF8E24AA), Color(0xFFBA68C8)),
                                    listOf(Color(0xFF1E88E5), Color(0xFF42A5F5)),
                                    listOf(Color(0xFF00ACC1), Color(0xFF26C6DA)),
                                    listOf(Color(0xFF43A047), Color(0xFF66BB6A)),
                                    listOf(Color(0xFFFB8C00), Color(0xFFFFB74D)),
                                    listOf(Color(0xFF5E35B1), Color(0xFF7E57C2))
                                )
                                val idx = kotlin.math.abs((channel.channel_id % gradients.size).toInt())
                                gradients[idx]
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(gradientColors)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!channel.channel_photo.isNullOrEmpty()) {
                                    val photoBytes = remember(channel.channel_photo) {
                                        try {
                                            Base64.decode(channel.channel_photo, Base64.DEFAULT)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (photoBytes != null) {
                                        val bitmap = remember(photoBytes) {
                                            BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Channel Avatar",
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                } else {
                                    val fallbackTitle = channel.channel_title ?: channel.channel_username ?: "Channel"
                                    Text(
                                        text = fallbackTitle.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.channel_title ?: channel.channel_username ?: "Channel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = channel.channel_username?.let { if (it.startsWith("@")) it else "@$it" } ?: "Telegram Channel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            
                            FilledTonalIconButton(
                                onClick = {
                                    channelToRemoveConfirm = channel
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                LottieTelegramDelete(
                                    size = 20.dp,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    if (channelToRemoveConfirm != null) {
        val target = channelToRemoveConfirm!!
        AlertDialog(
            onDismissRequest = { channelToRemoveConfirm = null },
            title = { Text("Remove Channel") },
            text = { Text("Are you sure you want to remove '${target.channel_title ?: target.channel_username ?: "this channel"}'? All cached videos will be deleted from your database.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveChannel(target.channel_id)
                        if (selectedChannel?.channel_id == target.channel_id) {
                            onSelectChannel(null)
                        }
                        channelToRemoveConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToRemoveConfirm = null }) { Text("Cancel") }
            }
        )
    }

    if (showWatchLaterSheet) {
        WatchLaterBottomSheet(
            viewModel = viewModel,
            onDismiss = { showWatchLaterSheet = false }
        )
    }

    boostChannelTarget?.let { channel ->
        TelegramBoostBottomSheet(
            channel = channel,
            onDismiss = { boostChannelTarget = null }
        )
    }
    }
}

@Composable
fun ArchiveTuneNavigationTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    subtitle: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        thumbnail?.invoke()

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            label?.let {
                Text(
                    text = it.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }

        trailingContent?.invoke()
    }
}

@Composable
fun ArchiveTuneEmptyPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    @androidx.annotation.RawRes lottieRes: Int? = null,
    @androidx.annotation.DrawableRes stickerRes: Int? = null,
    showBoostIcon: Boolean = false,
    showLearnIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (lottieRes != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(118.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f))
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                )
                            ),
                            RoundedCornerShape(26.dp)
                        )
                ) {
                    LottieStickerImage(
                        resId = lottieRes,
                        size = 100.dp,
                        modifier = Modifier.clip(RoundedCornerShape(22.dp))
                    )
                }
            } else if (stickerRes != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(118.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f))
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                )
                            ),
                            RoundedCornerShape(26.dp)
                        )
                ) {
                    AnimatedStickerImage(
                        resId = stickerRes,
                        size = 100.dp,
                        modifier = Modifier.clip(RoundedCornerShape(22.dp))
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            if (!actionButtonText.isNullOrEmpty() && onActionClick != null) {
                Spacer(Modifier.height(20.dp))
                val isBoostBtn = showBoostIcon || actionButtonText.contains("boost", ignoreCase = true)
                val isLearnBtn = showLearnIcon || actionButtonText.contains("learn", ignoreCase = true) || actionButtonText.contains("guide", ignoreCase = true)
                FilledTonalButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isBoostBtn) {
                            Color(0xFFFF9800).copy(alpha = 0.18f)
                        } else if (isLearnBtn) {
                            Color(0xFF2196F3).copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (isBoostBtn) {
                            Color(0xFFFF9800)
                        } else if (isLearnBtn) {
                            Color(0xFF2196F3)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                ) {
                    if (isBoostBtn) {
                        LottieTelegramBoost(size = 20.dp)
                    } else if (isLearnBtn) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = actionButtonText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ArchiveTuneChannelCard(
    channel: dev.anilbeesetti.nextplayer.core.data.network.models.ChannelResponse,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onBoostClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = NextColors.cardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Telegram Chat-Style Circular Avatar
            val fallbackGradients = remember {
                listOf(
                    listOf(Color(0xFF2AABEE), Color(0xFF229ED9)), // Telegram Blue
                    listOf(Color(0xFF7A68F8), Color(0xFF5D48E0)), // Violet
                    listOf(Color(0xFF00B4D8), Color(0xFF0077B6)), // Cyan
                    listOf(Color(0xFF4AC27E), Color(0xFF38A169)), // Emerald
                    listOf(Color(0xFFE5A642), Color(0xFFD68A1E)), // Amber
                    listOf(Color(0xFFEE5D8E), Color(0xFFD81B60)), // Pink
                    listOf(Color(0xFFE56555), Color(0xFFD34A40))  // Coral Red
                )
            }
            val gradientColors = remember(channel.channel_id) {
                val idx = kotlin.math.abs(channel.channel_id.hashCode()) % fallbackGradients.size
                fallbackGradients[idx]
            }

            val photoBytes = remember(channel.channel_photo) {
                if (!channel.channel_photo.isNullOrEmpty()) {
                    try {
                        Base64.decode(channel.channel_photo, Base64.DEFAULT)
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }
            val bitmap = remember(photoBytes) {
                if (photoBytes != null) {
                    try {
                        BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = gradientColors))
                    .border(
                        NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Channel Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val fallbackTitle = channel.channel_title?.takeIf { it.isNotBlank() }
                        ?: channel.channel_username?.takeIf { it.isNotBlank() }
                        ?: "C"
                    Text(
                        text = fallbackTitle.trim().removePrefix("@").take(1).uppercase().ifEmpty { "C" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Telegram Chat Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = channel.channel_title ?: channel.channel_username ?: "Channel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    LottieVerifiedBadge(size = 18.dp)
                }

                val subtitleText = remember(channel) {
                    buildString {
                        val username = channel.channel_username?.trim()
                        if (!username.isNullOrEmpty()) {
                            append(if (username.startsWith("@")) username else "@$username")
                        }
                        val count = channel.member_count
                        val subs = if (count >= 1_000_000) {
                            String.format("%.1fM", count / 1_000_000f).replace(".0", "")
                        } else if (count >= 1000) {
                            String.format("%.1fK", count / 1000f).replace(".0", "")
                        } else if (count > 0) {
                            count.toString()
                        } else null
                        if (subs != null) {
                            if (isNotEmpty()) append(" • ")
                            append("$subs subs")
                        }
                        if (channel.video_count > 0) {
                            if (isNotEmpty()) append(" • ")
                            append("${channel.video_count} videos")
                        }
                        if (isEmpty()) {
                            append("Telegram Channel")
                        }
                    }
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
            }

            // Trailing Section (Telegram Chat style)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (channel.video_count > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "${channel.video_count} vids",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Channel Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            if (onBoostClick != null) {
                                DropdownMenuItem(
                                    text = { Text("Boost Channel (/boost)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        LottieTelegramBoost(
                                            size = 20.dp,
                                            tint = Color(0xFFFF9800)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onBoostClick()
                                    }
                                )
                            }
                            val shelfUriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            DropdownMenuItem(
                                text = { Text("Guides & Bot (/learn)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.School,
                                        contentDescription = null,
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    shelfUriHandler.openUri("https://t.me/kryptonplayerbot?start=learn")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Channel", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    LottieTelegramDelete(
                                        size = 20.dp,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRemoveClick()
                                }
                            )
                        }
                    }

                    Icon(
                        imageVector = NextIcons.ArrowForward,
                        contentDescription = "Open Channel",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TelegramVideoItem(
    video: Video,
    downloadProgress: Float?,
    isLoading: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    viewModel: TelegramViewModel? = null,
    onClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    onDownload: () -> Unit,
    onLongClick: () -> Unit
) {
    val selectionBorder = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        NextColors.cardBorder()
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = selectionBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 116.dp, height = 76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(NextColors.cardBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
            ) {
                val streamUri = try { Uri.parse(video.uriString) } catch (e: Exception) { null }
                val chatId = streamUri?.getQueryParameter("chatId")?.toLongOrNull()
                val messageId = streamUri?.getQueryParameter("messageId")?.toLongOrNull()
                val fileIdFromUri = streamUri?.getQueryParameter("fileId")
                val thumbFileIdFromUri = streamUri?.getQueryParameter("thumbFileId")
                    ?: (video.artworkUri?.substringAfterLast("/api/thumbnail/")?.takeIf { it.isNotEmpty() && !it.startsWith("http") })
                    ?: if (video.isImage) fileIdFromUri else null

                TelegramAsyncImage(
                    thumbFileId = thumbFileIdFromUri,
                    chatId = chatId,
                    messageId = messageId,
                    fallbackUrl = video.artworkUri,
                    contentDescription = "Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    tdlibMediaResolver = viewModel?.tdlibMediaResolver,
                    imageDiskCache = viewModel?.imageDiskCache
                )
                
                if (isLoading) {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center).size(36.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // Duration Badge overlay on thumbnail
                if (video.formattedDuration.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Selection checkmark overlay
                Box(modifier = Modifier.align(Alignment.TopStart)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSelectionMode,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                                )
                                .border(
                                    1.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.nameWithExtension,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val ext = remember(video.nameWithExtension) {
                        video.nameWithExtension.substringAfterLast('.', "").uppercase()
                    }
                    if (ext.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        ) {
                            Text(
                                text = ext,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = video.formattedFileSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Actions - hidden in selection mode (batch actions are in top bar)
            if (!isSelectionMode) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                val context = LocalContext.current
                val downloadedKeys by viewModel?.downloadedKeys?.collectAsState() ?: remember { mutableStateOf(emptySet()) }
                val isDownloaded = remember(video, downloadProgress, downloadedKeys) {
                    if (viewModel != null) {
                        viewModel.isVideoDownloaded(video, downloadedKeys)
                    } else {
                        downloadedKeys.contains(video.nameWithExtension) ||
                        (video.externalId != null && downloadedKeys.contains(video.externalId)) ||
                        downloadedKeys.contains(video.id.toString())
                    }
                }

                if (isDownloaded && downloadProgress == null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = NextIcons.TelegramCloudDone),
                                contentDescription = "Saved",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else if (downloadProgress != null) {
                    val isSavingToGallery = viewModel?.isSavingToGallery(video) == true
                    val isPaused = viewModel?.isDownloadPaused(video) == true
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                width = 1.dp,
                                color = if (isSavingToGallery) Color(0xFF4CAF50).copy(alpha = 0.36f)
                                else if (isPaused) Color(0xFFFF9800).copy(alpha = 0.32f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                shape = CircleShape
                            )
                    ) {
                        if (isSavingToGallery) {
                            CircularProgressIndicator(
                                progress = { downloadProgress.coerceIn(0.01f, 1f) },
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF4CAF50),
                                trackColor = MaterialTheme.colorScheme.secondaryContainer,
                            )
                        } else if (isPaused) {
                            CircularProgressIndicator(
                                progress = { if (downloadProgress > 0f) downloadProgress else 0.5f },
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFFF9800),
                                trackColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        } else if (downloadProgress < 0f) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.secondaryContainer,
                            )
                        } else {
                            CircularWavyProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(26.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.secondaryContainer,
                            )
                        }
                        IconButton(
                            enabled = !isSavingToGallery,
                            onClick = {
                                if (isPaused) {
                                    viewModel?.resumeDownload(video)
                                } else {
                                    viewModel?.pauseDownload(video)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isSavingToGallery -> Icons.Filled.PhotoLibrary
                                    isPaused -> Icons.Filled.PlayArrow
                                    else -> Icons.Filled.Pause
                                },
                                contentDescription = when {
                                    isSavingToGallery -> "Saving to Gallery"
                                    isPaused -> "Resume Download"
                                    else -> "Pause Download"
                                },
                                tint = when {
                                    isSavingToGallery -> Color(0xFF4CAF50)
                                    isPaused -> Color(0xFFFF9800)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF2AABEE).copy(alpha = 0.18f),
                                        Color(0xFF229ED9).copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF2AABEE).copy(alpha = 0.45f),
                                        Color(0xFF229ED9).copy(alpha = 0.20f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Download to Watch",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF2AABEE)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                FilledTonalIconButton(
                    onClick = onMoreOptionsClick,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More Options",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            } // end if (!isSelectionMode)
        }
    }
}



@Composable
private fun CloudStatsCard(
    totalFiles: Int,
    totalSizeFormatted: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = NextColors.cardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .border(
                            NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = NextIcons.TelegramCloud),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Cloud Storage Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalFiles files • $totalSizeFormatted total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledTonalIconButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = NextIcons.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelegramSortFilterBottomSheet(
    currentSort: TelegramSortOrder,
    currentFilterOnlyDownloaded: Boolean,
    onSortSelected: (TelegramSortOrder) -> Unit,
    onFilterChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sort & Filter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(NextColors.cardBorder(), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Downloaded Only",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Show only files saved locally",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = currentFilterOnlyDownloaded,
                    onCheckedChange = onFilterChanged
                )
            }
            
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            val sortOptions = listOf(
                TelegramSortOrder.DATE_ADDED_DESC to "Date added (Newest first)",
                TelegramSortOrder.DATE_ADDED_ASC to "Date added (Oldest first)",
                TelegramSortOrder.NAME_ASC to "Name (A to Z)",
                TelegramSortOrder.NAME_DESC to "Name (Z to A)",
                TelegramSortOrder.SIZE_DESC to "Size (Largest first)",
                TelegramSortOrder.SIZE_ASC to "Size (Smallest first)",
                TelegramSortOrder.DURATION_DESC to "Duration (Longest first)",
                TelegramSortOrder.DURATION_ASC to "Duration (Shortest first)"
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                sortOptions.forEach { (order, label) ->
                    val isSelected = currentSort == order
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable { onSortSelected(order) }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSortSelected(order) }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

internal fun formatTelegramFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramConnectScreen(
    isLinking: Boolean,
    linkError: String?,
    onLinkClick: (String) -> Unit,
    onNavigateUp: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    PremiumMeshBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NextTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = NextIcons.Telegram),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.Unspecified
                            )
                            Text(
                                text = "Link Telegram Cloud",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(imageVector = NextIcons.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xEE161822),
                                    Color(0xFA0F1017),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.05f),
                                ),
                            ),
                            shape = RoundedCornerShape(32.dp),
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Icon(
                        painter = painterResource(id = NextIcons.TelegramCloud),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Unspecified
                    )
                    
                    Text(
                        text = "Access Telegram Cloud",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StepItem(
                            number = 1,
                            text = "Open Telegram and go to the bot:"
                        ) {
                            Text(
                                text = "t.me/kryptonplayerbot",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        uriHandler.openUri("https://t.me/kryptonplayerbot")
                                    }
                            )
                        }

                        StepItem(
                            number = 2,
                            text = "Send /start, /token or /learn to retrieve your linking code and guide"
                        )

                        StepItem(
                            number = 3,
                            text = "Copy the code and paste it below"
                        )
                    }
                    
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Enter App Linking Code") },
                        placeholder = { Text("KRYPTON-XXXXXX") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        isError = linkError != null,
                        supportingText = {
                            if (linkError != null) {
                                Text(text = linkError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        trailingIcon = {
                            if (code.isNotEmpty()) {
                                IconButton(onClick = { code = "" }) {
                                    Icon(imageVector = NextIcons.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                    
                    Button(
                        onClick = { onLinkClick(code) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLinking && code.isNotBlank(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        if (isLinking) {
                            LoadingIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Link Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    number: Int,
    text: String,
    content: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .border(NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (content != null) {
                Spacer(modifier = Modifier.height(4.dp))
                content()
            }
        }
    }
}


@Composable
fun TelegramCloudHeroBanner(
    title: String,
    subtitle: String,
    onPlayAll: () -> Unit,
    onReelsMode: () -> Unit,
    onSyncPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = NextColors.cardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.22f),
                            tertiaryColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.18f))
                            .border(1.dp, primaryColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cloud,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sync Play / Watch Party action button
                    FilledTonalIconButton(
                        onClick = onSyncPlay,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LiveTv,
                            contentDescription = "Sync Play",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    FilledTonalButton(
                        onClick = onReelsMode,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Reels Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelegramChannelsHeroBanner(
    title: String,
    subtitle: String,
    onAddChannel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = NextColors.cardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.20f),
                            secondaryColor.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.18f))
                            .border(1.dp, primaryColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onAddChannel,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun WatchPartyBanner(
    onJoinClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "popcornPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "popcornPulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LiveTv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale
                        )
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sync Play (Watch Party)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Watch videos synchronously with friends. Long-press any video to host a room, or tap join.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onJoinClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Join Watch Party", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AnimatedAddChannelItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "addButtonScale"
    )
    
    var targetRotation by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "addButtonRotation"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    targetRotation += 90f
                    onClick()
                }
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Channel",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    }
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Add",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
