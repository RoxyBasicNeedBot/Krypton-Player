package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class EqualizerPreset(val label: String, val gains: List<Float>) {
    FLAT("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("Bass Boost", listOf(6f, 4f, 1f, 0f, 0f)),
    VOCAL_CLARITY("Vocal / Dialogue", listOf(-2f, 1f, 4f, 3f, 1f)),
    CINEMA_3D("Cinema 3D", listOf(5f, 2f, -1f, 3f, 5f)),
    ROCK("Rock", listOf(4f, 2f, -1f, 2f, 4f)),
    POP("Pop", listOf(-1f, 2f, 4f, 2f, -1f)),
    ELECTRONIC("Electronic", listOf(5f, 3f, 0f, 2f, 4f)),
}

/**
 * AudioEqualizerBottomSheet: SimpMusic 5-band graphic equalizer with neon curve graph and sound presets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEqualizerBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    val bandGains = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }
    var selectedPreset by remember { mutableStateOf(EqualizerPreset.FLAT) }
    var bassBoost by remember { mutableFloatStateOf(0f) }
    var virtualizer by remember { mutableFloatStateOf(0f) }

    GlassModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Cinema Equalizer & Audio FX",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconButton(
                    onClick = {
                        selectedPreset = EqualizerPreset.FLAT
                        for (i in bandGains.indices) bandGains[i] = 0f
                        bassBoost = 0f
                        virtualizer = 0f
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Presets Horizontal Pill Row
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EqualizerPreset.entries.forEach { preset ->
                    val isSelected = preset == selectedPreset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            )
                            .clickable {
                                selectedPreset = preset
                                for (i in preset.gains.indices) {
                                    bandGains[i] = preset.gains[i]
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = preset.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Neon Frequency Curve Graph Canvas
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val stepX = width / (bandGains.size - 1)

                val path = Path()
                val points = bandGains.mapIndexed { index, gain ->
                    val x = index * stepX
                    val y = midY - (gain / 10f) * (midY * 0.8f)
                    Offset(x, y)
                }

                path.moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cx = (prev.x + curr.x) / 2f
                    path.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }

                // Draw Neon Glow Wave Line
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(primaryColor, Color(0xFF00E5FF), Color(0xFFFF4081)),
                    ),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // 5 Band Sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                bandGains.indices.forEach { index ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "${bandGains[index].toInt()}dB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Slider(
                            value = bandGains[index],
                            onValueChange = { bandGains[index] = it },
                            valueRange = -10f..10f,
                            modifier = Modifier.height(100.dp),
                        )
                        Text(
                            text = bandLabels[index],
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Bass Boost & 3D Spatial Virtualizer Sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bass Boost: ${(bassBoost * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Slider(
                        value = bassBoost,
                        onValueChange = { bassBoost = it },
                        valueRange = 0f..1f,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3D Virtualizer: ${(virtualizer * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Slider(
                        value = virtualizer,
                        onValueChange = { virtualizer = it },
                        valueRange = 0f..1f,
                    )
                }
            }
        }
    }
}
