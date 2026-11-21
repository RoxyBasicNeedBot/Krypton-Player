package dev.roxy.kryptonplayer.feature.videopicker.composables.foryou

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.data.network.NetworkConfig
import dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker.ExploreItem

/**
 * Signature Google TV / Netflix style "Top 10 Today" rail with large ranking typography.
 * Completely free of any "Rent" or "Buy" badges — tailor-made for Next Player's free streaming.
 */
@Composable
fun GoogleTvTop10Row(
    title: String = "Top 10 Today in Movies & Shows",
    subtitle: String = "Most-watched blockbusters • Free to stream",
    items: List<ExploreItem>,
    onMediaClick: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val top10Items = items.filter { it is ExploreItem.MovieItem || it is ExploreItem.TvShowItem }.take(10)
    if (top10Items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF8897AA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Rail with Numbered Rank Badges
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                items = top10Items,
                key = { _, item -> item.uniqueKey }
            ) { index, item ->
                val rank = index + 1
                val id: Int
                val titleText: String
                val posterPath: String?
                val isTv: Boolean
                val rating: Double

                when (item) {
                    is ExploreItem.MovieItem -> {
                        id = item.id
                        titleText = item.title
                        posterPath = item.posterPath
                        isTv = false
                        rating = item.rating
                    }
                    is ExploreItem.TvShowItem -> {
                        id = item.id
                        titleText = item.name
                        posterPath = item.posterPath
                        isTv = true
                        rating = item.rating
                    }
                    else -> return@itemsIndexed
                }

                val posterUrl = posterPath?.let { NetworkConfig.tmdbImageUrl("w500", it) }

                Box(
                    modifier = Modifier
                        .width(170.dp)
                        .height(210.dp)
                        .clickable { onMediaClick(id, isTv) }
                ) {
                    // Giant Stylized Rank Number (Layered behind/left of the card)
                    Text(
                        text = "$rank",
                        color = Color(0x334E6078),
                        fontSize = 86.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-4).dp, y = 8.dp)
                    )

                    // Vertical Poster Card
                    Box(
                        modifier = Modifier
                            .width(128.dp)
                            .height(188.dp)
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F1522))
                            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(12.dp))
                    ) {
                        if (!posterUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = posterUrl,
                                contentDescription = titleText,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF161F30))
                            )
                        }

                        // Bottom Scrim Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.55f to Color.Transparent,
                                        0.80f to Color(0xCC07090E),
                                        1.0f to Color(0xFF07090E)
                                    )
                                )
                        )

                        // Top Badges: 4K & Free Stream
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xCC00ED82))
                                    .padding(horizontal = 4.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "FREE",
                                    color = Color.Black,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0x99000000))
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
                        }

                        // Bottom Title & Match
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = titleText,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (rating > 0.0) {
                                val matchScore = (rating * 10).toInt().coerceIn(75, 99)
                                Text(
                                    text = "$matchScore% Match",
                                    color = Color(0xFF00ED82),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
