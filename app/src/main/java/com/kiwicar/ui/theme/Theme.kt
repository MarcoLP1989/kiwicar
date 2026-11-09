package com.kiwicar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KiwiDarkColorScheme = darkColorScheme(
    primary = KiwiPrimary,
    secondary = KiwiSecondary,
    tertiary = KiwiTertiary,
)

@Composable
fun KiwiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KiwiDarkColorScheme,
        typography = KiwiTypography,
        content = content,
    )
}
