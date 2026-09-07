package de.loxitation.pht.data.remote

import de.loxitation.pht.data.remote.dto.BloodPressureMeasurementRequest
import de.loxitation.pht.data.remote.dto.BloodPressureMeasurementResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface PhtApiService {
    @POST("api/v1/measurements/blood-pressure")
    suspend fun postBloodPressureMeasurement(
        @Header("X-API-Key") apiKey: String,
        @Body body: BloodPressureMeasurementRequest,
    ): Response<BloodPressureMeasurementResponse>
}
