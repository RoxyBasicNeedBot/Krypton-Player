package dev.roxy.kryptonplayer.feature.videopicker.composables


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Refresh
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.LottieStickerImage
import dev.roxy.kryptonplayer.core.ui.components.shimmer
import dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker.ExploreCategory
import dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker.ExploreItem
import dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker.TmdbExploreUiState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.rememberCoroutineScope
import dev.roxy.kryptonplayer.core.data.network.NetworkConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TmdbExploreView(
    tmdbExploreState: TmdbExploreUiState,
    onSeeAllCategoryClick: (ExploreCategory) -> Unit,
    onClearSeeAll: () -> Unit,
    onTmdbMediaClick: (Int, Boolean) -> Unit,
    onTmdbActorClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onLoadMore: (ExploreCategory) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onRetry: () -> Unit = {},
) {
    val actorMinWidth = 110.dp
    val mediaMinWidth = 125.dp
    val contentHorizontalPadding = 8.dp
    val itemSpacing = 2.dp

    if (tmdbExploreState.activeSeeAllCategory != null) {
        val category = tmdbExploreState.activeSeeAllCategory
        val title = category.displayName
        val rawItems = when (category) {
            ExploreCategory.POPULAR_MOVIES -> tmdbExploreState.popularMovies
            ExploreCategory.TRENDING_TV -> tmdbExploreState.popularTvShows
            ExploreCategory.POPULAR_ACTORS -> tmdbExploreState.popularActors
            ExploreCategory.UPCOMING_MOVIES -> tmdbExploreState.upcomingMovies
            ExploreCategory.TOP_RATED_MOVIES -> tmdbExploreState.topRatedMovies
            ExploreCategory.TOP_RATED_TV -> tmdbExploreState.topRatedTvShows
            ExploreCategory.ACTION_MOVIES -> tmdbExploreState.actionMovies
            ExploreCategory.COMEDY_MOVIES -> tmdbExploreState.comedyMovies
            ExploreCategory.SCIFI_MOVIES -> tmdbExploreState.scifiMovies
        }
        val items = remember(rawItems) { rawItems.distinctBy { it.uniqueKey } }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(contentPadding)
        ) {
            // Header with Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onClearSeeAll,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )
                ) {
                    Icon(
                        imageVector = NextIcons.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Vertical Grid of results
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxWidth = this.maxWidth - (contentHorizontalPadding * 2) - itemSpacing
                val maxActors = (maxWidth / actorMinWidth).toInt().coerceAtLeast(1)
                val maxMedia = (maxWidth / mediaMinWidth).toInt().coerceAtLeast(1)
                val spans = lcm(maxActors, maxMedia)

                val singleActorSpan = spans / maxActors
                val singleMediaSpan = spans / maxMedia

                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Fixed(spans),
                    contentPadding = PaddingValues(horizontal = contentHorizontalPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                ) {
                    items(
                        items = items,
                        key = { item -> item.uniqueKey },
                        span = { item ->
                            when (item) {
                                is ExploreItem.ActorItem -> GridItemSpan(singleActorSpan)
                                else -> GridItemSpan(singleMediaSpan)
                            }
                        }
                    ) { item ->
                        when (item) {
                            is ExploreItem.MovieItem -> {
                                TmdbMediaCard(
                                    title = item.title,
                                    posterPath = item.posterPath,
                                    rating = item.rating,
                                    releaseDate = item.releaseDate,
                                    onClick = { onTmdbMediaClick(item.id, false) },
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                            is ExploreItem.TvShowItem -> {
                                TmdbMediaCard(
                                    title = item.name,
                                    posterPath = item.posterPath,
                                    rating = item.rating,
                                    releaseDate = item.firstAirDate,
                                    onClick = { onTmdbMediaClick(item.id, true) },
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                            is ExploreItem.ActorItem -> {
                                TmdbActorCard(
                                    name = item.name,
                                    profilePath = item.profilePath,
                                    department = item.department,
                                    onClick = { onTmdbActorClick(item.id) },
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }

                    if (tmdbExploreState.hasReachedEnd[category] != true) {
                        item(
                            span = { GridItemSpan(spans) }
                        ) {
                            LaunchedEffect(category) {
                                onLoadMore(category)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    } else if (tmdbExploreState.isLoading && tmdbExploreState.popularMovies.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF07090E))
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shimmer()
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                dev.roxy.kryptonplayer.core.ui.components.ExploreGridSkeleton(cardWidth = 125.dp)
            }
        }
    } else if (tmdbExploreState.error != null && tmdbExploreState.popularMovies.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF07090E))
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LottieStickerImage(
                        resId = R.raw.anim_telegram_link_broken,
                        size = 110.dp,
                        contentDescription = "Connection Lost"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Unable to Connect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Check your internet connection and try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retry Connection",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    } else {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF07090E))
                .verticalScroll(scrollState)
                .padding(contentPadding)
        ) {
            val billboardItems = remember(tmdbExploreState.trending, tmdbExploreState.popularMovies) {
                (tmdbExploreState.trending + tmdbExploreState.popularMovies).distinctBy { it.uniqueKey }.take(8)
            }

            if (billboardItems.isNotEmpty()) {
                JioHotstarHeroBillboard(
                    items = billboardItems,
                    onMediaClick = onTmdbMediaClick
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Section 1: Popular Movies
            ExploreSection(
                title = ExploreCategory.POPULAR_MOVIES.displayName,
                items = tmdbExploreState.popularMovies,
                onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.POPULAR_MOVIES) },
                onMediaClick = onTmdbMediaClick,
                onActorClick = onTmdbActorClick
            )

            // Section 2: Your Next Watch (Trending TV)
            ExploreSection(
                title = ExploreCategory.TRENDING_TV.displayName,
                items = tmdbExploreState.popularTvShows,
                onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.TRENDING_TV) },
                onMediaClick = onTmdbMediaClick,
                onActorClick = onTmdbActorClick
            )

            // Section 3: Critically Acclaimed Movies (Top Rated)
            ExploreSection(
                title = ExploreCategory.TOP_RATED_MOVIES.displayName,
                items = tmdbExploreState.topRatedMovies,
                onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.TOP_RATED_MOVIES) },
                onMediaClick = onTmdbMediaClick,
                onActorClick = onTmdbActorClick
            )

            // Section 4: Action & Adventure
            ExploreSection(
                title = ExploreCategory.ACTION_MOVIES.displayName,
                items = tmdbExploreState.actionMovies,
                onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.ACTION_MOVIES) },
                onMediaClick = onTmdbMediaClick,
                onActorClick = onTmdbActorClick
            )

            // Section 5: Top Rated TV Series
            ExploreSection(
                title = ExploreCategory.TOP_RATED_TV.displayName,
                items = tmdbExploreState.topRatedTvShows,
                onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.TOP_RATED_TV) },
                onMediaClick = onTmdbMediaClick,
                onActorClick = onTmdbActorClick
            )

            // Section 6: Popular Cast & Stars
            ExploreSection(
                title = ExploreCategory.POPULAR_ACTORS.displayName,
                items = tmdbExploreState.popularActors,
                onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.POPULAR_ACTORS) },
                onMediaClick = onTmdbMediaClick,
                onActorClick = onTmdbActorClick
            )

            // Section 7: Comedies & Fun
            ExploreSection(
                title = ExploreCategory.COMEDY_MOVIES.displayName,
                items = tmdbExploreState.comedyMovies,
                onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.COMEDY_MOVIES) },
                onMediaClick = onTmdbMediaClick,
                onActorClick = onTmdbActorClick
            )

            // Section 8: Sci-Fi & Supernatural
            ExploreSection(
                title = ExploreCategory.SCIFI_MOVIES.displayName,
                items = tmdbExploreState.scifiMovies,
                onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.SCIFI_MOVIES) },
                onMediaClick = onTmdbMediaClick,
                onActorClick = onTmdbActorClick
            )

            // Section 9: Worth The Wait (Upcoming Movies)
            ExploreSection(
                title = ExploreCategory.UPCOMING_MOVIES.displayName,
                items = tmdbExploreState.upcomingMovies,
                onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.UPCOMING_MOVIES) },
                onMediaClick = onTmdbMediaClick,
                onActorClick = onTmdbActorClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


/**
 * JioHotstar-style auto-advancing Hero Billboard Carousel.
 * Features:
 * - 420dp immersive cinematic frame with authentic JioHotstar Inter typography
 * - Auto-advances every 4.5 seconds with touch drag detection (pauses when touched)
 * - Multi-layer JioHotstar bottom fade gradient for crisp text legibility
 * - Category / Trending rank badge & IMDb / TMDB ratings
 * - Title, Year, Content type, UHD 4K, 5.1 Audio badges, and overview synopsis
 * - "Watch Details" solid white button & "Watchlist" frosted button
 * - JioHotstar dynamic indicator pills with animated width
 */
@Composable
fun JioHotstarHeroBillboard(
    items: List<ExploreItem>,
    onMediaClick: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val validItems = remember(items) {
        items.filter { it is ExploreItem.MovieItem || it is ExploreItem.TvShowItem }.take(8)
    }
    if (validItems.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { validItems.size }
    )
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-advance every 4.5 seconds (resets and pauses smoothly when user touches/drags)
    LaunchedEffect(pagerState.currentPage, isDragged, validItems.size) {
        if (validItems.size > 1 && !isDragged) {
            delay(4500L)
            val nextPage = (pagerState.currentPage + 1) % validItems.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        // Main framed card with JioHotstar signature styling
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF090B10)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val item = validItems[page]
                    val id: Int
                    val title: String
                    val posterPath: String?
                    val backdropPath: String?
                    val rating: Double
                    val releaseDate: String?
                    val overview: String?
                    val isTv: Boolean

                    when (item) {
                        is ExploreItem.MovieItem -> {
                            id = item.id
                            title = item.title
                            posterPath = item.posterPath
                            backdropPath = item.backdropPath
                            rating = item.rating
                            releaseDate = item.releaseDate
                            overview = item.overview
                            isTv = false
                        }
                        is ExploreItem.TvShowItem -> {
                            id = item.id
                            title = item.name
                            posterPath = item.posterPath
                            backdropPath = item.backdropPath
                            rating = item.rating
                            releaseDate = item.firstAirDate
                            overview = item.overview
                            isTv = true
                        }
                        else -> return@HorizontalPager
                    }

                    val imagePath = backdropPath ?: posterPath
                    val imageUrl = imagePath?.let {
                        NetworkConfig.tmdbImageUrl(if (backdropPath != null) "w780" else "w500", it)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onMediaClick(id, isTv) }
                    ) {
                        // High-res Backdrop / Poster Image
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF131722))
                            )
                        }

                        // Top Scrim: Soft shadow for top pill badges & contrast
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.70f),
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = 140f
                                    )
                                )
                        )

                        // Bottom Scrim: Multi-stop JioHotstar cinematic gradient fade into deep black
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.25f to Color(0x40090B10),
                                        0.55f to Color(0xDD090B10),
                                        0.80f to Color(0xFF090B10),
                                        1.0f to Color(0xFF090B10)
                                    )
                                )
                        )

                        // Top Badges Row (JioHotstar Hot & Trending Pill + IMDb Rating)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Trending Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.22f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🔥 #${page + 1} IN TOP 10 TODAY",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFFFF4D6D)
                                    )
                                }
                            }

                            // Top Right: Official IMDb Rating Chip
                            if (rating > 0.0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E1E24).copy(alpha = 0.85f),
                                    border = BorderStroke(0.6.dp, Color(0xFFF5C518).copy(alpha = 0.65f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(dev.roxy.kryptonplayer.core.ui.R.drawable.ic_imdb_logo),
                                            contentDescription = "IMDb",
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.5.dp))
                                        Text(
                                            text = String.format(Locale.US, "%.1f", rating),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Overlay: Title, Metadata, Action Buttons, and Page Indicators
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            // Movie / Show Title (JioHotstar SemiBold Authentic Inter Typography)
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 28.sp,
                                    letterSpacing = 0.15.sp
                                ),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(7.dp))

                            // Metadata Tags Row: Year, Category Type, 4K UHD, 5.1 AUDIO, TMDB Match %
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val year = releaseDate?.take(4)
                                if (!year.isNullOrBlank()) {
                                    Text(
                                        text = year,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.40f)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isTv) "Series" else "Movie",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }

                                // TMDB Match Chip
                                if (rating > 0.0) {
                                    val tmdbScore = (rating * 10).coerceIn(0.0, 100.0).toInt()
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF032541).copy(alpha = 0.85f),
                                        border = BorderStroke(0.5.dp, Color(0xFF01D277).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(dev.roxy.kryptonplayer.core.ui.R.drawable.ic_tmdb_logo),
                                                contentDescription = "TMDB",
                                                tint = Color(0xFF01D277),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${tmdbScore}%",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF01D277)
                                            )
                                        }
                                    }
                                }
                            }

                            // Overview Teaser
                            if (!overview.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(7.dp))
                                Text(
                                    text = overview,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.74f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Buttons Row (Watch Details + Watchlist)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Watch Details Primary Action (JioHotstar Solid White pill)
                                Button(
                                    onClick = { onMediaClick(id, isTv) },
                                    shape = RoundedCornerShape(22.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Watch Details",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                }

                                // Watchlist Secondary Action (JioHotstar Frosted glass pill)
                                var isWatchlisted by remember(id) { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = { isWatchlisted = !isWatchlisted },
                                    shape = RoundedCornerShape(22.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.12f),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isWatchlisted) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (isWatchlisted) Color(0xFF0075FF) else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isWatchlisted) "In Watchlist" else "Watchlist",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.5.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            // Dynamic Indicator Pills (JioHotstar dynamic blue active pill)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 13.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(validItems.size) { pageIndex ->
                                    val isSelected = pagerState.currentPage == pageIndex
                                    val width by animateDpAsState(
                                        targetValue = if (isSelected) 22.dp else 6.dp,
                                        animationSpec = tween(durationMillis = 280),
                                        label = "billboardIndicatorWidth"
                                    )
                                    val indicatorColor = if (isSelected) Color(0xFF0075FF) else Color.White.copy(alpha = 0.32f)

                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.5.dp)
                                            .width(width)
                                            .height(3.5.dp)
                                            .clip(CircleShape)
                                            .background(indicatorColor)
                                            .clickable {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pageIndex)
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExploreSection(
    title: String,
    items: List<ExploreItem>,
    onSeeAllClick: () -> Unit,
    onMediaClick: (Int, Boolean) -> Unit,
    onActorClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSeeAllClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "See All",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items.take(15)) { item ->
                when (item) {
                    is ExploreItem.MovieItem -> {
                        Box(modifier = Modifier.width(125.dp)) {
                            TmdbMediaCard(
                                title = item.title,
                                posterPath = item.posterPath,
                                rating = item.rating,
                                releaseDate = item.releaseDate,
                                onClick = { onMediaClick(item.id, false) }
                            )
                        }
                    }
                    is ExploreItem.TvShowItem -> {
                        Box(modifier = Modifier.width(125.dp)) {
                            TmdbMediaCard(
                                title = item.name,
                                posterPath = item.posterPath,
                                rating = item.rating,
                                releaseDate = item.firstAirDate,
                                onClick = { onMediaClick(item.id, true) }
                            )
                        }
                    }
                    is ExploreItem.ActorItem -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(110.dp)
                                .clickable { onActorClick(item.id) }
                        ) {
                            val imageUrl = item.profilePath?.let { dev.roxy.kryptonplayer.core.data.network.NetworkConfig.tmdbImageUrl("w185", it) }
                            if (imageUrl != null) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.name.take(2),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.name,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            if (item.department != null) {
                                Text(
                                    text = item.department,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    }
}


