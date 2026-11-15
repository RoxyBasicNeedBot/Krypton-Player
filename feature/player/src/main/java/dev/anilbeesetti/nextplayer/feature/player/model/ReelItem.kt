package dev.anilbeesetti.nextplayer.feature.player.model

import kotlinx.serialization.Serializable

@Serializable
data class ReelItem(
    val uri: String,                  // stream URL or content:// URI
    val title: String,                // video name
    val duration: Long = 0,           // duration in ms
    val thumbnailUri: String? = null, // artwork URI
    val source: String = "local",     // "local", "cloud", or "channel"
    val isImage: Boolean = false,     // true if it is an image
    val dbId: String? = null          // database/cloud ID for deletion
)
