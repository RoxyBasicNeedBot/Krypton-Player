package dev.roxy.kryptonplayer.feature.videopicker.composables

import android.widget.Toast
import dev.roxy.kryptonplayer.core.common.CrashBreadcrumbs
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import dev.roxy.kryptonplayer.core.common.Utils
import dev.roxy.kryptonplayer.core.model.Folder
import dev.roxy.kryptonplayer.core.model.Video
import dev.roxy.kryptonplayer.core.ui.components.bouncyPress
import dev.roxy.kryptonplayer.core.ui.components.LiquidGlassIconButton
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.theme.smoothScrimBrush
import dev.roxy.kryptonplayer.core.ui.theme.toImmersiveBackground
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import dev.roxy.kryptonplayer.core.ui.liquidglass.PlatformBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.rememberBackdrop
import dev.roxy.kryptonplayer.core.ui.liquidglass.layerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts dominant, vibrant and dark muted palette colors dynamically from artwork.
 */
@Composable
fun rememberFolderPaletteColors(imageUrl: Any?): Triple<Color, Color, Color> {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    var colors by remember(imageUrl) { mutableStateOf(Triple(primary, secondary, surface)) }

    LaunchedEffect(imageUrl) {
        if (imageUrl == null) {
            colors = Triple(primary, secondary, surface)
            return@LaunchedEffect
        }
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(128)
            .allowHardware(false)
            .build()

        val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
        if (result is SuccessResult) {
            val bitmap = result.image.toBitmap()
            val palette = withContext(Dispatchers.Default) {
                Palette.from(bitmap).maximumColorCount(16).generate()
            }
            val dominant = palette.dominantSwatch?.rgb?.let { Color(it) }
                ?: palette.vibrantSwatch?.rgb?.let { Color(it) }
                ?: palette.mutedSwatch?.rgb?.let { Color(it) }
                ?: Color(palette.getDominantColor(primary.toArgb()))
            val vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) }
                ?: palette.lightVibrantSwatch?.rgb?.let { Color(it) }
                ?: dominant
            val darkMuted = palette.darkMutedSwatch?.rgb?.let { Color(it) }
                ?: palette.darkVibrantSwatch?.rgb?.let { Color(it) }
                ?: surface
            colors = Triple(dominant, vibrant, darkMuted)
        }
    }

    val animatedDominant by animateColorAsState(targetValue = colors.first, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "dominant")
    val animatedVibrant by animateColorAsState(targetValue = colors.second, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "vibrant")
    val animatedDarkMuted by animateColorAsState(targetValue = colors.third, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "darkMuted")

    return Triple(animatedDominant, animatedVibrant, animatedDarkMuted)
}


/**
 * Liquid Glass UI 1:1 Match with Reference (YouTube Music / Apple Music Playlist Style):
 * - Top Navigation: Circular Back button on left, Single connected Capsule Pill [ ♡ | 🔍 | ⋮ ] on right.
 * - Backdrop Cover: Dynamic ambient radial glow + smooth gradient melting into dark background.
 * - Center Typography:
 *     - Folder Title (ExtraBold, large)
 *     - Subtitle 1 ("Next Player")
 *     - Subtitle 2 ("Folder • X videos • Y MB")
 * - Center Action Row:
 *     - [ 🔀 ] 48dp Frosted Glass Shuffle Button
 *     - [ ▶ Play ] High-contrast Solid White Capsule Pill Button
 *     - [ 📥 ] 48dp Frosted Glass Download / Reels Button
 * - Description Line: Centered subtle summary / path
 * - Track Count Section Header: Left-aligned "X tracks" right above the media items
 */
@Composable
fun FolderHeroHeader(
    folder: Folder,
    onPlayAllClick: (List<Video>) -> Unit,
    modifier: Modifier = Modifier,
    onShuffleClick: ((List<Video>) -> Unit)? = null,
    onReelsClick: ((List<Video>) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onQuickSettingsClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    moreMenuContent: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val videos = remember(folder) {
        if (folder.allMediaList.isNotEmpty()) folder.allMediaList else folder.mediaList
    }
    val targetVideo = remember(folder, videos) {
        folder.recentlyPlayedVideo ?: folder.firstVideo ?: videos.firstOrNull()
    }
    val coverModel = remember(targetVideo) {
        targetVideo?.let { video ->
            when {
                !video.artworkUri.isNullOrBlank() -> video.artworkUri
                video.isImage -> video.uriString.ifEmpty { video.path }
                video.uriString.isNotBlank() && !video.uriString.startsWith("http") -> video.uriString
                video.path.isNotBlank() -> video.path
                else -> null
            }
        }
    }

    // Extract dynamic ambient palette colors from thumbnail with smooth interpolation
    val (dominantColor, vibrantColor, _) = rememberFolderPaletteColors(coverModel)
    val immersiveBg = dominantColor.toImmersiveBackground()

    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 400),
        label = "dominantColor"
    )
    val animatedVibrantColor by animateColorAsState(
        targetValue = vibrantColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 400),
        label = "vibrantColor"
    )
    val animatedImmersiveBg by animateColorAsState(
        targetValue = immersiveBg,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 450),
        label = "immersiveBg"
    )

    var hasLoaded by remember(folder.path) { mutableStateOf(false) }
    LaunchedEffect(folder.path) {
        hasLoaded = true
    }
    val heroAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (hasLoaded) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "heroAlpha"
    )

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val coverHeight = (screenHeight * 0.38f).coerceIn(280.dp, 360.dp)

    var isFavorite by rememberSaveable(folder.path) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. Top Cover Backdrop with Ambient Glow & Floating Liquid Glass Controls
        val artworkBackdrop = rememberBackdrop(Color.Black)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverHeight),
        ) {
            // Backdrop Source: Artwork and ambient glow only (no controls) with smooth entrance fade-in
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = heroAlpha }
                    .layerBackdrop(artworkBackdrop),
            ) {
                // Ambient Radial Glow behind cover
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    animatedVibrantColor.copy(alpha = 0.45f),
                                    animatedDominantColor.copy(alpha = 0.25f),
                                    Color.Transparent,
                                ),
                                radius = 800f,
                            ),
                        ),
                )

                // Cover Image
                if (coverModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = targetVideo?.displayName ?: folder.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Top Scrim for Status Bar & Navigation Controls readability (SimpMusic smoothstep)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            smoothScrimBrush(
                                from = Color.Black.copy(alpha = 0.70f),
                                to = Color.Transparent,
                                steps = 16,
                            ),
                        ),
                )

                // Bottom smooth gradient fade melting the artwork into ambient dark background (SimpMusic smoothstep)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            smoothScrimBrush(
                                from = Color.Transparent,
                                to = animatedImmersiveBg,
                                steps = 24,
                            ),
                        ),
                )
            }
        }

        // 2. Information Section with Large Title, Subtitles, Action Cluster, Description
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title: Bold Large (e.g. K.iNG)
            Text(
                text = folder.name.ifEmpty { "Videos" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle 1: Krypton Player
            Text(
                text = "Krypton Player",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle 2: Folder • 24 videos • 1.2 GB
            val subtitle2 = remember(folder, videos.size) {
                buildString {
                    append("Folder • ${videos.size} ${if (videos.size == 1) "video" else "videos"}")
                    if (folder.mediaSize > 0) {
                        append(" • ${Utils.formatFileSize(folder.mediaSize)}")
                    } else if (folder.mediaDuration > 0) {
                        append(" • ${Utils.formatDurationMillis(folder.mediaDuration)}")
                    }
                }
            }
            Text(
                text = subtitle2,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                ),
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Cluster Row: [ ♡ ]  [ 🔀 ]  [ ▶ Play ]  [ 🎬 ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Favorite / Heart Button (Circular Frosted Glass 48dp)
                LiquidGlassIconButton(
                    onClick = {
                        CrashBreadcrumbs.recordClick("Favorite Button", "FolderHeroHeader", if (!isFavorite) "Added" else "Removed")
                        isFavorite = !isFavorite
                        Toast.makeText(
                            context,
                            if (isFavorite) "Added to Favorites ❤️" else "Removed from Favorites",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    size = 48.dp,
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF4081) else Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Shuffle Button (Circular Frosted Glass 48dp)
                LiquidGlassIconButton(
                    onClick = {
                        CrashBreadcrumbs.recordClick("Shuffle Button", "FolderHeroHeader", "${videos.size} videos")
                        val shuffled = videos.shuffled()
                        onShuffleClick?.invoke(shuffled) ?: onPlayAllClick(shuffled)
                    },
                    size = 48.dp,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Play Capsule Button (Solid High Contrast White)
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 120.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .bouncyPress()
                        .clickable {
                            CrashBreadcrumbs.recordClick("Play Button", "FolderHeroHeader", "${videos.size} videos")
                            onPlayAllClick(videos)
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                    }
                }

                // Download / Reels Action Button (Circular Frosted Glass 48dp)
                LiquidGlassIconButton(
                    onClick = {
                        if (onReelsClick != null) {
                            CrashBreadcrumbs.recordClick("Reels Button", "FolderHeroHeader", "${videos.size} videos")
                            onReelsClick(videos)
                        } else {
                            CrashBreadcrumbs.recordClick("Play All Button", "FolderHeroHeader", "${videos.size} videos")
                            onPlayAllClick(videos)
                        }
                    },
                    size = 48.dp,
                ) {
                    if (onReelsClick != null) {
                        Icon(
                            painter = painterResource(id = NextIcons.Reel),
                            contentDescription = "Reels",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Download or Reels",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description Line: Centered subtle summary / path
            val descText = remember(folder) {
                if (folder.path.isNotEmpty() && folder.path != folder.name) {
                    folder.path
                } else {
                    "All videos in ${folder.name.ifEmpty { "this folder" }}"
                }
            }
            Text(
                text = descText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                ),
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Track Count Section Header: Left-aligned "99 tracks" / "X videos"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = "${videos.size} tracks",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}
