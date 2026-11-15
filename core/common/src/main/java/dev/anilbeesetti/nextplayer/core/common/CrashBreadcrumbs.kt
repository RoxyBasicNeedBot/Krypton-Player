package dev.anilbeesetti.nextplayer.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object CrashBreadcrumbs {
    private const val MAX_BREADCRUMBS = 120
    private val breadcrumbs = ConcurrentLinkedQueue<BreadcrumbEntry>()

    data class BreadcrumbEntry(
        val timestamp: Long,
        val category: String,
        val message: String,
    ) {
        override fun toString(): String {
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
            return "[$time] [$category] $message"
        }
    }

    fun record(category: String, message: String) {
        // Offline breadcrumb recording disabled
    }

    fun recordClick(iconOrTarget: String, screen: String = "", extra: String = "") {
        val details = buildString {
            append("Clicked: $iconOrTarget")
            if (screen.isNotBlank()) append(" on $screen")
            if (extra.isNotBlank()) append(" ($extra)")
        }
        record("CLICK", details)
    }

    fun recordNav(screenName: String) {
        record("NAV", "Opened Screen / Tab: $screenName")
    }

    fun recordAction(action: String) {
        record("UI", action)
    }

    fun recordCompose(component: String, phase: String) {
        record("COMPOSE", "$phase: $component")
    }

    fun recordGesture(gesture: String, target: String) {
        record("GESTURE", "$gesture on $target")
    }

    fun recordDownload(videoName: String, state: String) {
        record("DOWNLOAD", "$state: $videoName")
    }

    fun recordPlayer(videoName: String, state: String) {
        record("PLAYER", "$state: $videoName")
    }

    fun recordError(tag: String, error: String) {
        record("ERROR", "[$tag] $error")
    }

    fun lastClick(): String? {
        return breadcrumbs.toList().asReversed()
            .firstOrNull { it.category == "CLICK" }
            ?.message
    }

    fun lastClickEntry(): BreadcrumbEntry? {
        return breadcrumbs.toList().asReversed()
            .firstOrNull { it.category == "CLICK" }
    }

    fun lastNav(): String? {
        return breadcrumbs.toList().asReversed()
            .firstOrNull { it.category == "NAV" }
            ?.message
            ?.removePrefix("Opened Screen / Tab: ")
    }

    fun lastAction(): String? {
        return breadcrumbs.toList().asReversed()
            .firstOrNull { it.category in listOf("UI", "PLAYER", "DOWNLOAD", "GESTURE", "COMPOSE") }
            ?.message
    }

    fun dump(): String {
        return buildString {
            if (breadcrumbs.isEmpty()) {
                appendLine("No recent actions recorded.")
            } else {
                for (b in breadcrumbs) {
                    appendLine(b.toString())
                }
            }
        }
    }

    fun getEntries(): List<BreadcrumbEntry> {
        return breadcrumbs.toList()
    }
}
