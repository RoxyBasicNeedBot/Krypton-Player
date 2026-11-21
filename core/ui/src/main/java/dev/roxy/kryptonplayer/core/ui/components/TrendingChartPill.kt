package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.ui.theme.LocalIsDarkTheme

/**
 * TrendingChartPill: Warm-amber tinted glass pill button for quick access to trending charts.
 */
@Composable
fun TrendingChartPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Top 10 Trending This Week",
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor = if (isDark) Color(0xFF1E140A) else Color(0xFFFFF3E0)
    val borderColor = if (isDark) Color(0xFF5D3F1A) else Color(0xFFFFCC80)
    val labelColor = if (isDark) Color(0xFFFFD54F) else Color(0xFFE65100)

    Surface(
        modifier = modifier
            .bouncyPress()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LottieTelegramSparkles(size = 18.dp)
            Text(
                text = label,
                fontSize = 13.sp,
                color = labelColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
