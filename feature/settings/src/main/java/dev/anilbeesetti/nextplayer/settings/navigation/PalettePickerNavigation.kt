package dev.anilbeesetti.nextplayer.settings.navigation

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
        dev.anilbeesetti.nextplayer.settings.screens.appearance.PalettePickerScreen(onNavigateUp = onNavigateUp)
    }
}
