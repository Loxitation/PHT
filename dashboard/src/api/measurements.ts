import { apiClient } from './client'
import type { BloodPressureMeasurement } from '../types/measurement'

export async function fetchBloodPressureMeasurements(
  limit = 200,
): Promise<BloodPressureMeasurement[]> {
  const response = await apiClient.get<BloodPressureMeasurement[]>('/api/v1/measurements', {
    params: { measurement_type: 'blood_pressure', limit },
  })
  return response.data
}
