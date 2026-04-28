package com.example.guardianhealth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardianhealth.data.Alert
import com.example.guardianhealth.data.HealthData
import com.example.guardianhealth.data.ServiceRequest
import com.example.guardianhealth.data.VitalsAnalysisRequest
import com.example.guardianhealth.data.VitalsAnalysisResponse
import com.example.guardianhealth.network.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

enum class HealthStatus { NORMAL, WARNING, EMERGENCY }

enum class SimulationMode { NORMAL, HIGH_HR, LOW_SPO2, EMERGENCY }

data class HealthUiState(
    val heartRate: Int = 75,
    val spo2: Int = 98,
    val status: HealthStatus = HealthStatus.NORMAL,
    val mode: SimulationMode = SimulationMode.NORMAL,
    val isEmergencyDialogVisible: Boolean = false,
    val waveformPoints: List<Float> = List(60) { 0f },
    val alerts: List<Alert> = emptyList(),
    val isLoadingAlerts: Boolean = false,
    val snackbarMessage: String? = null,
    val previousHeartRate: Int = 75,
    val aiAnalysisResult: VitalsAnalysisResponse? = null,
    val isAiAnalyzing: Boolean = false
)

class HealthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState

    companion object {
        const val PATIENT_NAME = "Demo Guest"
        const val ROOM_NUMBER = "Room 203"
        const val LOCATION = "Floor 2 - Wing B"
    }

    private var simulationJob: Job? = null
    private var waveformJob: Job? = null
    private var alertPollingJob: Job? = null
    private val waveformBuffer = ArrayDeque<Float>(60)
    private var emergencyAlreadySent = false

    init {
        repeat(60) { waveformBuffer.addLast(0f) }
        startWaveformGeneration()
        setMode(SimulationMode.NORMAL)
        startAlertPolling()
    }

    // ─── Waveform ────────────────────────────────────────────────────────────

    private fun startWaveformGeneration() {
        waveformJob?.cancel()
        waveformJob = viewModelScope.launch {
            var tick = 0
            while (true) {
                val hr = _uiState.value.heartRate
                val point = ecgPoint(tick, hr)
                if (waveformBuffer.size >= 60) waveformBuffer.removeFirst()
                waveformBuffer.addLast(point)
                
                val pointsList = waveformBuffer.toList()
                _uiState.update { it.copy(waveformPoints = pointsList) }
                
                tick++
                delay(80L)
            }
        }
    }

    /** Generates a synthetic ECG-like waveform point (P, QRS complex, T wave) */
    private fun ecgPoint(tick: Int, hr: Int): Float {
        val period = (60.0 / hr * 1000 / 80).toInt().coerceAtLeast(1)
        val phase = tick % period
        val norm = phase.toFloat() / period
        return when {
            norm < 0.05f -> norm / 0.05f * 0.15f               // P wave rise
            norm < 0.10f -> (0.10f - norm) / 0.05f * 0.15f     // P wave fall
            norm < 0.15f -> 0f                                   // PR segment
            norm < 0.20f -> -(norm - 0.15f) / 0.05f * 0.25f    // Q wave
            norm < 0.25f -> (norm - 0.20f) / 0.05f * 1.0f      // R wave rise
            norm < 0.30f -> 1.0f - (norm - 0.25f) / 0.05f * 1.2f // R-S fall
            norm < 0.35f -> -0.2f + (norm - 0.30f) / 0.05f * 0.2f // S wave
            norm < 0.50f -> 0f                                   // ST segment
            norm < 0.60f -> (norm - 0.50f) / 0.10f * 0.3f      // T wave rise
            norm < 0.70f -> (0.70f - norm) / 0.10f * 0.3f      // T wave fall
            else -> 0f
        }
    }

    // ─── Simulation Modes ────────────────────────────────────────────────────

    fun setMode(mode: SimulationMode) {
        emergencyAlreadySent = false
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            when (mode) {
                SimulationMode.NORMAL    -> runNormal()
                SimulationMode.HIGH_HR   -> runHighHR()
                SimulationMode.LOW_SPO2  -> runLowSpO2()
                SimulationMode.EMERGENCY -> runEmergency()
            }
        }
    }

    private suspend fun runNormal() {
        var hr = _uiState.value.heartRate
        var spo2 = _uiState.value.spo2
        // Smoothly return to normal range
        while (hr > 82 || spo2 < 96) {
            if (hr > 82) hr -= 3
            if (spo2 < 96) spo2++
            applyVitals(hr.coerceIn(60, 200), spo2.coerceIn(70, 100), SimulationMode.NORMAL)
            delay(250L)
        }
        // Maintain normal with micro-variation
        while (true) {
            hr = Random.nextInt(70, 81)
            spo2 = Random.nextInt(97, 100)
            applyVitals(hr, spo2, SimulationMode.NORMAL)
            delay(2000L)
        }
    }

    private suspend fun runHighHR() {
        var hr = _uiState.value.heartRate
        val target = Random.nextInt(145, 161)
        // Gradual ramp-up
        while (hr < target) {
            hr += 4
            applyVitals(hr.coerceIn(60, 200), _uiState.value.spo2, SimulationMode.HIGH_HR)
            delay(180L)
        }
        // Sustain with variation
        while (true) {
            hr = target + Random.nextInt(-4, 5)
            applyVitals(hr, _uiState.value.spo2, SimulationMode.HIGH_HR)
            delay(1200L)
        }
    }

    private suspend fun runLowSpO2() {
        var spo2 = _uiState.value.spo2
        val target = Random.nextInt(80, 86)
        // Gradual drop
        while (spo2 > target) {
            spo2--
            applyVitals(_uiState.value.heartRate, spo2, SimulationMode.LOW_SPO2)
            delay(350L)
        }
        // Sustain with variation
        while (true) {
            spo2 = target + Random.nextInt(-1, 2)
            applyVitals(_uiState.value.heartRate, spo2.coerceIn(70, 100), SimulationMode.LOW_SPO2)
            delay(1500L)
        }
    }

    private suspend fun runEmergency() {
        var hr = _uiState.value.heartRate
        var spo2 = _uiState.value.spo2
        val targetHR = Random.nextInt(155, 175)
        val targetSpO2 = Random.nextInt(78, 84)
        // Rapid deterioration
        while (hr < targetHR || spo2 > targetSpO2) {
            if (hr < targetHR) hr += 6
            if (spo2 > targetSpO2) spo2 -= 2
            applyVitals(hr.coerceIn(60, 220), spo2.coerceIn(70, 100), SimulationMode.EMERGENCY)
            delay(120L)
        }
        while (true) {
            hr = targetHR + Random.nextInt(-3, 8)
            spo2 = targetSpO2 + Random.nextInt(-2, 2)
            applyVitals(hr.coerceIn(60, 220), spo2.coerceIn(70, 100), SimulationMode.EMERGENCY)
            delay(900L)
        }
    }

    // ─── Vitals + Status Logic ────────────────────────────────────────────────

    private fun applyVitals(hr: Int, spo2: Int, mode: SimulationMode) {
        val prevHR = _uiState.value.heartRate
        val status = determineStatus(hr, spo2, prevHR)
        val isEmergency = status == HealthStatus.EMERGENCY

        _uiState.update { currentState ->
            currentState.copy(
                heartRate = hr,
                spo2 = spo2,
                status = status,
                mode = mode,
                previousHeartRate = prevHR,
                isEmergencyDialogVisible = if (isEmergency) true else currentState.isEmergencyDialogVisible
            )
        }

        // Send to backend only once per emergency event
        if (isEmergency && !emergencyAlreadySent) {
            emergencyAlreadySent = true
            dispatchEmergency(hr, spo2)
        }
        if (!isEmergency) emergencyAlreadySent = false
    }

    /** Anomaly detection: sudden HR jump, low SpO2, or critical thresholds */
    private fun determineStatus(hr: Int, spo2: Int, prevHR: Int): HealthStatus {
        val suddenSpike = abs(hr - prevHR) > 40
        return when {
            hr > 150 || spo2 < 85 || suddenSpike -> HealthStatus.EMERGENCY
            hr > 100 || hr < 55 || spo2 < 95     -> HealthStatus.WARNING
            else                                   -> HealthStatus.NORMAL
        }
    }

    // ─── Backend Calls ────────────────────────────────────────────────────────

    private fun dispatchEmergency(hr: Int, spo2: Int) {
        viewModelScope.launch {
            try {
                ApiClient.api.postEmergency(
                    HealthData(
                        heart_rate = hr,
                        spo2 = spo2,
                        timestamp = nowIso(),
                        location = LOCATION,
                        room_number = ROOM_NUMBER,
                        status = "emergency"
                    )
                )
                fetchAlerts()
            } catch (_: Exception) { /* silent in demo */ }
        }
    }

    fun sendServiceRequest(type: String) {
        val label = if (type == "room_service") "Room Service" else "Assistance"
        viewModelScope.launch {
            try {
                ApiClient.api.requestService(
                    ServiceRequest(
                        location = LOCATION,
                        type = type,
                        timestamp = nowIso(),
                        room_number = ROOM_NUMBER
                    )
                )
                _uiState.update { it.copy(snackbarMessage = "✅ $label request sent to staff.") }
                fetchAlerts()
            } catch (_: Exception) {
                _uiState.update { it.copy(snackbarMessage = "⚠️ Could not reach server. Check connection.") }
            }
        }
    }

    fun fetchAlerts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAlerts = true) }
            try {
                val resp = ApiClient.api.getAlerts()
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(
                        alerts = resp.body() ?: emptyList(),
                        isLoadingAlerts = false
                    ) }
                } else {
                    _uiState.update { it.copy(isLoadingAlerts = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingAlerts = false) }
            }
        }
    }

    fun resolveAlert(alertId: Int) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.api.resolveAlert(alertId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(snackbarMessage = "✅ Alert #$alertId marked as resolved.") }
                    fetchAlerts()
                } else {
                    _uiState.update { it.copy(snackbarMessage = "❌ Failed to resolve alert.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "⚠️ Error: ${e.message}") }
            }
        }
    }

    private fun startAlertPolling() {
        alertPollingJob?.cancel()
        alertPollingJob = viewModelScope.launch {
            while (true) {
                fetchAlerts()
                delay(5000L)
            }
        }
    }

    fun analyzeVitalsWithAi() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiAnalyzing = true) }
            try {
                val resp = ApiClient.api.postAiAnalyze(
                    VitalsAnalysisRequest(
                        heart_rate = _uiState.value.heartRate,
                        spo2 = _uiState.value.spo2,
                        symptoms = "None"
                    )
                )
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(
                        aiAnalysisResult = resp.body(),
                        isAiAnalyzing = false
                    ) }
                } else {
                    _uiState.update { it.copy(
                        isAiAnalyzing = false,
                        snackbarMessage = "AI Analysis failed."
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isAiAnalyzing = false,
                    snackbarMessage = "AI Analysis failed: ${e.message}"
                ) }
            }
        }
    }

    fun dismissAiAnalysis() {
        _uiState.update { it.copy(aiAnalysisResult = null) }
    }

    fun dismissEmergencyDialog() {
        _uiState.update { it.copy(isEmergencyDialogVisible = false) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        waveformJob?.cancel()
        alertPollingJob?.cancel()
    }
}
