package dev.anilbeesetti.nextplayer.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.anilbeesetti.nextplayer.MainActivity
import dev.anilbeesetti.nextplayer.R

object FcmNotificationHelper {
    const val CHANNEL_ID = "krypton_fcm_notifications"
    const val CHANNEL_NAME = "Announcements & Updates"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for announcements, updates, and messages from Krypton Player"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
}

class KryptonFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "KryptonFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Registration Token: $token")
        saveToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "FCM Message received from: ${remoteMessage.from}")

        // 1. Resolve Title
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: remoteMessage.data["subject"]
            ?: "Krypton Player"

        // 2. Resolve Body
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["content"]
            ?: ""

        if (title.isNotBlank() || body.isNotBlank()) {
            showNotification(title, body, remoteMessage.data, remoteMessage.messageId)
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        messageId: String?
    ) {
        FcmNotificationHelper.createNotificationChannel(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val requestCode = ((messageId?.hashCode() ?: System.currentTimeMillis().toInt()) and 0xFFFF)
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, FcmNotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = (messageId?.hashCode() ?: System.currentTimeMillis().toInt())
        notificationManager?.notify(notificationId, notificationBuilder.build())
    }

    private fun saveToken(token: String) {
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val data = mapOf(
                    "fcmToken" to token,
                    "lastTokenUpdate" to System.currentTimeMillis()
                )
                FirebaseFirestore.getInstance().collection("users").document(user.uid)
                    .set(data, SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save FCM token to Firestore: ${e.message}")
        }
    }
}
