package com.github.krfr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 应用内主题模式：跟随系统 / 浅色 / 深色 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val LightColorScheme = lightColorScheme(
    primary = KrfrPrimary,
    onPrimary = KrfrOnPrimary,
    primaryContainer = KrfrPrimaryContainer,
    onPrimaryContainer = KrfrOnPrimaryContainer,
    secondary = KrfrSecondary,
    onSecondary = KrfrOnSecondary,
    secondaryContainer = KrfrSecondaryContainer,
    onSecondaryContainer = KrfrOnSecondaryContainer,
    tertiary = KrfrCodeAccent,
    onTertiary = Color.White,
    tertiaryContainer = KrfrVioletSoft,
    onTertiaryContainer = KrfrCodeAccent,
    background = KrfrBackground,
    onBackground = KrfrOnSurface,
    surface = KrfrSurface2,
    onSurface = KrfrOnSurface,
    surfaceVariant = KrfrSurfaceVariant,
    onSurfaceVariant = KrfrOnSurfaceVariant,
    outline = KrfrOutline,
    outlineVariant = KrfrOutlineVariant,
    error = KrfrError,
    errorContainer = KrfrErrorContainer,
    surfaceTint = Color.Transparent
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkOnSurfaceVariant,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = DarkLink,
    onTertiary = DarkBackground,
    tertiaryContainer = Color(0xFF1E3A40),
    onTertiaryContainer = Color(0xFF9FD8DE),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = DarkError,
    errorContainer = DarkErrorContainer,
    surfaceTint = Color.Transparent
)

@Composable
fun KrfrTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = KrfrTypography,
        shapes = KrfrShapes,
        content = content
    )
}
