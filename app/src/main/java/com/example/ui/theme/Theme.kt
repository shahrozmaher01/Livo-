package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LivoColorScheme = darkColorScheme(
  primary = LivoViolet,
  onPrimary = TextPrimary,
  primaryContainer = DarkSurfaceElevated,
  onPrimaryContainer = LivoVioletLight,
  secondary = LivoCyan,
  onSecondary = DarkBackground,
  secondaryContainer = DarkSurfaceCard,
  onSecondaryContainer = LivoCyanLight,
  tertiary = LivoGold,
  onTertiary = DarkBackground,
  tertiaryContainer = DarkSurfaceCard,
  onTertiaryContainer = LivoGoldLight,
  background = DarkBackground,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = DarkSurfaceBorder,
  error = LivoRose,
  onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Livo is a rich dark-first social streaming app
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = LivoColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = DarkBackground.toArgb()
      window.navigationBarColor = DarkBackground.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
