package dev.roxy.kryptonplayer.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.roxy.kryptonplayer.core.data.repository.LocalMediaRepository
import dev.roxy.kryptonplayer.core.data.repository.LocalPreferencesRepository
import dev.roxy.kryptonplayer.core.data.repository.LocalSearchHistoryRepository
import dev.roxy.kryptonplayer.core.data.repository.MediaRepository
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.data.repository.SearchHistoryRepository
import dev.roxy.kryptonplayer.core.data.repository.WatchPartyRepository
import dev.roxy.kryptonplayer.core.data.repository.RemoteWatchPartyRepository
import dev.roxy.kryptonplayer.core.data.repository.TmdbRepository
import dev.roxy.kryptonplayer.core.data.repository.TmdbRepositoryImpl
import dev.roxy.kryptonplayer.core.data.repository.ImdbRepository
import dev.roxy.kryptonplayer.core.data.repository.ImdbRepositoryImpl
import dev.roxy.kryptonplayer.core.data.repository.HighlightsRepository
import dev.roxy.kryptonplayer.core.data.repository.HighlightsRepositoryImpl
import dev.roxy.kryptonplayer.core.data.repository.ForYouRepository
import dev.roxy.kryptonplayer.core.data.repository.ForYouRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    @Singleton
    fun bindsTmdbRepository(
        tmdbRepository: TmdbRepositoryImpl,
    ): TmdbRepository

    @Binds
    @Singleton
    fun bindsImdbRepository(
        imdbRepository: ImdbRepositoryImpl,
    ): ImdbRepository

    @Binds
    fun bindsMediaRepository(
        videoRepository: LocalMediaRepository,
    ): MediaRepository

    @Binds
    @Singleton
    fun bindsPreferencesRepository(
        preferencesRepository: LocalPreferencesRepository,
    ): PreferencesRepository

    @Binds
    @Singleton
    fun bindsSearchHistoryRepository(
        searchHistoryRepository: LocalSearchHistoryRepository,
    ): SearchHistoryRepository

    @Binds
    @Singleton
    fun bindsWatchPartyRepository(
        watchPartyRepository: RemoteWatchPartyRepository,
    ): WatchPartyRepository

    @Binds
    @Singleton
    fun bindsHighlightsRepository(
        highlightsRepository: HighlightsRepositoryImpl,
    ): HighlightsRepository

    @Binds
    @Singleton
    fun bindsForYouRepository(
        forYouRepository: ForYouRepositoryImpl,
    ): ForYouRepository
}
