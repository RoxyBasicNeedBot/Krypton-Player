package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme

@Composable
fun NextSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedIcon: ImageVector = NextIcons.Check,
) {
    val thumbContent: (@Composable () -> Unit)? = if (checked) {
        {
            Icon(
                imageVector = checkedIcon,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        }
    } else {
        null
    }

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = thumbContent,
    )
}

@PreviewLightDark
@Composable
private fun NextSwitchPreview() {
    KryptonPlayerTheme {
        NextSwitch(
            checked = true,
            onCheckedChange = {},
        )
    }
}
