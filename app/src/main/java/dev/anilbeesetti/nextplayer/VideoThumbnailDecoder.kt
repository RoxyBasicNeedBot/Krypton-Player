package dev.anilbeesetti.nextplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.ContentMetadata
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.toAndroidUri
import okio.FileSystem
import androidx.core.graphics.get
import io.github.anilbeesetti.nextlib.mediainfo.MediaThumbnailRetriever
import kotlin.math.abs
import coil3.decode.DecodeUtils
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.util.component1
import coil3.util.component2
import kotlin.math.roundToInt

class VideoThumbnailDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val strategy: ThumbnailStrategy,
    private val diskCache: Lazy<DiskCache?>,
) : Decoder {

    private val diskCacheKey: String
        get() = options.diskCacheKey ?: run {
            val metadata = source.metadata
            when {
                metadata is ContentMetadata -> metadata.uri.toAndroidUri().toString()
                source.fileSystem === FileSystem.SYSTEM -> source.file().toFile().path
                else -> error("Not supported")
            }
        }

    @OptIn(ExperimentalCoilApi::class, androidx.media3.common.util.UnstableApi::class)
    override suspend fun decode(): DecodeResult {
        readFromDiskCache()?.use { snapshot ->
            val file = snapshot.data.toFile()

            // Read cached image dimensions (no pixel allocation)
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            file.inputStream().use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            val cachedWidth = boundsOpts.outWidth
            val cachedHeight = boundsOpts.outHeight

            // Determine what size the caller actually wants
            val requestedWidth = options.size.width.pxOrElse { 0 }
            val requestedHeight = options.size.height.pxOrElse { 0 }

            // Cache is sufficient only if requested size <= cached size (or size is unspecified)
            val cacheIsSufficient = (requestedWidth == 0 || requestedWidth <= cachedWidth) &&
                    (requestedHeight == 0 || requestedHeight <= cachedHeight)

            if (cacheIsSufficient) {
                val dstSize = computeDstSize(cachedWidth, cachedHeight)
                val sampledBitmap = file.inputStream().use { BitmapFactory.decodeStream(it) }
                val normalizedBitmap = normalizeBitmap(
                    inBitmap = sampledBitmap,
                    srcWidth = cachedWidth,
                    srcHeight = cachedHeight,
                    dstSize = dstSize,
                )
                return DecodeResult(
                    image = normalizedBitmap.toDrawable(options.context.resources).asImage(),
                    isSampled = DecodeUtils.computeSizeMultiplier(
                        srcWidth = cachedWidth,
                        srcHeight = cachedHeight,
                        dstWidth = normalizedBitmap.width,
                        dstHeight = normalizedBitmap.height,
                        scale = options.scale,
                        maxSize = options.maxBitmapSize,
                    ) < 1.0,
                )
            }
        }

        // Cache miss OR cache insufficient: decode fresh thumbnail
        val rawBitmap = try {
            MediaMetadataRetriever().use { nativeRetriever ->
                MediaThumbnailRetriever().use { ffmpegRetriever ->
                    var nativeOk = false
                    try {
                        nativeRetriever.setDataSource(source)
                        nativeOk = true
                    } catch (_: Exception) {}

                    var ffmpegOk = false
                    try {
                        ffmpegRetriever.setDataSource(source)
                        ffmpegOk = true
                    } catch (_: Exception) {}

                    if (!nativeOk && !ffmpegOk) {
                        throw IllegalStateException("Both native and ffmpeg retrievers failed to open source.")
                    }

                    // First, try to get embedded picture (album art/metadata thumbnail)
                    val embeddedPicture = (if (nativeOk) nativeRetriever.embeddedPicture else null)
                        ?: (if (ffmpegOk) ffmpegRetriever.getEmbeddedPicture() else null)
                    val embeddedPictureBitmap = embeddedPicture?.let { pictureBytes ->
                        BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size)
                    }

                    if (embeddedPictureBitmap != null) return@use embeddedPictureBitmap

                    val videoDuration = if (nativeOk) {
                        nativeRetriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L
                    } else {
                        0L
                    }

                    val frame = when (strategy) {
                        is ThumbnailStrategy.FirstFrame -> {
                            val nativeFrame = if (nativeOk) nativeRetriever.getFrameAtTime(0) else null
                            nativeFrame ?: (if (ffmpegOk) ffmpegRetriever.getFrameAtTime(0) else null)
                        }

                        is ThumbnailStrategy.FrameAtPercentage -> {
                            val timeUs = (videoDuration * strategy.percentage * 1000).toLong()
                            val nativeFrame = if (nativeOk) nativeRetriever.getFrameAtTime(timeUs) else null
                            nativeFrame ?: (if (ffmpegOk) ffmpegRetriever.getFrameAtTime(timeUs) else null)
                        }

                        is ThumbnailStrategy.Hybrid -> {
                            val firstFrame = if (nativeOk) nativeRetriever.getFrameAtTime(0) else null
                            if (firstFrame == null || isSolidColor(firstFrame)) {
                                val timeUs = (videoDuration * strategy.percentage * 1000).toLong()
                                val nativeFrame = if (nativeOk) nativeRetriever.getFrameAtTime(timeUs) else null
                                nativeFrame ?: (if (ffmpegOk) ffmpegRetriever.getFrameAtTime(timeUs) else null)
                            } else {
                                firstFrame
                            }
                        }
                    }
                    frame ?: throw IllegalStateException("Failed to extract frame from primary retrievers.")
                }
            }
        } catch (e: Exception) {
            val uri = when {
                source.metadata is ContentMetadata -> (source.metadata as ContentMetadata).uri.toAndroidUri()
                source.fileSystem === FileSystem.SYSTEM -> Uri.fromFile(source.file().toFile())
                else -> null
            }
            if (uri != null) {
                try {
                    val extractor = dev.anilbeesetti.nextplayer.feature.player.thumbnail.Media3FrameExtractor(options.context)
                    val timeMs = when (strategy) {
                        is ThumbnailStrategy.FirstFrame -> 0L
                        is ThumbnailStrategy.FrameAtPercentage -> 5000L
                        is ThumbnailStrategy.Hybrid -> 0L
                    }
                    extractor.extractFrame(uri, timeMs) ?: throw e
                } catch (ex: Exception) {
                    android.util.Log.e("VideoThumbnailDecoder", "Media3FrameExtractor fallback failed", ex)
                    throw e
                }
            } else {
                throw e
            }
        }

        val srcWidth = rawBitmap.width
        val srcHeight = rawBitmap.height
        val dstSize = computeDstSize(srcWidth, srcHeight)
        val scaledBitmap = normalizeBitmap(
            inBitmap = rawBitmap,
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstSize = dstSize,
        )

        writeToDiskCache(scaledBitmap)

        return DecodeResult(
            image = scaledBitmap.toDrawable(options.context.resources).asImage(),
            isSampled = DecodeUtils.computeSizeMultiplier(
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstWidth = scaledBitmap.width,
                dstHeight = scaledBitmap.height,
                scale = options.scale,
                maxSize = options.maxBitmapSize,
            ) < 1.0,
        )
    }

    private fun MediaMetadataRetriever.setDataSource(source: ImageSource) {
        val metadata = source.metadata
        when {
            metadata is ContentMetadata -> {
                setDataSource(options.context, metadata.uri.toAndroidUri())
            }

            source.fileSystem === FileSystem.SYSTEM -> {
                setDataSource(source.file().toFile().path)
            }

            else -> error("Not supported")
        }
    }

    private fun MediaThumbnailRetriever.setDataSource(source: ImageSource) {
        val metadata = source.metadata
        when {
            metadata is ContentMetadata -> {
                setDataSource(options.context, metadata.uri.toAndroidUri())
            }

            source.fileSystem === FileSystem.SYSTEM -> {
                setDataSource(source.file().toFile().path)
            }

            else -> error("Not supported")
        }
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) {
            diskCache.value?.openSnapshot(diskCacheKey)
        } else {
            null
        }
    }

    private fun writeToDiskCache(inBitmap: Bitmap) {
        if (!options.diskCachePolicy.writeEnabled) return
        val editor = diskCache.value?.openEditor(diskCacheKey) ?: return
        try {
            editor.data.toFile().outputStream().use { output ->
                inBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            editor.commit()
        } catch (_: Exception) {
            runCatching { editor.abort() }
        }
    }

    private fun normalizeBitmap(inBitmap: Bitmap, srcWidth: Int, srcHeight: Int, dstSize: Size): Bitmap {
        val scale = DecodeUtils.computeSizeMultiplier(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstSize.width.pxOrElse { inBitmap.width },
            dstHeight = dstSize.height.pxOrElse { inBitmap.height },
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        ).toFloat()

        if (scale == 1f) return inBitmap

        val dstWidth = (scale * inBitmap.width).roundToInt()
        val dstHeight = (scale * inBitmap.height).roundToInt()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val outBitmap = createBitmap(dstWidth, dstHeight, inBitmap.config ?: Bitmap.Config.ARGB_8888)
        outBitmap.applyCanvas {
            scale(scale, scale)
            drawBitmap(inBitmap, 0f, 0f, paint)
        }
        inBitmap.recycle()
        return outBitmap
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun computeDstSize(srcWidth: Int, srcHeight: Int): Size {
        if (srcWidth <= 0 || srcHeight <= 0) return Size.ORIGINAL

        val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            targetSize = options.size,
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        )
        val rawScale = DecodeUtils.computeSizeMultiplier(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        )
        val finalScale = if (options.precision == Precision.INEXACT) {
            rawScale.coerceAtMost(1.0)
        } else {
            rawScale
        }
        return Size(
            (finalScale * srcWidth).roundToInt(),
            (finalScale * srcHeight).roundToInt(),
        )
    }

    class Factory(
        private val thumbnailStrategy: () -> ThumbnailStrategy,
    ) : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result.mimeType)) return null
            return VideoThumbnailDecoder(
                source = result.source,
                options = options,
                strategy = thumbnailStrategy(),
                diskCache = lazy { imageLoader.diskCache },
            )
        }

        private fun isApplicable(mimeType: String?): Boolean {
            return mimeType != null && mimeType.startsWith("video/")
        }
    }
}

private inline fun <T> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> T): T {
    try {
        return block(this)
    } finally {
        // We must call 'close' on API 29+ to avoid a strict mode warning.
        if (SDK_INT >= 29) {
            close()
        } else {
            release()
        }
    }
}

/**
 * Sealed class representing different thumbnail generation strategies
 */
sealed class ThumbnailStrategy {
    data object FirstFrame : ThumbnailStrategy()
    data class FrameAtPercentage(val percentage: Float = 0.33f) : ThumbnailStrategy()
    data class Hybrid(val percentage: Float = 0.33f) : ThumbnailStrategy()
}

/**
 * Checks if a bitmap is mostly a solid color.
 * Uses a scaling approach to sample the center region and compares pixels to the center color.
 * Returns true if [threshold] (default 95%) or more of sampled pixels are similar.
 */
private fun isSolidColor(bitmap: Bitmap, threshold: Float = 0.7f): Boolean {
    val width = bitmap.width
    val height = bitmap.height

    // Sample a grid in the center region (avoiding edges which may be black bars)
    val marginX = width / 10
    val marginY = height / 10
    val sampleAreaRight = width - marginX
    val sampleAreaBottom = height - marginY

    // Create a grid of sample points
    val gridSize = 10
    val stepX = (sampleAreaRight - marginX) / gridSize
    val stepY = (sampleAreaBottom - marginY) / gridSize

    if (stepX <= 0 || stepY <= 0) return false

    val sampledColors = mutableListOf<Int>()

    for (x in 0 until gridSize) {
        for (y in 0 until gridSize) {
            val pixelX = marginX + x * stepX
            val pixelY = marginY + y * stepY
            if (pixelX < width && pixelY < height) {
                sampledColors.add(bitmap[pixelX, pixelY])
            }
        }
    }

    if (sampledColors.isEmpty()) return false

    // Use the first color as reference
    val referenceColor = sampledColors[0]
    val referenceR = (referenceColor shr 16) and 0xFF
    val referenceG = (referenceColor shr 8) and 0xFF
    val referenceB = referenceColor and 0xFF

    // Count similar colors (within a tolerance)
    val tolerance = 30 // RGB tolerance
    val similarCount = sampledColors.count { color ->
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF

        abs(r - referenceR) <= tolerance &&
                abs(g - referenceG) <= tolerance &&
                abs(b - referenceB) <= tolerance
    }

    val similarityRatio = similarCount.toFloat() / sampledColors.size
    return similarityRatio >= threshold
}
