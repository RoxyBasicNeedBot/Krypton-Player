package dev.roxy.kryptonplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions

const val statsPreferencesNavigationRoute = "stats_preferences_route"

fun NavController.navigateToStats(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(statsPreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.statsScreen(onNavigateUp: () -> Unit) {
    composable(route = statsPreferencesNavigationRoute) {
        // We will create and bind StatsScreen here
        dev.roxy.kryptonplayer.settings.screens.stats.StatsScreen(onNavigateUp = onNavigateUp)
    }
}
