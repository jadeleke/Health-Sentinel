from datetime import datetime

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.analysis.baseline import analyze_summary
from app.agents.onetwo_health_agent import generate_health_insight
from app.database import get_db
from app.models import AnalysisResultModel, DailyHealthSummaryModel
from app.routers.auth import require_api_key
from app.schemas import AnalysisOutput, DailyHealthSummaryIn, DailyHealthSummaryOut
from app.services.telegram_service import send_telegram_message
from app.routers.telegram import format_report

router = APIRouter(prefix="/health", tags=["health"], dependencies=[Depends(require_api_key)])


def to_model(payload: DailyHealthSummaryIn) -> DailyHealthSummaryModel:
    return DailyHealthSummaryModel(
        date=payload.date,
        steps=payload.steps,
        sleep_minutes=payload.sleepMinutes,
        resting_heart_rate=payload.restingHeartRate,
        avg_heart_rate=payload.avgHeartRate,
        min_heart_rate=payload.minHeartRate,
        max_heart_rate=payload.maxHeartRate,
        body_temperature_celsius=payload.bodyTemperatureCelsius,
        oxygen_saturation_percent=payload.oxygenSaturationPercent,
        source=payload.source,
    )


def to_out(model: DailyHealthSummaryModel) -> DailyHealthSummaryOut:
    return DailyHealthSummaryOut(
        date=model.date,
        steps=model.steps,
        sleepMinutes=model.sleep_minutes,
        restingHeartRate=model.resting_heart_rate,
        avgHeartRate=model.avg_heart_rate,
        minHeartRate=model.min_heart_rate,
        maxHeartRate=model.max_heart_rate,
        bodyTemperatureCelsius=model.body_temperature_celsius,
        oxygenSaturationPercent=model.oxygen_saturation_percent,
        source=model.source,
    )


def upsert(db: Session, payload: DailyHealthSummaryIn) -> DailyHealthSummaryModel:
    existing = db.get(DailyHealthSummaryModel, payload.date)
    if existing is None:
        existing = to_model(payload)
        db.add(existing)
    else:
        existing.steps = payload.steps
        existing.sleep_minutes = payload.sleepMinutes
        existing.resting_heart_rate = payload.restingHeartRate
        existing.avg_heart_rate = payload.avgHeartRate
        existing.min_heart_rate = payload.minHeartRate
        existing.max_heart_rate = payload.maxHeartRate
        existing.body_temperature_celsius = payload.bodyTemperatureCelsius
        existing.oxygen_saturation_percent = payload.oxygenSaturationPercent
        existing.source = payload.source
        existing.updated_at = datetime.utcnow()
    return existing


def analyze_and_store(db: Session, model: DailyHealthSummaryModel) -> AnalysisOutput:
    import json

    deltas, score, status, facts, risks, recommendation = analyze_summary(db, model)
    agent_input = {
        "date": model.date,
        "metrics": {
            "steps": model.steps,
            "sleepMinutes": model.sleep_minutes,
            "restingHeartRate": model.resting_heart_rate,
            "avgHeartRate": model.avg_heart_rate,
            "minHeartRate": model.min_heart_rate,
            "maxHeartRate": model.max_heart_rate,
            "bodyTemperatureCelsius": model.body_temperature_celsius,
            "oxygenSaturationPercent": model.oxygen_saturation_percent,
        },
        "baselines": {
            "restingHrAvg7d": deltas["restingHrAvg7d"],
            "sleepAvg7d": deltas["sleepAvg7d"],
            "stepsAvg7d": deltas["stepsAvg7d"],
            "temperatureAvg7d": deltas["temperatureAvg7d"],
        },
        "deltas": deltas,
        "anomalyScore": score,
        "status": status,
        "riskFactors": risks,
    }
    insight = generate_health_insight(agent_input)
    result = AnalysisOutput(
        date=model.date,
        anomalyScore=score,
        status=status,
        facts=facts,
        riskFactors=risks,
        recommendation=recommendation,
        llmInsight=insight,
        disclaimer="Personal experiment only. Not medical advice.",
    )
    existing = db.get(AnalysisResultModel, model.date)
    if existing is None:
        existing = AnalysisResultModel(date=model.date, anomaly_score=score, status=status)
        db.add(existing)
    existing.anomaly_score = score
    existing.status = status
    existing.facts_json = json.dumps(facts)
    existing.risk_factors_json = json.dumps(risks)
    existing.recommendation = recommendation
    existing.llm_insight = insight
    existing.disclaimer = result.disclaimer
    existing.updated_at = datetime.utcnow()
    db.commit()
    return result


@router.post("/daily-summary", response_model=DailyHealthSummaryOut)
def post_daily_summary(payload: DailyHealthSummaryIn, db: Session = Depends(get_db)):
    model = upsert(db, payload)
    db.commit()
    db.refresh(model)
    result = analyze_and_store(db, model)
    if result.status == "Elevated":
        try:
            send_telegram_message(format_report(model, result))
        except Exception:
            pass
    return to_out(model)


@router.post("/bulk-summary", response_model=list[DailyHealthSummaryOut])
def post_bulk_summary(payloads: list[DailyHealthSummaryIn], db: Session = Depends(get_db)):
    models = [upsert(db, payload) for payload in payloads]
    db.commit()
    for model in models:
        db.refresh(model)
    latest = max(models, key=lambda item: item.date) if models else None
    if latest is not None:
        result = analyze_and_store(db, latest)
        if result.status == "Elevated":
            try:
                send_telegram_message(format_report(latest, result))
            except Exception:
                pass
    return [to_out(model) for model in sorted(models, key=lambda item: item.date, reverse=True)]


@router.get("/recent", response_model=list[DailyHealthSummaryOut])
def recent(days: int = 30, db: Session = Depends(get_db)):
    rows = (
        db.query(DailyHealthSummaryModel)
        .order_by(DailyHealthSummaryModel.date.desc())
        .limit(max(1, min(days, 365)))
        .all()
    )
    return [to_out(row) for row in rows]
