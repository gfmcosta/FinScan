"""
In-memory WebSocket connection manager.
Maps user_id → list of active WebSocket connections (one user may have
multiple open tabs / devices).
"""
from __future__ import annotations

import asyncio
import logging
from typing import Dict, List, Optional

from fastapi import WebSocket

logger = logging.getLogger(__name__)


class ConnectionManager:
    def __init__(self) -> None:
        # user_id → [WebSocket, ...]
        self._connections: Dict[int, List[WebSocket]] = {}
        self._lock = asyncio.Lock()
        # Set by main.py on startup so background threads can schedule coroutines
        self._loop: Optional[asyncio.AbstractEventLoop] = None

    def set_loop(self, loop: asyncio.AbstractEventLoop) -> None:
        self._loop = loop

    # ── Connection lifecycle ──────────────────────────────────────────────────

    async def connect(self, websocket: WebSocket, user_id: int) -> None:
        await websocket.accept()
        async with self._lock:
            self._connections.setdefault(user_id, []).append(websocket)
        logger.info("WS connect  user_id=%s  total_users=%s", user_id, len(self._connections))

    async def disconnect(self, websocket: WebSocket, user_id: int) -> None:
        async with self._lock:
            conns = self._connections.get(user_id, [])
            if websocket in conns:
                conns.remove(websocket)
            if not conns:
                self._connections.pop(user_id, None)
        logger.info("WS disconnect user_id=%s  total_users=%s", user_id, len(self._connections))

    # ── Sending ───────────────────────────────────────────────────────────────

    async def send_to_user(self, user_id: int, payload: dict) -> bool:
        """Send *payload* to all connections for *user_id*.
        Returns True if at least one connection received it."""
        async with self._lock:
            conns = list(self._connections.get(user_id, []))

        if not conns:
            return False

        dead: List[WebSocket] = []
        sent = False
        for ws in conns:
            try:
                await ws.send_json(payload)
                sent = True
            except Exception:
                dead.append(ws)

        # Clean up dead sockets
        if dead:
            async with self._lock:
                for ws in dead:
                    try:
                        self._connections[user_id].remove(ws)
                    except (KeyError, ValueError):
                        pass

        return sent

    async def broadcast(self, payload: dict) -> int:
        """Send *payload* to every connected user. Returns number of users reached."""
        async with self._lock:
            user_ids = list(self._connections.keys())

        reached = 0
        for uid in user_ids:
            if await self.send_to_user(uid, payload):
                reached += 1
        return reached

    # ── Introspection ─────────────────────────────────────────────────────────

    def send_to_user_threadsafe(self, user_id: int, payload: dict) -> None:
        """Schedule a send from a synchronous background thread."""
        if self._loop is None or not self._loop.is_running():
            return
        asyncio.run_coroutine_threadsafe(
            self.send_to_user(user_id, payload), self._loop
        )

    @property
    def connected_user_ids(self) -> List[int]:
        return list(self._connections.keys())

    @property
    def connection_count(self) -> int:
        return sum(len(v) for v in self._connections.values())


# Module-level singleton shared across the entire process
manager = ConnectionManager()
