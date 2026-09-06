"""Pydantic schemas for measurement endpoints."""
from datetime import datetime
import uuid

from pydantic import BaseModel, ConfigDict, Field, field_validator


class BloodPressureMeasurementIn(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    """Payload sent by the Android app after decoding a Bluetooth SIG Blood Pressure
    Measurement characteristic. Field names mirror Health Connect's BloodPressureRecord.
    """

    measurement_uuid: uuid.UUID = Field(
        ...,
        description="Client-generated UUID for idempotency",
        alias="measurementUuid",
    )
    device_id: uuid.UUID | None = Field(None, alias="deviceId")

    systolic: float = Field(..., gt=0, description="mmHg")
    diastolic: float = Field(..., gt=0, description="mmHg")
    pulse: int | None = Field(None, ge=0, le=300, description="bpm")

    time: datetime = Field(..., description="Timestamp the reading was recorded (with offset)")
    zone_offset_seconds: int | None = Field(
        None,
        description="UTC offset in seconds at recording time",
        alias="zoneOffset",
    )

    body_position: str | None = Field(None, alias="bodyPosition")
    measurement_location: str | None = Field(None, alias="measurementLocation")
    measurement_status_raw: int | None = Field(None, alias="measurementStatusRaw")

    raw_payload: dict = Field(default_factory=dict, description="Full decoded payload for traceability", alias="rawPayload")

    @field_validator("time")
    @classmethod
    def ensure_timezone_aware(cls, value: datetime) -> datetime:
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("time must include a timezone offset")
        return value


class MeasurementOut(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: uuid.UUID
    measurement_type: str
    measurement_uuid: uuid.UUID
    recorded_at: datetime
    created_at: datetime
    device_id: uuid.UUID | None
    raw_payload: dict


class BloodPressureMeasurementOut(MeasurementOut):
    systolic: float | None = None
    diastolic: float | None = None
    pulse: int | None = None
    body_position: str | None = Field(None, alias="bodyPosition")
    measurement_location: str | None = Field(None, alias="measurementLocation")
    measurement_status_raw: int | None = Field(None, alias="measurementStatusRaw")
