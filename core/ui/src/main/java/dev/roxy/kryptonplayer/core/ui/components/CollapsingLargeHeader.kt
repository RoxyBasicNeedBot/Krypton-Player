package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CollapsingLargeHeader: Apple Music style bold header that gracefully scales and fades.
 */
@Composable
fun CollapsingLargeHeader(
    title: String,
    scrollOffset: Float,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    val collapseFraction = (scrollOffset / 120f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = collapseFraction * 0.95f),
            )
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.invoke()

            Text(
                text = title,
                fontSize = (28 - (8 * collapseFraction)).sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (navigationIcon != null) 12.dp else 0.dp),
            )

            actions?.invoke()
        }
    }
}
