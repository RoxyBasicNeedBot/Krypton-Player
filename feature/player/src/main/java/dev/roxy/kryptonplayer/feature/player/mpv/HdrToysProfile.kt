package dev.roxy.kryptonplayer.feature.player.mpv

enum class HdrToysProfile(
    val displayName: String,
    val shaderPaths: List<String>,
) {
    OFF(
        displayName = "Off",
        shaderPaths = emptyList(),
    ),
    BT2390(
        displayName = "BT.2390 (Broadcast HDR)",
        shaderPaths = listOf(
            "hdr-toys/transfer-function/pq_inv.glsl",
            "hdr-toys/utils/transform.glsl",
            "hdr-toys/tone-mapping/bt2390.glsl",
            "hdr-toys/gamut-mapping/bottosson.glsl",
            "hdr-toys/transfer-function/bt1886.glsl",
        ),
    ),
    BT2446A(
        displayName = "BT.2446A (Perceptual)",
        shaderPaths = listOf(
            "hdr-toys/transfer-function/pq_inv.glsl",
            "hdr-toys/utils/transform.glsl",
            "hdr-toys/tone-mapping/bt2446a.glsl",
            "hdr-toys/gamut-mapping/bottosson.glsl",
            "hdr-toys/transfer-function/bt1886.glsl",
        ),
    ),
    ST2094_10(
        displayName = "SMPTE ST 2094-10 (Dynamic)",
        shaderPaths = listOf(
            "hdr-toys/transfer-function/pq_inv.glsl",
            "hdr-toys/utils/transform.glsl",
            "hdr-toys/tone-mapping/st2094-10.glsl",
            "hdr-toys/gamut-mapping/jedypod.glsl",
            "hdr-toys/transfer-function/bt1886.glsl",
        ),
    ),
    REINHARD(
        displayName = "Reinhard (Filmic Roll-Off)",
        shaderPaths = listOf(
            "hdr-toys/transfer-function/pq_inv.glsl",
            "hdr-toys/utils/transform.glsl",
            "hdr-toys/tone-mapping/reinhard.glsl",
            "hdr-toys/gamut-mapping/clip.glsl",
            "hdr-toys/transfer-function/bt1886.glsl",
        ),
    ),
    ;

    companion object {
        val allShaderPaths: Set<String> =
            entries.flatMap { it.shaderPaths }.toSet()

        fun mpvPaths(profile: HdrToysProfile): List<String> =
            profile.shaderPaths.map { "~~/shaders/$it" }
    }
}
