package de.loxitation.pht.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Connection lifecycle state surfaced to the UI layer. */
sealed interface BleConnectionState {
    data object Idle : BleConnectionState
    data object Scanning : BleConnectionState
    data class Connecting(val deviceName: String?) : BleConnectionState
    data object Connected : BleConnectionState
    data class MeasurementReceived(val measurement: ParsedMeasurement) : BleConnectionState
    data class Error(val message: String) : BleConnectionState
}

/**
 * Scans for and connects to a single [DeviceProfile] at a time, enables indications/notifications
 * on its measurement characteristic, and surfaces decoded measurements via [state].
 *
 * Callers are responsible for verifying BLUETOOTH_SCAN/BLUETOOTH_CONNECT (API 31+) or
 * BLUETOOTH/BLUETOOTH_ADMIN/ACCESS_FINE_LOCATION (API 26-30) permissions before calling
 * [startScan]; the BLE calls below assume permissions are already granted.
 */
@SuppressLint("MissingPermission")
class BleConnectionManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter get() = bluetoothManager?.adapter

    private var gatt: BluetoothGatt? = null
    private var activeProfile: DeviceProfile? = null
    private var isScanning = false

    private val _state = MutableStateFlow<BleConnectionState>(BleConnectionState.Idle)
    val state: StateFlow<BleConnectionState> = _state.asStateFlow()

    fun startScan(profile: DeviceProfile) {
        val bleScanner = adapter?.bluetoothLeScanner
        if (bleScanner == null) {
            _state.value = BleConnectionState.Error("Bluetooth ist nicht verfügbar oder deaktiviert")
            return
        }

        activeProfile = profile
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(profile.serviceUuid))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        isScanning = true
        _state.value = BleConnectionState.Scanning
        bleScanner.startScan(listOf(filter), settings, scanCallback)
    }

    fun stopScan() {
        if (!isScanning) return
        isScanning = false
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        if (_state.value is BleConnectionState.Scanning) {
            _state.value = BleConnectionState.Idle
        }
    }

    fun disconnect() {
        cleanupGatt()
        activeProfile = null
        _state.value = BleConnectionState.Idle
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            stopScan()
            connect(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _state.value = BleConnectionState.Error("BLE-Scan fehlgeschlagen (Code $errorCode)")
        }
    }

    private fun connect(device: BluetoothDevice) {
        _state.value = BleConnectionState.Connecting(runCatching { device.name }.getOrNull())
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> gatt.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> {
                    cleanupGatt()
                    if (_state.value !is BleConnectionState.MeasurementReceived) {
                        _state.value = BleConnectionState.Idle
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val profile = activeProfile
            if (profile == null) {
                _state.value = BleConnectionState.Error("Kein aktives Geräteprofil")
                return
            }

            val characteristic = gatt.getService(profile.serviceUuid)
                ?.getCharacteristic(profile.measurementCharacteristicUuid)
            if (characteristic == null) {
                _state.value = BleConnectionState.Error("Erwartete Charakteristik nicht gefunden")
                return
            }

            gatt.setCharacteristicNotification(characteristic, true)
            enableClientConfigDescriptor(gatt, characteristic, profile.usesIndications)
            _state.value = BleConnectionState.Connected
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            // API 33+ path: value is delivered as a parameter.
            handleCharacteristicValue(value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            // Pre-API 33 path: the platform doesn't call the 3-arg overload above, so we read
            // the value from the characteristic itself here.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                handleCharacteristicValue(characteristic.value ?: return)
            }
        }
    }

    private fun enableClientConfigDescriptor(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        useIndications: Boolean,
    ) {
        val descriptor = characteristic.getDescriptor(BluetoothSigUuids.CLIENT_CHARACTERISTIC_CONFIG)
            ?: return
        val value = if (useIndications) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun handleCharacteristicValue(value: ByteArray) {
        val profile = activeProfile ?: return
        when (val parsed = profile.parse(value)) {
            null -> _state.value = BleConnectionState.Error("Messwert konnte nicht dekodiert werden")
            else -> _state.value = BleConnectionState.MeasurementReceived(parsed)
        }
    }

    private fun cleanupGatt() {
        gatt?.close()
        gatt = null
    }
}
