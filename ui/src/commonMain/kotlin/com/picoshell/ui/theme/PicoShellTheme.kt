package com.picoshell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightPalette = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFF8FBFA),
    secondary = Color(0xFFD97706),
    onSecondary = Color(0xFF221407),
    tertiary = Color(0xFF8B5E34),
    background = Color(0xFFF4EDE1),
    onBackground = Color(0xFF162238),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF162238),
    surfaceVariant = Color(0xFFE7D9C8),
    onSurfaceVariant = Color(0xFF4B5563),
    error = Color(0xFF9A3412),
)

private val DarkPalette = darkColorScheme(
    primary = Color(0xFF4FD1C5),
    onPrimary = Color(0xFF082F2F),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFF3B2200),
    tertiary = Color(0xFFD6A15B),
    background = Color(0xFF111827),
    onBackground = Color(0xFFF8F3EB),
    surface = Color(0xFF182132),
    onSurface = Color(0xFFF8F3EB),
    surfaceVariant = Color(0xFF293242),
    onSurfaceVariant = Color(0xFFD1D5DB),
    error = Color(0xFFF97316),
)

private val PicoShellTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

@Composable
fun PicoShellTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkPalette else LightPalette,
        typography = PicoShellTypography,
        content = content,
    )
}
