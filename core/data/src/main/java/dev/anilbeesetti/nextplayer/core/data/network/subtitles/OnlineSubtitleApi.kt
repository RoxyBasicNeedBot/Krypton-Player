package dev.anilbeesetti.nextplayer.core.data.network.subtitles

import dev.anilbeesetti.nextplayer.core.data.BuildConfig
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface OpenSubtitlesApi {

    @GET("subtitles")
    suspend fun searchSubtitles(
        @Query("query") query: String,
        @Query("languages") languages: String? = null,
        @Header("Api-Key") apiKey: String = BuildConfig.OPENSUBTITLES_API_KEY,
        @Header("User-Agent") userAgent: String = "NextPlayer v1.0"
    ): OpenSubtitlesSearchResponse

    @POST("download")
    suspend fun getDownloadLink(
        @Body body: OpenSubtitlesDownloadRequest,
        @Header("Api-Key") apiKey: String = BuildConfig.OPENSUBTITLES_API_KEY,
        @Header("User-Agent") userAgent: String = "NextPlayer v1.0"
    ): OpenSubtitlesDownloadResponse
}

interface SubdlApi {

    @GET("subtitles")
    suspend fun searchSubtitles(
        @Query("film_name") query: String,
        @Query("languages") languages: String? = null,
        @Query("type") type: String = "all"
    ): SubdlSearchResponse
}

interface ImdbSuggestApi {

    @GET("suggestion/x/{query}.json")
    suspend fun suggest(
        @retrofit2.http.Path("query") query: String,
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    ): ImdbSuggestResponse
}

interface StremioSubtitleApi {

    @GET("subtitles/series/{id}.json")
    suspend fun getSeriesSubtitles(
        @retrofit2.http.Path(value = "id", encoded = true) id: String
    ): StremioSubtitleResponse

    @GET("subtitles/movie/{id}.json")
    suspend fun getMovieSubtitles(
        @retrofit2.http.Path(value = "id", encoded = true) id: String
    ): StremioSubtitleResponse
}
