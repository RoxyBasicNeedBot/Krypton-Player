package dev.roxy.kryptonplayer.core.data.reporting

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import dev.roxy.kryptonplayer.core.common.CrashBreadcrumbs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceSpecs(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val androidVersion: String,
    val sdkInt: Int,
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val supportedAbis: List<String>,
    val freeRamMb: Long,
    val totalRamMb: Long,
    val maxHeapMb: Long,
    val freeInternalStorageMb: Long,
)

/**
 * Central aggregator that collects device specifications, media engine diagnostics,
 * user breadcrumbs, and sanitized logcat into unified reports and GitHub issue templates.
 */
object AppLogCollector {

    fun collectDeviceSpecs(context: Context): DeviceSpecs {
        val pm = context.packageManager
        val pkgName = context.packageName
        val (verName, verCode) = try {
            val pInfo = pm.getPackageInfo(pkgName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                pInfo.versionCode.toLong()
            }
            Pair(pInfo.versionName ?: "1.0.0", code)
        } catch (_: Exception) {
            Pair("1.0.0", 1L)
        }

        val runtime = Runtime.getRuntime()
        val maxHeapMb = runtime.maxMemory() / 1024 / 1024

        var totalRamMb = runtime.totalMemory() / 1024 / 1024
        var freeRamMb = runtime.freeMemory() / 1024 / 1024

        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                totalRamMb = memInfo.totalMem / 1024 / 1024
                freeRamMb = memInfo.availMem / 1024 / 1024
            }
        } catch (_: Exception) {}

        val freeStorageMb = try {
            val stat = StatFs(context.filesDir.absolutePath)
            (stat.availableBlocksLong * stat.blockSizeLong) / 1024 / 1024
        } catch (_: Exception) {
            -1L
        }

        val abis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.toList()
        } else {
            @Suppress("DEPRECATION")
            listOf(Build.CPU_ABI, Build.CPU_ABI2).filter { it.isNotBlank() }
        }

        return DeviceSpecs(
            appName = "Krypton Player",
            packageName = pkgName,
            versionName = verName,
            versionCode = verCode,
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            brand = Build.BRAND,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            supportedAbis = abis,
            freeRamMb = freeRamMb,
            totalRamMb = totalRamMb,
            maxHeapMb = maxHeapMb,
            freeInternalStorageMb = freeStorageMb,
        )
    }

    fun formatDeviceSpecsMarkdown(specs: DeviceSpecs): String {
        return buildString {
            appendLine("- **App Version**: ${specs.versionName} (${specs.versionCode})")
            appendLine("- **Package**: ${specs.packageName}")
            appendLine("- **Android Version**: Android ${specs.androidVersion} (API ${specs.sdkInt})")
            appendLine("- **Device**: ${specs.manufacturer} ${specs.model} (${specs.brand} / ${specs.device})")
            appendLine("- **CPU ABI**: ${specs.supportedAbis.joinToString(", ")}")
            appendLine("- **RAM**: ${specs.freeRamMb}MB available / ${specs.totalRamMb}MB total (Max Heap: ${specs.maxHeapMb}MB)")
            if (specs.freeInternalStorageMb > 0) {
                appendLine("- **Free Internal Storage**: ${specs.freeInternalStorageMb}MB")
            }
        }.trimEnd()
    }

    /**
     * Generates a fully formatted GitHub Issue Markdown body compliant with project specs.
     *
     * @param context Android context
     * @param issueTitle Summary / headline of the issue
     * @param issueCategory Classification (Playback Error, Crash, UI/Controls, Network, Audio/Subtitle, Other)
     * @param userDescription User-provided description or steps to reproduce
     * @param fullLogcatText Sanitized logcat content to place in the collapsible section
     */
    fun generateGitHubIssueMarkdown(
        context: Context,
        issueCategory: String,
        userDescription: String = "",
        fullLogcatText: String = "",
    ): String {
        val specs = collectDeviceSpecs(context)
        val mediaDiagnostics = MediaDiagnosticsTracker.formatDiagnosticsMarkdown()
        val breadcrumbs = CrashBreadcrumbs.dump().trim()

        return buildString {
            appendLine("### Device & App Specs")
            appendLine(formatDeviceSpecsMarkdown(specs))
            appendLine()

            appendLine("### Failure Reason / Issue Summary")
            appendLine("- **Category**: $issueCategory")
            if (userDescription.isNotBlank()) {
                appendLine("- **Description**: $userDescription")
            } else {
                appendLine("- **Description**: Please describe what happened and steps to reproduce.")
            }
            appendLine()

            appendLine("### Media Engine Diagnostics")
            appendLine(mediaDiagnostics)
            appendLine()

            if (breadcrumbs.isNotBlank() && breadcrumbs != "No recent actions recorded.") {
                appendLine("### Recent User Breadcrumbs")
                appendLine("<details><summary>Click to view Breadcrumbs</summary>")
                appendLine()
                appendLine("```text")
                appendLine(breadcrumbs)
                appendLine("```")
                appendLine("</details>")
                appendLine()
            }

            appendLine("### System & Playback Logs (Dump)")
            appendLine("<details><summary>Click to view Dump Log</summary>")
            appendLine()
            appendLine("```text")
            if (fullLogcatText.isNotBlank()) {
                appendLine(fullLogcatText.trim())
            } else {
                appendLine("<!-- Full dump log copied to your clipboard. Paste here if needed. -->")
            }
            appendLine("```")
            appendLine("</details>")
        }
    }

    /**
     * Generates a comprehensive full plain-text dump for export to file or raw sharing.
     */
    fun generateFullPlainTextDump(
        context: Context,
        issueSummary: String = "",
        logcatLines: List<LogcatEntry> = emptyList(),
    ): String {
        val specs = collectDeviceSpecs(context)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val breadcrumbs = CrashBreadcrumbs.dump()
        val mediaDiag = MediaDiagnosticsTracker.getDiagnostics()

        return buildString {
            appendLine("=================================================================")
            appendLine("             KRYPTON PLAYER COMPREHENSIVE DUMP LOG               ")
            appendLine("=================================================================")
            appendLine("Generated At: $timestamp")
            if (issueSummary.isNotBlank()) {
                appendLine("Issue Summary: $issueSummary")
            }
            appendLine()

            appendLine("----------------- DEVICE & APP SPECIFICATIONS -------------------")
            appendLine("App Name: ${specs.appName}")
            appendLine("Package: ${specs.packageName}")
            appendLine("Version: ${specs.versionName} (${specs.versionCode})")
            appendLine("Android OS: Android ${specs.androidVersion} (API ${specs.sdkInt})")
            appendLine("Device: ${specs.manufacturer} ${specs.model} (${specs.brand} / ${specs.device})")
            appendLine("Supported ABIs: ${specs.supportedAbis.joinToString(", ")}")
            appendLine("RAM: ${specs.freeRamMb}MB free / ${specs.totalRamMb}MB total (Max Heap: ${specs.maxHeapMb}MB)")
            appendLine("Free Internal Storage: ${specs.freeInternalStorageMb}MB")
            appendLine()

            appendLine("------------------- MEDIA ENGINE DIAGNOSTICS --------------------")
            appendLine("Active Media: ${mediaDiag.title}")
            appendLine("URI: ${mediaDiag.uriString}")
            appendLine("URI Scheme: ${mediaDiag.uriScheme}")
            appendLine("Is Local Source: ${mediaDiag.isLocalSource}")
            appendLine("Local File Size: ${mediaDiag.localFileSizeFormatted}")
            appendLine("MIME Type: ${mediaDiag.mimeType}")
            appendLine("Playback State: ${mediaDiag.playbackState} (isPlaying=${mediaDiag.isPlaying})")
            appendLine("Position / Duration: ${mediaDiag.positionMs / 1000}s / ${mediaDiag.durationMs / 1000}s")
            appendLine("Video Decoder: ${mediaDiag.videoCodec} (${mediaDiag.videoResolution} @ ${mediaDiag.frameRate}fps)")
            appendLine("Audio Output: ${mediaDiag.audioCodec} (${mediaDiag.audioChannels} channels, ${mediaDiag.audioSampleRate}Hz)")
            if (mediaDiag.lastErrorCodeName.isNotBlank() || mediaDiag.lastErrorMessage.isNotBlank()) {
                appendLine("Last Error Code: ${mediaDiag.lastErrorCodeName} (${mediaDiag.lastErrorCode})")
                appendLine("Last Error Message: ${mediaDiag.lastErrorMessage}")
                appendLine("HTTP Response Code: ${mediaDiag.httpResponseCode}")
                appendLine("Root Cause: ${mediaDiag.lastErrorCause}")
            }
            if (mediaDiag.recentEvents.isNotEmpty()) {
                appendLine("Media Events Timeline:")
                mediaDiag.recentEvents.forEach { appendLine("  $it") }
            }
            appendLine()

            appendLine("------------------- RECENT USER BREADCRUMBS --------------------")
            appendLine(breadcrumbs.trim())
            appendLine()

            appendLine("------------------- SANITIZED SYSTEM LOGCAT ---------------------")
            if (logcatLines.isNotEmpty()) {
                for (entry in logcatLines) {
                    appendLine(entry.raw)
                }
            } else {
                appendLine("(Logcat empty or not requested)")
            }
            appendLine("====================== END OF DUMP LOG ==========================")
        }
    }
}
