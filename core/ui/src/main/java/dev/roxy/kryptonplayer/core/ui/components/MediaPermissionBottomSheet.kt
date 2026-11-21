package dev.roxy.kryptonplayer.core.ui.components

import androidx.compose.runtime.Composable

/**
 * MediaPermissionBottomSheet: Backward-compatible wrapper delegating to [AppPermissionBottomSheet]
 * with [AppPermissionType.STORAGE].
 */
@Composable
fun MediaPermissionBottomSheet(
    isRePermission: Boolean = false,
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    AppPermissionBottomSheet(
        type = AppPermissionType.STORAGE,
        isRePermission = isRePermission,
        onGrant = onGrant,
        onDismiss = onDismiss,
        onOpenSettings = onOpenSettings
    )
}
