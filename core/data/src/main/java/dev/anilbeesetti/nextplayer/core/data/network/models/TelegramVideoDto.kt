package dev.anilbeesetti.nextplayer.core.data.network.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class TelegramVideoResponse(
    @SerializedName("videos") val videos: List<TelegramVideoDto>
)

@Keep
data class TelegramVideoDto(
    @SerializedName("_id")
    val id: String? = null,
    @SerializedName("file_id")
    val fileId: String? = null,
    @SerializedName("chat_id")
    val chatId: Long? = null,
    @SerializedName("channel_id")
    val channelId: Long? = null,
    @SerializedName("message_id")
    val messageId: Long? = null,
    @SerializedName("file_name")
    val fileName: String? = null,
    @SerializedName("file_size")
    val fileSize: Long? = null,
    @SerializedName("mime_type")
    val mimeType: String? = null,
    @SerializedName("user_id")
    val userId: Long? = null,
    @SerializedName("duration")
    val duration: Double? = null,
    @SerializedName("thumb_url")
    val thumbUrl: String? = null,
    @SerializedName("thumb_file_id")
    val thumbFileId: String? = null,
    @SerializedName("added_at")
    val addedAt: String? = null
)
