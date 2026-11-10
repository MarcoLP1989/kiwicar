package com.kiwicar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fully specified (no M3 purple defaults leaking): green brand, amber for oil, neutral greys for surfaces.
private val KiwiCarDarkColorScheme = darkColorScheme(
    primary = KiwiCarPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = KiwiCarSecondary,
    onSecondary = Color(0xFF0B2A0E),
    secondaryContainer = Color(0xFF24402A),
    onSecondaryContainer = Color(0xFFCFE8D1),
    tertiary = KiwiCarTertiary,
    onTertiary = Color(0xFF3E2700),
    tertiaryContainer = Color(0xFF4A3410),
    onTertiaryContainer = Color(0xFFFFE0B2),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB8B8B8),
    surfaceTint = KiwiCarPrimary,
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF3A3A3A),
    surfaceBright = Color(0xFF2F2F2F),
    surfaceDim = Color(0xFF121212),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF252525),
    surfaceContainerHighest = Color(0xFF2A2A2A),
)

@Composable
fun KiwiCarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KiwiCarDarkColorScheme,
        typography = KiwiCarTypography,
        content = content,
    )
}
