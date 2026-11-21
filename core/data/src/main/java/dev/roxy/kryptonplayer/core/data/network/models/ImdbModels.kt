package dev.roxy.kryptonplayer.core.data.network.models

import com.google.gson.annotations.SerializedName

data class ImdbTitleResponse(
    val titles: List<ImdbTitle>,
    val totalCount: Int? = null,
    val nextPageToken: String? = null
)

data class ImdbTitle(
    val id: String,
    val type: String?,
    val isAdult: Boolean? = null,
    val primaryTitle: String?,
    val originalTitle: String?,
    val primaryImage: ImdbImage?,
    val startYear: Int?,
    val endYear: Int?,
    val runtimeSeconds: Int?,
    val genres: List<String>?,
    val rating: ImdbRating?,
    val metacritic: ImdbMetacritic? = null,
    val plot: String?,
    val directors: List<ImdbName>?,
    val writers: List<ImdbName>? = null,
    val stars: List<ImdbName>? = null,
    val originCountries: List<ImdbCountry>? = null,
    val spokenLanguages: List<ImdbLanguage>? = null
)

data class ImdbImage(
    val url: String?,
    val width: Int?,
    val height: Int?,
    val type: String? = null
)

data class ImdbRating(
    val aggregateRating: Double?,
    val voteCount: Int?
)

data class ImdbMetacritic(
    val url: String?,
    val score: Int?,
    val reviewCount: Int?
)

data class ImdbName(
    val id: String,
    val displayName: String?,
    val alternativeNames: List<String>? = null,
    val primaryImage: ImdbImage? = null,
    val primaryProfessions: List<String>? = null,
    val biography: String? = null,
    val heightCm: Int? = null,
    val birthName: String? = null,
    val birthDate: ImdbPrecisionDate? = null,
    val birthLocation: String? = null,
    val deathDate: ImdbPrecisionDate? = null,
    val deathLocation: String? = null,
    val deathReason: String? = null,
    val meterRanking: ImdbNameMeterRanking? = null
)

data class ImdbPrecisionDate(
    val year: Int,
    val month: Int? = null,
    val day: Int? = null
) {
    fun toReleaseDateString(): String {
        return if (month != null && day != null) {
            String.format("%04d-%02d-%02d", year, month, day)
        } else if (month != null) {
            String.format("%04d-%02d-01", year, month)
        } else {
            String.format("%04d-01-01", year)
        }
    }
}

data class ImdbNameMeterRanking(
    val currentRank: Int,
    val changeDirection: String,
    val difference: Int
)

data class ImdbCountry(
    val code: String,
    val name: String
)

data class ImdbLanguage(
    val code: String,
    val name: String
)

data class ImdbCreditsResponse(
    val credits: List<ImdbCredit>,
    val totalCount: Int? = null,
    val nextPageToken: String? = null
)

data class ImdbCredit(
    val title: ImdbTitle? = null,
    val name: ImdbName? = null,
    val category: String?,
    val characters: List<String>? = null,
    val episodeCount: Int? = null
)

data class ImdbEpisodesResponse(
    val episodes: List<ImdbEpisode>,
    val totalCount: Int? = null,
    val nextPageToken: String? = null
)

data class ImdbEpisode(
    val id: String,
    val title: String?,
    val primaryImage: ImdbImage?,
    val season: String?,
    val episodeNumber: Int?,
    val runtimeSeconds: Int?,
    val plot: String?,
    val rating: ImdbRating?,
    val releaseDate: ImdbPrecisionDate?
)

data class ImdbSeasonsResponse(
    val seasons: List<ImdbSeason>
)

data class ImdbSeason(
    val season: String,
    val episodeCount: Int
)

data class ImdbParentsGuideResponse(
    val parentsGuide: List<ImdbParentsGuideItem>? = null
)

data class ImdbParentsGuideItem(
    val category: String, // SEXUAL_CONTENT, VIOLENCE, etc.
    val severityBreakdowns: List<ImdbParentsGuideSeverity>? = null,
    val reviews: List<ImdbParentsGuideReview>? = null
)

data class ImdbParentsGuideSeverity(
    val severityLevel: String,
    val voteCount: Int
)

data class ImdbParentsGuideReview(
    val text: String,
    val isSpoiler: Boolean
)

data class ImdbBoxOffice(
    val domesticGross: ImdbMoney? = null,
    val worldwideGross: ImdbMoney? = null,
    val openingWeekendGross: ImdbOpeningWeekendGross? = null,
    val productionBudget: ImdbMoney? = null
)

data class ImdbMoney(
    val amount: String,
    val currency: String
)

data class ImdbOpeningWeekendGross(
    val gross: ImdbMoney,
    val weekendEndDate: ImdbPrecisionDate
)

data class ImdbCertificatesResponse(
    val certificates: List<ImdbCertificate>? = null,
    val totalCount: Int? = null
)

data class ImdbCertificate(
    val rating: String,
    val country: ImdbCountry,
    val attributes: List<String>? = null
)

data class ImdbTriviaResponse(
    val trivia: List<ImdbTriviaItem>? = null
)

data class ImdbTriviaItem(
    val id: String,
    val text: String,
    val interestCount: Int? = null,
    val voteCount: Int? = null
)

data class ImdbRelationshipsResponse(
    val relationships: List<ImdbRelationshipItem>? = null
)

data class ImdbRelationshipItem(
    val name: ImdbName,
    val relationType: String,
    val attributes: List<String>? = null
)

data class ImdbFilmographyResponse(
    val credits: List<ImdbCredit>
)

data class ImdbAKAsResponse(
    val akas: List<ImdbAKA>
)

data class ImdbAKA(
    val text: String,
    val country: ImdbCountry?,
    val language: ImdbLanguage?,
    val attributes: List<String>? = null
)

data class ImdbImagesResponse(
    val images: List<ImdbImage>? = null,
    val totalCount: Int? = null,
    val nextPageToken: String? = null
)
