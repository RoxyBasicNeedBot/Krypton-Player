package dev.roxy.kryptonplayer.core.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Commit
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import dev.roxy.kryptonplayer.core.data.update.ApkAsset
import dev.roxy.kryptonplayer.core.data.update.GitHubCommitInfo
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import java.text.SimpleDateFormat
import java.util.Locale

private fun formatApkSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * ArchiveTune-style App Update Dialog with:
 * - Version badge comparison & update status
 * - Multiple APK build variants selector (ARM64-v8a, ARMeabi-v7a, Universal) with sizes & recommended indicator
 * - Dedicated tabs for: "APK Builds", "Release Notes", and "Commit History"
 * - Real-time download progress bar with MB tracking
 * - Safe one-tap direct install flow
 */
@Composable
fun AppUpdateDialog(
    versionName: String,
    changelog: String,
    currentVersionName: String = "",
    fileSize: Long = 0L,
    releaseDate: String = "",
    downloadProgress: Int = -1,
    bytesDownloaded: Long = 0L,
    totalBytes: Long = 0L,
    isReadyToInstall: Boolean = false,
    apkAssets: List<ApkAsset> = emptyList(),
    commits: List<GitHubCommitInfo> = emptyList(),
    isUpToDate: Boolean = false,
    onDownload: () -> Unit,
    onDownloadAsset: ((ApkAsset) -> Unit)? = null,
    onInstall: () -> Unit = {},
    onDismiss: () -> Unit,
    onOpenReleaseUrl: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val isDownloading = downloadProgress in 0..100
    val effectiveSize = if (totalBytes > 0L) totalBytes else fileSize
    val sizeText = remember(effectiveSize) { formatApkSize(effectiveSize) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isDownloading) downloadProgress / 100f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "downloadProgressAnim",
    )

    // Build variants resolution
    val effectiveAssets = remember(apkAssets, fileSize) {
        if (apkAssets.isNotEmpty()) {
            apkAssets
        } else {
            listOf(
                ApkAsset("app-arm64-v8a-release.apk", "", fileSize, "arm64-v8a", isRecommended = true),
                ApkAsset("app-armeabi-v7a-release.apk", "", fileSize, "armeabi-v7a", isRecommended = false),
                ApkAsset("app-universal-release.apk", "", fileSize, "universal", isRecommended = false),
            )
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Builds, 1: Changelog, 2: Commits

    AlertDialog(
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = false,
        ),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // --- Top Glowing Icon Banner ---
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = when {
                                    isReadyToInstall -> listOf(Color(0xFF4CAF50).copy(alpha = 0.25f), Color(0xFF81C784).copy(alpha = 0.15f))
                                    isUpToDate -> listOf(Color(0xFF2E7D32).copy(alpha = 0.20f), Color(0xFF4CAF50).copy(alpha = 0.10f))
                                    else -> listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    )
                                },
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when {
                            isReadyToInstall || isUpToDate -> Icons.Rounded.CheckCircle
                            else -> Icons.Rounded.Download
                        },
                        contentDescription = null,
                        tint = when {
                            isReadyToInstall || isUpToDate -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(30.dp),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Title ---
                Text(
                    text = when {
                        isReadyToInstall -> "Update Ready to Install"
                        isDownloading -> "Downloading Update"
                        isUpToDate -> "Krypton Player is Up to Date"
                        else -> "Krypton Player v$versionName"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // --- Subtitle / Context description ---
                Text(
                    text = when {
                        isReadyToInstall -> "The update package is ready. Tap install to finish."
                        isDownloading -> "Downloading APK package... Keep app open or background."
                        isUpToDate -> "You are on the latest version. You can reinstall builds or view commit history below."
                        else -> "A new version of Krypton Player is available with the latest fixes & improvements."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // --- Version Comparison Banner ---
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (currentVersionName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Text(
                                text = "v$currentVersionName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }

                        Icon(
                            imageVector = NextIcons.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(14.dp),
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "v$versionName",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            if (!isUpToDate) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        text = "NEW",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }

                    if (sizeText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = sizeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Progress Indicator Area (if downloading) ---
                AnimatedVisibility(
                    visible = isDownloading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            strokeCap = StrokeCap.Round,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val transferredText = if (bytesDownloaded > 0L && totalBytes > 0L) {
                                "${formatApkSize(bytesDownloaded)} / ${formatApkSize(totalBytes)}"
                            } else {
                                "Downloading package..."
                            }
                            Text(
                                text = transferredText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "$downloadProgress%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // --- Tab Selector: APK Builds | Release Notes | Commits ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Tab 0: APK Builds
                    TabButton(
                        title = "APK Builds",
                        count = effectiveAssets.size,
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                    )

                    // Tab 1: Changelog
                    TabButton(
                        title = "Notes",
                        count = null,
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(0.9f),
                    )

                    // Tab 2: Commits
                    TabButton(
                        title = "Commits",
                        count = if (commits.isNotEmpty()) commits.size else null,
                        isSelected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Content Box (Scrollable area for selected tab) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(10.dp),
                ) {
                    when (selectedTab) {
                        0 -> {
                            // TAB 0: APK BUILDS
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                effectiveAssets.forEach { asset ->
                                    ApkBuildCard(
                                        asset = asset,
                                        isDownloading = isDownloading,
                                        isReadyToInstall = isReadyToInstall,
                                        onDownload = {
                                            if (onDownloadAsset != null) {
                                                onDownloadAsset(asset)
                                            } else {
                                                onDownload()
                                            }
                                        },
                                        onInstall = onInstall,
                                    )
                                }
                            }
                        }
                        1 -> {
                            // TAB 1: RELEASE NOTES
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Release Notes",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (onOpenReleaseUrl != null) {
                                        IconButton(
                                            onClick = onOpenReleaseUrl,
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.OpenInBrowser,
                                                contentDescription = "Open in GitHub",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                AppUpdateFormattedChangelog(changelog = changelog)
                            }
                        }
                        2 -> {
                            // TAB 2: RECENT COMMITS
                            if (commits.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No recent commits available.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    commits.take(15).forEach { commit ->
                                        CommitItemCard(commit = commit, uriHandler = uriHandler, context = context)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isReadyToInstall) {
                Button(
                    onClick = onInstall,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Install Update",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else if (isDownloading) {
                FilledTonalButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = "Background",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Button(
                    onClick = onDownload,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUpToDate) "Reinstall APK" else "Download",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onOpenReleaseUrl != null && !isDownloading) {
                    TextButton(
                        onClick = onOpenReleaseUrl,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Browser",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (!isDownloading) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            text = if (isReadyToInstall || isUpToDate) "Close" else "Later",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

@Composable
private fun TabButton(
    title: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (count != null && count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ApkBuildCard(
    asset: ApkAsset,
    isDownloading: Boolean,
    isReadyToInstall: Boolean,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val sizeText = remember(asset.size) { formatApkSize(asset.size) }
    val abiTitle = remember(asset.abi) {
        when (asset.abi.lowercase()) {
            "arm64-v8a", "arm64" -> "ARM64 (64-bit)"
            "armeabi-v7a", "armv7" -> "ARMv7 (32-bit)"
            "universal" -> "Universal"
            else -> asset.abi.uppercase()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (asset.isRecommended) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (asset.isRecommended) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (asset.isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Android,
                            contentDescription = null,
                            tint = if (asset.isRecommended) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = abiTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (asset.isRecommended) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = "RECOMMENDED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (sizeText.isNotBlank()) "$sizeText • ${asset.name}" else asset.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isReadyToInstall && asset.isRecommended) {
                Button(
                    onClick = onInstall,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.height(34.dp),
                ) {
                    Text("Install", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            } else if (isDownloading && asset.isRecommended) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                OutlinedButton(
                    onClick = onDownload,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Get", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CommitItemCard(
    commit: GitHubCommitInfo,
    uriHandler: UriHandler,
    context: Context,
) {
    val formattedDate = remember(commit.date) {
        try {
            if (commit.date.length >= 10) {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val d = parser.parse(commit.date.substring(0, 10))
                d?.let { formatter.format(it) } ?: commit.date.substring(0, 10)
            } else {
                commit.date
            }
        } catch (_: Exception) {
            commit.date
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = commit.shortSha,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = commit.authorName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (formattedDate.isNotBlank()) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = commit.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (commit.commitUrl.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "View commit ↗",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                try {
                                    uriHandler.openUri(commit.commitUrl)
                                } catch (_: Exception) {}
                            }
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AppUpdateFormattedChangelog(
    changelog: String,
    modifier: Modifier = Modifier,
) {
    val lines = remember(changelog) {
        if (changelog.isBlank()) listOf("• Performance improvements and bug fixes.")
        else changelog.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        lines.forEach { line ->
            when {
                line.startsWith("#") -> {
                    val headerText = line.trimStart('#', ' ')
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                line.startsWith("*") || line.startsWith("-") -> {
                    val bulletText = line.substring(1).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = bulletText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp,
                        )
                    }
                }
                else -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp,
                    )
                }
            }
        }
    }
}
