package dev.roxy.kryptonplayer.feature.player.ui.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons

@Composable
fun ProMediaHudOverlay(
    player: PlaybackController,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot by player.state.collectAsState()

    val frameW = snapshot.videoWidth
    val frameH = snapshot.videoHeight
    val resolution = if (frameW > 0 && frameH > 0) "${frameW}x$frameH" else "Detecting..."
    val fps = if (snapshot.containerFps > 0f) "%.2f fps".format(snapshot.containerFps) else "N/A"
    val videoCodec = snapshot.videoCodec?.uppercase() ?: "Auto"
    val hwdec = snapshot.hwdecCurrent?.ifBlank { "software" } ?: "mediacodec"
    val colorHdr = when {
        snapshot.colorTransfer?.contains("pq", ignoreCase = true) == true -> "HDR10 / PQ"
        snapshot.colorTransfer?.contains("hlg", ignoreCase = true) == true -> "HLG / BT.2020"
        snapshot.hdrMode != "OFF" -> snapshot.hdrMode
        else -> "SDR / BT.709"
    }

    val audioCodec = snapshot.audioCodec?.uppercase() ?: "N/A"
    val audioChannels = when (snapshot.audioChannels) {
        6 -> "5.1 Surround (6 Ch)"
        8 -> "7.1 Surround (8 Ch)"
        2 -> "Stereo (2 Ch)"
        1 -> "Mono (1 Ch)"
        else -> if (snapshot.audioChannels > 0) "${snapshot.audioChannels} Ch" else "Stereo"
    }
    val audioSampleRate = if (snapshot.audioSampleRate > 0) "${snapshot.audioSampleRate} Hz" else "48000 Hz"
    val bufferedSec = ((snapshot.bufferedPositionMs - snapshot.currentPositionMs).coerceAtLeast(0L) / 1000.0)
    val bufferText = "%.1fs ahead".format(bufferedSec)

    Card(
        modifier = modifier
            .padding(16.dp)
            .width(320.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF090D16).copy(alpha = 0.88f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_feather_activity),
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STATS FOR NERDS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                        ),
                        color = Color(0xFF00E676),
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = NextIcons.Close,
                        contentDescription = "Close HUD",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HudSectionTitle("VIDEO STREAM")
            HudStatRow("Resolution", resolution)
            HudStatRow("Frame Rate", fps)
            HudStatRow("Codec", videoCodec)
            HudStatRow("Color Space", colorHdr)
            HudStatRow("Anime4K", snapshot.anime4kMode)

            Spacer(modifier = Modifier.height(8.dp))

            HudSectionTitle("AUDIO STREAM")
            HudStatRow("Codec", audioCodec)
            HudStatRow("Channels", audioChannels)
            HudStatRow("Sample Rate", audioSampleRate)

            Spacer(modifier = Modifier.height(8.dp))

            HudSectionTitle("PLAYBACK ENGINE (libmpv)")
            HudStatRow("Decoder", hwdec)
            HudStatRow("Buffer Health", bufferText)
            HudStatRow("Speed", "%.2fx".format(snapshot.playbackSpeed))
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
            letterSpacing = 1.2.sp,
        ),
        color = Color(0xFF64748B),
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

@Composable
private fun HudStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = Color(0xFF94A3B8),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            ),
            color = Color(0xFFF1F5F9),
        )
    }
}
