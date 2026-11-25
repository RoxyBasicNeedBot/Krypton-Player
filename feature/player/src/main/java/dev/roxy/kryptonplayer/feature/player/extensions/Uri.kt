package dev.roxy.kryptonplayer.feature.player.extensions

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable

fun Uri.getSubtitleMime(): String {
    return when {
        path?.endsWith(".ssa") == true || path?.endsWith(".ass") == true -> "text/x-ssa"
        path?.endsWith(".vtt") == true -> "text/vtt"
        path?.endsWith(".ttml") == true || path?.endsWith(".xml") == true || path?.endsWith(".dfxp") == true -> "application/ttml+xml"
        else -> "application/x-subrip"
    }
}

val Uri.isSchemaContent: Boolean
    get() = ContentResolver.SCHEME_CONTENT.equals(scheme, ignoreCase = true)

@Suppress("DEPRECATION")
fun Bundle.getParcelableUriArray(key: String): ArrayList<out Parcelable>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, Uri::class.java)
    } else {
        getParcelableArrayList(key)
    }
}
