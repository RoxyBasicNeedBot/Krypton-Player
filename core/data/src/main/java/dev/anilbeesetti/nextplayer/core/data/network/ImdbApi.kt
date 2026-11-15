package dev.anilbeesetti.nextplayer.core.data.network

import dev.anilbeesetti.nextplayer.core.data.network.models.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ImdbApi {
    @GET("titles/{id}")
    suspend fun getTitle(@Path("id") id: String): ImdbTitle

    @GET("titles:batchGet")
    suspend fun batchGetTitles(@Query("titleIds") ids: List<String>): ImdbTitleResponse

    @GET("titles")
    suspend fun listTitles(
        @Query("types") types: List<String>? = null,
        @Query("genres") genres: List<String>? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortOrder") sortOrder: String? = null,
        @Query("minAggregateRating") minRating: Float? = null,
        @Query("minVoteCount") minVotes: Int? = null,
        @Query("pageToken") pageToken: String? = null,
        @Query("pageSize") pageSize: Int? = null
    ): ImdbTitleResponse

    @GET("search/titles")
    suspend fun searchTitles(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20
    ): ImdbTitleResponse

    @GET("titles/{id}/credits")
    suspend fun getCredits(
        @Path("id") id: String,
        @Query("categories") categories: List<String>? = null,
        @Query("pageSize") pageSize: Int = 20,
        @Query("pageToken") pageToken: String? = null
    ): ImdbCreditsResponse

    @GET("titles/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") id: String,
        @Query("season") season: String? = null,
        @Query("pageSize") pageSize: Int = 20,
        @Query("pageToken") pageToken: String? = null
    ): ImdbEpisodesResponse

    @GET("titles/{id}/seasons")
    suspend fun getSeasons(@Path("id") id: String): ImdbSeasonsResponse

    @GET("titles/{id}/parentsGuide")
    suspend fun getParentsGuide(@Path("id") id: String): ImdbParentsGuideResponse

    @GET("titles/{id}/boxOffice")
    suspend fun getBoxOffice(@Path("id") id: String): ImdbBoxOffice

    @GET("titles/{id}/certificates")
    suspend fun getCertificates(
        @Path("id") id: String,
        @Query("pageSize") pageSize: Int = 50
    ): ImdbCertificatesResponse

    @GET("titles/{id}/akas")
    suspend fun getAKAs(@Path("id") id: String): ImdbAKAsResponse

    @GET("names/{id}")
    suspend fun getName(@Path("id") id: String): ImdbName

    @GET("names/{id}/filmography")
    suspend fun getFilmography(@Path("id") id: String): ImdbFilmographyResponse

    @GET("names/{id}/relationships")
    suspend fun getRelationships(@Path("id") id: String): ImdbRelationshipsResponse

    @GET("names/{id}/trivia")
    suspend fun getTrivia(@Path("id") id: String): ImdbTriviaResponse

    @GET("titles/{id}/images")
    suspend fun getImages(
        @Path("id") id: String,
        @Query("pageSize") pageSize: Int = 50,
        @Query("pageToken") pageToken: String? = null
    ): ImdbImagesResponse
}
