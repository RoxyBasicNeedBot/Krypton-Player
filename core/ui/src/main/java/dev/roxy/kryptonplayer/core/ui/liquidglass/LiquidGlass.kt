package dev.roxy.kryptonplayer.core.ui.liquidglass

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.sign
import com.kyant.backdrop.backdrops.layerBackdrop as nativeBackdrop

/**
 * The backdrop a glass surface samples from (Kyant Backdrop API).
 */
typealias PlatformBackdrop = LayerBackdrop

/** Marks a composable as the source layer that sibling glass surfaces refract. */
fun Modifier.layerBackdrop(backdrop: PlatformBackdrop): Modifier = this.nativeBackdrop(backdrop)

@Composable
fun rememberBackdrop(color: Color = MaterialTheme.colorScheme.surface): PlatformBackdrop =
    rememberLayerBackdrop {
        drawRect(color)
        drawContent()
    }

fun Modifier.drawBackdropCustomShape(
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
    isDark: Boolean = true,
): Modifier =
    this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            runCatching {
                val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
                vibrancy()
                colorControls(
                    brightness = 0.05f,
                    contrast = 1f,
                    saturation = 1.5f,
                )
                blur(
                    if (l > 0f) {
                        lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                    } else {
                        lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                    },
                )
                lens(24f.dp.toPx(), size.minDimension / 2f, true)
            }
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            layer.record { drawBackdrop() }
        },
        shape = { shape },
        onDrawSurface = {
            val darken = lerp(0.12f, 0.5f, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
            drawRect((if (isDark) Color.Black else Color.White).copy(alpha = if (isDark) darken else darken * 0.4f))
        },
    )
