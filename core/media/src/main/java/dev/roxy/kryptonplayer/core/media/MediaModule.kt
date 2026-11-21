package dev.roxy.kryptonplayer.core.media

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.crossfade
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.roxy.kryptonplayer.core.media.services.LocalMediaService
import dev.roxy.kryptonplayer.core.media.services.MediaService
import dev.roxy.kryptonplayer.core.media.sync.LocalMediaInfoSynchronizer
import dev.roxy.kryptonplayer.core.media.sync.LocalMediaSynchronizer
import dev.roxy.kryptonplayer.core.media.sync.MediaInfoSynchronizer
import dev.roxy.kryptonplayer.core.media.sync.MediaSynchronizer
import dev.roxy.kryptonplayer.core.model.ApplicationPreferences
import dev.roxy.kryptonplayer.core.model.ThumbnailGenerationStrategy
import javax.inject.Singleton
import okio.FileSystem

@Module
@InstallIn(SingletonComponent::class)
interface MediaModule {

    @Binds
    @Singleton
    fun bindsMediaSynchronizer(
        mediaSynchronizer: LocalMediaSynchronizer,
    ): MediaSynchronizer

    @Binds
    @Singleton
    fun bindsMediaInfoSynchronizer(
        mediaInfoSynchronizer: LocalMediaInfoSynchronizer,
    ): MediaInfoSynchronizer

    @Binds
    @Singleton
    fun bindMediaService(
        mediaService: LocalMediaService,
    ): MediaService
}
