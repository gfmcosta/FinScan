from datetime import UTC, datetime, timedelta

from jose import JWTError, jwt

from app.core.config import settings
from app.schemas.token import TokenPayload


def create_access_token(
    subject: str, role: str, expires_delta: timedelta | None = None
) -> str:
    expire = datetime.now(UTC) + (
        expires_delta or timedelta(minutes=settings.access_token_expire_minutes)
    )
    to_encode = {"sub": subject, "role": role, "exp": expire, "type": "access"}
    return jwt.encode(to_encode, settings.secret_key, algorithm=settings.algorithm)


def create_refresh_token(subject: str, role: str) -> str:
    expire = datetime.now(UTC) + timedelta(days=settings.refresh_token_expire_days)
    to_encode = {"sub": subject, "role": role, "exp": expire, "type": "refresh"}
    return jwt.encode(to_encode, settings.secret_key, algorithm=settings.algorithm)


def decode_access_token(token: str) -> TokenPayload:
    try:
        payload = jwt.decode(
            token, settings.secret_key, algorithms=[settings.algorithm]
        )
        return TokenPayload(**payload)
    except JWTError as exc:
        raise ValueError("Invalid token") from exc


def decode_refresh_token(token: str) -> TokenPayload:
    try:
        payload = jwt.decode(
            token, settings.secret_key, algorithms=[settings.algorithm]
        )
        if payload.get("type") != "refresh":
            raise ValueError("Not a refresh token")
        return TokenPayload(**payload)
    except JWTError as exc:
        raise ValueError("Invalid refresh token") from exc
