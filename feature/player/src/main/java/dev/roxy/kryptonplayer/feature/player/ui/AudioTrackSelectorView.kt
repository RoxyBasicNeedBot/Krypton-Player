package dev.roxy.kryptonplayer.feature.player.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.feature.player.extensions.getName
import dev.roxy.kryptonplayer.feature.player.state.rememberTracksState

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.AudioTrackSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    playerPreferences: dev.roxy.kryptonplayer.core.model.PlayerPreferences? = null,
    onUpdatePlayerPreferences: ((dev.roxy.kryptonplayer.core.model.PlayerPreferences) -> dev.roxy.kryptonplayer.core.model.PlayerPreferences) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val audioTracksState = rememberTracksState(player, C.TRACK_TYPE_AUDIO)
    val selectedTrackIndex = audioTracksState.tracks.indexOfFirst { it.isSelected }
    val activeTrackName = if (selectedTrackIndex != -1) {
        audioTracksState.tracks[selectedTrackIndex].mediaTrackGroup.getName(C.TRACK_TYPE_AUDIO, selectedTrackIndex)
    } else null

    var audioBoostEnabled by remember(playerPreferences?.enableVolumeBoost) {
        mutableStateOf(playerPreferences?.enableVolumeBoost ?: true)
    }
    var audioDelayMs by remember { mutableLongStateOf(0L) }

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Audio Settings & Streams",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header summary card
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
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio Streams",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (activeTrackName != null) "Active: $activeTrackName" else "${audioTracksState.tracks.size} tracks available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Audio Tracks List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AVAILABLE AUDIO STREAMS (${audioTracksState.tracks.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )

                if (audioTracksState.tracks.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "No audio tracks detected in media",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    audioTracksState.tracks.forEachIndexed { index, track ->
                        val isSelected = track.isSelected
                        val trackFormat = (0 until track.mediaTrackGroup.length).map { track.mediaTrackGroup.getFormat(it) }.firstOrNull()
                        val trackName = track.mediaTrackGroup.getName(C.TRACK_TYPE_AUDIO, index)

                        val channelStr = when (trackFormat?.channelCount) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            6 -> "5.1 Surround"
                            8 -> "7.1 Surround"
                            null -> null
                            else -> "${trackFormat.channelCount} CH"
                        }
                        val sampleRateStr = if (trackFormat != null && trackFormat.sampleRate > 0) "${trackFormat.sampleRate / 1000} kHz" else null
                        val codecStr = trackFormat?.sampleMimeType?.substringAfter("/")?.uppercase()

                        Card(
                            onClick = {
                                audioTracksState.switchTrack(index)
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
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
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

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp),
                                    ) {
                                        if (codecStr != null) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(codecStr, fontSize = 10.sp) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                ),
                                            )
                                        }
                                        if (channelStr != null) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(channelStr, fontSize = 10.sp) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                ),
                                            )
                                        }
                                        if (sampleRateStr != null) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(sampleRateStr, fontSize = 10.sp) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Mute / Disable Track Card
                    val isNoneSelected = audioTracksState.tracks.none { it.isSelected }
                    Card(
                        onClick = {
                            audioTracksState.switchTrack(-1)
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
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            // Audio Enhancement & Boost
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AUDIO BOOST & ENHANCEMENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )

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
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Audio Volume Boost",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Boost volume up to 200% for quiet movies",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            dev.roxy.kryptonplayer.core.ui.components.NextSwitch(
                                checked = audioBoostEnabled,
                                onCheckedChange = {
                                    audioBoostEnabled = it
                                    onUpdatePlayerPreferences { pref -> pref.copy(enableVolumeBoost = it) }
                                },
                            )
                        }
                    }
                }
            }

            // Audio Synchronization / Delay
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AUDIO SYNCHRONIZATION (DELAY)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )

                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
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
                                    text = "Offset Delay",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Text(
                                text = if (audioDelayMs >= 0) "+${audioDelayMs} ms" else "${audioDelayMs} ms",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (audioDelayMs != 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledTonalButton(
                                onClick = { audioDelayMs -= 100L },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("-100ms", fontSize = 12.sp)
                            }
                            FilledTonalButton(
                                onClick = { audioDelayMs = 0L },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Text("Reset", fontSize = 12.sp)
                            }
                            FilledTonalButton(
                                onClick = { audioDelayMs += 100L },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("+100ms", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
