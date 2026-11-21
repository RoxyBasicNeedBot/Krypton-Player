package dev.roxy.kryptonplayer.core.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.East
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.data.update.ApkAsset
import dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo
import dev.roxy.kryptonplayer.core.data.update.UpdateState
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Visual states for the animated pill update action button.
 */
sealed interface UpdateActionButtonState {
    data class Checking(val text: String = "Checking for updates…") : UpdateActionButtonState

    data class Available(
        val fileSizeText: String? = null,
        val customText: String? = null,
    ) : UpdateActionButtonState

    data class Downloading(
        val progress: Int,
        val speedText: String? = null,
        val downloadedText: String? = null,
        val totalText: String? = null,
    ) : UpdateActionButtonState

    data class Verifying(
        val text: String = "Verifying package…",
    ) : UpdateActionButtonState

    data class ReadyToInstall(
        val customText: String = "Install Update",
    ) : UpdateActionButtonState

    data class Error(
        val message: String = "Download failed · Tap to retry",
        val canRetry: Boolean = true,
    ) : UpdateActionButtonState

    data class UpToDate(
        val text: String = "You're up to date",
    ) : UpdateActionButtonState
}

/**
 * Converts a domain [UpdateState] into an [UpdateActionButtonState].
 */
fun UpdateState.toUpdateActionButtonState(fileSize: Long = 0L): UpdateActionButtonState {
    return when (this) {
        is UpdateState.Checking -> UpdateActionButtonState.Checking()
        is UpdateState.Available -> UpdateActionButtonState.Available(
            fileSizeText = if (fileSize > 0L) formatUpdateBytes(fileSize) else null,
        )
        is UpdateState.Downloading -> UpdateActionButtonState.Downloading(
            progress = progress.coerceIn(0, 100),
            speedText = if (speedBytesPerSec > 0L) formatUpdateSpeed(speedBytesPerSec) else null,
            downloadedText = if (bytesDownloaded > 0L) formatUpdateBytes(bytesDownloaded) else null,
            totalText = if (totalBytes > 0L) formatUpdateBytes(totalBytes) else null,
        )
        is UpdateState.Verifying -> UpdateActionButtonState.Verifying(
            text = message.ifBlank { "Verifying package…" },
        )
        is UpdateState.ReadyToInstall -> UpdateActionButtonState.ReadyToInstall()
        is UpdateState.Error -> UpdateActionButtonState.Error(
            message = if (canRetry) "Retry Download" else "Update Unavailable",
            canRetry = canRetry,
        )
        is UpdateState.UpToDate -> UpdateActionButtonState.UpToDate()
        is UpdateState.Idle -> UpdateActionButtonState.Available(
            fileSizeText = if (fileSize > 0L) formatUpdateBytes(fileSize) else null,
        )
    }
}

/**
 * Detects the primary CPU ABI for the current device.
 */
fun detectDeviceUpdateAbi(): String {
    val supportedAbis = (Build.SUPPORTED_ABIS ?: emptyArray()).map { it.lowercase(Locale.US) }
    for (abi in supportedAbis) {
        if (abi.contains("arm64-v8a") || abi.contains("arm64") || abi.contains("aarch64")) return "arm64-v8a"
        if (abi.contains("armeabi-v7a") || abi.contains("armv7") || abi.contains("armeabi")) return "armeabi-v7a"
        if (abi.contains("x86_64")) return "x86_64"
        if (abi.contains("x86")) return "x86"
    }
    return "universal"
}

/**
 * Dedicated Material 3 Modal Bottom Sheet for in-app updates.
 *
 * Includes:
 * - Rounded top surface with a centered pill drag handle
 * - State-reactive header badge and status subtitle
 * - Side-by-side Current -> Latest version comparison card
 * - Horizontally scrollable metadata chips (channel badge, ABI, file size, release date)
 * - Optional APK architecture variant switcher when multiple builds are attached
 * - Expandable Markdown release notes card
 * - Live verification / error status feedback
 * - Sticky bottom action bar with an animated horizontal-fill pill button,
 *   cancel support during active downloads, snooze ("Remind me later"), skip version,
 *   and external release page link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateSheet(
    info: AppUpdateInfo,
    updateState: UpdateState,
    currentVersionName: String,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    appTitle: String = "Krypton Player Update",
    onDownloadAsset: ((ApkAsset) -> Unit)? = null,
    onCancelDownload: (() -> Unit)? = null,
    onRemindLater: (() -> Unit)? = null,
    onSkipVersion: (() -> Unit)? = null,
    onOpenReleaseUrl: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val context = LocalContext.current
    val isDownloading = updateState is UpdateState.Downloading
    val isVerifying = updateState is UpdateState.Verifying
    val isReady = updateState is UpdateState.ReadyToInstall
    val isError = updateState is UpdateState.Error
    val isBusy = isDownloading || isVerifying

    val defaultAsset = remember(info.apkAssets) {
        info.apkAssets.firstOrNull { it.isRecommended } ?: info.apkAssets.firstOrNull()
    }
    var selectedAsset by remember(info.tagName, info.apkAssets) {
        mutableStateOf(defaultAsset)
    }

    val activeFileSize = selectedAsset?.size?.takeIf { it > 0L } ?: info.fileSize
    val detectedAbi = remember { detectDeviceUpdateAbi() }
    val activeAbiLabel = remember(selectedAsset, detectedAbi) {
        val abi = selectedAsset?.abi?.ifBlank { detectedAbi } ?: detectedAbi
        if (selectedAsset?.isRecommended != false) "$abi · Recommended" else abi
    }
    val formattedDate = remember(info.releaseDate) { formatReleaseDate(info.releaseDate) }

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
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
            )
        },
        modifier = modifier.testTag("app_update_sheet"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Scrollable Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 6.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Header Row ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UpdateHeaderBadge(
                        isReady = isReady,
                        isError = isError,
                        isVerifying = isVerifying,
                        isDownloading = isDownloading,
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isReady -> "Ready to Install"
                                isError -> "Update Interrupted"
                                isVerifying -> "Verifying Package"
                                isDownloading -> "Downloading Update"
                                else -> appTitle
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val subtitleText = when (val state = updateState) {
                            is UpdateState.ReadyToInstall -> "Verified and ready to install"
                            is UpdateState.Verifying -> state.message.ifBlank { "Checking APK signature & integrity" }
                            is UpdateState.Downloading -> {
                                val dl = if (state.bytesDownloaded > 0L) formatUpdateBytes(state.bytesDownloaded) else "0 MB"
                                val total = if (state.totalBytes > 0L) formatUpdateBytes(state.totalBytes) else formatUpdateBytes(activeFileSize)
                                val spd = if (state.speedBytesPerSec > 0L) " · ${formatUpdateSpeed(state.speedBytesPerSec)}" else ""
                                "$dl / $total$spd"
                            }
                            is UpdateState.Error -> "Something went wrong during the update"
                            else -> "New release ready with the latest improvements"
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (!isBusy) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("update_close_button"),
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

                // ── Version Comparison Card ───────────────────────────────────
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
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatVersionTag(currentVersionName),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

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

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Latest",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatVersionTag(info.versionName),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // ── Metadata Chip Strip ───────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isStable = !info.versionName.contains("beta", ignoreCase = true) &&
                        !info.versionName.contains("nightly", ignoreCase = true) &&
                        !info.versionName.contains("alpha", ignoreCase = true) &&
                        !info.versionName.contains("rc", ignoreCase = true)

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

                    UpdateMetaChip(
                        icon = Icons.Rounded.Memory,
                        text = activeAbiLabel,
                    )

                    if (activeFileSize > 0L) {
                        UpdateMetaChip(
                            icon = Icons.Rounded.FolderZip,
                            text = formatUpdateBytes(activeFileSize),
                        )
                    }

                    if (formattedDate.isNotBlank()) {
                        UpdateMetaChip(
                            icon = Icons.Rounded.CalendarToday,
                            text = formattedDate,
                        )
                    }
                }

                // ── APK Build Variant Selector (when multiple APKs exist) ─────
                if (info.apkAssets.size > 1 && onDownloadAsset != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Build Variant",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            info.apkAssets.forEach { asset ->
                                val isSelected = selectedAsset?.name == asset.name
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                        },
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isBusy) { selectedAsset = asset },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = asset.abi,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                        if (asset.size > 0L) {
                                            Text(
                                                text = formatUpdateBytes(asset.size),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Inline Error Banner ───────────────────────────────────────
                AnimatedVisibility(visible = isError, enter = fadeIn(), exit = fadeOut()) {
                    val errorMsg = (updateState as? UpdateState.Error)?.message
                        ?: "Failed to complete update."
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                // ── Inline Verification Banner ────────────────────────────────
                AnimatedVisibility(visible = isVerifying, enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = (updateState as? UpdateState.Verifying)?.message
                                    ?: "Verifying package signature & integrity…",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                // ── Changelog Card ────────────────────────────────────────────
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
                                    text = "What's New in ${formatVersionTag(info.versionName)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            IconButton(
                                onClick = { isChangelogExpanded = !isChangelogExpanded },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("update_changelog_expand_button"),
                            ) {
                                Icon(
                                    imageVector = if (isChangelogExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = if (isChangelogExpanded) "Collapse release notes" else "Expand release notes",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isChangelogExpanded) {
                                        Modifier
                                    } else {
                                        Modifier.heightIn(max = 220.dp)
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

            // ── Sticky Bottom Action Bar ──────────────────────────────────────
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val triggerDownload = {
                        val chosen = selectedAsset
                        if (chosen != null && onDownloadAsset != null) {
                            onDownloadAsset(chosen)
                        } else {
                            onDownload()
                        }
                    }

                    UpdateActionButton(
                        state = updateState.toUpdateActionButtonState(fileSize = activeFileSize),
                        onClick = {
                            when (updateState) {
                                is UpdateState.ReadyToInstall -> onInstall()
                                is UpdateState.Downloading, is UpdateState.Verifying, is UpdateState.Checking -> Unit
                                is UpdateState.Error -> {
                                    if (updateState.canRetry) {
                                        triggerDownload()
                                    }
                                }
                                else -> triggerDownload()
                            }
                        },
                        enabled = !isBusy && (updateState !is UpdateState.Error || updateState.canRetry),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_primary_action_button"),
                    )

                    // Secondary Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isDownloading && onCancelDownload != null) {
                            TextButton(
                                onClick = onCancelDownload,
                                modifier = Modifier.testTag("update_cancel_download_button"),
                            ) {
                                Text(
                                    text = "Cancel Download",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    onRemindLater?.invoke() ?: onDismiss()
                                },
                                enabled = !isBusy,
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.testTag("update_remind_later_button"),
                            ) {
                                Text(
                                    text = "Remind me later",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            if (onSkipVersion != null && info.tagName.isNotBlank() && !isReady) {
                                Text(
                                    text = "·",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                                TextButton(
                                    onClick = onSkipVersion,
                                    enabled = !isBusy,
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.testTag("update_skip_version_button"),
                                ) {
                                    Text(
                                        text = "Skip",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }

                            if (info.releaseUrl.isNotBlank() || onOpenReleaseUrl != null) {
                                Text(
                                    text = "·",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                                TextButton(
                                    onClick = {
                                        if (onOpenReleaseUrl != null) {
                                            onOpenReleaseUrl()
                                        } else {
                                            openExternalUrl(context, info.releaseUrl)
                                        }
                                    },
                                    enabled = !isBusy,
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.testTag("update_release_page_button"),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = "Release Page",
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
}

/**
 * Animated pill button with spring tactile feedback, horizontal progress fill,
 * leading edge glow during downloads, shimmer during checks, and smooth crossfades.
 */
@Composable
fun UpdateActionButton(
    state: UpdateActionButtonState,
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

    val isInteractive = enabled &&
        state !is UpdateActionButtonState.UpToDate &&
        state !is UpdateActionButtonState.Checking &&
        state !is UpdateActionButtonState.Verifying

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "updateButtonPressScale",
    )

    val targetFillFraction = when (state) {
        is UpdateActionButtonState.Downloading -> (state.progress / 100f).coerceIn(0f, 1f)
        is UpdateActionButtonState.Verifying -> 1f
        is UpdateActionButtonState.ReadyToInstall -> 1f
        else -> 0f
    }

    val animatedFillFraction by animateFloatAsState(
        targetValue = targetFillFraction,
        animationSpec = tween(
            durationMillis = if (state is UpdateActionButtonState.ReadyToInstall) 300 else 180,
            easing = LinearOutSlowInEasing,
        ),
        label = "updateButtonFillFraction",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "updateShimmerTransition")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "updateShimmerTranslate",
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
                    Color.White.copy(alpha = if (state is UpdateActionButtonState.ReadyToInstall) 0.45f else 0.20f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    Color.White.copy(alpha = if (state is UpdateActionButtonState.ReadyToInstall) 0.45f else 0.20f),
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
            if (animatedFillFraction > 0f) {
                val fillBrush = if (state is UpdateActionButtonState.ReadyToInstall) {
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
                            progressFillColor.copy(alpha = 0.48f),
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

                if (state is UpdateActionButtonState.Downloading && animatedFillFraction in 0.02f..0.98f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = animatedFillFraction),
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

            if (state is UpdateActionButtonState.Checking || state is UpdateActionButtonState.Verifying) {
                val shimmerBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.24f),
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

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)))
                        .togetherWith(fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                },
                label = "updateButtonContent",
            ) { targetState ->
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    when (targetState) {
                        is UpdateActionButtonState.Checking -> {
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        is UpdateActionButtonState.Available -> {
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

                        is UpdateActionButtonState.Downloading -> {
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

                        is UpdateActionButtonState.Verifying -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                strokeCap = StrokeCap.Round,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = targetState.text,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        is UpdateActionButtonState.ReadyToInstall -> {
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

                        is UpdateActionButtonState.Error -> {
                            Icon(
                                imageVector = if (targetState.canRetry) Icons.Rounded.Refresh else Icons.Rounded.WarningAmber,
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

                        is UpdateActionButtonState.UpToDate -> {
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

@Composable
private fun UpdateHeaderBadge(
    isReady: Boolean,
    isError: Boolean,
    isVerifying: Boolean,
    isDownloading: Boolean,
) {
    val bgBrush = when {
        isReady -> Brush.linearGradient(
            listOf(Color(0xFF10B981).copy(alpha = 0.25f), Color(0xFF059669).copy(alpha = 0.12f)),
        )
        isError -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
            ),
        )
        else -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
            ),
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush),
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
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
            )
            isVerifying -> {
                val transition = rememberInfiniteTransition(label = "verifyBadgeRotation")
                val rotation by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "verifyRotation",
                )
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(rotation),
                )
            }
            isDownloading -> Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            else -> Icon(
                imageVector = Icons.Rounded.RocketLaunch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
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

private fun formatVersionTag(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "v0.0.0"
    return if (trimmed.startsWith("v", ignoreCase = true)) trimmed else "v$trimmed"
}

private fun formatReleaseDate(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return try {
        if (trimmed.length >= 10) {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val parsed = parser.parse(trimmed.substring(0, 10))
            parsed?.let { formatter.format(it) } ?: trimmed.substring(0, 10)
        } else {
            trimmed
        }
    } catch (_: Exception) {
        trimmed.take(10)
    }
}

private fun formatUpdateBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    }
}

private fun formatUpdateSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return ""
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB/s", mb)
    } else {
        String.format(Locale.US, "%.0f KB/s", kb)
    }
}

private fun openExternalUrl(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
