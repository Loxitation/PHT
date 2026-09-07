package de.loxitation.pht.ble

import java.util.UUID

/**
 * Abstraction for a supported BLE health device. Each device type (blood pressure monitor,
 * scale, wearable, ...) implements this interface so the BLE connection layer and the UI stay
 * generic while device-specific parsing lives in one place per device.
 *
 * Adding a new device later means adding a new [DeviceProfile] implementation and registering it
 * in [DeviceProfileRegistry] - no changes needed to [BleConnectionManager].
 */
interface DeviceProfile {
    /** Stable identifier stored server-side as `devices.device_type`, e.g. "beurer_bm48". */
    val deviceType: String

    /** Human-readable name shown in the UI, e.g. "Beurer BM48". */
    val displayName: String

    /** Primary GATT service this device profile listens to. */
    val serviceUuid: UUID

    /** Characteristic within [serviceUuid] that delivers measurements via indicate/notify. */
    val measurementCharacteristicUuid: UUID

    /** Whether the characteristic uses indications (with ack) rather than plain notifications. */
    val usesIndications: Boolean

    /** Parses a raw characteristic payload into a typed measurement, or null if unparseable. */
    fun parse(rawBytes: ByteArray): ParsedMeasurement?
}

/** Sealed result of parsing a device's raw BLE payload into a typed, server-schema-shaped value. */
sealed interface ParsedMeasurement {
    val rawHex: String

    data class BloodPressure(
        val systolic: Float,
        val diastolic: Float,
        val meanArterialPressure: Float?,
        val pulse: Int?,
        val timestamp: java.time.LocalDateTime?,
        val measurementStatusRaw: Int?,
        val isKPa: Boolean,
        override val rawHex: String,
    ) : ParsedMeasurement
}

/** Well-known Bluetooth SIG 16-bit UUIDs, expanded to the full 128-bit base UUID form. */
object BluetoothSigUuids {
    fun from16Bit(shortUuid: Long): UUID =
        UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", shortUuid))

    val BLOOD_PRESSURE_SERVICE: UUID = from16Bit(0x1810)
    val BLOOD_PRESSURE_MEASUREMENT: UUID = from16Bit(0x2A35)
    val CLIENT_CHARACTERISTIC_CONFIG: UUID = from16Bit(0x2902)
}
