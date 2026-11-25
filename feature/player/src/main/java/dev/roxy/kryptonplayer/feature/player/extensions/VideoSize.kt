package dev.roxy.kryptonplayer.feature.player.extensions

data class PlayerVideoSize(
    val width: Int = 0,
    val height: Int = 0,
) {
    val isPortrait: Boolean
        get() = height > width

    companion object {
        val UNKNOWN = PlayerVideoSize(0, 0)
    }
}
