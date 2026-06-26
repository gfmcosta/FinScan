"""
Real-time notification endpoints.

• WS  /api/v1/notifications/ws?token=<jwt>
      Android clients connect here to receive push messages.

• POST /api/v1/notifications/send
      Admin: send a notification to one user or broadcast to all.

• GET  /api/v1/notifications/connected
      Admin: list currently-connected user IDs and socket count.
"""
from __future__ import annotations

from typing import Annotated, Optional

from fastapi import APIRouter, Depends, Header, HTTPException, Query, WebSocket, WebSocketDisconnect, status
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.core.config import settings
from app.models.user import User
from app.security.jwt import decode_access_token
from app.services.notification_manager import manager

router = APIRouter(prefix="/notifications", tags=["notifications"])


# ── Schemas ───────────────────────────────────────────────────────────────────

class SendNotificationRequest(BaseModel):
    title: str
    body: str
    user_id: Optional[int] = None   # None → broadcast to all connected users
    data: Optional[dict] = None     # extra payload the app can act on


class SendNotificationResponse(BaseModel):
    delivered_to: int               # number of users who received it
    broadcast: bool


class ConnectedUsersResponse(BaseModel):
    user_ids: list[int]
    total_connections: int


# ── Admin guard ───────────────────────────────────────────────────────────────

def require_admin_key(x_admin_key: str = Header(..., alias="X-Admin-Key")) -> None:
    if x_admin_key != settings.admin_key:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Invalid admin key")


# ── WebSocket endpoint ────────────────────────────────────────────────────────

@router.websocket("/ws")
async def websocket_endpoint(
    websocket: WebSocket,
    token: str = Query(...),
    db: Session = Depends(get_db),
) -> None:
    """
    Android clients connect with their JWT:
        ws://host/api/v1/notifications/ws?token=<access_token>

    After connecting, the client receives JSON messages:
        {"title": "...", "body": "...", "data": {...}}

    The server also sends a heartbeat ping every 30 s so the OS doesn't
    kill the connection on mobile.
    """
    # Authenticate
    try:
        payload = decode_access_token(token)
        user = db.query(User).filter(User.username == payload.sub).first()
        if not user:
            await websocket.close(code=4001)
            return
    except ValueError:
        await websocket.close(code=4001)
        return

    await manager.connect(websocket, user.id)
    try:
        # Send a welcome message so the client knows it's live
        await websocket.send_json({"type": "connected", "user_id": user.id})

        # Keep the connection alive — listen for pings from the client
        # (Android will send "ping" text frames every 30 s)
        while True:
            data = await websocket.receive_text()
            if data == "ping":
                await websocket.send_text("pong")

    except WebSocketDisconnect:
        pass
    finally:
        await manager.disconnect(websocket, user.id)


# ── Send endpoint (admin) ─────────────────────────────────────────────────────

@router.post("/send", response_model=SendNotificationResponse)
async def send_notification(
    payload: SendNotificationRequest,
    _: Annotated[None, Depends(require_admin_key)],
) -> SendNotificationResponse:
    """Send a push notification to a specific user or broadcast to all."""
    message = {
        "type": "notification",
        "title": payload.title,
        "body": payload.body,
        "data": payload.data or {},
    }

    if payload.user_id is not None:
        reached = 1 if await manager.send_to_user(payload.user_id, message) else 0
        return SendNotificationResponse(delivered_to=reached, broadcast=False)
    else:
        reached = await manager.broadcast(message)
        return SendNotificationResponse(delivered_to=reached, broadcast=True)


# ── Connected users (admin) ───────────────────────────────────────────────────

@router.get("/connected", response_model=ConnectedUsersResponse)
def connected_users(
    _: Annotated[None, Depends(require_admin_key)],
) -> ConnectedUsersResponse:
    return ConnectedUsersResponse(
        user_ids=manager.connected_user_ids,
        total_connections=manager.connection_count,
    )
