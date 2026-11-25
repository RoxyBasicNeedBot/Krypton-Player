package dev.roxy.kryptonplayer.core.data.repository

import android.content.Context
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class CountryInfo(
    val countryCode: String,
    val countryName: String,
    val isIndia: Boolean
)

@Singleton
class CountryDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val prefs = context.getSharedPreferences("geo_country_detector_prefs", Context.MODE_PRIVATE)
    private var inMemoryCountry: CountryInfo? = null

    suspend fun detectCountry(): CountryInfo = withContext(Dispatchers.IO) {
        inMemoryCountry?.let { return@withContext it }

        // 1. Check local cached country (valid for 24 hours)
        val cachedCode = prefs.getString("cached_country_code", null)
        val cachedName = prefs.getString("cached_country_name", null)
        val cachedTime = prefs.getLong("cached_timestamp", 0L)
        val isExpired = (System.currentTimeMillis() - cachedTime) > TimeUnit.HOURS.toMillis(24)

        if (!cachedCode.isNullOrBlank() && !cachedName.isNullOrBlank() && !isExpired) {
            val info = CountryInfo(
                countryCode = cachedCode.uppercase(Locale.ENGLISH),
                countryName = cachedName,
                isIndia = cachedCode.equals("IN", ignoreCase = true)
            )
            inMemoryCountry = info
            return@withContext info
        }

        // 2. Fetch via fast IP-based Geolocation API
        val detected = fetchCountryFromIp() ?: getDeviceFallbackCountry()

        // Cache the result
        prefs.edit()
            .putString("cached_country_code", detected.countryCode)
            .putString("cached_country_name", detected.countryName)
            .putLong("cached_timestamp", System.currentTimeMillis())
            .apply()

        inMemoryCountry = detected
        detected
    }

    private fun fetchCountryFromIp(): CountryInfo? {
        val endpoints = listOf(
            "https://api.country.is/",
            "http://ip-api.com/json/?fields=countryCode,country"
        )

        for (url in endpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 KryptonPlayer/1.0")
                    .build()

                // Short 3-second timeout for quick resolution
                val client = okHttpClient.newBuilder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)

                        val code = when {
                            json.has("country") && json.getString("country").length == 2 -> json.getString("country")
                            json.has("countryCode") -> json.getString("countryCode")
                            else -> null
                        }

                        if (!code.isNullOrBlank() && code.length == 2) {
                            val upperCode = code.uppercase(Locale.ENGLISH)
                            val name = if (json.has("country") && json.getString("country").length > 2) {
                                json.getString("country")
                            } else {
                                Locale("", upperCode).displayCountry.ifBlank { upperCode }
                            }

                            return CountryInfo(
                                countryCode = upperCode,
                                countryName = name,
                                isIndia = upperCode == "IN"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }
        return null
    }

    private fun getDeviceFallbackCountry(): CountryInfo {
        // Telephony network country
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = telephonyManager?.simCountryIso?.takeIf { it.isNotBlank() }
            val networkCountry = telephonyManager?.networkCountryIso?.takeIf { it.isNotBlank() }
            val teleCode = (simCountry ?: networkCountry)?.uppercase(Locale.ENGLISH)
            if (!teleCode.isNullOrBlank() && teleCode.length == 2) {
                return CountryInfo(
                    countryCode = teleCode,
                    countryName = Locale("", teleCode).displayCountry.ifBlank { teleCode },
                    isIndia = teleCode == "IN"
                )
            }
        } catch (_: Exception) {}

        // Default device locale
        val localeCountry = Locale.getDefault().country.uppercase(Locale.ENGLISH)
        val finalCode = if (localeCountry.length == 2) localeCountry else "US"
        return CountryInfo(
            countryCode = finalCode,
            countryName = Locale("", finalCode).displayCountry.ifBlank { finalCode },
            isIndia = finalCode == "IN"
        )
    }
}
