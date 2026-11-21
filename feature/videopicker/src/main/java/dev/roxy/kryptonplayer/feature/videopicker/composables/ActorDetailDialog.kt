package dev.roxy.kryptonplayer.feature.videopicker.composables

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.data.network.ArtistImageItem
import dev.roxy.kryptonplayer.core.data.network.DuckDuckGoImageClient
import dev.roxy.kryptonplayer.core.data.network.models.MediaStill
import dev.roxy.kryptonplayer.core.data.network.models.StillSource
import dev.roxy.kryptonplayer.core.data.network.toMediaStill
import android.util.Log
import dev.roxy.kryptonplayer.core.data.repository.TmdbRepository
import dev.roxy.kryptonplayer.core.ui.theme.NextColors
import dev.roxy.kryptonplayer.core.ui.theme.ForceDarkContent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ActorDetailViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
    private val duckDuckGoImageClient: DuckDuckGoImageClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActorDetailUiState>(ActorDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _artistImages = MutableStateFlow<List<ArtistImageItem>>(emptyList())
    val artistImages = _artistImages.asStateFlow()

    private val _isLoadingMoreImages = MutableStateFlow(false)
    val isLoadingMoreImages = _isLoadingMoreImages.asStateFlow()

    private var currentActorName: String = ""
    private var currentImagePage = 0
    private var isEndReached = false

    private val actorCache = mutableMapOf<Int, ActorDetailUiState.Success>()

    fun loadActorDetails(id: Int) {
        val cached = actorCache[id]
        if (cached != null) {
            _uiState.value = cached
            if (currentActorName != cached.data.name) {
                currentActorName = cached.data.name
                loadArtistImages(cached.data.name, reset = true)
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = ActorDetailUiState.Loading

            val triviaDeferred = async { tmdbRepository.getTrivia(id).getOrNull() }
            val relationshipsDeferred = async { tmdbRepository.getRelationships(id).getOrNull() }

            tmdbRepository.getPersonDetails(id).onSuccess { details ->
                currentActorName = details.name
                loadArtistImages(details.name, reset = true)
                val filmography = mutableListOf<FilmographyItem>()

                val movieCast = details.movie_credits?.cast ?: emptyList()
                val tvCast = details.tv_credits?.cast ?: emptyList()

                movieCast.forEach { movie ->
                    filmography.add(
                        FilmographyItem(
                            id = movie.id,
                            title = movie.title,
                            posterPath = movie.poster_path,
                            rating = movie.vote_average,
                            isTvShow = false
                        )
                    )
                }

                tvCast.forEach { tv ->
                    filmography.add(
                        FilmographyItem(
                            id = tv.id,
                            title = tv.name,
                            posterPath = tv.poster_path,
                            rating = tv.vote_average,
                            isTvShow = true
                        )
                    )
                }

                val sortedFilmography = filmography
                    .distinctBy { it.id to it.isTvShow }
                    .sortedByDescending { it.rating }

                val trivia = triviaDeferred.await()?.map { it.text } ?: emptyList()
                val relationships = relationshipsDeferred.await()?.map { relation ->
                    val name = relation.name.displayName ?: ""
                    val type = relation.relationType
                    val attrs = relation.attributes?.joinToString(", ")?.let { " ($it)" } ?: ""
                    "$name - ${type.replaceFirstChar { it.lowercase() }}$attrs"
                } ?: emptyList()

                val formattedBirthday = formatBirthdayAndAge(details.birthday)
                val totalCredits = movieCast.size + tvCast.size

                val successState = ActorDetailUiState.Success(
                    ActorDetailData(
                        id = details.id,
                        name = details.name,
                        biography = details.biography,
                        profilePath = details.profile_path,
                        birthday = details.birthday,
                        placeOfBirth = details.place_of_birth,
                        filmography = sortedFilmography,
                        movieCount = movieCast.size,
                        tvCount = tvCast.size,
                        totalCredits = totalCredits,
                        formattedBirthdayAndAge = formattedBirthday,
                        trivia = trivia,
                        relationships = relationships
                    )
                )
                actorCache[details.id] = successState
                _uiState.value = successState
            }.onFailure { error ->
                _uiState.value = ActorDetailUiState.Error(error.localizedMessage ?: "Failed to load actor details")
            }
        }
    }

    private fun formatBirthdayAndAge(birthday: String?): String? {
        if (birthday.isNullOrBlank()) return null
        return try {
            val parts = birthday.split("-")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val monthStr = months.getOrNull(month) ?: parts[1]
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                var age = currentYear - year
                if (currentMonth < month || (currentMonth == month && currentDay < day)) {
                    age--
                }
                "$day-$monthStr-$year ($age)"
            } else {
                birthday
            }
        } catch (_: Exception) {
            birthday
        }
    }

    fun loadArtistImages(name: String, reset: Boolean = false) {
        if (name.isBlank()) return
        if (reset) {
            currentImagePage = 0
            isEndReached = false
            _artistImages.value = emptyList()
        }
        if (_isLoadingMoreImages.value || (isEndReached && !reset)) return

        viewModelScope.launch {
            _isLoadingMoreImages.value = true
            try {
                val query = name.trim()
                val newImages = duckDuckGoImageClient.searchArtistImages(query, page = currentImagePage)
                if (newImages.isEmpty()) {
                    isEndReached = true
                } else {
                    currentImagePage++
                    _artistImages.update { existing ->
                        val existingUrls = existing.map { it.originalUrl }.toSet()
                        existing + newImages.filter { it.originalUrl !in existingUrls }
                    }
                }
            } catch (e: Exception) {
                Log.e("ActorDetailViewModel", "Error fetching DDG images: ${e.message}")
            } finally {
                _isLoadingMoreImages.value = false
            }
        }
    }

    fun loadMoreImages() {
        if (currentActorName.isNotBlank() && !_isLoadingMoreImages.value && !isEndReached) {
            loadArtistImages(currentActorName, reset = false)
        }
    }
}

sealed interface ActorDetailUiState {
    object Loading : ActorDetailUiState
    data class Success(val data: ActorDetailData) : ActorDetailUiState
    data class Error(val message: String) : ActorDetailUiState
}

data class ActorDetailData(
    val id: Int,
    val name: String,
    val biography: String?,
    val profilePath: String?,
    val birthday: String?,
    val placeOfBirth: String?,
    val filmography: List<FilmographyItem>,
    val movieCount: Int = 0,
    val tvCount: Int = 0,
    val totalCredits: Int = 0,
    val formattedBirthdayAndAge: String? = null,
    val trivia: List<String> = emptyList(),
    val relationships: List<String> = emptyList()
)

data class FilmographyItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val rating: Double,
    val isTvShow: Boolean
)

private enum class MediaFilterTab {
    ACTING,
    PRODUCTION,
    CREW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorDetailDialog(
    actorId: Int,
    onDismiss: () -> Unit,
    onMediaClick: (Int, Boolean) -> Unit,
    viewModel: ActorDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(actorId) {
        viewModel.loadActorDetails(actorId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val artistImages by viewModel.artistImages.collectAsState()
    val isLoadingMoreImages by viewModel.isLoadingMoreImages.collectAsState()

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
                ActorDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFE58B24))
                    }
                }
                is ActorDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE58B24))
                        ) {
                            Text("Close")
                        }
                    }
                }
                is ActorDetailUiState.Success -> {
                    val data = state.data
                    val profileUrl = data.profilePath?.let { dev.roxy.kryptonplayer.core.data.network.NetworkConfig.tmdbImageUrl("h632", it) }
                    val fullProfileUrl = data.profilePath?.let { dev.roxy.kryptonplayer.core.data.network.NetworkConfig.tmdbImageUrl("original", it) } ?: profileUrl

                    val allStills = remember(data, artistImages) {
                        val list = mutableListOf<MediaStill>()
                        if (fullProfileUrl != null) {
                            list.add(
                                MediaStill(
                                    id = "actor_official_${data.id}",
                                    thumbnailUrl = profileUrl ?: fullProfileUrl,
                                    fullUrl = fullProfileUrl,
                                    source = StillSource.TMDB,
                                    caption = data.name
                                )
                            )
                        }
                        artistImages.forEachIndexed { index, item ->
                            list.add(item.toMediaStill("actor_ddg_${data.id}_$index"))
                        }
                        list
                    }

                    val posterPagerState = rememberPagerState(
                        initialPage = 0,
                        pageCount = { if (allStills.isEmpty()) 1 else allStills.size }
                    )

                    // Auto-load next batch of images when swiping near the end (unlimited swipe)
                    LaunchedEffect(posterPagerState.currentPage, allStills.size) {
                        if (allStills.isNotEmpty() && posterPagerState.currentPage >= allStills.size - 4) {
                            viewModel.loadMoreImages()
                        }
                    }

                    var showFullProfilePhoto by remember { mutableStateOf(false) }
                    var lightboxInitialIndex by remember { mutableIntStateOf(0) }
                    var isFavorite by remember { mutableStateOf(false) }
                    var bioExpanded by remember { mutableStateOf(false) }
                    var selectedTab by remember { mutableStateOf(MediaFilterTab.ACTING) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0E0F14))
                    ) {
                        // ── 1. Blurred Ambient Aura Background ─────────────────────
                        val currentAuraUrl = allStills.getOrNull(posterPagerState.currentPage)?.fullUrl ?: profileUrl
                        if (!currentAuraUrl.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(520.dp)
                            ) {
                                AsyncImage(
                                    model = currentAuraUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(50.dp)
                                )

                                // Gradient overlay for smooth transition to deep dark
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

                        // ── 2. Scrollable Content ──────────────────────────────────
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

                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "https://www.themoviedb.org/person/${data.id}")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ── 3. Centered Squircle Portrait Card ───────────────────
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(26.dp),
                                    border = BorderStroke(1.5.dp, Color(0xFF00D6FF).copy(alpha = 0.35f)),
                                    shadowElevation = 18.dp,
                                    color = Color(0xFF1B1D25),
                                    modifier = Modifier
                                        .width(185.dp)
                                        .height(260.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (allStills.isNotEmpty()) {
                                            HorizontalPager(
                                                state = posterPagerState,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        lightboxInitialIndex = posterPagerState.currentPage
                                                        showFullProfilePhoto = true
                                                    }
                                            ) { pageIndex ->
                                                val still = allStills.getOrNull(pageIndex)
                                                if (still != null) {
                                                    AsyncImage(
                                                        model = still.fullUrl.ifEmpty { still.thumbnailUrl },
                                                        contentDescription = "${data.name} photo $pageIndex",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = data.name.take(2).uppercase(),
                                                            fontSize = 42.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }

                                            // Bottom-center Swipe Counter Pill (e.g. 📸 1/35)
                                            if (allStills.size > 1) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color.Black.copy(alpha = 0.72f),
                                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.PhotoLibrary,
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.85f),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = "${posterPagerState.currentPage + 1}/${allStills.size}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Color.White
                                                        )
                                                        if (isLoadingMoreImages) {
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(9.dp),
                                                                strokeWidth = 1.5.dp,
                                                                color = Color(0xFFE58B24)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = data.name.take(2).uppercase(),
                                                    fontSize = 42.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Person Name
                                Text(
                                    text = data.name,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Role & Credits Subtitle
                                Text(
                                    text = "Acting • ${data.totalCredits} Credits",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontWeight = FontWeight.Normal
                                )

                                // Birth & Age Line
                                if (!data.formattedBirthdayAndAge.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = data.formattedBirthdayAndAge,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.55f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // ── 4. Action Pill Container (SVG Icons, NO Emoji) ───
                                Surface(
                                    shape = RoundedCornerShape(32.dp),
                                    color = Color(0xFF1B1E27).copy(alpha = 0.95f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { isFavorite = !isFavorite },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = "Favorite",
                                                tint = if (isFavorite) Color(0xFFFF4B6E) else Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (allStills.isNotEmpty()) {
                                                    lightboxInitialIndex = posterPagerState.currentPage
                                                    showFullProfilePhoto = true
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PhotoLibrary,
                                                contentDescription = "Gallery",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { /* Profile info */ },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Badge,
                                                contentDescription = "ID Badge",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "https://www.themoviedb.org/person/${data.id}")
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreHoriz,
                                                contentDescription = "More Options",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // ── 5. Stats Badges Row (SVG Icons, NO Emoji) ────────
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFF1E212B).copy(alpha = 0.85f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Movie,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "0/${data.movieCount} movies",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFF1E212B).copy(alpha = 0.85f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tv,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "0/${data.tvCount} shows",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }

                            // ── 6. Location Pin Row (SVG Icon, NO Emoji) ─────────────
                            if (!data.placeOfBirth.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Place,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.65f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = data.placeOfBirth,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // ── 7. Expandable Biography ─────────────────────────────
                            if (!data.biography.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                ) {
                                    Text(
                                        text = data.biography,
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp,
                                        color = Color.White.copy(alpha = 0.82f),
                                        maxLines = if (bioExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (data.biography.length > 140) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (bioExpanded) "LESS" else "MORE",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE58B24),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { bioExpanded = !bioExpanded }
                                                .padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // ── 7.5. Photos & Wallpapers Section (DuckDuckGo Unlimited Stream) ───
                            if (allStills.size > 1 || isLoadingMoreImages) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PhotoLibrary,
                                            contentDescription = null,
                                            tint = Color(0xFFE58B24),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Photos & Wallpapers",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.08f)
                                        ) {
                                            Text(
                                                text = "${allStills.size}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (allStills.isNotEmpty()) {
                                        Text(
                                            text = "View All",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFE58B24),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    lightboxInitialIndex = posterPagerState.currentPage
                                                    showFullProfilePhoto = true
                                                }
                                                .padding(4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp)
                                ) {
                                    itemsIndexed(allStills) { idx, still ->
                                        val isCurrent = posterPagerState.currentPage == idx
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                width = if (isCurrent) 2.dp else 1.dp,
                                                color = if (isCurrent) Color(0xFF00D6FF) else Color.White.copy(alpha = 0.1f)
                                            ),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D25)),
                                            modifier = Modifier
                                                .width(100.dp)
                                                .height(130.dp)
                                                .clickable {
                                                    lightboxInitialIndex = idx
                                                    showFullProfilePhoto = true
                                                }
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                AsyncImage(
                                                    model = still.thumbnailUrl.ifEmpty { still.fullUrl },
                                                    contentDescription = still.caption,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (isCurrent) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFF00D6FF),
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .align(Alignment.TopEnd)
                                                            .padding(2.dp)
                                                    ) {}
                                                }
                                            }
                                        }
                                    }

                                    if (isLoadingMoreImages) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .width(70.dp)
                                                    .height(130.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.White.copy(alpha = 0.05f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color(0xFFE58B24)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ── 8. Filmography Header & Filter Chips ─────────────────
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Movies",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Filter Chips Row (Acting, Production, Crew)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val tabs = listOf(
                                    MediaFilterTab.ACTING to "Acting",
                                    MediaFilterTab.PRODUCTION to "Production",
                                    MediaFilterTab.CREW to "Crew"
                                )
                                tabs.forEach { (tab, label) ->
                                    val isSelected = selectedTab == tab
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) Color(0xFF8B4713) else Color(0xFF1E212B),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFFE58B24).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.clickable { selectedTab = tab }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Filtered Media List
                            val displayedItems = when (selectedTab) {
                                MediaFilterTab.ACTING -> data.filmography.filter { !it.isTvShow }
                                MediaFilterTab.PRODUCTION -> data.filmography.filter { it.isTvShow }
                                MediaFilterTab.CREW -> data.filmography
                            }.ifEmpty { data.filmography }

                            if (displayedItems.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
                                ) {
                                    items(displayedItems) { media ->
                                        Column(
                                            modifier = Modifier
                                                .width(105.dp)
                                                .clickable { onMediaClick(media.id, media.isTvShow) }
                                        ) {
                                            val posterUrl = media.posterPath?.let { dev.roxy.kryptonplayer.core.data.network.NetworkConfig.tmdbImageUrl("w185", it) }
                                            if (posterUrl != null) {
                                                AsyncImage(
                                                    model = posterUrl,
                                                    contentDescription = media.title,
                                                    modifier = Modifier
                                                        .width(105.dp)
                                                        .height(155.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(Color(0xFF1B1D25)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .width(105.dp)
                                                        .height(155.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(Color.White.copy(alpha = 0.08f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = media.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.White,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(6.dp),
                                                        maxLines = 4,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = media.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Star,
                                                    contentDescription = null,
                                                    tint = NextColors.RatingGold,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = String.format("%.1f", media.rating),
                                                    fontSize = 11.sp,
                                                    color = Color.LightGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ── 9. Trivia / Relationships Section if available ──────
                            if (data.trivia.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                var triviaExpanded by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Trivia",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (data.trivia.size > 3) {
                                        TextButton(onClick = { triviaExpanded = !triviaExpanded }) {
                                            Text(
                                                if (triviaExpanded) "Show Less" else "Show All",
                                                color = Color(0xFFE58B24)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C24)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val displayedTrivia = if (triviaExpanded) data.trivia else data.trivia.take(3)
                                        displayedTrivia.forEach { item ->
                                            Text(
                                                text = item,
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                lineHeight = 19.sp
                                            )
                                            if (item != displayedTrivia.last()) {
                                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(40.dp))
                            Spacer(modifier = Modifier.navigationBarsPadding())
                        }

                        if (showFullProfilePhoto && allStills.isNotEmpty()) {
                            SceneLightboxDialog(
                                stills = allStills,
                                initialIndex = lightboxInitialIndex.coerceIn(0, allStills.lastIndex),
                                title = data.name,
                                onDismiss = { showFullProfilePhoto = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
