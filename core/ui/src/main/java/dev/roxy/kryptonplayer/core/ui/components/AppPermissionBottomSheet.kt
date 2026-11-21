package dev.roxy.kryptonplayer.core.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.ui.R

enum class AppPermissionType {
    STORAGE,
    NOTIFICATION
}

/**
 * AppPermissionBottomSheet: God-Level, ultra-polished permission and re-permission bottom sheet.
 * Handles initial request rationale as well as re-permission guidance to Android App Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPermissionBottomSheet(
    type: AppPermissionType,
    isRePermission: Boolean = false,
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sheetBg = if (isDark) Color(0xFF12151F) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardSurface = if (isDark) Color(0xFF181C2A) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color(0xFF282F45) else Color(0xFFE2E8F0)
    val accentBlue = Color(0xFF2563EB)
    val accentCyan = Color(0xFF06B6D4)
    val accentViolet = Color(0xFF7C3AED)
    val primaryButtonBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val primaryButtonText = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)

    fun openAppSettings(ctx: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }


    val titleText = when {
        isRePermission && type == AppPermissionType.STORAGE -> "Enable Storage in Settings"
        isRePermission && type == AppPermissionType.NOTIFICATION -> "Enable Notifications in Settings"
        type == AppPermissionType.STORAGE -> "Storage Access Required"
        else -> "Notifications & Playback Controls"
    }

    val subtitleText = when {
        isRePermission && type == AppPermissionType.STORAGE ->
            "Storage access was previously declined. To play your local media, please enable storage permission in device settings."
        isRePermission && type == AppPermissionType.NOTIFICATION ->
            "Notifications were previously declined. To show playback controls on your lock screen, please enable notifications in device settings."
        type == AppPermissionType.STORAGE ->
            "To discover, index, and play your local videos, movies, and audio files, Krypton Player needs access to your media storage."
        else ->
            "Stay in control with lock screen media controls, background download progress, and timely update alerts."
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Premium Animated Vector Sticker (60fps Lottie Telegram Animation)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                (if (type == AppPermissionType.STORAGE) accentBlue else accentViolet).copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val lottieRes = when {
                    isRePermission -> R.raw.anim_telegram_shield
                    type == AppPermissionType.STORAGE -> R.raw.photos
                    else -> R.raw.anim_telegram_bell
                }
                LottieStickerImage(
                    resId = lottieRes,
                    size = 96.dp,
                    contentDescription = titleText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Re-permission pill badge
            if (isRePermission) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = accentViolet.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentViolet.copy(alpha = 0.35f)),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "PERMISSION ACTION REQUIRED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA78BFA) else accentViolet,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. Title & Description
            Text(
                text = titleText,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.4).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitleText,
                fontSize = 13.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Numbered Steps Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = cardSurface,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isRePermission) {
                        PermissionStepRow(
                            stepNumber = "1",
                            title = "Open App Settings",
                            description = "Tap the button below to jump directly into Android's app permissions for Krypton Player.",
                            accentColor = accentBlue,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        PermissionStepRow(
                            stepNumber = "2",
                            title = "Select Permissions",
                            description = "Locate and open the Permissions section in the app settings screen.",
                            accentColor = accentCyan,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        PermissionStepRow(
                            stepNumber = "3",
                            title = "Switch to Allow",
                            description = if (type == AppPermissionType.STORAGE)
                                "Allow access to Photos and Videos so your local library appears instantly."
                            else
                                "Turn on Notifications to enable player controls in your notification shade.",
                            accentColor = accentViolet,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    } else if (type == AppPermissionType.STORAGE) {
                        PermissionStepRow(
                            stepNumber = "1",
                            title = "Local Media Discovery",
                            description = "Scan and play video and audio files from your device folders and SD card.",
                            accentColor = accentBlue,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        PermissionStepRow(
                            stepNumber = "2",
                            title = "Smart Playback & Subtitles",
                            description = "Resume where you left off, manage playlists, and automatically load local subtitle tracks.",
                            accentColor = accentCyan,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        PermissionStepRow(
                            stepNumber = "3",
                            title = "100% Offline & Private",
                            description = "All media operations stay strictly on your phone. No files are ever sent to the cloud.",
                            accentColor = accentViolet,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    } else {
                        PermissionStepRow(
                            stepNumber = "1",
                            title = "Lock Screen & Shade Controls",
                            description = "Control playback, pause, and skip tracks directly from your notification center and lock screen.",
                            accentColor = accentBlue,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        PermissionStepRow(
                            stepNumber = "2",
                            title = "Download & Sync Progress",
                            description = "Live progress for background Telegram downloads and media indexing so you know when files are ready.",
                            accentColor = accentCyan,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        PermissionStepRow(
                            stepNumber = "3",
                            title = "Clean & Zero Spam",
                            description = "Only essential media status and important app updates. No annoying promotional spam ever.",
                            accentColor = accentViolet,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Privacy / No-Spam Guarantee Badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = accentBlue.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, accentBlue.copy(alpha = 0.22f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_custom_shield),
                        contentDescription = null,
                        tint = accentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (type == AppPermissionType.STORAGE) "PRIVACY GUARANTEE" else "NO SPAM PROMISE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentBlue,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (type == AppPermissionType.STORAGE)
                                "We do not collect, read, or upload personal media files."
                            else
                                "Only active media player controls and background task status are shown.",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 5. Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(percent = 50),
                    border = BorderStroke(1.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary)
                ) {
                    Text(
                        text = if (isRePermission) "Maybe Later" else "Not Now",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        if (isRePermission) {
                            if (onOpenSettings != null) {
                                onOpenSettings()
                            } else {
                                openAppSettings(context)
                            }
                            onDismiss()
                        } else {
                            onGrant()
                        }
                    },
                    modifier = Modifier
                        .weight(1.35f)
                        .height(50.dp),
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonBg,
                        contentColor = primaryButtonText
                    )
                ) {
                    Text(
                        text = when {
                            isRePermission -> "Open Settings"
                            type == AppPermissionType.STORAGE -> "Allow Access"
                            else -> "Enable Controls"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PermissionStepRow(
    stepNumber: String,
    title: String,
    description: String,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(accentColor.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
