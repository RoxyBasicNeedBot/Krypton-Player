package dev.roxy.kryptonplayer.core.data.reporting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

enum class LogLevel(val priorityLetter: Char, val label: String) {
    VERBOSE('V', "Verbose"),
    DEBUG('D', "Debug"),
    INFO('I', "Info"),
    WARN('W', "Warning"),
    ERROR('E', "Error"),
    ASSERT('A', "Assert");

    companion object {
        fun fromChar(char: Char): LogLevel = when (char) {
            'V' -> VERBOSE
            'D' -> DEBUG
            'I' -> INFO
            'W' -> WARN
            'E' -> ERROR
            'A' -> ASSERT
            else -> DEBUG
        }
    }
}

data class LogcatEntry(
    val raw: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val timestamp: String,
)

/**
 * Utility to dump, parse, sanitize, and filter system and player runtime logs using `logcat -d`.
 */
object LogcatDumper {

    // Regex for standard logcat time format: "04-12 14:22:05.123 D/MyTag( 1234): message"
    private val LOGCAT_TIME_REGEX = Pattern.compile(
        """^([0-9]{2}-[0-9]{2}\s+[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3})\s+([VDIWEA])/([^(]+)(?:\(\s*([0-9]+)\))?:\s*(.*)$"""
    )

    /**
     * Dumps and sanitizes recent system logcat messages asynchronously.
     *
     * @param limit Maximum number of recent log lines to capture (default 1500).
     * @param minLevel Minimum log level priority to include.
     * @param tagFilter Optional tag filter substring.
     */
    suspend fun dumpLogcat(
        limit: Int = 1500,
        minLevel: LogLevel = LogLevel.VERBOSE,
        tagFilter: String? = null,
    ): List<LogcatEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<LogcatEntry>()
        try {
            val process = Runtime.getRuntime().exec("logcat -d -v time")
            val rawLines = BufferedReader(InputStreamReader(process.inputStream)).use { it.readLines() }

            val tailLines = rawLines.takeLast(limit.coerceIn(100, 3000))

            for (rawLine in tailLines) {
                val sanitized = LogSanitizer.sanitize(rawLine)
                val matcher = LOGCAT_TIME_REGEX.matcher(sanitized)

                val entry = if (matcher.matches()) {
                    val timestamp = matcher.group(1) ?: ""
                    val levelChar = matcher.group(2)?.firstOrNull() ?: 'D'
                    val tag = matcher.group(3)?.trim() ?: ""
                    val message = matcher.group(5) ?: ""
                    val level = LogLevel.fromChar(levelChar)
                    LogcatEntry(sanitized, level, tag, message, timestamp)
                } else {
                    LogcatEntry(
                        raw = sanitized,
                        level = LogLevel.DEBUG,
                        tag = "System",
                        message = sanitized,
                        timestamp = "",
                    )
                }

                if (entry.level.ordinal >= minLevel.ordinal) {
                    if (tagFilter.isNullOrBlank() || entry.tag.contains(tagFilter, ignoreCase = true)) {
                        entries.add(entry)
                    }
                }
            }
        } catch (e: Exception) {
            entries.add(
                LogcatEntry(
                    raw = "Unable to dump system logcat: ${e.message}",
                    level = LogLevel.ERROR,
                    tag = "LogcatDumper",
                    message = "Unable to dump system logcat: ${e.message}",
                    timestamp = "",
                )
            )
        }
        entries
    }

    /**
     * Convenience method to return raw sanitized logcat as a single String.
     */
    suspend fun dumpLogcatString(limit: Int = 1500): String = withContext(Dispatchers.IO) {
        val list = dumpLogcat(limit = limit)
        list.joinToString("\n") { it.raw }
    }
}
