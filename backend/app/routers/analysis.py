import json

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import AnalysisResultModel, DailyHealthSummaryModel
from app.routers.auth import require_api_key
from app.routers.health import analyze_and_store
from app.schemas import AnalysisOutput

router = APIRouter(prefix="/analysis", tags=["analysis"], dependencies=[Depends(require_api_key)])


@router.post("/run", response_model=AnalysisOutput)
def run_analysis(db: Session = Depends(get_db)):
    latest = db.query(DailyHealthSummaryModel).order_by(DailyHealthSummaryModel.date.desc()).first()
    if latest is None:
        raise HTTPException(status_code=404, detail="No summaries available")
    return analyze_and_store(db, latest)


def result_to_schema(result: AnalysisResultModel) -> AnalysisOutput:
    return AnalysisOutput(
        date=result.date,
        anomalyScore=result.anomaly_score,
        status=result.status,
        facts=json.loads(result.facts_json),
        riskFactors=json.loads(result.risk_factors_json),
        recommendation=result.recommendation,
        llmInsight=result.llm_insight,
        disclaimer=result.disclaimer,
    )
