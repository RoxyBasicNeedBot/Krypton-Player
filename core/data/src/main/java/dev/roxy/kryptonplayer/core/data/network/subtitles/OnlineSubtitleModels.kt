package dev.roxy.kryptonplayer.core.data.network.subtitles

import com.google.gson.annotations.SerializedName

data class OnlineSubtitleItem(
    val id: String,
    val title: String,
    val language: String,
    val languageCode: String,
    val release: String,
    val downloadCount: Int,
    val rating: Float,
    val downloadUrl: String,
    val source: String, // "OpenSubtitles", "SubDL", "OpenSource Mirror"
    val isHearingImpaired: Boolean = false,
    val format: String = "srt"
)

// ── OpenSubtitles.com REST API v1 Models ──────────────────

data class OpenSubtitlesSearchResponse(
    @SerializedName("total_pages") val totalPages: Int? = null,
    @SerializedName("total_count") val totalCount: Int? = null,
    @SerializedName("data") val data: List<OpenSubtitleData>? = null
)

data class OpenSubtitleData(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("attributes") val attributes: OpenSubtitleAttributes? = null
)

data class OpenSubtitleAttributes(
    @SerializedName("subtitle_id") val subtitleId: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("download_count") val downloadCount: Int? = null,
    @SerializedName("ratings") val ratings: Float? = null,
    @SerializedName("release") val release: String? = null,
    @SerializedName("feature_details") val featureDetails: OpenSubtitleFeatureDetails? = null,
    @SerializedName("files") val files: List<OpenSubtitleFile>? = null,
    @SerializedName("hearing_impaired") val hearingImpaired: Boolean? = null
)

data class OpenSubtitleFeatureDetails(
    @SerializedName("title") val title: String? = null,
    @SerializedName("movie_name") val movieName: String? = null,
    @SerializedName("year") val year: Int? = null
)

data class OpenSubtitleFile(
    @SerializedName("file_id") val fileId: Int? = null,
    @SerializedName("cd_number") val cdNumber: Int? = null,
    @SerializedName("file_name") val fileName: String? = null
)

data class OpenSubtitlesDownloadRequest(
    @SerializedName("file_id") val fileId: Int,
    @SerializedName("sub_format") val subFormat: String = "srt"
)

data class OpenSubtitlesDownloadResponse(
    @SerializedName("link") val link: String? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("requests") val requests: Int? = null,
    @SerializedName("remaining") val remaining: Int? = null,
    @SerializedName("message") val message: String? = null
)

// ── SubDL API Models (Multi-Language Provider for all media) ──────

data class SubdlSearchResponse(
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("subtitles") val subtitles: List<SubdlSubtitleItem>? = null
)

data class SubdlSubtitleItem(
    @SerializedName("release_name") val releaseName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("lang") val lang: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("hi") val hearingImpaired: Boolean? = null
)

// ── IMDb Suggestion API Models ────────────────────────────

data class ImdbSuggestResponse(
    @SerializedName("d") val d: List<ImdbSuggestItem>? = null,
    @SerializedName("q") val q: String? = null
)

data class ImdbSuggestItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("l") val title: String? = null,
    @SerializedName("y") val year: Int? = null,
    @SerializedName("s") val stars: String? = null,
    @SerializedName("q") val typeDesc: String? = null,
    @SerializedName("qid") val typeId: String? = null,
    @SerializedName("i") val image: ImdbSuggestImage? = null
)

data class ImdbSuggestImage(
    @SerializedName("imageUrl") val imageUrl: String? = null
)

// ── Stremio OpenSubtitles v3 Engine Models ───────────────

data class StremioSubtitleResponse(
    @SerializedName("subtitles") val subtitles: List<StremioSubtitleItem>? = null
)

data class StremioSubtitleItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("lang") val lang: String? = null,
    @SerializedName("format") val format: String? = "srt"
)
