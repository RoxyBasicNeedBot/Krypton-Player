package dev.roxy.kryptonplayer.feature.player.mpv

import dev.roxy.kryptonplayer.core.common.playback.PlaybackTrack
import `is`.xyz.mpv.MPVNode

internal object MpvTrackSelector {
    data class TrackSnapshot(
        val videoTracks: List<PlaybackTrack>,
        val audioTracks: List<PlaybackTrack>,
        val subtitleTracks: List<PlaybackTrack>,
    )

    fun parseTrackList(node: MPVNode?): TrackSnapshot {
        val entries = node?.asArray() ?: return TrackSnapshot(emptyList(), emptyList(), emptyList())
        val videoTracks = mutableListOf<PlaybackTrack>()
        val audioTracks = mutableListOf<PlaybackTrack>()
        val subtitleTracks = mutableListOf<PlaybackTrack>()

        for (entry in entries) {
            val map = entry.asMap() ?: continue
            val type = map["type"]?.asString() ?: continue
            val mpvId = map["id"]?.asInt()?.toInt() ?: continue
            val title = map["title"]?.asString().orEmpty()
            val lang = map["lang"]?.asString()
            val codec = map["codec"]?.asString()
            val selected = map["selected"]?.asBoolean() == true
            val isDefault = map["default"]?.asBoolean() == true
            val isForced = map["forced"]?.asBoolean() == true
            val isExternal = map["external"]?.asBoolean() == true
            val channels = (map["demux-channel-count"]?.asInt() ?: map["audio-channels"]?.asInt() ?: 0L).toInt()
            val sampleRate = (map["demux-samplerate"]?.asInt() ?: 0L).toInt()
            val bitrate = (map["demux-bitrate"]?.asInt() ?: 0L).toInt()
            val width = (map["demux-w"]?.asInt() ?: 0L).toInt()
            val height = (map["demux-h"]?.asInt() ?: 0L).toInt()
            val fps = (map["demux-fps"]?.asDouble() ?: 0.0).toFloat()

            val targetList = when (type) {
                "video" -> videoTracks
                "audio" -> audioTracks
                "sub" -> subtitleTracks
                else -> continue
            }
            val index = targetList.size
            val fallbackTitle = buildFallbackTitle(type, index, lang, title)
            targetList += PlaybackTrack(
                id = mpvId,
                index = index,
                title = fallbackTitle,
                language = lang,
                codec = codec,
                mimeType = codec,
                isSelected = selected,
                isDefault = isDefault,
                isForced = isForced,
                isExternal = isExternal,
                channels = channels,
                sampleRate = sampleRate,
                bitrate = bitrate,
                width = width,
                height = height,
                fps = fps,
            )
        }
        return TrackSnapshot(
            videoTracks = videoTracks,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
        )
    }

    fun selectBestAudioTrack(
        audioTracks: List<PlaybackTrack>,
        preferredLangs: List<String>,
    ): PlaybackTrack? {
        if (audioTracks.isEmpty()) return null
        val ignoreKeywords = listOf("commentary", "description", "adh", "comment", "extra")
        for (pref in preferredLangs) {
            val normalized = pref.trim().lowercase()
            if (normalized.isEmpty()) continue
            val match = audioTracks.firstOrNull { track ->
                val lang = track.language?.lowercase().orEmpty()
                (lang == normalized || lang.startsWith(normalized)) &&
                    ignoreKeywords.none { track.title.contains(it, ignoreCase = true) }
            }
            if (match != null) return match
        }
        return audioTracks.firstOrNull { track ->
            track.isDefault && ignoreKeywords.none { track.title.contains(it, ignoreCase = true) }
        } ?: audioTracks.firstOrNull()
    }

    fun selectBestSubtitleTrack(
        subtitleTracks: List<PlaybackTrack>,
        preferredLangs: List<String>,
    ): PlaybackTrack? {
        if (subtitleTracks.isEmpty()) return null
        val external = subtitleTracks.firstOrNull { it.isExternal }
        if (external != null) return external
        val ignoreSubs = listOf("signs", "songs", "lyrics", "forced", "sdh", "colored", "karaoke")
        for (pref in preferredLangs) {
            val normalized = pref.trim().lowercase()
            if (normalized.isEmpty()) continue
            val cleanMatch = subtitleTracks.firstOrNull { track ->
                val lang = track.language?.lowercase().orEmpty()
                (lang == normalized || lang.startsWith(normalized)) &&
                    !track.isForced &&
                    ignoreSubs.none { track.title.contains(it, ignoreCase = true) }
            }
            if (cleanMatch != null) return cleanMatch
            val anyLangMatch = subtitleTracks.firstOrNull { track ->
                val lang = track.language?.lowercase().orEmpty()
                lang == normalized || lang.startsWith(normalized)
            }
            if (anyLangMatch != null) return anyLangMatch
        }
        return subtitleTracks.firstOrNull { it.isDefault }
    }

    private fun buildFallbackTitle(
        type: String,
        index: Int,
        lang: String?,
        rawTitle: String,
    ): String {
        val cleanTitle = rawTitle.trim()
        val cleanLang = lang?.trim()?.takeIf { it.isNotEmpty() && !it.equals("und", ignoreCase = true) }
        return when {
            cleanTitle.isNotEmpty() && cleanLang != null && !cleanTitle.contains(cleanLang, ignoreCase = true) ->
                "$cleanTitle ($cleanLang)"
            cleanTitle.isNotEmpty() -> cleanTitle
            cleanLang != null -> "${cleanLang.uppercase()} Track ${index + 1}"
            type == "audio" -> "Audio Track ${index + 1}"
            type == "sub" -> "Subtitle Track ${index + 1}"
            else -> "Video Track ${index + 1}"
        }
    }
}
