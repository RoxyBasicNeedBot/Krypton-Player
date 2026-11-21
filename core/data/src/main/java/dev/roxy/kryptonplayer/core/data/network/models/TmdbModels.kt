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
        val jsonObject = json.asJsonObject
        val mediaType = jsonObject.get("media_type")?.asString ?: "movie"
        return when (mediaType) {
            "movie" -> context.deserialize(json, TmdbSearchResult.Movie::class.java)
            "tv" -> context.deserialize(json, TmdbSearchResult.TvShow::class.java)
            "person" -> context.deserialize(json, TmdbSearchResult.Person::class.java)
            else -> context.deserialize(json, TmdbSearchResult.Movie::class.java)
        }
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
        get() = (titles ?: results ?: emptyList()).map { it.title }
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
