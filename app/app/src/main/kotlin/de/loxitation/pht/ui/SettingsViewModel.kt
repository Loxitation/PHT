package de.loxitation.pht.ui

import androidx.lifecycle.ViewModel
import de.loxitation.pht.data.repository.PhtSettings
import de.loxitation.pht.data.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<PhtSettings> = repository.settings

    fun save(serverUrl: String, apiKey: String, deviceId: String) {
        repository.save(serverUrl, apiKey, deviceId)
    }
}
