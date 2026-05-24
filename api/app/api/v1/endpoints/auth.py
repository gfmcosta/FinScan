import random
import string
from datetime import datetime, timedelta, UTC
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

from app.api.deps import get_db, get_current_user
from app.models.user import User
from app.schemas.token import Token
from app.schemas.user import UserCreate, UserRead, ChangePasswordRequest, ForgotPasswordRequest, ResetPasswordRequest
from app.security.jwt import create_access_token
from app.security.password import get_password_hash, verify_password
from app.services.email_service import send_reset_code_email


router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=UserRead, status_code=status.HTTP_201_CREATED)
def register(user_in: UserCreate, db: Annotated[Session, Depends(get_db)]) -> User:
    exists = (
        db.query(User)
        .filter((User.username == user_in.username) | (User.email == user_in.email))
        .first()
    )
    if exists:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Username or email already exists",
        )
    # Validations
    if user_in.role not in ["admin", "user"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Role must be either 'admin' or 'user'",
        )
    if user_in.username.strip() == "" or user_in.email.strip() == "" or user_in.password.strip() == "":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Username, email, and password cannot be empty",
        )
    if user_in.email.count("@") != 1 or "." not in user_in.email.split("@")[1]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid email format",
        )
    
    user = User(
        username=user_in.username,
        email=user_in.email,
        name=user_in.name,
        hashed_password=get_password_hash(user_in.password),
        role=user_in.role,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


@router.post("/login")
def login(
    form_data: Annotated[OAuth2PasswordRequestForm, Depends()],
    db: Annotated[Session, Depends(get_db)],
) -> Token:
    user = db.query(User).filter(User.username == form_data.username).first()
    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
        )
    # Validations
    if user.username.strip() == "" or user.email.strip() == "" or user.hashed_password.strip() == "":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="User data is incomplete",
        )
    if user.email.count("@") != 1 or "." not in user.email.split("@")[1]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid email format",
        )
    token = create_access_token(subject=user.username, role=user.role.value)
    return Token(access_token=token, name=user.name)


@router.post("/change-password", status_code=status.HTTP_200_OK)
def change_password(
    request: ChangePasswordRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
):
    """
    Change password for the current user.
    """
    if not verify_password(request.current_password, current_user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Incorrect current password",
        )

    current_user.hashed_password = get_password_hash(request.new_password)
    db.add(current_user)
    db.commit()
    db.refresh(current_user)
    return {"detail": "Password changed successfully"}


@router.post("/forgot-password")
def forgot_password(
    request: ForgotPasswordRequest,
    db: Annotated[Session, Depends(get_db)],
):
    user = db.query(User).filter(User.email == request.email).first()
    if not user:
        # We return 200 even if user doesn't exist for security reasons (don't leak emails)
        return {"detail": "If the email exists, a reset code has been sent"}

    # Generate 6-digit code
    code = "".join(random.choices(string.digits, k=6))
    user.reset_code = code
    user.reset_code_expires_at = datetime.now(UTC) + timedelta(minutes=15)

    db.add(user)
    db.commit()

    # Send email
    if send_reset_code_email(user.email, code):
        return {"detail": "Reset code sent successfully"}
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to send reset email",
        )


@router.post("/reset-password")
def reset_password(
    request: ResetPasswordRequest,
    db: Annotated[Session, Depends(get_db)],
):
    user = db.query(User).filter(User.email == request.email).first()
    if not user or user.reset_code != request.code:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid email or code",
        )

    if user.reset_code_expires_at.replace(tzinfo=UTC) < datetime.now(UTC):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Reset code has expired",
        )

    user.hashed_password = get_password_hash(request.new_password)
    user.reset_code = None
    user.reset_code_expires_at = None

    db.add(user)
    db.commit()

    return {"detail": "Password reset successfully"}
