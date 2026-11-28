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

    private const val MAX_GITHUB_MARKDOWN_CHARS = 55_000
    private const val MAX_DESCRIPTION_CHARS = 8_000
    private const val MAX_DIAGNOSTICS_CHARS = 10_000
    private const val MAX_BREADCRUMBS_CHARS = 10_000
    private const val LOGCAT_TRUNCATION_BANNER = "... [older log lines truncated to stay within GitHub issue body limit] ..."

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
        val sanitizedCategory = LogSanitizer.sanitize(issueCategory).trim().ifBlank { "Bug Report" }
        val sanitizedDescription = LogSanitizer.sanitizeAll(userDescription).trim()
            .let { if (it.length > MAX_DESCRIPTION_CHARS) it.take(MAX_DESCRIPTION_CHARS) + "\n...(description truncated)" else it }
        val sanitizedDiagnostics = LogSanitizer.sanitizeAll(MediaDiagnosticsTracker.formatDiagnosticsMarkdown()).trim()
            .let { if (it.length > MAX_DIAGNOSTICS_CHARS) truncateTrailingLines(it, MAX_DIAGNOSTICS_CHARS) else it }
        val sanitizedBreadcrumbs = LogSanitizer.sanitizeAll(CrashBreadcrumbs.dump()).trim()
            .let { if (it.length > MAX_BREADCRUMBS_CHARS) truncateTrailingLines(it, MAX_BREADCRUMBS_CHARS) else it }
        val sanitizedLogcat = LogSanitizer.sanitizeAll(fullLogcatText).trim()

        val prefix = buildString {
            appendLine("### Device & App Specs")
            appendLine(formatDeviceSpecsMarkdown(specs))
            appendLine()

            appendLine("### Failure Reason / Issue Summary")
            appendLine("- **Category**: $sanitizedCategory")
            if (sanitizedDescription.isNotBlank()) {
                appendLine("- **Description**: $sanitizedDescription")
            } else {
                appendLine("- **Description**: Please describe what happened and steps to reproduce.")
            }
            appendLine()

            appendLine("### Media Engine Diagnostics")
            appendLine(sanitizedDiagnostics)
            appendLine()

            if (sanitizedBreadcrumbs.isNotBlank() && sanitizedBreadcrumbs != "No recent actions recorded.") {
                appendLine("### Recent User Breadcrumbs")
                appendLine("<details><summary>Click to view Breadcrumbs</summary>")
                appendLine()
                appendLine("```text")
                appendLine(sanitizedBreadcrumbs)
                appendLine("```")
                appendLine("</details>")
                appendLine()
            }

            appendLine("### System & Playback Logs (Dump)")
            appendLine("<details><summary>Click to view Dump Log</summary>")
            appendLine()
            appendLine("```text")
        }

        val suffix = "\n```\n</details>\n"
        val logBudget = (MAX_GITHUB_MARKDOWN_CHARS - prefix.length - suffix.length).coerceAtLeast(0)

        val logContent = if (sanitizedLogcat.isNotBlank()) {
            truncateTrailingLines(sanitizedLogcat, logBudget)
        } else {
            "<!-- Full dump log copied to your clipboard. Paste here if needed. -->"
        }

        val result = prefix + logContent + suffix
        return if (result.length <= MAX_GITHUB_MARKDOWN_CHARS) {
            result
        } else {
            val safeBodyLimit = (MAX_GITHUB_MARKDOWN_CHARS - suffix.length).coerceAtLeast(0)
            prefix.take(safeBodyLimit) + suffix
        }
    }

    private fun truncateTrailingLines(text: String, maxChars: Int): String {
        if (text.length <= maxChars) return text
        if (maxChars <= 0) return ""
        val bannerWithNewline = "$LOGCAT_TRUNCATION_BANNER\n"
        if (maxChars <= bannerWithNewline.length) {
            return text.takeLast(maxChars)
        }

        val budget = maxChars - bannerWithNewline.length
        val lines = text.lines()
        val selected = ArrayList<String>()
        var usedChars = 0

        for (i in lines.lastIndex downTo 0) {
            val line = lines[i]
            val addition = if (selected.isEmpty()) line.length else line.length + 1
            if (usedChars + addition > budget) {
                break
            }
            selected.add(line)
            usedChars += addition
        }

        if (selected.isEmpty() && lines.isNotEmpty()) {
            return bannerWithNewline + lines.last().takeLast(budget)
        }

        return bannerWithNewline + selected.asReversed().joinToString("\n")
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
        val sanitizedSummary = LogSanitizer.sanitizeAll(issueSummary).trim()
        val sanitizedBreadcrumbs = LogSanitizer.sanitizeAll(CrashBreadcrumbs.dump()).trim()
        val mediaDiag = MediaDiagnosticsTracker.getDiagnostics()

        val sanitizedMediaSection = LogSanitizer.sanitizeAll(
            buildString {
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
            }.trimEnd()
        )

        val sanitizedLogcatBlock = if (logcatLines.isNotEmpty()) {
            LogSanitizer.sanitizeAll(logcatLines.joinToString("\n") { it.raw })
        } else {
            "(Logcat empty or not requested)"
        }

        return buildString {
            appendLine("=================================================================")
            appendLine("             KRYPTON PLAYER COMPREHENSIVE DUMP LOG               ")
            appendLine("=================================================================")
            appendLine("Generated At: $timestamp")
            if (sanitizedSummary.isNotBlank()) {
                appendLine("Issue Summary: $sanitizedSummary")
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
            appendLine(sanitizedMediaSection)
            appendLine()

            appendLine("------------------- RECENT USER BREADCRUMBS --------------------")
            appendLine(sanitizedBreadcrumbs)
            appendLine()

            appendLine("------------------- SANITIZED SYSTEM LOGCAT ---------------------")
            appendLine(sanitizedLogcatBlock)
            appendLine("====================== END OF DUMP LOG ==========================")
        }
    }
}
