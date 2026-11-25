package dev.roxy.kryptonplayer.feature.player.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.roxy.kryptonplayer.core.common.playback.FrameThumbnailExtractor
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.feature.player.mpv.MpvPlaybackController
import dev.roxy.kryptonplayer.feature.player.thumbnail.MpvFrameExtractor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerEngineModule {
    @Binds
    @Singleton
    abstract fun bindPlaybackController(impl: MpvPlaybackController): PlaybackController

    @Binds
    @Singleton
    abstract fun bindFrameThumbnailExtractor(impl: MpvFrameExtractor): FrameThumbnailExtractor
}
