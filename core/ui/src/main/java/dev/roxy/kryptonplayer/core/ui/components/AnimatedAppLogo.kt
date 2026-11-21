package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.ui.R

@Suppress("UNUSED_PARAMETER")
@Composable
fun AnimatedAppLogo(
    modifier: Modifier = Modifier,
    useGradient: Boolean = true,
    singleColor: Color = Color.White,
    animateOnEntry: Boolean = true,
    allowTapToReplay: Boolean = true,
    externalTrigger: Int = 0,
) {
    Image(
        painter = painterResource(R.drawable.app_logo_monochrome),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(singleColor),
        modifier = modifier,
    )
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun LaserAppLogoBadge(
    modifier: Modifier = Modifier,
    containerSize: Dp = 62.dp,
    logoSize: Dp = 46.dp,
    cornerRadius: Dp = 18.dp,
    borderWidth: Dp = 1.dp,
    borderAlpha: Float = 0.5f,
    containerColor: Color = Color.Black,
    borderColors: List<Color>? = null,
    animateOnEntry: Boolean = true,
    allowTapToReplay: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(shape)
            .background(containerColor)
            .border(borderWidth, Color.White.copy(alpha = borderAlpha), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo_monochrome),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(logoSize),
        )
    }
}
