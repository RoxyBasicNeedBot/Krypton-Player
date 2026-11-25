package dev.roxy.kryptonplayer.core.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.roxy.kryptonplayer.core.data.network.models.SyncEvent
import dev.roxy.kryptonplayer.core.data.network.models.SyncMember
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncWebSocketClient @Inject constructor() {

    companion object {
        private const val TAG = "SyncWebSocket"
        private const val BASE_WS_URL = "wss://your-backup.onrender.com"
    }

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null

    private val _syncEvents = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 64)
    val syncEvents: SharedFlow<SyncEvent> = _syncEvents.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect(roomId: String, token: String) {
        disconnect() // Ensure clean state

        okHttpClient = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val url = "$BASE_WS_URL/ws/sync/$roomId?token=$token"
        Log.d(TAG, "Connecting to: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = okHttpClient?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected to room $roomId")
                _isConnected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received: $text")
                try {
                    val json = JsonParser.parseString(text).asJsonObject
                    val event = parseEvent(json)
                    if (event != null) {
                        _syncEvents.tryEmit(event)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message: $text", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                _isConnected.value = false
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                _isConnected.value = false
                _syncEvents.tryEmit(SyncEvent.Disconnected)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                _isConnected.value = false
                _syncEvents.tryEmit(SyncEvent.Disconnected)
            }
        })
    }

    private fun parseEvent(json: JsonObject): SyncEvent? {
        val serverTime = json.get("server_time")?.asLong ?: 0L
        return when (json.get("type")?.asString) {
            "room_info" -> SyncEvent.RoomInfo(
                roomId = json.get("room_id")?.asString ?: "",
                fileId = json.get("file_id")?.asString ?: "",
                hostUsername = json.get("host_username")?.asString ?: "",
                members = json.getAsJsonArray("members")?.map {
                    val m = it.asJsonObject
                    SyncMember(m.get("user_id")?.asInt ?: 0, m.get("username")?.asString ?: "")
                } ?: emptyList(),
                memberCount = json.get("member_count")?.asInt ?: 0
            )
            "play" -> SyncEvent.Play(
                positionMs = json.get("position_ms")?.asLong ?: 0,
                fromUser = json.get("from_user")?.asString ?: "",
                serverTime = serverTime
            )
            "pause" -> SyncEvent.Pause(
                positionMs = json.get("position_ms")?.asLong ?: 0,
                fromUser = json.get("from_user")?.asString ?: "",
                serverTime = serverTime
            )
            "seek" -> SyncEvent.Seek(
                positionMs = json.get("position_ms")?.asLong ?: 0,
                fromUser = json.get("from_user")?.asString ?: "",
                serverTime = serverTime
            )
            "user_joined" -> SyncEvent.UserJoined(
                username = json.get("username")?.asString ?: "",
                memberCount = json.get("member_count")?.asInt ?: 0
            )
            "user_left" -> SyncEvent.UserLeft(
                username = json.get("username")?.asString ?: "",
                memberCount = json.get("member_count")?.asInt ?: 0
            )
            "chat" -> SyncEvent.Chat(
                message = json.get("message")?.asString ?: "",
                fromUser = json.get("from_user")?.asString ?: "",
                timestamp = json.get("timestamp")?.asString ?: ""
            )
            "error" -> SyncEvent.Error(
                message = json.get("message")?.asString ?: "Unknown error"
            )
            "room_closed" -> SyncEvent.RoomClosed
            else -> null
        }
    }

    fun sendPlay(positionMs: Long) {
        sendJson(mapOf("type" to "play", "position_ms" to positionMs))
    }

    fun sendPause(positionMs: Long) {
        sendJson(mapOf("type" to "pause", "position_ms" to positionMs))
    }

    fun sendSeek(positionMs: Long) {
        sendJson(mapOf("type" to "seek", "position_ms" to positionMs))
    }

    fun sendChat(message: String) {
        sendJson(mapOf("type" to "chat", "message" to message))
    }

    fun sendSyncPosition(positionMs: Long) {
        sendJson(mapOf("type" to "sync_position", "position_ms" to positionMs))
    }

    private fun sendJson(data: Map<String, Any>) {
        val json = gson.toJson(data)
        Log.d(TAG, "Sending: $json")
        webSocket?.send(json)
    }

    fun disconnect() {
        _isConnected.value = false
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        okHttpClient?.dispatcher?.executorService?.shutdown()
        okHttpClient = null
    }
}
