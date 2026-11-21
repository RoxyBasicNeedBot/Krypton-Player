package dev.roxy.kryptonplayer.feature.player.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.feature.player.state.AudioBrand
import dev.roxy.kryptonplayer.feature.player.state.AudioDeviceType
import dev.roxy.kryptonplayer.feature.player.state.AudioOutputState
import dev.roxy.kryptonplayer.feature.player.state.getAudioBrandIconRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputBottomSheet(
    audioOutputState: AudioOutputState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sheetBg = Color(0xFF10121A)
    val cardBg = Color(0xFF181C28)
    val selectedCardBg = Color(0xFF1E2840)
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)
    val accentBlue = Color(0xFF2563EB)
    val borderColor = Color(0xFF2B334D)
    val selectedBorderColor = Color(0xFF3B82F6)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(accentBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (audioOutputState.currentDevice.type == AudioDeviceType.SPEAKER) {
                            Icon(
                                painter = painterResource(R.drawable.ic_audio_speaker_clean),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            val headerIcon = when (audioOutputState.currentDevice.type) {
                                AudioDeviceType.EARBUDS_BLUETOOTH -> getAudioBrandIconRes(audioOutputState.currentDevice.brand)
                                AudioDeviceType.HEADPHONES_WIRED -> R.drawable.ic_headset
                                AudioDeviceType.USB -> R.drawable.ic_usb_cast_custom
                                else -> R.drawable.ic_audio_speaker_clean
                            }
                            Icon(
                                painter = painterResource(headerIcon),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Audio Output",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Select active playback device",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Devices List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                audioOutputState.availableDevices.forEach { device ->
                    val isSelected = device.isSelected
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                audioOutputState.selectDevice(device)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) selectedCardBg else cardBg,
                        border = BorderStroke(1.dp, if (isSelected) selectedBorderColor else borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (isSelected) accentBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (device.type == AudioDeviceType.SPEAKER) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_audio_speaker_clean),
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else textSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        val deviceIcon = when (device.type) {
                                            AudioDeviceType.EARBUDS_BLUETOOTH -> getAudioBrandIconRes(device.brand)
                                            AudioDeviceType.HEADPHONES_WIRED -> R.drawable.ic_headset
                                            AudioDeviceType.USB -> R.drawable.ic_usb_cast_custom
                                            else -> R.drawable.ic_audio_speaker_clean
                                        }
                                        Icon(
                                            painter = painterResource(deviceIcon),
                                            contentDescription = null,
                                            tint = if (device.type == AudioDeviceType.EARBUDS_BLUETOOTH) Color.Unspecified else (if (isSelected) Color(0xFF60A5FA) else textSecondary),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = device.name,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else textPrimary
                                    )
                                    val brandTag = when {
                                        device.type == AudioDeviceType.SPEAKER -> "Built-in"
                                        device.type == AudioDeviceType.HEADPHONES_WIRED -> "Wired"
                                        device.type == AudioDeviceType.USB -> "USB"
                                        device.brand == AudioBrand.APPLE_AIRPODS -> "Apple"
                                        device.brand == AudioBrand.REALME -> "Realme"
                                        device.brand == AudioBrand.ONEPLUS -> "OnePlus"
                                        device.brand == AudioBrand.BOAT -> "boAt"
                                        device.brand == AudioBrand.SAMSUNG -> "Samsung"
                                        device.brand == AudioBrand.SONY -> "Sony"
                                        device.brand == AudioBrand.JBL -> "JBL"
                                        device.brand == AudioBrand.NOISE -> "Noise"
                                        device.brand == AudioBrand.BOULT -> "Boult"
                                        device.brand == AudioBrand.NOTHING -> "Nothing"
                                        device.brand == AudioBrand.XIAOMI -> "Xiaomi"
                                        device.brand == AudioBrand.GOOGLE -> "Google"
                                        else -> "Bluetooth"
                                    }
                                    Text(
                                        text = if (isSelected) "$brandTag • Active Output" else "$brandTag • Tap to switch",
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color(0xFF60A5FA) else textSecondary
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(accentBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // System Output / Bluetooth Settings Action Button
            OutlinedButton(
                onClick = {
                    audioOutputState.openSystemMediaOutput()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = textSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "System Output & Bluetooth Settings",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
