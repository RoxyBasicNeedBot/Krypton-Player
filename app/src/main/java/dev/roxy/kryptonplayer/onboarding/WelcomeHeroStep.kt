package dev.roxy.kryptonplayer.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.ui.preview.DayNightPreview
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.R
import dev.roxy.kryptonplayer.core.ui.theme.JioHotstarFontFamily
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin

/**
 * Single batch-drawn matte dot grid texture.
 */
@Composable
internal fun KryptonWelcomeDots(
    spacing: Dp,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val step = spacing.toPx().coerceAtLeast(8f)
                val dotSize = 1.6.dp.toPx()
                val width = size.width
                val height = size.height
                val points = buildList {
                    var y = step / 2f
                    while (y < height) {
                        var x = step / 2f
                        while (x < width) {
                            add(Offset(x, y))
                            x += step
                        }
                        y += step
                    }
                }
                onDrawBehind {
                    drawPoints(
                        points = points,
                        pointMode = PointMode.Points,
                        color = KryptonOnbColors.Dot,
                        strokeWidth = dotSize,
                        cap = StrokeCap.Round,
                    )
                }
            },
    )
}

/**
 * Rows of giant outlined/hollow welcome words in multiple languages drifting in alternating
 * horizontal directions behind the mascot, driven by a single frame clock with zero recomposition.
 */
@Composable
internal fun KryptonWelcomeWords(
    seconds: FloatState,
    fontSize: Dp,
    firstRowTop: Dp,
    modifier: Modifier = Modifier,
) {
    val words = stringArrayResource(R.array.onb_welcome_words).toList()
    val density = LocalDensity.current
    val family = JioHotstarFontFamily
    val measurer = rememberTextMeasurer(cacheSize = words.size.coerceAtLeast(16))

    val layouts = remember(words, fontSize, density, family, measurer) {
        val style = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = with(density) { fontSize.toSp() },
            letterSpacing = (-0.02).em,
        )
        words.map { word ->
            measurer.measure(
                text = word,
                style = style,
                overflow = TextOverflow.Clip,
                softWrap = false,
                maxLines = 1,
            )
        }
    }

    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .drawWithCache {
                val list = layouts
                val fontPx = fontSize.toPx()
                val gap = fontPx * 0.45f
                val pitch = fontPx * 0.94f
                val top = firstRowTop.toPx()
                val outlineStroke = Stroke(width = 1.dp.toPx())
                val rows = if (list.isEmpty()) 0 else ceil((size.height - top) / pitch).toInt().coerceAtLeast(1)
                val order = Array(rows) { row -> IntArray(words.size) { (it + row * 5) % words.size } }
                val starts = Array(rows) { FloatArray(words.size) }
                var loopLength = 0f
                if (list.isNotEmpty()) {
                    for (row in 0 until rows) {
                        var x = 0f
                        for (i in order[row].indices) {
                            starts[row][i] = x
                            x += list[order[row][i]].size.width + gap
                        }
                        loopLength = x
                    }
                }
                val speed = fontPx * 0.16f

                onDrawBehind {
                    if (list.isEmpty() || loopLength <= 0f) return@onDrawBehind
                    val currentLayouts = list
                    val t = seconds.floatValue

                    for (row in 0 until rows) {
                        val y = top + row * pitch
                        val dir = if (row % 2 == 0) -1f else 1f
                        val rawShift = (t * speed * dir) % loopLength
                        val shift = if (rawShift < 0f) rawShift + loopLength else rawShift

                        for (i in order[row].indices) {
                            val layoutResult = currentLayouts[order[row][i]]
                            val wordWidth = layoutResult.size.width.toFloat()
                            var drawX = starts[row][i] + shift - loopLength
                            while (drawX < size.width + gap) {
                                if (drawX + wordWidth > -gap) {
                                    drawText(
                                        textLayoutResult = layoutResult,
                                        color = KryptonOnbColors.WordOutline,
                                        topLeft = Offset(drawX, y),
                                        drawStyle = outlineStroke,
                                    )
                                }
                                drawX += loopLength
                            }
                        }
                    }
                }
            },
    )
}

@Composable
private fun rememberWelcomeClock(): FloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (startNanos == 0L) startNanos = frameNanos
                seconds.floatValue = (frameNanos - startNanos) / 1_000_000_000f
            }
        }
    }
    return seconds
}

/**
 * Stage 1 — Welcome Hero for Krypton Player:
 * Exact matte #1C1C1C sheet, dot grid, alternating drifting hollow multi-language welcome words,
 * custom Krypton cel-shaded anime mascot (holding a cinema clapperboard, no phone),
 * subtle breathing idle motion, and tap-anywhere advance.
 */
@Composable
internal fun WelcomeHeroStep(
    isHiding: Boolean,
    onNext: () -> Unit,
) {
    val seconds = rememberWelcomeClock()
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    val leave by animateFloatAsState(
        targetValue = if (isHiding) 1f else 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "welcomeLeave",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptonOnbColors.Page)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = stringResource(R.string.onb_next),
                role = Role.Button,
                onClick = onNext,
            )
            .testTag("onboarding_welcome_tap_area"),
    ) {
        val unit = remember(maxWidth, maxHeight) {
            min(maxWidth.value / 400f, maxHeight.value / 820f).coerceIn(0.82f, 1.25f)
        }
        val titleStart = (30f * unit).dp
        val titleTop = (66f * unit).dp
        val wordsTop = (205f * unit).dp
        val wordsFontSize = (104f * unit).dp
        val fadeHeight = (240f * unit).dp

        // Layer 1: Batch-drawn matte dot grid
        KryptonWelcomeDots(spacing = (22f * unit).dp)

        // Layer 2: Alternating drifting hollow welcome words
        KryptonWelcomeWords(
            seconds = seconds,
            fontSize = wordsFontSize,
            firstRowTop = wordsTop,
        )

        // Layer 3: Custom Krypton anime mascot (transparent cutout with gentle breathing idle motion)
        val mascotWidth = (maxWidth * 1.02f).coerceAtMost(500.dp)
        val mascotHeight = (maxHeight * 0.72f).coerceAtMost(640.dp)
        Image(
            painter = painterResource(id = R.drawable.img_krypton_mascot),
            contentDescription = stringResource(R.string.onb_mascot_desc),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (28f * unit).dp)
                .size(width = mascotWidth, height = mascotHeight)
                .graphicsLayer {
                    val t = seconds.floatValue
                    val breathY = sin(t * 1.65f) * 5.dp.toPx()
                    val gentleTilt = sin(t * 0.95f) * 0.85f
                    val enterRise = (1f - enter.value) * 34.dp.toPx()
                    val exitDrop = leave * 56.dp.toPx()
                    translationY = breathY + enterRise + exitDrop
                    rotationZ = gentleTilt
                    alpha = enter.value * (1f - leave)
                },
        )

        // Layer 4: Bottom vertical melt scrim blending mascot cleanly into #1C1C1C
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(fadeHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to KryptonOnbColors.Page.copy(alpha = 0.86f),
                        1f to KryptonOnbColors.Page,
                    ),
                ),
        )

        // Layer 5: Top-left editorial header ("Welcome to / Krypton")
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = titleStart, top = titleTop)
                .graphicsLayer {
                    alpha = enter.value * (1f - leave)
                    translationY = (1f - enter.value) * 18.dp.toPx()
                },
        ) {
            Text(
                text = stringResource(R.string.onb_welcome_to),
                fontFamily = JioHotstarFontFamily,
                color = KryptonOnbColors.WelcomeLead,
                fontSize = (26f * unit).sp,
                lineHeight = (30f * unit).sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = KryptonTitleTracking,
            )
            Text(
                text = stringResource(R.string.onb_brand_name),
                fontFamily = JioHotstarFontFamily,
                color = KryptonOnbColors.Title,
                fontSize = (58f * unit).sp,
                lineHeight = (60f * unit).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = KryptonTitleTracking,
            )
        }

        // Layer 6: Bottom-left closing line ("Let's start watching! / Tap anywhere to continue")
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = titleStart, end = 24.dp, bottom = 30.dp)
                .graphicsLayer {
                    alpha = enter.value * (1f - leave)
                },
        ) {
            Text(
                text = stringResource(R.string.onb_lets_watch),
                fontFamily = JioHotstarFontFamily,
                color = KryptonOnbColors.ClosingLine,
                fontSize = (23f * unit).sp,
                lineHeight = (27f * unit).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = KryptonTitleTracking,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.onb_tap_anywhere),
                fontFamily = JioHotstarFontFamily,
                color = KryptonOnbColors.Muted,
                fontSize = (14f * unit).sp,
                lineHeight = (18f * unit).sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@DayNightPreview
@Composable
private fun WelcomeHeroStepPreview() {
    KryptonPlayerTheme {
        WelcomeHeroStep(
            isHiding = false,
            onNext = {},
        )
    }
}
