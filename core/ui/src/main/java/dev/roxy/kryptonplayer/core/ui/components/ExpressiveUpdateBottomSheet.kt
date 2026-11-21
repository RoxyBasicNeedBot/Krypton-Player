package dev.roxy.kryptonplayer.core.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.East
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import java.util.Locale

/**
 * Detects the device's primary application binary interface (ABI).
 */
fun detectDeviceAbi(): String {
    val supportedAbis = (Build.SUPPORTED_ABIS ?: emptyArray()).map { it.lowercase() }
    for (abi in supportedAbis) {
        if (abi.contains("arm64-v8a") || abi.contains("arm64")) return "arm64-v8a"
        if (abi.contains("armeabi-v7a") || abi.contains("armv7")) return "armeabi-v7a"
        if (abi.contains("x86_64")) return "x86_64"
    }
    return "universal"
}

/**
 * Expressive Material 3 Modal Bottom Sheet for Krypton Player updates.
 *
 * Implements Mochy-inspired aesthetics:
 * - Expressive drag handle
 * - Branded header: "Krypton Player Update" with rocket/download icon
 * - Version comparison card: `Current: vX.Y.Z` ➔ `Latest: vA.B.C` (bold primary container)
 * - Metadata chips: Release date, file size, "STABLE" green badge (`#9CF58A` text)
 * - Architecture chip showing detected ABI (`arm64-v8a [Recommended]`)
 * - Expandable and scrollable [ChangelogMarkdown]
 * - Bottom sticky action bar with [MochyPromptButton] and secondary "Remind me later" action
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveUpdateBottomSheet(
    info: AppUpdateInfo,
    updateState: UpdateState,
    currentVersionName: String,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onRemindLater: (() -> Unit)? = null,
    onSkipVersion: (() -> Unit)? = null,
    onOpenReleaseUrl: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val context = LocalContext.current
    val isDownloading = updateState is UpdateState.Downloading
    val isVerifying = updateState is UpdateState.Verifying
    val isBusy = isDownloading || isVerifying

    val detectedAbi = remember { detectDeviceAbi() }
    val recommendedAbiText = if (detectedAbi == "arm64-v8a") "arm64-v8a [Recommended]" else "$detectedAbi [Recommended]"

    var isChangelogExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isBusy) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            // Expressive Drag Handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Scrollable Main Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Header Row: Icon + Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                    ),
                                ),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Krypton Player Update",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "New release ready to enhance your experience",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (!isBusy) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close update sheet",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // Version Comparison Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                            ),
                        ),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Current version block
                        Column {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayCurrent = if (currentVersionName.startsWith("v", ignoreCase = true)) {
                                currentVersionName
                            } else {
                                "v$currentVersionName"
                            }
                            Text(
                                text = displayCurrent,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        // Center Arrow Connector
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.East,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // Latest version block
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Latest",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayLatest = if (info.versionName.startsWith("v", ignoreCase = true)) {
                                info.versionName
                            } else {
                                "v${info.versionName}"
                            }
                            Text(
                                text = displayLatest,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Metadata Chips: STABLE green badge (#9CF58A), Architecture, File Size, Release Date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // STABLE Green Badge (#9CF58A text)
                    val isStable = !info.versionName.contains("beta", ignoreCase = true) &&
                        !info.versionName.contains("nightly", ignoreCase = true) &&
                        !info.versionName.contains("alpha", ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isStable) Color(0xFF0F2B17) else Color(0xFF2E240D),
                        border = BorderStroke(
                            1.dp,
                            if (isStable) Color(0xFF1B5E20) else Color(0xFF5E450E),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = if (isStable) Color(0xFF9CF58A) else Color(0xFFFFD54F),
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = if (isStable) "STABLE" else "PRE-RELEASE",
                                color = if (isStable) Color(0xFF9CF58A) else Color(0xFFFFD54F),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    // Architecture Chip: arm64-v8a [Recommended]
                    UpdateMetaChip(
                        icon = Icons.Rounded.Memory,
                        text = recommendedAbiText,
                    )

                    // File Size Chip
                    if (info.fileSize > 0) {
                        UpdateMetaChip(
                            icon = Icons.Rounded.FolderZip,
                            text = formatBytes(info.fileSize),
                        )
                    }

                    // Release Date Chip
                    if (info.releaseDate.isNotBlank()) {
                        UpdateMetaChip(
                            icon = Icons.Rounded.CalendarToday,
                            text = info.releaseDate,
                        )
                    }
                }

                // Changelog Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "What's New in ${info.versionName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            // Expand / Collapse Toggle Button
                            IconButton(
                                onClick = { isChangelogExpanded = !isChangelogExpanded },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    imageVector = if (isChangelogExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = if (isChangelogExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scrollable / Expandable Markdown Content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isChangelogExpanded) {
                                        Modifier
                                    } else {
                                        Modifier.heightIn(max = 240.dp)
                                    },
                                )
                                .verticalScroll(rememberScrollState()),
                        ) {
                            ChangelogMarkdown(
                                markdown = info.changelog,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                strongColor = MaterialTheme.colorScheme.onSurface,
                                linkColor = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Bottom Sticky Action Bar with MochyPromptButton
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Signature Mochy Animated Progress Button
                    MochyPromptButton(
                        updateState = updateState,
                        fileSize = info.fileSize,
                        onClick = {
                            when (updateState) {
                                is UpdateState.ReadyToInstall -> onInstall()
                                is UpdateState.Downloading -> { /* Handled in background */ }
                                is UpdateState.Error -> onDownload()
                                else -> onDownload()
                            }
                        },
                        enabled = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Secondary Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                onRemindLater?.invoke() ?: onDismiss()
                            },
                            enabled = !isBusy,
                        ) {
                            Text(
                                text = "Remind me later",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        if (info.releaseUrl.isNotBlank() || onOpenReleaseUrl != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "·",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = {
                                    if (onOpenReleaseUrl != null) {
                                        onOpenReleaseUrl()
                                    } else {
                                        openUrl(context, info.releaseUrl)
                                    }
                                },
                                enabled = !isBusy,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "View on GitHub",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.OpenInBrowser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateMetaChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.60f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    }
}

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
