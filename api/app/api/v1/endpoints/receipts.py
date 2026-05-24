import base64
from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db
from app.models.receipt import Receipt
from app.models.user import User
from app.schemas.receipt import (
    ExpenseStats,
    GroupTotal,
    ReceiptCreate,
    ReceiptOCRInput,
    ReceiptRead,
)
from app.services.ocr_service import parse_receipt_with_gemini


router = APIRouter(prefix="/receipts", tags=["receipts"])


@router.post("", response_model=ReceiptRead)
def create_receipt(
    payload: ReceiptCreate,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> Receipt:
    receipt = Receipt(**payload.model_dump(), owner_id=current_user.id)
    db.add(receipt)
    db.commit()
    db.refresh(receipt)
    return receipt


@router.post("/scan", response_model=ReceiptRead)
def scan_receipt(
    payload: ReceiptOCRInput,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> Receipt:
    parsed = parse_receipt_with_gemini(payload.image_base64, payload.mime_type)

    receipt = Receipt(
        store=parsed.store,
        category=parsed.category,
        total=parsed.total,
        currency=parsed.currency,
        purchase_date=parsed.purchase_date,
        latitude=payload.latitude,
        longitude=payload.longitude,
        owner_id=current_user.id,
    )
    db.add(receipt)
    db.commit()
    db.refresh(receipt)
    return receipt


@router.post("/scan-file", response_model=ReceiptRead)
async def scan_receipt_file(
    file: Annotated[UploadFile, File(...)],
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
    latitude: Annotated[float | None, Form()] = None,
    longitude: Annotated[float | None, Form()] = None,
) -> Receipt:
    allowed_types = {"image/jpeg", "image/png", "application/pdf"}
    content_type = (file.content_type or "").lower()
    if content_type not in allowed_types:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Unsupported file type. Use image/jpeg, image/png, or application/pdf",
        )

    file_bytes = await file.read()
    if not file_bytes:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Empty file upload",
        )

    file_base64 = base64.b64encode(file_bytes).decode("ascii")
    parsed = parse_receipt_with_gemini(file_base64, content_type)

    receipt = Receipt(
        store=parsed.store,
        category=parsed.category,
        total=parsed.total,
        currency=parsed.currency,
        purchase_date=parsed.purchase_date,
        latitude=latitude,
        longitude=longitude,
        owner_id=current_user.id,
    )
    db.add(receipt)
    db.commit()
    db.refresh(receipt)
    return receipt


@router.get("", response_model=list[ReceiptRead])
def list_receipts(
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
    start_date: Annotated[datetime | None, Query()] = None,
    end_date: Annotated[datetime | None, Query()] = None,
) -> list[Receipt]:
    query = db.query(Receipt).filter(Receipt.owner_id == current_user.id)
    if start_date:
        query = query.filter(Receipt.purchase_date >= start_date)
    if end_date:
        query = query.filter(Receipt.purchase_date <= end_date)
    return query.order_by(Receipt.purchase_date.desc()).all()


@router.get("/stats")
def expenses_stats(
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
    start_date: Annotated[datetime | None, Query()] = None,
    end_date: Annotated[datetime | None, Query()] = None,
) -> ExpenseStats:
    base_query = db.query(Receipt).filter(Receipt.owner_id == current_user.id)
    if start_date:
        base_query = base_query.filter(Receipt.purchase_date >= start_date)
    if end_date:
        base_query = base_query.filter(Receipt.purchase_date <= end_date)

    category_rows = (
        base_query.with_entities(Receipt.category, func.sum(Receipt.total))
        .group_by(Receipt.category)
        .order_by(func.sum(Receipt.total).desc())
        .all()
    )
    store_rows = (
        base_query.with_entities(Receipt.store, func.sum(Receipt.total))
        .group_by(Receipt.store)
        .order_by(func.sum(Receipt.total).desc())
        .all()
    )

    return ExpenseStats(
        by_category=[
            GroupTotal(key=row[0], total=float(row[1] or 0)) for row in category_rows
        ],
        by_store=[
            GroupTotal(key=row[0], total=float(row[1] or 0)) for row in store_rows
        ],
    )
