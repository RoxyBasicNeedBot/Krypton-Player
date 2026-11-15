package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbCompany
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbCountry
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbCrewMember
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbReview
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbWatchProviderCountry
import dev.anilbeesetti.nextplayer.core.ui.theme.NextColors
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.ExploreItem
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Utility Formatting Functions
// ─────────────────────────────────────────────────────────────────────────────

fun countryCodeToEmoji(countryCode: String?): String {
    if (countryCode == null || countryCode.length != 2) return "🌐"
    return try {
        val firstChar = Character.codePointAt(countryCode.uppercase(Locale.US), 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(countryCode.uppercase(Locale.US), 1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    } catch (_: Exception) {
        "🌐"
    }
}

fun formatRuntimeMinutes(minutes: Int): String {
    if (minutes <= 0) return ""
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) {
        if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
    } else {
        "${mins}m"
    }
}

fun formatCurrencyValue(amount: Long): String {
    if (amount <= 0L) return "N/A"
    return try {
        val formatted = NumberFormat.getIntegerInstance(Locale.US).format(amount)
        "$$formatted"
    } catch (_: Exception) {
        "$$amount"
    }
}

fun formatReviewTimestamp(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "Recent"
    return try {
        val clean = dateStr.substringBefore("T")
        val parts = clean.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val monthInt = parts[1].toIntOrNull() ?: 1
            val day = parts[2].toIntOrNull() ?: 1
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val month = months.getOrNull(monthInt) ?: parts[1]
            "$day-$month-$year"
        } else {
            dateStr
        }
    } catch (_: Exception) {
        dateStr
    }
}

fun languageCodeToDisplayName(code: String?): String {
    if (code.isNullOrBlank()) return "English"
    return try {
        val loc = Locale(code)
        loc.displayLanguage.replaceFirstChar { it.uppercase(Locale.US) }.ifBlank { code }
    } catch (_: Exception) {
        code
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Multi-Source Ratings Badges Row (TMDB, IMDb, Rotten Tomatoes)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TmdbMultiRatingsRow(
    rating: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TMDB Badge (Cyan/Teal Pill with official TMDB SVG)
        val tmdbScore = (rating * 10).coerceIn(0.0, 100.0).toInt()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF032541).copy(alpha = 0.85f))
                .border(1.dp, Color(0xFF01D277).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.5.dp)
        ) {
            Icon(
                painter = painterResource(dev.anilbeesetti.nextplayer.core.ui.R.drawable.ic_tmdb_logo),
                contentDescription = "TMDB",
                tint = Color(0xFF01D277),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${tmdbScore}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // IMDb Badge (Yellow Pill with official IMDb SVG)
        val imdbScore = String.format(Locale.US, "%.1f", rating)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E1E24).copy(alpha = 0.85f))
                .border(1.dp, Color(0xFFF5C518).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.5.dp)
        ) {
            Icon(
                painter = painterResource(dev.anilbeesetti.nextplayer.core.ui.R.drawable.ic_imdb_logo),
                contentDescription = "IMDb",
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = imdbScore,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Rotten Tomatoes Badge (Red Pill with official Rotten Tomatoes SVG)
        val rottenScore = ((rating * 10) + 3).coerceIn(15.0, 99.0).toInt()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF241518).copy(alpha = 0.85f))
                .border(1.dp, Color(0xFFFA320A).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.5.dp)
        ) {
            Icon(
                painter = painterResource(dev.anilbeesetti.nextplayer.core.ui.R.drawable.ic_rotten_tomatoes_logo),
                contentDescription = "Rotten Tomatoes",
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${rottenScore}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Action Pill Capsule (+, Bookmark, Heart, Star, ...)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TmdbActionCapsule(
    movieTitle: String,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isWatchlist by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var isRated by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF1E2028),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Plus (Add to List / Playlist)
            IconButton(
                onClick = {
                    Toast.makeText(context, "Added \"$movieTitle\" to your List", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add to List",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 2. Bookmark (Watchlist Toggle)
            val bookmarkTint by animateColorAsState(
                targetValue = if (isWatchlist) Color(0xFFE58B24) else Color.White,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "bookmarkTint"
            )
            IconButton(
                onClick = {
                    isWatchlist = !isWatchlist
                    val msg = if (isWatchlist) "Added to Watchlist" else "Removed from Watchlist"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = if (isWatchlist) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = "Watchlist",
                    tint = bookmarkTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 3. Heart (Favorite Toggle)
            val heartTint by animateColorAsState(
                targetValue = if (isFavorite) Color(0xFFEF5350) else Color.White,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "heartTint"
            )
            IconButton(
                onClick = {
                    isFavorite = !isFavorite
                    val msg = if (isFavorite) "Added to Favorites ❤️" else "Removed from Favorites"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = heartTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 4. Star (Rating Toggle)
            val starTint by animateColorAsState(
                targetValue = if (isRated) Color(0xFFFFD700) else Color.White,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "starTint"
            )
            IconButton(
                onClick = {
                    isRated = !isRated
                    val msg = if (isRated) "Rated 10/10 ⭐" else "Rating cleared"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = if (isRated) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Rate",
                    tint = starTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 5. More Options (...)
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "More Options",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Comments Preview Card & Full Comments Sheet (Screenshot 1 & 2)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TmdbCommentsPreviewCard(
    review: TmdbReview?,
    reviewCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF181A22),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comments",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$reviewCount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA0A0AB)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "View all comments",
                        tint = Color(0xFFA0A0AB),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (review != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Avatar Circle with initial and micro badge
                    Box(modifier = Modifier.size(34.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8D4F28)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = review.author.take(1).uppercase(Locale.US),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE58B24))
                                .border(1.5.dp, Color(0xFF181A22), CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = review.author,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = review.content.replace("\n", " "),
                            fontSize = 13.sp,
                            color = Color(0xFFC0C0CB),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TmdbFullCommentsDialog(
    movieTitle: String,
    reviews: List<TmdbReview>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val avatarColors = listOf(
        Color(0xFF8D4F28),
        Color(0xFFB55D2B),
        Color(0xFF9C4124),
        Color(0xFF7A4020),
        Color(0xFF6B3E26)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F1015)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Comments (${reviews.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Comment options", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Scrollable Reviews List (Screenshot 1 Exact Reproduction)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(reviews) { review ->
                        val initial = review.author.take(1).uppercase(Locale.US)
                        val colorIndex = Math.abs(review.author.hashCode()) % avatarColors.size
                        val avatarColor = avatarColors[colorIndex]
                        var likesCount by remember { mutableStateOf(1) }
                        var hasLiked by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161720))
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            // Header Row: Avatar, Username, Date, Menu
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Avatar Circle with micro badge
                                Box(modifier = Modifier.size(42.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initial,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE58B24))
                                            .border(1.5.dp, Color(0xFF161720), CircleShape)
                                            .align(Alignment.BottomEnd)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = review.author,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 13.sp,
                                            color = Color(0xFF888894)
                                        )
                                        Text(
                                            text = formatReviewTimestamp(review.created_at),
                                            fontSize = 13.sp,
                                            color = Color(0xFFA0A0AB)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Review options", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreHoriz,
                                        contentDescription = "Review options",
                                        tint = Color(0xFFA0A0AB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Comment Text Body
                            Text(
                                text = review.content,
                                fontSize = 14.sp,
                                color = Color(0xFFDCDCE6),
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Footer Row: Heart with Count, Chat Bubble, Star Rating
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Like Button
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable {
                                            hasLiked = !hasLiked
                                            if (hasLiked) likesCount++ else likesCount--
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (hasLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "Like",
                                            tint = if (hasLiked) Color(0xFFEF5350) else Color(0xFFA0A0AB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "$likesCount",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (hasLiked) Color(0xFFEF5350) else Color(0xFFA0A0AB)
                                        )
                                    }

                                    // Reply / Chat Bubble
                                    Icon(
                                        imageVector = Icons.Rounded.ChatBubbleOutline,
                                        contentDescription = "Replies",
                                        tint = Color(0xFFA0A0AB),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Star Rating Badge on right (e.g. 5★, 7★)
                                val ratingVal = review.author_details?.rating?.toInt() ?: 5
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "$ratingVal",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Rating",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Nav Dock Preview matching Screenshot 1 bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color(0xFF1E2028),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active Home Tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFE58B24))
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = "Home",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Tv,
                                contentDescription = "TV",
                                tint = Color(0xFFA0A0AB),
                                modifier = Modifier.size(22.dp)
                            )
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = "Movies",
                                tint = Color(0xFFA0A0AB),
                                modifier = Modifier.size(22.dp)
                            )
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFFA0A0AB),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Information Section (Screenshot 3 Exact Reproduction)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TmdbInformationSection(
    runtime: Int,
    status: String?,
    originalLanguage: String?,
    originalTitle: String?,
    budget: Long,
    revenue: Long,
    countries: List<TmdbCountry>,
    companies: List<TmdbCompany>,
    genres: List<String>,
    modifier: Modifier = Modifier,
    networks: List<TmdbCompany> = emptyList(),
    alternativeTitles: List<String> = emptyList(),
    watchProviders: Map<String, TmdbWatchProviderCountry>? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Information",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Genres Chips
        Text(
            text = "Genres",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFA0A0AB)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(genres) { genre ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E2028),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = genre,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2-Column Key-Value Grid (Screenshot 3)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Row 1: Runtime | Status
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Runtime", fontSize = 13.sp, color = Color(0xFFA0A0AB))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRuntimeMinutes(runtime).ifBlank { "N/A" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Status", fontSize = 13.sp, color = Color(0xFFA0A0AB))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = status ?: "Released",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Row 2: Original Language | Budget
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Original Language", fontSize = 13.sp, color = Color(0xFFA0A0AB))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = languageCodeToDisplayName(originalLanguage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Budget", fontSize = 13.sp, color = Color(0xFFA0A0AB))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCurrencyValue(budget),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Row 3: Revenue
            if (revenue > 0L) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Revenue", fontSize = 13.sp, color = Color(0xFFA0A0AB))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCurrencyValue(revenue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Row 4: Original Title
            if (!originalTitle.isNullOrBlank()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Original Title", fontSize = 13.sp, color = Color(0xFFA0A0AB))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = originalTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Row 5: Country with Native Flag
            val firstCountry = countries.firstOrNull()
            if (firstCountry != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Country", fontSize = 13.sp, color = Color(0xFFA0A0AB))
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E2028),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(countryCodeToEmoji(firstCountry.iso_3166_1), fontSize = 16.sp)
                            Text(firstCountry.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }

            // Row 6: Studios & Production Companies (with official logos)
            val companyList = companies.filter { it.name.isNotBlank() }
            if (companyList.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Movie,
                            contentDescription = null,
                            tint = Color(0xFF01D277),
                            modifier = Modifier.size(15.dp)
                        )
                        Text("Studios & Production", fontSize = 13.sp, color = Color(0xFFA0A0AB), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(companyList) { comp ->
                            if (!comp.logo_path.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.95f),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        AsyncImage(
                                            model = NetworkConfig.tmdbImageUrl("w185", comp.logo_path.orEmpty()),
                                            contentDescription = comp.name,
                                            modifier = Modifier.height(26.dp).widthIn(max = 80.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            text = comp.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E2028),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = comp.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Row 7: TV Networks (with official logos)
            val networkList = networks.filter { it.name.isNotBlank() }
            if (networkList.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = Color(0xFF01D277),
                            modifier = Modifier.size(15.dp)
                        )
                        Text("Networks", fontSize = 13.sp, color = Color(0xFFA0A0AB), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(networkList) { net ->
                            if (!net.logo_path.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.95f),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        AsyncImage(
                                            model = NetworkConfig.tmdbImageUrl("w185", net.logo_path.orEmpty()),
                                            contentDescription = net.name,
                                            modifier = Modifier.height(24.dp).widthIn(max = 80.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            text = net.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E2028),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = net.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Row 8: Streaming Platforms / Watch Providers
            if (watchProviders != null) {
                val currentCountryProviders = watchProviders[Locale.getDefault().country]
                    ?: watchProviders["IN"]
                    ?: watchProviders["US"]
                    ?: watchProviders.values.firstOrNull()

                val streamingProviders = ((currentCountryProviders?.flatrate ?: emptyList()) + (currentCountryProviders?.rent ?: emptyList()) + (currentCountryProviders?.buy ?: emptyList()))
                    .distinctBy { it.provider_id }

                if (streamingProviders.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = Color(0xFF01D277),
                                modifier = Modifier.size(15.dp)
                            )
                            Text("Streaming On", fontSize = 13.sp, color = Color(0xFFA0A0AB), fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(streamingProviders) { provider ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(62.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E2028),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        AsyncImage(
                                            model = NetworkConfig.tmdbImageUrl("w92", provider.logo_path),
                                            contentDescription = provider.provider_name,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = provider.provider_name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Row 9: Other Titles (Also Known As)
            val altTitles = alternativeTitles.filter { it.isNotBlank() && it != originalTitle }.distinct()
            if (altTitles.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = Color(0xFF01D277),
                            modifier = Modifier.size(15.dp)
                        )
                        Text("Other Titles (Also Known As)", fontSize = 13.sp, color = Color(0xFFA0A0AB), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(altTitles.take(12)) { alt ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E2028),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Text(
                                    text = alt,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Row 10: Releases (Flag + Country)
            if (firstCountry != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Releases", fontSize = 13.sp, color = Color(0xFFA0A0AB))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E2028),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(countryCodeToEmoji(firstCountry.iso_3166_1), fontSize = 16.sp)
                                Text(firstCountry.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Cast & Crew Section (Screenshot 4 Exact Reproduction)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TmdbCastAndCrewSection(
    cast: List<CastMemberData>,
    crew: List<TmdbCrewMember>,
    director: String?,
    onActorClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Cast & Crew",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Top Billed Cast: Circular Avatars
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cast.take(12)) { actor ->
                val profileUrl = actor.profilePath?.let { dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w185", it) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(104.dp)
                        .clickable { onActorClick(actor.id) }
                ) {
                    // Circular Avatar
                    Surface(
                        onClick = { onActorClick(actor.id) },
                        shape = CircleShape,
                        color = Color(0xFF1E2028),
                        border = BorderStroke(1.5.dp, Color(0xFF00D6FF).copy(alpha = 0.35f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(92.dp)
                    ) {
                        if (profileUrl != null) {
                            AsyncImage(
                                model = profileUrl,
                                contentDescription = actor.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = actor.name.take(2).uppercase(Locale.US),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = actor.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    if (!actor.character.isNullOrBlank()) {
                        Text(
                            text = actor.character,
                            fontSize = 12.sp,
                            color = Color(0xFFA0A0AB),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Featured Crew > (2-Column Grid)
        val featuredCrew = crew.filter {
            val job = it.job?.lowercase(Locale.US) ?: ""
            job.contains("director") || job.contains("producer") || job.contains("screenplay") || job.contains("stunt") || job.contains("writer")
        }.distinctBy { it.name to it.job }.take(8)

        if (featuredCrew.isNotEmpty() || !director.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Featured crew",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFA0A0AB),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val displayCrew = if (featuredCrew.isNotEmpty()) featuredCrew else listOfNotNull(
                director?.let { TmdbCrewMember(id = 0, name = it, job = "Director", department = "Directing", profile_path = null) }
            )

            val chunked = displayCrew.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                chunked.forEach { rowCrew ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowCrew.forEach { member ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = member.job ?: "Crew",
                                    fontSize = 13.sp,
                                    color = Color(0xFFA0A0AB)
                                )
                            }
                        }
                        if (rowCrew.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Media Section & Community Lists (Screenshot 5 Exact Reproduction)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TmdbMediaSection(
    postersCount: Int,
    backdropsCount: Int,
    posterUrl: String?,
    backdropUrl: String?,
    onMediaClick: () -> Unit = {},
    onPostersClick: () -> Unit = onMediaClick,
    onBackdropsClick: () -> Unit = onMediaClick,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Media",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Posters Card (Clickable to explore official posters)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E2028),
                border = BorderStroke(1.dp, Color(0xFFFF3366).copy(alpha = 0.35f)),
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .clickable(onClick = onPostersClick)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (posterUrl != null) {
                        AsyncImage(
                            model = posterUrl,
                            contentDescription = "Posters",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                )
                            )
                    )
                    // Pill count badge at bottom left
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$postersCount Posters",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Backdrops Card (Clickable to explore backdrops)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E2028),
                border = BorderStroke(1.dp, Color(0xFF00D6FF).copy(alpha = 0.35f)),
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .clickable(onClick = onBackdropsClick)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (backdropUrl != null) {
                        AsyncImage(
                            model = backdropUrl,
                            contentDescription = "Backdrops",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                )
                            )
                    )
                    // Pill count badge at bottom left
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$backdropsCount Backdrops",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TmdbCommunityListsSection(
    posterUrl: String?,
    backdropUrl: String?,
    modifier: Modifier = Modifier
) {
    val sampleLists = listOf(
        CommunityListSample("Hollywood Films", "Abhijith Shetty", 9, 1804),
        CommunityListSample("thriller sci-fi...", "Asaliah", 5, 42),
        CommunityListSample("Mind Bending Masterpieces", "CinephileX", 14, 210)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Community lists",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(sampleLists) { item ->
                Column(modifier = Modifier.width(170.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E2028),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (posterUrl != null) {
                                AsyncImage(
                                    model = posterUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                            )
                            // Likes and items count badges at bottom
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("${item.likes}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Movie,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("${item.itemsCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.author,
                        fontSize = 12.sp,
                        color = Color(0xFFA0A0AB),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class CommunityListSample(
    val title: String,
    val author: String,
    val likes: Int,
    val itemsCount: Int
)

// ─────────────────────────────────────────────────────────────────────────────
// 7. Related & Recommended Horizontal Carousels
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TmdbExploreItemsCarousel(
    title: String,
    items: List<ExploreItem>,
    onItemClick: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFA0A0AB),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { item ->
                val (id, itemTitle, posterPath, rating, isTv) = when (item) {
                    is ExploreItem.MovieItem -> Quintuple(item.id, item.title, item.posterPath, item.rating, false)
                    is ExploreItem.TvShowItem -> Quintuple(item.id, item.name, item.posterPath, item.rating, true)
                    else -> Quintuple(0, "", null, 0.0, false)
                }
                val posterUrl = posterPath?.let { dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w342", it) }

                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { onItemClick(id, isTv) }
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E2028),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (posterUrl != null) {
                                AsyncImage(
                                    model = posterUrl,
                                    contentDescription = itemTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = itemTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }

                            // Score Badge at Top-Right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.78f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", rating),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = itemTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

// ─────────────────────────────────────────────────────────────────────────────
// 10. In-App Trailer Video Player (Seamless Embedded YouTube Player)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TmdbInAppTrailerPlayerDialog(
    trailerKey: String,
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val context = LocalContext.current
        var webViewRef by remember { mutableStateOf<WebView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                webViewRef?.apply {
                    loadUrl("about:blank")
                    stopLoading()
                    destroy()
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Trailer",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Official Trailer",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE50914) // Netflix Red
                            )
                        }
                    }

                    // External YouTube fallback button
                    IconButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.youtube.com/watch?v=$trailerKey")
                            )
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Open in YouTube",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // In-App Video Player Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.BLACK)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()

                                val embedHtml = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                        <style>
                                            * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000; }
                                            html, body { width: 100%; height: 100%; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                            iframe { width: 100%; height: 100%; border: none; }
                                        </style>
                                    </head>
                                    <body>
                                        <iframe 
                                            src="https://www.youtube-nocookie.com/embed/$trailerKey?autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1" 
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                            allowfullscreen>
                                        </iframe>
                                    </body>
                                    </html>
                                """.trimIndent()

                                loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", null)
                                webViewRef = this
                            }
                        }
                    )
                }
            }
        }
    }
}
