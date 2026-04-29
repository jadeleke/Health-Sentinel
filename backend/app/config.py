from functools import lru_cache

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    health_sentinel_api_key: str = "change-me"
    telegram_bot_token: str | None = None
    telegram_chat_id: str | None = None
    llm_provider: str = "deterministic"
    openai_api_key: str | None = None
    google_api_key: str | None = None
    daily_report_time: str = "07:30"
    database_url: str = "sqlite:///./health_sentinel.db"

    class Config:
        env_file = ".env"
        extra = "ignore"


@lru_cache
def get_settings() -> Settings:
    return Settings()
