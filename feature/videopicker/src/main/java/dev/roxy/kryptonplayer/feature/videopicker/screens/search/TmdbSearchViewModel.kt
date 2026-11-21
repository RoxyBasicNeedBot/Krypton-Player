package dev.roxy.kryptonplayer.feature.videopicker.screens.search

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.data.repository.SearchHistoryRepository
import dev.roxy.kryptonplayer.core.data.repository.TmdbRepository
import dev.roxy.kryptonplayer.core.data.network.models.TmdbSearchResult
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TmdbSearchViewModel @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {

    private val uiStateInternal = MutableStateFlow(TmdbSearchUiState())
    val uiState = uiStateInternal.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    private var currentPage = 1
    private var hasReachedEnd = false
    private var isLoadingMore = false

    init {
        collectSearchHistory()
        collectSearchResults()
    }

    private fun collectSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.searchHistory.collect { history ->
                uiStateInternal.update { it.copy(searchHistory = history) }
            }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun collectSearchResults() {
        viewModelScope.launch {
            searchQuery.debounce(SEARCH_DEBOUNCE_MS)
                .flatMapLatest { query ->
                    flow {
                        if (query.isBlank()) {
                            emit(Result.success(emptyList()))
                        } else {
                            currentPage = 1
                            hasReachedEnd = false
                            isLoadingMore = false
                            uiStateInternal.update { it.copy(isSearching = true, error = null, hasReachedEnd = false) }
                            emit(tmdbRepository.searchMulti(query, 1))
                        }
                    }
                }
                .collect { result ->
                    result.onSuccess { list ->
                        uiStateInternal.update {
                            it.copy(
                                isSearching = false,
                                results = list,
                                error = null
                            )
                        }
                    }.onFailure { error ->
                        uiStateInternal.update {
                            it.copy(
                                isSearching = false,
                                results = emptyList(),
                                error = error.localizedMessage ?: "Search failed"
                            )
                        }
                    }
                }
        }
    }

    fun loadNextPage() {
        val query = searchQuery.value
        if (query.isBlank() || isLoadingMore || hasReachedEnd) return

        isLoadingMore = true
        uiStateInternal.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            val nextPage = currentPage + 1
            tmdbRepository.searchMulti(query, nextPage).onSuccess { list ->
                isLoadingMore = false
                if (list.isEmpty()) {
                    hasReachedEnd = true
                    uiStateInternal.update { it.copy(isLoadingMore = false, hasReachedEnd = true) }
                } else {
                    currentPage = nextPage
                    uiStateInternal.update { state ->
                        state.copy(
                            isLoadingMore = false,
                            results = state.results + list
                        )
                    }
                }
            }.onFailure { error ->
                isLoadingMore = false
                uiStateInternal.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun onEvent(event: TmdbSearchUiEvent) {
        when (event) {
            is TmdbSearchUiEvent.OnQueryChange -> onQueryChange(event.query)
            is TmdbSearchUiEvent.OnSearch -> onSearch(event.query)
            is TmdbSearchUiEvent.OnHistoryItemClick -> onHistoryItemClick(event.query)
            is TmdbSearchUiEvent.OnRemoveHistoryItem -> removeHistoryItem(event.query)
            is TmdbSearchUiEvent.OnClearHistory -> clearHistory()
            is TmdbSearchUiEvent.LoadNextPage -> loadNextPage()
        }
    }

    private fun onQueryChange(query: String) {
        uiStateInternal.update { it.copy(query = query, isSearching = query.isNotBlank()) }
        searchQuery.value = query
    }

    private fun onSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            searchHistoryRepository.addSearchQuery(query)
        }
    }

    private fun onHistoryItemClick(query: String) {
        uiStateInternal.update { it.copy(query = query, isSearching = true) }
        searchQuery.value = query
        onSearch(query)
    }

    private fun removeHistoryItem(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeSearchQuery(query)
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearHistory()
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}

@Stable
data class TmdbSearchUiState(
    val query: String = "",
    val searchHistory: List<String> = emptyList(),
    val results: List<TmdbSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasReachedEnd: Boolean = false,
    val error: String? = null
)

sealed interface TmdbSearchUiEvent {
    data class OnQueryChange(val query: String) : TmdbSearchUiEvent
    data class OnSearch(val query: String) : TmdbSearchUiEvent
    data class OnHistoryItemClick(val query: String) : TmdbSearchUiEvent
    data class OnRemoveHistoryItem(val query: String) : TmdbSearchUiEvent
    data object OnClearHistory : TmdbSearchUiEvent
    data object LoadNextPage : TmdbSearchUiEvent
}
