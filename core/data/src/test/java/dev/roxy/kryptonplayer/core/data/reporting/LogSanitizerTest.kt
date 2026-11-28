package dev.roxy.kryptonplayer.core.data.reporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {

    @Test
    fun `redacts Authorization headers and Bearer tokens`() {
        val authHeader = "Authorization: Basic dXNlcjpwYXNzMTIz"
        assertEquals("Authorization: [REDACTED]", LogSanitizer.sanitize(authHeader))

        val bearerLine = "Calling API with Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig_123=="
        assertEquals(
            "Calling API with Bearer [REDACTED_BEARER_TOKEN]",
            LogSanitizer.sanitize(bearerLine),
        )
    }

    @Test
    fun `redacts Cookie, X-Agni-Builder-Token, X-Api-Key, and Client-Key headers`() {
        assertEquals(
            "Cookie: [REDACTED]",
            LogSanitizer.sanitize("Cookie: session=abc123; theme=dark"),
        )
        assertEquals(
            "set-cookie: [REDACTED]",
            LogSanitizer.sanitize("set-cookie: sid=xyz789; HttpOnly"),
        )
        assertEquals(
            "X-Agni-Builder-Token: [REDACTED]",
            LogSanitizer.sanitize("X-Agni-Builder-Token: super_secret_builder_val"),
        )
        assertEquals(
            "X-Api-Key: [REDACTED]",
            LogSanitizer.sanitize("X-Api-Key: api_secret_value_99"),
        )
        assertEquals(
            "Client-Key: [REDACTED]",
            LogSanitizer.sanitize("Client-Key: ck_live_987654321"),
        )
    }

    @Test
    fun `redacts GitHub classic and fine-grained PAT tokens`() {
        val classicPrefixes = listOf("ghp_", "gho_", "ghu_", "ghs_", "ghr_")
        for (prefix in classicPrefixes) {
            val token = prefix + "A1b2C3d4E5f6".repeat(3)
            val result = LogSanitizer.sanitize("Failed auth with token $token in worker")
            assertEquals("Failed auth with token [REDACTED_GITHUB_TOKEN] in worker", result)
            assertFalse(result.contains(token))
        }

        val fineGrained = "github_pat_" + "1234567890_AbCdE".repeat(5) + "XY"
        val fgResult = LogSanitizer.sanitize("Leak: $fineGrained")
        assertEquals("Leak: [REDACTED_GITHUB_TOKEN]", fgResult)
        assertFalse(fgResult.contains("1234567890_AbCdE"))
    }

    @Test
    fun `redacts Google and Gemini API keys`() {
        val fakeGoogleKey = "AIza" + "SyDummy" + "1234567890_abcdefghijklmnopq"
        assertEquals(39, fakeGoogleKey.length)

        val result = LogSanitizer.sanitize("Gemini request failed using $fakeGoogleKey")
        assertEquals("Gemini request failed using [REDACTED_API_KEY]", result)
        assertFalse(result.contains("SyDummy"))
    }

    @Test
    fun `redacts Telegram bot tokens and auth hashes`() {
        val fakeBotToken = "bot" + "123456789" + ":" + "AbCdEfGhIjKlMnOpQrStUvWxYz123456789"
        val botResult = LogSanitizer.sanitize("GET https://api.telegram.org/$fakeBotToken/getUpdates")
        assertEquals(
            "GET https://api.telegram.org/bot[REDACTED_BOT_TOKEN]/getUpdates",
            botResult,
        )

        val hashLine = "auth_key_id=1234567890abcdef1234 session_key=abcdef1234567890abcd api_hash=0123456789abcdef0123"
        assertEquals(
            "auth_key_id=[REDACTED] session_key=[REDACTED] api_hash=[REDACTED]",
            LogSanitizer.sanitize(hashLine),
        )
    }

    @Test
    fun `redacts embedded basic-auth credentials in URLs while preserving host and path`() {
        val httpUrl = "Opening stream https://alice:s3cr3tPass@cdn.example.com:8443/live/movie.m3u8"
        assertEquals(
            "Opening stream https://[REDACTED_CREDS]@cdn.example.com:8443/live/movie.m3u8",
            LogSanitizer.sanitize(httpUrl),
        )

        val rtspUrl = "rtsp://admin:pass123@192.168.1.25:554/stream1"
        assertEquals(
            "rtsp://[REDACTED_CREDS]@192.168.1.25:554/stream1",
            LogSanitizer.sanitize(rtspUrl),
        )
    }

    @Test
    fun `redacts sensitive query parameters and JSON keys while leaving safe fields intact`() {
        val queryUrl = "https://cdn.example.com/video.mkv?quality=1080p&token=abc123&sig=xyz999&lang=en"
        assertEquals(
            "https://cdn.example.com/video.mkv?quality=1080p&token=[REDACTED]&sig=[REDACTED]&lang=en",
            LogSanitizer.sanitize(queryUrl),
        )

        val jsonLine = """{"codec":"hevc","apiKey":"secretValue123","password":"myPassword","fps":60}"""
        assertEquals(
            """{"codec":"hevc","apiKey":"[REDACTED]","password":"[REDACTED]","fps":60}""",
            LogSanitizer.sanitize(jsonLine),
        )
    }

    @Test
    fun `redacts email addresses`() {
        val line = "Account signed in as user.name+test@sub.example.com for sync"
        assertEquals("Account signed in as [REDACTED_EMAIL] for sync", LogSanitizer.sanitize(line))
    }

    @Test
    fun `sanitizeAll scrubs multi-line logs and preserves blank lines`() {
        val fakeGithub = "ghp_" + "XyZ987654321".repeat(3)
        val multiLine = """
            04-12 14:22:05.100 I/mpv( 1234): Opening https://user:pass@media.example.org/v.mp4?token=secret
            
            04-12 14:22:05.200 D/Network( 1234): Cookie: sid=secretCookie
            04-12 14:22:05.300 E/Relay( 1234): Token $fakeGithub used by tester@example.org
        """.trimIndent()

        val sanitized = LogSanitizer.sanitizeAll(multiLine)
        val lines = sanitized.lines()
        assertEquals(4, lines.size)
        assertEquals(
            "04-12 14:22:05.100 I/mpv( 1234): Opening https://[REDACTED_CREDS]@media.example.org/v.mp4?token=[REDACTED]",
            lines[0],
        )
        assertEquals("", lines[1])
        assertEquals("04-12 14:22:05.200 D/Network( 1234): Cookie: [REDACTED]", lines[2])
        assertEquals(
            "04-12 14:22:05.300 E/Relay( 1234): Token [REDACTED_GITHUB_TOKEN] used by [REDACTED_EMAIL]",
            lines[3],
        )
    }

    @Test
    fun `preserves normal Android crash stack traces and mpv playback logs`() {
        val normalLogs = listOf(
            "04-12 14:22:05.123 E/AndroidRuntime( 1234): FATAL EXCEPTION: main",
            "04-12 14:22:05.124 E/AndroidRuntime( 1234): java.lang.IllegalStateException: Player error at dev.roxy.kryptonplayer.Player.play(Player.kt:42)",
            "04-12 14:22:05.125 I/mpv( 1234): [ffmpeg/video] h264: Reinit context to 1920x1080, pix_fmt: yuv420p",
            "04-12 14:22:05.126 D/mpv( 1234): [ao/audiotrack] format: 48000Hz stereo 2ch float",
            "04-12 14:22:05.127 D/MediaCodec( 1234): Decoder c2.qti.hevc.decoder@1.0 initialized (1920x1080 @ 23.976fps)",
            "04-12 14:22:05.128 D/Activity( 1234): Resumed MainActivity@4a2b1c3",
            "04-12 14:22:05.129 I/Stream( 1234): Playing https://cdn.example.com/movies/sample_1080p.mkv?quality=1080p&lang=en",
        )

        for (line in normalLogs) {
            assertEquals("Expected normal log line to stay unmodified", line, LogSanitizer.sanitize(line))
        }
        assertTrue(LogSanitizer.sanitize("   ").isBlank())
    }
}
