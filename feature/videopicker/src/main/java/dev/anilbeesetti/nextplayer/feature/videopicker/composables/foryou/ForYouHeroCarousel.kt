package dev.anilbeesetti.nextplayer.feature.videopicker.composables.foryou

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.anilbeesetti.nextplayer.core.model.ForYouFeaturedItem
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.highlights.HighlightIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Giant Cinema Billboard Carousel for the "For You" section inspired by Google TV,
 * crafted with authentic JioHotstar dark textures, 5-second automatic sliding,
 * segmented story timer lines, and rich metadata.
 */
@Composable
fun ForYouHeroCarousel(
    items: List<ForYouFeaturedItem>,
    modifier: Modifier = Modifier,
    onWatchClick: ((ForYouFeaturedItem) -> Unit)? = null,
    onWatchlistClick: ((ForYouFeaturedItem) -> Unit)? = null,
    onItemClick: ((ForYouFeaturedItem) -> Unit)? = null
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val coroutineScope = rememberCoroutineScope()

    // 5-Second Auto Slide Timer (Deck Progress Indicator)
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(pagerState.currentPage, isDragged) {
        if (!isDragged) {
            progressAnim.snapTo(0f)
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
            // 5 Seconds complete -> Slide to next item
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(510.dp)
            .background(Color(0xFF07090E))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onItemClick?.invoke(item) }
            ) {
                // 1. Full-Bleed 4K Backdrop Image
                AsyncImage(
                    model = item.backdropUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Triple-Layer Cinema Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color(0x9907090E),
                                0.25f to Color.Transparent,
                                0.55f to Color(0x6607090E),
                                0.82f to Color(0xE607090E),
                                1.0f to Color(0xFF07090E)
                            )
                        )
                )

                // Left Subtle Vignette for Text Contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to Color(0xAA07090E),
                                0.65f to Color.Transparent
                            )
                        )
                )

                // 3. Foreground Billboard Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    // Top Pick Brand Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC07090E))
                            .border(1.dp, Color(0x3300ED82), RoundedCornerShape(6.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = HighlightIcons.SparkleStar,
                            contentDescription = null,
                            tint = Color(0xFF00ED82),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "TOP PICK FOR YOU",
                            color = Color(0xFF00ED82),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Giant Title
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tagline
                    Text(
                        text = item.tagline,
                        color = Color(0xFFBAC5D6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metadata Pill Badges (Match, Year, Age, Badges)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Match Score
                        Text(
                            text = "${item.matchScore}% Match",
                            color = Color(0xFF00ED82),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "•",
                            color = Color(0xFF5C667A),
                            fontSize = 12.sp
                        )

                        // Year
                        Text(
                            text = item.releaseYear,
                            color = Color(0xFFBAC5D6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Age Rating Box
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.ageRating,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Quality Badge (4K / Atmos)
                        item.qualityBadges.take(1).forEach { badge ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x220075FF))
                                    .border(0.8.dp, Color(0x660075FF), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    color = Color(0xFF38B6FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Dual Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Primary Watch Button (Cyan Gradient)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF0075FF), Color(0xFF00C6FF))
                                    )
                                )
                                .clickable { onWatchClick?.invoke(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = HighlightIcons.PlayRound,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Watch Now",
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Secondary Watchlist Button (Glass Acrylic)
                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xCC0D121F))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .clickable { onWatchlistClick?.invoke(item) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "+",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Watchlist",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. 5-Second Segmented Story Progress Bar (Google TV Deck Indicator)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEachIndexed { index, _ ->
                val isActive = index == pagerState.currentPage
                val isPast = index < pagerState.currentPage

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                ) {
                    if (isPast) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                        )
                    } else if (isActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressAnim.value)
                                .height(3.dp)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}
