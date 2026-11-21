package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme

@Composable
fun DoneButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(text = stringResource(R.string.done))
    }
}

@Composable
fun CancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(text = stringResource(R.string.cancel))
    }
}

@PreviewLightDark
@Composable
private fun ButtonsPreview() {
    KryptonPlayerTheme {
        Row {
            CancelButton(onClick = {})
            DoneButton(onClick = {})
        }
    }
}
