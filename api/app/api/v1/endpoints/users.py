import base64
import os
import secrets
import uuid
from typing import Annotated

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db, require_roles
from app.core.config import settings
from app.models.user import User, UserRole
from app.schemas.user import UserRead, UserUpdate
from app.services.email_service import send_email_change_email

UPLOADS_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "..", "..", "uploads")

router = APIRouter(prefix="/users", tags=["users"])


def _save_avatar(base64_str: str) -> str:
    """Decode base64 image, save to uploads/, return filename."""
    os.makedirs(UPLOADS_DIR, exist_ok=True)
    data = base64.b64decode(base64_str)
    filename = f"{uuid.uuid4()}.jpg"
    path = os.path.join(UPLOADS_DIR, filename)
    with open(path, "wb") as f:
        f.write(data)
    return filename


def _delete_avatar(filename: str | None) -> None:
    """Delete an old avatar file if it exists."""
    if not filename:
        return
    path = os.path.join(UPLOADS_DIR, filename)
    try:
        if os.path.isfile(path):
            os.remove(path)
    except Exception:
        pass


@router.get("/me", response_model=UserRead)
def me(current_user: Annotated[User, Depends(get_current_user)]) -> User:
    return current_user


@router.patch("/me", response_model=UserRead)
def update_me(
    payload: UserUpdate,
    background_tasks: BackgroundTasks,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> User:
    # Username change: check uniqueness
    if payload.username is not None and payload.username != current_user.username:
        if db.query(User).filter(User.username == payload.username, User.id != current_user.id).first():
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="username_taken")
        current_user.username = payload.username

    # Name change: direct update
    if payload.name is not None:
        current_user.name = payload.name

    # Avatar: decode base64, save as file, store only filename
    if payload.avatar_base64 is not None:
        old_filename = current_user.avatar
        try:
            new_filename = _save_avatar(payload.avatar_base64)
        except Exception:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid avatar image")
        _delete_avatar(old_filename)
        current_user.avatar = new_filename
    elif payload.avatar_base64 == "":
        # Empty string = remove avatar
        _delete_avatar(current_user.avatar)
        current_user.avatar = None

    # Email change: check uniqueness, store pending, send confirmation
    if payload.email is not None and payload.email != current_user.email:
        if db.query(User).filter(User.email == payload.email, User.id != current_user.id).first():
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="email_taken")
        token = secrets.token_urlsafe(32)
        current_user.pending_email = payload.email
        current_user.email_change_token = token
        background_tasks.add_task(send_email_change_email, payload.email, token, settings.base_url)

    db.add(current_user)
    db.commit()
    db.refresh(current_user)
    return current_user


@router.get("", response_model=list[UserRead])
def list_users(
    db: Annotated[Session, Depends(get_db)],
    _: Annotated[User, Depends(require_roles(UserRole.admin))],
) -> list[User]:
    return db.query(User).all()
