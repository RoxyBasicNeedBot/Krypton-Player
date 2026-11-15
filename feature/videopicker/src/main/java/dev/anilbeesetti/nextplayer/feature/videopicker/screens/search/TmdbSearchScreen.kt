@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package dev.anilbeesetti.nextplayer.feature.videopicker.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.anilbeesetti.nextplayer.core.common.CrashBreadcrumbs
import dev.anilbeesetti.nextplayer.core.data.network.models.TmdbSearchResult
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextSegmentedListItem
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.ChannelLoadingPlaceholder
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.PremiumMeshBackground

@Composable
fun TmdbSearchRoute(
    onNavigateUp: () -> Unit,
    onMediaClick: (Int, Boolean) -> Unit,
    onActorClick: (Int) -> Unit,
    viewModel: TmdbSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        CrashBreadcrumbs.recordNav("TmdbSearchRoute")
    }
    TmdbSearchScreen(
        uiState = uiState,
        onNavigateUp = {
            CrashBreadcrumbs.recordClick("Back Button", "TmdbSearchScreen")
            onNavigateUp()
        },
        onMediaClick = { id, isTv ->
            CrashBreadcrumbs.recordClick("TMDB Media Item ($id, isTv=$isTv)", "TmdbSearchScreen")
            onMediaClick(id, isTv)
        },
        onActorClick = { id ->
            CrashBreadcrumbs.recordClick("TMDB Actor Item ($id)", "TmdbSearchScreen")
            onActorClick(id)
        },
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmdbSearchScreen(
    uiState: TmdbSearchUiState,
    onNavigateUp: () -> Unit,
    onMediaClick: (Int, Boolean) -> Unit,
    onActorClick: (Int) -> Unit,
    onEvent: (TmdbSearchUiEvent) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler {
        if (uiState.query.isNotEmpty()) {
            onEvent(TmdbSearchUiEvent.OnQueryChange(""))
        } else {
            onNavigateUp()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        PremiumMeshBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    NextTopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        title = {
                            OutlinedTextField(
                                value = uiState.query,
                                onValueChange = { onEvent(TmdbSearchUiEvent.OnQueryChange(it)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                placeholder = {
                                    Text(
                                        text = "Search TMDB database...",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                trailingIcon = {
                                    if (uiState.query.isNotEmpty()) {
                                        IconButton(onClick = { onEvent(TmdbSearchUiEvent.OnQueryChange("")) }) {
                                            Icon(
                                                imageVector = NextIcons.Close,
                                                contentDescription = "Clear search",
                                            )
                                        }
                                    } else if (uiState.isSearching) {
                                        LoadingIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        onEvent(TmdbSearchUiEvent.OnSearch(uiState.query))
                                        keyboardController?.hide()
                                    },
                                ),
                                shape = CircleShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    errorBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                ),
                            )
                        },
                        navigationIcon = {
                            FilledTonalIconButton(onClick = onNavigateUp) {
                                Icon(
                                    imageVector = NextIcons.ArrowBack,
                                    contentDescription = stringResource(id = R.string.navigate_up),
                                )
                            }
                        },
                    )
                },
            ) { scaffoldPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                ) {
                    if (uiState.query.isBlank()) {
                        TmdbSearchSuggestionsContent(
                            searchHistory = uiState.searchHistory,
                            onHistoryItemClick = { onEvent(TmdbSearchUiEvent.OnHistoryItemClick(it)) },
                            onRemoveHistoryItem = { onEvent(TmdbSearchUiEvent.OnRemoveHistoryItem(it)) },
                            onClearHistory = { onEvent(TmdbSearchUiEvent.OnClearHistory) }
                        )
                    } else {
                        TmdbSearchResultsContent(
                            query = uiState.query,
                            results = uiState.results,
                            error = uiState.error,
                            isSearching = uiState.isSearching,
                            hasReachedEnd = uiState.hasReachedEnd,
                            onLoadMore = { onEvent(TmdbSearchUiEvent.LoadNextPage) },
                            onMediaClick = { id, isTv ->
            CrashBreadcrumbs.recordClick("TMDB Media Item ($id, isTv=$isTv)", "TmdbSearchScreen")
            onMediaClick(id, isTv)
        },
                            onActorClick = { id ->
            CrashBreadcrumbs.recordClick("TMDB Actor Item ($id)", "TmdbSearchScreen")
            onActorClick(id)
        },
                            onSuggestionClick = { onEvent(TmdbSearchUiEvent.OnHistoryItemClick(it)) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TmdbSearchSuggestionsContent(
    searchHistory: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (searchHistory.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ChannelLoadingPlaceholder(
                lottieRes = R.raw.anim_duck_search,
                title = "Search Movies & TV Shows",
                subtitle = "Discover popular movies, trending series, and actors from TMDB",
                isLoading = false,
                suggestions = listOf("Trending", "Top Rated", "Marvel", "Anime", "/boost", "/learn"),
                onSuggestionClick = onHistoryItemClick
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClearHistory) {
                Text(text = "Clear All")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchHistory) { query ->
                NextSegmentedListItem(
                    onClick = { onHistoryItemClick(query) },
                    content = {
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { onRemoveHistoryItem(query) }) {
                            Icon(
                                imageVector = NextIcons.Close,
                                contentDescription = "Remove search query"
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TmdbSearchResultsContent(
    query: String = "",
    results: List<TmdbSearchResult>,
    error: String?,
    isSearching: Boolean,
    hasReachedEnd: Boolean,
    onLoadMore: () -> Unit,
    onMediaClick: (Int, Boolean) -> Unit,
    onActorClick: (Int) -> Unit,
    onSuggestionClick: ((String) -> Unit)? = null
) {
    if (isSearching) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 40.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            ChannelLoadingPlaceholder(
                lottieRes = R.raw.anim_duck_search,
                title = "Searching TMDB...",
                subtitle = "Querying titles, cast members, and TV seasons",
                isLoading = true
            )
        }
        return
    }

    if (error != null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 40.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            ChannelLoadingPlaceholder(
                lottieRes = R.raw.anim_duck_sad,
                title = "TMDB Search Error",
                subtitle = error,
                isLoading = false,
                actionButtonText = "Try Trending",
                onActionClick = { onSuggestionClick?.invoke("Trending") },
                suggestions = listOf("/boost", "/learn", "Trending", "Top Rated"),
                onSuggestionClick = onSuggestionClick
            )
        }
        return
    }

    if (results.isEmpty()) {
        val isBoost = query.trim().equals("/boost", ignoreCase = true) || query.contains("boost", ignoreCase = true)
        val isLearn = query.trim().equals("/learn", ignoreCase = true) || query.contains("learn", ignoreCase = true)
        val uriHandler = LocalUriHandler.current
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 40.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            ChannelLoadingPlaceholder(
                lottieRes = if (isBoost) R.raw.anim_duck_boost
                    else if (isLearn) R.raw.anim_duck_learn
                    else R.raw.anim_duck_empty,
                title = if (isBoost) "Boost TMDB Search"
                    else if (isLearn) "TMDB Search Guides (/learn)"
                    else "No TMDB Results Found",
                subtitle = if (isBoost) "Boost TMDB discovery across trending, upcoming, and highest-rated titles."
                    else if (isLearn) "Learn tips on searching movies, seasons, episodes, and actor filmographies."
                    else "We couldn't find any movies, TV shows, or actors matching your search.",
                isLoading = false,
                showBoostIcon = isBoost,
                showLearnIcon = isLearn,
                actionButtonText = if (isBoost) "Discover Trending" else if (isLearn) "Open Guides (/learn)" else null,
                onActionClick = if (isBoost) ({ onSuggestionClick?.invoke("Trending") })
                    else if (isLearn) ({ try { uriHandler.openUri("https://t.me/kryptonplayerbot?start=learn") } catch (_: Exception) {} })
                    else null,
                suggestions = if (isBoost) {
                    listOf("/learn", "Trending", "Top Rated", "Marvel", "Anime")
                } else if (isLearn) {
                    listOf("/boost", "Trending", "Top Rated", "Marvel", "Anime")
                } else {
                    listOf("/boost", "/learn", "Trending", "Top Rated", "Marvel", "Anime")
                },
                onSuggestionClick = onSuggestionClick
            )
        }
        return
    }

    val movies = results.filterIsInstance<TmdbSearchResult.Movie>()
    val tvShows = results.filterIsInstance<TmdbSearchResult.TvShow>()
    val people = results.filterIsInstance<TmdbSearchResult.Person>()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (movies.isNotEmpty()) {
            item {
                ListSectionTitle(text = "Movies", contentPadding = PaddingValues(top = 8.dp, bottom = 4.dp))
            }
            itemsIndexed(movies, key = { index, movie -> "movie_${movie.id}_$index" }) { _, movie ->
                TmdbSearchItemRow(
                    title = movie.title,
                    imagePath = movie.poster_path,
                    rating = movie.vote_average,
                    year = movie.release_date?.take(4),
                    isCircle = false,
                    onClick = { onMediaClick(movie.id, false) }
                )
            }
        }

        if (tvShows.isNotEmpty()) {
            item {
                ListSectionTitle(text = "TV Shows", contentPadding = PaddingValues(top = 8.dp, bottom = 4.dp))
            }
            itemsIndexed(tvShows, key = { index, tv -> "tv_${tv.id}_$index" }) { _, tv ->
                TmdbSearchItemRow(
                    title = tv.name,
                    imagePath = tv.poster_path,
                    rating = tv.vote_average,
                    year = tv.first_air_date?.take(4),
                    isCircle = false,
                    onClick = { onMediaClick(tv.id, true) }
                )
            }
        }

        if (people.isNotEmpty()) {
            item {
                ListSectionTitle(text = "People", contentPadding = PaddingValues(top = 8.dp, bottom = 4.dp))
            }
            itemsIndexed(people, key = { index, person -> "person_${person.id}_$index" }) { _, person ->
                TmdbSearchItemRow(
                    title = person.name,
                    imagePath = person.profile_path,
                    rating = null,
                    year = person.known_for_department,
                    isCircle = true,
                    onClick = { onActorClick(person.id) }
                )
            }
        }

        if (!hasReachedEnd) {
            item {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TmdbSearchItemRow(
    title: String,
    imagePath: String?,
    rating: Double?,
    year: String?,
    isCircle: Boolean,
    onClick: () -> Unit
) {
    val imageUrl = imagePath?.let { dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.tmdbImageUrl("w185", it) }

    NextSegmentedListItem(
        onClick = onClick,
        leadingContent = {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(48.dp)
                        .let { 
                            if (isCircle) it.clip(CircleShape) else it.clip(RoundedCornerShape(6.dp)) 
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .let { 
                            if (isCircle) it.clip(CircleShape) else it.clip(RoundedCornerShape(6.dp)) 
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.take(2),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        content = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (rating != null) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format("%.1f", rating),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (year != null) {
                            Text(text = "•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (year != null) {
                        Text(
                            text = year,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}
