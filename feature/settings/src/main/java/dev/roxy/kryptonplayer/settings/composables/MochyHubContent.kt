package dev.roxy.kryptonplayer.settings.composables

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.CancelButton
import dev.roxy.kryptonplayer.core.ui.components.DoneButton
import dev.roxy.kryptonplayer.core.ui.components.MochyBottomBlurScrim
import dev.roxy.kryptonplayer.core.ui.components.MochyMotion
import dev.roxy.kryptonplayer.core.ui.components.MochyNavigationDock
import dev.roxy.kryptonplayer.core.ui.components.MochyPromptButton
import dev.roxy.kryptonplayer.core.ui.components.MochyPromptButtonState
import dev.roxy.kryptonplayer.core.ui.components.MochyScrollBlurHeader
import dev.roxy.kryptonplayer.core.ui.components.NextDialog
import dev.roxy.kryptonplayer.core.ui.components.mochyItemPress
import dev.roxy.kryptonplayer.core.ui.components.mochyScrollFadeEdge
import dev.roxy.kryptonplayer.core.ui.components.toPromptButtonState
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.liquidglass.layerBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.rememberBackdrop
import dev.roxy.kryptonplayer.settings.HubUserStatsSummary
import dev.roxy.kryptonplayer.settings.Setting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Full Mochi-style 4th-Tab Krypton Hub Experience:
 * - Atmospheric Hero Stage with user's circular Profile Photo, Username, Krypton Player title, and live Watch Statistics
 * - Signature featured "App Settings" card (opens dedicated All Settings screen)
 * - Live animated Updates card with MochyPromptButton
 * - 2x2 Textured Bento Quick-Access tiles (Appearance, Watch Party, Statistics, Storage)
 * - Lead Developer (ROXY) & Community card with in-app Contributors & Credits sheet (no Boost or duplicate Telegram cards)
 * - Real GPU-blurred scroll header + floating Mochi navigation dock
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MochyHubContent(
    appName: String,
    appVersion: String,
    primaryAbi: String,
    developerName: String,
    userStats: HubUserStatsSummary,
    updateState: UpdateState,
    pendingUpdateInfo: AppUpdateInfo?,
    onOpenAllSettings: () -> Unit,
    onSettingClick: (Setting) -> Unit,
    onUpdateAction: () -> Unit,
    onOpenLocalVideo: () -> Unit,
    onOpenNetworkStream: () -> Unit,
    onOpenTelegram: () -> Unit,
    onOpenGitHub: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onSearchClick: () -> Unit,
    onMoreMenuClick: () -> Unit,
    moreMenuContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val backdrop = rememberBackdrop(colorScheme.background)

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

    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showContributorsSheet by rememberSaveable { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    runCatching {
                        val destFile = File(context.filesDir, "user_profile_avatar_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        context.filesDir.listFiles()?.forEach { file ->
                            if (file.name.startsWith("user_profile_avatar_") && file.absolutePath != destFile.absolutePath) {
                                runCatching { file.delete() }
                            }
                        }
                        destFile.absolutePath
                    }.getOrNull() ?: uri.toString()
                }
                profilePicUri = savedPath
                profilePrefs.edit().putString("profile_pic_uri", savedPath).apply()
            }
        }
    }

    if (showEditProfileDialog) {
        HubEditProfileDialog(
            initialName = currentUsername,
            profilePicUri = profilePicUri,
            onPickPhoto = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onDismiss = { showEditProfileDialog = false },
            onSaveName = { newName ->
                val cleanName = newName.trim().ifBlank { "Roxy" }
                currentUsername = cleanName
                profilePrefs.edit().putString("username", cleanName).apply()
                showEditProfileDialog = false
            },
        )
    }

    if (showContributorsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showContributorsSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0E1016),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            HubContributorsBottomSheetContent(
                developerName = developerName,
                onOpenGitHub = onOpenGitHub,
                onOpenAbout = {
                    showContributorsSheet = false
                    onSettingClick(Setting.ABOUT)
                },
                onClose = { showContributorsSheet = false },
            )
        }
    }

    val scrollFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 110f).coerceIn(0f, 1f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
    ) {
        // Live GPU backdrop source layer for scroll header & floating navigation dock
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
                    .mochyScrollFadeEdge(topFadeHeight = 18.dp, bottomFadeHeight = 56.dp),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 72.dp,
                    bottom = 132.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "hub_status_spacer") {
                    Spacer(modifier = Modifier.statusBarsPadding())
                }

                // 1. Atmospheric Mochi Hero Stage Card with User Profile Photo, Name & Live Stats
                item(key = "hub_hero_stage") {
                    MochyStaggeredReveal(index = 0) {
                        MochyHeroStageCard(
                            appName = appName,
                            versionName = appVersion,
                            abi = primaryAbi,
                            username = currentUsername,
                            profilePicUri = profilePicUri,
                            userStats = userStats,
                            onEditProfile = { showEditProfileDialog = true },
                            onPickPhoto = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onStatsClick = { onSettingClick(Setting.STATS) },
                            onOpenLocalVideo = onOpenLocalVideo,
                            onOpenNetworkStream = onOpenNetworkStream,
                        )
                    }
                }

                // 2. Signature Featured "App Settings" Hero Card (opens All Settings)
                item(key = "hub_all_settings_card") {
                    MochyStaggeredReveal(index = 1) {
                        AppSettingsHeroCard(
                            onClick = onOpenAllSettings,
                        )
                    }
                }

                // 3. Signature Mochi Updates Card with animated MochyPromptButton
                item(key = "hub_updates_card") {
                    MochyStaggeredReveal(index = 2) {
                        MochyUpdatesHubCard(
                            appVersion = appVersion,
                            updateState = updateState,
                            pendingUpdateInfo = pendingUpdateInfo,
                            onUpdateClick = onUpdateAction,
                            onChangelogClick = { onSettingClick(Setting.ABOUT) },
                        )
                    }
                }

                // 4. 2x2 Textured Bento Quick Shortcuts
                item(key = "hub_bento_header") {
                    MochyStaggeredReveal(index = 3) {
                        HubSectionLabel(title = "Quick Access")
                    }
                }

                item(key = "hub_bento_row_1") {
                    MochyStaggeredReveal(index = 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            HubBentoTile(
                                title = "Appearance",
                                subtitle = "AMOLED, dock & style",
                                iconRes = R.drawable.palette,
                                texture = MochyCardTexture.DOT_MATRIX,
                                testTag = "hub_tile_appearance",
                                onClick = { onSettingClick(Setting.APPEARANCE) },
                                modifier = Modifier.weight(1f),
                            )
                            HubBentoTile(
                                title = "Watch Party",
                                subtitle = "Telegram Cloud sync",
                                iconRes = R.drawable.sync,
                                texture = MochyCardTexture.DIAGONAL_GRAIN,
                                testTag = "hub_tile_watch_party",
                                onClick = { onSettingClick(Setting.WATCH_PARTY) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                item(key = "hub_bento_row_2") {
                    MochyStaggeredReveal(index = 5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            HubBentoTile(
                                title = "Statistics",
                                subtitle = "Watch history & stats",
                                iconRes = R.drawable.stats,
                                texture = MochyCardTexture.CONCENTRIC_RADAR,
                                testTag = "hub_tile_stats",
                                onClick = { onSettingClick(Setting.STATS) },
                                modifier = Modifier.weight(1f),
                            )
                            HubBentoTile(
                                title = "Storage",
                                subtitle = "Cache & downloads",
                                iconRes = R.drawable.storage,
                                texture = MochyCardTexture.DOT_MATRIX,
                                testTag = "hub_tile_storage",
                                onClick = { onSettingClick(Setting.STORAGE) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // 5. Lead Developer, Community & Credits Spotlight (deduplicated, no Boost)
                item(key = "hub_creator_header") {
                    MochyStaggeredReveal(index = 6) {
                        HubSectionLabel(title = "Developer, Community & Credits")
                    }
                }

                item(key = "hub_lead_dev_card") {
                    MochyStaggeredReveal(index = 7) {
                        HubLeadDeveloperCard(
                            developerName = developerName,
                            onOpenTelegram = onOpenTelegram,
                            onOpenGitHub = onOpenGitHub,
                            onOpenContributors = { showContributorsSheet = true },
                            onOpenAbout = { onSettingClick(Setting.ABOUT) },
                        )
                    }
                }

                // 6. Minimal Signature Footer
                item(key = "hub_footer") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "${appName.uppercase()} • PURE AMOLED",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        )
                        Text(
                            text = "v$appVersion · $primaryAbi · Crafted by $developerName",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                        )
                    }
                }
            }
        }

        // Floating scroll-reactive frosted glass header
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(25.dp),
                    )
                    Text(
                        text = appName,
                        fontSize = (24f - 4f * easedScroll).sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = (-0.5f + 0.2f * easedScroll).sp,
                        modifier = Modifier.graphicsLayer {
                            translationY = (1f - easedScroll) * 2f
                        },
                    )
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
                        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.20f)),
                    ) {
                        Text(
                            text = "HUB",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Box {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.18f)),
                    ) {
                        IconButton(
                            onClick = onMoreMenuClick,
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
                    moreMenuContent()
                }
            }
        }

        // Progressive bottom depth scrim + live GPU-blurred floating Mochi navigation dock
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

@Composable
private fun MochyHeroStageCard(
    appName: String,
    versionName: String,
    abi: String,
    username: String,
    profilePicUri: String?,
    userStats: HubUserStatsSummary,
    onEditProfile: () -> Unit,
    onPickPhoto: () -> Unit,
    onStatsClick: () -> Unit,
    onOpenLocalVideo: () -> Unit,
    onOpenNetworkStream: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "profile_ring_anim")
    val ringAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "profile_ring_angle",
    )

    MochyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mochy_hub_identity_card"),
        texture = MochyCardTexture.AURORA_MESH,
        cornerRadius = 28.dp,
        enable3DTilt = false,
        shimmerSweep = true,
        elevated = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Large Circular User Profile Avatar with camera badge & rotating specular ring
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clickable(onClick = onPickPhoto)
                    .testTag("hub_profile_avatar"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = ringAngle }
                        .border(
                            width = 1.8.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.88f),
                                    Color.White.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.88f),
                                ),
                            ),
                            shape = CircleShape,
                        ),
                )

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF232632), Color(0xFF12141B)),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!profilePicUri.isNullOrBlank()) {
                        val model = if (profilePicUri.contains("://")) profilePicUri else "file://$profilePicUri"
                        AsyncImage(
                            model = model,
                            contentDescription = username,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                            contentDescription = "Krypton Player Logo",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                // Edit photo badge
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFF0E1016)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AddAPhoto,
                            contentDescription = "Change Profile Photo",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // User Name + Edit Pill & Krypton Player Brand Identity
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onEditProfile)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = Color.White,
                    )
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Profile Name",
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(15.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                    HubMetaPill(text = "v$versionName")
                    HubMetaPill(text = abi)
                }
            }

            // User Watch Statistics Strip directly under Krypton Player
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onStatsClick)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HubHeroStatItem(
                    icon = Icons.Rounded.Schedule,
                    value = formatWatchTimeShort(userStats.totalWatchTimeMs),
                    label = "Watch Time",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.12f)),
                )
                HubHeroStatItem(
                    icon = Icons.Rounded.Movie,
                    value = userStats.watchedVideosCount.toString(),
                    label = "Watched",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.12f)),
                )
                HubHeroStatItem(
                    icon = Icons.Rounded.VideoLibrary,
                    value = userStats.totalVideosInLibrary.toString(),
                    label = "Library",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.12f)),
                )
                HubHeroStatItem(
                    icon = Icons.Rounded.Folder,
                    value = userStats.uniqueFoldersCount.toString(),
                    label = "Folders",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HubActionPill(
                    label = "Local Video",
                    iconVector = NextIcons.FileOpen,
                    onClick = onOpenLocalVideo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hub_quick_local_video"),
                )
                HubActionPill(
                    label = "Network Stream",
                    iconVector = NextIcons.Link,
                    onClick = onOpenNetworkStream,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hub_quick_network_stream"),
                )
            }
        }
    }
}

@Composable
private fun HubHeroStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.68f),
            maxLines = 1,
        )
    }
}

private fun formatWatchTimeShort(ms: Long): String {
    if (ms <= 0L) return "0m"
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60L
    val mins = totalMinutes % 60L
    return when {
        hours > 0L -> "${hours}h ${mins}m"
        totalMinutes > 0L -> "${totalMinutes}m"
        else -> "<1m"
    }
}

@Composable
private fun HubEditProfileDialog(
    initialName: String,
    profilePicUri: String?,
    onPickPhoto: () -> Unit,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    NextDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Cinema Profile") },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B1E28))
                        .border(1.2.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                        .clickable(onClick = onPickPhoto),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!profilePicUri.isNullOrBlank()) {
                        val model = if (profilePicUri.contains("://")) profilePicUri else "file://$profilePicUri"
                        AsyncImage(
                            model = model,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AddAPhoto,
                            contentDescription = "Pick Photo",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Text(
                    text = "Tap photo to update avatar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Display Name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    ),
                )
            }
        },
        confirmButton = {
            DoneButton(
                enabled = name.isNotBlank(),
                onClick = { onSaveName(name) },
            )
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
    )
}

@Composable
private fun HubContributorsBottomSheetContent(
    developerName: String,
    onOpenGitHub: () -> Unit,
    onOpenAbout: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f)),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column {
                Text(
                    text = "Contributors & Credits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "People and open-source craft behind Krypton Player",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f),
                )
            }
        }

        // Lead Architect Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF171922),
            border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.18f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AsyncImage(
                    model = R.drawable.dev_profile,
                    contentDescription = developerName,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = developerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        HubMetaPill(text = "CREATOR & LEAD")
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "@RoxyBasicNeedBot • Core Engine, AMOLED UI, Telegram Cloud & Media3 Architecture",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.76f),
                    )
                }
            }
        }

        // Core Acknowledgements & Ecosystem
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF15171F),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Open-Source Foundations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                ContributorCreditRow(
                    name = "AndroidX Media3 & ExoPlayer",
                    role = "Hardware-accelerated 4K HDR video & audio pipeline",
                )
                ContributorCreditRow(
                    name = "Next Player & FFmpeg Decoder",
                    role = "Multi-format software/hardware decoding & subtitle rendering",
                )
                ContributorCreditRow(
                    name = "TDLib (Telegram Database Library)",
                    role = "Zero-lag Telegram Cloud streaming & Watch Party sync",
                )
                ContributorCreditRow(
                    name = "Jetpack Compose & Liquid Glass",
                    role = "Pure AMOLED obsidian UI & real-time GPU backdrop blur",
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onOpenAbout,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
            ) {
                Text("Open Licenses", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ContributorCreditRow(
    name: String,
    role: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Text(
            text = role,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.68f),
        )
    }
}

/**
 * Signature Mochi Featured "App Settings" Card — primary entry into all 14 settings modules.
 */
@Composable
private fun AppSettingsHeroCard(
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "app_settings_card_anim")
    val iconPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "app_settings_icon_pulse",
    )

    MochyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hub_app_settings_card"),
        texture = MochyCardTexture.CONCENTRIC_RADAR,
        cornerRadius = 24.dp,
        enable3DTilt = true,
        maxTilt = 4.5f,
        shimmerSweep = true,
        elevated = true,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = iconPulse
                        scaleY = iconPulse
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.White, Color(0xFFD6D8E0)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "App Settings",
                    tint = Color.Black,
                    modifier = Modifier.size(25.dp),
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
                        text = "App Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.26f)),
                    ) {
                        Text(
                            text = "ALL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = "Player, gestures, audio, subtitles, lyrics, decoder & system",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f),
                border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.24f)),
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Mochi Signature Updates Card featuring the animated [MochyPromptButton].
 */
@Composable
private fun MochyUpdatesHubCard(
    appVersion: String,
    updateState: UpdateState,
    pendingUpdateInfo: AppUpdateInfo?,
    onUpdateClick: () -> Unit,
    onChangelogClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val buttonState: MochyPromptButtonState = remember(updateState, pendingUpdateInfo) {
        when (updateState) {
            is UpdateState.Idle -> MochyPromptButtonState.UpdateAvailable(
                customText = "Check for Updates",
            )
            else -> updateState.toPromptButtonState(
                fileSize = pendingUpdateInfo?.fileSize?.takeIf { it > 0L }
                    ?: pendingUpdateInfo?.apkAssets?.firstOrNull()?.size
                    ?: 0L,
            )
        }
    }

    MochyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hub_updates_card"),
        texture = MochyCardTexture.DIAGONAL_GRAIN,
        cornerRadius = 24.dp,
        enable3DTilt = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Updates & Releases",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                    )
                    Text(
                        text = when (updateState) {
                            is UpdateState.Available -> "New release v${updateState.info.versionName} ready"
                            is UpdateState.Downloading -> "Downloading update package…"
                            is UpdateState.ReadyToInstall -> "Verified APK ready to install"
                            else -> "Installed build v$appVersion"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }

                val changelogInteraction = remember { MutableInteractionSource() }
                val isChangelogPressed by changelogInteraction.collectIsPressedAsState()
                Surface(
                    onClick = onChangelogClick,
                    interactionSource = changelogInteraction,
                    modifier = Modifier
                        .mochyItemPress(changelogInteraction, pressedScale = 0.95f)
                        .testTag("hub_changelog_chip"),
                    shape = CircleShape,
                    color = if (isChangelogPressed) Color.White.copy(alpha = 0.22f) else colorScheme.surfaceContainerHigh,
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = if (isChangelogPressed) 0.42f else 0.20f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.history),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "What's New",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }

            MochyPromptButton(
                state = buttonState,
                onClick = onUpdateClick,
                height = 50.dp,
                containerColor = Color(0xFF1A1C24),
                progressFillColor = Color.White,
                readyContainerColor = Color.White,
                onReadyContentColor = Color.Black,
                modifier = Modifier.testTag("mochy_hub_update_pill"),
            )
        }
    }
}

@Composable
private fun HubBentoTile(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    texture: MochyCardTexture,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    MochyGlassCard(
        modifier = modifier
            .heightIn(min = 114.dp)
            .testTag(testTag),
        texture = texture,
        cornerRadius = 22.dp,
        enable3DTilt = true,
        maxTilt = 5.5f,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HubLeadDeveloperCard(
    developerName: String,
    onOpenTelegram: () -> Unit,
    onOpenGitHub: () -> Unit,
    onOpenContributors: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "dev_ring_glow")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dev_ring_rotation",
    )

    MochyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hub_lead_dev_card"),
        texture = MochyCardTexture.DOT_MATRIX,
        cornerRadius = 24.dp,
        enable3DTilt = true,
        maxTilt = 3.5f,
        onClick = onOpenAbout,
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
                // Avatar with animated rotating specular ring
                Box(
                    modifier = Modifier.size(62.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = ringRotation }
                            .border(
                                width = 1.6.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.10f),
                                        Color.White.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.10f),
                                        Color.White.copy(alpha = 0.85f),
                                    ),
                                ),
                                shape = CircleShape,
                            ),
                    )
                    AsyncImage(
                        model = R.drawable.dev_profile,
                        contentDescription = "Lead Developer",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            text = developerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                        )
                        HubMetaPill(text = "LEAD DEV")
                    }
                    Text(
                        text = "Creator • Core Architecture, Mochi UI & Cloud",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp),
                )
            }

            HorizontalDivider(
                thickness = 0.6.dp,
                color = Color.White.copy(alpha = 0.12f),
            )

            // Single clean row of Community, Repository & In-App Contributors/Credits pills (no Boost, no duplicate Telegram)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HubDrawablePill(
                    label = "Telegram",
                    iconRes = R.drawable.ic_telegram,
                    onClick = onOpenTelegram,
                    modifier = Modifier.weight(1f),
                )
                HubDrawablePill(
                    label = "GitHub",
                    iconRes = R.drawable.ic_github,
                    onClick = onOpenGitHub,
                    modifier = Modifier.weight(1f),
                )
                HubDrawablePill(
                    label = "Credits",
                    iconRes = R.drawable.info,
                    onClick = onOpenContributors,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hub_credits_button"),
                )
            }
        }
    }
}

@Composable
private fun HubSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.0.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
        modifier = Modifier.padding(start = 6.dp, top = 4.dp),
    )
}

@Composable
private fun HubMetaPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.22f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
        )
    }
}

@Composable
private fun HubActionPill(
    label: String,
    iconVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.11f),
        animationSpec = tween(150),
        label = "action_pill_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) Color.White.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.22f),
        animationSpec = tween(150),
        label = "action_pill_border",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = 44.dp)
            .mochyItemPress(interactionSource, pressedScale = 0.96f),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(0.85.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HubDrawablePill(
    label: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(150),
        label = "drawable_pill_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.20f),
        animationSpec = tween(150),
        label = "drawable_pill_border",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = 42.dp)
            .mochyItemPress(interactionSource, pressedScale = 0.96f),
        shape = RoundedCornerShape(13.dp),
        color = containerColor,
        border = BorderStroke(0.8.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
