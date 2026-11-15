package dev.anilbeesetti.nextplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions

const val backupRestorePreferencesNavigationRoute = "backup_restore_preferences_route"

fun NavController.navigateToBackupRestore(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(backupRestorePreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.backupRestoreScreen(onNavigateUp: () -> Unit) {
    composable(route = backupRestorePreferencesNavigationRoute) {
        dev.anilbeesetti.nextplayer.settings.screens.general.BackupAndRestoreScreen(onNavigateUp = onNavigateUp)
    }
}
