package dev.roxy.kryptonplayer.settings.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.roxy.kryptonplayer.core.model.DecoderPriority
import dev.roxy.kryptonplayer.core.ui.R

@Composable
fun DecoderPriority.name(): String {
    val stringRes = when (this) {
        DecoderPriority.PREFER_DEVICE -> R.string.prefer_device_decoders
        DecoderPriority.PREFER_APP -> R.string.prefer_app_decoders
        DecoderPriority.DEVICE_ONLY -> R.string.device_decoders_only
    }

    return stringResource(id = stringRes)
}
