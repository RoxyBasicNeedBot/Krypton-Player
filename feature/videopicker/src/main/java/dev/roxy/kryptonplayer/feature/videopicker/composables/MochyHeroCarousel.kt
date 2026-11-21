package dev.roxy.kryptonplayer.feature.videopicker.composables

import android.net.Uri
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.roxy.kryptonplayer.core.common.extensions.prettyName
import dev.roxy.kryptonplayer.core.model.Video
import dev.roxy.kryptonplayer.core.ui.components.press3D
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Mochy-calibrated hero pacing & physics
private val HeroSlideEasing = CubicBezierEasing(0.32f, 0f, 0.15f, 1f)
private const val HeroSlideMillis = 1150
private const val HeroAdvanceMillis = 5500L
private val HeroPillContent = Color(0xFF2F2F2F)
private val HeroPillFill = Color.White.copy(alpha = 0.72f)
private val HeroLabelColor = Color.White.copy(alpha = 0.75f)

private const val PageMediaTravel = 0.18f
private const val PageTextTravel = 0.42f

// Mochy Gacha Reel Constants (scaled from 527f design)
private const val GachaDesignHeight = 310f
private const val GachaItemSize = 135f
private const val GachaFocusSize = 165f
private const val GachaPitch = 150f
private const val GachaFocusGap = 12f
private const val GachaCenterY = 145f
private const val GachaOverhang = 18f
private const val GachaCornerRadius = 18f
private const val GachaPointerX = 90f
private const val GachaPointerY = 150f
private const val GachaPointerWidth = 46f
private const val GachaPointerHeight = 52f

private val GachaRollEasing = CubicBezierEasing(0.15f, 0.45f, 0.2f, 1f)
private val GachaPointerColor = Color(0xFFF0908D)
private val GachaPipColor = Color(0xFF3C3C3C)

@Immutable
data class MochyHeroLabel(
    val text: String,
    val coverUrl: String? = null,
    val subject: String? = null
)

private enum class HeroPillKind { Play, Pause, View, Roll, Rolling, Shuffle }

/**
 * Mochy-Grade Hero Carousel for Krypton Player Home Tab:
 * - Card 0: "Continue Playing" Banner with dynamic progress and play/pause pill
 * - Card 1: "Gacha here! / Random plays" with 3D cylindrical drum reel, coral pointer kickback & slot machine physics
 * - Card 2: "Surprise Me / Discover" instant library shuffle card
 *
 * Includes Mochy's asymmetric spring drift, user-drag curved wipe, and dynamic pill indicators.
 */
@Composable
fun MochyHeroCarousel(
    videos: List<Video>,
    onVideoClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) return

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    // Mochy exact: 60% of screen height (HeroHeightFraction = 0.6f)
    val heroHeightDp: Dp = (screenHeight * 0.60f).coerceIn(320.dp, 560.dp)

    val lastPlayed = videos.firstOrNull()
    val gachaCandidates = remember(videos) { videos.shuffled().take(12) }

    val pageCount = if (videos.size >= 2) 3 else 1
    val isSingleItem = pageCount == 1

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pageCount }
    )

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    var userSwipe by remember { mutableStateOf(false) }
    LaunchedEffect(isDragged) { if (isDragged) userSwipe = true }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.collect { inProgress ->
            if (!inProgress) userSwipe = false
        }
    }
    val wipeCurve by animateFloatAsState(
        targetValue = if (userSwipe) 1f else 0f,
        animationSpec = tween(260),
        label = "heroWipeCurve"
    )

    // Gacha State surviving composition
    val gacha = remember { MochyGachaState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(gachaCandidates) {
        if (gacha.phase != MochyGachaPhase.Idle) return@LaunchedEffect
        gacha.candidates = gachaCandidates
    }

    // Pointer kickback & haptic tick as reel passes covers
    LaunchedEffect(gacha) {
        var lastBuzz = 0L
        snapshotFlow { gacha.reel.value.roundToInt() }
            .drop(1)
            .collect {
                if (gacha.phase != MochyGachaPhase.Rolling) return@collect
                val now = SystemClock.uptimeMillis()
                if (now - lastBuzz >= 40L) {
                    lastBuzz = now
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                launch {
                    gacha.pointerKick.snapTo(1f)
                    gacha.pointerKick.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 900f))
                }
            }
    }

    fun rollGacha() {
        if (gacha.phase == MochyGachaPhase.Rolling || gacha.candidates.size < 2) return
        scope.launch {
            gacha.phase = MochyGachaPhase.Rolling
            try {
                val count = gacha.candidates.size
                val winner = Random.nextInt(count)
                val duration = Random.nextInt(3500, 5200)
                val start = gacha.reel.value.roundToInt()
                gacha.reel.snapTo(start.toFloat())
                val spins = count * 3
                val landing = start + spins + (winner - (start + spins)).mod(count)
                gacha.reel.animateTo(landing.toFloat(), tween(duration, easing = GachaRollEasing))

                val won = gacha.candidates[winner]
                gacha.result = won
                gacha.phase = MochyGachaPhase.Landed
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                launch {
                    gacha.landPop.snapTo(1f)
                    gacha.landPop.animateTo(1.1f, tween(120))
                    gacha.landPop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 320f))
                }
                launch {
                    gacha.burst.snapTo(0.001f)
                    gacha.burst.animateTo(1f, tween(1000, easing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)))
                    gacha.burst.snapTo(0f)
                }
                delay(600)
                onVideoClick(won.uriString.toUri())
            } finally {
                if (gacha.phase == MochyGachaPhase.Rolling) gacha.phase = MochyGachaPhase.Idle
            }
        }
    }

    // Auto-advance loop (pauses while dragged or while rolling)
    if (!isSingleItem) {
        LaunchedEffect(pagerState, isDragged, gacha.phase) {
            if (isDragged || gacha.phase == MochyGachaPhase.Rolling) return@LaunchedEffect
            while (true) {
                val onGacha = pagerState.currentPage == 1
                if (onGacha && gacha.phase != MochyGachaPhase.Idle) return@LaunchedEffect
                delay(if (onGacha) HeroAdvanceMillis * 2 else HeroAdvanceMillis)
                val nextPage = (pagerState.currentPage + 1) % pageCount
                pagerState.animateScrollToPage(
                    nextPage,
                    animationSpec = tween(HeroSlideMillis, easing = HeroSlideEasing)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeightDp)
                // Mochy: full-bleed clip, no visible border
                .clip(RoundedCornerShape(0.dp))
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isSingleItem && gacha.phase != MochyGachaPhase.Rolling,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageOffset = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction

                when (page) {
                    0 -> {
                        // Card 0: Continue Playing
                        ContinuePage(
                            video = lastPlayed ?: videos.first(),
                            pageOffset = { pageOffset },
                            onPlayClick = {
                                onVideoClick((lastPlayed ?: videos.first()).uriString.toUri())
                            }
                        )
                    }
                    1 -> {
                        // Card 1: Gacha Random Play
                        GachaPage(
                            state = gacha,
                            pageOffset = { pageOffset },
                            onRoll = { rollGacha() },
                            onResultClick = {
                                gacha.result?.let { onVideoClick(it.uriString.toUri()) }
                            }
                        )
                    }
                    else -> {
                        // Card 2: Surprise Me / Library Shuffle
                        ShuffleHeroPage(
                            totalVideos = videos.size,
                            pageOffset = { pageOffset },
                            onShuffleClick = {
                                val random = videos.random()
                                onVideoClick(random.uriString.toUri())
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mochy Dynamic Pill Pager Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val isSelected = pagerState.currentPage == index
                val targetWidth = if (isSelected) 26.dp else 8.dp
                val animWidth by animateFloatAsState(
                    targetValue = targetWidth.value,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                    label = "pillWidth"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(animWidth.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isSelected) Color(0xFF38B6FF) else Color.White.copy(alpha = 0.18f)
                        )
                )
            }
        }
    }
}

/** Card 0: Mochy Continue Page with Drift and Hero Scrims */
@Composable
private fun ContinuePage(
    video: Video,
    pageOffset: () -> Float,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    val prettyTitle = remember(video) {
        video.displayName.takeIf { it.isNotBlank() } ?: File(video.path).nameWithoutExtension
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Video Backdrop with Mochy Drift
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(video.uriString.takeIf { it.isNotEmpty() } ?: video.path)
                .crossfade(300)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .mochyMediaDrift(pageOffset)
        )

        // Hero Multi-Stop Dark Scrims
        MochyHeroScrims()

        // Hero Text + Pill Drift
        Box(
            modifier = Modifier
                .fillMaxSize()
                .mochyTextDrift(pageOffset)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 20.dp, bottom = 64.dp)
            ) {
                // Label line: "Continue" badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Continue",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38B6FF)
                    )
                    if (video.formattedDuration.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "• ${video.formattedDuration}",
                            fontSize = 12.5.sp,
                            color = HeroLabelColor
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))

                // Title
                Text(
                    text = prettyTitle,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Optional Progress bar
                if (video.playedPercentage in 0.01f..0.99f) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { video.playedPercentage },
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(3.dp)
                            .clip(CircleShape),
                        color = Color(0xFF38B6FF),
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Mochy 33dp Translucent Hero Pill
                MochyHeroPill(
                    kind = HeroPillKind.Play,
                    label = "Play",  // Mochy: "Play" not "Resume"
                    onClick = onPlayClick
                )
            }
        }
    }
}

/** Card 1: Full Mochy Gacha 3D Drum Reel, Pointer Kickback & Burst Ring */
@Composable
private fun GachaPage(
    state: MochyGachaState,
    pageOffset: () -> Float,
    onRoll: () -> Unit,
    onResultClick: () -> Unit
) {
    val rolling = state.phase == MochyGachaPhase.Rolling
    val result = state.result
    val candidates = state.candidates
    val context = LocalContext.current

    val speed = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state) {
        var lastNanos = 0L
        var lastReel = state.reel.value
        while (isActive) {
            val now = SystemClock.uptimeMillis()
            val dt = if (lastNanos == 0L) 1f / 60f else ((now - lastNanos) / 1000f).coerceIn(0f, 0.1f)
            lastNanos = now
            val reel = state.reel.value
            val instant = abs(reel - lastReel) / dt.coerceAtLeast(1e-3f)
            speed.floatValue += (instant - speed.floatValue) * 0.15f
            lastReel = reel
            delay(16)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val unit = maxHeight / GachaDesignHeight
        val focus = unit * GachaFocusSize
        val coverShape = RoundedCornerShape(unit * GachaCornerRadius)
        val count = candidates.size

        // Atmospheric Deep Radial Background Wash
        Box(
            modifier = Modifier
                .fillMaxSize()
                .mochyMediaDrift(pageOffset)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF281438), Color(0xFF13172C), Color(0xFF07090E)),
                        center = Offset(Float.POSITIVE_INFINITY, 0f),
                        radius = 900f
                    )
                )
        )

        // Win Burst Ring Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val win = state.burst.value
            if (win > 0f && win < 1f) {
                val focusPx = focus.toPx()
                val overhangPx = (unit * GachaOverhang).toPx()
                val glowX = size.width + overhangPx - focusPx / 2f
                val glowY = (unit * GachaCenterY).toPx()

                drawCircle(
                    color = Color(0xFF38B6FF).copy(alpha = (1f - win) * 0.9f),
                    radius = focusPx * (0.5f + win * 1.1f),
                    center = Offset(glowX, glowY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = ((1f - win) * 8f + 1f).dp.toPx()
                    )
                )
            }
        }

        // Mochy 3D Cylindrical Drum Reel Covers
        candidates.forEachIndexed { index, video ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.uriString.takeIf { it.isNotEmpty() } ?: video.path)
                    .crossfade(200)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = unit * GachaOverhang, y = unit * GachaCenterY - focus / 2)
                    .size(focus)
                    .graphicsLayer {
                        val d = reelDistance(index, state.reel.value, count)
                        val near = (1f - abs(d)).coerceIn(0f, 1f)
                        val landed = if (near > 0.5f) state.landPop.value else 1f
                        val scale = (GachaItemSize + (GachaFocusSize - GachaItemSize) * near) / GachaFocusSize * landed
                        val stretch = 1f + (speed.floatValue / 120f).coerceAtMost(0.1f)

                        scaleX = scale
                        scaleY = scale * stretch
                        transformOrigin = TransformOrigin(1f, 0.5f)

                        val slots = d * GachaPitch + GachaFocusGap * d.coerceIn(-1f, 1f)
                        translationY = slots * unit.toPx()
                        rotationX = (-d * 18f).coerceIn(-50f, 50f)
                        cameraDistance = 14f * density

                        alpha = (2.7f - abs(d)).coerceIn(0f, 1f)
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        shape = coverShape
                        clip = true
                    }
            )
        }

        // Hero Multi-Stop Dark Scrims over Reel
        MochyHeroScrims()

        // Coral Pointer with Kickback
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = GachaPointerColor,
            modifier = Modifier
                .offset(
                    x = unit * (GachaPointerX - GachaPointerWidth / 2f),
                    y = unit * (GachaPointerY - GachaPointerHeight / 2f),
                )
                .size(width = unit * GachaPointerWidth, height = unit * GachaPointerHeight)
                .graphicsLayer {
                    val kick = state.pointerKick.value
                    translationX = -kick * 7.dp.toPx()
                    rotationZ = -kick * 9f
                    val win = state.burst.value
                    val pop = if (win > 0f) 1f + 0.18f * sin(win * PI.toFloat()) else 1f
                    scaleX = pop
                    scaleY = pop
                }
        )

        // Clickable hotspot over the center drum cover
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = unit * GachaOverhang, y = unit * GachaCenterY - focus / 2)
                .size(focus)
                .clickable(enabled = !rolling, onClick = onRoll)
        )

        // Left-side Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .mochyTextDrift(pageOffset)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 20.dp, bottom = 64.dp)
            ) {
                Text(
                    text = when {
                        result == null || rolling -> "Gacha here!"
                        else -> "You got"
                    },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF5252)
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = if (result == null || rolling) "Random plays" else result.displayName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(14.dp))

                // Roll Pill with Tumbling Die
                MochyHeroPill(
                    kind = if (rolling) HeroPillKind.Rolling else HeroPillKind.Roll,
                    label = if (rolling) "rolling!!" else "roll!!",
                    enabled = !rolling,
                    onClick = onRoll
                )
            }
        }
    }
}

/** Card 2: Surprise Me / Library Shuffle */
@Composable
private fun ShuffleHeroPage(
    totalVideos: Int,
    pageOffset: () -> Float,
    onShuffleClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0F1E36), Color(0xFF091424), Color(0xFF07090E))
                )
            )
    ) {
        MochyHeroScrims()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .mochyTextDrift(pageOffset)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 20.dp, bottom = 64.dp)
            ) {
                Text(
                    text = "Discover • $totalVideos items",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00ED82)
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "Surprise Me",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Jump directly into an unwatched library pick.",
                    fontSize = 12.5.sp,
                    color = HeroLabelColor,
                    maxLines = 1
                )

                Spacer(Modifier.height(14.dp))

                MochyHeroPill(
                    kind = HeroPillKind.Shuffle,
                    label = "Shuffle Play",
                    onClick = onShuffleClick
                )
            }
        }
    }
}

/** Mochy's Signature 33dp Translucent Hero Pill with Scale/Spring Glyphs */
@Composable
private fun MochyHeroPill(
    kind: HeroPillKind,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .press3D(maxTilt = 3f, pressedScale = 0.94f)
            .height(33.dp)  // Mochy exact: 33dp
            .clip(RoundedCornerShape(50))
            .background(HeroPillFill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (kind) {
                HeroPillKind.Play -> {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = HeroPillContent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                HeroPillKind.Roll, HeroPillKind.Rolling -> {
                    GachaDie(spinning = kind == HeroPillKind.Rolling)
                }
                HeroPillKind.Shuffle -> {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = null,
                        tint = HeroPillContent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = HeroPillContent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = HeroPillContent
            )
        }
    }
}

/** Mochy's Tumbling 5-pip Die on Roll Pill */
@Composable
private fun GachaDie(spinning: Boolean) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            while (true) {
                rotation.animateTo(rotation.value + 360f, tween(600, easing = LinearEasing))
            }
        } else {
            val rest = (rotation.value / 90f).roundToInt() * 90f
            rotation.animateTo(rest, spring(dampingRatio = 0.45f, stiffness = 300f))
        }
    }

    Canvas(
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer { rotationZ = rotation.value }
    ) {
        drawRoundRect(
            color = Color.White,
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        val pip = 1.8.dp.toPx()
        val near = size.width * 0.28f
        val far = size.width * 0.72f
        val mid = size.width / 2f

        listOf(
            Offset(near, near), Offset(far, near),
            Offset(mid, mid),
            Offset(near, far), Offset(far, far),
        ).forEach { center ->
            drawCircle(color = GachaPipColor, radius = pip, center = center)
        }
    }
}

/** Mochy Multi-Stop Scrims (exact stop ratios from Mochy HeroCarousel.kt Figma) */
@Composable
private fun MochyHeroScrims() {
    // Mochy: 36% base dim. heroScrimBlack(g) = Color.Black.copy(alpha = 1 - (1 - 0.36f) * (1 - g))
    val topEnd = 0.463f
    val bottomStart = 0.533f
    val bottomEnd = 0.989f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val scrim = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 1f - 0.64f * (1f - 0.86f)),
                    0.475f * topEnd to Color.Black.copy(alpha = 1f - 0.64f * (1f - 0.59f)),
                    topEnd to Color.Black.copy(alpha = 0.36f),
                    bottomStart to Color.Black.copy(alpha = 0.36f),
                    bottomStart + 0.457f * (bottomEnd - bottomStart) to Color.Black.copy(alpha = 1f - 0.64f * (1f - 0.59f)),
                    bottomEnd to Color.Black.copy(alpha = 1f),
                    1f to Color.Black.copy(alpha = 1f),
                    startY = 0f,
                    endY = size.height,
                )
                onDrawBehind { drawRect(scrim) }
            }
    )
}

/** Mochy Parallax Media Drift inside Hero Pager */
private fun Modifier.mochyMediaDrift(pageOffset: () -> Float): Modifier = graphicsLayer {
    val d = pageOffset().coerceIn(-1f, 1f)
    val scale = 1f + 2f * PageMediaTravel * abs(d)
    scaleX = scale
    scaleY = scale
    translationX = d * size.width * PageMediaTravel
}

/** Mochy Text Drift with Fast Alpha Exit */
private fun Modifier.mochyTextDrift(pageOffset: () -> Float): Modifier = graphicsLayer {
    val d = pageOffset()
    translationX = d * size.width * (PageTextTravel + PageMediaTravel)
    alpha = (1f - abs(d) * 1.9f).coerceIn(0f, 1f)
}

/** Relative Distance in Slots for 3D Drum Reel */
private fun reelDistance(index: Int, reel: Float, count: Int): Float {
    if (count <= 0) return 0f
    var d = (reel - index) % count
    if (d < -count / 2f) d += count
    if (d >= count / 2f) d -= count
    return d
}

private enum class MochyGachaPhase { Idle, Rolling, Landed }

@Stable
private class MochyGachaState {
    var candidates by mutableStateOf<List<Video>>(emptyList())
    var phase by mutableStateOf(MochyGachaPhase.Idle)
    var result by mutableStateOf<Video?>(null)

    val reel = Animatable(0f)
    val pointerKick = Animatable(0f)
    val landPop = Animatable(1f)
    val burst = Animatable(0f)
}
