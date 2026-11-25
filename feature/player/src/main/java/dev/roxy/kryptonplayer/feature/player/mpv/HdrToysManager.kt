package dev.roxy.kryptonplayer.feature.player.mpv

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class HdrToysManager(
    private val context: Context,
    private val shaderRuntime: MpvShaderRuntime,
) {
    private var initialized = false

    @Volatile
    private var applied = false

    @Synchronized
    fun initialize(): Boolean {
        if (initialized && requiredShadersExist()) return true
        return runCatching {
            val destination = File(context.filesDir, TARGET_DIR)
            destination.mkdirs()
            copyAssetDirectory(ASSET_DIR, destination)
            val ready = requiredShadersExist()
            initialized = ready
            ready
        }.onFailure { error ->
            initialized = false
            Log.w(TAG, "Failed to initialize hdr-toys shaders", error)
        }.getOrDefault(false)
    }

    fun apply(profile: HdrToysProfile): Boolean {
        if (profile == HdrToysProfile.OFF) {
            clear()
            return true
        }
        if (!initialize()) {
            clear()
            return false
        }
        clear()
        HdrToysProfile.mpvPaths(profile).forEach { shaderPath ->
            shaderRuntime.command("change-list", "glsl-shaders", "append", shaderPath)
        }
        applied = true
        return true
    }

    fun clear() {
        if (!applied) return
        applied = false
        HdrToysProfile.allShaderPaths.forEach { relPath ->
            runCatching { shaderRuntime.command("change-list", "glsl-shaders", "remove", "~~/shaders/$relPath") }
            val absPath = File(context.filesDir, "shaders/$relPath").absolutePath
            runCatching { shaderRuntime.command("change-list", "glsl-shaders", "remove", absPath) }
        }
        runCatching {
            val activeShaders = shaderRuntime.getPropertyString("glsl-shaders")
            if (!activeShaders.isNullOrEmpty()) {
                val remaining = activeShaders
                    .split(":")
                    .map { it.trim() }
                    .filter { path -> path.isNotEmpty() && !path.contains("hdr-toys") }
                shaderRuntime.setPropertyString("glsl-shaders", remaining.joinToString(":"))
            }
        }
    }

    private fun requiredShadersExist(): Boolean =
        HdrToysProfile.allShaderPaths.all { shaderPath ->
            val file = File(context.filesDir, "shaders/$shaderPath")
            file.exists() && file.length() > 0L
        }

    private fun copyAssetDirectory(
        assetPath: String,
        destination: File,
    ) {
        val children = context.assets.list(assetPath).orEmpty()
        destination.mkdirs()
        children.forEach { child ->
            val childAssetPath = "$assetPath/$child"
            val childDestination = File(destination, child)
            val nestedChildren = context.assets.list(childAssetPath).orEmpty()
            if (nestedChildren.isEmpty() && child.endsWith(".glsl")) {
                copyAssetFile(childAssetPath, childDestination)
            } else {
                copyAssetDirectory(childAssetPath, childDestination)
            }
        }
    }

    private fun copyAssetFile(
        assetPath: String,
        destination: File,
    ) {
        destination.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }

    private companion object {
        const val TAG = "HdrToysManager"
        const val ASSET_DIR = "shaders/hdr-toys"
        const val TARGET_DIR = "shaders/hdr-toys"
    }
}
