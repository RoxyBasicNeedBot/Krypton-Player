package dev.roxy.kryptonplayer.feature.videopicker.composables

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import android.view.ViewParent
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import dev.roxy.kryptonplayer.core.data.network.models.MediaStill
import dev.roxy.kryptonplayer.core.data.network.models.StillSource
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons

@Composable
fun SceneLightboxDialog(
    stills: List<MediaStill>,
    initialIndex: Int,
    title: String = "",
    onSetThumbnail: ((MediaStill) -> Unit)? = null,
    onSaveFrame: ((MediaStill) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (stills.isEmpty()) return

    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, stills.lastIndex),
        pageCount = { stills.size }
    )

    var controlsVisible by remember { mutableStateOf(true) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            var parent: ViewParent? = view.parent
            while (parent != null && parent !is DialogWindowProvider) {
                parent = parent.parent
            }
            val window = (parent as? DialogWindowProvider)?.window
                ?: (view.rootView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                userScrollEnabled = !isCurrentPageZoomed
            ) { page ->
                val still = stills[page]
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage != page) {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }

                val isZoomed = scale > 1.05f
                LaunchedEffect(isZoomed, pagerState.currentPage == page) {
                    if (pagerState.currentPage == page) {
                        isCurrentPageZoomed = isZoomed
                    }
                }

                val imageRequest = remember(still.fullUrl) {
                    val builder = ImageRequest.Builder(context)
                        .data(still.fullUrl)
                        .crossfade(true)
                    if (still.fullUrl.contains("fancaps.net")) {
                        builder.httpHeaders(
                            NetworkHeaders.Builder()
                                .set("Referer", "https://fancaps.net/")
                                .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                .build()
                        )
                    } else if (dev.roxy.kryptonplayer.core.data.network.NetworkConfig.isWorkerUrl(still.fullUrl)) {
                        builder.httpHeaders(
                            NetworkHeaders.Builder()
                                .set("X-Arch-Client-Key", dev.roxy.kryptonplayer.core.data.network.NetworkConfig.CLIENT_KEY)
                                .build()
                        )
                    }
                    builder.build()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { controlsVisible = !controlsVisible },
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                        offset = Offset.Zero
                                    }
                                }
                            )
                        }
                        .pointerInput(isZoomed) {
                            if (isZoomed) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                                    if (newScale <= 1.05f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = newScale
                                        val maxOffsetX = (size.width * (scale - 1)) / 2f
                                        val maxOffsetY = (size.height * (scale - 1)) / 2f
                                        offset = Offset(
                                            x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                            y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = still.caption ?: "Frame ${page + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Top Header Bar
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                val currentStill = stills.getOrNull(pagerState.currentPage)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${stills.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray.copy(alpha = 0.8f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom Action Bar
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val currentStill = stills.getOrNull(pagerState.currentPage)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentStill?.let { still ->
                        LightboxActionButton(
                            icon = NextIcons.Image,
                            label = "Set Thumbnail",
                            onClick = {
                                if (onSetThumbnail != null) {
                                    onSetThumbnail(still)
                                } else {
                                    Toast.makeText(context, "Thumbnail set for video", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        LightboxActionButton(
                            icon = NextIcons.DownloadVector,
                            label = "Save Frame",
                            onClick = {
                                if (onSaveFrame != null) {
                                    onSaveFrame(still)
                                } else {
                                    Toast.makeText(context, "Saving frame to Pictures/KryptonPlayer/Frames...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        LightboxActionButton(
                            icon = Icons.Default.Share,
                            label = "Share Frame",
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    this.type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "$title Frame")
                                    putExtra(Intent.EXTRA_TEXT, "${still.caption ?: title}\n${still.fullUrl}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Movie Frame"))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LightboxActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium
        )
    }
}

// Backward-compatible wrapper for existing calls
@Composable
fun ImageGalleryDialog(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val stills = remember(images) {
        images.mapIndexed { index, path ->
            val isFullUrl = path.startsWith("http://") || path.startsWith("https://")
            val fullUrl = if (isFullUrl) path else dev.roxy.kryptonplayer.core.data.network.NetworkConfig.tmdbImageUrl("original", path)
            val thumbUrl = if (isFullUrl) path else dev.roxy.kryptonplayer.core.data.network.NetworkConfig.tmdbImageUrl("w780", path)
            MediaStill(
                id = "gallery_$index",
                thumbnailUrl = thumbUrl,
                fullUrl = fullUrl,
                source = StillSource.TMDB,
                caption = "Screenshot #${index + 1}"
            )
        }
    }

    SceneLightboxDialog(
        stills = stills,
        initialIndex = initialIndex,
        onDismiss = onDismiss
    )
}
