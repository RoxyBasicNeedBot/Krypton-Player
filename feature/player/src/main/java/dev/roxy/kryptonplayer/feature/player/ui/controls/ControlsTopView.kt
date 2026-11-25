@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package dev.roxy.kryptonplayer.feature.player.ui.controls

import android.content.Context
import android.content.res.Configuration
import android.os.BatteryManager
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.LottieHeartLikeButton
import dev.roxy.kryptonplayer.core.ui.components.LottieLyricsIcon
import dev.roxy.kryptonplayer.core.ui.components.WatchPartyPulseIndicator
import dev.roxy.kryptonplayer.core.ui.extensions.copy
import dev.roxy.kryptonplayer.feature.player.buttons.PlayerButton
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    onAudioClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onMoreOptionsClick: () -> Unit = {},
    watchPartyRoomId: String? = null,
    watchPartyMemberCount: Int = 0,
    onWatchPartyChatClick: (() -> Unit)? = null,
    onWatchPartyInfoClick: () -> Unit = {},
    hasLyrics: Boolean = false,
    onLyricsClick: () -> Unit = {},
    decoderText: String = "HW",
    onDecoderClick: () -> Unit = {},
    showLikeButton: Boolean = true,
    isLiked: Boolean = false,
    onToggleLike: () -> Unit = {}
) {
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()

    Row(
        modifier = modifier
            .padding(systemBarsPadding.copy(bottom = 0.dp))
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Back Button
        PlayerButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Back",
                modifier = Modifier.size(20.dp)
            )
        }

        // Title (Full visibility in portrait & landscape)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title.ifBlank { "Playing Video" },
                style = MaterialTheme.typography.titleMedium.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        blurRadius = 8f
                    ),
                    lineHeight = 20.sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Live Watch Party Badge (if active)
        if (watchPartyRoomId != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(Color(0xFFE53935), shape = RoundedCornerShape(16.dp))
                    .clickable { onWatchPartyInfoClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                WatchPartyPulseIndicator(
                    color = Color(0xFF00E676),
                    height = 12.dp,
                )
                Text(
                    text = "LIVE ($watchPartyMemberCount)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            if (onWatchPartyChatClick != null) {
                PlayerButton(onClick = onWatchPartyChatClick) {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = "Chat",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Instant Live Synced Lyrics Button (if song has lyrics)
        if (hasLyrics) {
            PlayerButton(
                modifier = Modifier.size(40.dp),
                onClick = onLyricsClick
            ) {
                LottieLyricsIcon(
                    size = 24.dp,
                    isPlaying = true
                )
            }
        }

        // Lottie Heart Like Button (Top Bar)
        if (showLikeButton) {
            LottieHeartLikeButton(
                isLiked = isLiked,
                onToggleLike = onToggleLike,
                size = 26.dp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Subtitles Button
        PlayerButton(
            modifier = Modifier.size(40.dp),
            onClick = onSubtitleClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_subtitle_track),
                contentDescription = "Subtitles",
                modifier = Modifier.size(20.dp),
                tint = Color.White,
            )
        }

        // Audio Track Button
        PlayerButton(
            modifier = Modifier.size(40.dp),
            onClick = onAudioClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_audio_track),
                contentDescription = "Audio Track",
                modifier = Modifier.size(20.dp),
                tint = Color.White,
            )
        }

        // Overflow Horizontal Three-Dot Button (More Options)
        PlayerButton(
            modifier = Modifier.size(40.dp),
            onClick = onMoreOptionsClick,
        ) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "More Options",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BatteryAndClockOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var batteryPercentage by remember { mutableIntStateOf(100) }
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            batteryPercentage = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            currentTime = timeFormat.format(Date())
            delay(10000L)
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.BatteryChargingFull,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "$batteryPercentage%",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(10.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = currentTime,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}
