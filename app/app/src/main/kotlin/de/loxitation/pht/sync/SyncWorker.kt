package de.loxitation.pht.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.loxitation.pht.PhtApplication
import de.loxitation.pht.data.remote.NetworkModule

/**
 * Pushes every locally queued (PENDING or previously FAILED) measurement to the server.
 * Idempotency is guaranteed server-side via `measurement_uuid`, so re-sending after a retry
 * never creates duplicates (see server/app/api/v1/measurements.py).
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PhtApplication
        val settings = app.settingsRepository.settings.value
        if (!settings.isConfigured) {
            // Nothing configured yet (no server URL / API key / device id) - not an error,
            // just nothing to do until the user finishes Settings.
            return Result.success()
        }

        val pending = app.measurementRepository.getPending()
        if (pending.isEmpty()) return Result.success()

        val api = NetworkModule.createApiService(settings.serverUrl)
        var anyFailure = false

        for (entity in pending) {
            try {
                val response = api.postBloodPressureMeasurement(
                    apiKey = settings.apiKey,
                    body = app.measurementRepository.toRequestDto(entity),
                )
                if (response.isSuccessful) {
                    app.measurementRepository.markSynced(entity.measurementUuid)
                } else {
                    anyFailure = true
                    app.measurementRepository.markFailed(entity.measurementUuid, "HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                anyFailure = true
                app.measurementRepository.markFailed(entity.measurementUuid, e.message ?: "Unbekannter Fehler")
            }
        }

        // WorkManager applies the exponential backoff configured in SyncScheduler before the
        // next attempt when Result.retry() is returned.
        return if (anyFailure) Result.retry() else Result.success()
    }
}
