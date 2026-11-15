package dev.anilbeesetti.nextplayer.core.common

import android.Manifest
import android.content.res.Resources
import android.os.Build
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

val storagePermission = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_VIDEO
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Manifest.permission.READ_EXTERNAL_STORAGE
    else -> Manifest.permission.WRITE_EXTERNAL_STORAGE
}

/**
 * Utility functions.
 */
object Utils {

    /**
     * Converts px to dp.
     */
    fun pxToDp(px: Float): Float {
        return px / Resources.getSystem().displayMetrics.density
    }

    /**
     * Formats the given duration in milliseconds to a string in the format of `mm:ss` or `hh:mm:ss`.
     */
    fun formatDurationMillis(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
            TimeUnit.MINUTES.toSeconds(minutes) -
            TimeUnit.HOURS.toSeconds(hours)
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats the given duration in milliseconds to a string in the format of
     * `+mm:ss` or `+hh:mm:ss` or `-mm:ss` or `-hh:mm:ss`.
     */
    fun formatDurationMillisSign(millis: Long): String {
        return if (millis >= 0) {
            "+${formatDurationMillis(millis)}"
        } else {
            "-${formatDurationMillis(abs(millis))}"
        }
    }

    fun formatFileSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size < kb -> "$size B"
            size < mb -> "%.2f KB".format(size / kb.toDouble())
            size < gb -> "%.2f MB".format(size / mb.toDouble())
            else -> "%.2f GB".format(size / gb.toDouble())
        }
    }

    fun formatBitrate(bitrate: Long): String? {
        if (bitrate <= 0) {
            return null
        }

        val kiloBitrate = bitrate.toDouble() / 1000.0
        val megaBitrate = kiloBitrate / 1000.0
        val gigaBitrate = megaBitrate / 1000.0

        return when {
            gigaBitrate >= 1.0 -> String.format("%.1f Gbps", gigaBitrate)
            megaBitrate >= 1.0 -> String.format("%.1f Mbps", megaBitrate)
            kiloBitrate >= 1.0 -> String.format("%.1f kbps", kiloBitrate)
            else -> String.format("%d bps", bitrate)
        }
    }

    fun formatLanguage(language: String?): String? {
        return language?.let { lang -> Locale.forLanguageTag(lang).displayLanguage.takeIf { it.isNotEmpty() } }
    }

    /**
     * Cleans raw Telegram / Cloud / local movie filenames and extracts (CleanTitle, Year).
     */
    fun cleanMediaTitle(mediaTitle: String): Pair<String, String?> {
        // 1. Normalize unicode (e.g., Mathematical bold 𝗙𝗶𝗹𝗲 𝗡𝗮𝗺𝗲 -> File Name)
        var raw = java.text.Normalizer.normalize(mediaTitle, java.text.Normalizer.Form.NFKD)
            .replace(Regex("""\.[a-zA-Z0-9]{2,4}$"""), "") // Remove extension
            .replace(Regex("""https?://\S+"""), " ") // Remove URLs
            .replace(Regex("""@[a-zA-Z0-9_]+"""), " ") // Remove Telegram handles
            .replace(Regex("""[➥➔►▶★⭐🔥✨🎬]"""), " ") // Remove symbols
        // Convert all separators and punctuation to spaces safely
        raw = raw.map { c ->
            if (c in "._-+()[]{}:,;!|~/#@*^&`'\"\\") ' ' else c
        }.joinToString("")
        raw = raw.replace(Regex("""\s+"""), " ").trim()

        // Extract 4-digit release year (1900-2029)
        val yearMatch = Regex("""\b(19\d{2}|20[0-2]\d)\b""").find(raw)
        val extractedYear = yearMatch?.value

        // Strip resolution, codecs, source, audio, release groups, and language tags
        val cleaned = raw.replace(
            Regex("""(?i)\b(1080p|720p|480p|360p|2160p|4k|2k|uhd|fhd|hd|sd|hdr|hdr10|hdr10plus|dv|dovi|dolby|vision|atmos|ddp|dd5\.1|ddp5\.1|5\.1|7\.1|dts|dts-hd|ac3|eac3|aac|flac|mp3|hevc|x264|x265|h264|h265|h\s*265|h\s*264|avc|10bit|8bit|bluray|blu-ray|bdrip|brrip|webrip|web-dl|webdl|web|hdtv|hdrip|dvdrip|dvd|remux|proper|repack|extended|unrated|directors cut|imax|multi|dual\s*audio|hindi|dubbed|dub|org|english|tamil|telugu|kannada|malayalam|korean|japanese|chinese|esubs|esub|msub|sub|subs|yts|yify|combined|nf|netflix|amzn|dsnp|zee5|hotstar|sonyliv|voot|jio|season|episode|s\d{1,2}e\d{1,2}|s\d{1,2}|e\d{1,2}|dl|mkv|mp4)\b"""),
            " "
        ).replace(Regex("""\s+"""), " ").trim()

        val titleWithoutYear = if (extractedYear != null) {
            cleaned.replace(extractedYear, " ").replace(Regex("""\s+"""), " ").trim()
        } else {
            cleaned
        }

        val finalTitle = if (titleWithoutYear.length >= 2) titleWithoutYear else if (cleaned.length >= 2) cleaned else raw.take(40)
        return Pair(finalTitle, extractedYear)
    }
}
