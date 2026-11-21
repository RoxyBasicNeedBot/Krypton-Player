package dev.roxy.kryptonplayer.feature.player.service

import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource.MediaPeriodId
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.BandwidthMeter
import dev.roxy.kryptonplayer.core.common.Logger

/**
 * Wraps every [ExoTrackSelection] produced by [DefaultTrackSelector] so that
 * [ExoTrackSelection.isTrackExcluded] and [ExoTrackSelection.excludeTrack] guard against
 * out-of-bounds track indices.
 *
 * Mitigates a Jetpack Media3 1.10.0 / 1.10.1 regression in
 * `HlsChunkSource.createFallbackOptions`, which iterates over HLS redundant/variant groups
 * and calls `BaseTrackSelection.isTrackExcluded(i, nowMs)` even when the active
 * [ExoTrackSelection] has fewer tracks (`length() < redundantGroups.length`), throwing a
 * fatal [ArrayIndexOutOfBoundsException] during HLS chunk or playlist load errors.
 */
@UnstableApi
internal class BoundsSafeTrackSelectionFactory(
    private val delegate: ExoTrackSelection.Factory = AdaptiveTrackSelection.Factory(),
) : ExoTrackSelection.Factory {

    override fun createTrackSelections(
        definitions: Array<ExoTrackSelection.Definition?>,
        bandwidthMeter: BandwidthMeter,
        mediaPeriodId: MediaPeriodId,
        timeline: Timeline,
    ): Array<ExoTrackSelection?> {
        val selections = delegate.createTrackSelections(
            definitions,
            bandwidthMeter,
            mediaPeriodId,
            timeline,
        )
        for (i in selections.indices) {
            val selection = selections[i]
            if (selection != null && selection !is BoundsSafeExoTrackSelection) {
                selections[i] = BoundsSafeExoTrackSelection(selection)
            }
        }
        return selections
    }
}

@UnstableApi
private class BoundsSafeExoTrackSelection(
    private val delegate: ExoTrackSelection,
) : ExoTrackSelection by delegate {

    override fun isTrackExcluded(index: Int, nowMs: Long): Boolean {
        val trackCount = delegate.length()
        if (index !in 0 until trackCount) {
            Logger.logDebug(
                "BoundsSafeTrackSelection",
                "Guarded out-of-bounds isTrackExcluded(index=$index, length=$trackCount)",
            )
            // Treat indices outside the active track selection as excluded so
            // HlsChunkSource.createFallbackOptions computes the accurate remaining
            // selectable fallback count (numberOfTracks - numberOfExcludedTracks).
            return true
        }
        return delegate.isTrackExcluded(index, nowMs)
    }

    override fun excludeTrack(index: Int, exclusionDurationMs: Long): Boolean {
        val trackCount = delegate.length()
        if (index !in 0 until trackCount) {
            Logger.logDebug(
                "BoundsSafeTrackSelection",
                "Ignored out-of-bounds excludeTrack(index=$index, length=$trackCount)",
            )
            return false
        }
        return delegate.excludeTrack(index, exclusionDurationMs)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoundsSafeExoTrackSelection) return false
        return delegate == other.delegate
    }

    override fun hashCode(): Int = delegate.hashCode()
}
