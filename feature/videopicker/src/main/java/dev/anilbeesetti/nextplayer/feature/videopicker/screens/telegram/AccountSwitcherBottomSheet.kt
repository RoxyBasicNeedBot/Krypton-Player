package dev.anilbeesetti.nextplayer.feature.videopicker.screens.telegram

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.core.model.TelegramAccount
import dev.anilbeesetti.nextplayer.core.ui.components.LottieTelegramDelete
import dev.anilbeesetti.nextplayer.core.ui.components.LottieVerifiedBadge
import dev.anilbeesetti.nextplayer.core.ui.theme.NextColors
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherBottomSheet(
    activeToken: String,
    activeUserId: Long,
    accounts: List<TelegramAccount>,
    onDismiss: () -> Unit,
    onSwitchAccount: (TelegramAccount) -> Unit,
    onAddAccount: (String, (Boolean, String?) -> Unit) -> Unit,
    onRemoveAccount: (TelegramAccount) -> Unit,
    onLogoutActive: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToRemove by remember { mutableStateOf<TelegramAccount?>(null) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Telegram Accounts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Switch between multiple saved accounts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account List
            val distinctAccounts = remember(accounts) { accounts.distinctBy { it.token + it.userId } }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(distinctAccounts, key = { index, account -> "${account.token}_${account.userId}_$index" }) { _, account ->
                    val isActive = account.token == activeToken || (account.userId != 0L && account.userId == activeUserId)

                    AccountItemCard(
                        account = account,
                        isActive = isActive,
                        onClick = {
                            if (!isActive) {
                                onSwitchAccount(account)
                                onDismiss()
                            }
                        },
                        onDeleteClick = {
                            accountToRemove = account
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Add Account Button (Nagram Style Card)
                    Surface(
                        onClick = { showAddAccountDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                NextColors.cardBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add Another Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Link with auth token or login code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Log Out Active Account Button
                    OutlinedButton(
                        onClick = { showLogoutConfirmDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log Out Current Account",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var newTokenInput by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf<String?>(null) }
        var isLinking by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isLinking) showAddAccountDialog = false
            },
            title = { Text("Add Telegram Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your Telegram Auth Token from the bot (e.g. ARCH-XXXX, /token, or paste bot link).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = newTokenInput,
                        onValueChange = {
                            newTokenInput = it
                            inputError = null
                        },
                        label = { Text("Auth Token / Linking Code") },
                        placeholder = { Text("e.g. ARCH-ABCD123") },
                        singleLine = true,
                        enabled = !isLinking,
                        isError = inputError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (inputError != null) {
                        Text(
                            text = inputError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isLinking,
                    onClick = {
                        val trimmed = newTokenInput.trim()
                        if (trimmed.isEmpty()) {
                            inputError = "Token cannot be empty"
                        } else {
                            isLinking = true
                            inputError = null
                            onAddAccount(trimmed) { success, err ->
                                isLinking = false
                                if (success) {
                                    showAddAccountDialog = false
                                    onDismiss()
                                    android.widget.Toast.makeText(context, "Account connected successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    inputError = err ?: "Invalid or banned linking token"
                                }
                            }
                        }
                    }
                ) {
                    if (isLinking) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...")
                    } else {
                        Text("Add & Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLinking,
                    onClick = { showAddAccountDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Account Confirmation Dialog
    if (accountToRemove != null) {
        val acc = accountToRemove!!
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            title = { Text("Remove Account?") },
            text = {
                Text("Are you sure you want to remove '${acc.firstName.ifEmpty { acc.username.ifEmpty { "Account" } }}' from this device?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveAccount(acc)
                        accountToRemove = null
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Log Out Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Log Out?") },
            text = {
                Text("Are you sure you want to log out of your current Telegram account?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogoutActive()
                        onDismiss()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AccountItemCard(
    account: TelegramAccount,
    isActive: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val displayName = account.firstName.ifEmpty { account.username.ifEmpty { "Telegram User" } }
    val subtitle = when {
        account.username.isNotEmpty() -> "@${account.username}"
        account.userId != 0L -> "ID: ${account.userId}"
        account.token.isNotEmpty() -> "Token: ${account.token.take(12)}..."
        else -> "Logged In"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                if (isActive) NextColors.cardBorder(MaterialTheme.colorScheme.primary, width = 1.5.dp) else NextColors.cardBorder(),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Photo
            val bitmap = remember(account.profilePhoto) {
                if (account.profilePhoto.isNotEmpty()) {
                    try {
                        val bytes = Base64.decode(account.profilePhoto, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (_: Exception) { null }
                } else null
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = displayName.take(1).uppercase()
                    Text(
                        text = initial,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Name & Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LottieVerifiedBadge(size = 16.dp)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status Indicator & Delete Action
            if (isActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Active",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                FilledTonalIconButton(
                    onClick = onDeleteClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    LottieTelegramDelete(
                        size = 20.dp,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
