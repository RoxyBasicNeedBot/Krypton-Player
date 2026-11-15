package dev.anilbeesetti.nextplayer.core.data.reporting

import java.util.regex.Pattern

/**
 * Utility that scrubs and sanitizes sensitive user information from system and playback logs
 * before they are exported, shared, or posted to public GitHub issues.
 */
object LogSanitizer {

    private val BEARER_TOKEN_PATTERN = Pattern.compile(
        """(?i)(Bearer\s+)[A-Za-z0-9._~+/-]+=*"""
    )

    private val TELEGRAM_BOT_TOKEN_PATTERN = Pattern.compile(
        """bot[0-9]{8,11}:[a-zA-Z0-9_-]{35}"""
    )

    private val SENSITIVE_QUERY_PARAMS_PATTERN = Pattern.compile(
        """(?i)([?&](?:token|auth|key|secret|apiKey|api_key|access_token|refresh_token|password|signature|sig)=)([^&\s"'>]+)"""
    )

    private val SENSITIVE_JSON_KEYS_PATTERN = Pattern.compile(
        """(?i)("(?:token|password|secret|apiKey|api_key|access_token|auth_token|client_secret)"\s*:\s*")([^"]+)(")"""
    )

    private val AUTHORIZATION_HEADER_PATTERN = Pattern.compile(
        """(?i)(Authorization:\s*)([^\r\n]+)"""
    )

    private val TELEGRAM_AUTH_HASH_PATTERN = Pattern.compile(
        """(?i)(auth_key_id|session_key|secret_key|api_hash)=([a-zA-Z0-9_-]{16,})"""
    )

    /**
     * Sanitizes a single log line, replacing any sensitive tokens, authorization credentials,
     * or private session keys with safe redaction placeholders.
     */
    fun sanitize(line: String): String {
        if (line.isBlank()) return line

        var result = line

        // 1. Redact Authorization headers
        result = AUTHORIZATION_HEADER_PATTERN.matcher(result).replaceAll("$1[REDACTED]")

        // 2. Redact Bearer tokens
        result = BEARER_TOKEN_PATTERN.matcher(result).replaceAll("$1[REDACTED_BEARER_TOKEN]")

        // 3. Redact Telegram Bot tokens
        result = TELEGRAM_BOT_TOKEN_PATTERN.matcher(result).replaceAll("bot[REDACTED_BOT_TOKEN]")

        // 4. Redact sensitive query parameters in URLs
        result = SENSITIVE_QUERY_PARAMS_PATTERN.matcher(result).replaceAll("$1[REDACTED]")

        // 5. Redact sensitive JSON keys
        result = SENSITIVE_JSON_KEYS_PATTERN.matcher(result).replaceAll("$1[REDACTED]$3")

        // 6. Redact Telegram session / auth hashes
        result = TELEGRAM_AUTH_HASH_PATTERN.matcher(result).replaceAll("$1=[REDACTED]")

        return result
    }

    /**
     * Sanitizes a multi-line log string.
     */
    fun sanitizeAll(content: String): String {
        return content.lineSequence().map { sanitize(it) }.joinToString("\n")
    }
}
