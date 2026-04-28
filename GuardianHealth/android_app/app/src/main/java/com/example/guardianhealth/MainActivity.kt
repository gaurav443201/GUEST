package com.example.guardianhealth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.guardianhealth.ui.DashboardScreen
import com.example.guardianhealth.ui.HistoryScreen
import com.example.guardianhealth.ui.theme.GuardianHealthTheme
import com.example.guardianhealth.viewmodel.HealthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuardianHealthTheme {
                GuardianHealthApp()
            }
        }
    }
}

@Composable
fun GuardianHealthApp() {
    val navController = rememberNavController()
    // Single shared ViewModel scoped to the NavHost
    val sharedViewModel: HealthViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                viewModel = sharedViewModel,
                onNavigateToHistory = { navController.navigate("history") }
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
