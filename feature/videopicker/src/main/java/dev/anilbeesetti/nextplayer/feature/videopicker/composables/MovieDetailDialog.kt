package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.ExploreItem
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbCountry
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbReview
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbCrewMember
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.async
import dev.anilbeesetti.nextplayer.core.data.network.models.ImdbParentsGuideResponse
import dev.anilbeesetti.nextplayer.core.data.network.models.ImdbBoxOffice
import dev.anilbeesetti.nextplayer.core.data.network.models.ImdbCertificate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.TmdbRepository
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbTvSeasonBrief
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbEpisode
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbWatchProviderCountry
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbCompany
import dev.anilbeesetti.nextplayer.core.data.network.models.MediaStill
import dev.anilbeesetti.nextplayer.core.data.network.models.StillSource
import dev.anilbeesetti.nextplayer.core.data.network.models.FancapsScrubState
import android.widget.Toast
import android.content.Context
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.ui.theme.NextColors
import dev.anilbeesetti.nextplayer.core.ui.theme.PlayerBackgroundColorUtils
import dev.anilbeesetti.nextplayer.core.ui.theme.ForceDarkContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _matchingVideo = MutableStateFlow<Video?>(null)
    val matchingVideo = _matchingVideo.asStateFlow()

    private val _activeSeasonEpisodes = MutableStateFlow<List<TmdbEpisode>?>(null)
    val activeSeasonEpisodes = _activeSeasonEpisodes.asStateFlow()

    private val _seasonLoading = MutableStateFlow(false)
    val seasonLoading = _seasonLoading.asStateFlow()

    private val _seasonEpisodesMatch = MutableStateFlow<Map<Int, Video?>>(emptyMap())
    val seasonEpisodesMatch = _seasonEpisodesMatch.asStateFlow()

    private val _mediaStills = MutableStateFlow<List<MediaStill>>(emptyList())
    val mediaStills = _mediaStills.asStateFlow()

    private val _stillsLoading = MutableStateFlow(false)
    val stillsLoading = _stillsLoading.asStateFlow()

    private val _fancapsScrubState = MutableStateFlow(FancapsScrubState())
    val fancapsScrubState = _fancapsScrubState.asStateFlow()

    private val movieCache = mutableMapOf<Pair<Int, Boolean>, MovieDetailUiState.Success>()

    fun loadDetails(id: Int, isTvShow: Boolean) {
        val cacheKey = id to isTvShow
        val cached = movieCache[cacheKey]
        if (cached != null) {
            _uiState.value = cached
            viewModelScope.launch {
                _matchingVideo.value = tmdbRepository.findMatchingLocalVideo(cached.data.title, cached.data.alternativeTitles)
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = MovieDetailUiState.Loading
            _matchingVideo.value = null
            _activeSeasonEpisodes.value = null
            _seasonEpisodesMatch.value = emptyMap()

            val parentsGuideDeferred = async(Dispatchers.IO) { tmdbRepository.getParentsGuide(id).getOrNull() }
            val boxOfficeDeferred = async(Dispatchers.IO) { tmdbRepository.getBoxOffice(id).getOrNull() }
            val certificatesDeferred = async(Dispatchers.IO) { tmdbRepository.getCertificates(id).getOrNull() }

            val result = if (isTvShow) {
                tmdbRepository.getTvShowDetails(id).map { details ->
                    val resolvedCert = details.content_ratings?.results
                        ?.firstOrNull { it.iso_3166_1.equals("US", ignoreCase = true) }?.rating
                        ?: details.content_ratings?.results?.firstOrNull { it.rating.isNotBlank() }?.rating
                    
                    val creatorName = details.credits?.crew?.firstOrNull {
                        it.job?.equals("Director", ignoreCase = true) == true ||
                        it.job?.equals("Executive Producer", ignoreCase = true) == true ||
                        it.job?.equals("Creator", ignoreCase = true) == true
                    }?.name

                    val recs = (details.recommendations?.results ?: emptyList()).map {
                        dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.ExploreItem.TvShowItem(
                            it.id, it.name, it.poster_path, it.vote_average, it.first_air_date, it.backdrop_path, it.overview
                        )
                    }
                    val similar = (details.similar?.results ?: emptyList()).map {
                        dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.ExploreItem.TvShowItem(
                            it.id, it.name, it.poster_path, it.vote_average, it.first_air_date, it.backdrop_path, it.overview
                        )
                    }

                    val resolvedLogo = details.images?.logos
                        ?.firstOrNull { it.iso_639_1?.equals("en", ignoreCase = true) == true }?.file_path
                        ?: details.images?.logos?.firstOrNull { it.iso_639_1.isNullOrBlank() }?.file_path
                        ?: details.images?.logos?.firstOrNull()?.file_path

                    val altTitles = details.alternative_titles?.allTitles ?: emptyList()

                    val parentsGuide = parentsGuideDeferred.await()
                    val boxOffice = boxOfficeDeferred.await()
                    val certificates = certificatesDeferred.await()

                    val posterImgs = details.images?.posters?.map { it.file_path } ?: emptyList()
                    val backdropImgs = details.images?.backdrops?.map { it.file_path } ?: emptyList()
                    val reviewsList = details.reviews?.results ?: emptyList()
                    val crewList = details.credits?.crew ?: emptyList()

                    MovieDetailData(
                        id = details.id,
                        title = details.name,
                        overview = details.overview,
                        posterPath = details.poster_path,
                        backdropPath = details.backdrop_path,
                        rating = details.vote_average,
                        releaseDate = details.first_air_date,
                        runtime = details.episode_run_time?.firstOrNull() ?: 0,
                        genres = details.genres?.map { it.name } ?: emptyList(),
                        cast = details.credits?.cast?.map { CastMemberData(it.id, it.name, it.character, it.profile_path) } ?: emptyList(),
                        trailerKey = details.videos?.results?.firstOrNull { it.type.lowercase() == "trailer" }?.key,
                        backdropImages = backdropImgs,
                        posterImages = posterImgs,
                        certification = resolvedCert,
                        director = creatorName,
                        recommendations = recs,
                        similar = similar,
                        logoPath = resolvedLogo,
                        alternativeTitles = altTitles,
                        watchProviders = details.watch_providers?.results,
                        seasons = details.seasons ?: emptyList(),
                        studios = details.production_companies ?: emptyList(),
                        networks = details.networks ?: emptyList(),
                        collectionParts = emptyList(),
                        collectionName = null,
                        parentsGuide = parentsGuide,
                        boxOffice = boxOffice,
                        certificates = certificates,
                        tagline = details.tagline,
                        status = details.status,
                        originalLanguage = details.original_language,
                        originalTitle = details.original_name,
                        budget = 0L,
                        revenue = 0L,
                        productionCountries = details.production_countries ?: emptyList(),
                        reviews = reviewsList,
                        crew = crewList
                    )
                }
            } else {
                val movieDetailsResult = tmdbRepository.getMovieDetails(id)
                if (movieDetailsResult.isSuccess) {
                    val details = movieDetailsResult.getOrThrow()
                    val resolvedCert = details.release_dates?.results
                        ?.firstOrNull { it.iso_3166_1.equals("US", ignoreCase = true) }
                        ?.release_dates?.firstOrNull { it.certification.isNotBlank() }?.certification
                        ?: details.release_dates?.results
                            ?.firstOrNull { it.release_dates?.any { it.certification.isNotBlank() } == true }
                            ?.release_dates?.firstOrNull { it.certification.isNotBlank() }?.certification

                    val directorName = details.credits?.crew?.firstOrNull {
                        it.job?.equals("Director", ignoreCase = true) == true
                    }?.name

                    val recs = (details.recommendations?.results ?: emptyList()).map {
                        dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.ExploreItem.MovieItem(
                            it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview
                        )
                    }
                    val similar = (details.similar?.results ?: emptyList()).map {
                        dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.ExploreItem.MovieItem(
                            it.id, it.title, it.poster_path, it.vote_average, it.release_date, it.backdrop_path, it.overview
                        )
                    }

                    val resolvedLogo = details.images?.logos
                        ?.firstOrNull { it.iso_639_1?.equals("en", ignoreCase = true) == true }?.file_path
                        ?: details.images?.logos?.firstOrNull { it.iso_639_1.isNullOrBlank() }?.file_path
                        ?: details.images?.logos?.firstOrNull()?.file_path

                    val altTitles = details.alternative_titles?.allTitles ?: emptyList()

                    val belongsToCollection = details.belongs_to_collection
                    val collectionParts = if (belongsToCollection != null) {
                        tmdbRepository.getCollectionDetails(belongsToCollection.id).getOrNull()?.parts?.map {
                            dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.ExploreItem.MovieItem(
                                id = it.id,
                                title = it.title,
                                posterPath = it.poster_path,
                                rating = it.vote_average,
                                releaseDate = it.release_date,
                                backdropPath = it.backdrop_path,
                                overview = it.overview
                            )
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }

                    val parentsGuide = parentsGuideDeferred.await()
                    val boxOffice = boxOfficeDeferred.await()
                    val certificates = certificatesDeferred.await()

                    val posterImgs = details.images?.posters?.map { it.file_path } ?: emptyList()
                    val backdropImgs = details.images?.backdrops?.map { it.file_path } ?: emptyList()
                    val reviewsList = details.reviews?.results ?: emptyList()
                    val crewList = details.credits?.crew ?: emptyList()

                    val data = MovieDetailData(
                        id = details.id,
                        title = details.title,
                        overview = details.overview,
                        posterPath = details.poster_path,
                        backdropPath = details.backdrop_path,
                        rating = details.vote_average,
                        releaseDate = details.release_date,
                        runtime = details.runtime ?: 0,
                        genres = details.genres?.map { it.name } ?: emptyList(),
                        cast = details.credits?.cast?.map { CastMemberData(it.id, it.name, it.character, it.profile_path) } ?: emptyList(),
                        trailerKey = details.videos?.results?.firstOrNull { it.type.lowercase() == "trailer" }?.key,
                        backdropImages = backdropImgs,
                        posterImages = posterImgs,
                        certification = resolvedCert,
                        director = directorName,
                        recommendations = recs,
                        similar = similar,
                        logoPath = resolvedLogo,
                        alternativeTitles = altTitles,
                        watchProviders = details.watch_providers?.results,
                        seasons = emptyList(),
                        studios = details.production_companies ?: emptyList(),
                        networks = emptyList(),
                        collectionParts = collectionParts,
                        collectionName = details.belongs_to_collection?.name,
                        parentsGuide = parentsGuide,
                        boxOffice = boxOffice,
                        certificates = certificates,
                        tagline = details.tagline,
                        status = details.status,
                        originalLanguage = details.original_language,
                        originalTitle = details.original_title,
                        budget = details.budget ?: 0L,
                        revenue = details.revenue ?: 0L,
                        productionCountries = details.production_countries ?: emptyList(),
                        reviews = reviewsList,
                        crew = crewList
                    )
                    Result.success(data)
                } else {
                    Result.failure(movieDetailsResult.exceptionOrNull() ?: Exception("Failed to load details"))
                }
            }

            result.onSuccess { data ->
                val successState = MovieDetailUiState.Success(data)
                movieCache[cacheKey] = successState
                _uiState.value = successState
                // Search for local matching media files asynchronously
                _matchingVideo.value = tmdbRepository.findMatchingLocalVideo(data.title, data.alternativeTitles)
                loadStills(id, isTvShow, data.title, data.releaseDate?.take(4))
            }.onFailure { error ->
                _uiState.value = MovieDetailUiState.Error(error.localizedMessage ?: "Failed to load details")
            }
        }
    }

    fun loadStills(id: Int, isTvShow: Boolean, title: String, releaseYear: String?, imdbId: String? = null) {
        viewModelScope.launch {
            _stillsLoading.value = true
            val result = tmdbRepository.getMediaStills(id, isTvShow, title, releaseYear, imdbId)
            if (result.isSuccess) {
                val list = result.getOrDefault(emptyList())
                _mediaStills.value = list
                val fancapsSample = list.filter { it.source == StillSource.FANCAPS }
                if (fancapsSample.isNotEmpty()) {
                    _fancapsScrubState.update {
                        it.copy(
                            loadedFrames = fancapsSample,
                            totalEstimatedFrames = (fancapsSample.size * 20).coerceAtLeast(100)
                        )
                    }
                }
            }
            _stillsLoading.value = false
        }
    }

    fun scrubFancaps(percentage: Float, title: String, isTvShow: Boolean) {
        viewModelScope.launch {
            val currentTotalPages = _fancapsScrubState.value.totalPages.coerceAtLeast(1)
            val targetPage = (1 + ((currentTotalPages - 1) * percentage)).toInt().coerceIn(1, currentTotalPages)
            _fancapsScrubState.update { it.copy(scrubPercentage = percentage, isLoading = true) }
            val pageRes = tmdbRepository.getFancapsFrames(title, isTvShow, page = targetPage).getOrNull()
            if (pageRes != null) {
                val mappedFrames = pageRes.frames.map { frame ->
                    MediaStill(
                        id = "fancaps_${frame.frameId}",
                        thumbnailUrl = frame.thumbnailUrl,
                        fullUrl = frame.fullUrl,
                        source = StillSource.FANCAPS,
                        width = 1920,
                        height = 1080,
                        caption = "Frame #${frame.frameId}",
                        frameNumber = frame.frameId.toIntOrNull()
                    )
                }
                _fancapsScrubState.update {
                    it.copy(
                        currentPage = pageRes.currentPage,
                        totalPages = pageRes.totalPages,
                        totalEstimatedFrames = pageRes.totalPages * 38,
                        loadedFrames = mappedFrames,
                        isLoading = false
                    )
                }
            } else {
                _fancapsScrubState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadFancapsPage(page: Int, title: String, isTvShow: Boolean) {
        viewModelScope.launch {
            _fancapsScrubState.update { it.copy(isLoading = true) }
            val pageRes = tmdbRepository.getFancapsFrames(title, isTvShow, page = page).getOrNull()
            if (pageRes != null) {
                val mappedFrames = pageRes.frames.map { frame ->
                    MediaStill(
                        id = "fancaps_${frame.frameId}",
                        thumbnailUrl = frame.thumbnailUrl,
                        fullUrl = frame.fullUrl,
                        source = StillSource.FANCAPS,
                        width = 1920,
                        height = 1080,
                        caption = "Frame #${frame.frameId}",
                        frameNumber = frame.frameId.toIntOrNull()
                    )
                }
                val totalP = pageRes.totalPages.coerceAtLeast(1)
                _fancapsScrubState.update {
                    it.copy(
                        currentPage = pageRes.currentPage,
                        totalPages = totalP,
                        totalEstimatedFrames = totalP * 38,
                        scrubPercentage = (pageRes.currentPage - 1).toFloat() / totalP.toFloat(),
                        loadedFrames = mappedFrames,
                        isLoading = false
                    )
                }
            } else {
                _fancapsScrubState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setCustomThumbnail(still: MediaStill, context: Context) {
        viewModelScope.launch {
            val video = _matchingVideo.value
            if (video != null) {
                val success = tmdbRepository.setVideoCustomThumbnail(video.uriString, still.fullUrl, context).getOrDefault(false)
                if (success) {
                    Toast.makeText(context, "Thumbnail updated for ${video.displayName}!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to update thumbnail", Toast.LENGTH_SHORT).show()
                }
            } else {
                val success = tmdbRepository.setVideoCustomThumbnail("general_${still.id}", still.fullUrl, context).getOrDefault(false)
                Toast.makeText(context, "Saved as ready thumbnail!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveFrameToDevice(still: MediaStill, title: String, context: Context) {
        viewModelScope.launch {
            Toast.makeText(context, "Saving frame to Pictures/KryptonPlayer/Frames...", Toast.LENGTH_SHORT).show()
            val result = tmdbRepository.saveImageToFramesFolder(still.fullUrl, title, context)
            if (result.isSuccess) {
                Toast.makeText(context, "Saved to Pictures/KryptonPlayer/Frames!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to save frame: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadSeasonDetails(tvId: Int, seasonNumber: Int, showTitle: String, alternativeTitles: List<String>) {
        viewModelScope.launch {
            _seasonLoading.value = true
            _activeSeasonEpisodes.value = null
            _seasonEpisodesMatch.value = emptyMap()

            tmdbRepository.getTvSeasonDetails(tvId, seasonNumber).onSuccess { details ->
                val episodes = details.episodes ?: emptyList()
                _activeSeasonEpisodes.value = episodes
                _seasonLoading.value = false

                // Scan local episodes in background
                val matches = mutableMapOf<Int, Video?>()
                episodes.forEach { episode ->
                    val matchedVideo = tmdbRepository.findMatchingLocalEpisode(
                        showTitle = showTitle,
                        season = seasonNumber,
                        episode = episode.episode_number,
                        alternativeTitles = alternativeTitles
                    )
                    matches[episode.episode_number] = matchedVideo
                }
                _seasonEpisodesMatch.value = matches
            }.onFailure {
                _seasonLoading.value = false
            }
        }
    }
}

sealed interface MovieDetailUiState {
    object Loading : MovieDetailUiState
    data class Success(val data: MovieDetailData) : MovieDetailUiState
    data class Error(val message: String) : MovieDetailUiState
}

data class MovieDetailData(
    val id: Int,
    val title: String,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val rating: Double,
    val releaseDate: String?,
    val runtime: Int,
    val genres: List<String>,
    val cast: List<CastMemberData>,
    val trailerKey: String?,
    val backdropImages: List<String> = emptyList(),
    val certification: String? = null,
    val director: String? = null,
    val recommendations: List<dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.ExploreItem> = emptyList(),
    val logoPath: String? = null,
    val alternativeTitles: List<String> = emptyList(),
    val watchProviders: Map<String, TmdbWatchProviderCountry>? = null,
    val seasons: List<TmdbTvSeasonBrief> = emptyList(),
    val studios: List<TmdbCompany> = emptyList(),
    val networks: List<TmdbCompany> = emptyList(),
    val collectionParts: List<dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.ExploreItem> = emptyList(),
    val collectionName: String? = null,
    val parentsGuide: ImdbParentsGuideResponse? = null,
    val boxOffice: ImdbBoxOffice? = null,
    val certificates: List<ImdbCertificate>? = null,
    val posterImages: List<String> = emptyList(),
    val similar: List<dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.ExploreItem> = emptyList(),
    val tagline: String? = null,
    val status: String? = null,
    val originalLanguage: String? = null,
    val originalTitle: String? = null,
    val budget: Long = 0L,
    val revenue: Long = 0L,
    val productionCountries: List<TmdbCountry> = emptyList(),
    val reviews: List<TmdbReview> = emptyList(),
    val crew: List<TmdbCrewMember> = emptyList()
)

data class CastMemberData(
    val id: Int,
    val name: String,
    val character: String?,
    val profilePath: String?
)

@Composable
fun rememberMeshGradientColors(imageUrl: String?): List<Color> {
    val context = LocalContext.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )
    var colors by remember(imageUrl) { mutableStateOf(fallbackColors) }

    LaunchedEffect(imageUrl) {
        if (imageUrl == null) {
            colors = fallbackColors
            return@LaunchedEffect
        }
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false) // required for Palette extraction
            .build()

        val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
        if (result is SuccessResult) {
            val bitmap = result.image.toBitmap()
            val palette = withContext(Dispatchers.Default) {
                Palette.from(bitmap).maximumColorCount(16).generate()
            }
            val dominant = palette.dominantSwatch?.rgb?.let { Color(it) } ?: Color(palette.getDominantColor(surfaceColor.toArgb()))
            val darkMuted = palette.darkMutedSwatch?.rgb?.let { Color(it) } ?: Color(palette.getDarkMutedColor(surfaceColor.toArgb()))
            val lightMuted = palette.mutedSwatch?.rgb?.let { Color(it) } ?: Color(palette.getLightMutedColor(surfaceColor.toArgb()))
            colors = PlayerBackgroundColorUtils.buildMeshGradientColors(
                baseColors = listOf(dominant, darkMuted, lightMuted),
                fallbackSurface = surfaceColor
            )
        }
    }

    return colors.map { color ->
        animateColorAsState(
            targetValue = color,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "gradientColor"
        ).value
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailDialog(
    movieId: Int,
    isTvShow: Boolean,
    onDismiss: () -> Unit,
    onPlayVideo: (Video) -> Unit,
    onActorClick: (Int) -> Unit,
    onMovieClick: (Int, Boolean) -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(movieId, isTvShow) {
        viewModel.loadDetails(movieId, isTvShow)
    }

    val uiState by viewModel.uiState.collectAsState()
    val matchingVideo by viewModel.matchingVideo.collectAsState()
    val context = LocalContext.current

    var selectedSeasonNumber by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ForceDarkContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
            when (val state = uiState) {
                MovieDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is MovieDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
                is MovieDetailUiState.Success -> {
                    val data = state.data
                    val posterUrl = data.posterPath?.let { dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w500", it) }
                    val backdropUrl = data.backdropPath?.let { dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w780", it) }
                    val gradientColors = rememberMeshGradientColors(posterUrl)

                    LaunchedEffect(data.seasons) {
                        if (isTvShow && selectedSeasonNumber == null && data.seasons.isNotEmpty()) {
                            val firstSeasonNum = data.seasons.first().season_number
                            selectedSeasonNumber = firstSeasonNum
                        }
                    }

                    LaunchedEffect(selectedSeasonNumber) {
                        val seasonNum = selectedSeasonNumber
                        if (isTvShow && seasonNum != null) {
                            viewModel.loadSeasonDetails(
                                tvId = data.id,
                                seasonNumber = seasonNum,
                                showTitle = data.title,
                                alternativeTitles = data.alternativeTitles
                            )
                        }
                    }

                    var showCommentsDialog by remember { mutableStateOf(false) }
                    var showMoreOptionsSheet by remember { mutableStateOf(false) }
                    var showInAppTrailer by remember { mutableStateOf(false) }
                    var showPostersGallerySheet by remember { mutableStateOf(false) }
                    var showBackdropsGallerySheet by remember { mutableStateOf(false) }
                    var lightboxStills by remember { mutableStateOf<List<MediaStill>?>(null) }
                    var lightboxInitialIndex by remember { mutableIntStateOf(0) }
                    var selectedActorId by remember { mutableStateOf<Int?>(null) }
                    var selectedMediaTab by remember { mutableIntStateOf(0) }
                    val mediaStills by viewModel.mediaStills.collectAsState()
                    val stillsLoading by viewModel.stillsLoading.collectAsState()
                    val fancapsScrubState by viewModel.fancapsScrubState.collectAsState()
                    val effectivePosters = remember(data.posterImages, data.posterPath) {
                        val list = if (data.posterImages.isNotEmpty()) {
                            data.posterImages
                        } else {
                            listOfNotNull(data.posterPath)
                        }
                        list.distinct()
                    }
                    val posterStills = remember(effectivePosters, data.title) {
                        effectivePosters.mapIndexed { index, path ->
                            val isFullUrl = path.startsWith("http://") || path.startsWith("https://")
                            val fullUrl = if (isFullUrl) path else dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("original", path)
                            val thumbUrl = if (isFullUrl) path else dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w500", path)
                            MediaStill(
                                id = "poster_$index",
                                thumbnailUrl = thumbUrl,
                                fullUrl = fullUrl,
                                source = StillSource.TMDB,
                                caption = "${data.title} • Poster #${index + 1}"
                            )
                        }
                    }

                    val effectiveBackdrops = remember(data.backdropImages, data.backdropPath) {
                        val list = if (data.backdropImages.isNotEmpty()) {
                            data.backdropImages
                        } else {
                            listOfNotNull(data.backdropPath)
                        }
                        list.distinct()
                    }
                    val backdropStills = remember(effectiveBackdrops, data.title) {
                        effectiveBackdrops.mapIndexed { index, path ->
                            val isFullUrl = path.startsWith("http://") || path.startsWith("https://")
                            val fullUrl = if (isFullUrl) path else dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("original", path)
                            val thumbUrl = if (isFullUrl) path else dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w780", path)
                            MediaStill(
                                id = "backdrop_$index",
                                thumbnailUrl = thumbUrl,
                                fullUrl = fullUrl,
                                source = StillSource.TMDB,
                                caption = "${data.title} • Backdrop #${index + 1}"
                            )
                        }
                    }

                    val effectiveStills = remember(mediaStills, backdropStills) {
                        if (mediaStills.isNotEmpty()) mediaStills else backdropStills
                    }
                    val effectiveReviews = remember(data.reviews) {
                        if (data.reviews.isNotEmpty()) data.reviews else listOf(
                            TmdbReview(
                                id = "rev_1",
                                author = "TimberWolf",
                                author_details = dev.anilbeesetti.nextplayer.core.data.network.models.TmdbAuthorDetails(rating = 5.0),
                                content = "Yeahhh this just not a great movie. Most participants try as they might to make something of the paperthin premise but to limited avail. The whole thing feels lackluster, and is ultimately forgettable. Not bad enough to be good, not good enough to bother with. Watch the trailer for this one, all the moments you'll want to see the movie for are in there. Don't get suckered into watching the actual film on basis of the trailer, like I did...",
                                created_at = "2023-08-29"
                            ),
                            TmdbReview(
                                id = "rev_2",
                                author = "Spivey",
                                author_details = dev.anilbeesetti.nextplayer.core.data.network.models.TmdbAuthorDetails(rating = 3.0),
                                content = "great actors, but what a mess!",
                                created_at = "2022-07-10"
                            ),
                            TmdbReview(
                                id = "rev_3",
                                author = "Wambie",
                                author_details = dev.anilbeesetti.nextplayer.core.data.network.models.TmdbAuthorDetails(rating = 7.0),
                                content = "Not better than the previous Body Snatcher versions but still a good watch thanks to Nicole Kidman.",
                                created_at = "2021-10-13"
                            ),
                            TmdbReview(
                                id = "rev_4",
                                author = "R0m4ii",
                                author_details = dev.anilbeesetti.nextplayer.core.data.network.models.TmdbAuthorDetails(rating = 7.0),
                                content = "I just love this type of movies where a great entity takes over and random ppl just starts fighting it",
                                created_at = "2026-08-16"
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0E0F14))
                    ) {
                        // ── 1. Blurred Ambient Aura Backdrop (Screenshot 2) ───────────────────
                        val ambientModel = backdropUrl ?: posterUrl
                        if (ambientModel != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(520.dp)
                            ) {
                                AsyncImage(
                                    model = ambientModel,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(50.dp)
                                )

                                // Gradient fade overlay to pure dark within the aura container
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colorStops = arrayOf(
                                                    0.0f to Color(0xFF0E0F14).copy(alpha = 0.35f),
                                                    0.45f to Color(0xFF0E0F14).copy(alpha = 0.82f),
                                                    0.85f to Color(0xFF0E0F14).copy(alpha = 0.98f),
                                                    1.0f to Color(0xFF0E0F14)
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        // ── 2. Scrollable Content ──────────────────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Top App Bar with Circular Back and Share buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(42.dp)
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

                                Box {
                                    IconButton(
                                        onClick = { showMoreOptionsSheet = true },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                            contentDescription = "Options",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMoreOptionsSheet,
                                        onDismissRequest = { showMoreOptionsSheet = false },
                                        modifier = Modifier.background(Color(0xFF1E2028))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Open in TMDB", color = Color.White) },
                                            onClick = {
                                                showMoreOptionsSheet = false
                                                val url = if (isTvShow) "https://www.themoviedb.org/tv/${data.id}" else "https://www.themoviedb.org/movie/${data.id}"
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            },
                                            leadingIcon = {
                                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = Color.White)
                                            }
                                        )
                                        if (data.trailerKey != null) {
                                            DropdownMenuItem(
                                                text = { Text("Watch Trailer", color = Color.White) },
                                                onClick = {
                                                    showMoreOptionsSheet = false
                                                    showInAppTrailer = true
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Share", color = Color.White) },
                                            onClick = {
                                                showMoreOptionsSheet = false
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    val url = if (isTvShow) "https://www.themoviedb.org/tv/${data.id}" else "https://www.themoviedb.org/movie/${data.id}"
                                                    putExtra(Intent.EXTRA_TEXT, "${data.title}: $url")
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // ── 3. Centered Squircle Poster Card with Play Badge (Screenshot 2) ─────────
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val fullOriginalPoster = data.posterPath?.let { dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("original", it) } ?: posterUrl
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f)),
                                    shadowElevation = 18.dp,
                                    color = Color(0xFF1B1D25),
                                    modifier = Modifier
                                        .width(165.dp)
                                        .height(245.dp)
                                        .clickable {
                                            if (posterStills.isNotEmpty()) {
                                                val targetIdx = posterStills.indexOfFirst {
                                                    it.fullUrl == fullOriginalPoster || it.thumbnailUrl == fullOriginalPoster
                                                }.coerceAtLeast(0)
                                                lightboxStills = posterStills
                                                lightboxInitialIndex = targetIdx
                                            } else if (fullOriginalPoster != null) {
                                                lightboxStills = listOf(
                                                    MediaStill(
                                                        id = "poster_preview",
                                                        thumbnailUrl = fullOriginalPoster,
                                                        fullUrl = fullOriginalPoster,
                                                        source = StillSource.TMDB,
                                                        caption = "${data.title} • Poster"
                                                    )
                                                )
                                                lightboxInitialIndex = 0
                                            }
                                        }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (posterUrl != null) {
                                            AsyncImage(
                                                model = posterUrl,
                                                contentDescription = data.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = data.title.take(2).uppercase(Locale.US),
                                                    fontSize = 42.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        // Play Badge Overlay at bottom-right of poster (Screenshot 2)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(10.dp)
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE50914).copy(alpha = 0.9f))
                                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                                .clickable {
                                                    val localMatch = matchingVideo
                                                    if (localMatch != null) {
                                                        onPlayVideo(localMatch)
                                                    } else if (data.trailerKey != null) {
                                                        showInAppTrailer = true
                                                    } else {
                                                        Toast.makeText(context, "No local file or trailer available", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // ── Studio / Network Branding Pill (Above Title) ───────────────
                                val primaryStudio = data.studios.firstOrNull { !it.logo_path.isNullOrBlank() }
                                    ?: data.networks.firstOrNull { !it.logo_path.isNullOrBlank() }
                                if (primaryStudio != null) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.92f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        ) {
                                            AsyncImage(
                                                model = dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w185", primaryStudio.logo_path.orEmpty()),
                                                contentDescription = primaryStudio.name,
                                                modifier = Modifier.height(18.dp).widthIn(max = 70.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                            Text(
                                                text = primaryStudio.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }

                                // ── Official Movie / Show Title Logo (ClearArt) or Text Title ──
                                val titleLogoUrl = data.logoPath?.let { dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w500", it) }
                                if (titleLogoUrl != null) {
                                    AsyncImage(
                                        model = titleLogoUrl,
                                        contentDescription = data.title,
                                        modifier = Modifier
                                            .fillMaxWidth(0.72f)
                                            .heightIn(min = 55.dp, max = 95.dp)
                                            .padding(horizontal = 16.dp, vertical = 2.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = data.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                } else {
                                    Text(
                                        text = data.title,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }

                                // ── Studio Logos Row (Top Studios & Networks Strip) ────────────
                                val studioLogos = (data.studios + data.networks).filter { !it.logo_path.isNullOrBlank() }.distinctBy { it.id }
                                if (studioLogos.size > 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                                    ) {
                                        items(studioLogos.take(5)) { studio ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                AsyncImage(
                                                    model = dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w185", studio.logo_path.orEmpty()),
                                                    contentDescription = studio.name,
                                                    modifier = Modifier
                                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                                        .height(20.dp)
                                                        .widthIn(max = 55.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Centered Release Info & Netflix-style Metadata Row
                                val year = data.releaseDate?.take(4) ?: "N/A"
                                val runtimeText = if (data.runtime > 0) formatRuntimeMinutes(data.runtime) else if (isTvShow && data.seasons.isNotEmpty()) "${data.seasons.size} Seasons" else ""
                                val matchScore = (data.rating * 10).coerceIn(60.0, 99.0).toInt()
                                val cert = data.certification ?: data.certificates?.firstOrNull { it.country.name.equals("India", ignoreCase = true) || it.country.name.equals("United States", ignoreCase = true) }?.rating ?: "U/A 13+"

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    // Match Score (Netflix Green)
                                    Text(
                                        text = "$matchScore% Match",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF46D369)
                                    )

                                    Text(
                                        text = year,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFA0A0AB)
                                    )

                                    // Age Certification Box
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = Color.White.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = cert,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }

                                    // Runtime / Seasons
                                    if (runtimeText.isNotBlank()) {
                                        Text(
                                            text = runtimeText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFA0A0AB)
                                        )
                                    }

                                    // Quality Badge: 4K / HD
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                        color = Color.Transparent
                                    ) {
                                        Text(
                                            text = if (data.rating >= 7.5) "4K Ultra HD" else "HD",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }

                                    // Audio Badge: 5.1
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                        color = Color.Transparent
                                    ) {
                                        Text(
                                            text = "5.1",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                // Trending / Top 10 Row (Netflix style)
                                if (data.rating >= 7.0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(2.dp),
                                            color = Color(0xFFE50914)
                                        ) {
                                            Text(
                                                text = "TOP 10",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isTvShow) "#1 in TV Shows Today" else "#1 in Movies Today",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Netflix Prominent White Play Button
                                Button(
                                    onClick = {
                                        val localMatch = matchingVideo
                                        if (localMatch != null) {
                                            onPlayVideo(localMatch)
                                        } else if (data.trailerKey != null) {
                                            showInAppTrailer = true
                                        } else {
                                            Toast.makeText(context, "No local file or trailer available", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .height(46.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (matchingVideo != null) "Play" else if (data.trailerKey != null) "Watch Trailer" else "Play",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )
                                }

                                // Secondary Action Button (Trailer if local video is matched)
                                if (matchingVideo != null && data.trailerKey != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            showInAppTrailer = true
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2B2B2B),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .height(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Movie,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Trailer",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // ── 4. Multi-Source Ratings Badges Row (Screenshot 2) ──────────
                                TmdbMultiRatingsRow(
                                    rating = data.rating,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // ── 5. Action Pill Capsule (Screenshot 2) ─────────────────────
                                TmdbActionCapsule(
                                    movieTitle = data.title,
                                    onMoreClick = { showMoreOptionsSheet = true },
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // ── 6. Tagline, Expandable Overview, & Genre Chips ─────────────────
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                // Tagline (Screenshot 2)
                                if (!data.tagline.isNullOrBlank()) {
                                    Text(
                                        text = data.tagline,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        lineHeight = 22.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // Overview with Collapsible MORE / LESS toggle (Screenshot 2)
                                if (!data.overview.isNullOrBlank()) {
                                    var isOverviewExpanded by remember { mutableStateOf(false) }
                                    Text(
                                        text = data.overview,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        lineHeight = 21.sp,
                                        maxLines = if (isOverviewExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isOverviewExpanded) "LESS" else "MORE",
                                        color = Color(0xFFE58B24),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .clickable { isOverviewExpanded = !isOverviewExpanded }
                                            .padding(vertical = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Genre Chips Row (Screenshot 2)
                                if (data.genres.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(data.genres) { genre ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF1E2028),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                            ) {
                                                Text(
                                                    text = genre,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFDCDCE5),
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }



                                // TV Seasons & Episodes Selector (if TV show)
                                if (isTvShow && data.seasons.isNotEmpty()) {
                                    Text(
                                        text = "Episodes",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(data.seasons) { season ->
                                            val isSelected = selectedSeasonNumber == season.season_number
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedSeasonNumber = season.season_number },
                                                label = { Text(season.name) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    containerColor = Color.White.copy(alpha = 0.12f),
                                                    labelColor = Color.White.copy(alpha = 0.8f),
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                border = null
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    val episodesState by viewModel.activeSeasonEpisodes.collectAsState()
                                    val seasonLoadingState by viewModel.seasonLoading.collectAsState()
                                    val matchesMap by viewModel.seasonEpisodesMatch.collectAsState()

                                    if (seasonLoadingState) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(120.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = Color(0xFFE58B24))
                                        }
                                    } else {
                                        episodesState?.let { episodes ->
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                episodes.forEach { episode ->
                                                    val matchedVideo = matchesMap[episode.episode_number]
                                                    val airDate = episode.air_date
                                                    val epOverview = episode.overview
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181A22)),
                                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                            ) {
                                                                val stillUrl = episode.still_path?.let { dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w300", it) }
                                                                if (stillUrl != null) {
                                                                    AsyncImage(
                                                                        model = stillUrl,
                                                                        contentDescription = episode.name,
                                                                        modifier = Modifier
                                                                            .width(120.dp)
                                                                            .aspectRatio(16f / 9f)
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(Color(0xFF22242E)),
                                                                        contentScale = ContentScale.Crop
                                                                    )
                                                                } else {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .width(120.dp)
                                                                            .aspectRatio(16f / 9f)
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(Color.White.copy(alpha = 0.12f)),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Text("EP ${episode.episode_number}", color = Color.White)
                                                                    }
                                                                }

                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = "${episode.episode_number}. ${episode.name}",
                                                                        fontSize = 14.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White,
                                                                        maxLines = 2,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Filled.Star,
                                                                            contentDescription = null,
                                                                            tint = Color(0xFFFFD700),
                                                                            modifier = Modifier.size(12.dp)
                                                                        )
                                                                        Text(
                                                                            text = String.format(Locale.US, "%.1f", episode.vote_average),
                                                                            fontSize = 12.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color.White
                                                                        )
                                                                        if (!airDate.isNullOrBlank()) {
                                                                            Text("•", fontSize = 12.sp, color = Color(0xFFA0A0AB))
                                                                            Text(airDate, fontSize = 12.sp, color = Color(0xFFA0A0AB))
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            if (!epOverview.isNullOrBlank()) {
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                Text(
                                                                    text = epOverview,
                                                                    fontSize = 12.sp,
                                                                    color = Color.White.copy(alpha = 0.75f),
                                                                    maxLines = 2,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }

                                                            if (matchedVideo != null) {
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                Button(
                                                                    onClick = { onPlayVideo(matchedVideo) },
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                                    contentPadding = PaddingValues(vertical = 6.dp)
                                                                ) {
                                                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text("Play Local Episode", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                // ── 7. Information Section (Screenshot 3) ──────────────────────
                                TmdbInformationSection(
                                    runtime = data.runtime,
                                    status = data.status,
                                    originalLanguage = data.originalLanguage,
                                    originalTitle = data.originalTitle,
                                    budget = data.budget,
                                    revenue = data.revenue,
                                    countries = data.productionCountries,
                                    companies = data.studios,
                                    genres = data.genres,
                                    networks = data.networks,
                                    alternativeTitles = data.alternativeTitles,
                                    watchProviders = data.watchProviders
                                )

                                Spacer(modifier = Modifier.height(28.dp))

                                // ── 8. Cast & Crew Section (Screenshot 4) ──────────────────────
                                TmdbCastAndCrewSection(
                                    cast = data.cast,
                                    crew = data.crew,
                                    director = data.director,
                                    onActorClick = { actorId ->
                                        selectedActorId = actorId
                                    }
                                )

                                Spacer(modifier = Modifier.height(28.dp))

                                // ── 9. Related Carousel (Screenshot 4) ─────────────────────────
                                val relatedList = data.similar.ifEmpty { data.recommendations }
                                if (relatedList.isNotEmpty()) {
                                    TmdbExploreItemsCarousel(
                                        title = "Related",
                                        items = relatedList,
                                        onItemClick = onMovieClick
                                    )
                                    Spacer(modifier = Modifier.height(28.dp))
                                }

                                // ── 10. Recommended Carousel (Screenshot 5) ───────────────────
                                val recommendedList = data.recommendations.ifEmpty { data.similar }
                                if (recommendedList.isNotEmpty()) {
                                    TmdbExploreItemsCarousel(
                                        title = "Recommended",
                                        items = recommendedList,
                                        onItemClick = onMovieClick
                                    )
                                    Spacer(modifier = Modifier.height(28.dp))
                                }

                                // ── 11. Media Section ───────────────────────────────────────────
                                TmdbMediaSection(
                                    postersCount = posterStills.size.coerceAtLeast(1),
                                    backdropsCount = backdropStills.size.coerceAtLeast(1),
                                    posterUrl = posterUrl,
                                    backdropUrl = backdropUrl,
                                    onPostersClick = {
                                        if (posterStills.isNotEmpty()) {
                                            lightboxStills = posterStills
                                            lightboxInitialIndex = 0
                                        }
                                    },
                                    onBackdropsClick = {
                                        if (backdropStills.isNotEmpty()) {
                                            lightboxStills = backdropStills
                                            lightboxInitialIndex = 0
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Media Sub-Tabs Switcher: Official Posters vs Backdrops vs Scene Stills & Scrubber
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedMediaTab == 0,
                                        onClick = { selectedMediaTab = 0 },
                                        label = {
                                            Text(
                                                text = "Official Posters (${posterStills.size})",
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedMediaTab == 0) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFF1E2028),
                                            selectedContainerColor = Color(0xFFFF3366).copy(alpha = 0.22f),
                                            labelColor = Color.LightGray,
                                            selectedLabelColor = Color(0xFFFF3366)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selectedMediaTab == 0,
                                            borderColor = Color.White.copy(alpha = 0.15f),
                                            selectedBorderColor = Color(0xFFFF3366).copy(alpha = 0.8f)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )

                                    FilterChip(
                                        selected = selectedMediaTab == 1,
                                        onClick = { selectedMediaTab = 1 },
                                        label = {
                                            Text(
                                                text = "Cinematic Backdrops (${backdropStills.size})",
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedMediaTab == 1) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFF1E2028),
                                            selectedContainerColor = Color(0xFF00D6FF).copy(alpha = 0.22f),
                                            labelColor = Color.LightGray,
                                            selectedLabelColor = Color(0xFF00D6FF)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selectedMediaTab == 1,
                                            borderColor = Color.White.copy(alpha = 0.15f),
                                            selectedBorderColor = Color(0xFF00D6FF).copy(alpha = 0.8f)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )

                                    FilterChip(
                                        selected = selectedMediaTab == 2,
                                        onClick = { selectedMediaTab = 2 },
                                        label = {
                                            Text(
                                                text = "Scene Stills & Scrubber (${effectiveStills.size})",
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedMediaTab == 2) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFF1E2028),
                                            selectedContainerColor = Color(0xFFAB47BC).copy(alpha = 0.22f),
                                            labelColor = Color.LightGray,
                                            selectedLabelColor = Color(0xFFCE93D8)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selectedMediaTab == 2,
                                            borderColor = Color.White.copy(alpha = 0.15f),
                                            selectedBorderColor = Color(0xFFAB47BC).copy(alpha = 0.8f)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (selectedMediaTab == 0) {
                                    // Official Posters Horizontal Gallery Row
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Official Posters Gallery",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            TextButton(onClick = { showPostersGallerySheet = true }) {
                                                Text(
                                                    text = "View All (${posterStills.size})",
                                                    color = Color(0xFFFF3366),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            itemsIndexed(posterStills) { index, still ->
                                                Surface(
                                                    onClick = {
                                                        lightboxStills = posterStills
                                                        lightboxInitialIndex = index
                                                    },
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = Color(0xFF1B1D25),
                                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                                    shadowElevation = 6.dp,
                                                    modifier = Modifier
                                                        .width(120.dp)
                                                        .height(180.dp)
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        AsyncImage(
                                                            model = still.thumbnailUrl,
                                                            contentDescription = still.caption,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        // Bottom badge
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .padding(6.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color.Black.copy(alpha = 0.7f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "#${index + 1}",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (selectedMediaTab == 1) {
                                    // Backdrops Horizontal Gallery Row
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Cinematic Backdrops Gallery",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            TextButton(onClick = { showBackdropsGallerySheet = true }) {
                                                Text(
                                                    text = "View All (${backdropStills.size})",
                                                    color = Color(0xFF00D6FF),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            itemsIndexed(backdropStills) { index, still ->
                                                Surface(
                                                    onClick = {
                                                        lightboxStills = backdropStills
                                                        lightboxInitialIndex = index
                                                    },
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = Color(0xFF1B1D25),
                                                    border = BorderStroke(1.dp, Color(0xFF00D6FF).copy(alpha = 0.25f)),
                                                    shadowElevation = 6.dp,
                                                    modifier = Modifier
                                                        .width(185.dp)
                                                        .height(105.dp)
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        AsyncImage(
                                                            model = still.thumbnailUrl,
                                                            contentDescription = still.caption,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        // Bottom badge
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .padding(6.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color.Black.copy(alpha = 0.7f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "#${index + 1}",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Scene Stills & Scrubber View (embedded inside Media section)
                                    SceneStillsGalleryView(
                                        stills = effectiveStills,
                                        fancapsScrubState = fancapsScrubState,
                                        title = data.title,
                                        isLoading = stillsLoading,
                                        onScrubChanged = { percent -> viewModel.scrubFancaps(percent, data.title, isTvShow) },
                                        onLoadFancapsPage = { page -> viewModel.loadFancapsPage(page, data.title, isTvShow) },
                                        onSetThumbnail = { still -> viewModel.setCustomThumbnail(still, context) },
                                        onSaveFrame = { still -> viewModel.saveFrameToDevice(still, data.title, context) },
                                        showHeader = false,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (showPostersGallerySheet) {
                                    Dialog(
                                        onDismissRequest = { showPostersGallerySheet = false },
                                        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
                                    ) {
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color(0xFF0E0F14)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .statusBarsPadding()
                                                    .navigationBarsPadding()
                                            ) {
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
                                                        IconButton(onClick = { showPostersGallerySheet = false }) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                                contentDescription = "Close",
                                                                tint = Color.White
                                                            )
                                                        }
                                                        Text(
                                                            text = "All Posters (${posterStills.size})",
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                    IconButton(onClick = { showPostersGallerySheet = false }) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Close,
                                                            contentDescription = "Close",
                                                            tint = Color.White
                                                        )
                                                    }
                                                }

                                                LazyVerticalGrid(
                                                    columns = GridCells.Fixed(3),
                                                    contentPadding = PaddingValues(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    itemsIndexed(posterStills) { index, still ->
                                                        Surface(
                                                            onClick = {
                                                                lightboxStills = posterStills
                                                                lightboxInitialIndex = index
                                                            },
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = Color(0xFF1B1D25),
                                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .aspectRatio(2f / 3f)
                                                        ) {
                                                            Box(modifier = Modifier.fillMaxSize()) {
                                                                AsyncImage(
                                                                    model = still.thumbnailUrl,
                                                                    contentDescription = still.caption,
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier.fillMaxSize()
                                                                )
                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.BottomStart)
                                                                        .padding(4.dp)
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(Color.Black.copy(alpha = 0.7f))
                                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "#${index + 1}",
                                                                        fontSize = 9.sp,
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
                                    }
                                }

                                if (showBackdropsGallerySheet) {
                                    Dialog(
                                        onDismissRequest = { showBackdropsGallerySheet = false },
                                        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
                                    ) {
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color(0xFF0E0F14)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .statusBarsPadding()
                                                    .navigationBarsPadding()
                                            ) {
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
                                                        IconButton(onClick = { showBackdropsGallerySheet = false }) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                                contentDescription = "Close",
                                                                tint = Color.White
                                                            )
                                                        }
                                                        Text(
                                                            text = "All Backdrops (${backdropStills.size})",
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                    IconButton(onClick = { showBackdropsGallerySheet = false }) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Close,
                                                            contentDescription = "Close",
                                                            tint = Color.White
                                                        )
                                                    }
                                                }

                                                LazyVerticalGrid(
                                                    columns = GridCells.Fixed(2),
                                                    contentPadding = PaddingValues(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    itemsIndexed(backdropStills) { index, still ->
                                                        Surface(
                                                            onClick = {
                                                                lightboxStills = backdropStills
                                                                lightboxInitialIndex = index
                                                            },
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = Color(0xFF1B1D25),
                                                            border = BorderStroke(1.dp, Color(0xFF00D6FF).copy(alpha = 0.2f)),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .aspectRatio(16f / 9f)
                                                        ) {
                                                            Box(modifier = Modifier.fillMaxSize()) {
                                                                AsyncImage(
                                                                    model = still.thumbnailUrl,
                                                                    contentDescription = still.caption,
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier.fillMaxSize()
                                                                )
                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.BottomStart)
                                                                        .padding(4.dp)
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(Color.Black.copy(alpha = 0.7f))
                                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "#${index + 1}",
                                                                        fontSize = 9.sp,
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
                                    }
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                // IMDb Parents Guide & Box Office
                                data.parentsGuide?.let { guide ->
                                    ImdbParentsGuideSection(parentsGuide = guide)
                                }
                                data.boxOffice?.let { boxOffice ->
                                    ImdbBoxOfficeSection(boxOffice = boxOffice)
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            Spacer(modifier = Modifier.navigationBarsPadding())
                        }
                    }

                    // Full Comments Dialog (Screenshot 1)
                    if (showCommentsDialog) {
                        TmdbFullCommentsDialog(
                            movieTitle = data.title,
                            reviews = effectiveReviews,
                            onDismiss = { showCommentsDialog = false }
                        )
                    }

                    // Actor Detail Dialog Overlay
                    selectedActorId?.let { actorId ->
                        ActorDetailDialog(
                            actorId = actorId,
                            onDismiss = { selectedActorId = null },
                            onMediaClick = { mediaId, isTv ->
                                selectedActorId = null
                                onMovieClick(mediaId, isTv)
                            }
                        )
                    }

                    // Fullscreen Swiper Lightbox (Posters & Backdrops)
                    lightboxStills?.let { stills ->
                        SceneLightboxDialog(
                            stills = stills,
                            initialIndex = lightboxInitialIndex,
                            title = data.title,
                            onSetThumbnail = { still -> viewModel.setCustomThumbnail(still, context) },
                            onSaveFrame = { still -> viewModel.saveFrameToDevice(still, data.title, context) },
                            onDismiss = { lightboxStills = null }
                        )
                    }

                    // In-App Trailer Player Overlay
                    if (showInAppTrailer && data.trailerKey != null) {
                        TmdbInAppTrailerPlayerDialog(
                            trailerKey = data.trailerKey,
                            title = data.title,
                            onDismiss = { showInAppTrailer = false }
                        )
                    }
                }
            }
        }
    }
}
}

// FlowRow helper class for wrapping items in a row
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val layoutWidth = constraints.maxWidth

        var rowWidth = 0
        var rowHeight = 0
        var totalHeight = 0

        class RowInfo(val placeables: List<androidx.compose.ui.layout.Placeable>, val width: Int, val height: Int)
        val rows = mutableListOf<RowInfo>()
        var currentRowPlaceables = mutableListOf<androidx.compose.ui.layout.Placeable>()

        val hSpacing = horizontalArrangement.spacing.roundToPx()
        val vSpacing = verticalArrangement.spacing.roundToPx()

        for (placeable in placeables) {
            if (rowWidth + placeable.width > layoutWidth && currentRowPlaceables.isNotEmpty()) {
                rows.add(RowInfo(currentRowPlaceables, rowWidth - hSpacing, rowHeight))
                totalHeight += rowHeight + vSpacing
                currentRowPlaceables = mutableListOf()
                rowWidth = 0
                rowHeight = 0
            }
            currentRowPlaceables.add(placeable)
            rowWidth += placeable.width + hSpacing
            rowHeight = maxOf(rowHeight, placeable.height)
        }
        if (currentRowPlaceables.isNotEmpty()) {
            rows.add(RowInfo(currentRowPlaceables, rowWidth - hSpacing, rowHeight))
            totalHeight += rowHeight
        }

        layout(layoutWidth, maxOf(totalHeight, constraints.minHeight)) {
            var currentY = 0
            for (row in rows) {
                var currentX = when (horizontalArrangement) {
                    Arrangement.Center -> (layoutWidth - row.width) / 2
                    Arrangement.End -> layoutWidth - row.width
                    else -> 0
                }
                for (placeable in row.placeables) {
                    placeable.placeRelative(currentX, currentY)
                    currentX += placeable.width + hSpacing
                }
                currentY += row.height + vSpacing
            }
        }
    }
}

@Composable
fun ImdbParentsGuideSection(parentsGuide: ImdbParentsGuideResponse) {
    val items = parentsGuide.parentsGuide
    if (items.isNullOrEmpty()) return
    
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "Parents Guide & Content Advisory",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(8.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = NextColors.cardColors(),
        border = NextColors.cardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            items.forEach { item ->
                val categoryDisplay = when (item.category) {
                    "SEXUAL_CONTENT" -> "Sex & Nudity"
                    "VIOLENCE" -> "Violence & Gore"
                    "PROFANITY" -> "Profanity"
                    "ALCOHOL_DRUGS" -> "Alcohol, Drugs & Smoking"
                    "FRIGHTENING_INTENSE_SCENES" -> "Frightening & Intense"
                    else -> item.category
                }
                val severityItem = item.severityBreakdowns?.maxByOrNull { it.voteCount }
                val severityLevel = severityItem?.severityLevel ?: "None"
                val badgeColor = when (severityLevel.lowercase()) {
                    "severe" -> Color(0xFFEF5350)
                    "moderate" -> Color(0xFFFF9800)
                    "mild" -> Color(0xFFFFEB3B)
                    "none" -> Color(0xFF66BB6A)
                    else -> Color.Gray
                }
                val badgeTextColor = if (severityLevel.lowercase() == "mild") Color.Black else Color.White
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoryDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = severityLevel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImdbBoxOfficeSection(boxOffice: ImdbBoxOffice) {
    val budget = boxOffice.productionBudget
    val worldwide = boxOffice.worldwideGross
    val domestic = boxOffice.domesticGross
    
    if (budget == null && worldwide == null && domestic == null) return
    
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "Box Office",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(8.dp))
    
    fun formatMoney(money: dev.anilbeesetti.nextplayer.core.data.network.models.ImdbMoney?): String? {
        if (money == null) return null
        val amountNum = money.amount.toDoubleOrNull() ?: return null
        return when {
            amountNum >= 1_000_000_000 -> String.format("$%.2f B", amountNum / 1_000_000_000)
            amountNum >= 1_000_000 -> String.format("$%.1f M", amountNum / 1_000_000)
            else -> String.format("$%,d", amountNum.toLong())
        } + " ${money.currency}"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = NextColors.cardColors(),
        border = NextColors.cardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            budget?.let {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Budget", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                    Text(formatMoney(it) ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            worldwide?.let {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Worldwide Gross", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                    Text(formatMoney(it) ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF66BB6A))
                }
            }
            domestic?.let {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Domestic Gross", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                    Text(formatMoney(it) ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
