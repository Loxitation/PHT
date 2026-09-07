package de.loxitation.pht.ble

/**
 * Beurer BM48 blood pressure monitor. It implements the standard Bluetooth SIG Blood Pressure
 * Service/Characteristic, so this profile is a thin wrapper around [BloodPressureMeasurementParser].
 * Any other BLE blood pressure monitor conforming to the same SIG profile would work identically.
 */
class BeurerBm48Profile : DeviceProfile {
    override val deviceType: String = "beurer_bm48"
    override val displayName: String = "Beurer BM48"
    override val serviceUuid = BluetoothSigUuids.BLOOD_PRESSURE_SERVICE
    override val measurementCharacteristicUuid = BluetoothSigUuids.BLOOD_PRESSURE_MEASUREMENT
    override val usesIndications = true

    override fun parse(rawBytes: ByteArray): ParsedMeasurement? =
        BloodPressureMeasurementParser.parse(rawBytes)
}
