package dev.roxy.kryptonplayer.core.data.network.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class TmdbResponse<T>(
    val page: Int,
    val results: List<T>,
    val total_pages: Int,
    val total_results: Int
)

data class TmdbMovie(
    val id: Int,
    val title: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Double,
    val release_date: String?,
    val overview: String?
)

data class TmdbTvShow(
    val id: Int,
    val name: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Double,
    val first_air_date: String?,
    val overview: String?
)

data class TmdbPerson(
    val id: Int,
    val name: String,
    val profile_path: String?,
    val known_for_department: String?
)

sealed class TmdbSearchResult {
    data class Movie(
        val id: Int,
        val title: String,
        val poster_path: String?,
        val vote_average: Double,
        val release_date: String?,
        val overview: String?
    ) : TmdbSearchResult()

    data class TvShow(
        val id: Int,
        val name: String,
        val poster_path: String?,
        val vote_average: Double,
        val first_air_date: String?,
        val overview: String?
    ) : TmdbSearchResult()

    data class Person(
        val id: Int,
        val name: String,
        val profile_path: String?,
        val known_for_department: String?
    ) : TmdbSearchResult()
}

class TmdbSearchResultDeserializer : JsonDeserializer<TmdbSearchResult> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): TmdbSearchResult {
        if (!json.isJsonObject) {
            return TmdbSearchResult.Movie(
                id = 0,
                title = "",
                poster_path = null,
                vote_average = 0.0,
                release_date = null,
                overview = null
            )
        }
        val jsonObject = json.asJsonObject
        val mediaTypeEl = jsonObject.get("media_type")
        val mediaType = if (mediaTypeEl != null && !mediaTypeEl.isJsonNull && mediaTypeEl.isJsonPrimitive) {
            mediaTypeEl.asString
        } else {
            "movie"
        }
        val parsed: TmdbSearchResult? = when (mediaType) {
            "movie" -> context.deserialize(json, TmdbSearchResult.Movie::class.java)
            "tv" -> context.deserialize(json, TmdbSearchResult.TvShow::class.java)
            "person" -> context.deserialize(json, TmdbSearchResult.Person::class.java)
            else -> context.deserialize(json, TmdbSearchResult.Movie::class.java)
        }
        return (parsed ?: TmdbSearchResult.Movie(
            id = 0,
            title = "",
            poster_path = null,
            vote_average = 0.0,
            release_date = null,
            overview = null
        )).sanitized()
    }
}

data class TmdbImageResponse(
    val backdrops: List<TmdbImage>?,
    val posters: List<TmdbImage>?,
    val logos: List<TmdbImage>? = null
)

data class TmdbImage(
    val file_path: String,
    val width: Int,
    val height: Int,
    val aspect_ratio: Double? = null,
    val iso_639_1: String? = null
)

data class TmdbReleaseDatesResponse(
    val results: List<TmdbReleaseDatesResult>?
)

data class TmdbReleaseDatesResult(
    val iso_3166_1: String,
    val release_dates: List<TmdbReleaseDateItem>?
)

data class TmdbReleaseDateItem(
    val certification: String,
    val type: Int?
)

data class TmdbContentRatingsResponse(
    val results: List<TmdbContentRatingItem>?
)

data class TmdbContentRatingItem(
    val iso_3166_1: String,
    val rating: String
)

data class TmdbExternalIds(
    val imdb_id: String? = null,
    val tvdb_id: Int? = null,
    val id: Int? = null
)

data class TmdbMovieDetails(
    val id: Int,
    val title: String,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Double,
    val release_date: String?,
    val runtime: Int?,
    val genres: List<TmdbGenre>?,
    val credits: TmdbCredits?,
    val videos: TmdbVideoResponse?,
    val images: TmdbImageResponse?,
    val release_dates: TmdbReleaseDatesResponse?,
    val belongs_to_collection: TmdbCollectionBrief? = null,
    val production_companies: List<TmdbCompany>? = null,
    val similar: TmdbResponse<TmdbMovie>?,
    val recommendations: TmdbResponse<TmdbMovie>?,
    @SerializedName("watch/providers") val watch_providers: TmdbWatchProvidersResponse? = null,
    val alternative_titles: TmdbAlternativeTitlesResponse? = null,
    val imdb_id: String? = null,
    val external_ids: TmdbExternalIds? = null,
    val tagline: String? = null,
    val status: String? = null,
    val original_language: String? = null,
    val original_title: String? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    val production_countries: List<TmdbCountry>? = null,
    val reviews: TmdbResponse<TmdbReview>? = null
)

data class TmdbTvShowDetails(
    val id: Int,
    val name: String,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Double,
    val first_air_date: String?,
    val episode_run_time: List<Int>?,
    val genres: List<TmdbGenre>?,
    val credits: TmdbCredits?,
    val videos: TmdbVideoResponse?,
    val images: TmdbImageResponse?,
    val content_ratings: TmdbContentRatingsResponse?,
    val production_companies: List<TmdbCompany>? = null,
    val networks: List<TmdbCompany>? = null,
    val similar: TmdbResponse<TmdbTvShow>?,
    val recommendations: TmdbResponse<TmdbTvShow>?,
    val seasons: List<TmdbTvSeasonBrief>? = null,
    @SerializedName("watch/providers") val watch_providers: TmdbWatchProvidersResponse? = null,
    val alternative_titles: TmdbAlternativeTitlesResponse? = null,
    val external_ids: TmdbExternalIds? = null,
    val tagline: String? = null,
    val status: String? = null,
    val original_language: String? = null,
    val original_name: String? = null,
    val production_countries: List<TmdbCountry>? = null,
    val reviews: TmdbResponse<TmdbReview>? = null
)

data class TmdbCollectionBrief(
    val id: Int,
    val name: String,
    val poster_path: String?,
    val backdrop_path: String?
)

data class TmdbCollectionDetails(
    val id: Int,
    val name: String,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val parts: List<TmdbMovie>?
)

data class TmdbCompany(
    val id: Int,
    val name: String,
    val logo_path: String?,
    val origin_country: String?
)

data class TmdbGenre(
    val id: Int,
    val name: String
)

data class TmdbCredits(
    val cast: List<TmdbCastMember>?,
    val crew: List<TmdbCrewMember>?
)

data class TmdbCastMember(
    val id: Int,
    val name: String,
    val character: String?,
    val profile_path: String?
)

data class TmdbCrewMember(
    val id: Int,
    val name: String,
    val job: String?,
    val department: String?,
    val profile_path: String?
)

data class TmdbVideoResponse(
    val results: List<TmdbVideo>?
)

data class TmdbVideo(
    val key: String,
    val site: String,
    val type: String
)

data class TmdbPersonDetails(
    val id: Int,
    val name: String,
    val biography: String?,
    val profile_path: String?,
    val birthday: String?,
    val place_of_birth: String?,
    val movie_credits: TmdbPersonMovieCredits?,
    val tv_credits: TmdbPersonTvCredits?
)

data class TmdbPersonMovieCredits(
    val cast: List<TmdbMovie>?
)

data class TmdbPersonTvCredits(
    val cast: List<TmdbTvShow>?
)

data class TmdbWatchProvidersResponse(
    val results: Map<String, TmdbWatchProviderCountry>?
)

data class TmdbWatchProviderCountry(
    val link: String?,
    val flatrate: List<TmdbWatchProviderItem>?,
    val rent: List<TmdbWatchProviderItem>?,
    val buy: List<TmdbWatchProviderItem>?
)

data class TmdbWatchProviderItem(
    val logo_path: String,
    val provider_id: Int,
    val provider_name: String,
    val display_priority: Int
)

data class TmdbAlternativeTitlesResponse(
    val titles: List<TmdbAlternativeTitleItem>? = null,
    val results: List<TmdbAlternativeTitleItem>? = null
) {
    val allTitles: List<String>
        get() = (gsonSafeList<TmdbAlternativeTitleItem>(titles)
            .ifEmpty { gsonSafeList(results) })
            .mapNotNull { gsonSafeString(it.title).takeIf(String::isNotBlank) }
}

data class TmdbAlternativeTitleItem(
    val iso_3166_1: String,
    val title: String,
    val type: String?
)

data class TmdbTvSeasonBrief(
    val id: Int,
    val name: String,
    val season_number: Int,
    val episode_count: Int,
    val poster_path: String?,
    val air_date: String?
)

data class TmdbTvSeasonDetails(
    val id: Int,
    val name: String,
    val overview: String?,
    val season_number: Int,
    val poster_path: String?,
    val episodes: List<TmdbEpisode>?
)

data class TmdbEpisode(
    val id: Int,
    val name: String,
    val overview: String?,
    val episode_number: Int,
    val season_number: Int,
    val still_path: String?,
    val air_date: String?,
    val vote_average: Double
)

data class TmdbCountry(
    val iso_3166_1: String,
    val name: String
)

data class TmdbReview(
    val id: String,
    val author: String,
    val author_details: TmdbAuthorDetails? = null,
    val content: String,
    val created_at: String? = null,
    val url: String? = null
)

data class TmdbAuthorDetails(
    val name: String? = null,
    val username: String? = null,
    val avatar_path: String? = null,
    val rating: Double? = null
)

// ── Gson Runtime Null-Safety Sanitizers ─────────────────────────────────────
// Gson uses Unsafe/reflection and can inject null into non-null Kotlin fields
// when JSON keys are omitted or explicitly null. Passing values through Any?
// prevents kotlinc/R8 from eliding the runtime null check.

internal fun gsonSafeString(value: Any?, fallback: String = ""): String =
    (value as? String) ?: fallback

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> gsonSafeList(value: Any?): List<T> {
    val raw = value as? List<*> ?: return emptyList()
    return raw.mapNotNull { it as? T }
}

internal fun <T : Any> TmdbResponse<T>?.safeResults(transform: (T) -> T = { it }): List<T> {
    if (this == null) return emptyList()
    return gsonSafeList<T>(results).map(transform)
}

internal fun <T : Any> TmdbResponse<T>.sanitized(transform: (T) -> T): TmdbResponse<T> =
    copy(results = gsonSafeList<T>(results).map(transform))

internal fun TmdbMovie.sanitized(): TmdbMovie = copy(
    title = gsonSafeString(title),
    poster_path = poster_path,
    backdrop_path = backdrop_path,
    release_date = release_date,
    overview = overview
)

internal fun TmdbTvShow.sanitized(): TmdbTvShow = copy(
    name = gsonSafeString(name),
    poster_path = poster_path,
    backdrop_path = backdrop_path,
    first_air_date = first_air_date,
    overview = overview
)

internal fun TmdbPerson.sanitized(): TmdbPerson = copy(
    name = gsonSafeString(name),
    profile_path = profile_path,
    known_for_department = known_for_department
)

internal fun TmdbSearchResult.sanitized(): TmdbSearchResult = when (this) {
    is TmdbSearchResult.Movie -> copy(title = gsonSafeString(title))
    is TmdbSearchResult.TvShow -> copy(name = gsonSafeString(name))
    is TmdbSearchResult.Person -> copy(name = gsonSafeString(name))
}

internal fun TmdbImage.sanitized(): TmdbImage = copy(
    file_path = gsonSafeString(file_path)
)

internal fun TmdbImageResponse.sanitized(): TmdbImageResponse = copy(
    backdrops = backdrops?.let { gsonSafeList<TmdbImage>(it).map(TmdbImage::sanitized).filter { img -> img.file_path.isNotBlank() } },
    posters = posters?.let { gsonSafeList<TmdbImage>(it).map(TmdbImage::sanitized).filter { img -> img.file_path.isNotBlank() } },
    logos = logos?.let { gsonSafeList<TmdbImage>(it).map(TmdbImage::sanitized).filter { img -> img.file_path.isNotBlank() } }
)

internal fun TmdbGenre.sanitized(): TmdbGenre = copy(name = gsonSafeString(name))

internal fun TmdbCastMember.sanitized(): TmdbCastMember = copy(name = gsonSafeString(name))

internal fun TmdbCrewMember.sanitized(): TmdbCrewMember = copy(name = gsonSafeString(name))

internal fun TmdbCredits.sanitized(): TmdbCredits = copy(
    cast = cast?.let { gsonSafeList<TmdbCastMember>(it).map(TmdbCastMember::sanitized) },
    crew = crew?.let { gsonSafeList<TmdbCrewMember>(it).map(TmdbCrewMember::sanitized) }
)

internal fun TmdbVideo.sanitized(): TmdbVideo = copy(
    key = gsonSafeString(key),
    site = gsonSafeString(site),
    type = gsonSafeString(type)
)

internal fun TmdbVideoResponse.sanitized(): TmdbVideoResponse = copy(
    results = results?.let { gsonSafeList<TmdbVideo>(it).map(TmdbVideo::sanitized).filter { v -> v.key.isNotBlank() } }
)

internal fun TmdbReleaseDatesResponse.sanitized(): TmdbReleaseDatesResponse = copy(
    results = results?.let { list ->
        gsonSafeList<TmdbReleaseDatesResult>(list).map { r ->
            r.copy(
                iso_3166_1 = gsonSafeString(r.iso_3166_1),
                release_dates = r.release_dates?.let { items ->
                    gsonSafeList<TmdbReleaseDateItem>(items).map { item ->
                        item.copy(certification = gsonSafeString(item.certification))
                    }
                }
            )
        }
    }
)

internal fun TmdbContentRatingsResponse.sanitized(): TmdbContentRatingsResponse = copy(
    results = results?.let { list ->
        gsonSafeList<TmdbContentRatingItem>(list).map { item ->
            item.copy(
                iso_3166_1 = gsonSafeString(item.iso_3166_1),
                rating = gsonSafeString(item.rating)
            )
        }
    }
)

internal fun TmdbCollectionBrief.sanitized(): TmdbCollectionBrief = copy(
    name = gsonSafeString(name)
)

internal fun TmdbCollectionDetails.sanitized(): TmdbCollectionDetails = copy(
    name = gsonSafeString(name),
    parts = parts?.let { gsonSafeList<TmdbMovie>(it).map(TmdbMovie::sanitized) }
)

internal fun TmdbCompany.sanitized(): TmdbCompany = copy(
    name = gsonSafeString(name)
)

internal fun TmdbCountry.sanitized(): TmdbCountry = copy(
    iso_3166_1 = gsonSafeString(iso_3166_1),
    name = gsonSafeString(name)
)

internal fun TmdbWatchProviderItem.sanitized(): TmdbWatchProviderItem = copy(
    logo_path = gsonSafeString(logo_path),
    provider_name = gsonSafeString(provider_name)
)

internal fun TmdbWatchProvidersResponse.sanitized(): TmdbWatchProvidersResponse {
    val rawMap = results as? Map<*, *> ?: return copy(results = emptyMap())
    val safeMap = buildMap<String, TmdbWatchProviderCountry> {
        for ((k, v) in rawMap) {
            val key = k as? String ?: continue
            val country = v as? TmdbWatchProviderCountry ?: continue
            put(
                key,
                country.copy(
                    flatrate = country.flatrate?.let { gsonSafeList<TmdbWatchProviderItem>(it).map(TmdbWatchProviderItem::sanitized) },
                    rent = country.rent?.let { gsonSafeList<TmdbWatchProviderItem>(it).map(TmdbWatchProviderItem::sanitized) },
                    buy = country.buy?.let { gsonSafeList<TmdbWatchProviderItem>(it).map(TmdbWatchProviderItem::sanitized) }
                )
            )
        }
    }
    return copy(results = safeMap)
}

internal fun TmdbAlternativeTitlesResponse.sanitized(): TmdbAlternativeTitlesResponse = copy(
    titles = titles?.let { list ->
        gsonSafeList<TmdbAlternativeTitleItem>(list).map { item ->
            item.copy(
                iso_3166_1 = gsonSafeString(item.iso_3166_1),
                title = gsonSafeString(item.title)
            )
        }
    },
    results = results?.let { list ->
        gsonSafeList<TmdbAlternativeTitleItem>(list).map { item ->
            item.copy(
                iso_3166_1 = gsonSafeString(item.iso_3166_1),
                title = gsonSafeString(item.title)
            )
        }
    }
)

internal fun TmdbReview.sanitized(): TmdbReview = copy(
    id = gsonSafeString(id),
    author = gsonSafeString(author, "Anonymous"),
    content = gsonSafeString(content)
)

internal fun TmdbTvSeasonBrief.sanitized(): TmdbTvSeasonBrief = copy(
    name = gsonSafeString(name, "Season $season_number")
)

internal fun TmdbEpisode.sanitized(): TmdbEpisode = copy(
    name = gsonSafeString(name, "Episode $episode_number")
)

internal fun TmdbTvSeasonDetails.sanitized(): TmdbTvSeasonDetails = copy(
    name = gsonSafeString(name, "Season $season_number"),
    episodes = episodes?.let { gsonSafeList<TmdbEpisode>(it).map(TmdbEpisode::sanitized) }
)

internal fun TmdbPersonDetails.sanitized(): TmdbPersonDetails = copy(
    name = gsonSafeString(name),
    movie_credits = movie_credits?.let { mc ->
        mc.copy(cast = mc.cast?.let { gsonSafeList<TmdbMovie>(it).map(TmdbMovie::sanitized) })
    },
    tv_credits = tv_credits?.let { tc ->
        tc.copy(cast = tc.cast?.let { gsonSafeList<TmdbTvShow>(it).map(TmdbTvShow::sanitized) })
    }
)

internal fun TmdbMovieDetails.sanitized(): TmdbMovieDetails = copy(
    title = gsonSafeString(title).ifBlank { gsonSafeString(original_title) },
    genres = genres?.let { gsonSafeList<TmdbGenre>(it).map(TmdbGenre::sanitized) },
    credits = credits?.sanitized(),
    videos = videos?.sanitized(),
    images = images?.sanitized(),
    release_dates = release_dates?.sanitized(),
    belongs_to_collection = belongs_to_collection?.sanitized(),
    production_companies = production_companies?.let { gsonSafeList<TmdbCompany>(it).map(TmdbCompany::sanitized) },
    similar = similar?.sanitized(TmdbMovie::sanitized),
    recommendations = recommendations?.sanitized(TmdbMovie::sanitized),
    watch_providers = watch_providers?.sanitized(),
    alternative_titles = alternative_titles?.sanitized(),
    production_countries = production_countries?.let { gsonSafeList<TmdbCountry>(it).map(TmdbCountry::sanitized) },
    reviews = reviews?.sanitized(TmdbReview::sanitized)
)

internal fun TmdbTvShowDetails.sanitized(): TmdbTvShowDetails = copy(
    name = gsonSafeString(name).ifBlank { gsonSafeString(original_name) },
    episode_run_time = episode_run_time?.let { gsonSafeList<Int>(it) },
    genres = genres?.let { gsonSafeList<TmdbGenre>(it).map(TmdbGenre::sanitized) },
    credits = credits?.sanitized(),
    videos = videos?.sanitized(),
    images = images?.sanitized(),
    content_ratings = content_ratings?.sanitized(),
    production_companies = production_companies?.let { gsonSafeList<TmdbCompany>(it).map(TmdbCompany::sanitized) },
    networks = networks?.let { gsonSafeList<TmdbCompany>(it).map(TmdbCompany::sanitized) },
    similar = similar?.sanitized(TmdbTvShow::sanitized),
    recommendations = recommendations?.sanitized(TmdbTvShow::sanitized),
    seasons = seasons?.let { gsonSafeList<TmdbTvSeasonBrief>(it).map(TmdbTvSeasonBrief::sanitized) },
    watch_providers = watch_providers?.sanitized(),
    alternative_titles = alternative_titles?.sanitized(),
    production_countries = production_countries?.let { gsonSafeList<TmdbCountry>(it).map(TmdbCountry::sanitized) },
    reviews = reviews?.sanitized(TmdbReview::sanitized)
)

