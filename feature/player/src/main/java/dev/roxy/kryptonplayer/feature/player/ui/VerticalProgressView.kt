package dev.roxy.kryptonplayer.feature.player.ui

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme

/**
 * 1:1 color definitions from mpvrx (app.gyrolet.mpvrx.ui.theme.Color.kt)
 * - MpvrxPrimary: Lavender purple (0xFFE8B5EF) for icon and base slider end
 * - MpvrxPrimaryContainer: Deep violet purple (0xFF5F3768) for base slider start
 * - MpvrxBoostError: Vivid error container red (0xFFFF5449) for boost overflow
 */
val MpvrxPrimary = Color(0xFFFFFFFF)
val MpvrxPrimaryContainer = Color(0xFFB4B8C4)
val MpvrxBoostError = Color(0xFFFF5449)

private const val NORMAL_MAX_PERCENTAGE = 100

fun percentage(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
): Float = if (range.endInclusive == range.start) 0f else ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

fun percentage(
    value: Int,
    range: ClosedRange<Int>,
): Float = if (range.endInclusive == range.start) 0f else ((value - range.start - 0f) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

@Composable
fun VerticalSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    overflowValue: Float? = null,
    overflowRange: ClosedFloatingPointRange<Float>? = null,
    colorStart: Color = MpvrxPrimaryContainer,
    colorEnd: Color = MpvrxPrimary,
    overflowColor: Color = MpvrxBoostError,
) {
    val coercedValue = value.coerceIn(range)
    val gradientBrush = remember(colorStart, colorEnd) { Brush.verticalGradient(listOf(colorStart, colorEnd)) }
    Box(
        modifier =
        modifier
            .height(130.dp)
            .width(36.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xD90A0A0D))
            .border(0.9.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val targetHeight by animateFloatAsState(
            percentage(coercedValue, range),
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
            label = "vsliderheight",
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(targetHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(gradientBrush),
        )
        if (overflowRange != null && overflowValue != null) {
            val overflowHeight by animateFloatAsState(
                percentage(overflowValue, overflowRange),
                label = "vslideroverflowheight",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(overflowHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(overflowColor),
            )
        }
    }
}

@Composable
fun VerticalSlider(
    value: Int,
    range: ClosedRange<Int>,
    modifier: Modifier = Modifier,
    overflowValue: Int? = null,
    overflowRange: ClosedRange<Int>? = null,
    colorStart: Color = MpvrxPrimaryContainer,
    colorEnd: Color = MpvrxPrimary,
    overflowColor: Color = MpvrxBoostError,
) {
    val coercedValue = value.coerceIn(range)
    val gradientBrush = remember(colorStart, colorEnd) { Brush.verticalGradient(listOf(colorStart, colorEnd)) }
    Box(
        modifier =
        modifier
            .height(130.dp)
            .width(36.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xD90A0A0D))
            .border(0.9.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val targetHeight by animateFloatAsState(
            percentage(coercedValue, range),
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
            label = "vsliderheight",
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(targetHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(gradientBrush),
        )
        if (overflowRange != null && overflowValue != null) {
            val overflowHeight by animateFloatAsState(
                percentage(overflowValue, overflowRange),
                label = "vslideroverflowheight",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(overflowHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(overflowColor),
            )
        }
    }
}

@Composable
fun BrightnessSlider(
    brightness: Float,
    positiveRange: ClosedFloatingPointRange<Float> = 0f..1f,
    negativeRange: ClosedFloatingPointRange<Float> = 0f..0.75f,
    modifier: Modifier = Modifier,
    colorStart: Color = MpvrxPrimaryContainer,
    colorEnd: Color = MpvrxPrimary,
    overflowColor: Color = MpvrxBoostError,
    customIcon: Painter? = null,
) {
    val coercedBrightness = brightness.coerceIn(-negativeRange.endInclusive, positiveRange.endInclusive)
    val percentInt = (coercedBrightness * 100).toInt()
    val percentText = remember(percentInt) { "$percentInt%" }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = percentText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 48.dp),
            )
            VerticalSlider(
                coercedBrightness.coerceIn(0f, positiveRange.endInclusive),
                positiveRange,
                overflowValue = (-coercedBrightness).coerceIn(0f, negativeRange.endInclusive),
                overflowRange = negativeRange,
                colorStart = colorStart,
                colorEnd = colorEnd,
                overflowColor = overflowColor,
            )
            if (customIcon != null) {
                Icon(
                    painter = customIcon,
                    contentDescription = null,
                    tint = colorEnd,
                )
            } else {
                Icon(
                    imageVector = when {
                        coercedBrightness < 0 -> Icons.Rounded.Brightness6
                        percentage(coercedBrightness, positiveRange) <= 0.3f -> Icons.Rounded.BrightnessLow
                        percentage(coercedBrightness, positiveRange) <= 0.6f -> Icons.Rounded.BrightnessMedium
                        else -> Icons.Rounded.BrightnessHigh
                    },
                    contentDescription = null,
                    tint = colorEnd,
                )
            }
        }
    }
}

@Composable
fun VolumeSlider(
    volume: Int,
    volumePercentage: Int,
    mpvVolume: Int,
    range: ClosedRange<Int>,
    boostRange: ClosedRange<Int>?,
    modifier: Modifier = Modifier,
    displayAsPercentage: Boolean = false,
    colorStart: Color = MpvrxPrimaryContainer,
    colorEnd: Color = MpvrxPrimary,
    overflowColor: Color = MpvrxBoostError,
    customIcon: Painter? = null,
) {
    val percentage = volumePercentage.coerceIn(0, 100)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val boostVolume = mpvVolume - 100
            val textStr = getVolumeSliderText(volume, mpvVolume, boostVolume, percentage, displayAsPercentage)
            val volumeText = remember(textStr, displayAsPercentage) {
                textStr + if (displayAsPercentage && !textStr.contains('%')) "%" else ""
            }
            Text(
                text = volumeText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 48.dp),
            )
            VerticalSlider(
                if (displayAsPercentage) percentage else volume,
                if (displayAsPercentage) 0..100 else range,
                overflowValue = boostVolume,
                overflowRange = boostRange,
                colorStart = colorStart,
                colorEnd = colorEnd,
                overflowColor = overflowColor,
            )
            if (customIcon != null) {
                Icon(
                    painter = customIcon,
                    contentDescription = null,
                    tint = colorEnd,
                )
            } else {
                Icon(
                    imageVector = when (percentage) {
                        0 -> Icons.AutoMirrored.Rounded.VolumeOff
                        in 0..30 -> Icons.Rounded.VolumeMute
                        in 30..60 -> Icons.AutoMirrored.Rounded.VolumeDown
                        in 60..100 -> Icons.AutoMirrored.Rounded.VolumeUp
                        else -> Icons.AutoMirrored.Rounded.VolumeOff
                    },
                    contentDescription = null,
                    tint = colorEnd,
                )
            }
        }
    }
}

fun getVolumeSliderText(
    volume: Int,
    mpvVolume: Int,
    boostVolume: Int,
    percentage: Int,
    displayAsPercentage: Boolean,
): String {
    return when {
        mpvVolume == 100 ->
            if (displayAsPercentage) {
                "$percentage"
            } else {
                "$volume"
            }

        mpvVolume > 100 -> {
            if (displayAsPercentage) {
                "${percentage + boostVolume}"
            } else {
                "${volume + boostVolume}"
            }
        }

        else -> {
            if (displayAsPercentage) {
                "$percentage"
            } else {
                "$volume"
            }
        }
    }
}

/**
 * Vertical progress indicator for volume/brightness matching mpvrx 1:1.
 *
 * @param value Current percentage value (0 to maxValue)
 * @param maxValue Maximum value (100 for normal, 200 for boosted volume)
 * @param isVolume True if indicating volume, false if brightness
 * @param colorStart Top gradient color for base track
 * @param colorEnd Bottom gradient color and icon tint
 * @param boostColor Overflow track color for volume boost above 100%
 * @param icon Optional custom icon override
 */
@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    @IntRange(from = 0, to = 200) value: Int,
    maxValue: Int = NORMAL_MAX_PERCENTAGE,
    isVolume: Boolean = true,
    colorStart: Color = MpvrxPrimaryContainer,
    colorEnd: Color = MpvrxPrimary,
    boostColor: Color = MpvrxBoostError,
    icon: Painter? = null,
) {
    if (isVolume) {
        val normalVol = value.coerceIn(0, 100)
        val boostVol = (value - 100).coerceAtLeast(0)
        val hasBoost = maxValue > 100
        val boostCap = if (hasBoost) maxValue - 100 else 0
        VolumeSlider(
            volume = normalVol,
            volumePercentage = normalVol,
            mpvVolume = 100 + boostVol,
            range = 0..100,
            boostRange = if (hasBoost && (boostCap > 0 || boostVol > 0)) 0..maxOf(boostCap, boostVol) else null,
            modifier = modifier,
            displayAsPercentage = true,
            colorStart = colorStart,
            colorEnd = colorEnd,
            overflowColor = boostColor,
            customIcon = icon,
        )
    } else {
        val brightnessFraction = (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
        BrightnessSlider(
            brightness = brightnessFraction,
            positiveRange = 0f..1f,
            negativeRange = 0f..0.75f,
            modifier = modifier,
            colorStart = colorStart,
            colorEnd = colorEnd,
            overflowColor = boostColor,
            customIcon = icon,
        )
    }
}

@Preview
@Composable
private fun VerticalProgressPreview() {
    KryptonPlayerTheme {
        VerticalProgressView(
            value = 50,
            isVolume = true,
        )
    }
}

@Preview
@Composable
private fun VerticalProgressBoostPreview() {
    KryptonPlayerTheme {
        VerticalProgressView(
            value = 150,
            maxValue = 200,
            isVolume = true,
        )
    }
}

@Preview
@Composable
private fun VerticalProgressBrightnessPreview() {
    KryptonPlayerTheme {
        VerticalProgressView(
            value = 75,
            isVolume = false,
        )
    }
}
