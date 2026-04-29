from typing import Literal

from pydantic import BaseModel, Field


class DailyHealthSummaryIn(BaseModel):
    date: str = Field(pattern=r"^\d{4}-\d{2}-\d{2}$")
    steps: int | None = None
    sleepMinutes: int | None = None
    restingHeartRate: float | None = None
    avgHeartRate: float | None = None
    minHeartRate: float | None = None
    maxHeartRate: float | None = None
    bodyTemperatureCelsius: float | None = None
    oxygenSaturationPercent: float | None = None
    source: str = "health_connect"


class DailyHealthSummaryOut(DailyHealthSummaryIn):
    pass


class AnalysisOutput(BaseModel):
    date: str
    anomalyScore: float
    status: Literal["Normal", "Watch", "Elevated"]
    facts: list[str]
    riskFactors: list[str]
    recommendation: str
    llmInsight: str
    disclaimer: str
