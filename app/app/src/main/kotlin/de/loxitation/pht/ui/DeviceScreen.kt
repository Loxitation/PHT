package de.loxitation.pht.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.loxitation.pht.R
import de.loxitation.pht.ble.BlePermissions
import de.loxitation.pht.ble.BleConnectionState

@Composable
fun DeviceScreen(viewModel: DeviceViewModel) {
    val context = LocalContext.current
    val state by viewModel.connectionState.collectAsState()
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.all { it }) {
            permissionDenied = false
            viewModel.startScan()
        } else {
            permissionDenied = true
        }
    }

    fun hasAllPermissions(): Boolean = BlePermissions.required().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = statusText(state), style = MaterialTheme.typography.titleMedium)

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(12.dp))

        if (permissionDenied) {
            Text(
                text = stringResource(R.string.device_permission_required),
                style = MaterialTheme.typography.bodyMedium,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
        }

        val isScanning = state is BleConnectionState.Scanning || state is BleConnectionState.Connecting
        Button(onClick = {
            if (isScanning) {
                viewModel.stopScan()
            } else if (hasAllPermissions()) {
                viewModel.startScan()
            } else {
                permissionLauncher.launch(BlePermissions.required())
            }
        }) {
            Text(
                if (isScanning) {
                    stringResource(R.string.device_scan_stop)
                } else {
                    stringResource(R.string.device_scan_start)
                },
            )
        }
    }
}

@Composable
private fun statusText(state: BleConnectionState): String = when (state) {
    is BleConnectionState.Idle -> stringResource(R.string.device_status_idle)
    is BleConnectionState.Scanning -> stringResource(R.string.device_status_scanning)
    is BleConnectionState.Connecting -> stringResource(R.string.device_status_connecting)
    is BleConnectionState.Connected -> stringResource(R.string.device_status_connected)
    is BleConnectionState.MeasurementReceived -> stringResource(R.string.device_status_measurement_received)
    is BleConnectionState.Error -> stringResource(R.string.device_status_error, state.message)
}
