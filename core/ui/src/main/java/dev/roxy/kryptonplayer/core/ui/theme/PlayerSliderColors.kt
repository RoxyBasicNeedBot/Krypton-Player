package dev.roxy.kryptonplayer.core.ui.theme

import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Player slider color configuration for consistent styling across all slider types
 *
 * This object provides standardized color schemes for Default, Squiggly, and Slim sliders
 * used in the player interface, ensuring visual consistency and proper contrast.
 */
object PlayerSliderColors {
    @Composable
    fun getSliderColors(
        activeColor: Color,
        inactiveAlpha: Float = 0.25f,
    ): SliderColors =
        SliderDefaults.colors(
            activeTrackColor = activeColor,
            activeTickColor = activeColor,
            thumbColor = activeColor,
            inactiveTrackColor = activeColor.copy(alpha = inactiveAlpha),
        )

    @Composable
    fun standardSliderColors(buttonColor: Color): SliderColors =
        getSliderColors(
            activeColor = buttonColor,
            inactiveAlpha = Config.INACTIVE_TRACK_ALPHA,
        )

    @Composable
    fun wavySliderColors(buttonColor: Color): SliderColors =
        SliderDefaults.colors(
            activeTrackColor = buttonColor,
            activeTickColor = buttonColor,
            thumbColor = Color.Transparent,
            inactiveTrackColor = buttonColor.copy(alpha = Config.INACTIVE_TRACK_ALPHA),
            inactiveTickColor = buttonColor.copy(alpha = Config.INACTIVE_TICK_ALPHA),
        )

    @Composable
    fun thickSliderColors(buttonColor: Color): SliderColors =
        getSliderColors(
            activeColor = buttonColor,
            inactiveAlpha = Config.THICK_INACTIVE_TRACK_ALPHA,
        )

    @Composable
    fun circularSliderColors(buttonColor: Color): SliderColors =
        SliderDefaults.colors(
            activeTrackColor = buttonColor,
            activeTickColor = buttonColor,
            thumbColor = buttonColor,
            inactiveTrackColor = buttonColor.copy(alpha = Config.INACTIVE_TRACK_ALPHA),
        )

    @Composable
    fun simpleSliderColors(buttonColor: Color): SliderColors =
        SliderDefaults.colors(
            activeTrackColor = buttonColor.copy(alpha = Config.SIMPLE_ACTIVE_TRACK_ALPHA),
            activeTickColor = buttonColor.copy(alpha = Config.SIMPLE_ACTIVE_TRACK_ALPHA),
            thumbColor = Color.Transparent,
            inactiveTrackColor = buttonColor.copy(alpha = Config.SIMPLE_INACTIVE_TRACK_ALPHA),
            inactiveTickColor = buttonColor.copy(alpha = Config.SIMPLE_INACTIVE_TRACK_ALPHA),
        )

    object Config {
        const val INACTIVE_TRACK_ALPHA = 0.25f
        const val INACTIVE_TICK_ALPHA = 0.25f
        const val THICK_INACTIVE_TRACK_ALPHA = 0.25f
        const val SIMPLE_ACTIVE_TRACK_ALPHA = 0.85f
        const val SIMPLE_INACTIVE_TRACK_ALPHA = 0.20f
    }
}
