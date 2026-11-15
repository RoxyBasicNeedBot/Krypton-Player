package dev.anilbeesetti.nextplayer.core.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramLikedRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TelegramLikedRepository"
        private const val LIKED_DIR = "telegram_liked"
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val likedDir: File by lazy {
        File(context.filesDir, LIKED_DIR).apply { mkdirs() }
    }

    private val _likedVideos = MutableStateFlow<List<Video>>(emptyList())
    val likedVideos: StateFlow<List<Video>> = _likedVideos.asStateFlow()

    private val _likedKeys = MutableStateFlow<Set<String>>(emptySet())
    val likedKeys: StateFlow<Set<String>> = _likedKeys.asStateFlow()

    private var currentToken: String = ""

    private fun getUserSuffix(token: String?): String {
        if (token.isNullOrBlank()) return "default"
        return token.trim().takeLast(12).replace(Regex("[^a-zA-Z0-9]"), "_")
    }

    private fun getLikedFile(token: String?): File {
        return File(likedDir, "liked_videos_${getUserSuffix(token)}.json")
    }

    private fun extractKeys(video: Video): Set<String> {
        val keys = mutableSetOf<String>()
        video.externalId?.takeIf { it.isNotBlank() && it != "stream" }?.let {
            keys.add(it)
            keys.add(it.trim())
        }

        fun parseUriAndAdd(raw: String?) {
            if (raw.isNullOrBlank()) return
            val str = raw.trim()
            keys.add(str)
            if (str.startsWith("tdlib://")) {
                try {
                    val uri = android.net.Uri.parse(str)
                    uri.getQueryParameter("fileId")?.takeIf { it.isNotBlank() }?.let { keys.add(it) }
                    uri.getQueryParameter("name")?.takeIf { it.isNotBlank() }?.let {
                        keys.add(it)
                        keys.add(it.substringAfterLast("/"))
                    }
                    val chatId = uri.getQueryParameter("chatId")
                    val messageId = uri.getQueryParameter("messageId")
                    if (!chatId.isNullOrBlank() && !messageId.isNullOrBlank()) {
                        keys.add("${chatId}_$messageId")
                    }
                } catch (_: Exception) {}
            } else if (str.contains("/api/stream/")) {
                val id = str.substringAfter("/api/stream/").substringBefore("?").substringBefore("/")
                if (id.isNotBlank() && id != "stream") keys.add(id)
            }
            val filename = str.substringBefore("?").substringAfterLast("/")
            if (filename.isNotBlank() && filename != "stream") {
                keys.add(filename)
            }
        }

        parseUriAndAdd(video.path)
        parseUriAndAdd(video.uriString)
        video.nameWithExtension.takeIf { it.isNotBlank() }?.let {
            keys.add(it)
            keys.add(it.trim())
        }
        return keys
    }

    private fun buildAllKeys(list: List<Video>): Set<String> {
        val set = mutableSetOf<String>()
        for (v in list) {
            set.addAll(extractKeys(v))
        }
        return set
    }

    private fun matchesVideo(existing: Video, target: Video, key: String?): Boolean {
        if (!target.externalId.isNullOrBlank() && target.externalId != "stream" && existing.externalId == target.externalId) return true
        if (target.path.isNotBlank() && existing.path == target.path) return true
        if (target.uriString.isNotBlank() && existing.uriString == target.uriString) return true
        if (target.nameWithExtension.isNotBlank() && existing.nameWithExtension == target.nameWithExtension) return true

        if (!key.isNullOrBlank()) {
            val existingKeys = extractKeys(existing)
            val cleanKey = key.trim()
            if (existingKeys.contains(cleanKey)) return true
            if (cleanKey.startsWith("tdlib://")) {
                try {
                    val uri = android.net.Uri.parse(cleanKey)
                    val fileId = uri.getQueryParameter("fileId")
                    if (!fileId.isNullOrBlank() && existingKeys.contains(fileId)) return true
                    val name = uri.getQueryParameter("name")
                    if (!name.isNullOrBlank() && (existingKeys.contains(name) || existingKeys.contains(name.substringAfterLast("/")))) return true
                    val chatId = uri.getQueryParameter("chatId")
                    val messageId = uri.getQueryParameter("messageId")
                    if (!chatId.isNullOrBlank() && !messageId.isNullOrBlank() && existingKeys.contains("${chatId}_$messageId")) return true
                } catch (_: Exception) {}
            } else if (cleanKey.contains("/api/stream/")) {
                val streamId = cleanKey.substringAfter("/api/stream/").substringBefore("?").substringBefore("/")
                if (streamId.isNotBlank() && streamId != "stream" && existingKeys.contains(streamId)) return true
            }
            val filename = cleanKey.substringBefore("?").substringAfterLast("/")
            if (filename.isNotBlank() && filename != "stream" && existingKeys.contains(filename)) return true
        }
        return false
    }

    fun loadForUser(token: String) {
        currentToken = token
        scope.launch {
            try {
                val file = getLikedFile(token)
                if (file.exists()) {
                    val json = file.readText(Charsets.UTF_8)
                    val type = object : TypeToken<List<Video>>() {}.type
                    val list: List<Video> = gson.fromJson(json, type) ?: emptyList()
                    _likedVideos.value = list
                    _likedKeys.value = buildAllKeys(list)
                } else {
                    _likedVideos.value = emptyList()
                    _likedKeys.value = emptySet()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading liked videos for user", e)
                _likedVideos.value = emptyList()
                _likedKeys.value = emptySet()
            }
        }
    }

    fun isLiked(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        val cleanKey = key.trim()
        val currentKeys = _likedKeys.value
        if (currentKeys.contains(cleanKey)) return true
        if (cleanKey.startsWith("tdlib://")) {
            try {
                val uri = android.net.Uri.parse(cleanKey)
                val fileId = uri.getQueryParameter("fileId")
                if (!fileId.isNullOrBlank() && currentKeys.contains(fileId)) return true
                val name = uri.getQueryParameter("name")
                if (!name.isNullOrBlank() && (currentKeys.contains(name) || currentKeys.contains(name.substringAfterLast("/")))) return true
                val chatId = uri.getQueryParameter("chatId")
                val messageId = uri.getQueryParameter("messageId")
                if (!chatId.isNullOrBlank() && !messageId.isNullOrBlank() && currentKeys.contains("${chatId}_$messageId")) return true
            } catch (_: Exception) {}
        }
        if (cleanKey.contains("/api/stream/")) {
            val streamId = cleanKey.substringAfter("/api/stream/").substringBefore("?").substringBefore("/")
            if (streamId.isNotBlank() && streamId != "stream" && currentKeys.contains(streamId)) return true
        }
        val filename = cleanKey.substringBefore("?").substringAfterLast("/")
        if (filename.isNotBlank() && filename != "stream" && currentKeys.contains(filename)) return true
        return false
    }

    fun toggleLike(video: Video, token: String = currentToken): Boolean {
        val targetToken = if (token.isNotBlank()) token else currentToken
        val key = video.externalId?.takeIf { it.isNotBlank() } ?: video.path
        val isCurrentlyLiked = isLiked(key) || isLiked(video.nameWithExtension) || isLiked(video.uriString)
        val newLikedState = !isCurrentlyLiked

        val currentList = _likedVideos.value.toMutableList()
        if (newLikedState) {
            currentList.removeAll { matchesVideo(it, video, key) }
            currentList.add(0, video)
        } else {
            currentList.removeAll { matchesVideo(it, video, key) }
        }

        // Update in-memory state SYNCHRONOUSLY for instant UI response
        _likedVideos.value = currentList
        _likedKeys.value = buildAllKeys(currentList)

        // Save to disk asynchronously
        scope.launch(Dispatchers.IO) {
            try {
                val file = getLikedFile(targetToken)
                val json = gson.toJson(currentList)
                file.writeText(json, Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving liked videos", e)
            }
        }

        return newLikedState
    }

    fun removeLike(key: String?, token: String = currentToken) {
        if (key.isNullOrBlank()) return
        val targetToken = if (token.isNotBlank()) token else currentToken
        val currentList = _likedVideos.value.toMutableList()
        currentList.removeAll { it.externalId == key || it.path == key || it.uriString == key || it.nameWithExtension == key }
        _likedVideos.value = currentList
        _likedKeys.value = buildAllKeys(currentList)

        scope.launch(Dispatchers.IO) {
            try {
                val file = getLikedFile(targetToken)
                val json = gson.toJson(currentList)
                file.writeText(json, Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing liked video", e)
            }
        }
    }

    fun clearAll(token: String = currentToken) {
        val targetToken = if (token.isNotBlank()) token else currentToken
        _likedVideos.value = emptyList()
        _likedKeys.value = emptySet()
        scope.launch(Dispatchers.IO) {
            try {
                val file = getLikedFile(targetToken)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing liked videos", e)
            }
        }
    }
}
