package dev.roxy.kryptonplayer.feature.player.ui.soundtrack

import android.Manifest
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.data.repository.SoundtrackItem
import dev.roxy.kryptonplayer.core.data.shazam.ShazamRecognizedTrack
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.feature.player.PlayerViewModel
import dev.roxy.kryptonplayer.feature.player.ShazamRecognitionUiState
import dev.roxy.kryptonplayer.feature.player.SoundtrackUiState
import kotlinx.coroutines.delay

/**
 * Anthropic Brand Guidelines Color Palette
 * Main: Dark #141413, Light #faf9f5, Mid Gray #b0aea5, Light Gray #e8e6dc
 * Accents: Coral #d97757, Sky Blue #6a9bcc, Moss Green #788c5d
 */
private object AnthropicBrand {
    val Dark = Color(0xFF141413)
    val Light = Color(0xFFFAF9F5)
    val MidGray = Color(0xFFB0AEA5)
    val LightGray = Color(0xFFE8E6DC)
    val Coral = Color(0xFFD97757)
    val SkyBlue = Color(0xFF6A9BCC)
    val MossGreen = Color(0xFF788C5D)
    val CardDark = Color(0xFF1E1E1C)
    val BorderDark = Color(0xFF2B2926)
    val ButtonDark = Color(0xFF262522)
}

data class ShazamPreviewInfo(
    val title: String,
    val artist: String,
    val coverUrl: String?
)

private fun formatMillis(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundtrackFinderBottomSheet(
    viewModel: PlayerViewModel,
    videoTitle: String,
    videoUri: Uri? = null,
    currentPositionMs: Long = 0L,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Shazam Video/Live, 1 = Movie OSTs

    val shazamState by viewModel.shazamState.collectAsStateWithLifecycle()
    val soundtrackState by viewModel.soundtrackState.collectAsStateWithLifecycle()

    var isBoostEnabled by remember { mutableStateOf(true) }
    var playingPreviewUrl by remember { mutableStateOf<String?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewCurrentPositionMs by remember { mutableLongStateOf(0L) }
    var previewDurationMs by remember { mutableLongStateOf(30000L) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewTrackInfo by remember { mutableStateOf<ShazamPreviewInfo?>(null) }

    val initialCleanQuery = remember(videoTitle) {
        dev.roxy.kryptonplayer.core.common.Utils.cleanMediaTitle(videoTitle).first
    }
    var searchQuery by remember { mutableStateOf(initialCleanQuery) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startLiveShazamRecognition(context, boostMode = isBoostEnabled)
        }
    }

    LaunchedEffect(videoTitle) {
        viewModel.loadSoundtracks(videoTitle)
    }

    // Polling loop for audio preview playback progress bar
    LaunchedEffect(playingPreviewUrl, isPreviewPlaying) {
        while (playingPreviewUrl != null && isPreviewPlaying) {
            try {
                previewPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        previewCurrentPositionMs = mp.currentPosition.toLong()
                        val dur = mp.duration.toLong()
                        if (dur > 0) previewDurationMs = dur
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
            delay(200L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                previewPlayer?.stop()
                previewPlayer?.release()
                previewPlayer = null
                playingPreviewUrl = null
                isPreviewPlaying = false
                previewTrackInfo = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playAudioPreview(url: String, title: String? = null, artist: String? = null, coverUrl: String? = null) {
        try {
            if (playingPreviewUrl == url && isPreviewPlaying) {
                previewPlayer?.pause()
                isPreviewPlaying = false
                return
            } else if (playingPreviewUrl == url && !isPreviewPlaying) {
                previewPlayer?.start()
                isPreviewPlaying = true
                return
            }

            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
            previewCurrentPositionMs = 0L
            isPreviewPlaying = false
            if (title != null && artist != null) {
                previewTrackInfo = ShazamPreviewInfo(title, artist, coverUrl)
            }

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener {
                    start()
                    playingPreviewUrl = url
                    isPreviewPlaying = true
                    val dur = duration.toLong()
                    if (dur > 0) previewDurationMs = dur
                }
                setOnCompletionListener {
                    playingPreviewUrl = null
                    isPreviewPlaying = false
                    previewCurrentPositionMs = 0L
                    previewTrackInfo = null
                }
                setOnErrorListener { _, _, _ ->
                    playingPreviewUrl = null
                    isPreviewPlaying = false
                    previewCurrentPositionMs = 0L
                    previewTrackInfo = null
                    true
                }
                prepareAsync()
            }
            previewPlayer = mp
        } catch (e: Exception) {
            e.printStackTrace()
            playingPreviewUrl = null
            isPreviewPlaying = false
            previewTrackInfo = null
        }
    }

    fun stopAudioPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
            playingPreviewUrl = null
            isPreviewPlaying = false
            previewCurrentPositionMs = 0L
            previewTrackInfo = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseResumeAudioPreview() {
        try {
            previewPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    isPreviewPlaying = false
                } else {
                    mp.start()
                    isPreviewPlaying = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekAudioPreview(targetPosMs: Long) {
        try {
            previewPlayer?.seekTo(targetPosMs.toInt())
            previewCurrentPositionMs = targetPosMs
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopAudioPreview()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = AnthropicBrand.Dark,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AnthropicBrand.Coral.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_soundtrack_custom),
                            contentDescription = null,
                            tint = AnthropicBrand.Coral,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Shazam & Soundtracks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AnthropicBrand.Light
                        )
                        Text(
                            text = "Apple Music • High-Gain Audio Boost • 30s Preview",
                            style = MaterialTheme.typography.bodySmall,
                            color = AnthropicBrand.MidGray
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = AnthropicBrand.CardDark,
                    border = BorderStroke(1.dp, AnthropicBrand.BorderDark),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            stopAudioPreview()
                            onDismiss()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AnthropicBrand.MidGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs (Shazam Live Recognition vs Movie Soundtracks)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AnthropicBrand.CardDark,
                contentColor = AnthropicBrand.Coral,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AnthropicBrand.Coral,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 0) AnthropicBrand.Coral else AnthropicBrand.MidGray
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Shazam Audio",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) AnthropicBrand.Light else AnthropicBrand.MidGray
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 1) AnthropicBrand.Coral else AnthropicBrand.MidGray
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Movie Soundtracks",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) AnthropicBrand.Light else AnthropicBrand.MidGray
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    // Shazam Real-Time Recognition View
                    ShazamLiveRecognitionView(
                        state = shazamState,
                        hasVideoUri = videoUri != null,
                        isBoostEnabled = isBoostEnabled,
                        onToggleBoost = { isBoostEnabled = it },
                        playingPreviewUrl = playingPreviewUrl,
                        isPreviewPlaying = isPreviewPlaying,
                        previewPositionMs = previewCurrentPositionMs,
                        previewDurationMs = previewDurationMs,
                        previewTrackInfo = previewTrackInfo,
                        onPlayPreview = { url, trackTitle, artist, coverUrl ->
                            playAudioPreview(url, trackTitle, artist, coverUrl)
                        },
                        onPauseResumePreview = { pauseResumeAudioPreview() },
                        onStopPreview = { stopAudioPreview() },
                        onSeekPreview = { targetPos -> seekAudioPreview(targetPos) },
                        onRecognizeVideoScene = {
                            if (videoUri != null) {
                                viewModel.startVideoAudioShazamRecognition(
                                    context = context,
                                    videoUri = videoUri,
                                    positionMs = currentPositionMs,
                                    boostMode = isBoostEnabled
                                )
                            }
                        },
                        onStartMicListening = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onReset = {
                            stopAudioPreview()
                            viewModel.resetShazamState()
                        }
                    )
                }
                1 -> {
                    // Movie Soundtracks List View
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search movie soundtrack or song...", color = AnthropicBrand.MidGray) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = AnthropicBrand.MidGray)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.loadSoundtracks(searchQuery) }) {
                                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search", tint = AnthropicBrand.Coral)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.loadSoundtracks(searchQuery) }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AnthropicBrand.Coral,
                            unfocusedBorderColor = AnthropicBrand.BorderDark,
                            focusedTextColor = AnthropicBrand.Light,
                            unfocusedTextColor = AnthropicBrand.Light,
                            focusedContainerColor = AnthropicBrand.CardDark,
                            unfocusedContainerColor = AnthropicBrand.CardDark
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    when (val s = soundtrackState) {
                        is SoundtrackUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(color = AnthropicBrand.Coral)
                                    Text(
                                        text = "Finding official soundtracks and songs...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AnthropicBrand.MidGray
                                    )
                                }
                            }
                        }
                        is SoundtrackUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = s.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AnthropicBrand.MidGray,
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.loadSoundtracks(searchQuery) },
                                        border = BorderStroke(1.dp, AnthropicBrand.BorderDark)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = AnthropicBrand.Light)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Try Again", color = AnthropicBrand.Light)
                                    }
                                }
                            }
                        }
                        is SoundtrackUiState.Success -> {
                            val distinctTracks = remember(s.tracks) { s.tracks.distinctBy { it.id } }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(distinctTracks, key = { index, track -> "${track.id}_$index" }) { _, track ->
                                    SoundtrackItemRow(
                                        track = track,
                                        isPlaying = playingPreviewUrl == track.previewUrl && isPreviewPlaying,
                                        onPlayClick = {
                                            track.previewUrl?.let { url ->
                                                playAudioPreview(url, track.title, track.artist, track.coverUrl)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        SoundtrackUiState.Idle -> {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShazamLiveRecognitionView(
    state: ShazamRecognitionUiState,
    hasVideoUri: Boolean,
    isBoostEnabled: Boolean,
    onToggleBoost: (Boolean) -> Unit,
    playingPreviewUrl: String?,
    isPreviewPlaying: Boolean,
    previewPositionMs: Long,
    previewDurationMs: Long,
    previewTrackInfo: ShazamPreviewInfo?,
    onPlayPreview: (String, String, String, String?) -> Unit,
    onPauseResumePreview: () -> Unit,
    onStopPreview: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onRecognizeVideoScene: () -> Unit,
    onStartMicListening: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (state is ShazamRecognitionUiState.Success) Arrangement.Top else Arrangement.Center
    ) {
        when (state) {
            is ShazamRecognitionUiState.Idle -> {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(AnthropicBrand.Coral, Color(0xFFB85D3D))
                            )
                        )
                        .clickable {
                            if (hasVideoUri) onRecognizeVideoScene() else onStartMicListening()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_soundtrack_custom),
                        contentDescription = "Tap to Shazam",
                        tint = AnthropicBrand.Light,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Identify Playing Music",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AnthropicBrand.Light
                )
                Text(
                    text = "Bit-for-bit digital audio extraction at current playback position",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnthropicBrand.MidGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // High-Gain Audio Boost Toggle
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AnthropicBrand.CardDark),
                    border = BorderStroke(1.dp, AnthropicBrand.BorderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isBoostEnabled) AnthropicBrand.Coral.copy(alpha = 0.2f)
                                        else AnthropicBrand.BorderDark
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = if (isBoostEnabled) AnthropicBrand.Coral else AnthropicBrand.MidGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Dynamic Audio Boost (+18dB AGC)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AnthropicBrand.Light
                                )
                                Text(
                                    text = "Amplifies quiet background music & isolates speech",
                                    fontSize = 11.sp,
                                    color = AnthropicBrand.MidGray
                                )
                            }
                        }

                        Switch(
                            checked = isBoostEnabled,
                            onCheckedChange = onToggleBoost,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AnthropicBrand.Light,
                                checkedTrackColor = AnthropicBrand.Coral,
                                uncheckedThumbColor = AnthropicBrand.MidGray,
                                uncheckedTrackColor = AnthropicBrand.ButtonDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRecognizeVideoScene,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AnthropicBrand.Coral)
                    ) {
                        Icon(imageVector = Icons.Filled.Bolt, contentDescription = null, tint = AnthropicBrand.Light)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shazam Current Scene", color = AnthropicBrand.Light, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onStartMicListening,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AnthropicBrand.BorderDark)
                    ) {
                        Icon(imageVector = Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = AnthropicBrand.Light)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mic Listen", color = AnthropicBrand.Light)
                    }
                }
            }

            is ShazamRecognitionUiState.Listening -> {
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(AnthropicBrand.Coral.copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(AnthropicBrand.Coral, Color(0xFFB85D3D))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = "Listening",
                            tint = AnthropicBrand.Light,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Listening to audio...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AnthropicBrand.Light
                )
                Text(
                    text = "Sampling audio fingerprint with ShazamKit Engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnthropicBrand.MidGray,
                    textAlign = TextAlign.Center
                )
            }

            is ShazamRecognitionUiState.Analyzing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(46.dp),
                    color = AnthropicBrand.Coral,
                    strokeWidth = 3.5.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Extracting Video Audio & Querying Shazam...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AnthropicBrand.Light
                )
                Text(
                    text = "Normalized with dynamic gain boost • Matching 100M+ global music catalog",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnthropicBrand.MidGray,
                    textAlign = TextAlign.Center
                )
            }

            is ShazamRecognitionUiState.Success -> {
                val track = state.track
                var showMoreMenu by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Large Hero Album Art with Circular Share Button
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(AnthropicBrand.CardDark),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        if (!track.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = track.coverUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(AnthropicBrand.CardDark, AnthropicBrand.ButtonDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Audiotrack,
                                    contentDescription = null,
                                    tint = AnthropicBrand.Coral,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        // Floating Circular Share Button in bottom right corner
                        Surface(
                            shape = CircleShape,
                            color = Color(0xCC141413),
                            border = BorderStroke(1.dp, Color(0x40FAF9F5)),
                            modifier = Modifier
                                .padding(10.dp)
                                .size(36.dp)
                                .clickable {
                                    val shareUrl = track.shareUrl ?: track.appleMusicUrl ?: "https://www.shazam.com"
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Check out \"${track.title}\" by ${track.subtitle}: $shareUrl"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Track"))
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    tint = AnthropicBrand.Light,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Song Title & Artist Heading
                    Text(
                        text = track.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AnthropicBrand.Light,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val searchUrl = "https://www.google.com/search?q=${Uri.encode("${track.subtitle} artist")}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)))
                        }
                    ) {
                        Text(
                            text = track.subtitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AnthropicBrand.MidGray
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = AnthropicBrand.MidGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Metadata line (Shazam count • Genre • Year)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_soundtrack_custom),
                            contentDescription = null,
                            tint = AnthropicBrand.Coral,
                            modifier = Modifier.size(13.dp)
                        )
                        val shazamCountText = track.shazamCount ?: "94,292"
                        val genreText = track.genre ?: "Soundtrack"
                        val yearText = track.releaseDate?.take(4) ?: "2024"
                        Text(
                            text = "$shazamCountText Shazams • $genreText • $yearText",
                            fontSize = 12.sp,
                            color = AnthropicBrand.MidGray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Action Bar: Open in Apple Music pill, 3-dots, Circular Play/Pause Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Open in Apple Music Button
                        Surface(
                            onClick = {
                                val url = track.appleMusicUrl ?: track.spotifyUrl ?: track.youtubeSearchUrl
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            shape = RoundedCornerShape(26.dp),
                            color = AnthropicBrand.CardDark,
                            border = BorderStroke(1.dp, AnthropicBrand.BorderDark),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFC3C44)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Audiotrack,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Open in Apple Music",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AnthropicBrand.Light
                                    )
                                    Text(
                                        text = "Get up to 1 month free",
                                        fontSize = 10.sp,
                                        color = AnthropicBrand.MidGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Three-dots overflow button with dropdown menu
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = AnthropicBrand.CardDark,
                                border = BorderStroke(1.dp, AnthropicBrand.BorderDark),
                                modifier = Modifier
                                    .size(46.dp)
                                    .clickable { showMoreMenu = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = "More Options",
                                        tint = AnthropicBrand.Light,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open in YouTube") },
                                    onClick = {
                                        showMoreMenu = false
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(track.youtubeSearchUrl)))
                                    }
                                )
                                if (track.spotifyUrl != null) {
                                    DropdownMenuItem(
                                        text = { Text("Open in Spotify") },
                                        onClick = {
                                            showMoreMenu = false
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(track.spotifyUrl)))
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Copy Song Name") },
                                    onClick = {
                                        showMoreMenu = false
                                        clipboardManager.setText(AnnotatedString("${track.title} - ${track.subtitle}"))
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        // Circular 30s Preview Play/Pause button
                        val previewUrl = track.previewUrl
                        if (!previewUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            val isThisPreviewPlaying = playingPreviewUrl == previewUrl && isPreviewPlaying

                            Surface(
                                shape = CircleShape,
                                color = AnthropicBrand.Light,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        if (isThisPreviewPlaying) {
                                            onPauseResumePreview()
                                        } else {
                                            onPlayPreview(previewUrl, track.title, track.subtitle, track.coverUrl)
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isThisPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isThisPreviewPlaying) "Pause Preview" else "Play Preview",
                                        tint = AnthropicBrand.Dark,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. "Song Details" Section Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = AnthropicBrand.CardDark),
                        border = BorderStroke(1.dp, AnthropicBrand.BorderDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Song Details",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = AnthropicBrand.Light
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Summary paragraph
                            val albumName = track.album ?: "${track.title} - Single"
                            val releaseDateStr = track.releaseDate ?: "2024"
                            val labelStr = track.label ?: "Universal Music Group"
                            Text(
                                text = "${track.title} was released on $releaseDateStr by $labelStr as a part of the album $albumName.",
                                fontSize = 13.sp,
                                color = AnthropicBrand.MidGray,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Metadata rows
                            DetailRow(label = "Album", value = albumName)
                            Spacer(modifier = Modifier.height(6.dp))
                            DetailRow(label = "Release Date", value = releaseDateStr)
                            Spacer(modifier = Modifier.height(6.dp))
                            DetailRow(label = "Label", value = labelStr)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Audio Characteristic Sliders / Meters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "BPM",
                                        fontSize = 13.sp,
                                        color = AnthropicBrand.MidGray
                                    )
                                    Text(
                                        text = "${track.bpm}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AnthropicBrand.Light
                                    )
                                }

                                CharacteristicMeterBar(
                                    label = "Melodicness",
                                    value = track.melodicness,
                                    infoExplanation = "Ratio of melodic pitches to percussive elements",
                                    context = context
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                CharacteristicMeterBar(
                                    label = "Acousticness",
                                    value = track.acousticness,
                                    infoExplanation = "Confidence measure whether the track is acoustic",
                                    context = context,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                CharacteristicMeterBar(
                                    label = "Valence",
                                    value = track.valence,
                                    infoExplanation = "Musical positiveness conveyed by track (happy vs sad)",
                                    context = context,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                CharacteristicMeterBar(
                                    label = "Danceability",
                                    value = track.danceability,
                                    infoExplanation = "How suitable a track is for dancing based on tempo & beat strength",
                                    context = context,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                CharacteristicMeterBar(
                                    label = "Energy",
                                    value = track.energy,
                                    infoExplanation = "Perceptual measure of intensity, activity, and loudness",
                                    context = context,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // "More Details" button
                            Surface(
                                onClick = {
                                    val shareUrl = track.shareUrl ?: track.appleMusicUrl ?: "https://www.shazam.com"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl)))
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = AnthropicBrand.ButtonDark,
                                border = BorderStroke(1.dp, AnthropicBrand.BorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "More Details",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AnthropicBrand.Light
                                    )
                                }
                            }
                        }
                    }

                    // Lyrics Preview (if available)
                    if (track.lyrics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = AnthropicBrand.CardDark),
                            border = BorderStroke(1.dp, AnthropicBrand.BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "LYRICS PREVIEW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AnthropicBrand.Coral
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = track.lyrics.take(3).joinToString("\n"),
                                    fontSize = 13.sp,
                                    color = AnthropicBrand.Light,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // "Shazam Another Scene" Button
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AnthropicBrand.BorderDark)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AnthropicBrand.Light
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shazam Another Scene", color = AnthropicBrand.Light)
                    }
                }
            }

            is ShazamRecognitionUiState.Error -> {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = AnthropicBrand.Coral,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "No Song Detected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AnthropicBrand.Light
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = AnthropicBrand.MidGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRecognizeVideoScene,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AnthropicBrand.Coral)
                    ) {
                        Icon(imageVector = Icons.Filled.Bolt, contentDescription = null, tint = AnthropicBrand.Light)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Re-scan with High Boost (+18dB)", color = AnthropicBrand.Light, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onStartMicListening,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AnthropicBrand.BorderDark)
                    ) {
                        Icon(imageVector = Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = AnthropicBrand.Light)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Microphone", color = AnthropicBrand.Light)
                    }
                }
            }
        }
    }

    // 5. Floating Bottom "Now Previewing" Audio Player Bar
    AnimatedVisibility(visible = playingPreviewUrl != null && previewTrackInfo != null) {
        previewTrackInfo?.let { info ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = AnthropicBrand.Light,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (!info.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = info.coverUrl,
                                    contentDescription = info.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF262522)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Audiotrack,
                                        contentDescription = null,
                                        tint = AnthropicBrand.Coral,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Now Previewing",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF706E66)
                                )
                                Text(
                                    text = info.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AnthropicBrand.Dark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = info.artist,
                                    fontSize = 11.sp,
                                    color = Color(0xFF706E66),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Apple Music Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AnthropicBrand.Dark,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = "\uF8FFMusic",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AnthropicBrand.Light,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Play/Pause Control
                        IconButton(
                            onClick = onPauseResumePreview,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPreviewPlaying) "Pause" else "Play",
                                tint = AnthropicBrand.Dark,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Close Button
                        IconButton(
                            onClick = onStopPreview,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close Preview",
                                tint = Color(0xFF706E66),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress indicator with time labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatMillis(previewPositionMs),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF706E66)
                        )

                        val progressFraction = if (previewDurationMs > 0) {
                            (previewPositionMs.toFloat() / previewDurationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        var barWidthPx by remember { mutableFloatStateOf(1f) }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .weight(1f)
                                .height(14.dp)
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .onSizeChanged { barWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                                .pointerInput(previewDurationMs) {
                                    detectTapGestures { offset ->
                                        if (barWidthPx > 0f && previewDurationMs > 0L) {
                                            val fraction = (offset.x / barWidthPx).coerceIn(0f, 1f)
                                            val targetMs = (fraction * previewDurationMs).toLong()
                                            onSeekPreview(targetMs)
                                        }
                                    }
                                },
                            color = AnthropicBrand.Coral,
                            trackColor = Color(0xFFE8E6DC)
                        )

                        Text(
                            text = formatMillis(previewDurationMs),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF706E66)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AnthropicBrand.MidGray
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AnthropicBrand.Light,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CharacteristicMeterBar(
    label: String,
    value: Float,
    infoExplanation: String,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = AnthropicBrand.MidGray
            )
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "$label info",
                tint = AnthropicBrand.MidGray,
                modifier = Modifier
                    .size(13.dp)
                    .clickable {
                        Toast.makeText(context, "$label: $infoExplanation", Toast.LENGTH_SHORT).show()
                    }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AnthropicBrand.ButtonDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = value.coerceIn(0.05f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(AnthropicBrand.Coral, AnthropicBrand.SkyBlue)
                        )
                    )
            )
        }
    }
}

@Composable
private fun SoundtrackItemRow(
    track: SoundtrackItem,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    val preview = track.previewUrl
    val cover = track.coverUrl
    val webUrl = track.spotifyUrl ?: track.youtubeUrl

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AnthropicBrand.CardDark),
        border = if (isPlaying) BorderStroke(1.dp, AnthropicBrand.Coral) else BorderStroke(1.dp, AnthropicBrand.BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album Art
            if (!cover.isNullOrBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AnthropicBrand.Coral.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Audiotrack,
                        contentDescription = null,
                        tint = AnthropicBrand.Coral,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AnthropicBrand.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = AnthropicBrand.MidGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val album = track.album
                if (!album.isNullOrBlank()) {
                    Text(
                        text = album,
                        style = MaterialTheme.typography.labelSmall,
                        color = AnthropicBrand.MidGray.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Preview Play / Pause Button
            if (!preview.isNullOrBlank()) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) AnthropicBrand.Coral else AnthropicBrand.ButtonDark)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Preview" else "Play Preview",
                        tint = if (isPlaying) AnthropicBrand.Light else AnthropicBrand.Light,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Open in External / Web
            if (!webUrl.isNullOrBlank()) {
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = "Open",
                        tint = AnthropicBrand.MidGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
