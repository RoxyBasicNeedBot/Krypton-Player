package dev.roxy.kryptonplayer.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import dev.roxy.kryptonplayer.core.common.MainTargetTab
import dev.roxy.kryptonplayer.core.common.MediaPickerTabController
import dev.roxy.kryptonplayer.feature.videopicker.navigation.navigateToSearch
import dev.roxy.kryptonplayer.settings.Setting
import dev.roxy.kryptonplayer.feature.videopicker.navigation.navigateToTelegramCloud
import dev.roxy.kryptonplayer.settings.navigation.aboutPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.appearancePreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.audioPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.decoderPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.folderPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.generalPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.librariesScreen
import dev.roxy.kryptonplayer.settings.navigation.mediaLibraryPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.navigateToAboutPreferences
import dev.roxy.kryptonplayer.settings.navigation.navigateToAppearancePreferences
import dev.roxy.kryptonplayer.settings.navigation.gesturePreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.navigateToAudioPreferences
import dev.roxy.kryptonplayer.settings.navigation.navigateToDecoderPreferences
import dev.roxy.kryptonplayer.settings.navigation.navigateToGesturePreferences
import dev.roxy.kryptonplayer.settings.navigation.navigateToFolderPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.navigateToGeneralPreferences
import dev.roxy.kryptonplayer.settings.navigation.navigateToLibraries
import dev.roxy.kryptonplayer.settings.navigation.navigateToMediaLibraryPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.navigateToPlayerPreferences
import dev.roxy.kryptonplayer.settings.navigation.navigateToSubtitlePreferences
import dev.roxy.kryptonplayer.settings.navigation.navigateToLyricsPreferences
import dev.roxy.kryptonplayer.settings.navigation.navigateToThumbnailPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.playerPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.settingsNavigationRoute
import dev.roxy.kryptonplayer.settings.navigation.settingsScreen
import dev.roxy.kryptonplayer.settings.navigation.subtitlePreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.lyricsPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.thumbnailPreferencesScreen
import dev.roxy.kryptonplayer.settings.navigation.statsScreen
import dev.roxy.kryptonplayer.settings.navigation.backupRestoreScreen
import dev.roxy.kryptonplayer.settings.navigation.storageScreen
import dev.roxy.kryptonplayer.settings.navigation.themeCreatorScreen
import dev.roxy.kryptonplayer.settings.navigation.palettePickerScreen
import dev.roxy.kryptonplayer.settings.navigation.navigateToStats
import dev.roxy.kryptonplayer.settings.navigation.navigateToBackupRestore
import dev.roxy.kryptonplayer.settings.navigation.navigateToStorage
import dev.roxy.kryptonplayer.settings.navigation.navigateToThemeCreator
import dev.roxy.kryptonplayer.settings.navigation.navigateToPalettePicker

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
            onSearchClick = {
                MediaPickerTabController.openTab(MainTargetTab.SEARCH)
                if (!navController.popBackStack(SETTINGS_ROUTE, inclusive = true)) {
                    if (!navController.popBackStack(settingsNavigationRoute, inclusive = true)) {
                        navController.navigateUp()
                    }
                }
            },
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
