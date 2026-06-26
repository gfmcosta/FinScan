import asyncio
import os

from fastapi import FastAPI
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles

from app.api.v1.endpoints.auth import router as auth_router
from app.api.v1.endpoints.categories import router as categories_router
from app.api.v1.endpoints.chatbot import router as chatbot_router
from app.api.v1.endpoints.notifications import router as notifications_router
from app.api.v1.endpoints.receipts import router as receipts_router
from app.api.v1.endpoints.reports import router as reports_router
from app.api.v1.endpoints.users import router as users_router
from app.core.config import settings
from app.db.session import init_db
from app.services.notification_manager import manager as ws_manager

UPLOADS_DIR = os.path.join(os.path.dirname(__file__), "..", "uploads")
REPORTS_DIR = os.path.join(os.path.dirname(__file__), "..", "reports")

app = FastAPI(title=settings.app_name)


@app.on_event("startup")
async def on_startup() -> None:
    os.makedirs(UPLOADS_DIR, exist_ok=True)
    os.makedirs(REPORTS_DIR, exist_ok=True)
    app.mount("/uploads", StaticFiles(directory=UPLOADS_DIR), name="uploads")
    init_db()
    # Store event loop reference so background threads can schedule WS sends
    ws_manager.set_loop(asyncio.get_event_loop())


app.include_router(auth_router,          prefix=settings.api_v1_prefix)
app.include_router(categories_router,    prefix=settings.api_v1_prefix)
app.include_router(users_router,         prefix=settings.api_v1_prefix)
app.include_router(receipts_router,      prefix=settings.api_v1_prefix)
app.include_router(chatbot_router,       prefix=settings.api_v1_prefix)
app.include_router(reports_router,       prefix=settings.api_v1_prefix)
app.include_router(notifications_router, prefix=settings.api_v1_prefix)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


_ADMIN_HTML_PATH = os.path.join(os.path.dirname(__file__), "templates", "admin_notifications.html")


@app.get("/admin", response_class=HTMLResponse, include_in_schema=False)
def admin_panel() -> HTMLResponse:
    """Notification admin web interface."""
    with open(_ADMIN_HTML_PATH, encoding="utf-8") as f:
        return HTMLResponse(content=f.read())
