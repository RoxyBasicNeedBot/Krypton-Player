package dev.roxy.kryptonplayer.settings.contributors

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

@Immutable
data class ContributorUiModel(
    val login: String,
    val avatarUrl: String,
    val profileUrl: String,
    val contributions: Int,
    val isLeadDev: Boolean = false,
)

/**
 * Fetches and caches live GitHub contributors for RoxyBasicNeedBot/Krypton-Player.
 */
object GitHubContributorsRepository {
    private const val CONTRIBUTORS_URL =
        "https://api.github.com/repos/RoxyBasicNeedBot/Krypton-Player/contributors?per_page=30"
    private const val LEAD_DEV_LOGIN = "RoxyBasicNeedBot"
    private const val TIMEOUT_MS = 8000

    private val fetchMutex = Mutex()

    @Volatile
    private var cachedContributors: List<ContributorUiModel>? = null

    fun fallbackContributors(): List<ContributorUiModel> = listOf(
        ContributorUiModel(
            login = LEAD_DEV_LOGIN,
            avatarUrl = "https://github.com/$LEAD_DEV_LOGIN.png?size=160",
            profileUrl = "https://github.com/$LEAD_DEV_LOGIN",
            contributions = 1,
            isLeadDev = true,
        ),
    )

    fun getCachedContributors(): List<ContributorUiModel> = cachedContributors.orEmpty()

    suspend fun fetchContributors(forceRefresh: Boolean = false): List<ContributorUiModel> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                cachedContributors?.let { return@withContext it }
            }

            fetchMutex.withLock {
                if (!forceRefresh) {
                    cachedContributors?.let { return@withLock it }
                }

                val liveList = runCatching { requestContributorsFromApi() }
                    .onFailure { throwable ->
                        if (throwable is CancellationException) throw throwable
                    }
                    .getOrNull()

                if (!liveList.isNullOrEmpty()) {
                    cachedContributors = liveList
                    return@withLock liveList
                }

                return@withLock cachedContributors ?: fallbackContributors()
            }
        }

    private fun requestContributorsFromApi(): List<ContributorUiModel> {
        val connection = (URL(CONTRIBUTORS_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "KryptonPlayer-Android")
        }

        return try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                emptyList()
            } else {
                val payload = connection.inputStream.bufferedReader().use { it.readText() }
                parseContributorsJson(payload)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseContributorsJson(payload: String): List<ContributorUiModel> {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return emptyList()

        val jsonArray = JSONArray(trimmed)
        val rawParsed = ArrayList<ContributorUiModel>(jsonArray.length())

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val login = item.optString("login", "").trim()
            val type = item.optString("type", "").trim()
            if (login.isBlank() ||
                type.equals("Bot", ignoreCase = true) ||
                login.endsWith("[bot]", ignoreCase = true)
            ) {
                continue
            }

            val avatarUrl = item.optString("avatar_url", "")
                .trim()
                .ifBlank { "https://github.com/$login.png?size=160" }
            val profileUrl = item.optString("html_url", "")
                .trim()
                .ifBlank { "https://github.com/$login" }
            val contributions = item.optInt("contributions", 0).coerceAtLeast(0)

            rawParsed.add(
                ContributorUiModel(
                    login = login,
                    avatarUrl = avatarUrl,
                    profileUrl = profileUrl,
                    contributions = contributions,
                    isLeadDev = false,
                ),
            )
        }

        if (rawParsed.isEmpty()) return emptyList()

        val sorted = rawParsed.sortedByDescending { it.contributions }
        val hasExplicitLead = sorted.any { it.login.equals(LEAD_DEV_LOGIN, ignoreCase = true) }

        return sorted.mapIndexed { index, contributor ->
            val isLead = if (hasExplicitLead) {
                contributor.login.equals(LEAD_DEV_LOGIN, ignoreCase = true)
            } else {
                index == 0
            }
            contributor.copy(isLeadDev = isLead)
        }
    }
}
