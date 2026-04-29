from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import DailyHealthSummaryModel
from app.routers.auth import require_api_key
from app.schemas import AnalysisOutput
from app.services.telegram_service import send_telegram_message

router = APIRouter(prefix="/telegram", tags=["telegram"], dependencies=[Depends(require_api_key)])


def fmt(value, suffix: str = "") -> str:
    return f"{value}{suffix}" if value is not None else "missing"


def sleep_text(minutes: int | None) -> str:
    return "missing" if minutes is None else f"{minutes // 60}h {minutes % 60}m"


def format_report(summary: DailyHealthSummaryModel, analysis: AnalysisOutput) -> str:
    signals = "\n".join([f"- {item}" for item in analysis.riskFactors]) or "- No major baseline deviations"
    return (
        "Health Sentinel - Daily Summary\n"
        f"Date: {summary.date}\n"
        f"Status: {analysis.status}\n"
        f"Score: {analysis.anomalyScore:.2f}\n\n"
        "Metrics:\n"
        f"- Steps: {fmt(f'{summary.steps:,}' if summary.steps is not None else None)}\n"
        f"- Sleep: {sleep_text(summary.sleep_minutes)}\n"
        f"- Resting HR: {fmt(summary.resting_heart_rate, ' bpm')}\n"
        f"- Body temp: {fmt(summary.body_temperature_celsius, ' C')}\n"
        f"- SpO2: {fmt(summary.oxygen_saturation_percent, '%')}\n\n"
        "Signals:\n"
        f"{signals}\n\n"
        "Insight:\n"
        f"{analysis.llmInsight}\n\n"
        "Disclaimer:\n"
        "Personal experiment only. Not medical advice."
    )


def send_latest_report(db: Session):
    from app.routers.health import analyze_and_store

    latest = db.query(DailyHealthSummaryModel).order_by(DailyHealthSummaryModel.date.desc()).first()
    if latest is None:
        raise HTTPException(status_code=404, detail="No summaries available")
    analysis = analyze_and_store(db, latest)
    return send_telegram_message(format_report(latest, analysis))


@router.post("/test")
def telegram_test():
    return send_telegram_message("Health Sentinel test message. Personal experiment only. Not medical advice.")


@router.post("/send-latest")
def telegram_send_latest(db: Session = Depends(get_db)):
    return send_latest_report(db)
