package dev.roxy.kryptonplayer.core.agni

/**
 * Kotlin facade over KryptonPlayer's Agni native core, transparently falling back to equivalent
 * pure-Kotlin logic on host JVM tests or builds without bundled native binaries.
 */
object AgniCoreEngine {

    fun isNativeActive(): Boolean = AgniNativeBridge.isLoaded()

    fun version(): Int {
        if (!AgniNativeBridge.isLoaded()) return 0
        return runCatching { AgniNativeBridge.nativeVersion() }.getOrDefault(0)
    }

    fun normalizeTitle(raw: String): String {
        if (AgniNativeBridge.isLoaded()) {
            val native = runCatching { AgniNativeBridge.nativeNormalizeTitle(raw) }.getOrNull()
            if (native != null) return native
        }
        return normalizeTitleFallback(raw)
    }

    fun scoreCandidate(query: String, title: String): Int {
        if (AgniNativeBridge.isLoaded()) {
            val native = runCatching { AgniNativeBridge.nativeScoreCandidate(query, title) }.getOrNull()
            if (native != null) return native
        }
        return scoreCandidateFallback(query, title)
    }

    fun sanitizeLog(line: String): String {
        if (AgniNativeBridge.isLoaded()) {
            val native = runCatching { AgniNativeBridge.nativeSanitizeLog(line) }.getOrNull()
            if (native != null) return native
        }
        return sanitizeLogFallback(line)
    }

    internal fun normalizeTitleFallback(raw: String): String {
        if (raw.isEmpty()) return ""
        val out = StringBuilder(raw.length)
        var bracketDepth = 0
        var parenDepth = 0
        var prevSpace = true
        for (ch in raw) {
            when (ch) {
                '[' -> bracketDepth++
                ']' -> if (bracketDepth > 0) bracketDepth--
                '(' -> parenDepth++
                ')' -> if (parenDepth > 0) parenDepth--
                else -> if (bracketDepth == 0 && parenDepth == 0) {
                    if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' ||
                        ch == '.' || ch == '_' || ch == '-' || ch == '/' || ch == '\\'
                    ) {
                        if (!prevSpace) {
                            out.append(' ')
                            prevSpace = true
                        }
                    } else {
                        out.append(if (ch in 'A'..'Z') (ch.code + 32).toChar() else ch)
                        prevSpace = false
                    }
                }
            }
        }
        if (out.isNotEmpty() && out[out.length - 1] == ' ') {
            out.setLength(out.length - 1)
        }
        return out.toString()
    }

    internal fun scoreCandidateFallback(query: String, title: String): Int {
        val q = normalizeTitleFallback(query)
        val t = normalizeTitleFallback(title)
        if (q.isEmpty() || t.isEmpty()) return 0
        if (q == t) return 1000
        if (t.startsWith(q)) return 850
        if (t.contains(q)) return 700

        var matched = 0
        var total = 0
        var pos = 0
        while (pos < q.length) {
            val start = pos
            while (pos < q.length && q[pos] != ' ') {
                pos++
            }
            if (pos > start) {
                total++
                val token = q.substring(start, pos)
                if (t.contains(token)) matched++
            }
            pos++
        }
        return if (total == 0) 0 else (matched * 600) / total
    }

    internal fun sanitizeLogFallback(line: String): String {
        val idx = line.indexOf("Bearer ")
        if (idx < 0) return line
        val start = idx + 7
        var end = start
        while (end < line.length) {
            val c = line[end]
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '"' || c == ',') {
                return line.substring(0, start) + "[REDACTED]" + line.substring(end)
            }
            end++
        }
        return line.substring(0, start) + "[REDACTED]"
    }
}
