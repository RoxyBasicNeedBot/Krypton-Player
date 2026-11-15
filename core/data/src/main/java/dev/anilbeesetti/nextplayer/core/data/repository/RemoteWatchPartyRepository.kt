package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.network.TelegramApi
import dev.anilbeesetti.nextplayer.core.data.network.models.CreateSyncRoomRequest
import dev.anilbeesetti.nextplayer.core.data.network.models.SyncRoomResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteWatchPartyRepository @Inject constructor(
    private val telegramApi: TelegramApi
) : WatchPartyRepository {
    override suspend fun createSyncRoom(token: String, fileId: String): SyncRoomResponse? {
        return try {
            telegramApi.createSyncRoom(token, CreateSyncRoomRequest(fileId))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun joinSyncRoom(roomId: String, token: String): SyncRoomResponse? {
        return try {
            telegramApi.joinSyncRoom(roomId, token)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
