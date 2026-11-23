package dev.roxy.kryptonplayer.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PremiumMeshBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = LocalMeshBackgroundEnabled.current,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        content()
    }
}
