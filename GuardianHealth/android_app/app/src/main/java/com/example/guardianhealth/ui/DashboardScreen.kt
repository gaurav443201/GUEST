package com.example.guardianhealth.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guardianhealth.ui.theme.*
import com.example.guardianhealth.viewmodel.*
import kotlinx.coroutines.delay
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToHistory: () -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar trigger
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val bgColor by animateColorAsState(
        targetValue = when (state.status) {
            HealthStatus.EMERGENCY -> Color(0xFF1A0000)
            HealthStatus.WARNING   -> Color(0xFF1A1000)
            HealthStatus.NORMAL    -> NavyDeep
        },
        animationSpec = tween(600),
        label = "bg"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = bgColor
    ) { padding ->

        // Emergency full-screen overlay
        AnimatedVisibility(
            visible = state.isEmergencyDialogVisible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            EmergencyOverlay(
                heartRate = state.heartRate,
                spo2 = state.spo2,
                onDismiss = { viewModel.dismissEmergencyDialog() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DashboardHeader(onNavigateToHistory = onNavigateToHistory)
            PatientCard()
            StatusBanner(status = state.status, heartRate = state.heartRate, spo2 = state.spo2)
            HeartRateCard(
                heartRate = state.heartRate,
                waveformPoints = state.waveformPoints,
                status = state.status
            )
            SpO2Card(spo2 = state.spo2, status = state.status)
            SimulationControls(
                currentMode = state.mode,
                onNormal    = { viewModel.setMode(SimulationMode.NORMAL) },
                onHighHR    = { viewModel.setMode(SimulationMode.HIGH_HR) },
                onLowSpO2   = { viewModel.setMode(SimulationMode.LOW_SPO2) },
                onEmergency = { viewModel.setMode(SimulationMode.EMERGENCY) }
            )
            ServiceButtons(
                onRoomService = { viewModel.sendServiceRequest("room_service") },
                onAssistance  = { viewModel.sendServiceRequest("assistance") }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(onNavigateToHistory: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Guardian Health",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CyanPrimary,
                letterSpacing = 0.5.sp
            )
            Text(
                "Real-time Patient Monitoring",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        IconButton(
            onClick = onNavigateToHistory,
            modifier = Modifier
                .background(NavyCard, CircleShape)
                .size(42.dp)
        ) {
            Icon(Icons.Default.List, contentDescription = "History", tint = CyanPrimary)
        }
    }
}

// ── Patient Card ──────────────────────────────────────────────────────────────

@Composable
private fun PatientCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(BlueAccent, CyanDim)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    HealthViewModel.PATIENT_NAME,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    "${HealthViewModel.ROOM_NUMBER}  •  ${HealthViewModel.LOCATION}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("ID: GH-2031", fontSize = 10.sp, color = TextMuted)
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .background(GreenSurface, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("● ACTIVE", fontSize = 10.sp, color = GreenVital, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Status Banner ─────────────────────────────────────────────────────────────

@Composable
private fun StatusBanner(status: HealthStatus, heartRate: Int, spo2: Int) {
    val (bgColor, borderColor, icon, label, sublabel) = when (status) {
        HealthStatus.NORMAL    -> StatusData(GreenSurface, GreenBorder, Icons.Default.CheckCircle, "All Vitals Normal", "Patient is stable")
        HealthStatus.WARNING   -> StatusData(AmberSurface, AmberBorder, Icons.Default.Warning, "Warning Detected", "Vitals approaching critical range")
        HealthStatus.EMERGENCY -> StatusData(RedSurface, RedBorder, Icons.Default.Error, "⚠ EMERGENCY", "Immediate attention required")
    }
    val textColor = when (status) {
        HealthStatus.NORMAL    -> GreenVital
        HealthStatus.WARNING   -> AmberVital
        HealthStatus.EMERGENCY -> RedVital
    }

    val shake by rememberInfiniteTransition(label = "shake").animateFloat(
        initialValue = 0f,
        targetValue = if (status == HealthStatus.EMERGENCY) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(80, easing = LinearEasing), RepeatMode.Reverse),
        label = "shakeVal"
    )
    val offsetX = if (status == HealthStatus.EMERGENCY) (shake * 6f - 3f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                Text(sublabel, fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("HR: $heartRate bpm", fontSize = 11.sp, color = textColor)
                Text("SpO₂: $spo2%", fontSize = 11.sp, color = textColor)
            }
        }
    }
}

private data class StatusData(
    val bgColor: Color,
    val borderColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val sublabel: String
)

// ── Heart Rate Card with ECG Waveform ─────────────────────────────────────────

@Composable
private fun HeartRateCard(heartRate: Int, waveformPoints: List<Float>, status: HealthStatus) {
    val waveColor = when (status) {
        HealthStatus.NORMAL    -> WaveformGreen
        HealthStatus.WARNING   -> WaveformAmber
        HealthStatus.EMERGENCY -> WaveformRed
    }
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween((60000 / heartRate.coerceAtLeast(40)), easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = waveColor,
                        modifier = Modifier.size(20.dp).scale(pulseScale)
                    )
                    Text("Heart Rate", fontSize = 13.sp, color = TextSecondary)
                }
                Text("ECG SIMULATION", fontSize = 10.sp, color = TextMuted, letterSpacing = 1.sp)
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    heartRate.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = waveColor,
                    lineHeight = 48.sp
                )
                Text("bpm", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
            }

            Spacer(Modifier.height(12.dp))

            // ECG waveform canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h * 0.6f
                val pts = waveformPoints
                if (pts.size < 2) return@Canvas

                // Grid lines
                val gridPaint = Paint().apply { color = NavyBorder }
                for (i in 0..4) {
                    val y = h * i / 4
                    drawLine(NavyBorder, Offset(0f, y), Offset(w, y), 0.5f)
                }

                // Waveform path
                val path = Path()
                pts.forEachIndexed { idx, value ->
                    val x = idx.toFloat() / (pts.size - 1) * w
                    val y = midY - (value * midY * 0.85f)
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                // Glow effect (thick, transparent)
                drawPath(path, color = waveColor.copy(alpha = 0.2f), style = Stroke(width = 6f, cap = StrokeCap.Round))
                // Actual line
                drawPath(path, color = waveColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

                // Moving scan line
                val scanX = (pts.size - 1).toFloat() / (pts.size - 1) * w
                drawLine(waveColor.copy(alpha = 0.5f), Offset(scanX - 2f, 0f), Offset(scanX - 2f, h), 1f)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("NORMAL: 60-100 bpm", fontSize = 10.sp, color = TextMuted)
                val hrStatus = when {
                    heartRate > 150 -> "CRITICAL"
                    heartRate > 100 -> "TACHYCARDIA"
                    heartRate < 60  -> "BRADYCARDIA"
                    else            -> "NORMAL SINUS"
                }
                Text(hrStatus, fontSize = 10.sp, color = waveColor, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ── SpO2 Card ─────────────────────────────────────────────────────────────────

@Composable
private fun SpO2Card(spo2: Int, status: HealthStatus) {
    val valueColor = when {
        spo2 < 85  -> RedVital
        spo2 < 95  -> AmberVital
        else       -> GreenVital
    }
    val animatedSpo2 by animateFloatAsState(
        targetValue = spo2.toFloat(),
        animationSpec = tween(600),
        label = "spo2"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular gauge
            Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(90.dp)) {
                    val stroke = 8f
                    val inset = stroke / 2
                    // Background arc
                    drawArc(
                        color = NavyBorder,
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke)
                    )
                    // Value arc
                    val sweep = (animatedSpo2 / 100f) * 260f
                    drawArc(
                        brush = Brush.sweepGradient(listOf(valueColor.copy(alpha = 0.6f), valueColor)),
                        startAngle = 140f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${spo2}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = valueColor
                    )
                    Text("SpO₂", fontSize = 9.sp, color = TextMuted)
                }
            }

            // Details
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Blood Oxygen Saturation", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Text("Pulse Oximetry Simulation", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                val spo2Label = when {
                    spo2 < 85  -> "HYPOXIA — CRITICAL"
                    spo2 < 90  -> "SEVERE HYPOXEMIA"
                    spo2 < 95  -> "MILD HYPOXEMIA"
                    else       -> "NORMAL"
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = valueColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(spo2Label, fontSize = 10.sp, color = valueColor, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Text("Normal range: 95% – 100%", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

// ── Simulation Controls ───────────────────────────────────────────────────────

@Composable
private fun SimulationControls(
    currentMode: SimulationMode,
    onNormal: () -> Unit,
    onHighHR: () -> Unit,
    onLowSpO2: () -> Unit,
    onEmergency: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                Text("Simulation Controls", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            }
            Text("Select a scenario to simulate patient vitals", fontSize = 11.sp, color = TextSecondary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimButton(
                    label = "Normal",
                    icon = "🟢",
                    selected = currentMode == SimulationMode.NORMAL,
                    color = GreenVital,
                    modifier = Modifier.weight(1f),
                    onClick = onNormal
                )
                SimButton(
                    label = "High HR",
                    icon = "⚡",
                    selected = currentMode == SimulationMode.HIGH_HR,
                    color = AmberVital,
                    modifier = Modifier.weight(1f),
                    onClick = onHighHR
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimButton(
                    label = "Low SpO₂",
                    icon = "💧",
                    selected = currentMode == SimulationMode.LOW_SPO2,
                    color = AmberVital,
                    modifier = Modifier.weight(1f),
                    onClick = onLowSpO2
                )
                SimButton(
                    label = "Emergency",
                    icon = "🚨",
                    selected = currentMode == SimulationMode.EMERGENCY,
                    color = RedVital,
                    modifier = Modifier.weight(1f),
                    onClick = onEmergency
                )
            }
        }
    }
}

@Composable
private fun SimButton(
    label: String,
    icon: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (selected) color.copy(alpha = 0.2f) else NavyElevated
    val borderColor = if (selected) color else NavyBorder

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = bgColor,
            contentColor = if (selected) color else TextSecondary
        ),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Text(
            "$icon  $label",
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

// ── Service Buttons ───────────────────────────────────────────────────────────

@Composable
private fun ServiceButtons(onRoomService: () -> Unit, onAssistance: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "GUEST SERVICES",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ServiceCard(
                icon = Icons.Default.RoomService,
                label = "Room Service",
                sublabel = "Low Priority",
                accentColor = GreenVital,
                modifier = Modifier.weight(1f),
                onClick = onRoomService
            )
            ServiceCard(
                icon = Icons.Default.SupportAgent,
                label = "Call Assistance",
                sublabel = "Med Priority",
                accentColor = AmberVital,
                modifier = Modifier.weight(1f),
                onClick = onAssistance
            )
        }
    }
}

@Composable
private fun ServiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sublabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(sublabel, fontSize = 10.sp, color = accentColor)
        }
    }
}

// ── Emergency Overlay ─────────────────────────────────────────────────────────

@Composable
fun EmergencyOverlay(heartRate: Int, spo2: Int, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergency")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE1A0000))
            .zIndex(10f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .scale(scale)
                .padding(32.dp)
        ) {
            Text("🚨", fontSize = 64.sp)
            Text(
                "EMERGENCY DETECTED",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RedGlow,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "Critical patient vitals detected.\nStaff has been notified.",
                fontSize = 14.sp,
                color = TextPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Vital readouts
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$heartRate", fontSize = 36.sp, fontWeight = FontWeight.Black, color = RedVital)
                    Text("bpm", fontSize = 12.sp, color = TextSecondary)
                    Text("Heart Rate", fontSize = 10.sp, color = TextMuted)
                }
                Box(modifier = Modifier.width(1.dp).height(60.dp).background(NavyBorder))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$spo2%", fontSize = 36.sp, fontWeight = FontWeight.Black, color = RedVital)
                    Text("oxygen", fontSize = 12.sp, color = TextSecondary)
                    Text("SpO₂ Level", fontSize = 10.sp, color = TextMuted)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedSurface,
                    contentColor = RedVital
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, RedVital),
                modifier = Modifier.fillMaxWidth(0.6f).height(48.dp)
            ) {
                Text("ACKNOWLEDGE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 13.sp)
            }
        }
    }
}
