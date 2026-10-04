/** Field names mirror server/app/schemas/measurement.py (BloodPressureMeasurementOut). */
export interface BloodPressureMeasurement {
  id: string
  measurement_type: string
  measurement_uuid: string
  recorded_at: string
  created_at: string
  device_id: string | null
  raw_payload: Record<string, unknown>
  systolic: number | null
  diastolic: number | null
  pulse: number | null
  bodyPosition: string | null
  measurementLocation: string | null
  measurementStatusRaw: number | null
}
