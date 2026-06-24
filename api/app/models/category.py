from sqlalchemy import Boolean, Column, Integer, String, ForeignKey
from sqlalchemy.orm import relationship

from app.db.base_class import Base


class Category(Base):
    __tablename__ = "category"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    icon = Column(String, default="Category")
    owner_id = Column(Integer, ForeignKey("user.id"), nullable=True)
    is_default = Column(Boolean, default=False)

    owner = relationship("User", back_populates="categories")
