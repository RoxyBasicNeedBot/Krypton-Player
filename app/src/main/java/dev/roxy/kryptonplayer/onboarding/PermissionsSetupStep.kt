package dev.roxy.kryptonplayer.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.roxy.kryptonplayer.core.ui.preview.DayNightPreview
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.R
import dev.roxy.kryptonplayer.core.common.hasMediaAccessPermission
import dev.roxy.kryptonplayer.core.common.storagePermissions
import dev.roxy.kryptonplayer.core.ui.theme.JioHotstarFontFamily

/**
 * Stage 3 — Integrated Storage & Notification Permissions Hub.
 */
@Composable
internal fun PermissionsSetupStep(
    onStorageGranted: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val permPrefs = remember { context.getSharedPreferences("krypton_permission_prefs", Context.MODE_PRIVATE) }

    fun checkStorage(): Boolean = context.hasMediaAccessPermission()

    fun checkNotification(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    var hasStorage by remember { mutableStateOf(checkStorage()) }
    var hasNotification by remember { mutableStateOf(checkNotification()) }
    var storagePermanentlyDenied by remember { mutableStateOf(false) }
    var notifPermanentlyDenied by remember { mutableStateOf(false) }

    fun refreshPermissionStates() {
        val nowStorage = checkStorage()
        if (nowStorage && !hasStorage) {
            onStorageGranted()
        }
        hasStorage = nowStorage
        hasNotification = checkNotification()

        if (!hasStorage && activity != null) {
            val askedBefore = permPrefs.getBoolean("has_requested_storage", false)
            storagePermanentlyDenied = askedBefore &&
                storagePermissions.none { perm ->
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
                }
        } else {
            storagePermanentlyDenied = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotification && activity != null) {
            val askedBefore = permPrefs.getBoolean("has_requested_notifications", false)
            notifPermanentlyDenied = askedBefore &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notifPermanentlyDenied = false
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshPermissionStates()
    }

    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        val granted = checkStorage()
        hasStorage = granted
        if (granted) {
            storagePermanentlyDenied = false
            onStorageGranted()
        } else if (activity != null) {
            storagePermanentlyDenied = storagePermissions.none { perm ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotification = granted
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null) {
            notifPermanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }

    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun requestStorage() {
        if (hasStorage) return
        if (storagePermanentlyDenied) {
            openAppSettings()
        } else {
            permPrefs.edit().putBoolean("has_requested_storage", true).apply()
            storageLauncher.launch(storagePermissions)
        }
    }

    fun requestNotifications() {
        if (hasNotification || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (notifPermanentlyDenied) {
            openAppSettings()
        } else {
            permPrefs.edit().putBoolean("has_requested_notifications", true).apply()
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    KryptonOnboardingPage(
        badge = {
            KryptonOnbBadge(
                icon = Icons.Rounded.VerifiedUser,
                contentDescription = stringResource(R.string.onb_perm_title),
            )
        },
        title = stringResource(R.string.onb_perm_title),
        subtitle = stringResource(R.string.onb_perm_subtitle),
        stepIndex = 1,
        totalSteps = 3,
        footer = {
            if (hasStorage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KryptonOnbSecondaryButton(
                        text = stringResource(R.string.onb_back),
                        onClick = onBack,
                        testTag = "onboarding_perm_back_button",
                        modifier = Modifier.weight(0.42f),
                    )
                    KryptonOnbPrimaryButton(
                        text = stringResource(R.string.onb_continue),
                        onClick = onNext,
                        testTag = "onboarding_perm_continue_button",
                        modifier = Modifier.weight(0.58f),
                    )
                }
            } else {
                KryptonOnbPrimaryButton(
                    text = if (storagePermanentlyDenied) {
                        stringResource(R.string.onb_perm_open_settings)
                    } else {
                        stringResource(R.string.onb_perm_allow_storage)
                    },
                    onClick = { requestStorage() },
                    testTag = "onboarding_perm_allow_storage_button",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KryptonOnbSecondaryButton(
                        text = stringResource(R.string.onb_back),
                        onClick = onBack,
                        testTag = "onboarding_perm_back_button",
                        modifier = Modifier.weight(0.42f),
                    )
                    KryptonOnbSecondaryButton(
                        text = stringResource(R.string.onb_perm_skip),
                        onClick = onNext,
                        testTag = "onboarding_perm_skip_button",
                        modifier = Modifier.weight(0.58f),
                    )
                }
            }
        },
    ) {
        PermissionCard(
            icon = Icons.Rounded.FolderOpen,
            title = stringResource(R.string.onb_perm_storage_title),
            description = stringResource(R.string.onb_perm_storage_desc),
            isGranted = hasStorage,
            statusLabel = when {
                hasStorage -> stringResource(R.string.onb_perm_granted)
                storagePermanentlyDenied -> stringResource(R.string.onb_perm_open_settings)
                else -> stringResource(R.string.onb_perm_required)
            },
            onClick = { requestStorage() },
            testTag = "onboarding_perm_storage_card",
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(Modifier.height(12.dp))
            PermissionCard(
                icon = Icons.Rounded.NotificationsNone,
                title = stringResource(R.string.onb_perm_notif_title),
                description = stringResource(R.string.onb_perm_notif_desc),
                isGranted = hasNotification,
                statusLabel = when {
                    hasNotification -> stringResource(R.string.onb_perm_granted)
                    notifPermanentlyDenied -> stringResource(R.string.onb_perm_open_settings)
                    else -> stringResource(R.string.onb_perm_optional)
                },
                onClick = { requestNotifications() },
                testTag = "onboarding_perm_notif_card",
            )
        }

        Spacer(Modifier.height(18.dp))

        // Matte privacy guarantee strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = KryptonOnbColors.Card.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, KryptonOnbColors.CardBorder.copy(alpha = 0.6f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = KryptonOnbColors.Subtitle,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.onb_perm_privacy_title),
                        fontFamily = JioHotstarFontFamily,
                        color = KryptonOnbColors.Title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.onb_perm_privacy_desc),
                        fontFamily = JioHotstarFontFamily,
                        color = KryptonOnbColors.Muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    statusLabel: String,
    onClick: () -> Unit,
    testTag: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isGranted) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "permCardScale",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                enabled = !isGranted,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = if (isPressed && !isGranted) KryptonOnbColors.CardPressed else KryptonOnbColors.Card,
        border = BorderStroke(
            width = 1.dp,
            color = if (isGranted) {
                KryptonOnbColors.SageAccent.copy(alpha = 0.45f)
            } else {
                KryptonOnbColors.CardBorder
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (isGranted) KryptonOnbColors.SageContainer else KryptonOnbColors.CardElevated,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Rounded.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isGranted) KryptonOnbColors.SageAccent else KryptonOnbColors.Title,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        fontFamily = JioHotstarFontFamily,
                        color = KryptonOnbColors.Title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.02).em,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isGranted) KryptonOnbColors.SageContainer else KryptonOnbColors.CardElevated,
                    ) {
                        Text(
                            text = statusLabel,
                            fontFamily = JioHotstarFontFamily,
                            color = if (isGranted) KryptonOnbColors.SageAccent else KryptonOnbColors.Subtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    fontFamily = JioHotstarFontFamily,
                    color = KryptonOnbColors.Muted,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@DayNightPreview
@Composable
private fun PermissionsSetupStepPreview() {
    KryptonPlayerTheme {
        PermissionsSetupStep(
            onStorageGranted = {},
            onBack = {},
            onNext = {},
        )
    }
}
