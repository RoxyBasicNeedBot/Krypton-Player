package dev.roxy.kryptonplayer.core.data.cache

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramImageDiskCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TelegramImageDiskCache"
        private const val THUMB_DIR = "telegram_thumbs"
    }

    private val thumbDir: File by lazy {
        File(context.cacheDir, THUMB_DIR).apply { mkdirs() }
    }

    private fun hashKey(key: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(key.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            key.replace(Regex("[^a-zA-Z0-9_]"), "_")
        }
    }

    private fun isValidImageFile(file: File): Boolean {
        if (!file.exists() || file.length() <= 0) return false
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            false
        }
    }

    fun getCachedFile(key: String): File? {
        if (key.isBlank()) return null
        val filename = "${hashKey(key)}.jpg"
        val file = File(thumbDir, filename)
        if (file.exists() && file.length() > 0) {
            if (isValidImageFile(file)) {
                return file
            } else {
                // Delete invalid or non-image cached file (e.g. previously poisoned with MP4 or 0-bytes)
                file.delete()
                return null
            }
        }
        return null
    }

    fun hasCache(key: String): Boolean {
        return getCachedFile(key) != null
    }

    fun putFile(key: String, sourceFile: File): File? {
        if (key.isBlank() || !sourceFile.exists() || sourceFile.length() <= 0) return null
        if (!isValidImageFile(sourceFile)) return null
        return try {
            val targetFile = File(thumbDir, "${hashKey(key)}.jpg")
            sourceFile.copyTo(targetFile, overwrite = true)
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache image file for key: $key", e)
            null
        }
    }

    fun putBytes(key: String, bytes: ByteArray): File? {
        if (key.isBlank() || bytes.isEmpty()) return null
        return try {
            val targetFile = File(thumbDir, "${hashKey(key)}.jpg")
            FileOutputStream(targetFile).use { it.write(bytes) }
            if (isValidImageFile(targetFile)) {
                targetFile
            } else {
                targetFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache image bytes for key: $key", e)
            null
        }
    }

    fun getCacheSizeBytes(): Long {
        return thumbDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearCache(): Boolean {
        return try {
            thumbDir.deleteRecursively()
            thumbDir.mkdirs()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing thumbnail cache", e)
            false
        }
    }
}
