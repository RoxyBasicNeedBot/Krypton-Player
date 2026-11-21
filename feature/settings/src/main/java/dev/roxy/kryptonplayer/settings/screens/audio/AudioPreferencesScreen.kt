package dev.roxy.kryptonplayer.settings.screens.audio

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.components.ClickablePreferenceItem
import dev.roxy.kryptonplayer.core.ui.components.ListSectionTitle
import dev.roxy.kryptonplayer.core.ui.components.NextTopAppBar
import dev.roxy.kryptonplayer.core.ui.components.PreferenceSwitch
import dev.roxy.kryptonplayer.core.ui.components.RadioTextButton
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.settings.composables.OptionsDialog
import dev.roxy.kryptonplayer.settings.utils.LocalesHelper

@Composable
fun AudioPreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: AudioPreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AudioPreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AudioPreferencesContent(
    uiState: AudioPreferencesUiState,
    onEvent: (AudioPreferencesUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val languages = remember { listOf(Pair("None", "")) + LocalesHelper.getAvailableLocales() }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.audio),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
            ListSectionTitle(text = stringResource(id = R.string.playback))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.preferred_audio_lang),
                    description = LocalesHelper.getLocaleDisplayLanguage(uiState.preferences.preferredAudioLanguage)
                        .takeIf { it.isNotBlank() } ?: stringResource(R.string.preferred_audio_lang_description),
                    icon = NextIcons.Language,
                    onClick = { onEvent(AudioPreferencesUiEvent.ShowDialog(AudioPreferenceDialog.AudioLanguageDialog)) },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = stringResource(R.string.require_audio_focus),
                    description = stringResource(R.string.require_audio_focus_desc),
                    icon = NextIcons.Focus,
                    isChecked = uiState.preferences.requireAudioFocus,
                    onClick = { onEvent(AudioPreferencesUiEvent.ToggleRequireAudioFocus) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.pause_on_headset_disconnect),
                    description = stringResource(id = R.string.pause_on_headset_disconnect_desc),
                    icon = NextIcons.HeadsetOff,
                    isChecked = uiState.preferences.pauseOnHeadsetDisconnect,
                    onClick = { onEvent(AudioPreferencesUiEvent.TogglePauseOnHeadsetDisconnect) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.system_volume_panel),
                    description = stringResource(id = R.string.system_volume_panel_desc),
                    icon = NextIcons.Headset,
                    isChecked = uiState.preferences.showSystemVolumePanel,
                    onClick = { onEvent(AudioPreferencesUiEvent.ToggleShowSystemVolumePanel) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.volume_boost),
                    description = stringResource(id = R.string.volume_boost_desc),
                    icon = NextIcons.VolumeUp,
                    isChecked = uiState.preferences.enableVolumeBoost,
                    onClick = { onEvent(AudioPreferencesUiEvent.ToggleVolumeBoost) },
                    isLastItem = true
                )
            }
        }

        uiState.showDialog?.let { showDialog ->
            when (showDialog) {
                AudioPreferenceDialog.AudioLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.preferred_audio_lang),
                        onDismissClick = { onEvent(AudioPreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(languages) {
                            RadioTextButton(
                                text = it.first,
                                selected = it.second == uiState.preferences.preferredAudioLanguage,
                                onClick = {
                                    onEvent(AudioPreferencesUiEvent.UpdateAudioLanguage(it.second))
                                    onEvent(AudioPreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@PreviewLightDark
@Composable
private fun AudioPreferencesScreenPreview() {
    KryptonPlayerTheme {
        AudioPreferencesContent(
            uiState = AudioPreferencesUiState(),
            onNavigateUp = {},
            onEvent = {},
        )
    }
}
