package dev.anilbeesetti.nextplayer.settings.screens.appearance

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitch
import dev.anilbeesetti.nextplayer.core.ui.components.RadioTextButton
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.core.ui.theme.PremiumMeshBackground
import dev.anilbeesetti.nextplayer.settings.composables.OptionsDialog

@Composable
fun AppearancePreferencesScreen(
    onNavigateUp: () -> Unit,
    onThemeCreatorClick: () -> Unit = {},
    onPalettePickerClick: () -> Unit = {},
    viewModel: AppearancePreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppearancePreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppearancePreferencesContent(
    uiState: AppearancePreferencesUiState,
    onEvent: (AppearancePreferencesEvent) -> Unit,
    onNavigateUp: () -> Unit = {},
) {
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }

    PremiumMeshBackground(
        enabled = uiState.preferences.enableMeshBackground,
    ) {
        Scaffold(
            topBar = {
                NextTopAppBar(
                    title = stringResource(id = R.string.appearance_name),
                    navigationIcon = {
                        FilledTonalIconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = NextIcons.ArrowBack,
                                contentDescription = stringResource(id = R.string.navigate_up),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    ListSectionTitle(text = stringResource(id = R.string.appearance_name))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        // Permanent / Always On Dark Theme Option
                        PreferenceItem(
                            title = stringResource(id = R.string.dark_theme),
                            description = stringResource(id = R.string.dark_theme_desc),
                            icon = Icons.Rounded.DarkMode,
                            enabled = true,
                            onClick = {},
                            isFirstItem = true,
                            trailingContent = {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.always_on),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            },
                        )

                        // Pure Black / AMOLED Surface Option for OLED Battery Saving
                        PreferenceSwitch(
                            title = stringResource(id = R.string.pure_black_theme),
                            description = stringResource(id = R.string.pure_black_theme_desc),
                            icon = NextIcons.Contrast,
                            isChecked = uiState.preferences.pureBlack,
                            onClick = { onEvent(AppearancePreferencesEvent.TogglePureBlack) },
                            isLastItem = true,
                        )
                    }

                    ListSectionTitle(text = "Navigation Bar")
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        val currentNavbarLabel = if (uiState.preferences.navigationBarStyle == "FLOATING_GLASS") {
                            "Floating Glass Pill"
                        } else {
                            "Cinema Curved Dock"
                        }
                        ClickablePreferenceItem(
                            title = "Navigation Bar Style",
                            description = currentNavbarLabel,
                            icon = Icons.Rounded.Navigation,
                            onClick = { onEvent(AppearancePreferencesEvent.ShowDialog(AppearancePreferenceDialog.NavigationBarStyle)) },
                            isFirstItem = true,
                            isLastItem = true,
                        )
                    }

                    ListSectionTitle(text = stringResource(id = R.string.player_name))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        ClickablePreferenceItem(
                            title = "Player Slider Style",
                            description = uiState.preferences.playerSliderStyle.lowercase().replaceFirstChar { it.uppercase() },
                            icon = Icons.Rounded.LinearScale,
                            onClick = { onEvent(AppearancePreferencesEvent.ShowDialog(AppearancePreferenceDialog.SliderStyle)) },
                            isFirstItem = true,
                            isLastItem = true,
                        )
                    }

                    if (uiState.showDialog != null) {
                        when (uiState.showDialog) {
                            AppearancePreferenceDialog.SliderStyle -> {
                                OptionsDialog(
                                    text = "Player Slider Style",
                                    onDismissClick = { onEvent(AppearancePreferencesEvent.ShowDialog(null)) },
                                    options = {
                                        items(listOf("STANDARD", "WAVY", "THICK", "SIMPLE")) { style ->
                                            RadioTextButton(
                                                text = style.lowercase().replaceFirstChar { it.uppercase() },
                                                selected = (style == uiState.preferences.playerSliderStyle.uppercase()),
                                                onClick = {
                                                    onEvent(AppearancePreferencesEvent.UpdatePlayerSliderStyle(style))
                                                    onEvent(AppearancePreferencesEvent.ShowDialog(null))
                                                },
                                            )
                                        }
                                    }
                                )
                            }
                            AppearancePreferenceDialog.NavigationBarStyle -> {
                                OptionsDialog(
                                    text = "Navigation Bar Style",
                                    onDismissClick = { onEvent(AppearancePreferencesEvent.ShowDialog(null)) },
                                    options = {
                                        val styles = listOf(
                                            "CURVED_DOCK" to "Cinema Curved Dock",
                                            "FLOATING_GLASS" to "Floating Glass Pill"
                                        )
                                        items(styles) { (styleKey, styleTitle) ->
                                            RadioTextButton(
                                                text = styleTitle,
                                                selected = (styleKey == uiState.preferences.navigationBarStyle),
                                                onClick = {
                                                    if (styleKey != uiState.preferences.navigationBarStyle) {
                                                        onEvent(AppearancePreferencesEvent.UpdateNavigationBarStyle(styleKey))
                                                        showRestartDialog = true
                                                    }
                                                    onEvent(AppearancePreferencesEvent.ShowDialog(null))
                                                },
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (showRestartDialog) {
                        val context = LocalContext.current
                        AlertDialog(
                            onDismissRequest = { showRestartDialog = false },
                            title = {
                                Text(
                                    text = "Restart App to Apply Style",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = "Krypton Player needs to restart for the new navigation bar layout to take effect cleanly."
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showRestartDialog = false
                                        restartApp(context)
                                    }
                                ) {
                                    Text(
                                        text = "Restart Now",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRestartDialog = false }) {
                                    Text("Later")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AppearancePreferencesScreenPreview() {
    NextPlayerTheme {
        AppearancePreferencesContent(
            uiState = AppearancePreferencesUiState(),
            onEvent = {},
        )
    }
}

private fun restartApp(context: Context) {
    try {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val restartIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(restartIntent)
        Runtime.getRuntime().exit(0)
    } catch (_: Exception) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
