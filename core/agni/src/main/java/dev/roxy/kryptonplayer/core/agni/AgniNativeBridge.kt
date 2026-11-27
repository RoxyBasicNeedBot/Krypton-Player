package dev.roxy.kryptonplayer.core.agni

import androidx.annotation.Keep

/**
 * Private JNI boundary for compiled Agni native binaries (`libkrypton_agni.so`).
 * All dynamic symbols stay hidden except `JNI_OnLoad`, which binds methods via `RegisterNatives`.
 */
@Keep
internal object AgniNativeBridge {
    private val loadAttempted: Boolean by lazy {
        try {
            System.loadLibrary("krypton_agni")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun isLoaded(): Boolean = loadAttempted

    @JvmStatic
    external fun nativeVersion(): Int

    @JvmStatic
    external fun nativeNormalizeTitle(raw: String): String

    @JvmStatic
    external fun nativeScoreCandidate(query: String, title: String): Int

    @JvmStatic
    external fun nativeSanitizeLog(line: String): String
}
