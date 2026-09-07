package de.loxitation.pht.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Schedules [SyncWorker] runs: an immediate (expedited-effort) attempt right after a new
 * measurement is queued, plus a periodic safety-net run so nothing gets stuck if the immediate
 * attempt fails while the app is closed.
 */
object SyncScheduler {
    private const val UNIQUE_IMMEDIATE_WORK = "pht_immediate_sync"
    private const val UNIQUE_PERIODIC_WORK = "pht_periodic_sync"
    private val PERIODIC_INTERVAL = 30L to TimeUnit.MINUTES

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueueImmediateSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_IMMEDIATE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun enqueuePeriodicSync(context: Context) {
        val (interval, unit) = PERIODIC_INTERVAL
        val request = PeriodicWorkRequestBuilder<SyncWorker>(interval, unit)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
