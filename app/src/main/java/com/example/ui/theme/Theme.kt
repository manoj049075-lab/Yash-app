package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkStudioColorScheme = darkColorScheme(
    primary = ElectricCobalt,
    onPrimary = Color.White,
    primaryContainer = DeepCobalt,
    onPrimaryContainer = Color.White,
    secondary = EmeraldMint,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = EmeraldMintSoft,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = CyanGlow,
    onTertiary = Color(0xFF083344),
    background = StudioBgDark,
    onBackground = TextPrimaryDark,
    surface = StudioSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = StudioBorderDark,
    error = CoralError,
    onError = Color.White
)

private val LightStudioColorScheme = lightColorScheme(
    primary = DeepCobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = Color(0xFF0891B2),
    onTertiary = Color.White,
    background = StudioBgLight,
    onBackground = TextPrimaryLight,
    surface = StudioSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = StudioSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = StudioBorderLight,
    error = CoralError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkStudioColorScheme else LightStudioColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
