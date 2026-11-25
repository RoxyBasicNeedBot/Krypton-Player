package dev.roxy.kryptonplayer.feature.player.mpv

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object VulkanCapabilities {
    private const val VULKAN_1_1_VERSION = 0x401000

    fun isVulkanSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val hardware = "${Build.HARDWARE} ${Build.MODEL} ${Build.BOARD}".lowercase()
        if (hardware.contains("goldfish") || hardware.contains("ranchu") || hardware.contains("emulator")) {
            return false
        }
        return runCatching {
            val pm = context.packageManager
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, VULKAN_1_1_VERSION) &&
                pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 1)
        }.getOrDefault(false)
    }
}
