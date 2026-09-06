"""initial schema

Revision ID: 20260906_01
Revises:
Create Date: 2026-09-06 21:30:00
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = "20260906_01"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute("CREATE EXTENSION IF NOT EXISTS timescaledb")

    op.create_table(
        "users",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("username", sa.String(length=64), nullable=False),
        sa.Column("hashed_password", sa.String(length=255), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False),
        sa.Column("is_admin", sa.Boolean(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_users_username"), "users", ["username"], unique=True)

    op.create_table(
        "refresh_tokens",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("jti", sa.String(length=64), nullable=False),
        sa.Column("revoked", sa.Boolean(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_refresh_tokens_jti"), "refresh_tokens", ["jti"], unique=True)

    op.create_table(
        "devices",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("name", sa.String(length=128), nullable=False),
        sa.Column("device_type", sa.String(length=64), nullable=False),
        sa.Column("ble_address", sa.String(length=32), nullable=True),
        sa.Column("api_key_hash", sa.String(length=255), nullable=False),
        sa.Column("api_key_prefix", sa.String(length=16), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("last_seen_at", sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_devices_api_key_prefix"), "devices", ["api_key_prefix"], unique=False)
    op.create_index(op.f("ix_devices_user_id"), "devices", ["user_id"], unique=False)

    op.create_table(
        "measurements",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("device_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("measurement_type", sa.String(length=64), nullable=False),
        sa.Column("measurement_uuid", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("recorded_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("raw_payload", postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.ForeignKeyConstraint(["device_id"], ["devices.id"]),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("measurement_uuid"),
    )
    op.create_index(op.f("ix_measurements_device_id"), "measurements", ["device_id"], unique=False)
    op.create_index(op.f("ix_measurements_measurement_type"), "measurements", ["measurement_type"], unique=False)
    op.create_index(op.f("ix_measurements_measurement_uuid"), "measurements", ["measurement_uuid"], unique=True)
    op.create_index(op.f("ix_measurements_recorded_at"), "measurements", ["recorded_at"], unique=False)
    op.create_index(op.f("ix_measurements_user_id"), "measurements", ["user_id"], unique=False)

    op.create_table(
        "blood_pressure_readings",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("measurement_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("systolic", sa.Float(), nullable=False),
        sa.Column("diastolic", sa.Float(), nullable=False),
        sa.Column("pulse", sa.SmallInteger(), nullable=True),
        sa.Column("body_position", sa.String(length=32), nullable=True),
        sa.Column("measurement_location", sa.String(length=32), nullable=True),
        sa.Column("measurement_status_raw", sa.Integer(), nullable=True),
        sa.ForeignKeyConstraint(["measurement_id"], ["measurements.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("measurement_id"),
    )


def downgrade() -> None:
    op.drop_table("blood_pressure_readings")
    op.drop_index(op.f("ix_measurements_user_id"), table_name="measurements")
    op.drop_index(op.f("ix_measurements_recorded_at"), table_name="measurements")
    op.drop_index(op.f("ix_measurements_measurement_uuid"), table_name="measurements")
    op.drop_index(op.f("ix_measurements_measurement_type"), table_name="measurements")
    op.drop_index(op.f("ix_measurements_device_id"), table_name="measurements")
    op.drop_table("measurements")
    op.drop_index(op.f("ix_devices_user_id"), table_name="devices")
    op.drop_index(op.f("ix_devices_api_key_prefix"), table_name="devices")
    op.drop_table("devices")
    op.drop_index(op.f("ix_refresh_tokens_jti"), table_name="refresh_tokens")
    op.drop_table("refresh_tokens")
    op.drop_index(op.f("ix_users_username"), table_name="users")
    op.drop_table("users")
