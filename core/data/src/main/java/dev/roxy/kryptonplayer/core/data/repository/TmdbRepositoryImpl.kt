package dev.roxy.kryptonplayer.core.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dev.roxy.kryptonplayer.core.data.mappers.toVideo
import dev.roxy.kryptonplayer.core.data.network.FanartApi
import dev.roxy.kryptonplayer.core.data.network.FancapsScraper
import dev.roxy.kryptonplayer.core.data.network.ImdbApi
import dev.roxy.kryptonplayer.core.data.network.NetworkConfig
import dev.roxy.kryptonplayer.core.data.network.TmdbApi
import dev.roxy.kryptonplayer.core.data.network.models.*
import dev.roxy.kryptonplayer.core.database.dao.MediumDao
import dev.roxy.kryptonplayer.core.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbRepositoryImpl @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val imdbRepository: ImdbRepository,
    private val preferencesRepository: PreferencesRepository,
    private val mediumDao: MediumDao,
    private val fanartApi: FanartApi,
    private val fancapsScraper: FancapsScraper,
    private val imdbApi: ImdbApi,
    private val okHttpClient: OkHttpClient
) : TmdbRepository {

    private fun useImdb(): Boolean {
        return preferencesRepository.applicationPreferences.value.metadataProvider == "IMDB"
    }

    override suspend fun getPopularMovies(page: Int): Result<List<TmdbMovie>> {
        if (useImdb()) {
            val result = imdbRepository.getPopularMovies(page)
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.getPopularMovies(page = page).safeResults(TmdbMovie::sanitized) }
    }

    override suspend fun getPopularTvShows(page: Int): Result<List<TmdbTvShow>> {
        if (useImdb()) {
            val result = imdbRepository.getPopularTvShows(page)
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.getPopularTvShows(page = page).safeResults(TmdbTvShow::sanitized) }
    }

    override suspend fun getTrendingMovies(page: Int): Result<List<TmdbMovie>> {
        if (useImdb()) {
            val result = imdbRepository.getTrendingMovies(page)
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.getTrendingMovies(page = page).safeResults(TmdbMovie::sanitized) }
    }

    override suspend fun getTrendingTvShows(page: Int): Result<List<TmdbTvShow>> {
        if (useImdb()) {
            val result = imdbRepository.getTrendingTvShows(page)
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.getTrendingTvShows(page = page).safeResults(TmdbTvShow::sanitized) }
    }

    override suspend fun getPopularPeople(page: Int): Result<List<TmdbPerson>> {
        if (useImdb()) {
            val result = imdbRepository.getPopularPeople(page)
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.getPopularPeople(page = page).safeResults(TmdbPerson::sanitized) }
    }

    override suspend fun discoverMovies(genreId: String, page: Int): Result<List<TmdbMovie>> {
        if (useImdb()) {
            val result = imdbRepository.discoverMovies(genreId, page)
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.discoverMovies(genreId = genreId, page = page).safeResults(TmdbMovie::sanitized) }
    }

    override suspend fun searchMulti(query: String, page: Int): Result<List<TmdbSearchResult>> {
        if (useImdb()) {
            val result = imdbRepository.searchMulti(query, page)
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.searchMulti(query = query, page = page).safeResults(TmdbSearchResult::sanitized) }
    }

    override suspend fun getMovieDetails(id: Int): Result<TmdbMovieDetails> {
        if (useImdb()) {
            val result = imdbRepository.getMovieDetails(id).map { it.sanitized() }
            if (result.isSuccess) return result
        }
        return runCatching {
            val details = tmdbApi.getMovieDetails(id).sanitized()
            if (details.id == 0 && details.title.isBlank()) {
                throw IllegalStateException("Movie details unavailable for id=$id")
            }
            details
        }
    }

    override suspend fun getTvShowDetails(id: Int): Result<TmdbTvShowDetails> {
        if (useImdb()) {
            val result = imdbRepository.getTvShowDetails(id).map { it.sanitized() }
            if (result.isSuccess) return result
        }
        return runCatching {
            val details = tmdbApi.getTvShowDetails(id).sanitized()
            if (details.id == 0 && details.name.isBlank()) {
                throw IllegalStateException("TV show details unavailable for id=$id")
            }
            details
        }
    }

    override suspend fun getPersonDetails(id: Int): Result<TmdbPersonDetails> {
        if (useImdb()) {
            val result = imdbRepository.getPersonDetails(id).map { it.sanitized() }
            if (result.isSuccess) return result
        }
        return runCatching {
            val details = tmdbApi.getPersonDetails(id).sanitized()
            if (details.id == 0 && details.name.isBlank()) {
                throw IllegalStateException("Person details unavailable for id=$id")
            }
            details
        }
    }

    override suspend fun getTvSeasonDetails(tvId: Int, seasonNumber: Int): Result<TmdbTvSeasonDetails> {
        if (useImdb()) {
            val result = imdbRepository.getTvSeasonDetails(tvId, seasonNumber).map { it.sanitized() }
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.getTvSeasonDetails(tvId, seasonNumber).sanitized() }
    }

    override suspend fun getCollectionDetails(collectionId: Int): Result<TmdbCollectionDetails> {
        // IMDb doesn't have a direct equivalent of TMDB collections, so we always fall back or return empty
        return runCatching { tmdbApi.getCollectionDetails(collectionId).sanitized() }
    }

    override suspend fun getUpcomingMovies(page: Int): Result<List<TmdbMovie>> {
        if (useImdb()) {
            val result = imdbRepository.getUpcomingMovies(page)
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.getUpcomingMovies(page = page).safeResults(TmdbMovie::sanitized) }
    }

    override suspend fun getTopRatedMovies(page: Int): Result<List<TmdbMovie>> {
        if (useImdb()) {
            val result = imdbRepository.getTopRatedMovies(page)
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.getTopRatedMovies(page = page).safeResults(TmdbMovie::sanitized) }
    }

    override suspend fun getTopRatedTvShows(page: Int): Result<List<TmdbTvShow>> {
        if (useImdb()) {
            val result = imdbRepository.getTopRatedTvShows(page)
            if (result.isSuccess) return result
        }
        return runCatching { tmdbApi.getTopRatedTvShows(page = page).safeResults(TmdbTvShow::sanitized) }
    }

    override suspend fun findMatchingLocalVideo(title: String, alternativeTitles: List<String>): Video? {
        val allLocalMedia = runCatching { mediumDao.getAllWithInfo().first() }.getOrNull() ?: return null
        return allLocalMedia.map { it.toVideo() }.firstOrNull { video ->
            TmdbLocalMatcher.isFuzzyMatch(title, video.displayName, alternativeTitles)
        }
    }

    override suspend fun findMatchingLocalEpisode(
        showTitle: String,
        season: Int,
        episode: Int,
        alternativeTitles: List<String>
    ): Video? {
        val allLocalMedia = runCatching { mediumDao.getAllWithInfo().first() }.getOrNull() ?: return null
        return allLocalMedia.map { it.toVideo() }.firstOrNull { video ->
            TmdbLocalMatcher.isEpisodeMatch(showTitle, season, episode, video.displayName, alternativeTitles)
        }
    }

    private fun intToTitleId(id: Int): String {
        return "tt" + String.format("%07d", id)
    }

    private suspend fun resolveImdbId(id: Int, isTvShow: Boolean = false): String? {
        if (useImdb()) {
            return intToTitleId(id)
        }
        return if (isTvShow) {
            runCatching { tmdbApi.getTvExternalIds(id) }.getOrNull()?.imdb_id
        } else {
            runCatching { tmdbApi.getMovieExternalIds(id) }.getOrNull()?.imdb_id
        }
    }

    override suspend fun getParentsGuide(id: Int): Result<ImdbParentsGuideResponse> {
        return runCatching {
            val titleId = resolveImdbId(id) ?: intToTitleId(id)
            val raw = imdbApi.getParentsGuide(titleId)
            val safeItems = gsonSafeList<ImdbParentsGuideItem>(raw.parentsGuide).map { item ->
                item.copy(
                    category = gsonSafeString(item.category),
                    severityBreakdowns = item.severityBreakdowns?.let { list ->
                        gsonSafeList<ImdbParentsGuideSeverity>(list).map { sev ->
                            sev.copy(severityLevel = gsonSafeString(sev.severityLevel))
                        }
                    },
                    reviews = item.reviews?.let { list ->
                        gsonSafeList<ImdbParentsGuideReview>(list).map { rev ->
                            rev.copy(text = gsonSafeString(rev.text))
                        }
                    }
                )
            }
            ImdbParentsGuideResponse(parentsGuide = safeItems)
        }
    }

    override suspend fun getBoxOffice(id: Int): Result<ImdbBoxOffice> {
        return runCatching {
            val titleId = resolveImdbId(id) ?: intToTitleId(id)
            val raw = imdbApi.getBoxOffice(titleId)
            fun ImdbMoney?.safeMoney(): ImdbMoney? = this?.copy(
                amount = gsonSafeString(amount),
                currency = gsonSafeString(currency, "USD")
            )
            ImdbBoxOffice(
                domesticGross = raw.domesticGross.safeMoney(),
                worldwideGross = raw.worldwideGross.safeMoney(),
                openingWeekendGross = raw.openingWeekendGross?.let { ow ->
                    val safeGross = ((ow.gross as Any?) as? ImdbMoney)?.safeMoney()
                    val safeDate = (ow.weekendEndDate as Any?) as? ImdbPrecisionDate
                    if (safeGross != null && safeDate != null) {
                        ImdbOpeningWeekendGross(gross = safeGross, weekendEndDate = safeDate)
                    } else null
                },
                productionBudget = raw.productionBudget.safeMoney()
            )
        }
    }

    override suspend fun getCertificates(id: Int): Result<List<ImdbCertificate>> {
        return runCatching {
            val titleId = resolveImdbId(id) ?: intToTitleId(id)
            gsonSafeList<ImdbCertificate>(imdbApi.getCertificates(titleId).certificates).mapNotNull { cert ->
                val rawCountry = (cert.country as Any?) as? ImdbCountry
                ImdbCertificate(
                    rating = gsonSafeString(cert.rating),
                    country = ImdbCountry(
                        code = gsonSafeString(rawCountry?.code),
                        name = gsonSafeString(rawCountry?.name)
                    ),
                    attributes = cert.attributes?.let { gsonSafeList<String>(it) }
                ).takeIf { it.rating.isNotBlank() }
            }
        }
    }

    override suspend fun getTrivia(personId: Int): Result<List<ImdbTriviaItem>> {
        return imdbRepository.getTrivia(personId)
    }

    override suspend fun getRelationships(personId: Int): Result<List<ImdbRelationshipItem>> {
        return imdbRepository.getRelationships(personId)
    }

    override suspend fun getMediaStills(
        id: Int,
        isTvShow: Boolean,
        title: String,
        releaseYear: String?,
        imdbId: String?
    ): Result<List<MediaStill>> = withContext(Dispatchers.IO) {
        runCatching {
            val stillsList = mutableListOf<MediaStill>()

            var resolvedImdbId = imdbId
            var resolvedTvdbId: Int? = null
            if (resolvedImdbId.isNullOrBlank()) {
                if (isTvShow) {
                    val ext = runCatching { tmdbApi.getTvExternalIds(id) }.getOrNull()
                    resolvedImdbId = ext?.imdb_id
                    resolvedTvdbId = ext?.tvdb_id
                } else {
                    val ext = runCatching { tmdbApi.getMovieExternalIds(id) }.getOrNull()
                    resolvedImdbId = ext?.imdb_id
                }
            }

            val tmdbDeferred = async {
                runCatching {
                    val imgResponse = if (isTvShow) {
                        tmdbApi.getTvImages(id)
                    } else {
                        tmdbApi.getMovieImages(id)
                    }
                    imgResponse.backdrops?.mapIndexed { index, backdrop ->
                        val path = backdrop.file_path
                        MediaStill(
                            id = "tmdb_${id}_$index",
                            thumbnailUrl = NetworkConfig.tmdbImageUrl("w780", path ?: ""),
                            fullUrl = NetworkConfig.tmdbImageUrl("original", path ?: ""),
                            source = StillSource.TMDB,
                            width = backdrop.width,
                            height = backdrop.height,
                            caption = "TMDB 4K Still #${index + 1}"
                        )
                    } ?: emptyList()
                }.getOrElse { emptyList() }
            }

            val fanartDeferred = async {
                runCatching {
                    val fanartId = if (isTvShow) {
                        (resolvedTvdbId ?: id).toString()
                    } else {
                        resolvedImdbId ?: id.toString()
                    }
                    if (isTvShow) {
                        val res = fanartApi.getTvImages(fanartId)
                        val backgrounds = res.showbackground ?: emptyList()
                        backgrounds.mapIndexed { index, bg ->
                            MediaStill(
                                id = "fanart_tv_${bg.id}_$index",
                                thumbnailUrl = bg.url,
                                fullUrl = bg.url,
                                source = StillSource.FANART,
                                width = 1920,
                                height = 1080,
                                caption = "Fanart 1080p #${index + 1}"
                            )
                        }
                    } else {
                        val res = fanartApi.getMovieImages(fanartId)
                        val backgrounds = (res.movie4kbackground ?: emptyList()) + (res.moviebackground ?: emptyList())
                        backgrounds.mapIndexed { index, bg ->
                            MediaStill(
                                id = "fanart_movie_${bg.id}_$index",
                                thumbnailUrl = bg.url,
                                fullUrl = bg.url,
                                source = StillSource.FANART,
                                width = if (index < (res.movie4kbackground?.size ?: 0)) 3840 else 1920,
                                height = if (index < (res.movie4kbackground?.size ?: 0)) 2160 else 1080,
                                caption = "Fanart UHD #${index + 1}"
                            )
                        }
                    }
                }.getOrElse { emptyList() }
            }

            val imdbDeferred = async {
                runCatching {
                    val targetImdbId = resolvedImdbId ?: return@runCatching emptyList()
                    val res = imdbApi.getImages(targetImdbId, pageSize = 50)
                    val images = res.images ?: emptyList()
                    val stills = images.filter { it.type == "still_frame" || it.type == "production" || it.type == null }
                    val listToUse = if (stills.isNotEmpty()) stills else images
                    listToUse.mapIndexed { index, img ->
                        val url = img.url ?: ""
                        if (url.isBlank()) return@mapIndexed null
                        MediaStill(
                            id = "imdb_${targetImdbId}_$index",
                            thumbnailUrl = url,
                            fullUrl = url,
                            source = StillSource.IMDB,
                            width = img.width ?: 1920,
                            height = img.height ?: 1080,
                            caption = "IMDb Still #${index + 1}"
                        )
                    }.filterNotNull()
                }.getOrElse { emptyList() }
            }

            val fancapsSampleDeferred = async {
                runCatching {
                    val fancapsUrl = if (isTvShow) {
                        fancapsScraper.searchTv(title)
                    } else {
                        fancapsScraper.searchMovie(title, releaseYear)
                    }
                    if (fancapsUrl != null) {
                        val pageResult = fancapsScraper.getFramesFromPage(fancapsUrl, page = 1, isTv = isTvShow)
                        pageResult.frames.take(40).mapIndexed { index, frame ->
                            MediaStill(
                                id = "fancaps_${frame.frameId}",
                                thumbnailUrl = frame.thumbnailUrl,
                                fullUrl = frame.fullUrl,
                                source = StillSource.FANCAPS,
                                width = 1920,
                                height = 1080,
                                caption = "Frame #${frame.frameId}",
                                frameNumber = frame.frameId.toIntOrNull()
                            )
                        }
                    } else {
                        emptyList()
                    }
                }.getOrElse { emptyList() }
            }

            stillsList.addAll(tmdbDeferred.await())
            stillsList.addAll(fanartDeferred.await())
            stillsList.addAll(imdbDeferred.await())
            stillsList.addAll(fancapsSampleDeferred.await())

            stillsList
        }
    }

    override suspend fun getFancapsFrames(
        title: String,
        isTvShow: Boolean,
        season: Int,
        page: Int
    ): Result<FancapsPageResult> = withContext(Dispatchers.IO) {
        runCatching {
            val pageUrl = if (isTvShow) {
                fancapsScraper.searchTv(title, season)
            } else {
                fancapsScraper.searchMovie(title)
            } ?: throw IllegalStateException("No Fancaps archive found for $title")

            fancapsScraper.getFramesFromPage(pageUrl, page = page, isTv = isTvShow)
        }
    }

    override suspend fun setVideoCustomThumbnail(
        videoUri: String,
        imageUrl: String,
        context: Context
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBuilder = Request.Builder().url(imageUrl)
            if (imageUrl.contains("fancaps.net")) {
                requestBuilder.header("Referer", "https://fancaps.net/")
                requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            } else if (dev.roxy.kryptonplayer.core.data.network.NetworkConfig.isWorkerUrl(imageUrl)) {
                requestBuilder.header("X-Arch-Client-Key", dev.roxy.kryptonplayer.core.data.network.NetworkConfig.CLIENT_KEY)
            }
            val bytes = okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("Download failed with HTTP ${response.code()}")
                response.body()?.bytes() ?: throw IllegalStateException("Empty body")
            }

            val thumbsDir = File(context.filesDir, "custom_thumbnails").apply { mkdirs() }
            val safeName = "thumb_" + Math.abs(videoUri.hashCode()) + ".jpg"
            val thumbFile = File(thumbsDir, safeName)
            thumbFile.writeBytes(bytes)

            val existingMedium = mediumDao.get(videoUri)
            if (existingMedium != null) {
                mediumDao.upsert(existingMedium.copy(thumbnailPath = thumbFile.absolutePath))
            }

            true
        }
    }

    override suspend fun saveImageToFramesFolder(
        imageUrl: String,
        title: String,
        context: Context
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBuilder = Request.Builder().url(imageUrl)
            if (imageUrl.contains("fancaps.net")) {
                requestBuilder.header("Referer", "https://fancaps.net/")
                requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            } else if (dev.roxy.kryptonplayer.core.data.network.NetworkConfig.isWorkerUrl(imageUrl)) {
                requestBuilder.header("X-Arch-Client-Key", dev.roxy.kryptonplayer.core.data.network.NetworkConfig.CLIENT_KEY)
            }
            val bytes = okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("Download failed with HTTP ${response.code()}")
                response.body()?.bytes() ?: throw IllegalStateException("Empty body")
            }

            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val fileName = "${cleanTitle}_frame_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KryptonPlayer/Frames")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Failed to create MediaStore entry")

                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                } ?: throw IllegalStateException("Failed to open output stream")

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                uri.toString()
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val framesDir = File(picturesDir, "KryptonPlayer/Frames").apply { mkdirs() }
                val targetFile = File(framesDir, fileName)
                targetFile.writeBytes(bytes)
                targetFile.absolutePath
            }
        }
    }

    override suspend fun getReviews(id: Int, isTvShow: Boolean): Result<List<TmdbReview>> {
        return runCatching {
            if (isTvShow) {
                tmdbApi.getTvReviews(id).safeResults(TmdbReview::sanitized)
            } else {
                tmdbApi.getMovieReviews(id).safeResults(TmdbReview::sanitized)
            }
        }
    }
}
