import httpx

from app.config import get_settings


def send_telegram_message(text: str) -> dict:
    settings = get_settings()
    if not settings.telegram_bot_token or not settings.telegram_chat_id:
        raise RuntimeError("Telegram is not configured")
    url = f"https://api.telegram.org/bot{settings.telegram_bot_token}/sendMessage"
    response = httpx.post(
        url,
        json={
            "chat_id": settings.telegram_chat_id,
            "text": text,
            "parse_mode": "Markdown",
        },
        timeout=20,
    )
    response.raise_for_status()
    return response.json()
