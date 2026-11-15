package dev.anilbeesetti.nextplayer.core.data.tdlib

import android.graphics.BitmapFactory
import android.util.Log
import dev.anilbeesetti.nextplayer.core.data.cache.TelegramImageDiskCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class TdlibFileInfo(
    val fileId: Int,
    val fileSize: Long,
    val localPath: String,
    val isCompleted: Boolean,
    val downloadedSize: Long = 0L
)

/**
 * Resolves a Telegram message (chatId, messageId) to a TDLib native file or thumbnail.
 */
@Singleton
class TdlibMediaResolver @Inject constructor(
    private val tdlibManager: TdlibManager,
    private val imageDiskCache: TelegramImageDiskCache,
    private val telegramCacheManager: dev.anilbeesetti.nextplayer.core.data.cache.TelegramCacheManager,
    private val moovPrefetcher: SmartMoovPrefetcher,
    private val thumbnailHelper: TelegramThumbnailHelper
) {
    companion object {
        private const val TAG = "TdlibMediaResolver"
        private const val RESOLVE_TIMEOUT_MS = 15_000L
        private const val THUMB_TIMEOUT_MS = 10_000L
    }

    private val inFlightThumbDownloads = ConcurrentHashMap<Int, CompletableDeferred<String?>>()

    suspend fun resolveFile(
        chatId: Long? = null,
        messageId: Long? = null,
        fileId: String? = null,
        channelUsername: String? = null,
        expectedFileSize: Long = 0L
    ): TdlibFileInfo? = withTimeoutOrNull(RESOLVE_TIMEOUT_MS) {
        if (!tdlibManager.isReady()) {
            Log.d(TAG, "TDLib not ready yet, awaiting MTProto ready state...")
            withTimeoutOrNull(5000L) {
                tdlibManager.authState.filter { it is TdlibManager.AuthState.Ready }.first()
            }
        }
        if (!tdlibManager.isReady()) {
            Log.w(TAG, "TDLib is not ready, cannot resolve media (fileId=$fileId, chatId=$chatId, msgId=$messageId)")
            return@withTimeoutOrNull null
        }

        // Register and await known channel username first to load supergroup access hash
        val resolvedUsername = channelUsername
            ?: (chatId?.let { tdlibManager.getRegisteredChannelUsername(it) })
            ?: (chatId?.let { telegramCacheManager.getChannelUsername(it) })
        if (chatId != null && !resolvedUsername.isNullOrEmpty()) {
            tdlibManager.registerChannelUsername(chatId, resolvedUsername)
        }
        if (!resolvedUsername.isNullOrEmpty() && tdlibManager.getResolvedChat(resolvedUsername) == null && (chatId == null || !tdlibManager.isChannelResolved(chatId))) {
            tdlibManager.searchPublicChatSuspend(resolvedUsername)
        }

        // Ensure supergroup or chat is loaded in TDLib before querying files or messages
        if (chatId != null) {
            tdlibManager.ensureChatSuspend(chatId)
        }

        // 1. Try direct remote file ID resolution (instant MTProto 2.0 mapping)
        if (!fileId.isNullOrEmpty()) {
            val remoteFile = getRemoteFileWithTimeout(fileId)
            if (remoteFile != null) {
                val fileInfo = processFileToTdlibFileInfo(remoteFile, expectedFileSize)
                if (fileInfo != null) {
                    return@withTimeoutOrNull fileInfo
                }
            } else {
                Log.d(TAG, "Remote file ID $fileId returned null from TDLib, falling back to message lookup")
            }
        }

        // 2. Fallback to message lookup if chatId & messageId are present
        if (chatId != null && messageId != null) {
            val message = getMessageWithFallback(chatId, messageId)
            if (message != null) {
                if (message.content is TdApi.MessageVideo) {
                    val v = (message.content as TdApi.MessageVideo).video
                    if (moovPrefetcher.requiresMoovPrefetch(v)) {
                        moovPrefetcher.prefetchAsync(v.video.id, v.video.size)
                    }
                }

                val file: TdApi.File? = when (val content = message.content) {
                    is TdApi.MessageVideo -> content.video.video
                    is TdApi.MessageDocument -> content.document.document
                    is TdApi.MessageAnimation -> content.animation.animation
                    is TdApi.MessageAudio -> content.audio.audio
                    is TdApi.MessageVideoNote -> content.videoNote.video
                    else -> {
                        Log.w(TAG, "Unsupported message content type: ${content.javaClass.simpleName}")
                        null
                    }
                }

                if (file != null) {
                    val fileInfo = processFileToTdlibFileInfo(file, expectedFileSize)
                    if (fileInfo != null) {
                        return@withTimeoutOrNull fileInfo
                    }
                }
            }
        }

        null
    }

    private suspend fun getRemoteFileWithTimeout(fileId: String): TdApi.File? = withTimeoutOrNull(8000L) {
        suspendCancellableCoroutine { cont ->
            tdlibManager.getRemoteFile(fileId) { file ->
                if (cont.isActive) cont.resume(file)
            }
        }
    }

    private fun processFileToTdlibFileInfo(file: TdApi.File, expectedFileSize: Long = 0L): TdlibFileInfo {
        val resolvedSize = when {
            file.size > 0 -> file.size
            expectedFileSize > 0 -> expectedFileSize
            file.expectedSize > 65536L -> file.expectedSize
            else -> 0L
        }

        val localPath = file.local.path
        val isCompleted = file.local.isDownloadingCompleted &&
            (resolvedSize <= 0 || file.local.downloadedSize >= (resolvedSize * 0.95).toLong()) &&
            (localPath.isEmpty() || File(localPath).length() >= (resolvedSize * 0.95).toLong())

        Log.i(TAG, "[TDLIB_RESOLVE] Resolved TDLib file: id=${file.id}, size=$resolvedSize, path=$localPath, completed=$isCompleted")
        tdlibManager.addLog(
            level = "FETCH",
            message = "MTProto Media Resolved: fileId=${file.id}, size=$resolvedSize bytes",
            tag = "FETCH"
        )

        val downloadedBytes = maxOf(file.local.downloadedSize, file.local.downloadedPrefixSize)

        return TdlibFileInfo(
            fileId = file.id,
            fileSize = resolvedSize,
            localPath = localPath,
            isCompleted = isCompleted,
            downloadedSize = downloadedBytes
        )
    }

    private fun isValidImageFile(file: File): Boolean {
        if (!file.exists() || file.length() <= 0) return false
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resolveThumbnail(
        chatId: Long? = null,
        messageId: Long? = null,
        thumbFileId: String? = null
    ): String? = withTimeoutOrNull(THUMB_TIMEOUT_MS) {
        val msgKey = if (chatId != null && messageId != null) "thumb_${chatId}_$messageId" else null
        val fileKey = if (!thumbFileId.isNullOrEmpty()) "thumb_$thumbFileId" else null

        // 1. Fast local disk cache hit (0ms)
        val cached = (msgKey?.let { imageDiskCache.getCachedFile(it) })
            ?: (fileKey?.let { imageDiskCache.getCachedFile(it) })
        if (cached != null) return@withTimeoutOrNull cached.absolutePath

        if (!tdlibManager.isReady()) {
            withTimeoutOrNull(2000L) {
                tdlibManager.authState.filter { it is TdlibManager.AuthState.Ready }.first()
            }
        }
        if (!tdlibManager.isReady()) return@withTimeoutOrNull null

        val cacheKeys = listOfNotNull(msgKey, fileKey)

        // 2. Primary: Message content lookup (chatId & messageId provide exact thumbnail from message)
        if (chatId != null && messageId != null) {
            val resolvedUsername = (chatId.let { tdlibManager.getRegisteredChannelUsername(it) })
                ?: (chatId.let { telegramCacheManager.getChannelUsername(it) })
            if (!resolvedUsername.isNullOrEmpty()) {
                tdlibManager.registerChannelUsername(chatId, resolvedUsername)
                if (tdlibManager.getResolvedChat(resolvedUsername) == null && !tdlibManager.isChannelResolved(chatId)) {
                    tdlibManager.searchPublicChat(resolvedUsername)
                }
            }
            val message = getMessageWithFallback(chatId, messageId)
            if (message != null) {
                // Pre-cache minithumbnail to disk and memory for instant 0ms placeholder
                val mini = thumbnailHelper.extractMinithumbnail(message)
                if (mini != null) {
                    for (k in cacheKeys) {
                        thumbnailHelper.saveMinithumbnailToDisk(k, mini)
                    }
                }

                val thumbFile: TdApi.File? = when (val content = message.content) {
                    is TdApi.MessageVideo -> content.video.thumbnail?.file
                    is TdApi.MessagePhoto -> content.photo.sizes.lastOrNull()?.photo
                    is TdApi.MessageDocument -> content.document.thumbnail?.file
                    is TdApi.MessageAnimation -> content.animation.thumbnail?.file
                    else -> null
                }
                if (thumbFile != null) {
                    val path = downloadThumbFile(thumbFile, cacheKeys)
                    if (path != null) return@withTimeoutOrNull path
                }
            }
        }

        // 3. Fallback: Direct MTProto remote file lookup (only if thumbFileId is a small thumbnail)
        if (!thumbFileId.isNullOrEmpty()) {
            val remoteFile = getRemoteFileWithTimeout(thumbFileId)
            // Safety guard: only treat as thumbnail if size is within reasonable thumbnail bounds (< 2MB)
            if (remoteFile != null && remoteFile.size in 1..(2 * 1024 * 1024L)) {
                val path = downloadThumbFile(remoteFile, cacheKeys)
                if (path != null) return@withTimeoutOrNull path
            }
        }

        null
    }

    private suspend fun downloadThumbFile(thumbFile: TdApi.File, cacheKeys: List<String>): String? {
        if (thumbFile.size > 5 * 1024 * 1024L) {
            // Guard: thumbnail should never be larger than 5MB
            return null
        }

        // 1. Fast local disk cache hit (0ms)
        for (key in cacheKeys) {
            val existing = imageDiskCache.getCachedFile(key)
            if (existing != null && isValidImageFile(existing)) {
                return existing.absolutePath
            }
        }

        // 2. Already downloaded in TDLib local storage
        if (thumbFile.local.isDownloadingCompleted && thumbFile.local.path.isNotEmpty()) {
            val src = File(thumbFile.local.path)
            if (isValidImageFile(src)) {
                var cachedTarget: File? = null
                cacheKeys.forEach { key ->
                    val saved = imageDiskCache.putFile(key, src)
                    if (cachedTarget == null) cachedTarget = saved
                }
                return cachedTarget?.absolutePath ?: src.absolutePath
            }
        }

        // 3. Deduplicate in-flight download so multiple callers wait on the same job
        val deferred = inFlightThumbDownloads.computeIfAbsent(thumbFile.id) { fileId ->
            val d = CompletableDeferred<String?>()
            val listener: (TdApi.File) -> Unit = { f ->
                if (f.id == fileId && f.local.isDownloadingCompleted && f.local.path.isNotEmpty()) {
                    val src = File(f.local.path)
                    val path = if (isValidImageFile(src)) {
                        var cachedTarget: File? = null
                        cacheKeys.forEach { key ->
                            val saved = imageDiskCache.putFile(key, src)
                            if (cachedTarget == null) cachedTarget = saved
                        }
                        cachedTarget?.absolutePath ?: src.absolutePath
                    } else null
                    d.complete(path)
                }
            }
            tdlibManager.registerFileListener(fileId, listener)
            tdlibManager.downloadFile(fileId, offset = 0, limit = 0, priority = 1)
            d.invokeOnCompletion {
                tdlibManager.unregisterFileListener(fileId, listener)
                inFlightThumbDownloads.remove(fileId)
            }
            d
        }

        // 4. Await download completion. Even if UI times out, TDLib completes in background into disk cache!
        return withTimeoutOrNull(THUMB_TIMEOUT_MS) {
            try {
                deferred.await()
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun getMessageWithFallback(chatId: Long, messageId: Long): TdApi.Message? {
        val absId = kotlin.math.abs(chatId)
        val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
        val supergroupChatId = -(1000000000000L + bareId)
        val isKnownChannel = chatId < 0 || absId > 1000000000000L || tdlibManager.getRegisteredChannelUsername(chatId) != null || telegramCacheManager.getChannelUsername(chatId) != null
        val primaryChatId = if (isKnownChannel) supergroupChatId else chatId
        val fallbackChatId = if (primaryChatId == supergroupChatId) chatId else supergroupChatId

        // 1. Ensure primary supergroup/chat is loaded in TDLib
        tdlibManager.ensureChatSuspend(primaryChatId)

        // 2. Query primary chat ID (getMessage handles shiftedId and rawId fallbacks)
        val msg1 = getSingleMessageSuspend(primaryChatId, messageId)
        if (msg1 != null) return msg1

        // 3. Query fallback chat ID if different
        if (fallbackChatId != primaryChatId) {
            tdlibManager.ensureChatSuspend(fallbackChatId)
            val msg2 = getSingleMessageSuspend(fallbackChatId, messageId)
            if (msg2 != null) return msg2
        }

        return null
    }

    private suspend fun getSingleMessageSuspend(chatId: Long, messageId: Long): TdApi.Message? {
        return suspendCancellableCoroutine { cont ->
            tdlibManager.getMessage(chatId, messageId) { msg ->
                if (cont.isActive) cont.resume(msg)
            }
        }
    }
}
