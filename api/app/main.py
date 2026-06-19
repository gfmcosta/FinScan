from fastapi import FastAPI

from app.api.v1.endpoints.auth import router as auth_router
from app.api.v1.endpoints.categories import router as categories_router
from app.api.v1.endpoints.chatbot import router as chatbot_router
from app.api.v1.endpoints.receipts import router as receipts_router
from app.api.v1.endpoints.users import router as users_router
from app.core.config import settings
from app.db.session import init_db


app = FastAPI(title=settings.app_name)


@app.on_event("startup")
def on_startup() -> None:
    init_db()


app.include_router(auth_router, prefix=settings.api_v1_prefix)
app.include_router(categories_router, prefix=settings.api_v1_prefix)
app.include_router(users_router, prefix=settings.api_v1_prefix)
app.include_router(receipts_router, prefix=settings.api_v1_prefix)
app.include_router(chatbot_router, prefix=settings.api_v1_prefix)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
