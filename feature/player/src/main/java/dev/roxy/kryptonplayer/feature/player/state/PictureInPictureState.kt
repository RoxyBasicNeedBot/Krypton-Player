package dev.roxy.kryptonplayer.feature.player.state

import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import dev.roxy.kryptonplayer.core.common.extensions.isPipFeatureSupported
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.ui.R as coreUiR
import dev.roxy.kryptonplayer.feature.player.extensions.seekBack
import dev.roxy.kryptonplayer.feature.player.extensions.seekForward
import dev.roxy.kryptonplayer.feature.player.mpv.PlaybackSession

@Composable
fun rememberPictureInPictureState(
    player: PlaybackController,
    autoEnter: Boolean = true,
): PictureInPictureState {
    val activity = LocalActivity.current as ComponentActivity
    val pipState = remember(activity, player, autoEnter) {
        PictureInPictureState(
            activity = activity,
            player = player,
            autoEnter = autoEnter,
        )
    }
    DisposableEffect(pipState, activity) {
        pipState.registerReceiver()
        onDispose { pipState.unregisterReceiver() }
    }
    LaunchedEffect(pipState) { pipState.observe() }
    return pipState
}

class PictureInPictureState(
    private val activity: ComponentActivity,
    private val player: PlaybackController,
    private val autoEnter: Boolean = true,
) {
    val isPipSupported: Boolean get() = activity.isPipFeatureSupported

    var isInPipMode: Boolean by mutableStateOf(false)
        private set

    val isInPictureInPictureMode: Boolean get() = isInPipMode

    private var videoRect: Rect? = null
    private var receiverRegistered = false

    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_PIP_CONTROL) return
            when (intent.getIntExtra(EXTRA_CONTROL_TYPE, -1)) {
                CONTROL_TYPE_PLAY -> player.play()
                CONTROL_TYPE_PAUSE -> player.pause()
                CONTROL_TYPE_NEXT -> player.seekToNext()
                CONTROL_TYPE_PREVIOUS -> player.seekToPrevious()
                CONTROL_TYPE_REWIND -> player.seekBack()
                CONTROL_TYPE_FORWARD -> player.seekForward()
            }
        }
    }

    private val onPipModeChangedListener = Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
        isInPipMode = info.isInPictureInPictureMode
        if (!info.isInPictureInPictureMode && player.state.value.isPlaying) {
            PlaybackSession.setPropertyString("vid", "auto")
        }
    }

    fun registerReceiver() {
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                activity,
                pipActionReceiver,
                IntentFilter(ACTION_PIP_CONTROL),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            activity.addOnPictureInPictureModeChangedListener(onPipModeChangedListener)
            receiverRegistered = true
        }
    }

    fun unregisterReceiver() {
        if (receiverRegistered) {
            runCatching { activity.unregisterReceiver(pipActionReceiver) }
            runCatching { activity.removeOnPictureInPictureModeChangedListener(onPipModeChangedListener) }
            receiverRegistered = false
        }
    }

    suspend fun observe() {
        player.state.collect { snapshot ->
            updatePictureInPictureParams(snapshot.isPlaying, snapshot.videoWidth, snapshot.videoHeight)
        }
    }

    fun setVideoViewRect(rect: Rect) {
        videoRect = rect
        val snap = player.state.value
        updatePictureInPictureParams(snap.isPlaying, snap.videoWidth, snap.videoHeight)
    }

    fun enterPictureInPictureMode(): Boolean = openPictureInPicture()

    fun openPictureInPictureSettings() {
        val packageUri = Uri.fromParts("package", activity.packageName, null)
        val pipIntent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", packageUri)
        if (runCatching { activity.startActivity(pipIntent) }.isFailure) {
            runCatching {
                activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
            }
        }
    }

    fun openPictureInPicture(): Boolean {
        if (!isPipSupported || !hasPipPermission()) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val snap = player.state.value
            val params = buildPipParams(snap.isPlaying, snap.videoWidth, snap.videoHeight)
            if (params != null) {
                activity.enterPictureInPictureMode(params)
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    fun hasPipPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appOps = activity.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), activity.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), activity.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun updatePictureInPictureParams(isPlaying: Boolean, width: Int, height: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupported && !activity.isFinishing) {
            buildPipParams(isPlaying, width, height)?.let { params ->
                runCatching { activity.setPictureInPictureParams(params) }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(isPlaying: Boolean, width: Int, height: Int): PictureInPictureParams? {
        if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return null
        val builder = PictureInPictureParams.Builder()
        if (width > 0 && height > 0) {
            val ratio = (width.toFloat() / height.toFloat()).coerceIn(0.45f, 2.2f)
            builder.setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
        }
        videoRect?.let { builder.setSourceRectHint(it) }
        builder.setActions(buildRemoteActions(isPlaying))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter && isPlaying)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildRemoteActions(isPlaying: Boolean): List<RemoteAction> {
        val snap = player.state.value
        val actions = mutableListOf<RemoteAction>()
        if (snap.hasPrevious) {
            actions += createRemoteAction(coreUiR.drawable.ic_skip_prev, coreUiR.string.player_controls_previous, CONTROL_TYPE_PREVIOUS, 10)
        } else {
            actions += createRemoteAction(coreUiR.drawable.ic_skip_prev, coreUiR.string.player_controls_previous, CONTROL_TYPE_REWIND, 11)
        }
        if (isPlaying) {
            actions += createRemoteAction(coreUiR.drawable.ic_pause, coreUiR.string.play_pause, CONTROL_TYPE_PAUSE, 12)
        } else {
            actions += createRemoteAction(coreUiR.drawable.ic_play, coreUiR.string.play_pause, CONTROL_TYPE_PLAY, 13)
        }
        if (snap.hasNext) {
            actions += createRemoteAction(coreUiR.drawable.ic_skip_next, coreUiR.string.player_controls_next, CONTROL_TYPE_NEXT, 14)
        } else {
            actions += createRemoteAction(coreUiR.drawable.ic_skip_next, coreUiR.string.player_controls_next, CONTROL_TYPE_FORWARD, 15)
        }
        return actions
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        @DrawableRes iconResId: Int,
        @StringRes titleResId: Int,
        controlType: Int,
        requestCode: Int,
    ): RemoteAction {
        val title = activity.getString(titleResId)
        val pendingIntent = PendingIntent.getBroadcast(
            activity,
            requestCode,
            Intent(ACTION_PIP_CONTROL).setPackage(activity.packageName).putExtra(EXTRA_CONTROL_TYPE, controlType),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return RemoteAction(Icon.createWithResource(activity, iconResId), title, title, pendingIntent)
    }

    companion object {
        private const val ACTION_PIP_CONTROL = "dev.roxy.kryptonplayer.pip.CONTROL"
        private const val EXTRA_CONTROL_TYPE = "control_type"
        private const val CONTROL_TYPE_PLAY = 1
        private const val CONTROL_TYPE_PAUSE = 2
        private const val CONTROL_TYPE_NEXT = 3
        private const val CONTROL_TYPE_PREVIOUS = 4
        private const val CONTROL_TYPE_REWIND = 5
        private const val CONTROL_TYPE_FORWARD = 6
    }
}
