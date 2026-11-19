package br.com.smarttarget.android.airdeck.hid

enum class HostDeviceType {
    COMPUTER,
    TV_OR_MEDIA,
    PHONE_OR_TABLET,
    OTHER
}

enum class DeviceBondState {
    NONE,
    BONDING,
    BONDED
}

data class BondedHostDevice(
    val name: String,
    val address: String,
    val type: HostDeviceType
)

data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val type: HostDeviceType,
    val bondState: DeviceBondState,
    val rssi: Int? = null
)

data class DiscoveryState(
    val isDiscovering: Boolean = false,
    val discoveredDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val pairingDeviceAddress: String? = null,
    val discoveryError: String? = null
)

sealed interface HidConnectionState {
    data class Unsupported(val reason: String) : HidConnectionState
    data class PermissionRequired(val missingPermissions: List<String>) : HidConnectionState
    data object BluetoothDisabled : HidConnectionState
    data object Registering : HidConnectionState
    data class Disconnected(
        val bondedDevices: List<BondedHostDevice>,
        val isDiscoverable: Boolean,
        val wasUserInitiated: Boolean = false
    ) : HidConnectionState
    data class Connecting(val device: BondedHostDevice) : HidConnectionState
    data class Connected(val device: BondedHostDevice) : HidConnectionState
    data class Error(val message: String, val bondedDevices: List<BondedHostDevice> = emptyList()) :
        HidConnectionState
}

data class HidPacketStats(
    val totalReportsSent: Long = 0L,
    val lastReportLabel: String = "Pronto",
    val lastBytesHex: String = "--",
    val wasDeliveredToHost: Boolean = false
)
