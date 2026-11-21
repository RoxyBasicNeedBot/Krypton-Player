package dev.roxy.kryptonplayer.core.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.Keep
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.roxy.kryptonplayer.core.data.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Keep
data class ApkAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val abi: String, // "arm64-v8a", "armeabi-v7a", "x86_64", "universal"
    val isRecommended: Boolean = false,
    val sha256Digest: String? = null,
)

@Keep
data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String,
    val downloadUrl: String,
    val fileName: String,
    val fileSize: Long,
    val isUpdateAvailable: Boolean,
    val releaseDate: String = "",
    val releaseUrl: String = "",
    val source: String = "",
    val tagName: String = "",
    val sha256Expected: String? = null,
    val apkAssets: List<ApkAsset> = emptyList(),
)

@Keep
data class GitHubCommitInfo(
    val sha: String,
    val shortSha: String,
    val message: String,
    val authorName: String,
    val date: String,
    val commitUrl: String,
)

@Keep
data class GitHubReleaseDto(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("html_url") val htmlUrl: String? = null,
    @SerializedName("assets") val assets: List<GitHubAssetDto> = emptyList(),
)

@Keep
data class GitHubAssetDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("size") val size: Long = 0L,
    @SerializedName("browser_download_url") val browserDownloadUrl: String = "",
    @SerializedName("url") val url: String? = null,
    @SerializedName("digest") val digest: String? = null,
)

typealias GitHubRelease = GitHubReleaseDto
typealias GitHubAsset = GitHubAssetDto

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val info: AppUpdateInfo) : UpdateState
    data class UpToDate(val currentVersion: String = "") : UpdateState
    data class Downloading(
        val progress: Int,
        val bytesDownloaded: Long = 0L,
        val totalBytes: Long = 0L,
        val speedBytesPerSec: Long = 0L,
        val info: AppUpdateInfo? = null,
    ) : UpdateState
    data class Verifying(val message: String = "Verifying package signature...") : UpdateState
    data class ReadyToInstall(val apkUri: Uri, val info: AppUpdateInfo? = null) : UpdateState
    data class Error(val message: String, val canRetry: Boolean = true, val info: AppUpdateInfo? = null) : UpdateState
}

object VersionComparator {
    fun cleanTag(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("release-", ignoreCase = true)) {
            cleaned = cleaned.substring("release-".length).trim()
        } else if (cleaned.startsWith("release/", ignoreCase = true)) {
            cleaned = cleaned.substring("release/".length).trim()
        } else if (cleaned.startsWith("rel-", ignoreCase = true)) {
            cleaned = cleaned.substring("rel-".length).trim()
        }
        if (cleaned.startsWith("v", ignoreCase = true)) {
            cleaned = cleaned.substring(1).trim()
        }
        return cleaned
    }

    fun parseSegments(version: String): List<Int> {
        val cleaned = cleanTag(version)
        val mainPart = cleaned.split("-", "+", "_", " ").firstOrNull() ?: ""
        return mainPart.split(".").map { segment ->
            val digits = segment.takeWhile { it.isDigit() }
            digits.toIntOrNull() ?: 0
        }
    }

    fun extractVersionCode(tag: String, name: String? = null, body: String? = null): Int {
        // 1. Check explicit build or versionCode tags in release notes/body
        if (!body.isNullOrBlank()) {
            val bodyMatch = Regex("""(?i)(?:versioncode|build\s*code|build\s*number|build)\s*[:=]\s*(\d+)""").find(body)
            if (bodyMatch != null) {
                bodyMatch.groupValues[1].toIntOrNull()?.let { if (it > 0) return it }
            }
        }
        // 2. Check tag name for build metadata (e.g., +104, -build.104, _b104)
        val tagBuildMatch = Regex("""(?i)(?:\+|build[-._]?|b[-._]?)(\d+)""").find(tag)
        if (tagBuildMatch != null) {
            tagBuildMatch.groupValues[1].toIntOrNull()?.let { if (it > 0) return it }
        }
        // 3. Check release title/name for build metadata
        if (!name.isNullOrBlank()) {
            val nameMatch = Regex("""(?i)(?:\+|build[-._]?|b[-._]?)(\d+)""").find(name)
            if (nameMatch != null) {
                nameMatch.groupValues[1].toIntOrNull()?.let { if (it > 0) return it }
            }
        }
        // 4. If tag has 4 numeric segments (e.g. 1.2.3.104)
        val segments = parseSegments(tag)
        if (segments.size >= 4 && segments[3] > 0) {
            return segments[3]
        }
        return 0
    }

    fun compare(v1: String, v2: String): Int {
        val segs1 = parseSegments(v1)
        val segs2 = parseSegments(v2)
        val maxLen = maxOf(segs1.size, segs2.size)

        for (i in 0 until maxLen) {
            val num1 = segs1.getOrElse(i) { 0 }
            val num2 = segs2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }

    fun isNewer(remoteVersion: String, currentVersion: String): Boolean {
        if (remoteVersion.isBlank()) return false
        if (currentVersion.isBlank()) return true
        return compare(remoteVersion, currentVersion) > 0
    }

    fun isUpdateAvailable(
        remoteVersionCode: Int,
        currentVersionCode: Int,
        remoteVersionName: String,
        currentVersionName: String,
    ): Boolean {
        if (remoteVersionName.isBlank() && remoteVersionCode <= 0) return false

        val nameComparison = if (remoteVersionName.isNotBlank() && currentVersionName.isNotBlank()) {
            compare(remoteVersionName, currentVersionName)
        } else 0

        // If version name indicates a newer version, an update is available
        if (nameComparison > 0) return true
        // If version name indicates an older version, no update
        if (nameComparison < 0) return false

        // Version names are identical: check version code if both are available
        if (remoteVersionCode > 0 && currentVersionCode > 0) {
            return remoteVersionCode > currentVersionCode
        }

        // If current version name could not be resolved, treat valid remote version as newer
        return currentVersionName.isBlank() && remoteVersionName.isNotBlank()
    }

    fun extractVersion(raw: String): String {
        return cleanTag(raw)
    }
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "KryptonAppUpdateManager"
        private const val GITHUB_REPO = "RoxyBasicNeedBot/Krypton-Player"
        private const val RELEASES_LATEST_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
        private const val RELEASES_ALL_URL = "https://api.github.com/repos/$GITHUB_REPO/releases?per_page=30"
        private const val COMMITS_URL = "https://api.github.com/repos/$GITHUB_REPO/commits?per_page=30"
        private const val USER_AGENT = "KryptonPlayer-SelfUpdate"

        private const val PREFS_NAME = "krypton_app_update_prefs"
        private const val KEY_LAST_CHECK_TIME = "last_check_time_ms"
        private const val KEY_CACHED_ETAG = "cached_etag"
        private const val KEY_CACHED_RELEASE_JSON = "cached_release_json"
        private const val KEY_SKIPPED_TAG = "skipped_tag"
        private const val KEY_PENDING_INSTALL_URI = "pending_install_uri"
        private const val KEY_PENDING_APK_PATH = "pending_apk_path"
        private const val KEY_PROMPT_SNOOZE_TIMESTAMP = "update_prompt_snooze_timestamp"

        private const val THROTTLE_INTERVAL_MS = 6L * 3600L * 1000L // 6 hours
        private const val SNOOZE_DURATION_MS = 24L * 3600L * 1000L // 24 hours
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _changelogReleases = MutableStateFlow<List<AppUpdateInfo>>(emptyList())
    val changelogReleases: StateFlow<List<AppUpdateInfo>> = _changelogReleases.asStateFlow()

    private val _gitHubCommits = MutableStateFlow<List<GitHubCommitInfo>>(emptyList())
    val gitHubCommits: StateFlow<List<GitHubCommitInfo>> = _gitHubCommits.asStateFlow()

    private var downloadJob: Job? = null
    private var pendingInstallUri: Uri? = null
    private var pendingApkFile: File? = null

    init {
        // Cleanup leftover .part and temporary update files on app startup
        cleanupOldUpdateFiles()
    }

    private fun cleanupOldUpdateFiles() {
        scope.launch(Dispatchers.IO) {
            try {
                val updatesDir = File(context.cacheDir, "updates")
                if (updatesDir.exists() && updatesDir.isDirectory) {
                    val files = updatesDir.listFiles() ?: return@launch
                    val pendingPath = prefs.getString(KEY_PENDING_APK_PATH, null)
                    for (file in files) {
                        if (file.absolutePath == pendingPath) {
                            continue
                        }
                        if (file.name.endsWith(".part") || System.currentTimeMillis() - file.lastModified() > 24 * 3600 * 1000L) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning old Krypton Player update files: ${e.message}")
            }
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun savePendingInstall(uri: Uri, apkFile: File? = null) {
        pendingInstallUri = uri
        pendingApkFile = apkFile
        prefs.edit().apply {
            putString(KEY_PENDING_INSTALL_URI, uri.toString())
            if (apkFile != null) {
                putString(KEY_PENDING_APK_PATH, apkFile.absolutePath)
            } else {
                remove(KEY_PENDING_APK_PATH)
            }
        }.apply()
        Log.d(TAG, "Saved pending installation: uri=$uri, file=${apkFile?.name}")
    }

    fun clearPendingInstall() {
        pendingInstallUri = null
        pendingApkFile = null
        prefs.edit()
            .remove(KEY_PENDING_INSTALL_URI)
            .remove(KEY_PENDING_APK_PATH)
            .apply()
        Log.d(TAG, "Cleared pending installation")
    }

    fun getPendingInstallUri(): Uri? {
        if (pendingInstallUri != null) return pendingInstallUri
        val savedPath = prefs.getString(KEY_PENDING_APK_PATH, null)
        if (!savedPath.isNullOrBlank()) {
            val file = File(savedPath)
            if (file.exists()) {
                pendingApkFile = file
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                    pendingInstallUri = uri
                    return uri
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to regenerate URI for pending APK: ${e.message}")
                }
            } else {
                clearPendingInstall()
                return null
            }
        }
        val savedUriStr = prefs.getString(KEY_PENDING_INSTALL_URI, null)
        if (!savedUriStr.isNullOrBlank()) {
            try {
                pendingInstallUri = Uri.parse(savedUriStr)
            } catch (_: Exception) {}
        }
        return pendingInstallUri
    }

    fun getPendingApkFile(): File? {
        if (pendingApkFile != null && pendingApkFile!!.exists()) return pendingApkFile
        val savedPath = prefs.getString(KEY_PENDING_APK_PATH, null)
        if (!savedPath.isNullOrBlank()) {
            val file = File(savedPath)
            if (file.exists()) {
                pendingApkFile = file
                return file
            }
        }
        return null
    }

    fun hasPendingInstall(): Boolean {
        return getPendingInstallUri() != null || (updateState.value as? UpdateState.ReadyToInstall)?.apkUri != null
    }

    fun requestInstallPermission(): Boolean {
        val intents = mutableListOf<Intent>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Primary: Manage unknown app sources targeted specifically to this package
            intents.add(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            )
            // Fallback 1: Manage unknown app sources general list
            intents.add(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            )
        }

        // Fallback 2: Security settings (standard fallback for custom OEM ROMs, Android TV, Fire OS)
        intents.add(
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        )

        // Fallback 3: General device settings
        intents.add(
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.i(TAG, "Successfully launched permission settings intent: ${intent.action} (data=${intent.data})")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch intent ${intent.action}: ${e.message}")
            }
        }

        Log.e(TAG, "All multi-tier fallback intents for unknown sources permission failed")
        return false
    }

    fun resumePendingInstall(): Boolean {
        val uri = getPendingInstallUri() ?: (updateState.value as? UpdateState.ReadyToInstall)?.apkUri
        if (uri != null && canRequestPackageInstalls()) {
            Log.d(TAG, "Resuming pending installation for: $uri")
            val file = getPendingApkFile()
            installApk(uri, file)
            return true
        }
        return false
    }

    fun skipVersion(tag: String) {
        val clean = VersionComparator.cleanTag(tag)
        prefs.edit().putString(KEY_SKIPPED_TAG, clean).apply()
        Log.i(TAG, "Marked Krypton Player version as skipped: $clean")
        _updateState.value = UpdateState.Idle
    }

    /**
     * Snoozes the update prompt for the specified duration (default 24 hours).
     */
    fun snoozeUpdatePrompt(durationMs: Long = SNOOZE_DURATION_MS) {
        val snoozeTime = System.currentTimeMillis()
        prefs.edit().putLong(KEY_PROMPT_SNOOZE_TIMESTAMP, snoozeTime).apply()
        Log.i(TAG, "Snoozed update prompt for ${durationMs / 3600000L} hours at $snoozeTime")
    }

    /**
     * Returns true if the update prompt is currently snoozed.
     */
    fun isUpdatePromptSnoozed(durationMs: Long = SNOOZE_DURATION_MS): Boolean {
        val snoozeTime = prefs.getLong(KEY_PROMPT_SNOOZE_TIMESTAMP, 0L)
        if (snoozeTime <= 0L) return false
        val isSnoozed = (System.currentTimeMillis() - snoozeTime) < durationMs
        return isSnoozed
    }

    /**
     * Clears the update prompt snooze state.
     */
    fun clearUpdatePromptSnooze() {
        prefs.edit().remove(KEY_PROMPT_SNOOZE_TIMESTAMP).apply()
    }

    private fun isVersionSkipped(tag: String): Boolean {
        val skipped = prefs.getString(KEY_SKIPPED_TAG, "") ?: ""
        if (skipped.isBlank()) return false
        val clean = VersionComparator.cleanTag(tag)
        return skipped.equals(clean, ignoreCase = true)
    }

    fun getInstalledAppArchitecture(): String {
        val supportedAbis = (Build.SUPPORTED_ABIS ?: emptyArray()).map { it.lowercase().trim() }
        for (abi in supportedAbis) {
            if (abi.contains("arm64-v8a") || abi.contains("arm64") || abi.contains("aarch64")) return "arm64-v8a"
            if (abi.contains("armeabi-v7a") || abi.contains("armv7") || abi.contains("armeabi")) return "armeabi-v7a"
            if (abi.contains("x86_64")) return "x86_64"
            if (abi.contains("x86")) return "x86"
        }
        return "universal"
    }

    fun getPrimaryAbi(): String = getInstalledAppArchitecture()

    fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = getPackageInfo()
            PackageInfoCompat.getLongVersionCode(pInfo).toInt()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve current versionCode: ${e.message}")
            0
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = getPackageInfo()
            pInfo.versionName?.trim().orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve current versionName: ${e.message}")
            ""
        }
    }

    private fun getPackageInfo(): PackageInfo {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
    }

    private fun resolveCurrentVersion(passedCode: Int, passedName: String): Pair<Int, String> {
        val resolvedCode = if (passedCode > 0) passedCode else getCurrentVersionCode()
        val resolvedName = if (passedName.isNotBlank()) passedName.trim() else getCurrentVersionName()
        return Pair(resolvedCode, resolvedName)
    }

    private fun selectBestAsset(assets: List<GitHubAssetDto>): GitHubAssetDto? {
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        val supportedAbis = (Build.SUPPORTED_ABIS ?: emptyArray()).map { it.lowercase().trim() }
        val isArm64Device = supportedAbis.any { it.contains("arm64-v8a") || it.contains("arm64") || it.contains("aarch64") }
        val isArmV7Device = supportedAbis.any { it.contains("armeabi-v7a") || it.contains("armv7") || it.contains("armeabi") }
        val isX86_64Device = supportedAbis.any { it.contains("x86_64") }
        val isX86Device = supportedAbis.any { it == "x86" || it.contains("i686") || it.contains("i386") }

        val arm64Asset = apkAssets.firstOrNull { asset ->
            val l = asset.name.lowercase()
            l.contains("arm64-v8a") || l.contains("arm64") || l.contains("aarch64")
        }

        val universalAsset = apkAssets.firstOrNull { asset ->
            asset.name.lowercase().contains("universal")
        } ?: apkAssets.firstOrNull { asset ->
            val l = asset.name.lowercase()
            !l.contains("arm64") && !l.contains("aarch64") &&
                !l.contains("armv7") && !l.contains("armeabi") &&
                !l.contains("x86")
        }

        val armV7Asset = apkAssets.firstOrNull { asset ->
            val l = asset.name.lowercase()
            (l.contains("armeabi-v7a") || l.contains("armv7") || l.contains("armeabi")) &&
                !l.contains("arm64") && !l.contains("aarch64")
        }

        val x86_64Asset = apkAssets.firstOrNull { asset ->
            asset.name.lowercase().contains("x86_64")
        }

        val x86Asset = apkAssets.firstOrNull { asset ->
            val l = asset.name.lowercase()
            l.contains("x86") && !l.contains("x86_64")
        }

        return when {
            // Modern 64-bit ARM devices: arm64-v8a -> universal -> armeabi-v7a
            isArm64Device -> {
                arm64Asset ?: universalAsset ?: armV7Asset ?: apkAssets.firstOrNull()
            }
            // Older 32-bit ARM devices: armeabi-v7a -> universal
            isArmV7Device && !isArm64Device -> {
                armV7Asset ?: universalAsset ?: apkAssets.firstOrNull()
            }
            // 64-bit x86 devices: x86_64 -> universal -> arm64 (translation)
            isX86_64Device -> {
                x86_64Asset ?: universalAsset ?: arm64Asset ?: apkAssets.firstOrNull()
            }
            // 32-bit x86 devices: x86 -> universal -> armeabi-v7a (translation)
            isX86Device -> {
                x86Asset ?: universalAsset ?: armV7Asset ?: apkAssets.firstOrNull()
            }
            // Generic fallback if SUPPORTED_ABIS is empty or unrecognized
            else -> {
                arm64Asset ?: universalAsset ?: armV7Asset ?: apkAssets.firstOrNull()
            }
        }
    }

    suspend fun checkForUpdates(
        currentVersionCode: Int = 0,
        currentVersionName: String = "",
        manual: Boolean = false,
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        if (!BuildConfig.ENABLE_SELF_UPDATE) {
            Log.d(TAG, "Krypton Player self-update is disabled by build configuration.")
            _updateState.value = UpdateState.Idle
            return@withContext null
        }

        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)

        if (!manual && (now - lastCheck < THROTTLE_INTERVAL_MS) && lastCheck > 0L) {
            Log.d(TAG, "Krypton Player update check throttled (checked within last 6 hours). Skipping auto-check.")
            return@withContext null
        }

        _updateState.value = UpdateState.Checking
        val (activeCode, activeName) = resolveCurrentVersion(currentVersionCode, currentVersionName)
        val cachedEtag = prefs.getString(KEY_CACHED_ETAG, null)

        val requestBuilder = Request.Builder()
            .url(RELEASES_LATEST_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)

        if (!cachedEtag.isNullOrBlank()) {
            requestBuilder.header("If-None-Match", cachedEtag)
        }

        var releaseDto: GitHubReleaseDto? = null

        try {
            val response: Response = httpClient.newCall(requestBuilder.build()).execute()
            response.use { resp ->
                val code = resp.code()

                if (code == 304) {
                    // Not Modified - release hasn't changed. Use cached JSON.
                    Log.d(TAG, "GitHub releases returned 304 Not Modified. Using cached release data.")
                    val cachedJson = prefs.getString(KEY_CACHED_RELEASE_JSON, null)
                    if (!cachedJson.isNullOrBlank()) {
                        releaseDto = gson.fromJson(cachedJson, GitHubReleaseDto::class.java)
                    }
                    prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply()
                } else if (code in 200..299) {
                    val bodyString = resp.body()?.string()
                    if (!bodyString.isNullOrBlank()) {
                        releaseDto = gson.fromJson(bodyString, GitHubReleaseDto::class.java)
                        val newEtag = resp.header("ETag")
                        prefs.edit()
                            .putLong(KEY_LAST_CHECK_TIME, now)
                            .putString(KEY_CACHED_RELEASE_JSON, bodyString)
                            .apply {
                                if (!newEtag.isNullOrBlank()) {
                                    putString(KEY_CACHED_ETAG, newEtag)
                                }
                            }
                            .apply()
                    }
                } else if (code == 403 || code == 429) {
                    val remaining = resp.header("x-ratelimit-remaining")
                    val reset = resp.header("x-ratelimit-reset")
                    Log.w(TAG, "GitHub API rate limit reached (HTTP $code, remaining=$remaining, reset=$reset).")
                    val cachedJson = prefs.getString(KEY_CACHED_RELEASE_JSON, null)
                    if (!cachedJson.isNullOrBlank()) {
                        Log.i(TAG, "Rate limited by GitHub API; falling back to cached release data.")
                        releaseDto = gson.fromJson(cachedJson, GitHubReleaseDto::class.java)
                    } else {
                        if (manual) {
                            _updateState.value = UpdateState.Error("GitHub API rate limit reached. Please try again later.")
                        } else {
                            _updateState.value = UpdateState.Idle
                        }
                        return@withContext null
                    }
                } else {
                    Log.w(TAG, "GitHub releases API returned error HTTP $code: ${resp.message()}")
                    val cachedJson = prefs.getString(KEY_CACHED_RELEASE_JSON, null)
                    if (!cachedJson.isNullOrBlank()) {
                        Log.i(TAG, "Falling back to cached release data after HTTP $code.")
                        releaseDto = gson.fromJson(cachedJson, GitHubReleaseDto::class.java)
                    } else {
                        if (manual) {
                            _updateState.value = UpdateState.Error("Server returned code $code: ${resp.message()}")
                        } else {
                            _updateState.value = UpdateState.Idle
                        }
                        return@withContext null
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reaching GitHub releases API: ${e.message}")
            val cachedJson = prefs.getString(KEY_CACHED_RELEASE_JSON, null)
            if (!cachedJson.isNullOrBlank()) {
                Log.i(TAG, "Network exception; falling back to cached release data.")
                releaseDto = gson.fromJson(cachedJson, GitHubReleaseDto::class.java)
            } else {
                if (manual) {
                    _updateState.value = UpdateState.Error(e.message ?: "Failed to connect to update servers.")
                } else {
                    _updateState.value = UpdateState.Idle
                }
                return@withContext null
            }
        }

        if (releaseDto?.tagName.isNullOrBlank()) {
            val cachedJson = prefs.getString(KEY_CACHED_RELEASE_JSON, null)
            if (!cachedJson.isNullOrBlank()) {
                releaseDto = gson.fromJson(cachedJson, GitHubReleaseDto::class.java)
            }
        }

        val release = releaseDto
        if (release == null || release.tagName.isBlank()) {
            if (manual) {
                _updateState.value = UpdateState.Error("No releases available for Krypton Player.")
            } else {
                _updateState.value = UpdateState.Idle
            }
            return@withContext null
        }

        val remoteVersionName = VersionComparator.cleanTag(release.tagName)
        val remoteVersionCode = VersionComparator.extractVersionCode(
            tag = release.tagName,
            name = release.name,
            body = release.body,
        )

        val isNewer = VersionComparator.isUpdateAvailable(
            remoteVersionCode = remoteVersionCode,
            currentVersionCode = activeCode,
            remoteVersionName = remoteVersionName,
            currentVersionName = activeName,
        )

        if (!manual && isVersionSkipped(release.tagName)) {
            Log.d(TAG, "Release ${release.tagName} was skipped by user. Suppressing update dialog.")
            _updateState.value = UpdateState.Idle
            return@withContext null
        }

        val bestAsset = selectBestAsset(release.assets)

        val parsedAssets = release.assets.mapNotNull { asset ->
            val name = asset.name
            if (!name.endsWith(".apk", ignoreCase = true)) return@mapNotNull null
            val lowerName = name.lowercase()
            val abi = when {
                lowerName.contains("arm64-v8a") || lowerName.contains("arm64") || lowerName.contains("aarch64") -> "arm64-v8a"
                (lowerName.contains("armeabi-v7a") || lowerName.contains("armv7") || lowerName.contains("armeabi")) &&
                    !lowerName.contains("arm64") && !lowerName.contains("aarch64") -> "armeabi-v7a"
                lowerName.contains("x86_64") -> "x86_64"
                lowerName.contains("x86") -> "x86"
                else -> "universal"
            }
            ApkAsset(
                name = name,
                downloadUrl = asset.browserDownloadUrl.ifBlank { asset.url ?: "" },
                size = asset.size,
                abi = abi,
                isRecommended = (bestAsset?.name == name),
                sha256Digest = asset.digest,
            )
        }.sortedWith(compareByDescending<ApkAsset> { it.isRecommended }.thenBy { it.abi })

        // Check if there is a .sha256 checksum asset in the release
        val targetApkName = bestAsset?.name ?: ""
        val sha256Asset = release.assets.firstOrNull {
            it.name.equals("$targetApkName.sha256", ignoreCase = true) ||
                it.name.endsWith(".sha256", ignoreCase = true)
        }

        val expectedSha = bestAsset?.digest?.removePrefix("sha256:")?.trim()

        val defaultApkName = if (remoteVersionName.isNotBlank()) "Krypton-Player-v$remoteVersionName.apk" else "Krypton-Player.apk"

        val info = AppUpdateInfo(
            versionName = remoteVersionName,
            versionCode = remoteVersionCode,
            changelog = release.body ?: "",
            downloadUrl = bestAsset?.browserDownloadUrl ?: "",
            fileName = bestAsset?.name ?: defaultApkName,
            fileSize = bestAsset?.size ?: 0L,
            isUpdateAvailable = isNewer,
            releaseDate = release.publishedAt ?: "",
            releaseUrl = release.htmlUrl ?: "https://github.com/$GITHUB_REPO/releases",
            source = "GitHub ($GITHUB_REPO)",
            tagName = release.tagName,
            sha256Expected = expectedSha,
            apkAssets = parsedAssets,
        )

        Log.d(TAG, "Krypton Player update check result: remote=$remoteVersionName (code=$remoteVersionCode), current=$activeName (code=$activeCode), isNewer=$isNewer, asset=${info.fileName}")

        if (isNewer) {
            _updateState.value = UpdateState.Available(info)
        } else {
            _updateState.value = UpdateState.UpToDate(activeName)
        }

        return@withContext info
    }

    fun downloadAndInstall(info: AppUpdateInfo, asset: ApkAsset? = null) {
        val targetAsset = asset ?: info.apkAssets.firstOrNull { it.isRecommended } ?: info.apkAssets.firstOrNull()
        val targetUrl = targetAsset?.downloadUrl?.takeIf { it.isNotBlank() } ?: info.downloadUrl
        val targetFileName = targetAsset?.name?.takeIf { it.isNotBlank() } ?: info.fileName
        val targetSize = targetAsset?.size?.takeIf { it > 0L } ?: info.fileSize

        if (targetUrl.isBlank()) {
            _updateState.value = UpdateState.Error("No download URL available for this update.", canRetry = false, info = info)
            return
        }

        // Verify host security: must be github.com or githubusercontent.com or related CDN
        val parsedUri = Uri.parse(targetUrl)
        val host = parsedUri.host?.lowercase() ?: ""
        val isAllowedHost = host.endsWith("github.com") ||
            host.endsWith("githubusercontent.com") ||
            host.endsWith("amazonaws.com") ||
            host.endsWith("cloudfront.net")
        if (!isAllowedHost) {
            _updateState.value = UpdateState.Error("Untrusted download host: $host", canRetry = false, info = info)
            return
        }

        val requiredStorage = if (targetSize > 0L) (targetSize * 1.5).toLong() else 60L * 1024L * 1024L
        if (context.cacheDir.usableSpace < requiredStorage) {
            _updateState.value = UpdateState.Error(
                "Insufficient storage space. Need at least ${requiredStorage / (1024 * 1024)} MB free.",
                canRetry = true,
                info = info,
            )
            return
        }

        val updatedInfo = info.copy(
            downloadUrl = targetUrl,
            fileName = targetFileName,
            fileSize = targetSize,
        )

        startDownload(updatedInfo, targetAsset)
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        cleanupOldUpdateFiles()
        val current = _updateState.value
        val fallbackInfo = when (current) {
            is UpdateState.Downloading -> current.info
            is UpdateState.Available -> current.info
            is UpdateState.Error -> current.info
            else -> null
        }
        if (fallbackInfo != null) {
            _updateState.value = UpdateState.Available(fallbackInfo)
        } else {
            _updateState.value = UpdateState.Idle
        }
    }

    private fun startDownload(info: AppUpdateInfo, asset: ApkAsset?) {
        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkName = if (info.fileName.endsWith(".apk")) info.fileName else "${info.fileName}.apk"
            val partFile = File(updatesDir, "$apkName.part")
            val destFile = File(updatesDir, apkName)

            if (partFile.exists()) {
                partFile.delete()
            }
            if (destFile.exists()) {
                destFile.delete()
            }

            _updateState.value = UpdateState.Downloading(
                progress = 0,
                bytesDownloaded = 0L,
                totalBytes = info.fileSize,
                speedBytesPerSec = 0L,
                info = info,
            )

            val request = Request.Builder()
                .url(info.downloadUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/octet-stream")
                .build()

            var totalBytes = info.fileSize
            var downloadedBytes = 0L

            try {
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("HTTP error ${response.code()}: ${response.message()}")
                }

                val body = response.body() ?: throw IOException("Empty response body from update server")
                val contentLen = body.contentLength()
                if (contentLen > 0L) {
                    totalBytes = contentLen
                }

                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastSpeedCalc = 0L
                var currentSpeed = 0L
                var lastUiUpdateTime = 0L

                body.byteStream().use { inputStream ->
                    FileOutputStream(partFile).use { outputStream ->
                        val buffer = ByteArray(64 * 1024)
                        var read = inputStream.read(buffer)

                        while (isActive && read != -1) {
                            outputStream.write(buffer, 0, read)
                            downloadedBytes += read
                            bytesSinceLastSpeedCalc += read

                            val now = System.currentTimeMillis()
                            val speedDuration = now - lastSpeedCalcTime
                            if (speedDuration >= 350L) {
                                currentSpeed = (bytesSinceLastSpeedCalc * 1000L) / speedDuration
                                bytesSinceLastSpeedCalc = 0L
                                lastSpeedCalcTime = now
                            }

                            if (now - lastUiUpdateTime >= 150L) {
                                val progress = if (totalBytes > 0L) {
                                    ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                } else -1

                                _updateState.value = UpdateState.Downloading(
                                    progress = progress,
                                    bytesDownloaded = downloadedBytes,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = currentSpeed,
                                    info = info,
                                )
                                lastUiUpdateTime = now
                            }

                            read = inputStream.read(buffer)
                        }
                    }
                }

                if (!isActive) {
                    partFile.delete()
                    return@launch
                }

                // Download completed, rename .part to .apk
                if (partFile.renameTo(destFile)) {
                    _updateState.value = UpdateState.Verifying("Verifying Krypton Player update integrity...")
                    verifyAndPrepareInstall(destFile, info, asset)
                } else {
                    throw IOException("Could not finalize downloaded Krypton Player update package.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for Krypton Player update", e)
                partFile.delete()
                if (isActive) {
                    _updateState.value = UpdateState.Error("Download failed: ${e.message}", canRetry = true, info = info)
                }
            }
        }
    }

    private suspend fun verifyAndPrepareInstall(
        apkFile: File,
        info: AppUpdateInfo,
        asset: ApkAsset?,
    ) = withContext(Dispatchers.IO) {
        // 1. Verify SHA-256 Checksum if digest or companion sha256 asset is available
        val expectedSha = asset?.sha256Digest?.removePrefix("sha256:")?.trim()
            ?: info.sha256Expected
            ?: fetchRemoteSha256Checksum(info.fileName)

        if (!expectedSha.isNullOrBlank()) {
            val calculatedSha = calculateSha256(apkFile)
            if (!calculatedSha.equals(expectedSha, ignoreCase = true)) {
                Log.e(TAG, "SHA-256 verification failed! Calculated: $calculatedSha, Expected: $expectedSha")
                apkFile.delete()
                _updateState.value = UpdateState.Error(
                    "Verification failed: Checksum mismatch. The file may be corrupted.",
                    canRetry = true,
                    info = info,
                )
                return@withContext
            }
            Log.d(TAG, "SHA-256 verification passed: $calculatedSha")
        } else {
            Log.d(TAG, "No SHA-256 checksum provided for ${info.fileName}, skipping checksum check.")
        }

        // 2. Package Archive and Signing Verification
        val archivePackageInfo = getArchivePackageInfo(apkFile)

        if (archivePackageInfo == null || archivePackageInfo.packageName.isNullOrBlank()) {
            apkFile.delete()
            _updateState.value = UpdateState.Error(
                "Invalid APK package: The downloaded Krypton Player update file could not be parsed.",
                canRetry = false,
                info = info,
            )
            return@withContext
        }

        // Check package name
        if (archivePackageInfo.packageName != context.packageName) {
            Log.e(TAG, "Package name mismatch: APK is ${archivePackageInfo.packageName}, expected ${context.packageName}")
            apkFile.delete()
            _updateState.value = UpdateState.Error(
                "Package mismatch: Expected ${context.packageName} but got ${archivePackageInfo.packageName}",
                canRetry = false,
                info = info,
            )
            return@withContext
        }

        // Check signing certificates against current Krypton Player signatures
        val signaturesMatch = verifySignatures(archivePackageInfo)
        if (!signaturesMatch) {
            Log.e(TAG, "Security verification failed: APK signing certificate does not match installed Krypton Player.")
            apkFile.delete()
            _updateState.value = UpdateState.Error(
                "Security check failed: APK signature does not match Krypton Player. For your security, this update cannot be installed.",
                canRetry = false,
                info = info,
            )
            return@withContext
        }

        // All checks passed! Prepare install URI
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
            savePendingInstall(contentUri, apkFile)
            _updateState.value = UpdateState.ReadyToInstall(contentUri, info)
            installApk(contentUri, apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "FileProvider error: ${e.message}", e)
            _updateState.value = UpdateState.Error("Failed to prepare installation package: ${e.message}", info = info)
        }
    }

    fun extractSignatures(packageInfo: PackageInfo): List<Signature> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            if (signingInfo != null) {
                val certs = if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
                if (!certs.isNullOrEmpty()) {
                    return certs.toList()
                }
            }
        }
        @Suppress("DEPRECATION")
        val sigs = packageInfo.signatures
        return sigs?.toList() ?: emptyList()
    }

    fun getArchivePackageInfo(apkFile: File): PackageInfo? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        var archiveInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(flags.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse APK archive: ${e.message}")
            null
        }

        // On some Android 9-13 devices, getPackageArchiveInfo with GET_SIGNING_CERTIFICATES
        // might not populate signingInfo unless GET_SIGNATURES is also passed as fallback
        if (archiveInfo != null && extractSignatures(archiveInfo).isEmpty()) {
            try {
                @Suppress("DEPRECATION")
                val fallbackInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
                if (fallbackInfo != null && extractSignatures(fallbackInfo).isNotEmpty()) {
                    archiveInfo = fallbackInfo
                }
            } catch (_: Exception) {}
        }

        return archiveInfo
    }

    private fun verifySignatures(archivePackageInfo: PackageInfo): Boolean {
        return try {
            val pm = context.packageManager
            val installedInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val installedSignatures = extractSignatures(installedInfo)
            val archiveSignatures = extractSignatures(archivePackageInfo)

            if (installedSignatures.isEmpty() || archiveSignatures.isEmpty()) {
                Log.w(TAG, "Signatures could not be fully extracted (installed=${installedSignatures.size}, archive=${archiveSignatures.size}). Allowing installation to proceed.")
                return true
            }

            val installedHashes = installedSignatures.map { it.toCharsString() }.toSet()
            val archiveHashes = archiveSignatures.map { it.toCharsString() }.toSet()

            val matches = installedHashes.intersect(archiveHashes).isNotEmpty()
            if (!matches) {
                Log.e(TAG, "Security check failed: APK signature mismatch. Installed: $installedHashes, Archive: $archiveHashes")
            }
            matches
        } catch (e: Exception) {
            Log.w(TAG, "Signature comparison exception: ${e.message}")
            true
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(64 * 1024)
            var read = inputStream.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = inputStream.read(buffer)
            }
        }
        val bytes = digest.digest()
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun fetchRemoteSha256Checksum(apkFileName: String): String? {
        return try {
            val url = "https://github.com/$GITHUB_REPO/releases/latest/download/$apkFileName.sha256"
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val content = response.body()?.string()?.trim() ?: return null
                // Extract 64-char hex string
                val hexRegex = Regex("""\b[a-fA-F0-9]{64}\b""")
                hexRegex.find(content)?.value
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun installApk(contentUri: Uri? = null, apkFile: File? = null) {
        val uri = contentUri ?: getPendingInstallUri() ?: (updateState.value as? UpdateState.ReadyToInstall)?.apkUri
        if (uri == null) {
            _updateState.value = UpdateState.Error("No update package available to install.")
            return
        }

        val fileToSave = apkFile ?: getPendingApkFile()
        savePendingInstall(uri, fileToSave)

        if (!canRequestPackageInstalls()) {
            Log.i(TAG, "Install unknown apps permission not granted for Krypton Player. Requesting via multi-tier fallback...")
            requestInstallPermission()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Explicit URI grants to resolved PackageInstaller packages
            val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            val targetPackages = mutableSetOf<String>()
            for (resolveInfo in resolveInfoList) {
                resolveInfo.activityInfo?.packageName?.let { targetPackages.add(it) }
            }
            // Add standard system package installer package names for explicit grant fallback
            targetPackages.add("com.google.android.packageinstaller")
            targetPackages.add("com.android.packageinstaller")

            for (pkg in targetPackages) {
                try {
                    context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not grant URI permission to $pkg: ${e.message}")
                }
            }

            context.startActivity(intent)
            Log.i(TAG, "Successfully launched PackageInstaller for Krypton Player update: $uri")

            // Once the PackageInstaller is launched successfully, clear pending installation
            // so returning to Krypton Player does not repeatedly trigger installation.
            clearPendingInstall()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer for Krypton Player", e)
            _updateState.value = UpdateState.Error("Failed to launch installer: ${e.message}")
        }
    }

    suspend fun getChangelogReleases(forceRefresh: Boolean = false): List<AppUpdateInfo> = withContext(Dispatchers.IO) {
        if (!forceRefresh && _changelogReleases.value.isNotEmpty()) {
            return@withContext _changelogReleases.value
        }

        val requestBuilder = Request.Builder()
            .url(RELEASES_ALL_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)

        val list = mutableListOf<AppUpdateInfo>()
        try {
            val response = httpClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string()?.trim() ?: ""
                val releases = gson.fromJson(jsonStr, Array<GitHubReleaseDto>::class.java) ?: emptyArray()
                val (activeCode, activeName) = resolveCurrentVersion(0, "")

                for (rel in releases) {
                    val ver = VersionComparator.cleanTag(rel.tagName)
                    val relCode = VersionComparator.extractVersionCode(
                        tag = rel.tagName,
                        name = rel.name,
                        body = rel.body,
                    )
                    val isNewer = VersionComparator.isUpdateAvailable(
                        remoteVersionCode = relCode,
                        currentVersionCode = activeCode,
                        remoteVersionName = ver,
                        currentVersionName = activeName,
                    )
                    val best = selectBestAsset(rel.assets)
                    val defaultApkName = if (ver.isNotBlank()) "Krypton-Player-v$ver.apk" else "Krypton-Player.apk"
                    list.add(
                        AppUpdateInfo(
                            versionName = ver,
                            versionCode = relCode,
                            changelog = rel.body ?: "",
                            downloadUrl = best?.browserDownloadUrl ?: "",
                            fileName = best?.name ?: defaultApkName,
                            fileSize = best?.size ?: 0L,
                            isUpdateAvailable = isNewer,
                            releaseDate = rel.publishedAt ?: "",
                            releaseUrl = rel.htmlUrl ?: "https://github.com/$GITHUB_REPO/releases",
                            source = "GitHub ($GITHUB_REPO)",
                            tagName = rel.tagName,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching changelog releases for Krypton Player: ${e.message}")
        }

        _changelogReleases.value = list
        return@withContext list
    }

    suspend fun getRecentCommits(forceRefresh: Boolean = false): List<GitHubCommitInfo> = withContext(Dispatchers.IO) {
        if (!forceRefresh && _gitHubCommits.value.isNotEmpty()) {
            return@withContext _gitHubCommits.value
        }

        val requestBuilder = Request.Builder()
            .url(COMMITS_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)

        val list = mutableListOf<GitHubCommitInfo>()
        try {
            val response = httpClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string()?.trim() ?: ""
                val array = gson.fromJson(jsonStr, List::class.java) ?: emptyList<Any>()
                for (item in array) {
                    val cMap = item as? Map<String, Any?> ?: continue
                    val sha = (cMap["sha"] as? String) ?: continue
                    val commitObj = cMap["commit"] as? Map<String, Any?> ?: continue
                    val message = (commitObj["message"] as? String) ?: ""
                    val authorObj = commitObj["author"] as? Map<String, Any?>
                    val authorName = (authorObj?.get("name") as? String) ?: "Developer"
                    val date = (authorObj?.get("date") as? String) ?: ""
                    val htmlUrl = (cMap["html_url"] as? String) ?: "https://github.com/$GITHUB_REPO/commit/$sha"

                    list.add(
                        GitHubCommitInfo(
                            sha = sha,
                            shortSha = sha.take(7),
                            message = message.trim(),
                            authorName = authorName,
                            date = date,
                            commitUrl = htmlUrl,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching GitHub commits for Krypton Player: ${e.message}")
        }

        _gitHubCommits.value = list
        return@withContext list
    }

    fun resetState() {
        downloadJob?.cancel()
        downloadJob = null
        _updateState.value = UpdateState.Idle
    }
}
