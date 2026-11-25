package dev.roxy.kryptonplayer.feature.videopicker.composables.foryou

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.data.network.NetworkConfig
import dev.roxy.kryptonplayer.feature.videopicker.composables.highlights.HighlightIcons
import dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker.ExploreItem

enum class YourStuffFilter(val label: String) {
    ALL("All"),
    WATCHLIST("Watchlist"),
    KEEP_WATCHING("Keep watching"),
    LIBRARY("Library & Downloads")
}

/**
 * Signature Google TV "Your stuff" Experience built for Next Player.
 *
 * Implements:
 * - Google TV "Your stuff" hub architecture (Watchlist, Keep Watching, Library & Downloads).
 * - JioHotstar Cinema Dark (#07090E, glass acrylic cards, glowing neon accents).
 * - STRICTLY NO RENT / NO BUY FEATURES: everything is 100% free streaming and device media.
 */
@Composable
fun YourStuffSection(
    watchlistItems: List<ExploreItem>,
    onMediaClick: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onBrowseExplore: (() -> Unit)? = null
) {
    var activeFilter by remember { mutableStateOf(YourStuffFilter.ALL) }

    // Simulated / interactive local watchlist state
    val localWatchlist = remember(watchlistItems) {
        mutableStateListOf<ExploreItem>().apply {
            addAll(watchlistItems.take(8))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 1. Google TV "Your stuff" Sub-Pills (Filter Row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            YourStuffFilter.values().forEach { filter ->
                val isSelected = filter == activeFilter

                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF161F30) else Color(0xFF0C1019),
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "filter_pill_bg_${filter.name}"
                )

                val pillBorder by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF00ED82) else Color(0x22FFFFFF),
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "filter_pill_border_${filter.name}"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(pillBg)
                        .border(
                            width = if (isSelected) 1.dp else 0.6.dp,
                            brush = if (isSelected) {
                                Brush.horizontalGradient(listOf(Color(0xFF00ED82), Color(0xFF0075FF)))
                            } else {
                                Brush.linearGradient(listOf(pillBorder, pillBorder))
                            },
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable { activeFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        color = if (isSelected) Color.White else Color(0xFF9EABB8),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================================
        // SECTION 1: KEEP WATCHING (Continue Watching)
        // ==========================================
        if (activeFilter == YourStuffFilter.ALL || activeFilter == YourStuffFilter.KEEP_WATCHING) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                // Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Keep watching",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        )
                        Text(
                            text = "Pick up right where you left off",
                            color = Color(0xFF8897AA),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Landscape 16:9 In-Progress Video Cards
                val continueItems = watchlistItems.take(4)
                if (continueItems.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(continueItems) { item ->
                            val id = item.id
                            val isTv = item is ExploreItem.TvShowItem
                            val title = when (item) {
                                is ExploreItem.MovieItem -> item.title
                                is ExploreItem.TvShowItem -> item.name
                                else -> "Video"
                            }
                            val backdrop = when (item) {
                                is ExploreItem.MovieItem -> item.backdropPath ?: item.posterPath
                                is ExploreItem.TvShowItem -> item.backdropPath ?: item.posterPath
                                else -> null
                            }
                            val imageUrl = backdrop?.let { NetworkConfig.tmdbImageUrl("w780", it) }

                            Column(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable { onMediaClick(id, isTv) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9.5f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0D121F))
                                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                ) {
                                    if (imageUrl != null) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Dark gradient scrim
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0f to Color.Transparent,
                                                    0.6f to Color.Transparent,
                                                    1f to Color(0xCC07090E)
                                                )
                                            )
                                    )

                                    // Center Play Circular Icon
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .align(Alignment.Center)
                                            .clip(CircleShape)
                                            .background(Color(0xBB000000))
                                            .border(1.dp, Color(0x44FFFFFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Resume Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Playback Progress Bar at bottom of card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.5.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(Color(0x55000000))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.62f)
                                                .height(3.5.dp)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF00ED82), Color(0xFF0075FF))
                                                    )
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "42m remaining • 4K UHD",
                                    color = Color(0xFF00ED82),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 2: WATCHLIST
        // ==========================================
        if (activeFilter == YourStuffFilter.ALL || activeFilter == YourStuffFilter.WATCHLIST) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                // Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Watchlist",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF161F30))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${localWatchlist.size} saved",
                                    color = Color(0xFF00ED82),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Movies and shows saved to watch • Free stream",
                            color = Color(0xFF8897AA),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (localWatchlist.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(localWatchlist, key = { it.uniqueKey }) { item ->
                            val id = item.id
                            val isTv = item is ExploreItem.TvShowItem
                            val title = when (item) {
                                is ExploreItem.MovieItem -> item.title
                                is ExploreItem.TvShowItem -> item.name
                                else -> "Title"
                            }
                            val posterPath = when (item) {
                                is ExploreItem.MovieItem -> item.posterPath
                                is ExploreItem.TvShowItem -> item.posterPath
                                else -> null
                            }
                            val rating = when (item) {
                                is ExploreItem.MovieItem -> item.rating
                                is ExploreItem.TvShowItem -> item.rating
                                else -> 0.0
                            }
                            val posterUrl = posterPath?.let { NetworkConfig.tmdbImageUrl("w500", it) }

                            Column(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { onMediaClick(id, isTv) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0D121F))
                                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                ) {
                                    if (posterUrl != null) {
                                        AsyncImage(
                                            model = posterUrl,
                                            contentDescription = title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Top Right Watchlist Checkmark pill
                                    Box(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(Color(0xDD00ED82))
                                            .clickable {
                                                localWatchlist.remove(item)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Saved in Watchlist",
                                            tint = Color.Black,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    // Top Left FREE Stream Badge (NO RENT!)
                                    Box(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .align(Alignment.TopStart)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xDD000000))
                                            .border(0.5.dp, Color(0x44FFFFFF), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = "4K UHD",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Bottom Scrim
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0f to Color.Transparent,
                                                    0.6f to Color.Transparent,
                                                    1f to Color(0xDD07090E)
                                                )
                                            )
                                    )

                                    if (rating > 0.0) {
                                        val matchScore = (rating * 10).toInt().coerceIn(75, 99)
                                        Text(
                                            text = "$matchScore% Match",
                                            color = Color(0xFF00ED82),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = if (isTv) "Series • Free" else "Movie • Free",
                                    color = Color(0xFF8897AA),
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                } else {
                    // Empty Watchlist state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0D121F))
                            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = Color(0xFF0075FF),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your Watchlist is empty",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Save movies and shows from Explore to watch them anytime.",
                                color = Color(0xFF8897AA),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 3: YOUR LIBRARY & DOWNLOADS
        // (Replacing Google TV's Rentals/Purchases with Next Player Free Media)
        // ==========================================
        if (activeFilter == YourStuffFilter.ALL || activeFilter == YourStuffFilter.LIBRARY) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Section Header
                Text(
                    text = "Your Library & Downloads",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
                Text(
                    text = "Offline device videos & cloud downloads • Zero fees",
                    color = Color(0xFF8897AA),
                    fontSize = 11.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card 1: On This Device (Local Videos)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0E1626), Color(0xFF131D33))
                            )
                        )
                        .border(1.dp, Color(0x330075FF), RoundedCornerShape(14.dp))
                        .clickable { onBrowseExplore?.invoke() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x330075FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = Color(0xFF38B6FF),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Device Media & Local Folders",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Fast offline playback • libmpv engine",
                                    color = Color(0xFFBAC5D6),
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF38B6FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 2: Telegram Downloads & Cloud Media
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0E1A22), Color(0xFF12232E))
                            )
                        )
                        .border(1.dp, Color(0x3300ED82), RoundedCornerShape(14.dp))
                        .clickable { onBrowseExplore?.invoke() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3300ED82)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Send,
                                    contentDescription = null,
                                    tint = Color(0xFF00ED82),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Telegram Downloads & Saved Streams",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Direct streaming & background downloads",
                                    color = Color(0xFFBAC5D6),
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF00ED82),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
