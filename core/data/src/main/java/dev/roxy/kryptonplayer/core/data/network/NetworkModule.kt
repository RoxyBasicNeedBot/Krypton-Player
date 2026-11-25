package dev.roxy.kryptonplayer.core.data.network

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.roxy.kryptonplayer.core.data.network.models.TmdbSearchResult
import dev.roxy.kryptonplayer.core.data.network.models.TmdbSearchResultDeserializer
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Backend server URL & client key dynamically loaded via NetworkConfig
    val BASE_URL: String get() = NetworkConfig.BASE_URL
    val ARCH_CLIENT_KEY: String get() = NetworkConfig.CLIENT_KEY

    @Provides
    @Singleton
    fun provideGson(): com.google.gson.Gson {
        return com.google.gson.Gson()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): OkHttpClient {
        val prefs = context.getSharedPreferences("device_auth_prefs", android.content.Context.MODE_PRIVATE)
        return OkHttpClient.Builder()
            .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val token = prefs.getString("approval_token", null)
                val deviceId = prefs.getString("stable_device_id", null) 
                    ?: android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
                    ?: ""
                val builder = original.newBuilder()
                    .header("User-Agent", "KryptonPlayer/1.0 (Linux; Android 14; Mobile)")
                    .header("X-Arch-Client-Key", ARCH_CLIENT_KEY)
                    .header("X-Device-Id", deviceId)

                if (!token.isNullOrBlank()) {
                    builder.header("X-Approval-Token", token)
                }

                val request = builder.build()
                val originalUrl = request.url
                val primaryHost = try {
                    java.net.URI(BASE_URL).host ?: "your-worker.domain.workers.dev"
                } catch (_: Exception) {
                    "your-worker.domain.workers.dev"
                }
                val backupHost = "your-backup.onrender.com"

                val isGatewayRequest = originalUrl.host == primaryHost || originalUrl.host == backupHost

                if (!isGatewayRequest) {
                    return@addInterceptor chain.proceed(request)
                }

                try {
                    val response = chain.proceed(request)
                    if (!response.isSuccessful && response.code in 502..504) {
                        response.close()
                        val altHost = if (originalUrl.host == primaryHost) backupHost else primaryHost
                        val altUrl = originalUrl.newBuilder().host(altHost).build()
                        val altRequest = request.newBuilder().url(altUrl).build()
                        return@addInterceptor try {
                            chain.proceed(altRequest)
                        } catch (e: java.io.IOException) {
                            tryDirectTmdbIfApplicable(chain, request, originalUrl) ?: throw e
                        }
                    }
                    response
                } catch (e: java.io.IOException) {
                    val altHost = if (originalUrl.host == primaryHost) backupHost else primaryHost
                    val altUrl = originalUrl.newBuilder().host(altHost).build()
                    val altRequest = request.newBuilder().url(altUrl).build()
                    try {
                        chain.proceed(altRequest)
                    } catch (e2: java.io.IOException) {
                        tryDirectTmdbIfApplicable(chain, request, originalUrl) ?: throw e2
                    }
                }
            }
            .build()
    }

    private fun tryDirectTmdbIfApplicable(
        chain: okhttp3.Interceptor.Chain,
        originalRequest: okhttp3.Request,
        originalUrl: okhttp3.HttpUrl
    ): okhttp3.Response? {
        val pathSegments = originalUrl.pathSegments
        val tmdbIndex = pathSegments.indexOf("tmdb")
        if (tmdbIndex == -1 || tmdbIndex == pathSegments.size - 1) return null

        val remainingPath = pathSegments.subList(tmdbIndex + 1, pathSegments.size)
        if (remainingPath.isNotEmpty() && remainingPath[0] == "image") {
            val imageSegments = remainingPath.subList(1, remainingPath.size)
            val directImageUrl = okhttp3.HttpUrl.Builder()
                .scheme("https")
                .host("image.tmdb.org")
                .addPathSegment("t")
                .addPathSegment("p")
            for (seg in imageSegments) {
                directImageUrl.addPathSegment(seg)
            }
            val directReq = originalRequest.newBuilder()
                .url(directImageUrl.build())
                .removeHeader("X-Arch-Client-Key")
                .removeHeader("X-Device-Id")
                .removeHeader("X-Approval-Token")
                .build()
            return try { chain.proceed(directReq) } catch (_: Exception) { null }
        }

        val directApiUrl = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("api.themoviedb.org")
            .addPathSegment("3")
        for (seg in remainingPath) {
            directApiUrl.addPathSegment(seg)
        }
        for (i in 0 until originalUrl.querySize) {
            val name = originalUrl.queryParameterName(i)
            val value = originalUrl.queryParameterValue(i) ?: ""
            if (name != "api_key") {
                directApiUrl.addQueryParameter(name, value)
            }
        }
        val apiKey = dev.roxy.kryptonplayer.core.data.BuildConfig.TMDB_API_KEY.ifBlank { "" }
        directApiUrl.addQueryParameter("api_key", apiKey)

        val directReq = originalRequest.newBuilder()
            .url(directApiUrl.build())
            .removeHeader("X-Arch-Client-Key")
            .removeHeader("X-Device-Id")
            .removeHeader("X-Approval-Token")
            .build()

        return try { chain.proceed(directReq) } catch (_: Exception) { null }
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTelegramApi(retrofit: Retrofit): TelegramApi {
        return retrofit.create(TelegramApi::class.java)
    }

    @Provides
    @Singleton
    @Named("tmdb")
    fun provideTmdbRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder()
            .registerTypeAdapter(TmdbSearchResult::class.java, TmdbSearchResultDeserializer())
            .create()

        return Retrofit.Builder()
            .baseUrl("${BASE_URL}tmdb/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(@Named("tmdb") retrofit: Retrofit): TmdbApi {
        return retrofit.create(TmdbApi::class.java)
    }

    @Provides
    @Singleton
    @Named("imdb")
    fun provideImdbRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("${BASE_URL}imdb/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideImdbApi(@Named("imdb") retrofit: Retrofit): ImdbApi {
        return retrofit.create(ImdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenSubtitlesApi(): dev.roxy.kryptonplayer.core.data.network.subtitles.OpenSubtitlesApi {
        return Retrofit.Builder()
            .baseUrl("https://api.opensubtitles.com/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(dev.roxy.kryptonplayer.core.data.network.subtitles.OpenSubtitlesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSubdlApi(): dev.roxy.kryptonplayer.core.data.network.subtitles.SubdlApi {
        return Retrofit.Builder()
            .baseUrl("https://api.subdl.com/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(dev.roxy.kryptonplayer.core.data.network.subtitles.SubdlApi::class.java)
    }

    @Provides
    @Singleton
    fun provideImdbSuggestApi(): dev.roxy.kryptonplayer.core.data.network.subtitles.ImdbSuggestApi {
        return Retrofit.Builder()
            .baseUrl("https://v3.sg.media-imdb.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(dev.roxy.kryptonplayer.core.data.network.subtitles.ImdbSuggestApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStremioSubtitleApi(): dev.roxy.kryptonplayer.core.data.network.subtitles.StremioSubtitleApi {
        return Retrofit.Builder()
            .baseUrl("https://opensubtitles-v3.strem.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(dev.roxy.kryptonplayer.core.data.network.subtitles.StremioSubtitleApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFanartApi(okHttpClient: OkHttpClient): FanartApi {
        return Retrofit.Builder()
            .baseUrl("https://webservice.fanart.tv/v3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FanartApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFancapsScraper(): FancapsScraper {
        return FancapsScraper()
    }
}


