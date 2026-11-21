package dev.roxy.kryptonplayer.core.data.repository

import dev.roxy.kryptonplayer.core.data.network.models.*

interface ImdbRepository {
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
    suspend fun getUpcomingMovies(page: Int = 1): Result<List<TmdbMovie>>
    suspend fun getTopRatedMovies(page: Int = 1): Result<List<TmdbMovie>>
    suspend fun getTopRatedTvShows(page: Int = 1): Result<List<TmdbTvShow>>
    
    // IMDb-specific additional features
    suspend fun getParentsGuide(id: Int): Result<ImdbParentsGuideResponse>
    suspend fun getBoxOffice(id: Int): Result<ImdbBoxOffice>
    suspend fun getCertificates(id: Int): Result<List<ImdbCertificate>>
    suspend fun getTrivia(personId: Int): Result<List<ImdbTriviaItem>>
    suspend fun getRelationships(personId: Int): Result<List<ImdbRelationshipItem>>
}
