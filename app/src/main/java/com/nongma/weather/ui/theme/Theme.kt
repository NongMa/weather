package com.nongma.weather.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E63D0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E5FF),
    onPrimaryContainer = Color(0xFF001C3F),
    secondary = Color(0xFF556174),
    onSecondary = Color.White,
    background = Color(0xFFF4F7FC),
    onBackground = Color(0xFF11141A),
    surface = Color(0xFFFCFDFF),
    onSurface = Color(0xFF11141A),
    surfaceVariant = Color(0xFFE1E6EF),
    onSurfaceVariant = Color(0xFF434858),
    outline = Color(0xFF737787),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9EC1FF),
    onPrimary = Color(0xFF00306A),
    primaryContainer = Color(0xFF0B4790),
    onPrimaryContainer = Color(0xFFD6E5FF),
    secondary = Color(0xFFBDC7DB),
    onSecondary = Color(0xFF263141),
    background = Color(0xFF070B14),
    onBackground = Color(0xFFE3E2E8),
    surface = Color(0xFF0D111B),
    onSurface = Color(0xFFE3E2E8),
    surfaceVariant = Color(0xFF434858),
    onSurfaceVariant = Color(0xFFC3C6D2),
    outline = Color(0xFF8D909E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun WeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // The UI paints its own iOS-style sky, so the branded palette is the default.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
