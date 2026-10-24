package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EdenseNavy,
    onPrimary = Color.White,
    primaryContainer = EdenseNavyLight,
    onPrimaryContainer = Color.White,
    secondary = EdenseGreen,
    onSecondary = Color.White,
    secondaryContainer = EdenseGreenContainer,
    onSecondaryContainer = EdenseGreenOnContainer,
    tertiary = EdenseAmber,
    onTertiary = Color.White,
    tertiaryContainer = EdenseAmberContainer,
    onTertiaryContainer = EdenseAmberOnContainer,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF0F1E36),
    primaryContainer = EdenseNavyLight,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = EdenseGreenDark,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF78350F),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

@Composable
fun EdenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
