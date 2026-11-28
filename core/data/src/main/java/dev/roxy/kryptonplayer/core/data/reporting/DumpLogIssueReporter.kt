package dev.roxy.kryptonplayer.core.data.reporting

import android.content.Context
import dev.roxy.kryptonplayer.core.common.CrashBreadcrumbs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class DumpLogBundle(
    val specs: DeviceSpecs,
    val logLineCount: Int,
    val sanitizedLogcat: String,
    val markdownReport: String,
    val fullPlainTextDump: String,
)

sealed interface DumpLogSubmitResult {
    data class IssueCreated(
        val issueNumber: Int,
        val issueUrl: String,
        val markdownReport: String,
    ) : DumpLogSubmitResult

    data class FallbackReady(
        val reason: String,
        val markdownReport: String,
        val fullPlainTextDump: String,
        val prefilledIssueUrl: String,
    ) : DumpLogSubmitResult
}

/**
 * Collects sanitized diagnostic dump logs and submits GitHub issues via the
 * Krypton Agni Cloudflare Worker relay (with zero client-side GitHub credentials),
 * falling back cleanly to a prefilled GitHub new-issue URL and full dump report.
 */
object DumpLogIssueReporter {

    private const val DUMP_LOG_RELAY_URL = "https://agni-core.agni-dev-core.workers.dev/issues/dump-log"
    private const val GITHUB_NEW_ISSUE_URL = "https://github.com/RoxyBasicNeedBot/Krypton-Player/issues/new"
    private const val CLIENT_HEADER_VALUE = "krypton-player-android"
    private const val LOGCAT_PREVIEW_LIMIT = 1200
    private const val MAX_TITLE_CHARS = 180
    private const val MAX_PREFILLED_URL_CHARS = 6000
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun collectPreview(
        context: Context,
        category: String = "Bug Report",
        description: String = "",
    ): DumpLogBundle = withContext(Dispatchers.IO) {
        val specs = AppLogCollector.collectDeviceSpecs(context)
        val logEntries = LogcatDumper.dumpLogcat(limit = LOGCAT_PREVIEW_LIMIT)
        val sanitizedLogcat = LogSanitizer.sanitizeAll(
            logEntries.joinToString("\n") { it.raw }
        ).trim()

        val markdownReport = AppLogCollector.generateGitHubIssueMarkdown(
            context = context,
            issueCategory = category,
            userDescription = description,
            fullLogcatText = sanitizedLogcat,
        )

        val fullPlainTextDump = AppLogCollector.generateFullPlainTextDump(
            context = context,
            issueSummary = description,
            logcatLines = logEntries,
        )

        DumpLogBundle(
            specs = specs,
            logLineCount = logEntries.size,
            sanitizedLogcat = sanitizedLogcat,
            markdownReport = markdownReport,
            fullPlainTextDump = fullPlainTextDump,
        )
    }

    suspend fun submitDumpLogIssue(
        context: Context,
        title: String,
        category: String,
        description: String,
    ): DumpLogSubmitResult = withContext(Dispatchers.IO) {
        val cleanCategory = LogSanitizer.sanitize(category)
            .replace(Regex("[\\r\\n]+"), " ")
            .trim()
            .ifBlank { "Bug Report" }

        val cleanDescription = LogSanitizer.sanitizeAll(description).trim()

        val cleanTitle = LogSanitizer.sanitize(title)
            .replace(Regex("[\\r\\n]+"), " ")
            .trim()
            .ifBlank { "[$cleanCategory] Krypton Player Bug Report" }
            .take(MAX_TITLE_CHARS)

        val bundle = collectPreview(
            context = context,
            category = cleanCategory,
            description = cleanDescription,
        )

        val prefilledUrl = buildPrefilledIssueUrl(
            title = cleanTitle,
            category = cleanCategory,
            description = cleanDescription,
            specs = bundle.specs,
        )

        val payloadJson = JSONObject().apply {
            put("title", cleanTitle)
            put("category", cleanCategory)
            put("description", cleanDescription)
            put("body", bundle.markdownReport)
            put("appVersion", "${bundle.specs.versionName} (${bundle.specs.versionCode})")
            put("deviceModel", "${bundle.specs.manufacturer} ${bundle.specs.model}".trim())
            put("androidVersion", "Android ${bundle.specs.androidVersion} (API ${bundle.specs.sdkInt})")
            put("abi", bundle.specs.supportedAbis.joinToString(", ").ifBlank { "unknown" })
        }.toString()

        val safeVersionForHeader = bundle.specs.versionName
            .replace(Regex("[^\\x20-\\x7E]"), "")
            .ifBlank { "1.0.0" }

        val request = Request.Builder()
            .url(DUMP_LOG_RELAY_URL)
            .header("Content-Type", "application/json")
            .header("X-Agni-Client", CLIENT_HEADER_VALUE)
            .header("User-Agent", "KryptonPlayer-Android/$safeVersionForHeader")
            .post(payloadJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                val json = parseJsonOrNull(rawBody)

                val okFlag = json?.optBoolean("ok", true) ?: false
                val issueNumber = json?.optInt("issueNumber", 0) ?: 0
                val issueUrl = json?.optString("issueUrl", "").orEmpty()

                if (response.isSuccessful && okFlag && issueNumber > 0 && issueUrl.isNotBlank()) {
                    return@withContext DumpLogSubmitResult.IssueCreated(
                        issueNumber = issueNumber,
                        issueUrl = issueUrl,
                        markdownReport = bundle.markdownReport,
                    )
                }

                val errorCode = json?.optString("code", "").orEmpty()
                val errorMessage = json?.optString("error", "").orEmpty()
                val reason = mapHttpFailureReason(
                    statusCode = response.code,
                    errorCode = errorCode,
                    errorMessage = errorMessage,
                )

                DumpLogSubmitResult.FallbackReady(
                    reason = reason,
                    markdownReport = bundle.markdownReport,
                    fullPlainTextDump = bundle.fullPlainTextDump,
                    prefilledIssueUrl = prefilledUrl,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            DumpLogSubmitResult.FallbackReady(
                reason = "Network unreachable (${e.message ?: e.javaClass.simpleName})",
                markdownReport = bundle.markdownReport,
                fullPlainTextDump = bundle.fullPlainTextDump,
                prefilledIssueUrl = prefilledUrl,
            )
        } catch (e: Exception) {
            DumpLogSubmitResult.FallbackReady(
                reason = "Unable to reach issue relay (${e.message ?: e.javaClass.simpleName})",
                markdownReport = bundle.markdownReport,
                fullPlainTextDump = bundle.fullPlainTextDump,
                prefilledIssueUrl = prefilledUrl,
            )
        }
    }

    private fun parseJsonOrNull(raw: String): JSONObject? {
        if (raw.isBlank()) return null
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun mapHttpFailureReason(
        statusCode: Int,
        errorCode: String,
        errorMessage: String,
    ): String {
        return when {
            statusCode == 503 || errorCode == "TOKEN_UNCONFIGURED" ->
                "Server GitHub issue relay token is not configured yet"
            statusCode == 413 ->
                "Dump log exceeded server payload limit (HTTP 413)"
            statusCode == 429 ->
                "Issue relay rate limit reached. Please try again shortly."
            statusCode == 401 || statusCode == 403 ->
                errorMessage.ifBlank { "Issue relay rejected request (HTTP $statusCode)" }
            errorMessage.isNotBlank() ->
                "Issue relay returned HTTP $statusCode: $errorMessage"
            else ->
                "Issue relay returned HTTP $statusCode"
        }
    }

    private fun buildPrefilledIssueUrl(
        title: String,
        category: String,
        description: String,
        specs: DeviceSpecs,
    ): String {
        val sanitizedDiagnostics = LogSanitizer.sanitizeAll(
            MediaDiagnosticsTracker.formatDiagnosticsMarkdown()
        ).trim()
        val sanitizedBreadcrumbs = LogSanitizer.sanitizeAll(
            CrashBreadcrumbs.dump()
        ).trim()

        val compactBody = buildString {
            appendLine("### Device & App Specs")
            appendLine(AppLogCollector.formatDeviceSpecsMarkdown(specs))
            appendLine()
            appendLine("### Failure Reason / Issue Summary")
            appendLine("- **Category**: $category")
            if (description.isNotBlank()) {
                appendLine("- **Description**: ${description.take(1200)}")
            } else {
                appendLine("- **Description**: Please describe what happened and steps to reproduce.")
            }
            appendLine()
            appendLine("### Media Engine Diagnostics")
            appendLine(sanitizedDiagnostics.take(1200))
            if (sanitizedBreadcrumbs.isNotBlank() && sanitizedBreadcrumbs != "No recent actions recorded.") {
                appendLine()
                appendLine("### Recent User Breadcrumbs")
                appendLine("```text")
                appendLine(sanitizedBreadcrumbs.lines().takeLast(20).joinToString("\n").take(1500))
                appendLine("```")
            }
            appendLine()
            appendLine("<!-- Full sanitized dump log copied or ready in-app. Paste logs here if needed. -->")
        }.trim()

        val encodedTitle = urlEncode(title)
        val basePrefix = "$GITHUB_NEW_ISSUE_URL?title=$encodedTitle&body="
        if (basePrefix.length >= MAX_PREFILLED_URL_CHARS) {
            return "$GITHUB_NEW_ISSUE_URL?title=${urlEncode(title.take(100))}"
        }

        val fullCandidate = basePrefix + urlEncode(compactBody)
        if (fullCandidate.length <= MAX_PREFILLED_URL_CHARS) {
            return fullCandidate
        }

        val truncationNote = "\n...(truncated)"
        var low = 0
        var high = compactBody.length
        var bestBody = ""

        while (low <= high) {
            val mid = (low + high) ushr 1
            val safeEnd = adjustForSurrogatePair(compactBody, mid)
            val candidateBody = compactBody.substring(0, safeEnd) + truncationNote
            val encodedCandidate = basePrefix + urlEncode(candidateBody)
            if (encodedCandidate.length <= MAX_PREFILLED_URL_CHARS) {
                bestBody = candidateBody
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return if (bestBody.isNotEmpty()) {
            basePrefix + urlEncode(bestBody)
        } else {
            basePrefix.removeSuffix("&body=")
        }
    }

    private fun adjustForSurrogatePair(text: String, index: Int): Int {
        if (index <= 0 || index >= text.length) return index.coerceIn(0, text.length)
        return if (Character.isHighSurrogate(text[index - 1]) && Character.isLowSurrogate(text[index])) {
            index - 1
        } else {
            index
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}
