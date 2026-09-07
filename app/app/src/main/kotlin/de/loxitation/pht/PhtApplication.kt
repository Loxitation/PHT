package de.loxitation.pht

import android.app.Application
import de.loxitation.pht.data.local.AppDatabase
import de.loxitation.pht.data.repository.MeasurementRepository
import de.loxitation.pht.data.repository.SettingsRepository
import de.loxitation.pht.sync.SyncScheduler

/**
 * Minimal hand-rolled dependency container (no DI framework) exposing the app's singletons.
 * Kept intentionally simple for this phase; swap for Hilt/Koin later if the graph grows.
 */
class PhtApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val measurementRepository: MeasurementRepository by lazy {
        MeasurementRepository(database.bloodPressureMeasurementDao())
    }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        SyncScheduler.enqueuePeriodicSync(this)
    }
}
