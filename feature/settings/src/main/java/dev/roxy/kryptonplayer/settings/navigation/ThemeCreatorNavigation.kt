package dev.roxy.kryptonplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions

const val themeCreatorNavigationRoute = "theme_creator_route"

fun NavController.navigateToThemeCreator(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(themeCreatorNavigationRoute, navOptions)
}

fun NavGraphBuilder.themeCreatorScreen(onNavigateUp: () -> Unit) {
    composable(route = themeCreatorNavigationRoute) {
        dev.roxy.kryptonplayer.settings.screens.appearance.ThemeCreatorScreen(onNavigateUp = onNavigateUp)
    }
}
