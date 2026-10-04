package com.aprax.htmlrun.ui.theme

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
import com.aprax.htmlrun.data.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4F9DFF),
    onPrimary = Color(0xFF06121F),
    primaryContainer = Color(0xFF12314F),
    onPrimaryContainer = Color(0xFFD6E7FF),
    secondary = Color(0xFF7B5CFF),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFC8D1E0),
    surface = Color(0xFF151922),
    onSurface = Color(0xFFC8D1E0),
    surfaceVariant = Color(0xFF1B202B),
    onSurfaceVariant = Color(0xFF93A1BB),
    surfaceContainer = Color(0xFF181D27),
    surfaceContainerHigh = Color(0xFF1E2430),
    outline = Color(0xFF2B3344),
    outlineVariant = Color(0xFF232A38),
    error = Color(0xFFFF6B6B),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B62D6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E6FF),
    onPrimaryContainer = Color(0xFF06214F),
    secondary = Color(0xFF5B3FE0),
    background = Color(0xFFF7F8FB),
    onBackground = Color(0xFF171B22),
    surface = Color.White,
    onSurface = Color(0xFF171B22),
    surfaceVariant = Color(0xFFEDF0F6),
    onSurfaceVariant = Color(0xFF4C5666),
    surfaceContainer = Color(0xFFF1F3F8),
    surfaceContainerHigh = Color(0xFFE9ECF4),
    outline = Color(0xFFC9D0DC),
    outlineVariant = Color(0xFFDDE2EB),
    error = Color(0xFFD13438),
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
fun HtmlRunnerTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = RunnerTypography,
        content = content,
    )
}
