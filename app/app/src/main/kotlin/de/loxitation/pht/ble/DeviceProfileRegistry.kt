package de.loxitation.pht.ble

/**
 * Central place to register supported [DeviceProfile]s. Adding support for a new device type
 * (scale, wearable, ...) means creating a new [DeviceProfile] implementation and adding it here.
 */
object DeviceProfileRegistry {
    private val profiles: List<DeviceProfile> = listOf(
        BeurerBm48Profile(),
    )

    fun all(): List<DeviceProfile> = profiles

    fun byDeviceType(deviceType: String): DeviceProfile? =
        profiles.firstOrNull { it.deviceType == deviceType }

    fun byServiceUuid(serviceUuid: java.util.UUID): DeviceProfile? =
        profiles.firstOrNull { it.serviceUuid == serviceUuid }
}
