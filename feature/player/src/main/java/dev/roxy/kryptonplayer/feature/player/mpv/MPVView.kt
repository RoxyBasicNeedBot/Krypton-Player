package dev.roxy.kryptonplayer.feature.player.mpv

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.ViewGroup
import dev.roxy.kryptonplayer.core.model.Font
import dev.roxy.kryptonplayer.core.model.PlayerPreferences
import dev.roxy.kryptonplayer.feature.player.R
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.MPVLib

class MPVView(
    context: Context,
    attributes: AttributeSet,
) : BaseMPVView(context, attributes) {
    var forceOpenGlFallback: Boolean = false
    var surfaceBindingEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                isSurfaceReady = false
                PlaybackSession.unbindSurface(this)
            }
        }
    var isSurfaceReady: Boolean = false
        private set
    var onSurfaceReady: (() -> Unit)? = null

    private var cachedPreferences: PlayerPreferences = PlayerPreferences()
    private var gpuNextEnabled: Boolean = false
    private var currentAnime4KMode: Anime4KManager.Mode = Anime4KManager.Mode.OFF
    private var currentHdrMode: HdrScreenMode = HdrScreenMode.OFF

    private val anime4KManager: Anime4KManager by lazy { Anime4KManager(context.applicationContext) }
    private val hdrToysManager: HdrToysManager by lazy { HdrToysManager(context.applicationContext, PlaybackSession) }

    fun initializeSession(
        preferences: PlayerPreferences,
        gpuNextEnabled: Boolean = false,
    ): Result<Boolean> {
        cachedPreferences = preferences
        this.gpuNextEnabled = gpuNextEnabled
        val configKey = computeCoreConfigKey(
            context = context,
            preferences = preferences,
            anime4kEnabled = currentAnime4KMode != Anime4KManager.Mode.OFF,
            gpuNextEnabled = gpuNextEnabled,
            forceOpenGlFallback = forceOpenGlFallback,
        )
        val result = PlaybackSession.initialize(
            context = context,
            configDir = context.filesDir.path,
            cacheDir = context.cacheDir.path,
            coreConfigurationKey = configKey,
            initOptions = ::initOptions,
            postInitOptions = ::postInitOptions,
            observeProperties = ::observeProperties,
        )
        if (result.isSuccess) {
            if (result.getOrNull() == true) {
                isSurfaceReady = false
            }
            holder.removeCallback(this)
            holder.addCallback(this)
            if (surfaceBindingEnabled && holder.surface?.isValid == true) {
                surfaceCreated(holder)
            }
        } else {
            isSurfaceReady = false
        }
        return result
    }

    fun releaseSurface() {
        holder.removeCallback(this)
        isSurfaceReady = false
        PlaybackSession.unbindSurface(this)
    }

    fun rebindCurrentSurface(): Boolean {
        if (!surfaceBindingEnabled || holder.surface?.isValid != true) return false
        holder.removeCallback(this)
        holder.addCallback(this)
        val bound = bindHolderSurface(holder)
        if (bound) {
            notifySurfaceReady()
        }
        return bound
    }

    private fun bindHolderSurface(
        holder: SurfaceHolder,
        width: Int? = null,
        height: Int? = null,
    ): Boolean {
        val surface = holder.surface ?: return false
        if (!surfaceBindingEnabled || !surface.isValid) return false
        val frame = holder.surfaceFrame
        val resolvedWidth = width?.takeIf { it > 0 }
            ?: this.width.takeIf { it > 0 }
            ?: frame.width().takeIf { it > 0 }
        val resolvedHeight = height?.takeIf { it > 0 }
            ?: this.height.takeIf { it > 0 }
            ?: frame.height().takeIf { it > 0 }
        isSurfaceReady = PlaybackSession.bindSurface(
            surface = surface,
            width = resolvedWidth,
            height = resolvedHeight,
            owner = this,
        )
        return isSurfaceReady
    }

    private fun notifySurfaceReady() {
        post {
            if (isSurfaceReady && holder.surface?.isValid == true) {
                onSurfaceReady?.invoke()
            }
        }
    }

    fun applyAnime4KMode(mode: Anime4KManager.Mode, quality: Anime4KManager.Quality = Anime4KManager.DEFAULT_QUALITY) {
        currentAnime4KMode = mode
        if (!PlaybackSession.isInitialized) return
        if (mode == Anime4KManager.Mode.OFF) {
            clearAnime4KShaders()
            return
        }
        val selection = selectRuntimeStableAnime4K(mode, quality, context)
        if (selection.mode == Anime4KManager.Mode.OFF) {
            clearAnime4KShaders()
        } else {
            applyAnime4KShaderChain(anime4KManager, selection.mode, selection.quality)
        }
    }

    fun applyHdrMode(mode: HdrScreenMode, toysProfile: HdrToysProfile = HdrToysProfile.OFF) {
        currentHdrMode = mode
        if (!PlaybackSession.isInitialized) return
        HdrScreenOutput.applyProperties(mode, pipelineReady = gpuNextEnabled)
        if (toysProfile != HdrToysProfile.OFF) {
            hdrToysManager.apply(toysProfile)
        } else {
            hdrToysManager.clear()
        }
    }

    override fun initOptions() {
        applyInitOptions(
            context = context,
            preferences = cachedPreferences,
            anime4kEnabled = currentAnime4KMode != Anime4KManager.Mode.OFF,
            gpuNextEnabled = gpuNextEnabled,
            forceOpenGlFallback = forceOpenGlFallback,
            hdrMode = currentHdrMode,
        )
    }

    override fun postInitOptions() {
        if (currentAnime4KMode != Anime4KManager.Mode.OFF) {
            applyAnime4KMode(currentAnime4KMode)
        }
        if (currentHdrMode != HdrScreenMode.OFF) {
            HdrScreenOutput.applyProperties(currentHdrMode, pipelineReady = gpuNextEnabled)
        }
    }

    override fun observeProperties() {
        observeDefaultProperties()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (bindHolderSurface(holder)) {
            notifySurfaceReady()
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        if (!surfaceBindingEnabled || width <= 0 || height <= 0) return
        if (!isSurfaceReady) {
            if (bindHolderSurface(holder, width = width, height = height)) {
                notifySurfaceReady()
            }
        } else {
            PlaybackSession.resizeSurface(width = width, height = height, owner = this)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceReady = false
        PlaybackSession.unbindSurface(this)
    }

    companion object {
        fun create(context: Context): MPVView =
            (LayoutInflater.from(context).inflate(R.layout.mpv_surface_view, null, false) as MPVView).apply {
                if (layoutParams == null) {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            }

        fun computeCoreConfigKey(
            context: Context,
            preferences: PlayerPreferences,
            anime4kEnabled: Boolean = false,
            gpuNextEnabled: Boolean = false,
            forceOpenGlFallback: Boolean = false,
        ): String {
            val vulkanCapable = VulkanCapabilities.isVulkanSupported(context)
            val backend = RendererBackendPolicy.select(
                anime4kEnabled = anime4kEnabled,
                gpuNextEnabled = gpuNextEnabled,
                vulkanCapable = vulkanCapable,
                forceOpenGlFallback = forceOpenGlFallback,
            )
            return "${backend.configurationKey}|dec=${preferences.decoderPriority.name}"
        }

        fun applyInitOptions(
            context: Context,
            preferences: PlayerPreferences,
            anime4kEnabled: Boolean = false,
            gpuNextEnabled: Boolean = false,
            forceOpenGlFallback: Boolean = false,
            hdrMode: HdrScreenMode = HdrScreenMode.OFF,
        ) {
            val vulkanCapable = VulkanCapabilities.isVulkanSupported(context)
            val backend = RendererBackendPolicy.select(
                anime4kEnabled = anime4kEnabled,
                gpuNextEnabled = gpuNextEnabled,
                vulkanCapable = vulkanCapable,
                forceOpenGlFallback = forceOpenGlFallback,
            )
            PlaybackSession.setVideoOutput(backend.vo)
            PlaybackSession.setOptionString("gpu-context", backend.gpuContext)
            PlaybackSession.setOptionString("gpu-api", backend.gpuApi)
            PlaybackSession.setOptionString("hwdec", VideoCodecSupportInspector.resolveHwdecOption(preferences.decoderPriority))
            val hwdecCodecs = VideoCodecSupportInspector.resolveSupportedHwdecCodecs(preferences.decoderPriority)
            if (hwdecCodecs.isNotEmpty()) {
                PlaybackSession.setOptionString("hwdec-codecs", hwdecCodecs)
            }
            PlaybackSession.setOptionString("ao", "audiotrack,opensles")
            PlaybackSession.setOptionString("volume-max", "350")
            PlaybackSession.setOptionString("keep-open", "no")
            PlaybackSession.setOptionString("hr-seek", "yes")
            PlaybackSession.setOptionString("hr-seek-framedrop", "yes")

            if (preferences.preferredAudioLanguage.isNotBlank()) {
                PlaybackSession.setOptionString("alang", preferences.preferredAudioLanguage)
            }
            if (preferences.preferredSubtitleLanguage.isNotBlank()) {
                PlaybackSession.setOptionString("slang", preferences.preferredSubtitleLanguage)
            }
            PlaybackSession.setOptionString(
                "sub-ass-override",
                if (preferences.applyEmbeddedStyles) "no" else "force",
            )
            PlaybackSession.setOptionString("sub-font", preferences.subtitleFont.toMpvFontName())
            PlaybackSession.setOptionString("sub-font-size", (preferences.subtitleTextSize * 2).coerceIn(16, 120).toString())
            PlaybackSession.setOptionString("sub-bold", if (preferences.subtitleTextBold) "yes" else "no")
            if (preferences.subtitleBackground) {
                PlaybackSession.setOptionString("sub-back-color", "#80000000")
            }
            if (preferences.subtitleTextEncoding.isNotBlank()) {
                PlaybackSession.setOptionString("sub-codepage", preferences.subtitleTextEncoding)
            }
            if (anime4kEnabled) {
                applyAnime4KStabilityOptions(backend == MpvRenderBackend.VULKAN_GPU_NEXT)
            }
            HdrScreenOutput.applyOptions(hdrMode, pipelineReady = gpuNextEnabled)
        }

        fun observeDefaultProperties() {
            with(PlaybackSession) {
                observeProperty("time-pos", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
                observeProperty("duration", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
                observeProperty("demuxer-cache-time", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
                observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
                observeProperty("core-idle", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
                observeProperty("paused-for-cache", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
                observeProperty("track-list", MPVLib.MpvFormat.MPV_FORMAT_NODE)
                observeProperty("speed", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
                observeProperty("video-params/w", MPVLib.MpvFormat.MPV_FORMAT_INT64)
                observeProperty("video-params/h", MPVLib.MpvFormat.MPV_FORMAT_INT64)
                observeProperty("video-params/aspect", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
                observeProperty("video-params/gamma", MPVLib.MpvFormat.MPV_FORMAT_STRING)
                observeProperty("container-fps", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
                observeProperty("video-codec", MPVLib.MpvFormat.MPV_FORMAT_STRING)
                observeProperty("audio-codec", MPVLib.MpvFormat.MPV_FORMAT_STRING)
                observeProperty("hwdec-current", MPVLib.MpvFormat.MPV_FORMAT_STRING)
                observeProperty("video-bitrate", MPVLib.MpvFormat.MPV_FORMAT_INT64)
                observeProperty("audio-bitrate", MPVLib.MpvFormat.MPV_FORMAT_INT64)
                observeProperty("aid", MPVLib.MpvFormat.MPV_FORMAT_STRING)
                observeProperty("sid", MPVLib.MpvFormat.MPV_FORMAT_STRING)
            }
        }
    }
}

private fun Font.toMpvFontName(): String = when (this) {
    Font.DEFAULT -> "sans-serif"
    Font.MONOSPACE -> "monospace"
    Font.SANS_SERIF -> "sans-serif"
    Font.SERIF -> "serif"
}
