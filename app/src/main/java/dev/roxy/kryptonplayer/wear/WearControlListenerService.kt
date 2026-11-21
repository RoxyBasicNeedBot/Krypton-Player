package dev.roxy.kryptonplayer.wear

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dev.roxy.kryptonplayer.feature.player.service.PlayerService
import org.json.JSONObject

class WearControlListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearControlService"

        const val PATH_PLAY_PAUSE = "/krypton/play_pause"
        const val PATH_NEXT = "/krypton/next"
        const val PATH_PREV = "/krypton/prev"
        const val PATH_VOLUME_UP = "/krypton/volume_up"
        const val PATH_VOLUME_DOWN = "/krypton/volume_down"
        const val PATH_GET_STATUS = "/krypton/get_status"
        const val PATH_STATUS = "/krypton/status"

        fun broadcastStatusToWear(
            context: Context,
            title: String,
            isPlaying: Boolean,
            positionMs: Long = 0L,
            durationMs: Long = 0L
        ) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val json = JSONObject().apply {
                    put("title", title)
                    put("isPlaying", isPlaying)
                    put("positionMs", positionMs)
                    put("durationMs", durationMs)
                    put("volume", currentVol)
                    put("maxVolume", maxVol)
                }
                val payload = json.toString().toByteArray(Charsets.UTF_8)
                val messageClient = Wearable.getMessageClient(context)
                Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                    for (node in nodes) {
                        messageClient.sendMessage(node.id, PATH_STATUS, payload)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to broadcast status to Wear OS", e)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val sourceNodeId = messageEvent.sourceNodeId
        Log.d(TAG, "Received message from wear: path=$path, node=$sourceNodeId")

        when (path) {
            PATH_VOLUME_UP -> {
                adjustVolume(AudioManager.ADJUST_RAISE)
                requestStatusAndReply(sourceNodeId)
            }
            PATH_VOLUME_DOWN -> {
                adjustVolume(AudioManager.ADJUST_LOWER)
                requestStatusAndReply(sourceNodeId)
            }
            PATH_PLAY_PAUSE, PATH_NEXT, PATH_PREV, PATH_GET_STATUS -> {
                handlePlayerCommand(path, sourceNodeId)
            }
            else -> {
                super.onMessageReceived(messageEvent)
            }
        }
    }

    private fun adjustVolume(direction: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun handlePlayerCommand(path: String, sourceNodeId: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                val sessionToken = SessionToken(
                    applicationContext,
                    ComponentName(applicationContext, PlayerService::class.java)
                )
                val controllerFuture = MediaController.Builder(applicationContext, sessionToken).buildAsync()
                controllerFuture.addListener({
                    try {
                        val controller = controllerFuture.get()
                        when (path) {
                            PATH_PLAY_PAUSE -> {
                                if (controller.isPlaying) {
                                    controller.pause()
                                } else {
                                    controller.play()
                                }
                            }
                            PATH_NEXT -> {
                                if (controller.hasNextMediaItem()) {
                                    controller.seekToNextMediaItem()
                                }
                            }
                            PATH_PREV -> {
                                if (controller.hasPreviousMediaItem()) {
                                    controller.seekToPreviousMediaItem()
                                } else {
                                    controller.seekTo(0L)
                                }
                            }
                            PATH_GET_STATUS -> {
                                // Query status and return current playback state
                            }
                        }
                        sendStatusReply(controller, sourceNodeId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error executing player command on MediaController: ${e.message}")
                        sendStatusReply(null, sourceNodeId)
                    } finally {
                        MediaController.releaseFuture(controllerFuture)
                    }
                }, ContextCompat.getMainExecutor(applicationContext))
            } catch (e: Exception) {
                Log.e(TAG, "Error binding MediaController: ${e.message}")
                sendStatusReply(null, sourceNodeId)
            }
        }
    }

    private fun requestStatusAndReply(sourceNodeId: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                val sessionToken = SessionToken(
                    applicationContext,
                    ComponentName(applicationContext, PlayerService::class.java)
                )
                val controllerFuture = MediaController.Builder(applicationContext, sessionToken).buildAsync()
                controllerFuture.addListener({
                    try {
                        val controller = controllerFuture.get()
                        sendStatusReply(controller, sourceNodeId)
                    } catch (e: Exception) {
                        sendStatusReply(null, sourceNodeId)
                    } finally {
                        MediaController.releaseFuture(controllerFuture)
                    }
                }, ContextCompat.getMainExecutor(applicationContext))
            } catch (e: Exception) {
                sendStatusReply(null, sourceNodeId)
            }
        }
    }

    private fun sendStatusReply(controller: MediaController?, targetNodeId: String) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            val title = controller?.currentMediaItem?.mediaMetadata?.title?.toString()
                ?: controller?.currentMediaItem?.mediaId
                ?: "Krypton Player"
            val isPlaying = controller?.isPlaying ?: false
            val positionMs = controller?.currentPosition ?: 0L
            val durationMs = controller?.duration?.takeIf { it > 0 } ?: 0L

            val json = JSONObject().apply {
                put("title", title)
                put("isPlaying", isPlaying)
                put("positionMs", positionMs)
                put("durationMs", durationMs)
                put("volume", currentVol)
                put("maxVolume", maxVol)
            }

            val payload = json.toString().toByteArray(Charsets.UTF_8)
            val messageClient = Wearable.getMessageClient(applicationContext)
            messageClient.sendMessage(targetNodeId, PATH_STATUS, payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending status reply to wear", e)
        }
    }
}
