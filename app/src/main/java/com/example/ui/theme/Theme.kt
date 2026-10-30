package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val XmpCameraDarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = AmoledBlack,
    primaryContainer = GoldAccent.copy(alpha = 0.2f),
    onPrimaryContainer = GoldAccentLight,
    secondary = SilverAccent,
    onSecondary = AmoledBlack,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = RecordingRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = XmpCameraDarkColorScheme,
        typography = Typography,
        content = content
    )
}
