package dev.roxy.kryptonplayer.core.data.reporting

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

    private val SENSITIVE_HEADERS_PATTERN = Pattern.compile(
        """(?i)\b((?:(?:Set-)?Cookie|X-Agni-Builder-Token|X-Api-Key|Client-Key):\s*)([^\r\n]+)"""
    )

    private val GITHUB_CLASSIC_TOKEN_PATTERN = Pattern.compile(
        """\bgh[pousr]_[A-Za-z0-9]{20,}\b"""
    )

    private val GITHUB_PAT_PATTERN = Pattern.compile(
        """\bgithub_pat_[A-Za-z0-9_]{20,}\b"""
    )

    private val GOOGLE_API_KEY_PATTERN = Pattern.compile(
        """\bAIza[0-9A-Za-z_-]{35}\b"""
    )

    private val BASIC_AUTH_URL_PATTERN = Pattern.compile(
        """([a-zA-Z][a-zA-Z0-9+.-]*://)([^/\s:@]+:[^/\s@]+@)"""
    )

    private val EMAIL_PATTERN = Pattern.compile(
        """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}\b"""
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

        // 1. Redact Authorization and sensitive custom HTTP headers
        result = AUTHORIZATION_HEADER_PATTERN.matcher(result).replaceAll("$1[REDACTED]")
        result = SENSITIVE_HEADERS_PATTERN.matcher(result).replaceAll("$1[REDACTED]")

        // 2. Redact Bearer tokens
        result = BEARER_TOKEN_PATTERN.matcher(result).replaceAll("$1[REDACTED_BEARER_TOKEN]")

        // 3. Redact GitHub classic and fine-grained PATs
        result = GITHUB_CLASSIC_TOKEN_PATTERN.matcher(result).replaceAll("[REDACTED_GITHUB_TOKEN]")
        result = GITHUB_PAT_PATTERN.matcher(result).replaceAll("[REDACTED_GITHUB_TOKEN]")

        // 4. Redact Google / Gemini API keys
        result = GOOGLE_API_KEY_PATTERN.matcher(result).replaceAll("[REDACTED_API_KEY]")

        // 5. Redact Telegram Bot tokens
        result = TELEGRAM_BOT_TOKEN_PATTERN.matcher(result).replaceAll("bot[REDACTED_BOT_TOKEN]")

        // 6. Redact embedded basic-auth credentials in URLs BEFORE email redaction so host stays intact
        result = BASIC_AUTH_URL_PATTERN.matcher(result).replaceAll("$1[REDACTED_CREDS]@")

        // 7. Redact sensitive query parameters in URLs
        result = SENSITIVE_QUERY_PARAMS_PATTERN.matcher(result).replaceAll("$1[REDACTED]")

        // 8. Redact sensitive JSON keys
        result = SENSITIVE_JSON_KEYS_PATTERN.matcher(result).replaceAll("$1[REDACTED]$3")

        // 9. Redact Telegram session / auth hashes
        result = TELEGRAM_AUTH_HASH_PATTERN.matcher(result).replaceAll("$1=[REDACTED]")

        // 10. Redact email addresses
        result = EMAIL_PATTERN.matcher(result).replaceAll("[REDACTED_EMAIL]")

        return result
    }

    /**
     * Sanitizes a multi-line log string.
     */
    fun sanitizeAll(content: String): String {
        return content.lineSequence().map { sanitize(it) }.joinToString("\n")
    }
}
