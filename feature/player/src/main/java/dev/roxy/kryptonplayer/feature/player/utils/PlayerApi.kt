package dev.roxy.kryptonplayer.feature.player.utils

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import dev.roxy.kryptonplayer.feature.player.PlayerActivity
import dev.roxy.kryptonplayer.feature.player.extensions.getParcelableUriArray

class PlayerApi(val activity: PlayerActivity) {
    private val extras: Bundle? = activity.intent.extras

    val isApiAccess: Boolean get() = extras != null
    val hasPosition: Boolean get() = extras?.containsKey(API_POSITION) == true
    val hasTitle: Boolean get() = extras?.containsKey(API_TITLE) == true
    val shouldReturnResult: Boolean get() = extras?.containsKey(API_RETURN_RESULT) == true

    val title: String?
        get() = if (hasTitle) extras?.getString(API_TITLE) else null

    val position: Int?
        get() = if (hasPosition) extras?.getInt(API_POSITION) else null

    fun getSubs(): List<Sub> {
        if (extras == null) return emptyList()
        if (!extras.containsKey(API_SUBS)) return emptyList()

        val subs = extras.getParcelableUriArray(API_SUBS) ?: return emptyList()
        val subsName = extras.getStringArray(API_SUBS_NAME)
        val subsEnable = extras.getParcelableUriArray(API_SUBS_ENABLE)
        val selectedSubUri = subsEnable?.firstOrNull() as? Uri

        return subs.mapIndexedNotNull { index, parcelable ->
            val subUri = parcelable as? Uri ?: return@mapIndexedNotNull null
            val subName = subsName?.getOrNull(index)
            val isSelected = selectedSubUri != null && selectedSubUri == subUri
            Sub(name = subName, uri = subUri, isSelected = isSelected)
        }
    }

    fun getPlaylist(): List<String> {
        if (extras == null) return emptyList()
        val videoList = extras.getParcelableUriArray(API_VIDEO_LIST) ?: return emptyList()
        return videoList.map { it.toString() }
    }

    fun getResult(isPlaybackFinished: Boolean, duration: Long, position: Long): Intent {
        return Intent(API_RESULT_INTENT).apply {
            if (isPlaybackFinished) {
                putExtra(API_END_BY, API_END_BY_COMPLETION)
            } else {
                putExtra(API_END_BY, API_END_BY_USER)
                if (duration > 0L) putExtra(API_DURATION, duration.toInt())
                if (position >= 0L) putExtra(API_POSITION, position.toInt())
            }
        }
    }

    data class Sub(
        val name: String?,
        val uri: Uri,
        val isSelected: Boolean,
    )

    companion object {
        private const val API_TITLE = "title"
        private const val API_POSITION = "position"
        private const val API_DURATION = "duration"
        private const val API_RETURN_RESULT = "return_result"
        private const val API_END_BY = "end_by"
        private const val API_END_BY_COMPLETION = "playback_completion"
        private const val API_END_BY_USER = "user"
        private const val API_SUBS = "subs"
        private const val API_SUBS_ENABLE = "subs.enable"
        private const val API_SUBS_NAME = "subs.name"
        private const val API_VIDEO_LIST = "video_list"
        private const val API_RESULT_INTENT = "com.mxtech.intent.result.VIEW"

        /** Public key used by callers (e.g. MediaNavGraph) to pass a playlist of URIs. */
        const val API_PLAYLIST = API_VIDEO_LIST
    }
}
