package dev.anilbeesetti.nextplayer.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import dev.anilbeesetti.nextplayer.core.common.MainTargetTab
import dev.anilbeesetti.nextplayer.core.common.MediaPickerTabController
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.navigateToSearch
import dev.anilbeesetti.nextplayer.settings.Setting
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.navigateToTelegramCloud
import dev.anilbeesetti.nextplayer.settings.navigation.aboutPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.appearancePreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.audioPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.decoderPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.folderPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.generalPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.librariesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.mediaLibraryPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToAboutPreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToAppearancePreferences
import dev.anilbeesetti.nextplayer.settings.navigation.gesturePreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToAudioPreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToDecoderPreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToGesturePreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToFolderPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToGeneralPreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToLibraries
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToMediaLibraryPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToPlayerPreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToSubtitlePreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToLyricsPreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToThumbnailPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.playerPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.settingsNavigationRoute
import dev.anilbeesetti.nextplayer.settings.navigation.settingsScreen
import dev.anilbeesetti.nextplayer.settings.navigation.subtitlePreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.lyricsPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.thumbnailPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.navigation.statsScreen
import dev.anilbeesetti.nextplayer.settings.navigation.backupRestoreScreen
import dev.anilbeesetti.nextplayer.settings.navigation.storageScreen
import dev.anilbeesetti.nextplayer.settings.navigation.themeCreatorScreen
import dev.anilbeesetti.nextplayer.settings.navigation.palettePickerScreen
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToStats
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToBackupRestore
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToStorage
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToThemeCreator
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToPalettePicker

const val SETTINGS_ROUTE = "settings_nav_route"

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
) {
    navigation(
        startDestination = settingsNavigationRoute,
        route = SETTINGS_ROUTE,
    ) {
        settingsScreen(
            onNavigateUp = navController::navigateUp,
            onNavigateToHome = {
                MediaPickerTabController.openTab(MainTargetTab.HOME)
                if (!navController.popBackStack(SETTINGS_ROUTE, inclusive = true)) {
                    if (!navController.popBackStack(settingsNavigationRoute, inclusive = true)) {
                        navController.navigateUp()
                    }
                }
            },
            onNavigateToExplore = {
                MediaPickerTabController.openTab(MainTargetTab.EXPLORE)
                if (!navController.popBackStack(SETTINGS_ROUTE, inclusive = true)) {
                    if (!navController.popBackStack(settingsNavigationRoute, inclusive = true)) {
                        navController.navigateUp()
                    }
                }
            },
            onSearchClick = navController::navigateToSearch,
            onItemClick = { setting ->
                when (setting) {
                    Setting.APPEARANCE -> navController.navigateToAppearancePreferences()
                    Setting.MEDIA_LIBRARY -> navController.navigateToMediaLibraryPreferencesScreen()
                    Setting.PLAYER -> navController.navigateToPlayerPreferences()
                    Setting.GESTURES -> navController.navigateToGesturePreferences()
                    Setting.DECODER -> navController.navigateToDecoderPreferences()
                    Setting.AUDIO -> navController.navigateToAudioPreferences()
                    Setting.SUBTITLE -> navController.navigateToSubtitlePreferences()
                    Setting.LYRICS -> navController.navigateToLyricsPreferences()
                    Setting.GENERAL -> navController.navigateToGeneralPreferences()
                    Setting.STATS -> navController.navigateToStats()
                    Setting.BACKUP_RESTORE -> navController.navigateToBackupRestore()
                    Setting.STORAGE -> navController.navigateToStorage()
                    Setting.WATCH_PARTY -> navController.navigateToTelegramCloud()
                    Setting.ABOUT -> navController.navigateToAboutPreferences()
                }
            },
        )
        appearancePreferencesScreen(
            onNavigateUp = navController::navigateUp,
            onThemeCreatorClick = navController::navigateToThemeCreator,
            onPalettePickerClick = navController::navigateToPalettePicker,
        )
        themeCreatorScreen(
            onNavigateUp = navController::navigateUp,
        )
        palettePickerScreen(
            onNavigateUp = navController::navigateUp,
        )
        statsScreen(
            onNavigateUp = navController::navigateUp,
        )
        backupRestoreScreen(
            onNavigateUp = navController::navigateUp,
        )
        storageScreen(
            onNavigateUp = navController::navigateUp,
        )
        mediaLibraryPreferencesScreen(
            onNavigateUp = navController::navigateUp,
            onFolderSettingClick = navController::navigateToFolderPreferencesScreen,
            onThumbnailSettingClick = navController::navigateToThumbnailPreferencesScreen,
        )
        thumbnailPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        folderPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        playerPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        gesturePreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        decoderPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        audioPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        subtitlePreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        lyricsPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        generalPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        aboutPreferencesScreen(
            onLibrariesClick = navController::navigateToLibraries,
            onNavigateUp = navController::navigateUp,
        )
        librariesScreen(
            onNavigateUp = navController::navigateUp,
        )
    }
}
