package dev.anilbeesetti.nextplayer.core.data.lyrics

object LyricsParser {
    private val TIME_TAG_REGEX = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?\\]")
    private val TTML_P_REGEX = Regex("<p[^>]*begin=[\"']([^\"']+)[\"'][^>]*>(.*?)</p>", RegexOption.IGNORE_CASE)
    private val XML_TAG_REGEX = Regex("<[^>]+>")

    fun parse(content: String): List<LyricsLine> {
        val trimmed = content.trim()
        if (trimmed.startsWith("<") && (trimmed.contains("<tt", ignoreCase = true) || trimmed.contains("<p begin=", ignoreCase = true))) {
            return parseTtml(trimmed)
        }

        val lines = mutableListOf<LyricsLine>()
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isNotBlank()) {
                val matches = TIME_TAG_REGEX.findAll(line).toList()
                if (matches.isNotEmpty()) {
                    val text = line.replace(TIME_TAG_REGEX, "").trim()
                    if (text.isNotBlank()) {
                        for (match in matches) {
                            val (minStr, secStr, msStr) = match.destructured
                            val min = minStr.toLongOrNull() ?: 0L
                            val sec = secStr.toLongOrNull() ?: 0L
                            var ms = msStr.toLongOrNull() ?: 0L
                            if (msStr.length == 2) ms *= 10
                            val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                            lines.add(LyricsLine(totalMs, text))
                        }
                    }
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun parseTtml(ttml: String): List<LyricsLine> {
        val lines = mutableListOf<LyricsLine>()
        TTML_P_REGEX.findAll(ttml).forEach { match ->
            val beginStr = match.groupValues[1]
            val rawText = match.groupValues[2]
            val text = rawText.replace(XML_TAG_REGEX, "").replace("&apos;", "'").replace("&quot;", "\"").replace("&amp;", "&").trim()
            if (text.isNotBlank()) {
                val timeMs = parseTtmlTime(beginStr)
                lines.add(LyricsLine(timeMs, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun parseTtmlTime(timeStr: String): Long {
        val clean = timeStr.trim().removeSuffix("s")
        if (clean.contains(":")) {
            val parts = clean.split(":")
            return when (parts.size) {
                2 -> {
                    val m = parts[0].toLongOrNull() ?: 0L
                    val s = parts[1].toDoubleOrNull() ?: 0.0
                    m * 60_000L + (s * 1000.0).toLong()
                }
                3 -> {
                    val h = parts[0].toLongOrNull() ?: 0L
                    val m = parts[1].toLongOrNull() ?: 0L
                    val s = parts[2].toDoubleOrNull() ?: 0.0
                    h * 3_600_000L + m * 60_000L + (s * 1000.0).toLong()
                }
                else -> 0L
            }
        }
        val sec = clean.toDoubleOrNull() ?: 0.0
        return (sec * 1000.0).toLong()
    }

    fun findCurrentLineIndex(lines: List<LyricsLine>, currentPositionMs: Long, leadMs: Long = 300L): Int {
        if (lines.isEmpty()) return -1
        val target = currentPositionMs + leadMs
        var low = 0
        var high = lines.lastIndex

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midTime = lines[mid].timeMs
            if (midTime <= target) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return high.coerceIn(0, lines.lastIndex)
    }
}
