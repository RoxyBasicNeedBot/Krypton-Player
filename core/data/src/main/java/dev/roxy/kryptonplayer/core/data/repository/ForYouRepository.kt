package dev.roxy.kryptonplayer.core.data.repository

import dev.roxy.kryptonplayer.core.model.ForYouFeaturedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ForYouRepository {
    suspend fun getForYouFeaturedItems(): Result<List<ForYouFeaturedItem>>
}

@Singleton
class ForYouRepositoryImpl @Inject constructor(
    private val tmdbRepository: TmdbRepository,
    private val countryDetector: CountryDetector
) : ForYouRepository {

    private var cachedItems: List<ForYouFeaturedItem> = emptyList()

    override suspend fun getForYouFeaturedItems(): Result<List<ForYouFeaturedItem>> = withContext(Dispatchers.IO) {
        try {
            val country = countryDetector.detectCountry()
            val list = mutableListOf<ForYouFeaturedItem>()

            // 1. Fetch TMDB Trending movies
            val trending = tmdbRepository.getTrendingMovies(1).getOrNull()
            if (!trending.isNullOrEmpty()) {
                trending.take(6).forEachIndexed { idx, movie ->
                    val backdrop = movie.backdrop_path?.let { "https://image.tmdb.org/t/p/original$it" }
                        ?: movie.poster_path?.let { "https://image.tmdb.org/t/p/original$it" }

                    if (!backdrop.isNullOrBlank()) {
                        val title = movie.title
                        val match = 90 + ((idx * 3) % 9)
                        val year = movie.release_date?.take(4) ?: "2026"
                        val vote = movie.vote_average.toFloat()

                        list.add(
                            ForYouFeaturedItem(
                                id = "foryou_${movie.id}",
                                title = title,
                                tagline = "Trending #1 in ${country.countryName}",
                                overview = movie.overview?.takeIf { it.isNotBlank() }
                                    ?: "An explosive, critically-acclaimed cinematic spectacle featuring high-stakes storytelling and peerless visuals.",
                                backdropUrl = backdrop,
                                posterUrl = movie.poster_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                                rating = vote,
                                matchScore = match,
                                releaseYear = year,
                                ageRating = if (movie.vote_average >= 8.0) "U/A 16+" else "U",
                                qualityBadges = listOf("4K UHD", "Dolby Vision", "Dolby Atmos 5.1"),
                                genres = listOf("Action", "Drama", "Thriller"),
                                isHotstarSpecial = true
                            )
                        )
                    }
                }
            }

            // 2. Fallback / Curated picks if network response is insufficient
            if (list.size < 4) {
                list.addAll(getCuratedForYouFallback(country))
            }

            val result = list.distinctBy { it.id }.take(6)
            cachedItems = result
            Result.success(result)
        } catch (e: Exception) {
            val fallback = getCuratedForYouFallback(countryDetector.detectCountry())
            cachedItems = fallback
            Result.success(fallback)
        }
    }

    private fun getCuratedForYouFallback(country: CountryInfo): List<ForYouFeaturedItem> {
        return if (country.isIndia) {
            listOf(
                ForYouFeaturedItem(
                    id = "curated_foryou_in_1",
                    title = "King: The Rule of Shadows",
                    tagline = "Shah Rukh Khan & Suhana Khan in High-Stakes Espionage",
                    overview = "A seasoned assassin is forced out of retirement to protect a young operative across the underworld of Europe in a battle against impossible odds.",
                    backdropUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1920&q=85",
                    rating = 9.2f,
                    matchScore = 99,
                    releaseYear = "2026",
                    ageRating = "U/A 16+",
                    qualityBadges = listOf("4K UHD", "IMAX Enhanced", "Dolby Atmos"),
                    genres = listOf("Action", "Spy Thriller", "Crime"),
                    isHotstarSpecial = true
                ),
                ForYouFeaturedItem(
                    id = "curated_foryou_in_2",
                    title = "Jailer 2: The Return of Muthuvel",
                    tagline = "Superstar Rajinikanth Unleashes Unstoppable Fury",
                    overview = "Tiger Muthuvel Pandian unearths a global criminal syndicate that threatens national security, igniting an explosive tactical warfare.",
                    backdropUrl = "https://images.unsplash.com/photo-1578328819058-b69f3a3b0f6b?w=1920&q=85",
                    rating = 9.0f,
                    matchScore = 98,
                    releaseYear = "2026",
                    ageRating = "U/A 16+",
                    qualityBadges = listOf("4K UHD", "Dolby Vision", "5.1 Surround"),
                    genres = listOf("Action", "Revenge Drama", "Mass"),
                    isHotstarSpecial = true
                ),
                ForYouFeaturedItem(
                    id = "curated_foryou_in_3",
                    title = "Ramayana: The Epic Beginning",
                    tagline = "Ranbir Kapoor & Yash in Indian Cinema's Grandest Saga",
                    overview = "A timeless epic reimagined on a monumental scale, chronicling virtue, destiny, and the legendary clash between divine righteousness and cosmic darkness.",
                    backdropUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1920&q=85",
                    rating = 9.4f,
                    matchScore = 97,
                    releaseYear = "2026",
                    ageRating = "U",
                    qualityBadges = listOf("4K UHD", "Dolby Atmos", "HDR10+"),
                    genres = listOf("Mythological", "Epic", "Adventure"),
                    isHotstarSpecial = true
                )
            )
        } else {
            listOf(
                ForYouFeaturedItem(
                    id = "curated_foryou_global_1",
                    title = "Dune: Messiah - The Golden Path",
                    tagline = "Denis Villeneuve's Definitive Sci-Fi Masterpiece",
                    overview = "Paul Atreides faces the unintended cosmic jihad waged across thousands of worlds in his name, confronting his fate on the desert planet of Arrakis.",
                    backdropUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1920&q=85",
                    rating = 9.1f,
                    matchScore = 99,
                    releaseYear = "2026",
                    ageRating = "PG-13",
                    qualityBadges = listOf("4K UHD", "IMAX 1.43:1", "Dolby Atmos"),
                    genres = listOf("Sci-Fi", "Adventure", "Drama"),
                    isHotstarSpecial = true
                ),
                ForYouFeaturedItem(
                    id = "curated_foryou_global_2",
                    title = "Avengers: Secret Wars",
                    tagline = "The Multiverse Collapses in the Ultimate Marvel Climax",
                    overview = "Heroes across divergent timelines stand unified at the edge of existence to defend reality against destruction in a battle for all time.",
                    backdropUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1920&q=85",
                    rating = 8.9f,
                    matchScore = 96,
                    releaseYear = "2026",
                    ageRating = "PG-13",
                    qualityBadges = listOf("4K UHD", "Dolby Vision", "Dolby Atmos"),
                    genres = listOf("Superhero", "Action", "Sci-Fi"),
                    isHotstarSpecial = true
                )
            )
        }
    }
}
