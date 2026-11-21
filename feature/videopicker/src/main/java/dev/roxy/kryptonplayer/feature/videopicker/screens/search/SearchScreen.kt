@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.roxy.kryptonplayer.feature.videopicker.screens.search

import android.net.Uri
import dev.roxy.kryptonplayer.core.common.CrashBreadcrumbs
import dev.roxy.kryptonplayer.core.ui.theme.LocalMediaController
import dev.roxy.kryptonplayer.feature.videopicker.composables.rememberPlayerState
import androidx.media3.common.Player
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import dev.roxy.kryptonplayer.core.domain.SearchResults
import dev.roxy.kryptonplayer.core.domain.asRootFolder
import dev.roxy.kryptonplayer.core.model.ApplicationPreferences
import dev.roxy.kryptonplayer.core.model.Folder
import dev.roxy.kryptonplayer.core.model.MediaLayoutMode
import dev.roxy.kryptonplayer.core.model.Video
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.NextTopAppBar
import dev.roxy.kryptonplayer.core.ui.components.ListSectionTitle
import dev.roxy.kryptonplayer.core.ui.components.NextSegmentedListItem
import dev.roxy.kryptonplayer.core.ui.components.ChannelLoadingPlaceholder
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.extensions.copy
import dev.roxy.kryptonplayer.core.ui.extensions.plus
import dev.roxy.kryptonplayer.core.ui.theme.PremiumMeshBackground
import dev.roxy.kryptonplayer.feature.videopicker.composables.FolderItem
import dev.roxy.kryptonplayer.feature.videopicker.composables.MediaView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

@Composable
fun SearchRoute(
    viewModel: SearchViewModel = hiltViewModel(),
    onPlayVideo: (uri: Uri) -> Unit,
    onFolderClick: (folderPath: String) -> Unit,
    onNavigateUp: () -> Unit,
) {
    LaunchedEffect(Unit) {
        CrashBreadcrumbs.recordNav("SearchRoute")
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SearchScreen(
        uiState = uiState,
        onNavigateUp = {
            CrashBreadcrumbs.recordClick("Back Button", "SearchScreen")
            onNavigateUp()
        },
        onFolderClick = {
            CrashBreadcrumbs.recordClick("Folder Search Result: $it", "SearchScreen")
            onFolderClick(it)
        },
        onVideoClick = {
            CrashBreadcrumbs.recordClick("Video Search Result: $it", "SearchScreen")
            onPlayVideo(it)
        },
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    uiState: SearchUiState,
    onNavigateUp: () -> Unit = {},
    onFolderClick: (String) -> Unit = {},
    onVideoClick: (Uri) -> Unit = {},
    onEvent: (SearchUiEvent) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val mediaController = LocalMediaController.current
    val isMiniPlayerVisible = mediaController?.let { controller ->
        val playerState = rememberPlayerState(controller)
        playerState.currentMediaItem != null && playerState.playbackState != Player.STATE_IDLE
    } ?: false
    val keyboardController = LocalSoftwareKeyboardController.current

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
                                onValueChange = { onEvent(SearchUiEvent.OnQueryChange(it)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.search_videos_and_folders),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                trailingIcon = {
                                    if (uiState.query.isNotEmpty()) {
                                        IconButton(onClick = {
                                            CrashBreadcrumbs.recordClick("Clear Search Query", "SearchScreen")
                                            onEvent(SearchUiEvent.OnQueryChange(""))
                                        }) {
                                            Icon(
                                                imageVector = NextIcons.Close,
                                                contentDescription = stringResource(R.string.clear_history),
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
                                        onEvent(SearchUiEvent.OnSearch(uiState.query))
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
                    val updatedScaffoldPadding = PaddingValues(
                        bottom = if (isMiniPlayerVisible) 80.dp else 0.dp
                    )

                    if (uiState.query.isBlank()) {
                        SuggestionsContent(
                            searchHistory = uiState.searchHistory,
                            popularFolders = uiState.popularFolders,
                            preferences = uiState.preferences,
                            contentPadding = updatedScaffoldPadding,
                            onHistoryItemClick = { onEvent(SearchUiEvent.OnHistoryItemClick(it)) },
                            onRemoveHistoryItem = { onEvent(SearchUiEvent.OnRemoveHistoryItem(it)) },
                            onClearHistory = { onEvent(SearchUiEvent.OnClearHistory) },
                            onFolderClick = onFolderClick,
                        )
                    } else {
                        SearchResultsContent(
                            query = uiState.query,
                            searchResults = uiState.searchResults,
                            preferences = uiState.preferences,
                            isSearching = uiState.isSearching,
                            contentPadding = updatedScaffoldPadding,
                            onFolderClick = onFolderClick,
                            onVideoClick = onVideoClick,
                            onVideoLoaded = { onEvent(SearchUiEvent.AddToSync(it)) },
                            onHistoryItemClick = { onEvent(SearchUiEvent.OnHistoryItemClick(it)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionsContent(
    searchHistory: List<String>,
    popularFolders: List<Folder>,
    preferences: ApplicationPreferences,
    contentPadding: PaddingValues = PaddingValues(),
    onHistoryItemClick: (String) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    onFolderClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp) + contentPadding,
    ) {
        if (searchHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ListSectionTitle(
                        text = stringResource(R.string.recent_searches),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(text = stringResource(R.string.clear_history))
                    }
                }
            }

            items(
                items = searchHistory,
                key = { "history_$it" },
            ) { query ->
                SearchHistoryItem(
                    query = query,
                    onClick = { onHistoryItemClick(query) },
                    onRemove = { onRemoveHistoryItem(query) },
                )
            }
        }

        if (popularFolders.isNotEmpty()) {
            item {
                ListSectionTitle(
                    text = stringResource(R.string.popular_folders),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = if (searchHistory.isNotEmpty()) 20.dp else 12.dp,
                        bottom = 8.dp,
                    ),
                )
            }

            itemsIndexed(
                items = popularFolders,
                key = { _, folder -> "popular_${folder.path}" },
            ) { index, folder ->
                FolderItem(
                    folder = folder,
                    isRecentlyPlayedFolder = false,
                    preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.LIST),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    isFirstItem = index == 0,
                    isLastItem = index == popularFolders.lastIndex,
                    onClick = { onFolderClick(folder.path) },
                )
            }
        }

        if (searchHistory.isEmpty() && popularFolders.isEmpty()) {
            item {
                ChannelLoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 40.dp),
                    lottieRes = R.raw.anim_duck_search,
                    title = stringResource(R.string.search_videos_and_folders),
                    subtitle = "Search your device storage for videos, movies, and folders.",
                    isLoading = false,
                    suggestions = listOf("Movies", "Downloads", "Camera", "Telegram", "/boost", "/learn"),
                    onSuggestionClick = { onHistoryItemClick(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchHistoryItem(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    NextSegmentedListItem(
        modifier = Modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        onClick = onClick,
        leadingContent = {
            Icon(
                imageVector = NextIcons.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = NextIcons.Close,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        content = {
            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun SearchResultsContent(
    query: String = "",
    searchResults: SearchResults,
    preferences: ApplicationPreferences,
    isSearching: Boolean,
    contentPadding: PaddingValues = PaddingValues(),
    onFolderClick: (String) -> Unit,
    onVideoClick: (Uri) -> Unit,
    onVideoLoaded: (Uri) -> Unit,
    onHistoryItemClick: ((String) -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = isSearching,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 40.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            ChannelLoadingPlaceholder(
                lottieRes = R.raw.anim_duck_search,
                title = "Searching Device...",
                subtitle = "Scanning local directories and media files",
                isLoading = true
            )
        }
    }

    AnimatedVisibility(
        visible = !isSearching,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        if (searchResults.isEmpty) {
            val isBoost = query.trim().equals("/boost", ignoreCase = true) || query.contains("boost", ignoreCase = true)
            val isLearn = query.trim().equals("/learn", ignoreCase = true) || query.contains("learn", ignoreCase = true)
            val uriHandler = LocalUriHandler.current
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                ChannelLoadingPlaceholder(
                    lottieRes = if (isBoost) R.raw.anim_duck_boost
                        else if (isLearn) R.raw.anim_duck_learn
                        else R.raw.anim_duck_empty,
                    title = if (isBoost) "Boost Device Storage"
                        else if (isLearn) "Storage & Search Guides (/learn)"
                        else stringResource(R.string.no_results_found),
                    subtitle = if (isBoost) "Supercharge indexing for faster search across external SD and internal storage."
                        else if (isLearn) "Learn how to use filters, folder navigation, and video search shortcuts."
                        else "No videos or folders matched your query.",
                    isLoading = false,
                    showBoostIcon = isBoost,
                    showLearnIcon = isLearn,
                    actionButtonText = if (isBoost) "Browse Videos" else if (isLearn) "Open Guides (/learn)" else null,
                    onActionClick = if (isBoost) ({ onHistoryItemClick?.invoke("Movies") })
                        else if (isLearn) ({ try { uriHandler.openUri("https://t.me/kryptonplayerbot?start=learn") } catch (_: Exception) {} })
                        else null,
                    suggestions = if (isBoost) {
                        listOf("/learn", "Movies", "Downloads", "Camera", "Telegram")
                    } else if (isLearn) {
                        listOf("/boost", "Movies", "Downloads", "Camera", "Telegram")
                    } else {
                        listOf("/boost", "/learn", "Movies", "Downloads", "Camera", "Telegram")
                    },
                    onSuggestionClick = onHistoryItemClick
                )
            }
        } else {
            MediaView(
                rootFolder = searchResults.asRootFolder(),
                preferences = preferences,
                onFolderClick = onFolderClick,
                onVideoClick = onVideoClick,
                onVideoLoaded = onVideoLoaded,
                contentPadding = contentPadding,
            )
        }
    }
}
