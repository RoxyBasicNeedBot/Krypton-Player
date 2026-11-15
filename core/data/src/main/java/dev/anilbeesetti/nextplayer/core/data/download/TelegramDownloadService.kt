package dev.anilbeesetti.nextplayer.core.data.download

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
import dagger.hilt.android.AndroidEntryPoint
import dev.anilbeesetti.nextplayer.core.data.R as coreDataR
import dev.anilbeesetti.nextplayer.core.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TelegramDownloadService : Service() {

    companion object {
        private const val TAG = "TelegramDownloadService"
        const val CHANNEL_ID = "archplayer_telegram_downloads"
        const val NOTIFICATION_ID = 10001

        const val ACTION_START = "dev.anilbeesetti.nextplayer.action.START_DOWNLOAD_SERVICE"
        const val ACTION_PAUSE = "dev.anilbeesetti.nextplayer.action.PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "dev.anilbeesetti.nextplayer.action.RESUME_DOWNLOAD"
        const val ACTION_CANCEL = "dev.anilbeesetti.nextplayer.action.CANCEL_DOWNLOAD"
        const val ACTION_PAUSE_ALL = "dev.anilbeesetti.nextplayer.action.PAUSE_ALL_DOWNLOADS"
        const val ACTION_RESUME_ALL = "dev.anilbeesetti.nextplayer.action.RESUME_ALL_DOWNLOADS"
        const val ACTION_CANCEL_ALL = "dev.anilbeesetti.nextplayer.action.CANCEL_ALL_DOWNLOADS"
        const val ACTION_STOP = "dev.anilbeesetti.nextplayer.action.STOP_DOWNLOAD_SERVICE"

        const val EXTRA_KEY = "extra_download_key"

        @Volatile
        var isRunning = false
            internal set

        fun start(context: Context) {
            if (isRunning) {
                Log.d(TAG, "TelegramDownloadService is already running")
                return
            }
            try {
                val intent = Intent(context, TelegramDownloadService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isRunning = true
            } catch (e: Exception) {
                isRunning = false
                Log.e(TAG, "Failed to start TelegramDownloadService: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            if (!isRunning) return
            try {
                val intent = Intent(context, TelegramDownloadService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop TelegramDownloadService: ${e.message}", e)
            }
        }

        fun showCompletionNotification(context: Context, title: String, key: String) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Telegram Downloads",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Ongoing Telegram downloads and progress"
                        setShowBadge(false)
                        setSound(null, null)
                        enableVibration(false)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                    notificationManager?.createNotificationChannel(channel)
                }

                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(
                    context,
                    Class.forName("dev.anilbeesetti.nextplayer.MainActivity")
                )
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(coreDataR.drawable.ic_launcher_monochrome)
                    .setContentTitle("Download Complete")
                    .setContentText(title)
                    .setSubText("Krypton Player")
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager?.notify(key.hashCode(), notification)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to show completion notification: ${e.message}")
            }
        }
    }

    @Inject
    lateinit var injectedDownloadManager: TelegramDownloadManager

    @Inject
    lateinit var tdlibManager: dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibManager

    private val downloadManager: TelegramDownloadManager?
        get() = if (::injectedDownloadManager.isInitialized) injectedDownloadManager else TelegramDownloadManager.getInstance()

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isForegroundStarted = false
    private var lastNotificationUpdateTime = 0L

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.i(TAG, "TelegramDownloadService onCreate")
        createNotificationChannel()
        acquireLocks()
        startInitialForeground()
        observeDownloadTasks()
        startBackgroundKeepAlive()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand: action=$action")
        acquireLocks()
        startInitialForeground()

        val dm = downloadManager
        when (action) {
            ACTION_PAUSE -> {
                val key = intent.getStringExtra(EXTRA_KEY)
                if (key != null) dm?.pauseDownload(key)
            }
            ACTION_RESUME -> {
                val key = intent.getStringExtra(EXTRA_KEY)
                if (key != null) dm?.resumeDownload(key)
            }
            ACTION_CANCEL -> {
                val key = intent.getStringExtra(EXTRA_KEY)
                if (key != null) dm?.cancelDownload(key)
            }
            ACTION_PAUSE_ALL -> {
                dm?.pauseAllDownloads()
            }
            ACTION_RESUME_ALL -> {
                dm?.resumeAllDownloads()
            }
            ACTION_CANCEL_ALL -> {
                dm?.cancelAllDownloads()
            }
            ACTION_STOP -> {
                stopForegroundSafely()
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "TelegramDownloadService onDestroy")
        isRunning = false
        serviceScope.cancel()
        releaseLocks()
        isForegroundStarted = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInitialForeground() {
        if (!isForegroundStarted) {
            val notification = buildInitialNotification()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                isForegroundStarted = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground: ${e.message}", e)
            }
        }
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

    private fun observeDownloadTasks() {
        serviceScope.launch {
            var dm = downloadManager
            while (dm == null && isActive) {
                delay(200L)
                dm = downloadManager
            }
            if (dm == null) return@launch

            dm.activeDownloadTasks.collect { tasks ->
                val runningTasks = tasks.values.filter { !it.isPaused }
                val hasActive = dm.hasActiveDownloads()

                if (runningTasks.isNotEmpty()) {
                    updateNotification(runningTasks)
                } else if (hasActive) {
                    val activeTasks = tasks.values.toList()
                    if (activeTasks.isNotEmpty()) {
                        updateNotification(activeTasks)
                    }
                } else {
                    val pausedTasks = tasks.values.filter { it.isPaused }
                    if (pausedTasks.isNotEmpty()) {
                        updatePausedNotification(pausedTasks)
                    } else {
                        // Grace period before stopping the service completely
                        delay(2500L)
                        if (!dm.hasActiveDownloads()) {
                            Log.i(TAG, "No active running or queued downloads remaining. Stopping service.")
                            stopForegroundSafely()
                            stopSelf()
                        }
                    }
                }
            }
        }
    }

    private fun updateNotification(runningTasks: List<DownloadTask>) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateTime < 750L) return
        lastNotificationUpdateTime = now

        try {
            val notification = if (runningTasks.size == 1) {
                buildSingleTaskNotification(runningTasks.first())
            } else {
                buildMultiTaskNotification(runningTasks)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification: ${e.message}")
        }
    }

    private fun buildSingleTaskNotification(task: DownloadTask): Notification {
        val progressInt = (task.progress * 100).toInt().coerceIn(0, 100)
        val isIndeterminate = task.totalBytes <= 0L || task.progress <= 0f

        val speedText = if (task.speedBytesPerSec > 0 && !task.isPaused) " • ${TelegramDownloadManager.formatFileSize(task.speedBytesPerSec)}/s" else ""
        val sizeText = if (task.totalBytes > 0) {
            "${TelegramDownloadManager.formatFileSize(task.downloadedBytes)} / ${TelegramDownloadManager.formatFileSize(task.totalBytes)}"
        } else if (task.downloadedBytes > 0) {
            TelegramDownloadManager.formatFileSize(task.downloadedBytes)
        } else {
            task.status
        }
        val contentText = if (task.isPaused) "$sizeText • Paused" else "$sizeText$speedText"

        val actionPendingIntent = if (task.isPaused) {
            createServicePendingIntent(ACTION_RESUME, task.key, 101)
        } else {
            createServicePendingIntent(ACTION_PAUSE, task.key, 101)
        }
        val actionTitle = if (task.isPaused) "Resume" else "Pause"
        val actionIcon = if (task.isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause

        val cancelPendingIntent = createServicePendingIntent(ACTION_CANCEL, task.key, 102)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(coreDataR.drawable.ic_launcher_monochrome)
            .setContentTitle(task.title)
            .setContentText(contentText)
            .setSubText("Downloading")
            .setProgress(100, progressInt, isIndeterminate)
            .setOngoing(!task.isPaused)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(createContentIntent())
            .addAction(actionIcon, actionTitle, actionPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun buildMultiTaskNotification(tasks: List<DownloadTask>): Notification {
        val totalDownloaded = tasks.sumOf { it.downloadedBytes }
        val totalBytes = tasks.sumOf { it.totalBytes }
        val totalSpeed = tasks.sumOf { it.speedBytesPerSec }
        val avgProgress = if (totalBytes > 0) (totalDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
        val progressInt = (avgProgress * 100).toInt().coerceIn(0, 100)
        val isIndeterminate = totalBytes <= 0L

        val speedText = if (totalSpeed > 0) " • ${TelegramDownloadManager.formatFileSize(totalSpeed)}/s" else ""
        val sizeText = if (totalBytes > 0) {
            "${TelegramDownloadManager.formatFileSize(totalDownloaded)} / ${TelegramDownloadManager.formatFileSize(totalBytes)}"
        } else {
            "${tasks.size} active downloads"
        }

        val pauseAllPendingIntent = createServicePendingIntent(ACTION_PAUSE_ALL, null, 201)
        val cancelAllPendingIntent = createServicePendingIntent(ACTION_CANCEL_ALL, null, 202)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(coreDataR.drawable.ic_launcher_monochrome)
            .setContentTitle("Downloading ${tasks.size} files")
            .setContentText("$sizeText$speedText")
            .setSubText("Krypton Player")
            .setProgress(100, progressInt, isIndeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(createContentIntent())
            .addAction(android.R.drawable.ic_media_pause, "Pause All", pauseAllPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel All", cancelAllPendingIntent)
            .build()
    }

    private fun updatePausedNotification(pausedTasks: List<DownloadTask>) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateTime < 750L) return
        lastNotificationUpdateTime = now

        try {
            val notification = if (pausedTasks.size == 1) {
                buildSinglePausedNotification(pausedTasks.first())
            } else {
                buildMultiPausedNotification(pausedTasks)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update paused notification: ${e.message}")
        }
    }

    private fun buildSinglePausedNotification(task: DownloadTask): Notification {
        val progressInt = (task.progress * 100).toInt().coerceIn(0, 100)
        val isIndeterminate = task.totalBytes <= 0L || task.progress <= 0f

        val sizeText = if (task.totalBytes > 0) {
            "${TelegramDownloadManager.formatFileSize(task.downloadedBytes)} / ${TelegramDownloadManager.formatFileSize(task.totalBytes)}"
        } else if (task.downloadedBytes > 0) {
            TelegramDownloadManager.formatFileSize(task.downloadedBytes)
        } else {
            "Paused"
        }
        val contentText = "$sizeText • Paused"

        val resumePendingIntent = createServicePendingIntent(ACTION_RESUME, task.key, 103)
        val cancelPendingIntent = createServicePendingIntent(ACTION_CANCEL, task.key, 104)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(coreDataR.drawable.ic_launcher_monochrome)
            .setContentTitle(task.title)
            .setContentText(contentText)
            .setSubText("Paused")
            .setProgress(100, progressInt, isIndeterminate)
            .setOngoing(false)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(createContentIntent())
            .addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun buildMultiPausedNotification(tasks: List<DownloadTask>): Notification {
        val totalDownloaded = tasks.sumOf { it.downloadedBytes }
        val totalBytes = tasks.sumOf { it.totalBytes }
        val avgProgress = if (totalBytes > 0) (totalDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
        val progressInt = (avgProgress * 100).toInt().coerceIn(0, 100)
        val isIndeterminate = totalBytes <= 0L

        val sizeText = if (totalBytes > 0) {
            "${TelegramDownloadManager.formatFileSize(totalDownloaded)} / ${TelegramDownloadManager.formatFileSize(totalBytes)}"
        } else {
            "${tasks.size} paused downloads"
        }

        val resumeAllPendingIntent = createServicePendingIntent(ACTION_RESUME_ALL, null, 203)
        val cancelAllPendingIntent = createServicePendingIntent(ACTION_CANCEL_ALL, null, 204)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(coreDataR.drawable.ic_launcher_monochrome)
            .setContentTitle("${tasks.size} downloads paused")
            .setContentText("$sizeText • Paused")
            .setSubText("Krypton Player")
            .setProgress(100, progressInt, isIndeterminate)
            .setOngoing(false)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(createContentIntent())
            .addAction(android.R.drawable.ic_media_play, "Resume All", resumeAllPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel All", cancelAllPendingIntent)
            .build()
    }

    private fun buildInitialNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(coreDataR.drawable.ic_launcher_monochrome)
            .setContentTitle("Krypton Player Downloads")
            .setContentText("Preparing Telegram download...")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(createContentIntent())
            .build()
    }

    private fun createServicePendingIntent(actionStr: String, key: String?, requestCode: Int): PendingIntent {
        val intent = Intent(this, TelegramDownloadService::class.java).apply {
            action = actionStr
            if (key != null) putExtra(EXTRA_KEY, key)
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createContentIntent(): PendingIntent {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(
            this,
            Class.forName("dev.anilbeesetti.nextplayer.MainActivity")
        )
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Telegram Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing Telegram downloads and progress"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun acquireLocks() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "KryptonPlayer:TelegramDownloadServiceWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(6 * 60 * 60 * 1000L)
                Log.i(TAG, "Service WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire Service WakeLock: ${e.message}")
        }

        try {
            if (wifiLock == null) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                wifiLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    wifiManager?.createWifiLock(
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                        "KryptonPlayer:TelegramDownloadServiceWifiLock"
                    )
                } else {
                    wifiManager?.createWifiLock(
                        WifiManager.WIFI_MODE_FULL,
                        "KryptonPlayer:TelegramDownloadServiceWifiLock"
                    )
                }?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
                Log.i(TAG, "Service WifiLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire Service WifiLock: ${e.message}")
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(TAG, "Service WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release Service WakeLock: ${e.message}")
        }

        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                Log.i(TAG, "Service WifiLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release Service WifiLock: ${e.message}")
        }
    }

    private fun startBackgroundKeepAlive() {
        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(20000L)
                val dm = downloadManager
                if (dm != null && dm.hasActiveDownloads()) {
                    acquireLocks()
                    try {
                        if (::tdlibManager.isInitialized) {
                            tdlibManager.keepAliveForBackgroundDownload()
                            tdlibManager.optimizeNetworkForDownload()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Keep-alive ping error: ${e.message}")
                    }
                }
            }
        }
    }
}
