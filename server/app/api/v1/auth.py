"""JWT login / refresh endpoints for the web dashboard."""
import uuid

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.database import get_db
from app.core.deps import get_current_user
from app.core.rate_limit import limiter
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    verify_password,
)
from app.models.user import RefreshToken, User
from app.schemas.auth import LoginRequest, RefreshRequest, RefreshTokenPair, TokenPair

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])
settings = get_settings()


@router.post("/login", response_model=TokenPair)
@limiter.limit(settings.login_rate_limit)
def login(request: Request, payload: LoginRequest, db: Session = Depends(get_db)) -> TokenPair:
    user = db.query(User).filter(User.username == payload.username).first()
    if user is None or not user.is_active or not verify_password(payload.password, user.hashed_password):
        # Deliberately generic error message to avoid username enumeration.
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Invalid username or password")

    jti = str(uuid.uuid4())
    from datetime import datetime, timedelta, timezone

    refresh_record = RefreshToken(
        user_id=user.id,
        jti=jti,
        expires_at=datetime.now(timezone.utc) + timedelta(days=settings.refresh_token_expire_days),
    )
    db.add(refresh_record)
    db.commit()

    return TokenPair(
        access_token=create_access_token(str(user.id)),
        refresh_token=create_refresh_token(str(user.id), jti),
    )


@router.post("/refresh", response_model=RefreshTokenPair)
def refresh(payload: RefreshRequest, db: Session = Depends(get_db)) -> RefreshTokenPair:
    data = decode_token(payload.refresh_token)
    if data is None or data.get("type") != "refresh":
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Invalid or expired refresh token")

    jti = data.get("jti")
    record = db.query(RefreshToken).filter(RefreshToken.jti == jti).first()
    if record is None or record.revoked:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Refresh token revoked or unknown")

    # Rotation: revoke the used refresh token and replace it with a freshly issued pair.
    record.revoked = True
    new_jti = str(uuid.uuid4())
    from datetime import datetime, timedelta, timezone

    new_record = RefreshToken(
        user_id=record.user_id,
        jti=new_jti,
        expires_at=datetime.now(timezone.utc) + timedelta(days=settings.refresh_token_expire_days),
    )
    db.add(new_record)
    db.commit()

    return RefreshTokenPair(
        access_token=create_access_token(str(record.user_id)),
        refresh_token=create_refresh_token(str(record.user_id), new_jti),
    )


@router.get("/me")
def me(current_user: User = Depends(get_current_user)):
    return {"id": str(current_user.id), "username": current_user.username, "is_admin": current_user.is_admin}
