package dev.roxy.kryptonplayer.feature.player.mpv

import android.content.Context
import android.util.Log

internal data class Anime4KSelection(
    val mode: Anime4KManager.Mode,
    val quality: Anime4KManager.Quality,
    val reason: String? = null,
)

internal fun selectRuntimeStableAnime4K(
    mode: Anime4KManager.Mode,
    quality: Anime4KManager.Quality,
    context: Context? = null,
    enableIn4k: Boolean = false,
): Anime4KSelection {
    val width = PlaybackSession.getPropertyInt("video-params/w") ?: 0
    val height = PlaybackSession.getPropertyInt("video-params/h") ?: 0
    val pixels = width.toLong() * height.toLong()

    if (!enableIn4k && pixels >= 3840L * 2160L) {
        return Anime4KSelection(
            mode = Anime4KManager.Mode.OFF,
            quality = Anime4KManager.DEFAULT_QUALITY,
            reason = "Disabled Anime4K for 4K+ playback to prevent thermal throttling",
        )
    }

    if (context != null) {
        val headroom = ThermalMonitor.getHeadroom(context)
        if (ThermalMonitor.shouldThrottleAnime4K(headroom)) {
            Log.i(
                "Anime4KPlayback",
                "Thermal headroom high (%.2f) — preemptively downgrading Anime4K to C/Fast".format(headroom),
            )
            return Anime4KSelection(
                mode = Anime4KManager.Mode.C,
                quality = Anime4KManager.Quality.FAST,
                reason = "Thermal headroom high; downgraded to C/Fast",
            )
        }
    }

    val droppedFrames = PlaybackSession.getPropertyInt("frame-drop-count") ?: 0
    val delayedFrames = PlaybackSession.getPropertyInt("vo-delayed-frame-count") ?: 0
    val mistimedFrames = PlaybackSession.getPropertyInt("mistimed-frame-count") ?: 0
    if (droppedFrames >= 15 || delayedFrames >= 25 || mistimedFrames >= 40) {
        return Anime4KSelection(
            mode = Anime4KManager.Mode.C,
            quality = Anime4KManager.Quality.FAST,
            reason = "Runtime frame pressure detected; downgraded to C/Fast",
        )
    }

    return Anime4KSelection(mode = mode, quality = quality)
}

internal fun clearAnime4KShaders() {
    withPreservedVideoGeometry {
        setShaderList(currentShaderList().filterNot(::isBuiltInAnime4KShaderPath))
    }
}

internal fun applyAnime4KShaderChain(
    anime4kManager: Anime4KManager,
    mode: Anime4KManager.Mode,
    quality: Anime4KManager.Quality,
): Boolean {
    if (!anime4kManager.initialize()) return false
    val shaderPaths = anime4kManager.getShaderPaths(mode, quality)
    if (shaderPaths.isEmpty()) return false

    withPreservedVideoGeometry {
        val retainedShaders = currentShaderList().filterNot(::isBuiltInAnime4KShaderPath)
        setShaderList(shaderPaths + retainedShaders)
    }
    return true
}

internal fun applyAnime4KStabilityOptions(useVulkan: Boolean) {
    if (!useVulkan) {
        PlaybackSession.setOptionString("opengl-pbo", "yes")
        PlaybackSession.setOptionString("opengl-early-flush", "no")
    }
    PlaybackSession.setOptionString("vd-lavc-dr", "yes")
}

private data class VideoGeometrySnapshot(
    val doubles: Map<String, Double>,
    val strings: Map<String, String>,
)

private inline fun withPreservedVideoGeometry(block: () -> Unit) {
    val snapshot = captureVideoGeometry()
    block()
    restoreVideoGeometry(snapshot)
}

private fun captureVideoGeometry(): VideoGeometrySnapshot =
    VideoGeometrySnapshot(
        doubles = VIDEO_GEOMETRY_DOUBLE_PROPS
            .mapNotNull { prop -> PlaybackSession.getPropertyDouble(prop)?.let { prop to it } }
            .toMap(),
        strings = VIDEO_GEOMETRY_STRING_PROPS
            .mapNotNull { prop -> PlaybackSession.getPropertyString(prop)?.takeIf { it.isNotBlank() }?.let { prop to it } }
            .toMap(),
    )

private fun restoreVideoGeometry(snapshot: VideoGeometrySnapshot) {
    snapshot.doubles.forEach { (prop, value) -> runCatching { PlaybackSession.setPropertyDouble(prop, value) } }
    snapshot.strings.forEach { (prop, value) -> runCatching { PlaybackSession.setPropertyString(prop, value) } }
}

private val VIDEO_GEOMETRY_DOUBLE_PROPS = listOf(
    "video-zoom",
    "video-pan-x",
    "video-pan-y",
    "video-align-x",
    "video-align-y",
    "video-aspect-override",
    "panscan",
    "brightness",
    "contrast",
    "saturation",
    "gamma",
    "hue",
    "sharpen",
)

private val VIDEO_GEOMETRY_STRING_PROPS = listOf("video-unscaled")

private fun currentShaderList(): List<String> =
    PlaybackSession
        .getPropertyString("glsl-shaders")
        ?.split(":")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()

private fun setShaderList(shaderPaths: List<String>) {
    PlaybackSession.setPropertyString("glsl-shaders", shaderPaths.joinToString(":"))
}

private fun isBuiltInAnime4KShaderPath(path: String): Boolean {
    val fileName = path.replace('\\', '/').substringAfterLast('/')
    return fileName in Anime4KManager.BUILT_IN_SHADER_FILES
}
