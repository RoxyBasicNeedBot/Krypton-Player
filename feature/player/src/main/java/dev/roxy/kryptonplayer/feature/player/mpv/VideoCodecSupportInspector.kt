package dev.roxy.kryptonplayer.feature.player.mpv

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import dev.roxy.kryptonplayer.core.model.DecoderPriority

object VideoCodecSupportInspector {
    private val SOFTWARE_DECODER_PREFIXES = listOf(
        "OMX.google.",
        "c2.android.",
        "OMX.SEC.avc.sw.",
        "OMX.ffmpeg.",
    )

    private val MIME_TO_MPV_CODEC = listOf(
        "video/avc" to "h264",
        "video/hevc" to "hevc",
        "video/x-vnd.on2.vp9" to "vp9",
        "video/x-vnd.on2.vp8" to "vp8",
        "video/av01" to "av1",
        "video/mp4v-es" to "mpeg4",
        "video/mpeg2" to "mpeg2video",
        "video/wvc1" to "vc1",
    )

    fun resolveHwdecOption(priority: DecoderPriority): String = when (priority) {
        DecoderPriority.PREFER_APP -> "no"
        DecoderPriority.DEVICE_ONLY -> "mediacodec,mediacodec-copy"
        DecoderPriority.PREFER_DEVICE -> "mediacodec,mediacodec-copy,no"
    }

    fun resolveSupportedHwdecCodecs(priority: DecoderPriority): String {
        if (priority == DecoderPriority.PREFER_APP) return ""
        return runCatching {
            val codecInfos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            val hardwareMimes = mutableSetOf<String>()
            for (info in codecInfos) {
                if (info.isEncoder) continue
                if (!isHardwareDecoder(info)) continue
                for (type in info.supportedTypes) {
                    hardwareMimes += type.lowercase()
                }
            }
            MIME_TO_MPV_CODEC
                .filter { (mime, _) -> mime in hardwareMimes }
                .joinToString(",") { (_, mpvName) -> mpvName }
        }.getOrDefault("h264,hevc,vp9")
    }

    private fun isHardwareDecoder(info: MediaCodecInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return info.isHardwareAccelerated
        }
        val name = info.name
        return SOFTWARE_DECODER_PREFIXES.none { name.startsWith(it, ignoreCase = true) }
    }
}
