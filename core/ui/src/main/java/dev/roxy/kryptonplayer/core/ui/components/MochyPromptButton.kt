package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import java.util.Locale

/**
 * High-level state representing the visual phase of the Mochy-inspired update prompt button.
 */
sealed interface MochyPromptButtonState {
    /** Checking for updates with active shimmer/spinner */
    data class Checking(val text: String = "Checking for updates…") : MochyPromptButtonState

    /** An update is available and waiting for download */
    data class UpdateAvailable(
        val fileSizeText: String? = null,
        val customText: String? = null,
    ) : MochyPromptButtonState

    /** Actively downloading the update binary with progress and speed info */
    data class Downloading(
        val progress: Int, // 0..100
        val speedText: String? = null,
        val downloadedText: String? = null,
        val totalText: String? = null,
    ) : MochyPromptButtonState

    /** Download finished and verified; ready for package installation */
    data class ReadyToInstall(
        val customText: String = "Install Update",
    ) : MochyPromptButtonState

    /** Error occurred during check or download */
    data class Error(
        val message: String = "Download failed · Retry",
        val canRetry: Boolean = true,
    ) : MochyPromptButtonState

    /** Krypton Player is already on the newest version */
    data class UpToDate(
        val text: String = "Krypton Player is up to date",
    ) : MochyPromptButtonState
}

/**
 * Maps a domain [UpdateState] into a visual [MochyPromptButtonState].
 */
fun UpdateState.toPromptButtonState(fileSize: Long = 0L): MochyPromptButtonState {
    return when (this) {
        is UpdateState.Checking -> MochyPromptButtonState.Checking()
        is UpdateState.Available -> MochyPromptButtonState.UpdateAvailable(
            fileSizeText = if (fileSize > 0) formatFileSize(fileSize) else null,
        )
        is UpdateState.Downloading -> {
            val speed = if (speedBytesPerSec > 0) formatSpeed(speedBytesPerSec) else null
            MochyPromptButtonState.Downloading(
                progress = progress.coerceIn(0, 100),
                speedText = speed,
                downloadedText = if (bytesDownloaded > 0) formatFileSize(bytesDownloaded) else null,
                totalText = if (totalBytes > 0) formatFileSize(totalBytes) else null,
            )
        }
        is UpdateState.Verifying -> MochyPromptButtonState.Downloading(
            progress = 100,
            speedText = "Verifying package…",
        )
        is UpdateState.ReadyToInstall -> MochyPromptButtonState.ReadyToInstall()
        is UpdateState.Error -> MochyPromptButtonState.Error(message = message)
        is UpdateState.UpToDate -> MochyPromptButtonState.UpToDate()
        is UpdateState.Idle -> MochyPromptButtonState.UpdateAvailable(
            fileSizeText = if (fileSize > 0) formatFileSize(fileSize) else null,
        )
    }
}

/**
 * The signature Mochy animated progress pill button for Krypton Player.
 *
 * Features:
 * - Expressive horizontal fill animating smoothly from 0% to 100% as the download progresses.
 * - Reactive tactile spring bounce on press interaction.
 * - Dynamic state transitions between:
 *   - "Checking" -> Shimmer wave and smooth spinner
 *   - "Update Available" -> "Update Now (42.8 MB)" with download arrow
 *   - "Downloading" -> Horizontal progress fill with "Downloading… 45% (4.2 MB/s)"
 *   - "Ready to Install" -> Accent filled button with Rocket icon and "Install Update"
 *   - "Error" -> Retry trigger with warning icon
 * - Standard Material 3 token integration with dark/light mode support.
 */
@Composable
fun MochyPromptButton(
    state: MochyPromptButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.90f),
    progressFillColor: Color = MaterialTheme.colorScheme.primary,
    readyContainerColor: Color = MaterialTheme.colorScheme.primary,
    onReadyContentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val isInteractive = enabled && state !is MochyPromptButtonState.UpToDate

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "mochyPressScale",
    )

    // Progress target for the horizontal fill bar
    val targetFillFraction = when (state) {
        is MochyPromptButtonState.Downloading -> (state.progress / 100f).coerceIn(0f, 1f)
        is MochyPromptButtonState.ReadyToInstall -> 1f
        else -> 0f
    }

    val animatedFillFraction by animateFloatAsState(
        targetValue = targetFillFraction,
        animationSpec = tween(
            durationMillis = if (state is MochyPromptButtonState.ReadyToInstall) 300 else 180,
            easing = LinearOutSlowInEasing,
        ),
        label = "mochyFillFraction",
    )

    // Shimmer sweep for the Checking state
    val infiniteTransition = rememberInfiniteTransition(label = "mochyShimmerTransition")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mochyShimmerTranslate",
    )

    val shape = RoundedCornerShape(percent = 50)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(pressScale),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (state is MochyPromptButtonState.ReadyToInstall) 0.45f else 0.20f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    Color.White.copy(alpha = if (state is MochyPromptButtonState.ReadyToInstall) 0.45f else 0.20f),
                ),
            ),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(containerColor)
                .clickable(
                    enabled = isInteractive,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Horizontal Progress Fill Layer
            if (animatedFillFraction > 0f) {
                val fillBrush = if (state is MochyPromptButtonState.ReadyToInstall) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            readyContainerColor,
                            readyContainerColor.copy(alpha = 0.88f),
                        ),
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            progressFillColor.copy(alpha = 0.25f),
                            progressFillColor.copy(alpha = 0.45f),
                        ),
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedFillFraction)
                        .background(fillBrush),
                )

                // Leading glowing edge indicator during active download
                if (state is MochyPromptButtonState.Downloading && animatedFillFraction in 0.02f..0.98f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = animatedFillFraction)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(progressFillColor.copy(alpha = 0.85f)),
                        )
                    }
                }
            }

            // Checking State Shimmer Wave
            if (state is MochyPromptButtonState.Checking) {
                val shimmerBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    startX = shimmerTranslate * 600f,
                    endX = (shimmerTranslate + 0.5f) * 600f,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(shimmerBrush),
                )
            }

            // Foreground Label and Icon Content
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)))
                        .togetherWith(fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                },
                label = "mochyButtonContent",
            ) { targetState ->
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    when (targetState) {
                        is MochyPromptButtonState.Checking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                strokeCap = StrokeCap.Round,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = targetState.text,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        is MochyPromptButtonState.UpdateAvailable -> {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val labelText = targetState.customText ?: if (targetState.fileSizeText != null) {
                                "Update Now (${targetState.fileSizeText})"
                            } else {
                                "Update Now"
                            }
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        is MochyPromptButtonState.Downloading -> {
                            CircularProgressIndicator(
                                progress = { targetState.progress / 100f },
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val speedSuffix = if (!targetState.speedText.isNullOrBlank()) {
                                " (${targetState.speedText})"
                            } else {
                                ""
                            }
                            Text(
                                text = "Downloading… ${targetState.progress}%$speedSuffix",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        is MochyPromptButtonState.ReadyToInstall -> {
                            Icon(
                                imageVector = Icons.Rounded.RocketLaunch,
                                contentDescription = null,
                                tint = onReadyContentColor,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = targetState.customText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = onReadyContentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        is MochyPromptButtonState.Error -> {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = targetState.message,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        is MochyPromptButtonState.UpToDate -> {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF9CF58A),
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = targetState.text,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Convenient overload accepting [UpdateState] directly from Krypton Player updater engine.
 */
@Composable
fun MochyPromptButton(
    updateState: UpdateState,
    fileSize: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MochyPromptButton(
        state = updateState.toPromptButtonState(fileSize = fileSize),
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return ""
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB/s", mb)
    } else {
        String.format(Locale.US, "%.0f KB/s", kb)
    }
}
