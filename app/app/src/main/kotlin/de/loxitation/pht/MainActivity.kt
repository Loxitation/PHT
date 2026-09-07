package de.loxitation.pht

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.loxitation.pht.ui.DeviceScreen
import de.loxitation.pht.ui.DeviceViewModel
import de.loxitation.pht.ui.MeasurementsScreen
import de.loxitation.pht.ui.MeasurementsViewModel
import de.loxitation.pht.ui.SettingsScreen
import de.loxitation.pht.ui.SettingsViewModel
import de.loxitation.pht.ui.theme.PhtTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhtTheme {
                PhtApp()
            }
        }
    }
}

private enum class PhtTab(val labelRes: Int) {
    MEASUREMENTS(R.string.nav_measurements),
    DEVICE(R.string.nav_device),
    SETTINGS(R.string.nav_settings),
}

@Composable
private fun PhtApp() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as PhtApplication
    var selectedTab by remember { mutableIntStateOf(0) }

    val measurementsViewModel: MeasurementsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { MeasurementsViewModel(app.measurementRepository) }
        },
    )
    val deviceViewModel: DeviceViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DeviceViewModel(app, app.measurementRepository, app.settingsRepository) }
        },
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(app.settingsRepository) }
        },
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(PhtTab.MEASUREMENTS.labelRes)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Bluetooth, contentDescription = null) },
                    label = { Text(stringResource(PhtTab.DEVICE.labelRes)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(PhtTab.SETTINGS.labelRes)) },
                )
            }
        },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> MeasurementsScreen(measurementsViewModel)
                1 -> DeviceScreen(deviceViewModel)
                else -> SettingsScreen(settingsViewModel)
            }
        }
    }
}
