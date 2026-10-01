package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MafiaCrimson,
    onPrimary = TextPrimaryDark,
    primaryContainer = MafiaCrimsonDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = MafiaGold,
    onSecondary = MafiaDarkBg,
    secondaryContainer = MafiaSurfaceVariant,
    onSecondaryContainer = MafiaGold,
    tertiary = SuspicionGreen,
    onTertiary = TextPrimaryDark,
    background = MafiaDarkBg,
    onBackground = TextPrimaryDark,
    surface = MafiaCardBg,
    onSurface = TextPrimaryDark,
    surfaceVariant = MafiaSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = MafiaBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryRed,
    onPrimary = TextPrimaryDark,
    primaryContainer = Color(0xFFFFCDD2),
    onPrimaryContainer = Color(0xFFB71C1C),
    secondary = SecondarySlate,
    onSecondary = TextPrimaryDark,
    tertiary = TertiaryGold,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1E293B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFEDF2F7),
    onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFFCBD5E0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark Noir aesthetic for Mafia
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

