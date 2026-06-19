from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict


class ReceiptBase(BaseModel):
    store: str
    category_id: int
    total: float
    purchase_date: datetime
    latitude: float | None = None
    longitude: float | None = None


class ReceiptCreate(ReceiptBase):
    pass


class ReceiptUpdate(BaseModel):
    store: str | None = None
    category_id: int | None = None
    total: float | None = None
    purchase_date: datetime | None = None
    latitude: float | None = None
    longitude: float | None = None


class ReceiptRead(ReceiptBase):
    id: int
    owner_id: int
    category_name: str = ""

    model_config = ConfigDict(from_attributes=True)


class ReceiptOCRInput(BaseModel):
    image_base64: str
    mime_type: Literal["image/jpeg", "image/png",
                       "application/pdf"] = "image/jpeg"
    latitude: float | None = None
    longitude: float | None = None


class ReceiptOCRParsed(BaseModel):
    store: str
    category: str
    total: float
    purchase_date: datetime


class GroupTotal(BaseModel):
    key: str
    total: float


class ExpenseStats(BaseModel):
    by_category: list[GroupTotal]
    by_store: list[GroupTotal]
