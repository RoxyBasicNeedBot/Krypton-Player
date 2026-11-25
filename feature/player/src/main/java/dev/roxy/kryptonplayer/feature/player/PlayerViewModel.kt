package dev.roxy.kryptonplayer.feature.player

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.data.repository.MediaRepository
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.domain.GetSortedPlaylistUseCase
import dev.roxy.kryptonplayer.core.model.ApplicationPreferences
import dev.roxy.kryptonplayer.core.model.LoopMode
import dev.roxy.kryptonplayer.core.model.PlayerPreferences
import dev.roxy.kryptonplayer.core.model.Video
import dev.roxy.kryptonplayer.core.model.VideoContentScale
import dev.roxy.kryptonplayer.feature.player.state.SubtitleOptionsEvent
import dev.roxy.kryptonplayer.feature.player.state.VideoZoomEvent
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import androidx.lifecycle.SavedStateHandle
import dev.roxy.kryptonplayer.core.data.network.SyncWebSocketClient
import dev.roxy.kryptonplayer.core.data.network.models.SyncMember
import dev.roxy.kryptonplayer.core.data.repository.WatchPartyRepository
import dev.roxy.kryptonplayer.core.data.lyrics.LyricsRepository
import dev.roxy.kryptonplayer.core.data.lyrics.LyricsUiState
import dev.roxy.kryptonplayer.core.data.lyrics.LyricsData

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getSortedPlaylistUseCase: GetSortedPlaylistUseCase,
    private val syncWebSocketClient: SyncWebSocketClient,
    private val watchPartyRepository: WatchPartyRepository,
    private val savedStateHandle: SavedStateHandle,
    private val onlineSubtitleRepository: dev.roxy.kryptonplayer.core.data.repository.OnlineSubtitleRepository,
    private val subtitleTranslator: dev.roxy.kryptonplayer.core.data.subtitles.SubtitleTranslator,
    private val usbMediaServer: dev.roxy.kryptonplayer.core.data.server.UsbMediaServer,
    private val usbConnectionHelper: dev.roxy.kryptonplayer.core.data.server.UsbConnectionHelper,
    private val bookmarkRepository: dev.roxy.kryptonplayer.core.data.repository.BookmarkRepository,
    private val tmdbRepository: dev.roxy.kryptonplayer.core.data.repository.TmdbRepository,
    private val soundtrackRepository: dev.roxy.kryptonplayer.core.data.repository.SoundtrackRepository,
    private val imdbSuggestApi: dev.roxy.kryptonplayer.core.data.network.subtitles.ImdbSuggestApi,
    private val lyricsRepository: dev.roxy.kryptonplayer.core.data.lyrics.LyricsRepository,
    val telegramLikedRepository: dev.roxy.kryptonplayer.core.data.repository.TelegramLikedRepository
) : ViewModel() {

    val likedKeys = telegramLikedRepository.likedKeys

    init {
        val initialToken = preferencesRepository.playerPreferences.value.telegramToken
        telegramLikedRepository.loadForUser(initialToken)
        viewModelScope.launch {
            preferencesRepository.playerPreferences
                .map { it.telegramToken }
                .distinctUntilChanged()
                .collect { token ->
                    telegramLikedRepository.loadForUser(token)
                }
        }
    }

    fun isMediaLiked(vararg candidateKeys: String?): Boolean {
        return candidateKeys.filterNotNull().any { telegramLikedRepository.isLiked(it) }
    }

    fun toggleLikeMedia(title: String, pathOrUri: String) {
        val token = preferencesRepository.playerPreferences.value.telegramToken
        val parsedUri = try { Uri.parse(pathOrUri) } catch (_: Exception) { null }
        val extractedId = when {
            pathOrUri.startsWith("tdlib://") -> parsedUri?.getQueryParameter("fileId")
            pathOrUri.contains("/api/stream/") -> pathOrUri.substringAfter("/api/stream/").substringBefore("?").substringBefore("/")
            else -> null
        }?.takeIf { it.isNotBlank() && it != "stream" }

        val resolvedTitle = when {
            pathOrUri.startsWith("tdlib://") -> parsedUri?.getQueryParameter("name") ?: title
            else -> title
        }

        val video = dev.roxy.kryptonplayer.core.model.Video(
            id = 0L,
            path = pathOrUri,
            parentPath = "Player",
            duration = 0L,
            uriString = pathOrUri,
            nameWithExtension = resolvedTitle,
            width = 0,
            height = 0,
            size = 0L,
            formattedDuration = "",
            formattedFileSize = "",
            artworkUri = null,
            externalId = extractedId ?: pathOrUri,
            isImage = false
        )
        telegramLikedRepository.toggleLike(video, token)
    }

    // ── Instant Synced Karaoke Lyrics State ─────────────────
    val playerPreferences = preferencesRepository.playerPreferences

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val lyricsState = _lyricsState.asStateFlow()

    private val _hasLyrics = MutableStateFlow(false)
    val hasLyrics = _hasLyrics.asStateFlow()

    private var lyricsSearchJob: kotlinx.coroutines.Job? = null
    private var currentLyricsMediaUri: String? = null
    private var lastSearchedLyricsKey: String? = null

    fun checkAndLoadLyrics(
        context: Context? = null,
        videoUri: Uri? = null,
        mediaTitle: String,
        artist: String? = null,
        mediaUri: String? = null
    ) {
        val clean = dev.roxy.kryptonplayer.core.data.lyrics.LyricsRepository.cleanVideoSongTitle(mediaTitle)
        if (clean.isBlank()) return

        val uriKey = mediaUri ?: videoUri?.toString()
        // If switching to a different media item/URI, reset state cleanly
        if (uriKey != null && uriKey != currentLyricsMediaUri) {
            lyricsSearchJob?.cancel()
            currentLyricsMediaUri = uriKey
            lastSearchedLyricsKey = null
            _lyricsState.value = LyricsUiState.Idle
            _hasLyrics.value = false
        }

        // Never overwrite already-loaded valid lyrics for current media
        if (_lyricsState.value is LyricsUiState.Available && _hasLyrics.value) {
            return
        }

        val searchKey = "${clean.lowercase()}_${(artist ?: "").lowercase()}_$uriKey"
        if (searchKey == lastSearchedLyricsKey && _lyricsState.value !is LyricsUiState.Idle) return
        lastSearchedLyricsKey = searchKey

        lyricsSearchJob?.cancel()
        lyricsSearchJob = viewModelScope.launch(Dispatchers.IO) {
            _lyricsState.value = LyricsUiState.Searching

            // 1. Direct song search if explicit ID3 artist metadata is present (authentic audio track)
            if (!artist.isNullOrBlank() && artist != "Unknown Artist") {
                val directResult = lyricsRepository.searchLyrics(clean, artist)
                if (directResult.isSuccess) {
                    val data = directResult.getOrThrow()
                    if (data.isSynced || !data.plainLyrics.isNullOrBlank()) {
                        _lyricsState.value = LyricsUiState.Available(data)
                        _hasLyrics.value = true
                        return@launch
                    }
                }
            }

            // 2. User Requirement: For video/unverified media, DO NOT fetch purely by title (prevents false lyrics on movies).
            // Process audio with Shazam up to first 3 minutes (in ~30-second windows) to detect genuine song.
            if (context != null && videoUri != null) {
                // Sample at 15s, 45s, 75s, 105s, 135s, 165s (max 3 minutes)
                val sampleOffsetsMs = listOf(15_000L, 45_000L, 75_000L, 105_000L, 135_000L, 165_000L)
                for (samplePosMs in sampleOffsetsMs) {
                    if (!isActive) return@launch
                    try {
                        val shazamResult = soundtrackRepository.recognizeVideoAudio(
                            context = context,
                            videoUri = videoUri,
                            positionMs = samplePosMs,
                            boostMode = true
                        )
                        if (shazamResult.isSuccess) {
                            val track = shazamResult.getOrNull()
                            if (track != null && track.title.isNotBlank()) {
                                val recognizedLyrics = lyricsRepository.searchLyrics(track.title, track.subtitle)
                                if (recognizedLyrics.isSuccess) {
                                    val data = recognizedLyrics.getOrThrow()
                                    if (data.isSynced || !data.plainLyrics.isNullOrBlank()) {
                                        _lyricsState.value = LyricsUiState.Available(data)
                                        _hasLyrics.value = true
                                        return@launch
                                    }
                                }
                            }
                        }
                    } catch (ignored: Exception) {
                        // Continue to next sample window
                    }
                }
            }

            // If not recognized by Shazam within first 3 minutes, do not show false lyrics
            if (_lyricsState.value !is LyricsUiState.Available) {
                _lyricsState.value = LyricsUiState.Error("No song recognized in this video")
                _hasLyrics.value = false
            }
        }
    }

    fun checkAndLoadLyrics(mediaTitle: String, artist: String? = null, mediaUri: String? = null) {
        checkAndLoadLyrics(
            context = null,
            videoUri = null,
            mediaTitle = mediaTitle,
            artist = artist,
            mediaUri = mediaUri
        )
    }

    fun searchLyricsManually(query: String) {
        if (query.isBlank()) return
        val parts = query.split(" - ", limit = 2)
        val track = if (parts.size > 1) parts[1].trim() else query.trim()
        val artist = if (parts.size > 1) parts[0].trim() else null
        lastSearchedLyricsKey = null
        lyricsSearchJob?.cancel()
        lyricsSearchJob = viewModelScope.launch {
            _lyricsState.value = LyricsUiState.Searching
            lyricsRepository.searchLyrics(track, artist)
                .onSuccess { data ->
                    _lyricsState.value = LyricsUiState.Available(data)
                    _hasLyrics.value = data.isSynced || !data.plainLyrics.isNullOrBlank()
                }
                .onFailure {
                    if (_lyricsState.value !is LyricsUiState.Available) {
                        _lyricsState.value = LyricsUiState.Error("No lyrics found for '$query'")
                        _hasLyrics.value = false
                    }
                }
        }
    }

    fun updateLyricsSyncOffset(deltaMs: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(lyricsSyncOffset = it.lyricsSyncOffset + deltaMs)
            }
        }
    }

    fun resetLyricsSyncOffset() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(lyricsSyncOffset = 0)
            }
        }
    }

    fun clearLyricsCache() {
        lyricsRepository.clearCache()
    }

    // ── Amazon X-Ray Live Actor & Character State ───────────
    private val _xRayState = MutableStateFlow<XRayUiState>(XRayUiState.Idle)
    val xRayState = _xRayState.asStateFlow()

    fun loadXRayData(mediaTitle: String) {
        viewModelScope.launch {
            _xRayState.value = XRayUiState.Loading
            try {
                val cleanSong = dev.roxy.kryptonplayer.core.data.lyrics.LyricsRepository.cleanVideoSongTitle(mediaTitle)
                var (cleanMovie, year) = dev.roxy.kryptonplayer.core.common.Utils.cleanMediaTitle(mediaTitle)

                // Check if title has strong music characteristics
                val hasMusicClues = mediaTitle.contains(" - ") ||
                    mediaTitle.contains(" | ") ||
                    mediaTitle.contains(Regex("(?i)\\b(feat|ft|official video|music video|lyric video|audio|song|mv|remix|lofi|slowed|ost|soundtrack)\\b"))

                // 1. If music clues exist, try ArchiveTune / Deezer / iTunes Artist & Song X-Ray FIRST!
                if (hasMusicClues || cleanSong.isNotBlank()) {
                    val musicCandidates = listOf(
                        cleanSong,
                        cleanMovie,
                        mediaTitle.replace(Regex("(?i)\\.(mp4|mkv|webm|avi|mov|flv|ts|m4v|mp3|aac|flac|wav)$"), "").replace("_", " ").trim()
                    ).distinct().filter { it.isNotBlank() }

                    for (mQuery in musicCandidates) {
                        val songXRay = fetchMusicVideoArtistXRay(mQuery, year)
                        if (songXRay != null) {
                            _xRayState.value = songXRay
                            return@launch
                        }
                    }
                }

                // 2. Try TMDb Multi Search for Movies & TV Shows
                try {
                    val searchCandidates = listOf(
                        cleanMovie,
                        cleanMovie.replace(Regex("[:\\-_]"), " ").replace(Regex("\\s+"), " ").trim()
                    ).distinct().filter { it.isNotBlank() }

                    for (candidate in searchCandidates) {
                        val searchRes = tmdbRepository.searchMulti(candidate)
                        val results = searchRes.getOrNull() ?: emptyList()
                        val topMatch = if (year != null) {
                            results.firstOrNull { r ->
                                when (r) {
                                    is dev.roxy.kryptonplayer.core.data.network.models.TmdbSearchResult.Movie -> r.release_date?.startsWith(year) == true
                                    is dev.roxy.kryptonplayer.core.data.network.models.TmdbSearchResult.TvShow -> r.first_air_date?.startsWith(year) == true
                                    else -> false
                                }
                            } ?: results.firstOrNull()
                        } else {
                            results.firstOrNull()
                        }

                        if (topMatch != null) {
                            when (topMatch) {
                                is dev.roxy.kryptonplayer.core.data.network.models.TmdbSearchResult.Movie -> {
                                    val details = tmdbRepository.getMovieDetails(topMatch.id).getOrNull()
                                    if (details != null && !details.credits?.cast.isNullOrEmpty()) {
                                        val director = details.credits?.crew?.firstOrNull { it.job?.equals("Director", ignoreCase = true) == true }?.name
                                        _xRayState.value = XRayUiState.Success(
                                            title = details.title,
                                            overview = details.overview,
                                            director = director,
                                            rating = details.vote_average,
                                            releaseYear = details.release_date?.take(4) ?: year,
                                            posterPath = details.poster_path,
                                            backdropPath = details.backdrop_path,
                                            genres = details.genres?.mapNotNull { it.name } ?: emptyList(),
                                            cast = details.credits?.cast ?: emptyList(),
                                            crew = details.credits?.crew ?: emptyList()
                                        )
                                        return@launch
                                    }
                                }
                                is dev.roxy.kryptonplayer.core.data.network.models.TmdbSearchResult.TvShow -> {
                                    val details = tmdbRepository.getTvShowDetails(topMatch.id).getOrNull()
                                    if (details != null && !details.credits?.cast.isNullOrEmpty()) {
                                        val director = details.credits?.crew?.firstOrNull {
                                            it.job?.equals("Director", ignoreCase = true) == true ||
                                            it.job?.equals("Executive Producer", ignoreCase = true) == true ||
                                            it.job?.equals("Creator", ignoreCase = true) == true
                                        }?.name
                                        _xRayState.value = XRayUiState.Success(
                                            title = details.name,
                                            overview = details.overview,
                                            director = director,
                                            rating = details.vote_average,
                                            releaseYear = details.first_air_date?.take(4) ?: year,
                                            posterPath = details.poster_path,
                                            backdropPath = details.backdrop_path,
                                            genres = details.genres?.mapNotNull { it.name } ?: emptyList(),
                                            cast = details.credits?.cast ?: emptyList(),
                                            crew = details.credits?.crew ?: emptyList()
                                        )
                                        return@launch
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PlayerViewModel", "TMDb X-Ray search failed", e)
                }

                // 3. Fallback: IMDb Suggestion API
                val cleanForSuggest = cleanMovie.lowercase().replace(Regex("[^a-zA-Z0-9]"), "_").take(25)
                val suggestResp = imdbSuggestApi.suggest(cleanForSuggest)
                val topMatch = suggestResp.d?.firstOrNull { it.typeId != "name" } ?: suggestResp.d?.firstOrNull()
                if (topMatch != null) {
                    val rawStars = topMatch.stars?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                    val starsList = rawStars.mapIndexed { index, starName ->
                        dev.roxy.kryptonplayer.core.data.network.models.TmdbCastMember(
                            id = index + 1,
                            name = starName,
                            character = "Main Cast",
                            profile_path = null
                        )
                    }

                    _xRayState.value = XRayUiState.Success(
                        title = topMatch.title ?: cleanMovie,
                        overview = "Starring ${topMatch.stars ?: "Cast"}. Recognized as ${topMatch.typeDesc ?: "Movie"}.",
                        director = topMatch.typeDesc ?: "Feature Presentation",
                        rating = 8.5,
                        releaseYear = topMatch.year?.toString() ?: year,
                        posterPath = topMatch.image?.imageUrl,
                        backdropPath = null,
                        genres = listOfNotNull(topMatch.typeDesc, "Cinema"),
                        cast = starsList,
                        crew = emptyList()
                    )
                    return@launch
                }

                _xRayState.value = XRayUiState.Error("No X-Ray information found for '$cleanMovie'")
            } catch (e: Exception) {
                _xRayState.value = XRayUiState.Error(e.message ?: "Failed to load X-Ray data")
            }
        }
    }

    private suspend fun fetchMusicVideoArtistXRay(query: String, fallbackYear: String?): XRayUiState.Success? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // A. Try Deezer Music & Artist Engine (ArchiveTune engine)
        try {
            val q = java.net.URLEncoder.encode(query, "UTF-8")
            val deezerUrl = "https://api.deezer.com/search?q=$q&limit=3"
            val conn = (java.net.URL(deezerUrl).openConnection() as java.net.HttpURLConnection).apply {
                setRequestProperty("User-Agent", "KryptonPlayer/1.0")
                connectTimeout = 4000
                readTimeout = 4000
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(body)
                val data = json.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val top = data.getJSONObject(0)
                    val trackTitle = top.optString("title", query)
                    val artistObj = top.optJSONObject("artist")
                    val leadArtistName = artistObj?.optString("name", "Artist") ?: "Artist"
                    val leadArtistPic = artistObj?.optString("picture_medium")?.takeIf { it.isNotBlank() } ?: artistObj?.optString("picture_big")
                    val albumObj = top.optJSONObject("album")
                    val albumTitle = albumObj?.optString("title")
                    val albumCover = albumObj?.optString("cover_big")?.takeIf { it.isNotBlank() } ?: albumObj?.optString("cover_medium")

                    val castList = mutableListOf<dev.roxy.kryptonplayer.core.data.network.models.TmdbCastMember>()
                    castList.add(
                        dev.roxy.kryptonplayer.core.data.network.models.TmdbCastMember(
                            id = 1,
                            name = leadArtistName,
                            character = "Lead Vocalist / Singer",
                            profile_path = leadArtistPic
                        )
                    )

                    // Check for featured artists
                    val artistNames = query.split(Regex("[,&]|(?i)\\b(feat\\.?|ft\\.?|featuring|with|x)\\b"))
                        .map { it.trim() }
                        .filter { it.isNotBlank() && !it.equals(leadArtistName, ignoreCase = true) }

                    for ((idx, fName) in artistNames.take(4).withIndex()) {
                        var fPic: String? = null
                        try {
                            val fSug = imdbSuggestApi.suggest(fName.lowercase().replace(Regex("[^a-zA-Z0-9]"), "_"))
                            fPic = fSug.d?.firstOrNull { it.image?.imageUrl != null }?.image?.imageUrl
                        } catch (_: Exception) {}

                        castList.add(
                            dev.roxy.kryptonplayer.core.data.network.models.TmdbCastMember(
                                id = idx + 2,
                                name = fName,
                                character = "Featured Artist / Composer",
                                profile_path = fPic
                            )
                        )
                    }

                    return@withContext XRayUiState.Success(
                        title = trackTitle,
                        overview = "Album: ${albumTitle ?: "Single"} • Lead Singer: $leadArtistName",
                        director = "Music Director",
                        rating = 9.2,
                        releaseYear = fallbackYear,
                        posterPath = albumCover,
                        backdropPath = albumCover,
                        genres = listOf("Music Video", "Song"),
                        cast = castList,
                        crew = emptyList()
                    )
                }
            }
        } catch (_: Exception) {}

        // B. Try iTunes Music Engine
        try {
            val itunesResults = soundtrackRepository.searchItunes(query)
            if (itunesResults.isNotEmpty()) {
                val topSong = itunesResults.first()
                val artistNames = topSong.artist.split(Regex("[,&]|(?i)\\b(feat\\.?|ft\\.?|featuring|with)\\b"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val castList = artistNames.mapIndexed { index, name ->
                    var artistPic: String? = null
                    try {
                        val tmdbRes = tmdbRepository.searchMulti(name).getOrNull()
                        val pSearch = tmdbRes?.filterIsInstance<dev.roxy.kryptonplayer.core.data.network.models.TmdbSearchResult.Person>()?.firstOrNull()
                        if (pSearch?.profile_path != null) {
                            artistPic = "https://image.tmdb.org/t/p/w185${pSearch.profile_path}"
                        }
                    } catch (_: Exception) {}

                    if (artistPic == null) {
                        try {
                            val imdbSug = imdbSuggestApi.suggest(name.lowercase().replace(Regex("[^a-zA-Z0-9]"), "_"))
                            val m = imdbSug.d?.firstOrNull { it.typeId == "name" || it.typeDesc == "Actor" || it.typeDesc == "Actress" }
                                ?: imdbSug.d?.firstOrNull { it.image?.imageUrl != null }
                            artistPic = m?.image?.imageUrl
                        } catch (_: Exception) {}
                    }

                    dev.roxy.kryptonplayer.core.data.network.models.TmdbCastMember(
                        id = index + 100,
                        name = name,
                        character = if (index == 0) "Lead Artist / Singer" else "Featured Artist / Composer",
                        profile_path = artistPic
                    )
                }

                return@withContext XRayUiState.Success(
                    title = topSong.title,
                    overview = "Album: ${topSong.album} • Artist: ${topSong.artist}",
                    director = "Music Track",
                    rating = 9.2,
                    releaseYear = fallbackYear,
                    posterPath = topSong.coverUrl,
                    backdropPath = null,
                    genres = listOf("Music Video", "Song"),
                    cast = castList,
                    crew = emptyList()
                )
            }
        } catch (_: Exception) {}

        null
    }

    // ── Real-Time Shazam Audio Recognition State ────────────
    private val _shazamState = MutableStateFlow<ShazamRecognitionUiState>(ShazamRecognitionUiState.Idle)
    val shazamState: StateFlow<ShazamRecognitionUiState> = _shazamState.asStateFlow()


    fun startVideoAudioShazamRecognition(
        context: android.content.Context,
        videoUri: android.net.Uri?,
        positionMs: Long,
        boostMode: Boolean = true
    ) {
        viewModelScope.launch {
            if (videoUri == null) {
                // Fallback to microphone
                startLiveShazamRecognition(context)
                return@launch
            }

            _shazamState.value = ShazamRecognitionUiState.Analyzing
            val result = soundtrackRepository.recognizeVideoAudio(context, videoUri, positionMs, boostMode = boostMode)
            result.onSuccess { track ->
                _shazamState.value = ShazamRecognitionUiState.Success(track)
            }.onFailure { err ->
                // If video extraction failed (e.g. streaming without range support), prompt mic option
                _shazamState.value = ShazamRecognitionUiState.Error(err.message ?: "Could not detect song in this video scene. Try Microphone.")
            }
        }
    }

    fun startLiveShazamRecognition(context: android.content.Context, boostMode: Boolean = true) {
        viewModelScope.launch {
            _shazamState.value = ShazamRecognitionUiState.Listening(0f)
            val result = soundtrackRepository.recognizeLiveAudio(
                context = context,
                boostMode = boostMode,
                onAmplitude = { amp ->
                    if (_shazamState.value is ShazamRecognitionUiState.Listening) {
                        _shazamState.value = ShazamRecognitionUiState.Listening(amp)
                    }
                }
            )
            _shazamState.value = ShazamRecognitionUiState.Analyzing
            result.onSuccess { track ->
                _shazamState.value = ShazamRecognitionUiState.Success(track)
            }.onFailure { err ->
                _shazamState.value = ShazamRecognitionUiState.Error(err.message ?: "No song match found. Please try again.")
            }
        }
    }

    fun resetShazamState() {
        _shazamState.value = ShazamRecognitionUiState.Idle
    }

    // ── In-Player Soundtrack & Song Finder State ────────────
    private val _soundtrackState = MutableStateFlow<SoundtrackUiState>(SoundtrackUiState.Idle)
    val soundtrackState = _soundtrackState.asStateFlow()

    fun loadSoundtracks(mediaTitle: String) {
        viewModelScope.launch {
            _soundtrackState.value = SoundtrackUiState.Loading
            try {
                val (clean, _) = dev.roxy.kryptonplayer.core.common.Utils.cleanMediaTitle(mediaTitle)
                val list = soundtrackRepository.getSoundtracksForMedia(clean)

                if (list.isEmpty()) {
                    _soundtrackState.value = SoundtrackUiState.Error("No soundtracks found for '$clean'")
                } else {
                    _soundtrackState.value = SoundtrackUiState.Success(list)
                    usbMediaServer.soundtrackList = list.map {
                        dev.roxy.kryptonplayer.core.data.server.SoundtrackSongItem(
                            title = it.title,
                            artist = it.artist,
                            previewUrl = it.previewUrl,
                            coverUrl = it.coverUrl
                        )
                    }
                }
            } catch (e: Exception) {
                _soundtrackState.value = SoundtrackUiState.Error(e.message ?: "Failed to load soundtracks")
            }
        }
    }


    // ── Smart Scene Bookmarks & Time Notes ──────────────────
    private val _currentMediaUri = MutableStateFlow<String?>(null)

    val bookmarks: StateFlow<List<dev.roxy.kryptonplayer.core.model.BookmarkItem>> = _currentMediaUri
        .filterNotNull()
        .flatMapLatest { uri -> bookmarkRepository.getBookmarks(uri) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setMediaUriForBookmarks(uri: String) {
        _currentMediaUri.value = uri
    }

    fun addBookmark(
        mediaUri: String,
        title: String,
        positionMs: Long,
        formattedTime: String,
        thumbnailPath: String? = null
    ) {
        viewModelScope.launch {
            bookmarkRepository.addBookmark(
                mediaUri = mediaUri,
                title = title,
                positionMs = positionMs,
                formattedTime = formattedTime,
                thumbnailPath = thumbnailPath
            )
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            bookmarkRepository.deleteBookmark(id)
        }
    }

    private val _onlineSubtitleState = MutableStateFlow<OnlineSubtitleSearchState>(OnlineSubtitleSearchState.Idle)
    val onlineSubtitleState = _onlineSubtitleState.asStateFlow()

    private val _downloadingSubtitleId = MutableStateFlow<String?>(null)
    val downloadingSubtitleId = _downloadingSubtitleId.asStateFlow()

    private val _translatingState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translatingState = _translatingState.asStateFlow()

    // ── USB PC Cinema Streaming State ─────────────────────────
    val isUsbServerRunning = usbMediaServer.isServerRunning

    private val _usbStreamingUrls = MutableStateFlow<List<dev.roxy.kryptonplayer.core.data.server.UsbConnectionHelper.UsbIpInfo>>(emptyList())
    val usbStreamingUrls = _usbStreamingUrls.asStateFlow()

    fun getUsbPairingPin(): String = usbMediaServer.pairingPin

    fun startUsbStreaming(
        videoUri: Uri,
        title: String,
        subtitleFile: java.io.File? = null,
        initialPositionMs: Long = 0L
    ) {
        _usbStreamingUrls.value = usbConnectionHelper.getAvailableStreamingUrls()
        usbMediaServer.startServer(
            videoUri = videoUri,
            title = title,
            subtitleFile = subtitleFile,
            initialPositionMs = initialPositionMs
        )
    }

    fun stopUsbStreaming() {
        usbMediaServer.stopServer()
    }

    fun refreshUsbUrls() {
        _usbStreamingUrls.value = usbConnectionHelper.getAvailableStreamingUrls()
    }

    fun updateUsbMediaPosition(positionMs: Long, isPlaying: Boolean) {
        usbMediaServer.activePositionMs = positionMs
        usbMediaServer.isPlaying = isPlaying
    }

    fun searchOnlineSubtitles(query: String, languages: String? = null) {
        viewModelScope.launch {
            _onlineSubtitleState.value = OnlineSubtitleSearchState.Loading
            try {
                val results = onlineSubtitleRepository.searchSubtitles(query, languages)
                if (results.isEmpty()) {
                    _onlineSubtitleState.value = OnlineSubtitleSearchState.Error("No subtitles found. Try a different search query.")
                } else {
                    _onlineSubtitleState.value = OnlineSubtitleSearchState.Success(results)
                }
            } catch (e: Exception) {
                _onlineSubtitleState.value = OnlineSubtitleSearchState.Error(e.message ?: "Failed to search subtitles")
            }
        }
    }

    fun downloadAndApplySubtitle(
        item: dev.roxy.kryptonplayer.core.data.network.subtitles.OnlineSubtitleItem,
        onError: ((String) -> Unit)? = null,
        onFileReady: (java.io.File) -> Unit
    ) {
        viewModelScope.launch {
            _downloadingSubtitleId.value = item.id
            try {
                val file = onlineSubtitleRepository.downloadSubtitle(item)
                if (file != null && file.exists() && file.length() > 0) {
                    onFileReady(file)
                } else {
                    onError?.invoke("Could not download subtitle. Please try another provider or link.")
                }
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Failed to download subtitle")
            } finally {
                _downloadingSubtitleId.value = null
            }
        }
    }

    fun translateSubtitle(
        sourceFile: java.io.File,
        targetLang: String? = null,
        onError: ((String) -> Unit)? = null,
        onTranslated: (java.io.File) -> Unit
    ) {
        val effectiveTargetLang = targetLang ?: preferencesRepository.playerPreferences.value.subtitleTranslationTargetLang.takeIf { it.isNotBlank() } ?: "en"
        viewModelScope.launch {
            _translatingState.value = TranslationState.Translating(0f)
            try {
                val translatedFile = subtitleTranslator.translateSubtitleFile(
                    sourceFile = sourceFile,
                    targetLanguageCode = effectiveTargetLang,
                    onProgress = { p ->
                        _translatingState.value = TranslationState.Translating(p)
                    }
                )
                if (translatedFile != null && translatedFile.exists() && translatedFile.length() > 0) {
                    _translatingState.value = TranslationState.Success(translatedFile)
                    onTranslated(translatedFile)
                } else {
                    _translatingState.value = TranslationState.Error("Translation failed")
                    onError?.invoke("Translation failed. Please try again.")
                }
            } catch (e: Exception) {
                _translatingState.value = TranslationState.Error(e.message ?: "Failed to translate subtitle")
                onError?.invoke(e.message ?: "Failed to translate subtitle")
            }
        }
    }

    var playWhenReady: Boolean = true

    private val internalUiState = MutableStateFlow(
        PlayerUiState(
            playerPreferences = preferencesRepository.playerPreferences.value,
            applicationPreferences = preferencesRepository.applicationPreferences.value,
        ),
    )
    val uiState = internalUiState.asStateFlow()

    private val _watchPartyCommands = MutableSharedFlow<WatchPartyPlayerCommand>()
    val watchPartyCommands = _watchPartyCommands.asSharedFlow()

    // Sync state tracking
    private var heartbeatJob: Job? = null

    /**
     * Counter-based sync guard: incremented when a remote command starts,
     * decremented by the UI layer after the player has settled.
     * Any value > 0 means "we are applying a remote event, don't echo back."
     */
    private val _syncGuardCounter = AtomicInteger(0)
    val isSyncGuardActive: Boolean get() = _syncGuardCounter.get() > 0

    fun incrementSyncGuard() { _syncGuardCounter.incrementAndGet() }
    fun decrementSyncGuard() {
        val v = _syncGuardCounter.decrementAndGet()
        if (v < 0) _syncGuardCounter.set(0) // safety clamp
    }

    // Drift correction: track the "authority" position from the last sync event
    private var lastRemotePositionMs: Long = 0L
    private var lastRemoteServerTime: Long = 0L
    private var lastRemoteState: String = "pause" // "play" or "pause"

    // Cooldowns to prevent feedback loops
    @Volatile var lastRemoteEventTime: Long = 0L // System.currentTimeMillis() of last remote event
        private set
    private var lastHardSyncTime: Long = 0L
    private var lastAdjustedSpeed: Float = NORMAL_SPEED

    companion object {
        private const val DRIFT_THRESHOLD_MS = 3500L // 3.5 seconds — hard seek
        private const val HEARTBEAT_INTERVAL_MS = 2000L // send position every 2s
        private const val CATCH_UP_SPEED = 1.1f
        private const val SLOW_DOWN_SPEED = 0.9f
        private const val NORMAL_SPEED = 1.0f
        private const val SOFT_DRIFT_THRESHOLD_MS = 1500L // 1.5 seconds — speed adjustment
        private const val REMOTE_EVENT_COOLDOWN_MS = 5000L // don't drift-correct within 5s of remote event
        private const val MIN_HARD_SYNC_INTERVAL_MS = 5000L // max 1 hard sync per 5s
    }

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { prefs ->
                internalUiState.update { it.copy(playerPreferences = prefs) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect { appPrefs ->
                internalUiState.update { it.copy(applicationPreferences = appPrefs) }
            }
        }

        val roomId = savedStateHandle.get<String>("watch_party_room_id")
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (roomId != null && token.isNotEmpty()) {
            internalUiState.update { it.copy(watchPartyRoomId = roomId) }
            syncWebSocketClient.connect(roomId, token)

            viewModelScope.launch {
                syncWebSocketClient.syncEvents.collect { event ->
                    when (event) {
                        is dev.roxy.kryptonplayer.core.data.network.models.SyncEvent.RoomInfo -> {
                            internalUiState.update {
                                it.copy(
                                    isConnectedToWatchParty = true,
                                    watchPartyHostUsername = event.hostUsername,
                                    watchPartyMembers = event.members,
                                    watchPartyMemberCount = event.memberCount
                                )
                            }
                            // Start heartbeat once connected
                            startHeartbeat()
                        }
                        is dev.roxy.kryptonplayer.core.data.network.models.SyncEvent.Play -> {
                            lastRemotePositionMs = event.positionMs
                            lastRemoteServerTime = event.serverTime
                            lastRemoteState = "play"
                            lastRemoteEventTime = System.currentTimeMillis()
                            _watchPartyCommands.emit(WatchPartyPlayerCommand.SeekAndPlay(event.positionMs))
                            addSystemMessage("${event.fromUser} played")
                        }
                        is dev.roxy.kryptonplayer.core.data.network.models.SyncEvent.Pause -> {
                            lastRemotePositionMs = event.positionMs
                            lastRemoteServerTime = event.serverTime
                            lastRemoteState = "pause"
                            lastRemoteEventTime = System.currentTimeMillis()
                            _watchPartyCommands.emit(WatchPartyPlayerCommand.SeekAndPause(event.positionMs))
                            addSystemMessage("${event.fromUser} paused")
                        }
                        is dev.roxy.kryptonplayer.core.data.network.models.SyncEvent.Seek -> {
                            lastRemotePositionMs = event.positionMs
                            lastRemoteServerTime = event.serverTime
                            lastRemoteEventTime = System.currentTimeMillis()
                            _watchPartyCommands.emit(WatchPartyPlayerCommand.Seek(event.positionMs))
                            addSystemMessage("${event.fromUser} seeked to ${formatTime(event.positionMs)}")
                        }
                        is dev.roxy.kryptonplayer.core.data.network.models.SyncEvent.UserJoined -> {
                            internalUiState.update {
                                it.copy(
                                    watchPartyMemberCount = event.memberCount
                                )
                            }
                            addSystemMessage("${event.username} joined the party")
                        }
                        is dev.roxy.kryptonplayer.core.data.network.models.SyncEvent.UserLeft -> {
                            internalUiState.update {
                                it.copy(
                                    watchPartyMemberCount = event.memberCount
                                )
                            }
                            addSystemMessage("${event.username} left the party")
                        }
                        is dev.roxy.kryptonplayer.core.data.network.models.SyncEvent.Chat -> {
                            val msg = WatchPartyMessage(
                                sender = event.fromUser,
                                message = event.message,
                                isSystem = false,
                                timestamp = event.timestamp
                            )
                            internalUiState.update {
                                it.copy(watchPartyMessages = it.watchPartyMessages + msg)
                            }
                        }
                        is dev.roxy.kryptonplayer.core.data.network.models.SyncEvent.RoomClosed -> {
                            addSystemMessage("Watch Party room was closed")
                            internalUiState.update { it.copy(isConnectedToWatchParty = false) }
                            stopHeartbeat()
                        }
                        is dev.roxy.kryptonplayer.core.data.network.models.SyncEvent.Disconnected -> {
                            addSystemMessage("Disconnected from Watch Party")
                            internalUiState.update { it.copy(isConnectedToWatchParty = false) }
                            stopHeartbeat()
                        }
                        is dev.roxy.kryptonplayer.core.data.network.models.SyncEvent.Error -> {
                            addSystemMessage("Error: ${event.message}")
                        }
                    }
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL_MS)
                // The UI layer updates currentPlayerPositionMs periodically
                val pos = internalUiState.value.currentPlayerPositionMs
                if (pos >= 0) {
                    syncWebSocketClient.sendSyncPosition(pos)
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /**
     * Called by the UI layer every ~2 seconds with the player's current position.
     * This method calculates drift against the last known remote position
     * and emits speed adjustment or hard-seek commands to keep devices in sync.
     *
     * COOLDOWNS:
     * - Won't drift-correct within 5s of the last remote Play/Pause/Seek event
     * - Won't hard-sync more than once per 5s
     * - Won't spam AdjustSpeed(NORMAL) if already at normal speed
     */
    fun updatePlayerPositionForSync(positionMs: Long) {
        internalUiState.update { it.copy(currentPlayerPositionMs = positionMs) }
    }

    fun reportLocalPosition(localPositionMs: Long, isPlaying: Boolean) {
        internalUiState.update { it.copy(currentPlayerPositionMs = localPositionMs) }

        if (!uiState.value.isConnectedToWatchParty) return
        if (lastRemoteServerTime == 0L) return // No remote reference yet
        if (lastRemoteState != "play" || !isPlaying) return // Only drift-correct during playback

        val now = System.currentTimeMillis()

        // COOLDOWN: Don't drift-correct within 5s of receiving a remote event.
        // The player is still settling (buffering/seeking) after remote commands.
        if (now - lastRemoteEventTime < REMOTE_EVENT_COOLDOWN_MS) return

        // Calculate what the expected position should be now
        val elapsedSinceRemoteEvent = now - lastRemoteServerTime
        val expectedPosition = lastRemotePositionMs + elapsedSinceRemoteEvent
        val drift = localPositionMs - expectedPosition // positive = ahead, negative = behind

        val absDrift = kotlin.math.abs(drift)

        if (absDrift > DRIFT_THRESHOLD_MS) {
            // RATE LIMIT: max 1 hard sync per 5s
            if (now - lastHardSyncTime < MIN_HARD_SYNC_INTERVAL_MS) return
            lastHardSyncTime = now
            // Hard correction: seek to expected position
            viewModelScope.launch {
                _watchPartyCommands.emit(WatchPartyPlayerCommand.HardSync(expectedPosition))
            }
        } else if (absDrift > SOFT_DRIFT_THRESHOLD_MS) {
            // Soft correction: speed up or slow down slightly
            val speed = if (drift < 0) CATCH_UP_SPEED else SLOW_DOWN_SPEED
            if (lastAdjustedSpeed != speed) {
                lastAdjustedSpeed = speed
                viewModelScope.launch {
                    _watchPartyCommands.emit(WatchPartyPlayerCommand.AdjustSpeed(speed))
                }
            }
        } else {
            // Within acceptable range, restore normal speed only if we were adjusting
            if (lastAdjustedSpeed != NORMAL_SPEED) {
                lastAdjustedSpeed = NORMAL_SPEED
                viewModelScope.launch {
                    _watchPartyCommands.emit(WatchPartyPlayerCommand.AdjustSpeed(NORMAL_SPEED))
                }
            }
        }
    }

    fun sendPlayToWatchParty(positionMs: Long) {
        if (uiState.value.isConnectedToWatchParty) {
            syncWebSocketClient.sendPlay(positionMs)
        }
    }

    fun sendPauseToWatchParty(positionMs: Long) {
        if (uiState.value.isConnectedToWatchParty) {
            syncWebSocketClient.sendPause(positionMs)
        }
    }

    fun sendSeekToWatchParty(positionMs: Long) {
        if (uiState.value.isConnectedToWatchParty) {
            syncWebSocketClient.sendSeek(positionMs)
        }
    }

    fun sendChatMessage(message: String) {
        if (uiState.value.isConnectedToWatchParty && message.isNotBlank()) {
            syncWebSocketClient.sendChat(message)
        }
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60)) % 24
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun addSystemMessage(text: String) {
        val msg = WatchPartyMessage(
            sender = "System",
            message = text,
            isSystem = true
        )
        internalUiState.update {
            it.copy(watchPartyMessages = it.watchPartyMessages + msg)
        }
    }

    private val roomPattern = Regex("^ROXY-[0-9A-Fa-f]{4}$")

    fun joinWatchPartyFromDeepLink(
        roomId: String,
        onResult: (streamUrl: String?, videoName: String?, error: String?) -> Unit
    ) {
        val sanitizedRoomId = roomId.trim().uppercase()
        if (!roomPattern.matches(sanitizedRoomId)) {
            onResult(null, null, "Invalid Room Code format. Must be ROXY-XXXX.")
            return
        }
        
        val token = preferencesRepository.playerPreferences.value.telegramToken
        if (token.isEmpty()) {
            onResult(null, null, "Please log in to Telegram Cloud first to join a Watch Party.")
            return
        }

        viewModelScope.launch {
            internalUiState.update { it.copy(isJoiningFromDeepLink = true) }
            try {
                val response = watchPartyRepository.joinSyncRoom(sanitizedRoomId, token)
                if (response != null) {
                    val streamUrl = dev.roxy.kryptonplayer.core.data.network.NetworkConfig.streamUrl(response.fileId)
                    onResult(streamUrl, response.videoName ?: "Watch Party Video", null)
                } else {
                    onResult(null, null, "Watch Party room not found or expired.")
                }
            } catch (e: Exception) {
                onResult(null, null, e.message ?: "Failed to connect to sync server.")
            } finally {
                internalUiState.update { it.copy(isJoiningFromDeepLink = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopHeartbeat()
        syncWebSocketClient.disconnect()
    }


    suspend fun getPlaylistFromUri(uri: Uri): List<Video> {
        return getSortedPlaylistUseCase.invoke(uri)
    }

    fun updateVideoZoom(uri: String, zoom: Float) {
        viewModelScope.launch {
            mediaRepository.updateMediumZoom(uri, zoom)
        }
    }

    fun updatePlayerBrightness(value: Float) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(playerBrightness = value) }
        }
    }

    fun updateVideoContentScale(contentScale: VideoContentScale) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(playerVideoZoom = contentScale) }
        }
    }

    fun setLoopMode(loopMode: LoopMode) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(loopMode = loopMode) }
        }
    }

    fun updatePlayerPreferences(update: (PlayerPreferences) -> PlayerPreferences) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences(update)
        }
    }

    fun updateApplicationPreferences(update: (ApplicationPreferences) -> ApplicationPreferences) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences(update)
        }
    }

    fun setQualityUrls(urls: List<String>) {
        internalUiState.update { it.copy(qualityUrls = urls) }
    }

    fun onVideoZoomEvent(event: VideoZoomEvent) {
        when (event) {
            is VideoZoomEvent.ContentScaleChanged -> {
                updateVideoContentScale(event.contentScale)
            }
            is VideoZoomEvent.ZoomChanged -> {
                updateVideoZoom(event.mediaItem.mediaId, event.zoom)
            }
        }
    }

    fun onSubtitleOptionEvent(event: SubtitleOptionsEvent) {
        when (event) {
            is SubtitleOptionsEvent.DelayChanged -> {
                updateSubtitleDelay(event.mediaItem.mediaId, event.delay)
            }
            is SubtitleOptionsEvent.SpeedChanged -> {
                updateSubtitleSpeed(event.mediaItem.mediaId, event.speed)
            }
        }
    }

    private fun updateSubtitleDelay(uri: String, delay: Long) {
        viewModelScope.launch {
            mediaRepository.updateSubtitleDelay(uri, delay)
        }
    }

    private fun updateSubtitleSpeed(uri: String, speed: Float) {
        viewModelScope.launch {
            mediaRepository.updateSubtitleSpeed(uri, speed)
        }
    }
}

@Stable
data class PlayerUiState(
    val playerPreferences: PlayerPreferences? = null,
    val applicationPreferences: ApplicationPreferences? = null,
    val qualityUrls: List<String> = emptyList(),
    val watchPartyRoomId: String? = null,
    val isConnectedToWatchParty: Boolean = false,
    val watchPartyHostUsername: String? = null,
    val watchPartyMembers: List<SyncMember> = emptyList(),
    val watchPartyMemberCount: Int = 0,
    val watchPartyMessages: List<WatchPartyMessage> = emptyList(),
    val currentPlayerPositionMs: Long = -1L,
    val isJoiningFromDeepLink: Boolean = false
)

data class WatchPartyMessage(
    val sender: String,
    val message: String,
    val isSystem: Boolean = false,
    val timestamp: String = ""
)

sealed interface WatchPartyPlayerCommand {
    data class SeekAndPlay(val positionMs: Long) : WatchPartyPlayerCommand
    data class SeekAndPause(val positionMs: Long) : WatchPartyPlayerCommand
    data class Seek(val positionMs: Long) : WatchPartyPlayerCommand
    data class HardSync(val positionMs: Long) : WatchPartyPlayerCommand
    data class AdjustSpeed(val speed: Float) : WatchPartyPlayerCommand
}

sealed interface PlayerEvent

sealed interface OnlineSubtitleSearchState {
    data object Idle : OnlineSubtitleSearchState
    data object Loading : OnlineSubtitleSearchState
    data class Success(val subtitles: List<dev.roxy.kryptonplayer.core.data.network.subtitles.OnlineSubtitleItem>) : OnlineSubtitleSearchState
    data class Error(val message: String) : OnlineSubtitleSearchState
}

sealed interface TranslationState {
    data object Idle : TranslationState
    data class Translating(val progress: Float) : TranslationState
    data class Success(val file: java.io.File) : TranslationState
    data class Error(val message: String) : TranslationState
}

sealed interface XRayUiState {
    data object Idle : XRayUiState
    data object Loading : XRayUiState
    data class Success(
        val title: String,
        val overview: String?,
        val director: String?,
        val rating: Double,
        val releaseYear: String?,
        val posterPath: String? = null,
        val backdropPath: String? = null,
        val genres: List<String> = emptyList(),
        val cast: List<dev.roxy.kryptonplayer.core.data.network.models.TmdbCastMember>,
        val crew: List<dev.roxy.kryptonplayer.core.data.network.models.TmdbCrewMember> = emptyList()
    ) : XRayUiState
    data class Error(val message: String) : XRayUiState
}


sealed interface ShazamRecognitionUiState {
    data object Idle : ShazamRecognitionUiState
    data class Listening(val amplitude: Float = 0f) : ShazamRecognitionUiState
    data object Analyzing : ShazamRecognitionUiState
    data class Success(val track: dev.roxy.kryptonplayer.core.data.shazam.ShazamRecognizedTrack) : ShazamRecognitionUiState
    data class Error(val message: String) : ShazamRecognitionUiState
}

sealed interface SoundtrackUiState {
    data object Idle : SoundtrackUiState
    data object Loading : SoundtrackUiState
    data class Success(val tracks: List<dev.roxy.kryptonplayer.core.data.repository.SoundtrackItem>) : SoundtrackUiState
    data class Error(val message: String) : SoundtrackUiState
}

