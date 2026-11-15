package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.network.models.SyncRoomResponse

interface WatchPartyRepository {
    suspend fun createSyncRoom(token: String, fileId: String): SyncRoomResponse?
    suspend fun joinSyncRoom(roomId: String, token: String): SyncRoomResponse?
}
