package dev.roxy.kryptonplayer.feature.player.mpv

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class PlaybackPhase {
    UNINITIALIZED,
    INITIALIZING,
    IDLE,
    LOADING,
    READY,
    STOPPING,
    ERROR,
}

data class PlaybackSessionState(
    val phase: PlaybackPhase = PlaybackPhase.UNINITIALIZED,
    val generation: Long = 0L,
    val activeGeneration: Long = 0L,
    val surfaceAttached: Boolean = false,
    val paused: Boolean = true,
    val currentUri: String? = null,
    val error: String? = null,
)

enum class MpvEndFileReason {
    EOF,
    STOP,
    QUIT,
    ERROR,
    REDIRECT,
    UNKNOWN,
}

object PlaybackSession : MPVLib.EventObserver, MpvShaderRuntime {
    private const val TAG = "PlaybackSession"
    private const val SEEK_AUDIO_FALLBACK_RESTORE_MS = 220L
    private const val SEEK_AUDIO_RESTORE_DELAY_MS = 60L
    private const val TRANSITION_AUDIO_RESTORE_DELAY_MS = 80L
    private val STREAMING_OPTION_SCHEMES = setOf("http", "https", "rtsp", "rtmp", "rtmps")

    private val nativeLock = ReentrantLock(true)
    private val observers = CopyOnWriteArraySet<MPVLib.EventObserver>()
    private val _state = MutableStateFlow(PlaybackSessionState())
    val state: StateFlow<PlaybackSessionState> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var initialized = false
    private var nativeCoreReady = false
    private var nativeCoreCreated = false
    private var applicationContext: Context? = null
    private var desiredVideoOutput = "gpu"
    private var activeCoreConfigurationKey: String? = null
    private var attachedSurfaceOwner: Any? = null
    private var desiredPaused = true
    private var loadedGeneration = 0L
    private var streamingOptionsApplied = false
    private var assetsCopied = false
    private var defaultUserAgent: String? = null
    private var appliedUserAgent: String? = null

    private var seekAudioGuardToken = 0L
    private var seekAudioGuardPreviousMute: Boolean? = null
    private var transitionAudioGuardToken = 0L
    private var transitionAudioGuardPreviousMute: Boolean? = null
    private var transitionAudioGuardCanRestore = false

    val isInitialized: Boolean
        get() = initialized

    internal val isNativeCorePendingInit: Boolean
        get() = nativeLock.withLock { !nativeCoreReady }

    fun invalidateCoreConfiguration() {
        nativeLock.withLock { activeCoreConfigurationKey = null }
    }

    internal fun prewarmNativeCore(context: Context) {
        nativeLock.withLock {
            if (nativeCoreCreated) return
            val appCtx = context.applicationContext
            applicationContext = appCtx
            ensureAssetsCopiedLocked(appCtx)
            runCatching { MPVLib.create(appCtx) }
                .onSuccess {
                    nativeCoreCreated = true
                    runCatching { MPVLib.setThumbnailJavaVM(appCtx) }
                }
                .onFailure { error -> Log.w(TAG, "Failed to prewarm libmpv core", error) }
        }
    }

    fun initialize(
        context: Context,
        configDir: String,
        cacheDir: String,
        coreConfigurationKey: String,
        initOptions: () -> Unit,
        postInitOptions: () -> Unit,
        observeProperties: () -> Unit,
    ): Result<Boolean> = runCatching {
        nativeLock.withLock {
            if (initialized && activeCoreConfigurationKey == coreConfigurationKey) {
                return@withLock false
            }
            if (initialized) {
                destroyLocked()
            }

            val appCtx = context.applicationContext
            applicationContext = appCtx
            ensureAssetsCopiedLocked(appCtx)
            nativeCoreReady = false
            desiredPaused = true
            loadedGeneration = 0L
            streamingOptionsApplied = false
            defaultUserAgent = null
            appliedUserAgent = null
            clearSeekAudioGuardLocked(restoreMute = false)
            clearTransitionAudioGuardLocked(restoreMute = false)
            updateState { it.copy(phase = PlaybackPhase.INITIALIZING, error = null) }

            try {
                if (!nativeCoreCreated) {
                    MPVLib.create(appCtx)
                    nativeCoreCreated = true
                    runCatching { MPVLib.setThumbnailJavaVM(appCtx) }
                }
                MPVLib.setOptionString("config", "no")
                MPVLib.setOptionString("config-dir", configDir)
                MPVLib.setOptionString("gpu-shader-cache-dir", cacheDir)
                MPVLib.setOptionString("icc-cache-dir", cacheDir)
                initOptions()
                MPVLib.init()
                nativeCoreReady = true
                defaultUserAgent = runCatching { MPVLib.getPropertyString("user-agent") }.getOrNull()
                appliedUserAgent = defaultUserAgent
                postInitOptions()
                MPVLib.getPropertyString("vo")
                    ?.takeIf { it.isNotBlank() && it != "null" }
                    ?.let { desiredVideoOutput = it }
                MPVLib.setPropertyString("vo", "null")
                MPVLib.setOptionString("force-window", "no")
                MPVLib.setOptionString("idle", "yes")
                MPVLib.addObserver(this)
                observeProperties()
                initialized = true
                activeCoreConfigurationKey = coreConfigurationKey
                updateState { it.copy(phase = PlaybackPhase.IDLE, paused = true, error = null) }
                true
            } catch (error: Throwable) {
                runCatching { MPVLib.removeObserver(this) }
                runCatching { MPVLib.destroy() }
                initialized = false
                nativeCoreReady = false
                nativeCoreCreated = false
                activeCoreConfigurationKey = null
                updateState {
                    it.copy(
                        phase = PlaybackPhase.ERROR,
                        error = error.message ?: error.javaClass.simpleName,
                    )
                }
                throw error
            }
        }
    }

    fun bindSurface(
        surface: Surface,
        width: Int? = null,
        height: Int? = null,
        owner: Any,
    ): Boolean = withCore(default = false) {
        if (!surface.isValid) return@withCore false
        if (_state.value.surfaceAttached && attachedSurfaceOwner !== owner) {
            detachRendererSurfaceLocked()
        }
        MPVLib.attachSurface(surface)
        if (width != null && width > 0 && height != null && height > 0) {
            MPVLib.setPropertyString("android-surface-size", "${width}x$height")
        }
        MPVLib.setOptionString("force-window", "yes")
        MPVLib.setPropertyString("vo", desiredVideoOutput)
        attachedSurfaceOwner = owner
        updateState { it.copy(surfaceAttached = true) }
        val vid = runCatching { MPVLib.getPropertyInt("vid") }.getOrNull()
        if (vid == null || vid <= 0) {
            runCatching { MPVLib.setPropertyString("vid", "auto") }
        }
        true
    }

    fun resizeSurface(
        width: Int,
        height: Int,
        owner: Any,
    ): Boolean = withCore(default = false) {
        if (width <= 0 || height <= 0 || attachedSurfaceOwner !== owner || !_state.value.surfaceAttached) {
            return@withCore false
        }
        MPVLib.setPropertyString("android-surface-size", "${width}x$height")
        true
    }

    fun unbindSurface(owner: Any): Boolean = withCore(default = false) {
        if (attachedSurfaceOwner !== owner || !_state.value.surfaceAttached) return@withCore false
        detachRendererSurfaceLocked()
        true
    }

    fun setVideoOutput(videoOutput: String) {
        desiredVideoOutput = videoOutput
        withCore(Unit, allowInitializing = true) {
            MPVLib.setOptionString("vo", if (_state.value.surfaceAttached) videoOutput else "null")
        }
    }

    fun load(
        playableUri: String,
        initialPositionSeconds: Double? = null,
        startPaused: Boolean = false,
        title: String = "",
        headers: Map<String, String> = emptyMap(),
    ): Long = withCore(default = -1L) {
        val resolvedUri = resolvePlayablePath(playableUri)
        applyStreamingOptionsLocked(resolvedUri)
        desiredPaused = startPaused
        clearSeekAudioGuardLocked(restoreMute = true)
        beginTransitionAudioGuardLocked(canRestore = true)

        val generation = _state.value.generation + 1L
        val initialPos = initialPositionSeconds?.takeIf { it.isFinite() && it > 0.0 }
        updateState {
            it.copy(
                phase = PlaybackPhase.LOADING,
                generation = generation,
                activeGeneration = generation,
                paused = startPaused,
                currentUri = playableUri,
                error = null,
            )
        }

        val userAgent = headers.entries.firstOrNull { it.key.equals("user-agent", ignoreCase = true) }?.value
            ?: defaultUserAgent.orEmpty()
        if (userAgent != appliedUserAgent) {
            runCatching { MPVLib.setPropertyString("user-agent", userAgent) }
            appliedUserAgent = userAgent
        }
        val extraHeaders = headers.entries
            .filterNot { it.key.equals("user-agent", ignoreCase = true) }
            .joinToString(",") { "${it.key}:${it.value}" }
        runCatching { MPVLib.setPropertyString("http-header-fields", extraHeaders) }
        runCatching { MPVLib.setPropertyString("force-media-title", title) }

        if (!_state.value.surfaceAttached) {
            runCatching { MPVLib.setPropertyString("vo", "null") }
            runCatching { MPVLib.setOptionString("force-window", "no") }
        }

        val loadOptions = buildList {
            add("pause=yes")
            add(if (_state.value.surfaceAttached) "vid=auto" else "vid=no")
            initialPos?.let { add("start=$it") }
        }.joinToString(",")

        MPVLib.command("loadfile", resolvedUri, "replace", "-1", loadOptions)
        generation
    }

    fun stop() {
        withCore(Unit) {
            desiredPaused = true
            loadedGeneration = 0L
            beginTransitionAudioGuardLocked(canRestore = false)
            clearSeekAudioGuardLocked(restoreMute = false)
            runCatching { MPVLib.setPropertyBoolean("pause", true) }
            val previousPhase = _state.value.phase
            updateState {
                it.copy(
                    phase = PlaybackPhase.STOPPING,
                    generation = it.generation + 1L,
                    activeGeneration = 0L,
                    paused = true,
                    error = null,
                )
            }
            if (previousPhase !in setOf(PlaybackPhase.LOADING, PlaybackPhase.READY)) {
                finalizeStopLocked()
            } else {
                runCatching { MPVLib.command("stop") }.onFailure { finalizeStopLocked() }
            }
        }
    }

    fun destroy() {
        nativeLock.withLock {
            if (!initialized) return
            destroyLocked()
        }
    }

    fun addObserver(observer: MPVLib.EventObserver) {
        observers += observer
    }

    fun removeObserver(observer: MPVLib.EventObserver) {
        observers -= observer
    }

    override fun command(vararg args: String) {
        withCore(Unit) {
            if (args.firstOrNull() == "seek") {
                beginSeekAudioGuardLocked()
            }
            MPVLib.command(*args)
        }
    }

    fun commandNode(vararg command: String): MPVNode? =
        withCore(null) { MPVLib.commandNode(*command) }

    fun observeProperty(
        property: String,
        format: Int,
    ) {
        withCore(Unit) {
            MPVLib.observeProperty(property, format)
        }
    }

    fun setOptionString(
        property: String,
        value: String,
    ) {
        withCore(Unit, allowInitializing = true) {
            MPVLib.setOptionString(property, value)
        }
    }

    fun getPropertyInt(property: String): Int? =
        withReadyCore(null) { MPVLib.getPropertyInt(property) }

    fun setPropertyInt(
        property: String,
        value: Int,
    ) {
        withReadyCore(Unit) { MPVLib.setPropertyInt(property, value) }
    }

    fun getPropertyDouble(property: String): Double? =
        withReadyCore(null) { MPVLib.getPropertyDouble(property) }

    fun setPropertyDouble(
        property: String,
        value: Double,
    ) {
        withReadyCore(Unit) { MPVLib.setPropertyDouble(property, value) }
    }

    fun getPropertyBoolean(property: String): Boolean? =
        withReadyCore(null) { MPVLib.getPropertyBoolean(property) }

    fun setPropertyBoolean(
        property: String,
        value: Boolean,
    ) {
        withReadyCore(Unit) {
            if (property == "pause") {
                desiredPaused = value
                if (value) clearTransitionAudioGuardLocked(restoreMute = true)
            }
            MPVLib.setPropertyBoolean(property, value)
            if (property == "pause") {
                updateState { it.copy(paused = value) }
            }
        }
    }

    override fun getPropertyString(property: String): String? =
        withReadyCore(null) { MPVLib.getPropertyString(property) }

    override fun setPropertyString(
        property: String,
        value: String,
    ) {
        withReadyCore(Unit) { MPVLib.setPropertyString(property, value) }
    }

    fun getPropertyNode(property: String): MPVNode? =
        withReadyCore(null) { MPVLib.getPropertyNode(property) }

    fun grabThumbnailFast(
        path: String,
        positionSeconds: Double,
        dimension: Int,
        useHwDec: Boolean = true,
    ): Bitmap? {
        if (!nativeLock.withLock { initialized }) return null
        return runCatching { MPVLib.grabThumbnailFast(path, positionSeconds, dimension, useHwDec) }.getOrNull()
    }

    override fun eventProperty(property: String) {
        observers.forEach { observer -> runCatching { observer.eventProperty(property) } }
    }

    override fun eventProperty(
        property: String,
        value: Long,
    ) {
        observers.forEach { observer -> runCatching { observer.eventProperty(property, value) } }
    }

    override fun eventProperty(
        property: String,
        value: Boolean,
    ) {
        if (property == "pause") {
            updateState { it.copy(paused = value) }
        }
        observers.forEach { observer -> runCatching { observer.eventProperty(property, value) } }
    }

    override fun eventProperty(
        property: String,
        value: String,
    ) {
        observers.forEach { observer -> runCatching { observer.eventProperty(property, value) } }
    }

    override fun eventProperty(
        property: String,
        value: Double,
    ) {
        observers.forEach { observer -> runCatching { observer.eventProperty(property, value) } }
    }

    override fun eventProperty(
        property: String,
        value: MPVNode,
    ) {
        observers.forEach { observer -> runCatching { observer.eventProperty(property, value) } }
    }

    override fun event(
        eventId: Int,
        data: MPVNode,
    ) {
        val forward = nativeLock.withLock {
            when (eventId) {
                MPVLib.MpvEvent.MPV_EVENT_START_FILE -> {
                    if (_state.value.phase != PlaybackPhase.STOPPING) {
                        updateState { it.copy(phase = PlaybackPhase.LOADING, activeGeneration = it.generation) }
                    }
                    true
                }
                MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
                    val current = _state.value
                    if (current.phase == PlaybackPhase.STOPPING) {
                        runCatching { MPVLib.command("stop") }
                        return@withLock true
                    }
                    loadedGeneration = current.generation
                    MPVLib.setPropertyBoolean("pause", desiredPaused)
                    if (current.surfaceAttached) {
                        val vid = runCatching { MPVLib.getPropertyInt("vid") }.getOrNull()
                        if (vid == null || vid <= 0) {
                            runCatching { MPVLib.setPropertyString("vid", "auto") }
                        }
                    }
                    updateState {
                        it.copy(
                            phase = PlaybackPhase.READY,
                            activeGeneration = it.generation,
                            paused = desiredPaused,
                            error = null,
                        )
                    }
                    true
                }
                MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
                    scheduleSeekAudioGuardRestoreLocked(SEEK_AUDIO_RESTORE_DELAY_MS)
                    if (_state.value.phase != PlaybackPhase.STOPPING) {
                        scheduleTransitionAudioGuardRestoreLocked(TRANSITION_AUDIO_RESTORE_DELAY_MS)
                    }
                    true
                }
                MPVLib.MpvEvent.MPV_EVENT_END_FILE -> {
                    val current = _state.value
                    val reason = parseEndFileReason(data)
                    if (current.phase == PlaybackPhase.STOPPING) {
                        if (reason == MpvEndFileReason.REDIRECT) {
                            runCatching { MPVLib.command("stop") }
                        } else {
                            finalizeStopLocked()
                        }
                        return@withLock true
                    }
                    if (reason == MpvEndFileReason.REDIRECT && loadedGeneration != current.generation) {
                        updateState { it.copy(activeGeneration = 0L) }
                    } else {
                        val failedBeforeReady = loadedGeneration != current.generation
                        val isFailure = reason == MpvEndFileReason.ERROR ||
                            (failedBeforeReady && reason !in setOf(MpvEndFileReason.STOP, MpvEndFileReason.QUIT))
                        val error = if (isFailure) {
                            parseEndFileError(data) ?: "Playback failed (${reason.name.lowercase()})"
                        } else {
                            null
                        }
                        updateState {
                            it.copy(
                                phase = if (isFailure) PlaybackPhase.ERROR else PlaybackPhase.IDLE,
                                activeGeneration = 0L,
                                paused = true,
                                error = error,
                            )
                        }
                    }
                    true
                }
                MPVLib.MpvEvent.MPV_EVENT_SHUTDOWN -> {
                    initialized = false
                    nativeCoreReady = false
                    loadedGeneration = 0L
                    clearSeekAudioGuardLocked(restoreMute = false)
                    clearTransitionAudioGuardLocked(restoreMute = false)
                    updateState {
                        it.copy(phase = PlaybackPhase.UNINITIALIZED, surfaceAttached = false, paused = true)
                    }
                    true
                }
                else -> true
            }
        }
        if (forward) {
            observers.forEach { observer -> runCatching { observer.event(eventId, data) } }
        }
    }

    internal fun parseEndFileReason(data: MPVNode): MpvEndFileReason {
        val reasonNode = runCatching { data["reason"] }.getOrNull() ?: return MpvEndFileReason.UNKNOWN
        reasonNode.asString()?.lowercase()?.let { reason ->
            return when (reason) {
                "eof" -> MpvEndFileReason.EOF
                "stop" -> MpvEndFileReason.STOP
                "quit" -> MpvEndFileReason.QUIT
                "error" -> MpvEndFileReason.ERROR
                "redirect" -> MpvEndFileReason.REDIRECT
                else -> MpvEndFileReason.UNKNOWN
            }
        }
        return when (reasonNode.asInt()?.toInt()) {
            0 -> MpvEndFileReason.EOF
            2 -> MpvEndFileReason.STOP
            3 -> MpvEndFileReason.QUIT
            4 -> MpvEndFileReason.ERROR
            5 -> MpvEndFileReason.REDIRECT
            else -> MpvEndFileReason.UNKNOWN
        }
    }

    internal fun parseEndFileError(data: MPVNode): String? =
        sequenceOf(
            runCatching { data["error"] }.getOrNull(),
            runCatching { data["file_error"] }.getOrNull(),
        )
            .mapNotNull { node -> node?.asString() ?: node?.asInt()?.toString() }
            .firstOrNull { value -> value.isNotBlank() && value != "0" }

    private fun finalizeStopLocked() {
        updateState {
            it.copy(
                phase = PlaybackPhase.IDLE,
                activeGeneration = 0L,
                paused = true,
                currentUri = null,
                error = null,
            )
        }
    }

    private fun destroyLocked() {
        desiredPaused = true
        loadedGeneration = 0L
        clearSeekAudioGuardLocked(restoreMute = false)
        clearTransitionAudioGuardLocked(restoreMute = false)
        runCatching { MPVLib.setPropertyBoolean("mute", true) }
        runCatching { MPVLib.setPropertyBoolean("pause", true) }
        runCatching { MPVLib.setPropertyString("vo", "null") }
        runCatching { MPVLib.detachSurface() }
        attachedSurfaceOwner = null
        runCatching { MPVLib.removeObserver(this) }
        runCatching { MPVLib.destroy() }
        observers.clear()
        initialized = false
        nativeCoreReady = false
        nativeCoreCreated = false
        activeCoreConfigurationKey = null
        updateState {
            it.copy(
                phase = PlaybackPhase.UNINITIALIZED,
                surfaceAttached = false,
                paused = true,
                currentUri = null,
            )
        }
    }

    private fun detachRendererSurfaceLocked() {
        runCatching { MPVLib.setPropertyString("vo", "null") }
        runCatching { MPVLib.setOptionString("force-window", "no") }
        runCatching { MPVLib.detachSurface() }
        attachedSurfaceOwner = null
        updateState { it.copy(surfaceAttached = false) }
    }

    private fun ensureAssetsCopiedLocked(context: Context) {
        if (assetsCopied) return
        runCatching { Utils.copyAssets(context) }
        assetsCopied = true
    }

    private fun resolvePlayablePath(playableUri: String): String {
        val parsed = runCatching { Uri.parse(playableUri) }.getOrNull() ?: return playableUri
        if (!ContentResolver.SCHEME_CONTENT.equals(parsed.scheme, ignoreCase = true)) {
            return playableUri
        }
        val context = applicationContext ?: return playableUri
        return runCatching {
            context.contentResolver.openFileDescriptor(parsed, "r")?.use { pfd ->
                val fd = pfd.detachFd()
                "fd://$fd"
            }
        }.getOrNull() ?: playableUri
    }

    private fun applyStreamingOptionsLocked(playableUri: String) {
        if (streamingOptionsApplied) return
        val scheme = playableUri.substringBefore("://", missingDelimiterValue = "").lowercase()
        if (scheme !in STREAMING_OPTION_SCHEMES) return
        val context = applicationContext ?: return
        streamingOptionsApplied = true

        setPropertyString("hls-bitrate", "no")
        setPropertyString("cache", "auto")
        setPropertyString("cache-pause", "yes")
        setPropertyString("cache-pause-wait", "2")
        setPropertyString("demuxer-max-bytes", "64MiB")
        setPropertyString("demuxer-max-back-bytes", "32MiB")
        val cacert = File(context.filesDir, "cacert.pem")
        if (cacert.exists()) {
            setPropertyString("tls-verify", "yes")
            setPropertyString("tls-ca-file", cacert.absolutePath)
        }
        setPropertyString(
            "demuxer-lavf-o",
            "http_persistent=0,reconnect=1,reconnect_on_network_error=1,reconnect_streamed=1," +
                "reconnect_delay_max=5,reconnect_max_retries=5,reconnect_delay_total_max=20",
        )
        setPropertyString(
            "stream-lavf-o",
            "reconnect=1,reconnect_on_network_error=1,reconnect_on_http_error=5xx,reconnect_streamed=1," +
                "reconnect_delay_max=5,reconnect_max_retries=5,reconnect_delay_total_max=20",
        )
    }

    private fun beginSeekAudioGuardLocked() {
        if (_state.value.paused || _state.value.phase != PlaybackPhase.READY) return
        if (seekAudioGuardPreviousMute == null) {
            val wasMuted = runCatching { MPVLib.getPropertyBoolean("mute") }.getOrNull() ?: false
            seekAudioGuardPreviousMute = wasMuted
            if (!wasMuted) runCatching { MPVLib.setPropertyBoolean("mute", true) }
        }
        seekAudioGuardToken++
        scheduleSeekAudioGuardRestoreLocked(SEEK_AUDIO_FALLBACK_RESTORE_MS)
    }

    private fun scheduleSeekAudioGuardRestoreLocked(delayMs: Long) {
        if (seekAudioGuardPreviousMute == null) return
        val token = seekAudioGuardToken
        mainHandler.postDelayed(
            {
                nativeLock.withLock {
                    if (!initialized || token != seekAudioGuardToken) return@withLock
                    clearSeekAudioGuardLocked(restoreMute = true)
                }
            },
            delayMs,
        )
    }

    private fun clearSeekAudioGuardLocked(restoreMute: Boolean) {
        val previousMute = seekAudioGuardPreviousMute
        seekAudioGuardPreviousMute = null
        seekAudioGuardToken++
        if (restoreMute && initialized && previousMute != null) {
            runCatching { MPVLib.setPropertyBoolean("mute", previousMute) }
        }
    }

    private fun beginTransitionAudioGuardLocked(canRestore: Boolean) {
        if (transitionAudioGuardPreviousMute == null) {
            transitionAudioGuardPreviousMute = seekAudioGuardPreviousMute
                ?: runCatching { MPVLib.getPropertyBoolean("mute") }.getOrNull()
                ?: false
        }
        runCatching { MPVLib.setPropertyBoolean("mute", true) }
        transitionAudioGuardCanRestore = canRestore
        transitionAudioGuardToken++
    }

    private fun scheduleTransitionAudioGuardRestoreLocked(delayMs: Long) {
        if (transitionAudioGuardPreviousMute == null || !transitionAudioGuardCanRestore) return
        val token = ++transitionAudioGuardToken
        mainHandler.postDelayed(
            {
                nativeLock.withLock {
                    if (!initialized || token != transitionAudioGuardToken) return@withLock
                    clearTransitionAudioGuardLocked(restoreMute = true)
                }
            },
            delayMs,
        )
    }

    private fun clearTransitionAudioGuardLocked(restoreMute: Boolean) {
        val previousMute = transitionAudioGuardPreviousMute
        transitionAudioGuardPreviousMute = null
        transitionAudioGuardCanRestore = false
        transitionAudioGuardToken++
        if (restoreMute && initialized && previousMute != null) {
            runCatching { MPVLib.setPropertyBoolean("mute", previousMute) }
        }
    }

    private inline fun <T> withCore(
        default: T,
        allowInitializing: Boolean = true,
        block: () -> T,
    ): T = nativeLock.withLock {
        if (!initialized && !(allowInitializing && _state.value.phase == PlaybackPhase.INITIALIZING)) {
            return@withLock default
        }
        block()
    }

    private inline fun <T> withReadyCore(
        default: T,
        block: () -> T,
    ): T = nativeLock.withLock {
        if (!nativeCoreReady) return@withLock default
        block()
    }

    private inline fun updateState(transform: (PlaybackSessionState) -> PlaybackSessionState) {
        _state.update(transform)
    }
}
