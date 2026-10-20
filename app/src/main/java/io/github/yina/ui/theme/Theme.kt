package io.github.yina.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Pine700,
    onPrimary = Color.White,
    primaryContainer = Pine200,
    onPrimaryContainer = Pine900,
    secondary = Color(0xFF5A6B63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EAE5),
    onSecondaryContainer = Color(0xFF16241E),
    tertiary = Amber600,
    onTertiary = Color.White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Color(0xFF3D2410),
    background = Sand000,
    onBackground = Ink900,
    surface = Sand025,
    onSurface = Ink900,
    surfaceVariant = Sand100,
    onSurfaceVariant = Ink600,
    outline = Color(0xFF8A8F89),
    outlineVariant = Sand200,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Pine300,
    onPrimary = Color(0xFF0E3026),
    primaryContainer = Pine800,
    onPrimaryContainer = Color(0xFFC8EBDA),
    secondary = Color(0xFFB7C4BD),
    onSecondary = Color(0xFF22332C),
    secondaryContainer = Color(0xFF354B41),
    onSecondaryContainer = Color(0xFFD3E3DA),
    tertiary = Amber300,
    onTertiary = Color(0xFF3D2410),
    tertiaryContainer = Color(0xFF57371B),
    onTertiaryContainer = Color(0xFFF7DFC8),
    background = Night000,
    onBackground = Mist900,
    surface = Night025,
    onSurface = Mist900,
    surfaceVariant = Night100,
    onSurfaceVariant = Mist600,
    outline = Color(0xFF6E756F),
    outlineVariant = Night200,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun YinaTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = YinaTypography,
        shapes = YinaShapes,
        content = content,
    )
}
