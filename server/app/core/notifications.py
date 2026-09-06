"""Gotify client placeholder for later alerting integration."""
from __future__ import annotations

import httpx

from app.core.config import get_settings
from app.core.logging import get_logger

settings = get_settings()
logger = get_logger(__name__)


class GotifyClient:
    def __init__(self) -> None:
        self.enabled = settings.gotify_enabled and bool(settings.gotify_url and settings.gotify_token)

    async def send_message(self, title: str, message: str, priority: int = 5) -> None:
        if not self.enabled:
            logger.info("gotify_disabled", title=title)
            return

        headers = {"X-Gotify-Key": settings.gotify_token}
        payload = {"title": title, "message": message, "priority": priority}
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(f"{settings.gotify_url.rstrip('/')}/message", json=payload, headers=headers)
            response.raise_for_status()
