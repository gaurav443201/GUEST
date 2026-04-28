package com.example.guardianhealth.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guardianhealth.data.Alert
import com.example.guardianhealth.ui.theme.*
import com.example.guardianhealth.viewmodel.HealthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
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

    // Refresh when entering this screen
    LaunchedEffect(Unit) { viewModel.fetchAlerts() }

    Scaffold(
        containerColor = NavyDeep,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Alert History", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Text("${state.alerts.size} events recorded", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchAlerts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavySurface)
            )
        }
    ) { padding ->

        if (state.isLoadingAlerts) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyanPrimary)
            }
            return@Scaffold
        }

        if (state.alerts.isEmpty()) {
            EmptyHistoryView(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary cards
            item { SummaryRow(alerts = state.alerts) }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Text(
                    "EVENT LOG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.5.sp
                )
            }

            items(state.alerts, key = { it.id }) { alert ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 3 }
                ) {
                    AlertRow(
                        alert = alert,
                        onResolve = { viewModel.resolveAlert(alert.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Summary Row ───────────────────────────────────────────────────────────────

@Composable
private fun SummaryRow(alerts: List<Alert>) {
    val emergencies = alerts.count { it.type == "emergency" }
    val services    = alerts.count { it.type == "room_service" }
    val assistance  = alerts.count { it.type == "assistance" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip("🔴 Emergency", emergencies, RedVital, Modifier.weight(1f))
        SummaryChip("🟡 Assistance", assistance, AmberVital, Modifier.weight(1f))
        SummaryChip("🟢 Service", services, GreenVital, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryChip(label: String, count: Int, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 14.sp)
        }
    }
}

// ── Alert Row ─────────────────────────────────────────────────────────────────

@Composable
private fun AlertRow(alert: Alert, onResolve: () -> Unit) {
    val (accentColor, bgColor, typeLabel, priorityLabel) = when (alert.type) {
        "emergency"    -> AlertStyle(RedVital, RedSurface, "🚨 Emergency", "HIGH PRIORITY")
        "assistance"   -> AlertStyle(AmberVital, AmberSurface, "🔔 Assistance", "MEDIUM PRIORITY")
        "room_service" -> AlertStyle(GreenVital, GreenSurface, "🛎 Room Service", "LOW PRIORITY")
        else           -> AlertStyle(TextSecondary, NavyCard, alert.type, "—")
    }
    val isResolved = alert.status == "resolved"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isResolved) 0.55f else 1f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isResolved) NavyCard else bgColor
        ),
        border = BorderStroke(
            width = if (isResolved) 1.dp else 1.5.dp,
            color = if (isResolved) NavyBorder else accentColor.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left accent bar
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(64.dp)
                        .background(
                            if (isResolved) NavyBorder else accentColor,
                            RoundedCornerShape(4.dp)
                        )
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(typeLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            color = if (isResolved) TextSecondary else TextPrimary)
                        Text(
                            "#${alert.id}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(Icons.Default.MeetingRoom, alert.room_number, accentColor, isResolved)
                        InfoChip(Icons.Default.AccessTime, formatTime(alert.timestamp), accentColor, isResolved)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (alert.heart_rate != null) {
                            InfoChip(Icons.Default.Favorite, "${alert.heart_rate} bpm", accentColor, isResolved)
                        }
                        if (alert.spo2 != null) {
                            InfoChip(Icons.Default.WaterDrop, "${alert.spo2}% SpO₂", accentColor, isResolved)
                        }
                    }
                }

                // Right: status badges
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isResolved) NavyElevated else accentColor.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            priorityLabel,
                            fontSize = 9.sp,
                            color = if (isResolved) TextMuted else accentColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(NavyElevated, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (isResolved) "✔ RESOLVED" else "● ACTIVE",
                            fontSize = 9.sp,
                            color = if (isResolved) GreenVital else accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Resolve Button (Visible only if not resolved)
            if (!isResolved) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onResolve,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("MARK AS RESOLVED", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    dimmed: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (dimmed) TextMuted else color.copy(alpha = 0.7f),
            modifier = Modifier.size(11.dp)
        )
        Text(
            text,
            fontSize = 11.sp,
            color = if (dimmed) TextMuted else TextSecondary
        )
    }
}

private data class AlertStyle(
    val accentColor: Color,
    val bgColor: Color,
    val typeLabel: String,
    val priorityLabel: String
)

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyHistoryView(modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📋", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("No Events Yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(
            "Alerts and service requests\nwill appear here in real-time.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatTime(iso: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val date = sdf.parse(iso)
        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(date!!)
    } catch (_: Exception) { iso.takeLast(8) }
}
