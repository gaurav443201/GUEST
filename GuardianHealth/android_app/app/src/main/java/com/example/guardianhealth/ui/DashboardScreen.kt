package com.example.guardianhealth.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guardianhealth.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val heartRate by viewModel.heartRate.collectAsState()
    val spo2 by viewModel.spo2.collectAsState()
    val status by viewModel.status.collectAsState()
    val isEmergency by viewModel.emergencyDetected.collectAsState()

    val backgroundColor = when (status) {
        "Normal" -> Color(0xFFF4F9F4)
        "Warning" -> Color(0xFFFFFDF2)
        "Emergency" -> Color(0xFFFFF0F0)
        else -> Color.White
    }

    val statusColor = when (status) {
        "Normal" -> Color(0xFF4CAF50)
        "Warning" -> Color(0xFFFFC107)
        "Emergency" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Guardian Health",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        // Status Indicator
        Card(
            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Status: $status",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        if (isEmergency) {
            Text(
                text = "EMERGENCY DETECTED! Alert Sent.",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Vitals Section
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            VitalCard("Heart Rate", "$heartRate BPM", statusColor, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            VitalCard("SpO2", "$spo2%", statusColor, Modifier.weight(1f))
        }

        // Animated Graph
        HeartBeatGraph(heartRate, statusColor)

        Spacer(modifier = Modifier.height(24.dp))

        // Simulation Controls
        Text("Simulation Controls", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { viewModel.setNormalMode() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Normal") }
            Button(onClick = { viewModel.increaseHeartRate() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) { Text("HR ↑") }
            Button(onClick = { viewModel.decreaseSpO2() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) { Text("SpO2 ↓") }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.triggerEmergency() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Trigger Emergency") }
        
        Spacer(modifier = Modifier.weight(1f))

        // Assistance Controls
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = { viewModel.requestRoomService() }) { Text("Request Room Service") }
            OutlinedButton(onClick = { viewModel.callAssistance() }) { Text("Call Assistance") }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun VitalCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 16.sp, color = Color.Gray)
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun HeartBeatGraph(heartRate: Int, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val speed = (60f / heartRate.coerceAtLeast(40)) * 1000 // duration of one beat in ms
    
    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(speed.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val width = size.width
            val height = size.height
            val cy = height / 2

            val waveWidth = width / 3
            val currentX = (xOffset * width) % width

            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(0f, cy),
                end = Offset(width, cy),
                strokeWidth = 2f
            )

            // Draw a simplified ECG waveform shape
            val path = androidx.compose.ui.graphics.Path()
            var startDrawX = currentX - waveWidth
            if (startDrawX < 0) startDrawX += width

            path.moveTo(currentX, cy)
            path.lineTo(currentX + 10, cy)
            path.lineTo(currentX + 25, cy - height/3)
            path.lineTo(currentX + 40, cy + height/2)
            path.lineTo(currentX + 55, cy - height/4)
            path.lineTo(currentX + 70, cy)
            path.lineTo(currentX + waveWidth, cy)

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
        }
    }
}
