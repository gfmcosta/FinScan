import random
import secrets
import string
from datetime import datetime, timedelta
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status, BackgroundTasks
from fastapi.responses import HTMLResponse
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

from app.api.deps import get_db, get_current_user
from app.core.config import settings
from app.models.user import User
from app.schemas.token import Token, RefreshRequest
from app.schemas.user import UserCreate, UserRead, ChangePasswordRequest, ForgotPasswordRequest, ResetPasswordRequest
from app.security.jwt import create_access_token, create_refresh_token, decode_refresh_token
from app.security.password import get_password_hash, verify_password
from app.services.email_service import send_reset_code_email, send_verification_email, send_email_change_email


router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=UserRead, status_code=status.HTTP_201_CREATED)
def register(
    user_in: UserCreate,
    background_tasks: BackgroundTasks,
    db: Annotated[Session, Depends(get_db)],
) -> User:
    exists = (
        db.query(User)
        .filter((User.username == user_in.username) | (User.email == user_in.email))
        .first()
    )
    if exists:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Username or email already exists")
    if user_in.role not in ["admin", "user"]:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Role must be either 'admin' or 'user'")
    if user_in.username.strip() == "" or user_in.email.strip() == "" or user_in.password.strip() == "":
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Username, email, and password cannot be empty")
    if " " in user_in.username:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Username must not contain spaces")
    if " " in user_in.email:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Email must not contain spaces")
    if user_in.email.count("@") != 1 or "." not in user_in.email.split("@")[1]:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid email format")
    if user_in.name and any(c.isdigit() for c in user_in.name):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Name must not contain numbers")

    verification_token = secrets.token_urlsafe(32)

    user = User(
        username=user_in.username,
        email=user_in.email,
        name=user_in.name,
        hashed_password=get_password_hash(user_in.password),
        role=user_in.role,
        is_verified=False,
        verification_token=verification_token,
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    background_tasks.add_task(send_verification_email, user.email, verification_token, settings.base_url)

    return user


@router.get("/verify-email", response_class=HTMLResponse)
def verify_email(token: str, db: Annotated[Session, Depends(get_db)]):
    user = db.query(User).filter(User.verification_token == token).first()
    if not user:
        return HTMLResponse(
            content=_html_page("Invalid link", "This verification link is invalid or has already been used.", success=False),
            status_code=400,
        )
    user.is_verified = True
    user.verification_token = None
    db.add(user)
    db.commit()
    return HTMLResponse(content=_html_page("Email verified!", "Your FinScan account is now active. You can log in in the app.", success=True))


@router.get("/confirm-email-change", response_class=HTMLResponse)
def confirm_email_change(token: str, db: Annotated[Session, Depends(get_db)]):
    user = db.query(User).filter(User.email_change_token == token).first()
    if not user or not user.pending_email:
        return HTMLResponse(
            content=_html_page("Invalid link", "This link is invalid or has already been used.", success=False),
            status_code=400,
        )
    taken = db.query(User).filter(User.email == user.pending_email, User.id != user.id).first()
    if taken:
        user.pending_email = None
        user.email_change_token = None
        db.add(user)
        db.commit()
        return HTMLResponse(
            content=_html_page("Email unavailable", "That email address is already in use by another account.", success=False),
            status_code=409,
        )
    user.email = user.pending_email
    user.pending_email = None
    user.email_change_token = None
    db.add(user)
    db.commit()
    return HTMLResponse(content=_html_page("Email updated!", "Your new email address has been confirmed successfully.", success=True))


@router.post("/login")
def login(
    form_data: Annotated[OAuth2PasswordRequestForm, Depends()],
    db: Annotated[Session, Depends(get_db)],
) -> Token:
    user = db.query(User).filter(User.username == form_data.username).first()
    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Incorrect username or password")

    # Block login only if explicitly unverified (None = legacy user, allowed through)
    if user.is_verified is False:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="email_not_verified")

    access_token = create_access_token(subject=str(user.id), role=user.role.value)
    refresh_token = create_refresh_token(subject=str(user.id), role=user.role.value)
    return Token(access_token=access_token, refresh_token=refresh_token, name=user.name, email=user.email)


@router.post("/change-password", status_code=status.HTTP_200_OK)
def change_password(
    request: ChangePasswordRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
):
    if not verify_password(request.current_password, current_user.hashed_password):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Incorrect current password")
    current_user.hashed_password = get_password_hash(request.new_password)
    db.add(current_user)
    db.commit()
    db.refresh(current_user)
    return {"detail": "Password changed successfully"}


@router.post("/forgot-password")
def forgot_password(
    request: ForgotPasswordRequest,
    background_tasks: BackgroundTasks,
    db: Annotated[Session, Depends(get_db)],
):
    user = db.query(User).filter(User.email == request.email).first()
    if not user:
        return {"detail": "If the email exists, a reset code has been sent"}

    code = "".join(random.choices(string.digits, k=6))
    user.reset_code = code
    user.reset_code_expires_at = datetime.now() + timedelta(minutes=15)
    db.add(user)
    db.commit()

    print(f"DEBUG: Enviar e-mail para {user.email} com código {code}", flush=True)
    background_tasks.add_task(send_reset_code_email, user.email, code)

    return {"detail": "Reset code sent successfully"}


@router.post("/reset-password")
def reset_password(request: ResetPasswordRequest, db: Annotated[Session, Depends(get_db)]):
    user = db.query(User).filter(User.email == request.email).first()
    if not user or user.reset_code != request.code:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid email or code")

    now = datetime.now()
    expires_at = user.reset_code_expires_at
    if expires_at:
        if not hasattr(expires_at, "hour"):
            expires_at = datetime.combine(expires_at, datetime.min.time())
        if expires_at < now:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Reset code has expired")

    user.hashed_password = get_password_hash(request.new_password)
    user.reset_code = None
    user.reset_code_expires_at = None
    db.add(user)
    db.commit()
    return {"detail": "Password reset successfully"}


@router.post("/refresh")
def refresh(payload: RefreshRequest, db: Annotated[Session, Depends(get_db)]) -> Token:
    try:
        token_data = decode_refresh_token(payload.refresh_token)
    except ValueError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired refresh token")

    try:
        user_id = int(token_data.sub)
    except (ValueError, TypeError):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token subject")

    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")

    access_token = create_access_token(subject=str(user.id), role=user.role.value)
    new_refresh_token = create_refresh_token(subject=str(user.id), role=user.role.value)
    return Token(access_token=access_token, refresh_token=new_refresh_token, name=user.name, email=user.email)


def _html_page(title: str, message: str, success: bool = True) -> str:
    color = "#4CAF50" if success else "#F44336"
    icon = "✓" if success else "✗"
    return f"""<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>FinScan – {title}</title>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
body{{font-family:Arial,sans-serif;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;background:#f5f5f5}}
.card{{background:#fff;border-radius:16px;padding:40px;text-align:center;max-width:400px;box-shadow:0 2px 12px rgba(0,0,0,.1)}}
.icon{{font-size:64px;color:{color}}}
h1{{color:#333}}p{{color:#666}}
</style>
</head><body>
<div class="card"><div class="icon">{icon}</div><h1>{title}</h1><p>{message}</p></div>
</body></html>"""
