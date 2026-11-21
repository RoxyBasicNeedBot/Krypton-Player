package dev.roxy.kryptonplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions

const val palettePickerNavigationRoute = "palette_picker_route"

fun NavController.navigateToPalettePicker(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(palettePickerNavigationRoute, navOptions)
}

fun NavGraphBuilder.palettePickerScreen(onNavigateUp: () -> Unit) {
    composable(route = palettePickerNavigationRoute) {
        dev.roxy.kryptonplayer.settings.screens.appearance.PalettePickerScreen(onNavigateUp = onNavigateUp)
    }
}
