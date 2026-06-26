import os
from datetime import datetime
from typing import Annotated, Optional

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Query, status
from fastapi.responses import FileResponse
from sqlalchemy import extract
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db
from app.models.receipt import Receipt
from app.models.report import Report
from app.models.user import User
from app.schemas.report import GenerateReportRequest, ReportRead
from app.services.report_service import REPORTS_DIR, generate_pdf

router = APIRouter(prefix="/reports", tags=["reports"])


def _generate_in_background(report_id: int, locale: str) -> None:
    """Background task: generate PDF and update report status."""
    from app.db.session import SessionLocal

    db = SessionLocal()
    try:
        report = db.query(Report).filter(Report.id == report_id).first()
        if not report:
            return

        query = db.query(Receipt).filter(Receipt.owner_id == report.user_id)
        if report.date_from:
            query = query.filter(
                Receipt.purchase_date >= datetime.combine(report.date_from, datetime.min.time())
            )
        if report.date_to:
            query = query.filter(
                Receipt.purchase_date <= datetime.combine(report.date_to, datetime.max.time())
            )
        receipts = query.all()

        user = db.query(User).filter(User.id == report.user_id).first()
        username = user.username if user else "unknown"

        filename = generate_pdf(receipts, report.date_from, report.date_to, username, locale)

        report.filename = filename
        report.status = "completed"
        db.add(report)
        db.commit()

    except Exception as exc:
        db.rollback()
        try:
            report = db.query(Report).filter(Report.id == report_id).first()
            if report:
                report.status = "failed"
                db.add(report)
                db.commit()
        except Exception:
            pass
        raise exc
    finally:
        db.close()


@router.post("/generate", response_model=ReportRead, status_code=status.HTTP_202_ACCEPTED)
def generate_report(
    payload: GenerateReportRequest,
    background_tasks: BackgroundTasks,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> Report:
    report = Report(
        user_id=current_user.id,
        status="generating",
        date_from=payload.date_from,
        date_to=payload.date_to,
    )
    db.add(report)
    db.commit()
    db.refresh(report)
    background_tasks.add_task(_generate_in_background, report.id, payload.locale)
    return report


@router.get("", response_model=list[ReportRead])
def list_reports(
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
    skip: int = Query(0, ge=0),
    limit: int = Query(11, ge=1, le=50),
    month: Optional[int] = Query(None, ge=1, le=12),
    year: Optional[int] = Query(None, ge=2000),
) -> list[Report]:
    q = db.query(Report).filter(Report.user_id == current_user.id)
    if month is not None:
        q = q.filter(extract("month", Report.created_at) == month)
    if year is not None:
        q = q.filter(extract("year", Report.created_at) == year)
    return q.order_by(Report.created_at.desc()).offset(skip).limit(limit).all()


@router.delete("/{report_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_report(
    report_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> None:
    report = db.query(Report).filter(
        Report.id == report_id,
        Report.user_id == current_user.id,
    ).first()
    if not report:
        raise HTTPException(status_code=404, detail="Report not found")

    # Remove the PDF file if it exists
    if report.filename:
        filepath = os.path.join(REPORTS_DIR, report.filename)
        if os.path.exists(filepath):
            os.remove(filepath)

    db.delete(report)
    db.commit()


@router.get("/{report_id}/download")
def download_report(
    report_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
):
    report = db.query(Report).filter(
        Report.id == report_id,
        Report.user_id == current_user.id,
    ).first()
    if not report:
        raise HTTPException(status_code=404, detail="Report not found")
    if report.status != "completed" or not report.filename:
        raise HTTPException(status_code=400, detail="Report is not ready yet")

    filepath = os.path.join(REPORTS_DIR, report.filename)
    if not os.path.exists(filepath):
        raise HTTPException(status_code=404, detail="Report file not found")

    if report.date_from or report.date_to:
        from_s = report.date_from.strftime("%Y%m%d") if report.date_from else "start"
        to_s   = report.date_to.strftime("%Y%m%d")   if report.date_to   else "end"
        dl_name = f"FinScan_Report_{from_s}_{to_s}.pdf"
    else:
        dl_name = "FinScan_Report_AllTime.pdf"

    return FileResponse(filepath, media_type="application/pdf", filename=dl_name)
