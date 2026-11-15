package dev.anilbeesetti.nextplayer.settings.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.anilbeesetti.nextplayer.core.model.ThemeConfig
import dev.anilbeesetti.nextplayer.core.ui.R

@Composable
fun ThemeConfig.name(): String {
    val stringRes = when (this) {
        ThemeConfig.SYSTEM -> R.string.theme_system
        ThemeConfig.OFF -> R.string.theme_light
        ThemeConfig.ON -> R.string.theme_dark
    }

    return stringResource(id = stringRes)
}
