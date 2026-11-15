package dev.anilbeesetti.nextplayer.core.data.tdlib

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

data class TdlibLogEntry(
    val timestamp: String,
    val level: String, // "INFO", "SUCCESS", "STREAM", "FETCH", "WARN", "ERROR"
    val tag: String = "CORE",
    val message: String,
    val details: String? = null
)

data class TdlibStreamingStats(
    val fileName: String = "Telegram Video",
    val chatId: Long = 0L,
    val messageId: Long = 0L,
    val fileId: String = "",
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val isDirectMtproto: Boolean = true,
    val status: String = "Fetching Data..."
)

/**
 * Manages the TDLib client lifecycle and Telegram Bot authentication.
 * Uses official TDLib (github.com/tdlib/td) via MTProto 2.0.
 */
@Singleton
class TdlibManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TdlibManager"
        private const val MAX_LOGS = 300
    }

    private var client: Client? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    sealed class AuthState {
        object Idle : AuthState()
        object Initializing : AuthState()
        object Ready : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _liveLogs = MutableStateFlow<List<TdlibLogEntry>>(emptyList())
    val liveLogs: StateFlow<List<TdlibLogEntry>> = _liveLogs.asStateFlow()

    private val _streamingStats = MutableStateFlow<TdlibStreamingStats?>(null)
    val streamingStats: StateFlow<TdlibStreamingStats?> = _streamingStats.asStateFlow()

    private val fileListeners = ConcurrentHashMap<Int, CopyOnWriteArrayList<(TdApi.File) -> Unit>>()
    private val cachedFiles = ConcurrentHashMap<Int, TdApi.File>()
    private val explicitFullDownloads = ConcurrentHashMap.newKeySet<Int>()

    fun markExplicitFullDownload(fileId: Int) {
        explicitFullDownloads.add(fileId)
    }

    fun unmarkExplicitFullDownload(fileId: Int) {
        explicitFullDownloads.remove(fileId)
    }

    fun isExplicitFullDownload(fileId: Int): Boolean = explicitFullDownloads.contains(fileId)

    fun hasActiveFullDownload(): Boolean = explicitFullDownloads.isNotEmpty()

    private var lastBotToken: String = ""
    private var lastApiId: Int = 0
    private var lastApiHash: String = ""

    fun addLog(level: String, message: String, tag: String = "CORE", details: String? = null) {
        // TDLib live logs disabled
    }

    fun updateStreamingStats(
        fileName: String,
        chatId: Long,
        messageId: Long,
        fileId: String,
        totalBytes: Long,
        downloadedBytes: Long,
        isDirectMtproto: Boolean,
        status: String
    ) {
        _streamingStats.value = TdlibStreamingStats(
            fileName = fileName,
            chatId = chatId,
            messageId = messageId,
            fileId = fileId,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            isDirectMtproto = isDirectMtproto,
            status = status
        )
    }

    fun updateStreamingProgress(downloadedBytes: Long, totalBytes: Long) {
        val current = _streamingStats.value ?: return
        _streamingStats.value = current.copy(
            downloadedBytes = downloadedBytes,
            totalBytes = if (totalBytes > 0) totalBytes else current.totalBytes,
            status = if (downloadedBytes >= totalBytes && totalBytes > 0) "Completed" else "Streaming Chunks..."
        )
    }

    fun clearStreamingStats() {
        _streamingStats.value = null
    }

    fun clearLogs() {
        _liveLogs.value = emptyList()
        addLog("INFO", "Logs cleared by user", "SYS")
    }

    fun registerFileListener(fileId: Int, listener: (TdApi.File) -> Unit) {
        val list = fileListeners.computeIfAbsent(fileId) { CopyOnWriteArrayList() }
        list.addIfAbsent(listener)
        cachedFiles[fileId]?.let { listener(it) }
    }

    fun unregisterFileListener(fileId: Int, listener: ((TdApi.File) -> Unit)? = null) {
        if (listener != null) {
            fileListeners[fileId]?.remove(listener)
            if (fileListeners[fileId]?.isEmpty() == true) {
                fileListeners.remove(fileId)
            }
        } else {
            fileListeners.remove(fileId)
        }
    }

    /**
     * Initializes TDLib and authenticates directly as a Bot via MTProto.
     */
    @Synchronized
    fun initialize(botToken: String, apiId: Int, apiHash: String) {
        val effectiveBotToken = botToken.ifBlank { "" }
        val effectiveApiId = if (apiId != 0) apiId else 0
        val effectiveApiHash = apiHash.ifBlank { "" }

        lastBotToken = effectiveBotToken
        lastApiId = effectiveApiId
        lastApiHash = effectiveApiHash

        if (effectiveBotToken.isBlank() || effectiveApiId == 0 || effectiveApiHash.isBlank()) {
            Log.w(TAG, "Cannot init TDLib: missing credentials")
            addLog("WARN", "Cannot init TDLib: Telegram API credentials missing")
            _authState.value = AuthState.Error("Missing Telegram API credentials")
            return
        }

        if (client != null || _authState.value is AuthState.Initializing || _authState.value is AuthState.Ready) {
            Log.d(TAG, "TDLib already initializing or ready, ignoring duplicate call")
            return
        }

        _authState.value = AuthState.Initializing
        addLog("INFO", "Initializing Telegram MTProto 2.0 Engine (API ID: $apiId)...")
        Log.i(TAG, "[TDLIB] Initializing TDLib Direct MTProto client with Bot Token...")

        try {
            try {
                Client.execute(TdApi.SetLogVerbosityLevel(0))
            } catch (e: Throwable) {
                Log.w(TAG, "Native TDLib (libtdjni.so) not available on this device: ${e.message}")
                addLog("WARN", "Native TDLib (libtdjni.so) not loaded. Cloudflare fallback active.")
                _authState.value = AuthState.Error("Native TDLib not loaded")
                return
            }

            client = Client.create({ update ->
                handleUpdate(update, effectiveBotToken, effectiveApiId, effectiveApiHash)
            }, { e ->
                Log.e(TAG, "TDLib update exception", e)
                addLog("ERROR", "TDLib update exception: ${e.message}")
                _authState.value = AuthState.Error("TDLib error: ${e.message}")
            }, { e ->
                Log.e(TAG, "TDLib default exception", e)
                addLog("WARN", "TDLib exception: ${e.message}")
            })
            addLog("SUCCESS", "TDLib native MTProto client instance created")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create TDLib client", e)
            addLog("ERROR", "Failed to create native TDLib client: ${e.message}")
            _authState.value = AuthState.Error("Failed to init TDLib: ${e.message}")
        }
    }

    @Synchronized
    fun reconnect() {
        if (lastBotToken.isNotEmpty() && lastApiId != 0 && lastApiHash.isNotEmpty()) {
            addLog("INFO", "Reconnecting TDLib MTProto session...")
            shutdown()
            initialize(lastBotToken, lastApiId, lastApiHash)
        } else {
            addLog("WARN", "Cannot reconnect: No cached Telegram bot credentials")
        }
    }

    private fun handleUpdate(update: TdApi.Object, botToken: String, apiId: Int, apiHash: String) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> {
                handleAuthState(update.authorizationState, botToken, apiId, apiHash)
            }
            is TdApi.UpdateFile -> {
                val file = update.file
                cachedFiles[file.id] = file
                fileListeners[file.id]?.forEach { it(file) }
                if (file.local.isDownloadingCompleted && (file.size > 250_000L || isExplicitFullDownload(file.id))) {
                    addLog("SUCCESS", "File download completed: id=${file.id}, size=${file.size} bytes")
                }
            }
            else -> {
                val updateName = update::class.java.simpleName
                if (updateName.isNotEmpty() && updateName != "Object") {
                    Log.v(TAG, "TDLib update: $updateName")
                }
            }
        }
    }

    private fun handleAuthState(
        state: TdApi.AuthorizationState,
        botToken: String,
        apiId: Int,
        apiHash: String
    ) {
        val stateName = state::class.java.simpleName
        Log.d(TAG, "TDLib Auth State: $stateName")
        addLog("INFO", "Auth State: $stateName")

        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val tdDirectory = File(context.filesDir, "tdlib_stream")
                if (!tdDirectory.exists()) {
                    tdDirectory.mkdirs()
                }

                val params = TdApi.SetTdlibParameters().apply {
                    databaseDirectory = tdDirectory.absolutePath
                    useMessageDatabase = true
                    useSecretChats = false
                    this.apiId = apiId
                    this.apiHash = apiHash
                    systemLanguageCode = "en"
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                    systemVersion = Build.VERSION.RELEASE
                    applicationVersion = "1.0"
                }

                addLog("INFO", "Sending SetTdlibParameters (dir=${tdDirectory.name})...")
                client?.send(params) { result ->
                    if (result is TdApi.Error) {
                        Log.e(TAG, "SetTdlibParameters error: ${result.code} - ${result.message}")
                        addLog("ERROR", "SetTdlibParameters error: ${result.message}")
                        _authState.value = AuthState.Error("TDLib parameters error: ${result.message}")
                    } else {
                        addLog("SUCCESS", "SetTdlibParameters accepted by TDLib core")
                    }
                }
            }

            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                Log.i(TAG, "Authenticating TDLib as Bot...")
                addLog("INFO", "Authenticating Bot token with Telegram MTProto servers...")
                client?.send(TdApi.CheckAuthenticationBotToken(botToken)) { result ->
                    if (result is TdApi.Error) {
                        Log.e(TAG, "Bot auth error: ${result.code} - ${result.message}")
                        addLog("ERROR", "Bot auth failed: ${result.message}")
                        _authState.value = AuthState.Error("Bot auth failed: ${result.message}")
                    } else {
                        addLog("SUCCESS", "Bot token verified by Telegram MTProto")
                    }
                }
            }

            is TdApi.AuthorizationStateReady -> {
                Log.i(TAG, "[TDLIB] Bot authenticated successfully! Direct MTProto streaming READY.")
                addLog("SUCCESS", "TDLib Ready! Direct MTProto 2.0 streaming & downloads ACTIVE (0 Server Bandwidth)")
                _authState.value = AuthState.Ready

                // Unlock maximum high-speed network bandwidth in TDLib MTProto engine (Nagram/CherryGram boost)
                optimizeNetworkForDownload()
                client?.send(TdApi.SetOption("storage_max_files_size", TdApi.OptionValueInteger(50L * 1024 * 1024 * 1024L)), null)
                client?.send(TdApi.SetOption("storage_max_file_count", TdApi.OptionValueInteger(100000L)), null)
                client?.send(TdApi.SetOption("storage_immunity_delay", TdApi.OptionValueInteger(86400L)), null)
                client?.send(TdApi.SetOption("reuse_uploaded_photos_by_hash", TdApi.OptionValueBoolean(true)), null)
                addLog("SUCCESS", "TDLib MTProto high-speed unmetered mode & booster options enabled")

                // Channels will be resolved on-demand when selected or when media is loaded

                try {
                    client?.send(TdApi.LoadChats(TdApi.ChatListMain(), 100)) { result ->
                        if (result is TdApi.Ok) {
                            addLog("INFO", "Preloaded recent Telegram chats into TDLib")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "LoadChats error: ${e.message}")
                }
            }

            is TdApi.AuthorizationStateClosed -> {
                Log.i(TAG, "TDLib session closed")
                addLog("WARN", "TDLib session closed")
                _authState.value = AuthState.Idle
                client = null
            }
        }
    }

    private val channelUsernames = java.util.concurrent.ConcurrentHashMap<Long, String>()
    private val resolvedPublicChats = java.util.concurrent.ConcurrentHashMap<String, TdApi.Chat>()
    private val resolvedChatsById = java.util.concurrent.ConcurrentHashMap<Long, TdApi.Chat>()
    private val failedPublicChatSearches = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val inFlightPublicChatSearches = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<(TdApi.Chat?) -> Unit>>()
    private val failedChats = java.util.concurrent.ConcurrentHashMap<Long, Long>()

    private fun cacheResolvedChat(chat: TdApi.Chat) {
        resolvedChatsById[chat.id] = chat
        val absId = kotlin.math.abs(chat.id)
        val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
        val fullId = 1000000000000L + bareId
        resolvedChatsById[bareId] = chat
        resolvedChatsById[-bareId] = chat
        resolvedChatsById[fullId] = chat
        resolvedChatsById[-fullId] = chat
    }

    fun registerChannelUsername(channelId: Long, username: String) {
        val clean = username.removePrefix("@").removePrefix("https://t.me/").trim()
        if (clean.isNotEmpty()) {
            val absId = kotlin.math.abs(channelId)
            val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
            val fullId = 1000000000000L + bareId
            channelUsernames[bareId] = clean
            channelUsernames[-bareId] = clean
            channelUsernames[fullId] = clean
            channelUsernames[-fullId] = clean
            channelUsernames[channelId] = clean
            channelUsernames[-channelId] = clean
        }
    }

    fun getRegisteredChannelUsername(channelId: Long): String? {
        channelUsernames[channelId]?.let { return it }
        channelUsernames[-channelId]?.let { return it }
        val absId = kotlin.math.abs(channelId)
        val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
        val fullId = 1000000000000L + bareId
        channelUsernames[bareId]?.let { return it }
        channelUsernames[-bareId]?.let { return it }
        channelUsernames[fullId]?.let { return it }
        channelUsernames[-fullId]?.let { return it }
        return null
    }

    fun getResolvedChat(username: String): TdApi.Chat? {
        val clean = username.removePrefix("@").removePrefix("https://t.me/").trim().lowercase()
        return resolvedPublicChats[clean]
    }

    fun getResolvedChat(channelId: Long): TdApi.Chat? {
        resolvedChatsById[channelId]?.let { return it }
        val absId = kotlin.math.abs(channelId)
        val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
        val fullId = 1000000000000L + bareId
        resolvedChatsById[bareId]?.let { return it }
        resolvedChatsById[-bareId]?.let { return it }
        resolvedChatsById[fullId]?.let { return it }
        resolvedChatsById[-fullId]?.let { return it }
        return null
    }

    fun isChannelResolved(channelId: Long): Boolean {
        if (getResolvedChat(channelId) != null) return true
        val u = getRegisteredChannelUsername(channelId)
        return !u.isNullOrEmpty() && getResolvedChat(u) != null
    }

    /**
     * Ensures that a chat exists and is loaded in TDLib's internal database cache.
     * Prevents 'Chat not found' errors when querying messages.
     */
    fun ensureChat(chatId: Long, forceRefresh: Boolean = false, callback: ((Boolean) -> Unit)? = null) {
        val c = client
        if (c == null || _authState.value != AuthState.Ready) {
            callback?.invoke(false)
            return
        }

        if (!forceRefresh) {
            if (getResolvedChat(chatId) != null) {
                callback?.invoke(true)
                return
            }
            val lastFailed = failedChats[chatId]
            if (lastFailed != null && System.currentTimeMillis() - lastFailed < 5_000L) {
                callback?.invoke(false)
                return
            }
        } else {
            val absId = kotlin.math.abs(chatId)
            val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
            val fullId = 1000000000000L + bareId
            resolvedChatsById.remove(chatId)
            resolvedChatsById.remove(-chatId)
            resolvedChatsById.remove(bareId)
            resolvedChatsById.remove(-bareId)
            resolvedChatsById.remove(fullId)
            resolvedChatsById.remove(-fullId)
            failedChats.remove(chatId)
            failedChats.remove(-chatId)
            failedChats.remove(bareId)
            failedChats.remove(-bareId)
        }

        val knownUsername = getRegisteredChannelUsername(chatId)
        if (!knownUsername.isNullOrEmpty()) {
            val key = knownUsername.lowercase()
            if (!forceRefresh && resolvedPublicChats.containsKey(key)) {
                callback?.invoke(true)
                return
            }
            searchPublicChat(knownUsername, forceRefresh = forceRefresh) { pubChat ->
                if (pubChat != null) {
                    failedChats.remove(chatId)
                    addLog("SUCCESS", "Resolved supergroup chat via public username: @$knownUsername (id=$chatId)")
                    callback?.invoke(true)
                } else {
                    checkChatOrContinue(chatId, callback)
                }
            }
        } else {
            checkChatOrContinue(chatId, callback)
        }
    }

    suspend fun ensureChatSuspend(chatId: Long, forceRefresh: Boolean = false): Boolean = withTimeoutOrNull(8000L) {
        if (!isReady()) {
            withTimeoutOrNull(4000L) {
                authState.filter { it is AuthState.Ready }.first()
            }
        }
        if (!isReady()) return@withTimeoutOrNull false

        suspendCancellableCoroutine { cont ->
            ensureChat(chatId, forceRefresh) { ok ->
                if (cont.isActive) cont.resume(ok)
            }
        }
    } ?: false

    private fun checkChatOrContinue(chatId: Long, callback: ((Boolean) -> Unit)?) {
        val c = client ?: run {
            callback?.invoke(false)
            return
        }
        c.send(TdApi.GetChat(chatId)) { res ->
            if (res is TdApi.Chat) {
                cacheResolvedChat(res)
                failedChats.remove(chatId)
                callback?.invoke(true)
            } else {
                continueChatCreation(chatId, callback)
            }
        }
    }

    private fun continueChatCreation(chatId: Long, callback: ((Boolean) -> Unit)?) {
        val c = client ?: run {
            callback?.invoke(false)
            return
        }
        val absId = kotlin.math.abs(chatId)
        val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
        val isSupergroup = (absId > 1000000000000L) || (getRegisteredChannelUsername(chatId) != null)

        if (isSupergroup) {
            c.send(TdApi.CreateSupergroupChat(bareId, false)) { res2 ->
                if (res2 is TdApi.Chat) {
                    val chat = res2 as TdApi.Chat
                    cacheResolvedChat(chat)
                    failedChats.remove(chatId)
                    failedChats.remove(-chatId)
                    failedChats.remove(bareId)
                    failedChats.remove(-bareId)
                    addLog("SUCCESS", "Fetched supergroup/channel from server: $bareId")
                    callback?.invoke(true)
                } else {
                    // Fallback with force = true: create local chat entity in TDLib and query server info
                    c.send(TdApi.CreateSupergroupChat(bareId, true)) { res3 ->
                        if (res3 is TdApi.Chat) {
                            val chat = res3 as TdApi.Chat
                            cacheResolvedChat(chat)
                            failedChats.remove(chatId)
                            failedChats.remove(-chatId)
                            failedChats.remove(bareId)
                            failedChats.remove(-bareId)
                            addLog("SUCCESS", "Created supergroup/channel entity (forced): $bareId (chatId=${chat.id})")
                            c.send(TdApi.GetSupergroup(bareId), null)
                            c.send(TdApi.GetSupergroupFullInfo(bareId), null)
                            callback?.invoke(true)
                        } else {
                            failedChats[chatId] = System.currentTimeMillis()
                            callback?.invoke(false)
                        }
                    }
                }
            }
        } else if (chatId > 0) {
            // Private chat with a user (e.g. Bot <-> User chat)
            c.send(TdApi.CreatePrivateChat(chatId, false)) { createRes ->
                if (createRes is TdApi.Chat) {
                    cacheResolvedChat(createRes)
                    failedChats.remove(chatId)
                    addLog("SUCCESS", "Fetched private chat from server: $chatId")
                    callback?.invoke(true)
                } else {
                    c.send(TdApi.CreatePrivateChat(chatId, true)) { res2 ->
                        val ok = res2 is TdApi.Chat
                        if (ok) {
                            cacheResolvedChat(res2 as TdApi.Chat)
                            failedChats.remove(chatId)
                            addLog("SUCCESS", "Created private chat in TDLib: $chatId")
                        } else {
                            failedChats[chatId] = System.currentTimeMillis()
                        }
                        callback?.invoke(ok)
                    }
                }
            }
        } else if (chatId < 0) {
            // Basic group
            c.send(TdApi.CreateBasicGroupChat(absId, false)) { createRes ->
                if (createRes is TdApi.Chat) {
                    cacheResolvedChat(createRes)
                    failedChats.remove(chatId)
                    callback?.invoke(true)
                } else {
                    c.send(TdApi.CreateBasicGroupChat(absId, true)) { res2 ->
                        val ok = res2 is TdApi.Chat
                        if (ok) {
                            cacheResolvedChat(res2 as TdApi.Chat)
                            failedChats.remove(chatId)
                        } else {
                            failedChats[chatId] = System.currentTimeMillis()
                        }
                        callback?.invoke(ok)
                    }
                }
            }
        } else {
            callback?.invoke(false)
        }
    }

    /**
     * Searches and resolves a public channel or group by username (e.g. @channel or channel),
     * ensuring TDLib caches the chat, supergroup info, and access hash for subsequent calls.
     * Caches resolved chats in memory and deduplicates concurrent in-flight requests.
     */
    fun searchPublicChat(username: String, forceRefresh: Boolean = false, callback: ((TdApi.Chat?) -> Unit)? = null) {
        val clean = username.removePrefix("@").removePrefix("https://t.me/").trim()
        if (clean.isEmpty()) {
            callback?.invoke(null)
            return
        }
        val key = clean.lowercase()

        // 1. Fast memory cache hit (0ms) unless force refresh is explicitly requested
        if (!forceRefresh) {
            resolvedPublicChats[key]?.let { cachedChat ->
                callback?.invoke(cachedChat)
                return
            }

            // Negative cache hit: avoid hammering TDLib with failed public chat lookups within 10s
            val lastFailed = failedPublicChatSearches[key]
            if (lastFailed != null && (System.currentTimeMillis() - lastFailed < 10_000L)) {
                callback?.invoke(null)
                return
            }
        } else {
            resolvedPublicChats.remove(key)
            failedPublicChatSearches.remove(key)
        }

        val c = client
        if (c == null || _authState.value != AuthState.Ready) {
            callback?.invoke(null)
            return
        }

        // 2. In-flight request deduplication: queue subsequent concurrent callers
        var isFirstCaller = false
        synchronized(inFlightPublicChatSearches) {
            val pendingList = inFlightPublicChatSearches[key]
            if (pendingList != null) {
                if (callback != null) pendingList.add(callback)
                return
            } else {
                val newList = java.util.concurrent.CopyOnWriteArrayList<(TdApi.Chat?) -> Unit>()
                if (callback != null) newList.add(callback)
                inFlightPublicChatSearches[key] = newList
                isFirstCaller = true
            }
        }

        if (!isFirstCaller) return

        c.send(TdApi.SearchPublicChat(clean)) { res ->
            val pendingCallbacks = synchronized(inFlightPublicChatSearches) {
                inFlightPublicChatSearches.remove(key)
            }
            if (res is TdApi.Chat) {
                failedPublicChatSearches.remove(key)
                addLog("SUCCESS", "Resolved public channel: @$clean (id=${res.id})")
                resolvedPublicChats[key] = res
                cacheResolvedChat(res)
                registerChannelUsername(res.id, clean)
                pendingCallbacks?.forEach { cb ->
                    try { cb(res) } catch (e: Exception) { Log.e(TAG, "Callback error in searchPublicChat", e) }
                }
            } else {
                failedPublicChatSearches[key] = System.currentTimeMillis()
                val err = res as? TdApi.Error
                if (err != null) {
                    Log.w(TAG, "SearchPublicChat failed for @$clean: ${err.code} - ${err.message}")
                }
                pendingCallbacks?.forEach { cb ->
                    try { cb(null) } catch (e: Exception) { Log.e(TAG, "Callback error in searchPublicChat", e) }
                }
            }
        }
    }

    suspend fun searchPublicChatSuspend(username: String, forceRefresh: Boolean = false): TdApi.Chat? = withTimeoutOrNull(5000L) {
        val clean = username.removePrefix("@").removePrefix("https://t.me/").trim()
        if (clean.isEmpty()) return@withTimeoutOrNull null

        val key = clean.lowercase()
        if (!forceRefresh) {
            resolvedPublicChats[key]?.let { return@withTimeoutOrNull it }
        }

        if (!isReady()) {
            withTimeoutOrNull(4000L) {
                authState.filter { it is AuthState.Ready }.first()
            }
        }
        if (!isReady()) return@withTimeoutOrNull null

        suspendCancellableCoroutine { cont ->
            searchPublicChat(clean, forceRefresh) { chat ->
                if (cont.isActive) cont.resume(chat)
            }
        }
    }

    /**
     * Resolves a Telegram remote file ID (e.g. BQACAgU...) directly into a TDLib File object.
     * Modeled after Nagram and CherryGram pure MTProto resolution:
     * Tries untyped FileType (Temp) first to avoid type mismatch restrictions, then falls back to Video and Document.
     * Sanitizes Base64URL encoding so '+' and '/' are converted to '-' and '_', testing unpadded and padded variants.
     */
    fun getRemoteFile(remoteFileId: String, callback: ((TdApi.File?) -> Unit)? = null) {
        val c = client
        if (c == null || _authState.value != AuthState.Ready) {
            addLog("WARN", "getRemoteFile failed: TDLib not ready")
            callback?.invoke(null)
            return
        }

        val trimmed = remoteFileId.trim()
        val urlSafeClean = trimmed.replace(' ', '-').replace('+', '-').replace('/', '_').trimEnd('=')
        val padded = when (urlSafeClean.length % 4) {
            2 -> urlSafeClean + "=="
            3 -> urlSafeClean + "="
            else -> urlSafeClean
        }

        // 1. Try untyped with clean unpadded URL-safe Base64 (FileType::Temp accepts any file type in TDLib)
        c.send(TdApi.GetRemoteFile(urlSafeClean, null)) { result ->
            if (result is TdApi.File) {
                addLog("SUCCESS", "Resolved remote file: id=${result.id}, size=${result.size}")
                callback?.invoke(result)
            } else {
                // 2. Try untyped with padded URL-safe Base64
                c.send(TdApi.GetRemoteFile(padded, null)) { padResult ->
                    if (padResult is TdApi.File) {
                        addLog("SUCCESS", "Resolved remote file (padded): id=${padResult.id}, size=${padResult.size}")
                        callback?.invoke(padResult)
                    } else {
                        // 3. Try explicitly as FileTypeVideo
                        c.send(TdApi.GetRemoteFile(urlSafeClean, TdApi.FileTypeVideo())) { vidResult ->
                            if (vidResult is TdApi.File) {
                                addLog("SUCCESS", "Resolved remote video file: id=${vidResult.id}, size=${vidResult.size}")
                                callback?.invoke(vidResult)
                            } else {
                                // 4. Try explicitly as FileTypeDocument
                                c.send(TdApi.GetRemoteFile(urlSafeClean, TdApi.FileTypeDocument())) { docResult ->
                                    if (docResult is TdApi.File) {
                                        addLog("SUCCESS", "Resolved remote document file: id=${docResult.id}, size=${docResult.size}")
                                        callback?.invoke(docResult)
                                    } else {
                                        val err = docResult as? TdApi.Error ?: vidResult as? TdApi.Error ?: padResult as? TdApi.Error ?: result as? TdApi.Error
                                        Log.w(TAG, "GetRemoteFile error ($urlSafeClean): ${err?.code} - ${err?.message}")
                                        addLog("WARN", "GetRemoteFile error: ${err?.message}")
                                        callback?.invoke(null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun getMessage(chatId: Long, messageId: Long, callback: ((TdApi.Message?) -> Unit)? = null) {
        val c = client
        if (c == null || _authState.value != AuthState.Ready) {
            addLog("WARN", "getMessage failed: TDLib not ready (chatId=$chatId, messageId=$messageId)")
            callback?.invoke(null)
            return
        }

        val isAlreadyShifted = (messageId >= 1_048_576L) && ((messageId and 0xFFFFFL) == 0L)
        val shiftedId = if (isAlreadyShifted) messageId else (messageId shl 20)
        val rawId = if (isAlreadyShifted) (messageId shr 20) else messageId

        val absId = kotlin.math.abs(chatId)
        val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
        val isSupergroup = (absId > 1000000000000L) || (getRegisteredChannelUsername(chatId) != null)
        val effectiveChatId = if (isSupergroup && chatId > 0) {
            -(1000000000000L + bareId)
        } else {
            chatId
        }

        fun tryRawIdFallbackOrError(errorMsg: String, onComplete: () -> Unit) {
            if (shiftedId != rawId) {
                c.send(TdApi.GetMessage(effectiveChatId, rawId)) { rawRes ->
                    if (rawRes is TdApi.Message) {
                        addLog("SUCCESS", "Resolved Telegram message with raw ID: $effectiveChatId / $rawId")
                        callback?.invoke(rawRes)
                    } else {
                        addLog("ERROR", "GetMessage error ($effectiveChatId, $shiftedId/$rawId): $errorMsg")
                        onComplete()
                    }
                }
            } else {
                addLog("ERROR", "GetMessage error ($effectiveChatId, $shiftedId): $errorMsg")
                onComplete()
            }
        }

        c.send(TdApi.GetMessage(effectiveChatId, shiftedId)) { result ->
            when (result) {
                is TdApi.Message -> {
                    addLog("SUCCESS", "Resolved Telegram message: $effectiveChatId / $shiftedId")
                    callback?.invoke(result)
                }
                is TdApi.Error -> {
                    val isChannelInvalid = result.message.contains("CHANNEL_INVALID", ignoreCase = true)
                    val isChatNotFound = result.message.contains("Chat not found", ignoreCase = true)
                    if (isChannelInvalid) {
                        val knownUsername = getRegisteredChannelUsername(effectiveChatId)
                        if (!knownUsername.isNullOrEmpty()) {
                            addLog("INFO", "CHANNEL_INVALID for chat $effectiveChatId. Re-resolving public chat @$knownUsername...")
                            searchPublicChat(knownUsername, forceRefresh = true) { chat ->
                                if (chat != null) {
                                    c.send(TdApi.GetMessage(chat.id, shiftedId)) { retryResult ->
                                        if (retryResult is TdApi.Message) {
                                            addLog("SUCCESS", "Resolved Telegram message after username sync: ${chat.id} / $shiftedId")
                                            callback?.invoke(retryResult)
                                        } else if (shiftedId != rawId) {
                                            c.send(TdApi.GetMessage(chat.id, rawId)) { rawRetry ->
                                                if (rawRetry is TdApi.Message) {
                                                    addLog("SUCCESS", "Resolved Telegram message after username sync (raw ID): ${chat.id} / $rawId")
                                                    callback?.invoke(rawRetry)
                                                } else {
                                                    callback?.invoke(null)
                                                }
                                            }
                                        } else {
                                            callback?.invoke(null)
                                        }
                                    }
                                } else {
                                    addLog("ERROR", "Failed to resolve public chat @$knownUsername on CHANNEL_INVALID")
                                    callback?.invoke(null)
                                }
                            }
                        } else {
                            Log.e(TAG, "GetMessage CHANNEL_INVALID for $effectiveChatId without known username")
                            addLog("ERROR", "GetMessage error ($effectiveChatId, $shiftedId): ${result.message}")
                            callback?.invoke(null)
                        }
                    } else if (isChatNotFound) {
                        val knownUsername = getRegisteredChannelUsername(effectiveChatId)
                        if (!knownUsername.isNullOrEmpty()) {
                            searchPublicChat(knownUsername, forceRefresh = false) { pubChat ->
                                if (pubChat != null) {
                                    c.send(TdApi.GetMessage(pubChat.id, shiftedId)) { retryResult ->
                                        if (retryResult is TdApi.Message) {
                                            addLog("SUCCESS", "Resolved Telegram message after username sync: ${pubChat.id} / $shiftedId")
                                            callback?.invoke(retryResult)
                                        } else if (shiftedId != rawId) {
                                            c.send(TdApi.GetMessage(pubChat.id, rawId)) { rawRetry ->
                                                if (rawRetry is TdApi.Message) {
                                                    addLog("SUCCESS", "Resolved Telegram message after username sync (raw ID): ${pubChat.id} / $rawId")
                                                    callback?.invoke(rawRetry)
                                                } else {
                                                    callback?.invoke(null)
                                                }
                                            }
                                        } else {
                                            callback?.invoke(null)
                                        }
                                    }
                                } else {
                                    callback?.invoke(null)
                                }
                            }
                        } else {
                            addLog("INFO", "Chat $effectiveChatId not found ($effectiveChatId, $shiftedId): ${result.message}. Ensuring chat is loaded...")
                            ensureChat(effectiveChatId, forceRefresh = true) { loaded ->
                                if (loaded) {
                                    c.send(TdApi.GetMessage(effectiveChatId, shiftedId)) { retryResult ->
                                        if (retryResult is TdApi.Message) {
                                            addLog("SUCCESS", "Resolved Telegram message after chat sync: $effectiveChatId / $shiftedId")
                                            callback?.invoke(retryResult)
                                        } else if (shiftedId != rawId) {
                                            c.send(TdApi.GetMessage(effectiveChatId, rawId)) { rawRetry ->
                                                if (rawRetry is TdApi.Message) {
                                                    addLog("SUCCESS", "Resolved Telegram message after chat sync (raw ID): $effectiveChatId / $rawId")
                                                    callback?.invoke(rawRetry)
                                                } else {
                                                    callback?.invoke(null)
                                                }
                                            }
                                        } else {
                                            callback?.invoke(null)
                                        }
                                    }
                                } else {
                                    tryRawIdFallbackOrError(result.message) { callback?.invoke(null) }
                                }
                            }
                        }
                    } else {
                        // Not chat-not-found; e.g. message not found with shiftedId
                        tryRawIdFallbackOrError(result.message) { callback?.invoke(null) }
                    }
                }
                else -> callback?.invoke(null)
            }
        }
    }

    /**
     * Applies peak performance MTProto settings for high-speed file transfers and live streaming.
     * Modeled after Nagram and CherryGram download acceleration tricks:
     * 1. SetNetworkType(WiFi) removes cellular throttling and unlocks maximum chunk sizes.
     * 2. use_pfs = false removes Diffie-Hellman PFS re-handshake latency.
     * 3. use_quick_ack = true speeds up chunk ACKs.
     * 4. use_storage_optimizer = true avoids cache thrashing.
     * 5. ignore_background_updates = true dedicates TCP streams to media downloading.
     * 6. disable_persistent_network_statistics = true stops disk write overhead per chunk.
     * 7. prefer_ipv6 = false avoids mobile carrier IPv6 routing bottlenecks.
     * 8. storage_max_files_size & storage_immunity_delay prevent eviction of downloading parts.
     */
    fun getAppropriateNetworkType(): TdApi.NetworkType {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)
            when {
                caps == null -> TdApi.NetworkTypeOther()
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> TdApi.NetworkTypeWiFi()
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                    if (tm?.isNetworkRoaming == true) {
                        TdApi.NetworkTypeMobileRoaming()
                    } else {
                        TdApi.NetworkTypeMobile()
                    }
                }
                else -> TdApi.NetworkTypeOther()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to determine network type: ${e.message}")
            TdApi.NetworkTypeOther()
        }
    }

    fun optimizeNetworkForDownload() {
        val c = client ?: return
        val netType = getAppropriateNetworkType()
        c.send(TdApi.SetNetworkType(netType), null)
        c.send(TdApi.SetOption("use_pfs", TdApi.OptionValueBoolean(false)), null)
        c.send(TdApi.SetOption("use_quick_ack", TdApi.OptionValueBoolean(true)), null)
        c.send(TdApi.SetOption("use_storage_optimizer", TdApi.OptionValueBoolean(true)), null)
        c.send(TdApi.SetOption("ignore_background_updates", TdApi.OptionValueBoolean(false)), null)
        c.send(TdApi.SetOption("online", TdApi.OptionValueBoolean(true)), null)
        c.send(TdApi.SetOption("is_network_online", TdApi.OptionValueBoolean(true)), null)
        c.send(TdApi.SetOption("disable_persistent_network_statistics", TdApi.OptionValueBoolean(true)), null)
        c.send(TdApi.SetOption("disable_network_statistics", TdApi.OptionValueBoolean(true)), null)
        c.send(TdApi.SetOption("prefer_ipv6", TdApi.OptionValueBoolean(false)), null)
        c.send(TdApi.SetOption("storage_max_files_size", TdApi.OptionValueInteger(50L * 1024 * 1024 * 1024L)), null)
        c.send(TdApi.SetOption("storage_max_file_count", TdApi.OptionValueInteger(100000L)), null)
        c.send(TdApi.SetOption("storage_immunity_delay", TdApi.OptionValueInteger(86400L)), null)
        c.send(TdApi.SetOption("reuse_uploaded_photos_by_hash", TdApi.OptionValueBoolean(true)), null)
    }

    fun keepAliveForBackgroundDownload() {
        val c = client ?: return
        c.send(TdApi.SetOption("online", TdApi.OptionValueBoolean(true)), null)
        c.send(TdApi.SetOption("is_network_online", TdApi.OptionValueBoolean(true)), null)
        c.send(TdApi.SetOption("ignore_background_updates", TdApi.OptionValueBoolean(false)), null)
        val netType = getAppropriateNetworkType()
        c.send(TdApi.SetNetworkType(netType), null)
    }

    fun downloadFile(fileId: Int, offset: Long = 0, limit: Long = 0, priority: Int = 32) {
        if (priority > 1) {
            addLog("INFO", "Requesting MTProto chunk download: fileId=$fileId, offset=$offset, limit=$limit, priority=$priority")
        }
        client?.send(TdApi.DownloadFile(fileId, priority, offset, limit, false)) { result ->
            if (result is TdApi.Error) {
                Log.e(TAG, "DownloadFile error ($fileId): ${result.code} - ${result.message}")
                if (priority > 1) {
                    addLog("ERROR", "DownloadFile error ($fileId): ${result.message}")
                }
            }
        }
    }

    fun addFileToDownloads(fileId: Int, chatId: Long = 0L, messageId: Long = 0L, priority: Int = 32) {
        if (chatId == 0L || messageId == 0L) return
        val isAlreadyShifted = (messageId >= 1_048_576L) && ((messageId and 0xFFFFFL) == 0L)
        val shiftedId = if (isAlreadyShifted) messageId else (messageId shl 20)
        val absId = kotlin.math.abs(chatId)
        val bareId = if (absId > 1000000000000L) absId - 1000000000000L else absId
        val isSupergroup = (absId > 1000000000000L) || (getRegisteredChannelUsername(chatId) != null)
        val effectiveChatId = if (isSupergroup && chatId > 0) {
            -(1000000000000L + bareId)
        } else {
            chatId
        }
        client?.send(TdApi.AddFileToDownloads(fileId, effectiveChatId, shiftedId, priority)) { result ->
            if (result is TdApi.Error) {
                Log.w(TAG, "AddFileToDownloads error ($fileId): ${result.message}")
            } else {
                Log.i(TAG, "AddFileToDownloads registered fileId=$fileId in TDLib background download manager")
            }
        }
    }

    fun toggleDownloadIsPaused(fileId: Int, isPaused: Boolean) {
        client?.send(TdApi.ToggleDownloadIsPaused(fileId, isPaused)) { result ->
            if (result is TdApi.Error) {
                Log.w(TAG, "ToggleDownloadIsPaused error ($fileId, isPaused=$isPaused): ${result.message}")
            } else {
                Log.i(TAG, "ToggleDownloadIsPaused success ($fileId, isPaused=$isPaused)")
            }
        }
    }

    fun getFile(fileId: Int, callback: ((TdApi.File?) -> Unit)? = null) {
        val c = client
        if (c == null || _authState.value != AuthState.Ready) {
            callback?.invoke(null)
            return
        }
        c.send(TdApi.GetFile(fileId)) { result ->
            if (result is TdApi.File) {
                cachedFiles[result.id] = result
                callback?.invoke(result)
            } else {
                callback?.invoke(null)
            }
        }
    }

    private val pendingStreamingCancelJobs = ConcurrentHashMap<Int, Job>()

    fun cancelStreamingDownload(fileId: Int, delayMs: Long = 3500L) {
        if (isExplicitFullDownload(fileId)) return
        val job = CoroutineScope(Dispatchers.IO).launch {
            delay(delayMs)
            pendingStreamingCancelJobs.remove(fileId)
            addLog("INFO", "Cancelled streaming download for fileId=$fileId")
            client?.send(TdApi.CancelDownloadFile(fileId, false), null)
        }
        val prev = pendingStreamingCancelJobs.put(fileId, job)
        prev?.cancel()
    }

    fun keepStreamingDownload(fileId: Int) {
        val prev = pendingStreamingCancelJobs.remove(fileId)
        prev?.cancel()
    }

    fun cancelDownload(fileId: Int) {
        keepStreamingDownload(fileId)
        if (isExplicitFullDownload(fileId) || pendingStreamingCancelJobs.containsKey(fileId)) {
            addLog("INFO", "Cancelled download for fileId=$fileId")
        }
        client?.send(TdApi.RemoveFileFromDownloads(fileId, false), null)
        client?.send(TdApi.CancelDownloadFile(fileId, false), null)
    }

    /**
     * Executes a TDLib query asynchronously and suspends until a result is received.
     * Returns null if TDLib is not ready within the timeout or if the query fails.
     */
    suspend fun <T : TdApi.Object> sendQuery(query: TdApi.Function<T>, timeoutMs: Long = 15_000L): TdApi.Object? = withTimeoutOrNull(timeoutMs) {
        if (!isReady()) {
            withTimeoutOrNull(4000L) {
                authState.filter { it is AuthState.Ready }.first()
            }
        }
        val c = client ?: return@withTimeoutOrNull null
        suspendCancellableCoroutine { cont ->
            c.send(query) { result ->
                if (cont.isActive) cont.resume(result)
            }
        }
    }

    /**
     * Sends a raw TDLib query with an optional result handler callback.
     */
    fun sendRaw(query: TdApi.Function<*>, handler: ((TdApi.Object) -> Unit)? = null) {
        val c = client
        if (c == null) {
            handler?.invoke(TdApi.Error(500, "TDLib client not initialized"))
            return
        }
        c.send(query) { result ->
            handler?.invoke(result)
        }
    }

    fun isReady(): Boolean = _authState.value == AuthState.Ready

    @Synchronized
    fun shutdown() {
        try {
            pendingStreamingCancelJobs.values.forEach { it.cancel() }
            pendingStreamingCancelJobs.clear()
            addLog("INFO", "Shutting down TDLib client")
            client?.send(TdApi.Close(), null)
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TDLib", e)
        }
        client = null
        _authState.value = AuthState.Idle
        resolvedPublicChats.clear()
        resolvedChatsById.clear()
        failedPublicChatSearches.clear()
        synchronized(inFlightPublicChatSearches) {
            inFlightPublicChatSearches.clear()
        }
    }
}
