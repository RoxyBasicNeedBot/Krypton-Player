package dev.roxy.kryptonplayer.core.data.tdlib

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.roxy.kryptonplayer.core.data.download.TelegramDownloadManager
import dev.roxy.kryptonplayer.core.data.network.NetworkConfig
import dev.roxy.kryptonplayer.core.data.network.models.ChannelPostDto
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.model.Video
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Pure Kotlin stream resolver for Telegram (`tdlib://`), offline cached files, and protected HTTP streams.
 *
 * Resolution priority:
 * 1. Valid local / offline downloaded media files.
 * 2. Direct TDLib MTProto progressive streaming ([TdlibRandomAccessStream]) with [SmartMoovPrefetcher].
 * 3. Resilient HTTP fallback (with `/proxy/stream` headers, `Referer`, and 403/503 desktop UA retry).
 */
class SmartTelegramStreamResolver(
    private val context: Context,
    private val tdlibManager: TdlibManager,
    private val tdlibMediaResolver: TdlibMediaResolver,
    private val preferencesRepository: PreferencesRepository? = null,
    private val okHttpClient: OkHttpClient = defaultHttpClient,
) {

    companion object {
        private const val TAG = "SmartTelegramResolver"
        private const val OFFLINE_FOLDER = "offline_media"
        private const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

        @Volatile
        private var lastWarnedVideoKey: String? = null

        private val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }

        private fun sanitizeFileName(name: String): String {
            return name.replace(Regex("[\\/:*?\"<>|\\r\\n\\t]"), "_").trim()
        }
    }

    data class StreamMetadata(
        val originalUri: Uri,
        val chatId: Long?,
        val messageId: Long?,
        val fileId: String,
        val fileName: String,
        val decodedFileName: String,
        val fallbackUrl: String,
        val paramUsername: String?,
        val effectiveToken: String,
        val knownContentLength: Long,
        val customHeaders: Map<String, String> = emptyMap(),
    )

    data class ResolvedStreamInfo(
        val chatId: Long?,
        val messageId: Long?,
        val fileId: String,
        val fileName: String,
        val fileSize: Long,
        val streamUrl: String,
        val fallbackUrl: String,
    )

    data class ProbedSource(
        val totalSize: Long,
        val mimeType: String,
        val offlineFile: File? = null,
        val tdlibFileId: Int? = null,
        val tdlibLocalPath: String? = null,
        val httpFallbackUrl: String? = null,
    )

    data class OpenStreamResult(
        val inputStream: InputStream,
        val totalSize: Long,
        val contentLength: Long,
        val mimeType: String,
        val isPartial: Boolean,
        val rangeStart: Long,
        val rangeEndInclusive: Long,
    )

    fun extractMetadata(uri: Uri, customHeaders: Map<String, String> = emptyMap()): StreamMetadata {
        val paramSize = runCatching { uri.getQueryParameter("size")?.toLongOrNull() }.getOrNull() ?: -1L
        var knownContentLength = if (paramSize > 0L) paramSize else -1L

        val rawFileName = runCatching {
            uri.getQueryParameter("fileName") ?: uri.getQueryParameter("name") ?: uri.lastPathSegment ?: ""
        }.getOrDefault(uri.lastPathSegment ?: "")

        val fileName = runCatching {
            if (rawFileName.contains("%")) URLDecoder.decode(rawFileName, "UTF-8") else rawFileName
        }.getOrDefault(rawFileName)

        val decodedFileName = runCatching { URLDecoder.decode(fileName, "UTF-8") }.getOrDefault(fileName)
        var fileId = runCatching { uri.getQueryParameter("fileId") ?: "" }.getOrDefault("")
        var chatId = runCatching { uri.getQueryParameter("chatId")?.toLongOrNull() }.getOrNull()
        var messageId = runCatching { uri.getQueryParameter("messageId")?.toLongOrNull() }.getOrNull()
        var fallbackUrl = runCatching { uri.getQueryParameter("fallback") ?: "" }.getOrDefault("")
        val paramUsername = runCatching { uri.getQueryParameter("username") }.getOrNull()
            ?: chatId?.let { tdlibManager.getRegisteredChannelUsername(it) }
        val effectiveToken = runCatching {
            uri.getQueryParameter("token")?.takeIf { it.isNotBlank() }
                ?: preferencesRepository?.playerPreferences?.value?.telegramToken?.takeIf { it.isNotBlank() }
                ?: ""
        }.getOrDefault("")

        if (chatId == null || messageId == null || fileId.isEmpty() || fallbackUrl.isEmpty() || knownContentLength <= 0L) {
            val cached = findMetadataFromCache(fileName, decodedFileName, fileId)
            if (cached != null) {
                if (chatId == null) chatId = cached.chatId
                if (messageId == null) messageId = cached.messageId
                if (fileId.isEmpty()) fileId = cached.fileId
                if (fallbackUrl.isEmpty()) fallbackUrl = cached.fallbackUrl
                if (knownContentLength <= 0L && cached.fileSize > 0L) {
                    knownContentLength = cached.fileSize
                }
            }
        }

        return StreamMetadata(
            originalUri = uri,
            chatId = chatId,
            messageId = messageId,
            fileId = fileId,
            fileName = fileName,
            decodedFileName = decodedFileName,
            fallbackUrl = fallbackUrl,
            paramUsername = paramUsername,
            effectiveToken = effectiveToken,
            knownContentLength = knownContentLength,
            customHeaders = customHeaders,
        )
    }

    fun probeSource(meta: StreamMetadata): ProbedSource {
        val uriString = meta.originalUri.toString()
        val mime = guessMimeType(meta.fileName.ifBlank { uriString })

        // 1. Offline / local file check
        val offlineFile = findValidOfflineFile(meta)
        if (offlineFile != null) {
            return ProbedSource(
                totalSize = offlineFile.length(),
                mimeType = mime,
                offlineFile = offlineFile,
            )
        }

        // 2. Direct TDLib MTProto check
        val canUseTdlib = (uriString.startsWith("tdlib://stream") || uriString.contains("/api/stream/")) &&
            ((meta.chatId != null && meta.messageId != null) || meta.fileId.isNotEmpty()) &&
            tdlibManager.isReady()

        if (canUseTdlib) {
            try {
                val fileInfo = runBlocking {
                    tdlibMediaResolver.resolveFile(
                        meta.chatId,
                        meta.messageId,
                        meta.fileId,
                        meta.paramUsername,
                        expectedFileSize = meta.knownContentLength,
                    )
                }
                if (fileInfo != null) {
                    val effectiveSize = when {
                        fileInfo.fileSize > 65536L -> fileInfo.fileSize
                        meta.knownContentLength > 65536L -> meta.knownContentLength
                        fileInfo.fileSize > 0L -> fileInfo.fileSize
                        meta.knownContentLength > 0L -> meta.knownContentLength
                        else -> 0L
                    }
                    val tdLocal = File(fileInfo.localPath)
                    if (fileInfo.isCompleted &&
                        tdLocal.exists() &&
                        tdLocal.length() >= effectiveSize &&
                        tdLocal.length() >= TelegramDownloadManager.MIN_VALID_VIDEO_SIZE
                    ) {
                        return ProbedSource(
                            totalSize = tdLocal.length(),
                            mimeType = mime,
                            offlineFile = tdLocal,
                        )
                    }
                    return ProbedSource(
                        totalSize = effectiveSize,
                        mimeType = mime,
                        tdlibFileId = fileInfo.fileId,
                        tdlibLocalPath = fileInfo.localPath,
                        httpFallbackUrl = resolveFallbackHttpUrl(meta).takeIf { it.isNotEmpty() },
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "TDLib probe failed, checking fallback HTTP: ${t.message}")
            }
        }

        // 3. HTTP fallback / standard HTTP
        val fallbackUrl = resolveFallbackHttpUrl(meta)
        val targetHttp = when {
            uriString.startsWith("http://") || uriString.startsWith("https://") -> uriString
            fallbackUrl.isNotEmpty() -> fallbackUrl
            else -> ""
        }
        if (targetHttp.isNotEmpty()) {
            val headSize = if (meta.knownContentLength > 0L) meta.knownContentLength else probeHttpContentLength(targetHttp, meta.customHeaders)
            return ProbedSource(
                totalSize = headSize,
                mimeType = mime,
                httpFallbackUrl = targetHttp,
            )
        }

        return ProbedSource(
            totalSize = meta.knownContentLength,
            mimeType = mime,
        )
    }

    @Throws(IOException::class)
    fun openStream(
        meta: StreamMetadata,
        rangeStart: Long,
        rangeEndInclusive: Long?,
        probed: ProbedSource? = null,
    ): OpenStreamResult {
        val resolvedProbe = probed ?: probeSource(meta)
        val start = rangeStart.coerceAtLeast(0L)

        // 1. Offline / local file
        val offline = resolvedProbe.offlineFile
        if (offline != null && offline.exists() && start < offline.length()) {
            val fileLen = offline.length()
            val end = (rangeEndInclusive ?: (fileLen - 1L)).coerceAtMost(fileLen - 1L)
            val contentLen = (end - start + 1L).coerceAtLeast(0L)
            val raf = RandomAccessFile(offline, "r")
            raf.seek(start)
            val stream = BoundedRandomAccessInputStream(raf, contentLen)
            return OpenStreamResult(
                inputStream = stream,
                totalSize = fileLen,
                contentLength = contentLen,
                mimeType = resolvedProbe.mimeType,
                isPartial = start > 0L || end < fileLen - 1L,
                rangeStart = start,
                rangeEndInclusive = end,
            )
        }

        // 2. TDLib progressive MTProto stream
        if (resolvedProbe.tdlibFileId != null && resolvedProbe.tdlibLocalPath != null) {
            val totalSize = resolvedProbe.totalSize
            val lengthToRead = if (rangeEndInclusive != null) {
                (rangeEndInclusive - start + 1L).coerceAtLeast(0L)
            } else if (totalSize > 0L) {
                (totalSize - start).coerceAtLeast(0L)
            } else {
                TdlibRandomAccessStream.LENGTH_UNSET
            }

            tdlibManager.updateStreamingStats(
                fileName = meta.fileName.ifEmpty { "Telegram Video (Msg #${meta.messageId ?: meta.fileId})" },
                chatId = meta.chatId ?: 0L,
                messageId = meta.messageId ?: 0L,
                fileId = resolvedProbe.tdlibFileId.toString(),
                totalBytes = totalSize,
                downloadedBytes = 0L,
                isDirectMtproto = true,
                status = "MTProto Direct Stream",
            )

            val tdStream = TdlibRandomAccessStream(
                tdlibManager = tdlibManager,
                tdlibFileId = resolvedProbe.tdlibFileId,
                totalFileSize = totalSize,
                localFilePath = resolvedProbe.tdlibLocalPath,
            )
            try {
                val openedLen = tdStream.open(offset = start, length = lengthToRead)
                val knownTotal = tdStream.getTotalFileSize().takeIf { it > 0L } ?: totalSize
                val effectiveLen = when {
                    openedLen > 0L -> openedLen
                    knownTotal > start -> knownTotal - start
                    else -> -1L
                }
                val end = when {
                    rangeEndInclusive != null && knownTotal > 0L -> rangeEndInclusive.coerceAtMost(knownTotal - 1L)
                    rangeEndInclusive != null -> rangeEndInclusive
                    knownTotal > 0L -> knownTotal - 1L
                    else -> start
                }
                val rawInput = tdStream.asInputStream()
                val guardedStream = ResilientFailoverInputStream(
                    primary = rawInput,
                    startPosition = start,
                    expectedTotalSize = knownTotal,
                    meta = meta,
                    fallbackOpener = { resumePos -> openHttpFallbackStream(meta, resumePos, rangeEndInclusive) },
                )
                return OpenStreamResult(
                    inputStream = guardedStream,
                    totalSize = knownTotal,
                    contentLength = effectiveLen,
                    mimeType = resolvedProbe.mimeType,
                    isPartial = start > 0L || (knownTotal > 0L && end < knownTotal - 1L),
                    rangeStart = start,
                    rangeEndInclusive = end,
                )
            } catch (e: Throwable) {
                tdStream.close()
                Log.w(TAG, "TDLib stream open failed at $start, trying HTTP fallback: ${e.message}")
            }
        }

        // 3. HTTP fallback / standard HTTP
        return openHttpFallbackStream(meta, start, rangeEndInclusive, resolvedProbe.httpFallbackUrl)
    }

    @Throws(IOException::class)
    private fun openHttpFallbackStream(
        meta: StreamMetadata,
        start: Long,
        rangeEndInclusive: Long?,
        preferredUrl: String? = null,
    ): OpenStreamResult {
        val url = preferredUrl?.takeIf { it.isNotBlank() }
            ?: resolveFallbackHttpUrl(meta).takeIf { it.isNotBlank() }
            ?: meta.originalUri.toString().takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: throw IOException("No playable TDLib or HTTP stream available for ${meta.originalUri}")

        if (meta.originalUri.scheme == "tdlib") {
            notifyFallbackToastOnce(meta, url)
            tdlibManager.updateStreamingStats(
                fileName = meta.fileName.ifEmpty { "Telegram Video (Fallback)" },
                chatId = meta.chatId ?: 0L,
                messageId = meta.messageId ?: 0L,
                fileId = meta.fileId,
                totalBytes = meta.knownContentLength,
                downloadedBytes = 0L,
                isDirectMtproto = false,
                status = "Cloudflare Fallback Stream",
            )
        }

        val response = executeHttpRequestWithProxyHeaders(url, meta.customHeaders, start, rangeEndInclusive)
        if (!response.isSuccessful && response.code != 206) {
            val code = response.code
            response.close()
            throw IOException("HTTP stream error $code for $url")
        }

        val body = response.body ?: run {
            response.close()
            throw IOException("Empty HTTP response body for $url")
        }

        val contentRangeTotal = response.header("Content-Range")
            ?.substringAfter('/')
            ?.toLongOrNull()
        val bodyLen = body.contentLength()
        val totalSize = contentRangeTotal
            ?: if (response.code == 206 && bodyLen > 0L) start + bodyLen else if (bodyLen > 0L) bodyLen else meta.knownContentLength
        val effectiveLen = if (bodyLen > 0L) bodyLen else if (totalSize > start) totalSize - start else -1L
        val end = when {
            rangeEndInclusive != null && totalSize > 0L -> rangeEndInclusive.coerceAtMost(totalSize - 1L)
            totalSize > 0L -> totalSize - 1L
            bodyLen > 0L -> start + bodyLen - 1L
            else -> start
        }

        val guardedStream = SnoopGuardedHttpInputStream(
            response = response,
            sourceStream = body.byteStream(),
            startPosition = start,
        )

        return OpenStreamResult(
            inputStream = guardedStream,
            totalSize = totalSize,
            contentLength = effectiveLen,
            mimeType = guessMimeType(meta.fileName.ifBlank { url }),
            isPartial = response.code == 206 || start > 0L,
            rangeStart = start,
            rangeEndInclusive = end,
        )
    }

    private fun executeHttpRequestWithProxyHeaders(
        url: String,
        extraHeaders: Map<String, String>,
        rangeStart: Long,
        rangeEndInclusive: Long?,
    ): Response {
        val headers = buildProxyHeaders(url, extraHeaders, desktopFallback = false)
        val firstResponse = executeSingleGet(url, headers, rangeStart, rangeEndInclusive)
        if (firstResponse.code == 403 || firstResponse.code == 503) {
            firstResponse.close()
            val retryHeaders = buildProxyHeaders(url, extraHeaders, desktopFallback = true)
            return executeSingleGet(url, retryHeaders, rangeStart, rangeEndInclusive)
        }
        return firstResponse
    }

    private fun executeSingleGet(
        url: String,
        headers: Map<String, String>,
        rangeStart: Long,
        rangeEndInclusive: Long?,
    ): Response {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (k, v) -> builder.header(k, v) }
        if (rangeStart > 0L || rangeEndInclusive != null) {
            val rangeValue = if (rangeEndInclusive != null) "bytes=$rangeStart-$rangeEndInclusive" else "bytes=$rangeStart-"
            builder.header("Range", rangeValue)
        }
        return okHttpClient.newCall(builder.build()).execute()
    }

    private fun probeHttpContentLength(url: String, extraHeaders: Map<String, String>): Long {
        return runCatching {
            val headers = buildProxyHeaders(url, extraHeaders, desktopFallback = false)
            val req = Request.Builder().url(url).head().apply {
                headers.forEach { (k, v) -> header(k, v) }
            }.build()
            okHttpClient.newCall(req).execute().use { resp ->
                resp.header("Content-Length")?.toLongOrNull() ?: -1L
            }
        }.getOrDefault(-1L)
    }

    fun buildProxyHeaders(
        url: String,
        extraHeaders: Map<String, String>,
        desktopFallback: Boolean = false,
    ): Map<String, String> {
        val uri = Uri.parse(url)
        val headers = extraHeaders.toMutableMap()
        if (url.contains("/proxy/stream")) {
            headers["x-api-key"] = "ok223377"
        }
        if (uri.scheme == "http" || uri.scheme == "https") {
            if (desktopFallback) {
                headers["User-Agent"] = DESKTOP_UA
            } else if (!headers.containsKey("User-Agent")) {
                headers["User-Agent"] = MOBILE_UA
            }
            headers.putIfAbsent("Accept", "*/*")
            headers.putIfAbsent("Accept-Language", "en-US,en;q=0.9")
            headers.putIfAbsent("Sec-Fetch-Mode", "no-cors")
            headers.putIfAbsent("Sec-Fetch-Dest", "video")
            val host = uri.host.orEmpty()
            if (host.isNotEmpty()) {
                if (desktopFallback) {
                    headers["Origin"] = "${uri.scheme}://$host"
                }
                if (desktopFallback || !headers.containsKey("Referer")) {
                    headers["Referer"] = if (host.contains("scdn.st") || host.contains("streamtape")) {
                        "https://streamtape.com/"
                    } else {
                        "${uri.scheme}://$host/"
                    }
                }
            }
        }
        return headers
    }

    private fun resolveFallbackHttpUrl(meta: StreamMetadata): String {
        val rawFallback = when {
            meta.fallbackUrl.isNotEmpty() -> meta.fallbackUrl
            TelegramDownloadManager.getFallbackUrl(meta.fileName)?.isNotEmpty() == true ->
                TelegramDownloadManager.getFallbackUrl(meta.fileName)!!
            meta.decodedFileName != meta.fileName && TelegramDownloadManager.getFallbackUrl(meta.decodedFileName)?.isNotEmpty() == true ->
                TelegramDownloadManager.getFallbackUrl(meta.decodedFileName)!!
            meta.fileId.isNotEmpty() && TelegramDownloadManager.getFallbackUrl(meta.fileId)?.isNotEmpty() == true ->
                TelegramDownloadManager.getFallbackUrl(meta.fileId)!!
            meta.fileId.isNotEmpty() -> NetworkConfig.streamUrl(meta.fileId)
            else -> ""
        }

        return if (rawFallback.contains("workers.dev") && !rawFallback.contains("token=") && meta.effectiveToken.isNotBlank()) {
            if (rawFallback.contains("?")) "$rawFallback&token=${meta.effectiveToken}" else "$rawFallback?token=${meta.effectiveToken}"
        } else {
            rawFallback
        }
    }

    private fun notifyFallbackToastOnce(meta: StreamMetadata, resolvedFallback: String) {
        val videoKey = when {
            meta.fileId.isNotEmpty() -> meta.fileId
            meta.chatId != null && meta.messageId != null -> "${meta.chatId}:${meta.messageId}"
            else -> resolvedFallback
        }
        val shouldWarn = synchronized(SmartTelegramStreamResolver::class.java) {
            if (lastWarnedVideoKey != videoKey) {
                lastWarnedVideoKey = videoKey
                true
            } else {
                false
            }
        }
        if (shouldWarn) {
            Handler(Looper.getMainLooper()).post {
                runCatching {
                    Toast.makeText(
                        context,
                        "Streaming via Cloud (slower). Add this channel to get faster direct streaming.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun findValidOfflineFile(meta: StreamMetadata): File? {
        val uriString = meta.originalUri.toString()
        val rawPath = if (uriString.startsWith("file://")) meta.originalUri.path else null
        val decodedPath = runCatching { if (rawPath != null) URLDecoder.decode(rawPath, "UTF-8") else null }.getOrNull()

        val offlineFile = decodedPath?.let { File(it) }?.takeIf { it.exists() }
            ?: rawPath?.let { File(it) }?.takeIf { it.exists() }
            ?: findOfflineFile(meta.fileName, meta.fileId)
            ?: (if (meta.decodedFileName != meta.fileName) findOfflineFile(meta.decodedFileName, meta.fileId) else null)

        if (offlineFile == null) return null
        val isFileInOfflineDir = offlineFile.absolutePath.contains(OFFLINE_FOLDER)
        val isCorruptedStub = offlineFile.length() < TelegramDownloadManager.MIN_VALID_VIDEO_SIZE ||
            offlineFile.name.endsWith(".part") ||
            offlineFile.name.endsWith(".tmp")

        if (isCorruptedStub) {
            if (isFileInOfflineDir && offlineFile.length() < TelegramDownloadManager.MIN_VALID_VIDEO_SIZE && !offlineFile.name.endsWith(".part")) {
                runCatching { offlineFile.delete() }
            }
            return null
        }

        val valid = if (isFileInOfflineDir) {
            offlineFile.length() >= TelegramDownloadManager.MIN_VALID_VIDEO_SIZE && !offlineFile.name.endsWith(".part")
        } else {
            offlineFile.length() > 0L
        }
        return offlineFile.takeIf { valid }
    }

    private fun findMetadataFromCache(fileName: String, decodedFileName: String, fileId: String): ResolvedStreamInfo? {
        if (fileName.isBlank() && decodedFileName.isBlank() && fileId.isBlank()) return null
        return try {
            val cacheDir = File(context.filesDir, "telegram_cache")
            if (!cacheDir.exists()) return null
            val files = cacheDir.listFiles() ?: return null

            val targetNames = mutableSetOf<String>()
            if (fileName.isNotBlank()) {
                val fn = fileName.lowercase()
                targetNames.add(fn)
                targetNames.add(sanitizeFileName(fn))
                targetNames.add(fn.substringBeforeLast("."))
                targetNames.add(sanitizeFileName(fn).substringBeforeLast("."))
            }
            if (decodedFileName.isNotBlank()) {
                val dfn = decodedFileName.lowercase()
                targetNames.add(dfn)
                targetNames.add(sanitizeFileName(dfn))
                targetNames.add(dfn.substringBeforeLast("."))
                targetNames.add(sanitizeFileName(dfn).substringBeforeLast("."))
            }

            for (file in files) {
                if (!file.isFile || !file.name.endsWith(".json")) continue
                if (file.name.startsWith("cached_posts_")) {
                    val json = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: continue
                    val type = object : TypeToken<List<ChannelPostDto>>() {}.type
                    val posts: List<ChannelPostDto>? = runCatching { Gson().fromJson<List<ChannelPostDto>>(json, type) }.getOrNull()
                    posts?.forEach { post ->
                        val postName = post.fileName?.lowercase()
                        val postNameNoExt = postName?.substringBeforeLast(".")
                        val postFileId = post.fileId ?: ""
                        val nameMatches = (postName != null && (targetNames.contains(postName) || (postNameNoExt != null && targetNames.contains(postNameNoExt)))) ||
                            (post.caption != null && targetNames.any { it.length > 3 && post.caption.lowercase().contains(it) })
                        if (nameMatches || (fileId.isNotEmpty() && postFileId == fileId)) {
                            val fId = post.fileId ?: ""
                            val safeName = runCatching { URLEncoder.encode(post.fileName ?: "video.mp4", "UTF-8") }.getOrDefault("video.mp4")
                            val fallback = if (fId.isNotEmpty()) NetworkConfig.streamUrl(fId) else ""
                            val stream = "tdlib://stream?chatId=${post.channelId}&messageId=${post.messageId}&fileId=$fId&size=${post.fileSize ?: 0L}&name=$safeName"
                            if (post.fileName != null) {
                                TelegramDownloadManager.registerFallbackUrl(post.fileName, stream)
                                if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(post.fileName, fallback)
                            }
                            if (fId.isNotEmpty()) {
                                TelegramDownloadManager.registerFallbackUrl(fId, stream)
                                if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(fId, fallback)
                            }
                            return ResolvedStreamInfo(
                                chatId = post.channelId,
                                messageId = post.messageId.toLong(),
                                fileId = fId,
                                fileName = post.fileName ?: fileName,
                                fileSize = post.fileSize ?: 0L,
                                streamUrl = stream,
                                fallbackUrl = fallback,
                            )
                        }
                        post.mediaItems?.forEach { item ->
                            val itemName = item.fileName?.lowercase()
                            val itemNameNoExt = itemName?.substringBeforeLast(".")
                            val itemFileId = item.fileId
                            val itemMatches = (itemName != null && (targetNames.contains(itemName) || (itemNameNoExt != null && targetNames.contains(itemNameNoExt)))) ||
                                (fileId.isNotEmpty() && itemFileId == fileId)
                            if (itemMatches) {
                                val fId = item.fileId
                                val msgId = (item.messageId ?: post.messageId).toLong()
                                val safeName = runCatching { URLEncoder.encode(item.fileName ?: "video.mp4", "UTF-8") }.getOrDefault("video.mp4")
                                val fallback = if (fId.isNotEmpty()) NetworkConfig.streamUrl(fId) else ""
                                val stream = "tdlib://stream?chatId=${post.channelId}&messageId=$msgId&fileId=$fId&size=${item.fileSize ?: 0L}&name=$safeName"
                                if (item.fileName != null) {
                                    TelegramDownloadManager.registerFallbackUrl(item.fileName, stream)
                                    if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(item.fileName, fallback)
                                }
                                if (fId.isNotEmpty()) {
                                    TelegramDownloadManager.registerFallbackUrl(fId, stream)
                                    if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(fId, fallback)
                                }
                                return ResolvedStreamInfo(
                                    chatId = post.channelId,
                                    messageId = msgId,
                                    fileId = fId,
                                    fileName = item.fileName ?: fileName,
                                    fileSize = item.fileSize ?: 0L,
                                    streamUrl = stream,
                                    fallbackUrl = fallback,
                                )
                            }
                        }
                    }
                } else if (file.name.startsWith("cached_cloud_videos_")) {
                    val json = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: continue
                    val type = object : TypeToken<List<Video>>() {}.type
                    val videos: List<Video>? = runCatching { Gson().fromJson<List<Video>>(json, type) }.getOrNull()
                    videos?.forEach { video ->
                        val vName = video.nameWithExtension.lowercase()
                        val vNameNoExt = vName.substringBeforeLast(".")
                        val vExtId = video.externalId ?: ""
                        if (targetNames.contains(vName) || targetNames.contains(vNameNoExt) || (fileId.isNotEmpty() && vExtId == fileId)) {
                            val stream = video.uriString.takeIf { it.startsWith("tdlib") || it.startsWith("http") }
                                ?: video.path.takeIf { it.startsWith("http") }
                                ?: ""
                            val fallback = if (vExtId.isNotEmpty()) NetworkConfig.streamUrl(vExtId) else ""
                            val parsedUri = Uri.parse(video.uriString)
                            val cId = parsedUri.getQueryParameter("chatId")?.toLongOrNull()
                            val mId = parsedUri.getQueryParameter("messageId")?.toLongOrNull()
                            if (video.nameWithExtension.isNotEmpty()) {
                                TelegramDownloadManager.registerFallbackUrl(video.nameWithExtension, stream)
                                if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(video.nameWithExtension, fallback)
                            }
                            if (vExtId.isNotEmpty()) {
                                TelegramDownloadManager.registerFallbackUrl(vExtId, stream)
                                if (fallback.isNotEmpty()) TelegramDownloadManager.registerFallbackUrl(vExtId, fallback)
                            }
                            return ResolvedStreamInfo(
                                chatId = cId,
                                messageId = mId,
                                fileId = vExtId,
                                fileName = video.nameWithExtension,
                                fileSize = video.size,
                                streamUrl = stream,
                                fallbackUrl = fallback,
                            )
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "findMetadataFromCache failed: ${e.message}")
            null
        }
    }

    fun findOfflineFile(fileName: String, fileId: String): File? {
        val candidateNames = mutableListOf<String>()
        if (fileName.isNotBlank()) {
            val decoded = runCatching { URLDecoder.decode(fileName, "UTF-8") }.getOrDefault(fileName)
            val safe = sanitizeFileName(decoded)
            candidateNames.add(safe)
            if (safe != decoded) candidateNames.add(decoded)
            if (fileName != decoded) candidateNames.add(fileName)
            val safeRaw = sanitizeFileName(fileName)
            if (safeRaw != safe) candidateNames.add(safeRaw)
        }
        if (fileId.isNotBlank()) {
            candidateNames.add("$fileId.mp4")
            candidateNames.add(fileId)
        }

        val allCandidates = mutableSetOf<String>()
        for (name in candidateNames) {
            allCandidates.add(name)
            if (!name.endsWith(".mp4", ignoreCase = true)) {
                allCandidates.add("$name.mp4")
            }
        }

        val dirs = listOfNotNull(
            context.getExternalFilesDir(OFFLINE_FOLDER),
            File(context.filesDir, OFFLINE_FOLDER),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "KryptonPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "KryptonPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ArchPlayer"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ArchPlayer"),
        )

        for (dir in dirs) {
            if (!dir.exists()) continue
            for (name in allCandidates) {
                val f = File(dir, name)
                if (f.exists() && f.isFile && !f.name.endsWith(".part") && !f.name.endsWith(".tmp")) {
                    if (f.length() >= TelegramDownloadManager.MIN_VALID_VIDEO_SIZE) {
                        return f
                    }
                }
            }
            val dirFiles = dir.listFiles() ?: continue
            for (file in dirFiles) {
                if (file.isFile && !file.name.endsWith(".part") && !file.name.endsWith(".tmp")) {
                    if (allCandidates.any {
                            it.equals(file.name, ignoreCase = true) ||
                                it.removeSuffix(".mp4").equals(file.name.removeSuffix(".mp4"), ignoreCase = true)
                        }
                    ) {
                        if (file.length() >= TelegramDownloadManager.MIN_VALID_VIDEO_SIZE) {
                            return file
                        }
                    }
                }
            }
        }
        return null
    }

    private fun guessMimeType(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".mkv") -> "video/x-matroska"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".mov") -> "video/quicktime"
            lower.endsWith(".avi") -> "video/x-msvideo"
            lower.endsWith(".ts") -> "video/mp2t"
            lower.endsWith(".m3u8") -> "application/vnd.apple.mpegurl"
            else -> "video/mp4"
        }
    }

    private class BoundedRandomAccessInputStream(
        private val raf: RandomAccessFile,
        private var remaining: Long,
    ) : InputStream() {
        private val oneByte = ByteArray(1)

        override fun read(): Int {
            val n = read(oneByte, 0, 1)
            return if (n < 0) -1 else oneByte[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0L) return -1
            val toRead = minOf(len.toLong(), remaining).toInt()
            val n = raf.read(b, off, toRead)
            if (n > 0) {
                remaining -= n
            }
            return n
        }

        override fun close() {
            runCatching { raf.close() }
        }
    }

    private class SnoopGuardedHttpInputStream(
        private val response: Response,
        private val sourceStream: InputStream,
        private val startPosition: Long,
    ) : InputStream() {
        private var pos = startPosition
        private val oneByte = ByteArray(1)

        override fun read(): Int {
            val n = read(oneByte, 0, 1)
            return if (n < 0) -1 else oneByte[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = sourceStream.read(b, off, len)
            if (n > 0) {
                if (pos == 0L && n >= 2) {
                    val b0 = b[off]
                    val b1 = b[off + 1]
                    val isJsonOrHtml = (b0 == '{'.code.toByte() && (b1 == '"'.code.toByte() || b1 == ' '.code.toByte())) ||
                        b0 == '<'.code.toByte()
                    if (isJsonOrHtml) {
                        val sample = String(b, off, minOf(n, 120))
                        throw IOException("HTTP stream returned JSON/HTML payload instead of media: $sample")
                    }
                }
                pos += n
            }
            return n
        }

        override fun close() {
            runCatching { sourceStream.close() }
            runCatching { response.close() }
        }
    }

    private class ResilientFailoverInputStream(
        private var primary: InputStream,
        startPosition: Long,
        private val expectedTotalSize: Long,
        private val meta: StreamMetadata,
        private val fallbackOpener: (Long) -> OpenStreamResult,
    ) : InputStream() {
        private var currentPos = startPosition
        private var switchedToFallback = false
        private val oneByte = ByteArray(1)

        override fun read(): Int {
            val n = read(oneByte, 0, 1)
            return if (n < 0) -1 else oneByte[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            return try {
                val n = primary.read(b, off, len)
                if (n > 0) {
                    currentPos += n
                    n
                } else if (n < 0 && expectedTotalSize > 0L && currentPos < expectedTotalSize && !switchedToFallback) {
                    switchToFallbackAndRead(b, off, len, IOException("Premature EOF at $currentPos/$expectedTotalSize"))
                } else {
                    n
                }
            } catch (e: IOException) {
                if (switchedToFallback) throw e
                switchToFallbackAndRead(b, off, len, e)
            }
        }

        private fun switchToFallbackAndRead(b: ByteArray, off: Int, len: Int, cause: IOException): Int {
            switchedToFallback = true
            runCatching { primary.close() }
            val fallbackResult = runCatching { fallbackOpener(currentPos) }.getOrElse { throw cause }
            primary = fallbackResult.inputStream
            val n = primary.read(b, off, len)
            if (n > 0) currentPos += n
            return n
        }

        override fun close() {
            runCatching { primary.close() }
        }
    }
}
