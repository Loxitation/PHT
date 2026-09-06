"""FastAPI application entrypoint."""
from fastapi import FastAPI
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded

from app.api.v1 import auth, health, measurements
from app.core.config import get_settings
from app.core.logging import configure_logging, get_logger
from app.core.rate_limit import limiter

settings = get_settings()
configure_logging()
logger = get_logger(__name__)

app = FastAPI(title=settings.app_name)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

app.include_router(health.router)
app.include_router(auth.router)
app.include_router(measurements.router)


@app.on_event("startup")
def on_startup() -> None:
    logger.info("app_startup", app_name=settings.app_name)
