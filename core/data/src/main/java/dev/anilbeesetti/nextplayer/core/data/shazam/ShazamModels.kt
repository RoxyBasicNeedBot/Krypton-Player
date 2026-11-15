package dev.anilbeesetti.nextplayer.core.data.shazam

import com.google.gson.annotations.SerializedName

data class ShazamRecognizedTrack(
    val title: String,
    val subtitle: String, // Artist
    val coverUrl: String?,
    val backgroundUrl: String?,
    val shareUrl: String?,
    val appleMusicUrl: String?,
    val spotifyUrl: String?,
    val youtubeSearchUrl: String,
    val genre: String?,
    val isrc: String?,
    val lyrics: List<String> = emptyList(),
    val previewUrl: String? = null,
    val album: String? = null,
    val releaseDate: String? = null,
    val label: String? = null,
    val bpm: Int = 92,
    val melodicness: Float = 0.82f,
    val acousticness: Float = 0.40f,
    val valence: Float = 0.68f,
    val danceability: Float = 0.74f,
    val energy: Float = 0.80f,
    val shazamCount: String? = null,
)

data class ShazamRequestPayload(
    @SerializedName("geolocation") val geolocation: ShazamGeolocation,
    @SerializedName("signature") val signature: ShazamSignaturePayload,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("timezone") val timezone: String
)

data class ShazamGeolocation(
    @SerializedName("altitude") val altitude: Double = 150.0,
    @SerializedName("latitude") val latitude: Double = 40.7128,
    @SerializedName("longitude") val longitude: Double = -74.0060
)

data class ShazamSignaturePayload(
    @SerializedName("samplems") val samplems: Long,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("uri") val uri: String
)

data class ShazamApiResponse(
    @SerializedName("matches") val matches: List<ShazamMatch>?,
    @SerializedName("track") val track: ShazamTrack?
)

data class ShazamMatch(
    @SerializedName("id") val id: String?,
    @SerializedName("offset") val offset: Double?
)

data class ShazamTrack(
    @SerializedName("key") val key: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("isrc") val isrc: String?,
    @SerializedName("images") val images: ShazamImages?,
    @SerializedName("share") val share: ShazamShare?,
    @SerializedName("genres") val genres: ShazamGenres?,
    @SerializedName("sections") val sections: List<ShazamSection>?,
    @SerializedName("hub") val hub: ShazamHub?
)

data class ShazamHub(
    @SerializedName("actions") val actions: List<ShazamAction>?
)

data class ShazamAction(
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("uri") val uri: String?
)

data class ShazamImages(
    @SerializedName("coverart") val coverart: String?,
    @SerializedName("coverarthq") val coverarthq: String?,
    @SerializedName("background") val background: String?
)

data class ShazamShare(
    @SerializedName("subject") val subject: String?,
    @SerializedName("text") val text: String?,
    @SerializedName("href") val href: String?
)

data class ShazamGenres(
    @SerializedName("primary") val primary: String?
)

data class ShazamSection(
    @SerializedName("type") val type: String?,
    @SerializedName("text") val text: List<String>?,
    @SerializedName("metadata") val metadata: List<ShazamMetadataItem>? = null
)

data class ShazamMetadataItem(
    @SerializedName("title") val title: String?,
    @SerializedName("text") val text: String?
)
