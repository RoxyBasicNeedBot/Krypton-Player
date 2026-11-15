package dev.anilbeesetti.nextplayer.core.data.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AudioRingtoneExtractor {
    private const val TAG = "AudioRingtoneExtractor"
    private const val DEFAULT_BUFFER_SIZE = 512 * 1024

    suspend fun extractAudio(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long,
        trackTitle: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, sourceUri, null)
            } catch (e: Exception) {
                extractor.setDataSource(sourceUri.toString())
            }

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                extractor.release()
                throw IllegalStateException("No audio track found in media")
            }

            extractor.selectTrack(audioTrackIndex)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeTitle = trackTitle?.replace(Regex("[^a-zA-Z0-9_]"), "_")?.take(20) ?: "Ringtone"
            val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES) ?: context.cacheDir, "ArchRingtones").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(outputDir, "Arch_${safeTitle}_${timeStamp}.m4a")

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex != audioTrackIndex) {
                    if (trackIndex < 0) break
                    extractor.advance()
                    continue
                }

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.presentationTimeUs = (sampleTime - startUs).coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlags
                bufferInfo.offset = 0

                muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                extractor.advance()
            }

            try {
                muxer.stop()
                muxer.release()
            } catch (e: Exception) {
                Log.w(TAG, "Muxer release error: ${e.message}")
            }
            extractor.release()

            outputFile
        }
    }

    suspend fun saveAndSetRingtone(
        context: Context,
        audioFile: File,
        title: String,
        type: Int = RingtoneManager.TYPE_RINGTONE
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${title}.m4a")
                put(MediaStore.MediaColumns.TITLE, title)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.IS_RINGTONE, type == RingtoneManager.TYPE_RINGTONE)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, type == RingtoneManager.TYPE_NOTIFICATION)
                put(MediaStore.Audio.Media.IS_ALARM, type == RingtoneManager.TYPE_ALARM)
                put(MediaStore.Audio.Media.IS_MUSIC, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relPath = when (type) {
                        RingtoneManager.TYPE_NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
                        RingtoneManager.TYPE_ALARM -> Environment.DIRECTORY_ALARMS
                        else -> Environment.DIRECTORY_RINGTONES
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val newUri = context.contentResolver.insert(collection, contentValues)
                ?: throw IllegalStateException("Failed to insert into MediaStore")

            context.contentResolver.openOutputStream(newUri)?.use { out ->
                FileInputStream(audioFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(context)) {
                RingtoneManager.setActualDefaultRingtoneUri(context, type, newUri)
            }

            newUri
        }
    }
}
