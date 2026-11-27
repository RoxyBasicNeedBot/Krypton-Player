package dev.roxy.kryptonplayer.core.agni

import org.junit.Assert.assertEquals
import org.junit.Test

class AgniCoreEngineTest {

    @Test
    fun normalizeTitle_stripsTagsAndSeparators() {
        val cleaned = AgniCoreEngine.normalizeTitle("[SubsPlease] The.Wild.Robot.2024.(1080p).mkv")
        assertEquals("the wild robot 2024 mkv", cleaned)
    }

    @Test
    fun scoreCandidate_ranksExactAbovePartial() {
        val exact = AgniCoreEngine.scoreCandidate("Interstellar", "Interstellar (2014)")
        val prefix = AgniCoreEngine.scoreCandidate("Interstellar", "Interstellar IMAX Edition")
        val partial = AgniCoreEngine.scoreCandidate("Interstellar Space", "Journey Beyond Space")
        assertEquals(1000, exact)
        assertEquals(850, prefix)
        assertEquals(300, partial)
    }

    @Test
    fun sanitizeLog_redactsBearerToken() {
        val raw = "GET /api header Authorization: Bearer super_secret_token_12345, retry=1"
        val clean = AgniCoreEngine.sanitizeLog(raw)
        assertEquals("GET /api header Authorization: Bearer [REDACTED], retry=1", clean)
    }
}
