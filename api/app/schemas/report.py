from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel


class GenerateReportRequest(BaseModel):
    date_from: Optional[date] = None   # null = since forever
    date_to: Optional[date] = None
    locale: str = "en"                 # "pt" or "en"


class ReportRead(BaseModel):
    id: int
    status: str
    filename: Optional[str] = None
    date_from: Optional[date] = None
    date_to: Optional[date] = None
    created_at: datetime

    model_config = {"from_attributes": True}
