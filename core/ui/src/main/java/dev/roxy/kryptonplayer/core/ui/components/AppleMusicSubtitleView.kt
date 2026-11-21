package dev.roxy.kryptonplayer.core.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.ui.theme.LocalAppColors

/**
 * Data model for a synchronized subtitle line with timestamps.
 */
data class SubtitleLineItem(
    val id: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val translation: String? = null,
)

/**
 * AppleMusicSubtitleView: Synchronized subtitle viewer with depth-of-field blur on inactive lines
 * and live glowing highlight on the active dialogue line. Tapping any line seeks the player.
 */
@Composable
fun AppleMusicSubtitleView(
    lines: List<SubtitleLineItem>,
    currentPositionMs: Long,
    onSeekToTimestamp: (Long) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = LocalAppColors.current.lyricActive,
) {
    val listState = rememberLazyListState()

    val activeIndex = remember(lines, currentPositionMs) {
        lines.indexOfFirst { line ->
            currentPositionMs in line.startMs..line.endMs
        }.takeIf { it >= 0 } ?: lines.indexOfLast { it.startMs <= currentPositionMs }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(
                index = (activeIndex - 1).coerceAtLeast(0),
                scrollOffset = 0,
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 60.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(lines, key = { index, item -> "${item.id}_$index" }) { index, line ->
            val isActive = index == activeIndex

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 0.95f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "subScale",
            )

            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1.0f else 0.40f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "subAlpha",
            )

            val textColor by animateColorAsState(
                targetValue = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "subTextColor",
            )

            val blurRadius = if (!isActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 2.dp else 0.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSeekToTimestamp(line.startMs) },
                    )
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = line.text,
                        color = textColor.copy(alpha = alpha),
                        fontSize = 20.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                        lineHeight = 28.sp,
                    )
                    if (!line.translation.isNullOrEmpty()) {
                        Text(
                            text = line.translation,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
                            fontSize = 15.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
        }
    }
}
