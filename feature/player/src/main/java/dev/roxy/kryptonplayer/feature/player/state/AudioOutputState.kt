package dev.roxy.kryptonplayer.feature.player.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

enum class AudioDeviceType {
    SPEAKER,
    EARBUDS_BLUETOOTH,
    HEADPHONES_WIRED,
    USB,
    OTHER
}

enum class AudioBrand {
    APPLE_AIRPODS,
    REALME,
    ONEPLUS,
    BOAT,
    SAMSUNG,
    SONY,
    JBL,
    NOISE,
    BOULT,
    NOTHING,
    XIAOMI,
    GOOGLE,
    GENERIC_BLUETOOTH
}

fun detectAudioBrand(name: String): AudioBrand {
    val lower = name.lowercase()
    return when {
        lower.contains("airpod") || lower.contains("beats") || lower.contains("apple") || lower.contains("earpods") -> AudioBrand.APPLE_AIRPODS
        lower.contains("realme") || lower.contains("buds air") || lower.contains("buds q") || lower.contains("buds wireless") || lower.contains("dizo") -> AudioBrand.REALME
        lower.contains("oneplus") || lower.contains("nord buds") || lower.contains("bullets") -> AudioBrand.ONEPLUS
        lower.contains("boat") || lower.contains("airdopes") || lower.contains("rockerz") || lower.contains("nirvana") -> AudioBrand.BOAT
        lower.contains("galaxy buds") || lower.contains("galaxy bud") || lower.contains("samsung") || lower.contains("buds pro") || lower.contains("buds live") || lower.contains("buds2") || lower.contains("buds3") -> AudioBrand.SAMSUNG
        lower.contains("sony") || lower.contains("wh-") || lower.contains("wf-") || lower.contains("linkbuds") || lower.contains("xb910") || lower.contains("ch720") -> AudioBrand.SONY
        lower.contains("jbl") || lower.contains("tune") || lower.contains("wave") || lower.contains("flip") || lower.contains("charge") || lower.contains("live pro") -> AudioBrand.JBL
        lower.contains("noise") || lower.contains("colorfit") || lower.contains("buds vs") || lower.contains("shots") -> AudioBrand.NOISE
        lower.contains("boult") || lower.contains("airbass") || lower.contains("drift") -> AudioBrand.BOULT
        lower.contains("nothing") || lower.contains("ear (1)") || lower.contains("ear (2)") || lower.contains("ear (a)") || lower.contains("cmf") -> AudioBrand.NOTHING
        lower.contains("redmi") || lower.contains("xiaomi") || lower.contains("mi ") || lower.contains("poco") -> AudioBrand.XIAOMI
        lower.contains("pixel buds") || lower.contains("google") -> AudioBrand.GOOGLE
        else -> AudioBrand.GENERIC_BLUETOOTH
    }
}

fun getAudioBrandIconRes(brand: AudioBrand): Int {
    return when (brand) {
        AudioBrand.APPLE_AIRPODS -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_apple_airpods
        AudioBrand.REALME -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_realme
        AudioBrand.ONEPLUS -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_oneplus
        AudioBrand.BOAT -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_boat
        AudioBrand.SAMSUNG -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_samsung
        AudioBrand.SONY -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_sony
        AudioBrand.JBL -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_jbl
        AudioBrand.NOISE -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_noise
        AudioBrand.BOULT -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_boult
        AudioBrand.NOTHING -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_nothing
        AudioBrand.XIAOMI -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_xiaomi
        AudioBrand.GOOGLE -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_brand_google
        AudioBrand.GENERIC_BLUETOOTH -> dev.roxy.kryptonplayer.core.ui.R.drawable.ic_audio_earbuds
    }
}

data class AudioOutputDevice(
    val id: String,
    val name: String,
    val type: AudioDeviceType,
    val isSelected: Boolean,
    val rawDeviceInfo: AudioDeviceInfo? = null,
    val brand: AudioBrand = detectAudioBrand(name)
)

@Composable
fun rememberAudioOutputState(): AudioOutputState {
    val context = LocalContext.current
    val state = remember(context) { AudioOutputState(context) }
    DisposableEffect(state) {
        state.startListening()
        onDispose {
            state.stopListening()
        }
    }
    return state
}

@Stable
class AudioOutputState(private val context: Context) {
    private val audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)!!

    var availableDevices by mutableStateOf<List<AudioOutputDevice>>(emptyList())
        private set

    var currentDevice by mutableStateOf(
        AudioOutputDevice(
            id = "speaker",
            name = "Phone Speaker",
            type = AudioDeviceType.SPEAKER,
            isSelected = true
        )
    )
        private set

    private var isManualSpeakerOverride by mutableStateOf(false)

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshDevices()
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshDevices()
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshDevices()
        }
    }

    fun startListening() {
        refreshDevices()
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, Handler(Looper.getMainLooper()))
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        }
        runCatching {
            ContextCompat.registerReceiver(
                context,
                broadcastReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }

    fun stopListening() {
        runCatching {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        }
        runCatching {
            context.unregisterReceiver(broadcastReceiver)
        }
    }

    fun selectDevice(device: AudioOutputDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (device.type == AudioDeviceType.SPEAKER) {
                isManualSpeakerOverride = true
                val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                if (speakerDevice != null) {
                    audioManager.setCommunicationDevice(speakerDevice)
                } else {
                    audioManager.clearCommunicationDevice()
                }
            } else if (device.rawDeviceInfo != null) {
                isManualSpeakerOverride = false
                val target = audioManager.availableCommunicationDevices.firstOrNull {
                    it.id == device.rawDeviceInfo.id || it.type == device.rawDeviceInfo.type
                }
                if (target != null) {
                    audioManager.setCommunicationDevice(target)
                } else {
                    audioManager.clearCommunicationDevice()
                }
            } else {
                isManualSpeakerOverride = false
                audioManager.clearCommunicationDevice()
            }
        } else {
            if (device.type == AudioDeviceType.SPEAKER) {
                isManualSpeakerOverride = true
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
            } else {
                isManualSpeakerOverride = false
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
            }
        }
        refreshDevices()
    }

    fun openSystemMediaOutput() {
        try {
            val intent = Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG").apply {
                setPackage("com.android.systemui")
                putExtra("package_name", context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    fun refreshDevices() {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val devicesList = mutableListOf<AudioOutputDevice>()

        val btDevices = outputs.filter {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && it.type == AudioDeviceInfo.TYPE_HEARING_AID)
        }

        val wiredDevices = outputs.filter {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        }
        val usbDevices = outputs.filter {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.type == AudioDeviceInfo.TYPE_USB_HEADSET)
        }

        val speakerInfo = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }

        val activeType: AudioDeviceType = when {
            isManualSpeakerOverride -> AudioDeviceType.SPEAKER
            btDevices.isNotEmpty() -> AudioDeviceType.EARBUDS_BLUETOOTH
            wiredDevices.isNotEmpty() -> AudioDeviceType.HEADPHONES_WIRED
            usbDevices.isNotEmpty() -> AudioDeviceType.USB
            else -> AudioDeviceType.SPEAKER
        }

        btDevices.forEach { dev ->
            val name = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth Device"
            devicesList.add(
                AudioOutputDevice(
                    id = "bt_${dev.id}",
                    name = name,
                    type = AudioDeviceType.EARBUDS_BLUETOOTH,
                    isSelected = activeType == AudioDeviceType.EARBUDS_BLUETOOTH,
                    rawDeviceInfo = dev
                )
            )
        }

        wiredDevices.forEach { dev ->
            val name = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Wired Headphones"
            devicesList.add(
                AudioOutputDevice(
                    id = "wired_${dev.id}",
                    name = name,
                    type = AudioDeviceType.HEADPHONES_WIRED,
                    isSelected = activeType == AudioDeviceType.HEADPHONES_WIRED,
                    rawDeviceInfo = dev
                )
            )
        }

        usbDevices.forEach { dev ->
            val name = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: "USB Audio"
            devicesList.add(
                AudioOutputDevice(
                    id = "usb_${dev.id}",
                    name = name,
                    type = AudioDeviceType.USB,
                    isSelected = activeType == AudioDeviceType.USB,
                    rawDeviceInfo = dev
                )
            )
        }

        val isSpeakerActive = activeType == AudioDeviceType.SPEAKER
        val speakerDevice = AudioOutputDevice(
            id = "speaker",
            name = "Phone Speaker",
            type = AudioDeviceType.SPEAKER,
            isSelected = isSpeakerActive,
            rawDeviceInfo = speakerInfo
        )
        devicesList.add(speakerDevice)

        availableDevices = devicesList
        currentDevice = devicesList.firstOrNull { it.isSelected } ?: speakerDevice
    }
}
