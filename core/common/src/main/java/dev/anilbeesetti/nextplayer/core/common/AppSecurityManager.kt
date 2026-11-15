package dev.anilbeesetti.nextplayer.core.common

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest

enum class UsbDebugPolicy {
    OFF,
    SOFT,
    STRICT;

    companion object {
        fun fromString(value: String?): UsbDebugPolicy {
            return when (value?.trim()?.uppercase()) {
                "OFF" -> OFF
                "SOFT" -> SOFT
                else -> STRICT // Default is STRICT for Telegram Cloud feature
            }
        }
    }
}

sealed interface SecurityVerdict {
    object Allowed : SecurityVerdict
    data class Blocked(val reason: String, val openSettingsAction: Boolean = false) : SecurityVerdict
}

object AppSecurityManager {

    @Volatile
    var currentUsbPolicy: UsbDebugPolicy = UsbDebugPolicy.STRICT

    fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun isUsbDebuggingEnabled(context: Context): Boolean {
        if (isDebuggable(context)) return false // Debug builds must stay easy to work with

        val adbEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0
        } catch (_: Exception) {
            false
        }
        val debuggerConnected = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
        return adbEnabled || debuggerConnected
    }

    fun isHookingDetected(context: Context): Boolean {
        if (isDebuggable(context)) return false

        // 1. Check default Frida server port (27042)
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", 27042), 40)
                return true
            }
        } catch (_: Exception) {}

        // 2. Check /proc/self/maps for frida or xposed libraries
        try {
            BufferedReader(FileReader("/proc/self/maps")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line?.lowercase() ?: ""
                    if (l.contains("frida") || l.contains("xposedbridge") || l.contains("gadget")) {
                        return true
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Check Xposed class via reflection
        try {
            Class.forName("de.robv.android.xposed.XposedBridge", false, context.classLoader)
            return true
        } catch (_: Exception) {}

        return false
    }

    fun isRootOrEmulatorDetected(context: Context): Boolean {
        if (isDebuggable(context)) return false

        val suPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in suPaths) {
            if (File(path).exists()) return true
        }

        val tags = Build.TAGS
        if (tags != null && tags.contains("test-keys")) return true

        val isEmulator = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.BOARD.contains("goldfish")

        return isEmulator
    }

    fun getSigningCertificateSha256(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val cert = signatures?.firstOrNull()?.toByteArray() ?: return ""
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(cert)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Checks security requirements for the Telegram Cloud feature.
     * Only executed in RELEASE builds.
     */
    fun verifyTelegramCloudAccess(context: Context): SecurityVerdict {
        if (isDebuggable(context)) return SecurityVerdict.Allowed

        // 1. Tamper & Hooking detection (Frida, Xposed, attached debugger)
        if (isHookingDetected(context) || Debug.isDebuggerConnected()) {
            return SecurityVerdict.Blocked(
                reason = "Tampering or dynamic instrumentation detected. Telegram Cloud cannot run in this environment."
            )
        }

        // 2. USB Debugging Policy
        when (currentUsbPolicy) {
            UsbDebugPolicy.OFF -> { /* Allowed */ }
            UsbDebugPolicy.SOFT -> {
                // Soft signal: do not block user, only log/report
            }
            UsbDebugPolicy.STRICT -> {
                if (isUsbDebuggingEnabled(context)) {
                    return SecurityVerdict.Blocked(
                        reason = "Turn off USB debugging to use this feature",
                        openSettingsAction = true
                    )
                }
            }
        }

        return SecurityVerdict.Allowed
    }
}
