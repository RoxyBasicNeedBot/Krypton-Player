package dev.roxy.kryptonplayer.feature.player.mpv

enum class MpvRenderBackend(
    val vo: String,
    val gpuApi: String,
    val gpuContext: String,
    val hwdec: String,
    val configurationKey: String,
) {
    OPENGL_GPU(
        vo = "gpu",
        gpuApi = "opengl",
        gpuContext = "android",
        hwdec = "mediacodec,mediacodec-copy",
        configurationKey = "opengl-gpu",
    ),
    OPENGL_GPU_NEXT(
        vo = "gpu-next",
        gpuApi = "opengl",
        gpuContext = "android",
        hwdec = "mediacodec,mediacodec-copy",
        configurationKey = "opengl-gpu-next",
    ),
    VULKAN_GPU_NEXT(
        vo = "gpu-next",
        gpuApi = "vulkan",
        gpuContext = "androidvk",
        hwdec = "mediacodec-copy",
        configurationKey = "vulkan-gpu-next",
    ),
}

object RendererBackendPolicy {
    fun select(
        anime4kEnabled: Boolean,
        gpuNextEnabled: Boolean,
        vulkanCapable: Boolean,
        forceOpenGlFallback: Boolean = false,
    ): MpvRenderBackend {
        if (forceOpenGlFallback) {
            return MpvRenderBackend.OPENGL_GPU
        }
        return when {
            gpuNextEnabled && vulkanCapable -> MpvRenderBackend.VULKAN_GPU_NEXT
            gpuNextEnabled -> MpvRenderBackend.OPENGL_GPU_NEXT
            anime4kEnabled -> MpvRenderBackend.OPENGL_GPU
            else -> MpvRenderBackend.OPENGL_GPU
        }
    }
}
