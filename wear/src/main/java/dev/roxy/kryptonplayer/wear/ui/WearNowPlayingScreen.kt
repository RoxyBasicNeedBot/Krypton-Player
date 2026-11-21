package dev.roxy.kryptonplayer.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

private const val CMD_PLAY_PAUSE = "/krypton/play_pause"
private const val CMD_NEXT = "/krypton/next"
private const val CMD_PREV = "/krypton/prev"
private const val CMD_VOLUME_UP = "/krypton/volume_up"
private const val CMD_VOLUME_DOWN = "/krypton/volume_down"
private const val CMD_GET_STATUS = "/krypton/get_status"
private const val PATH_STATUS = "/krypton/status"

@Composable
fun WearNowPlayingScreen() {
    val context = LocalContext.current
    val messageClient = remember { Wearable.getMessageClient(context) }
    val nodeClient = remember { Wearable.getNodeClient(context) }

    var mediaTitle by remember { mutableStateOf("No Media Playing") }
    var isPlaying by remember { mutableStateOf(false) }
    var volumePercent by remember { mutableIntStateOf(-1) }
    var isConnected by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    var accumulatedScroll by remember { mutableFloatStateOf(0f) }
    val scrollThreshold = 35f

    fun sendCommand(path: String) {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isNotEmpty()) {
                isConnected = true
                for (node in nodes) {
                    messageClient.sendMessage(node.id, path, ByteArray(0))
                }
            } else {
                isConnected = false
            }
        }
    }

    DisposableEffect(messageClient, nodeClient) {
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == PATH_STATUS) {
                try {
                    val jsonStr = String(event.data, Charsets.UTF_8)
                    val json = JSONObject(jsonStr)
                    val title = json.optString("title", "No Media Playing")
                    val playing = json.optBoolean("isPlaying", false)
                    val vol = json.optInt("volume", -1)
                    val maxVol = json.optInt("maxVolume", 1)

                    mediaTitle = if (title.isBlank()) "No Media Playing" else title
                    isPlaying = playing
                    isConnected = true
                    if (vol >= 0 && maxVol > 0) {
                        volumePercent = ((vol.toFloat() / maxVol.toFloat()) * 100).toInt()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        messageClient.addListener(listener)

        // Initial status request
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            isConnected = nodes.isNotEmpty()
            for (node in nodes) {
                messageClient.sendMessage(node.id, CMD_GET_STATUS, ByteArray(0))
            }
        }

        onDispose {
            messageClient.removeListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onRotaryScrollEvent { event ->
                    accumulatedScroll += event.verticalScrollPixels
                    if (accumulatedScroll >= scrollThreshold) {
                        accumulatedScroll = 0f
                        sendCommand(CMD_VOLUME_DOWN)
                        true
                    } else if (accumulatedScroll <= -scrollThreshold) {
                        accumulatedScroll = 0f
                        sendCommand(CMD_VOLUME_UP)
                        true
                    } else {
                        true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Status Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) (if (isPlaying) "Playing" else "Paused") else "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        maxLines = 1,
                        fontSize = 11.sp
                    )
                }

                // Media Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = mediaTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp
                    )
                }

                // Playback Controls Row: Previous, Play/Pause, Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { sendCommand(CMD_PREV) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Prominent Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7C4DFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                isPlaying = !isPlaying
                                sendCommand(CMD_PLAY_PAUSE)
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { sendCommand(CMD_NEXT) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Bottom Volume Controls Row (supports tap & rotary crown)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { sendCommand(CMD_VOLUME_DOWN) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeDown,
                            contentDescription = "Volume Down",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (volumePercent >= 0) "$volumePercent%" else "Vol",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { sendCommand(CMD_VOLUME_UP) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume Up",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
