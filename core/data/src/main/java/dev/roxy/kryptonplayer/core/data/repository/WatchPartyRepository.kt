package dev.roxy.kryptonplayer.core.data.repository

import dev.roxy.kryptonplayer.core.data.network.models.SyncRoomResponse

interface WatchPartyRepository {
    suspend fun createSyncRoom(token: String, fileId: String): SyncRoomResponse?
    suspend fun joinSyncRoom(roomId: String, token: String): SyncRoomResponse?
}
