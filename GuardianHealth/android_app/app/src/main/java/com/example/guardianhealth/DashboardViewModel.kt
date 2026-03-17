package com.example.guardianhealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel : ViewModel() {

    private val _heartRate = MutableStateFlow(75)
    val heartRate: StateFlow<Int> = _heartRate

    private val _spo2 = MutableStateFlow(98)
    val spo2: StateFlow<Int> = _spo2

    private val _status = MutableStateFlow("Normal")
    val status: StateFlow<String> = _status // Normal, Warning, Emergency

    private val _emergencyDetected = MutableStateFlow(false)
    val emergencyDetected: StateFlow<Boolean> = _emergencyDetected

    private val location = "Room 203"

    init {
        startSimulation()
    }

    private fun startSimulation() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_emergencyDetected.value) {
                    // Small random fluctuations in normal mode
                    if (Math.random() > 0.5) {
                        val shift = listOf(-1, 0, 1).random()
                        _heartRate.value += shift
                    }
                }
                checkVitals()
            }
        }
    }

    private fun checkVitals() {
        val hr = _heartRate.value
        val o2 = _spo2.value

        if (hr > 150 || o2 < 85) {
            if (!_emergencyDetected.value) {
                _status.value = "Emergency"
                _emergencyDetected.value = true
                sendEmergencyData(hr, o2)
            }
        } else if (hr > 120 || o2 < 90) {
            _status.value = "Warning"
        } else {
            _status.value = "Normal"
        }
    }

    fun setNormalMode() {
        _heartRate.value = (70..80).random()
        _spo2.value = (97..99).random()
        _emergencyDetected.value = false
        _status.value = "Normal"
    }

    fun increaseHeartRate() {
        viewModelScope.launch {
            for (i in 1..20) {
                if (_heartRate.value < 160) {
                    _heartRate.value += 4
                }
                delay(200)
                checkVitals()
            }
        }
    }

    fun decreaseSpO2() {
        viewModelScope.launch {
            for (i in 1..10) {
                if (_spo2.value > 80) {
                    _spo2.value -= 2
                }
                delay(300)
                checkVitals()
            }
        }
    }

    fun triggerEmergency() {
        _heartRate.value = 155
        _spo2.value = 84
        checkVitals()
    }

    private fun sendEmergencyData(hr: Int, o2: Int) {
        viewModelScope.launch {
            try {
                val data = HealthData(
                    heart_rate = hr,
                    spo2 = o2,
                    timestamp = getCurrentTime(),
                    location = location
                )
                RetrofitClient.instance.triggerEmergency(data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun requestRoomService() {
        viewModelScope.launch {
            try {
                val data = ServiceRequest(
                    location = location,
                    type = "room_service",
                    timestamp = getCurrentTime()
                )
                RetrofitClient.instance.requestService(data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun callAssistance() {
        viewModelScope.launch {
            try {
                val data = ServiceRequest(
                    location = location,
                    type = "assistance",
                    timestamp = getCurrentTime()
                )
                RetrofitClient.instance.requestService(data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
