package dev.anilbeesetti.nextplayer.core.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.data.network.DeviceSignupRequestDto
import dev.anilbeesetti.nextplayer.core.data.network.DeviceSignupResponse
import dev.anilbeesetti.nextplayer.core.data.network.DeviceStatusResponse
import dev.anilbeesetti.nextplayer.core.data.network.TelegramApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telegramApi: TelegramApi
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "device_auth_prefs"
        private const val KEY_APPROVAL_TOKEN = "approval_token"
        private const val KEY_APPROVAL_STATUS = "approval_status"
        private const val KEY_STABLE_DEVICE_ID = "stable_device_id"
        private const val KEY_IS_SIGNED_IN = "is_signed_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHOTO = "user_photo"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
    }

    fun isUserSignedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_SIGNED_IN, false) || isLocallyApproved()
    }

    fun saveUserAccount(email: String, displayName: String, photoUrl: String?, provider: String = "google") {
        val dummyToken = "auth_${UUID.randomUUID()}"
        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, true)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, displayName)
            .putString(KEY_USER_PHOTO, photoUrl)
            .putString(KEY_AUTH_PROVIDER, provider)
            .putString(KEY_APPROVAL_TOKEN, dummyToken)
            .putString(KEY_APPROVAL_STATUS, "approved")
            .apply()
        saveApproval(dummyToken)
    }

    fun continueAsGuest() {
        val guestToken = "guest_${UUID.randomUUID()}"
        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, true)
            .putString(KEY_USER_NAME, "Guest User")
            .putString(KEY_AUTH_PROVIDER, "guest")
            .putString(KEY_APPROVAL_TOKEN, guestToken)
            .putString(KEY_APPROVAL_STATUS, "approved")
            .apply()
        saveApproval(guestToken)
    }

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserPhoto(): String? = prefs.getString(KEY_USER_PHOTO, null)
    fun getAuthProvider(): String? = prefs.getString(KEY_AUTH_PROVIDER, null)

    fun signOut() {
        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, false)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PHOTO)
            .remove(KEY_AUTH_PROVIDER)
            .remove(KEY_APPROVAL_TOKEN)
            .apply()
        clearApproval()
    }

    val deviceId: String
        get() {
            val existing = prefs.getString(KEY_STABLE_DEVICE_ID, null)
            if (!existing.isNullOrBlank()) return existing

            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val finalId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                androidId
            } else {
                UUID.randomUUID().toString().replace("-", "").take(16)
            }
            prefs.edit().putString(KEY_STABLE_DEVICE_ID, finalId).apply()
            return finalId
        }

    val brand: String
        get() = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    val model: String
        get() = Build.MODEL

    val androidVersion: String
        get() = Build.VERSION.RELEASE ?: "Unknown"

    private val tokenFile: File
        get() = File(context.filesDir, "device_auth.token")

    fun isLocallyApproved(): Boolean {
        return !getApprovalToken().isNullOrBlank()
    }

    fun getApprovalToken(): String? {
        // 1. Check SharedPreferences
        val fromPrefs = prefs.getString(KEY_APPROVAL_TOKEN, null)
        if (!fromPrefs.isNullOrBlank()) return fromPrefs

        // 2. Fallback to private file cache (Telegram/Nagram cache resilience)
        try {
            if (tokenFile.exists()) {
                val fromFile = tokenFile.readText().trim()
                if (fromFile.isNotBlank()) {
                    prefs.edit()
                        .putString(KEY_APPROVAL_TOKEN, fromFile)
                        .putString(KEY_APPROVAL_STATUS, "approved")
                        .commit()
                    return fromFile
                }
            }
        } catch (_: Exception) {}

        return null
    }

    fun saveApproval(token: String) {
        prefs.edit()
            .putString(KEY_APPROVAL_TOKEN, token)
            .putString(KEY_APPROVAL_STATUS, "approved")
            .commit()

        try {
            tokenFile.writeText(token.trim())
        } catch (_: Exception) {}
    }

    fun saveStatus(status: String) {
        prefs.edit()
            .putString(KEY_APPROVAL_STATUS, status)
            .commit()
    }

    fun getApprovalStatus(): String {
        return prefs.getString(KEY_APPROVAL_STATUS, if (isLocallyApproved()) "approved" else "not_registered") ?: "not_registered"
    }

    fun clearApproval() {
        prefs.edit()
            .remove(KEY_APPROVAL_TOKEN)
            .putString(KEY_APPROVAL_STATUS, "rejected")
            .commit()

        try {
            if (tokenFile.exists()) {
                tokenFile.delete()
            }
        } catch (_: Exception) {}
    }

    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    fun observeNetworkConnectivity(): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(isOnline())
            }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }
        trySend(isOnline())
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            trySend(isOnline())
        }
        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }

    suspend fun requestAccess(): Result<DeviceSignupResponse> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext Result.failure(IllegalStateException("Internet connection required to request device authorization."))
        }
        try {
            val req = DeviceSignupRequestDto(
                deviceId = deviceId,
                brand = brand,
                model = model,
                androidVersion = androidVersion
            )
            val resp = telegramApi.signupDevice(req)
            if (resp.status == "approved" && !resp.approvalToken.isNullOrBlank()) {
                saveApproval(resp.approvalToken)
            } else {
                saveStatus(resp.status)
            }
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkStatus(): Result<DeviceStatusResponse> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext Result.failure(IllegalStateException("Internet connection required to verify device authorization."))
        }
        try {
            val resp = telegramApi.getDeviceStatus(deviceId)
            when (resp.status) {
                "approved" -> {
                    if (!resp.approvalToken.isNullOrBlank()) {
                        saveApproval(resp.approvalToken)
                    }
                }
                "rejected" -> {
                    clearApproval()
                }
                else -> {
                    saveStatus(resp.status)
                }
            }
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
