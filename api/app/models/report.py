from datetime import datetime

from sqlalchemy import Column, DateTime, Date, ForeignKey, Integer, String
from sqlalchemy.orm import relationship

from app.db.base_class import Base


class Report(Base):
    __tablename__ = "report"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user.id"), nullable=False)
    filename = Column(String, nullable=True)          # GUID.pdf — null while generating
    status = Column(String, default="generating")     # generating | completed | failed
    date_from = Column(Date, nullable=True)           # null = "since forever"
    date_to = Column(Date, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User")
