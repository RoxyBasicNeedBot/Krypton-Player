package dev.roxy.kryptonplayer.core.data.lyrics

import com.google.gson.annotations.SerializedName

data class LrcLibResponse(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("trackName") val trackName: String? = null,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("albumName") val albumName: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("instrumental") val instrumental: Boolean = false,
    @SerializedName("plainLyrics") val plainLyrics: String? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null
)

data class LyricsLine(
    val timeMs: Long,
    val text: String
)

data class LyricsData(
    val trackName: String,
    val artistName: String,
    val albumName: String? = null,
    val isSynced: Boolean,
    val lines: List<LyricsLine>,
    val plainLyrics: String? = null,
    val durationSeconds: Double? = null
)

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Searching : LyricsUiState
    data class Available(val lyrics: LyricsData) : LyricsUiState
    data class Error(val message: String) : LyricsUiState
}
