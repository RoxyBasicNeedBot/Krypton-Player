package dev.roxy.kryptonplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import dev.roxy.kryptonplayer.settings.screens.appearance.AppearancePreferencesScreen

const val appearancePreferencesNavigationRoute = "appearance_preferences_route"

fun NavController.navigateToAppearancePreferences(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(appearancePreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.appearancePreferencesScreen(
    onNavigateUp: () -> Unit,
    onThemeCreatorClick: () -> Unit,
    onPalettePickerClick: () -> Unit,
) {
    composable(route = appearancePreferencesNavigationRoute) {
        AppearancePreferencesScreen(
            onNavigateUp = onNavigateUp,
            onThemeCreatorClick = onThemeCreatorClick,
            onPalettePickerClick = onPalettePickerClick,
        )
    }
}
