package dev.anilbeesetti.nextplayer.feature.videopicker.screens.onboarding

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * ProfileStore (1:1 with Mochy's ProfileStore.kt).
 * Handles persistent storage of the user's profile picture by copying
 * the selected gallery/camera picture into the app's internal files directory.
 */
object ProfileStore {

    private const val PhotoPrefix = "profile_pic_"

    /**
     * Copies [uri] into the app's private files directory and returns the absolute path.
     */
    fun copyPhoto(context: Context, uri: Uri): String? = runCatching {
        val file = File(context.filesDir, "$PhotoPrefix${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    }.getOrNull()

    /**
     * Saves [uri] into the app files, updates SharedPreferences, and removes any previous picture file.
     */
    fun savePhoto(context: Context, uri: Uri?): String? {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences("krypton_profile_prefs", Context.MODE_PRIVATE)
        val oldPath = prefs.getString("profile_pic_uri", null)

        val newPath = uri?.let { copyPhoto(app, it) }
        if (newPath != null) {
            prefs.edit().putString("profile_pic_uri", newPath).apply()
            oldPath?.let { deleteOldPhoto(app, it) }
        }
        return newPath
    }

    /**
     * Deletes the user's profile photo and clears the preference.
     */
    fun deletePhoto(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences("krypton_profile_prefs", Context.MODE_PRIVATE)
        val oldPath = prefs.getString("profile_pic_uri", null)
        prefs.edit().remove("profile_pic_uri").apply()
        oldPath?.let { deleteOldPhoto(app, it) }
    }

    /**
     * Removes an old photo file if it was previously created by this store.
     */
    private fun deleteOldPhoto(context: Context, path: String) {
        try {
            val file = File(path)
            if (file.parentFile == context.filesDir && file.name.startsWith(PhotoPrefix)) {
                file.delete()
            }
        } catch (_: Exception) {}
    }
}
