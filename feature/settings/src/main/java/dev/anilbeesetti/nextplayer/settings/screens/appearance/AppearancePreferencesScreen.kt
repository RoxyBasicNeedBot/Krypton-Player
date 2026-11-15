package dev.anilbeesetti.nextplayer.settings.screens.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.ThemeConfig
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextSegmentedListItem
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitch
import dev.anilbeesetti.nextplayer.core.ui.components.RadioTextButton
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.core.ui.theme.LocalIsDarkTheme
import dev.anilbeesetti.nextplayer.core.ui.theme.supportsDynamicTheming
import dev.anilbeesetti.nextplayer.core.ui.theme.PremiumMeshBackground
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.anilbeesetti.nextplayer.settings.composables.OptionsDialog
import dev.anilbeesetti.nextplayer.settings.extensions.name
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Navigation
import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppearancePreferencesScreen(
    onNavigateUp: () -> Unit,
    onThemeCreatorClick: () -> Unit,
    onPalettePickerClick: () -> Unit,
    viewModel: AppearancePreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppearancePreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
        onThemeCreatorClick = onThemeCreatorClick,
        onPalettePickerClick = onPalettePickerClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppearancePreferencesContent(
    uiState: AppearancePreferencesUiState,
    onEvent: (AppearancePreferencesEvent) -> Unit,
    onNavigateUp: () -> Unit = {},
    onThemeCreatorClick: () -> Unit = {},
    onPalettePickerClick: () -> Unit = {},
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
                ThemeModeSelectorCard(
                    selectedTheme = uiState.preferences.themeConfig,
                    onThemeSelected = { config ->
                        onEvent(AppearancePreferencesEvent.UpdateThemeConfig(config))
                    },
                    onCardClick = { onEvent(AppearancePreferencesEvent.ShowDialog(AppearancePreferenceDialog.Theme)) },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = stringResource(R.string.high_contrast_dark_theme),
                    description = stringResource(R.string.high_contrast_dark_theme_desc),
                    icon = NextIcons.Contrast,
                    isChecked = uiState.preferences.useHighContrastDarkTheme,
                    onClick = { onEvent(AppearancePreferencesEvent.ToggleUseHighContrastDarkTheme) },
                    enabled = uiState.preferences.themeConfig != ThemeConfig.OFF,
                )
                PreferenceSwitch(
                    title = stringResource(R.string.pure_black_theme),
                    description = stringResource(R.string.pure_black_theme_desc),
                    icon = NextIcons.Contrast,
                    isChecked = uiState.preferences.pureBlack,
                    onClick = { onEvent(AppearancePreferencesEvent.TogglePureBlack) },
                    enabled = uiState.preferences.themeConfig != ThemeConfig.OFF,
                )
                PreferenceSwitch(
                    title = stringResource(R.string.dynamic_theme),
                    description = stringResource(R.string.dynamic_theme_description),
                    icon = NextIcons.Appearance,
                    isChecked = uiState.preferences.useDynamicColors,
                    onClick = { onEvent(AppearancePreferencesEvent.ToggleUseDynamicColors) },
                    enabled = supportsDynamicTheming(),
                )
                PreferenceSwitch(
                    title = stringResource(R.string.premium_mesh_background),
                    description = stringResource(R.string.premium_mesh_background_desc),
                    icon = NextIcons.Appearance,
                    isChecked = uiState.preferences.enableMeshBackground,
                    onClick = { onEvent(AppearancePreferencesEvent.ToggleEnableMeshBackground) },
                    isLastItem = true,
                )
            }

            ListSectionTitle(text = "Custom seeds & layout options")
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = "Seed Palette Creator",
                    description = "Choose or construct custom theme seed colors",
                    icon = NextIcons.Appearance,
                    onClick = onThemeCreatorClick,
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = "Preset Palettes",
                    description = "Pick preset theme palettes (Synthwave, Sage Green...)",
                    icon = NextIcons.Appearance,
                    onClick = onPalettePickerClick,
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
                    AppearancePreferenceDialog.Theme -> {
                        OptionsDialog(
                            text = stringResource(id = R.string.choose_theme),
                            onDismissClick = { onEvent(AppearancePreferencesEvent.ShowDialog(null)) },
                            options = {
                                ThemeConfig.entries.forEach { config ->
                                    item {
                                        RadioTextButton(
                                            text = config.name(),
                                            selected = (config == uiState.preferences.themeConfig),
                                            onClick = {
                                                onEvent(AppearancePreferencesEvent.UpdateThemeConfig(config))
                                                onEvent(AppearancePreferencesEvent.ShowDialog(null))
                                            },
                                        )
                                    }
                                }
                            }
                        )
                    }
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
                            text = "Next Player needs to restart for the new navigation bar layout to take effect cleanly."
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeModeSelectorCard(
    selectedTheme: ThemeConfig,
    onThemeSelected: (ThemeConfig) -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = true,
) {
    val isDark = LocalIsDarkTheme.current
    NextSegmentedListItem(
        modifier = modifier,
        isFirstItem = isFirstItem,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (selectedTheme) {
                        ThemeConfig.OFF -> Icons.Rounded.LightMode
                        ThemeConfig.ON -> Icons.Rounded.DarkMode
                        ThemeConfig.SYSTEM -> Icons.Rounded.BrightnessAuto
                    },
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        content = {
            Text(
                text = stringResource(id = R.string.app_theme),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = selectedTheme.name(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isDark) {
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.3f else 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ThemeSegmentPill(
                        title = stringResource(R.string.theme_light),
                        icon = Icons.Rounded.LightMode,
                        selected = selectedTheme == ThemeConfig.OFF,
                        onClick = { onThemeSelected(ThemeConfig.OFF) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeSegmentPill(
                        title = stringResource(R.string.theme_dark),
                        icon = Icons.Rounded.DarkMode,
                        selected = selectedTheme == ThemeConfig.ON,
                        onClick = { onThemeSelected(ThemeConfig.ON) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeSegmentPill(
                        title = stringResource(R.string.theme_system),
                        icon = Icons.Rounded.BrightnessAuto,
                        selected = selectedTheme == ThemeConfig.SYSTEM,
                        onClick = { onThemeSelected(ThemeConfig.SYSTEM) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        onClick = onCardClick,
    )
}

@Composable
private fun ThemeSegmentPill(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else if (isDark) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "themeSegmentBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "themeSegmentContent"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color = backgroundColor,
        contentColor = contentColor,
        border = if (selected) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.40f)
        ),
        modifier = modifier.height(38.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
    } catch (e: Exception) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
