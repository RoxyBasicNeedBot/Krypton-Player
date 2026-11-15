package dev.anilbeesetti.nextplayer.core.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ArchiveTune-aligned semantic color tokens and accessors.
 */
object NextColors {
    val ArchiveTuneCoral = Color(0xFFED5564)
    val ArchiveTuneCoralDark = Color(0xFFBA1A32)
    val ArchiveTuneCoralContainer = Color(0xFFFFDAD9)
    val ArchiveTuneCoralContainerDark = Color(0xFF8F0D27)

    val PureBlack = Color(0xFF000000)
    val DarkSurface = Color(0xFF000000)
    val DarkSurfaceVariant = Color(0xFF0D0D0D)
    val DarkSurfaceContainerLow = Color(0xFF050505)
    val DarkSurfaceContainer = Color(0xFF0A0A0A)
    val DarkSurfaceContainerHigh = Color(0xFF121212)
    val DarkSurfaceContainerHighest = Color(0xFF181818)

    val RatingGold = Color(0xFFFFD700)
    val CardBorderDark = Color(0x33FFFFFF)
    val CardBorderLight = Color(0x1F000000)

    @Composable
    fun cardColors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
    ): CardColors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    @Composable
    fun cardBorder(
        color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        width: Dp = 1.dp,
    ): BorderStroke = BorderStroke(width, color)
}
