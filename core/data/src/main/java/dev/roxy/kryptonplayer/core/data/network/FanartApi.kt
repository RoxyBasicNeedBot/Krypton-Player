package dev.roxy.kryptonplayer.core.data.network

import dev.roxy.kryptonplayer.core.data.BuildConfig
import dev.roxy.kryptonplayer.core.data.network.models.FanartMovieResponse
import dev.roxy.kryptonplayer.core.data.network.models.FanartTvResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FanartApi {
    companion object {
        val API_KEY: String get() = BuildConfig.FANART_API_KEY
    }

    @GET("movies/{id}")
    suspend fun getMovieImages(
        @Path("id") id: String,
        @Query("api_key") apiKey: String = BuildConfig.FANART_API_KEY
    ): FanartMovieResponse

    @GET("tv/{id}")
    suspend fun getTvImages(
        @Path("id") id: String,
        @Query("api_key") apiKey: String = BuildConfig.FANART_API_KEY
    ): FanartTvResponse
}
