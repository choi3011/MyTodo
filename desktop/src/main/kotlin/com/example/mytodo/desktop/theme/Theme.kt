package com.example.mytodo.desktop.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5DFFF),
    onPrimaryContainer = Color(0xFF1F1066),
    secondary = BrandMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E6),
    onSecondaryContainer = Color(0xFF55003C),
    tertiary = BrandAmber,
    onTertiary = Color(0xFF402900),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = Color(0xFFE2DEF2),
    error = BrandCoral,
    onError = Color.White,
    errorContainer = Color(0xFFFFD9D6),
    onErrorContainer = Color(0xFF6A0014),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB7A9FF),
    onPrimary = Color(0xFF1A0F60),
    primaryContainer = Color(0xFF3A2B9B),
    onPrimaryContainer = Color(0xFFE5DFFF),
    secondary = Color(0xFFFF85B4),
    onSecondary = Color(0xFF59002F),
    tertiary = Color(0xFFFFC95C),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = Color(0xFF302845),
    error = Color(0xFFFF8A82),
    onError = Color(0xFF430005),
)

@Composable
fun MyTodoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography,
        content = content,
    )
}
