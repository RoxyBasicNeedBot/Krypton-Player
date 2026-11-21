package dev.roxy.kryptonplayer.core.data.tdlib

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.util.LruCache
import org.drinkless.tdlib.TdApi

/**
 * High-performance 0ms decoder for Telegram MiniThumbnails.
 *
 * In Telegram MTProto / TDLib, MiniThumbnails are ultra-compact preview thumbnails
 * (typically 8x8 or 16x16) embedded directly inside message payloads without requiring
 * separate network downloads.
 *
 * This decoder handles:
 * 1. Fully-constructed JPEG bytes from TDLib (starts with SOI marker 0xFF, 0xD8).
 * 2. Raw packed Telegram minithumbnail bytes (starts with 0x01, height, width, scan data).
 * 3. In-memory LRU caching to guarantee instantaneous 0ms placeholder retrieval.
 */
object MinithumbnailDecoder {
    private const val TAG = "MinithumbnailDecoder"

    // Standard Telegram JPEG header (623 bytes) matching official TDLib PhotoSize.cpp implementation
    private val JPEG_HEADER: ByteArray = try {
        Base64.decode(
            "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDACgcHiMeGSgjISMtKygwPGRBPDc3PHtYXUlkkYCZlo+AjIqgtObDoKrarYqMyP/L2u71////m8H////6/+b9//j/2wBDASstLTw1PHZBQXb4pYyl+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj/wAARCAAAAAADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwA=",
            Base64.DEFAULT
        )
    } catch (_: Throwable) {
        ByteArray(0)
    }

    // Standard JPEG EOI marker (0xFF, 0xD9)
    private val JPEG_FOOTER = byteArrayOf(0xFF.toByte(), 0xD9.toByte())

    // In-memory LRU cache storing decoded Bitmaps for 0ms access (capacity: 256 bitmaps)
    private val memoryCache = object : LruCache<String, Bitmap>(256) {}

    /**
     * Reconstructs or extracts valid JPEG byte array from Telegram minithumbnail payload.
     */
    fun toJpegBytes(data: ByteArray?): ByteArray? {
        if (data == null || data.isEmpty()) return null

        // 1. Direct standard JPEG check (SOI marker 0xFF 0xD8)
        if (data.size >= 2 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte()) {
            return data
        }

        // 2. Packed Telegram format: 0x01, height, width, scan payload
        if (data[0] == 0x01.toByte() && data.size >= 3 && JPEG_HEADER.size >= 167) {
            val height = data[1]
            val width = data[2]
            val payload = data.copyOfRange(3, data.size)

            val part1 = JPEG_HEADER.copyOfRange(0, 164)
            val mid = JPEG_HEADER[165]
            val part2 = JPEG_HEADER.copyOfRange(167, JPEG_HEADER.size)

            val totalLen = part1.size + 1 + 1 + 1 + part2.size + payload.size + JPEG_FOOTER.size
            val reconstructed = ByteArray(totalLen)
            var offset = 0

            System.arraycopy(part1, 0, reconstructed, offset, part1.size)
            offset += part1.size

            reconstructed[offset++] = height
            reconstructed[offset++] = mid
            reconstructed[offset++] = width

            System.arraycopy(part2, 0, reconstructed, offset, part2.size)
            offset += part2.size

            System.arraycopy(payload, 0, reconstructed, offset, payload.size)
            offset += payload.size

            System.arraycopy(JPEG_FOOTER, 0, reconstructed, offset, JPEG_FOOTER.size)
            return reconstructed
        }

        // Fallback: return as-is
        return data
    }

    /**
     * Decodes a byte array into an Android [Bitmap] with memory LRU cache support.
     */
    fun decodeToBitmap(data: ByteArray?, cacheKey: String? = null): Bitmap? {
        if (data == null || data.isEmpty()) return null

        if (cacheKey != null) {
            memoryCache.get(cacheKey)?.let { return it }
        }

        val jpegBytes = toJpegBytes(data) ?: return null
        return try {
            val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            if (bitmap != null && cacheKey != null) {
                memoryCache.put(cacheKey, bitmap)
            }
            bitmap
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to decode minithumbnail JPEG: ${e.message}")
            null
        }
    }

    /**
     * Decodes a TDLib [TdApi.Minithumbnail] into a [Bitmap].
     */
    fun decode(minithumbnail: TdApi.Minithumbnail?, cacheKey: String? = null): Bitmap? {
        if (minithumbnail == null) return null
        return decodeToBitmap(minithumbnail.data, cacheKey)
    }

    /**
     * Checks whether the memory cache has a cached bitmap for [cacheKey].
     */
    fun getCachedBitmap(cacheKey: String): Bitmap? {
        return memoryCache.get(cacheKey)
    }

    /**
     * Clears all cached bitmaps.
     */
    fun clearCache() {
        memoryCache.evictAll()
    }
}
