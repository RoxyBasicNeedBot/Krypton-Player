package dev.roxy.kryptonplayer.core.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dev.roxy.kryptonplayer.core.data.network.models.MediaStill
import dev.roxy.kryptonplayer.core.data.network.models.StillSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class ArtistImageItem(
    val title: String,
    val originalUrl: String,
    val thumbnailUrl: String,
    val width: Int = 0,
    val height: Int = 0,
    val source: String = "DuckDuckGo"
)

private data class DuckDuckGoApiResponse(
    @SerializedName("results") val results: List<DuckDuckGoApiImage>?
)

private data class DuckDuckGoApiImage(
    @SerializedName("title") val title: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("height") val height: Int?,
    @SerializedName("width") val width: Int?,
    @SerializedName("source") val source: String?
)

private data class BingIuscJson(
    @SerializedName("murl") val murl: String? = null,
    @SerializedName("turl") val turl: String? = null,
    @SerializedName("t") val title: String? = null,
    @SerializedName("w") val width: Int? = null,
    @SerializedName("h") val height: Int? = null
)

@Singleton
class DuckDuckGoImageClient @Inject constructor(
    private val baseClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "DuckDuckGoImageClient"
        private val VQD_REGEXES = listOf(
            Regex("""vqd=['"]?([^'"&]+)"""),
            Regex("""vqd=([\d-]+)"""),
            Regex("""vqd="([^"]+)"""")
        )
        private val BING_IUSC_REGEX = Pattern.compile("""class="iusc"[^>]*m="([^"]+)"""")
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }

    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
    private val vqdCache = mutableMapOf<String, Pair<String, Long>>()
    private val VQD_CACHE_TTL_MS = 25 * 60 * 1000L // 25 mins

    private val client: OkHttpClient by lazy {
        baseClient.newBuilder()
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val list = cookieStore.getOrPut(url.host()) { mutableListOf() }
                    list.removeAll { old -> cookies.any { it.name() == old.name() } }
                    list.addAll(cookies)
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return cookieStore[url.host()] ?: emptyList()
                }
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Search unlimited artist/actor images using DuckDuckGo with Bing fallback.
     * Supports pagination via page parameter (page * 50 offset).
     */
    suspend fun searchArtistImages(query: String, page: Int = 0): List<ArtistImageItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        // 1. Attempt DuckDuckGo Image Search
        val ddgResults = trySearchDuckDuckGo(cleanQuery, page)
        if (ddgResults.isNotEmpty()) {
            return@withContext ddgResults
        }

        // 2. Fallback to Bing scraper
        Log.d(TAG, "DuckDuckGo returned 0 results for '$cleanQuery', trying Bing scraper fallback (page $page)...")
        val bingResults = trySearchBing(cleanQuery, page)
        if (bingResults.isNotEmpty()) {
            return@withContext bingResults
        }

        emptyList()
    }

    private fun trySearchDuckDuckGo(query: String, page: Int): List<ArtistImageItem> {
        try {
            val vqd = getVqd(query)
            if (vqd == null) {
                Log.w(TAG, "Failed to get VQD for '$query'")
                return emptyList()
            }

            val offset = page * 50
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://duckduckgo.com/i.js?q=$encoded&o=json&vqd=$vqd&f=,,,,,&p=1&s=$offset"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://duckduckgo.com/")
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "DDG image request failed: HTTP ${response.code()}")
                if (response.code() == 403 || response.code() == 429) {
                    vqdCache.remove(query)
                }
                return emptyList()
            }

            val body = response.body()?.string() ?: return emptyList()
            val parsed = gson.fromJson(body, DuckDuckGoApiResponse::class.java)
            val items = parsed?.results?.mapNotNull { item ->
                val orig = item.image ?: item.url ?: return@mapNotNull null
                val thumb = item.thumbnail ?: orig
                ArtistImageItem(
                    title = item.title ?: query,
                    originalUrl = orig,
                    thumbnailUrl = thumb,
                    width = item.width ?: 0,
                    height = item.height ?: 0,
                    source = "DuckDuckGo"
                )
            } ?: emptyList()

            // Deduplicate
            val seen = mutableSetOf<String>()
            return items.filter { seen.add(it.originalUrl) }
        } catch (e: Exception) {
            Log.e(TAG, "DuckDuckGo search error: ${e.message}", e)
            return emptyList()
        }
    }

    private fun getVqd(query: String): String? {
        val cached = vqdCache[query]
        val now = System.currentTimeMillis()
        if (cached != null && (now - cached.second) < VQD_CACHE_TTL_MS) {
            return cached.first
        }

        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://duckduckgo.com/?q=$encoded")
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val html = response.body()?.string() ?: return null

            var foundVqd: String? = null
            for (regex in VQD_REGEXES) {
                val match = regex.find(html)
                if (match != null && match.groupValues.size > 1) {
                    foundVqd = match.groupValues[1]
                    break
                }
            }

            if (foundVqd != null) {
                vqdCache[query] = Pair(foundVqd, now)
            }
            return foundVqd
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching VQD: ${e.message}", e)
            return null
        }
    }

    private fun trySearchBing(query: String, page: Int): List<ArtistImageItem> {
        return try {
            val first = page * 35 + 1
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.bing.com/images/search?q=$encoded&first=$first&FORM=HDRSC2"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val html = response.body()?.string() ?: return emptyList()

            val results = mutableListOf<ArtistImageItem>()
            val matcher = BING_IUSC_REGEX.matcher(html)
            val seen = mutableSetOf<String>()

            while (matcher.find()) {
                val jsonEscaped = matcher.group(1) ?: continue
                val jsonString = android.text.Html.fromHtml(jsonEscaped, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                try {
                    val parsed = gson.fromJson(jsonString, BingIuscJson::class.java)
                    val murl = parsed.murl
                    if (!murl.isNullOrBlank() && seen.add(murl)) {
                        results.add(
                            ArtistImageItem(
                                title = parsed.title ?: query,
                                originalUrl = murl,
                                thumbnailUrl = parsed.turl ?: murl,
                                width = parsed.width ?: 0,
                                height = parsed.height ?: 0,
                                source = "Bing"
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Bing search error: ${e.message}", e)
            emptyList()
        }
    }
}

fun ArtistImageItem.toMediaStill(id: String): MediaStill {
    return MediaStill(
        id = id,
        thumbnailUrl = thumbnailUrl,
        fullUrl = originalUrl,
        source = StillSource.DUCKDUCKGO,
        width = width,
        height = height,
        caption = title
    )
}
