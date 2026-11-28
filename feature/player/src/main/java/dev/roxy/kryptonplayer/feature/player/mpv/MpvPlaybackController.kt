package dev.roxy.kryptonplayer.feature.player.mpv

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.roxy.kryptonplayer.core.common.extensions.convertToUTF8
import dev.roxy.kryptonplayer.core.common.extensions.getFilenameFromUri
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController
import dev.roxy.kryptonplayer.core.common.playback.PlaybackEvent
import dev.roxy.kryptonplayer.core.common.playback.PlaybackFailure
import dev.roxy.kryptonplayer.core.common.playback.PlaybackLoopMode
import dev.roxy.kryptonplayer.core.common.playback.PlaybackMediaItem
import dev.roxy.kryptonplayer.core.common.playback.PlaybackStateSnapshot
import dev.roxy.kryptonplayer.core.data.repository.MediaRepository
import dev.roxy.kryptonplayer.core.data.repository.PreferencesRepository
import dev.roxy.kryptonplayer.core.data.tdlib.TelegramStreamingProxy
import dev.roxy.kryptonplayer.core.model.LoopMode
import dev.roxy.kryptonplayer.core.model.Resume
import dev.roxy.kryptonplayer.feature.player.service.PlayerService
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MpvPlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val telegramStreamingProxy: TelegramStreamingProxy,
) : PlaybackController, MPVLib.EventObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(
        PlaybackStateSnapshot(
            repeatMode = preferencesRepository.playerPreferences.value.loopMode.toPlaybackLoopMode(),
            playbackSpeed = preferencesRepository.playerPreferences.value.defaultPlaybackSpeed,
            isLoudnessGainSupported = true,
        ),
    )
    override val state: StateFlow<PlaybackStateSnapshot> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlaybackEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    private val anime4KManager: Anime4KManager by lazy { Anime4KManager(context) }
    private val hdrToysManager: HdrToysManager by lazy { HdrToysManager(context, PlaybackSession) }

    private var activeProxyUrl: String? = null
    private var loadJob: Job? = null
    @Volatile
    private var currentLoadGeneration: Long = 0L
    @Volatile
    private var pendingUserSeek: Boolean = false
    @Volatile
    private var pausedByCore: Boolean = true
    @Volatile
    private var pausedForCache: Boolean = false
    @Volatile
    private var lastPositionPersistElapsedMs: Long = 0L
    private val watchLock = Any()
    private var activeWatchUri: String? = null
    private var lastActiveWatchTimestampMs: Long? = null
    private var pendingWatchMs: Long = 0L
    private var shuffleOrder: List<Int> = emptyList()
    private var externalSubAppliedForGeneration: Long = -1L

    init {
        PlaybackSession.addObserver(this)
    }

    override fun setPlaylist(
        items: List<PlaybackMediaItem>,
        startIndex: Int,
        startPositionMs: Long,
        playWhenReady: Boolean,
    ) {
        if (items.isEmpty()) {
            stop(clearPlaylist = true)
            return
        }
        flushWatchAndPositionAsync(clearActiveUri = true)
        val safeIndex = startIndex.coerceIn(0, items.lastIndex)
        val prefs = preferencesRepository.playerPreferences.value
        val loopMode = prefs.loopMode.toPlaybackLoopMode()
        shuffleOrder = buildShuffleOrder(items.size, safeIndex)
        externalSubAppliedForGeneration = -1L

        _state.update {
            it.copy(
                playlist = items,
                currentIndex = safeIndex,
                repeatMode = loopMode,
                isEnded = false,
                isIdle = false,
                isLoading = true,
                isPlaying = false,
                currentPositionMs = if (startPositionMs >= 0L) startPositionMs else items[safeIndex].positionMs.coerceAtLeast(0L),
                durationMs = items[safeIndex].durationMs.coerceAtLeast(0L),
                error = null,
            )
        }

        loadJob?.cancel()
        loadJob = scope.launch {
            loadIndex(safeIndex, overrideStartPositionMs = startPositionMs, playWhenReady = playWhenReady)
        }
    }

    override fun addMediaItem(index: Int, item: PlaybackMediaItem) {
        _state.update { current ->
            val mutable = current.playlist.toMutableList()
            val targetIdx = index.coerceIn(0, mutable.size)
            mutable.add(targetIdx, item)
            val newIndex = if (current.currentIndex >= targetIdx && current.playlist.isNotEmpty()) {
                current.currentIndex + 1
            } else {
                current.currentIndex.coerceAtLeast(0)
            }
            shuffleOrder = buildShuffleOrder(mutable.size, newIndex)
            current.copy(playlist = mutable, currentIndex = newIndex)
        }
    }

    override fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            val list = current.playlist.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) {
                return@update current
            }
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            val updatedCurrent = when (current.currentIndex) {
                fromIndex -> toIndex
                in (toIndex until fromIndex) -> current.currentIndex + 1
                in ((fromIndex + 1)..toIndex) -> current.currentIndex - 1
                else -> current.currentIndex
            }
            shuffleOrder = buildShuffleOrder(list.size, updatedCurrent)
            current.copy(playlist = list, currentIndex = updatedCurrent)
        }
    }

    override fun removeMediaItem(index: Int) {
        val current = _state.value
        if (index !in current.playlist.indices) return
        if (current.playlist.size <= 1) {
            stop(clearPlaylist = true)
            return
        }
        val wasCurrent = index == current.currentIndex
        if (wasCurrent) {
            flushWatchAndPositionAsync(
                mediaId = current.currentItem?.mediaId,
                positionMs = current.currentPositionMs,
                clearActiveUri = true,
            )
        }
        val updatedList = current.playlist.toMutableList().apply { removeAt(index) }
        val nextIndex = when {
            index < current.currentIndex -> current.currentIndex - 1
            else -> current.currentIndex.coerceAtMost(updatedList.lastIndex)
        }
        shuffleOrder = buildShuffleOrder(updatedList.size, nextIndex)
        _state.update {
            it.copy(playlist = updatedList, currentIndex = nextIndex)
        }
        if (wasCurrent) {
            loadJob?.cancel()
            loadJob = scope.launch {
                loadIndex(nextIndex, overrideStartPositionMs = 0L, playWhenReady = current.isPlaying)
            }
        }
    }

    override fun play() {
        val current = _state.value
        if (current.isEnded && current.currentItem != null) {
            val index = current.currentIndex
            if (index in current.playlist.indices) {
                flushWatchAndPositionAsync(clearActiveUri = true)
                _state.update {
                    it.copy(
                        isEnded = false,
                        isIdle = false,
                        isLoading = true,
                        currentPositionMs = 0L,
                        error = null,
                    )
                }
                loadJob?.cancel()
                loadJob = scope.launch {
                    loadIndex(index, overrideStartPositionMs = 0L, playWhenReady = true)
                }
                return
            }
        }
        ensureHeadlessCoreInitialized()
        PlaybackSession.setPropertyBoolean("pause", false)
        PlayerService.start(context)
    }

    override fun pause() {
        PlaybackSession.setPropertyBoolean("pause", true)
        flushWatchAndPositionAsync(clearActiveUri = false)
    }

    override fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    override fun stop(clearPlaylist: Boolean) {
        loadJob?.cancel()
        loadJob = null
        flushWatchAndPositionAsync(clearActiveUri = true)
        unregisterActiveProxy()
        currentLoadGeneration = 0L
        pausedByCore = true
        PlaybackSession.stop()
        _state.update { current ->
            current.copy(
                isIdle = true,
                isLoading = false,
                isBuffering = false,
                isPlaying = false,
                isEnded = false,
                playlist = if (clearPlaylist) emptyList() else current.playlist,
                currentIndex = if (clearPlaylist) 0 else current.currentIndex,
                currentPositionMs = 0L,
                durationMs = 0L,
                bufferedPositionMs = 0L,
                videoTracks = emptyList(),
                audioTracks = emptyList(),
                subtitleTracks = emptyList(),
                error = null,
            )
        }
        PlayerService.stop(context)
    }

    override fun seekTo(positionMs: Long, isUserSeek: Boolean) {
        val duration = _state.value.durationMs
        val clamped = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        pendingUserSeek = isUserSeek
        PlaybackSession.command("seek", (clamped / 1000.0).toString(), "absolute")
        _state.update { it.copy(currentPositionMs = clamped, isEnded = false) }
    }

    override fun seekToItem(index: Int, positionMs: Long) {
        val current = _state.value
        if (index !in current.playlist.indices) return
        flushWatchAndPositionAsync(clearActiveUri = true)
        val keepPlaying = current.isPlaying || !pausedByCore
        _state.update {
            it.copy(
                currentIndex = index,
                isEnded = false,
                isLoading = true,
                currentPositionMs = positionMs.coerceAtLeast(0L),
            )
        }
        loadJob?.cancel()
        loadJob = scope.launch {
            loadIndex(index, overrideStartPositionMs = positionMs.coerceAtLeast(0L), playWhenReady = keepPlaying)
        }
    }

    override fun seekToNext() {
        val current = _state.value
        if (current.playlist.isEmpty()) return
        val nextIndex = if (current.shuffleEnabled && shuffleOrder.isNotEmpty()) {
            val pos = shuffleOrder.indexOf(current.currentIndex)
            if (pos >= 0 && pos < shuffleOrder.lastIndex) {
                shuffleOrder[pos + 1]
            } else if (current.repeatMode == PlaybackLoopMode.ALL) {
                shuffleOrder.first()
            } else {
                return
            }
        } else if (current.currentIndex + 1 < current.playlist.size) {
            current.currentIndex + 1
        } else if (current.repeatMode == PlaybackLoopMode.ALL) {
            0
        } else {
            return
        }
        seekToItem(nextIndex, 0L)
    }

    override fun seekToPrevious() {
        val current = _state.value
        if (current.playlist.isEmpty()) return
        val prevIndex = if (current.shuffleEnabled && shuffleOrder.isNotEmpty()) {
            val pos = shuffleOrder.indexOf(current.currentIndex)
            if (pos > 0) shuffleOrder[pos - 1]
            else if (current.repeatMode == PlaybackLoopMode.ALL) shuffleOrder.last()
            else return
        } else if (current.currentIndex > 0) {
            current.currentIndex - 1
        } else if (current.repeatMode == PlaybackLoopMode.ALL) {
            current.playlist.lastIndex
        } else {
            return
        }
        seekToItem(prevIndex, 0L)
    }

    override fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        PlaybackSession.setPropertyDouble("speed", clamped.toDouble())
        _state.update { it.copy(playbackSpeed = clamped) }
        updateCurrentItemMetadata { it.copy(playbackSpeed = clamped) }
        _state.value.currentItem?.mediaId?.let { id ->
            scope.launch(Dispatchers.IO) { mediaRepository.updateMediumPlaybackSpeed(id, clamped) }
        }
        _events.tryEmit(PlaybackEvent.PlaybackParametersChanged(clamped))
    }

    override fun setRepeatMode(mode: PlaybackLoopMode) {
        applyLoopFileProperty(mode)
        _state.update { it.copy(repeatMode = mode) }
        scope.launch(Dispatchers.IO) {
            preferencesRepository.updatePlayerPreferences {
                it.copy(
                    loopMode = when (mode) {
                        PlaybackLoopMode.OFF -> LoopMode.OFF
                        PlaybackLoopMode.ONE -> LoopMode.ONE
                        PlaybackLoopMode.ALL -> LoopMode.ALL
                    },
                )
            }
        }
    }

    private fun applyLoopFileProperty(mode: PlaybackLoopMode) {
        PlaybackSession.setPropertyString("loop-file", if (mode == PlaybackLoopMode.ONE) "inf" else "no")
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        val current = _state.value
        if (enabled) {
            shuffleOrder = buildShuffleOrder(current.playlist.size, current.currentIndex)
        }
        _state.update { it.copy(shuffleEnabled = enabled) }
    }

    override fun selectAudioTrack(index: Int) {
        val current = _state.value
        if (index < 0) {
            PlaybackSession.setPropertyString("aid", "no")
        } else {
            val track = current.audioTracks.getOrNull(index) ?: return
            PlaybackSession.setPropertyString("aid", track.id.toString())
        }
        updateCurrentItemMetadata { it.copy(audioTrackIndex = index) }
        current.currentItem?.mediaId?.let { mediaId ->
            scope.launch(Dispatchers.IO) { mediaRepository.updateMediumAudioTrack(mediaId, index) }
        }
        refreshTracksFromMpv()
    }

    override fun selectSubtitleTrack(index: Int) {
        val current = _state.value
        if (index < 0) {
            PlaybackSession.setPropertyString("sid", "no")
        } else {
            val track = current.subtitleTracks.getOrNull(index) ?: return
            PlaybackSession.setPropertyString("sid", track.id.toString())
        }
        updateCurrentItemMetadata { it.copy(subtitleTrackIndex = index) }
        current.currentItem?.mediaId?.let { mediaId ->
            scope.launch(Dispatchers.IO) { mediaRepository.updateMediumSubtitleTrack(mediaId, index) }
        }
        refreshTracksFromMpv()
    }

    override fun addExternalSubtitle(uri: Uri, select: Boolean) {
        val currentItem = _state.value.currentItem
        updateCurrentItemMetadata { item ->
            val updated = (item.externalSubtitles + uri).distinct()
            item.copy(externalSubtitles = updated)
        }
        if (currentItem != null) {
            scope.launch(Dispatchers.IO) {
                mediaRepository.addExternalSubtitleToMedium(currentItem.mediaId, uri)
            }
        }
        scope.launch {
            addExternalSubtitleInternal(uri, select)
        }
    }

    override fun setSubtitleDelayMs(delayMs: Long) {
        PlaybackSession.setPropertyDouble("sub-delay", delayMs / 1000.0)
        _state.update { it.copy(subtitleDelayMs = delayMs) }
        updateCurrentItemMetadata { it.copy(subtitleDelayMs = delayMs) }
        _state.value.currentItem?.mediaId?.let { id ->
            scope.launch(Dispatchers.IO) { mediaRepository.updateSubtitleDelay(id, delayMs) }
        }
    }

    override fun setSubtitleSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        PlaybackSession.setPropertyDouble("sub-speed", clamped.toDouble())
        _state.update { it.copy(subtitleSpeed = clamped) }
        updateCurrentItemMetadata { it.copy(subtitleSpeed = clamped) }
        _state.value.currentItem?.mediaId?.let { id ->
            scope.launch(Dispatchers.IO) { mediaRepository.updateSubtitleSpeed(id, clamped) }
        }
    }

    override fun setSkipSilenceEnabled(enabled: Boolean) {
        PlaybackSession.command(
            "af",
            "set",
            if (enabled) "silenceremove=stop_periods=-1:stop_duration=0.3:stop_threshold=-40dB" else "",
        )
        _state.update { it.copy(skipSilenceEnabled = enabled) }
    }

    override fun setLoudnessGainMb(gainMb: Int) {
        val clampedMb = gainMb.coerceIn(0, 2000)
        val prefs = preferencesRepository.playerPreferences.value
        val volumePercent = if (clampedMb <= 0 || !prefs.enableVolumeBoost) {
            100.0
        } else {
            100.0 + (clampedMb / 2000.0) * 250.0
        }
        PlaybackSession.setPropertyDouble("volume", volumePercent)
        _state.update { it.copy(volumeBoostGainMb = clampedMb) }
    }

    override fun setScrubbingMode(enabled: Boolean) {
        PlaybackSession.setPropertyString("hr-seek", if (enabled) "no" else "yes")
    }

    override fun updateCurrentItemMetadata(transform: (PlaybackMediaItem) -> PlaybackMediaItem) {
        _state.update { current ->
            val idx = current.currentIndex
            if (idx !in current.playlist.indices) return@update current
            val mutable = current.playlist.toMutableList()
            mutable[idx] = transform(mutable[idx])
            current.copy(playlist = mutable)
        }
    }

    override fun setAnime4KMode(mode: String) {
        val parsedMode = Anime4KManager.Mode.entries.firstOrNull {
            it.name.equals(mode, ignoreCase = true) || it.displayName.equals(mode, ignoreCase = true)
        } ?: Anime4KManager.Mode.OFF
        _state.update { it.copy(anime4kMode = parsedMode.name) }
        if (!PlaybackSession.isInitialized) return
        if (parsedMode == Anime4KManager.Mode.OFF) {
            clearAnime4KShaders()
        } else {
            val selection = selectRuntimeStableAnime4K(parsedMode, Anime4KManager.DEFAULT_QUALITY, context)
            if (selection.mode == Anime4KManager.Mode.OFF) {
                clearAnime4KShaders()
            } else {
                applyAnime4KShaderChain(anime4KManager, selection.mode, selection.quality)
            }
        }
    }

    override fun setHdrMode(mode: String) {
        val screenMode = HdrScreenMode.entries.firstOrNull {
            it.name.equals(mode, ignoreCase = true)
        } ?: HdrScreenMode.OFF
        val toysProfile = HdrToysProfile.entries.firstOrNull {
            it.name.equals(mode, ignoreCase = true)
        } ?: HdrToysProfile.OFF
        _state.update { it.copy(hdrMode = mode) }
        if (!PlaybackSession.isInitialized) return
        HdrScreenOutput.applyProperties(screenMode, pipelineReady = false)
        if (toysProfile != HdrToysProfile.OFF) {
            hdrToysManager.apply(toysProfile)
        } else {
            hdrToysManager.clear()
        }
    }

    private suspend fun loadIndex(
        index: Int,
        overrideStartPositionMs: Long,
        playWhenReady: Boolean,
    ) {
        val item = _state.value.playlist.getOrNull(index) ?: return
        ensureHeadlessCoreInitialized()
        val prefs = preferencesRepository.playerPreferences.value
        val videoState = runCatching { mediaRepository.getVideoState(item.mediaId) }.getOrNull()

        val resolvedStartMs = when {
            overrideStartPositionMs >= 0L -> overrideStartPositionMs
            item.positionMs > 0L -> item.positionMs
            prefs.resume == Resume.YES -> videoState?.position ?: 0L
            else -> 0L
        }

        val resolvedItem = if (videoState != null && prefs.rememberSelections) {
            item.copy(
                playbackSpeed = item.playbackSpeed ?: videoState.playbackSpeed,
                audioTrackIndex = item.audioTrackIndex ?: videoState.audioTrackIndex,
                subtitleTrackIndex = item.subtitleTrackIndex ?: videoState.subtitleTrackIndex,
                subtitleDelayMs = item.subtitleDelayMs ?: videoState.subtitleDelayMilliseconds,
                subtitleSpeed = item.subtitleSpeed ?: videoState.subtitleSpeed,
                externalSubtitles = (item.externalSubtitles + videoState.externalSubs).distinct(),
                videoScale = item.videoScale ?: videoState.videoScale,
            )
        } else {
            item
        }
        updateCurrentItemMetadata { resolvedItem }

        val proxyUrl = if (telegramStreamingProxy.shouldProxy(resolvedItem.uri)) {
            telegramStreamingProxy.registerUri(resolvedItem.uri)
        } else {
            null
        }
        unregisterActiveProxy(keepUrl = proxyUrl)
        activeProxyUrl = proxyUrl

        val targetUriString = proxyUrl ?: resolvedItem.uri.toString()
        val startSeconds = resolvedStartMs.takeIf { it > 0L }?.div(1000.0)
        pausedByCore = !playWhenReady
        currentLoadGeneration = PlaybackSession.state.value.generation + 1L
        currentLoadGeneration = PlaybackSession.load(
            playableUri = targetUriString,
            initialPositionSeconds = startSeconds,
            startPaused = !playWhenReady,
            title = resolvedItem.title,
        )
        applyLoopFileProperty(_state.value.repeatMode)
        if (playWhenReady) {
            PlayerService.start(context)
        }
        _events.tryEmit(PlaybackEvent.MediaItemTransition(resolvedItem, index))
    }

    private fun ensureHeadlessCoreInitialized() {
        if (PlaybackSession.isInitialized) return
        val prefs = preferencesRepository.playerPreferences.value
        val key = MPVView.computeCoreConfigKey(context, prefs)
        PlaybackSession.initialize(
            context = context,
            configDir = context.filesDir.path,
            cacheDir = context.cacheDir.path,
            coreConfigurationKey = key,
            initOptions = { MPVView.applyInitOptions(context, prefs) },
            postInitOptions = {},
            observeProperties = { MPVView.observeDefaultProperties() },
        )
    }

    private suspend fun addExternalSubtitleInternal(uri: Uri, select: Boolean) {
        if (!PlaybackSession.isInitialized) return
        val prefs = preferencesRepository.playerPreferences.value
        val (localPath, title) = withContext(Dispatchers.IO) {
            val charset = prefs.subtitleTextEncoding
                .takeIf { it.isNotBlank() && Charset.isSupported(it) }
                ?.let { Charset.forName(it) }
            val convertedUri = context.convertToUTF8(uri, charset)
            val path = copySubtitleToCacheIfNeeded(convertedUri, uri) ?: convertedUri.toString()
            val name = context.getFilenameFromUri(uri)
            path to name
        }
        val mode = if (select) "select" else "auto"
        PlaybackSession.command("sub-add", localPath, mode, title)
    }

    private fun copySubtitleToCacheIfNeeded(convertedUri: Uri, originalUri: Uri): String? {
        if (convertedUri.scheme.equals("file", ignoreCase = true) && !convertedUri.path.isNullOrBlank()) {
            return convertedUri.path
        }
        if (convertedUri.scheme.equals("http", ignoreCase = true) || convertedUri.scheme.equals("https", ignoreCase = true)) {
            return convertedUri.toString()
        }
        return runCatching {
            val subDir = File(context.cacheDir, "mpv_subs").apply { mkdirs() }
            val name = context.getFilenameFromUri(originalUri).replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val dest = File(subDir, "${System.currentTimeMillis()}_$name")
            context.contentResolver.openInputStream(convertedUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.takeIf { it.exists() && it.length() > 0 }?.absolutePath
        }.getOrNull()
    }

    private fun unregisterActiveProxy(keepUrl: String? = null) {
        val prev = activeProxyUrl
        if (prev != null && prev != keepUrl) {
            runCatching { telegramStreamingProxy.unregisterUrl(prev) }
            activeProxyUrl = null
        }
    }

    private fun persistCurrentPositionAsync() {
        val current = _state.value
        val item = current.currentItem ?: return
        val pos = current.currentPositionMs
        if (pos <= 0L) return
        scope.launch(Dispatchers.IO) {
            mediaRepository.updateMediumPosition(item.mediaId, pos)
        }
    }

    private fun isActivelyWatchingSnapshot(snapshot: PlaybackStateSnapshot = _state.value): Boolean =
        snapshot.isPlaying &&
            !snapshot.isBuffering &&
            !pausedByCore &&
            !snapshot.isLoading &&
            !snapshot.isEnded &&
            snapshot.currentItem != null

    private fun startOrResumeWatchTracking(mediaId: String) {
        if (mediaId.isBlank()) return
        val now = SystemClock.elapsedRealtime()
        var previousFlush: Pair<String, Long>? = null
        synchronized(watchLock) {
            if (activeWatchUri != null && activeWatchUri != mediaId) {
                accumulateWatchElapsedLocked(now)
                if (pendingWatchMs > 0L) {
                    previousFlush = activeWatchUri!! to pendingWatchMs
                }
                pendingWatchMs = 0L
            }
            activeWatchUri = mediaId
            if (lastActiveWatchTimestampMs == null) {
                lastActiveWatchTimestampMs = now
            }
        }
        previousFlush?.let { (uri, deltaMs) ->
            scope.launch(Dispatchers.IO) { mediaRepository.addWatchDuration(uri, deltaMs) }
        }
    }

    private fun recordWatchTick(mediaId: String) {
        if (mediaId.isBlank()) return
        val now = SystemClock.elapsedRealtime()
        var previousFlush: Pair<String, Long>? = null
        var periodicFlush: Pair<String, Long>? = null
        synchronized(watchLock) {
            if (activeWatchUri != mediaId) {
                if (activeWatchUri != null) {
                    accumulateWatchElapsedLocked(now)
                    if (pendingWatchMs > 0L) {
                        previousFlush = activeWatchUri!! to pendingWatchMs
                    }
                    pendingWatchMs = 0L
                }
                activeWatchUri = mediaId
                lastActiveWatchTimestampMs = now
            } else {
                accumulateWatchElapsedLocked(now)
                if (lastActiveWatchTimestampMs == null) {
                    lastActiveWatchTimestampMs = now
                }
                if (pendingWatchMs >= WATCH_FLUSH_THRESHOLD_MS) {
                    periodicFlush = mediaId to pendingWatchMs
                    pendingWatchMs = 0L
                }
            }
        }
        previousFlush?.let { (uri, deltaMs) ->
            scope.launch(Dispatchers.IO) { mediaRepository.addWatchDuration(uri, deltaMs) }
        }
        periodicFlush?.let { (uri, deltaMs) ->
            scope.launch(Dispatchers.IO) { mediaRepository.addWatchDuration(uri, deltaMs) }
        }
    }

    private fun pauseWatchTimerOnly() {
        val now = SystemClock.elapsedRealtime()
        synchronized(watchLock) {
            accumulateWatchElapsedLocked(now)
            lastActiveWatchTimestampMs = null
        }
    }

    private fun drainPendingWatchDuration(clearActiveUri: Boolean): Pair<String, Long>? {
        val now = SystemClock.elapsedRealtime()
        return synchronized(watchLock) {
            accumulateWatchElapsedLocked(now)
            lastActiveWatchTimestampMs = null
            val uri = activeWatchUri
            val delta = pendingWatchMs
            pendingWatchMs = 0L
            if (clearActiveUri) {
                activeWatchUri = null
            }
            if (uri != null && delta > 0L) uri to delta else null
        }
    }

    private fun flushWatchDurationAsync(clearActiveUri: Boolean = false) {
        val flush = drainPendingWatchDuration(clearActiveUri = clearActiveUri) ?: return
        scope.launch(Dispatchers.IO) {
            mediaRepository.addWatchDuration(flush.first, flush.second)
        }
    }

    private fun flushWatchAndPositionAsync(
        mediaId: String? = _state.value.currentItem?.mediaId,
        positionMs: Long = _state.value.currentPositionMs,
        clearActiveUri: Boolean = false,
    ) {
        val watchFlush = drainPendingWatchDuration(clearActiveUri = clearActiveUri)
        val persistMediaId = mediaId?.takeIf { it.isNotBlank() && positionMs > 0L }
        if (watchFlush == null && persistMediaId == null) return
        scope.launch(Dispatchers.IO) {
            if (watchFlush != null) {
                mediaRepository.addWatchDuration(watchFlush.first, watchFlush.second)
            }
            if (persistMediaId != null) {
                mediaRepository.updateMediumPosition(persistMediaId, positionMs)
            }
        }
    }

    private fun accumulateWatchElapsedLocked(nowElapsedMs: Long) {
        val last = lastActiveWatchTimestampMs ?: return
        if (activeWatchUri == null) {
            lastActiveWatchTimestampMs = null
            return
        }
        val delta = (nowElapsedMs - last).coerceIn(0L, MAX_WATCH_TICK_DELTA_MS)
        pendingWatchMs += delta
        lastActiveWatchTimestampMs = nowElapsedMs
    }

    private fun refreshTracksFromMpv(trackListNode: MPVNode? = null) {
        val node = trackListNode ?: PlaybackSession.getPropertyNode("track-list") ?: return
        val parsed = MpvTrackSelector.parseTrackList(node)
        val selectedAudio = parsed.audioTracks.firstOrNull { it.isSelected }
        _state.update {
            it.copy(
                videoTracks = parsed.videoTracks,
                audioTracks = parsed.audioTracks,
                subtitleTracks = parsed.subtitleTracks,
                audioChannels = selectedAudio?.channels ?: it.audioChannels,
                audioSampleRate = selectedAudio?.sampleRate ?: it.audioSampleRate,
            )
        }
        _events.tryEmit(PlaybackEvent.TracksChanged)
    }

    private fun applyPostLoadPreferences(generation: Long) {
        if (generation <= 0L || generation != currentLoadGeneration) return
        val item = _state.value.currentItem ?: return
        val prefs = preferencesRepository.playerPreferences.value

        item.playbackSpeed?.takeIf { it > 0f }?.let { speed ->
            PlaybackSession.setPropertyDouble("speed", speed.toDouble())
        } ?: run {
            PlaybackSession.setPropertyDouble("speed", prefs.defaultPlaybackSpeed.toDouble())
        }

        item.subtitleDelayMs?.let { delayMs ->
            PlaybackSession.setPropertyDouble("sub-delay", delayMs / 1000.0)
        }
        item.subtitleSpeed?.let { speed ->
            PlaybackSession.setPropertyDouble("sub-speed", speed.toDouble())
        }
        setLoudnessGainMb(_state.value.volumeBoostGainMb)
        if (_state.value.skipSilenceEnabled) {
            setSkipSilenceEnabled(true)
        }

        if (externalSubAppliedForGeneration != generation) {
            externalSubAppliedForGeneration = generation
            scope.launch {
                if (generation != currentLoadGeneration) return@launch
                item.externalSubtitles.forEachIndexed { idx, subUri ->
                    if (generation != currentLoadGeneration) return@launch
                    addExternalSubtitleInternal(subUri, select = idx == 0 && item.subtitleTrackIndex == null)
                }
            }
        }

        val trackNode = PlaybackSession.getPropertyNode("track-list")
        val parsed = MpvTrackSelector.parseTrackList(trackNode)
        if (prefs.rememberSelections) {
            item.audioTrackIndex?.let { audioIdx ->
                if (audioIdx < 0) {
                    PlaybackSession.setPropertyString("aid", "no")
                } else {
                    parsed.audioTracks.getOrNull(audioIdx)?.let {
                        PlaybackSession.setPropertyString("aid", it.id.toString())
                    }
                }
            }
            item.subtitleTrackIndex?.let { subIdx ->
                if (subIdx < 0) {
                    PlaybackSession.setPropertyString("sid", "no")
                } else {
                    parsed.subtitleTracks.getOrNull(subIdx)?.let {
                        PlaybackSession.setPropertyString("sid", it.id.toString())
                    }
                }
            }
        }

        scope.launch(Dispatchers.IO) {
            mediaRepository.updateMediumLastPlayedTime(item.mediaId, System.currentTimeMillis())
        }
        if (generation == currentLoadGeneration) {
            refreshTracksFromMpv()
        }
    }

    override fun eventProperty(property: String) = Unit

    override fun eventProperty(property: String, value: Long) {
        when (property) {
            "video-params/w" -> _state.update {
                val w = value.toInt()
                if (w > 0 && it.videoHeight > 0) _events.tryEmit(PlaybackEvent.VideoSizeChanged(w, it.videoHeight))
                it.copy(videoWidth = w)
            }
            "video-params/h" -> _state.update {
                val h = value.toInt()
                if (it.videoWidth > 0 && h > 0) _events.tryEmit(PlaybackEvent.VideoSizeChanged(it.videoWidth, h))
                it.copy(videoHeight = h)
            }
            "video-bitrate" -> _state.update { it.copy(videoBitrate = value.toInt()) }
            "audio-bitrate" -> _state.update { it.copy(audioBitrate = value.toInt()) }
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        when (property) {
            "pause" -> {
                val prevPlaying = _state.value.isPlaying
                pausedByCore = value
                val nowPlaying = !value && !pausedForCache && !_state.value.isLoading && !_state.value.isEnded
                _state.update { it.copy(isPlaying = nowPlaying) }
                if (nowPlaying && !pausedByCore && !pausedForCache) {
                    _state.value.currentItem?.mediaId?.let(::startOrResumeWatchTracking)
                    if (prevPlaying != nowPlaying) {
                        _events.tryEmit(PlaybackEvent.IsPlayingChanged(true))
                    }
                } else {
                    if (prevPlaying != nowPlaying) {
                        _events.tryEmit(PlaybackEvent.IsPlayingChanged(false))
                        flushWatchAndPositionAsync(clearActiveUri = false)
                    } else {
                        flushWatchDurationAsync(clearActiveUri = false)
                    }
                }
            }
            "paused-for-cache" -> {
                val prevPlaying = _state.value.isPlaying
                pausedForCache = value
                val nowPlaying = !pausedByCore && !value && !_state.value.isLoading && !_state.value.isEnded
                _state.update { it.copy(isBuffering = value, isPlaying = nowPlaying) }
                if (nowPlaying && !pausedByCore && !pausedForCache) {
                    _state.value.currentItem?.mediaId?.let(::startOrResumeWatchTracking)
                } else {
                    flushWatchDurationAsync(clearActiveUri = false)
                }
                if (prevPlaying != nowPlaying) {
                    _events.tryEmit(PlaybackEvent.IsPlayingChanged(nowPlaying))
                }
            }
        }
    }

    override fun eventProperty(property: String, value: String) {
        when (property) {
            "video-codec" -> _state.update { it.copy(videoCodec = value) }
            "audio-codec" -> _state.update { it.copy(audioCodec = value) }
            "hwdec-current" -> _state.update { it.copy(hwdecCurrent = value) }
            "video-params/gamma" -> _state.update { it.copy(colorTransfer = value) }
            "aid", "sid" -> refreshTracksFromMpv()
        }
    }

    override fun eventProperty(property: String, value: Double) {
        when (property) {
            "time-pos" -> {
                val posMs = (value * 1000.0).toLong().coerceAtLeast(0L)
                _state.update { it.copy(currentPositionMs = posMs) }
                val snap = _state.value
                if (isActivelyWatchingSnapshot(snap)) {
                    snap.currentItem?.mediaId?.let(::recordWatchTick)
                } else {
                    pauseWatchTimerOnly()
                }
                val now = SystemClock.elapsedRealtime()
                if (now - lastPositionPersistElapsedMs >= 3000L) {
                    lastPositionPersistElapsedMs = now
                    persistCurrentPositionAsync()
                }
            }
            "duration" -> {
                val durMs = (value * 1000.0).toLong().coerceAtLeast(0L)
                _state.update { it.copy(durationMs = durMs) }
            }
            "demuxer-cache-time" -> {
                val cacheMs = (value * 1000.0).toLong().coerceAtLeast(0L)
                _state.update { it.copy(bufferedPositionMs = it.currentPositionMs + cacheMs) }
            }
            "speed" -> {
                val speed = value.toFloat()
                _state.update { it.copy(playbackSpeed = speed) }
                _events.tryEmit(PlaybackEvent.PlaybackParametersChanged(speed))
            }
            "video-params/aspect" -> _state.update { it.copy(videoAspectRatio = value.toFloat()) }
            "container-fps" -> _state.update { it.copy(containerFps = value.toFloat()) }
        }
    }

    override fun eventProperty(property: String, value: MPVNode) {
        if (property == "track-list") {
            refreshTracksFromMpv(value)
        }
    }

    override fun event(eventId: Int, data: MPVNode) {
        when (eventId) {
            MPVLib.MpvEvent.MPV_EVENT_START_FILE -> {
                val sessionGen = PlaybackSession.state.value.generation
                if (currentLoadGeneration <= 0L || sessionGen != currentLoadGeneration) {
                    return
                }
                pauseWatchTimerOnly()
                _state.update { it.copy(isLoading = true, isIdle = false, isEnded = false, error = null) }
            }
            MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
                val gen = PlaybackSession.state.value.generation
                if (currentLoadGeneration <= 0L || gen != currentLoadGeneration) {
                    return
                }
                val prevPlaying = _state.value.isPlaying
                val nowPlaying = !pausedByCore && !pausedForCache
                _state.update {
                    it.copy(
                        isInitialized = true,
                        isLoading = false,
                        isIdle = false,
                        isBuffering = pausedForCache,
                        isPlaying = nowPlaying,
                        error = null,
                    )
                }
                if (nowPlaying) {
                    _state.value.currentItem?.mediaId?.let(::startOrResumeWatchTracking)
                } else {
                    pauseWatchTimerOnly()
                }
                if (!prevPlaying && nowPlaying) {
                    _events.tryEmit(PlaybackEvent.IsPlayingChanged(true))
                }
                scope.launch { applyPostLoadPreferences(gen) }
            }
            MPVLib.MpvEvent.MPV_EVENT_VIDEO_RECONFIG -> {
                val w = PlaybackSession.getPropertyInt("video-params/w") ?: 0
                val h = PlaybackSession.getPropertyInt("video-params/h") ?: 0
                val aspect = PlaybackSession.getPropertyDouble("video-params/aspect")?.toFloat() ?: 0f
                _state.update {
                    it.copy(
                        videoWidth = if (w > 0) w else it.videoWidth,
                        videoHeight = if (h > 0) h else it.videoHeight,
                        videoAspectRatio = if (aspect > 0f) aspect else it.videoAspectRatio,
                    )
                }
                if (w > 0 && h > 0) {
                    _events.tryEmit(PlaybackEvent.VideoSizeChanged(w, h))
                }
                _events.tryEmit(PlaybackEvent.FirstFrameRendered)
            }
            MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
                if (pendingUserSeek) {
                    val user = pendingUserSeek
                    pendingUserSeek = false
                    _events.tryEmit(PlaybackEvent.SeekCompleted(_state.value.currentPositionMs, user))
                }
                _events.tryEmit(PlaybackEvent.FirstFrameRendered)
            }
            MPVLib.MpvEvent.MPV_EVENT_SEEK -> {
                // Seek initiated; PLAYBACK_RESTART signals seek settle
            }
            MPVLib.MpvEvent.MPV_EVENT_END_FILE -> {
                val sessionState = PlaybackSession.state.value
                if (sessionState.phase == PlaybackPhase.STOPPING ||
                    currentLoadGeneration <= 0L ||
                    sessionState.generation != currentLoadGeneration
                ) {
                    return
                }
                when (PlaybackSession.parseEndFileReason(data)) {
                    MpvEndFileReason.EOF -> {
                        val handledGen = currentLoadGeneration
                        scope.launch {
                            if (handledGen != currentLoadGeneration) return@launch
                            val current = _state.value
                            val completedId = current.currentItem?.mediaId
                            val watchFlush = drainPendingWatchDuration(clearActiveUri = true)
                            if (watchFlush != null || completedId != null) {
                                scope.launch(Dispatchers.IO) {
                                    if (watchFlush != null) {
                                        mediaRepository.addWatchDuration(watchFlush.first, watchFlush.second)
                                    }
                                    if (completedId != null) {
                                        mediaRepository.updateMediumPosition(completedId, 0L)
                                    }
                                }
                            }
                            _state.update { it.copy(currentPositionMs = 0L) }
                            when {
                                current.repeatMode == PlaybackLoopMode.ONE -> seekToItem(current.currentIndex, 0L)
                                current.hasNext -> seekToNext()
                                current.repeatMode == PlaybackLoopMode.ALL && current.playlist.isNotEmpty() -> {
                                    val firstIdx = if (current.shuffleEnabled && shuffleOrder.isNotEmpty()) shuffleOrder.first() else 0
                                    seekToItem(firstIdx, 0L)
                                }
                                else -> {
                                    _state.update {
                                        it.copy(isEnded = true, isPlaying = false, isLoading = false, isBuffering = false)
                                    }
                                    _events.tryEmit(PlaybackEvent.IsPlayingChanged(false))
                                    _events.tryEmit(PlaybackEvent.PlaybackEnded)
                                }
                            }
                        }
                    }
                    MpvEndFileReason.ERROR -> {
                        flushWatchAndPositionAsync(clearActiveUri = true)
                        val msg = PlaybackSession.parseEndFileError(data) ?: "Playback error occurred"
                        val failure = PlaybackFailure(
                            message = msg,
                            uri = _state.value.currentItem?.uri?.toString(),
                        )
                        Log.e(TAG, "libmpv playback error: $msg")
                        _state.update {
                            it.copy(isLoading = false, isBuffering = false, isPlaying = false, error = failure)
                        }
                        _events.tryEmit(PlaybackEvent.Error(failure))
                    }
                    else -> Unit
                }
            }
            MPVLib.MpvEvent.MPV_EVENT_SHUTDOWN -> {
                flushWatchAndPositionAsync(clearActiveUri = true)
            }
        }
    }

    private fun buildShuffleOrder(size: Int, anchorIndex: Int): List<Int> {
        if (size <= 0) return emptyList()
        val others = (0 until size).filter { it != anchorIndex }.shuffled()
        return if (anchorIndex in 0 until size) listOf(anchorIndex) + others else others
    }

    private fun LoopMode.toPlaybackLoopMode(): PlaybackLoopMode = when (this) {
        LoopMode.OFF -> PlaybackLoopMode.OFF
        LoopMode.ONE -> PlaybackLoopMode.ONE
        LoopMode.ALL -> PlaybackLoopMode.ALL
    }

    private companion object {
        const val TAG = "MpvPlaybackController"
        const val WATCH_FLUSH_THRESHOLD_MS = 5_000L
        const val MAX_WATCH_TICK_DELTA_MS = 15_000L
    }
}
