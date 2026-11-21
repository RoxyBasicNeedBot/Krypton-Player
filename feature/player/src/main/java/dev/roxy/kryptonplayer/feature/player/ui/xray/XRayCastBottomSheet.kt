package dev.roxy.kryptonplayer.feature.player.ui.xray

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.data.network.models.TmdbCastMember
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.feature.player.PlayerViewModel
import dev.roxy.kryptonplayer.feature.player.XRayUiState
import dev.roxy.kryptonplayer.feature.player.extensions.formatted
import java.io.File
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XRayCastBottomSheet(
    player: Player? = null,
    viewModel: PlayerViewModel,
    videoTitle: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.xRayState.collectAsStateWithLifecycle()
    val initialCleanTitle = remember(videoTitle) {
        dev.roxy.kryptonplayer.core.common.Utils.cleanMediaTitle(videoTitle).first
    }
    var searchQuery by remember { mutableStateOf(initialCleanTitle) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = remember { listOf("Cast & Characters", "Media Information") }

    val context = LocalContext.current

    LaunchedEffect(videoTitle) {
        viewModel.loadXRayData(videoTitle)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xray_custom),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Amazon X-Ray Cinema",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Live Cast Guide & Media Specifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = NextIcons.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented Tab Row
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                tabs.forEachIndexed { index, title ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        icon = {
                            Icon(
                                imageVector = if (index == 0) Icons.Rounded.Movie else Icons.Rounded.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    ) {
                        Text(title, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (selectedTab == 0) {
                // TAB 0: Amazon X-Ray Cast & Character Guide
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search movie or show for X-Ray cast...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = NextIcons.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.loadXRayData(searchQuery) }) {
                                Icon(
                                    imageVector = NextIcons.Search,
                                    contentDescription = "Perform Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.loadXRayData(searchQuery)
                    }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                )

                Spacer(modifier = Modifier.height(14.dp))

                when (val s = state) {
                    is XRayUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                            )
                        }
                    }
                    is XRayUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = s.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Tip: Use the search bar above to look up by movie title.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                    is XRayUiState.Success -> {
                        // Movie / Show Spotlight Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                val posterPath = s.posterPath
                                val posterUrl = when {
                                    posterPath.isNullOrBlank() -> null
                                    posterPath.startsWith("http://") || posterPath.startsWith("https://") -> posterPath
                                    else -> "https://image.tmdb.org/t/p/w185$posterPath"
                                }

                                if (posterUrl != null) {
                                    AsyncImage(
                                        model = posterUrl,
                                        contentDescription = s.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(64.dp)
                                            .height(96.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = s.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (s.rating > 0.0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = NextIcons.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFB300),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "%.1f".format(s.rating),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFFFB300),
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp),
                                    ) {
                                        if (!s.releaseYear.isNullOrBlank()) {
                                            Text(
                                                text = s.releaseYear,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (s.genres.isNotEmpty()) {
                                            Text(
                                                text = "•  ${s.genres.take(2).joinToString(", ")}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    if (!s.director.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Directed by ${s.director}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }

                                    if (!s.overview.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = s.overview,
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (s.genres.contains("Music Video") || s.genres.contains("Song")) "FEATURED ARTISTS & SINGERS (${s.cast.size})" else "CAST & CHARACTERS (${s.cast.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(s.cast) { member ->
                                XRayCastCard(member)
                            }
                        }
                    }
                    is XRayUiState.Idle -> {}
                }
            } else {
                // TAB 1: Nosved-style Technical Media Specifications
                MediaSpecificationsContent(
                    player = player,
                    videoTitle = videoTitle,
                    context = context,
                )
            }
        }
    }
}

@Composable
private fun MediaSpecificationsContent(
    player: Player?,
    videoTitle: String,
    context: Context,
) {
    val videoFormat = player?.currentTracks?.groups
        ?.firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
        ?.getTrackFormat(0)
        ?: player?.currentTracks?.groups
            ?.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }
            ?.getTrackFormat(0)

    val audioFormat = player?.currentTracks?.groups
        ?.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
        ?.getTrackFormat(0)
        ?: player?.currentTracks?.groups
            ?.firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
            ?.getTrackFormat(0)

    val durationMs = if (player != null && player.duration != C.TIME_UNSET && player.duration > 0L) player.duration else null
    val durationStr = durationMs?.milliseconds?.formatted() ?: "Live / Unknown"

    val frameW = videoFormat?.width?.takeIf { it > 0 } ?: player?.videoSize?.width?.takeIf { it > 0 }
    val frameH = videoFormat?.height?.takeIf { it > 0 } ?: player?.videoSize?.height?.takeIf { it > 0 }

    val resolutionStr = if (frameW != null && frameH != null && frameW > 0 && frameH > 0) {
        "$frameW x $frameH"
    } else "Unknown"

    val resolutionBadge = if (frameH != null && frameH > 0) {
        when {
            frameH >= 2160 -> "4K UHD"
            frameH >= 1440 -> "2K QHD"
            frameH >= 1080 -> "1080p FHD"
            frameH >= 720 -> "720p HD"
            else -> "${frameH}p"
        }
    } else "HD"

    val fpsStr = if (videoFormat != null && videoFormat.frameRate > 0) {
        if (videoFormat.frameRate % 1f == 0f) "${videoFormat.frameRate.toInt()} fps"
        else String.format(Locale.US, "%.2f fps", videoFormat.frameRate)
    } else "Unknown"

    val videoMimeStr = videoFormat?.sampleMimeType ?: "Unknown"
    val videoCodecStr = videoFormat?.codecs ?: videoFormat?.sampleMimeType?.substringAfter("/")?.uppercase() ?: "Unknown"

    val videoBitrateStr = if (videoFormat != null && videoFormat.bitrate > 0) {
        if (videoFormat.bitrate >= 1_000_000) String.format(Locale.US, "%.2f Mbps", videoFormat.bitrate / 1_000_000f)
        else "${videoFormat.bitrate / 1000} kbps"
    } else "Unknown"

    val audioMimeStr = audioFormat?.sampleMimeType ?: "Unknown"
    val audioCodecStr = audioFormat?.codecs ?: audioFormat?.sampleMimeType?.substringAfter("/")?.uppercase() ?: "Unknown"

    val audioChannelsStr = when (audioFormat?.channelCount) {
        1 -> "Mono (1.0)"
        2 -> "Stereo (2.0)"
        6 -> "Surround (5.1)"
        8 -> "Surround (7.1)"
        null -> "Unknown"
        else -> "${audioFormat.channelCount} Channels"
    }

    val sampleRateStr = if (audioFormat != null && audioFormat.sampleRate > 0) "${audioFormat.sampleRate} Hz" else "Unknown"
    val audioLanguageStr = audioFormat?.language?.ifBlank { "Default" } ?: "Default"

    val uri = player?.currentMediaItem?.localConfiguration?.uri
    val uriStr = uri?.toString() ?: ""
    val fileSizeStr = remember(uri) {
        if (uri == null) return@remember "Unknown"
        try {
            if (uri.scheme == "file") {
                uri.path?.let { File(it).takeIf { f -> f.exists() }?.length() }?.let { formatFileSize(it) } ?: "Unknown"
            } else if (uri.scheme == "content") {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    formatFileSize(pfd.statSize)
                } ?: "Unknown"
            } else if (uri.scheme?.startsWith("http") == true) {
                "Network Stream"
            } else {
                "Unknown"
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Hero Card with Badges
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = videoTitle.substringBeforeLast("."),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(resolutionBadge, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text(videoCodecStr, style = MaterialTheme.typography.labelSmall) },
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text(audioCodecStr, style = MaterialTheme.typography.labelSmall) },
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text(durationStr, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        // General Information Card
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "General",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.Title,
                    label = "Filename",
                    value = videoTitle,
                    onClick = { copyToClipboard("Filename", videoTitle) },
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.Schedule,
                    label = "Duration",
                    value = durationStr,
                    onClick = { copyToClipboard("Duration", durationStr) },
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.Storage,
                    label = "File Size",
                    value = fileSizeStr,
                    onClick = { copyToClipboard("File Size", fileSizeStr) },
                )
                if (uriStr.isNotBlank()) {
                    InfoPropertyRow(
                        icon = Icons.Rounded.Folder,
                        label = "URI / Location",
                        value = uriStr,
                        onClick = { copyToClipboard("Location", uriStr) },
                    )
                }
            }
        }

        // Video Specifications Card
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Video Track",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.AspectRatio,
                    label = "Resolution",
                    value = resolutionStr,
                    onClick = { copyToClipboard("Resolution", resolutionStr) },
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.Speed,
                    label = "Frame Rate",
                    value = fpsStr,
                    onClick = { copyToClipboard("Frame Rate", fpsStr) },
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.Movie,
                    label = "MIME / Codec",
                    value = "$videoMimeStr ($videoCodecStr)",
                    onClick = { copyToClipboard("Video Codec", "$videoMimeStr ($videoCodecStr)") },
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.Speed,
                    label = "Bitrate",
                    value = videoBitrateStr,
                    onClick = { copyToClipboard("Bitrate", videoBitrateStr) },
                )
            }
        }

        // Audio Specifications Card
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Audio Track",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.Audiotrack,
                    label = "Format / Codec",
                    value = "$audioMimeStr ($audioCodecStr)",
                    onClick = { copyToClipboard("Audio Codec", "$audioMimeStr ($audioCodecStr)") },
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.GraphicEq,
                    label = "Channels",
                    value = audioChannelsStr,
                    onClick = { copyToClipboard("Channels", audioChannelsStr) },
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.Speed,
                    label = "Sample Rate",
                    value = sampleRateStr,
                    onClick = { copyToClipboard("Sample Rate", sampleRateStr) },
                )
                InfoPropertyRow(
                    icon = Icons.Rounded.Info,
                    label = "Language",
                    value = audioLanguageStr,
                    onClick = { copyToClipboard("Audio Language", audioLanguageStr) },
                )
            }
        }

        // Subtitle & Stream Summary Card
        val subtitleTracks = remember(player?.currentTracks) {
            player?.currentTracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT } ?: emptyList()
        }
        if (subtitleTracks.isNotEmpty()) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Subtitles (${subtitleTracks.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    subtitleTracks.forEachIndexed { idx, trackGroup ->
                        val trackFormat = (0 until trackGroup.length).map { trackGroup.getTrackFormat(it) }.firstOrNull()
                        val subName = trackFormat?.label ?: trackFormat?.language ?: "Track ${idx + 1}"
                        val subMime = trackFormat?.sampleMimeType ?: "text"
                        InfoPropertyRow(
                            icon = Icons.Rounded.Subtitles,
                            label = "Subtitle ${idx + 1}",
                            value = "$subName ($subMime)",
                            onClick = { copyToClipboard("Subtitle", "$subName ($subMime)") },
                        )
                    }
                }
            }
        }

        // Copy All Specs Button
        FilledTonalButton(
            onClick = {
                val fullSpecs = buildString {
                    appendLine("=== MEDIA SPECIFICATIONS ===")
                    appendLine("Title: $videoTitle")
                    appendLine("Duration: $durationStr")
                    appendLine("File Size: $fileSizeStr")
                    appendLine("Resolution: $resolutionStr")
                    appendLine("Frame Rate: $fpsStr")
                    appendLine("Video Codec: $videoMimeStr ($videoCodecStr)")
                    appendLine("Video Bitrate: $videoBitrateStr")
                    appendLine("Audio Codec: $audioMimeStr ($audioCodecStr)")
                    appendLine("Audio Channels: $audioChannelsStr")
                    appendLine("Sample Rate: $sampleRateStr")
                    appendLine("Language: $audioLanguageStr")
                    if (uriStr.isNotBlank()) appendLine("URI: $uriStr")
                }
                copyToClipboard("Full Media Specifications", fullSpecs)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy All Media Specifications")
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun InfoPropertyRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Unknown"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

@Composable
private fun XRayCastCard(member: TmdbCastMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val profilePath = member.profile_path
            val photoUrl = when {
                profilePath.isNullOrBlank() -> null
                profilePath.startsWith("http://") || profilePath.startsWith("https://") -> profilePath
                else -> "https://image.tmdb.org/t/p/w185$profilePath"
            }

            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = member.name.take(1),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val character = member.character
                if (!character.isNullOrBlank()) {
                    Text(
                        text = character,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
