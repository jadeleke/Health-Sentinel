from app.models import DailyHealthSummaryModel


def average(values: list[float]) -> float | None:
    return sum(values) / len(values) if values else None


def compute_deltas(target: DailyHealthSummaryModel, previous: list[DailyHealthSummaryModel]) -> dict[str, float | None]:
    resting_avg = average([x.resting_heart_rate for x in previous if x.resting_heart_rate is not None])
    sleep_avg = average([float(x.sleep_minutes) for x in previous if x.sleep_minutes is not None])
    steps_avg = average([float(x.steps) for x in previous if x.steps is not None])
    temp_avg = average([x.body_temperature_celsius for x in previous if x.body_temperature_celsius is not None])
    return {
        "restingHrDelta7d": None if target.resting_heart_rate is None or resting_avg is None else target.resting_heart_rate - resting_avg,
        "sleepDelta7d": None if target.sleep_minutes is None or sleep_avg is None else target.sleep_minutes - sleep_avg,
        "stepsDelta7d": None if target.steps is None or steps_avg is None else target.steps - steps_avg,
        "temperatureDelta7d": None if target.body_temperature_celsius is None or temp_avg is None else target.body_temperature_celsius - temp_avg,
        "restingHrAvg7d": resting_avg,
        "sleepAvg7d": sleep_avg,
        "stepsAvg7d": steps_avg,
        "temperatureAvg7d": temp_avg,
    }


def score_summary(target: DailyHealthSummaryModel, deltas: dict[str, float | None]) -> tuple[float, str, list[str]]:
    score = 0.0
    risks: list[str] = []
    rhr = deltas["restingHrDelta7d"]
    sleep = deltas["sleepDelta7d"]
    steps = deltas["stepsDelta7d"]
    temp = deltas["temperatureDelta7d"]

    if rhr is not None and rhr >= 8:
        score += 0.35
        risks.append(f"Resting HR is +{rhr:.0f} bpm above 7-day baseline")
    elif rhr is not None and rhr >= 5:
        score += 0.20
        risks.append(f"Resting HR is +{rhr:.0f} bpm above 7-day baseline")
    if sleep is not None and sleep <= -60:
        score += 0.20
        risks.append(f"Sleep is {sleep:.0f} minutes below 7-day baseline")
    if steps is not None and steps <= -2500:
        score += 0.15
        risks.append(f"Steps are {steps:.0f} below 7-day baseline")
    if temp is not None and temp >= 0.3:
        score += 0.20
        risks.append(f"Body temperature is +{temp:.1f} C above 7-day baseline")
    if target.oxygen_saturation_percent is not None and target.oxygen_saturation_percent < 95:
        score += 0.10
        risks.append(f"Oxygen saturation is {target.oxygen_saturation_percent:.1f}%")

    score = max(0.0, min(1.0, score))
    status = "Elevated" if score >= 0.60 else "Watch" if score >= 0.30 else "Normal"
    return score, status, risks
