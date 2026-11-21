package dev.roxy.kryptonplayer.feature.videopicker.screens.mediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbExploreItemTest {

    @Test
    fun testUniqueKeyGeneration() {
        val movie = ExploreItem.MovieItem(1248832, "The Wild Robot", null, 8.5, "2024-09-12")
        val tvShow = ExploreItem.TvShowItem(1248832, "Some TV Show", null, 8.0, "2024-01-01")
        val actor = ExploreItem.ActorItem(1248832, "Actor Name", null, "Acting")

        assertEquals("tmdb_movie_1248832", movie.uniqueKey)
        assertEquals("tmdb_tv_1248832", tvShow.uniqueKey)
        assertEquals("tmdb_actor_1248832", actor.uniqueKey)
    }

    @Test
    fun testDuplicateKeysPreventionWithDistinctBy() {
        val movie1 = ExploreItem.MovieItem(1248832, "The Wild Robot", null, 8.5, "2024-09-12")
        val movie2Duplicate = ExploreItem.MovieItem(1248832, "The Wild Robot (Updated)", null, 8.6, "2024-09-12")
        val movie3 = ExploreItem.MovieItem(999999, "Other Movie", null, 7.0, "2024-05-01")

        val listWithDuplicates = listOf(movie1, movie2Duplicate, movie3)
        val deduplicated = listWithDuplicates.distinctBy { it.uniqueKey }

        assertEquals(2, deduplicated.size)
        assertEquals(movie1.id, deduplicated[0].id)
        assertEquals(movie3.id, deduplicated[1].id)

        val keys = deduplicated.map { it.uniqueKey }
        assertEquals(keys.toSet().size, keys.size)
    }

    @Test
    fun testPaginationDeduplicationPreservesUniqueKeys() {
        val page1 = listOf(
            ExploreItem.MovieItem(1001, "Movie 1", null, 7.5, "2024-01-01"),
            ExploreItem.MovieItem(1248832, "The Wild Robot", null, 8.5, "2024-09-12")
        )

        // TMDb page 2 returns overlapping movie 1248832 plus a new movie 1002
        val page2 = listOf(
            ExploreItem.MovieItem(1248832, "The Wild Robot", null, 8.5, "2024-09-12"),
            ExploreItem.MovieItem(1002, "Movie 2", null, 6.8, "2024-02-01")
        )

        val existingKeys = page1.map { it.uniqueKey }.toSet()
        val uniqueNewItems = page2.filterNot { it.uniqueKey in existingKeys }.distinctBy { it.uniqueKey }
        val combined = page1 + uniqueNewItems

        assertEquals(3, combined.size)
        assertEquals(listOf(1001, 1248832, 1002), combined.map { it.id })

        val uniqueKeys = combined.map { it.uniqueKey }
        assertEquals(uniqueKeys.toSet().size, uniqueKeys.size)
    }
}
