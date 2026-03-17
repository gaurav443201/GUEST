from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

app = FastAPI(title="Guardian Health Backend")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class HealthData(BaseModel):
    heart_rate: int
    spo2: int
    timestamp: str
    location: str

class Alert(BaseModel):
    id: int
    type: str # 'emergency', 'service', 'assistance'
    heart_rate: Optional[int] = None
    spo2: Optional[int] = None
    timestamp: str
    location: str
    status: str # 'active', 'resolved'

alerts_db: List[Alert] = []
alert_counter = 1

@app.get("/")
def read_root():
    return {"message": "Guardian Health API is running successfully!"}

@app.post("/health-data")
def receive_health_data(data: HealthData):
    return {"status": "success", "message": "Data received"}

@app.post("/emergency")
def trigger_emergency(data: HealthData):
    global alert_counter
    new_alert = Alert(
        id=alert_counter,
        type="emergency",
        heart_rate=data.heart_rate,
        spo2=data.spo2,
        timestamp=data.timestamp,
        location=data.location,
        status="active"
    )
    alerts_db.append(new_alert)
    alert_counter += 1
    return {"status": "success", "alert_id": new_alert.id}

@app.get("/alerts", response_model=List[Alert])
def get_alerts():
    return sorted(alerts_db, key=lambda x: x.id, reverse=True)

@app.post("/clear")
def clear_alerts():
    global alerts_db
    alerts_db = []
    return {"status": "success", "message": "All alerts cleared"}

@app.post("/resolve/{alert_id}")
def resolve_alert(alert_id: int):
    for alert in alerts_db:
        if alert.id == alert_id:
            alert.status = "resolved"
            return {"status": "success", "message": f"Alert {alert_id} resolved"}
    return {"status": "error", "message": "Alert not found"}

class ServiceRequest(BaseModel):
    location: str
    type: str # 'room_service', 'assistance'
    timestamp: str

@app.post("/request-service")
def request_service(request: ServiceRequest):
    global alert_counter
    new_alert = Alert(
        id=alert_counter,
        type=request.type,
        timestamp=request.timestamp,
        location=request.location,
        status="active"
    )
    alerts_db.append(new_alert)
    alert_counter += 1
    return {"status": "success", "alert_id": new_alert.id}
