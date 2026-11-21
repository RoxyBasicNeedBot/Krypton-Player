package dev.roxy.kryptonplayer.core.domain

import dev.roxy.kryptonplayer.core.common.Dispatcher
import dev.roxy.kryptonplayer.core.common.NextDispatchers
import dev.roxy.kryptonplayer.core.data.repository.MediaRepository
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class GetRecentVideosUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    @Dispatcher(NextDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(limit: Int = 10): Flow<List<Video>> {
        return combine(
            mediaRepository.getVideosFlow(),
            preferencesRepository.applicationPreferences,
        ) { videos, preferences ->
            videos.filterNot { it.parentPath in preferences.excludeFolders }
                .sortedByDescending { it.dateModified }
                .take(limit)
        }.flowOn(defaultDispatcher)
    }
}
