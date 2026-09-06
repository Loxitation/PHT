"""Generic measurement envelope + typed blood pressure detail table.

Field naming for blood_pressure_readings follows Android Health Connect's
BloodPressureRecord conventions (systolic, diastolic, pulse, bodyPosition,
measurementLocation) so a future Health Connect integration needs no server
schema changes.
"""
import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, SmallInteger, String
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.database import Base


class Measurement(Base):
    """Generic envelope for any measurement type (blood pressure, weight, ...)."""

    __tablename__ = "measurements"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, index=True)
    device_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("devices.id"), nullable=True, index=True)

    measurement_type: Mapped[str] = mapped_column(String(64), nullable=False, index=True)  # e.g. "blood_pressure"

    # Client-generated UUID for idempotency: retried syncs with the same uuid are no-ops.
    measurement_uuid: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), unique=True, nullable=False, index=True)

    recorded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    # Full original decoded payload for traceability / future re-parsing.
    raw_payload: Mapped[dict] = mapped_column(JSONB, nullable=False, default=dict)

    user: Mapped["User"] = relationship(back_populates="measurements")
    device: Mapped["Device | None"] = relationship(back_populates="measurements")
    blood_pressure_reading: Mapped["BloodPressureReading | None"] = relationship(
        back_populates="measurement", cascade="all, delete-orphan", uselist=False
    )


class BloodPressureReading(Base):
    """Typed detail table for blood pressure measurements."""

    __tablename__ = "blood_pressure_readings"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    measurement_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("measurements.id"), unique=True, nullable=False
    )

    systolic: Mapped[float] = mapped_column(nullable=False)  # mmHg
    diastolic: Mapped[float] = mapped_column(nullable=False)  # mmHg
    pulse: Mapped[int | None] = mapped_column(SmallInteger, nullable=True)  # bpm

    body_position: Mapped[str | None] = mapped_column(String(32), nullable=True)
    measurement_location: Mapped[str | None] = mapped_column(String(32), nullable=True)

    # Raw Bluetooth SIG "Measurement Status" bitfield, kept for diagnostics (e.g. irregular pulse, cuff fit).
    measurement_status_raw: Mapped[int | None] = mapped_column(Integer, nullable=True)

    measurement: Mapped["Measurement"] = relationship(back_populates="blood_pressure_reading")
