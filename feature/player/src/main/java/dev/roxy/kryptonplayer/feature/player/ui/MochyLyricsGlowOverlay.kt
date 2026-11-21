package dev.roxy.kryptonplayer.feature.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.ui.components.MochyFrostedGlass

data class MochyLyricLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null
)

/**
 * Mochy-inspired Active Glow Karaoke Lyrics Overlay for Krypton Player.
 * - Word-by-word active glow line
 * - Dynamic spring scroll centering on active line
 * - Interlude break indicator dots
 * - Frosted glass blurred background backdrop
 * - Interactive line seek on tap
 */
@Composable
fun MochyLyricsGlowOverlay(
    lyrics: List<MochyLyricLine>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isSidePanel: Boolean = false
) {
    val activeIndex = remember(currentPositionMs, lyrics) {
        val idx = lyrics.indexOfLast { it.timeMs <= currentPositionMs }
        if (idx >= 0) idx else 0
    }

    val listState = rememberLazyListState()

    // Smooth spring auto-scroll to keep active line centered
    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    MochyFrostedGlass(
        modifier = modifier
            .fillMaxSize()
            .clip(if (isSidePanel) RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp) else RoundedCornerShape(0.dp)),
        blurRadius = 32.dp,
        tintColor = Color(0xFF04060A).copy(alpha = 0.85f),
        borderColor = Color.White.copy(alpha = 0.08f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(vertical = 120.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isActive = index == activeIndex
                    val isPassed = index < activeIndex

                    val lineScale by animateFloatAsState(
                        targetValue = if (isActive) 1.05f else 0.98f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                        label = "lineScale"
                    )

                    val lineAlpha by animateFloatAsState(
                        targetValue = when {
                            isActive -> 1.0f
                            isPassed -> 0.45f
                            else -> 0.30f
                        },
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        label = "lineAlpha"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color(0xFFBAC5D6),
                        animationSpec = tween(250),
                        label = "textColor"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = lineScale
                                scaleY = lineScale
                                alpha = lineAlpha
                            }
                            .clickable { onSeekTo(line.timeMs) }
                            .padding(vertical = 6.dp)
                    ) {
                        // Main Synced Lyric Line with Active Glow
                        Text(
                            text = line.text,
                            fontSize = if (isActive) 23.sp else 19.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = textColor,
                            lineHeight = 30.sp,
                            textAlign = if (isSidePanel) TextAlign.Start else TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Secondary Translation (if available)
                        if (!line.translation.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = line.translation,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF828A9E),
                                textAlign = if (isSidePanel) TextAlign.Start else TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Interlude Dots indicator if next lyric is > 8 seconds away
                        val nextLine = lyrics.getOrNull(index + 1)
                        if (isActive && nextLine != null && (nextLine.timeMs - line.timeMs) > 8000L) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isSidePanel) Arrangement.Start else Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(3) { dotIdx ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF38B6FF).copy(alpha = 0.7f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top and Bottom Soft Fade Edges (Progressive Seam Fade)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF04060A), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF04060A))
                        )
                    )
            )
        }
    }
}
