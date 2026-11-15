package dev.anilbeesetti.nextplayer.core.data.lyrics

import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository

@Singleton
class LyricsRepository @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {

    companion object {
        private const val TAG = "LyricsRepository"
        private const val LRCLIB_URL = "https://lrclib.net/api"
        private const val NETEASE_SEARCH_URL = "https://music.163.com/api/search/get/web"
        private const val NETEASE_LYRIC_URL = "https://music.163.com/api/song/lyric"
        private const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"

        fun cleanVideoSongTitle(rawTitle: String): String {
            var text = rawTitle

            // Extract from URI query params if present (e.g. tdlib://stream?...&name=Song.mp4)
            if (text.contains("name=") || text.contains("fileName=")) {
                try {
                    val uri = Uri.parse(text)
                    val param = uri.getQueryParameter("name") ?: uri.getQueryParameter("fileName")
                    if (!param.isNullOrBlank()) {
                        text = param
                    }
                } catch (_: Exception) {}
            }

            // URL Decode
            try {
                text = URLDecoder.decode(text, "UTF-8")
            } catch (_: Exception) {}

            // Take basename if path
            if (text.contains("/") || text.contains("\\")) {
                text = text.replace("\\", "/").substringAfterLast("/")
            }

            // Strip video and audio extensions
            text = text.replace(Regex("(?i)\\.(mp4|mkv|webm|avi|mov|flv|ts|m4v|mp3|aac|flac|wav|ogg|opus|m4a)$"), "")

            // Replace separators with spaces
            text = text.replace("_", " ").replace("+", " ")
            text = text.replace(Regex("(?<!\\bfeat)(?<!\\bft)\\.", RegexOption.IGNORE_CASE), " ")

            // Remove channel tags (@channel, t.me/channel)
            text = text.replace(Regex("@[a-zA-Z0-9_]+"), "")
            text = text.replace(Regex("(?i)t\\.me/[a-zA-Z0-9_]+"), "")

            // Remove resolution & codec brackets and words
            text = text.replace(Regex("(?i)\\b(1080p|720p|480p|360p|4k|2160p|8k|hdr|bluray|web-dl|webrip|dvdrip|x264|x265|hevc|60fps|aac|dts|hd)\\b"), "")

            // Remove bracketed labels containing video keywords e.g. [Full Song], (Official Video), (Audio Song), [Music Video], [Lyrics]
            text = text.replace(Regex("(?i)[\\[\\(\\{][^\\]\\)\\}]*?(?:official|video|audio|lyrics|lyric|remix|1080p|720p|4k|hd|song|teaser|trailer|lofi|slowed)[^\\]\\)\\}]*?[\\]\\)\\}]"), "")

            // Remove standalone video keywords
            text = text.replace(Regex("(?i)\\b(official music video|official video|music video|lyric video|lyrics video|lyrical video|full video|video song|full song|audio song|official audio|audio song|short film|visualizer|lyrics)\\b"), "")

            // Remove empty brackets
            text = text.replace(Regex("[\\[\\(\\{]\\s*[\\]\\)\\}]"), "")

            // Handle YouTube Pipe '|' clutter
            if (text.contains("|")) {
                val pipes = text.split("|").map { it.trim() }.filter { it.isNotBlank() }
                text = pipes.take(2).joinToString(" - ")
            }

            val cleaned = text.replace(Regex("\\s+"), " ").trim()
            return if (cleaned.isBlank()) rawTitle.substringBeforeLast(".").trim() else cleaned
        }
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    // Fast in-memory cache
    private val memoryCache = ConcurrentHashMap<String, LyricsData>()

    suspend fun searchLyrics(
        rawTitle: String,
        artist: String? = null,
        durationSeconds: Int? = null
    ): Result<LyricsData> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = cleanVideoSongTitle(rawTitle)
            val cacheKey = "${cleanTitle.lowercase()}_${(artist ?: "").lowercase()}"

            // Check cache
            memoryCache[cacheKey]?.let { cached ->
                return@runCatching cached
            }

            val candidates = generateCandidates(cleanTitle, artist)
            Log.i(TAG, "Searching lyrics for: '$rawTitle' -> Clean: '$cleanTitle' (${candidates.size} candidates)")

            val prefs = preferencesRepository.playerPreferences.value

            // 1. Try BetterLyrics (ArchiveTune Engine)
            if (prefs.enableBetterLyrics) {
                for (candidate in candidates) {
                    val betterResult = fetchFromBetterLyrics(candidate.first, candidate.second)
                    if (betterResult != null && (betterResult.isSynced || !betterResult.plainLyrics.isNullOrBlank())) {
                        Log.i(TAG, "[LYRICS] Found via BetterLyrics: ${betterResult.trackName} - ${betterResult.artistName}")
                        memoryCache[cacheKey] = betterResult
                        return@runCatching betterResult
                    }
                }
            }

            // 2. Try LRCLIB across candidates
            if (prefs.enableLrcLib) {
                for (candidate in candidates) {
                    val lrcLibResult = fetchFromLrcLib(candidate.first, candidate.second, durationSeconds)
                    if (lrcLibResult != null && (lrcLibResult.isSynced || !lrcLibResult.plainLyrics.isNullOrBlank())) {
                        Log.i(TAG, "[LYRICS] Found via LRCLIB: ${lrcLibResult.trackName} - ${lrcLibResult.artistName}")
                        memoryCache[cacheKey] = lrcLibResult
                        return@runCatching lrcLibResult
                    }
                }
            }

            // 3. Try NetEase 163 Cloud Music Engine (ArchiveTune Engine)
            if (prefs.enableNetEase) {
                for (candidate in candidates) {
                    val searchTerm = if (!candidate.second.isNullOrBlank()) "${candidate.first} ${candidate.second}" else candidate.first
                    val neteaseResult = fetchFromNetEase(searchTerm)
                    if (neteaseResult != null && (neteaseResult.isSynced || !neteaseResult.plainLyrics.isNullOrBlank())) {
                        Log.i(TAG, "[LYRICS] Found via NetEase Cloud Music: ${neteaseResult.trackName} - ${neteaseResult.artistName}")
                        memoryCache[cacheKey] = neteaseResult
                        return@runCatching neteaseResult
                    }
                }
            }

            // 4. Try KuGou Lyrics Engine (ArchiveTune Engine)
            if (prefs.enableKugou) {
                for (candidate in candidates) {
                    val kugouResult = fetchFromKuGou(candidate.first, candidate.second)
                    if (kugouResult != null && (kugouResult.isSynced || !kugouResult.plainLyrics.isNullOrBlank())) {
                        Log.i(TAG, "[LYRICS] Found via KuGou: ${kugouResult.trackName} - ${kugouResult.artistName}")
                        memoryCache[cacheKey] = kugouResult
                        return@runCatching kugouResult
                    }
                }
            }

            // 5. Try Paxsenix multi-source engine (Apple Music / Spotify / YouTube)
            if (prefs.enablePaxsenix) {
                for (candidate in candidates) {
                    val paxResult = fetchFromPaxsenix(candidate.first, candidate.second)
                    if (paxResult != null && (paxResult.isSynced || !paxResult.plainLyrics.isNullOrBlank())) {
                        Log.i(TAG, "[LYRICS] Found via Paxsenix: ${paxResult.trackName} - ${paxResult.artistName}")
                        memoryCache[cacheKey] = paxResult
                        return@runCatching paxResult
                    }
                }
            }

            // 6. Try iTunes Canonical Resolution -> then query LRCLIB / NetEase
            for (candidate in candidates.take(3)) {
                val q = if (!candidate.second.isNullOrBlank()) "${candidate.first} ${candidate.second}" else candidate.first
                val itunesSong = resolveCanonicalViaItunes(q)
                if (itunesSong != null) {
                    val lrcRes = (if (prefs.enableLrcLib) fetchFromLrcLib(itunesSong.first, itunesSong.second, durationSeconds) else null)
                        ?: (if (prefs.enableNetEase) fetchFromNetEase("${itunesSong.first} ${itunesSong.second}") else null)
                    if (lrcRes != null && (lrcRes.isSynced || !lrcRes.plainLyrics.isNullOrBlank())) {
                        Log.i(TAG, "[LYRICS] Found via iTunes Canonical Resolution: ${lrcRes.trackName} - ${lrcRes.artistName}")
                        memoryCache[cacheKey] = lrcRes
                        return@runCatching lrcRes
                    }
                }
            }

            // 7. Try Lyrics.ovh Plain Lyrics Fallback
            for (candidate in candidates) {
                if (!candidate.second.isNullOrBlank()) {
                    val ovh = fetchFromLyricsOvh(candidate.second!!, candidate.first)
                    if (ovh != null) {
                        memoryCache[cacheKey] = ovh
                        return@runCatching ovh
                    }
                }
            }

            throw IllegalStateException("No lyrics found for '$rawTitle'")
        }
    }

    private fun generateCandidates(cleanTitle: String, artist: String?): List<Pair<String, String?>> {
        val candidates = mutableListOf<Pair<String, String?>>()

        if (cleanTitle.contains(" - ")) {
            val parts = cleanTitle.split(" - ", limit = 2).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val p1 = parts[0]
                val p2 = parts[1]
                candidates.add(Pair(p2, p1)) // Track, Artist
                candidates.add(Pair(p1, p2)) // Artist, Track
                candidates.add(Pair(p2, null))
                candidates.add(Pair(p1, null))
                candidates.add(Pair("$p1 $p2", null))
            }
        }

        val featMatch = Regex("(?i)\\b(feat\\.?|ft\\.?|featuring|with|x)\\b").find(cleanTitle)
        if (featMatch != null) {
            val before = cleanTitle.substring(0, featMatch.range.first).trim()
            val after = cleanTitle.substring(featMatch.range.last + 1).trim()
            if (before.isNotBlank()) {
                candidates.add(Pair(before, after.takeIf { it.isNotBlank() }))
                candidates.add(Pair(before, null))
            }
        }

        if (!artist.isNullOrBlank()) {
            candidates.add(Pair(cleanTitle, artist))
        }

        candidates.add(Pair(cleanTitle, null))

        // Word slicing for long titles (e.g. "Tum Hi Ho Aashiqui 2" -> "Tum Hi Ho")
        val words = cleanTitle.split(" ").filter { it.isNotBlank() }
        if (words.size > 3) {
            candidates.add(Pair(words.take(3).joinToString(" "), null))
            candidates.add(Pair(words.take(4).joinToString(" "), null))
        }

        // Deduplicate
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Pair<String, String?>>()
        for ((t, a) in candidates) {
            val tClean = t.replace(Regex("[\\(\\[\\{].*?[\\)\\]\\}]"), "").trim()
            val finalT = if (tClean.isNotBlank()) tClean else t.trim()
            val key = "${finalT.lowercase()}_${(a ?: "").lowercase()}"
            if (finalT.isNotBlank() && seen.add(key)) {
                result.add(Pair(finalT, a?.trim()?.takeIf { it.isNotBlank() }))
            }
        }
        return result
    }

    private fun fetchFromLrcLib(trackName: String, artistName: String?, durationSeconds: Int?): LyricsData? {
        try {
            // 1. If artist is known, try exact match first via /api/get
            if (!artistName.isNullOrBlank()) {
                try {
                    val getUrl = "$LRCLIB_URL/get?track_name=${URLEncoder.encode(trackName, "UTF-8")}&artist_name=${URLEncoder.encode(artistName, "UTF-8")}"
                    val req = Request.Builder()
                        .url(getUrl)
                        .header("User-Agent", "NextPlayer/1.0 (https://github.com/uluploaderroxy/ARCH-PLAYER---SIMP)")
                        .build()
                    val resp = client.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body()?.string()
                        if (!body.isNullOrBlank()) {
                            val item: LrcLibResponse = gson.fromJson(body, LrcLibResponse::class.java)
                            val synced = item.syncedLyrics
                            val plain = item.plainLyrics
                            if (!synced.isNullOrBlank() || !plain.isNullOrBlank()) {
                                val lines = if (!synced.isNullOrBlank()) LyricsParser.parse(synced) else emptyList()
                                return LyricsData(
                                    trackName = item.trackName ?: trackName,
                                    artistName = item.artistName ?: artistName,
                                    albumName = item.albumName,
                                    durationSeconds = item.duration,
                                    isSynced = lines.isNotEmpty(),
                                    lines = lines,
                                    plainLyrics = plain ?: synced?.replace(Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{2,3})?\\]"), "")?.trim()
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Search via track_name & artist_name or general query
            val searchUrl = if (!artistName.isNullOrBlank()) {
                "$LRCLIB_URL/search?track_name=${URLEncoder.encode(trackName, "UTF-8")}&artist_name=${URLEncoder.encode(artistName, "UTF-8")}"
            } else {
                "$LRCLIB_URL/search?q=${URLEncoder.encode(trackName, "UTF-8")}"
            }

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "NextPlayer/1.0 (https://github.com/uluploaderroxy/ARCH-PLAYER---SIMP)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bodyString = response.body()?.string() ?: return null

            val type = object : TypeToken<List<LrcLibResponse>>() {}.type
            val list: List<LrcLibResponse> = gson.fromJson(bodyString, type) ?: emptyList()

            val bestMatch = list.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                ?: list.firstOrNull { !it.plainLyrics.isNullOrBlank() }

            if (bestMatch != null) {
                val syncedLrc = bestMatch.syncedLyrics
                val plainLrc = bestMatch.plainLyrics
                val lines = if (!syncedLrc.isNullOrBlank()) {
                    LyricsParser.parse(syncedLrc)
                } else emptyList()

                return LyricsData(
                    trackName = bestMatch.trackName ?: trackName,
                    artistName = bestMatch.artistName ?: (artistName ?: "Unknown Artist"),
                    albumName = bestMatch.albumName,
                    durationSeconds = bestMatch.duration,
                    isSynced = lines.isNotEmpty(),
                    lines = lines,
                    plainLyrics = plainLrc ?: syncedLrc?.replace(Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{2,3})?\\]"), "")?.trim()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "LRCLIB query failed for $trackName: ${e.message}")
        }
        return null
    }

    private fun fetchFromNetEase(query: String): LyricsData? {
        try {
            val searchUrl = "$NETEASE_SEARCH_URL?csrf_token=&s=${URLEncoder.encode(query, "UTF-8")}&type=1&offset=0&total=true&limit=3"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Referer", "https://music.163.com/")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body()?.string() ?: return null

            val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            val songs = json.getAsJsonObject("result")?.getAsJsonArray("songs") ?: return null
            if (songs.size() == 0) return null

            val firstSong = songs.get(0).asJsonObject
            val songId = firstSong.get("id").asLong
            val songName = firstSong.get("name").asString
            val artistName = try {
                firstSong.getAsJsonArray("artists")?.get(0)?.asJsonObject?.get("name")?.asString ?: "Unknown Artist"
            } catch (_: Exception) { "Unknown Artist" }

            val lyricUrl = "$NETEASE_LYRIC_URL?os=pc&id=$songId&lv=-1&kv=-1&tv=-1"
            val lrcReq = Request.Builder()
                .url(lyricUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Referer", "https://music.163.com/")
                .build()

            val lrcResp = client.newCall(lrcReq).execute()
            if (!lrcResp.isSuccessful) return null
            val lrcBody = lrcResp.body()?.string() ?: return null

            val lrcJson = gson.fromJson(lrcBody, com.google.gson.JsonObject::class.java)
            val rawLrc = lrcJson.getAsJsonObject("lrc")?.get("lyric")?.asString

            if (!rawLrc.isNullOrBlank()) {
                val lines = LyricsParser.parse(rawLrc)
                return LyricsData(
                    trackName = songName,
                    artistName = artistName,
                    albumName = null,
                    durationSeconds = null,
                    isSynced = lines.isNotEmpty(),
                    lines = lines,
                    plainLyrics = rawLrc.replace(Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{2,3})?\\]"), "").trim()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "NetEase query failed for $query: ${e.message}")
        }
        return null
    }

    private fun resolveCanonicalViaItunes(query: String): Pair<String, String>? {
        try {
            val url = "$ITUNES_SEARCH_URL?term=${URLEncoder.encode(query, "UTF-8")}&media=music&limit=1"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body()?.string() ?: return null

            val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            val results = json.getAsJsonArray("results") ?: return null
            if (results.size() > 0) {
                val item = results.get(0).asJsonObject
                val track = item.get("trackName")?.asString
                val artist = item.get("artistName")?.asString
                if (!track.isNullOrBlank() && !artist.isNullOrBlank()) {
                    return Pair(track, artist)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun fetchFromLyricsOvh(artist: String, title: String): LyricsData? {
        try {
            val url = "https://api.lyrics.ovh/v1/${URLEncoder.encode(artist, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body()?.string() ?: return null

            val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            val lyrics = json.get("lyrics")?.asString
            if (!lyrics.isNullOrBlank()) {
                return LyricsData(
                    trackName = title,
                    artistName = artist,
                    albumName = null,
                    durationSeconds = null,
                    isSynced = false,
                    lines = emptyList(),
                    plainLyrics = lyrics.trim()
                )
            }
        } catch (_: Exception) {}
        return null
    }

    fun clearCache() {
        memoryCache.clear()
    }

    val cacheCount: Int
        get() = memoryCache.size

    private fun fetchFromBetterLyrics(trackName: String, artistName: String?): LyricsData? {
        if (trackName.isBlank() || artistName.isNullOrBlank()) return null
        try {
            val q = URLEncoder.encode(trackName, "UTF-8")
            val a = URLEncoder.encode(artistName, "UTF-8")
            val endpoints = listOf(
                "https://lyrics-api.boidu.dev/getLyrics?s=$q&a=$a",
                "https://lyrics-api.boidu.dev/qq/getLyrics?s=$q&a=$a"
            )
            for (url in endpoints) {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "ArchiveTune/1.0")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body()?.string() ?: continue
                    val ttmlOrLrc = if (body.trim().startsWith("<")) {
                        body.trim()
                    } else {
                        try {
                            val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                            json.get("ttml")?.asString ?: json.get("lyrics")?.asString
                        } catch (_: Exception) { null }
                    }

                    if (!ttmlOrLrc.isNullOrBlank()) {
                        val lines = LyricsParser.parse(ttmlOrLrc)
                        return LyricsData(
                            trackName = trackName,
                            artistName = artistName ?: "Unknown Artist",
                            albumName = null,
                            durationSeconds = null,
                            isSynced = lines.isNotEmpty(),
                            lines = lines,
                            plainLyrics = ttmlOrLrc.replace(Regex("<[^>]+>"), "").trim()
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun fetchFromKuGou(trackName: String, artistName: String?): LyricsData? {
        try {
            val kw = URLEncoder.encode("$trackName ${artistName ?: ""}".trim(), "UTF-8")
            val searchUrl = "https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=$kw"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body()?.string() ?: return null
            val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            val candidates = json.getAsJsonArray("candidates") ?: return null
            if (candidates.size() == 0) return null

            val topCandidate = candidates.get(0).asJsonObject
            val id = topCandidate.get("id")?.asLong ?: return null
            val accessKey = topCandidate.get("accesskey")?.asString ?: return null

            val downloadUrl = "https://lyrics.kugou.com/download?ver=1&client=pc&fmt=lrc&charset=utf8&id=$id&accesskey=$accessKey"
            val dlReq = Request.Builder().url(downloadUrl).header("User-Agent", "Mozilla/5.0").build()
            val dlResp = client.newCall(dlReq).execute()
            if (!dlResp.isSuccessful) return null
            val dlBody = dlResp.body()?.string() ?: return null
            val dlJson = gson.fromJson(dlBody, com.google.gson.JsonObject::class.java)
            val base64Content = dlJson.get("content")?.asString ?: return null
            val decodedLrc = String(android.util.Base64.decode(base64Content, android.util.Base64.DEFAULT), Charsets.UTF_8)

            if (decodedLrc.isNotBlank()) {
                val lines = LyricsParser.parse(decodedLrc)
                return LyricsData(
                    trackName = trackName,
                    artistName = artistName ?: "Unknown Artist",
                    albumName = null,
                    durationSeconds = null,
                    isSynced = lines.isNotEmpty(),
                    lines = lines,
                    plainLyrics = decodedLrc.replace(Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{2,3})?\\]"), "").trim()
                )
            }
        } catch (_: Exception) {}
        return null
    }

    private fun fetchFromPaxsenix(trackName: String, artistName: String?): LyricsData? {
        try {
            val q = URLEncoder.encode("$trackName ${artistName ?: ""}".trim(), "UTF-8")
            val endpoints = listOf(
                "https://lyrics.paxsenix.org/spotify/search?q=$q" to "https://lyrics.paxsenix.org/spotify/lyrics?id=",
                "https://lyrics.paxsenix.org/youtube/search?q=$q" to "https://lyrics.paxsenix.org/youtube/lyrics?id="
            )

            for ((searchUrl, lyricsBaseUrl) in endpoints) {
                val searchReq = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "ArchiveTune/1.0")
                    .build()
                val searchResp = client.newCall(searchReq).execute()
                if (searchResp.isSuccessful) {
                    val searchBody = searchResp.body()?.string() ?: continue
                    val array = gson.fromJson(searchBody, com.google.gson.JsonArray::class.java)
                    if (array != null && array.size() > 0) {
                        val firstItem = array.get(0).asJsonObject
                        val realId = firstItem.get("realId")?.asString ?: firstItem.get("id")?.asString ?: continue
                        val lrcReq = Request.Builder()
                            .url("$lyricsBaseUrl$realId")
                            .header("User-Agent", "ArchiveTune/1.0")
                            .build()
                        val lrcResp = client.newCall(lrcReq).execute()
                        if (lrcResp.isSuccessful) {
                            val lrcBody = lrcResp.body()?.string() ?: continue
                            val lrcText = if (lrcBody.trim().startsWith("{")) {
                                try {
                                    val obj = gson.fromJson(lrcBody, com.google.gson.JsonObject::class.java)
                                    obj.get("lyrics")?.asString ?: obj.get("lrc")?.asString ?: obj.get("content")?.asString
                                } catch (_: Exception) { null }
                            } else {
                                lrcBody
                            }
                            if (!lrcText.isNullOrBlank()) {
                                val lines = LyricsParser.parse(lrcText)
                                return LyricsData(
                                    trackName = trackName,
                                    artistName = artistName ?: "Unknown Artist",
                                    albumName = null,
                                    durationSeconds = null,
                                    isSynced = lines.isNotEmpty(),
                                    lines = lines,
                                    plainLyrics = lrcText.replace(Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{2,3})?\\]"), "").trim()
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
