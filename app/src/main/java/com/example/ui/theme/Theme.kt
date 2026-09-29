package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PadelBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = PadelVoltYellow,
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = PadelVoltYellow,
    tertiary = PadelNeonGreen,
    onTertiary = Color.White,
    background = DarkNavyBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkNavySurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkNavySurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DarkNavyBorder,
    error = PadelCoralRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PadelBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAE6FD),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = LightCourtBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightCourtSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightCourtSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = LightCourtBorder,
    error = PadelCoralRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek athletic dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
