package dev.anilbeesetti.nextplayer.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anilbeesetti.nextplayer.core.data.repository.LocalMediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalPreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalSearchHistoryRepository
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.repository.SearchHistoryRepository
import dev.anilbeesetti.nextplayer.core.data.repository.WatchPartyRepository
import dev.anilbeesetti.nextplayer.core.data.repository.RemoteWatchPartyRepository
import dev.anilbeesetti.nextplayer.core.data.repository.TmdbRepository
import dev.anilbeesetti.nextplayer.core.data.repository.TmdbRepositoryImpl
import dev.anilbeesetti.nextplayer.core.data.repository.ImdbRepository
import dev.anilbeesetti.nextplayer.core.data.repository.ImdbRepositoryImpl
import dev.anilbeesetti.nextplayer.core.data.repository.HighlightsRepository
import dev.anilbeesetti.nextplayer.core.data.repository.HighlightsRepositoryImpl
import dev.anilbeesetti.nextplayer.core.data.repository.ForYouRepository
import dev.anilbeesetti.nextplayer.core.data.repository.ForYouRepositoryImpl
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
