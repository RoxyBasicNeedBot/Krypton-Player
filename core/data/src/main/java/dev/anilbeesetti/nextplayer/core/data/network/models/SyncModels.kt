package dev.anilbeesetti.nextplayer.core.data.network.models

import com.google.gson.annotations.SerializedName

// Request body for creating a sync room
data class CreateSyncRoomRequest(
    @SerializedName("file_id") val fileId: String
)

// Response from create/join room endpoints
data class SyncRoomResponse(
    @SerializedName("room_id") val roomId: String,
    @SerializedName("file_id") val fileId: String,
    @SerializedName("host_username") val hostUsername: String,
    @SerializedName("stream_url") val streamUrl: String? = null,
    @SerializedName("video_name") val videoName: String? = null,
    @SerializedName("member_count") val memberCount: Int = 0
)

// Sealed class for sync events received via WebSocket
sealed class SyncEvent {
    data class RoomInfo(
        val roomId: String,
        val fileId: String,
        val hostUsername: String,
        val members: List<SyncMember>,
        val memberCount: Int
    ) : SyncEvent()

    data class Play(val positionMs: Long, val fromUser: String, val serverTime: Long = 0) : SyncEvent()
    data class Pause(val positionMs: Long, val fromUser: String, val serverTime: Long = 0) : SyncEvent()
    data class Seek(val positionMs: Long, val fromUser: String, val serverTime: Long = 0) : SyncEvent()
    data class UserJoined(val username: String, val memberCount: Int) : SyncEvent()
    data class UserLeft(val username: String, val memberCount: Int) : SyncEvent()
    data class Chat(val message: String, val fromUser: String, val timestamp: String) : SyncEvent()
    data class Error(val message: String) : SyncEvent()
    data object RoomClosed : SyncEvent()
    data object Disconnected : SyncEvent()
}

data class SyncMember(
    @SerializedName("user_id") val userId: Int,
    val username: String
)

// Chat message to send
data class ChatMessage(
    val type: String = "chat",
    val message: String
)
