package dev.anilbeesetti.nextplayer.core.ui.components

import android.content.res.Resources
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * AnimatedStickerImage: Ultra-lean, memory-efficient animated sticker renderer using
 * Android's hardware-accelerated AnimatedImageDrawable (WebP/GIF) on API 28+
 * with safe automatic cleanup, dynamic resId change support, and zero third-party dependencies.
 */
@Composable
fun AnimatedStickerImage(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription?.let { this.contentDescription = it }
                    loadAndStartAnimation(this, resId)
                }
            },
            update = { imageView ->
                contentDescription?.let { imageView.contentDescription = it }
                val currentResId = imageView.tag as? Int
                if (currentResId != resId || imageView.drawable == null) {
                    loadAndStartAnimation(imageView, resId)
                } else {
                    val drawable = imageView.drawable
                    if (drawable is Animatable && !drawable.isRunning) {
                        drawable.start()
                    }
                }
            },
            onRelease = { imageView ->
                val drawable = imageView.drawable
                if (drawable is Animatable) {
                    drawable.stop()
                }
                imageView.setImageDrawable(null)
                imageView.tag = null
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun loadAndStartAnimation(imageView: ImageView, @DrawableRes resId: Int) {
    val previousDrawable = imageView.drawable
    if (previousDrawable is Animatable) {
        previousDrawable.stop()
    }
    imageView.tag = resId
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val drawable = Api28StickerHelper.decodeAnimatedDrawable(imageView.resources, resId)
            imageView.setImageDrawable(drawable)
            if (drawable is Animatable) {
                drawable.start()
            }
        } else {
            imageView.setImageResource(resId)
        }
    } catch (_: Throwable) {
        imageView.setImageResource(resId)
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private object Api28StickerHelper {
    fun decodeAnimatedDrawable(resources: Resources, @DrawableRes resId: Int): Drawable {
        val source = ImageDecoder.createSource(resources, resId)
        val drawable = ImageDecoder.decodeDrawable(source)
        if (drawable is AnimatedImageDrawable) {
            drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        }
        return drawable
    }
}
