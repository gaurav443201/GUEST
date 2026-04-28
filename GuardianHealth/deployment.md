# Guardian Health — Deployment & Connection Guide

## Overview

The system has three components:
| Component       | Tech Stack          | Location              |
|-----------------|---------------------|-----------------------|
| Android App     | Kotlin + Compose    | `android_app/`        |
| Backend API     | Python + FastAPI    | `backend/`            |
| Web Dashboard   | HTML + JS           | `web_dashboard/`      |

---

## 1. Deploy Backend on Render

### Step 1: Push backend to GitHub

```bash
# From the GuardianHealth/ folder
cd backend/
git init
git add .
git commit -m "Guardian Health API v2"
git remote add origin https://github.com/YOUR_USERNAME/guardian-health-api.git
git push -u origin main
```

### Step 2: Create a Render Web Service

1. Go to [https://render.com](https://render.com) → **New +** → **Web Service**
2. Connect your GitHub account and select the repository
3. Configure the service:

| Setting         | Value                                         |
|-----------------|-----------------------------------------------|
| Name            | `guardian-health-api`                         |
| Environment     | `Python 3`                                    |
| Region          | Choose closest (e.g. Singapore for India)     |
| Branch          | `main`                                        |
| Root Directory  | Leave blank (or `backend` if pushed full repo)|
| Build Command   | `pip install -r requirements.txt`             |
| Start Command   | `uvicorn main:app --host 0.0.0.0 --port $PORT`|

4. Click **Create Web Service**
5. Wait ~2 minutes for the first deploy to complete.

### Step 3: Get your Render URL

After deployment, your URL will look like:
```
https://guardian-health-api.onrender.com
```
> ⚠️ Free tier Render services "spin down" after 15 min of inactivity. First request after sleep takes ~30s.

### Step 4: Test the API

```bash
# Health check
curl https://guardian-health-api.onrender.com/

# View API docs (interactive Swagger UI)
open https://guardian-health-api.onrender.com/docs

# Trigger a test emergency
curl -X POST https://guardian-health-api.onrender.com/emergency \
  -H "Content-Type: application/json" \
  -d '{"heart_rate":165,"spo2":82,"timestamp":"2024-01-01T12:00:00","location":"Floor 2","room_number":"Room 203","status":"emergency"}'

# List alerts
curl https://guardian-health-api.onrender.com/alerts
```

---

## 2. Connect Android App to Backend

Open this file in Android Studio:

```
android_app/app/src/main/java/com/example/guardianhealth/network/ApiClient.kt
```

Replace the `BASE_URL`:

```kotlin
// For Render (deployed):
const val BASE_URL = "https://guardian-health-api.onrender.com/"

// For Android Emulator + local server:
const val BASE_URL = "http://10.0.2.2:8000/"

// For Physical device on same Wi-Fi:
const val BASE_URL = "http://YOUR_PC_IP:8000/"
```

Also verify `android_app/app/src/main/AndroidManifest.xml` has internet permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 3. Connect Web Dashboard to Backend

Open `web_dashboard/index.html` and update line ~222:

```javascript
// Before:
const API_URL = "https://guest-65oe.onrender.com";

// After (your new Render URL):
const API_URL = "https://guardian-health-api.onrender.com";
```

To view the dashboard, simply open `index.html` in any browser — no server needed.

---

## 4. Run Backend Locally (Development)

```bash
cd GuardianHealth/backend/

# Create virtual environment (recommended)
python -m venv venv
venv\Scripts\activate          # Windows
# source venv/bin/activate     # macOS/Linux

# Install dependencies
pip install -r requirements.txt

# Start server
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# API docs available at:
# http://localhost:8000/docs
```

---

## 5. Build Android App

1. Open `android_app/` folder in **Android Studio Iguana (2023.2.1)** or later
2. Wait for Gradle sync to complete
3. Update `BASE_URL` in `ApiClient.kt` (see Section 2 above)
4. Connect device or launch emulator
5. Click ▶ **Run**

### Required dependencies (auto-downloaded via Gradle):
- Jetpack Compose BOM `2024.x`
- Navigation Compose `2.8.9`
- Retrofit `2.x` + Gson Converter
- OkHttp `4.12.0`
- Material Icons Extended

---

## 6. Demo Flow (Quick Start)

1. **Open web dashboard** in browser (Staff Panel)
2. **Launch Android app** on device/emulator (Guest Device)
3. In the app, press **"Emergency"** button
4. Watch the dashboard turn red and show the emergency alert
5. Press **"Mark Resolved"** on dashboard → alert turns grey
6. Press **"Room Service"** or **"Call Assistance"** in app
7. New entries appear in dashboard under respective priorities
8. Open **History screen** in app (📋 icon) to see all logged events

---

## 7. API Endpoint Reference

| Method | Endpoint             | Description                        |
|--------|----------------------|------------------------------------|
| GET    | `/`                  | Health check / root info           |
| POST   | `/health-data`       | Send periodic vitals               |
| POST   | `/emergency`         | Trigger emergency alert            |
| GET    | `/alerts`            | Get all alerts (newest first)      |
| POST   | `/service-request`   | Send room service / assistance     |
| POST   | `/resolve/{id}`      | Mark alert as resolved             |
| POST   | `/clear`             | Clear all alerts (demo reset)      |
| GET    | `/stats`             | Summary statistics                 |
| GET    | `/docs`              | Interactive Swagger UI             |
