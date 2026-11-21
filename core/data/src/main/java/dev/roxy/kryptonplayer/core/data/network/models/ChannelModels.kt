package dev.roxy.kryptonplayer.core.data.network.models

import androidx.annotation.Keep

@Keep data class AddChannelRequest(val username: String)
@Keep data class ChannelResponse(
    val channel_id: Long,
    val channel_username: String?,
    val channel_title: String?,
    val channel_photo: String?,
    val member_count: Int,
    val video_count: Int,
    val post_count: Int?,
    val last_scraped_at: String?,
    val added_at: String?
)
@Keep data class ChannelsListResponse(val channels: List<ChannelResponse>)
