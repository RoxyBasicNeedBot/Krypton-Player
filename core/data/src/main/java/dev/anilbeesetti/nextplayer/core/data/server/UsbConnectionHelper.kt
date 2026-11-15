package dev.anilbeesetti.nextplayer.core.data.server

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbConnectionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DEFAULT_PORT = 8080
    }

    enum class ConnectionType {
        USB_TETHER,
        WIFI_LAN,
        HOTSPOT,
        ADB_FORWARD,
        OTHER
    }

    data class UsbIpInfo(
        val ipAddress: String,
        val interfaceName: String,
        val isUsbTethered: Boolean,
        val connectionType: ConnectionType = ConnectionType.OTHER,
        val typeLabel: String = if (isUsbTethered) "USB Cable" else "Network",
        val browserUrl: String,
        val vlcUrl: String
    )

    /**
     * Finds the best IP addresses for USB / LAN streaming.
     * Prioritizes USB tethering interfaces (rndis, ncm, usb, tether), then Wi-Fi/LAN, then Hotspot, then ADB localhost.
     */
    fun getAvailableStreamingUrls(port: Int = DEFAULT_PORT): List<UsbIpInfo> {
        val result = mutableListOf<UsbIpInfo>()

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val name = intf.name.lowercase()
                val addresses = intf.inetAddresses

                for (addr in addresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("127.")) continue

                        val (type, isUsb, label) = classifyInterface(name, host)

                        result.add(
                            UsbIpInfo(
                                ipAddress = host,
                                interfaceName = intf.displayName ?: intf.name,
                                isUsbTethered = isUsb,
                                connectionType = type,
                                typeLabel = label,
                                browserUrl = "http://$host:$port",
                                vlcUrl = "http://$host:$port/stream/video"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Add ADB localhost port-forwarding fallback as lowest priority
        result.add(
            UsbIpInfo(
                ipAddress = "127.0.0.1",
                interfaceName = "ADB Port Forward (localhost)",
                isUsbTethered = false,
                connectionType = ConnectionType.ADB_FORWARD,
                typeLabel = "ADB Localhost",
                browserUrl = "http://localhost:$port",
                vlcUrl = "http://localhost:$port/stream/video"
            )
        )

        // Sort priority:
        // 1. USB_TETHER
        // 2. WIFI_LAN
        // 3. HOTSPOT
        // 4. OTHER
        // 5. ADB_FORWARD
        return result.sortedWith(
            compareBy<UsbIpInfo> {
                when (it.connectionType) {
                    ConnectionType.USB_TETHER -> 0
                    ConnectionType.WIFI_LAN -> 1
                    ConnectionType.HOTSPOT -> 2
                    ConnectionType.OTHER -> 3
                    ConnectionType.ADB_FORWARD -> 4
                }
            }.thenBy { it.ipAddress }
        )
    }

    private fun classifyInterface(name: String, host: String): Triple<ConnectionType, Boolean, String> {
        return when {
            // USB Tethering: rndis, ncm, usb interfaces or standard 192.168.42.x
            name.contains("rndis") || name.contains("ncm") || name.contains("usb") -> {
                Triple(ConnectionType.USB_TETHER, true, "USB Tethering")
            }
            host.startsWith("192.168.42.") -> {
                Triple(ConnectionType.USB_TETHER, true, "USB Tethering")
            }
            // Hotspot / SoftAP
            name.contains("ap") || name.contains("softap") || host.startsWith("192.168.43.") -> {
                Triple(ConnectionType.HOTSPOT, false, "Mobile Hotspot")
            }
            // Wi-Fi / Ethernet LAN
            name.contains("wlan") || name.contains("eth") || name.contains("en") -> {
                Triple(ConnectionType.WIFI_LAN, false, "Wi-Fi / LAN")
            }
            // Cellular fallback or other private networks
            name.contains("ccmni") || name.contains("rmnet") || name.contains("pdp") -> {
                Triple(ConnectionType.OTHER, false, "Mobile Data")
            }
            else -> {
                Triple(ConnectionType.OTHER, false, "Network Interface")
            }
        }
    }
}
