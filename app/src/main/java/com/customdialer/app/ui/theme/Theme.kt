package com.customdialer.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// Dark theme colors matching the web app
val DarkBackground = Color(0xFF0f0f1a)
val DarkSurface = Color(0xFF1a1a2e)
val DarkSurfaceVariant = Color(0xFF252540)
val DarkCard = Color(0xFF16213e)
val PrimaryBlue = Color(0xFF4361ee)
val PrimaryBlueDark = Color(0xFF3651d4)
val AccentCyan = Color(0xFF00d4ff)
val AccentGreen = Color(0xFF00e676)
val AccentRed = Color(0xFFff5252)
val AccentOrange = Color(0xFFff9800)
val AccentYellow = Color(0xFFffd600)
val TextPrimary = Color(0xFFe0e0e0)
val TextSecondary = Color(0xFF9e9e9e)
val TextMuted = Color(0xFF666680)
val BorderColor = Color(0xFF2a2a4a)

// Light theme colors
val LightBackground = Color(0xFFF2F3F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8E8F0)
val LightCard = Color(0xFFFFFFFF)  // Cards get elevation shadow in light mode
val LightTextPrimary = Color(0xFF1a1a2e)
val LightTextSecondary = Color(0xFF555566)
val LightTextMuted = Color(0xFF999AAA)
val LightBorderColor = Color(0xFFDDDDE5)

// Theme state holder
object ThemeState {
    val isDarkMode = mutableStateOf(false)
}

// Adaptive color accessors
object AppColors {
    val background: Color get() = if (ThemeState.isDarkMode.value) DarkBackground else LightBackground
    val surface: Color get() = if (ThemeState.isDarkMode.value) DarkSurface else LightSurface
    val surfaceVariant: Color get() = if (ThemeState.isDarkMode.value) DarkSurfaceVariant else LightSurfaceVariant
    val card: Color get() = if (ThemeState.isDarkMode.value) DarkCard else LightCard
    val textPrimary: Color get() = if (ThemeState.isDarkMode.value) TextPrimary else LightTextPrimary
    val textSecondary: Color get() = if (ThemeState.isDarkMode.value) TextSecondary else LightTextSecondary
    val textMuted: Color get() = if (ThemeState.isDarkMode.value) TextMuted else LightTextMuted
    val border: Color get() = if (ThemeState.isDarkMode.value) BorderColor else LightBorderColor
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueDark,
    secondary = AccentCyan,
    onSecondary = Color.Black,
    tertiary = AccentGreen,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = Color.White,
    outline = BorderColor
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlue.copy(alpha = 0.12f),
    secondary = AccentCyan,
    onSecondary = Color.Black,
    tertiary = AccentGreen,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = AccentRed,
    onError = Color.White,
    outline = LightBorderColor
)

@Composable
fun CustomDialerTheme(
    darkTheme: Boolean = ThemeState.isDarkMode.value,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Update system bars to match theme
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = if (darkTheme) DarkSurface else LightSurface,
            darkIcons = !darkTheme
        )
        systemUiController.setNavigationBarColor(
            color = if (darkTheme) DarkSurface else LightSurface,
            darkIcons = !darkTheme
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
