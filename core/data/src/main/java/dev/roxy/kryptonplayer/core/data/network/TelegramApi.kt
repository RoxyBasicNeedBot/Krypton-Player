package dev.roxy.kryptonplayer.core.data.network

import dev.roxy.kryptonplayer.core.data.network.models.TelegramVideoResponse
import dev.roxy.kryptonplayer.core.data.network.models.CreateSyncRoomRequest
import dev.roxy.kryptonplayer.core.data.network.models.SyncRoomResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Body

import dev.roxy.kryptonplayer.core.data.network.models.AddChannelRequest
import dev.roxy.kryptonplayer.core.data.network.models.ChannelResponse
import dev.roxy.kryptonplayer.core.data.network.models.ChannelsListResponse

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

interface TelegramApi {
    @GET("api/videos")
    suspend fun getVideos(@Query("token") token: String): TelegramVideoResponse

    @DELETE("api/videos/{db_id}")
    suspend fun deleteVideo(
        @Path("db_id") dbId: String,
        @Query("token") token: String
    )

    @GET("api/auth/verify")
    suspend fun verifyToken(@Query("token") token: String): TelegramAuthResponse

    @GET("api/user/profile")
    suspend fun getUserProfile(@Query("token") token: String): TelegramUserProfileResponse

    @POST("api/sync/create")
    suspend fun createSyncRoom(
        @Query("token") token: String,
        @Body body: CreateSyncRoomRequest
    ): SyncRoomResponse

    @GET("api/sync/join/{roomId}")
    suspend fun joinSyncRoom(
        @Path("roomId") roomId: String,
        @Query("token") token: String
    ): SyncRoomResponse

    @POST("api/share/create")
    suspend fun createShareLink(
        @Query("token") token: String,
        @Body body: CreateShareRequest
    ): ShareResponse

    @POST("api/channels/add")
    suspend fun addChannel(
        @Query("token") token: String,
        @Body body: AddChannelRequest
    ): ChannelResponse

    @GET("api/channels")
    suspend fun getChannels(
        @Query("token") token: String
    ): ChannelsListResponse

    @DELETE("api/channels/{channelId}")
    suspend fun removeChannel(
        @Path("channelId") channelId: Long,
        @Query("token") token: String
    )

    @GET("api/channels/{channelId}/videos")
    suspend fun getChannelVideos(
        @Path("channelId") channelId: Long,
        @Query("token") token: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("q") query: String? = null
    ): TelegramVideoResponse

    @GET("api/channels/{channelId}/posts")
    suspend fun getChannelPosts(
        @Path("channelId") channelId: Long,
        @Query("token") token: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 30,
        @Query("q") searchQuery: String? = null,
        @Query("type") postType: String? = null
    ): dev.roxy.kryptonplayer.core.data.network.models.ChannelPostsResponse

    @GET("api/channels/{channelId}/stats")
    suspend fun getChannelStats(
        @Path("channelId") channelId: Long,
        @Query("token") token: String
    ): dev.roxy.kryptonplayer.core.data.network.models.ChannelStatsResponse

    @POST("api/channels/{channelId}/refresh")
    suspend fun refreshChannel(
        @Path("channelId") channelId: Long,
        @Query("token") token: String
    )

    @POST("api/auth/signup")
    suspend fun signupDevice(
        @Body body: DeviceSignupRequestDto
    ): DeviceSignupResponse

    @GET("api/auth/device-status")
    suspend fun getDeviceStatus(
        @Query("device_id") deviceId: String
    ): DeviceStatusResponse
}

@Keep
data class CreateShareRequest(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("duration_hours") val durationHours: Int
)

@Keep
data class ShareResponse(
    @SerializedName("share_url") val shareUrl: String,
    @SerializedName("expires_at") val expiresAt: String
)


@Keep
data class StreamConfigDto(
    @SerializedName("bot_token") val botToken: String? = null,
    @SerializedName("api_id") val apiId: Int? = null,
    @SerializedName("api_hash") val apiHash: String? = null
)

@Keep
data class TelegramAuthResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user_id") val user_id: Long,
    @SerializedName("username") val username: String?,
    @SerializedName("first_name") val first_name: String?,
    @SerializedName("profile_photo") val profile_photo: String?,
    @SerializedName("stream_config") val streamConfig: StreamConfigDto? = null
)

@Keep
data class TelegramUserProfileResponse(
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("profile_photo") val profilePhoto: String?
)

@Keep
data class DeviceSignupRequestDto(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("android_version") val androidVersion: String
)

@Keep
data class DeviceSignupResponse(
    @SerializedName("status") val status: String,
    @SerializedName("approval_token") val approvalToken: String? = null,
    @SerializedName("message") val message: String? = null
)

@Keep
data class DeviceStatusResponse(
    @SerializedName("status") val status: String,
    @SerializedName("approval_token") val approvalToken: String? = null,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("message") val message: String? = null
)
