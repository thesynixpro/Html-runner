package com.openprojects.htmlrunner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val RunnerColorScheme = darkColorScheme(
    primary = Color(0xFF4F9DFF),
    onPrimary = Color(0xFF06121F),
    secondary = Color(0xFF7B5CFF),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFC8D1E0),
    surface = Color(0xFF151922),
    onSurface = Color(0xFFC8D1E0),
    surfaceVariant = Color(0xFF1B202B),
    onSurfaceVariant = Color(0xFF93A1BB),
    outline = Color(0xFF2B3344),
    error = Color(0xFFFF6B6B),
)

private val RunnerTypography = Typography(
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = Color(0xFF7A869C),
    ),
)

@Composable
fun HtmlRunnerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RunnerColorScheme,
        typography = RunnerTypography,
        content = content,
    )
}