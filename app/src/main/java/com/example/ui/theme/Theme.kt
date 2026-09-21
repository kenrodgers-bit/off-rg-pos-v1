package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RgAccent,
    onPrimary = DarkBg,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = RgAccentLight,
    secondary = RgAccentDark,
    onSecondary = TextWhite,
    secondaryContainer = DarkSurfaceCard,
    onSecondaryContainer = TextWhite,
    tertiary = CreditBlue,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = TextMuted,
    outline = DarkBorder,
    outlineVariant = DarkDivider,
    error = AlertRed,
    onError = TextWhite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // RG POS is strictly a futuristic dark theme app per Part 1 design system specifications
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
