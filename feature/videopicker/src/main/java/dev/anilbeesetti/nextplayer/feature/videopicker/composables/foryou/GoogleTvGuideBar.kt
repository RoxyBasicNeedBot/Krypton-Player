package dev.anilbeesetti.nextplayer.feature.videopicker.composables.foryou

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GoogleTvTab(val title: String, val badge: String? = null) {
    FOR_YOU("For You", "✦"),
    YOUR_STUFF("Your stuff", "★"),
    MOVIES("Movies", null),
    SHOWS("Shows", null),
    HIGHLIGHTS("Highlights", "🔥"),
    FREE_STREAMS("Free & 4K", "4K")
}

/**
 * Signature Google TV-style Guide Bar / Pill Navigation.
 * Designed with authentic JioHotstar Cinema Dark glassmorphism, glowing neon borders,
 * zero default Material 3 styling, and smooth tab switching.
 */
@Composable
fun GoogleTvGuideBar(
    selectedTab: GoogleTvTab,
    onTabSelected: (GoogleTvTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF07090E))
            .horizontalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GoogleTvTab.values().forEach { tab ->
            val isSelected = tab == selectedTab

            val animatedBgColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF161F30) else Color(0xFF0C1019),
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "pill_bg_${tab.name}"
            )

            val animatedBorderColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF00ED82) else Color(0x22FFFFFF),
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "pill_border_${tab.name}"
            )

            val animatedTextColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color(0xFF9EABB8),
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "pill_text_${tab.name}"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(animatedBgColor)
                    .border(
                        width = if (isSelected) 1.2.dp else 0.8.dp,
                        brush = if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00ED82), Color(0xFF0075FF))
                            )
                        } else {
                            Brush.linearGradient(listOf(animatedBorderColor, animatedBorderColor))
                        },
                        shape = RoundedCornerShape(999.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (tab.badge != null) {
                        Text(
                            text = tab.badge,
                            color = if (isSelected) Color(0xFF00ED82) else Color(0xFF7F8C9E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = tab.title,
                        color = animatedTextColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}
