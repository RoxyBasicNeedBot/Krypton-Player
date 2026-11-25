package dev.roxy.kryptonplayer.feature.player.mpv

import android.content.Context
import android.os.Build
import android.os.PowerManager

object ThermalMonitor {
    private const val HEADROOM_LOOKAHEAD_SECONDS = 10
    private const val ANIME4K_THROTTLE_THRESHOLD = 0.85f

    fun getHeadroom(context: Context): Float {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return Float.NaN
        return runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.getThermalHeadroom(HEADROOM_LOOKAHEAD_SECONDS) ?: Float.NaN
        }.getOrDefault(Float.NaN)
    }

    fun shouldThrottleAnime4K(headroom: Float): Boolean =
        headroom.isFinite() && headroom >= ANIME4K_THROTTLE_THRESHOLD
}
