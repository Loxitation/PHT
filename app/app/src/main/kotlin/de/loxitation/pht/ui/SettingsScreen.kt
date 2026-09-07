package de.loxitation.pht.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.loxitation.pht.R

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()

    var serverUrl by remember(settings) { mutableStateOf(settings.serverUrl) }
    var apiKey by remember(settings) { mutableStateOf(settings.apiKey) }
    var deviceId by remember(settings) { mutableStateOf(settings.deviceId) }
    var savedJustNow by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            text = stringResource(R.string.settings_hint),
            style = MaterialTheme.typography.bodyMedium,
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it; savedJustNow = false },
            label = { Text(stringResource(R.string.settings_server_url)) },
            placeholder = { Text("https://pht.loxitation.de") },
            modifier = Modifier.fillMaxWidth(),
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; savedJustNow = false },
            label = { Text(stringResource(R.string.settings_api_key)) },
            modifier = Modifier.fillMaxWidth(),
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))

        OutlinedTextField(
            value = deviceId,
            onValueChange = { deviceId = it; savedJustNow = false },
            label = { Text(stringResource(R.string.settings_device_id)) },
            modifier = Modifier.fillMaxWidth(),
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(12.dp))

        Button(onClick = {
            viewModel.save(serverUrl, apiKey, deviceId)
            savedJustNow = true
        }) {
            Text(stringResource(R.string.settings_save))
        }

        if (savedJustNow) {
            Text(
                text = stringResource(R.string.settings_saved),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
