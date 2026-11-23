package dev.roxy.kryptonplayer.settings.composables

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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import dev.roxy.kryptonplayer.core.ui.components.MochyBottomBlurScrim
import dev.roxy.kryptonplayer.core.ui.components.MochyMotion
import dev.roxy.kryptonplayer.core.ui.components.MochyNavigationDock
import dev.roxy.kryptonplayer.core.ui.components.MochyPromptButton
import dev.roxy.kryptonplayer.core.ui.components.MochyPromptButtonState
import dev.roxy.kryptonplayer.core.ui.components.MochyScrollBlurHeader
import dev.roxy.kryptonplayer.core.ui.components.mochyItemPress
import dev.roxy.kryptonplayer.core.ui.components.mochyScrollFadeEdge
import dev.roxy.kryptonplayer.core.ui.components.toPromptButtonState
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.liquidglass.layerBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.rememberBackdrop
import dev.roxy.kryptonplayer.settings.Setting

/**
 * Full Mochi-style 4th-Tab Hub Experience:
 * - Atmospheric textured Hero Stage with floating interactive Mochi mascot, orbital rings & quick actions
 * - Signature featured "App Settings" card (opens dedicated All Settings screen)
 * - Live animated Updates card with MochyPromptButton (shimmer + download fill + rocket install)
 * - 2x2 Textured Bento Quick-Access tiles (Appearance, Watch Party, Statistics, Storage)
 * - Lead Developer (ROXY) spotlight card + Contributors & Open-Source Credits card
 * - Community & Support tactile pills (Telegram, Boost, GitHub)
 * - Real GPU-blurred scroll header + floating Mochi navigation dock
 */
@Composable
fun MochyHubContent(
    appName: String,
    appVersion: String,
    primaryAbi: String,
    developerName: String,
    updateState: UpdateState,
    pendingUpdateInfo: AppUpdateInfo?,
    onOpenAllSettings: () -> Unit,
    onSettingClick: (Setting) -> Unit,
    onUpdateAction: () -> Unit,
    onOpenLocalVideo: () -> Unit,
    onOpenNetworkStream: () -> Unit,
    onOpenTelegram: () -> Unit,
    onOpenBoost: () -> Unit,
    onOpenGitHub: () -> Unit,
    onOpenContributors: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onSearchClick: () -> Unit,
    onMoreMenuClick: () -> Unit,
    moreMenuContent: @Composable () -> Unit,
) {
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

                // 1. Atmospheric Mochi Hero Stage Card
                item(key = "hub_hero_stage") {
                    MochyStaggeredReveal(index = 0) {
                        MochyHeroStageCard(
                            appName = appName,
                            versionName = appVersion,
                            abi = primaryAbi,
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

                // 5. Lead Developer & Credits Spotlight
                item(key = "hub_creator_header") {
                    MochyStaggeredReveal(index = 6) {
                        HubSectionLabel(title = "Developer & Credits")
                    }
                }

                item(key = "hub_lead_dev_card") {
                    MochyStaggeredReveal(index = 7) {
                        HubLeadDeveloperCard(
                            developerName = developerName,
                            onOpenTelegram = onOpenTelegram,
                            onOpenGitHub = onOpenGitHub,
                            onOpenContributors = onOpenContributors,
                            onOpenAbout = { onSettingClick(Setting.ABOUT) },
                        )
                    }
                }

                // 6. Community & Support Row
                item(key = "hub_community_header") {
                    MochyStaggeredReveal(index = 8) {
                        HubSectionLabel(title = "Community & Support")
                    }
                }

                item(key = "hub_community_card") {
                    MochyStaggeredReveal(index = 9) {
                        HubCommunityCard(
                            onOpenTelegram = onOpenTelegram,
                            onOpenBoost = onOpenBoost,
                            onOpenGitHub = onOpenGitHub,
                        )
                    }
                }

                // 7. Minimal Signature Footer
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
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                        Text(
                            text = "v$appVersion · $primaryAbi · Crafted by $developerName",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
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
                    Text(
                        text = appName,
                        fontSize = (26f - 5f * easedScroll).sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = (-0.5f + 0.2f * easedScroll).sp,
                        modifier = Modifier.graphicsLayer {
                            translationY = (1f - easedScroll) * 2f
                        },
                    )
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        Text(
                            text = "HUB",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Box {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
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
    onOpenLocalVideo: () -> Unit,
    onOpenNetworkStream: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MochyFloatingMascotStage(size = 92.dp)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = "Pure AMOLED 4K HDR Cinema Engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HubMetaPill(text = "v$versionName")
                HubMetaPill(text = abi)
                HubMetaPill(text = "AMOLED")
                HubMetaPill(text = "Media3")
            }

            HorizontalDivider(
                thickness = 0.6.dp,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(top = 4.dp),
            )

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
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.18f)),
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
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colorScheme.onSurface,
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
                Surface(
                    onClick = onChangelogClick,
                    interactionSource = changelogInteraction,
                    modifier = Modifier
                        .mochyItemPress(changelogInteraction, pressedScale = 0.95f)
                        .testTag("hub_changelog_chip"),
                    shape = CircleShape,
                    color = colorScheme.surfaceContainerHigh,
                    border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.14f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.history),
                            contentDescription = null,
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "What's New",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                        )
                    }
                }
            }

            MochyPromptButton(
                state = buttonState,
                onClick = onUpdateClick,
                height = 50.dp,
                containerColor = Color(0xFF17181E),
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
                        .background(Color.White.copy(alpha = 0.09f))
                        .border(0.7.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
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
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
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
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp),
                )
            }

            HorizontalDivider(
                thickness = 0.6.dp,
                color = Color.White.copy(alpha = 0.08f),
            )

            // Built-with / Contributors & About pills
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
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HubCommunityCard(
    onOpenTelegram: () -> Unit,
    onOpenBoost: () -> Unit,
    onOpenGitHub: () -> Unit,
) {
    MochyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hub_community_card"),
        texture = MochyCardTexture.DIAGONAL_GRAIN,
        cornerRadius = 22.dp,
        enable3DTilt = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HubDrawablePill(
                    label = "Telegram",
                    iconRes = R.drawable.telegram,
                    onClick = onOpenTelegram,
                    modifier = Modifier.weight(1f),
                )
                HubDrawablePill(
                    label = "Boost",
                    iconRes = R.drawable.bolt,
                    onClick = onOpenBoost,
                    modifier = Modifier.weight(1f),
                )
                HubDrawablePill(
                    label = "Issues",
                    iconRes = R.drawable.ic_github,
                    onClick = onOpenGitHub,
                    modifier = Modifier.weight(1f),
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
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.0.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
        modifier = Modifier.padding(start = 6.dp, top = 4.dp),
    )
}

@Composable
private fun HubMetaPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
        )
    }
}

@Composable
private fun HubActionPill(
    label: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = 42.dp)
            .mochyItemPress(interactionSource, pressedScale = 0.96f),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.13f)),
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
                fontWeight = FontWeight.SemiBold,
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
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = 40.dp)
            .mochyItemPress(interactionSource, pressedScale = 0.96f),
        shape = RoundedCornerShape(13.dp),
        color = Color.White.copy(alpha = 0.065f),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.12f)),
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
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
