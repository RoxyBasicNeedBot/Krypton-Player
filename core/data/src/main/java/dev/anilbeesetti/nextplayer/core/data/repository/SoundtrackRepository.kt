package dev.anilbeesetti.nextplayer.core.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dev.anilbeesetti.nextplayer.core.data.shazam.ShazamAudioRecognizer
import dev.anilbeesetti.nextplayer.core.data.shazam.ShazamRecognizedTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class SoundtrackItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val previewUrl: String?,
    val durationSeconds: Int,
    val spotifyUrl: String? = null,
    val youtubeUrl: String? = null
)

data class ItunesSearchResponse(
    @SerializedName("resultCount") val resultCount: Int,
    @SerializedName("results") val results: List<ItunesTrack>?
)

data class ItunesTrack(
    @SerializedName("trackId") val trackId: Long?,
    @SerializedName("trackName") val trackName: String?,
    @SerializedName("artistName") val artistName: String?,
    @SerializedName("collectionName") val collectionName: String?,
    @SerializedName("artworkUrl100") val artworkUrl100: String?,
    @SerializedName("previewUrl") val previewUrl: String?,
    @SerializedName("trackTimeMillis") val trackTimeMillis: Long?,
    @SerializedName("trackViewUrl") val trackViewUrl: String?
)

data class DeezerSearchResponse(
    @SerializedName("data") val data: List<DeezerTrack>?
)

data class DeezerTrack(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("preview") val preview: String?,
    @SerializedName("link") val link: String?,
    @SerializedName("artist") val artist: DeezerArtist?,
    @SerializedName("album") val album: DeezerAlbum?
)

data class DeezerArtist(
    @SerializedName("name") val name: String?
)

data class DeezerAlbum(
    @SerializedName("title") val title: String?,
    @SerializedName("cover_medium") val coverMedium: String?
)

@Singleton
class SoundtrackRepository @Inject constructor(
    private val shazamRecognizer: ShazamAudioRecognizer
) {
    companion object {
        private const val TAG = "SoundtrackRepo"
    }

    private val gson = Gson()

    /**
     * Real Shazam Live Audio Recognition
     */

    /**
     * Direct In-Video Audio Recognition (Bit-for-bit digital audio extraction at current playback position)
     */
    suspend fun recognizeVideoAudio(
        context: Context,
        videoUri: android.net.Uri,
        positionMs: Long,
        boostMode: Boolean = true
    ): Result<ShazamRecognizedTrack> = shazamRecognizer.recognizeFromVideo(context, videoUri, positionMs, boostMode)

    suspend fun recognizeLiveAudio(
        context: Context,
        boostMode: Boolean = true,
        onAmplitude: (Float) -> Unit
    ): Result<ShazamRecognizedTrack> = shazamRecognizer.recognizeFromMicrophone(context, boostMode, onAmplitude)

    /**
     * Movie OST & Soundtrack Catalog Search
     */
    suspend fun getSoundtracksForMedia(mediaTitle: String): List<SoundtrackItem> = withContext(Dispatchers.IO) {
        val cleanTitle = cleanMediaTitle(mediaTitle)
        val candidates = getSearchCandidates(cleanTitle)

        for (candidate in candidates) {
            val itunesResults = searchItunes(candidate)
            if (itunesResults.isNotEmpty()) {
                return@withContext itunesResults
            }
            val deezerResults = searchDeezer(candidate)
            if (deezerResults.isNotEmpty()) {
                return@withContext deezerResults
            }
        }
        emptyList()
    }

    fun cleanMediaTitle(mediaTitle: String): String {
        return dev.anilbeesetti.nextplayer.core.common.Utils.cleanMediaTitle(mediaTitle).first
    }

    private fun getSearchCandidates(cleanTitle: String): List<String> {
        val list = mutableListOf<String>()
        if (cleanTitle.isNotBlank()) {
            list.add(cleanTitle)
            val words = cleanTitle.split(" ").filter { it.isNotBlank() }
            if (words.size > 3) {
                list.add(words.take(3).joinToString(" "))
            }
        }
        return list.distinct()
    }

    fun searchItunes(cleanTitle: String): List<SoundtrackItem> {
        return try {
            val queries = listOf(cleanTitle, cleanTitle + " soundtrack", cleanTitle + " theme")
            for (q in queries) {
                val encoded = URLEncoder.encode(q, "UTF-8")
                val url = "https://itunes.apple.com/search?term=" + encoded + "&media=music&entity=song&limit=30"
                val conn = (URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val resp = gson.fromJson(body, ItunesSearchResponse::class.java)
                    if (!resp.results.isNullOrEmpty()) {
                        return resp.results.mapNotNull { track ->
                            if (track.trackName != null && track.artistName != null) {
                                val query = URLEncoder.encode(track.trackName + " " + track.artistName, "UTF-8")
                                SoundtrackItem(
                                    id = track.trackId ?: System.currentTimeMillis(),
                                    title = track.trackName,
                                    artist = track.artistName,
                                    album = track.collectionName ?: "Original Soundtrack",
                                    coverUrl = track.artworkUrl100?.replace("100x100bb", "600x600bb"),
                                    previewUrl = track.previewUrl,
                                    durationSeconds = ((track.trackTimeMillis ?: 0) / 1000).toInt(),
                                    spotifyUrl = "https://open.spotify.com/search/" + query,
                                    youtubeUrl = "https://www.youtube.com/results?search_query=" + query
                                )
                            } else null
                        }
                    }
                }
            }
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "iTunes search failed", e)
            emptyList()
        }
    }

    fun searchDeezer(cleanTitle: String): List<SoundtrackItem> {
        return try {
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://api.deezer.com/search?q=" + encoded + "&limit=25"
            val conn = (URL(url).openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val resp = gson.fromJson(body, DeezerSearchResponse::class.java)
                return resp.data?.map { track ->
                    val query = URLEncoder.encode(track.title + " " + (track.artist?.name ?: ""), "UTF-8")
                    SoundtrackItem(
                        id = track.id,
                        title = track.title,
                        artist = track.artist?.name ?: "Unknown Artist",
                        album = track.album?.title ?: "Album",
                        coverUrl = track.album?.coverMedium,
                        previewUrl = track.preview,
                        durationSeconds = track.duration,
                        spotifyUrl = "https://open.spotify.com/search/" + query,
                        youtubeUrl = "https://www.youtube.com/results?search_query=" + query
                    )
                } ?: emptyList()
            }
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Deezer search failed", e)
            emptyList()
        }
    }
}
