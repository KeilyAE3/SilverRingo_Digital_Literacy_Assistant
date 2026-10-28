package com.silverringo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SilverColors = lightColorScheme(
    primary = Color(0xFF211D4F),
    onPrimary = Color.White,
    secondary = Color(0xFF006C94),
    onSecondary = Color.White,
    tertiary = Color(0xFFFFC928),
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onBackground = Color(0xFF171C30),
    onSurface = Color(0xFF171C30),
    onSurfaceVariant = Color(0xFF4F6078)
)

@Composable
fun SilverRingoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SilverColors, typography = Typography, content = content)
}
