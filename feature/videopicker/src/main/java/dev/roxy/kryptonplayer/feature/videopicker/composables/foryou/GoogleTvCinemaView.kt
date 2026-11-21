package dev.roxy.kryptonplayer.feature.videopicker.composables.foryou

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.model.CelebritySpotlight
import dev.roxy.kryptonplayer.core.model.EntertainmentHighlight
import dev.roxy.kryptonplayer.core.model.ForYouFeaturedItem
import dev.roxy.kryptonplayer.feature.videopicker.composables.ExploreSection
import dev.roxy.kryptonplayer.feature.videopicker.composables.highlights.HighlightDetailModal
import dev.roxy.kryptonplayer.feature.videopicker.composables.highlights.HighlightsSection
import dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker.ExploreCategory
import dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker.ExploreItem
import dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker.TmdbExploreUiState

/**
 * Dedicated Google TV Cinema Experience for Next Player.
 *
 * Built with:
 * - Authentic JioHotstar Cinema Dark UI (#07090E, glowing neon cyan/green accents, glass cards).
 * - Google TV Guide Bar (Pill navigation: For You, Movies, Shows, Highlights, Free & 4K).
 * - 5-Second Auto-Sliding Giant Billboard Carousel with stories countdown line.
 * - Signature "Top 10 Today" rail with large ranking typography.
 * - In-App Entertainment Highlights & Celebrity Buzz popup modal (no external web browser).
 * - NO RENT / NO BUY / NO PRICE TAGS: strictly tailor-made for Next Player's free streaming.
 */
@Composable
fun GoogleTvCinemaView(
    forYouItems: List<ForYouFeaturedItem>,
    highlights: List<EntertainmentHighlight>,
    celebrities: List<CelebritySpotlight>,
    tmdbExploreState: TmdbExploreUiState,
    onMediaClick: (Int, Boolean) -> Unit,
    onActorClick: (Int) -> Unit,
    onSeeAllCategoryClick: (ExploreCategory) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    var selectedGuideTab by remember { mutableStateOf(GoogleTvTab.FOR_YOU) }
    var activeModalHighlight by remember { mutableStateOf<EntertainmentHighlight?>(null) }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
        ) {
            // 1. Google TV Signature Guide Bar (Pill selector)
            GoogleTvGuideBar(
                selectedTab = selectedGuideTab,
                onTabSelected = { selectedGuideTab = it },
                modifier = Modifier.fillMaxWidth()
            )

            when (selectedGuideTab) {
                // ==========================================
                // TAB 1: FOR YOU (Flagship Google TV Screen)
                // ==========================================
                GoogleTvTab.FOR_YOU -> {
                    // Giant Hero Billboard Carousel (5s Auto Slide, No Rent)
                    if (forYouItems.isNotEmpty()) {
                        ForYouHeroCarousel(
                            items = forYouItems,
                            onWatchClick = { item ->
                                val tmdbId = item.id.removePrefix("foryou_").removePrefix("curated_foryou_in_").removePrefix("curated_foryou_gl_").toIntOrNull()
                                if (tmdbId != null) {
                                    onMediaClick(tmdbId, false)
                                } else {
                                    val fallbackId = tmdbExploreState.trending.firstOrNull()?.id
                                    if (fallbackId != null) onMediaClick(fallbackId, false)
                                }
                            },
                            onItemClick = { item ->
                                val tmdbId = item.id.removePrefix("foryou_").removePrefix("curated_foryou_in_").removePrefix("curated_foryou_gl_").toIntOrNull()
                                if (tmdbId != null) {
                                    onMediaClick(tmdbId, false)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Iconic Google TV Top 10 Rail (Free Streams, 4K UHD, No Rent)
                    val top10Items = tmdbExploreState.trending.ifEmpty { tmdbExploreState.popularMovies }
                    if (top10Items.isNotEmpty()) {
                        GoogleTvTop10Row(
                            title = "Top 10 in India & Worldwide Today",
                            subtitle = "Most-streamed blockbusters • Free in 4K UHD",
                            items = top10Items,
                            onMediaClick = onMediaClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // In-App Highlights & Celebrity Buzz
                    if (highlights.isNotEmpty() || celebrities.isNotEmpty()) {
                        HighlightsSection(
                            highlights = highlights,
                            celebrities = celebrities,
                            onHighlightClick = { highlight ->
                                activeModalHighlight = highlight
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Action & Adventure Blockbusters
                    if (tmdbExploreState.actionMovies.isNotEmpty()) {
                        ExploreSection(
                            title = "High-Octane Action Blockbusters",
                            items = tmdbExploreState.actionMovies,
                            onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.ACTION_MOVIES) },
                            onMediaClick = onMediaClick,
                            onActorClick = onActorClick
                        )
                    }

                    // Binge-Worthy TV Series
                    if (tmdbExploreState.popularTvShows.isNotEmpty()) {
                        ExploreSection(
                            title = "Binge-Worthy Series for You",
                            items = tmdbExploreState.popularTvShows,
                            onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.TRENDING_TV) },
                            onMediaClick = onMediaClick,
                            onActorClick = onActorClick
                        )
                    }

                    // Popular Cast & Creators
                    if (tmdbExploreState.popularActors.isNotEmpty()) {
                        ExploreSection(
                            title = "Popular Cast & Directors",
                            items = tmdbExploreState.popularActors,
                            onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.POPULAR_ACTORS) },
                            onMediaClick = onMediaClick,
                            onActorClick = onActorClick
                        )
                    }

                    // Sci-Fi & Supernatural
                    if (tmdbExploreState.scifiMovies.isNotEmpty()) {
                        ExploreSection(
                            title = "Sci-Fi & Cosmic Spectacles",
                            items = tmdbExploreState.scifiMovies,
                            onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.SCIFI_MOVIES) },
                            onMediaClick = onMediaClick,
                            onActorClick = onActorClick
                        )
                    }
                }

                // ==========================================
                // TAB 2: YOUR STUFF (Google TV Personal Hub)
                // ==========================================
                GoogleTvTab.YOUR_STUFF -> {
                    val watchlist = tmdbExploreState.trending.ifEmpty { tmdbExploreState.popularMovies }
                    YourStuffSection(
                        watchlistItems = watchlist,
                        onMediaClick = onMediaClick,
                        onBrowseExplore = {
                            selectedGuideTab = GoogleTvTab.FOR_YOU
                        }
                    )
                }

                // ==========================================
                // TAB 3: MOVIES (Google TV Movies Experience)
                // ==========================================
                GoogleTvTab.MOVIES -> {
                    // Movies Billboard
                    if (forYouItems.isNotEmpty()) {
                        ForYouHeroCarousel(
                            items = forYouItems,
                            onWatchClick = { item ->
                                val tmdbId = item.id.removePrefix("foryou_").toIntOrNull()
                                if (tmdbId != null) onMediaClick(tmdbId, false)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Top 10 Movies Today
                    GoogleTvTop10Row(
                        title = "Top 10 Movies Today",
                        subtitle = "Trending blockbuster films • Free stream",
                        items = tmdbExploreState.popularMovies,
                        onMediaClick = onMediaClick
                    )

                    // Popular Movies
                    ExploreSection(
                        title = ExploreCategory.POPULAR_MOVIES.displayName,
                        items = tmdbExploreState.popularMovies,
                        onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.POPULAR_MOVIES) },
                        onMediaClick = onMediaClick,
                        onActorClick = onActorClick
                    )

                    // Critically Acclaimed
                    ExploreSection(
                        title = ExploreCategory.TOP_RATED_MOVIES.displayName,
                        items = tmdbExploreState.topRatedMovies,
                        onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.TOP_RATED_MOVIES) },
                        onMediaClick = onMediaClick,
                        onActorClick = onActorClick
                    )

                    // Comedies & Fun
                    ExploreSection(
                        title = ExploreCategory.COMEDY_MOVIES.displayName,
                        items = tmdbExploreState.comedyMovies,
                        onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.COMEDY_MOVIES) },
                        onMediaClick = onMediaClick,
                        onActorClick = onActorClick
                    )

                    // Worth The Wait
                    ExploreSection(
                        title = ExploreCategory.UPCOMING_MOVIES.displayName,
                        items = tmdbExploreState.upcomingMovies,
                        onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.UPCOMING_MOVIES) },
                        onMediaClick = onMediaClick,
                        onActorClick = onActorClick
                    )
                }

                // ==========================================
                // TAB 3: SHOWS (Google TV TV Series Experience)
                // ==========================================
                GoogleTvTab.SHOWS -> {
                    val tvItems = tmdbExploreState.popularTvShows.ifEmpty { tmdbExploreState.topRatedTvShows }

                    // Top 10 Series Today
                    GoogleTvTop10Row(
                        title = "Top 10 TV Shows & Web Series",
                        subtitle = "Binge-worthy seasons • All episodes unlocked",
                        items = tvItems,
                        onMediaClick = onMediaClick
                    )

                    // Trending TV
                    ExploreSection(
                        title = ExploreCategory.TRENDING_TV.displayName,
                        items = tmdbExploreState.popularTvShows,
                        onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.TRENDING_TV) },
                        onMediaClick = onMediaClick,
                        onActorClick = onActorClick
                    )

                    // Top Rated TV
                    ExploreSection(
                        title = ExploreCategory.TOP_RATED_TV.displayName,
                        items = tmdbExploreState.topRatedTvShows,
                        onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.TOP_RATED_TV) },
                        onMediaClick = onMediaClick,
                        onActorClick = onActorClick
                    )
                }

                // ==========================================
                // TAB 4: HIGHLIGHTS (Buzz & Celebrity Stories)
                // ==========================================
                GoogleTvTab.HIGHLIGHTS -> {
                    HighlightsSection(
                        highlights = highlights,
                        celebrities = celebrities,
                        onHighlightClick = { highlight ->
                            activeModalHighlight = highlight
                        }
                    )
                }

                // ==========================================
                // TAB 5: FREE & 4K (Next Player Pure Free Streams)
                // ==========================================
                GoogleTvTab.FREE_STREAMS -> {
                    val free4KItems = tmdbExploreState.topRatedMovies.ifEmpty { tmdbExploreState.popularMovies }

                    GoogleTvTop10Row(
                        title = "Free 4K Ultra HD Cinema",
                        subtitle = "Dolby Atmos 5.1 • Zero subscription required • Direct play",
                        items = free4KItems,
                        onMediaClick = onMediaClick
                    )

                    ExploreSection(
                        title = "Masterpiece Cinema in 4K HDR",
                        items = tmdbExploreState.topRatedMovies,
                        onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.TOP_RATED_MOVIES) },
                        onMediaClick = onMediaClick,
                        onActorClick = onActorClick
                    )

                    ExploreSection(
                        title = "Blockbuster Action in 4K",
                        items = tmdbExploreState.actionMovies,
                        onSeeAllClick = { onSeeAllCategoryClick(ExploreCategory.ACTION_MOVIES) },
                        onMediaClick = onMediaClick,
                        onActorClick = onActorClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // In-App Popup Modal for Highlights (No external browser!)
        activeModalHighlight?.let { highlight ->
            HighlightDetailModal(
                highlight = highlight,
                onDismiss = { activeModalHighlight = null }
            )
        }
    }
}
