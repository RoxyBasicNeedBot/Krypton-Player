package dev.roxy.kryptonplayer.core.data.tdlib

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loopback HTTP gateway (`127.0.0.1`) so `libmpv` can stream Telegram MTProto (`tdlib://`),
 * `/api/stream/`, `/proxy/stream`, offline files, and protected HTTP sources with byte-range seeking.
 */
@Singleton
class TelegramStreamingProxy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tdlibManager: TdlibManager,
    private val tdlibMediaResolver: TdlibMediaResolver,
    private val preferencesRepository: PreferencesRepository,
) : NanoHTTPD("127.0.0.1", 0) {

    companion object {
        private const val TAG = "TelegramStreamingProxy"
        private const val TOKEN_BYTES = 24
    }

    private data class RegisteredEntry(
        val token: String,
        val originalUri: Uri,
        val headers: Map<String, String>,
        @Volatile var cachedProbe: SmartTelegramStreamResolver.ProbedSource? = null,
    )

    private val resolver by lazy {
        SmartTelegramStreamResolver(
            context = context,
            tdlibManager = tdlibManager,
            tdlibMediaResolver = tdlibMediaResolver,
            preferencesRepository = preferencesRepository,
        )
    }
    private val random = SecureRandom()
    private val startLock = Any()
    private val tokenByUriKey = ConcurrentHashMap<String, String>()
    private val entriesByToken = ConcurrentHashMap<String, RegisteredEntry>()

    @Volatile
    private var started = false

    fun ensureStarted(): Int {
        if (started && isAlive) return listeningPort
        synchronized(startLock) {
            if (!started || !isAlive) {
                start(SOCKET_READ_TIMEOUT, false)
                started = true
                Log.i(TAG, "Started TelegramStreamingProxy on 127.0.0.1:$listeningPort")
            }
            return listeningPort
        }
    }

    fun shouldProxy(uri: Uri): Boolean {
        val raw = uri.toString()
        val scheme = uri.scheme?.lowercase().orEmpty()
        return scheme == "tdlib" ||
            raw.contains("/api/stream/") ||
            raw.contains("/proxy/stream") ||
            raw.contains("arch-core") ||
            raw.contains("arch-stream") ||
            raw.contains("workers.dev") ||
            raw.contains("streamtape") ||
            raw.contains("scdn.st")
    }

    fun registerUri(uri: Uri, headers: Map<String, String> = emptyMap()): String {
        if (!shouldProxy(uri)) {
            return uri.toString()
        }
        val port = ensureStarted()
        val uriKey = "${uri}|${headers.entries.sortedBy { it.key }.joinToString()}"
        val existingToken = tokenByUriKey[uriKey]
        if (existingToken != null && entriesByToken.containsKey(existingToken)) {
            val fileName = extractSafeName(uri)
            return "http://127.0.0.1:$port/stream/$existingToken/$fileName"
        }

        val token = generateToken()
        val entry = RegisteredEntry(
            token = token,
            originalUri = uri,
            headers = headers,
        )
        entriesByToken[token] = entry
        tokenByUriKey[uriKey] = token
        val fileName = extractSafeName(uri)
        return "http://127.0.0.1:$port/stream/$token/$fileName"
    }

    fun unregisterUrl(proxyUrl: String) {
        val uri = runCatching { Uri.parse(proxyUrl) }.getOrNull() ?: return
        val segments = uri.pathSegments
        if (segments.size >= 2 && segments[0] == "stream") {
            val token = segments[1]
            val removed = entriesByToken.remove(token) ?: return
            val uriKey = "${removed.originalUri}|${removed.headers.entries.sortedBy { it.key }.joinToString()}"
            tokenByUriKey.remove(uriKey, token)
        }
    }

    fun shutdown() {
        synchronized(startLock) {
            if (started) {
                runCatching { stop() }
                started = false
            }
            entriesByToken.clear()
            tokenByUriKey.clear()
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val method = session.method
        if (method != Method.GET && method != Method.HEAD) {
            return emptyResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", 0L)
        }

        val parts = session.uri.trimStart('/').split('/')
        if (parts.size < 2 || parts[0] != "stream") {
            return emptyResponse(Response.Status.NOT_FOUND, "text/plain", 0L)
        }
        val token = parts[1]
        val entry = entriesByToken[token] ?: return emptyResponse(Response.Status.NOT_FOUND, "text/plain", 0L)

        return try {
            val meta = resolver.extractMetadata(entry.originalUri, entry.headers)
            val probed = entry.cachedProbe ?: resolver.probeSource(meta).also {
                if (it.totalSize > 0L) {
                    entry.cachedProbe = it
                }
            }
            val totalSize = probed.totalSize
            val rangeHeader = session.headers["range"] ?: session.headers["Range"]

            if (method == Method.HEAD) {
                val len = if (totalSize > 0L) totalSize else -1L
                return emptyResponse(Response.Status.OK, probed.mimeType, len).apply {
                    addHeader("Accept-Ranges", "bytes")
                    if (totalSize > 0L) addHeader("Content-Length", totalSize.toString())
                    addHeader("Connection", "close")
                }
            }

            if (rangeHeader != null) {
                if (totalSize > 0L) {
                    val range = HttpByteRange.parse(rangeHeader, totalSize)
                        ?: return emptyResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", 0L).apply {
                            addHeader("Content-Range", "bytes */$totalSize")
                        }
                    val opened = resolver.openStream(meta, range.start, range.endInclusive, probed)
                    val len = if (opened.contentLength > 0L) opened.contentLength else range.length
                    return newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, opened.mimeType, opened.inputStream, len).apply {
                        addHeader("Accept-Ranges", "bytes")
                        addHeader("Content-Range", "bytes ${range.start}-${range.endInclusive}/$totalSize")
                        addHeader("Connection", "close")
                    }
                } else {
                    val unbounded = HttpByteRange.parseUnbounded(rangeHeader)
                    if (unbounded == null) {
                        return emptyResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", 0L)
                    }
                    val (start, requestedLen) = unbounded
                    val end = requestedLen?.let { start + it - 1L }
                    val opened = resolver.openStream(meta, start, end, probed)
                    val response = if (opened.contentLength > 0L) {
                        newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, opened.mimeType, opened.inputStream, opened.contentLength)
                    } else {
                        newChunkedResponse(Response.Status.PARTIAL_CONTENT, opened.mimeType, opened.inputStream)
                    }
                    return response.apply {
                        addHeader("Accept-Ranges", "bytes")
                        if (opened.totalSize > 0L) {
                            addHeader("Content-Range", "bytes $start-${opened.rangeEndInclusive}/${opened.totalSize}")
                        }
                        addHeader("Connection", "close")
                    }
                }
            }

            val opened = resolver.openStream(meta, 0L, null, probed)
            val response = if (opened.contentLength > 0L) {
                newFixedLengthResponse(Response.Status.OK, opened.mimeType, opened.inputStream, opened.contentLength)
            } else {
                newChunkedResponse(Response.Status.OK, opened.mimeType, opened.inputStream)
            }
            response.apply {
                addHeader("Accept-Ranges", "bytes")
                addHeader("Connection", "close")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Proxy stream serve failed for ${entry.originalUri}: ${e.message}", e)
            emptyResponse(Response.Status.INTERNAL_ERROR, "text/plain", 0L)
        }
    }

    private fun emptyResponse(status: Response.IStatus, mimeType: String, contentLength: Long): Response {
        return newFixedLengthResponse(status, mimeType, ByteArrayInputStream(ByteArray(0)), contentLength.coerceAtLeast(0L))
    }

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun extractSafeName(uri: Uri): String {
        val raw = runCatching {
            uri.getQueryParameter("fileName") ?: uri.getQueryParameter("name") ?: uri.lastPathSegment ?: "video.mp4"
        }.getOrDefault("video.mp4")
        val sanitized = raw.replace(Regex("[\\/:*?\"<>|\\r\\n\\t]"), "_").trim().ifBlank { "video.mp4" }
        return runCatching { URLEncoder.encode(sanitized, "UTF-8") }.getOrDefault("video.mp4")
    }
}
