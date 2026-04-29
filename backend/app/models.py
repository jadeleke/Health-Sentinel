from datetime import datetime

from sqlalchemy import DateTime, Float, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class DailyHealthSummaryModel(Base):
    __tablename__ = "daily_health_summaries"

    date: Mapped[str] = mapped_column(String(10), primary_key=True)
    steps: Mapped[int | None] = mapped_column(Integer, nullable=True)
    sleep_minutes: Mapped[int | None] = mapped_column(Integer, nullable=True)
    resting_heart_rate: Mapped[float | None] = mapped_column(Float, nullable=True)
    avg_heart_rate: Mapped[float | None] = mapped_column(Float, nullable=True)
    min_heart_rate: Mapped[float | None] = mapped_column(Float, nullable=True)
    max_heart_rate: Mapped[float | None] = mapped_column(Float, nullable=True)
    body_temperature_celsius: Mapped[float | None] = mapped_column(Float, nullable=True)
    oxygen_saturation_percent: Mapped[float | None] = mapped_column(Float, nullable=True)
    source: Mapped[str] = mapped_column(String(64), default="health_connect")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class AnalysisResultModel(Base):
    __tablename__ = "analysis_results"

    date: Mapped[str] = mapped_column(String(10), primary_key=True)
    anomaly_score: Mapped[float] = mapped_column(Float)
    status: Mapped[str] = mapped_column(String(16))
    facts_json: Mapped[str] = mapped_column(Text)
    risk_factors_json: Mapped[str] = mapped_column(Text)
    recommendation: Mapped[str] = mapped_column(Text)
    llm_insight: Mapped[str] = mapped_column(Text)
    disclaimer: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
