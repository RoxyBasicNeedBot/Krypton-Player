package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import dev.anilbeesetti.nextplayer.core.ui.R

@Composable
fun LottieVerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_verified_badge))

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(size)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Verified",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size)
            )
        }
    }
}
