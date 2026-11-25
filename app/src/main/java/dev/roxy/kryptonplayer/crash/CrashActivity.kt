package dev.roxy.kryptonplayer.crash

import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import dev.roxy.kryptonplayer.BuildConfig
import dev.roxy.kryptonplayer.MainActivity
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Attempt recovery from intent extras first, fallback to persistent CrashStorage
        val cachedCrash = CrashStorage.getLatestCrash(this)

        val exceptionString = intent.getStringExtra("exception")
            ?: cachedCrash?.stackTrace
            ?: ""
        val threadName = intent.getStringExtra("thread_name")
            ?: cachedCrash?.threadName
            ?: "unknown"
        val isAnr = intent.getBooleanExtra("is_anr", false) || (cachedCrash?.isAnr == true)
        val exceptionType = intent.getStringExtra("exception_type")
            ?: cachedCrash?.exceptionType
            ?: if (isAnr) "ApplicationNotResponding (ANR)" else "Exception"
        val exceptionMessage = intent.getStringExtra("exception_message")
            ?: cachedCrash?.exceptionMessage
            ?: if (isAnr) "Main UI thread was frozen for > 4000ms ('Close app / Wait')" else "No error message provided"
        val exceptionCause = intent.getStringExtra("exception_cause")
            ?: cachedCrash?.exceptionCause
            ?: "None"

        val analysis = if (cachedCrash != null && cachedCrash.culpritClass.isNotBlank() && cachedCrash.culpritClass != "UnknownClass") {
            CrashAnalysis(
                category = if (isAnr) "Main UI Thread Disk I/O (ANR)" else cachedCrash.exceptionType,
                culpritClass = cachedCrash.culpritClass,
                culpritMethod = cachedCrash.culpritMethod,
                culpritFile = cachedCrash.culpritFile,
                culpritLine = cachedCrash.culpritLine,
                culpritOperation = cachedCrash.culpritOperation,
                affectedSubsystem = cachedCrash.affectedSubsystem,
                plainDiagnosis = cachedCrash.plainDiagnosis,
                recommendation = cachedCrash.recommendation,
                appFrames = cachedCrash.appFrames,
                isObfuscatedFrame = cachedCrash.isObfuscatedFrame,
                bytecodeDetails = cachedCrash.bytecodeDetails,
                composeContext = cachedCrash.composeContext,
                triggerAction = cachedCrash.triggerAction,
                failureOrigin = cachedCrash.failureOrigin,
                reasonHindi = cachedCrash.reasonHindi,
                fixHindi = cachedCrash.fixHindi,
            )
        } else {
            CrashAnalyzer.analyze(null, exceptionString, isAnr)
        }

        val breadcrumbs = cachedCrash?.breadcrumbs?.takeIf { it.isNotBlank() } ?: CrashBreadcrumbs.dump()

        var logcat by mutableStateOf("")

        lifecycleScope.launch {
            logcat = collectLogcat()
        }

        setContent {
            val isDark = true

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { isDark },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { isDark },
                    ),
                )
            }

            KryptonPlayerTheme(
                darkTheme = isDark,
                dynamicColor = true,
            ) {
                val clipboard = LocalClipboard.current
                CrashScreen(
                    exceptionString = exceptionString,
                    threadName = threadName,
                    exceptionType = exceptionType,
                    exceptionMessage = exceptionMessage,
                    exceptionCause = exceptionCause,
                    isAnr = isAnr,
                    analysis = analysis,
                    breadcrumbs = breadcrumbs,
                    logcat = logcat,
                    onCopyCulpritClick = {
                        val lineDisplay = if (analysis.culpritLine > 0) ":${analysis.culpritLine}" else ""
                        val culpritSummary = "${analysis.culpritFile}$lineDisplay (${analysis.culpritClass}.${analysis.culpritMethod})"
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText("Culprit Code", culpritSummary)
                        )
                        Toast.makeText(this@CrashActivity, "Copied: $culpritSummary", Toast.LENGTH_SHORT).show()
                    },
                    onCopyKeepRuleClick = {
                        val rule = analysis.bytecodeDetails.suggestedKeepRule
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText("ProGuard Keep Rule", rule)
                        )
                        Toast.makeText(this@CrashActivity, "ProGuard rule copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onCopyFixClick = {
                        val fixText = analysis.fixHindi.ifBlank { analysis.recommendation }
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText("Crash Fix", fixText)
                        )
                        Toast.makeText(this@CrashActivity, "Fix copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onShareLogsClick = {
                        lifecycleScope.launch {
                            shareLogs(
                                deviceInfo = collectDeviceInfo(),
                                exceptionString = exceptionString,
                                threadName = threadName,
                                exceptionType = exceptionType,
                                exceptionMessage = exceptionMessage,
                                exceptionCause = exceptionCause,
                                isAnr = isAnr,
                                analysis = analysis,
                                breadcrumbs = breadcrumbs,
                                logcat = logcat,
                            )
                        }
                    },
                    onCopyLogsClick = {
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                null,
                                concatLogs(
                                    deviceInfo = collectDeviceInfo(),
                                    exceptionString = exceptionString,
                                    threadName = threadName,
                                    exceptionType = exceptionType,
                                    exceptionMessage = exceptionMessage,
                                    exceptionCause = exceptionCause,
                                    isAnr = isAnr,
                                    analysis = analysis,
                                    breadcrumbs = breadcrumbs,
                                    logcat = logcat,
                                ),
                            ),
                        )
                        Toast.makeText(this@CrashActivity, "Full crash report copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onRestartClick = {
                        CrashStorage.clearLatestCrash(this@CrashActivity)
                        finish()
                        val restartIntent = Intent(this@CrashActivity, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(restartIntent)
                        android.os.Process.killProcess(android.os.Process.myPid())
                    },
                )
            }
        }
    }

    private suspend fun shareLogs(
        deviceInfo: String,
        exceptionString: String,
        threadName: String,
        exceptionType: String,
        exceptionMessage: String,
        exceptionCause: String,
        isAnr: Boolean,
        analysis: CrashAnalysis,
        breadcrumbs: String,
        logcat: String,
    ) = withContext(Dispatchers.IO) {
        val file = File(cacheDir, "kryptonplayer_logs.txt").also {
            if (it.exists()) it.delete()
            it.createNewFile()
        }
        val logs = concatLogs(
            deviceInfo = deviceInfo,
            exceptionString = exceptionString,
            threadName = threadName,
            exceptionType = exceptionType,
            exceptionMessage = exceptionMessage,
            exceptionCause = exceptionCause,
            isAnr = isAnr,
            analysis = analysis,
            breadcrumbs = breadcrumbs,
            logcat = logcat,
        )
        file.writeText(text = logs)
        val uri = FileProvider.getUriForFile(
            this@CrashActivity,
            "$packageName.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            clipData = ClipData.newRawUri(null, uri)
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        startActivity(
            Intent.createChooser(intent, getString(R.string.crash_screen_share)),
        )
    }

    private fun concatLogs(
        deviceInfo: String,
        exceptionString: String,
        threadName: String,
        exceptionType: String,
        exceptionMessage: String,
        exceptionCause: String,
        isAnr: Boolean,
        analysis: CrashAnalysis,
        breadcrumbs: String,
        logcat: String,
    ): String {
        return StringBuilder().apply {
            appendLine("=== KRYPTON PLAYER ADVANCED CRASH & ANR REPORT ===")
            appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine()
            appendLine("=== ROOT CAUSE & EXACT CULPRIT CODE ===")
            val lineDisplay = if (analysis.culpritLine > 0) ":${analysis.culpritLine}" else ""
            val obfuscatedTag = if (analysis.isObfuscatedFrame) " [R8 Minified Frame]" else ""
            appendLine("Culprit: ${analysis.culpritFile}$lineDisplay (${analysis.culpritClass}.${analysis.culpritMethod})$obfuscatedTag")
            appendLine("Operation: ${analysis.culpritOperation}")
            appendLine("Subsystem: ${analysis.affectedSubsystem}")
            appendLine("Diagnosis: ${analysis.plainDiagnosis}")
            appendLine("Recommendation: ${analysis.recommendation}")
            if (analysis.appFrames.isNotEmpty()) {
                appendLine("App Call Stack:")
                analysis.appFrames.forEach { appendLine("  -> $it") }
            }
            appendLine()

            if (analysis.bytecodeDetails.isBytecodeIssue) {
                appendLine("=== BYTECODE & R8 DEVIRTUALIZATION DIAGNOSIS ===")
                appendLine("Issue Type: ${analysis.bytecodeDetails.failureType}")
                appendLine("Missing Symbol: ${analysis.bytecodeDetails.missingSymbol}")
                appendLine("Receiver: ${analysis.bytecodeDetails.receiverType}")
                appendLine("Technical Cause: ${analysis.bytecodeDetails.technicalCause}")
                appendLine("Suggested ProGuard / R8 Rule:")
                appendLine(analysis.bytecodeDetails.suggestedKeepRule)
                appendLine("Recommended Action Steps:")
                analysis.bytecodeDetails.solutionSteps.forEach { appendLine("  • $it") }
                appendLine()
            }

            if (analysis.composeContext.isComposeIssue) {
                appendLine("=== JETPACK COMPOSE LIFECYCLE CONTEXT ===")
                appendLine("Lifecycle Phase: ${analysis.composeContext.lifecyclePhase}")
                appendLine("Probable Screen: ${analysis.composeContext.probableScreen}")
                appendLine("Subsystem: ${analysis.composeContext.failingSubsystem}")
                appendLine("Technical Context: ${analysis.composeContext.technicalExplanation}")
                appendLine("Developer Guidance: ${analysis.composeContext.contextAdvice}")
                appendLine()
            }

            if (breadcrumbs.isNotBlank()) {
                appendLine("=== RECENT USER ACTIONS (BREADCRUMBS) ===")
                appendLine(breadcrumbs.trim())
                appendLine()
            }
            appendLine("=== DEVICE INFO ===")
            appendLine(deviceInfo)
            appendLine()
            appendLine("=== CRASH DETAILS ===")
            appendLine("Exception Type: $exceptionType")
            appendLine("Is ANR: $isAnr")
            appendLine("Message: $exceptionMessage")
            appendLine("Thread: $threadName")
            appendLine("Cause: $exceptionCause")
            appendLine()
            if (exceptionString.isNotBlank()) {
                appendLine("=== STACK TRACE / THREAD DUMP ===")
                appendLine(exceptionString)
                appendLine()
            }
            appendLine("=== SYSTEM LOGCAT ===")
            appendLine(logcat)
        }.toString()
    }

    private suspend fun collectLogcat(): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime()
            val reader = BufferedReader(InputStreamReader(process.exec("logcat -d").inputStream))
            val logcat = StringBuilder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                reader.lines().limit(1000).forEach(logcat::appendLine)
            } else {
                reader.readLines().takeLast(1000).forEach(logcat::appendLine)
            }
            logcat.toString()
        } catch (e: Exception) {
            "Unable to collect logcat: ${e.message}"
        }
    }

    private fun collectDeviceInfo(): String {
        val runtime = Runtime.getRuntime()
        val freeMb = runtime.freeMemory() / 1024 / 1024
        val totalMb = runtime.totalMemory() / 1024 / 1024
        val maxMb = runtime.maxMemory() / 1024 / 1024
        return """
            App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
            Android version: ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})
            Device brand: ${Build.BRAND}
            Device manufacturer: ${Build.MANUFACTURER}
            Device model: ${Build.MODEL} (${Build.DEVICE})
            Memory: ${freeMb}MB free / ${totalMb}MB total (max ${maxMb}MB)
        """.trimIndent()
    }
}

@Composable
private fun CrashTriggerBanner(
    triggerAction: String,
    probableScreen: String,
) {
    val displayTrigger = triggerAction.ifBlank { "User Action / Screen Transition" }
    val isLink = displayTrigger.contains("Link", ignoreCase = true)
    val isTelegram = displayTrigger.contains("Telegram", ignoreCase = true)
    val isSearch = displayTrigger.contains("Search", ignoreCase = true)
    val isReels = displayTrigger.contains("Reel", ignoreCase = true)

    val iconVector = when {
        isLink -> NextIcons.Link
        isTelegram -> NextIcons.Cloud
        isSearch -> NextIcons.Search
        isReels -> NextIcons.Movie
        else -> NextIcons.Priority
    }

    val containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.90f)
    val contentColor = MaterialTheme.colorScheme.onErrorContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(
                BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.primary,
                        )
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(26.dp),
            )
            Column {
                Text(
                    text = "🚨 Crash Trigger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                )
                Text(
                    text = "Action that provoked this crash:",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f),
                )
            }
        }

        // Highlight Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "⚡ Action:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            SelectionContainer {
                Text(
                    text = displayTrigger,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        val explanation = when {
            isLink -> "Triggered while opening the Network Stream dialog from Top Bar or Menu."
            isTelegram -> "Triggered while opening Telegram Cloud from Top Bar or Navigation."
            isSearch -> "Triggered while opening Search from Top Bar or Navigation."
            isReels -> "Triggered while opening Reels player from FAB or Action button."
            else -> "Triggered during action on screen '${probableScreen.ifBlank { "Home / MediaPicker" }}'."
        }

        Text(
            text = explanation,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )
    }
}

@Composable
private fun RootCauseAndFixCard(
    analysis: CrashAnalysis,
    onCopyFix: () -> Unit = {},
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = NextIcons.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Root Cause & Recommended Fix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                )
            }
        }

        // 1. Failure Origin Badge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "🏷️ Failure Origin:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = analysis.failureOrigin.ifBlank { "App Runtime / Minification" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            val lineDisplay = if (analysis.culpritLine > 0) ":${analysis.culpritLine}" else ""
            Text(
                text = "📍 Location: ${analysis.culpritFile}$lineDisplay (${analysis.culpritClass}.${analysis.culpritMethod})",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 2. Detailed Root Cause
        if (analysis.reasonHindi.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SelectionContainer {
                    Text(
                        text = analysis.reasonHindi,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // 3. Actionable Solution
        if (analysis.fixHindi.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "🛠️ Actionable Solution:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    OutlinedButton(
                        onClick = onCopyFix,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = NextIcons.Copy,
                            contentDescription = "Copy Fix",
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "Copy Fix",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                SelectionContainer {
                    Text(
                        text = analysis.fixHindi,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun CrashHistoryListView(
    crashes: List<CrashHistoryItem>,
    onSelectCrash: (CrashHistoryItem) -> Unit,
    onCopyCrash: (CrashHistoryItem) -> Unit,
    onDeleteCrash: (CrashHistoryItem) -> Unit,
    onClearAll: () -> Unit,
) {
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    val filterOptions = listOf(
        "All" to "All (${crashes.size})",
        "Link" to "🔗 Link",
        "Telegram" to "✈️ Telegram",
        "Search" to "🔍 Search",
        "Reels" to "🎬 Reels",
        "R8" to "⚙️ ProGuard/R8",
        "Compose" to "🎨 Compose",
        "ANR" to "⏳ ANR",
    )

    val filteredList = remember(crashes, selectedFilter) {
        when (selectedFilter) {
            "Link" -> crashes.filter { it.triggerAction.contains("Link", ignoreCase = true) || it.fullLog.contains("UrlDialog") }
            "Telegram" -> crashes.filter { it.triggerAction.contains("Telegram", ignoreCase = true) || it.fullLog.contains("Telegram") }
            "Search" -> crashes.filter { it.triggerAction.contains("Search", ignoreCase = true) || it.fullLog.contains("Search") }
            "Reels" -> crashes.filter { it.triggerAction.contains("Reel", ignoreCase = true) }
            "R8" -> crashes.filter { it.isBytecodeIssue || it.culpritFile == "SourceFile" || it.failureOrigin.contains("ProGuard", ignoreCase = true) }
            "Compose" -> crashes.filter { it.isComposeIssue || it.failureOrigin.contains("Compose", ignoreCase = true) }
            "ANR" -> crashes.filter { it.isAnr }
            else -> crashes
        }
    }

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Crashes?") },
            text = { Text("Are you sure you want to delete all saved crash logs?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAll()
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${crashes.size} Crash Log(s) Recorded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (crashes.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showClearConfirmDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = NextIcons.DeleteSweep,
                        contentDescription = "Clear History",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((key, label) in filterOptions) {
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (filteredList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = NextIcons.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = "No crashes found for this filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "No matching crash logs in history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (item in filteredList) {
                    CrashHistoryCard(
                        item = item,
                        onViewDetails = { onSelectCrash(item) },
                        onCopy = { onCopyCrash(item) },
                        onDelete = { onDeleteCrash(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CrashHistoryCard(
    item: CrashHistoryItem,
    onViewDetails: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val formattedDate = remember(item.timestamp) {
        SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))
    }

    val cardColor = if (item.isAnr) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceContainer
    val isR8 = item.isBytecodeIssue || item.culpritFile == "SourceFile"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .border(
                1.dp,
                if (isR8) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Row 1: Date + Failure Origin Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🕒 $formattedDate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = item.failureOrigin.take(35),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isR8) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }

        // Row 2: Trigger Banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = NextIcons.Priority,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Trigger: ${item.triggerAction}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Row 3: Culprit Location (File & Line)
        val lineDisplay = if (item.culpritLine > 0) ":${item.culpritLine}" else ""
        Text(
            text = "📍 ${item.culpritFile}$lineDisplay (${item.culpritClass}.${item.culpritMethod})",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Row 4: Exception Type & Message
        Text(
            text = "${item.exceptionType}: ${item.exceptionMessage.take(100)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // Row 5: Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onViewDetails,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "View Details",
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            OutlinedButton(
                onClick = onCopy,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Icon(
                    imageVector = NextIcons.Copy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(text = "Copy", style = MaterialTheme.typography.labelSmall)
            }

            FilledIconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = NextIcons.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun CrashDiagnosticsCard(
    exceptionType: String,
    exceptionMessage: String,
    threadName: String,
    exceptionCause: String,
    isAnr: Boolean = false,
) {
    val cardColor = MaterialTheme.colorScheme.errorContainer
    val contentColor = MaterialTheme.colorScheme.onErrorContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = NextIcons.Priority,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (isAnr) "Application Not Responding (ANR)" else "Crash Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Error: $exceptionType",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )

        Text(
            text = "Message: $exceptionMessage",
            style = MaterialTheme.typography.bodySmall,
            color = contentColor
        )

        Text(
            text = "Thread: $threadName",
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.8f)
        )

        if (exceptionCause != "None") {
            Text(
                text = "Cause: $exceptionCause",
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        val suggestion = when {
            isAnr || exceptionType.contains("ANR", ignoreCase = true) -> {
                "Application Not Responding (ANR): The main UI thread was frozen for > 4000ms. This usually happens when switching tabs or loading media while heavy tasks (like file checks or database calls) run on the main thread. We have captured the exact line blocking the UI in the stack trace below."
            }
            exceptionType.contains("OutOfMemoryError", ignoreCase = true) || exceptionMessage.contains("OOM", ignoreCase = true) -> {
                "Memory Exhausted (OOM): The app ran out of heap memory. This typically happens when decoding too many large bitmaps or caching huge payloads. Restarting the app will clear temporary allocations."
            }
            exceptionType.contains("PlaybackFailure", ignoreCase = true) ||
                exceptionMessage.contains("MPV_PLAYBACK_ERROR", ignoreCase = true) ||
                exceptionMessage.contains("libmpv", ignoreCase = true) ||
                exceptionMessage.contains("MPV", ignoreCase = true) ||
                exceptionMessage.contains("MediaCodec", ignoreCase = true) -> {
                "Video Playback Error: A libmpv/MediaCodec media rendering or decoding issue occurred. This might be due to a temporary network disruption, expired stream session, or incompatible codec. Check your internet connection and try reloading the stream."
            }
            exceptionType.contains("ConnectException", ignoreCase = true) || exceptionType.contains("UnknownHostException", ignoreCase = true) || exceptionType.contains("SocketTimeoutException", ignoreCase = true) -> {
                "Network Connection Error: Could not connect to the remote host. Please check if your internet is working and if the Telegram backend server is online and running."
            }
            else -> {
                "An unexpected exception occurred in the application. Please copy the logs below and report this issue on GitHub to help us fix it."
            }
        }

        Text(
            text = "Suggestion: $suggestion",
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun BytecodeDiagnosisCard(
    bytecodeDetails: BytecodeFailureDetails,
    onCopyKeepRule: () -> Unit,
) {
    if (!bytecodeDetails.isBytecodeIssue) return

    val containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
    val contentColor = MaterialTheme.colorScheme.onErrorContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = NextIcons.Decoder,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Bytecode & R8 Devirtualization Diagnosis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                )
            }
        }

        // Failure type badge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Issue Classification:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = bytecodeDetails.failureType,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            SelectionContainer {
                Text(
                    text = "Missing Symbol: ${bytecodeDetails.missingSymbol}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            SelectionContainer {
                Text(
                    text = "Receiver Target: ${bytecodeDetails.receiverType}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Technical Cause
        Text(
            text = bytecodeDetails.technicalCause,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )

        // Suggested ProGuard / R8 keep rule block
        if (bytecodeDetails.suggestedKeepRule.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Suggested ProGuard / R8 Keep Rule:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedButton(
                        onClick = onCopyKeepRule,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = NextIcons.Copy,
                            contentDescription = "Copy Rule",
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "Copy Rule",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                SelectionContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = bytecodeDetails.suggestedKeepRule,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Solution Steps
        if (bytecodeDetails.solutionSteps.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Recommended Fix Steps:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                for (step in bytecodeDetails.solutionSteps) {
                    Text(
                        text = "• $step",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposeContextCard(
    composeContext: ComposeFailureContext,
) {
    if (!composeContext.isComposeIssue) return

    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = NextIcons.Style,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = "Compose Node & Lifecycle Context",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Failing Phase: ${composeContext.lifecyclePhase}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Probable Screen: ${composeContext.probableScreen}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Subsystem: ${composeContext.failingSubsystem}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = composeContext.technicalExplanation,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Architecture Advice:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = composeContext.contextAdvice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CulpritDiagnosisCard(
    analysis: CrashAnalysis,
    onCopyCulprit: () -> Unit = {},
) {
    val containerColor = MaterialTheme.colorScheme.tertiaryContainer
    val contentColor = MaterialTheme.colorScheme.onTertiaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = NextIcons.BugReport,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Culprit Code & Root Cause",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                )
            }

            if (analysis.culpritClass != "UnknownClass") {
                FilledIconButton(
                    onClick = onCopyCulprit,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = NextIcons.Copy,
                        contentDescription = "Copy Culprit",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Culprit file and line highlight badge
        val lineDisplay = if (analysis.culpritLine > 0) ":${analysis.culpritLine}" else ""
        val culpritDisplay = "${analysis.culpritFile}$lineDisplay"

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Culprit Location:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectionContainer {
                Text(
                    text = culpritDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            SelectionContainer {
                Text(
                    text = "Method: fun ${analysis.culpritMethod}() in ${analysis.culpritClass}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (analysis.isObfuscatedFrame) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "⚠️ R8 Minified Frame (SourceFile)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "The original Kotlin file and class names were obfuscated by R8. Look at the Bytecode Diagnosis and Compose Context cards below to see the exact failing component and ProGuard rule.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        // Operation & Subsystem details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Operation:",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f),
                )
                Text(
                    text = analysis.culpritOperation,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Subsystem:",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f),
                )
                Text(
                    text = analysis.affectedSubsystem,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
            }
        }

        // Recommendation / Fix
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(dev.roxy.kryptonplayer.core.ui.R.drawable.ic_lightbulb_hint),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Recommended Fix:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = analysis.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        // App Call Stack if multiple frames
        if (analysis.appFrames.size > 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "↳ App Call Chain:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SelectionContainer {
                    Column {
                        for (frame in analysis.appFrames) {
                            Text(
                                text = "• $frame",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentBreadcrumbsCard(
    breadcrumbs: String,
) {
    if (breadcrumbs.isBlank()) return

    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = NextIcons.DashBoard,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = "Recent User Actions (Breadcrumbs)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }

        Text(
            text = "User actions captured leading up to the freeze/crash:",
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.8f),
        )

        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(10.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            Text(
                text = breadcrumbs.trim(),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CrashScreen(
    modifier: Modifier = Modifier,
    exceptionString: String,
    threadName: String,
    exceptionType: String,
    exceptionMessage: String,
    exceptionCause: String,
    isAnr: Boolean = false,
    analysis: CrashAnalysis,
    breadcrumbs: String,
    logcat: String,
    onCopyCulpritClick: () -> Unit = {},
    onCopyKeepRuleClick: () -> Unit = {},
    onCopyFixClick: () -> Unit = {},
    onShareLogsClick: () -> Unit = {},
    onCopyLogsClick: () -> Unit = {},
    onRestartClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var crashHistory by remember { mutableStateOf(CrashStorage.getAllCrashes(context)) }

    var activeAnalysis by remember { mutableStateOf(analysis) }
    var activeExceptionString by remember { mutableStateOf(exceptionString) }
    var activeExceptionType by remember { mutableStateOf(exceptionType) }
    var activeExceptionMessage by remember { mutableStateOf(exceptionMessage) }
    var activeExceptionCause by remember { mutableStateOf(exceptionCause) }
    var activeThreadName by remember { mutableStateOf(threadName) }
    var activeIsAnr by remember { mutableStateOf(isAnr) }
    var activeBreadcrumbs by remember { mutableStateOf(breadcrumbs) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            val borderColor = MaterialTheme.colorScheme.outline
            Column(
                Modifier
                    .drawBehind {
                        drawLine(
                            color = borderColor,
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = Dp.Hairline.value,
                        )
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onShareLogsClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.crash_screen_share))
                    }
                    FilledIconButton(onClick = onCopyLogsClick) {
                        Icon(imageVector = NextIcons.Copy, contentDescription = null)
                    }
                }
                OutlinedButton(
                    onClick = onRestartClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.crash_screen_restart))
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = NextIcons.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = if (activeIsAnr) "App Unresponsive (ANR)" else stringResource(R.string.crash_screen_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (activeIsAnr) "Krypton Player stopped responding and was intercepted by the ANR watchdog" else stringResource(R.string.crash_screen_subtitle, stringResource(R.string.app_name)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // TabRow: Tab 0 (Analysis) vs Tab 1 (Crash History List)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("🚨 Crash Analysis", fontWeight = FontWeight.Bold) },
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        crashHistory = CrashStorage.getAllCrashes(context)
                        selectedTabIndex = 1
                    },
                    text = { Text("📜 Crash History (${crashHistory.size})", fontWeight = FontWeight.Bold) },
                )
            }

            if (selectedTabIndex == 0) {
                // 1. Crash Trigger Banner (Exact action that provoked the crash)
                CrashTriggerBanner(
                    triggerAction = activeAnalysis.triggerAction,
                    probableScreen = activeAnalysis.composeContext.probableScreen,
                )

                // 2. Root Cause & Actionable Fix Card
                RootCauseAndFixCard(
                    analysis = activeAnalysis,
                    onCopyFix = onCopyFixClick,
                )

                // 3. Culprit Code & Location Card
                CulpritDiagnosisCard(
                    analysis = activeAnalysis,
                    onCopyCulprit = onCopyCulpritClick,
                )

                // 4. Bytecode & R8 Devirtualization Card (if applicable)
                if (activeAnalysis.bytecodeDetails.isBytecodeIssue) {
                    BytecodeDiagnosisCard(
                        bytecodeDetails = activeAnalysis.bytecodeDetails,
                        onCopyKeepRule = onCopyKeepRuleClick,
                    )
                }

                // 5. Jetpack Compose Lifecycle Context Card (if applicable)
                if (activeAnalysis.composeContext.isComposeIssue) {
                    ComposeContextCard(
                        composeContext = activeAnalysis.composeContext,
                    )
                }

                // 6. User Actions Breadcrumbs Timeline Card
                RecentBreadcrumbsCard(breadcrumbs = activeBreadcrumbs)

                // 7. Exception & Thread Diagnostics Card
                CrashDiagnosticsCard(
                    exceptionType = activeExceptionType,
                    exceptionMessage = activeExceptionMessage,
                    threadName = activeThreadName,
                    exceptionCause = activeExceptionCause,
                    isAnr = activeIsAnr,
                )

                Text(
                    text = stringResource(R.string.crash_screen_logs_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                LogsSelectionContainer(logs = activeExceptionString)
                Text(
                    text = stringResource(R.string.crash_screen_logcat),
                    style = MaterialTheme.typography.headlineSmall,
                )
                LogsSelectionContainer(logs = logcat)
            } else {
                // Tab 1: Interactive Crash History List
                CrashHistoryListView(
                    crashes = crashHistory,
                    onSelectCrash = { item ->
                        activeAnalysis = CrashAnalysis(
                            category = if (item.isAnr) "Main UI Thread Disk I/O (ANR)" else item.exceptionType,
                            culpritClass = item.culpritClass,
                            culpritMethod = item.culpritMethod,
                            culpritFile = item.culpritFile,
                            culpritLine = item.culpritLine,
                            culpritOperation = "Crash from History (${item.triggerAction})",
                            affectedSubsystem = item.failureOrigin,
                            plainDiagnosis = "Crash in ${item.culpritClass}.${item.culpritMethod}() at line ${item.culpritLine} of ${item.culpritFile}",
                            recommendation = item.recommendation,
                            appFrames = listOf("${item.culpritFile}:${item.culpritLine} (${item.culpritClass}.${item.culpritMethod})"),
                            isObfuscatedFrame = item.culpritFile == "SourceFile",
                            bytecodeDetails = BytecodeFailureDetails(
                                isBytecodeIssue = item.isBytecodeIssue,
                                failureType = if (item.isBytecodeIssue) "R8 Devirtualization / Missing Keep Rule" else "",
                                suggestedKeepRule = item.suggestedKeepRule,
                            ),
                            composeContext = ComposeFailureContext(
                                isComposeIssue = item.isComposeIssue,
                                probableScreen = item.triggerAction,
                            ),
                            triggerAction = item.triggerAction,
                            failureOrigin = item.failureOrigin,
                            reasonHindi = item.reasonHindi,
                            fixHindi = item.fixHindi,
                        )
                        activeExceptionString = item.fullLog
                        activeExceptionType = item.exceptionType
                        activeExceptionMessage = item.exceptionMessage
                        activeIsAnr = item.isAnr
                        selectedTabIndex = 0
                        Toast.makeText(context, "Loaded crash report (${item.exceptionType})", Toast.LENGTH_SHORT).show()
                    },
                    onCopyCrash = { item ->
                        val clip = (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager)
                        clip?.setPrimaryClip(ClipData.newPlainText("Crash Report", item.fullLog))
                        Toast.makeText(context, "Crash report copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteCrash = { item ->
                        CrashStorage.deleteCrashById(context, item.id)
                        crashHistory = CrashStorage.getAllCrashes(context)
                        Toast.makeText(context, "Crash record deleted", Toast.LENGTH_SHORT).show()
                    },
                    onClearAll = {
                        CrashStorage.clearAllCrashes(context)
                        crashHistory = emptyList()
                        Toast.makeText(context, "All crash records cleared", Toast.LENGTH_SHORT).show()
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LogsSelectionContainer(
    logs: String,
    modifier: Modifier = Modifier,
) {
    SelectionContainer(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        Text(
            text = logs,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun CrashLogsScreenPreview() {
    KryptonPlayerTheme {
        CrashScreen(
            exceptionString = "Exception message",
            threadName = "main",
            exceptionType = "OutOfMemoryError",
            exceptionMessage = "Failed to allocate 8208 bytes",
            exceptionCause = "None",
            isAnr = false,
            analysis = CrashAnalysis(
                category = "Main UI Thread Disk I/O (ANR)",
                culpritClass = "TelegramDownloadManager",
                culpritMethod = "getOfflineFile",
                culpritFile = "TelegramDownloadManager.kt",
                culpritLine = 442,
                culpritOperation = "Synchronous Disk / File System Access (File.exists)",
                affectedSubsystem = "Telegram Downloads & Cache",
                plainDiagnosis = "Main UI thread was frozen for > 4000ms because File.exists() was called synchronously.",
                recommendation = "Offload all File / Disk I/O operations to CoroutineScope(Dispatchers.IO).",
                appFrames = listOf("TelegramDownloadManager.kt:442 (TelegramDownloadManager.getOfflineFile)"),
            ),
            breadcrumbs = "[14:22:05.120] [NAV] Opened Screen: Telegram Cloud\n[14:22:07.450] [UI] Clicked Download: Inception.mp4",
            logcat = "Logcat message",
        )
    }
}
