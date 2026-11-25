package dev.roxy.kryptonplayer.core.data.reporting

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Snapshot of media playback diagnostics for bug reporting and dump logs.
 */
data class MediaDiagnosticInfo(
    val title: String = "None",
    val uriString: String = "",
    val uriScheme: String = "none",
    val isLocalSource: Boolean = false,
    val localFileSizeFormatted: String = "N/A",
    val mimeType: String = "unknown",
    val videoCodec: String = "Unknown / Direct",
    val videoResolution: String = "N/A",
    val frameRate: Float = 0f,
    val audioCodec: String = "Unknown",
    val audioChannels: Int = 0,
    val audioSampleRate: Int = 0,
    val playbackState: String = "IDLE",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastErrorCodeName: String = "",
    val lastErrorCode: Int = 0,
    val lastErrorMessage: String = "",
    val lastErrorCause: String = "",
    val httpResponseCode: Int = 0,
    val recentEvents: List<String> = emptyList(),
)

/**
 * Thread-safe collector for media playback engine state, decoder information,
 * and error diagnostics in Krypton Player.
 */
object MediaDiagnosticsTracker {

    private const val MAX_EVENTS = 60
    private val eventQueue = ConcurrentLinkedQueue<String>()

    @Volatile
    private var currentTitle: String = "None"

    @Volatile
    private var currentUri: String = ""

    @Volatile
    private var uriScheme: String = "none"

    @Volatile
    private var isLocal: Boolean = false

    @Volatile
    private var localFileSize: String = "N/A"

    @Volatile
    private var mimeType: String = "unknown"

    @Volatile
    private var videoCodec: String = "Hardware / libmpv Decoder"

    @Volatile
    private var videoResolution: String = "N/A"

    @Volatile
    private var frameRate: Float = 0f

    @Volatile
    private var audioCodec: String = "mpv AudioTrack"

    @Volatile
    private var audioChannels: Int = 0

    @Volatile
    private var audioSampleRate: Int = 0

    @Volatile
    private var playbackState: String = "IDLE"

    @Volatile
    private var isPlaying: Boolean = false

    @Volatile
    private var currentPositionMs: Long = 0L

    @Volatile
    private var currentDurationMs: Long = 0L

    @Volatile
    private var lastErrorCodeName: String = ""

    @Volatile
    private var lastErrorCode: Int = 0

    @Volatile
    private var lastErrorMessage: String = ""

    @Volatile
    private var lastErrorCause: String = ""

    @Volatile
    private var httpResponseCode: Int = 0

    fun recordEvent(event: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = "[$time] $event"
        eventQueue.offer(entry)
        while (eventQueue.size > MAX_EVENTS) {
            eventQueue.poll()
        }
    }

    fun onPlaybackStart(
        title: String,
        uri: String,
        mime: String? = null,
        isLocalFile: Boolean = false,
        fileSizeFormatted: String = "N/A",
    ) {
        currentTitle = title
        currentUri = LogSanitizer.sanitize(uri)
        uriScheme = try {
            android.net.Uri.parse(uri).scheme ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
        mimeType = mime ?: "video/*"
        isLocal = isLocalFile
        localFileSize = fileSizeFormatted
        lastErrorCodeName = ""
        lastErrorCode = 0
        lastErrorMessage = ""
        lastErrorCause = ""
        httpResponseCode = 0
        recordEvent("START: '$title' (Scheme: $uriScheme, Local: $isLocal)")
    }

    fun onVideoFormatChanged(
        codec: String?,
        width: Int,
        height: Int,
        fps: Float = 0f,
    ) {
        if (!codec.isNullOrBlank()) videoCodec = codec
        if (width > 0 && height > 0) videoResolution = "${width}x${height}"
        if (fps > 0f) frameRate = fps
        recordEvent("VIDEO_FORMAT: codec=$videoCodec, res=$videoResolution, fps=$frameRate")
    }

    fun onAudioFormatChanged(
        codec: String?,
        channels: Int,
        sampleRate: Int,
    ) {
        if (!codec.isNullOrBlank()) audioCodec = codec
        if (channels > 0) audioChannels = channels
        if (sampleRate > 0) audioSampleRate = sampleRate
        recordEvent("AUDIO_FORMAT: codec=$audioCodec, channels=$audioChannels, rate=${audioSampleRate}Hz")
    }

    fun onPlaybackStateChanged(
        state: String,
        playing: Boolean,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
    ) {
        playbackState = state
        isPlaying = playing
        currentPositionMs = positionMs
        currentDurationMs = durationMs
        recordEvent("STATE: $state, isPlaying=$playing, pos=${positionMs / 1000}s/${durationMs / 1000}s")
    }

    fun onPlaybackError(
        errorCodeName: String,
        errorCode: Int,
        message: String?,
        cause: Throwable?,
        httpCode: Int = 0,
    ) {
        lastErrorCodeName = errorCodeName
        this.lastErrorCode = errorCode
        lastErrorMessage = message ?: "Unknown playback error"
        lastErrorCause = cause?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: "None"
        httpResponseCode = httpCode
        recordEvent("ERROR: code=$errorCodeName ($errorCode), http=$httpCode, msg=$lastErrorMessage")
    }

    fun getDiagnostics(): MediaDiagnosticInfo {
        return MediaDiagnosticInfo(
            title = currentTitle,
            uriString = currentUri,
            uriScheme = uriScheme,
            isLocalSource = isLocal,
            localFileSizeFormatted = localFileSize,
            mimeType = mimeType,
            videoCodec = videoCodec,
            videoResolution = videoResolution,
            frameRate = frameRate,
            audioCodec = audioCodec,
            audioChannels = audioChannels,
            audioSampleRate = audioSampleRate,
            playbackState = playbackState,
            isPlaying = isPlaying,
            positionMs = currentPositionMs,
            durationMs = currentDurationMs,
            lastErrorCodeName = lastErrorCodeName,
            lastErrorCode = lastErrorCode,
            lastErrorMessage = lastErrorMessage,
            lastErrorCause = lastErrorCause,
            httpResponseCode = httpResponseCode,
            recentEvents = eventQueue.toList(),
        )
    }

    fun formatDiagnosticsMarkdown(): String {
        val info = getDiagnostics()
        return buildString {
            appendLine("- **Active Media**: ${info.title}")
            appendLine("- **Source Type**: ${if (info.isLocalSource) "Local / Offline Media" else "Online Stream (${info.uriScheme})' "}")
            if (info.isLocalSource) {
                appendLine("- **File Size**: ${info.localFileSizeFormatted}")
            }
            appendLine("- **MIME Type**: ${info.mimeType}")
            appendLine("- **Playback State**: ${info.playbackState} (isPlaying=${info.isPlaying})")
            appendLine("- **Video Decoder**: ${info.videoCodec} (${info.videoResolution} @ ${info.frameRate}fps)")
            appendLine("- **Audio Output**: ${info.audioCodec} (${info.audioChannels}ch, ${info.audioSampleRate}Hz)")
            if (info.lastErrorCodeName.isNotBlank() || info.lastErrorMessage.isNotBlank()) {
                appendLine("- **Last Playback Error**: ${info.lastErrorCodeName} (Code: ${info.lastErrorCode})")
                appendLine("- **Error Message**: ${info.lastErrorMessage}")
                if (info.httpResponseCode > 0) {
                    appendLine("- **HTTP Status Code**: ${info.httpResponseCode}")
                }
                if (info.lastErrorCause.isNotBlank() && info.lastErrorCause != "None") {
                    appendLine("- **Root Cause**: ${info.lastErrorCause}")
                }
            }
        }.trimEnd()
    }
}
