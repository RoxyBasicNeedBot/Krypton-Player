package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.ui.liquidglass.PlatformBackdrop

/**
 * CollapsingLargeHeader: Mochi / Apple Music style bold header that gracefully scales,
 * eases into focus, and activates live frosted backdrop blur on scroll.
 */
@Composable
fun CollapsingLargeHeader(
    title: String,
    scrollOffset: Float,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    backdrop: PlatformBackdrop? = null,
) {
    val rawFraction = (scrollOffset / 120f).coerceIn(0f, 1f)
    val collapseFraction = MochyMotion.EaseOut.transform(rawFraction)

    MochyScrollBlurHeader(
        scrollFraction = collapseFraction,
        modifier = modifier,
        backdrop = backdrop,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.invoke()

            Text(
                text = title,
                fontSize = (28f - (8f * collapseFraction)).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5f + 0.25f * collapseFraction).sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        translationY = (1f - collapseFraction) * 2f
                    }
                    .padding(horizontal = if (navigationIcon != null) 12.dp else 0.dp),
            )

            actions?.invoke()
        }
    }
}
