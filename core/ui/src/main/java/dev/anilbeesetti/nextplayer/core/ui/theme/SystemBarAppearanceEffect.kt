package dev.anilbeesetti.nextplayer.core.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dynamically controls system status bar and navigation bar icon light/dark appearance.
 * If [isDark] is true (dark surface / immersive content), status and nav bar icons are made LIGHT (white).
 * If [isDark] is false (light surface), status and nav bar icons are made DARK.
 * Automatically restores previous state upon leaving composition.
 */
@Composable
fun SystemBarAppearanceEffect(isDark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(isDark) {
            val window = (view.context as? Activity)?.window
                ?: run {
                    var ctx: Context? = view.context
                    while (ctx is ContextWrapper) {
                        if (ctx is Activity) return@run ctx.window
                        ctx = ctx.baseContext
                    }
                    null
                }
            val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
            val previousStatus = insetsController?.isAppearanceLightStatusBars
            val previousNav = insetsController?.isAppearanceLightNavigationBars

            // isAppearanceLightStatusBars = true -> dark icons (for light backgrounds)
            // isAppearanceLightStatusBars = false -> light icons (for dark backgrounds)
            insetsController?.isAppearanceLightStatusBars = !isDark
            insetsController?.isAppearanceLightNavigationBars = !isDark

            onDispose {
                if (previousStatus != null) insetsController.isAppearanceLightStatusBars = previousStatus
                if (previousNav != null) insetsController.isAppearanceLightNavigationBars = previousNav
            }
        }
    }
}
