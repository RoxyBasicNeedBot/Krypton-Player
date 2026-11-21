package dev.roxy.kryptonplayer.crash

import android.content.Context
import android.os.Build
import android.util.Log
import dev.roxy.kryptonplayer.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashHistoryItem(
    val id: String,
    val timestamp: Long,
    val exceptionType: String,
    val exceptionMessage: String,
    val triggerAction: String,
    val culpritFile: String,
    val culpritLine: Int,
    val culpritClass: String,
    val culpritMethod: String,
    val failureOrigin: String,
    val isAnr: Boolean,
    val isBytecodeIssue: Boolean,
    val isComposeIssue: Boolean,
    val reasonHindi: String = "",
    val fixHindi: String = "",
    val recommendation: String = "",
    val suggestedKeepRule: String = "",
    val fullLog: String = "",
)

data class CrashReportData(
    val timestamp: Long,
    val exceptionType: String,
    val exceptionMessage: String,
    val threadName: String,
    val exceptionCause: String,
    val stackTrace: String,
    val isAnr: Boolean = false,
    val fullLog: String = "",
    val culpritClass: String = "",
    val culpritMethod: String = "",
    val culpritFile: String = "",
    val culpritLine: Int = -1,
    val culpritOperation: String = "",
    val affectedSubsystem: String = "",
    val plainDiagnosis: String = "",
    val recommendation: String = "",
    val breadcrumbs: String = "",
    val appFrames: List<String> = emptyList(),
    val isObfuscatedFrame: Boolean = false,
    val bytecodeDetails: BytecodeFailureDetails = BytecodeFailureDetails(),
    val composeContext: ComposeFailureContext = ComposeFailureContext(),
    val triggerAction: String = "",
    val failureOrigin: String = "",
    val reasonHindi: String = "",
    val fixHindi: String = "",
)

object CrashStorage {
    private const val TAG = "CrashStorage"
    private const val PREFS_NAME = "arch_crash_reports_pref"
    private const val KEY_HAS_CRASH = "has_crash"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_TYPE = "type"
    private const val KEY_MESSAGE = "message"
    private const val KEY_THREAD = "thread"
    private const val KEY_CAUSE = "cause"
    private const val KEY_STACK = "stack"
    private const val KEY_IS_ANR = "is_anr"
    private const val KEY_FULL_LOG = "full_log"
    private const val KEY_CULPRIT_CLASS = "culprit_class"
    private const val KEY_CULPRIT_METHOD = "culprit_method"
    private const val KEY_CULPRIT_FILE = "culprit_file"
    private const val KEY_CULPRIT_LINE = "culprit_line"
    private const val KEY_CULPRIT_OP = "culprit_op"
    private const val KEY_SUBSYSTEM = "subsystem"
    private const val KEY_DIAGNOSIS = "diagnosis"
    private const val KEY_RECOMMENDATION = "recommendation"
    private const val KEY_BREADCRUMBS = "breadcrumbs"
    private const val KEY_APP_FRAMES = "app_frames"
    private const val KEY_IS_OBFUSCATED = "is_obfuscated"

    // Bytecode & R8 keys
    private const val KEY_IS_BYTECODE = "is_bytecode"
    private const val KEY_BYTECODE_TYPE = "bytecode_type"
    private const val KEY_BYTECODE_SYMBOL = "bytecode_symbol"
    private const val KEY_BYTECODE_RECEIVER = "bytecode_receiver"
    private const val KEY_BYTECODE_CAUSE = "bytecode_cause"
    private const val KEY_BYTECODE_KEEP_RULE = "bytecode_keep_rule"
    private const val KEY_BYTECODE_STEPS = "bytecode_steps"

    // Compose context keys
    private const val KEY_IS_COMPOSE = "is_compose"
    private const val KEY_COMPOSE_PHASE = "compose_phase"
    private const val KEY_COMPOSE_SUBSYSTEM = "compose_subsystem"
    private const val KEY_COMPOSE_SCREEN = "compose_screen"
    private const val KEY_COMPOSE_EXPLANATION = "compose_explanation"
    private const val KEY_COMPOSE_ADVICE = "compose_advice"

    // Trigger & Attribution keys
    private const val KEY_TRIGGER_ACTION = "trigger_action"
    private const val KEY_FAILURE_ORIGIN = "failure_origin"
    private const val KEY_REASON_HINDI = "reason_hindi"
    private const val KEY_FIX_HINDI = "fix_hindi"
    private const val KEY_CRASH_HISTORY_JSON = "crash_history_json"

    fun saveCrash(
        context: Context,
        throwable: Throwable?,
        threadName: String,
        isAnr: Boolean = false,
        extraInfo: String? = null,
    ): File? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val timestamp = System.currentTimeMillis()
            val id = timestamp.toString()
            val stackTrace = buildString {
                if (throwable != null) {
                    appendLine(throwable.stackTraceToString())
                }
                if (!extraInfo.isNullOrBlank()) {
                    appendLine()
                    appendLine(extraInfo)
                }
            }
            val exceptionType = if (isAnr) "ApplicationNotResponding (ANR)" else throwable?.javaClass?.simpleName ?: "UnknownException"
            val exceptionMessage = throwable?.message ?: if (isAnr) "Main UI thread frozen > 4000ms" else "No error message"
            val exceptionCause = throwable?.cause?.toString() ?: "None"
            val breadcrumbs = CrashBreadcrumbs.dump()
            val analysis = CrashAnalyzer.analyze(throwable, stackTrace, isAnr)

            val dir = File(context.filesDir, "crash_reports").apply { if (!exists()) mkdirs() }
            val latestFile = File(dir, "latest_crash.txt")
            val fullReport = buildString {
                appendLine("=== KRYPTON PLAYER CRASH REPORT ===")
                appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))}")
                appendLine("Is ANR: $isAnr")
                appendLine("Exception: $exceptionType")
                appendLine("Message: $exceptionMessage")
                appendLine("Thread: $threadName")
                appendLine("Cause: $exceptionCause")
                appendLine()
                appendLine("=== DEVICE INFO ===")
                appendLine(collectDeviceInfo())
                appendLine()
                appendLine("=== BREADCRUMBS ===")
                appendLine(breadcrumbs)
                appendLine()
                appendLine("=== STACK TRACE ===")
                appendLine(stackTrace)
            }
            latestFile.writeText(fullReport, Charsets.UTF_8)

            prefs.edit().apply {
                putBoolean(KEY_HAS_CRASH, true)
                putLong(KEY_TIMESTAMP, timestamp)
                putString(KEY_TYPE, exceptionType)
                putString(KEY_MESSAGE, exceptionMessage)
                putString(KEY_THREAD, threadName)
                putString(KEY_CAUSE, exceptionCause)
                putString(KEY_STACK, stackTrace)
                putBoolean(KEY_IS_ANR, isAnr)
                putString(KEY_FULL_LOG, fullReport)
                putString(KEY_CULPRIT_CLASS, analysis.culpritClass)
                putString(KEY_CULPRIT_METHOD, analysis.culpritMethod)
                putString(KEY_CULPRIT_FILE, analysis.culpritFile)
                putInt(KEY_CULPRIT_LINE, analysis.culpritLine)
                putString(KEY_CULPRIT_OP, analysis.culpritOperation)
                putString(KEY_SUBSYSTEM, analysis.affectedSubsystem)
                putString(KEY_DIAGNOSIS, analysis.plainDiagnosis)
                putString(KEY_RECOMMENDATION, analysis.recommendation)
                putString(KEY_BREADCRUMBS, breadcrumbs)
                putString(KEY_APP_FRAMES, analysis.appFrames.joinToString("\n"))
                putBoolean(KEY_IS_OBFUSCATED, analysis.isObfuscatedFrame)
                putBoolean(KEY_IS_BYTECODE, analysis.bytecodeDetails.isBytecodeIssue)
                putString(KEY_BYTECODE_TYPE, analysis.bytecodeDetails.failureType)
                putString(KEY_BYTECODE_SYMBOL, analysis.bytecodeDetails.missingSymbol)
                putString(KEY_BYTECODE_RECEIVER, analysis.bytecodeDetails.receiverType)
                putString(KEY_BYTECODE_CAUSE, analysis.bytecodeDetails.technicalCause)
                putString(KEY_BYTECODE_KEEP_RULE, analysis.bytecodeDetails.suggestedKeepRule)
                putString(KEY_BYTECODE_STEPS, analysis.bytecodeDetails.solutionSteps.joinToString("\n"))
                putBoolean(KEY_IS_COMPOSE, analysis.composeContext.isComposeIssue)
                putString(KEY_COMPOSE_PHASE, analysis.composeContext.lifecyclePhase)
                putString(KEY_COMPOSE_SUBSYSTEM, analysis.composeContext.failingSubsystem)
                putString(KEY_COMPOSE_SCREEN, analysis.composeContext.probableScreen)
                putString(KEY_COMPOSE_EXPLANATION, analysis.composeContext.technicalExplanation)
                putString(KEY_COMPOSE_ADVICE, analysis.composeContext.contextAdvice)
                putString(KEY_TRIGGER_ACTION, analysis.triggerAction)
                putString(KEY_FAILURE_ORIGIN, analysis.failureOrigin)
                putString(KEY_REASON_HINDI, analysis.reasonHindi)
                putString(KEY_FIX_HINDI, analysis.fixHindi)
                apply()
            }

            val historyDir = File(dir, "history")
            val historyItem = CrashHistoryItem(
                id = id,
                timestamp = timestamp,
                exceptionType = exceptionType,
                exceptionMessage = exceptionMessage,
                triggerAction = analysis.triggerAction,
                culpritFile = analysis.culpritFile,
                culpritLine = analysis.culpritLine,
                culpritClass = analysis.culpritClass,
                culpritMethod = analysis.culpritMethod,
                failureOrigin = analysis.failureOrigin,
                isAnr = isAnr,
                isBytecodeIssue = analysis.bytecodeDetails.isBytecodeIssue,
                isComposeIssue = analysis.composeContext.isComposeIssue,
                reasonHindi = analysis.reasonHindi,
                fixHindi = analysis.fixHindi,
                recommendation = analysis.recommendation,
                suggestedKeepRule = analysis.bytecodeDetails.suggestedKeepRule,
                fullLog = fullReport,
            )
            saveToHistory(prefs, historyDir, historyItem)

            latestFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash", e)
            null
        }
    }

    private fun saveToHistory(
        prefs: android.content.SharedPreferences,
        historyDir: File,
        item: CrashHistoryItem,
    ) {
        try {
            if (!historyDir.exists()) historyDir.mkdirs()
            val historyFile = File(historyDir, "crash_${item.id}.txt")
            historyFile.writeText(item.fullLog, Charsets.UTF_8)

            val jsonStr = prefs.getString(KEY_CRASH_HISTORY_JSON, "[]") ?: "[]"
            val array = JSONArray(jsonStr)

            val newObj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("exceptionType", item.exceptionType)
                put("exceptionMessage", item.exceptionMessage)
                put("triggerAction", item.triggerAction)
                put("culpritFile", item.culpritFile)
                put("culpritLine", item.culpritLine)
                put("culpritClass", item.culpritClass)
                put("culpritMethod", item.culpritMethod)
                put("failureOrigin", item.failureOrigin)
                put("isAnr", item.isAnr)
                put("isBytecodeIssue", item.isBytecodeIssue)
                put("isComposeIssue", item.isComposeIssue)
                put("reasonHindi", item.reasonHindi)
                put("fixHindi", item.fixHindi)
                put("recommendation", item.recommendation)
                put("suggestedKeepRule", item.suggestedKeepRule)
                put("fullLog", item.fullLog)
            }

            val updatedArray = JSONArray()
            updatedArray.put(newObj)
            for (i in 0 until array.length()) {
                if (updatedArray.length() >= 25) break
                val oldObj = array.getJSONObject(i)
                if (oldObj.optString("id") != item.id) {
                    updatedArray.put(oldObj)
                }
            }

            prefs.edit().putString(KEY_CRASH_HISTORY_JSON, updatedArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append crash to history", e)
        }
    }

    fun getAllCrashes(context: Context): List<CrashHistoryItem> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_CRASH_HISTORY_JSON, "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CrashHistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CrashHistoryItem(
                        id = obj.optString("id", ""),
                        timestamp = obj.optLong("timestamp", 0L),
                        exceptionType = obj.optString("exceptionType", ""),
                        exceptionMessage = obj.optString("exceptionMessage", ""),
                        triggerAction = obj.optString("triggerAction", ""),
                        culpritFile = obj.optString("culpritFile", ""),
                        culpritLine = obj.optInt("culpritLine", -1),
                        culpritClass = obj.optString("culpritClass", ""),
                        culpritMethod = obj.optString("culpritMethod", ""),
                        failureOrigin = obj.optString("failureOrigin", ""),
                        isAnr = obj.optBoolean("isAnr", false),
                        isBytecodeIssue = obj.optBoolean("isBytecodeIssue", false),
                        isComposeIssue = obj.optBoolean("isComposeIssue", false),
                        reasonHindi = obj.optString("reasonHindi", ""),
                        fixHindi = obj.optString("fixHindi", ""),
                        recommendation = obj.optString("recommendation", ""),
                        suggestedKeepRule = obj.optString("suggestedKeepRule", ""),
                        fullLog = obj.optString("fullLog", ""),
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load crash history", e)
            emptyList()
        }
    }

    fun deleteCrashById(context: Context, id: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_CRASH_HISTORY_JSON, "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") != id) {
                    newArray.put(obj)
                }
            }
            prefs.edit().putString(KEY_CRASH_HISTORY_JSON, newArray.toString()).apply()
            val file = File(context.filesDir, "crash_reports/history/crash_$id.txt")
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete crash: $id", e)
        }
    }

    fun clearAllCrashes(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            val dir = File(context.filesDir, "crash_reports")
            if (dir.exists()) dir.deleteRecursively()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all crash reports", e)
        }
    }

    fun getLatestCrash(context: Context): CrashReportData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS_CRASH, false)) {
            // Check file fallback
            val file = File(context.filesDir, "crash_reports/latest_crash.txt")
            if (file.exists() && file.length() > 0L) {
                val text = file.readText(Charsets.UTF_8)
                val isAnr = text.contains("Is ANR: true")
                val analysis = CrashAnalyzer.analyze(null, text, isAnr)
                return CrashReportData(
                    timestamp = file.lastModified(),
                    exceptionType = if (isAnr) "ApplicationNotResponding (ANR)" else "Crash",
                    exceptionMessage = text.lines().firstOrNull { it.startsWith("Message: ") }?.removePrefix("Message: ") ?: "Crash detected",
                    threadName = text.lines().firstOrNull { it.startsWith("Thread: ") }?.removePrefix("Thread: ") ?: "main",
                    exceptionCause = text.lines().firstOrNull { it.startsWith("Cause: ") }?.removePrefix("Cause: ") ?: "None",
                    stackTrace = text,
                    isAnr = isAnr,
                    fullLog = text,
                    culpritClass = analysis.culpritClass,
                    culpritMethod = analysis.culpritMethod,
                    culpritFile = analysis.culpritFile,
                    culpritLine = analysis.culpritLine,
                    culpritOperation = analysis.culpritOperation,
                    affectedSubsystem = analysis.affectedSubsystem,
                    plainDiagnosis = analysis.plainDiagnosis,
                    recommendation = analysis.recommendation,
                    breadcrumbs = "",
                    appFrames = analysis.appFrames,
                    isObfuscatedFrame = analysis.isObfuscatedFrame,
                    bytecodeDetails = analysis.bytecodeDetails,
                    composeContext = analysis.composeContext,
                    triggerAction = analysis.triggerAction,
                    failureOrigin = analysis.failureOrigin,
                    reasonHindi = analysis.reasonHindi,
                    fixHindi = analysis.fixHindi,
                )
            }
            return null
        }

        val appFramesStr = prefs.getString(KEY_APP_FRAMES, "") ?: ""
        val appFrames = if (appFramesStr.isNotBlank()) appFramesStr.lines().filter { it.isNotBlank() } else emptyList()

        val isBytecode = prefs.getBoolean(KEY_IS_BYTECODE, false)
        val bytecodeStepsStr = prefs.getString(KEY_BYTECODE_STEPS, "") ?: ""
        val bytecodeSteps = if (bytecodeStepsStr.isNotBlank()) bytecodeStepsStr.lines().filter { it.isNotBlank() } else emptyList()
        val bytecodeDetails = BytecodeFailureDetails(
            isBytecodeIssue = isBytecode,
            failureType = prefs.getString(KEY_BYTECODE_TYPE, "") ?: "",
            missingSymbol = prefs.getString(KEY_BYTECODE_SYMBOL, "") ?: "",
            receiverType = prefs.getString(KEY_BYTECODE_RECEIVER, "") ?: "",
            technicalCause = prefs.getString(KEY_BYTECODE_CAUSE, "") ?: "",
            suggestedKeepRule = prefs.getString(KEY_BYTECODE_KEEP_RULE, "") ?: "",
            solutionSteps = bytecodeSteps,
        )

        val isCompose = prefs.getBoolean(KEY_IS_COMPOSE, false)
        val composeContext = ComposeFailureContext(
            isComposeIssue = isCompose,
            lifecyclePhase = prefs.getString(KEY_COMPOSE_PHASE, "") ?: "",
            failingSubsystem = prefs.getString(KEY_COMPOSE_SUBSYSTEM, "") ?: "",
            probableScreen = prefs.getString(KEY_COMPOSE_SCREEN, "") ?: "",
            technicalExplanation = prefs.getString(KEY_COMPOSE_EXPLANATION, "") ?: "",
            contextAdvice = prefs.getString(KEY_COMPOSE_ADVICE, "") ?: "",
        )

        return CrashReportData(
            timestamp = prefs.getLong(KEY_TIMESTAMP, System.currentTimeMillis()),
            exceptionType = prefs.getString(KEY_TYPE, "Exception") ?: "Exception",
            exceptionMessage = prefs.getString(KEY_MESSAGE, "Crash occurred") ?: "Crash occurred",
            threadName = prefs.getString(KEY_THREAD, "main") ?: "main",
            exceptionCause = prefs.getString(KEY_CAUSE, "None") ?: "None",
            stackTrace = prefs.getString(KEY_STACK, "") ?: "",
            isAnr = prefs.getBoolean(KEY_IS_ANR, false),
            fullLog = prefs.getString(KEY_FULL_LOG, "") ?: "",
            culpritClass = prefs.getString(KEY_CULPRIT_CLASS, "") ?: "",
            culpritMethod = prefs.getString(KEY_CULPRIT_METHOD, "") ?: "",
            culpritFile = prefs.getString(KEY_CULPRIT_FILE, "") ?: "",
            culpritLine = prefs.getInt(KEY_CULPRIT_LINE, -1),
            culpritOperation = prefs.getString(KEY_CULPRIT_OP, "") ?: "",
            affectedSubsystem = prefs.getString(KEY_SUBSYSTEM, "") ?: "",
            plainDiagnosis = prefs.getString(KEY_DIAGNOSIS, "") ?: "",
            recommendation = prefs.getString(KEY_RECOMMENDATION, "") ?: "",
            breadcrumbs = prefs.getString(KEY_BREADCRUMBS, "") ?: "",
            appFrames = appFrames,
            isObfuscatedFrame = prefs.getBoolean(KEY_IS_OBFUSCATED, false),
            bytecodeDetails = bytecodeDetails,
            composeContext = composeContext,
            triggerAction = prefs.getString(KEY_TRIGGER_ACTION, "") ?: "",
            failureOrigin = prefs.getString(KEY_FAILURE_ORIGIN, "") ?: "",
            reasonHindi = prefs.getString(KEY_REASON_HINDI, "") ?: "",
            fixHindi = prefs.getString(KEY_FIX_HINDI, "") ?: "",
        )
    }

    fun clearLatestCrash(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val history = prefs.getString(KEY_CRASH_HISTORY_JSON, null)
            prefs.edit().clear().apply()
            if (history != null) {
                prefs.edit().putString(KEY_CRASH_HISTORY_JSON, history).apply()
            }
            val file = File(context.filesDir, "crash_reports/latest_crash.txt")
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash report", e)
        }
    }

    private fun collectDeviceInfo(): String = """
        App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
        Android version: ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})
        Device brand: ${Build.BRAND}
        Device manufacturer: ${Build.MANUFACTURER}
        Device model: ${Build.MODEL} (${Build.DEVICE})
        Memory: ${Runtime.getRuntime().freeMemory() / 1024 / 1024}MB free / ${Runtime.getRuntime().totalMemory() / 1024 / 1024}MB total (max ${Runtime.getRuntime().maxMemory() / 1024 / 1024}MB)
    """.trimIndent()
}
