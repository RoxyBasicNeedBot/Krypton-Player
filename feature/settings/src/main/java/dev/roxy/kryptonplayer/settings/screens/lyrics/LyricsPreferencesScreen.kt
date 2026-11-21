package dev.roxy.kryptonplayer.settings.screens.lyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.roxy.kryptonplayer.core.ui.components.CancelButton
import dev.roxy.kryptonplayer.core.ui.components.ClickablePreferenceItem
import dev.roxy.kryptonplayer.core.ui.components.ListSectionTitle
import dev.roxy.kryptonplayer.core.ui.components.NextDialog
import dev.roxy.kryptonplayer.core.ui.components.NextTopAppBar
import dev.roxy.kryptonplayer.core.ui.components.PreferenceSlider
import dev.roxy.kryptonplayer.core.ui.components.PreferenceSwitch
import dev.roxy.kryptonplayer.core.ui.components.RadioTextButton
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.preview.DayNightPreview
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.settings.composables.OptionsDialog
import kotlin.math.roundToInt

private data class LyricsModeOption(
    val key: String,
    val label: String,
    val description: String,
)

@Composable
fun LyricsPreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: LyricsPreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LyricsPreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LyricsPreferencesContent(
    uiState: LyricsPreferencesUiState,
    onEvent: (LyricsPreferencesUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val modes = remember {
        listOf(
            LyricsModeOption("ENHANCED", "Live Karaoke (Enhanced)", "Active line glow, spring scale & focus blur"),
            LyricsModeOption("V2", "Synced Line-by-Line", "Standard smooth synced line scrolling"),
            LyricsModeOption("PLAIN", "Plain Text", "Simple un-synced full lyrics view"),
        )
    }

    val currentModeLabel = remember(uiState.preferences.lyricsMode) {
        modes.firstOrNull { it.key == uiState.preferences.lyricsMode }?.label ?: "Live Karaoke (Enhanced)"
    }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = "Lyrics",
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // --- Section: Display & Styling ---
            ListSectionTitle(text = "Display & Styling")

            ClickablePreferenceItem(
                title = "Lyrics Display Mode",
                description = currentModeLabel,
                icon = NextIcons.Subtitle,
                isFirstItem = true,
                isLastItem = false,
                onClick = { onEvent(LyricsPreferencesUiEvent.ShowDialog(LyricsPreferenceDialog.MODE)) },
            )

            PreferenceSlider(
                title = "Text Size",
                description = "${uiState.preferences.lyricsTextSize} sp",
                icon = NextIcons.Font,
                isFirstItem = false,
                isLastItem = false,
                value = uiState.preferences.lyricsTextSize.toFloat(),
                valueRange = 16f..36f,
                onValueChange = { onEvent(LyricsPreferencesUiEvent.UpdateLyricsTextSize(it.roundToInt())) },
            )

            PreferenceSlider(
                title = "Line Spacing",
                description = "${(uiState.preferences.lyricsLineSpacing * 10).roundToInt() / 10f}x",
                icon = NextIcons.Tune,
                isFirstItem = false,
                isLastItem = false,
                value = uiState.preferences.lyricsLineSpacing,
                valueRange = 1.0f..2.0f,
                onValueChange = { onEvent(LyricsPreferencesUiEvent.UpdateLyricsLineSpacing((it * 10).roundToInt() / 10f)) },
            )

            PreferenceSwitch(
                title = "Focus Line Blur",
                description = "Subtly blur inactive lines for an atmospheric karaoke experience",
                icon = NextIcons.Contrast,
                isChecked = uiState.preferences.lyricsLineBlur,
                onClick = { onEvent(LyricsPreferencesUiEvent.ToggleLyricsLineBlur) },
                isFirstItem = false,
                isLastItem = false,
            )

            PreferenceSwitch(
                title = "Auto Scroll",
                description = "Automatically keep current playing line centered",
                icon = NextIcons.Star,
                isChecked = uiState.preferences.lyricsAutoScroll,
                onClick = { onEvent(LyricsPreferencesUiEvent.ToggleLyricsAutoScroll) },
                isFirstItem = false,
                isLastItem = false,
            )

            PreferenceSwitch(
                title = "Tap Line to Seek",
                description = "Tap any lyric line to immediately jump player to that position",
                icon = NextIcons.Speed,
                isChecked = uiState.preferences.lyricsClickToSeek,
                onClick = { onEvent(LyricsPreferencesUiEvent.ToggleLyricsClickToSeek) },
                isFirstItem = false,
                isLastItem = false,
            )

            PreferenceSwitch(
                title = "Romanize Non-Latin Lyrics",
                description = "Show phonetic pronunciation for Japanese, Korean, Chinese & Hindi songs",
                icon = NextIcons.Language,
                isChecked = uiState.preferences.lyricsRomanize,
                onClick = { onEvent(LyricsPreferencesUiEvent.ToggleLyricsRomanize) },
                isFirstItem = false,
                isLastItem = true,
            )

            // --- Section: Online Providers ---
            ListSectionTitle(text = "Online Lyrics Providers")

            PreferenceSwitch(
                title = "BetterLyrics (QQ / Kugou HD)",
                description = "High-precision word-by-word synced TTML karaoke lyrics",
                icon = NextIcons.Cloud,
                isChecked = uiState.preferences.enableBetterLyrics,
                onClick = { onEvent(LyricsPreferencesUiEvent.ToggleBetterLyrics) },
                isFirstItem = true,
                isLastItem = false,
            )

            PreferenceSwitch(
                title = "LRCLIB Provider",
                description = "Extensive open-source community synced lyrics library",
                icon = NextIcons.DownloadVector,
                isChecked = uiState.preferences.enableLrcLib,
                onClick = { onEvent(LyricsPreferencesUiEvent.ToggleLrcLib) },
                isFirstItem = false,
                isLastItem = false,
            )

            PreferenceSwitch(
                title = "KuGou Music",
                description = "Massive Asian and international music synced lyrics catalog",
                icon = NextIcons.Music,
                isChecked = uiState.preferences.enableKugou,
                onClick = { onEvent(LyricsPreferencesUiEvent.ToggleKuGou) },
                isFirstItem = false,
                isLastItem = false,
            )

            PreferenceSwitch(
                title = "Paxsenix (Apple Music / Spotify)",
                description = "Direct Apple Music & Spotify synced lyrics mirror",
                icon = NextIcons.Tune,
                isChecked = uiState.preferences.enablePaxsenix,
                onClick = { onEvent(LyricsPreferencesUiEvent.TogglePaxsenix) },
                isFirstItem = false,
                isLastItem = false,
            )

            PreferenceSwitch(
                title = "NetEase Cloud Music",
                description = "Comprehensive Asian pop & international synced lyrics",
                icon = NextIcons.History,
                isChecked = uiState.preferences.enableNetEase,
                onClick = { onEvent(LyricsPreferencesUiEvent.ToggleNetEase) },
                isFirstItem = false,
                isLastItem = true,
            )

            // --- Section: Cache & Storage ---
            ListSectionTitle(text = "Cache & Storage")

            ClickablePreferenceItem(
                title = "Clear Lyrics Cache",
                description = "Clear in-memory and temporary downloaded lyrics cache",
                icon = NextIcons.Delete,
                isFirstItem = true,
                isLastItem = true,
                onClick = { onEvent(LyricsPreferencesUiEvent.ShowDialog(LyricsPreferenceDialog.CLEAR_CACHE)) },
            )
        }
    }

    when (uiState.showDialog) {
        LyricsPreferenceDialog.MODE -> {
            OptionsDialog(
                text = "Lyrics Display Mode",
                onDismissClick = { onEvent(LyricsPreferencesUiEvent.ShowDialog(null)) },
            ) {
                items(modes) { mode ->
                    RadioTextButton(
                        text = "${mode.label}\n${mode.description}",
                        selected = uiState.preferences.lyricsMode == mode.key,
                        onClick = {
                            onEvent(LyricsPreferencesUiEvent.UpdateLyricsMode(mode.key))
                            onEvent(LyricsPreferencesUiEvent.ShowDialog(null))
                        },
                    )
                }
            }
        }
        LyricsPreferenceDialog.CLEAR_CACHE -> {
            NextDialog(
                onDismissRequest = { onEvent(LyricsPreferencesUiEvent.ShowDialog(null)) },
                title = { Text(text = "Clear Lyrics Cache") },
                content = {
                    Text(
                        text = "Are you sure you want to clear all cached lyrics? They will be re-fetched when playing media.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                dismissButton = {
                    CancelButton(onClick = { onEvent(LyricsPreferencesUiEvent.ShowDialog(null)) })
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEvent(LyricsPreferencesUiEvent.ClearLyricsCache)
                            onEvent(LyricsPreferencesUiEvent.ShowDialog(null))
                        },
                    ) {
                        Text(text = "Clear", color = MaterialTheme.colorScheme.error)
                    }
                },
            )
        }
        else -> Unit
    }
}

@DayNightPreview
@Composable
private fun LyricsPreferencesScreenPreview() {
    KryptonPlayerTheme {
        LyricsPreferencesContent(
            uiState = LyricsPreferencesUiState(),
            onEvent = {},
            onNavigateUp = {},
        )
    }
}
