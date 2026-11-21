package dev.roxy.kryptonplayer.core.data.network

import dev.roxy.kryptonplayer.core.data.network.models.FancapsFrame
import dev.roxy.kryptonplayer.core.data.network.models.FancapsPageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FancapsScraper @Inject constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val referer = "https://fancaps.net/"

    suspend fun searchMovie(title: String, releaseYear: String? = null): String? = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val searchUrl = "https://fancaps.net/search.php?q=" + encoded + "&submit=Submit+Query&MoviesCB=Movies"

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", userAgent)
                .header("Referer", referer)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                response.body()?.string() ?: return@runCatching null
            }

            val linkPattern = Regex("""href=["'](/movies/MovieImages\.php\?name=[^&"']+&movieid=\d+)["']""")
            val matches = linkPattern.findAll(html).map { it.groupValues[1] }.distinct().toList()
            if (matches.isEmpty()) return@runCatching null

            val bestMatch = if (!releaseYear.isNullOrBlank()) {
                matches.firstOrNull { it.contains(releaseYear) } ?: matches.first()
            } else {
                matches.first()
            }

            "https://fancaps.net" + bestMatch
        }.getOrNull()
    }

    suspend fun searchTv(title: String, season: Int = 1): String? = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val searchUrl = "https://fancaps.net/search.php?q=" + encoded + "&submit=Submit+Query&TVCB=TV"

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", userAgent)
                .header("Referer", referer)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                response.body()?.string() ?: return@runCatching null
            }

            val linkPattern = Regex("""href=["'](/tv/(?:showimages|episodeimages)\.php\?[^"']+)["']""")
            val matches = linkPattern.findAll(html).map { it.groupValues[1] }.distinct().toList()
            if (matches.isEmpty()) return@runCatching null

            val bestMatch = matches.firstOrNull { it.contains("Season_" + season, ignoreCase = true) }
                ?: matches.first()

            "https://fancaps.net" + bestMatch
        }.getOrNull()
    }

    suspend fun getFramesFromPage(mediaPageUrl: String, page: Int = 1, isTv: Boolean = false): FancapsPageResult = withContext(Dispatchers.IO) {
        runCatching {
            var targetUrl = if (mediaPageUrl.contains("page=")) {
                mediaPageUrl.replace(Regex("""page=\d+"""), "page=" + page)
            } else {
                val separator = if (mediaPageUrl.contains("?")) "&" else "?"
                mediaPageUrl + separator + "page=" + page
            }

            if (isTv && targetUrl.contains("showimages.php")) {
                val episodeUrl = getFirstEpisodeUrl(targetUrl)
                if (episodeUrl != null) {
                    val sep = if (episodeUrl.contains("?")) "&" else "?"
                    targetUrl = if (episodeUrl.contains("page=")) {
                        episodeUrl.replace(Regex("""page=\d+"""), "page=" + page)
                    } else {
                        episodeUrl + sep + "page=" + page
                    }
                }
            }

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", userAgent)
                .header("Referer", referer)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching FancapsPageResult("", targetUrl, page, 1, emptyList())
                response.body()?.string() ?: ""
            }

            val titleMatch = Regex("""<title>(.*?)</title>""").find(html)
            val parsedTitle = titleMatch?.groupValues?.getOrNull(1)?.replace("- FanCaps.net", "")?.trim() ?: "Frames"

            val pagePattern = Regex("""page=(\d+)""")
            val allPages = pagePattern.findAll(html).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
            val totalPages = (allPages + listOf(page, 1)).maxOrNull() ?: 1

            val imgPattern = Regex("""src=["'](https://([a-zA-Z0-9.-]+fancaps\.net)/(\d+)\.jpg)["']""")
            val frames = imgPattern.findAll(html).mapIndexed { index, match ->
                val thumbUrl = match.groupValues[1]
                val domain = match.groupValues[2]
                val frameId = match.groupValues[3]

                val cdnType = when {
                    domain.startsWith("tv") -> "fancaps-tvimages"
                    domain.startsWith("an") -> "fancaps-animeimages"
                    else -> "fancaps-movieimages"
                }
                val fullUrl = "https://cdni.fancaps.net/file/" + cdnType + "/" + frameId + ".jpg"

                FancapsFrame(
                    frameId = frameId,
                    thumbnailUrl = thumbUrl,
                    fullUrl = fullUrl,
                    pageNumber = page,
                    indexOnPage = index + 1
                )
            }.distinctBy { it.frameId }.toList()

            FancapsPageResult(
                title = parsedTitle,
                mediaUrl = targetUrl,
                currentPage = page,
                totalPages = totalPages,
                frames = frames
            )
        }.getOrElse {
            FancapsPageResult("", mediaPageUrl, page, 1, emptyList())
        }
    }

    private fun getFirstEpisodeUrl(showImagesUrl: String): String? {
        return runCatching {
            val request = Request.Builder()
                .url(showImagesUrl)
                .header("User-Agent", userAgent)
                .header("Referer", referer)
                .header("Accept", "text/html")
                .build()

            val html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                response.body()?.string() ?: return@runCatching null
            }

            val epPattern = Regex("""href=["'](/tv/episodeimages\.php\?[^"']+)["']""")
            val epMatch = epPattern.find(html)?.groupValues?.get(1) ?: return null
            "https://fancaps.net" + epMatch
        }.getOrNull()
    }
}
