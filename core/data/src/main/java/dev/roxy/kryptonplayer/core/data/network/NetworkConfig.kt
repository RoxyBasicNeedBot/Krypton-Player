package dev.roxy.kryptonplayer.core.data.network

import dev.roxy.kryptonplayer.core.data.BuildConfig

/**
 * Centralized network and endpoint configuration.
 * All URLs, API paths, and client headers are resolved from BuildConfig (injected via GitHub Secrets / Environment).
 */
object NetworkConfig {

    const val PRIMARY_GATEWAY_URL: String = "https://your-worker.domain.workers.dev/"
    const val BACKUP_RENDER_URL: String = "https://your-backup.onrender.com/"

    val BASE_URL: String
        get() {
            val raw = BuildConfig.ARCH_API_URL.trim()
            val effective = if (raw.isNotBlank() && !raw.contains("streamlit.app")) {
                raw
            } else {
                PRIMARY_GATEWAY_URL
            }
            return if (effective.endsWith("/")) effective else "$effective/"
        }

    val CLIENT_KEY: String
        get() = BuildConfig.ARCH_CLIENT_KEY.trim()

    val TMDB_BASE_URL: String
        get() = "${BASE_URL}tmdb/"

    val IMDB_BASE_URL: String
        get() = "${BASE_URL}imdb/"

    val UPDATE_URL: String
        get() = "${BASE_URL}api/app/update"

    fun streamUrl(fileId: String, tokenOrParams: String = ""): String {
        val query = when {
            tokenOrParams.isBlank() -> ""
            tokenOrParams.startsWith("?") -> tokenOrParams
            tokenOrParams.startsWith("&") -> "?${tokenOrParams.removePrefix("&")}"
            tokenOrParams.contains("=") -> "?$tokenOrParams"
            else -> "?token=$tokenOrParams"
        }
        return "${BASE_URL}api/stream/$fileId$query"
    }

    fun downloadUrl(fileId: String): String {
        return "${BASE_URL}api/app/download/$fileId"
    }

    fun thumbnailUrl(fileId: String): String {
        return "${BASE_URL}api/thumbnail/$fileId"
    }

    fun tmdbImageUrl(size: String, path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "${BASE_URL}tmdb/image/$size$cleanPath"
    }

    fun imdbImageUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val cleanPath = if (path.startsWith("/")) path.removePrefix("/") else path
        return "${IMDB_BASE_URL}image/$cleanPath"
    }

    fun syncJoinUrl(roomId: String): String {
        return "${BASE_URL}sync/join/$roomId"
    }

    fun isWorkerHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val baseHost = try {
            android.net.Uri.parse(BASE_URL).host
        } catch (_: Exception) {
            null
        }
        return baseHost != null && host.equals(baseHost, ignoreCase = true)
    }

    fun isWorkerUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith(BASE_URL)
    }
}
