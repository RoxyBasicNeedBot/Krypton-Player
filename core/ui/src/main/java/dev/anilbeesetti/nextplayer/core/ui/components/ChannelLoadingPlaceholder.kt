@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.theme.NextColors

/**
 * ChannelLoadingPlaceholder: Sleek, high-performance animated placeholder for channel loading,
 * search queries, suggestions, and empty states. Integrates lightweight animated JSON Lottie stickers
 * with ambient pulsing glow, quick suggestion chips, and optional progress or action triggers.
 */
@Composable
fun ChannelLoadingPlaceholder(
    modifier: Modifier = Modifier,
    @RawRes lottieRes: Int? = null,
    @DrawableRes stickerRes: Int? = null,
    stickerSize: Dp = 110.dp,
    title: String = "Loading...",
    subtitle: String? = null,
    isLoading: Boolean = false,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    showBoostIcon: Boolean = false,
    showLearnIcon: Boolean = false,
    suggestions: List<String> = emptyList(),
    onSuggestionClick: ((String) -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StickerGlowTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ambient glowing frame for the sticker
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(stickerSize + 28.dp)
            ) {
                // Soft radial breathing aura behind the sticker
                Box(
                    modifier = Modifier
                        .size(stickerSize + 24.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f * glowAlpha),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f * glowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Sticker container card with sleek rounded border
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(stickerSize + 8.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    val resolvedLottieRes = lottieRes ?: if (stickerRes == null) R.raw.anim_duck_empty else null
                    if (resolvedLottieRes != null) {
                        LottieStickerImage(
                            resId = resolvedLottieRes,
                            size = stickerSize,
                            modifier = Modifier.clip(RoundedCornerShape(24.dp))
                        )
                    } else if (stickerRes != null) {
                        AnimatedStickerImage(
                            resId = stickerRes,
                            size = stickerSize,
                            modifier = Modifier.clip(RoundedCornerShape(24.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (isLoading) {
                Spacer(Modifier.height(18.dp))
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Suggestion Chips (e.g. /boost, /learn, #video, 1080p)
            if (suggestions.isNotEmpty() && onSuggestionClick != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    suggestions.forEach { suggestion ->
                        val isBoost = suggestion.contains("boost", ignoreCase = true)
                        val isLearn = suggestion.contains("learn", ignoreCase = true)
                        Surface(
                            onClick = { onSuggestionClick(suggestion) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isBoost) {
                                Color(0xFFFF9800).copy(alpha = 0.15f)
                            } else if (isLearn) {
                                Color(0xFF2196F3).copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = NextColors.cardBorder(
                                if (isBoost) {
                                    Color(0xFFFF9800).copy(alpha = 0.35f)
                                } else if (isLearn) {
                                    Color(0xFF2196F3).copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isBoost) {
                                    LottieTelegramBoost(size = 18.dp)
                                } else if (isLearn) {
                                    Icon(
                                        imageVector = Icons.Rounded.School,
                                        contentDescription = null,
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (suggestion.startsWith("#")) {
                                    Text(
                                        text = "#",
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isBoost) Color(0xFFFF9800) else if (isLearn) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (!actionButtonText.isNullOrEmpty() && onActionClick != null) {
                Spacer(Modifier.height(20.dp))
                val isBoostBtn = showBoostIcon || actionButtonText.contains("boost", ignoreCase = true)
                val isLearnBtn = showLearnIcon || actionButtonText.contains("learn", ignoreCase = true) || actionButtonText.contains("guide", ignoreCase = true)
                FilledTonalButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isBoostBtn) {
                            Color(0xFFFF9800).copy(alpha = 0.18f)
                        } else if (isLearnBtn) {
                            Color(0xFF2196F3).copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (isBoostBtn) {
                            Color(0xFFFF9800)
                        } else if (isLearnBtn) {
                            Color(0xFF2196F3)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                ) {
                    if (isBoostBtn) {
                        LottieTelegramBoost(size = 20.dp)
                    } else if (isLearnBtn) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = actionButtonText, fontWeight = FontWeight.SemiBold)
                }
            }

            if (extraContent != null) {
                Spacer(Modifier.height(16.dp))
                extraContent()
            }
        }
    }
}
