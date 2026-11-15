package dev.anilbeesetti.nextplayer.feature.player.extractor

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.mp4.ResilientMp4Extractor

/**
 * ResilientExtractorsFactory wraps DefaultExtractorsFactory to substitute stock Mp4Extractor
 * with ResilientMp4Extractor and MatroskaExtractor with ResilientMatroskaExtractor.
 *
 * This prevents fatal container crashes in multi-track MP4s and enables smooth seeking
 * in unindexed MKV/WebM files (matching VLC media player behavior).
 */
@OptIn(UnstableApi::class)
class ResilientExtractorsFactory(
    private val constantBitrateSeekingEnabled: Boolean = true,
    private val constantBitrateSeekingAlwaysEnabled: Boolean = true,
) : ExtractorsFactory {

    private val defaultFactory = DefaultExtractorsFactory().apply {
        setConstantBitrateSeekingEnabled(constantBitrateSeekingEnabled)
        setConstantBitrateSeekingAlwaysEnabled(constantBitrateSeekingAlwaysEnabled)
        setMp4ExtractorFlags(
            Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS or
                Mp4Extractor.FLAG_READ_SEF_DATA
        )
        setFragmentedMp4ExtractorFlags(
            FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_TFDT_BOX or
                FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS
        )
    }

    override fun createExtractors(): Array<Extractor> {
        return createExtractors(Uri.EMPTY, emptyMap())
    }

    override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> {
        val baseExtractors = defaultFactory.createExtractors(uri, responseHeaders)
        val result = mutableListOf<Extractor>()

        val uriString = uri.toString().lowercase()
        val isMp4Like = uriString.endsWith(".mp4") || uriString.endsWith(".m4v") || uriString.contains("offline_media")
        val isMkvLike = uriString.endsWith(".mkv") || uriString.endsWith(".webm") ||
            uriString.contains(".mkv.") || uriString.contains(".webm.") ||
            uriString.contains("mkv") || uriString.contains("webm")

        for (extractor in baseExtractors) {
            when (extractor) {
                is Mp4Extractor -> {
                    result.add(
                        ResilientMp4Extractor(
                            Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS or
                                Mp4Extractor.FLAG_READ_SEF_DATA
                        )
                    )
                }
                is MatroskaExtractor -> {
                    result.add(ResilientMatroskaExtractor(extractor, uri))
                }
                else -> {
                    result.add(extractor)
                }
            }
        }

        if (isMkvLike) {
            val mkvIndex = result.indexOfFirst { it is ResilientMatroskaExtractor }
            if (mkvIndex > 0) {
                val mkvExtractor = result.removeAt(mkvIndex)
                result.add(0, mkvExtractor)
            } else if (mkvIndex == -1) {
                result.add(0, ResilientMatroskaExtractor(MatroskaExtractor(), uri))
            }
        } else if (isMp4Like) {
            val mp4Index = result.indexOfFirst { it is ResilientMp4Extractor }
            if (mp4Index > 0) {
                val mp4Extractor = result.removeAt(mp4Index)
                result.add(0, mp4Extractor)
            } else if (mp4Index == -1) {
                result.add(
                    0,
                    ResilientMp4Extractor(
                        Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS or
                            Mp4Extractor.FLAG_READ_SEF_DATA
                    )
                )
            }
        }

        return result.toTypedArray()
    }
}
