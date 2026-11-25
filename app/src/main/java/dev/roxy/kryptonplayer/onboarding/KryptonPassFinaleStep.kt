package dev.roxy.kryptonplayer.onboarding

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.R
import dev.roxy.kryptonplayer.core.ui.R as CoreUiR
import dev.roxy.kryptonplayer.core.ui.preview.DayNightPreview
import dev.roxy.kryptonplayer.core.ui.theme.JioHotstarFontFamily
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

private const val KRYPTON_PASS_ASPECT = 1.89f
private const val AUTO_ENTER_MILLIS = 3000L

/**
 * Stage 4 — Brushed Silver & Graphite Krypton Member Card Finale:
 * Matte monochrome brushed-silver metallic finish, zero neon, smooth specular sheen sweep,
 * and interactive 3D drag-tilt physics. Displays user's chosen profile name & photo.
 */
@Composable
internal fun KryptonPassFinaleStep(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val profilePrefs = remember { context.getSharedPreferences("krypton_profile_prefs", Context.MODE_PRIVATE) }
    val username = remember {
        profilePrefs.getString("username", null)?.takeIf { it.isNotBlank() } ?: "Cinema Member"
    }
    val profilePicUri = remember {
        profilePrefs.getString("profile_pic_uri", null)
    }

    val scope = rememberCoroutineScope()
    val enter = remember { Animatable(0f) }
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var hasCompleted by remember { mutableStateOf(false) }
    val clockSeconds = remember { mutableFloatStateOf(0f) }

    fun finishOnce() {
        if (!hasCompleted) {
            hasCompleted = true
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        enter.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (startNanos == 0L) startNanos = frameNanos
                clockSeconds.floatValue = (frameNanos - startNanos) / 1_000_000_000f
            }
        }
    }

    // Auto-complete gently after 3 seconds if user isn't actively tilting the card
    LaunchedEffect(isDragging) {
        if (!isDragging && !hasCompleted) {
            delay(AUTO_ENTER_MILLIS)
            if (!isDragging) {
                finishOnce()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptonOnbColors.Page),
    ) {
        val side = rememberOnbSidePadding(maxWidth)
        KryptonWelcomeDots(spacing = 22.dp)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = KryptonOnbColors.MaxContentWidth)
                .fillMaxSize()
                .padding(horizontal = side)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))

            // Interactive 3D Brushed-Silver Krypton Pass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .aspectRatio(KRYPTON_PASS_ASPECT)
                    .graphicsLayer {
                        val t = clockSeconds.floatValue
                        val idleTiltY = sin(t * 1.15f) * 3.5f
                        val idleTiltX = sin(t * 0.85f) * 2.0f
                        cameraDistance = 12f * density
                        rotationY = (tiltY.value + if (isDragging) 0f else idleTiltY).coerceIn(-22f, 22f)
                        rotationX = (tiltX.value + if (isDragging) 0f else idleTiltX).coerceIn(-16f, 16f)
                        scaleX = 0.90f + 0.10f * enter.value
                        scaleY = 0.90f + 0.10f * enter.value
                        alpha = enter.value
                        translationY = (1f - enter.value) * 28.dp.toPx()
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                scope.launch {
                                    tiltX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                                }
                                scope.launch {
                                    tiltY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                scope.launch { tiltX.animateTo(0f, spring()) }
                                scope.launch { tiltY.animateTo(0f, spring()) }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    tiltY.snapTo((tiltY.value + dragAmount.x * 0.10f).coerceIn(-20f, 20f))
                                    tiltX.snapTo((tiltX.value - dragAmount.y * 0.10f).coerceIn(-15f, 15f))
                                }
                            },
                        )
                    },
            ) {
                BrushedSilverKryptonCard(
                    clockSeconds = clockSeconds,
                    username = username,
                    profilePicUri = profilePicUri,
                )
            }

            Spacer(Modifier.height(34.dp))

            Text(
                text = stringResource(R.string.onb_done_title),
                fontFamily = JioHotstarFontFamily,
                color = KryptonOnbColors.Title,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = KryptonTitleTracking,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = enter.value
                    translationY = (1f - enter.value) * 14.dp.toPx()
                },
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onb_done_subtitle),
                fontFamily = JioHotstarFontFamily,
                color = KryptonOnbColors.Muted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = KryptonSubtitleTracking,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = enter.value },
            )

            Spacer(Modifier.weight(1f))

            KryptonOnbPrimaryButton(
                text = stringResource(R.string.onb_done_enter),
                onClick = { finishOnce() },
                testTag = "onboarding_done_enter_button",
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun BrushedSilverKryptonCard(
    clockSeconds: FloatState,
    username: String,
    profilePicUri: String?,
) {
    val cardShape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE7E7E7),
                        Color(0xFFCFCFCF),
                        Color(0xFFDCDCDC),
                        Color(0xFFB5B5B5),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.65f),
                shape = cardShape,
            ),
    ) {
        // Subtle metallic dot texture + diagonal specular sheen sweep
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val step = 12.dp.toPx()
                    val strokeW = 1.2.dp.toPx()
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val dots = buildList {
                        var y = step / 2f
                        while (y < canvasHeight) {
                            var x = step / 2f
                            while (x < canvasWidth) {
                                add(Offset(x, y))
                                x += step
                            }
                            y += step
                        }
                    }
                    val dotColor = Color.Black.copy(alpha = 0.045f)
                    val sheenColors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.42f),
                        Color.Transparent,
                    )

                    onDrawBehind {
                        drawPoints(
                            points = dots,
                            pointMode = PointMode.Points,
                            color = dotColor,
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round,
                        )

                        val sweepPhase = ((clockSeconds.floatValue * 0.42f) % 1.8f) - 0.4f
                        val startX = size.width * sweepPhase
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = sheenColors,
                                start = Offset(startX - size.width * 0.25f, 0f),
                                end = Offset(startX + size.width * 0.25f, size.height),
                            ),
                        )
                    }
                },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1C1C1C)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = CoreUiR.drawable.ic_launcher_monochrome),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "ID CARD",
                            fontFamily = JioHotstarFontFamily,
                            color = Color(0xFF555555),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.em,
                        )
                        Text(
                            text = "KRYPTON PLAYER",
                            fontFamily = JioHotstarFontFamily,
                            color = Color(0xFF151515),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).em,
                        )
                    }
                }

                if (!profilePicUri.isNullOrBlank()) {
                    val model = if (profilePicUri.contains("://")) profilePicUri else "file://$profilePicUri"
                    AsyncImage(
                        model = model,
                        contentDescription = username,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.2.dp, Color(0xFF1C1C1C), CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = "USR#001",
                        fontFamily = JioHotstarFontFamily,
                        color = Color(0xFF424242),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.06.em,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CINEMA MEMBER",
                        fontFamily = JioHotstarFontFamily,
                        color = Color(0xFF505050),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.08.em,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = username,
                        fontFamily = JioHotstarFontFamily,
                        color = Color(0xFF141414),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.03).em,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C1C1C))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "LOCAL · CLOUD",
                        fontFamily = JioHotstarFontFamily,
                        color = Color(0xFFE8E8E8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.06.em,
                    )
                }
            }
        }
    }
}

@DayNightPreview
@Composable
private fun KryptonPassFinaleStepPreview() {
    KryptonPlayerTheme {
        KryptonPassFinaleStep(
            onFinished = {},
        )
    }
}
