import enum

from sqlalchemy import Boolean, Column, Enum, Integer, String, DateTime
from sqlalchemy.orm import relationship

from app.db.base_class import Base


class UserRole(str, enum.Enum):
    user = "user"
    admin = "admin"
    analyst = "analyst"


class User(Base):
    __tablename__ = "user"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    name = Column(String, nullable=True)
    hashed_password = Column(String, nullable=False)
    is_active = Column(Boolean(), default=True)
    role = Column(Enum(UserRole), default=UserRole.user, nullable=False)
    reset_code = Column(String, nullable=True)
    reset_code_expires_at = Column(DateTime, nullable=True)

    receipts = relationship("Receipt", back_populates="owner")
