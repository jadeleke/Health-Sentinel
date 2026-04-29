from fastapi import Header, HTTPException

from app.config import get_settings


def require_api_key(x_health_sentinel_key: str | None = Header(default=None)):
    expected = get_settings().health_sentinel_api_key
    if not x_health_sentinel_key or x_health_sentinel_key != expected:
        raise HTTPException(status_code=401, detail="Invalid API key")
