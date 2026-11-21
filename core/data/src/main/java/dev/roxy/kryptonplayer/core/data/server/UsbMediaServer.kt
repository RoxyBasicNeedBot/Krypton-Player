package dev.roxy.kryptonplayer.core.data.server

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.roxy.kryptonplayer.core.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.net.wifi.WifiManager
import android.os.PowerManager
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpcomingMediaItem(
    val title: String,
    val uriString: String,
    val durationText: String = ""
)

data class SoundtrackSongItem(
    val title: String,
    val artist: String,
    val previewUrl: String? = null,
    val coverUrl: String? = null
)

/**
 * UsbMediaServer: Lightweight, high-throughput HTTP server for USB-tethered and local network streaming.
 * Provides a dedicated Vanta-style Web Cinema Player UI with upcoming playlist queue, soundtracks,
 * animated Lottie stickers, and reliable multi-mode VLC Player launching.
 */
@Singleton
class UsbMediaServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbConnectionHelper: UsbConnectionHelper
) {
    companion object {
        private const val TAG = "UsbMediaServer"
        const val PORT = 8080
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val serverScope = CoroutineScope(Dispatchers.IO)

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val wakeLock = powerManager?.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "KryptonPlayer:UsbMediaServerWakeLock"
    )?.apply {
        setReferenceCounted(false)
    }

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val wifiLock = wifiManager?.createWifiLock(
        WifiManager.WIFI_MODE_FULL_HIGH_PERF,
        "KryptonPlayer:UsbMediaServerWifiLock"
    )?.apply {
        setReferenceCounted(false)
    }

    private fun acquireLocks() {
        try {
            if (wakeLock?.isHeld != true) wakeLock?.acquire()
        } catch (_: Exception) {}
        try {
            if (wifiLock?.isHeld != true) wifiLock?.acquire()
        } catch (_: Exception) {}
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Exception) {}
    }

    // 4-Digit Pairing PIN for fast PC connect
    var pairingPin: String = (1000..9999).random().toString()
        private set

    // Current streaming media state
    var activeVideoUri: Uri? = null
    var activeVideoTitle: String = "Krypton Player Video"
    var activeSubtitleFile: File? = null
    var activePositionMs: Long = 0L
    var isPlaying: Boolean = true

    // Playlist & Soundtrack queues
    var upcomingList: List<UpcomingMediaItem> = emptyList()
    var soundtrackList: List<SoundtrackSongItem> = emptyList()

    // Remote control callbacks
    var onRemotePlayPause: (() -> Unit)? = null
    var onRemoteSeek: ((Long) -> Unit)? = null
    var onRemoteSelectVideo: ((String) -> Unit)? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount = _connectedClientsCount.asStateFlow()

    fun startServer(
        videoUri: Uri,
        title: String,
        subtitleFile: File? = null,
        initialPositionMs: Long = 0L,
        playlist: List<Video> = emptyList()
    ) {
        activeVideoUri = videoUri
        activeVideoTitle = title
        activeSubtitleFile = subtitleFile
        activePositionMs = initialPositionMs
        pairingPin = (1000..9999).random().toString()

        if (playlist.isNotEmpty()) {
            upcomingList = playlist.map {
                UpcomingMediaItem(
                    title = it.displayName,
                    uriString = it.uriString,
                    durationText = it.formattedDuration
                )
            }
        }

        acquireLocks()
        val primaryUrl = getAvailableStreamingUrls().firstOrNull()?.browserUrl ?: "http://localhost:$PORT"
        UsbCinemaService.start(context, title, pairingPin, primaryUrl)

        if (_isServerRunning.value) {
            Log.i(TAG, "USB Media Server already running; updated active media: $title ($videoUri)")
            return
        }

        serverJob = serverScope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                _isServerRunning.value = true
                Log.i(TAG, "[USB SERVER] Started on port $PORT (PIN: $pairingPin, Title: $title, URI: $videoUri)")

                while (_isServerRunning.value && serverSocket != null && !serverSocket!!.isClosed) {
                    val clientSocket = serverSocket!!.accept()
                    serverScope.launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (_isServerRunning.value) {
                    Log.e(TAG, "Server error", e)
                }
            } finally {
                stopServer()
            }
        }
    }

    fun stopServer() {
        _isServerRunning.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        _connectedClientsCount.value = 0
        releaseLocks()
        UsbCinemaService.stop(context)
        Log.i(TAG, "[USB SERVER] Stopped")
    }

    fun getAvailableStreamingUrls(): List<UsbConnectionHelper.UsbIpInfo> {
        return usbConnectionHelper.getAvailableStreamingUrls(PORT)
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.soTimeout = 0 // Never terminate continuous streaming connections
            socket.sendBufferSize = 256 * 1024
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            val reader = input.bufferedReader()
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            val headers = mutableMapOf<String, String>()
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
                line = reader.readLine()
            }

            val clientHost = headers["host"] ?: "localhost:$PORT"

            if (method == "OPTIONS") {
                sendCorsOk(output)
                return
            }

            when {
                path == "/" || path.startsWith("/player") -> {
                    serveWebCinemaPlayer(output, clientHost, method)
                }
                path == "/vlc" || path == "/vlc.m3u" || path == "/playlist.m3u" || path == "/play.m3u" || path == "/cinema.m3u" -> {
                    serveVlcPlaylist(output, clientHost, method)
                }
                path.startsWith("/stream/video") || path.startsWith("/video") -> {
                    serveVideoStream(output, method, headers["range"])
                }
                path.startsWith("/stream/subtitle") -> {
                    serveSubtitle(output, method)
                }
                path == "/api/status" -> {
                    serveStatusJson(output, method)
                }
                path == "/api/playlist" -> {
                    servePlaylistJson(output, method)
                }
                path == "/api/songs" -> {
                    serveSongsJson(output, method)
                }
                path.startsWith("/stickers/") -> {
                    serveStickerJson(path.removePrefix("/stickers/"), output)
                }
                path.startsWith("/api/command") -> {
                    handleCommand(path, output)
                }
                else -> {
                    send404(output)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Client socket handled/closed: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun serveVlcPlaylist(out: OutputStream, clientHost: String, method: String) {
        val host = clientHost.substringBefore(":")
        val port = clientHost.substringAfter(":", PORT.toString()).toIntOrNull() ?: PORT
        val m3u = """
            #EXTM3U
            #EXTINF:-1,$activeVideoTitle
            http://$host:$port/stream/video
        """.trimIndent()
        val bytes = m3u.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/x-mpegurl; charset=utf-8\r\n" +
                "Content-Disposition: attachment; filename=\"cinema.m3u\"\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        if (method != "HEAD") {
            out.write(bytes)
        }
        out.flush()
    }

    private fun serveStickerJson(stickerName: String, out: OutputStream) {
        try {
            val cleanName = stickerName.removeSuffix(".json").lowercase().replace("-", "_")
            val resId = context.resources.getIdentifier(cleanName, "raw", context.packageName)
            if (resId != 0) {
                val bytes = context.resources.openRawResource(resId).use { it.readBytes() }
                val header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Cache-Control: public, max-age=86400\r\n" +
                        "Connection: close\r\n\r\n"
                out.write(header.toByteArray(Charsets.UTF_8))
                out.write(bytes)
                out.flush()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        send404(out)
    }

    private fun servePlaylistJson(out: OutputStream, method: String) {
        val jsonList = upcomingList.joinToString(prefix = "[", postfix = "]") { item ->
            """{"title":${escapeJson(item.title)},"uri":${escapeJson(item.uriString)},"duration":${escapeJson(item.durationText)}}"""
        }
        val bytes = jsonList.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        if (method != "HEAD") out.write(bytes)
        out.flush()
    }

    private fun serveSongsJson(out: OutputStream, method: String) {
        val jsonList = soundtrackList.joinToString(prefix = "[", postfix = "]") { item ->
            """{"title":${escapeJson(item.title)},"artist":${escapeJson(item.artist)},"previewUrl":${escapeJson(item.previewUrl ?: "")},"coverUrl":${escapeJson(item.coverUrl ?: "")}}"""
        }
        val bytes = jsonList.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        if (method != "HEAD") out.write(bytes)
        out.flush()
    }

    private fun escapeJson(s: String): String {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\""
    }

    private fun serveWebCinemaPlayer(out: OutputStream, clientHost: String, method: String) {
        val hasSubtitles = activeSubtitleFile != null && activeSubtitleFile!!.exists()
        val subTrackTag = if (hasSubtitles) """<track label="English" kind="subtitles" srclang="en" src="/stream/subtitle" default>""" else ""
        val host = clientHost.substringBefore(":")
        val port = clientHost.substringAfter(":", PORT.toString()).toIntOrNull() ?: PORT
        val vlcStreamUrl = "http://$host:$port/stream/video"
        val m3uDownloadUrl = "http://$host:$port/vlc.m3u"
        val vlcProtocolUrl = "vlc://$vlcStreamUrl"
        val potplayerProtocolUrl = "potplayer://$vlcStreamUrl"

        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Krypton Player USB Cinema - $activeVideoTitle</title>
                <!-- Tailwind & Lottie Web Player -->
                <script src="https://cdn.tailwindcss.com"></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/lottie-web/5.12.2/lottie.min.js"></script>
                <style>
                    * { box-sizing: border-box; }
                    body {
                        background: radial-gradient(circle at 50% 0%, #1e1b4b 0%, #090d16 60%, #030712 100%);
                        color: #f8fafc;
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        min-height: 100vh;
                    }
                    .glass-card {
                        background: rgba(15, 23, 42, 0.75);
                        backdrop-filter: blur(16px);
                        border: 1px solid rgba(255, 255, 255, 0.1);
                    }
                    .custom-seekbar {
                        -webkit-appearance: none;
                        appearance: none;
                        width: 100%;
                        height: 6px;
                        border-radius: 9999px;
                        background: rgba(255, 255, 255, 0.2);
                        outline: none;
                        cursor: pointer;
                        transition: height 0.15s ease;
                    }
                    .custom-seekbar:hover { height: 10px; }
                    .custom-seekbar::-webkit-slider-thumb {
                        -webkit-appearance: none;
                        appearance: none;
                        width: 14px;
                        height: 14px;
                        border-radius: 50%;
                        background: #38bdf8;
                        box-shadow: 0 0 10px #38bdf8;
                        cursor: pointer;
                    }
                    .custom-volume {
                        -webkit-appearance: none;
                        appearance: none;
                        width: 70px;
                        height: 4px;
                        border-radius: 9999px;
                        background: rgba(255, 255, 255, 0.3);
                        outline: none;
                    }
                    .custom-volume::-webkit-slider-thumb {
                        -webkit-appearance: none;
                        width: 10px;
                        height: 10px;
                        border-radius: 50%;
                        background: #fff;
                    }
                    .double-tap-indicator {
                        position: absolute;
                        top: 50%;
                        transform: translateY(-50%);
                        pointer-events: none;
                        opacity: 0;
                        transition: opacity 0.25s ease;
                    }
                    .toast-popup {
                        position: fixed;
                        bottom: 24px;
                        right: 24px;
                        background: rgba(15, 23, 42, 0.9);
                        backdrop-filter: blur(12px);
                        border: 1px solid rgba(56, 189, 248, 0.4);
                        color: #38bdf8;
                        padding: 12px 20px;
                        border-radius: 14px;
                        box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.6);
                        font-weight: 600;
                        font-size: 13px;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        z-index: 100;
                        opacity: 0;
                        transform: translateY(10px);
                        transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
                        pointer-events: none;
                    }
                    .toast-popup.show {
                        opacity: 1;
                        transform: translateY(0);
                    }
                </style>
            </head>
            <body class="p-4 md:p-6 flex flex-col items-center">
                <!-- Top Header Navigation -->
                <header class="w-full max-w-6xl flex items-center justify-between gap-4 mb-4 flex-wrap">
                    <div class="flex items-center gap-3">
                        <div id="logoSticker" class="w-10 h-10"></div>
                        <div>
                            <h1 class="text-xl md:text-2xl font-extrabold bg-gradient-to-r from-sky-400 via-indigo-300 to-purple-400 bg-clip-text text-transparent">
                                Krypton Player USB Cinema
                            </h1>
                            <p class="text-xs text-slate-400">Direct High-Speed USB & LAN Cast</p>
                        </div>
                    </div>
                    <div class="flex items-center gap-2 flex-wrap">
                        <!-- Direct Launch VLC Button -->
                        <button onclick="launchVlcDirectly()" class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-gradient-to-r from-amber-500 to-orange-600 text-white text-xs font-bold shadow-lg hover:scale-105 transition cursor-pointer">
                            <svg class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3"/></svg> Open in VLC Player
                        </button>
                        <!-- Copy Stream URL Button -->
                        <button onclick="copyStreamUrl()" class="inline-flex items-center gap-1.5 px-3 py-2 rounded-full bg-white/10 hover:bg-white/20 border border-white/10 text-white text-xs font-semibold transition cursor-pointer" title="Copy Stream URL for VLC (Ctrl+N)">
                            <svg class="w-3.5 h-3.5 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><rect width="8" height="4" x="8" y="2" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg> Copy URL
                        </button>
                        <div class="px-3 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold flex items-center gap-2">
                            <span class="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span> Live Connected
                        </div>
                    </div>
                </header>

                <!-- Toast Notification Container -->
                <div id="toast" class="toast-popup">
                    <span id="toastMsg" class="flex items-center gap-1.5">
                        <svg class="w-4 h-4 stroke-current stroke-2 fill-none text-emerald-400" viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg>
                        Copied to clipboard!
                    </span>
                </div>

                <!-- VLC Launch Modal Dialog -->
                <div id="vlcModal" class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 hidden items-center justify-center p-4">
                    <div class="glass-card max-w-md w-full rounded-2xl p-6 border border-white/20 shadow-2xl flex flex-col gap-4">
                        <div class="flex items-center justify-between">
                            <div class="flex items-center gap-2">
                                <div class="w-7 h-7 rounded-lg bg-sky-500/20 text-sky-400 flex items-center justify-center">
                                    <svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><rect width="20" height="20" x="2" y="2" rx="2.18"/><path d="M7 2v20M17 2v20M2 12h20M2 7h5M2 17h5M17 17h5M17 7h5"/></svg>
                                </div>
                                <h3 class="font-extrabold text-base text-white">Opening in External Player</h3>
                            </div>
                            <button onclick="closeVlcModal()" class="text-slate-400 hover:text-white p-1">
                                <svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                            </button>
                        </div>
                        
                        <p class="text-xs text-slate-300">
                            Choose how you would like to play <b>$activeVideoTitle</b> on your PC:
                        </p>

                        <div class="flex flex-col gap-2.5">
                            <a href="$vlcProtocolUrl" class="p-3 rounded-xl bg-orange-600/20 hover:bg-orange-600/30 border border-orange-500/40 text-orange-400 hover:text-orange-300 font-bold text-xs flex items-center justify-between transition">
                                <div class="flex items-center gap-2.5">
                                    <svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><path d="M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z"/><path d="m12 15-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z"/></svg>
                                    <span>Launch VLC Player Directly (<code class="text-[11px]">vlc://</code>)</span>
                                </div>
                                <span class="text-slate-400 text-[10px]">App URI</span>
                            </a>

                            <a href="$m3uDownloadUrl" class="p-3 rounded-xl bg-sky-500/20 hover:bg-sky-500/30 border border-sky-500/40 text-sky-400 hover:text-sky-300 font-bold text-xs flex items-center justify-between transition">
                                <div class="flex items-center gap-2.5">
                                    <svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                                    <span>Download Playlist File (<code class="text-[11px]">cinema.m3u</code>)</span>
                                </div>
                                <span class="text-slate-400 text-[10px]">M3U File</span>
                            </a>

                            <a href="$potplayerProtocolUrl" class="p-3 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 border border-amber-500/40 text-amber-400 hover:text-amber-300 font-bold text-xs flex items-center justify-between transition">
                                <div class="flex items-center gap-2.5">
                                    <svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                                    <span>Launch in PotPlayer</span>
                                </div>
                                <span class="text-slate-400 text-[10px]">PotPlayer</span>
                            </a>

                            <button onclick="copyStreamUrl()" class="p-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-white font-bold text-xs flex items-center justify-between transition cursor-pointer text-left">
                                <div class="flex items-center gap-2.5">
                                    <svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><rect width="8" height="4" x="8" y="2" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
                                    <span>Copy Stream Link (Paste in VLC <kbd class="bg-black/50 px-1 py-0.5 rounded text-[10px]">Ctrl+N</kbd>)</span>
                                </div>
                                <span class="text-sky-400 text-[10px]">Copy</span>
                            </button>
                        </div>

                        <div class="bg-black/40 p-3 rounded-xl text-[11px] text-slate-400 flex flex-col gap-1 border border-white/5 font-mono">
                            <span class="text-slate-500 text-[10px]">Direct Stream URL:</span>
                            <span class="text-sky-400 select-all break-all">$vlcStreamUrl</span>
                        </div>
                    </div>
                </div>

                <!-- Main Cinema Grid: Player + Side Drawer -->
                <main class="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
                    <!-- Left: Dedicated WebPlayer Card (Span 2) -->
                    <div class="lg:col-span-2 flex flex-col gap-4">
                        <div id="playerContainer" class="relative group w-full rounded-2xl overflow-hidden glass-card shadow-2xl bg-black border border-white/10 select-none">
                            <video id="vPlayer" class="w-full max-h-[70vh] aspect-video object-contain bg-black cursor-pointer" autoplay preload="auto">
                                <source src="/stream/video">
                                $subTrackTag
                                Your browser does not support HTML5 video streaming.
                            </video>

                            <!-- Double Tap Left/Right Seek Indicators -->
                            <div id="seekLeft" class="double-tap-indicator left-8 flex flex-col items-center text-white bg-black/60 px-4 py-3 rounded-2xl backdrop-blur">
                                <span class="flex items-center gap-1.5 text-2xl font-bold">
                                    <svg class="w-6 h-6 fill-current" viewBox="0 0 24 24"><polygon points="11 19 2 12 11 5 11 19"/><polygon points="22 19 13 12 22 5 22 19"/></svg>
                                    -5s
                                </span>
                            </div>
                            <div id="seekRight" class="double-tap-indicator right-8 flex flex-col items-center text-white bg-black/60 px-4 py-3 rounded-2xl backdrop-blur">
                                <span class="flex items-center gap-1.5 text-2xl font-bold">
                                    +5s
                                    <svg class="w-6 h-6 fill-current" viewBox="0 0 24 24"><polygon points="13 19 22 12 13 5 13 19"/><polygon points="2 19 11 12 2 5 2 19"/></svg>
                                </span>
                            </div>

                            <!-- Dedicated Control Bar (Auto Hide) -->
                            <div id="controlBar" class="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/95 via-black/60 to-transparent p-4 flex flex-col gap-2 transition-opacity duration-300 opacity-100">
                                <!-- Top Seekbar with Hover Time Tooltip -->
                                <div class="relative w-full flex items-center">
                                    <input type="range" id="seekBar" class="custom-seekbar" min="0" max="100" value="0" step="0.1">
                                </div>

                                <!-- Action Buttons Row -->
                                <div class="flex items-center justify-between text-white text-sm">
                                    <div class="flex items-center gap-3">
                                        <!-- Play/Pause Button -->
                                        <button id="btnPlay" class="p-2 rounded-full hover:bg-white/20 transition flex items-center justify-center">
                                            <svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3"/></svg>
                                        </button>
                                        
                                        <!-- Volume Slider -->
                                        <div class="flex items-center gap-2">
                                            <button id="btnMute" class="p-1.5 rounded-full hover:bg-white/10 text-slate-300 hover:text-white flex items-center justify-center">
                                                <svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>
                                            </button>
                                            <input type="range" id="volBar" class="custom-volume" min="0" max="1" step="0.05" value="1">
                                        </div>

                                        <!-- Time Display -->
                                        <span id="timeText" class="text-xs font-mono text-slate-300">00:00 / 00:00</span>
                                    </div>

                                    <div class="flex items-center gap-1">
                                        <!-- Playback Speed -->
                                        <select id="speedSelect" class="bg-white/10 border border-white/20 text-xs rounded-lg px-2 py-1 text-slate-200 outline-none cursor-pointer">
                                            <option value="0.5">0.5x</option>
                                            <option value="0.75">0.75x</option>
                                            <option value="1.0" selected>1.0x</option>
                                            <option value="1.25">1.25x</option>
                                            <option value="1.5">1.5x</option>
                                            <option value="2.0">2.0x</option>
                                        </select>

                                        <!-- Screenshot Button -->
                                        <button id="btnScreenshot" class="p-2 rounded-full hover:bg-white/10 text-slate-300 hover:text-white" title="Take Screenshot (S)">
                                            <svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/></svg>
                                        </button>

                                        <!-- PiP Button -->
                                        <button id="btnPip" class="p-2 rounded-full hover:bg-white/10 text-slate-300 hover:text-white" title="Picture in Picture (P)">
                                            <svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><rect width="20" height="14" x="2" y="3" rx="2"/><rect width="8" height="6" x="12" y="9" rx="1"/></svg>
                                        </button>

                                        <!-- Fullscreen Button -->
                                        <button id="btnFullscreen" class="p-2 rounded-full hover:bg-white/10 text-slate-300 hover:text-white" title="Fullscreen (F)">
                                            <svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Active Video Info Bar -->
                        <div class="glass-card p-4 rounded-2xl flex items-center justify-between gap-3">
                            <div class="flex items-center gap-3">
                                <div class="w-10 h-10 rounded-xl bg-sky-500/20 text-sky-400 flex items-center justify-center">
                                    <svg class="w-5 h-5 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><rect width="20" height="20" x="2" y="2" rx="2.18"/><path d="M7 2v20M17 2v20M2 12h20M2 7h5M2 17h5M17 17h5M17 7h5"/></svg>
                                </div>
                                <div>
                                    <h2 class="font-bold text-base text-white truncate max-w-md">$activeVideoTitle</h2>
                                    <p class="text-xs text-slate-400">Streaming via Arch USB Protocol</p>
                                </div>
                            </div>
                            <div id="heartSticker" class="w-10 h-10 cursor-pointer" title="Like"></div>
                        </div>

                        <!-- VLC Direct Stream Link Box -->
                        <div class="glass-card p-3 rounded-xl flex items-center justify-between text-xs text-slate-400 gap-2">
                            <span class="flex items-center gap-1.5">
                                <svg class="w-4 h-4 stroke-current stroke-2 fill-none text-sky-400" viewBox="0 0 24 24"><path d="M4.9 19.1C1 15.2 1 8.8 4.9 4.9M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5M12 12h.01"/></svg>
                                <b>VLC Stream URL:</b>
                            </span>
                            <code class="bg-black/60 text-sky-400 px-3 py-1 rounded-lg select-all font-mono cursor-pointer" onclick="copyStreamUrl()">$vlcStreamUrl</code>
                        </div>
                    </div>

                    <!-- Right Side: Tabbed Drawer (Upcoming Playlist & Soundtracks) -->
                    <div class="flex flex-col gap-4">
                        <div class="glass-card rounded-2xl p-4 flex flex-col gap-3">
                            <!-- Tabs Header -->
                            <div class="flex rounded-xl bg-black/40 p-1 border border-white/10">
                                <button id="tabPlaylist" class="flex-1 py-1.5 text-xs font-bold rounded-lg bg-sky-500 text-white transition flex items-center justify-center gap-1.5">
                                    <svg class="w-3.5 h-3.5 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><rect width="8" height="4" x="8" y="2" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
                                    Upcoming Videos
                                </button>
                                <button id="tabSongs" class="flex-1 py-1.5 text-xs font-bold rounded-lg text-slate-400 hover:text-white transition flex items-center justify-center gap-1.5">
                                    <svg class="w-3.5 h-3.5 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
                                    Soundtracks
                                </button>
                            </div>

                            <!-- Playlist Queue Content -->
                            <div id="playlistContent" class="flex flex-col gap-2 max-h-[380px] overflow-y-auto pr-1">
                                <div class="flex items-center justify-between text-xs text-slate-400 mb-1">
                                    <span>Now Playing & Queue</span>
                                    <span id="queueCountBadge" class="px-2 py-0.5 rounded-full bg-white/10 text-white">0 items</span>
                                </div>
                                <div id="playlistItems" class="flex flex-col gap-2">
                                    <!-- Dynamic Playlist Items will be loaded here -->
                                    <div class="p-3 rounded-xl bg-sky-500/10 border border-sky-500/30 flex items-center gap-3">
                                        <span class="w-6 h-6 rounded-md bg-sky-500/20 text-sky-400 flex items-center justify-center">
                                            <svg class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3"/></svg>
                                        </span>
                                        <div class="flex-1 min-w-0">
                                            <p class="text-xs font-bold text-white truncate">$activeVideoTitle</p>
                                            <p class="text-[10px] text-sky-400">Current Video</p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Soundtracks Content -->
                            <div id="songsContent" class="hidden flex flex-col gap-2 max-h-[380px] overflow-y-auto pr-1">
                                <div class="flex items-center justify-between text-xs text-slate-400 mb-1">
                                    <span>Official Movie OSTs</span>
                                    <span class="text-[10px] text-indigo-400">Apple & Deezer</span>
                                </div>
                                <div id="songItems" class="flex flex-col gap-2">
                                    <p class="text-xs text-slate-400 text-center py-6">Search or play media to load OST tracks</p>
                                </div>
                            </div>
                        </div>

                        <!-- Keyboard Shortcuts Card -->
                        <div class="glass-card p-4 rounded-2xl flex flex-col gap-2 text-xs text-slate-400">
                            <h3 class="font-bold text-slate-200 flex items-center gap-1.5">
                                <svg class="w-4 h-4 stroke-current stroke-2 fill-none text-slate-400" viewBox="0 0 24 24"><rect width="20" height="14" x="2" y="4" rx="2"/><line x1="6" x2="6" y1="8" y2="8"/><line x1="10" x2="10" y1="8" y2="8"/><line x1="14" x2="14" y1="8" y2="8"/><line x1="18" x2="18" y1="8" y2="8"/><line x1="7" x2="17" y1="14" y2="14"/></svg>
                                Keyboard Shortcuts
                            </h3>
                            <div class="grid grid-cols-2 gap-2 text-[11px]">
                                <div><kbd class="bg-black/50 px-1.5 py-0.5 rounded border border-white/10 text-slate-200">Space</kbd> Play/Pause</div>
                                <div><kbd class="bg-black/50 px-1.5 py-0.5 rounded border border-white/10 text-slate-200">← / →</kbd> Seek 5s</div>
                                <div><kbd class="bg-black/50 px-1.5 py-0.5 rounded border border-white/10 text-slate-200">F</kbd> Fullscreen</div>
                                <div><kbd class="bg-black/50 px-1.5 py-0.5 rounded border border-white/10 text-slate-200">M</kbd> Mute</div>
                                <div><kbd class="bg-black/50 px-1.5 py-0.5 rounded border border-white/10 text-slate-200">P</kbd> Picture in Picture</div>
                                <div><kbd class="bg-black/50 px-1.5 py-0.5 rounded border border-white/10 text-slate-200">S</kbd> Screenshot</div>
                            </div>
                        </div>
                    </div>
                </main>

                <script>
                    var video = document.getElementById('vPlayer');
                    var btnPlay = document.getElementById('btnPlay');
                    var btnMute = document.getElementById('btnMute');
                    var volBar = document.getElementById('volBar');
                    var seekBar = document.getElementById('seekBar');
                    var timeText = document.getElementById('timeText');
                    var speedSelect = document.getElementById('speedSelect');
                    var btnPip = document.getElementById('btnPip');
                    var btnFullscreen = document.getElementById('btnFullscreen');
                    var btnScreenshot = document.getElementById('btnScreenshot');
                    var controlBar = document.getElementById('controlBar');
                    var seekLeft = document.getElementById('seekLeft');
                    var seekRight = document.getElementById('seekRight');
                    var vlcModal = document.getElementById('vlcModal');
                    var toast = document.getElementById('toast');
                    var toastMsg = document.getElementById('toastMsg');

                    var VLC_STREAM_URL = '$vlcStreamUrl';
                    var VLC_PROTOCOL_URL = '$vlcProtocolUrl';
                    var M3U_DOWNLOAD_URL = '$m3uDownloadUrl';

                    var iconPlay = '<svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3"/></svg>';
                    var iconPause = '<svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>';
                    var iconVolHigh = '<svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>';
                    var iconVolLow = '<svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>';
                    var iconVolMute = '<svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>';

                    function showToast(msg) {
                        toastMsg.innerHTML = '<svg class="w-4 h-4 stroke-current stroke-2 fill-none text-emerald-400" viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg> ' + msg;
                        toast.classList.add('show');
                        setTimeout(function() { toast.classList.remove('show'); }, 3000);
                    }

                    function copyStreamUrl() {
                        if (navigator.clipboard && navigator.clipboard.writeText) {
                            navigator.clipboard.writeText(VLC_STREAM_URL).then(function() {
                                showToast('Stream URL copied! Paste in VLC (Ctrl+N)');
                            }).catch(function() {
                                showToast('Stream URL: ' + VLC_STREAM_URL);
                            });
                        } else {
                            showToast('Stream URL: ' + VLC_STREAM_URL);
                        }
                    }

                    function launchVlcDirectly() {
                        vlcModal.style.display = 'flex';
                        window.location.href = VLC_PROTOCOL_URL;
                    }

                    function closeVlcModal() {
                        vlcModal.style.display = 'none';
                    }

                    vlcModal.addEventListener('click', function(e) {
                        if (e.target === vlcModal) closeVlcModal();
                    });

                    try {
                        lottie.loadAnimation({
                            container: document.getElementById('logoSticker'),
                            renderer: 'svg',
                            loop: true,
                            autoplay: true,
                            path: '/stickers/rocket.json'
                        });
                        lottie.loadAnimation({
                            container: document.getElementById('heartSticker'),
                            renderer: 'svg',
                            loop: true,
                            autoplay: true,
                            path: '/stickers/heart.json'
                        });
                    } catch (e) {
                        console.warn('Lottie sticker fallback', e);
                    }

                    function fmtTime(sec) {
                        if (isNaN(sec) || sec < 0) return '00:00';
                        var h = Math.floor(sec / 3600);
                        var m = Math.floor((sec % 3600) / 60);
                        var s = Math.floor(sec % 60);
                        if (h > 0) {
                            return h + ':' + String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
                        } else {
                            return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
                        }
                    }

                    function togglePlay() {
                        if (video.paused) {
                            video.play();
                            btnPlay.innerHTML = iconPause;
                        } else {
                            video.pause();
                            btnPlay.innerHTML = iconPlay;
                        }
                    }
                    btnPlay.addEventListener('click', togglePlay);
                    video.addEventListener('click', togglePlay);
                    video.addEventListener('play', function() { btnPlay.innerHTML = iconPause; });
                    video.addEventListener('pause', function() { btnPlay.innerHTML = iconPlay; });

                    var isDraggingSeek = false;
                    video.addEventListener('timeupdate', function() {
                        if (!isDraggingSeek) {
                            seekBar.value = (video.currentTime / (video.duration || 1)) * 100;
                        }
                        timeText.textContent = fmtTime(video.currentTime) + ' / ' + fmtTime(video.duration);
                    });

                    seekBar.addEventListener('input', function() {
                        isDraggingSeek = true;
                        var targetTime = (seekBar.value / 100) * video.duration;
                        timeText.textContent = fmtTime(targetTime) + ' / ' + fmtTime(video.duration);
                    });
                    seekBar.addEventListener('change', function() {
                        isDraggingSeek = false;
                        video.currentTime = (seekBar.value / 100) * video.duration;
                    });

                    volBar.addEventListener('input', function(e) {
                        video.volume = parseFloat(e.target.value);
                        video.muted = (video.volume === 0);
                        btnMute.innerHTML = video.muted ? iconVolMute : (video.volume > 0.5 ? iconVolHigh : iconVolLow);
                    });
                    btnMute.addEventListener('click', function() {
                        video.muted = !video.muted;
                        btnMute.innerHTML = video.muted ? iconVolMute : (video.volume > 0.5 ? iconVolHigh : iconVolLow);
                    });

                    speedSelect.addEventListener('change', function(e) {
                        video.playbackRate = parseFloat(e.target.value);
                    });

                    btnFullscreen.addEventListener('click', function() {
                        if (!document.fullscreenElement) {
                            document.getElementById('playerContainer').requestFullscreen().catch(function() {});
                        } else {
                            document.exitFullscreen().catch(function() {});
                        }
                    });

                    btnPip.addEventListener('click', function() {
                        if (document.pictureInPictureElement) {
                            document.exitPictureInPicture().catch(function() {});
                        } else if (document.pictureInPictureEnabled) {
                            video.requestPictureInPicture().catch(function() {});
                        }
                    });

                    btnScreenshot.addEventListener('click', function() {
                        try {
                            var canvas = document.createElement('canvas');
                            canvas.width = video.videoWidth || 1920;
                            canvas.height = video.videoHeight || 1080;
                            var ctx = canvas.getContext('2d');
                            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
                            var link = document.createElement('a');
                            link.download = 'screenshot_' + Date.now() + '.png';
                            link.href = canvas.toDataURL('image/png');
                            link.click();
                        } catch (e) { console.error(e); }
                    });

                    document.addEventListener('keydown', function(e) {
                        if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.tagName === 'SELECT') return;
                        if (e.code === 'Space' || e.code === 'KeyK') {
                            e.preventDefault();
                            togglePlay();
                        } else if (e.code === 'ArrowRight' || e.code === 'KeyL') {
                            video.currentTime += 5;
                            showIndicator(seekRight);
                        } else if (e.code === 'ArrowLeft' || e.code === 'KeyJ') {
                            video.currentTime -= 5;
                            showIndicator(seekLeft);
                        } else if (e.code === 'ArrowUp') {
                            e.preventDefault();
                            video.volume = Math.min(1, video.volume + 0.1);
                            volBar.value = video.volume;
                        } else if (e.code === 'ArrowDown') {
                            e.preventDefault();
                            video.volume = Math.max(0, video.volume - 0.1);
                            volBar.value = video.volume;
                        } else if (e.code === 'KeyF') {
                            btnFullscreen.click();
                        } else if (e.code === 'KeyM') {
                            btnMute.click();
                        } else if (e.code === 'KeyP') {
                            btnPip.click();
                        } else if (e.code === 'KeyS') {
                            btnScreenshot.click();
                        }
                    });

                    function showIndicator(el) {
                        el.style.opacity = '1';
                        setTimeout(function() { el.style.opacity = '0'; }, 600);
                    }

                    var hideTimeout;
                    var pContainer = document.getElementById('playerContainer');
                    pContainer.addEventListener('mousemove', function() {
                        controlBar.style.opacity = '1';
                        clearTimeout(hideTimeout);
                        hideTimeout = setTimeout(function() {
                            if (!video.paused) controlBar.style.opacity = '0';
                        }, 2500);
                    });

                    var tabPlaylist = document.getElementById('tabPlaylist');
                    var tabSongs = document.getElementById('tabSongs');
                    var playlistContent = document.getElementById('playlistContent');
                    var songsContent = document.getElementById('songsContent');

                    tabPlaylist.addEventListener('click', function() {
                        tabPlaylist.className = 'flex-1 py-1.5 text-xs font-bold rounded-lg bg-sky-500 text-white transition';
                        tabSongs.className = 'flex-1 py-1.5 text-xs font-bold rounded-lg text-slate-400 hover:text-white transition';
                        playlistContent.classList.remove('hidden');
                        songsContent.classList.add('hidden');
                    });

                    tabSongs.addEventListener('click', function() {
                        tabSongs.className = 'flex-1 py-1.5 text-xs font-bold rounded-lg bg-sky-500 text-white transition';
                        tabPlaylist.className = 'flex-1 py-1.5 text-xs font-bold rounded-lg text-slate-400 hover:text-white transition';
                        songsContent.classList.remove('hidden');
                        playlistContent.classList.add('hidden');
                        loadSoundtracks();
                    });

                    function loadPlaylist() {
                        fetch('/api/playlist')
                            .then(function(res) { return res.json(); })
                            .then(function(list) {
                                var container = document.getElementById('playlistItems');
                                if (list && list.length > 0) {
                                    document.getElementById('queueCountBadge').textContent = list.length + ' items';
                                    container.innerHTML = list.map(function(item, idx) {
                                        return '<div class="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/5 transition flex items-center justify-between gap-2 cursor-pointer" onclick="switchVideo(\'' + item.uri + '\')">' +
                                            '<div class="flex items-center gap-2 min-w-0">' +
                                                '<span class="text-xs text-slate-500 font-mono">' + (idx + 1) + '</span>' +
                                                '<span class="text-xs font-semibold text-slate-200 truncate">' + item.title + '</span>' +
                                            '</div>' +
                                            '<span class="text-[10px] text-slate-400 font-mono">' + (item.duration || '') + '</span>' +
                                        '</div>';
                                    }).join('');
                                }
                            })
                            .catch(function(e) { console.warn('Playlist fetch failed', e); });
                    }

                    function loadSoundtracks() {
                        fetch('/api/songs')
                            .then(function(res) { return res.json(); })
                            .then(function(songs) {
                                var container = document.getElementById('songItems');
                                if (songs && songs.length > 0) {
                                    container.innerHTML = songs.map(function(s) {
                                        var audioTag = s.previewUrl ? '<audio controls class="h-6 w-28 scale-90" src="' + s.previewUrl + '"></audio>' : '';
                                        return '<div class="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/5 transition flex items-center justify-between gap-2">' +
                                            '<div class="flex items-center gap-2.5 min-w-0">' +
                                                '<div class="w-8 h-8 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center">' +
                                                    '<svg class="w-4 h-4 stroke-current stroke-2 fill-none" viewBox="0 0 24 24"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>' +
                                                '</div>' +
                                                '<div class="min-w-0">' +
                                                    '<p class="text-xs font-bold text-white truncate">' + s.title + '</p>' +
                                                    '<p class="text-[10px] text-slate-400 truncate">' + s.artist + '</p>' +
                                                '</div>' +
                                            '</div>' +
                                            audioTag +
                                        '</div>';
                                    }).join('');
                                }
                            })
                            .catch(function(e) { console.warn('Songs fetch failed', e); });
                    }

                    function switchVideo(uri) {
                        fetch('/api/command?action=select&uri=' + encodeURIComponent(uri));
                    }

                    loadPlaylist();
                </script>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        if (method != "HEAD") {
            out.write(bytes)
        }
        out.flush()
    }

    private fun serveVideoStream(out: OutputStream, method: String, rangeHeader: String?) {
        val uri = activeVideoUri ?: run {
            send404(out)
            return
        }

        try {
            val uriString = uri.toString()
            if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                proxyHttpStream(uriString, method, rangeHeader, out)
                return
            }

            if (uriString.startsWith("tdlib://")) {
                val offlineFile = findTdlibOfflineFile(uri)
                if (offlineFile != null && offlineFile.exists() && offlineFile.length() > 0) {
                    serveLocalFileStream(offlineFile, method, rangeHeader, out)
                    return
                }
                val fallbackUrl = uri.getQueryParameter("fallback")
                if (!fallbackUrl.isNullOrEmpty()) {
                    proxyHttpStream(fallbackUrl, method, rangeHeader, out)
                    return
                }
            }

            val fileLength = getMediaLength(uri)
            val mimeType = getMediaMimeType(uri)

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                val range = rangeHeader.substring(6)
                val parts = range.split("-")
                val start = parts[0].toLongOrNull() ?: 0L
                val end = if (parts.size > 1 && parts[1].isNotEmpty()) {
                    parts[1].toLongOrNull() ?: (fileLength - 1)
                } else {
                    fileLength - 1
                }
                val contentLength = end - start + 1

                val header = "HTTP/1.1 206 Partial Content\r\n" +
                        "Content-Type: $mimeType\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Content-Range: bytes $start-$end/$fileLength\r\n" +
                        "Content-Length: $contentLength\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Connection: keep-alive\r\n\r\n"

                out.write(header.toByteArray(Charsets.UTF_8))
                if (method != "HEAD") {
                    sendMediaChunk(uri, start, contentLength, out)
                }
            } else {
                val header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: $mimeType\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Content-Length: $fileLength\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Connection: keep-alive\r\n\r\n"

                out.write(header.toByteArray(Charsets.UTF_8))
                if (method != "HEAD") {
                    sendMediaChunk(uri, 0, fileLength, out)
                }
            }
            out.flush()
        } catch (e: Exception) {
            Log.d(TAG, "Stream write ended: ${e.message}")
        }
    }

    private fun serveLocalFileStream(file: File, method: String, rangeHeader: String?, out: OutputStream) {
        val fileLength = file.length()
        val mimeType = when {
            file.name.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            file.name.endsWith(".webm", ignoreCase = true) -> "video/webm"
            file.name.endsWith(".avi", ignoreCase = true) -> "video/x-msvideo"
            file.name.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
            file.name.endsWith(".flv", ignoreCase = true) -> "video/x-flv"
            file.name.endsWith(".ts", ignoreCase = true) -> "video/mp2t"
            else -> "video/mp4"
        }

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val range = rangeHeader.substring(6)
            val parts = range.split("-")
            val start = parts[0].toLongOrNull() ?: 0L
            val end = if (parts.size > 1 && parts[1].isNotEmpty()) {
                parts[1].toLongOrNull() ?: (fileLength - 1)
            } else {
                fileLength - 1
            }
            val contentLength = end - start + 1

            val header = "HTTP/1.1 206 Partial Content\r\n" +
                    "Content-Type: $mimeType\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Content-Range: bytes $start-$end/$fileLength\r\n" +
                    "Content-Length: $contentLength\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: keep-alive\r\n\r\n"

            out.write(header.toByteArray(Charsets.UTF_8))
            if (method != "HEAD") {
                RandomAccessFile(file, "r").use { raf ->
                    if (start > 0) {
                        raf.seek(start)
                    }
                    val buffer = ByteArray(128 * 1024)
                    var bytesRemaining = contentLength
                    while (bytesRemaining > 0) {
                        val readSize = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                        val bytesRead = raf.read(buffer, 0, readSize)
                        if (bytesRead == -1) break
                        out.write(buffer, 0, bytesRead)
                        bytesRemaining -= bytesRead
                    }
                }
            }
        } else {
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: $mimeType\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Content-Length: $fileLength\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: keep-alive\r\n\r\n"

            out.write(header.toByteArray(Charsets.UTF_8))
            if (method != "HEAD") {
                RandomAccessFile(file, "r").use { raf ->
                    val buffer = ByteArray(128 * 1024)
                    var bytesRemaining = fileLength
                    while (bytesRemaining > 0) {
                        val readSize = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                        val bytesRead = raf.read(buffer, 0, readSize)
                        if (bytesRead == -1) break
                        out.write(buffer, 0, bytesRead)
                        bytesRemaining -= bytesRead
                    }
                }
            }
        }
        out.flush()
    }

    private fun serveSubtitle(out: OutputStream, method: String) {
        val file = activeSubtitleFile
        if (file == null || !file.exists()) {
            send404(out)
            return
        }
        val vttBytes = convertSrtToVtt(file).toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/vtt; charset=utf-8\r\n" +
                "Content-Length: ${vttBytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        if (method != "HEAD") {
            out.write(vttBytes)
        }
        out.flush()
    }

    private fun convertSrtToVtt(srtFile: File): String {
        return try {
            val text = srtFile.readText(Charsets.UTF_8)
            "WEBVTT\n\n" + text.replace(Regex("""(\d{2}:\d{2}:\d{2}),(\d{3})"""), "$1.$2")
        } catch (e: Exception) {
            "WEBVTT\n\n"
        }
    }

    private fun serveStatusJson(out: OutputStream, method: String) {
        val json = """
            {
                "title": ${escapeJson(activeVideoTitle)},
                "positionMs": $activePositionMs,
                "isPlaying": $isPlaying,
                "pin": "$pairingPin"
            }
        """.trimIndent()
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        if (method != "HEAD") {
            out.write(bytes)
        }
        out.flush()
    }

    private fun handleCommand(path: String, out: OutputStream) {
        val uri = Uri.parse(path)
        when (uri.getQueryParameter("action")) {
            "toggle" -> onRemotePlayPause?.invoke()
            "seek" -> {
                val to = uri.getQueryParameter("to")?.toLongOrNull()
                if (to != null) onRemoteSeek?.invoke(to)
            }
            "select" -> {
                val targetUri = uri.getQueryParameter("uri")
                if (targetUri != null) onRemoteSelectVideo?.invoke(targetUri)
            }
        }
        sendCorsOk(out)
    }

    private fun sendMediaChunk(uri: Uri, start: Long, length: Long, out: OutputStream) {
        if (uri.scheme == "content") {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    pfd.use { fd ->
                        FileInputStream(fd.fileDescriptor).use { stream ->
                            val channel = stream.channel
                            channel.position(start)
                            val buffer = ByteArray(128 * 1024)
                            var bytesRemaining = length
                            while (bytesRemaining > 0) {
                                val readSize = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                                val bytesRead = stream.read(buffer, 0, readSize)
                                if (bytesRead == -1) break
                                out.write(buffer, 0, bytesRead)
                                bytesRemaining -= bytesRead
                            }
                        }
                    }
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct channel seek failed for content URI, falling back to openInputStream: ${e.message}")
            }
        }

        val inputStream: InputStream = when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri) ?: return
            "file" -> FileInputStream(File(uri.path ?: return))
            else -> return
        }

        inputStream.use { stream ->
            if (start > 0) {
                var skipped = 0L
                while (skipped < start) {
                    val s = stream.skip(start - skipped)
                    if (s <= 0) break
                    skipped += s
                }
            }

            val buffer = ByteArray(128 * 1024)
            var bytesRemaining = length
            while (bytesRemaining > 0) {
                val readSize = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                val bytesRead = stream.read(buffer, 0, readSize)
                if (bytesRead == -1) break
                out.write(buffer, 0, bytesRead)
                bytesRemaining -= bytesRead
            }
        }
    }

    private fun proxyHttpStream(urlStr: String, method: String, rangeHeader: String?, out: OutputStream) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 30000
        conn.readTimeout = 0 // Infinite read timeout so streaming isn't severed when PC client buffers
        conn.setRequestProperty("User-Agent", "KryptonPlayer-UsbCinema/2.0")
        if (rangeHeader != null) {
            conn.setRequestProperty("Range", rangeHeader)
        }

        conn.connect()
        val responseCode = conn.responseCode
        val contentType = conn.contentType ?: "video/mp4"
        val contentLength = conn.contentLengthLong

        var header = "HTTP/1.1 $responseCode ${conn.responseMessage}\r\n" +
                "Content-Type: $contentType\r\n" +
                "Access-Control-Allow-Origin: *\r\n"

        val contentRange = conn.getHeaderField("Content-Range")
        if (contentRange != null) {
            header += "Content-Range: $contentRange\r\n"
        }
        if (contentLength > 0) {
            header += "Content-Length: $contentLength\r\n"
        }
        header += "Connection: keep-alive\r\n\r\n"

        out.write(header.toByteArray(Charsets.UTF_8))

        if (method != "HEAD") {
            val inStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            inStream?.use { stream ->
                val buffer = ByteArray(128 * 1024)
                var bytesRead = 0
                while (true) {
                    val count = stream.read(buffer)
                    if (count == -1) break
                    bytesRead = count
                    out.write(buffer, 0, bytesRead)
                }
            }
        }
        out.flush()
    }

    private fun findTdlibOfflineFile(uri: Uri): File? {
        val fileId = uri.getQueryParameter("id") ?: uri.lastPathSegment ?: return null
        
        // 1. Check TDLib internal files cache
        val tdlibDir = File(context.cacheDir, "tdlib_files")
        if (tdlibDir.exists()) {
            val matched = tdlibDir.walkTopDown().firstOrNull { it.name.contains(fileId) && it.length() > 0 }
            if (matched != null) return matched
        }

        // 2. Check offline videos external folder
        val extOffline = context.getExternalFilesDir("offline_videos")
        if (extOffline != null && extOffline.exists()) {
            val matched = extOffline.listFiles()?.firstOrNull { it.name.contains(fileId) && it.length() > 0 }
            if (matched != null) return matched
        }

        // 3. Check offline videos internal folder
        val intOffline = File(context.filesDir, "offline_videos")
        if (intOffline.exists()) {
            val matched = intOffline.listFiles()?.firstOrNull { it.name.contains(fileId) && it.length() > 0 }
            if (matched != null) return matched
        }

        return null
    }

    private fun getMediaLength(uri: Uri): Long {
        return when (uri.scheme) {
            "content" -> {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    it.length
                } ?: 0L
            }
            "file" -> File(uri.path ?: "").length()
            else -> 0L
        }
    }

    private fun getMediaMimeType(uri: Uri): String {
        return context.contentResolver.getType(uri) ?: run {
            val path = uri.path?.lowercase() ?: ""
            when {
                path.endsWith(".mkv") -> "video/x-matroska"
                path.endsWith(".webm") -> "video/webm"
                path.endsWith(".avi") -> "video/x-msvideo"
                path.endsWith(".mov") -> "video/quicktime"
                path.endsWith(".flv") -> "video/x-flv"
                path.endsWith(".ts") -> "video/mp2t"
                else -> "video/mp4"
            }
        }
    }

    private fun sendCorsOk(out: OutputStream) {
        val header = "HTTP/1.1 200 OK\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, HEAD, OPTIONS, POST\r\n" +
                "Access-Control-Allow-Headers: Range, Content-Type\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.flush()
    }

    private fun send404(out: OutputStream) {
        val body = "404 Not Found"
        val header = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: ${body.length}\r\n" +
                "Connection: close\r\n\r\n"
        out.write((header + body).toByteArray(Charsets.UTF_8))
        out.flush()
    }
}
