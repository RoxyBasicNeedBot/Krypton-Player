package dev.roxy.kryptonplayer.feature.player.mpv

enum class HdrScreenMode(val displayName: String) {
    OFF("Auto / SDR"),
    LINEAR("HDR Linear"),
    PQ("HDR10 (PQ)"),
    HLG("HLG"),
    BT2020("Wide Gamut (BT.2020)"),
}

object HdrScreenOutput {
    private fun hdrScreenOutputSettings(
        mode: HdrScreenMode,
        pipelineReady: Boolean,
        boostSdrToHdr: Boolean = false,
    ): List<Pair<String, String>> {
        if (mode == HdrScreenMode.OFF && !boostSdrToHdr) {
            return listOf(
                "target-colorspace-hint" to "auto",
                "target-trc" to "auto",
                "target-prim" to "auto",
                "tone-mapping" to "auto",
                "hdr-compute-peak" to "auto",
            )
        }
        val activeTrc = when (mode) {
            HdrScreenMode.OFF -> if (boostSdrToHdr) "pq" else "auto"
            HdrScreenMode.LINEAR -> "linear"
            HdrScreenMode.PQ -> "pq"
            HdrScreenMode.HLG -> "hlg"
            HdrScreenMode.BT2020 -> "auto"
        }
        val activePrim = when (mode) {
            HdrScreenMode.OFF -> if (boostSdrToHdr) "bt.2020" else "auto"
            else -> "bt.2020"
        }
        return buildList {
            add("target-colorspace-hint" to if (pipelineReady) "yes" else "no")
            add("target-trc" to activeTrc)
            add("target-prim" to activePrim)
            if (mode == HdrScreenMode.OFF && boostSdrToHdr) {
                add("tone-mapping" to "clip")
                add("hdr-compute-peak" to "no")
            } else {
                add("tone-mapping" to "bt.2390")
                add("hdr-compute-peak" to "yes")
            }
        }
    }

    fun applyOptions(
        mode: HdrScreenMode,
        pipelineReady: Boolean,
        boostSdrToHdr: Boolean = false,
    ) {
        if (mode == HdrScreenMode.OFF && !boostSdrToHdr && PlaybackSession.isNativeCorePendingInit) return
        hdrScreenOutputSettings(mode, pipelineReady, boostSdrToHdr).forEach { (property, value) ->
            PlaybackSession.setOptionString(property, value)
        }
    }

    fun applyProperties(
        mode: HdrScreenMode,
        pipelineReady: Boolean,
        boostSdrToHdr: Boolean = false,
    ) {
        hdrScreenOutputSettings(mode, pipelineReady, boostSdrToHdr).forEach { (property, value) ->
            PlaybackSession.setPropertyString(property, value)
        }
    }
}
