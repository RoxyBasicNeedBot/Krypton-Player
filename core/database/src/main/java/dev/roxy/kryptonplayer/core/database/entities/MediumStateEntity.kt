package dev.roxy.kryptonplayer.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_state",
    indices = [
        Index(value = ["uri"], unique = true),
    ],
)
data class MediumStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri")
    val uriString: String,
    @ColumnInfo(name = "playback_position")
    val playbackPosition: Long = 0,
    @ColumnInfo(name = "audio_track_index")
    val audioTrackIndex: Int? = null,
    @ColumnInfo(name = "subtitle_track_index")
    val subtitleTrackIndex: Int? = null,
    @ColumnInfo(name = "playback_speed")
    val playbackSpeed: Float? = null,
    @ColumnInfo(name = "last_played_time")
    val lastPlayedTime: Long? = null,
    @ColumnInfo(name = "external_subs")
    val externalSubs: String = "",
    @ColumnInfo(name = "video_scale")
    val videoScale: Float = 1f,
    @ColumnInfo(name = "subtitle_delay")
    val subtitleDelayMilliseconds: Long = 0,
    @ColumnInfo(name = "subtitle_speed")
    val subtitleSpeed: Float = 1f,
    @ColumnInfo(name = "watch_duration_ms", defaultValue = "0")
    val watchDurationMs: Long = 0L,
) {
    /**
     * Returns cumulative watch duration when recorded, falling back to non-negative
     * legacy playback position for pre-migration or unaccumulated rows.
     */
    fun effectiveWatchDurationMs(): Long =
        if (watchDurationMs > 0L) watchDurationMs else playbackPosition.coerceAtLeast(0L)
}
