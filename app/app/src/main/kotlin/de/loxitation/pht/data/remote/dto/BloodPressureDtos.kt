package de.loxitation.pht.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/v1/measurements/blood-pressure`. Field names and aliases mirror
 * server/app/schemas/measurement.py::BloodPressureMeasurementIn exactly.
 */
@Serializable
data class BloodPressureMeasurementRequest(
    @SerialName("measurementUuid") val measurementUuid: String,
    @SerialName("deviceId") val deviceId: String? = null,
    @SerialName("systolic") val systolic: Float,
    @SerialName("diastolic") val diastolic: Float,
    @SerialName("pulse") val pulse: Int? = null,
    @SerialName("time") val time: String,
    @SerialName("zoneOffset") val zoneOffsetSeconds: Int? = null,
    @SerialName("bodyPosition") val bodyPosition: String? = null,
    @SerialName("measurementLocation") val measurementLocation: String? = null,
    @SerialName("measurementStatusRaw") val measurementStatusRaw: Int? = null,
    @SerialName("rawPayload") val rawPayload: Map<String, String> = emptyMap(),
)

/** Response shape for both the create call and its idempotent-duplicate replay. */
@Serializable
data class BloodPressureMeasurementResponse(
    @SerialName("id") val id: String,
    @SerialName("measurement_type") val measurementType: String,
    @SerialName("measurement_uuid") val measurementUuid: String,
    @SerialName("recorded_at") val recordedAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("systolic") val systolic: Float? = null,
    @SerialName("diastolic") val diastolic: Float? = null,
    @SerialName("pulse") val pulse: Int? = null,
)
