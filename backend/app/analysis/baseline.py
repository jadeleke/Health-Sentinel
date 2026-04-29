from sqlalchemy.orm import Session

from app.analysis.scoring import compute_deltas, score_summary
from app.models import DailyHealthSummaryModel


def previous_seven_days(db: Session, date: str) -> list[DailyHealthSummaryModel]:
    return (
        db.query(DailyHealthSummaryModel)
        .filter(DailyHealthSummaryModel.date < date)
        .order_by(DailyHealthSummaryModel.date.desc())
        .limit(7)
        .all()
    )[::-1]


def facts_for(target: DailyHealthSummaryModel, deltas: dict[str, float | None]) -> list[str]:
    facts = [
        f"Steps: {target.steps if target.steps is not None else 'missing'}",
        f"Sleep minutes: {target.sleep_minutes if target.sleep_minutes is not None else 'missing'}",
        f"Resting HR: {target.resting_heart_rate if target.resting_heart_rate is not None else 'missing'}",
        f"Average HR: {target.avg_heart_rate if target.avg_heart_rate is not None else 'missing'}",
        f"Body temperature C: {target.body_temperature_celsius if target.body_temperature_celsius is not None else 'missing'}",
        f"Oxygen saturation percent: {target.oxygen_saturation_percent if target.oxygen_saturation_percent is not None else 'missing'}",
    ]
    for key in ("restingHrDelta7d", "sleepDelta7d", "stepsDelta7d", "temperatureDelta7d"):
        if deltas[key] is not None:
            facts.append(f"{key}: {deltas[key]:.2f}")
    return facts


def analyze_summary(db: Session, target: DailyHealthSummaryModel):
    previous = previous_seven_days(db, target.date)
    deltas = compute_deltas(target, previous)
    score, status, risk_factors = score_summary(target, deltas)
    recommendation = {
        "Normal": "Current signals are close to recent baseline. Keep monitoring trends.",
        "Watch": "Some signals are outside recent baseline. Consider rest and monitor how you feel.",
        "Elevated": "Several signals are elevated compared with recent baseline. Consider rest, hydration, and monitoring symptoms.",
    }[status]
    return deltas, score, status, facts_for(target, deltas), risk_factors, recommendation
