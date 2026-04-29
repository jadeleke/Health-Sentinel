# Health Sentinel

MVP personal wellness app for Oraimo Watch 6R data that has already synced into Android Health Connect.

This project has two parts:

- `app/`: Android Kotlin + Jetpack Compose app using Room and Android Health Connect.
- `backend/`: Python FastAPI backend using SQLite, deterministic anomaly scoring, optional OneTwo insight generation, and Telegram reports.

This is for personal experimentation only and does not provide medical advice.

## Android App

The Android app reads:

- Steps
- Heart rate
- Resting heart rate
- Sleep sessions and embedded sleep stages where available
- Body temperature where available
- Oxygen saturation where available

It stores one `DailyHealthSummary` per date in local Room storage, calculates rolling 7-day baseline deltas using previous days only, and shows `Normal`, `Watch`, or `Elevated` wellness status.

The Health Connect dependency uses the current official AndroidX release checked on April 29, 2026:

```kotlin
implementation("androidx.health.connect:connect-client:1.2.0-alpha04")
```

Android 14+ includes Health Connect. Android 13 and lower may require the Health Connect app from Google.

### Build Android

Open the repo in Android Studio and run the `app` configuration.

The app requests Health Connect permissions on first launch and includes a simple rationale screen. Use **Sync Now** to read the last 30 days from Health Connect.

### Backend Upload

In the Android History tab:

1. Set backend URL, for example `http://192.168.1.20:8000`.
2. Set the same API key as `HEALTH_SENTINEL_API_KEY`.
3. Tap **Upload latest summaries**.

Use a LAN IP or emulator host mapping that your Android device can reach.

## Backend Setup

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
```

Edit `.env`:

```env
HEALTH_SENTINEL_API_KEY=choose-a-long-random-value
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
LLM_PROVIDER=deterministic
DAILY_REPORT_TIME=07:30
```

Run:

```powershell
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Test:

```powershell
curl -H "X-Health-Sentinel-Key: choose-a-long-random-value" http://localhost:8000/health/recent
```

## Telegram Setup

1. Open Telegram and message `@BotFather`.
2. Create a bot with `/newbot`.
3. Put the returned token in `TELEGRAM_BOT_TOKEN`.
4. Send a message to your new bot.
5. Get your chat ID, for example by opening:
   `https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/getUpdates`
6. Put the chat ID in `TELEGRAM_CHAT_ID`.

Test Telegram:

```powershell
curl -X POST -H "X-Health-Sentinel-Key: choose-a-long-random-value" http://localhost:8000/telegram/test
```

Send latest wellness report:

```powershell
curl -X POST -H "X-Health-Sentinel-Key: choose-a-long-random-value" http://localhost:8000/telegram/send-latest
```

## API

- `POST /health/daily-summary`
- `POST /health/bulk-summary`
- `GET /health/recent?days=30`
- `POST /analysis/run`
- `POST /telegram/test`
- `POST /telegram/send-latest`

All protected endpoints require:

```text
X-Health-Sentinel-Key: <HEALTH_SENTINEL_API_KEY>
```

## OneTwo

The backend isolates OneTwo in `backend/app/agents/onetwo_health_agent.py`. If `LLM_PROVIDER` is not `onetwo`, or if the library/API is unavailable, it falls back to deterministic cautious text.

The LLM receives structured facts only and must not diagnose or invent facts.

## Future TODOs

- TimesFM forecasting
- LLM-generated explanations with a stable OneTwo provider adapter
- background sync
- cloud backup
- export to CSV
