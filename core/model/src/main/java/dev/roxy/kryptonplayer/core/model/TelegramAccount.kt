package dev.roxy.kryptonplayer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TelegramAccount(
    val token: String,
    val userId: Long,
    val username: String = "",
    val firstName: String = "",
    val profilePhoto: String = "",
    val botToken: String = "",
    val apiId: Int = 0,
    val apiHash: String = "",
    val phoneNumber: String = "",
    val isPhoneAuth: Boolean = false,
    val addedAt: Long = 0L
)
