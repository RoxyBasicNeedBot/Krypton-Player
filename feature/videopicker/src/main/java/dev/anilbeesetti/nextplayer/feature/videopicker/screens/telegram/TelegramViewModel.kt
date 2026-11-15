package dev.anilbeesetti.nextplayer.feature.videopicker.screens.telegram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.repository.TelegramRepository
import dev.anilbeesetti.nextplayer.core.data.repository.WatchPartyRepository
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import dev.anilbeesetti.nextplayer.core.data.network.models.ChannelResponse
import dev.anilbeesetti.nextplayer.core.data.network.models.ChannelPostDto
import dev.anilbeesetti.nextplayer.core.data.network.models.MediaItemDto
import dev.anilbeesetti.nextplayer.core.data.network.models.ChannelStatsResponse

enum class TelegramSortOrder {
    NAME_ASC,
    NAME_DESC,
    SIZE_ASC,
    SIZE_DESC,
    DURATION_ASC,
    DURATION_DESC,
    DATE_ADDED_ASC,
    DATE_ADDED_DESC
}

sealed class TelegramUiState {
    object Loading : TelegramUiState()
    data class Success(
        val videos: List<Video>,
        val isRefreshing: Boolean = false,
    ) : TelegramUiState()
    data class Error(val message: String) : TelegramUiState()
}

data class ChannelScrollPosition(
    val index: Int = 0,
    val offset: Int = 0,
    val postId: String? = null,
    val messageId: Long = 0L
)

@HiltViewModel
class TelegramViewModel @Inject constructor(
    private val telegramRepository: TelegramRepository,
    private val preferencesRepository: PreferencesRepository,
    private val watchPartyRepository: WatchPartyRepository,
    private val mediaRepository: MediaRepository,
    val tdlibManager: dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibManager,
    val telegramDownloadManager: dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager,
    val telegramCacheManager: dev.anilbeesetti.nextplayer.core.data.cache.TelegramCacheManager,
    val tdlibMediaResolver: dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibMediaResolver,
    val imageDiskCache: dev.anilbeesetti.nextplayer.core.data.cache.TelegramImageDiskCache,
    private val watchLaterRepository: dev.anilbeesetti.nextplayer.core.data.repository.WatchLaterRepository,
    val telegramLikedRepository: dev.anilbeesetti.nextplayer.core.data.repository.TelegramLikedRepository,
    val serverSearchManager: dev.anilbeesetti.nextplayer.core.data.tdlib.TelegramServerSearchManager,
    val storageManager: dev.anilbeesetti.nextplayer.core.data.tdlib.TelegramStorageManager,
    val albumGrouper: dev.anilbeesetti.nextplayer.core.data.tdlib.TelegramAlbumGrouper,
    val moovPrefetcher: dev.anilbeesetti.nextplayer.core.data.tdlib.SmartMoovPrefetcher,
    val thumbnailHelper: dev.anilbeesetti.nextplayer.core.data.tdlib.TelegramThumbnailHelper
) : ViewModel() {

    companion object {
        fun cleanToken(raw: String): String {
            var t = raw.trim()
            if (t.contains("/start ")) {
                t = t.substringAfter("/start ").trim()
            }
            if (t.contains("/token ")) {
                t = t.substringAfter("/token ").trim()
            }
            if (t.contains("start=")) {
                t = t.substringAfter("start=").substringBefore("&").substringBefore(" ").trim()
            }
            if (t.contains("token=")) {
                t = t.substringAfter("token=").substringBefore("&").substringBefore(" ").trim()
            }
            return t.replace("\"", "").replace("'", "").trim()
        }
    }

    val watchLaterQueue = watchLaterRepository.queueFlow

    val playerPreferences = preferencesRepository.playerPreferences

    fun updateKeepMediaIndex(index: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(keepMediaIndex = index) }
        }
    }

    fun updateMaxCacheSizeIndex(index: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(maxCacheSizeIndex = index) }
        }
    }

    fun updateAutoSaveVideos(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(autoSaveVideosToGallery = enabled) }
        }
    }

    fun updateAutoSavePhotos(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(autoSavePhotosToGallery = enabled) }
        }
    }

    fun updateAutoDownloadMobile(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(autoDownloadMobileData = enabled) }
        }
    }

    fun updateAutoDownloadWifi(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(autoDownloadWifi = enabled) }
        }
    }

    val tdlibAuthState = tdlibManager.authState
    val tdlibLiveLogs = tdlibManager.liveLogs
    val tdlibStreamingStats = tdlibManager.streamingStats

    fun reconnectTdlib() {
        tdlibManager.reconnect()
    }

    fun clearTdlibLogs() {
        tdlibManager.clearLogs()
    }



    private var channelVideosJob: kotlinx.coroutines.Job? = null

    private val _isLoading = MutableStateFlow(true)
    private val _rawVideos = MutableStateFlow<List<Video>>(emptyList())
    val rawVideos: StateFlow<List<Video>> = _rawVideos.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(TelegramSortOrder.DATE_ADDED_DESC)
    val sortOrder: StateFlow<TelegramSortOrder> = _sortOrder.asStateFlow()

    private val _showDownloadedOnly = MutableStateFlow(false)
    val showDownloadedOnly: StateFlow<Boolean> = _showDownloadedOnly.asStateFlow()

    private val _showStats = MutableStateFlow(true)
    val showStats: StateFlow<Boolean> = _showStats.asStateFlow()

    private val _isLinking = MutableStateFlow(false)
    val isLinking: StateFlow<Boolean> = _isLinking.asStateFlow()

    private val _linkError = MutableStateFlow<String?>(null)
    val linkError: StateFlow<String?> = _linkError.asStateFlow()

    val userToken: StateFlow<String> = preferencesRepository.playerPreferences
        .map { it.telegramToken }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = preferencesRepository.playerPreferences.value.telegramToken
        )

    private val _channels = MutableStateFlow<List<ChannelResponse>>(emptyList())
    val channels: StateFlow<List<ChannelResponse>> = _channels.asStateFlow()

    val filteredChannels: StateFlow<List<ChannelResponse>> = combine(
        _channels,
        _searchQuery
    ) { rawChannels, query ->
        if (query.isBlank()) {
            rawChannels
        } else {
            rawChannels.filter {
                (it.channel_title ?: "").contains(query, ignoreCase = true) ||
                (it.channel_username ?: "").contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedChannel = MutableStateFlow<ChannelResponse?>(null)
    val selectedChannel: StateFlow<ChannelResponse?> = _selectedChannel.asStateFlow()

    private val _channelVideos = MutableStateFlow<List<Video>>(emptyList())
    val channelVideos: StateFlow<List<Video>> = _channelVideos.asStateFlow()

    private val _channelPosts = MutableStateFlow<List<ChannelPostDto>>(emptyList())
    val channelPosts: StateFlow<List<ChannelPostDto>> = _channelPosts.asStateFlow()

    private val channelPostsCache = java.util.concurrent.ConcurrentHashMap<Long, List<ChannelPostDto>>()
    private val channelScrollPositions = java.util.concurrent.ConcurrentHashMap<Long, ChannelScrollPosition>()

    private val _channelStats = MutableStateFlow<ChannelStatsResponse?>(null)
    val channelStats: StateFlow<ChannelStatsResponse?> = _channelStats.asStateFlow()

    private val _selectedPostType = MutableStateFlow<String?>(null)
    val selectedPostType: StateFlow<String?> = _selectedPostType.asStateFlow()

    val playbackProgress: StateFlow<Map<String, Pair<Long, Long>>> = mediaRepository.getPlaybackProgressFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val continueWatchingCloudVideo: StateFlow<Video?> = combine(
        _rawVideos,
        playbackProgress
    ) { videos, progressMap ->
        videos.filter { video ->
            val progress = progressMap[video.uriString]
            if (progress != null) {
                val pos = progress.first
                val lastPlayed = progress.second
                val isFinished = video.duration > 0L && pos >= video.duration * 0.95f
                pos > 2000L && !isFinished && lastPlayed > 0L
            } else {
                false
            }
        }.map { video ->
            val progress = progressMap[video.uriString]!!
            video.copy(
                playbackPosition = progress.first,
                lastPlayedAt = java.util.Date(progress.second)
            )
        }.maxByOrNull { it.lastPlayedAt?.time ?: 0L }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allChannelVideos: StateFlow<List<Video>> = combine(
        _channelPosts,
        _channelVideos
    ) { posts, rawVideos ->
        val list = mutableListOf<Video>()
        rawVideos.forEach { list.add(it) }

        posts.forEach { post ->
            if (post.postType == "video" || post.postType == "photo" || post.postType == "image") {
                list.add(mapPostToVideo(post))
            } else if (post.postType == "album") {
                post.mediaItems?.forEach { item ->
                    list.add(mapMediaItemToVideo(item, post))
                }
            }
        }
        list.distinctBy { it.path }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val continueWatchingChannelVideo: StateFlow<Video?> = combine(
        allChannelVideos,
        playbackProgress
    ) { videos, progressMap ->
        videos.filter { video ->
            val progress = progressMap[video.uriString]
            if (progress != null) {
                val pos = progress.first
                val lastPlayed = progress.second
                val isFinished = video.duration > 0L && pos >= video.duration * 0.95f
                pos > 2000L && !isFinished && lastPlayed > 0L
            } else {
                false
            }
        }.map { video ->
            val progress = progressMap[video.uriString]!!
            video.copy(
                playbackPosition = progress.first,
                lastPlayedAt = java.util.Date(progress.second)
            )
        }.maxByOrNull { it.lastPlayedAt?.time ?: 0L }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val filteredChannelVideos: StateFlow<List<Video>> = combine(
        allChannelVideos,
        _sortOrder,
        _showDownloadedOnly
    ) { rawVideos, sort, downloadedOnly ->
        var filteredVideos = rawVideos

        if (downloadedOnly) {
            val moviesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
            val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
            filteredVideos = filteredVideos.filter {
                val dir = if (it.isImage) picturesDir else moviesDir
                java.io.File(dir, "NextPlayer/${it.nameWithExtension}").exists()
            }
        }

        when (sort) {
            TelegramSortOrder.NAME_ASC -> filteredVideos.sortedBy { it.nameWithExtension.lowercase() }
            TelegramSortOrder.NAME_DESC -> filteredVideos.sortedByDescending { it.nameWithExtension.lowercase() }
            TelegramSortOrder.SIZE_ASC -> filteredVideos.sortedBy { it.size }
            TelegramSortOrder.SIZE_DESC -> filteredVideos.sortedByDescending { it.size }
            TelegramSortOrder.DURATION_ASC -> filteredVideos.sortedBy { it.duration }
            TelegramSortOrder.DURATION_DESC -> filteredVideos.sortedByDescending { it.duration }
            TelegramSortOrder.DATE_ADDED_DESC -> filteredVideos
            TelegramSortOrder.DATE_ADDED_ASC -> filteredVideos.reversed()
        }
    }.flowOn(Dispatchers.IO)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isChannelLoading = MutableStateFlow(false)
    val isChannelLoading: StateFlow<Boolean> = _isChannelLoading.asStateFlow()

    private val _isChannelLoadingMore = MutableStateFlow(false)
    val isChannelLoadingMore: StateFlow<Boolean> = _isChannelLoadingMore.asStateFlow()

    private val _isChannelPostsLoadingMore = MutableStateFlow(false)
    val isChannelPostsLoadingMore: StateFlow<Boolean> = _isChannelPostsLoadingMore.asStateFlow()

    private val _isChannelPostsEndReached = MutableStateFlow(false)
    val isChannelPostsEndReached: StateFlow<Boolean> = _isChannelPostsEndReached.asStateFlow()

    private var channelVideosPage = 0
    private val PAGE_SIZE = 50
    private var isChannelVideosEndReached = false
    private var loadedSearchQuery = ""


    val uiState: StateFlow<TelegramUiState> = combine(
        combine(_isLoading, _error) { loading, err -> Pair(loading, err) },
        _rawVideos,
        _searchQuery,
        _sortOrder,
        _showDownloadedOnly
    ) { loadingAndError, rawVideos, query, sort, downloadedOnly ->
        val isLoading = loadingAndError.first
        val error = loadingAndError.second
        if (error != null) {
            TelegramUiState.Error(error)
        } else if (isLoading && rawVideos.isEmpty()) {
            TelegramUiState.Loading
        } else {
            var filteredVideos = if (query.isBlank()) {
                rawVideos
            } else {
                rawVideos.filter { it.nameWithExtension.contains(query, ignoreCase = true) }
            }

            if (downloadedOnly) {
                val currentDownloadedKeys = downloadedKeys.value
                filteredVideos = filteredVideos.filter {
                    isVideoDownloaded(it, currentDownloadedKeys)
                }
            }

            filteredVideos = when (sort) {
                TelegramSortOrder.NAME_ASC -> filteredVideos.sortedBy { it.nameWithExtension.lowercase() }
                TelegramSortOrder.NAME_DESC -> filteredVideos.sortedByDescending { it.nameWithExtension.lowercase() }
                TelegramSortOrder.SIZE_ASC -> filteredVideos.sortedBy { it.size }
                TelegramSortOrder.SIZE_DESC -> filteredVideos.sortedByDescending { it.size }
                TelegramSortOrder.DURATION_ASC -> filteredVideos.sortedBy { it.duration }
                TelegramSortOrder.DURATION_DESC -> filteredVideos.sortedByDescending { it.duration }
                TelegramSortOrder.DATE_ADDED_DESC -> filteredVideos
                TelegramSortOrder.DATE_ADDED_ASC -> filteredVideos.reversed()
            }

            TelegramUiState.Success(
                videos = filteredVideos,
                isRefreshing = isLoading,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TelegramUiState.Loading
    )

    val likedVideos = telegramLikedRepository.likedVideos

    fun isLiked(key: String?): Boolean = telegramLikedRepository.isLiked(key)
    fun toggleLike(video: Video): Boolean = telegramLikedRepository.toggleLike(video, preferencesRepository.playerPreferences.value.telegramToken)

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences
                .map { it.telegramToken }
                .distinctUntilChanged()
                .collect { token ->
                    telegramLikedRepository.loadForUser(token)
                    // Clear all in-memory states from previous account
                    _rawVideos.value = emptyList()
                    _channels.value = emptyList()
                    _selectedChannel.value = null
                    _channelVideos.value = emptyList()
                    _channelPosts.value = emptyList()
                    _channelStats.value = null
                    _selectedPostType.value = null

                    fetchVideos()
                    fetchChannels()
                    if (token.isNotEmpty()) {
                        refreshUserProfile()
                    }
                }
        }
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _searchQuery
                .debounce(350)
                .distinctUntilChanged()
                .collect { query ->
                    val channel = _selectedChannel.value
                    val token = preferencesRepository.playerPreferences.value.telegramToken
                    if (channel != null && token.isNotEmpty() && query.isNotBlank()) {
                        try {
                            val serverPosts = telegramRepository.getChannelPosts(
                                channelId = channel.channel_id,
                                token = token,
                                skip = 0,
                                limit = 50,
                                searchQuery = query,
                                postType = _selectedPostType.value
                            )
                            if (serverPosts.isNotEmpty()) {
                                _channelPosts.value = (_channelPosts.value + serverPosts).distinctBy { it.postId }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
        }
    }


    fun refreshUserProfile() {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                val authResponse = telegramRepository.verifyToken(token)
                if (authResponse != null) {
                    val streamConfig = authResponse.streamConfig
                    val botToken = streamConfig?.botToken
                    val apiId = streamConfig?.apiId ?: 0
                    val apiHash = streamConfig?.apiHash ?: ""

                    preferencesRepository.updatePlayerPreferences {
                        it.copy(
                            telegramUserId = authResponse.user_id,
                            telegramUsername = authResponse.username ?: it.telegramUsername,
                            telegramFirstName = authResponse.first_name ?: it.telegramFirstName,
                            telegramProfilePhoto = authResponse.profile_photo ?: it.telegramProfilePhoto,
                            telegramBotToken = if (!botToken.isNullOrEmpty()) botToken else it.telegramBotToken,
                            telegramApiId = if (apiId != 0) apiId else it.telegramApiId,
                            telegramApiHash = if (apiHash.isNotEmpty()) apiHash else it.telegramApiHash
                        )
                    }

                    if (!botToken.isNullOrEmpty() && apiId != 0 && apiHash.isNotEmpty()) {
                        tdlibManager.initialize(
                            botToken = botToken,
                            apiId = apiId,
                            apiHash = apiHash
                        )
                    }
                } else {
                    val profileResponse = telegramRepository.getUserProfile(token)
                    if (profileResponse != null) {
                        preferencesRepository.updatePlayerPreferences {
                            it.copy(
                                telegramFirstName = profileResponse.firstName ?: "",
                                telegramUsername = profileResponse.username ?: "Unknown",
                                telegramProfilePhoto = profileResponse.profilePhoto ?: ""
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchVideos() {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) {
            _rawVideos.value = emptyList()
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            if (_rawVideos.value.isEmpty()) {
                val cached = telegramCacheManager.getCloudVideos(token)
                if (cached.isNotEmpty()) {
                    _rawVideos.value = cached.filter { it.parentPath != "Channel Videos" && !it.uriString.contains("chatId=-") }
                }
            }
            _error.value = null
            _isLoading.value = true
            try {
                telegramRepository.getTelegramVideosFlow(token).collect { videos ->
                    _rawVideos.value = videos.filter { it.parentPath != "Channel Videos" && !it.uriString.contains("chatId=-") }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                val cached = telegramCacheManager.getCloudVideos(token)
                if (cached.isNotEmpty() || _rawVideos.value.isNotEmpty()) {
                    if (cached.isNotEmpty()) {
                        _rawVideos.value = cached.filter { it.parentPath != "Channel Videos" && !it.uriString.contains("chatId=-") }
                    }
                    _error.value = null
                } else {
                    _error.value = e.message ?: "Connection error"
                }
                _isLoading.value = false
            }
        }
    }

    fun fetchChannels() {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) {
            _channels.value = emptyList()
            _selectedChannel.value = null
            _channelVideos.value = emptyList()
            _channelPosts.value = emptyList()
            return
        }
        viewModelScope.launch {
            val cached = telegramCacheManager.getChannels(token)
            if (cached.isNotEmpty() && _channels.value.isEmpty()) {
                _channels.value = cached.distinctBy { it.channel_id }
                cached.forEach { ch ->
                    val u = ch.channel_username
                    if (!u.isNullOrEmpty()) {
                        tdlibManager.registerChannelUsername(ch.channel_id, u)
                    }
                }
            }
            try {
                val response = telegramRepository.getChannels(token)
                _channels.value = response.distinctBy { it.channel_id }
                telegramCacheManager.saveChannels(response, token)
                response.forEach { ch ->
                    val u = ch.channel_username
                    if (!u.isNullOrEmpty()) {
                        tdlibManager.registerChannelUsername(ch.channel_id, u)
                    }
                }

                if (response.isEmpty()) {
                    _selectedChannel.value = null
                    _channelVideos.value = emptyList()
                    _channelPosts.value = emptyList()
                } else if (_selectedChannel.value != null && response.none { it.channel_id == _selectedChannel.value?.channel_id }) {
                    selectChannel(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addChannel(username: String, onResult: (Boolean, String?) -> Unit) {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) {
            onResult(false, "Not logged in")
            return
        }
        viewModelScope.launch {
            _isChannelLoading.value = true
            try {
                val response = telegramRepository.addChannel(username, token)
                if (response != null) {
                    onResult(true, "Channel added! Scanning for videos...")
                    fetchChannels() // refresh list
                    selectChannel(response)
                } else {
                    onResult(false, "Failed to add channel")
                    _isChannelLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.message ?: "Unknown error")
                _isChannelLoading.value = false
            }
        }
    }

    fun removeChannel(channelId: Long) {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                val success = telegramRepository.removeChannel(channelId, token)
                if (success) {
                    if (_selectedChannel.value?.channel_id == channelId) {
                        _selectedChannel.value = null
                        _channelVideos.value = emptyList()
                        _channelPosts.value = emptyList()
                        _channelStats.value = null
                    }
                    fetchChannels()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectChannel(channel: ChannelResponse?) {
        val prevChannelId = _selectedChannel.value?.channel_id
        if (prevChannelId != null && _channelPosts.value.isNotEmpty()) {
            channelPostsCache[prevChannelId] = _channelPosts.value
        }
        _searchQuery.value = ""
        _isChannelPostsEndReached.value = false

        channelPostsJob?.cancel()
        channelVideosJob?.cancel()

        if (channel != null) {
            val token = preferencesRepository.playerPreferences.value.telegramToken
            // Immediately restore cached channel content for this channel
            if (prevChannelId != channel.channel_id) {
                val cachedPosts = channelPostsCache[channel.channel_id]
                    ?: (if (token.isNotEmpty()) telegramCacheManager.getChannelPosts(channel.channel_id, token) else emptyList())
                _channelPosts.value = cachedPosts
                _channelStats.value = if (token.isNotEmpty()) telegramCacheManager.getChannelStats(channel.channel_id, token) else null
                _channelVideos.value = emptyList()
            }
            _selectedChannel.value = channel

            val username = channel.channel_username
            if (!username.isNullOrEmpty()) {
                tdlibManager.registerChannelUsername(channel.channel_id, username)
                tdlibManager.searchPublicChat(username)
            } else {
                tdlibManager.ensureChat(channel.channel_id)
            }
            loadChannelVideos(channel.channel_id, isInitialLoad = true, searchQuery = "")
            loadChannelPosts(channel.channel_id, isInitialLoad = true, searchQuery = "", postType = _selectedPostType.value)
        } else {
            _channelVideos.value = emptyList()
            _channelPosts.value = emptyList()
            _channelStats.value = null
            _selectedChannel.value = null
        }
    }

    fun loadMoreChannelPosts() {
        val channel = _selectedChannel.value ?: return
        loadChannelPosts(
            channelId = channel.channel_id,
            isInitialLoad = false,
            searchQuery = _searchQuery.value,
            postType = _selectedPostType.value
        )
    }

    fun loadMoreChannelVideos() {
        val channel = _selectedChannel.value ?: return
        loadChannelVideos(channel.channel_id, isInitialLoad = false, searchQuery = _searchQuery.value)
        loadMoreChannelPosts()
    }

    fun loadChannelVideos(channelId: Long, isInitialLoad: Boolean = true, searchQuery: String = "") {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) return

        if (isInitialLoad) {
            channelVideosJob?.cancel()
            _isChannelLoading.value = true
            isChannelVideosEndReached = false
            loadedSearchQuery = searchQuery
        } else {
            if (searchQuery != loadedSearchQuery) return
            if (_isChannelLoadingMore.value || isChannelVideosEndReached) return
            _isChannelLoadingMore.value = true
        }

        channelVideosJob = viewModelScope.launch {
            if (_selectedChannel.value?.channel_id != channelId) return@launch
            val skip = if (isInitialLoad) 0 else _channelVideos.value.size
            try {
                // Instantaneous MTProto Server Search via TDLib searchChatMessages
                if (searchQuery.isNotBlank() && tdlibManager.isReady()) {
                    val fromMsgId = if (isInitialLoad) 0L else (_channelVideos.value.lastOrNull()?.id ?: 0L)
                    val serverRes = serverSearchManager.searchVideos(channelId, searchQuery, fromMessageId = fromMsgId, limit = PAGE_SIZE)
                    if (serverRes.videos.isNotEmpty()) {
                        if (_selectedChannel.value?.channel_id != channelId) return@launch
                        if (isInitialLoad) {
                            _channelVideos.value = serverRes.videos.distinctBy { it.id }
                        } else {
                            _channelVideos.value = (_channelVideos.value + serverRes.videos).distinctBy { it.id }
                        }
                        if (!serverRes.hasMore) {
                            isChannelVideosEndReached = true
                        }
                        return@launch
                    }
                }

                val videos = telegramRepository.getChannelVideos(channelId, token, skip, PAGE_SIZE, searchQuery.takeIf { it.isNotEmpty() })
                if (_selectedChannel.value?.channel_id != channelId) return@launch
                if (isInitialLoad) {
                    _channelVideos.value = videos.distinctBy { it.id }
                } else {
                    _channelVideos.value = (_channelVideos.value + videos).distinctBy { it.id }
                }
                if (videos.isEmpty() || videos.size < PAGE_SIZE) {
                    isChannelVideosEndReached = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isInitialLoad && _selectedChannel.value?.channel_id == channelId && _channelVideos.value.isEmpty()) {
                    _channelVideos.value = emptyList()
                }
            } finally {
                if (isInitialLoad && _selectedChannel.value?.channel_id == channelId) {
                    _isChannelLoading.value = false
                } else {
                    _isChannelLoadingMore.value = false
                }
            }
        }
    }

    private var channelPostsJob: kotlinx.coroutines.Job? = null
    private val POSTS_PAGE_SIZE = 50
    private var loadedPostsSearchQuery = ""
    private var loadedPostTypeFilter: String? = null

    fun setPostTypeFilter(type: String?) {
        _selectedPostType.value = type
        val channel = _selectedChannel.value
        if (channel != null) {
            loadChannelPosts(channel.channel_id, isInitialLoad = true, searchQuery = _searchQuery.value, postType = type)
        }
    }

    fun refreshChannel(channelId: Long) {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) return
        viewModelScope.launch {
            _isChannelLoading.value = true
            try {
                telegramRepository.refreshChannel(channelId, token)
                kotlinx.coroutines.delay(2000)
                loadChannelPosts(channelId, isInitialLoad = true, searchQuery = _searchQuery.value, postType = _selectedPostType.value)
                fetchChannels()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isChannelLoading.value = false
            }
        }
    }

    fun loadChannelPosts(channelId: Long, isInitialLoad: Boolean = true, searchQuery: String = "", postType: String? = null) {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) return

        if (isInitialLoad) {
            channelPostsJob?.cancel()
            _isChannelLoading.value = true
            _isChannelPostsEndReached.value = false
            loadedPostsSearchQuery = searchQuery
            loadedPostTypeFilter = postType
        } else {
            if (searchQuery != loadedPostsSearchQuery || postType != loadedPostTypeFilter) return
            if (_isChannelPostsLoadingMore.value || _isChannelPostsEndReached.value) return
            _isChannelPostsLoadingMore.value = true
        }

        channelPostsJob = viewModelScope.launch {
            if (_selectedChannel.value?.channel_id != channelId) return@launch
            val skip = if (isInitialLoad) 0 else _channelPosts.value.size
            try {
                if (isInitialLoad) {
                    val stats = telegramRepository.getChannelStats(channelId, token) ?: telegramCacheManager.getChannelStats(channelId, token)
                    if (_selectedChannel.value?.channel_id == channelId) {
                        _channelStats.value = stats
                    }
                    if (stats != null) {
                        telegramCacheManager.saveChannelStats(channelId, stats, token)
                    }
                }

                var attempts = 0
                val maxAttempts = if (isInitialLoad && searchQuery.isEmpty() && _channelPosts.value.isEmpty()) 3 else 1
                while (attempts < maxAttempts) {
                    if (_selectedChannel.value?.channel_id != channelId) return@launch
                    val posts = telegramRepository.getChannelPosts(channelId, token, skip, POSTS_PAGE_SIZE, searchQuery.takeIf { it.isNotEmpty() }, postType)
                    if (_selectedChannel.value?.channel_id != channelId) return@launch

                    if (posts.isNotEmpty() || !isInitialLoad || searchQuery.isNotEmpty() || postType != null) {
                        if (isInitialLoad) {
                            val existing = channelPostsCache[channelId] ?: _channelPosts.value
                            val merged = if (existing.isNotEmpty() && searchQuery.isEmpty() && postType == null) {
                                (posts + existing).distinctBy { it.postId }
                            } else {
                                posts.distinctBy { it.postId }
                            }
                            _channelPosts.value = merged
                            channelPostsCache[channelId] = merged
                            telegramCacheManager.saveChannelPosts(channelId, merged, token)
                        } else {
                            val merged = (_channelPosts.value + posts).distinctBy { it.postId }
                            _channelPosts.value = merged
                            channelPostsCache[channelId] = merged
                            telegramCacheManager.saveChannelPosts(channelId, merged, token)
                        }
                        if (posts.isEmpty() || posts.size < POSTS_PAGE_SIZE) {
                            _isChannelPostsEndReached.value = true
                        }
                        break
                    }
                    attempts++
                    if (attempts < maxAttempts) {
                        kotlinx.coroutines.delay(1500)
                    }
                }
                if (attempts >= maxAttempts && isInitialLoad && _selectedChannel.value?.channel_id == channelId) {
                    if (_channelPosts.value.isEmpty()) {
                        val cached = channelPostsCache[channelId] ?: telegramCacheManager.getChannelPosts(channelId, token)
                        if (cached.isNotEmpty()) {
                            _channelPosts.value = cached.distinctBy { it.postId }
                            channelPostsCache[channelId] = _channelPosts.value
                        } else {
                            _channelPosts.value = emptyList()
                        }
                    }
                    _isChannelPostsEndReached.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isInitialLoad && _selectedChannel.value?.channel_id == channelId && _channelPosts.value.isEmpty()) {
                    val cached = channelPostsCache[channelId] ?: telegramCacheManager.getChannelPosts(channelId, token)
                    if (cached.isNotEmpty()) {
                        _channelPosts.value = cached.distinctBy { it.postId }
                        channelPostsCache[channelId] = _channelPosts.value
                    } else {
                        _channelPosts.value = emptyList()
                    }
                }
            } finally {
                if (isInitialLoad && _selectedChannel.value?.channel_id == channelId) {
                    _isChannelLoading.value = false
                }
                _isChannelPostsLoadingMore.value = false
            }
        }
    }


    val savedAccounts: StateFlow<List<dev.anilbeesetti.nextplayer.core.model.TelegramAccount>> = preferencesRepository.playerPreferences
        .map { prefs ->
            val currentToken = prefs.telegramToken
            val list = prefs.savedTelegramAccounts.toMutableList()
            if (currentToken.isNotEmpty() && list.none { it.token == currentToken || (it.userId != 0L && it.userId == prefs.telegramUserId) }) {
                list.add(
                    0,
                    dev.anilbeesetti.nextplayer.core.model.TelegramAccount(
                        token = currentToken,
                        userId = prefs.telegramUserId,
                        username = prefs.telegramUsername,
                        firstName = prefs.telegramFirstName,
                        profilePhoto = prefs.telegramProfilePhoto,
                        botToken = prefs.telegramBotToken,
                        apiId = prefs.telegramApiId,
                        apiHash = prefs.telegramApiHash
                    )
                )
            }
            list
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun switchAccount(account: dev.anilbeesetti.nextplayer.core.model.TelegramAccount) {
        viewModelScope.launch {
            _rawVideos.value = emptyList()
            _channels.value = emptyList()
            _selectedChannel.value = null
            _channelVideos.value = emptyList()
            _channelPosts.value = emptyList()
            _channelStats.value = null
            _selectedPostType.value = null

            preferencesRepository.updatePlayerPreferences {
                val existing = it.savedTelegramAccounts.filterNot { a -> a.token == account.token || (a.userId != 0L && a.userId == account.userId) }
                it.copy(
                    telegramToken = account.token,
                    telegramUserId = account.userId,
                    telegramUsername = account.username,
                    telegramFirstName = account.firstName,
                    telegramProfilePhoto = account.profilePhoto,
                    telegramBotToken = account.botToken,
                    telegramApiId = account.apiId,
                    telegramApiHash = account.apiHash,
                    savedTelegramAccounts = listOf(account) + existing
                )
            }

            if (account.botToken.isNotEmpty() && account.apiId != 0 && account.apiHash.isNotEmpty()) {
                tdlibManager.initialize(
                    botToken = account.botToken,
                    apiId = account.apiId,
                    apiHash = account.apiHash
                )
            }

            fetchVideos()
            fetchChannels()
            refreshUserProfile()
        }
    }

    fun addAccount(token: String, onResult: (Boolean, String?) -> Unit) {
        val clean = cleanToken(token)
        if (clean.isBlank()) {
            onResult(false, "Token cannot be empty")
            return
        }
        viewModelScope.launch {
            _isLinking.value = true
            try {
                _rawVideos.value = emptyList()
                _channels.value = emptyList()
                _selectedChannel.value = null
                _channelVideos.value = emptyList()
                _channelPosts.value = emptyList()
                _channelStats.value = null
                _selectedPostType.value = null

                val response = telegramRepository.verifyToken(clean)
                if (response != null && response.success) {
                    val streamConfig = response.streamConfig
                    val newAccount = dev.anilbeesetti.nextplayer.core.model.TelegramAccount(
                        token = clean,
                        userId = response.user_id,
                        username = response.username ?: "Unknown",
                        firstName = response.first_name ?: "",
                        profilePhoto = response.profile_photo ?: "",
                        botToken = streamConfig?.botToken ?: "",
                        apiId = streamConfig?.apiId ?: 0,
                        apiHash = streamConfig?.apiHash ?: ""
                    )

                    preferencesRepository.updatePlayerPreferences {
                        val existing = it.savedTelegramAccounts.filterNot { a -> a.token == clean || (a.userId != 0L && a.userId == response.user_id) }
                        it.copy(
                            telegramToken = clean,
                            telegramUserId = response.user_id,
                            telegramUsername = response.username ?: "Unknown",
                            telegramFirstName = response.first_name ?: "",
                            telegramProfilePhoto = response.profile_photo ?: "",
                            telegramBotToken = streamConfig?.botToken ?: "",
                            telegramApiId = streamConfig?.apiId ?: 0,
                            telegramApiHash = streamConfig?.apiHash ?: "",
                            savedTelegramAccounts = listOf(newAccount) + existing
                        )
                    }

                    val botToken = streamConfig?.botToken
                    if (!botToken.isNullOrEmpty() && streamConfig != null) {
                        tdlibManager.initialize(
                            botToken = botToken,
                            apiId = streamConfig.apiId ?: 0,
                            apiHash = streamConfig.apiHash ?: ""
                        )
                    }
                    fetchVideos()
                    fetchChannels()
                    refreshUserProfile()
                    onResult(true, null)
                } else {
                    onResult(false, "Invalid or expired linking code. Send /token to bot.")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Connection error")
            } finally {
                _isLinking.value = false
            }
        }
    }

    fun removeAccount(account: dev.anilbeesetti.nextplayer.core.model.TelegramAccount) {
        viewModelScope.launch {
            val prefs = preferencesRepository.playerPreferences.value
            val currentToken = prefs.telegramToken
            val remainingAccounts = prefs.savedTelegramAccounts.filterNot { it.token == account.token && it.userId == account.userId }

            telegramCacheManager.clearUserData(account.token)

            if (currentToken == account.token || (prefs.telegramUserId != 0L && prefs.telegramUserId == account.userId)) {
                if (remainingAccounts.isNotEmpty()) {
                    preferencesRepository.updatePlayerPreferences {
                        it.copy(savedTelegramAccounts = remainingAccounts)
                    }
                    switchAccount(remainingAccounts.first())
                } else {
                    disconnectAccount()
                }
            } else {
                preferencesRepository.updatePlayerPreferences {
                    it.copy(savedTelegramAccounts = remainingAccounts)
                }
            }
        }
    }

    fun linkAccount(token: String, onResult: (Boolean) -> Unit) {
        val clean = cleanToken(token)
        if (clean.isBlank()) {
            _linkError.value = "Token cannot be empty"
            onResult(false)
            return
        }
        viewModelScope.launch {
            _isLinking.value = true
            _linkError.value = null
            try {
                // Clear any previous user state immediately
                _rawVideos.value = emptyList()
                _channels.value = emptyList()
                _selectedChannel.value = null
                _channelVideos.value = emptyList()
                _channelPosts.value = emptyList()
                _channelStats.value = null
                _selectedPostType.value = null

                val response = telegramRepository.verifyToken(clean)
                if (response != null && response.success) {
                    val streamConfig = response.streamConfig
                    val newAccount = dev.anilbeesetti.nextplayer.core.model.TelegramAccount(
                        token = clean,
                        userId = response.user_id,
                        username = response.username ?: "Unknown",
                        firstName = response.first_name ?: "",
                        profilePhoto = response.profile_photo ?: "",
                        botToken = streamConfig?.botToken ?: "",
                        apiId = streamConfig?.apiId ?: 0,
                        apiHash = streamConfig?.apiHash ?: ""
                    )
                    preferencesRepository.updatePlayerPreferences {
                        val existing = it.savedTelegramAccounts.filterNot { a -> a.token == clean || (a.userId != 0L && a.userId == response.user_id) }
                        it.copy(
                            telegramToken = clean,
                            telegramUserId = response.user_id,
                            telegramUsername = response.username ?: "Unknown",
                            telegramFirstName = response.first_name ?: "",
                            telegramProfilePhoto = response.profile_photo ?: "",
                            telegramBotToken = streamConfig?.botToken ?: "",
                            telegramApiId = streamConfig?.apiId ?: 0,
                            telegramApiHash = streamConfig?.apiHash ?: "",
                            savedTelegramAccounts = listOf(newAccount) + existing
                        )
                    }
                    val botToken = streamConfig?.botToken
                    if (!botToken.isNullOrEmpty() && streamConfig != null) {
                        tdlibManager.initialize(
                            botToken = botToken,
                            apiId = streamConfig.apiId ?: 0,
                            apiHash = streamConfig.apiHash ?: ""
                        )
                    }
                    fetchVideos()
                    fetchChannels()
                    refreshUserProfile()
                    onResult(true)
                } else {
                    _linkError.value = "Invalid or banned linking code"
                    onResult(false)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                _linkError.value = e.message ?: "Connection error"
                onResult(false)
            } finally {
                _isLinking.value = false
            }
        }
    }

    fun disconnectAccount() {
        viewModelScope.launch {
            val oldToken = preferencesRepository.playerPreferences.value.telegramToken
            tdlibManager.shutdown()
            telegramCacheManager.clearUserData(oldToken)
            telegramCacheManager.clearAll()
            telegramRepository.clearCache()
            preferencesRepository.updatePlayerPreferences {
                it.copy(
                    telegramToken = "",
                    telegramUserId = 0L,
                    telegramUsername = "",
                    telegramFirstName = "",
                    telegramProfilePhoto = "",
                    telegramBotToken = "",
                    telegramApiId = 0,
                    telegramApiHash = ""
                )
            }
            _rawVideos.value = emptyList()
            _channels.value = emptyList()
            _selectedChannel.value = null
            _channelVideos.value = emptyList()
            _channelPosts.value = emptyList()
            _channelStats.value = null
            _selectedPostType.value = null
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: TelegramSortOrder) {
        _sortOrder.value = order
    }

    fun setShowDownloadedOnly(onlyDownloaded: Boolean) {
        _showDownloadedOnly.value = onlyDownloaded
    }

    fun toggleStats() {
        _showStats.value = !_showStats.value
    }

    fun deleteVideo(dbId: String) {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) return
        viewModelScope.launch {
            val success = telegramRepository.deleteVideo(dbId, token)
            if (success) {
                fetchVideos()
            }
        }
    }

    fun createShareLink(fileId: String, durationHours: Int, onResult: (String?) -> Unit) {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) {
            onResult(null)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val shareUrl = telegramRepository.createShareLink(token, fileId, durationHours)
            _isLoading.value = false
            onResult(shareUrl)
        }
    }

    fun createSyncRoom(fileId: String, onResult: (String?, String?) -> Unit) {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) {
            onResult(null, "Not logged in")
            return
        }
        viewModelScope.launch {
            val response = watchPartyRepository.createSyncRoom(token, fileId)
            if (response != null) {
                onResult(response.roomId, null)
            } else {
                onResult(null, "Failed to create watch party")
            }
        }
    }

    fun joinSyncRoom(roomId: String, onResult: (String?, String?, String?) -> Unit) {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) {
            onResult(null, null, "Not logged in")
            return
        }
        viewModelScope.launch {
            val response = watchPartyRepository.joinSyncRoom(roomId, token)
            if (response != null) {
                val fullUrl = dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.streamUrl(response.fileId)
                onResult(fullUrl, response.videoName ?: "Watch Party Video", null)
            } else {
                onResult(null, null, "Room not found or is full")
            }
        }
    }


    val downloadProgress: StateFlow<Map<String, Float>> = telegramDownloadManager.activeDownloads
    val activeDownloadTasks: StateFlow<Map<String, dev.anilbeesetti.nextplayer.core.data.download.DownloadTask>> = telegramDownloadManager.activeDownloadTasks
    val downloadedKeys: StateFlow<Set<String>> = telegramDownloadManager.downloadedKeys

    val allKnownVideos: StateFlow<List<Video>> = combine(
        _rawVideos,
        allChannelVideos,
        likedVideos
    ) { raw, ch, liked ->
        (raw + ch + liked).distinctBy { it.externalId ?: it.path }
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val downloadedVideos: StateFlow<List<Video>> = combine(
        downloadedKeys,
        allKnownVideos
    ) { _, all ->
        withContext(Dispatchers.IO) {
            telegramDownloadManager.getDownloadedVideos(all)
        }
    }.flowOn(Dispatchers.IO)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLibraryVideos: StateFlow<List<Video>> = combine(
        downloadedVideos,
        likedVideos
    ) { downloaded, liked ->
        (downloaded + liked).distinctBy { it.externalId ?: it.path }
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val failedDownloads = telegramDownloadManager.failedDownloads

    fun dismissFailedDownload(key: String) {
        telegramDownloadManager.dismissFailedDownload(key)
    }

    fun retryFailedDownload(key: String) {
        val errorInfo = telegramDownloadManager.failedDownloads.value[key]
        telegramDownloadManager.dismissFailedDownload(key)
        val video = errorInfo?.video
            ?: _rawVideos.value.firstOrNull { (it.externalId ?: it.id.toString()) == key || it.nameWithExtension == errorInfo?.fileName }
            ?: _channelVideos.value.firstOrNull { (it.externalId ?: it.id.toString()) == key || it.nameWithExtension == errorInfo?.fileName }
            ?: allKnownVideos.value.firstOrNull { (it.externalId ?: it.id.toString()) == key || it.nameWithExtension == errorInfo?.fileName }
        if (video != null) {
            telegramDownloadManager.downloadToWatch(video)
        }
    }

    fun cancelDownload(videoKey: String) {
        telegramDownloadManager.cancelDownload(videoKey)
    }

    fun cancelDownload(video: Video) {
        telegramDownloadManager.cancelDownload(video.externalId ?: video.id.toString())
    }

    fun pauseDownload(videoKey: String) {
        telegramDownloadManager.pauseDownload(videoKey)
    }

    fun pauseDownload(video: Video) {
        telegramDownloadManager.pauseDownload(video.externalId ?: video.id.toString())
    }

    fun resumeDownload(videoKey: String) {
        telegramDownloadManager.resumeDownload(videoKey)
    }

    fun resumeDownload(video: Video) {
        telegramDownloadManager.resumeDownload(video)
    }

    fun isDownloadPaused(videoKey: String): Boolean {
        return telegramDownloadManager.isDownloadPaused(videoKey)
    }

    fun isDownloadPaused(video: Video): Boolean {
        return telegramDownloadManager.isDownloadPaused(video)
    }

    fun isSavingToGallery(video: Video): Boolean {
        return telegramDownloadManager.isSavingToGallery(video)
    }

    fun pauseAllDownloads() {
        telegramDownloadManager.pauseAllDownloads()
    }

    fun resumeAllDownloads() {
        telegramDownloadManager.resumeAllDownloads()
    }

    fun cancelAllDownloads() {
        telegramDownloadManager.cancelAllDownloads()
    }

    fun downloadToWatch(video: Video) {
        telegramDownloadManager.downloadToWatch(video)
    }

    fun saveToGallery(video: Video) {
        telegramDownloadManager.saveToGallery(video)
    }

    fun deleteFromOffline(video: Video): Boolean {
        return telegramDownloadManager.deleteFromOffline(video)
    }

    fun refreshDownloadedVideos() {
        telegramDownloadManager.refreshDownloadedKeys()
    }

    fun isVideoDownloaded(video: Video, keys: Set<String> = downloadedKeys.value): Boolean {
        if (keys.contains(video.nameWithExtension)) return true
        if (video.externalId != null && keys.contains(video.externalId)) return true
        if (keys.contains(video.id.toString())) return true
        val safeName = dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.sanitizeFileName(video.nameWithExtension)
        if (keys.contains(safeName) || keys.contains(safeName.removeSuffix(".mp4"))) return true
        val decoded = try { java.net.URLDecoder.decode(video.nameWithExtension, "UTF-8") } catch (_: Exception) { video.nameWithExtension }
        if (keys.contains(decoded) || keys.contains(decoded.removeSuffix(".mp4"))) return true
        return telegramDownloadManager.isDownloadedToWatch(video)
    }

    fun isDownloadedToWatch(video: Video): Boolean {
        return isVideoDownloaded(video, downloadedKeys.value)
    }

    fun getPlayableUri(video: Video): android.net.Uri {
        val offlineFile = telegramDownloadManager.getOfflineFile(video)
        if (offlineFile != null && offlineFile.exists() && !offlineFile.name.endsWith(".part") && offlineFile.length() >= dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.MIN_VALID_VIDEO_SIZE) {
            return android.net.Uri.fromFile(offlineFile)
        }
        if (video.path.startsWith("file://") || video.path.startsWith("/")) {
            val rawPath = if (video.path.startsWith("file://")) android.net.Uri.parse(video.path).path else video.path
            val decodedPath = try {
                if (rawPath != null) java.net.URLDecoder.decode(rawPath, "UTF-8") else null
            } catch (_: Exception) { rawPath }
            val directF = decodedPath?.let { java.io.File(it) }?.takeIf { it.exists() && it.length() >= dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.MIN_VALID_VIDEO_SIZE && !it.name.endsWith(".part") }
                ?: rawPath?.let { java.io.File(it) }?.takeIf { it.exists() && it.length() >= dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.MIN_VALID_VIDEO_SIZE && !it.name.endsWith(".part") }
            if (directF != null) {
                return android.net.Uri.fromFile(directF)
            }
        }
        val extId = video.externalId
        val baseRemoteStream = when {
            video.uriString.startsWith("tdlib://") -> video.uriString
            video.path.startsWith("http") -> video.path
            video.uriString.startsWith("http") -> video.uriString
            !extId.isNullOrEmpty() -> dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.streamUrl(extId)
            else -> video.uriString
        }
        val remoteStream = if (baseRemoteStream.startsWith("tdlib://") && !baseRemoteStream.contains("username=")) {
            try {
                val parsedUri = android.net.Uri.parse(baseRemoteStream)
                val cId = parsedUri.getQueryParameter("chatId")?.toLongOrNull()
                if (cId != null) {
                    val token = preferencesRepository.playerPreferences.value.telegramToken
                    val u = tdlibManager.getRegisteredChannelUsername(cId)
                        ?: telegramCacheManager.getChannelUsername(cId, token)
                    if (!u.isNullOrEmpty()) {
                        tdlibManager.registerChannelUsername(cId, u)
                        val sep = if (baseRemoteStream.contains("?")) "&" else "?"
                        "$baseRemoteStream${sep}username=${java.net.URLEncoder.encode(u, "UTF-8")}"
                    } else baseRemoteStream
                } else baseRemoteStream
            } catch (_: Exception) { baseRemoteStream }
        } else baseRemoteStream

        val fallbackUrl = if (!extId.isNullOrEmpty()) dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.streamUrl(extId) else ""
        dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(video.nameWithExtension, remoteStream)
        if (fallbackUrl.isNotEmpty()) {
            dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(video.nameWithExtension, fallbackUrl)
        }
        extId?.let {
            dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, remoteStream)
            if (fallbackUrl.isNotEmpty()) {
                dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, fallbackUrl)
            }
        }
        return if (remoteStream.startsWith("file://") && !extId.isNullOrEmpty()) {
            android.net.Uri.parse(dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.streamUrl(extId))
        } else {
            android.net.Uri.parse(remoteStream)
        }
    }

    fun getPlayableUriForPost(post: dev.anilbeesetti.nextplayer.core.data.network.models.ChannelPostDto): android.net.Uri {
        val safeName = try {
            java.net.URLEncoder.encode(post.fileName ?: "video.mp4", "UTF-8")
        } catch (_: Exception) { "video.mp4" }
        val token = preferencesRepository.playerPreferences.value.telegramToken
        val tokenParam = if (token.isNotBlank()) "?token=$token" else ""
        val fallbackUrl = dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig.streamUrl(post.fileId ?: "", tokenParam)
        val encodedFallback = try {
            java.net.URLEncoder.encode(fallbackUrl, "UTF-8")
        } catch (_: Exception) { fallbackUrl }

        val effectiveChannelId = if (post.channelId != 0L) post.channelId else (_selectedChannel.value?.channel_id ?: 0L)
        val channelUser = _channels.value.firstOrNull {
            val chAbs = kotlin.math.abs(it.channel_id)
            val postAbs = kotlin.math.abs(effectiveChannelId)
            val chBare = if (chAbs > 1000000000000L) chAbs - 1000000000000L else chAbs
            val postBare = if (postAbs > 1000000000000L) postAbs - 1000000000000L else postAbs
            chBare == postBare
        }?.channel_username
            ?: _selectedChannel.value?.channel_username
            ?: tdlibManager.getRegisteredChannelUsername(effectiveChannelId)
            ?: (if (token.isNotBlank()) telegramCacheManager.getChannelUsername(effectiveChannelId, token) else null)

        if (effectiveChannelId != 0L && !channelUser.isNullOrEmpty()) {
            tdlibManager.registerChannelUsername(effectiveChannelId, channelUser)
        }
        val userParam = if (!channelUser.isNullOrEmpty()) "&username=${java.net.URLEncoder.encode(channelUser, "UTF-8")}" else ""
        val tokenQueryParam = if (token.isNotBlank()) "&token=${java.net.URLEncoder.encode(token, "UTF-8")}" else ""
        val streamUrl = "tdlib://stream?chatId=${effectiveChannelId}&messageId=${post.messageId}&fileId=${post.fileId}&size=${post.fileSize ?: 0L}&name=$safeName$userParam$tokenQueryParam&fallback=$encodedFallback"

        // Register fallbacks immediately so that if local file playback ever fails, DataSource can recover
        post.fileName?.let {
            dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, streamUrl)
            dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, fallbackUrl)
        }
        post.fileId?.let {
            dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, streamUrl)
            dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.registerFallbackUrl(it, fallbackUrl)
        }

        val expectedSize = post.fileSize ?: -1L
        val offlineFile = telegramDownloadManager.getOfflineFile(post.fileName, post.fileId, expectedSize)
        if (offlineFile != null && offlineFile.exists() && !offlineFile.name.endsWith(".part")) {
            val fileLen = offlineFile.length()
            val isComplete = if (expectedSize > 0L) {
                fileLen >= (expectedSize * 0.95).toLong()
            } else {
                fileLen >= dev.anilbeesetti.nextplayer.core.data.download.TelegramDownloadManager.MIN_VALID_VIDEO_SIZE
            }
            if (isComplete) {
                return android.net.Uri.fromFile(offlineFile)
            } else {
                android.util.Log.i("TelegramViewModel", "Offline file is downloading/incomplete ($fileLen < expected $expectedSize), streaming via network without deleting partial file")
            }
        }
        return android.net.Uri.parse(streamUrl)
    }

    fun startDownload(context: android.content.Context, video: Video) {
        telegramDownloadManager.downloadToWatch(video)
    }

    fun getChannelScrollPosition(channelId: Long): ChannelScrollPosition? {
        val inMemory = channelScrollPositions[channelId]
        if (inMemory != null) return inMemory
        val saved = preferencesRepository.playerPreferences.value.telegramChannelScrollPositions[channelId.toString()] ?: return null
        return try {
            if (saved.contains("|")) {
                val parts = saved.split("|")
                val pid = parts.getOrNull(0)?.takeIf { it.isNotBlank() }
                val idx = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val off = parts.getOrNull(2)?.toIntOrNull() ?: 0
                val mid = parts.getOrNull(3)?.toLongOrNull() ?: 0L
                val pos = ChannelScrollPosition(index = idx, offset = off, postId = pid, messageId = mid)
                channelScrollPositions[channelId] = pos
                pos
            } else {
                val pos = ChannelScrollPosition(postId = saved)
                channelScrollPositions[channelId] = pos
                pos
            }
        } catch (_: Exception) {
            ChannelScrollPosition(postId = saved)
        }
    }

    fun saveChannelScrollState(channelId: Long, index: Int, offset: Int, postId: String?, messageId: Long = 0L) {
        if (channelId == 0L) return
        val pos = ChannelScrollPosition(index = index, offset = offset, postId = postId, messageId = messageId)
        channelScrollPositions[channelId] = pos
        val encoded = "${postId ?: ""}|$index|$offset|$messageId"
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { prefs ->
                prefs.copy(
                    telegramChannelScrollPositions = prefs.telegramChannelScrollPositions + (channelId.toString() to encoded)
                )
            }
        }
    }

    fun saveChannelScrollPosition(channelId: Long, postId: String) {
        saveChannelScrollState(channelId, 0, 0, postId)
    }

    fun addToWatchLater(
        fileId: String,
        chatId: Long,
        messageId: Long,
        title: String,
        subtitle: String = "",
        fileSize: Long = 0,
        durationSeconds: Long = 0,
        streamUrl: String,
        artworkUri: String? = null
    ) {
        viewModelScope.launch {
            watchLaterRepository.addToQueue(
                fileId = fileId,
                chatId = chatId,
                messageId = messageId,
                title = title,
                subtitle = subtitle,
                fileSize = fileSize,
                durationSeconds = durationSeconds,
                streamUrl = streamUrl,
                artworkUri = artworkUri
            )
        }
    }

    fun removeWatchLaterItem(id: Long) {
        viewModelScope.launch {
            watchLaterRepository.removeFromQueue(id)
        }
    }

    fun downloadWatchLaterItem(item: dev.anilbeesetti.nextplayer.core.model.WatchLaterItem) {
        watchLaterRepository.startDownload(item)
    }

    fun downloadAllWatchLater() {
        watchLaterRepository.downloadAllPending()
    }

    fun clearCompletedWatchLater() {
        viewModelScope.launch {
            watchLaterRepository.clearCompleted()
        }
    }

    val tdlibStorageStats = storageManager.storageStats

    fun refreshTdlibStorageStats() {
        viewModelScope.launch {
            storageManager.refreshStorageStats()
        }
    }

    fun clearTdlibStreamCache(
        clearVideos: Boolean = true,
        clearThumbnails: Boolean = true,
        clearAudio: Boolean = false,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val success = storageManager.clearStreamCache(clearVideos, clearThumbnails, clearAudio)
            onComplete?.invoke(success)
        }
    }

    fun groupVideosByAlbum(videos: List<Video>) = albumGrouper.groupVideosByAlbum(videos)
}
