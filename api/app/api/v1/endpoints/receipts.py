import base64
from datetime import datetime, timedelta
from typing import Annotated

from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, status
from sqlalchemy import func, or_
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db
from app.models.category import Category
from app.models.receipt import Receipt
from app.models.user import User
from app.schemas.receipt import (
    ExpenseStats,
    GroupTotal,
    ReceiptCreate,
    ReceiptOCRInput,
    ReceiptRead,
    ReceiptUpdate,
)
from app.services.ocr_service import parse_receipt_with_gemini


router = APIRouter(prefix="/receipts", tags=["receipts"])


@router.post("", response_model=ReceiptRead)
def create_receipt(
    payload: ReceiptCreate,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> Receipt:
    category = db.query(Category).filter(Category.id == payload.category_id).first()
    if not category:
        raise HTTPException(status_code=400, detail="Category not found")
    receipt = Receipt(**payload.model_dump(), owner_id=current_user.id)
    db.add(receipt)
    db.commit()
    db.refresh(receipt)
    return receipt


@router.post("/seed", response_model=list[ReceiptRead])
def seed_receipts(
    db: Annotated[Session, Depends(get_db)],
) -> list[Receipt]:
    user = db.query(User).filter(User.id == 1).first()
    if not user:
        raise HTTPException(status_code=400, detail="User 1 not found. Register first.")

    existing = db.query(Receipt).filter(Receipt.owner_id == 1).count()
    if existing > 0:
        raise HTTPException(status_code=400, detail="User 1 already has receipts")

    cat_map = {c.name: c.id for c in db.query(Category).filter(Category.is_default == True).all()}
    now = datetime.utcnow()
    samples = [
        ("Continente", "Supermarket", 45.30, 1),
        ("Pingo Doce", "Supermarket", 32.15, 3),
        ("McDonald's", "Restaurant", 12.50, 4),
        ("BP", "Fuel", 65.00, 5),
        ("FNAC", "Electronics", 89.99, 6),
        ("Auchan", "Supermarket", 28.40, 7),
        ("Galp", "Fuel", 55.20, 8),
        ("Zara", "Clothing", 39.90, 10),
        ("Worten", "Electronics", 149.99, 12),
        ("Restaurante O Lago", "Restaurant", 27.80, 14),
        ("Lidl", "Supermarket", 22.35, 1),
        ("Starbucks", "Cafe", 8.90, 16),
        ("Sport Zone", "Sports", 59.99, 18),
        ("Uber", "Transport", 14.50, 20),
        ("Cinema NOS", "Entertainment", 19.50, 22),
    ]
    created = []
    for store, cat_name, total, days_ago in samples:
        cid = cat_map.get(cat_name, cat_map.get("Other"))
        if cid is None:
            continue
        r = Receipt(store=store, category_id=cid, total=total,
                    purchase_date=now - timedelta(days=days_ago), owner_id=1)
        db.add(r)
        created.append(r)
    db.commit()
    for r in created:
        db.refresh(r)
    return created


@router.post("/scan", response_model=ReceiptRead)
def scan_receipt(
    payload: ReceiptOCRInput,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> Receipt:
    parsed = parse_receipt_with_gemini(payload.image_base64, payload.mime_type)

    category = (
        db.query(Category)
        .filter(
            Category.name == parsed.category,
            or_(Category.owner_id == current_user.id, Category.is_default == True),
        )
        .first()
    )
    if not category:
        category = Category(name=parsed.category, owner_id=current_user.id, is_default=False)
        db.add(category)
        db.flush()

    receipt = Receipt(
        store=parsed.store,
        category_id=category.id,
        total=parsed.total,
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

    category = (
        db.query(Category)
        .filter(
            Category.name == parsed.category,
            or_(Category.owner_id == current_user.id, Category.is_default == True),
        )
        .first()
    )
    if not category:
        category = Category(name=parsed.category, owner_id=current_user.id, is_default=False)
        db.add(category)
        db.flush()

    receipt = Receipt(
        store=parsed.store,
        category_id=category.id,
        total=parsed.total,
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
    search: Annotated[str | None, Query()] = None,
    skip: Annotated[int, Query(ge=0)] = 0,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
) -> list[Receipt]:
    query = (
        db.query(Receipt)
        .join(Category, Receipt.category_id == Category.id)
        .filter(Receipt.owner_id == current_user.id)
    )
    if start_date:
        query = query.filter(Receipt.purchase_date >= start_date)
    if end_date:
        query = query.filter(Receipt.purchase_date <= end_date)
    if search:
        pattern = f"%{search}%"
        conditions = [
            Receipt.store.ilike(pattern),
            Category.name.ilike(pattern),
        ]
        try:
            search_float = float(search)
            conditions.append(Receipt.total == search_float)
        except ValueError:
            pass
        query = query.filter(or_(*conditions))
    return query.order_by(Receipt.purchase_date.desc()).offset(skip).limit(limit).all()


@router.get("/stats")
def expenses_stats(
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
    start_date: Annotated[datetime | None, Query()] = None,
    end_date: Annotated[datetime | None, Query()] = None,
) -> ExpenseStats:
    base_query = (
        db.query(Receipt)
        .join(Category, Receipt.category_id == Category.id)
        .filter(Receipt.owner_id == current_user.id)
    )
    if start_date:
        base_query = base_query.filter(Receipt.purchase_date >= start_date)
    if end_date:
        base_query = base_query.filter(Receipt.purchase_date <= end_date)

    category_rows = (
        base_query.with_entities(Category.name, func.sum(Receipt.total))
        .group_by(Category.name)
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


@router.get("/{receipt_id}", response_model=ReceiptRead)
def get_receipt(
    receipt_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> Receipt:
    receipt = db.query(Receipt).filter(
        Receipt.id == receipt_id, Receipt.owner_id == current_user.id
    ).first()
    if not receipt:
        raise HTTPException(status_code=404, detail="Receipt not found")
    return receipt


@router.put("/{receipt_id}", response_model=ReceiptRead)
def update_receipt(
    receipt_id: int,
    payload: ReceiptUpdate,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> Receipt:
    receipt = db.query(Receipt).filter(
        Receipt.id == receipt_id, Receipt.owner_id == current_user.id
    ).first()
    if not receipt:
        raise HTTPException(status_code=404, detail="Receipt not found")
    if payload.category_id is not None:
        category = db.query(Category).filter(Category.id == payload.category_id).first()
        if not category:
            raise HTTPException(status_code=400, detail="Category not found")
    update_data = payload.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(receipt, key, value)
    db.commit()
    db.refresh(receipt)
    return receipt


@router.delete("/{receipt_id}", status_code=204)
def delete_receipt(
    receipt_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> None:
    receipt = db.query(Receipt).filter(
        Receipt.id == receipt_id, Receipt.owner_id == current_user.id
    ).first()
    if not receipt:
        raise HTTPException(status_code=404, detail="Receipt not found")
    db.delete(receipt)
    db.commit()
    return None
