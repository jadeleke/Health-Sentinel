from apscheduler.schedulers.background import BackgroundScheduler

from app.config import get_settings
from app.database import SessionLocal
from app.routers.telegram import send_latest_report


def start_scheduler() -> BackgroundScheduler:
    settings = get_settings()
    hour, minute = [int(x) for x in settings.daily_report_time.split(":", 1)]
    scheduler = BackgroundScheduler()

    def job():
        db = SessionLocal()
        try:
            send_latest_report(db)
        finally:
            db.close()

    # TODO: background sync - add Android/server coordination and richer retry handling.
    scheduler.add_job(job, "cron", hour=hour, minute=minute, id="daily_report", replace_existing=True)
    scheduler.start()
    return scheduler
