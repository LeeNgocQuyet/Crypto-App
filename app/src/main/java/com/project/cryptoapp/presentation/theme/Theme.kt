package com.project.cryptoapp.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberPrimary,
    secondary = CyberSecondary,
    tertiary = CyberTertiary,
    background = CyberBackground,
    surface = CyberSurface,
    surfaceVariant = CyberSurfaceVariant,
    error = CyberError,
    onPrimary = Color(0xFF001018),
    onSecondary = Color.White,
    onTertiary = Color(0xFF00140B),
    onBackground = CyberTextPrimary,
    onSurface = CyberTextPrimary,
    onSurfaceVariant = CyberTextSecondary,
    onError = Color.White,
)

@Composable
fun ECC512CryptoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
