package dev.roxy.kryptonplayer.core.data

import dev.roxy.kryptonplayer.core.data.update.VersionComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun testCleanVersionString() {
        assertEquals("0.17.5", VersionComparator.cleanVersionString("v0.17.5"))
        assertEquals("0.17.5", VersionComparator.cleanVersionString("V0.17.5"))
        assertEquals("1.0.0", VersionComparator.cleanVersionString("release-1.0.0"))
        assertEquals("3.0.9", VersionComparator.cleanVersionString("KryptonPlayer-3.0.9"))
        assertEquals("0.16.3", VersionComparator.cleanVersionString("  0.16.3  "))
    }

    @Test
    fun testParseSegments() {
        assertEquals(listOf(0, 17, 5), VersionComparator.parseSegments("0.17.5"))
        assertEquals(listOf(0, 17, 5), VersionComparator.parseSegments("v0.17.5-beta.1"))
        assertEquals(listOf(0, 17, 5), VersionComparator.parseSegments("v0.17.5+build.56"))
        assertEquals(listOf(3, 0, 9), VersionComparator.parseSegments("v3.0.9"))
        assertEquals(listOf(0, 16, 3, 1), VersionComparator.parseSegments("0.16.3.1"))
    }

    @Test
    fun testCompare() {
        // Newer minor version
        assertTrue(VersionComparator.compare("0.17.5", "0.16.3") > 0)
        assertTrue(VersionComparator.compare("v0.17.5", "0.16.3") > 0)

        // Older minor version
        assertTrue(VersionComparator.compare("0.16.3", "0.17.5") < 0)

        // Newer patch version
        assertTrue(VersionComparator.compare("0.16.4", "0.16.3") > 0)

        // Newer major version
        assertTrue(VersionComparator.compare("1.0.0", "0.17.5") > 0)
        assertTrue(VersionComparator.compare("v3.0.9", "0.16.3") > 0)

        // Equal versions
        assertEquals(0, VersionComparator.compare("0.16.3", "0.16.3"))
        assertEquals(0, VersionComparator.compare("v0.16.3", "0.16.3"))
        assertEquals(0, VersionComparator.compare("0.16.3.0", "0.16.3"))

        // Sub-patch
        assertTrue(VersionComparator.compare("0.16.3.1", "0.16.3") > 0)
        assertTrue(VersionComparator.compare("0.16.3", "0.16.3.1") < 0)
    }

    @Test
    fun testIsNewer() {
        assertTrue(VersionComparator.isNewer("0.17.5", "0.16.3"))
        assertTrue(VersionComparator.isNewer("v0.17.5", "0.16.3"))
        assertTrue(VersionComparator.isNewer("v3.0.9", "0.16.3"))
        assertFalse(VersionComparator.isNewer("0.16.3", "0.16.3"))
        assertFalse(VersionComparator.isNewer("0.16.2", "0.16.3"))
        assertFalse(VersionComparator.isNewer("", "0.16.3"))
    }

    @Test
    fun testIsUpdateAvailable_versionCodeScenarios() {
        // Scenario 1: Strictly higher versionCode is an update
        assertTrue(
            VersionComparator.isUpdateAvailable(
                remoteVersionCode = 57,
                currentVersionCode = 56,
                remoteVersionName = "0.16.4",
                currentVersionName = "0.16.3"
            )
        )

        // Scenario 2: Equal versionCode but higher semantic versionName (e.g. worker didn't bump code)
        assertTrue(
            VersionComparator.isUpdateAvailable(
                remoteVersionCode = 56,
                currentVersionCode = 56,
                remoteVersionName = "0.17.5",
                currentVersionName = "0.16.3"
            )
        )

        // Scenario 3: GitHub release with versionCode 0, but higher semantic versionName
        assertTrue(
            VersionComparator.isUpdateAvailable(
                remoteVersionCode = 0,
                currentVersionCode = 56,
                remoteVersionName = "v0.17.5",
                currentVersionName = "0.16.3"
            )
        )

        // Scenario 4: Same version - no update
        assertFalse(
            VersionComparator.isUpdateAvailable(
                remoteVersionCode = 56,
                currentVersionCode = 56,
                remoteVersionName = "0.16.3",
                currentVersionName = "0.16.3"
            )
        )

        // Scenario 5: Older version - no update
        assertFalse(
            VersionComparator.isUpdateAvailable(
                remoteVersionCode = 55,
                currentVersionCode = 56,
                remoteVersionName = "0.16.2",
                currentVersionName = "0.16.3"
            )
        )
    }
}
