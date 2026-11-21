package dev.roxy.kryptonplayer.settings.screens.about

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AboutPreferencesScreen(
    onLibrariesClick: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    AboutScreen(
        onLibrariesClick = onLibrariesClick,
        onNavigateUp = onNavigateUp,
        viewModel = viewModel,
    )
}
