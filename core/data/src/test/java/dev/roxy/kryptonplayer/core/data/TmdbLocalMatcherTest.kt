package dev.roxy.kryptonplayer.core.data

import dev.roxy.kryptonplayer.core.data.repository.TmdbLocalMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbLocalMatcherTest {

    @Test
    fun testFuzzyMatch_matchingScenarios() {
        // Standard Movie Title
        assertTrue(TmdbLocalMatcher.isFuzzyMatch("The Mandalorian", "Mandalorian.S01E01.1080p.WEBRip.x264-PSA.mkv"))
        
        // Single Word Title with minimum length 3
        assertTrue(TmdbLocalMatcher.isFuzzyMatch("Inception", "Inception.2010.1080p.BluRay.mp4"))
        
        // Complex character matching
        assertTrue(TmdbLocalMatcher.isFuzzyMatch("Spirited Away", "Spirited.Away.2001.Japanese.1080p.BluRay.x264.mp4"))
        
        // Title containing dashes/colons
        assertTrue(TmdbLocalMatcher.isFuzzyMatch("Spider-Man: Into the Spider-Verse", "Spider-Man.Into.the.Spider-Verse.2018.1080p.mp4"))
    }

    @Test
    fun testFuzzyMatch_nonMatchingScenarios() {
        // Different movies entirely
        assertFalse(TmdbLocalMatcher.isFuzzyMatch("The Mandalorian", "Inception.2010.mp4"))
        
        // Stop word filtering preventing false positive empty matches
        assertFalse(TmdbLocalMatcher.isFuzzyMatch("The", "SomeOtherMovie.mp4"))
    }

    @Test
    fun testFuzzyMatch_itMovieEdgeCase() {
        // "It" is length 2, filtered by token size >= 3. Stop words prevent it matching everything.
        assertFalse(TmdbLocalMatcher.isFuzzyMatch("It", "Knives.Out.it.srt"))
        assertFalse(TmdbLocalMatcher.isFuzzyMatch("It", "italy.trip.mp4"))
        assertFalse(TmdbLocalMatcher.isFuzzyMatch("It", "it.mp4"))
        
        // But movie titles with length >= 3 are matched correctly
        assertTrue(TmdbLocalMatcher.isFuzzyMatch("Tenet", "Tenet.2020.1080p.mp4"))
    }

    @Test
    fun testFuzzyMatch_alternativeTitles() {
        assertTrue(TmdbLocalMatcher.isFuzzyMatch("The Mandalorian", "Star.Wars.Mandalorian.S01E01.mp4", listOf("Star Wars: The Mandalorian")))
        assertTrue(TmdbLocalMatcher.isFuzzyMatch("Mandalorian", "Mandalore.mp4", listOf("Mandalore")))
        assertFalse(TmdbLocalMatcher.isFuzzyMatch("Mandalorian", "Inception.mp4", listOf("Star Wars")))
    }

    @Test
    fun testEpisodeMatching_scenarios() {
        // Test S01E03 format
        assertTrue(TmdbLocalMatcher.isEpisodeMatch("Breaking Bad", 1, 3, "Breaking.Bad.S01E03.mp4"))
        // Test 1x03 format
        assertTrue(TmdbLocalMatcher.isEpisodeMatch("Breaking Bad", 1, 3, "Breaking.Bad.1x03.mp4"))
        // Test ep03 format (assumes season 1)
        assertTrue(TmdbLocalMatcher.isEpisodeMatch("Breaking Bad", 1, 3, "Breaking.Bad.ep03.mp4"))
        // Non-matching season/episode
        assertFalse(TmdbLocalMatcher.isEpisodeMatch("Breaking Bad", 1, 3, "Breaking.Bad.S01E04.mp4"))
        assertFalse(TmdbLocalMatcher.isEpisodeMatch("Breaking Bad", 1, 3, "Breaking.Bad.S02E03.mp4"))
    }
}
