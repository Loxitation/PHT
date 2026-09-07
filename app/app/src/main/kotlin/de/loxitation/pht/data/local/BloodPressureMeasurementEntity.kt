package de.loxitation.pht.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Sync lifecycle of a locally stored measurement waiting to reach the PHT server. */
enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED,
}

/**
 * Local offline queue entry for a blood pressure measurement. Mirrors the server's
 * `POST /api/v1/measurements/blood-pressure` payload shape (see server/app/schemas/measurement.py)
 * so mapping to the network DTO is a straight field-for-field translation.
 *
 * [measurementUuid] is the primary key: it is generated once on-device and used by the server for
 * idempotent retries, so re-sending after a failed sync never creates a duplicate.
 */
@Entity(tableName = "blood_pressure_measurements")
data class BloodPressureMeasurementEntity(
    @PrimaryKey val measurementUuid: String,
    val deviceId: String?,
    val systolic: Float,
    val diastolic: Float,
    val pulse: Int?,
    val recordedAtEpochSeconds: Long,
    val zoneOffsetSeconds: Int,
    val bodyPosition: String? = null,
    val measurementLocation: String? = null,
    val measurementStatusRaw: Int? = null,
    val rawHex: String,
    val createdAtEpochMillis: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
)
