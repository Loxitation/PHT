"""Blood pressure sync endpoint (API-key protected) + generic measurement listing (JWT protected)."""
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.deps import get_current_user, get_device_from_api_key
from app.core.logging import get_logger
from app.models.device import Device
from app.models.measurement import BloodPressureReading, Measurement
from app.models.user import User
from app.schemas.measurement import BloodPressureMeasurementIn, BloodPressureMeasurementOut

router = APIRouter(prefix="/api/v1/measurements", tags=["measurements"])
logger = get_logger(__name__)


@router.post("/blood-pressure", response_model=BloodPressureMeasurementOut, status_code=status.HTTP_201_CREATED)
def create_blood_pressure_measurement(
    payload: BloodPressureMeasurementIn,
    device: Device = Depends(get_device_from_api_key),
    db: Session = Depends(get_db),
):
    existing = db.query(Measurement).filter(Measurement.measurement_uuid == payload.measurement_uuid).first()
    if existing is not None:
        # Idempotent retry: return the already-stored reading instead of erroring.
        logger.info("measurement_duplicate_ignored", measurement_uuid=str(payload.measurement_uuid))
        reading = existing.blood_pressure_reading
        return BloodPressureMeasurementOut(
            id=existing.id,
            measurement_type=existing.measurement_type,
            measurement_uuid=existing.measurement_uuid,
            recorded_at=existing.recorded_at,
            created_at=existing.created_at,
            device_id=existing.device_id,
            raw_payload=existing.raw_payload,
            systolic=reading.systolic if reading else None,
            diastolic=reading.diastolic if reading else None,
            pulse=reading.pulse if reading else None,
            body_position=reading.body_position if reading else None,
            measurement_location=reading.measurement_location if reading else None,
            measurement_status_raw=reading.measurement_status_raw if reading else None,
        )

    if payload.device_id is not None and payload.device_id != device.id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Payload device_id does not match authenticated device")

    measurement = Measurement(
        user_id=device.user_id,
        device_id=device.id,
        measurement_type="blood_pressure",
        measurement_uuid=payload.measurement_uuid,
        recorded_at=payload.time,
        raw_payload={
            **payload.raw_payload,
            "systolic": payload.systolic,
            "diastolic": payload.diastolic,
            "pulse": payload.pulse,
            "time": payload.time.isoformat(),
            "zone_offset_seconds": payload.zone_offset_seconds,
            "body_position": payload.body_position,
            "measurement_location": payload.measurement_location,
            "measurement_status_raw": payload.measurement_status_raw,
        },
    )
    db.add(measurement)
    db.flush()  # populate measurement.id before creating the child row

    reading = BloodPressureReading(
        measurement_id=measurement.id,
        systolic=payload.systolic,
        diastolic=payload.diastolic,
        pulse=payload.pulse,
        body_position=payload.body_position,
        measurement_location=payload.measurement_location,
        measurement_status_raw=payload.measurement_status_raw,
    )
    db.add(reading)

    device.last_seen_at = datetime.now(timezone.utc)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        existing = db.query(Measurement).filter(Measurement.measurement_uuid == payload.measurement_uuid).first()
        if existing is None:
            raise
        reading = existing.blood_pressure_reading
        logger.info("measurement_duplicate_race_ignored", measurement_uuid=str(payload.measurement_uuid))
        return BloodPressureMeasurementOut(
            id=existing.id,
            measurement_type=existing.measurement_type,
            measurement_uuid=existing.measurement_uuid,
            recorded_at=existing.recorded_at,
            created_at=existing.created_at,
            device_id=existing.device_id,
            raw_payload=existing.raw_payload,
            systolic=reading.systolic if reading else None,
            diastolic=reading.diastolic if reading else None,
            pulse=reading.pulse if reading else None,
            body_position=reading.body_position if reading else None,
            measurement_location=reading.measurement_location if reading else None,
            measurement_status_raw=reading.measurement_status_raw if reading else None,
        )
    db.refresh(measurement)

    logger.info(
        "measurement_stored",
        measurement_uuid=str(payload.measurement_uuid),
        device_id=str(device.id),
        systolic=payload.systolic,
        diastolic=payload.diastolic,
    )

    return BloodPressureMeasurementOut(
        id=measurement.id,
        measurement_type=measurement.measurement_type,
        measurement_uuid=measurement.measurement_uuid,
        recorded_at=measurement.recorded_at,
        created_at=measurement.created_at,
        device_id=measurement.device_id,
        raw_payload=measurement.raw_payload,
        systolic=reading.systolic,
        diastolic=reading.diastolic,
        pulse=reading.pulse,
        body_position=reading.body_position,
        measurement_location=reading.measurement_location,
        measurement_status_raw=reading.measurement_status_raw,
    )


@router.get("", response_model=list[BloodPressureMeasurementOut])
def list_measurements(
    measurement_type: str | None = None,
    limit: int = 100,
    offset: int = 0,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    query = db.query(Measurement).filter(Measurement.user_id == current_user.id)
    if measurement_type:
        query = query.filter(Measurement.measurement_type == measurement_type)
    query = query.order_by(Measurement.recorded_at.desc()).offset(offset).limit(min(limit, 500))
    measurements = query.all()

    results = []
    for measurement in measurements:
        reading = measurement.blood_pressure_reading
        results.append(
            BloodPressureMeasurementOut(
                id=measurement.id,
                measurement_type=measurement.measurement_type,
                measurement_uuid=measurement.measurement_uuid,
                recorded_at=measurement.recorded_at,
                created_at=measurement.created_at,
                device_id=measurement.device_id,
                raw_payload=measurement.raw_payload,
                systolic=reading.systolic if reading else None,
                diastolic=reading.diastolic if reading else None,
                pulse=reading.pulse if reading else None,
                body_position=reading.body_position if reading else None,
                measurement_location=reading.measurement_location if reading else None,
                measurement_status_raw=reading.measurement_status_raw if reading else None,
            )
        )
    return results
