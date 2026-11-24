package dev.roxy.kryptonplayer.settings.composables

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.model.Video
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.MochyBottomBlurScrim
import dev.roxy.kryptonplayer.core.ui.components.MochyMotion
import dev.roxy.kryptonplayer.core.ui.components.MochyNavigationDock
import dev.roxy.kryptonplayer.core.ui.components.MochyScrollBlurHeader
import dev.roxy.kryptonplayer.core.ui.components.mochyItemPress
import dev.roxy.kryptonplayer.core.ui.components.mochyScrollFadeEdge
import dev.roxy.kryptonplayer.core.ui.liquidglass.layerBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.rememberBackdrop
import dev.roxy.kryptonplayer.settings.FolderStatItem
import dev.roxy.kryptonplayer.settings.UserHubUiState
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mochi-style 4th Tab: Pure User Profile, Watch Telemetry & Library Stats Dashboard.
 * - Top bar has ONLY the crisp circular Settings gear icon (opens full All Settings).
 * - Body is 100% focused on the user's real profile, watch time, completion ring,
 *   quality breakdown, recently watched videos, and top folders — zero settings clutter.
 */
@Composable
fun MochyHubContent(
    userState: UserHubUiState,
    onOpenSettings: () -> Unit,
    onOpenDetailedStats: () -> Unit,
    onPlayVideo: (Video) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onSearchClick: () -> Unit,
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
                    top = 74.dp,
                    bottom = 132.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "profile_status_spacer") {
                    Spacer(modifier = Modifier.statusBarsPadding())
                }

                // 1. User Profile Identity Hero Card
                item(key = "user_profile_hero") {
                    MochyStaggeredReveal(index = 0) {
                        UserProfileHeroCard(
                            userState = userState,
                        )
                    }
                }

                // 2. Watch Progress & Cinema Activity Ring Card
                item(key = "user_watch_progress") {
                    MochyStaggeredReveal(index = 1) {
                        WatchProgressRingCard(
                            userState = userState,
                            onClick = onOpenDetailedStats,
                        )
                    }
                }

                // 3. 2x2 Live User Telemetry Bento Tiles
                item(key = "user_stats_bento_header") {
                    MochyStaggeredReveal(index = 2) {
                        UserSectionLabel(title = "Your Telemetry")
                    }
                }

                item(key = "user_stats_bento_row_1") {
                    MochyStaggeredReveal(index = 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatBentoTile(
                                value = formatWatchDuration(userState.totalWatchTimeMs),
                                label = "Watch Time",
                                caption = "Across all local videos",
                                iconRes = R.drawable.history,
                                texture = MochyCardTexture.DOT_MATRIX,
                                testTag = "hub_stat_watch_time",
                                onClick = onOpenDetailedStats,
                                modifier = Modifier.weight(1f),
                            )
                            StatBentoTile(
                                value = "${userState.watchedVideos}/${userState.totalVideos}",
                                label = "Watched",
                                caption = "${userState.completedVideos} completed",
                                iconRes = R.drawable.stats,
                                texture = MochyCardTexture.DIAGONAL_GRAIN,
                                testTag = "hub_stat_watched",
                                onClick = onOpenDetailedStats,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                item(key = "user_stats_bento_row_2") {
                    MochyStaggeredReveal(index = 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatBentoTile(
                                value = formatStorageSize(userState.totalStorageBytes),
                                label = "Media Library",
                                caption = "${userState.totalVideos} total files",
                                iconRes = R.drawable.storage,
                                texture = MochyCardTexture.CONCENTRIC_RADAR,
                                testTag = "hub_stat_storage",
                                onClick = onOpenDetailedStats,
                                modifier = Modifier.weight(1f),
                            )
                            StatBentoTile(
                                value = "${userState.totalFolders}",
                                label = "Folders",
                                caption = formatWatchDuration(userState.totalLibraryDurationMs) + " runtime",
                                iconRes = R.drawable.sync,
                                texture = MochyCardTexture.DOT_MATRIX,
                                testTag = "hub_stat_folders",
                                onClick = onOpenDetailedStats,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // 4. Resolution & Video Quality Breakdown
                item(key = "user_quality_breakdown") {
                    MochyStaggeredReveal(index = 5) {
                        QualityBreakdownCard(userState = userState)
                    }
                }

                // 5. Continue Watching / Recently Played
                item(key = "user_recent_header") {
                    MochyStaggeredReveal(index = 6) {
                        UserSectionLabel(title = "Recently Watched")
                    }
                }

                item(key = "user_recent_card") {
                    MochyStaggeredReveal(index = 7) {
                        RecentlyWatchedCard(
                            videos = userState.recentlyPlayed,
                            onPlayVideo = onPlayVideo,
                        )
                    }
                }

                // 6. Top Media Folders Breakdown
                if (userState.topFolders.isNotEmpty()) {
                    item(key = "user_folders_header") {
                        MochyStaggeredReveal(index = 8) {
                            UserSectionLabel(title = "Top Folders")
                        }
                    }

                    item(key = "user_folders_card") {
                        MochyStaggeredReveal(index = 9) {
                            TopFoldersBreakdownCard(folders = userState.topFolders)
                        }
                    }
                }
            }
        }

        // Floating scroll-reactive frosted glass header with ONLY the Settings icon on top-right
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
                    text = "Profile",
                    fontSize = (28f - 6f * easedScroll).sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    letterSpacing = (-0.5f + 0.2f * easedScroll).sp,
                    modifier = Modifier.graphicsLayer {
                        translationY = (1f - easedScroll) * 2f
                    },
                )

                val settingsInteraction = remember { MutableInteractionSource() }
                Surface(
                    shape = CircleShape,
                    color = colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                    border = BorderStroke(0.85.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier.mochyItemPress(settingsInteraction, pressedScale = 0.92f),
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        interactionSource = settingsInteraction,
                        modifier = Modifier.testTag("hub_top_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Open Settings",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(21.dp),
                        )
                    }
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
private fun UserProfileHeroCard(
    userState: UserHubUiState,
) {
    val colorScheme = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "profile_avatar_halo")
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "profile_halo_rot",
    )

    MochyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mochy_user_profile_card"),
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
            if (!userState.photoUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier.size(86.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = haloRotation }
                            .border(
                                width = 1.8.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.90f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.90f),
                                    ),
                                ),
                                shape = CircleShape,
                            ),
                    )
                    AsyncImage(
                        model = userState.photoUrl,
                        contentDescription = userState.displayName,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else {
                MochyFloatingMascotStage(size = 90.dp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = userState.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = userState.handleOrSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileMetaPill(text = userState.providerLabel)
                if (userState.count4K > 0) {
                    ProfileMetaPill(text = "${userState.count4K} 4K UHD")
                }
                ProfileMetaPill(text = "AMOLED")
            }

            HorizontalDivider(
                thickness = 0.6.dp,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(top = 4.dp),
            )

            // 3-column live summary strip inside Hero
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroQuickMetric(
                    value = formatWatchDuration(userState.totalWatchTimeMs),
                    label = "Watched",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(0.7.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.10f)),
                )
                HeroQuickMetric(
                    value = "${userState.totalVideos}",
                    label = "Videos",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(0.7.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.10f)),
                )
                HeroQuickMetric(
                    value = formatStorageSize(userState.totalStorageBytes),
                    label = "Library",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroQuickMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * Animated circular watch-completion ring card with real watch status breakdown.
 */
@Composable
private fun WatchProgressRingCard(
    userState: UserHubUiState,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = userState.watchedFraction,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "watch_ring_progress",
    )
    val pct = (animatedProgress * 100f).toInt()

    MochyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hub_watch_progress_card"),
        texture = MochyCardTexture.CONCENTRIC_RADAR,
        cornerRadius = 24.dp,
        enable3DTilt = true,
        maxTilt = 4.0f,
        elevated = true,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Animated circular progress ring
            Box(
                modifier = Modifier.size(86.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 7.dp.toPx()
                    val pad = strokeW / 2f + 2.dp.toPx()
                    val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
                    val topLeft = Offset(pad, pad)

                    drawArc(
                        color = Color.White.copy(alpha = 0.10f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    )

                    val sweep = (360f * animatedProgress).coerceAtLeast(if (animatedProgress > 0f) 6f else 0f)
                    if (sweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color.White,
                                    Color.White,
                                ),
                            ),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )

                        val endRad = Math.toRadians((-90.0 + sweep))
                        val cx = size.width / 2f + (arcSize.width / 2f) * cos(endRad).toFloat()
                        val cy = size.height / 2f + (arcSize.height / 2f) * sin(endRad).toFloat()
                        drawCircle(
                            color = Color.White,
                            radius = strokeW * 0.48f,
                            center = Offset(cx, cy),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = "SEEN",
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Cinema Activity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Detailed Statistics",
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp),
                    )
                }

                ActivityMetricRow(label = "Completed", value = "${userState.completedVideos} videos")
                ActivityMetricRow(label = "In Progress", value = "${userState.inProgressVideos} videos")
                ActivityMetricRow(
                    label = "Unwatched",
                    value = "${(userState.totalVideos - userState.watchedVideos).coerceAtLeast(0)} videos",
                )
            }
        }
    }
}

@Composable
private fun ActivityMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun StatBentoTile(
    value: String,
    label: String,
    caption: String,
    @DrawableRes iconRes: Int,
    texture: MochyCardTexture,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    MochyGlassCard(
        modifier = modifier
            .heightIn(min = 122.dp)
            .testTag(testTag),
        texture = texture,
        cornerRadius = 22.dp,
        enable3DTilt = true,
        maxTilt = 5.2f,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White.copy(alpha = 0.09f))
                    .border(0.7.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QualityBreakdownCard(
    userState: UserHubUiState,
) {
    val colorScheme = MaterialTheme.colorScheme
    val total = userState.totalVideos.coerceAtLeast(1).toFloat()
    val w4k = (userState.count4K / total).coerceIn(0f, 1f)
    val wFhd = (userState.countFullHd / total).coerceIn(0f, 1f)
    val wHd = (userState.countHd / total).coerceIn(0f, 1f)
    val wSd = (userState.countSd / total).coerceIn(0f, 1f)

    MochyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hub_quality_breakdown_card"),
        texture = MochyCardTexture.DIAGONAL_GRAIN,
        cornerRadius = 22.dp,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Library Quality",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                )
                ProfileMetaPill(text = "${userState.totalVideos} videos")
            }

            // Monochrome segmented quality bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (w4k > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(w4k)
                            .fillMaxHeight()
                            .background(Color.White),
                    )
                }
                if (wFhd > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(wFhd)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.68f)),
                    )
                }
                if (wHd > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(wHd)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.38f)),
                    )
                }
                if (wSd > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(wSd)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.18f)),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QualityLegendPill(label = "4K", count = userState.count4K, dotAlpha = 1.0f)
                QualityLegendPill(label = "1080p", count = userState.countFullHd, dotAlpha = 0.68f)
                QualityLegendPill(label = "720p", count = userState.countHd, dotAlpha = 0.38f)
                QualityLegendPill(label = "SD", count = userState.countSd, dotAlpha = 0.20f)
            }
        }
    }
}

@Composable
private fun QualityLegendPill(label: String, count: Int, dotAlpha: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = dotAlpha)),
        )
        Text(
            text = "$label · $count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecentlyWatchedCard(
    videos: List<Video>,
    onPlayVideo: (Video) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    MochyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hub_recently_watched_card"),
        texture = MochyCardTexture.DOT_MATRIX,
        cornerRadius = 22.dp,
        enable3DTilt = false,
    ) {
        if (videos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = "No watch history yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = "Videos you play will appear here with live resume progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                videos.forEachIndexed { index, video ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val progress = video.playedPercentage.coerceIn(0f, 1f)
                    val pct = (progress * 100f).toInt()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                            .mochyItemPress(interactionSource, pressedScale = 0.985f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onPlayVideo(video) },
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.6.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = video.artworkUri ?: video.uriString,
                                contentDescription = video.displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.58f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = video.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(Color.White),
                                    )
                                }
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (index < videos.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 90.dp, end = 14.dp),
                            thickness = 0.6.dp,
                            color = Color.White.copy(alpha = 0.07f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopFoldersBreakdownCard(
    folders: List<FolderStatItem>,
) {
    val colorScheme = MaterialTheme.colorScheme
    MochyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hub_top_folders_card"),
        texture = MochyCardTexture.DIAGONAL_GRAIN,
        cornerRadius = 22.dp,
        enable3DTilt = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            folders.forEach { folder ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.80f),
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${folder.videoCount} · ${formatStorageSize(folder.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.09f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(folder.shareFraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.White, Color.White.copy(alpha = 0.55f)),
                                    ),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSectionLabel(title: String) {
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
private fun ProfileMetaPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.13f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun formatWatchDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0m"
    val totalMinutes = durationMs / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours >= 100L -> "${hours}h"
        hours > 0L -> "${hours}h ${minutes}m"
        totalMinutes > 0L -> "${totalMinutes}m"
        else -> "<1m"
    }
}

private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.0f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}
