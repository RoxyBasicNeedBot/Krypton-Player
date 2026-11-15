package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import dev.anilbeesetti.nextplayer.core.ui.R

@Composable
fun LottieLibraryLogo(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    isPlaying: Boolean = true,
    iterations: Int = LottieConstants.IterateForever,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_library_trophy))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = iterations,
        restartOnPlay = true
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { if (isPlaying) progress else 0f },
                modifier = Modifier.size(size)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.VideoLibrary,
                contentDescription = "Library",
                tint = tint,
                modifier = Modifier.size(size)
            )
        }
    }
}
