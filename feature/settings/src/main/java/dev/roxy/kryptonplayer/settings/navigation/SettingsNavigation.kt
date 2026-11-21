package dev.roxy.kryptonplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import dev.roxy.kryptonplayer.settings.Setting
import dev.roxy.kryptonplayer.settings.SettingsScreen

const val settingsNavigationRoute = "settings_route"

fun NavController.navigateToSettings(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(settingsNavigationRoute, navOptions)
}

fun NavGraphBuilder.settingsScreen(
    onNavigateUp: () -> Unit,
    onItemClick: (Setting) -> Unit,
    onNavigateToHome: () -> Unit = onNavigateUp,
    onNavigateToExplore: () -> Unit = onNavigateUp,
    onSearchClick: () -> Unit = {},
) {
    composable(route = settingsNavigationRoute) {
        SettingsScreen(
            onNavigateUp = onNavigateUp,
            onItemClick = onItemClick,
            onNavigateToHome = onNavigateToHome,
            onNavigateToExplore = onNavigateToExplore,
            onSearchClick = onSearchClick,
        )
    }
}
