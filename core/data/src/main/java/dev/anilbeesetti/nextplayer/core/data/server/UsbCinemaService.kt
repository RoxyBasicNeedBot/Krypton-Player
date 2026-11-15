package dev.anilbeesetti.nextplayer.core.data.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.anilbeesetti.nextplayer.core.data.R as coreDataR
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UsbCinemaService: Persistent Foreground Service for USB & Wi-Fi PC Cinema streaming.
 * Holds CPU WakeLock and High-Performance WifiLock, preventing Android Doze mode and
 * background process freezes from terminating PC playback after 15-20 minutes.
 */
@AndroidEntryPoint
class UsbCinemaService : Service() {

    companion object {
        private const val TAG = "UsbCinemaService"
        const val CHANNEL_ID = "archplayer_usb_cinema"
        const val NOTIFICATION_ID = 10002

        const val ACTION_START = "dev.anilbeesetti.nextplayer.action.START_USB_CINEMA"
        const val ACTION_STOP = "dev.anilbeesetti.nextplayer.action.STOP_USB_CINEMA"

        const val EXTRA_TITLE = "extra_video_title"
        const val EXTRA_PIN = "extra_pairing_pin"
        const val EXTRA_URL = "extra_stream_url"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context, title: String, pin: String, streamUrl: String) {
            try {
                val intent = Intent(context, UsbCinemaService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_PIN, pin)
                    putExtra(EXTRA_URL, streamUrl)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isRunning = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start UsbCinemaService: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            if (!isRunning) return
            try {
                val intent = Intent(context, UsbCinemaService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop UsbCinemaService: ${e.message}", e)
            }
        }
    }

    @Inject
    lateinit var usbMediaServer: UsbMediaServer

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var isForegroundStarted = false

    private var currentTitle: String = "USB Cinema Stream"
    private var currentPin: String = ""
    private var currentUrl: String = ""

    override fun onCreate() {
        super.onCreate()
        acquireLocks()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand: action=$action")

        if (action == ACTION_STOP) {
            Log.i(TAG, "Received STOP action. Stopping server and service.")
            if (::usbMediaServer.isInitialized) {
                usbMediaServer.stopServer()
            }
            stopForegroundSafely()
            stopSelf()
            return START_NOT_STICKY
        }

        intent?.getStringExtra(EXTRA_TITLE)?.let { if (it.isNotBlank()) currentTitle = it }
        intent?.getStringExtra(EXTRA_PIN)?.let { currentPin = it }
        intent?.getStringExtra(EXTRA_URL)?.let { currentUrl = it }

        acquireLocks()
        startForegroundWithNotification()
        monitorServerState()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        isRunning = false
        serviceScope.cancel()
        releaseLocks()
        stopForegroundSafely()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "USB Cinema Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active streaming connection for PC Cinema and VLC"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                }
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundStarted = true
        } catch (e: Exception) {
            Log.w(TAG, "Standard startForeground failed: ${e.message}. Trying DATA_SYNC fallback...")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                isForegroundStarted = true
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback startForeground failed: ${e2.message}", e2)
            }
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, UsbCinemaService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2001,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                2002,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val text = buildString {
            append("Playing on PC")
            if (currentUrl.isNotBlank()) append(" • ").append(currentUrl)
            if (currentPin.isNotBlank()) append(" (PIN: ").append(currentPin).append(")")
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(coreDataR.drawable.ic_launcher_monochrome)
            .setContentTitle("🎬 Arch Cinema Server Active")
            .setContentText(currentTitle)
            .setSubText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Server", stopPendingIntent)
            .build()
    }

    private fun stopForegroundSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "stopForeground error: ${e.message}")
        }
        isForegroundStarted = false
    }

    private fun monitorServerState() {
        serviceScope.launch {
            while (isActive) {
                delay(15000L)
                if (::usbMediaServer.isInitialized && !usbMediaServer.isServerRunning.value) {
                    Log.i(TAG, "UsbMediaServer is no longer running. Stopping UsbCinemaService.")
                    stopForegroundSafely()
                    stopSelf()
                    break
                }
                acquireLocks()
            }
        }
    }

    private fun acquireLocks() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "KryptonPlayer:UsbCinemaServiceWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire()
                Log.d(TAG, "UsbCinemaService WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire Service WakeLock: ${e.message}")
        }

        try {
            if (wifiLock == null) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifiLock = wifiManager?.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "KryptonPlayer:UsbCinemaServiceWifiLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
                Log.d(TAG, "UsbCinemaService WifiLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire Service WifiLock: ${e.message}")
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "UsbCinemaService WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release Service WakeLock: ${e.message}")
        }
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                Log.d(TAG, "UsbCinemaService WifiLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release Service WifiLock: ${e.message}")
        }
    }
}
