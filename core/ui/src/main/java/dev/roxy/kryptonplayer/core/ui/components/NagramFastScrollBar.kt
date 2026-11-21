package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ultra-high-performance Nagram-style Fast ScrollBar for LazyColumn (LazyListState)
 * with zero-recomposition GPU offset placement, floating date/section bubble,
 * and non-blocking fling touch zones.
 */
@Composable
fun NagramFastScrollBar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    topPadding: Dp = 16.dp,
    bottomPadding: Dp = 16.dp,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    bubbleBackgroundColor: Color = Color(0xFF1E232D),
    bubbleTextColor: Color = Color.White,
    onFastScrollProgress: ((Float) -> Unit)? = null,
    sectionHeaderProvider: ((Int) -> String?)? = null
) {
    FastScrollBarInternal(
        totalItemsCountProvider = { lazyListState.layoutInfo.totalItemsCount },
        visibleItemIndexProvider = { lazyListState.firstVisibleItemIndex },
        visibleItemsCountProvider = { lazyListState.layoutInfo.visibleItemsInfo.size },
        isScrollInProgressProvider = { lazyListState.isScrollInProgress },
        onScrollToItem = { index -> lazyListState.scrollToItem(index) },
        modifier = modifier,
        topPadding = topPadding,
        bottomPadding = bottomPadding,
        thumbColor = thumbColor,
        bubbleBackgroundColor = bubbleBackgroundColor,
        bubbleTextColor = bubbleTextColor,
        onFastScrollProgress = onFastScrollProgress,
        sectionHeaderProvider = sectionHeaderProvider
    )
}

/**
 * Ultra-high-performance Nagram-style Fast ScrollBar for LazyVerticalGrid (LazyGridState)
 * with zero-recomposition GPU offset placement, floating date/section bubble,
 * and non-blocking fling touch zones.
 */
@Composable
fun NagramFastScrollBar(
    lazyGridState: LazyGridState,
    modifier: Modifier = Modifier,
    topPadding: Dp = 16.dp,
    bottomPadding: Dp = 16.dp,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    bubbleBackgroundColor: Color = Color(0xFF1E232D),
    bubbleTextColor: Color = Color.White,
    onFastScrollProgress: ((Float) -> Unit)? = null,
    sectionHeaderProvider: ((Int) -> String?)? = null
) {
    FastScrollBarInternal(
        totalItemsCountProvider = { lazyGridState.layoutInfo.totalItemsCount },
        visibleItemIndexProvider = { lazyGridState.firstVisibleItemIndex },
        visibleItemsCountProvider = { lazyGridState.layoutInfo.visibleItemsInfo.size },
        isScrollInProgressProvider = { lazyGridState.isScrollInProgress },
        onScrollToItem = { index -> lazyGridState.scrollToItem(index) },
        modifier = modifier,
        topPadding = topPadding,
        bottomPadding = bottomPadding,
        thumbColor = thumbColor,
        bubbleBackgroundColor = bubbleBackgroundColor,
        bubbleTextColor = bubbleTextColor,
        onFastScrollProgress = onFastScrollProgress,
        sectionHeaderProvider = sectionHeaderProvider
    )
}

@Composable
private fun FastScrollBarInternal(
    totalItemsCountProvider: () -> Int,
    visibleItemIndexProvider: () -> Int,
    visibleItemsCountProvider: () -> Int = { 1 },
    isScrollInProgressProvider: () -> Boolean,
    onScrollToItem: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = 16.dp,
    bottomPadding: Dp = 16.dp,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    bubbleBackgroundColor: Color = Color(0xFF1E232D),
    bubbleTextColor: Color = Color.White,
    onFastScrollProgress: ((Float) -> Unit)? = null,
    sectionHeaderProvider: ((Int) -> String?)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(0L) }
    var isVisible by remember { mutableStateOf(false) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val totalItemsCount by remember { derivedStateOf { totalItemsCountProvider() } }
    val visibleItemIndex by remember { derivedStateOf { visibleItemIndexProvider() } }
    val isScrollInProgress by remember { derivedStateOf { isScrollInProgressProvider() } }

    if (totalItemsCount <= 6) return

    val density = LocalDensity.current
    val thumbHeightDp = 40.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    // Auto-hide logic: 1.5s idle fade out
    LaunchedEffect(isScrollInProgress, isDragging) {
        if (isScrollInProgress || isDragging) {
            lastInteractionTime = System.currentTimeMillis()
            isVisible = true
        } else {
            delay(1500)
            if (!isDragging && !isScrollInProgressProvider()) {
                isVisible = false
            }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "fast_scroll_alpha"
    )

    // Derived normalized progress from list position (0f..1f)
    val listScrollProgress by remember {
        derivedStateOf {
            val total = totalItemsCountProvider()
            val current = visibleItemIndexProvider()
            val visibleCount = visibleItemsCountProvider().coerceAtLeast(1)
            val maxScrollable = (total - visibleCount).coerceAtLeast(1)
            if (total <= visibleCount) 0f
            else (current.toFloat() / maxScrollable).coerceIn(0f, 1f)
        }
    }

    val activeProgress by remember {
        derivedStateOf {
            if (isDragging) {
                val availableTrackHeight = (trackSize.height - thumbHeightPx).coerceAtLeast(1f)
                (dragOffsetY / availableTrackHeight).coerceIn(0f, 1f)
            } else {
                listScrollProgress
            }
        }
    }

    val bubbleText by remember {
        derivedStateOf {
            if (sectionHeaderProvider != null && (isDragging || isScrollInProgress)) {
                val total = totalItemsCountProvider()
                val visibleCount = visibleItemsCountProvider().coerceAtLeast(1)
                val maxScrollable = (total - visibleCount).coerceAtLeast(1)
                val targetIndex = (activeProgress * maxScrollable).toInt().coerceIn(0, (total - 1).coerceAtLeast(0))
                sectionHeaderProvider(targetIndex)
            } else null
        }
    }

    val thumbDragModifier = if (isVisible && alpha > 0.05f) {
        Modifier.pointerInput(totalItemsCount) {
            detectDragGestures(
                onDragStart = {
                    isDragging = true
                    isVisible = true
                    lastInteractionTime = System.currentTimeMillis()
                    val availableTrackHeight = (trackSize.height - thumbHeightPx).coerceAtLeast(1f)
                    dragOffsetY = listScrollProgress * availableTrackHeight
                },
                onDragEnd = {
                    isDragging = false
                    lastInteractionTime = System.currentTimeMillis()
                },
                onDragCancel = {
                    isDragging = false
                    lastInteractionTime = System.currentTimeMillis()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    lastInteractionTime = System.currentTimeMillis()
                    val total = totalItemsCountProvider()
                    val visibleCount = visibleItemsCountProvider().coerceAtLeast(1)
                    val maxScrollable = (total - visibleCount).coerceAtLeast(1)
                    if (trackSize.height > 0 && total > 0) {
                        val availableTrackHeight = (trackSize.height - thumbHeightPx).coerceAtLeast(1f)
                        dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(0f, availableTrackHeight)
                        val newProgress = (dragOffsetY / availableTrackHeight).coerceIn(0f, 1f)
                        val targetIndex = (newProgress * maxScrollable).toInt().coerceIn(0, total - 1)

                        onFastScrollProgress?.invoke(newProgress)
                        scrollJob?.cancel()
                        scrollJob = coroutineScope.launch {
                            onScrollToItem(targetIndex)
                        }
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .padding(top = topPadding, bottom = bottomPadding)
            .graphicsLayer { this.alpha = alpha }
            .onSizeChanged { trackSize = it }
    ) {
        // Floating Bubble (Date / Section Tooltip) with GPU-backed placement
        AnimatedVisibility(
            visible = (isDragging || isScrollInProgress) && !bubbleText.isNullOrEmpty(),
            enter = fadeIn(tween(100)) + scaleIn(tween(100)),
            exit = fadeOut(tween(150)) + scaleOut(tween(150)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val availableTrackHeight = (trackSize.height - thumbHeightPx).coerceAtLeast(0f)
                    val y = (activeProgress * availableTrackHeight - 8.dp.toPx()).coerceAtLeast(0f).toInt()
                    IntOffset(x = -48.dp.roundToPx(), y = y)
                }
        ) {
            Box(
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                bubbleBackgroundColor.copy(alpha = 0.95f),
                                bubbleBackgroundColor.copy(alpha = 0.90f)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bubbleText ?: "",
                    color = bubbleTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        // Fast Scroll Drag Thumb with GPU-backed translation offset & dedicated grab hit-target
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val availableTrackHeight = (trackSize.height - thumbHeightPx).coerceAtLeast(0f)
                    val y = if (isDragging) {
                        dragOffsetY.toInt().coerceIn(0, availableTrackHeight.toInt())
                    } else {
                        (listScrollProgress * availableTrackHeight).toInt()
                    }
                    IntOffset(x = 0, y = y)
                }
                .size(width = 28.dp, height = thumbHeightDp)
                .then(thumbDragModifier),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .width(if (isDragging) 6.dp else 4.dp)
                    .height(thumbHeightDp)
                    .clip(CircleShape)
                    .background(
                        if (isDragging) thumbColor else thumbColor.copy(alpha = 0.75f)
                    )
            )
        }
    }
}
