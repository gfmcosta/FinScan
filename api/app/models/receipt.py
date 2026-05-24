from datetime import datetime

from sqlalchemy import Column, DateTime, Float, ForeignKey, Integer, String
from sqlalchemy.orm import relationship

from app.db.base_class import Base


class Receipt(Base):
    __tablename__ = "receipt"

    id = Column(Integer, primary_key=True, index=True)
    store = Column(String, index=True, nullable=False)
    category = Column(String, index=True, nullable=False, default="other")
    purchase_date = Column(DateTime, default=datetime.utcnow, nullable=False)
    total = Column(Float, nullable=False)
    currency = Column(String, default="EUR", nullable=False)
    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)

    owner_id = Column(Integer, ForeignKey("user.id"), nullable=False)
    owner = relationship("User", back_populates="receipts")
