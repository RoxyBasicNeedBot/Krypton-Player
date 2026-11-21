package dev.roxy.kryptonplayer.core.media.sync

import android.net.Uri

interface MediaInfoSynchronizer {

    fun sync(uri: Uri)

    suspend fun clearThumbnailsCache()
}
