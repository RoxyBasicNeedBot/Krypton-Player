package dev.roxy.kryptonplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import dev.roxy.kryptonplayer.settings.screens.lyrics.LyricsPreferencesScreen

const val lyricsPreferencesNavigationRoute = "lyrics_preferences_route"

fun NavController.navigateToLyricsPreferences(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(lyricsPreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.lyricsPreferencesScreen(onNavigateUp: () -> Unit) {
    composable(route = lyricsPreferencesNavigationRoute) {
        LyricsPreferencesScreen(onNavigateUp = onNavigateUp)
    }
}
