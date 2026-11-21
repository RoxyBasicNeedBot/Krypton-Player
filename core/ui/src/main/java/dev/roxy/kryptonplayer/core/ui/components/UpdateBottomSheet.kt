package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import java.util.Locale

/**
 * Custom Made Liquid-Glass Update Sheet.
 *
 * Built strictly without Material 3 ModalBottomSheet to deliver a pristine,
 * fluid, translucent glass appearance that only appears when an update is
 * actually active or available.
 */
@Composable
fun UpdateBottomSheet(
    info: AppUpdateInfo,
    updateState: UpdateState,
    currentVersionName: String,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onSkipVersion: (() -> Unit)? = null,
    onCancelDownload: (() -> Unit)? = null,
    onOpenReleaseUrl: (() -> Unit)? = null,
) {
    val isDownloading = updateState is UpdateState.Downloading
    val isVerifying = updateState is UpdateState.Verifying
    val isReady = updateState is UpdateState.ReadyToInstall
    val isError = updateState is UpdateState.Error

    val downloadProgress = (updateState as? UpdateState.Downloading)?.progress ?: -1
    val bytesDownloaded = (updateState as? UpdateState.Downloading)?.bytesDownloaded ?: 0L
    val totalBytes = (updateState as? UpdateState.Downloading)?.totalBytes?.takeIf { it > 0L } ?: info.fileSize
    val downloadSpeed = (updateState as? UpdateState.Downloading)?.speedBytesPerSec ?: 0L

    Dialog(
        onDismissRequest = {
            if (!isDownloading && !isVerifying) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isDownloading && !isVerifying,
            dismissOnClickOutside = !isDownloading && !isVerifying,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isDownloading && !isVerifying,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 250),
                ) + fadeOut(),
            ) {
                // Liquid Glass Card Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}, // Prevent dismissing when clicking inside card
                        )
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF161A24).copy(alpha = 0.98f),
                                    Color(0xFF0E1017).copy(alpha = 0.99f),
                                ),
                            ),
                        )
                        .navigationBarsPadding()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // ── Top Glass Pill Drag Indicator ────────────────────────────
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f)),
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // ── Header: Glowing Status Icon + Dismiss Button ─────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Glowing status badge
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isReady -> Color(0xFF10B981).copy(alpha = 0.18f)
                                            isError -> Color(0xFFEF4444).copy(alpha = 0.18f)
                                            isVerifying -> Color(0xFF8B5CF6).copy(alpha = 0.18f)
                                            isDownloading -> Color(0xFF38BDF8).copy(alpha = 0.18f)
                                            else -> Color(0xFF0284C7).copy(alpha = 0.20f)
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    isReady -> Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(28.dp),
                                    )
                                    isError -> Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(28.dp),
                                    )
                                    isVerifying -> {
                                        val transition = rememberInfiniteTransition(label = "verify_rot")
                                        val rotation by transition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1200, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart,
                                            ),
                                            label = "rot",
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.Security,
                                            contentDescription = null,
                                            tint = Color(0xFFA78BFA),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .rotate(rotation),
                                        )
                                    }
                                    isDownloading -> Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(28.dp),
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isReady -> "Ready to Install"
                                        isError -> "Update Failed"
                                        isVerifying -> "Verifying Signature…"
                                        isDownloading -> "Downloading Update…"
                                        else -> "Update Available"
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.3).sp,
                                    ),
                                    color = Color.White,
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = when {
                                        isReady -> "Package verified and ready to install"
                                        isError -> (updateState as? UpdateState.Error)?.message ?: "An error occurred during update"
                                        isVerifying -> "Checking APK cryptographic integrity"
                                        isDownloading -> if (downloadSpeed > 0) formatSpeed(downloadSpeed) else "Transferring package"
                                        else -> "Krypton Player v${info.versionName}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            if (!isDownloading && !isVerifying) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Version Comparison & Metadata Pills ──────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentVersionName.isNotBlank()) {
                                    Text(
                                        text = "v$currentVersionName",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = Color.White.copy(alpha = 0.50f),
                                    )
                                    Text(
                                        text = "  →  ",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF38BDF8),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.22f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = "v${info.versionName}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = Color(0xFF7DD3FC),
                                    )
                                }
                            }

                            if (info.fileSize > 0L) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = formatSize(info.fileSize),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                        ),
                                        color = Color.White.copy(alpha = 0.75f),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ── Changelog / Release Notes Box ────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 210.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(14.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    text = "RELEASE NOTES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                    ),
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )

                                if (info.changelog.isNotBlank()) {
                                    CleanMarkdownText(markdown = info.changelog)
                                } else {
                                    Text(
                                        text = "No release notes provided for this release.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.50f),
                                    )
                                }
                            }
                        }

                        // ── Download Progress Bar (When Downloading) ─────────────────
                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (downloadProgress in 0..100) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = Color(0xFF38BDF8),
                                        trackColor = Color.White.copy(alpha = 0.10f),
                                        strokeCap = StrokeCap.Round,
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = Color(0xFF38BDF8),
                                        trackColor = Color.White.copy(alpha = 0.10f),
                                        strokeCap = StrokeCap.Round,
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    val dlMb = formatSize(bytesDownloaded)
                                    val totalMb = if (totalBytes > 0L) formatSize(totalBytes) else "..."
                                    Text(
                                        text = "$dlMb / $totalMb • ${if (downloadProgress >= 0) "$downloadProgress%" else "Downloading"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                        ),
                                        color = Color.White.copy(alpha = 0.70f),
                                    )
                                    if (downloadSpeed > 0) {
                                        Text(
                                            text = formatSpeed(downloadSpeed),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = Color(0xFF38BDF8),
                                        )
                                    }
                                }
                            }
                        }

                        // ── Verifying Indicator ──────────────────────────────────────
                        if (isVerifying) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFA78BFA),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Checking cryptographic package signatures...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFDDD6FE),
                                )
                            }
                        }

                        // ── Error Message Banner ─────────────────────────────────────
                        if (isError) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val errMsg = (updateState as? UpdateState.Error)?.message
                                ?: "Failed to complete update."
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFECACA),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Action Buttons ───────────────────────────────────────────
                        when {
                            isDownloading -> {
                                Button(
                                    onClick = { onCancelDownload?.invoke() ?: onDismiss() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.12f),
                                        contentColor = Color.White,
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(vertical = 14.dp),
                                ) {
                                    Text("Cancel Download", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            isVerifying -> {
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(vertical = 14.dp),
                                ) {
                                    Text("Verifying...", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            isReady -> {
                                Button(
                                    onClick = onInstall,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.White,
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(vertical = 14.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Install Update Now", fontWeight = FontWeight.Bold)
                                }
                            }
                            isError -> {
                                val canRetry = (updateState as? UpdateState.Error)?.canRetry ?: true
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (canRetry) {
                                        Button(
                                            onClick = onDownload,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF0284C7),
                                                contentColor = Color.White,
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Retry", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                        contentPadding = PaddingValues(vertical = 12.dp),
                                    ) {
                                        Text("Dismiss", color = Color.White.copy(alpha = 0.85f))
                                    }
                                }
                            }
                            else -> {
                                // Default Available State
                                Button(
                                    onClick = onDownload,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0284C7),
                                        contentColor = Color.White,
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(vertical = 14.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Update Now (v${info.versionName})", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    if (onSkipVersion != null && info.tagName.isNotBlank()) {
                                        TextButton(
                                            onClick = onSkipVersion,
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                        ) {
                                            Text(
                                                text = "Skip this version",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White.copy(alpha = 0.45f),
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (onOpenReleaseUrl != null && info.releaseUrl.isNotBlank()) {
                                            TextButton(
                                                onClick = onOpenReleaseUrl,
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.OpenInBrowser,
                                                    contentDescription = null,
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Release Page",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color(0xFF38BDF8),
                                                )
                                            }
                                        }

                                        TextButton(
                                            onClick = onDismiss,
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                        ) {
                                            Text(
                                                text = "Later",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                                color = Color.White.copy(alpha = 0.70f),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

/**
 * Lightweight, clean Markdown renderer for changelogs.
 * Handles headings (#, ##, ###), bullets (*, -, •), bold (**text**), and inline code (`code`).
 */
@Composable
private fun CleanMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val lines = markdown.lines()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                continue
            }

            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### ").trim(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF7DD3FC),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# ").trim(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    val content = trimmed.substring(2).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            text = parseInlineMarkdown(content),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp,
                            ),
                            color = Color.White.copy(alpha = 0.88f),
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(trimmed),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                        ),
                        color = Color.White.copy(alpha = 0.80f),
                    )
                }
            }
        }
    }
}

/**
 * Parses bold (**bold**) and inline code (`code`) into an AnnotatedString.
 */
private fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    val boldContent = text.substring(i + 2, end)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        append(boldContent)
                    }
                    i = end + 2
                    continue
                }
            } else if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    val codeContent = text.substring(i + 1, end)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8),
                            background = Color.White.copy(alpha = 0.08f),
                        ),
                    ) {
                        append(" $codeContent ")
                    }
                    i = end + 1
                    continue
                }
            }
            append(text[i])
            i++
        }
    }
}

private fun formatSize(bytes: Long): String {
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
