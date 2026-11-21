package dev.roxy.kryptonplayer

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.model.ThumbnailGenerationStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okio.FileSystem
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        preferencesRepository: PreferencesRepository,
        okHttpClient: OkHttpClient,
    ): ImageLoader {
        val imageOkHttpClient = okHttpClient.newBuilder()
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = 128
                    maxRequestsPerHost = 32
                }
            )
            .connectionPool(ConnectionPool(32, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .cache(Cache(context.cacheDir.resolve("http_image_cache"), 100L * 1024 * 1024))
            .addInterceptor { chain ->
                val original = chain.request()
                val newRequest = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(newRequest)
            }
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { imageOkHttpClient }
                    )
                )
                add(
                    VideoThumbnailDecoder.Factory(
                        thumbnailStrategy = {
                            val preferences = preferencesRepository.applicationPreferences.value
                            when (preferences.thumbnailGenerationStrategy) {
                                ThumbnailGenerationStrategy.FIRST_FRAME -> ThumbnailStrategy.FirstFrame
                                ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE -> ThumbnailStrategy.FrameAtPercentage(preferences.thumbnailFramePosition)
                                ThumbnailGenerationStrategy.HYBRID -> ThumbnailStrategy.Hybrid(preferences.thumbnailFramePosition)
                            }
                        },
                    ),
                )
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.30)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache(
                DiskCache.Builder()
                    .fileSystem(FileSystem.SYSTEM)
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.25)
                    .build(),
            )
            .crossfade(150)
            .build()
    }
}
