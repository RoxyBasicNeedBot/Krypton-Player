package dev.roxy.kryptonplayer.settings.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.roxy.kryptonplayer.core.model.Font
import dev.roxy.kryptonplayer.core.ui.R

@Composable
fun Font.name(): String {
    val stringRes = when (this) {
        Font.DEFAULT -> R.string.default_name
        Font.MONOSPACE -> R.string.monospace
        Font.SANS_SERIF -> R.string.sans_serif
        Font.SERIF -> R.string.serif
    }

    return stringResource(id = stringRes)
}
