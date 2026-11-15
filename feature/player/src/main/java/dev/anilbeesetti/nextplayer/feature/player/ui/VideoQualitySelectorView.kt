package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.VideoQualitySelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    qualities: List<String>,
    currentQualityUrl: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayView(
        modifier = modifier,
        show = show,
        title = "Select Video Quality",
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 24.dp)
                .selectableGroup(),
        ) {
            qualities.forEach { qualityUrl ->
                // Basic extraction of quality from URL. E.g. "https://xxx/720.mp4" -> "720p"
                val qualityName = qualityUrl.substringAfterLast("/").substringBeforeLast(".") + "p"
                RadioButtonRow(
                    selected = qualityUrl == currentQualityUrl,
                    text = qualityName,
                    onClick = {
                        onQualitySelected(qualityUrl)
                        onDismiss()
                    },
                )
            }
        }
    }
}
