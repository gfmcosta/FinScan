from pydantic import BaseModel, ConfigDict

from app.models.user import UserRole


class UserBase(BaseModel):
    username: str
    email: str
    name: str | None = None


class UserCreate(UserBase):
    password: str
    role: UserRole = UserRole.user


class UserRead(UserBase):
    id: int
    role: UserRole
    is_active: bool

    model_config = ConfigDict(from_attributes=True)


class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str


class ForgotPasswordRequest(BaseModel):
    email: str


class ResetPasswordRequest(BaseModel):
    email: str
    code: str
    new_password: str
