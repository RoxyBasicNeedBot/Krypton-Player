package dev.anilbeesetti.nextplayer.core.data.network.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ChannelPostDto(
    @SerializedName("post_id") val postId: String,
    @SerializedName("channel_id") val channelId: Long,
    @SerializedName("message_id") val messageId: Int,
    @SerializedName("post_type") val postType: String, // "video", "photo", "album", "text", "document", "audio"
    @SerializedName("caption") val caption: String?,
    @SerializedName("date") val date: String?,
    
    // Single media fields
    @SerializedName("file_id") val fileId: String?,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("file_size") val fileSize: Long?,
    @SerializedName("mime_type") val mimeType: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("thumb_file_id") val thumbFileId: String?,
    
    // Album media items
    @SerializedName("media_items") val mediaItems: List<MediaItemDto>?,
    
    // Metadata
    @SerializedName("views") val views: Int?,
    @SerializedName("forwards") val forwards: Int?,
    @SerializedName("reactions") val reactions: Map<String, Int>?,
    @SerializedName("reply_count") val replyCount: Int?
)

@Keep
data class MediaItemDto(
    @SerializedName("type") val type: String, // "photo", "video"
    @SerializedName("message_id") val messageId: Int?,
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("file_size") val fileSize: Long?,
    @SerializedName("mime_type") val mimeType: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("thumb_file_id") val thumbFileId: String?
)

@Keep
data class ChannelPostsResponse(
    @SerializedName("posts") val posts: List<ChannelPostDto>
)

@Keep
data class ChannelStatsResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("video") val video: Int,
    @SerializedName("photo") val photo: Int,
    @SerializedName("album") val album: Int,
    @SerializedName("text") val text: Int,
    @SerializedName("document") val document: Int,
    @SerializedName("audio") val audio: Int
)
