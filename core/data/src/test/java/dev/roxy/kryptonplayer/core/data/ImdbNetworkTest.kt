package dev.roxy.kryptonplayer.core.data

import dev.roxy.kryptonplayer.core.data.network.NetworkConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class ImdbNetworkTest {

    private val proxyBaseUrl = NetworkConfig.IMDB_BASE_URL

    private fun stringToId(id: String): Int {
        return id.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    private fun intToTitleId(id: Int): String {
        return "tt" + String.format("%07d", id)
    }

    private fun intToNameId(id: Int): String {
        return "nm" + String.format("%07d", id)
    }

    @Test
    fun testImdbIdMapping() {
        println("--- Testing IMDb ID Conversion Logic ---")
        
        // 7-digit ID
        val ttId1 = "tt0111161"
        val intId1 = stringToId(ttId1)
        assertEquals(111161, intId1)
        assertEquals(ttId1, intToTitleId(intId1))

        // 8-digit ID
        val ttId2 = "tt10293847"
        val intId2 = stringToId(ttId2)
        assertEquals(10293847, intId2)
        assertEquals(ttId2, intToTitleId(intId2))

        // Name ID
        val nmId = "nm0000093"
        val intNm = stringToId(nmId)
        assertEquals(93, intNm)
        assertEquals(nmId, intToNameId(intNm))
    }

    @Test
    fun testImdbProxyHealth() {
        println("--- Testing IMDb Proxy Health Endpoint ---")
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("$proxyBaseUrl/health")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string()
                println("Health Check Status: $code")
                println("Response: $body")
                assert(response.isSuccessful) { "Health check failed with status $code" }
            }
        } catch (e: Exception) {
            println("NETWORK ERROR: Could not reach proxy server: ${e.message}")
        }
    }

    @Test
    fun testImdbProxySearch() {
        println("--- Testing IMDb Proxy Search Endpoint ---")
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("$proxyBaseUrl/search/titles?query=Inception&limit=5")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string()
                println("Search Status: $code")
                if (response.isSuccessful) {
                    println("SUCCESS: Got search results from IMDb API!")
                    println("Response (first 500 chars): ${body?.take(500)}")
                } else {
                    println("FAILURE: Proxy returned status $code")
                    println("Error: $body")
                }
            }
        } catch (e: Exception) {
            println("NETWORK ERROR: ${e.message}")
        }
    }
}
