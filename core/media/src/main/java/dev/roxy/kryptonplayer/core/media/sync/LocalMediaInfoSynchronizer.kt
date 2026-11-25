package dev.roxy.kryptonplayer.core.media.sync

import android.content.Context
import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import coil3.ImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.roxy.kryptonplayer.core.common.Dispatcher
import dev.roxy.kryptonplayer.core.common.NextDispatchers
import dev.roxy.kryptonplayer.core.common.di.ApplicationScope
import dev.roxy.kryptonplayer.core.database.dao.MediumDao
import dev.roxy.kryptonplayer.core.database.entities.AudioStreamInfoEntity
import dev.roxy.kryptonplayer.core.database.entities.SubtitleStreamInfoEntity
import dev.roxy.kryptonplayer.core.database.entities.VideoStreamInfoEntity
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalMediaInfoSynchronizer @Inject constructor(
    private val mediumDao: MediumDao,
    private val imageLoader: ImageLoader,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    @Dispatcher(NextDispatchers.Default) private val dispatcher: CoroutineDispatcher,
) : MediaInfoSynchronizer {

    private val activeSyncJobs = mutableMapOf<String, Job>()
    private val mutex = Mutex()

    override fun sync(uri: Uri) {
        applicationScope.launch(dispatcher) {
            val uriString = uri.toString()

            mutex.withLock {
                activeSyncJobs[uriString]
            }?.join()

            val job = applicationScope.launch(dispatcher) {
                try {
                    performSync(uri)
                } finally {
                    mutex.withLock {
                        activeSyncJobs.remove(uriString)
                    }
                }
            }

            mutex.withLock {
                activeSyncJobs[uriString] = job
            }
        }
    }

    override suspend fun clearThumbnailsCache() {
        imageLoader.diskCache?.clear()
        imageLoader.memoryCache?.clear()
    }

    private suspend fun performSync(uri: Uri) {
        val medium = mediumDao.getWithInfo(uri.toString()) ?: return
        if (medium.videoStreamInfo != null) return

        val mediumUri = medium.mediumEntity.uriString
        val filePath = medium.mediumEntity.path

        val metadata = extractRetrieverMetadata(uri, filePath)
        val tracks = extractTracks(uri, filePath, mediumUri, metadata)

        val videoStreamInfo = tracks.videoStream ?: if (metadata.width > 0 && metadata.height > 0) {
            VideoStreamInfoEntity(
                index = 0,
                title = null,
                codecName = resolveFormatName(metadata.containerMime, filePath) ?: "video",
                language = null,
                disposition = 0,
                bitRate = metadata.bitRate,
                frameRate = metadata.frameRate,
                frameWidth = metadata.width,
                frameHeight = metadata.height,
                mediumUri = mediumUri,
            )
        } else {
            null
        }

        val extractedFormat = resolveFormatName(metadata.containerMime, filePath) ?: medium.mediumEntity.format
        val extractedDurationMs = maxOf(tracks.maxDurationMs, metadata.durationMs)
        val resolvedDuration = if (medium.mediumEntity.duration <= 0L && extractedDurationMs > 0L) {
            extractedDurationMs
        } else {
            medium.mediumEntity.duration
        }

        if (extractedFormat == null &&
            resolvedDuration <= 0L &&
            videoStreamInfo == null &&
            tracks.audioStreams.isEmpty() &&
            tracks.subtitleStreams.isEmpty()
        ) {
            return
        }

        val resolvedSize = if (medium.mediumEntity.size <= 0L) {
            try {
                val f = File(filePath)
                if (f.exists()) f.length() else medium.mediumEntity.size
            } catch (_: Exception) {
                medium.mediumEntity.size
            }
        } else {
            medium.mediumEntity.size
        }

        mediumDao.upsert(
            medium.mediumEntity.copy(
                format = extractedFormat,
                duration = resolvedDuration,
                size = resolvedSize,
            ),
        )
        videoStreamInfo?.let { mediumDao.upsertVideoStreamInfo(it) }
        tracks.audioStreams.forEach { mediumDao.upsertAudioStreamInfo(it) }
        tracks.subtitleStreams.forEach { mediumDao.upsertSubtitleStreamInfo(it) }
    }

    private fun extractTracks(
        uri: Uri,
        filePath: String,
        mediumUri: String,
        fallback: RetrieverMetadata,
    ): ExtractedTracks {
        val extractor = MediaExtractor()
        return try {
            val opened = runCatching {
                extractor.setDataSource(context, uri, null)
            }.isSuccess || (
                filePath.isNotBlank() && File(filePath).exists() && runCatching {
                    extractor.setDataSource(filePath)
                }.isSuccess
            )
            if (!opened) return ExtractedTracks()

            var videoStream: VideoStreamInfoEntity? = null
            val audioStreams = mutableListOf<AudioStreamInfoEntity>()
            val subtitleStreams = mutableListOf<SubtitleStreamInfoEntity>()
            var maxDurationMs = 0L

            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.stringOrNull(MediaFormat.KEY_MIME)?.lowercase(Locale.US) ?: continue
                val durationMs = (format.longOrNull(MediaFormat.KEY_DURATION) ?: 0L) / 1000L
                if (durationMs > maxDurationMs) {
                    maxDurationMs = durationMs
                }

                val codecName = normalizeCodecName(mime)
                val language = format.stringOrNull(MediaFormat.KEY_LANGUAGE)
                val bitRate = format.intOrNull(MediaFormat.KEY_BIT_RATE)?.toLong() ?: 0L

                when {
                    mime.startsWith("video/") && videoStream == null -> {
                        val width = format.intOrNull(MediaFormat.KEY_WIDTH) ?: fallback.width
                        val height = format.intOrNull(MediaFormat.KEY_HEIGHT) ?: fallback.height
                        val fps = format.floatOrNull(MediaFormat.KEY_FRAME_RATE)?.toDouble()
                            ?: format.intOrNull(MediaFormat.KEY_FRAME_RATE)?.toDouble()
                            ?: fallback.frameRate
                        videoStream = VideoStreamInfoEntity(
                            index = index,
                            title = null,
                            codecName = codecName,
                            language = language,
                            disposition = 0,
                            bitRate = if (bitRate > 0L) bitRate else fallback.bitRate,
                            frameRate = fps,
                            frameWidth = width,
                            frameHeight = height,
                            mediumUri = mediumUri,
                        )
                    }

                    mime.startsWith("audio/") -> {
                        val channels = format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 0
                        val sampleRate = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
                        val sampleFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            format.intOrNull(MediaFormat.KEY_PCM_ENCODING)?.let(::pcmEncodingName)
                        } else {
                            null
                        }
                        audioStreams.add(
                            AudioStreamInfoEntity(
                                index = index,
                                title = null,
                                codecName = codecName,
                                language = language,
                                disposition = 0,
                                bitRate = bitRate,
                                sampleFormat = sampleFormat,
                                sampleRate = sampleRate,
                                channels = channels,
                                channelLayout = inferChannelLayout(channels),
                                mediumUri = mediumUri,
                            ),
                        )
                    }

                    isSubtitleMime(mime) -> {
                        subtitleStreams.add(
                            SubtitleStreamInfoEntity(
                                index = index,
                                title = null,
                                codecName = codecName,
                                language = language,
                                disposition = 0,
                                mediumUri = mediumUri,
                            ),
                        )
                    }
                }
            }

            ExtractedTracks(
                videoStream = videoStream,
                audioStreams = audioStreams,
                subtitleStreams = subtitleStreams,
                maxDurationMs = maxDurationMs,
            )
        } catch (e: Exception) {
            Log.d(TAG, "sync: MediaExtractor exception", e)
            ExtractedTracks()
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun extractRetrieverMetadata(uri: Uri, filePath: String): RetrieverMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            val opened = runCatching {
                retriever.setDataSource(context, uri)
            }.isSuccess || (
                filePath.isNotBlank() && File(filePath).exists() && runCatching {
                    retriever.setDataSource(filePath)
                }.isSuccess
            )
            if (!opened) return RetrieverMetadata()

            RetrieverMetadata(
                containerMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                bitRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L,
                frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toDoubleOrNull() ?: 0.0,
            )
        } catch (e: Exception) {
            Log.d(TAG, "sync: MediaMetadataRetriever exception", e)
            RetrieverMetadata()
        } finally {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    retriever.close()
                } else {
                    retriever.release()
                }
            }
        }
    }

    private data class ExtractedTracks(
        val videoStream: VideoStreamInfoEntity? = null,
        val audioStreams: List<AudioStreamInfoEntity> = emptyList(),
        val subtitleStreams: List<SubtitleStreamInfoEntity> = emptyList(),
        val maxDurationMs: Long = 0L,
    )

    private data class RetrieverMetadata(
        val containerMime: String? = null,
        val durationMs: Long = 0L,
        val width: Int = 0,
        val height: Int = 0,
        val bitRate: Long = 0L,
        val frameRate: Double = 0.0,
    )

    companion object {
        private const val TAG = "MediaInfoSynchronizer"
    }
}

private fun MediaFormat.stringOrNull(key: String): String? =
    if (containsKey(key)) runCatching { getString(key) }.getOrNull() else null

private fun MediaFormat.intOrNull(key: String): Int? =
    if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

private fun MediaFormat.longOrNull(key: String): Long? =
    if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null

private fun MediaFormat.floatOrNull(key: String): Float? =
    if (containsKey(key)) runCatching { getFloat(key) }.getOrNull() else null

private fun isSubtitleMime(mime: String): Boolean =
    mime.startsWith("text/") ||
        mime.startsWith("application/x-sub") ||
        mime == "application/ttml+xml" ||
        mime == "application/cea-608" ||
        mime == "application/cea-708"

private fun normalizeCodecName(mime: String): String = when (mime) {
    MediaFormat.MIMETYPE_VIDEO_AVC -> "h264"
    MediaFormat.MIMETYPE_VIDEO_HEVC -> "hevc"
    MediaFormat.MIMETYPE_VIDEO_VP9 -> "vp9"
    MediaFormat.MIMETYPE_VIDEO_VP8 -> "vp8"
    "video/av01" -> "av1"
    MediaFormat.MIMETYPE_VIDEO_MPEG4 -> "mpeg4"
    MediaFormat.MIMETYPE_VIDEO_MPEG2 -> "mpeg2video"
    "video/dolby-vision" -> "dvhe"
    MediaFormat.MIMETYPE_AUDIO_AAC -> "aac"
    MediaFormat.MIMETYPE_AUDIO_MPEG -> "mp3"
    MediaFormat.MIMETYPE_AUDIO_OPUS -> "opus"
    MediaFormat.MIMETYPE_AUDIO_VORBIS -> "vorbis"
    MediaFormat.MIMETYPE_AUDIO_FLAC -> "flac"
    MediaFormat.MIMETYPE_AUDIO_AC3 -> "ac3"
    MediaFormat.MIMETYPE_AUDIO_EAC3, "audio/eac3-joc" -> "eac3"
    "audio/vnd.dts", "audio/vnd.dts.hd" -> "dts"
    "audio/true-hd" -> "truehd"
    MediaFormat.MIMETYPE_AUDIO_RAW -> "pcm"
    "application/x-subrip" -> "subrip"
    "text/vtt" -> "webvtt"
    "text/x-ssa" -> "ass"
    "application/cea-608" -> "eia_608"
    "application/cea-708" -> "eia_708"
    "application/ttml+xml" -> "ttml"
    else -> mime.substringAfter('/', mime)
}

private fun resolveFormatName(containerMime: String?, pathOrUri: String): String? {
    val mimeNormalized = when (containerMime?.lowercase(Locale.US)) {
        "video/mp4", "audio/mp4" -> "mp4"
        "video/x-matroska", "audio/x-matroska" -> "matroska"
        "video/webm", "audio/webm" -> "webm"
        "video/quicktime" -> "mov"
        "video/x-msvideo" -> "avi"
        "video/mp2t" -> "mpegts"
        "video/3gpp" -> "3gp"
        "video/x-flv" -> "flv"
        "audio/mpeg" -> "mp3"
        "audio/flac" -> "flac"
        "audio/ogg", "application/ogg" -> "ogg"
        else -> null
    }
    if (mimeNormalized != null) return mimeNormalized

    val ext = pathOrUri.substringAfterLast('.', "").lowercase(Locale.US)
    return when (ext) {
        "mkv", "mka" -> "matroska"
        "mp4", "m4v", "m4a" -> "mp4"
        "webm" -> "webm"
        "mov" -> "mov"
        "avi" -> "avi"
        "ts", "m2ts", "mts" -> "mpegts"
        "3gp" -> "3gp"
        "flv" -> "flv"
        "wmv", "asf" -> "asf"
        "mp3" -> "mp3"
        "flac" -> "flac"
        "ogg", "opus" -> "ogg"
        else -> ext.takeIf { it.length in 2..8 }
    }
}

private fun inferChannelLayout(channels: Int): String? = when (channels) {
    1 -> "mono"
    2 -> "stereo"
    6 -> "5.1"
    8 -> "7.1"
    else -> if (channels > 0) "${channels}ch" else null
}

private fun pcmEncodingName(encoding: Int): String? = when (encoding) {
    AudioFormat.ENCODING_PCM_8BIT -> "u8"
    AudioFormat.ENCODING_PCM_16BIT -> "s16"
    AudioFormat.ENCODING_PCM_FLOAT -> "flt"
    else -> null
}
