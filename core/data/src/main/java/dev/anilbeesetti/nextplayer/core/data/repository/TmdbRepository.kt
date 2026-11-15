package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.network.models.*
import dev.anilbeesetti.nextplayer.core.model.Video

interface TmdbRepository {
    suspend fun getPopularMovies(page: Int = 1): Result<List<TmdbMovie>>
    suspend fun getPopularTvShows(page: Int = 1): Result<List<TmdbTvShow>>
    suspend fun getTrendingMovies(page: Int = 1): Result<List<TmdbMovie>>
    suspend fun getTrendingTvShows(page: Int = 1): Result<List<TmdbTvShow>>
    suspend fun getPopularPeople(page: Int = 1): Result<List<TmdbPerson>>
    suspend fun discoverMovies(genreId: String, page: Int = 1): Result<List<TmdbMovie>>
    suspend fun searchMulti(query: String, page: Int = 1): Result<List<TmdbSearchResult>>
    suspend fun getMovieDetails(id: Int): Result<TmdbMovieDetails>
    suspend fun getTvShowDetails(id: Int): Result<TmdbTvShowDetails>
    suspend fun getPersonDetails(id: Int): Result<TmdbPersonDetails>
    suspend fun getTvSeasonDetails(tvId: Int, seasonNumber: Int): Result<TmdbTvSeasonDetails>
    suspend fun getCollectionDetails(collectionId: Int): Result<TmdbCollectionDetails>
    suspend fun getUpcomingMovies(page: Int = 1): Result<List<TmdbMovie>>
    suspend fun getTopRatedMovies(page: Int = 1): Result<List<TmdbMovie>>
    suspend fun getTopRatedTvShows(page: Int = 1): Result<List<TmdbTvShow>>
    suspend fun findMatchingLocalVideo(title: String, alternativeTitles: List<String> = emptyList()): Video?
    suspend fun findMatchingLocalEpisode(showTitle: String, season: Int, episode: Int, alternativeTitles: List<String> = emptyList()): Video?
    
    // IMDb-specific additional features
    suspend fun getParentsGuide(id: Int): Result<ImdbParentsGuideResponse>
    suspend fun getBoxOffice(id: Int): Result<ImdbBoxOffice>
    suspend fun getCertificates(id: Int): Result<List<ImdbCertificate>>
    suspend fun getTrivia(personId: Int): Result<List<ImdbTriviaItem>>
    suspend fun getRelationships(personId: Int): Result<List<ImdbRelationshipItem>>

    // Scene Stills & Frame-by-Frame features
    suspend fun getMediaStills(
        id: Int,
        isTvShow: Boolean,
        title: String,
        releaseYear: String?,
        imdbId: String? = null
    ): Result<List<MediaStill>>

    suspend fun getFancapsFrames(
        title: String,
        isTvShow: Boolean,
        season: Int = 1,
        page: Int = 1
    ): Result<FancapsPageResult>

    suspend fun setVideoCustomThumbnail(
        videoUri: String,
        imageUrl: String,
        context: android.content.Context
    ): Result<Boolean>

    suspend fun saveImageToFramesFolder(
        imageUrl: String,
        title: String,
        context: android.content.Context
    ): Result<String>

    suspend fun getReviews(id: Int, isTvShow: Boolean): Result<List<TmdbReview>>
}
