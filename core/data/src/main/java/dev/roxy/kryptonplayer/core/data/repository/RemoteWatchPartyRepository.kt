package dev.roxy.kryptonplayer.core.data.repository

import dev.roxy.kryptonplayer.core.data.network.TelegramApi
import dev.roxy.kryptonplayer.core.data.network.models.CreateSyncRoomRequest
import dev.roxy.kryptonplayer.core.data.network.models.SyncRoomResponse
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
