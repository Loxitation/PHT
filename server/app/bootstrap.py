"""Bootstrap helpers for first local admin user and first device API key."""
from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.core.api_key import generate_api_key
from app.core.database import SessionLocal
from app.core.logging import configure_logging, get_logger
from app.core.security import hash_password
from app.models.device import Device
from app.models.user import User

configure_logging()
logger = get_logger(__name__)


def create_bootstrap_entities(
    db: Session,
    username: str,
    password: str,
    device_name: str,
    device_type: str,
    ble_address: str | None,
) -> dict[str, str]:
    user = db.query(User).filter(User.username == username).first()
    if user is None:
        user = User(username=username, hashed_password=hash_password(password), is_admin=True, is_active=True)
        db.add(user)
        db.flush()

    raw_key, key_prefix, key_hash = generate_api_key()
    device = Device(
        user_id=user.id,
        name=device_name,
        device_type=device_type,
        ble_address=ble_address,
        api_key_hash=key_hash,
        api_key_prefix=key_prefix,
        is_active=True,
        last_seen_at=datetime.now(timezone.utc),
    )
    db.add(device)
    db.commit()
    db.refresh(device)

    logger.info("bootstrap_created", username=username, device_id=str(device.id))
    return {
        "username": username,
        "device_id": str(device.id),
        "api_key": raw_key,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--username", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--device-name", required=True)
    parser.add_argument("--device-type", required=True)
    parser.add_argument("--ble-address")
    args = parser.parse_args()

    db = SessionLocal()
    try:
        result = create_bootstrap_entities(
            db=db,
            username=args.username,
            password=args.password,
            device_name=args.device_name,
            device_type=args.device_type,
            ble_address=args.ble_address,
        )
    finally:
        db.close()

    print(json.dumps(result))


if __name__ == "__main__":
    main()
