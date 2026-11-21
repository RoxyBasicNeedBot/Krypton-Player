package dev.roxy.kryptonplayer.notification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AnnouncementLevel {
    INFO,
    WARNING,
    ALERT,
    UPDATE
}

data class AnnouncementBannerState(
    val isVisible: Boolean = false,
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val actionText: String? = null,
    val actionUrl: String? = null,
    val level: AnnouncementLevel = AnnouncementLevel.INFO
)

object FirebaseAnnouncementManager {
    private const val TAG = "FirebaseAnnouncement"
    private const val PREFS_NAME = "krypton_announcements_prefs"
    private const val KEY_DISMISSED_ID = "last_dismissed_announcement_id"

    private val _bannerState = MutableStateFlow(AnnouncementBannerState())
    val bannerState: StateFlow<AnnouncementBannerState> = _bannerState.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context, coroutineScope: CoroutineScope) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Fetch from Remote Config
        fetchFromRemoteConfig()

        // 2. Realtime listener from Firestore "announcements/active"
        coroutineScope.launch(Dispatchers.IO) {
            try {
                FirebaseFirestore.getInstance()
                    .collection("announcements")
                    .document("active")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w(TAG, "Listen failed for announcements/active: ${e.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val active = snapshot.getBoolean("active") ?: false
                            val id = snapshot.getString("id") ?: "active_announcement"
                            val title = snapshot.getString("title").orEmpty()
                            val message = snapshot.getString("message").orEmpty()
                            val actionText = snapshot.getString("actionText")
                            val actionUrl = snapshot.getString("actionUrl")
                            val levelStr = snapshot.getString("level").orEmpty()

                            val level = when (levelStr.lowercase()) {
                                "warning" -> AnnouncementLevel.WARNING
                                "alert" -> AnnouncementLevel.ALERT
                                "update" -> AnnouncementLevel.UPDATE
                                else -> AnnouncementLevel.INFO
                            }

                            val lastDismissed = prefs?.getString(KEY_DISMISSED_ID, "") ?: ""
                            val isDismissed = (id == lastDismissed)

                            if (active && !isDismissed && (title.isNotBlank() || message.isNotBlank())) {
                                _bannerState.value = AnnouncementBannerState(
                                    isVisible = true,
                                    id = id,
                                    title = title,
                                    message = message,
                                    actionText = actionText,
                                    actionUrl = actionUrl,
                                    level = level
                                )
                            } else if (!active || isDismissed) {
                                if (_bannerState.value.id == id) {
                                    _bannerState.value = _bannerState.value.copy(isVisible = false)
                                }
                            }
                        }
                    }
            } catch (ex: Exception) {
                Log.w(TAG, "Error initializing Firestore announcement listener: ${ex.message}")
            }
        }
    }

    fun fetchFromRemoteConfig() {
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val visible = remoteConfig.getBoolean("announcement_banner_visible")
            if (visible) {
                val id = remoteConfig.getString("announcement_banner_id").ifBlank { "rc_announcement" }
                val lastDismissed = prefs?.getString(KEY_DISMISSED_ID, "") ?: ""
                if (id != lastDismissed) {
                    val title = remoteConfig.getString("announcement_banner_title")
                    val message = remoteConfig.getString("announcement_banner_message")
                    val actionText = remoteConfig.getString("announcement_banner_action_text").takeIf { it.isNotBlank() }
                    val actionUrl = remoteConfig.getString("announcement_banner_action_url").takeIf { it.isNotBlank() }
                    val levelStr = remoteConfig.getString("announcement_banner_level")
                    val level = when (levelStr.lowercase()) {
                        "warning" -> AnnouncementLevel.WARNING
                        "alert" -> AnnouncementLevel.ALERT
                        "update" -> AnnouncementLevel.UPDATE
                        else -> AnnouncementLevel.INFO
                    }
                    if (title.isNotBlank() || message.isNotBlank()) {
                        _bannerState.value = AnnouncementBannerState(
                            isVisible = true,
                            id = id,
                            title = title,
                            message = message,
                            actionText = actionText,
                            actionUrl = actionUrl,
                            level = level
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load Remote Config announcement: ${e.message}")
        }
    }

    fun dismissBanner() {
        val currentId = _bannerState.value.id
        if (currentId.isNotBlank()) {
            prefs?.edit()?.putString(KEY_DISMISSED_ID, currentId)?.apply()
        }
        _bannerState.value = _bannerState.value.copy(isVisible = false)
    }
}
