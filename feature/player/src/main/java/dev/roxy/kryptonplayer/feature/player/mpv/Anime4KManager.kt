package dev.roxy.kryptonplayer.feature.player.mpv

import android.content.Context
import android.util.Log
import java.io.File

class Anime4KManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "Anime4KManager"
        val BUILT_IN_SHADER_FILES: Set<String> = Anime4KShaderCatalog.requiredFileSet
        val DEFAULT_QUALITY = Quality.BALANCED

        private val FP32_MARKERS = listOf(
            "floatBitsToUint",
            "uintBitsToFloat",
            "floatBitsToInt",
            "intBitsToFloat",
            "bitfieldExtract",
            "bitfieldInsert",
            "FsrEasu",
            "FsrRcas",
            "APrxLoRcpF1",
            "APrxLoRsqF1",
            "APrxMedRcpF1",
        )
    }

    enum class Quality(
        val suffix: String,
        val displayName: String,
    ) {
        FAST("S", "Fast"),
        BALANCED("M", "Balanced"),
        HIGH("L", "High"),
    }

    enum class Mode(
        val displayName: String,
    ) {
        OFF("Off"),
        A("Mode A (Restore + Upscale)"),
        B("Mode B (Soft Restore + Upscale)"),
        C("Mode C (Denoise + Upscale)"),
        A_PLUS("Mode A+A (Heavy Restore)"),
        B_PLUS("Mode B+B (Heavy Soft)"),
        C_PLUS("Mode C+A (Denoise + Restore)"),
        ARTCNN("ArtCNN"),
    }

    private var shaderDir: File? = null
    private var isInitialized = false

    @Volatile
    private var enableDarken: Boolean = true

    @Volatile
    private var enableThin: Boolean = true

    @Volatile
    private var enableDeblur: Boolean = false

    fun setPostFilters(
        darken: Boolean,
        thin: Boolean,
        deblur: Boolean,
    ) {
        enableDarken = darken
        enableThin = thin
        enableDeblur = deblur
    }

    @Synchronized
    fun initialize(): Boolean {
        if (isInitialized) return true
        return try {
            val dir = File(context.filesDir, Anime4KShaderCatalog.INSTALL_DIRECTORY)
            shaderDir = dir
            if (!dir.exists() && !dir.mkdirs()) return false

            val shaderFiles = context.assets
                .list(Anime4KShaderCatalog.ASSET_DIRECTORY)
                ?.filter { it.endsWith(".glsl") }
                .orEmpty()
            for (fileName in shaderFiles) {
                val forceCopy = fileName in Anime4KShaderCatalog.requiredFileSet
                if (!copyShaderFromAssets(fileName, forceCopy = forceCopy)) {
                    return false
                }
            }

            val missingRequiredFiles = Anime4KShaderCatalog.requiredFiles.any { required ->
                val file = File(dir, required)
                !file.exists() || file.length() <= 0L
            }
            if (missingRequiredFiles) return false

            removeLegacyFlatShaderCopies()
            isInitialized = true
            true
        } catch (_: Exception) {
            isInitialized = false
            false
        }
    }

    private fun copyShaderFromAssets(
        fileName: String,
        forceCopy: Boolean = false,
    ): Boolean {
        val destFile = File(shaderDir, fileName)
        if (!forceCopy && destFile.exists() && destFile.length() > 0) return true
        return try {
            val originalContent = context.assets
                .open("${Anime4KShaderCatalog.ASSET_DIRECTORY}/$fileName")
                .use { input -> input.bufferedReader().use { it.readText() } }
            destFile.writeText(optimizeShaderContent(fileName, originalContent))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy shader: $fileName", e)
            false
        }
    }

    private fun removeLegacyFlatShaderCopies() {
        val legacyDirectory = File(context.filesDir, "shaders")
        Anime4KShaderCatalog.requiredFiles.forEach { fileName ->
            File(legacyDirectory, fileName).takeIf(File::isFile)?.delete()
        }
    }

    private fun optimizeShaderContent(
        fileName: String,
        content: String,
    ): String {
        if (!fileName.endsWith(".glsl")) return content
        val lines = content.lines()
        val newLines = mutableListOf<String>()
        var inHeader = false
        var precisionInjectedForBlock = false
        var currentPassNeedsHighp = false

        for (i in lines.indices) {
            val line = lines[i]
            val trimmed = line.trim()
            if (trimmed.startsWith("//!")) {
                if (!inHeader) {
                    inHeader = true
                    precisionInjectedForBlock = false
                }
                newLines.add(line)
            } else {
                if (inHeader) {
                    inHeader = false
                    if (!precisionInjectedForBlock && trimmed.isNotEmpty() && !trimmed.startsWith("//")) {
                        currentPassNeedsHighp = passNeedsHighpFloat(lines, i)
                        val floatPrecision = if (currentPassNeedsHighp) "highp" else "mediump"
                        newLines.add("precision $floatPrecision float;")
                        newLines.add("precision highp int;")
                        precisionInjectedForBlock = true
                    }
                }
                val rewritten = if (precisionInjectedForBlock) {
                    rewriteLineWithExplicitPrecision(line, if (currentPassNeedsHighp) "highp" else "mediump")
                } else {
                    line
                }
                newLines.add(rewritten)
            }
        }
        return optimizeCreluPasses(newLines.joinToString("\n"))
    }

    private fun rewriteLineWithExplicitPrecision(
        line: String,
        precision: String,
    ): String {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) return line
        if (trimmed.startsWith("mediump ") || trimmed.startsWith("highp ") || trimmed.startsWith("lowp ")) return line

        val floatTypes = listOf("mat4", "mat3", "mat2", "vec4", "vec3", "vec2", "float")
        val leading = line.length - line.trimStart().length
        val indent = line.take(leading)

        for (type in floatTypes) {
            if (trimmed.startsWith("$type ") || trimmed.startsWith("$type(")) {
                val afterType = trimmed.removePrefix(type).trimStart()
                val isFunctionDef =
                    afterType.contains("(") &&
                        !afterType.substringBefore("(").contains("=") &&
                        afterType.first() != '('
                if (isFunctionDef) return line
                return "$indent$precision $trimmed"
            }
        }
        return line
    }

    private fun optimizeCreluPasses(content: String): String {
        val passes = content.split("(?=(?://!DESC|//!HOOK))".toRegex())
        return passes.joinToString("") { optimizeSinglePass(it) }
    }

    private fun optimizeSinglePass(pass: String): String {
        val go0Regex =
            """#define\s+go_0\([^\)]+\)\s+\(max\(\(?\s*([A-Za-z0-9_]+)_texOff\(vec2\([^\)]+\)\)\)?\s*,\s*0\.0\)\)"""
                .toRegex()
        val go1Regex =
            """#define\s+go_1\([^\)]+\)\s+\(max\(\-\(?\s*([A-Za-z0-9_]+)_texOff\(vec2\([^\)]+\)\)\)?\s*,\s*0\.0\)\)"""
                .toRegex()

        val match0 = go0Regex.find(pass)
        val match1 = go1Regex.find(pass)
        if (match0 == null || match1 == null) return pass

        val texName = match0.groupValues[1]
        if (texName != match1.groupValues[1]) return pass

        var optimized = pass
            .replace(match0.value, "// optimized go_0 macro")
            .replace(match1.value, "// optimized go_1 macro")

        val hookStartRegex = """vec4\s+hook\(\s*\)\s*\{""".toRegex()
        val hookDeclaration = """
            vec4 hook() {
                vec4 t_m1_m1 = ${texName}_texOff(vec2(-1.0, -1.0));
                vec4 t_m1_0  = ${texName}_texOff(vec2(-1.0, 0.0));
                vec4 t_m1_1  = ${texName}_texOff(vec2(-1.0, 1.0));
                vec4 t_0_m1  = ${texName}_texOff(vec2(0.0, -1.0));
                vec4 t_0_0   = ${texName}_texOff(vec2(0.0, 0.0));
                vec4 t_0_1   = ${texName}_texOff(vec2(0.0, 1.0));
                vec4 t_1_m1  = ${texName}_texOff(vec2(1.0, -1.0));
                vec4 t_1_0   = ${texName}_texOff(vec2(1.0, 0.0));
                vec4 t_1_1   = ${texName}_texOff(vec2(1.0, 1.0));
        """.trimIndent()

        optimized = optimized.replaceFirst(hookStartRegex, hookDeclaration)
        optimized = """go_0\(\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*\)"""
            .toRegex()
            .replace(optimized) { result ->
                "max(t_${mapCoord(result.groupValues[1])}_${mapCoord(result.groupValues[2])}, 0.0)"
            }
        optimized = """go_1\(\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*\)"""
            .toRegex()
            .replace(optimized) { result ->
                "max(-t_${mapCoord(result.groupValues[1])}_${mapCoord(result.groupValues[2])}, 0.0)"
            }

        return if (optimized.contains("_unknown")) pass else optimized
    }

    private fun passNeedsHighpFloat(
        lines: List<String>,
        bodyStart: Int,
    ): Boolean {
        var j = bodyStart
        while (j < lines.size && !lines[j].trim().startsWith("//!")) {
            val body = lines[j]
            if (FP32_MARKERS.any { body.contains(it) }) return true
            j++
        }
        return false
    }

    private fun mapCoord(c: String): String = when (c.trim()) {
        "-1.0", "-1" -> "m1"
        "0.0", "0" -> "0"
        "1.0", "1" -> "1"
        else -> "unknown"
    }

    fun getShaderPaths(mode: Mode): List<String> = getShaderPaths(mode, DEFAULT_QUALITY)

    fun getShaderPaths(
        mode: Mode,
        quality: Quality,
    ): List<String> = getShaderFiles(mode, quality).map { it.absolutePath }

    fun getShaderFiles(
        mode: Mode,
        quality: Quality,
    ): List<File> {
        if (mode == Mode.OFF) return emptyList()
        if (!isInitialized && !initialize()) return emptyList()
        val dir = shaderDir ?: return emptyList()
        if (!dir.exists()) return emptyList()

        val shaders = mutableListOf<File>()
        val q = quality.suffix
        shaders.add(File(dir, "Anime4K_Clamp_Highlights.glsl"))

        when (mode) {
            Mode.A -> {
                shaders.add(File(dir, "Anime4K_Restore_CNN_$q.glsl"))
                shaders.add(File(dir, "Anime4K_Upscale_CNN_x2_$q.glsl"))
                shaders.add(File(dir, "Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(File(dir, "Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.B -> {
                shaders.add(File(dir, "Anime4K_Restore_CNN_Soft_$q.glsl"))
                shaders.add(File(dir, "Anime4K_Upscale_CNN_x2_$q.glsl"))
                shaders.add(File(dir, "Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(File(dir, "Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.C -> {
                shaders.add(File(dir, "Anime4K_Upscale_Denoise_CNN_x2_$q.glsl"))
                shaders.add(File(dir, "Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(File(dir, "Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.A_PLUS -> {
                shaders.add(File(dir, "Anime4K_Restore_CNN_$q.glsl"))
                shaders.add(File(dir, "Anime4K_Upscale_CNN_x2_$q.glsl"))
                shaders.add(File(dir, "Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(File(dir, "Anime4K_Restore_CNN_$q.glsl"))
                shaders.add(File(dir, "Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.B_PLUS -> {
                shaders.add(File(dir, "Anime4K_Restore_CNN_Soft_$q.glsl"))
                shaders.add(File(dir, "Anime4K_Upscale_CNN_x2_$q.glsl"))
                shaders.add(File(dir, "Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(File(dir, "Anime4K_Restore_CNN_Soft_$q.glsl"))
                shaders.add(File(dir, "Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.C_PLUS -> {
                shaders.add(File(dir, "Anime4K_Upscale_Denoise_CNN_x2_$q.glsl"))
                shaders.add(File(dir, "Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(File(dir, "Anime4K_Restore_CNN_$q.glsl"))
                shaders.add(File(dir, "Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.ARTCNN -> {
                shaders.add(File(dir, "Ani4Kv2_ArtCNN_C4F32_i2_CMP.glsl"))
            }
            Mode.OFF -> Unit
        }

        if (enableDeblur) shaders.add(File(dir, "Anime4K_Deblur_DoG.glsl"))
        if (enableDarken) shaders.add(File(dir, if (quality == Quality.FAST) "Anime4K_Darken_Fast.glsl" else "Anime4K_Darken_HQ.glsl"))
        if (enableThin) shaders.add(File(dir, if (quality == Quality.FAST) "Anime4K_Thin_Fast.glsl" else "Anime4K_Thin_HQ.glsl"))

        return if (shaders.any { !it.exists() }) emptyList() else shaders
    }
}
