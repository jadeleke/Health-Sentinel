from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.database import Base, engine
from app.routers import analysis, health, telegram
from app.services.scheduler import start_scheduler


@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    scheduler = start_scheduler()
    yield
    scheduler.shutdown(wait=False)


app = FastAPI(
    title="Health Sentinel Backend",
    description="Personal wellness summaries. Not medical advice.",
    version="0.1.0",
    lifespan=lifespan,
)

app.include_router(health.router)
app.include_router(analysis.router)
app.include_router(telegram.router)


@app.get("/healthz")
def healthz():
    return {"ok": True}
