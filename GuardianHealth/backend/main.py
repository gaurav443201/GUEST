from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
import uuid
import os
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

app = FastAPI(
    title="Guardian Health API",
    description="Real-time health monitoring and crisis response system for hospitality environments.",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------- Models ----------

class HealthData(BaseModel):
    heart_rate: int
    spo2: int
    timestamp: str
    location: str
    room_number: str
    status: str  # 'normal', 'warning', 'emergency'

class Alert(BaseModel):
    id: int
    type: str           # 'emergency', 'room_service', 'assistance'
    priority: str       # 'high', 'medium', 'low'
    heart_rate: Optional[int] = None
    spo2: Optional[int] = None
    timestamp: str
    location: str
    room_number: str
    status: str         # 'active', 'resolved'

class ServiceRequest(BaseModel):
    location: str
    type: str           # 'room_service', 'assistance'
    timestamp: str
    room_number: str

class ApiResponse(BaseModel):
    status: str
    message: Optional[str] = None
    alert_id: Optional[int] = None

class VitalsAnalysisRequest(BaseModel):
    heart_rate: int
    spo2: int
    age: Optional[int] = 30
    symptoms: Optional[str] = "None"

class VitalsAnalysisResponse(BaseModel):
    status: str
    guest_advice: Optional[str] = None
    staff_action_plan: Optional[str] = None
    risk_level: Optional[str] = None
    message: Optional[str] = None

# ---------- In-Memory Store ----------

alerts_db: List[Alert] = []
alert_counter: int = 1

def get_priority(alert_type: str) -> str:
    return {"emergency": "high", "assistance": "medium", "room_service": "low"}.get(alert_type, "low")

# ---------- Routes ----------

@app.get("/", tags=["Health"])
def root():
    return {
        "message": "Guardian Health API v2.0 is running!",
        "docs": "/docs",
        "endpoints": ["/health-data", "/emergency", "/alerts", "/service-request", "/resolve/{id}", "/clear", "/ai/analyze"]
    }

@app.post("/ai/analyze", response_model=VitalsAnalysisResponse, tags=["AI"])
def ai_analyze_vitals(data: VitalsAnalysisRequest):
    """Use OpenAI to analyze vitals and provide advice/action plans."""
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        return VitalsAnalysisResponse(
            status="error",
            message="OpenAI API key not configured on the server."
        )

    try:
        prompt = f"""
        A hotel guest has the following vitals:
        - Heart Rate: {data.heart_rate} bpm
        - SpO2: {data.spo2}%
        - Symptoms: {data.symptoms}

        Provide a JSON response with exactly these keys:
        1. "guest_advice": A short, calming, 1-2 sentence advice directly addressing the guest.
        2. "staff_action_plan": A 3-step action plan for the hotel staff to handle this situation.
        3. "risk_level": "low", "medium", or "high" based on the severity of the vitals.
        """

        response = client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": "You are a medical AI assistant for a hotel emergency response system. Always respond in valid JSON."},
                {"role": "user", "content": prompt}
            ],
            response_format={ "type": "json_object" }
        )
        
        import json
        result = json.loads(response.choices[0].message.content)
        
        return VitalsAnalysisResponse(
            status="success",
            guest_advice=result.get("guest_advice"),
            staff_action_plan=result.get("staff_action_plan"),
            risk_level=result.get("risk_level")
        )
    except Exception as e:
        return VitalsAnalysisResponse(status="error", message=str(e))

@app.post("/health-data", response_model=ApiResponse, tags=["Vitals"])
def receive_health_data(data: HealthData):
    """Receive periodic health vitals from the mobile device."""
    return ApiResponse(status="success", message="Health data recorded successfully.")

@app.post("/emergency", response_model=ApiResponse, tags=["Emergency"])
def trigger_emergency(data: HealthData):
    """Trigger an emergency alert. Called automatically when critical vitals are detected."""
    global alert_counter
    new_alert = Alert(
        id=alert_counter,
        type="emergency",
        priority="high",
        heart_rate=data.heart_rate,
        spo2=data.spo2,
        timestamp=data.timestamp,
        location=data.location,
        room_number=data.room_number,
        status="active"
    )
    alerts_db.append(new_alert)
    alert_counter += 1
    return ApiResponse(status="success", message="Emergency alert dispatched.", alert_id=new_alert.id)

@app.get("/alerts", response_model=List[Alert], tags=["Alerts"])
def get_alerts():
    """Retrieve all alerts sorted by newest first."""
    return sorted(alerts_db, key=lambda x: x.id, reverse=True)

@app.post("/service-request", response_model=ApiResponse, tags=["Services"])
def request_service(request: ServiceRequest):
    """Submit a room service or assistance request from the guest."""
    global alert_counter
    priority = get_priority(request.type)
    new_alert = Alert(
        id=alert_counter,
        type=request.type,
        priority=priority,
        timestamp=request.timestamp,
        location=request.location,
        room_number=request.room_number,
        status="active"
    )
    alerts_db.append(new_alert)
    alert_counter += 1
    return ApiResponse(status="success", message=f"Service request received.", alert_id=new_alert.id)

@app.post("/resolve/{alert_id}", response_model=ApiResponse, tags=["Alerts"])
def resolve_alert(alert_id: int):
    """Mark a specific alert as resolved."""
    for alert in alerts_db:
        if alert.id == alert_id:
            alert.status = "resolved"
            return ApiResponse(status="success", message=f"Alert #{alert_id} marked as resolved.")
    raise HTTPException(status_code=404, detail=f"Alert #{alert_id} not found.")

@app.post("/clear", response_model=ApiResponse, tags=["Alerts"])
def clear_all_alerts():
    """Clear all alerts from the system (use for demo resets)."""
    global alerts_db, alert_counter
    alerts_db = []
    alert_counter = 1
    return ApiResponse(status="success", message="All alerts have been cleared.")

@app.get("/stats", tags=["Analytics"])
def get_stats():
    """Get summary statistics for the dashboard."""
    total = len(alerts_db)
    active = sum(1 for a in alerts_db if a.status == "active")
    emergencies = sum(1 for a in alerts_db if a.type == "emergency")
    resolved = sum(1 for a in alerts_db if a.status == "resolved")
    return {
        "total_alerts": total,
        "active_alerts": active,
        "resolved_alerts": resolved,
        "emergency_count": emergencies,
        "service_requests": sum(1 for a in alerts_db if a.type == "room_service"),
        "assistance_requests": sum(1 for a in alerts_db if a.type == "assistance"),
    }
