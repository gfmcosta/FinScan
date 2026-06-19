from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db
from app.models.receipt import Receipt
from app.models.user import User
from app.schemas.chat import ChatAnswer, ChatQuestion
from app.services.chatbot_service import ask_expense_chatbot


router = APIRouter(prefix="/chatbot", tags=["chatbot"])


@router.post("/ask")
def ask(
    payload: ChatQuestion,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> ChatAnswer:
    receipts = (
        db.query(Receipt)
        .filter(Receipt.owner_id == current_user.id)
        .order_by(Receipt.purchase_date.desc())
        .limit(50)
        .all()
    )
    context_lines = [
        f"{r.purchase_date.date()} | {r.store} | {r.category_name} | {r.total} €"
        for r in receipts
    ]
    context = "\n".join(context_lines) if context_lines else "Sem despesas registadas."

    answer = ask_expense_chatbot(context, payload.question)
    return ChatAnswer(answer=answer)
