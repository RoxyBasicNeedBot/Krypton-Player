package dev.roxy.kryptonplayer.core.data.network

import dev.roxy.kryptonplayer.core.data.network.models.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbMovie>

    @GET("tv/popular")
    suspend fun getPopularTvShows(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbTvShow>

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbMovie>

    @GET("trending/tv/week")
    suspend fun getTrendingTvShows(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbTvShow>

    @GET("person/popular")
    suspend fun getPopularPeople(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbPerson>

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("with_genres") genreId: String,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_count.gte") minVotes: Int = 100,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbMovie>

    @GET("search/person")
    suspend fun searchPerson(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbPerson>

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbSearchResult>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("append_to_response") appendToResponse: String = "credits,videos,images,release_dates,similar,recommendations,reviews,watch/providers,alternative_titles,external_ids",
        @Query("language") language: String = "en-US",
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): TmdbMovieDetails

    @GET("tv/{tv_id}")
    suspend fun getTvShowDetails(
        @Path("tv_id") tvId: Int,
        @Query("append_to_response") appendToResponse: String = "credits,videos,images,content_ratings,similar,recommendations,reviews,watch/providers,alternative_titles,external_ids",
        @Query("language") language: String = "en-US",
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): TmdbTvShowDetails

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getTvSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "en-US"
    ): TmdbTvSeasonDetails

    @GET("person/{person_id}")
    suspend fun getPersonDetails(
        @Path("person_id") personId: Int,
        @Query("append_to_response") appendToResponse: String = "movie_credits,tv_credits",
        @Query("language") language: String = "en-US"
    ): TmdbPersonDetails

    @GET("collection/{collection_id}")
    suspend fun getCollectionDetails(
        @Path("collection_id") collectionId: Int,
        @Query("language") language: String = "en-US"
    ): TmdbCollectionDetails

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbMovie>

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbMovie>

    @GET("tv/top_rated")
    suspend fun getTopRatedTvShows(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbTvShow>

    @GET("movie/{movie_id}/images")
    suspend fun getMovieImages(
        @Path("movie_id") movieId: Int,
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): TmdbImageResponse

    @GET("tv/{tv_id}/images")
    suspend fun getTvImages(
        @Path("tv_id") tvId: Int,
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): TmdbImageResponse

    @GET("movie/{movie_id}/external_ids")
    suspend fun getMovieExternalIds(
        @Path("movie_id") movieId: Int
    ): TmdbExternalIds

    @GET("tv/{tv_id}/external_ids")
    suspend fun getTvExternalIds(
        @Path("tv_id") tvId: Int
    ): TmdbExternalIds

    @GET("movie/{movie_id}/reviews")
    suspend fun getMovieReviews(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbReview>

    @GET("tv/{tv_id}/reviews")
    suspend fun getTvReviews(
        @Path("tv_id") tvId: Int,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbReview>
}
