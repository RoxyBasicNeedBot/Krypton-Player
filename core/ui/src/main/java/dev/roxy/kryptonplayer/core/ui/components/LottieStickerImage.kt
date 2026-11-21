package dev.roxy.kryptonplayer.core.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * LottieStickerImage: Ultra-efficient, zero-overhead vector animated sticker renderer
 * using Airbnb Lottie Compose for pure JSON Bodymovin/Telegram stickers.
 *
 * Provides sharp vector rendering at all densities, infinite loop playback,
 * zero bitmap memory leaks, and seamless Compose lifecycle binding.
 */
@Composable
fun LottieStickerImage(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
    isPlaying: Boolean = true,
    iterations: Int = LottieConstants.IterateForever,
    contentDescription: String? = null
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = iterations,
        restartOnPlay = true
    )

    Box(
        modifier = modifier
            .size(size)
            .clipToBounds()
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { if (isPlaying) progress else 0f },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
