package dev.roxy.kryptonplayer.settings.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.MochyMotion
import dev.roxy.kryptonplayer.core.ui.components.MochyScrollBlurHeader
import dev.roxy.kryptonplayer.core.ui.components.mochyItemPress
import dev.roxy.kryptonplayer.core.ui.components.mochyScrollFadeEdge
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.liquidglass.layerBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.rememberBackdrop
import dev.roxy.kryptonplayer.settings.Setting

/**
 * Dedicated "App Settings" screen opened from the featured App Settings card inside Mochi Hub.
 * Renders all 14 settings categories inside textured AMOLED obsidian glass groups with a live
 * scroll-blurred top header and back navigation back to the Hub.
 */
@Composable
fun AllSettingsContent(
    appVersion: String,
    updateState: UpdateState,
    onBackToHub: () -> Unit,
    onItemClick: (Setting) -> Unit,
    onUpdateAction: () -> Unit,
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
                item(key = "all_settings_status_spacer") {
                    Spacer(modifier = Modifier.statusBarsPadding())
                }

                // 1. Customisation & Interface
                item(key = "group_customisation") {
                    MochyStaggeredReveal(index = 0) {
                        AllSettingsCardGroup(
                            title = "Customisation & Interface",
                            texture = MochyCardTexture.DOT_MATRIX,
                            items = listOf(
                                AllSettingsRowItem(
                                    title = stringResource(id = R.string.appearance_name),
                                    subtitle = "AMOLED black, palettes, dock & seekbar",
                                    iconRes = R.drawable.palette,
                                    testTag = "setting_appearance",
                                    onClick = { onItemClick(Setting.APPEARANCE) },
                                ),
                                AllSettingsRowItem(
                                    title = stringResource(id = R.string.media_library),
                                    subtitle = stringResource(id = R.string.media_library_description),
                                    iconRes = R.drawable.history,
                                    testTag = "setting_media_library",
                                    onClick = { onItemClick(Setting.MEDIA_LIBRARY) },
                                ),
                                AllSettingsRowItem(
                                    title = "Storage & Cache",
                                    subtitle = "Manage downloads, cache & local storage",
                                    iconRes = R.drawable.storage,
                                    testTag = "setting_storage",
                                    onClick = { onItemClick(Setting.STORAGE) },
                                ),
                            ),
                        )
                    }
                }

                // 2. Playback & Behaviour
                item(key = "group_playback") {
                    MochyStaggeredReveal(index = 1) {
                        AllSettingsCardGroup(
                            title = "Playback & App Behaviour",
                            texture = MochyCardTexture.DIAGONAL_GRAIN,
                            items = listOf(
                                AllSettingsRowItem(
                                    title = stringResource(id = R.string.player_name),
                                    subtitle = stringResource(id = R.string.player_description),
                                    iconRes = R.drawable.tune,
                                    testTag = "setting_player",
                                    onClick = { onItemClick(Setting.PLAYER) },
                                ),
                                AllSettingsRowItem(
                                    title = stringResource(id = R.string.gestures_name),
                                    subtitle = stringResource(id = R.string.gestures_description),
                                    iconRes = R.drawable.swipe,
                                    testTag = "setting_gestures",
                                    onClick = { onItemClick(Setting.GESTURES) },
                                ),
                                AllSettingsRowItem(
                                    title = stringResource(id = R.string.audio),
                                    subtitle = stringResource(id = R.string.audio_desc),
                                    iconRes = R.drawable.equalizer,
                                    testTag = "setting_audio",
                                    onClick = { onItemClick(Setting.AUDIO) },
                                ),
                                AllSettingsRowItem(
                                    title = stringResource(id = R.string.decoder),
                                    subtitle = stringResource(id = R.string.decoder_desc),
                                    iconRes = R.drawable.sliders,
                                    testTag = "setting_decoder",
                                    onClick = { onItemClick(Setting.DECODER) },
                                ),
                                AllSettingsRowItem(
                                    title = stringResource(id = R.string.subtitle),
                                    subtitle = stringResource(id = R.string.subtitle_desc),
                                    iconRes = R.drawable.language,
                                    testTag = "setting_subtitle",
                                    onClick = { onItemClick(Setting.SUBTITLE) },
                                ),
                                AllSettingsRowItem(
                                    title = "Lyrics",
                                    subtitle = "Synced glow lyrics, style & providers",
                                    iconRes = R.drawable.lyrics,
                                    testTag = "setting_lyrics",
                                    onClick = { onItemClick(Setting.LYRICS) },
                                ),
                                AllSettingsRowItem(
                                    title = "Watch Party",
                                    subtitle = "Telegram Cloud & synchronized playback",
                                    iconRes = R.drawable.sync,
                                    testTag = "setting_watch_party",
                                    onClick = { onItemClick(Setting.WATCH_PARTY) },
                                ),
                            ),
                        )
                    }
                }

                // 3. System & Data
                item(key = "group_system") {
                    MochyStaggeredReveal(index = 2) {
                        AllSettingsCardGroup(
                            title = "System & Data",
                            texture = MochyCardTexture.DOT_MATRIX,
                            items = listOf(
                                AllSettingsRowItem(
                                    title = stringResource(id = R.string.general_name),
                                    subtitle = stringResource(id = R.string.general_description),
                                    iconRes = R.drawable.contrast,
                                    testTag = "setting_general",
                                    onClick = { onItemClick(Setting.GENERAL) },
                                ),
                                AllSettingsRowItem(
                                    title = "Statistics",
                                    subtitle = "Watch history & folder breakdown",
                                    iconRes = R.drawable.stats,
                                    testTag = "setting_stats",
                                    onClick = { onItemClick(Setting.STATS) },
                                ),
                                AllSettingsRowItem(
                                    title = "Backup & Restore",
                                    subtitle = "Export, import & migrate preferences",
                                    iconRes = R.drawable.backup,
                                    testTag = "setting_backup_restore",
                                    onClick = { onItemClick(Setting.BACKUP_RESTORE) },
                                ),
                            ),
                        )
                    }
                }

                // 4. Updates & About
                item(key = "group_about") {
                    MochyStaggeredReveal(index = 3) {
                        val isChecking = updateState is UpdateState.Checking
                        val updateSubtitle = when (val state = updateState) {
                            is UpdateState.Checking -> "Checking for updates..."
                            is UpdateState.Available -> "Update v${state.info.versionName} available"
                            is UpdateState.Downloading -> {
                                if (state.progress in 0..100) "Downloading ${state.progress}%" else "Downloading update..."
                            }
                            is UpdateState.ReadyToInstall -> "Tap to install downloaded update"
                            else -> "Current version v$appVersion"
                        }

                        AllSettingsCardGroup(
                            title = "Updates, Credits & About",
                            texture = MochyCardTexture.CONCENTRIC_RADAR,
                            items = listOf(
                                AllSettingsRowItem(
                                    title = "Updates",
                                    subtitle = updateSubtitle,
                                    iconVector = Icons.Rounded.SystemUpdate,
                                    isLoading = isChecking,
                                    testTag = "setting_check_updates",
                                    onClick = onUpdateAction,
                                ),
                                AllSettingsRowItem(
                                    title = stringResource(id = R.string.about_name),
                                    subtitle = "Lead developer, contributors, support & licenses",
                                    iconRes = R.drawable.info,
                                    testTag = "setting_about",
                                    onClick = { onItemClick(Setting.ABOUT) },
                                ),
                            ),
                        )
                    }
                }

                item(key = "all_settings_nav_spacer") {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }

        // Scroll-reactive frosted glass top bar with Back-to-Hub button
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        IconButton(
                            onClick = onBackToHub,
                            modifier = Modifier.testTag("all_settings_back_button"),
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
                        text = "App Settings",
                        fontSize = (24f - 4f * easedScroll).sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = (-0.4f + 0.15f * easedScroll).sp,
                        modifier = Modifier.graphicsLayer {
                            translationY = (1f - easedScroll) * 1.5f
                        },
                    )
                }

                Box {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        IconButton(
                            onClick = onMoreMenuClick,
                            modifier = Modifier.testTag("all_settings_more_button"),
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
    }
}

private data class AllSettingsRowItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int = 0,
    val iconVector: ImageVector? = null,
    val isLoading: Boolean = false,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
private fun AllSettingsCardGroup(
    title: String,
    texture: MochyCardTexture,
    items: List<AllSettingsRowItem>,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.9.sp,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        MochyGlassCard(
            modifier = Modifier.fillMaxWidth(),
            texture = texture,
            cornerRadius = 22.dp,
            enable3DTilt = false,
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
                            .testTag(item.testTag),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.65.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                item.isLoading -> CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colorScheme.primary,
                                )
                                item.iconRes != 0 -> Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp),
                                )
                                item.iconVector != null -> Icon(
                                    imageVector = item.iconVector,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp),
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
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
                            color = Color.White.copy(alpha = 0.07f),
                        )
                    }
                }
            }
        }
    }
}
