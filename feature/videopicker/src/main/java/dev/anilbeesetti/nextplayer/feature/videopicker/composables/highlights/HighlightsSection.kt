package dev.anilbeesetti.nextplayer.feature.videopicker.composables.highlights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import dev.anilbeesetti.nextplayer.core.model.CelebritySpotlight
import dev.anilbeesetti.nextplayer.core.model.EntertainmentHighlight

/**
 * Reusable Highlights & Entertainment Buzz section designed with authentic JioHotstar cinema dark textures.
 * Displays celebrity buzz, trending stories, and opens an in-app detail modal on click without opening web browsers.
 */
@Composable
fun HighlightsSection(
    highlights: List<EntertainmentHighlight>,
    celebrities: List<CelebritySpotlight>,
    modifier: Modifier = Modifier,
    onHighlightClick: ((EntertainmentHighlight) -> Unit)? = null
) {
    var selectedHighlight by remember { mutableStateOf<EntertainmentHighlight?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF07090E))
            .padding(vertical = 12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = HighlightIcons.SparkleStar,
                    contentDescription = null,
                    tint = Color(0xFF00ED82),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Highlights & Buzz",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = "Trending cinema scoops & celebrity spotlights",
                        color = Color(0xFF828A9E),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Celebrity Spotlight (Story-style circular avatars)
        if (celebrities.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(celebrities, key = { it.id }) { celeb ->
                    CelebrityStoryItem(
                        celebrity = celeb,
                        onClick = {
                            // Find matching highlight or show first highlight for this celeb
                            val match = highlights.find { it.actorName?.contains(celeb.name, ignoreCase = true) == true }
                                ?: highlights.firstOrNull()
                            if (match != null) {
                                if (onHighlightClick != null) onHighlightClick(match)
                                else selectedHighlight = match
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // 2. Horizontal Featured Highlights Cards (16:9 Cinema Cards)
        if (highlights.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(highlights.take(8), key = { it.id }) { item ->
                    HighlightCinemaCard(
                        highlight = item,
                        onClick = {
                            if (onHighlightClick != null) onHighlightClick(item)
                            else selectedHighlight = item
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Compact Feed Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                highlights.drop(8).take(4).forEach { item ->
                    HighlightCompactRowCard(
                        highlight = item,
                        onClick = {
                            if (onHighlightClick != null) onHighlightClick(item)
                            else selectedHighlight = item
                        }
                    )
                }
            }
        }
    }

    // In-app Popup Modal
    selectedHighlight?.let { item ->
        HighlightDetailModal(
            highlight = item,
            onDismiss = { selectedHighlight = null }
        )
    }
}

/**
 * Story-style circular avatar item for top celebrities.
 */
@Composable
fun CelebrityStoryItem(
    celebrity: CelebritySpotlight,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(Color(0xFF00ED82), Color(0xFF0075FF), Color(0xFF00ED82))
                    ),
                    shape = CircleShape
                )
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!celebrity.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = celebrity.avatarUrl,
                    contentDescription = celebrity.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF131724)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = celebrity.name.take(1),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = celebrity.name,
            color = Color(0xFFBAC5D6),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 16:9 Cinema Card matching JioHotstar's highlight style.
 */
@Composable
fun HighlightCinemaCard(
    highlight: EntertainmentHighlight,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D121F))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column {
            // Thumbnail with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9.5f)
            ) {
                if (!highlight.bannerUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = highlight.bannerUrl,
                        contentDescription = highlight.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF131724))
                    )
                }

                // Scrim overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xDD0D121F))
                            )
                        )
                )

                // Category Badge (top left)
                val badgeColor = Color(highlight.category.badgeColorHex)
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xD907090E))
                        .border(0.8.dp, badgeColor.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = highlight.category.label,
                        color = badgeColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Text Info below
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = highlight.title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = highlight.formattedTime,
                        color = Color(0xFF828A9E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = highlight.viewsCount,
                        color = Color(0xFFBAC5D6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Compact horizontal row card for vertical list feeds.
 */
@Composable
fun HighlightCompactRowCard(
    highlight: EntertainmentHighlight,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D121F))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            if (!highlight.bannerUrl.isNullOrBlank()) {
                AsyncImage(
                    model = highlight.bannerUrl,
                    contentDescription = highlight.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF131724))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val badgeColor = Color(highlight.category.badgeColorHex)
            Text(
                text = highlight.category.label,
                color = badgeColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = highlight.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${highlight.formattedTime} • ${highlight.viewsCount}",
                color = Color(0xFF828A9E),
                fontSize = 11.sp
            )
        }
    }
}
