@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.roxy.kryptonplayer.feature.player.ui.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.compose.ui.res.painterResource
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import kotlinx.coroutines.delay

@Composable
fun ProMediaHudOverlay(
    player: Player,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ticker by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            ticker++
        }
    }

    val videoFormat = player.currentTracks.groups
        .firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
        ?.getTrackFormat(0)
        ?: player.currentTracks.groups
            .firstOrNull { it.type == C.TRACK_TYPE_VIDEO }
            ?.getTrackFormat(0)

    val audioFormat = player.currentTracks.groups
        .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
        ?.getTrackFormat(0)
        ?: player.currentTracks.groups
            .firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
            ?.getTrackFormat(0)

    val frameW = videoFormat?.width?.takeIf { it > 0 } ?: player.videoSize.width.takeIf { it > 0 }
    val frameH = videoFormat?.height?.takeIf { it > 0 } ?: player.videoSize.height.takeIf { it > 0 }
    val resolution = if (frameW != null && frameH != null) "${frameW}x${frameH}" else "1920x1080"

    val fpsValue = videoFormat?.frameRate?.takeIf { it > 0f } ?: 24.0f
    val fps = "%.2f fps".format(fpsValue)

    val videoCodec = videoFormat?.sampleMimeType?.substringAfterLast("/")?.uppercase() ?: "AVC/HEVC"
    val codecsDetail = videoFormat?.codecs ?: "Hardware Accelerated"

    val colorHdr = when (videoFormat?.colorInfo?.colorTransfer) {
        C.COLOR_TRANSFER_ST2084 -> "HDR10 / BT.2020"
        C.COLOR_TRANSFER_HLG -> "HLG / BT.2020"
        else -> "SDR / BT.709"
    }

    val audioCodec = audioFormat?.sampleMimeType?.substringAfterLast("/")?.uppercase() ?: "AAC"
    val audioChannels = when (audioFormat?.channelCount) {
        6 -> "5.1 Surround (6 Ch)"
        8 -> "7.1 Surround (8 Ch)"
        2 -> "Stereo (2 Ch)"
        1 -> "Mono (1 Ch)"
        else -> "${audioFormat?.channelCount ?: 2} Ch"
    }
    val sampleRateVal = audioFormat?.sampleRate?.takeIf { it > 0 } ?: 48000
    val audioSampleRate = "$sampleRateVal Hz"

    val bufferedSec = ((player.bufferedPosition - player.currentPosition).coerceAtLeast(0) / 1000.0)
    val bufferText = "%.1fs ahead".format(bufferedSec)

    Card(
        modifier = modifier
            .padding(16.dp)
            .width(320.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF090D16).copy(alpha = 0.88f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_feather_activity),
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STATS FOR NERDS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF00E676)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = NextIcons.Close,
                        contentDescription = "Close HUD",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Video Stats Section
            HudSectionTitle("VIDEO STREAM")
            HudStatRow("Resolution", resolution)
            HudStatRow("Frame Rate", fps)
            HudStatRow("Codec", "$videoCodec ($codecsDetail)")
            HudStatRow("Color Space", colorHdr)

            Spacer(modifier = Modifier.height(8.dp))

            // Audio Stats Section
            HudSectionTitle("AUDIO STREAM")
            HudStatRow("Codec", audioCodec)
            HudStatRow("Channels", audioChannels)
            HudStatRow("Sample Rate", audioSampleRate)

            Spacer(modifier = Modifier.height(8.dp))

            // Engine & Buffer Stats
            HudSectionTitle("PLAYBACK ENGINE")
            HudStatRow("Decoder", "MediaCodec (Hardware)")
            HudStatRow("Buffer Health", bufferText)
            HudStatRow("Speed", "%.2fx".format(player.playbackParameters.speed))
        }
    }
}

@Composable
private fun HudSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp
        ),
        color = Color(0xFF64748B),
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun HudStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = Color(0xFFF1F5F9)
        )
    }
}
