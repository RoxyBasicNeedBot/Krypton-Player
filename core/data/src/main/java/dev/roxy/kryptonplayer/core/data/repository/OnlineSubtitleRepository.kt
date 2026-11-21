package dev.roxy.kryptonplayer.core.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.roxy.kryptonplayer.core.data.network.subtitles.OnlineSubtitleItem
import dev.roxy.kryptonplayer.core.data.network.subtitles.OpenSubtitlesApi
import dev.roxy.kryptonplayer.core.data.network.subtitles.OpenSubtitlesDownloadRequest
import dev.roxy.kryptonplayer.core.data.network.subtitles.SubdlApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineSubtitleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openSubtitlesApi: OpenSubtitlesApi,
    private val subdlApi: SubdlApi,
    private val imdbSuggestApi: dev.roxy.kryptonplayer.core.data.network.subtitles.ImdbSuggestApi,
    private val stremioSubtitleApi: dev.roxy.kryptonplayer.core.data.network.subtitles.StremioSubtitleApi
) {
    companion object {
        private const val TAG = "OnlineSubtitleRepo"
        private const val SUBTITLE_DIR = "downloaded_subtitles"
    }

    private val subtitleDir: File by lazy {
        File(context.cacheDir, SUBTITLE_DIR).apply { mkdirs() }
    }

    fun getCacheSizeBytes(): Long {
        return subtitleDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearCache(): Long {
        val size = getCacheSizeBytes()
        subtitleDir.deleteRecursively()
        subtitleDir.mkdirs()
        return size
    }

    fun cleanSearchQuery(rawName: String): String {
        val (clean, _) = dev.roxy.kryptonplayer.core.common.Utils.cleanMediaTitle(rawName)
        return clean.ifBlank { rawName.substringBeforeLast(".") }
    }

    private fun map3ToLanguage(code3: String): Pair<String, String> {
        val lower = code3.lowercase().trim()
        return when (lower) {
            "eng", "en" -> "English" to "en"
            "hin", "hi" -> "Hindi" to "hi"
            "spa", "spn", "es" -> "Spanish" to "es"
            "fre", "fra", "fr" -> "French" to "fr"
            "ger", "deu", "de" -> "German" to "de"
            "ara", "ar" -> "Arabic" to "ar"
            "kor", "ko" -> "Korean" to "ko"
            "ita", "it" -> "Italian" to "it"
            "jpn", "ja" -> "Japanese" to "ja"
            "rus", "ru" -> "Russian" to "ru"
            "por", "pob", "pt" -> "Portuguese" to "pt"
            "tam", "ta" -> "Tamil" to "ta"
            "tel", "te" -> "Telugu" to "te"
            "ben", "bn" -> "Bengali" to "bn"
            "mal", "ml" -> "Malayalam" to "ml"
            "tur", "tr" -> "Turkish" to "tr"
            "vie", "vi" -> "Vietnamese" to "vi"
            "zho", "zht", "zh" -> "Chinese" to "zh"
            "ind", "id" -> "Indonesian" to "id"
            "pol", "pl" -> "Polish" to "pl"
            "nld", "dut", "nl" -> "Dutch" to "nl"
            "swe", "sv" -> "Swedish" to "sv"
            "fin", "fi" -> "Finnish" to "fi"
            "dan", "da" -> "Danish" to "da"
            "nor", "no" -> "Norwegian" to "no"
            "cze", "ces", "cs" -> "Czech" to "cs"
            "ell", "el" -> "Greek" to "el"
            "heb", "he" -> "Hebrew" to "he"
            "hun", "hu" -> "Hungarian" to "hu"
            "ron", "ro" -> "Romanian" to "ro"
            "ukr", "uk" -> "Ukrainian" to "uk"
            "tha", "th" -> "Thai" to "th"
            "per", "fas", "fa" -> "Persian" to "fa"
            else -> lower.uppercase() to lower.take(2)
        }
    }

    suspend fun searchSubtitles(
        rawQuery: String,
        languages: String? = null // e.g. "en,hi,es"
    ): List<OnlineSubtitleItem> = withContext(Dispatchers.IO) {
        val cleanQuery = cleanSearchQuery(rawQuery)
        val queryToUse = if (cleanQuery.isNotBlank()) cleanQuery else rawQuery
        val results = mutableListOf<OnlineSubtitleItem>()

        // 1. Primary Engine: IMDb Suggest + Stremio OpenSubtitles v3 Engine
        val stremioDeferred = async {
            try {
                // Parse Season & Episode
                val sEMatch = Regex("""(?i)\b[sS](\d{1,2})[eE](\d{1,2})\b""").find(rawQuery)
                var season = sEMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                var episode = sEMatch?.groupValues?.getOrNull(2)?.toIntOrNull()

                if (episode == null) {
                    val epOnly = Regex("""(?i)\b(?:episode|ep|[eE])\s*(\d{1,2})\b""").find(rawQuery)
                    if (epOnly != null) {
                        season = 1
                        episode = epOnly.groupValues.getOrNull(1)?.toIntOrNull()
                    }
                }

                val titleForImdb = queryToUse
                    .replace(Regex("""(?i)\b[sS]\d{1,2}[eE]\d{1,2}\b"""), "")
                    .replace(Regex("""(?i)\b(?:episode|ep|[eE])\s*\d{1,2}\b"""), "")
                    .trim()

                val queriesToTry = listOf(
                    titleForImdb.lowercase().replace(Regex("[^a-zA-Z0-9]"), "_"),
                    cleanQuery.lowercase().replace(Regex("[^a-zA-Z0-9]"), "_")
                ).distinct()

                var imdbId: String? = null
                var matchedTitle: String? = null
                var matchedType: String? = null
                var matchedYear: Int? = null

                for (q in queriesToTry) {
                    if (q.isBlank()) continue
                    try {
                        val suggestResp = imdbSuggestApi.suggest(q)
                        val topMatch = suggestResp.d?.firstOrNull()
                        if (topMatch?.id != null) {
                            imdbId = topMatch.id
                            matchedTitle = topMatch.title
                            matchedType = topMatch.typeDesc
                            matchedYear = topMatch.year
                            break
                        }
                    } catch (_: Exception) {}
                }

                if (!imdbId.isNullOrBlank()) {
                    val isTv = (matchedType in listOf("TV series", "TV mini-series", "TV short", "tvSeries", "tvMiniSeries") || season != null || episode != null)
                    var subsList: List<dev.roxy.kryptonplayer.core.data.network.subtitles.StremioSubtitleItem> = emptyList()

                    if (isTv) {
                        val s = season ?: 1
                        val e = episode ?: 1
                        try {
                            subsList = stremioSubtitleApi.getSeriesSubtitles("$imdbId:$s:$e").subtitles ?: emptyList()
                        } catch (_: Exception) {}
                    }

                    // Fallback to movie endpoint if series returned no subtitles
                    if (subsList.isEmpty()) {
                        try {
                            subsList = stremioSubtitleApi.getMovieSubtitles(imdbId).subtitles ?: emptyList()
                        } catch (_: Exception) {}
                    }

                    subsList.mapNotNull { sub ->
                        val url = sub.url ?: return@mapNotNull null
                        val rawLang = sub.lang ?: "eng"
                        val (langName, langCode) = map3ToLanguage(rawLang)

                        if (languages != null && !languages.split(",").any { it.trim().equals(langCode, ignoreCase = true) || it.trim().equals(rawLang, ignoreCase = true) }) {
                            return@mapNotNull null
                        }

                        OnlineSubtitleItem(
                            id = "stremio_${sub.id ?: url.hashCode().toString()}",
                            title = matchedTitle ?: queryToUse,
                            language = langName,
                            languageCode = langCode,
                            release = if (isTv) "${matchedTitle ?: queryToUse} S${season ?: 1}E${episode ?: 1}" else "${matchedTitle ?: queryToUse} (${matchedYear ?: ""})",
                            downloadCount = 500,
                            rating = 4.8f,
                            downloadUrl = url,
                            source = "OpenSubtitles",
                            isHearingImpaired = false,
                            format = sub.format ?: "srt"
                        )
                    }
                } else {
                    emptyList<OnlineSubtitleItem>()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Stremio subtitle search failed for: $queryToUse", e)
                emptyList<OnlineSubtitleItem>()
            }
        }

        // 2. Secondary Engine: OpenSubtitles.com REST API
        val openSubsDeferred = async {
            try {
                val resp = openSubtitlesApi.searchSubtitles(
                    query = queryToUse,
                    languages = languages
                )
                resp.data?.mapNotNull { item ->
                    val attr = item.attributes ?: return@mapNotNull null
                    val file = attr.files?.firstOrNull()
                    val fileId = file?.fileId ?: return@mapNotNull null
                    val lang = attr.language ?: "en"
                    val (langName, langCode) = map3ToLanguage(lang)
                    OnlineSubtitleItem(
                        id = "os_$fileId",
                        title = attr.featureDetails?.title ?: attr.release ?: file.fileName ?: queryToUse,
                        language = langName,
                        languageCode = langCode,
                        release = attr.release ?: file.fileName ?: "WebRip",
                        downloadCount = attr.downloadCount ?: 0,
                        rating = attr.ratings ?: 0f,
                        downloadUrl = fileId.toString(),
                        source = "OpenSubtitles",
                        isHearingImpaired = attr.hearingImpaired == true
                    )
                } ?: emptyList<OnlineSubtitleItem>()
            } catch (e: Exception) {
                Log.w(TAG, "OpenSubtitles search failed for: $queryToUse", e)
                emptyList<OnlineSubtitleItem>()
            }
        }

        // 3. Fallback Engine: SubDL API
        val subdlDeferred = async {
            try {
                val resp = subdlApi.searchSubtitles(
                    query = queryToUse,
                    languages = languages
                )
                resp.subtitles?.mapIndexed { idx, item ->
                    val lang = item.lang ?: "English"
                    val url = item.url ?: ""
                    val (langName, langCode) = map3ToLanguage(lang)
                    OnlineSubtitleItem(
                        id = "subdl_${idx}_${item.releaseName.hashCode()}",
                        title = item.name ?: item.releaseName ?: queryToUse,
                        language = langName,
                        languageCode = langCode,
                        release = item.releaseName ?: "Standard Release",
                        downloadCount = 100,
                        rating = 4.5f,
                        downloadUrl = if (url.startsWith("http")) url else "https://dl.subdl.com$url",
                        source = "SubDL",
                        isHearingImpaired = item.hearingImpaired == true
                    )
                } ?: emptyList<OnlineSubtitleItem>()
            } catch (e: Exception) {
                Log.w(TAG, "SubDL search failed for: $queryToUse", e)
                emptyList<OnlineSubtitleItem>()
            }
        }

        // 4. Video & JAV / Adult Scene Index Engine: SubtitleCat
        val subtitleCatDeferred = async {
            try {
                val encodedQuery = java.net.URLEncoder.encode(queryToUse, "UTF-8")
                val url = "https://www.subtitlecat.com/index.php?search=$encodedQuery"
                val connection = URL(url).openConnection().apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val html = connection.getInputStream().bufferedReader().use { it.readText() }
                val matches = Regex("""<a\s+href=["'](subs/(\d+)/([^"']+)\.html)["']>([^<]+)</a>""", RegexOption.IGNORE_CASE).findAll(html)
                matches.mapIndexed { idx, match ->
                    val numId = match.groupValues[2]
                    val fileSlug = match.groupValues[3]
                    val linkTitle = match.groupValues[4].trim()
                    val dlUrl = "https://www.subtitlecat.com/subs/$numId/$fileSlug-en.srt"
                    OnlineSubtitleItem(
                        id = "cat_${numId}_$idx",
                        title = linkTitle,
                        language = "English",
                        languageCode = "en",
                        release = linkTitle,
                        downloadCount = 250,
                        rating = 4.6f,
                        downloadUrl = dlUrl,
                        source = "Community Subtitles",
                        isHearingImpaired = false,
                        format = "srt"
                    )
                }.toList()
            } catch (e: Exception) {
                Log.w(TAG, "SubtitleCat search failed for: $queryToUse", e)
                emptyList()
            }
        }

        results.addAll(stremioDeferred.await())
        results.addAll(subtitleCatDeferred.await())
        results.addAll(openSubsDeferred.await())
        results.addAll(subdlDeferred.await())

        // Deduplicate & sort
        results.distinctBy { "${it.languageCode}_${it.downloadUrl}" }
            .sortedWith(
                compareByDescending<OnlineSubtitleItem> { it.rating }
                    .thenByDescending { it.downloadCount }
            )
    }

    suspend fun downloadSubtitle(item: OnlineSubtitleItem): File? = withContext(Dispatchers.IO) {
        try {
            var downloadUrl = item.downloadUrl

            // If not an absolute http/https URL, resolve direct link via OpenSubtitles API
            if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
                val fileId = downloadUrl.toIntOrNull() ?: return@withContext null
                try {
                    val linkResp = openSubtitlesApi.getDownloadLink(
                        body = OpenSubtitlesDownloadRequest(fileId = fileId)
                    )
                    downloadUrl = linkResp.link ?: return@withContext null
                } catch (e: Exception) {
                    Log.w(TAG, "OpenSubtitles download link request failed for $fileId", e)
                    return@withContext null
                }
            }

            if (downloadUrl.isBlank()) return@withContext null

            val safeId = item.id.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val targetFile = File(subtitleDir, "$safeId.srt")
            if (targetFile.exists() && targetFile.length() > 0) return@withContext targetFile

            val bytes = fetchBytesWithRedirects(downloadUrl)
            if (bytes == null || bytes.isEmpty()) {
                Log.w(TAG, "Empty payload received for subtitle ${item.title}")
                return@withContext null
            }

            // Reject HTML error / Cloudflare challenge pages
            val headString = String(bytes.take(256).toByteArray(), Charsets.UTF_8).lowercase()
            if (headString.contains("<!doctype html") || headString.contains("<html") || headString.contains("<title>access denied")) {
                Log.w(TAG, "Received HTML error page instead of subtitle file: $headString")
                return@withContext null
            }

            // Check if downloaded payload is a GZIP archive (0x1F, 0x8B)
            if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
                java.util.zip.GZIPInputStream(bytes.inputStream()).use { gzIn ->
                    FileOutputStream(targetFile).use { out ->
                        gzIn.copyTo(out)
                    }
                }
            }
            // Check if downloaded payload is a ZIP archive (0x50, 0x4B)
            else if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                ZipInputStream(bytes.inputStream()).use { zipIn ->
                    var entry = zipIn.nextEntry
                    var found = false
                    while (entry != null) {
                        if (!entry.isDirectory && (entry.name.endsWith(".srt", ignoreCase = true) || entry.name.endsWith(".vtt", ignoreCase = true) || entry.name.endsWith(".ass", ignoreCase = true))) {
                            FileOutputStream(targetFile).use { out ->
                                zipIn.copyTo(out)
                            }
                            found = true
                            break
                        }
                        entry = zipIn.nextEntry
                    }
                    if (!found) {
                        ZipInputStream(bytes.inputStream()).use { zIn2 ->
                            val e2 = zIn2.nextEntry
                            if (e2 != null && !e2.isDirectory) {
                                FileOutputStream(targetFile).use { out -> zIn2.copyTo(out) }
                            }
                        }
                    }
                }
            } else {
                FileOutputStream(targetFile).use { it.write(bytes) }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                Log.i(TAG, "[SUBTITLE] Downloaded successfully: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                targetFile
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download subtitle: ${item.title}", e)
            null
        }
    }

    private fun fetchBytesWithRedirects(initialUrl: String, maxRedirects: Int = 8): ByteArray? {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects < maxRedirects) {
            try {
                val connection = (URL(currentUrl).openConnection() as java.net.HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    setRequestProperty("Accept", "*/*")
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: break
                    currentUrl = if (location.startsWith("http")) location else URL(URL(currentUrl), location).toString()
                    redirects++
                    continue
                }
                if (code in 200..299) {
                    return connection.inputStream.use { it.readBytes() }
                }
                break
            } catch (e: Exception) {
                Log.w(TAG, "Network error downloading subtitle from $currentUrl: ${e.message}")
                break
            }
        }
        return null
    }
}
