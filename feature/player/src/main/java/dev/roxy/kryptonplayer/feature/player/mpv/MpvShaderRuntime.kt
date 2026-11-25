package dev.roxy.kryptonplayer.feature.player.mpv

interface MpvShaderRuntime {
    fun command(vararg args: String)

    fun getPropertyString(property: String): String?

    fun setPropertyString(
        property: String,
        value: String,
    )
}
