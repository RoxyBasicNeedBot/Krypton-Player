package dev.anilbeesetti.nextplayer.core.data.subtitles

import android.content.Context
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            cont.resume(result)
        }
        addOnFailureListener { exception ->
            cont.resumeWith(Result.failure(exception))
        }
        addOnCanceledListener {
            cont.cancel()
        }
    }

@Singleton
class SubtitleTranslator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SubtitleTranslator"
        private const val CACHE_DIR_NAME = "translated_subtitles"
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
    }

    fun getCacheSizeBytes(): Long {
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearCache(): Long {
        val size = getCacheSizeBytes()
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        return size
    }

    /**
     * Translates a subtitle file (.srt / .vtt) into the target language (default English "en").
     * Uses on-device Google ML Kit translation with intelligent caching.
     */
    suspend fun translateSubtitleFile(
        sourceFile: File,
        targetLanguageCode: String = "en",
        sourceLanguageCode: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                Log.w(TAG, "Source subtitle file does not exist or is empty")
                return@withContext null
            }

            val cacheKey = "${sourceFile.nameWithoutExtension}_trans_$targetLanguageCode.srt"
            val cachedFile = File(cacheDir, cacheKey)
            if (cachedFile.exists() && cachedFile.length() > 0L) {
                Log.i(TAG, "Using cached translated subtitle: ${cachedFile.absolutePath}")
                return@withContext cachedFile
            }

            val rawContent = sourceFile.readText(Charsets.UTF_8)
            val blocks = parseSrtBlocks(rawContent)
            if (blocks.isEmpty()) {
                Log.w(TAG, "No subtitle blocks found in file")
                return@withContext null
            }

            // Detect source language if not provided
            val sampleText = blocks.take(10).joinToString(" ") { it.text }
            val detectedSource = sourceLanguageCode ?: detectLanguage(sampleText) ?: "auto"
            Log.i(TAG, "Detected source language: $detectedSource -> target: $targetLanguageCode")

            val translatedBlocks = mutableListOf<SrtBlock>()
            val targetMlKitLang = mapToMlKitLanguage(targetLanguageCode) ?: TranslateLanguage.ENGLISH
            val sourceMlKitLang = mapToMlKitLanguage(detectedSource)

            if (sourceMlKitLang != null && sourceMlKitLang != targetMlKitLang) {
                // Perform On-Device Google ML Kit Translation
                try {
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(sourceMlKitLang)
                        .setTargetLanguage(targetMlKitLang)
                        .build()
                    val translator = Translation.getClient(options)
                    val conditions = DownloadConditions.Builder().build()
                    translator.downloadModelIfNeeded(conditions).awaitTask()

                    val total = blocks.size
                    for ((index, block) in blocks.withIndex()) {
                        val translatedText = try {
                            if (block.text.isNotBlank()) {
                                translator.translate(block.text).awaitTask()
                            } else {
                                block.text
                            }
                        } catch (e: Exception) {
                            block.text
                        }
                        translatedBlocks.add(block.copy(text = translatedText))
                        onProgress?.invoke((index + 1).toFloat() / total.toFloat())
                    }
                    translator.close()
                } catch (e: Exception) {
                    Log.w(TAG, "ML Kit translation failed, falling back to open cloud translator", e)
                    translatedBlocks.clear()
                }
            }

            // Fast Batch Translation Fallback
            if (translatedBlocks.isEmpty()) {
                val total = blocks.size
                val batchSize = 25
                for (i in 0 until total step batchSize) {
                    val end = minOf(i + batchSize, total)
                    val chunk = blocks.subList(i, end)
                    val originalTexts = chunk.map { it.text }
                    val translatedTexts = translateBatchFast(originalTexts, detectedSource, targetLanguageCode)
                    
                    for (j in chunk.indices) {
                        val trans = translatedTexts.getOrNull(j)?.takeIf { it.isNotBlank() } ?: chunk[j].text
                        translatedBlocks.add(chunk[j].copy(text = trans))
                    }
                    onProgress?.invoke(end.toFloat() / total.toFloat())
                }
            }

            // Write output .srt file
            val outputContent = buildSrtString(translatedBlocks)
            FileOutputStream(cachedFile).use { it.write(outputContent.toByteArray(Charsets.UTF_8)) }

            if (cachedFile.exists() && cachedFile.length() > 0L) {
                Log.i(TAG, "[TRANSLATE] Subtitle translation successful: ${cachedFile.absolutePath}")
                cachedFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to translate subtitle file", e)
            null
        }
    }

    private suspend fun detectLanguage(text: String): String? {
        return try {
            val languageIdentifier = LanguageIdentification.getClient()
            val code = languageIdentifier.identifyLanguage(text).awaitTask()
            if (code == "und") null else code
        } catch (e: Exception) {
            null
        }
    }

    private fun mapToMlKitLanguage(code: String): String? {
        return when (code.lowercase().trim()) {
            "en", "eng" -> TranslateLanguage.ENGLISH
            "ja", "jpn" -> TranslateLanguage.JAPANESE
            "ko", "kor" -> TranslateLanguage.KOREAN
            "es", "spa" -> TranslateLanguage.SPANISH
            "fr", "fre", "fra" -> TranslateLanguage.FRENCH
            "de", "ger", "deu" -> TranslateLanguage.GERMAN
            "hi", "hin" -> TranslateLanguage.HINDI
            "zh", "zho", "zht" -> TranslateLanguage.CHINESE
            "ar", "ara" -> TranslateLanguage.ARABIC
            "ru", "rus" -> TranslateLanguage.RUSSIAN
            "pt", "por", "pob" -> TranslateLanguage.PORTUGUESE
            "it", "ita" -> TranslateLanguage.ITALIAN
            "tr", "tur" -> TranslateLanguage.TURKISH
            "vi", "vie" -> TranslateLanguage.VIETNAMESE
            "id", "ind" -> TranslateLanguage.INDONESIAN
            "pl", "pol" -> TranslateLanguage.POLISH
            "nl", "nld" -> TranslateLanguage.DUTCH
            "sv", "swe" -> TranslateLanguage.SWEDISH
            "th", "tha" -> TranslateLanguage.THAI
            "ta", "tam" -> TranslateLanguage.TAMIL
            "te", "tel" -> TranslateLanguage.TELUGU
            "bn", "ben" -> TranslateLanguage.BENGALI
            "mr", "mar" -> TranslateLanguage.MARATHI
            "ur", "urd" -> TranslateLanguage.URDU
            else -> TranslateLanguage.fromLanguageTag(code)
        }
    }

    private fun translateBatchFast(
        texts: List<String>,
        src: String,
        target: String
    ): List<String> {
        if (texts.isEmpty()) return emptyList()
        val s = if (src == "und" || src.isBlank()) "auto" else src
        val delimiter = "\n~@@~\n"
        val combined = texts.joinToString(delimiter)

        return try {
            val urlStr = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=$s&tl=$target&q=${URLEncoder.encode(combined, "UTF-8")}"
            val conn = URL(urlStr).openConnection().apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connectTimeout = 8000
                readTimeout = 8000
            }
            val json = conn.getInputStream().bufferedReader().use { it.readText() }
            val resultText = extractChromeTranslations(json)
            val parts = resultText.split(Regex("""\s*~@@~\s*"""))
            if (parts.size == texts.size) {
                parts.map { it.trim() }
            } else {
                texts.map { translateSingleLineFallback(it, s, target) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Batch translation failed, falling back to line-by-line", e)
            texts.map { translateSingleLineFallback(it, s, target) }
        }
    }

    private fun translateSingleLineFallback(text: String, src: String, target: String): String {
        if (text.isBlank()) return text
        return try {
            val s = if (src == "und" || src.isBlank()) "auto" else src
            val url = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=$s&tl=$target&q=${URLEncoder.encode(text, "UTF-8")}"
            val conn = URL(url).openConnection().apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connectTimeout = 4000
                readTimeout = 4000
            }
            val json = conn.getInputStream().bufferedReader().use { it.readText() }
            val result = extractChromeTranslations(json)
            result.ifBlank { text }
        } catch (e: Exception) {
            text
        }
    }

    private fun extractChromeTranslations(json: String): String {
        val sb = StringBuilder()
        var inString = false
        var isEscaped = false
        val currentStr = StringBuilder()
        var stringCount = 0

        for (ch in json) {
            if (isEscaped) {
                when (ch) {
                    'n' -> currentStr.append('\n')
                    'r' -> currentStr.append('\r')
                    't' -> currentStr.append('\t')
                    '"' -> currentStr.append('"')
                    '\\' -> currentStr.append('\\')
                    else -> currentStr.append(ch)
                }
                isEscaped = false
                continue
            }
            if (ch == '\\' && inString) {
                isEscaped = true
                continue
            }
            if (ch == '"') {
                if (inString) {
                    if (stringCount == 0) {
                        sb.append(currentStr.toString())
                    }
                    stringCount++
                    currentStr.clear()
                    inString = false
                } else {
                    inString = true
                }
                continue
            }
            if (inString) {
                currentStr.append(ch)
            }
        }
        return sb.toString()
    }

    private data class SrtBlock(
        val index: String,
        val timestamp: String,
        val text: String
    )

    private fun parseSrtBlocks(raw: String): List<SrtBlock> {
        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n").trim()
        val rawBlocks = normalized.split(Regex("""\n\s*\n"""))
        val result = mutableListOf<SrtBlock>()

        for (b in rawBlocks) {
            val lines = b.trim().split("\n")
            if (lines.size >= 2) {
                // Find timestamp line (e.g. 00:00:00,000 --> 00:00:00,000)
                val timeIndex = lines.indexOfFirst { it.contains("-->") }
                if (timeIndex != -1) {
                    val idx = if (timeIndex > 0) lines[0].trim() else "${result.size + 1}"
                    val timestamp = lines[timeIndex].trim()
                    val text = lines.drop(timeIndex + 1).joinToString("\n").trim()
                    result.add(SrtBlock(idx, timestamp, text))
                }
            }
        }
        return result
    }

    private fun buildSrtString(blocks: List<SrtBlock>): String {
        val sb = StringBuilder()
        for ((i, block) in blocks.withIndex()) {
            sb.append("${i + 1}\n")
            sb.append("${block.timestamp}\n")
            sb.append("${block.text}\n\n")
        }
        return sb.toString().trim() + "\n"
    }
}
