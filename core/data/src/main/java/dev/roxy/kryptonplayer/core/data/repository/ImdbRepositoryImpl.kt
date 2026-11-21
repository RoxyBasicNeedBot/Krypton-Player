package dev.roxy.kryptonplayer.core.data.repository

import dev.roxy.kryptonplayer.core.data.network.ImdbApi
import dev.roxy.kryptonplayer.core.data.network.NetworkConfig
import dev.roxy.kryptonplayer.core.data.network.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImdbRepositoryImpl @Inject constructor(
    private val imdbApi: ImdbApi
) : ImdbRepository {

    private fun stringToId(id: Any?): Int {
        return gsonSafeString(id).filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    private fun intToTitleId(id: Int): String {
        return "tt" + String.format("%07d", id)
    }

    private fun intToNameId(id: Int): String {
        return "nm" + String.format("%07d", id)
    }

    private fun getProxyImageUrl(url: String): String {
        val prefix = "https://m.media-amazon.com/images/"
        if (url.startsWith(prefix)) {
            val path = url.substring(prefix.length)
            return "${NetworkConfig.IMDB_BASE_URL}image/$path"
        }
        return url
    }

    private fun ImdbTitle.toTmdbMovie(): TmdbMovie {
        return TmdbMovie(
            id = stringToId(id),
            title = primaryTitle ?: originalTitle ?: "",
            poster_path = primaryImage?.url?.let { getProxyImageUrl(it) },
            backdrop_path = null,
            vote_average = rating?.aggregateRating ?: 0.0,
            release_date = startYear?.toString(),
            overview = plot
        )
    }

    private fun ImdbTitle.toTmdbTvShow(): TmdbTvShow {
        return TmdbTvShow(
            id = stringToId(id),
            name = primaryTitle ?: originalTitle ?: "",
            poster_path = primaryImage?.url?.let { getProxyImageUrl(it) },
            backdrop_path = null,
            vote_average = rating?.aggregateRating ?: 0.0,
            first_air_date = startYear?.toString(),
            overview = plot
        )
    }

    override suspend fun getPopularMovies(page: Int): Result<List<TmdbMovie>> {
        return runCatching {
            // Mapping popular titles (sorted by popularity desc)
            val response = imdbApi.listTitles(
                types = listOf("MOVIE"),
                sortBy = "SORT_BY_POPULARITY",
                sortOrder = "DESC",
                pageSize = 20,
                minVotes = 100
            )
            gsonSafeList<ImdbTitle>(response.titles).map { it.toTmdbMovie() }
        }
    }

    override suspend fun getPopularTvShows(page: Int): Result<List<TmdbTvShow>> {
        return runCatching {
            val response = imdbApi.listTitles(
                types = listOf("TV_SERIES"),
                sortBy = "SORT_BY_POPULARITY",
                sortOrder = "DESC",
                pageSize = 20,
                minVotes = 100
            )
            gsonSafeList<ImdbTitle>(response.titles).map { it.toTmdbTvShow() }
        }
    }

    override suspend fun getTrendingMovies(page: Int): Result<List<TmdbMovie>> {
        return runCatching {
            val response = imdbApi.listTitles(
                types = listOf("MOVIE"),
                sortBy = "SORT_BY_POPULARITY",
                sortOrder = "DESC",
                pageSize = 20
            )
            gsonSafeList<ImdbTitle>(response.titles).map { it.toTmdbMovie() }
        }
    }

    override suspend fun getTrendingTvShows(page: Int): Result<List<TmdbTvShow>> {
        return runCatching {
            val response = imdbApi.listTitles(
                types = listOf("TV_SERIES"),
                sortBy = "SORT_BY_POPULARITY",
                sortOrder = "DESC",
                pageSize = 20
            )
            gsonSafeList<ImdbTitle>(response.titles).map { it.toTmdbTvShow() }
        }
    }

    override suspend fun getPopularPeople(page: Int): Result<List<TmdbPerson>> {
        return runCatching {
            // IMDb does not have a general people list endpoint easily, but we can query top star meter ranks
            val response = imdbApi.listTitles(
                types = listOf("MOVIE"),
                sortBy = "SORT_BY_POPULARITY",
                sortOrder = "DESC",
                pageSize = 10
            )
            // Extract unique stars from popular movies as a proxy for popular people
            gsonSafeList<ImdbTitle>(response.titles)
                .flatMap { gsonSafeList<ImdbName>(it.stars) }
                .distinctBy { gsonSafeString(it.id) }
                .map { name ->
                    TmdbPerson(
                        id = stringToId(name.id),
                        name = name.displayName ?: "",
                        profile_path = name.primaryImage?.url?.let { getProxyImageUrl(it) },
                        known_for_department = "Actor"
                    )
                }
        }
    }

    override suspend fun discoverMovies(genreId: String, page: Int): Result<List<TmdbMovie>> {
        return runCatching {
            val response = imdbApi.listTitles(
                types = listOf("MOVIE"),
                genres = listOf(genreId),
                sortBy = "SORT_BY_POPULARITY",
                sortOrder = "DESC",
                pageSize = 20,
                minVotes = 100
            )
            gsonSafeList<ImdbTitle>(response.titles).map { it.toTmdbMovie() }
        }
    }

    override suspend fun searchMulti(query: String, page: Int): Result<List<TmdbSearchResult>> {
        return runCatching {
            val response = imdbApi.searchTitles(query = query, limit = 20)
            gsonSafeList<ImdbTitle>(response.titles).map { title ->
                if (title.type == "TV_SERIES" || title.type == "TV_MINI_SERIES") {
                    TmdbSearchResult.TvShow(
                        id = stringToId(title.id),
                        name = title.primaryTitle ?: title.originalTitle ?: "",
                        poster_path = title.primaryImage?.url?.let { getProxyImageUrl(it) },
                        vote_average = title.rating?.aggregateRating ?: 0.0,
                        first_air_date = title.startYear?.toString(),
                        overview = title.plot
                    )
                } else {
                    TmdbSearchResult.Movie(
                        id = stringToId(title.id),
                        title = title.primaryTitle ?: title.originalTitle ?: "",
                        poster_path = title.primaryImage?.url?.let { getProxyImageUrl(it) },
                        vote_average = title.rating?.aggregateRating ?: 0.0,
                        release_date = title.startYear?.toString(),
                        overview = title.plot
                    )
                }
            }
        }
    }

    override suspend fun getMovieDetails(id: Int): Result<TmdbMovieDetails> {
        return runCatching {
            val titleId = intToTitleId(id)
            val title = imdbApi.getTitle(titleId)
            
            // Get credits
            val creditsResult = runCatching { imdbApi.getCredits(titleId) }.getOrNull()
            val safeCredits = gsonSafeList<ImdbCredit>(creditsResult?.credits)
            val cast = safeCredits.filter { it.category == "actor" || it.category == "actress" }.map { credit ->
                TmdbCastMember(
                    id = stringToId(credit.name?.id),
                    name = credit.name?.displayName ?: "",
                    character = credit.characters?.firstOrNull() ?: "",
                    profile_path = credit.name?.primaryImage?.url?.let { getProxyImageUrl(it) }
                )
            }
            
            val crew = safeCredits.filter { it.category == "director" || it.category == "writer" }.map { credit ->
                TmdbCrewMember(
                    id = stringToId(credit.name?.id),
                    name = credit.name?.displayName ?: "",
                    job = credit.category,
                    department = credit.category,
                    profile_path = credit.name?.primaryImage?.url?.let { getProxyImageUrl(it) }
                )
            }

            // Get AKAs
            val akasResult = runCatching { imdbApi.getAKAs(titleId) }.getOrNull()
            val altTitles = gsonSafeList<ImdbAKA>(akasResult?.akas).map { aka ->
                TmdbAlternativeTitleItem(
                    iso_3166_1 = gsonSafeString(aka.country?.code, "US"),
                    title = gsonSafeString(aka.text),
                    type = aka.attributes?.firstOrNull()
                )
            }

            TmdbMovieDetails(
                id = stringToId(title.id),
                title = title.primaryTitle ?: title.originalTitle ?: "",
                overview = title.plot,
                poster_path = title.primaryImage?.url?.let { getProxyImageUrl(it) },
                backdrop_path = title.primaryImage?.url?.let { getProxyImageUrl(it) },
                vote_average = title.rating?.aggregateRating ?: 0.0,
                release_date = title.startYear?.toString(),
                runtime = title.runtimeSeconds?.let { it / 60 },
                genres = title.genres?.mapIndexed { index, genre -> TmdbGenre(index, gsonSafeString(genre)) },
                credits = TmdbCredits(cast, crew),
                videos = null,
                images = TmdbImageResponse(
                    backdrops = title.primaryImage?.url?.let { listOf(TmdbImage(getProxyImageUrl(it), title.primaryImage.width ?: 0, title.primaryImage.height ?: 0)) },
                    posters = title.primaryImage?.url?.let { listOf(TmdbImage(getProxyImageUrl(it), title.primaryImage.width ?: 0, title.primaryImage.height ?: 0)) }
                ),
                release_dates = null,
                belongs_to_collection = null,
                production_companies = null,
                similar = null,
                recommendations = null,
                alternative_titles = TmdbAlternativeTitlesResponse(titles = altTitles)
            ).sanitized()
        }
    }

    override suspend fun getTvShowDetails(id: Int): Result<TmdbTvShowDetails> {
        return runCatching {
            val titleId = intToTitleId(id)
            val title = imdbApi.getTitle(titleId)
            
            // Get credits
            val creditsResult = runCatching { imdbApi.getCredits(titleId) }.getOrNull()
            val safeCredits = gsonSafeList<ImdbCredit>(creditsResult?.credits)
            val cast = safeCredits.filter { it.category == "actor" || it.category == "actress" }.map { credit ->
                TmdbCastMember(
                    id = stringToId(credit.name?.id),
                    name = credit.name?.displayName ?: "",
                    character = credit.characters?.firstOrNull() ?: "",
                    profile_path = credit.name?.primaryImage?.url?.let { getProxyImageUrl(it) }
                )
            }
            
            val crew = safeCredits.filter { it.category == "director" || it.category == "writer" }.map { credit ->
                TmdbCrewMember(
                    id = stringToId(credit.name?.id),
                    name = credit.name?.displayName ?: "",
                    job = credit.category,
                    department = credit.category,
                    profile_path = credit.name?.primaryImage?.url?.let { getProxyImageUrl(it) }
                )
            }

            // Get AKAs
            val akasResult = runCatching { imdbApi.getAKAs(titleId) }.getOrNull()
            val altTitles = gsonSafeList<ImdbAKA>(akasResult?.akas).map { aka ->
                TmdbAlternativeTitleItem(
                    iso_3166_1 = gsonSafeString(aka.country?.code, "US"),
                    title = gsonSafeString(aka.text),
                    type = aka.attributes?.firstOrNull()
                )
            }

            // Get Seasons
            val seasonsResult = runCatching { imdbApi.getSeasons(titleId) }.getOrNull()
            val seasonsBrief = gsonSafeList<ImdbSeason>(seasonsResult?.seasons).mapIndexed { index, season ->
                val seasonStr = gsonSafeString(season.season, (index + 1).toString())
                TmdbTvSeasonBrief(
                    id = index + 1,
                    name = "Season $seasonStr",
                    season_number = seasonStr.toIntOrNull() ?: (index + 1),
                    episode_count = season.episodeCount,
                    poster_path = title.primaryImage?.url?.let { getProxyImageUrl(it) },
                    air_date = null
                )
            }

            TmdbTvShowDetails(
                id = stringToId(title.id),
                name = title.primaryTitle ?: title.originalTitle ?: "",
                overview = title.plot,
                poster_path = title.primaryImage?.url?.let { getProxyImageUrl(it) },
                backdrop_path = title.primaryImage?.url?.let { getProxyImageUrl(it) },
                vote_average = title.rating?.aggregateRating ?: 0.0,
                first_air_date = title.startYear?.toString(),
                episode_run_time = title.runtimeSeconds?.let { listOf(it / 60) },
                genres = title.genres?.mapIndexed { index, genre -> TmdbGenre(index, gsonSafeString(genre)) },
                credits = TmdbCredits(cast, crew),
                videos = null,
                images = TmdbImageResponse(
                    backdrops = title.primaryImage?.url?.let { listOf(TmdbImage(getProxyImageUrl(it), title.primaryImage.width ?: 0, title.primaryImage.height ?: 0)) },
                    posters = title.primaryImage?.url?.let { listOf(TmdbImage(getProxyImageUrl(it), title.primaryImage.width ?: 0, title.primaryImage.height ?: 0)) }
                ),
                content_ratings = null,
                production_companies = null,
                networks = null,
                similar = null,
                recommendations = null,
                seasons = seasonsBrief,
                alternative_titles = TmdbAlternativeTitlesResponse(titles = altTitles)
            ).sanitized()
        }
    }

    override suspend fun getPersonDetails(id: Int): Result<TmdbPersonDetails> {
        return runCatching {
            val nameId = intToNameId(id)
            val name = imdbApi.getName(nameId)
            
            // Get filmography
            val filmographyResult = runCatching { imdbApi.getFilmography(nameId) }.getOrNull()
            val safeFilmography = gsonSafeList<ImdbCredit>(filmographyResult?.credits)
            val movieCredits = safeFilmography.filter { it.title?.type == "MOVIE" }.map { credit ->
                TmdbMovie(
                    id = stringToId(credit.title?.id),
                    title = credit.title?.primaryTitle ?: credit.title?.originalTitle ?: "",
                    poster_path = credit.title?.primaryImage?.url?.let { getProxyImageUrl(it) },
                    backdrop_path = null,
                    vote_average = credit.title?.rating?.aggregateRating ?: 0.0,
                    release_date = credit.title?.startYear?.toString(),
                    overview = credit.title?.plot
                )
            }

            val tvCredits = safeFilmography.filter { it.title?.type == "TV_SERIES" || it.title?.type == "TV_MINI_SERIES" }.map { credit ->
                TmdbTvShow(
                    id = stringToId(credit.title?.id),
                    name = credit.title?.primaryTitle ?: credit.title?.originalTitle ?: "",
                    poster_path = credit.title?.primaryImage?.url?.let { getProxyImageUrl(it) },
                    backdrop_path = null,
                    vote_average = credit.title?.rating?.aggregateRating ?: 0.0,
                    first_air_date = credit.title?.startYear?.toString(),
                    overview = credit.title?.plot
                )
            }

            TmdbPersonDetails(
                id = stringToId(name.id),
                name = name.displayName ?: "",
                biography = name.biography,
                profile_path = name.primaryImage?.url?.let { getProxyImageUrl(it) },
                birthday = name.birthDate?.toReleaseDateString(),
                place_of_birth = name.birthLocation,
                movie_credits = TmdbPersonMovieCredits(movieCredits),
                tv_credits = TmdbPersonTvCredits(tvCredits)
            ).sanitized()
        }
    }

    override suspend fun getTvSeasonDetails(tvId: Int, seasonNumber: Int): Result<TmdbTvSeasonDetails> {
        return runCatching {
            val titleId = intToTitleId(tvId)
            val response = imdbApi.getEpisodes(titleId, season = seasonNumber.toString())
            
            val mappedEpisodes = gsonSafeList<ImdbEpisode>(response.episodes).map { episode ->
                TmdbEpisode(
                    id = stringToId(episode.id),
                    name = episode.title ?: "",
                    overview = episode.plot,
                    episode_number = episode.episodeNumber ?: 0,
                    season_number = seasonNumber,
                    still_path = episode.primaryImage?.url?.let { getProxyImageUrl(it) },
                    air_date = episode.releaseDate?.toReleaseDateString(),
                    vote_average = episode.rating?.aggregateRating ?: 0.0
                )
            }
            TmdbTvSeasonDetails(
                id = seasonNumber,
                name = "Season $seasonNumber",
                overview = null,
                season_number = seasonNumber,
                poster_path = null,
                episodes = mappedEpisodes
            ).sanitized()
        }
    }

    override suspend fun getUpcomingMovies(page: Int): Result<List<TmdbMovie>> {
        return runCatching {
            val response = imdbApi.listTitles(
                types = listOf("MOVIE"),
                sortBy = "SORT_BY_RELEASE_DATE",
                sortOrder = "DESC",
                pageSize = 20
            )
            gsonSafeList<ImdbTitle>(response.titles).map { it.toTmdbMovie() }
        }
    }

    override suspend fun getTopRatedMovies(page: Int): Result<List<TmdbMovie>> {
        return runCatching {
            val response = imdbApi.listTitles(
                types = listOf("MOVIE"),
                sortBy = "SORT_BY_USER_RATING",
                sortOrder = "DESC",
                minVotes = 1000,
                pageSize = 20
            )
            gsonSafeList<ImdbTitle>(response.titles).map { it.toTmdbMovie() }
        }
    }

    override suspend fun getTopRatedTvShows(page: Int): Result<List<TmdbTvShow>> {
        return runCatching {
            val response = imdbApi.listTitles(
                types = listOf("TV_SERIES"),
                sortBy = "SORT_BY_USER_RATING",
                sortOrder = "DESC",
                minVotes = 500,
                pageSize = 20
            )
            gsonSafeList<ImdbTitle>(response.titles).map { it.toTmdbTvShow() }
        }
    }

    // IMDb-specific implementations

    override suspend fun getParentsGuide(id: Int): Result<ImdbParentsGuideResponse> {
        return runCatching {
            imdbApi.getParentsGuide(intToTitleId(id))
        }
    }

    override suspend fun getBoxOffice(id: Int): Result<ImdbBoxOffice> {
        return runCatching {
            imdbApi.getBoxOffice(intToTitleId(id))
        }
    }

    override suspend fun getCertificates(id: Int): Result<List<ImdbCertificate>> {
        return runCatching {
            imdbApi.getCertificates(intToTitleId(id)).certificates ?: emptyList()
        }
    }

    override suspend fun getTrivia(personId: Int): Result<List<ImdbTriviaItem>> {
        return runCatching {
            imdbApi.getTrivia(intToNameId(personId)).trivia ?: emptyList()
        }
    }

    override suspend fun getRelationships(personId: Int): Result<List<ImdbRelationshipItem>> {
        return runCatching {
            imdbApi.getRelationships(intToNameId(personId)).relationships ?: emptyList()
        }
    }
}
