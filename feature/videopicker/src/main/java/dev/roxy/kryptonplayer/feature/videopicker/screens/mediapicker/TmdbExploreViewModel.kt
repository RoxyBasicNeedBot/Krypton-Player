package dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.data.repository.ForYouRepository
import dev.roxy.kryptonplayer.core.data.repository.HighlightsRepository
import dev.roxy.kryptonplayer.core.data.repository.TmdbRepository
import dev.roxy.kryptonplayer.core.model.CelebritySpotlight
import dev.roxy.kryptonplayer.core.model.EntertainmentHighlight
import dev.roxy.kryptonplayer.core.model.ForYouFeaturedItem
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExploreCategory(val displayName: String) {
    POPULAR_MOVIES("Popular Movies"),
    TRENDING_TV("Your Next Watch"),
    POPULAR_ACTORS("Popular Cast & Stars"),
    UPCOMING_MOVIES("Worth The Wait"),
    TOP_RATED_MOVIES("Critically Acclaimed Movies"),
    TOP_RATED_TV("Top Rated TV Series"),
    ACTION_MOVIES("Action & Adventure"),
    COMEDY_MOVIES("Comedies & Fun"),
    SCIFI_MOVIES("Sci-Fi & Supernatural")
}

sealed interface ExploreItem {
    val id: Int
    val uniqueKey: String
        get() = when (this) {
            is MovieItem -> "tmdb_movie_$id"
            is TvShowItem -> "tmdb_tv_$id"
            is ActorItem -> "tmdb_actor_$id"
        }

    data class MovieItem(
        override val id: Int,
        val title: String,
        val posterPath: String?,
        val rating: Double,
        val releaseDate: String?,
        val backdropPath: String? = null,
        val overview: String? = null
    ) : ExploreItem

    data class TvShowItem(
        override val id: Int,
        val name: String,
        val posterPath: String?,
        val rating: Double,
        val firstAirDate: String?,
        val backdropPath: String? = null,
        val overview: String? = null
    ) : ExploreItem

    data class ActorItem(
        override val id: Int,
        val name: String,
        val profilePath: String?,
        val department: String?
    ) : ExploreItem
}


@HiltViewModel
class TmdbExploreViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
    private val forYouRepository: ForYouRepository,
    private val highlightsRepository: HighlightsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TmdbExploreUiState())
    val uiState: StateFlow<TmdbExploreUiState> = _uiState.asStateFlow()

    init {
        fetchAllData()
    }

    fun showSeeAll(category: ExploreCategory) {
        _uiState.update { it.copy(activeSeeAllCategory = category) }
    }

    fun clearSeeAll() {
        _uiState.update { it.copy(activeSeeAllCategory = null) }
    }

    fun fetchAllData(forceRefresh: Boolean = false) {
        if (!forceRefresh && _uiState.value.trending.isNotEmpty()) {
            return
        }
        _uiState.update { it.copy(isLoading = it.trending.isEmpty(), error = null) }
        viewModelScope.launch {
            try {
                val forYouDeferred = async { forYouRepository.getForYouFeaturedItems().getOrElse { emptyList() } }
                val highlightsDeferred = async { highlightsRepository.getHighlights().getOrElse { emptyList() } }
                val celebritiesDeferred = async { highlightsRepository.getCelebritySpotlights().getOrElse { emptyList() } }
                val trendingDeferred = async { tmdbRepository.getTrendingMovies() }
                val popularMoviesDeferred = async { tmdbRepository.getPopularMovies() }
                val popularTvShowsDeferred = async { tmdbRepository.getTrendingTvShows() }
                val popularActorsDeferred = async { tmdbRepository.getPopularPeople() }
                val upcomingMoviesDeferred = async { tmdbRepository.getUpcomingMovies() }
                val topRatedMoviesDeferred = async { tmdbRepository.getTopRatedMovies() }
                val topRatedTvShowsDeferred = async { tmdbRepository.getTopRatedTvShows() }
                val actionMoviesDeferred = async { tmdbRepository.discoverMovies("28") }
                val comedyMoviesDeferred = async { tmdbRepository.discoverMovies("35") }
                val scifiMoviesDeferred = async { tmdbRepository.discoverMovies("878") }

                val forYouRes = forYouDeferred.await()
                val highlightsRes = highlightsDeferred.await()
                val celebritiesRes = celebritiesDeferred.await()

                val trendingRes = trendingDeferred.await()
                val popularMoviesRes = popularMoviesDeferred.await()
                val popularTvShowsRes = popularTvShowsDeferred.await()
                val popularActorsRes = popularActorsDeferred.await()
                val upcomingMoviesRes = upcomingMoviesDeferred.await()
                val topRatedMoviesRes = topRatedMoviesDeferred.await()
                val topRatedTvShowsRes = topRatedTvShowsDeferred.await()
                val actionMoviesRes = actionMoviesDeferred.await()
                val comedyMoviesRes = comedyMoviesDeferred.await()
                val scifiMoviesRes = scifiMoviesDeferred.await()

                var hasError = false
                var errorMessage = ""

                val trendingItems = trendingRes.getOrElse {
                    hasError = true
                    errorMessage = it.localizedMessage ?: "Failed to fetch trending"
                    emptyList()
                }.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }.distinctBy { it.uniqueKey }

                val popularMoviesItems = popularMoviesRes.getOrElse {
                    hasError = true
                    errorMessage = it.localizedMessage ?: "Failed to fetch popular movies"
                    emptyList()
                }.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }.distinctBy { it.uniqueKey }

                val popularTvShowsItems = popularTvShowsRes.getOrElse {
                    hasError = true
                    errorMessage = it.localizedMessage ?: "Failed to fetch popular TV shows"
                    emptyList()
                }.map { ExploreItem.TvShowItem(it.id, it.name, it.poster_path, it.vote_average, it.first_air_date, it.backdrop_path, it.overview) }.distinctBy { it.uniqueKey }

                val popularActorsItems = popularActorsRes.getOrElse {
                    hasError = true
                    errorMessage = it.localizedMessage ?: "Failed to fetch actors"
                    emptyList()
                }.map { ExploreItem.ActorItem(it.id, it.name, it.profile_path, it.known_for_department) }.distinctBy { it.uniqueKey }

                val upcomingMoviesItems = upcomingMoviesRes.getOrElse {
                    hasError = true
                    errorMessage = it.localizedMessage ?: "Failed to fetch upcoming movies"
                    emptyList()
                }.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }.distinctBy { it.uniqueKey }

                val topRatedMoviesItems = topRatedMoviesRes.getOrElse {
                    hasError = true
                    errorMessage = it.localizedMessage ?: "Failed to fetch top rated movies"
                    emptyList()
                }.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }.distinctBy { it.uniqueKey }

                val topRatedTvShowsItems = topRatedTvShowsRes.getOrElse {
                    hasError = true
                    errorMessage = it.localizedMessage ?: "Failed to fetch top rated TV shows"
                    emptyList()
                }.map { ExploreItem.TvShowItem(it.id, it.name, it.poster_path, it.vote_average, it.first_air_date, it.backdrop_path, it.overview) }.distinctBy { it.uniqueKey }

                val actionMoviesItems = actionMoviesRes.getOrElse {
                    hasError = true
                    errorMessage = it.localizedMessage ?: "Failed to fetch action movies"
                    emptyList()
                }.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }.distinctBy { it.uniqueKey }

                val comedyMoviesItems = comedyMoviesRes.getOrElse {
                    hasError = true
                    errorMessage = it.localizedMessage ?: "Failed to fetch comedy movies"
                    emptyList()
                }.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }.distinctBy { it.uniqueKey }

                val scifiMoviesItems = scifiMoviesRes.getOrElse {
                    hasError = true
                    errorMessage = it.localizedMessage ?: "Failed to fetch sci-fi movies"
                    emptyList()
                }.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }.distinctBy { it.uniqueKey }

                if (hasError && trendingItems.isEmpty() && popularMoviesItems.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            forYouItems = forYouRes,
                            highlights = highlightsRes,
                            celebrities = celebritiesRes,
                            trending = trendingItems,
                            popularMovies = popularMoviesItems,
                            popularTvShows = popularTvShowsItems,
                            popularActors = popularActorsItems,
                            upcomingMovies = upcomingMoviesItems,
                            topRatedMovies = topRatedMoviesItems,
                            topRatedTvShows = topRatedTvShowsItems,
                            actionMovies = actionMoviesItems,
                            comedyMovies = comedyMoviesItems,
                            scifiMovies = scifiMoviesItems,
                            isLoading = false,
                            error = null,
                            categoryPages = ExploreCategory.values().associateWith { 1 },
                            hasReachedEnd = emptyMap()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    fun loadNextPage(category: ExploreCategory) {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || currentState.hasReachedEnd[category] == true) return

        val currentPage = currentState.categoryPages[category] ?: 1
        val nextPage = currentPage + 1

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            try {
                val result = when (category) {
                    ExploreCategory.POPULAR_MOVIES -> tmdbRepository.getPopularMovies(nextPage).map { list ->
                        list.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }
                    }
                    ExploreCategory.TRENDING_TV -> tmdbRepository.getTrendingTvShows(nextPage).map { list ->
                        list.map { ExploreItem.TvShowItem(it.id, it.name, it.poster_path, it.vote_average, it.first_air_date, it.backdrop_path, it.overview) }
                    }
                    ExploreCategory.POPULAR_ACTORS -> tmdbRepository.getPopularPeople(nextPage).map { list ->
                        list.map { ExploreItem.ActorItem(it.id, it.name, it.profile_path, it.known_for_department) }
                    }
                    ExploreCategory.UPCOMING_MOVIES -> tmdbRepository.getUpcomingMovies(nextPage).map { list ->
                        list.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }
                    }
                    ExploreCategory.TOP_RATED_MOVIES -> tmdbRepository.getTopRatedMovies(nextPage).map { list ->
                        list.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }
                    }
                    ExploreCategory.TOP_RATED_TV -> tmdbRepository.getTopRatedTvShows(nextPage).map { list ->
                        list.map { ExploreItem.TvShowItem(it.id, it.name, it.poster_path, it.vote_average, it.first_air_date, it.backdrop_path, it.overview) }
                    }
                    ExploreCategory.ACTION_MOVIES -> tmdbRepository.discoverMovies("28", nextPage).map { list ->
                        list.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }
                    }
                    ExploreCategory.COMEDY_MOVIES -> tmdbRepository.discoverMovies("35", nextPage).map { list ->
                        list.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }
                    }
                    ExploreCategory.SCIFI_MOVIES -> tmdbRepository.discoverMovies("878", nextPage).map { list ->
                        list.map { ExploreItem.MovieItem(it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview) }
                    }
                }

                result.onSuccess { newItems ->
                    _uiState.update { state ->
                        val currentList = when (category) {
                            ExploreCategory.POPULAR_MOVIES -> state.popularMovies
                            ExploreCategory.TRENDING_TV -> state.popularTvShows
                            ExploreCategory.POPULAR_ACTORS -> state.popularActors
                            ExploreCategory.UPCOMING_MOVIES -> state.upcomingMovies
                            ExploreCategory.TOP_RATED_MOVIES -> state.topRatedMovies
                            ExploreCategory.TOP_RATED_TV -> state.topRatedTvShows
                            ExploreCategory.ACTION_MOVIES -> state.actionMovies
                            ExploreCategory.COMEDY_MOVIES -> state.comedyMovies
                            ExploreCategory.SCIFI_MOVIES -> state.scifiMovies
                        }
                        val existingKeys = currentList.map { it.uniqueKey }.toSet()
                        val uniqueNewItems = newItems.filterNot { it.uniqueKey in existingKeys }.distinctBy { it.uniqueKey }

                        if (newItems.isEmpty() || uniqueNewItems.isEmpty()) {
                            state.copy(
                                isLoadingMore = false,
                                hasReachedEnd = state.hasReachedEnd + (category to true)
                            )
                        } else {
                            val updatedList = currentList + uniqueNewItems
                            state.copy(
                                isLoadingMore = false,
                                popularMovies = if (category == ExploreCategory.POPULAR_MOVIES) updatedList else state.popularMovies,
                                popularTvShows = if (category == ExploreCategory.TRENDING_TV) updatedList else state.popularTvShows,
                                popularActors = if (category == ExploreCategory.POPULAR_ACTORS) updatedList else state.popularActors,
                                upcomingMovies = if (category == ExploreCategory.UPCOMING_MOVIES) updatedList else state.upcomingMovies,
                                topRatedMovies = if (category == ExploreCategory.TOP_RATED_MOVIES) updatedList else state.topRatedMovies,
                                topRatedTvShows = if (category == ExploreCategory.TOP_RATED_TV) updatedList else state.topRatedTvShows,
                                actionMovies = if (category == ExploreCategory.ACTION_MOVIES) updatedList else state.actionMovies,
                                comedyMovies = if (category == ExploreCategory.COMEDY_MOVIES) updatedList else state.comedyMovies,
                                scifiMovies = if (category == ExploreCategory.SCIFI_MOVIES) updatedList else state.scifiMovies,
                                categoryPages = state.categoryPages + (category to nextPage)
                            )
                        }
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
}

data class TmdbExploreUiState(
    val forYouItems: List<ForYouFeaturedItem> = emptyList(),
    val highlights: List<EntertainmentHighlight> = emptyList(),
    val celebrities: List<CelebritySpotlight> = emptyList(),
    val trending: List<ExploreItem> = emptyList(),
    val popularMovies: List<ExploreItem> = emptyList(),
    val popularTvShows: List<ExploreItem> = emptyList(),
    val popularActors: List<ExploreItem> = emptyList(),
    val upcomingMovies: List<ExploreItem> = emptyList(),
    val topRatedMovies: List<ExploreItem> = emptyList(),
    val topRatedTvShows: List<ExploreItem> = emptyList(),
    val actionMovies: List<ExploreItem> = emptyList(),
    val comedyMovies: List<ExploreItem> = emptyList(),
    val scifiMovies: List<ExploreItem> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val activeSeeAllCategory: ExploreCategory? = null,
    val categoryPages: Map<ExploreCategory, Int> = ExploreCategory.values().associateWith { 1 },
    val hasReachedEnd: Map<ExploreCategory, Boolean> = emptyMap()
)
