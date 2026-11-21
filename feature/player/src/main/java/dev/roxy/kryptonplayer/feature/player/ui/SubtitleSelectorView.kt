package dev.roxy.kryptonplayer.feature.player.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SubtitlesOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.roxy.kryptonplayer.core.model.Font
import dev.roxy.kryptonplayer.core.model.PlayerPreferences
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.NextSwitch
import dev.roxy.kryptonplayer.feature.player.extensions.getName
import dev.roxy.kryptonplayer.feature.player.state.SubtitleOptionsEvent
import dev.roxy.kryptonplayer.feature.player.state.rememberSubtitleOptionsState
import dev.roxy.kryptonplayer.feature.player.state.rememberTracksState
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.SubtitleSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onSelectSubtitleClick: () -> Unit,
    onDownloadOnlineSubtitleClick: () -> Unit = {},
    onEvent: (SubtitleOptionsEvent) -> Unit = {},
    playerPreferences: PlayerPreferences? = null,
    onUpdatePlayerPreferences: ((PlayerPreferences) -> PlayerPreferences) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val subtitleTracksState = rememberTracksState(player, C.TRACK_TYPE_TEXT)
    val subtitleOptionsState = rememberSubtitleOptionsState(player, onEvent)

    val selectedTrackIndex = subtitleTracksState.tracks.indexOfFirst { it.isSelected }
    val activeTrackName = if (selectedTrackIndex != -1) {
        subtitleTracksState.tracks[selectedTrackIndex].mediaTrackGroup.getName(C.TRACK_TYPE_TEXT, selectedTrackIndex)
    } else null

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Subtitle Style states (sync with preferences)
    var currentFontSize by remember(playerPreferences?.subtitleTextSize) {
        mutableFloatStateOf(playerPreferences?.subtitleTextSize?.toFloat() ?: 20f)
    }
    var isBold by remember(playerPreferences?.subtitleTextBold) {
        mutableStateOf(playerPreferences?.subtitleTextBold ?: true)
    }
    var hasBackground by remember(playerPreferences?.subtitleBackground) {
        mutableStateOf(playerPreferences?.subtitleBackground ?: false)
    }
    var selectedFont by remember(playerPreferences?.subtitleFont) {
        mutableStateOf(playerPreferences?.subtitleFont ?: Font.DEFAULT)
    }

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Subtitle Settings",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Subtitles,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Subtitles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = activeTrackName ?: "No Subtitles Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // 3-Segmented Navigation Tabs: Tracks, Style, Sync
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SegmentedButton(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.List,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                ) {
                    Text("Tracks", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                SegmentedButton(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Style,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                ) {
                    Text("Style", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                SegmentedButton(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                ) {
                    Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Tab Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedTabIndex) {
                    // TAB 0: Tracks & Online Download
                    0 -> {
                        // Quick Action Buttons (Download & Open File)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onDownloadOnlineSubtitleClick()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download Online", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    onSelectSubtitleClick()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Local File", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "AVAILABLE TRACKS (${subtitleTracksState.tracks.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )

                        if (subtitleTracksState.tracks.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "No subtitle tracks found. Click 'Download Online' above to search subtitles.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        } else {
                            subtitleTracksState.tracks.forEachIndexed { index, track ->
                                val isSelected = track.isSelected
                                val trackName = track.mediaTrackGroup.getName(C.TRACK_TYPE_TEXT, index)
                                val trackFormat = (0 until track.mediaTrackGroup.length).map { track.mediaTrackGroup.getFormat(it) }.firstOrNull()
                                val mimeStr = trackFormat?.sampleMimeType?.substringAfter("/")?.uppercase() ?: "TEXT"

                                Card(
                                    onClick = {
                                        subtitleTracksState.switchTrack(index)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainer
                                        },
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(34.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Subtitles,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = trackName,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "Format: $mimeStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }

                            // Disable Subtitles Card
                            val isNoneSelected = subtitleTracksState.tracks.none { it.isSelected }
                            Card(
                                onClick = {
                                    subtitleTracksState.switchTrack(-1)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isNoneSelected) {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isNoneSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isNoneSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isNoneSelected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(34.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.SubtitlesOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = stringResource(R.string.disable),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                        color = if (isNoneSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    // TAB 1: Subtitle Style & Live Preview
                    1 -> {
                        // Live Subtitle Preview Box
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.7f),
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                val fontFamily = when (selectedFont) {
                                    Font.DEFAULT -> FontFamily.Default
                                    Font.MONOSPACE -> FontFamily.Monospace
                                    Font.SANS_SERIF -> FontFamily.SansSerif
                                    Font.SERIF -> FontFamily.Serif
                                }
                                Box(
                                    modifier = if (hasBackground) {
                                        Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.75f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    } else Modifier,
                                ) {
                                    Text(
                                        text = "Sample Subtitle Text (Live Preview)",
                                        color = Color.White,
                                        fontSize = (currentFontSize * 0.8f).sp,
                                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = fontFamily,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        // Size Slider
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.FormatSize,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Subtitle Text Size",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Text(
                                        text = "${currentFontSize.toInt()} sp",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Slider(
                                    value = currentFontSize,
                                    onValueChange = {
                                        currentFontSize = it
                                        onUpdatePlayerPreferences { prefs -> prefs.copy(subtitleTextSize = it.toInt()) }
                                    },
                                    valueRange = 12f..36f,
                                    steps = 12,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        // Font Selector
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Font Family",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Font.entries.forEach { fontOption ->
                                        val isFontSelected = selectedFont == fontOption
                                        FilledTonalButton(
                                            onClick = {
                                                selectedFont = fontOption
                                                onUpdatePlayerPreferences { prefs -> prefs.copy(subtitleFont = fontOption) }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = if (isFontSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isFontSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            ),
                                        ) {
                                            Text(fontOption.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        // Bold & Background Toggles
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.FormatBold,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Bold Subtitle Text",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    NextSwitch(
                                        checked = isBold,
                                        onCheckedChange = {
                                            isBold = it
                                            onUpdatePlayerPreferences { prefs -> prefs.copy(subtitleTextBold = it) }
                                        },
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Style,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Subtitle Background Box",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    NextSwitch(
                                        checked = hasBackground,
                                        onCheckedChange = {
                                            hasBackground = it
                                            onUpdatePlayerPreferences { prefs -> prefs.copy(subtitleBackground = it) }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // TAB 2: Subtitle Synchronization (Delay & Speed)
                    2 -> {
                        // Subtitle Delay Offset
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Sync,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Subtitle Delay Offset",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    val delayVal = subtitleOptionsState.delayMilliseconds
                                    Text(
                                        text = if (delayVal >= 0) "+${delayVal} ms" else "${delayVal} ms",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (delayVal != 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    FilledTonalButton(
                                        onClick = { subtitleOptionsState.setDelay(subtitleOptionsState.delayMilliseconds - 500L) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("-500ms", fontSize = 11.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { subtitleOptionsState.setDelay(subtitleOptionsState.delayMilliseconds - 50L) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("-50ms", fontSize = 11.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { subtitleOptionsState.setDelay(0L) },
                                        modifier = Modifier.weight(0.9f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        ),
                                    ) {
                                        Text("Reset", fontSize = 11.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { subtitleOptionsState.setDelay(subtitleOptionsState.delayMilliseconds + 50L) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("+50ms", fontSize = 11.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { subtitleOptionsState.setDelay(subtitleOptionsState.delayMilliseconds + 500L) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("+500ms", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Subtitle Speed Multiplier
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Speed,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Subtitle Speed Ratio",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%.2fx", subtitleOptionsState.speedMultiplier),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (subtitleOptionsState.speedMultiplier != 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    FilledTonalButton(
                                        onClick = { subtitleOptionsState.setSpeed((subtitleOptionsState.speedMultiplier - 0.05f).coerceAtLeast(0.5f)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("-0.05x", fontSize = 12.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { subtitleOptionsState.setSpeed(1.0f) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        ),
                                    ) {
                                        Text("1.00x Reset", fontSize = 12.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { subtitleOptionsState.setSpeed((subtitleOptionsState.speedMultiplier + 0.05f).coerceAtMost(2.0f)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("+0.05x", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
