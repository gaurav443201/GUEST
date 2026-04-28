package com.example.guardianhealth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MedicalDarkColorScheme = darkColorScheme(
    primary          = CyanPrimary,
    onPrimary        = NavyDeep,
    primaryContainer = NavyCard,
    secondary        = GreenVital,
    onSecondary      = NavyDeep,
    background       = NavyDeep,
    surface          = NavySurface,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    error            = RedVital,
    onError          = TextPrimary,
    outline          = NavyBorder
)

@Composable
fun GuardianHealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MedicalDarkColorScheme,
        content = content
    )
}
