package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = GrokAccentCyan,
  onPrimary = Color.Black,
  primaryContainer = Color(0xFF034A64),
  onPrimaryContainer = Color(0xFFBCE9FF),
  secondary = GrokAccentPurple,
  onSecondary = Color.White,
  tertiary = GrokAccentGlow,
  background = GrokBlack,
  onBackground = GrokTextPrimary,
  surface = GrokDarkSurface,
  onSurface = GrokTextPrimary,
  surfaceVariant = GrokCardSurface,
  onSurfaceVariant = GrokTextSecondary,
  outline = GrokBorder,
  error = GrokError,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF0284C7),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = Color(0xFF0369A1),
  secondary = Color(0xFF7C3AED),
  onSecondary = Color.White,
  background = Color(0xFFF8FAFC),
  onBackground = Color(0xFF0F172A),
  surface = Color.White,
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = Color(0xFF475569),
  outline = Color(0xFFCBD5E1),
  error = GrokError,
  onError = Color.White
)

@Composable
fun GrokAppTheme(
  darkTheme: Boolean = true, // Default to sleek Grok dark mode
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
