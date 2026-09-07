package de.loxitation.pht.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.loxitation.pht.ble.BeurerBm48Profile
import de.loxitation.pht.ble.BleConnectionManager
import de.loxitation.pht.ble.BleConnectionState
import de.loxitation.pht.ble.DeviceProfile
import de.loxitation.pht.ble.ParsedMeasurement
import de.loxitation.pht.data.repository.MeasurementRepository
import de.loxitation.pht.data.repository.SettingsRepository
import de.loxitation.pht.sync.SyncScheduler
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeviceViewModel(
    private val appContext: Context,
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val bleConnectionManager = BleConnectionManager(appContext)
    val connectionState: StateFlow<BleConnectionState> = bleConnectionManager.state

    init {
        viewModelScope.launch {
            bleConnectionManager.state.collect { state ->
                if (state is BleConnectionState.MeasurementReceived) {
                    val measurement = state.measurement
                    if (measurement is ParsedMeasurement.BloodPressure) {
                        val deviceId = settingsRepository.settings.value.deviceId.ifBlank { null }
                        measurementRepository.saveFromBle(measurement, deviceId)
                        SyncScheduler.enqueueImmediateSync(appContext)
                    }
                    bleConnectionManager.disconnect()
                }
            }
        }
    }

    fun startScan(profile: DeviceProfile = BeurerBm48Profile()) = bleConnectionManager.startScan(profile)

    fun stopScan() = bleConnectionManager.stopScan()

    override fun onCleared() {
        bleConnectionManager.disconnect()
        super.onCleared()
    }
}
