package com.example.guardianhealth.data

data class HealthData(
    val heart_rate: Int,
    val spo2: Int,
    val timestamp: String,
    val location: String,
    val room_number: String,
    val status: String
)

data class Alert(
    val id: Int,
    val type: String,           // 'emergency', 'room_service', 'assistance'
    val priority: String,       // 'high', 'medium', 'low'
    val heart_rate: Int?,
    val spo2: Int?,
    val timestamp: String,
    val location: String,
    val room_number: String,
    val status: String          // 'active', 'resolved'
)

data class ServiceRequest(
    val location: String,
    val type: String,
    val timestamp: String,
    val room_number: String
)

data class ApiResponse(
    val status: String,
    val message: String? = null,
    val alert_id: Int? = null
)

data class Stats(
    val total_alerts: Int,
    val active_alerts: Int,
    val resolved_alerts: Int,
    val emergency_count: Int,
    val service_requests: Int,
    val assistance_requests: Int
)
