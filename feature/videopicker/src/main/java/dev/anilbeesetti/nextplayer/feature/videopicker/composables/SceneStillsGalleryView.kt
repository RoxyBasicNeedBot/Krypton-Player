package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.anilbeesetti.nextplayer.core.data.network.models.FancapsScrubState
import dev.anilbeesetti.nextplayer.core.data.network.models.MediaStill
import dev.anilbeesetti.nextplayer.core.data.network.models.StillSource
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.NextColors
import dev.anilbeesetti.nextplayer.core.ui.components.AnimatedCinemaIcon
import dev.anilbeesetti.nextplayer.core.ui.components.AnimatedStopwatchIcon

@Composable
fun SceneStillsGalleryView(
    stills: List<MediaStill>,
    fancapsScrubState: FancapsScrubState,
    title: String,
    isLoading: Boolean,
    onScrubChanged: (Float) -> Unit,
    onLoadFancapsPage: (Int) -> Unit,
    onSetThumbnail: (MediaStill) -> Unit,
    onSaveFrame: (MediaStill) -> Unit,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(StillSource.ALL) }
    var activeLightboxIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    // Filter stills based on tab
    val displayedStills = remember(stills, selectedTab, fancapsScrubState.loadedFrames) {
        when (selectedTab) {
            StillSource.ALL -> stills
            StillSource.TMDB -> stills.filter { it.source == StillSource.TMDB }
            StillSource.IMDB -> stills.filter { it.source == StillSource.IMDB }
            StillSource.FANART -> stills.filter { it.source == StillSource.FANART }
            StillSource.FANCAPS -> {
                if (fancapsScrubState.loadedFrames.isNotEmpty()) {
                    fancapsScrubState.loadedFrames
                } else {
                    stills.filter { it.source == StillSource.FANCAPS }
                }
            }
            StillSource.DUCKDUCKGO -> stills.filter { it.source == StillSource.DUCKDUCKGO }
        }
    }

    val totalStillsCount = remember(stills) { stills.count { it.source != StillSource.FANCAPS } }
    val fancapsCount = remember(fancapsScrubState.totalEstimatedFrames, stills) {
        if (fancapsScrubState.totalEstimatedFrames > 0) {
            "${fancapsScrubState.totalEstimatedFrames}+ Frames"
        } else {
            val count = stills.count { it.source == StillSource.FANCAPS }
            if (count > 0) "$count Frames" else "Frames Archive"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedCinemaIcon(size = 22.dp, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Scene Stills & Frames",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Total Stills Count Badge
                    if (totalStillsCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$totalStillsCount Stills",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Sequential Frames Count Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF8E24AA).copy(alpha = 0.35f))
                        .border(1.dp, Color(0xFFBA68C8).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = fancapsCount,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE1BEE7)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Source Filter Chips / Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val tabs = listOf(
                StillSource.ALL,
                StillSource.TMDB,
                StillSource.IMDB,
                StillSource.FANART,
                StillSource.FANCAPS
            )
            itemsIndexed(tabs) { _, tab ->
                val isSelected = selectedTab == tab
                val count = when (tab) {
                    StillSource.ALL -> stills.size
                    StillSource.TMDB -> stills.count { it.source == StillSource.TMDB }
                    StillSource.IMDB -> stills.count { it.source == StillSource.IMDB }
                    StillSource.FANART -> stills.count { it.source == StillSource.FANART }
                    StillSource.FANCAPS -> fancapsScrubState.loadedFrames.size.takeIf { it > 0 } ?: stills.count { it.source == StillSource.FANCAPS }
                    StillSource.DUCKDUCKGO -> stills.count { it.source == StillSource.DUCKDUCKGO }
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    label = {
                        Text(
                            text = if (count > 0 && tab != StillSource.ALL) "${tab.displayName} ($count)" else tab.displayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        labelColor = Color.LightGray,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.White.copy(alpha = 0.15f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Fancaps Interactive Scrubber & Timeline controls (When Frame-by-Frame tab is selected)
        AnimatedVisibility(
            visible = selectedTab == StillSource.FANCAPS,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1428).copy(alpha = 0.8f))
                    .border(1.dp, Color(0xFFAB47BC).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                // Scrubber Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedStopwatchIcon(size = 18.dp, tint = Color(0xFFE1BEE7))
                        Text(
                            text = "Scene Timeline Scrubber",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE1BEE7)
                        )
                    }
                    Text(
                        text = "Page ${fancapsScrubState.currentPage} of ${fancapsScrubState.totalPages}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Timeline Slider
                var sliderPos by remember(fancapsScrubState.scrubPercentage) { mutableFloatStateOf(fancapsScrubState.scrubPercentage) }
                Slider(
                    value = sliderPos,
                    onValueChange = { sliderPos = it },
                    onValueChangeFinished = { onScrubChanged(sliderPos) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFE040FB),
                        activeTrackColor = Color(0xFFAB47BC),
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Scene Jump Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        "Intro" to 0.0f,
                        "Act 1" to 0.25f,
                        "Midpoint" to 0.50f,
                        "Climax" to 0.75f,
                        "Ending" to 1.0f
                    ).forEach { (label, percent) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (kotlin.math.abs(sliderPos - percent) < 0.12f) Color(0xFFE040FB) else Color.Gray,
                            fontWeight = if (kotlin.math.abs(sliderPos - percent) < 0.12f) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    sliderPos = percent
                                    onScrubChanged(percent)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pagination Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (fancapsScrubState.currentPage > 1) {
                                onLoadFancapsPage(fancapsScrubState.currentPage - 1)
                            }
                        },
                        enabled = fancapsScrubState.currentPage > 1 && !fancapsScrubState.isLoading,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Page",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Prev Page", fontSize = 11.sp)
                    }

                    if (fancapsScrubState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFE040FB),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "${fancapsScrubState.loadedFrames.size} frames loaded",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            if (fancapsScrubState.currentPage < fancapsScrubState.totalPages) {
                                onLoadFancapsPage(fancapsScrubState.currentPage + 1)
                            }
                        },
                        enabled = fancapsScrubState.currentPage < fancapsScrubState.totalPages && !fancapsScrubState.isLoading,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Next Page", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Page",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stills Gallery Cards Row
        if (isLoading && displayedStills.isEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                }
            }
        } else if (displayedStills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No scene stills available in this category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(displayedStills) { index, still ->
                    StillCard(
                        still = still,
                        index = index,
                        onClick = { activeLightboxIndex = index }
                    )
                }
            }
        }
    }

    // Fullscreen Interactive Lightbox
    activeLightboxIndex?.let { index ->
        SceneLightboxDialog(
            stills = displayedStills,
            initialIndex = index,
            title = title,
            onSetThumbnail = onSetThumbnail,
            onSaveFrame = onSaveFrame,
            onDismiss = { activeLightboxIndex = null }
        )
    }
}

@Composable
private fun StillCard(
    still: MediaStill,
    index: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(still.thumbnailUrl) {
        val builder = ImageRequest.Builder(context)
            .data(still.thumbnailUrl)
            .crossfade(true)
        if (still.thumbnailUrl.contains("fancaps.net")) {
            builder.httpHeaders(
                NetworkHeaders.Builder()
                    .set("Referer", "https://fancaps.net/")
                    .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()
            )
        } else if (dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.isWorkerUrl(still.thumbnailUrl)) {
            builder.httpHeaders(
                NetworkHeaders.Builder()
                    .set("X-Arch-Client-Key", dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.CLIENT_KEY)
                    .build()
            )
        }
        builder.build()
    }

    Card(
        modifier = Modifier
            .width(180.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = NextColors.cardColors(),
        border = NextColors.cardBorder()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = still.caption ?: "Still #${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )



            // Frame Number / Caption (bottom left)
            Text(
                text = still.caption ?: "#${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}
