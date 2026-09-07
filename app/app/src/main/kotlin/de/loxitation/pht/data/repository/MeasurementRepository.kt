package de.loxitation.pht.data.repository

import de.loxitation.pht.ble.ParsedMeasurement
import de.loxitation.pht.data.local.BloodPressureMeasurementDao
import de.loxitation.pht.data.local.BloodPressureMeasurementEntity
import de.loxitation.pht.data.local.SyncStatus
import de.loxitation.pht.data.remote.dto.BloodPressureMeasurementRequest
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Bridges the BLE layer, the local Room offline queue, and the network DTOs. A measurement
 * received over BLE is always written to Room first (durable, survives process death / no
 * network) and only sent to the server afterwards via [de.loxitation.pht.sync.SyncWorker].
 */
class MeasurementRepository(private val dao: BloodPressureMeasurementDao) {

    fun observeAll(): Flow<List<BloodPressureMeasurementEntity>> = dao.observeAll()

    suspend fun getPending(): List<BloodPressureMeasurementEntity> = dao.getPending()

    suspend fun markSynced(measurementUuid: String) = dao.markSynced(measurementUuid)

    suspend fun markFailed(measurementUuid: String, error: String) =
        dao.markFailed(measurementUuid, error)

    /** Persists a freshly decoded BLE measurement to the local offline queue. */
    suspend fun saveFromBle(
        measurement: ParsedMeasurement.BloodPressure,
        deviceId: String?,
    ): BloodPressureMeasurementEntity {
        val zoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())
        val recordedAtEpochSeconds = measurement.timestamp
            ?.atOffset(zoneOffset)
            ?.toEpochSecond()
            ?: Instant.now().epochSecond

        val entity = BloodPressureMeasurementEntity(
            measurementUuid = UUID.randomUUID().toString(),
            deviceId = deviceId,
            systolic = measurement.systolic,
            diastolic = measurement.diastolic,
            pulse = measurement.pulse,
            recordedAtEpochSeconds = recordedAtEpochSeconds,
            zoneOffsetSeconds = zoneOffset.totalSeconds,
            measurementStatusRaw = measurement.measurementStatusRaw,
            rawHex = measurement.rawHex,
            createdAtEpochMillis = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING,
        )
        dao.insert(entity)
        return entity
    }

    /** Maps a queued entity to the server's request DTO (see server BloodPressureMeasurementIn). */
    fun toRequestDto(entity: BloodPressureMeasurementEntity): BloodPressureMeasurementRequest {
        val time = OffsetDateTime.ofInstant(
            Instant.ofEpochSecond(entity.recordedAtEpochSeconds),
            ZoneOffset.ofTotalSeconds(entity.zoneOffsetSeconds),
        ).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        return BloodPressureMeasurementRequest(
            measurementUuid = entity.measurementUuid,
            deviceId = entity.deviceId,
            systolic = entity.systolic,
            diastolic = entity.diastolic,
            pulse = entity.pulse,
            time = time,
            zoneOffsetSeconds = entity.zoneOffsetSeconds,
            bodyPosition = entity.bodyPosition,
            measurementLocation = entity.measurementLocation,
            measurementStatusRaw = entity.measurementStatusRaw,
            rawPayload = mapOf("rawHex" to entity.rawHex),
        )
    }
}
