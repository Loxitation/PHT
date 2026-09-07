package de.loxitation.pht.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PhtSettings(
    val serverUrl: String = "",
    val apiKey: String = "",
    val deviceId: String = "",
) {
    val isConfigured: Boolean get() = serverUrl.isNotBlank() && apiKey.isNotBlank() && deviceId.isNotBlank()
}

/**
 * Stores the server URL, per-installation API key, and device id in an
 * [EncryptedSharedPreferences] file backed by the Android Keystore, since the API key is a
 * revocable-but-sensitive credential (see server/app/core/api_key.py).
 *
 * NOTE: androidx.security:security-crypto 1.1.0 deprecated EncryptedSharedPreferences/MasterKey
 * in favor of a forthcoming Jetpack crypto API without a stable 1:1 replacement yet; the classes
 * remain fully functional. Revisit once the replacement API is stable.
 */
class SettingsRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "pht_secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PhtSettings> = _settings.asStateFlow()

    fun save(serverUrl: String, apiKey: String, deviceId: String) {
        prefs.edit()
            .putString(KEY_SERVER_URL, serverUrl.trim())
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_DEVICE_ID, deviceId.trim())
            .apply()
        _settings.value = loadSettings()
    }

    private fun loadSettings(): PhtSettings = PhtSettings(
        serverUrl = prefs.getString(KEY_SERVER_URL, "") ?: "",
        apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
        deviceId = prefs.getString(KEY_DEVICE_ID, "") ?: "",
    )

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
