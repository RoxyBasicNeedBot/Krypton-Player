package dev.roxy.kryptonplayer.core.database

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.roxy.kryptonplayer.core.database.dao.DirectoryDao
import dev.roxy.kryptonplayer.core.database.dao.MediumDao

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    fun provideMediumDao(db: MediaDatabase): MediumDao = db.mediumDao()

    @Provides
    fun provideMediumStateDao(db: MediaDatabase) = db.mediumStateDao()

    @Provides
    fun provideDirectoryDao(db: MediaDatabase): DirectoryDao = db.directoryDao()

    @Provides
    fun provideWatchLaterDao(db: MediaDatabase): dev.roxy.kryptonplayer.core.database.dao.WatchLaterDao = db.watchLaterDao()

    @Provides
    fun provideBookmarkDao(db: MediaDatabase): dev.roxy.kryptonplayer.core.database.dao.BookmarkDao = db.bookmarkDao()
}
