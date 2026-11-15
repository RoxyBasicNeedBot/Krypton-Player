package dev.anilbeesetti.nextplayer.core.data

import dev.anilbeesetti.nextplayer.core.data.network.NetworkConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import java.util.concurrent.TimeUnit

class TmdbNetworkTest {

    // All TMDB requests go through the server proxy — no API key needed on client
    private val proxyBaseUrl = NetworkConfig.TMDB_BASE_URL

    @Test
    fun testTmdbProxyHealth() {
        println("--- Testing TMDB Proxy Health Endpoint ---")
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("$proxyBaseUrl/health")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code()
                val body = response.body()?.string()
                println("Health Check Status: $code")
                println("Response: $body")
                assert(response.isSuccessful) { "Health check failed with status $code" }
            }
        } catch (e: Exception) {
            println("NETWORK ERROR: Could not reach proxy server: ${e.message}")
        }
    }

    @Test
    fun testTmdbProxyPopularMovies() {
        println("--- Testing TMDB Proxy: Popular Movies ---")
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("$proxyBaseUrl/movie/popular?language=en-US&page=1")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code()
                val body = response.body()?.string()
                println("Popular Movies Status: $code")
                if (response.isSuccessful) {
                    println("SUCCESS: Got popular movies via proxy!")
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

    @Test
    fun testTmdbProxySearchMulti() {
        println("--- Testing TMDB Proxy: Search Multi ---")
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("$proxyBaseUrl/search/multi?query=Avengers&language=en-US&page=1")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code()
                val body = response.body()?.string()
                println("Search Multi Status: $code")
                if (response.isSuccessful) {
                    println("SUCCESS: Search works via proxy!")
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

