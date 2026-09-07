package de.loxitation.pht.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodPressureMeasurementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: BloodPressureMeasurementEntity)

    @Query("SELECT * FROM blood_pressure_measurements ORDER BY recordedAtEpochSeconds DESC")
    fun observeAll(): Flow<List<BloodPressureMeasurementEntity>>

    @Query("SELECT * FROM blood_pressure_measurements WHERE syncStatus != 'SYNCED' ORDER BY createdAtEpochMillis ASC")
    suspend fun getPending(): List<BloodPressureMeasurementEntity>

    @Query(
        "UPDATE blood_pressure_measurements SET syncStatus = 'SYNCED', lastSyncError = NULL " +
            "WHERE measurementUuid = :measurementUuid",
    )
    suspend fun markSynced(measurementUuid: String)

    @Query(
        "UPDATE blood_pressure_measurements SET syncStatus = 'FAILED', " +
            "syncAttempts = syncAttempts + 1, lastSyncError = :error WHERE measurementUuid = :measurementUuid",
    )
    suspend fun markFailed(measurementUuid: String, error: String)
}
