package dev.roxy.kryptonplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions

const val storagePreferencesNavigationRoute = "storage_preferences_route"

fun NavController.navigateToStorage(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(storagePreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.storageScreen(onNavigateUp: () -> Unit) {
    composable(route = storagePreferencesNavigationRoute) {
        dev.roxy.kryptonplayer.settings.screens.general.StorageSettingsScreen(onNavigateUp = onNavigateUp)
    }
}
