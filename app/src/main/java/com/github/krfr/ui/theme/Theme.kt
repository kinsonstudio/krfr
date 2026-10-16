package com.github.krfr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 应用内明暗模式：跟随系统 / 浅色 / 深色 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/** 应用内配色风格：纸感墨绿（默认）/ 海蓝 / 暖橙 / 玫红 */
enum class ThemeStyle(val labelKey: String) {
    PAPER_GREEN("style_paper_green"),
    OCEAN_BLUE("style_ocean_blue"),
    SUNSET_ORANGE("style_sunset_orange"),
    ROSE_PINK("style_rose_pink")
}

private fun buildLightScheme(style: ThemeStyle) = when (style) {
    ThemeStyle.PAPER_GREEN -> lightColorScheme(
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
    ThemeStyle.OCEAN_BLUE -> lightColorScheme(
        primary = OceanLink,
        onPrimary = Color.White,
        primaryContainer = OceanLinkSoft,
        onPrimaryContainer = OceanInk,
        secondary = OceanMute,
        onSecondary = Color.White,
        secondaryContainer = OceanSurfaceVariant,
        onSecondaryContainer = OceanInk,
        tertiary = OceanAccent,
        onTertiary = Color.White,
        tertiaryContainer = OceanSurfaceVariant,
        onTertiaryContainer = OceanInk,
        background = OceanBg,
        onBackground = OceanInk,
        surface = OceanSurface,
        onSurface = OceanInk,
        surfaceVariant = OceanSurfaceVariant,
        onSurfaceVariant = OceanMute,
        outline = Color(0xFFD8E2EC),
        outlineVariant = Color(0xFFD8E2EC),
        error = KrfrError,
        errorContainer = KrfrErrorContainer,
        surfaceTint = Color.Transparent
    )
    ThemeStyle.SUNSET_ORANGE -> lightColorScheme(
        primary = SunsetLink,
        onPrimary = Color.White,
        primaryContainer = SunsetLinkSoft,
        onPrimaryContainer = SunsetInk,
        secondary = SunsetMute,
        onSecondary = Color.White,
        secondaryContainer = SunsetSurfaceVariant,
        onSecondaryContainer = SunsetInk,
        tertiary = SunsetAccent,
        onTertiary = Color.White,
        tertiaryContainer = SunsetSurfaceVariant,
        onTertiaryContainer = SunsetInk,
        background = SunsetBg,
        onBackground = SunsetInk,
        surface = SunsetSurface,
        onSurface = SunsetInk,
        surfaceVariant = SunsetSurfaceVariant,
        onSurfaceVariant = SunsetMute,
        outline = Color(0xFFE5D5C8),
        outlineVariant = Color(0xFFE5D5C8),
        error = KrfrError,
        errorContainer = KrfrErrorContainer,
        surfaceTint = Color.Transparent
    )
    ThemeStyle.ROSE_PINK -> lightColorScheme(
        primary = RoseLink,
        onPrimary = Color.White,
        primaryContainer = RoseLinkSoft,
        onPrimaryContainer = RoseInk,
        secondary = RoseMute,
        onSecondary = Color.White,
        secondaryContainer = RoseSurfaceVariant,
        onSecondaryContainer = RoseInk,
        tertiary = RoseAccent,
        onTertiary = Color.White,
        tertiaryContainer = RoseSurfaceVariant,
        onTertiaryContainer = RoseInk,
        background = RoseBg,
        onBackground = RoseInk,
        surface = RoseSurface,
        onSurface = RoseInk,
        surfaceVariant = RoseSurfaceVariant,
        onSurfaceVariant = RoseMute,
        outline = Color(0xFFE8D5DC),
        outlineVariant = Color(0xFFE8D5DC),
        error = KrfrError,
        errorContainer = KrfrErrorContainer,
        surfaceTint = Color.Transparent
    )
}

private fun buildDarkScheme(style: ThemeStyle) = when (style) {
    ThemeStyle.PAPER_GREEN -> darkColorScheme(
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
    ThemeStyle.OCEAN_BLUE -> darkColorScheme(
        primary = OceanDarkLink,
        onPrimary = OceanDarkOnPrimary,
        primaryContainer = OceanDarkPrimaryContainer,
        onPrimaryContainer = OceanDarkOnPrimaryContainer,
        secondary = OceanDarkOnSurfaceVariant,
        onSecondary = OceanDarkBg,
        secondaryContainer = OceanDarkSurfaceVariant,
        onSecondaryContainer = OceanDarkOnSurface,
        tertiary = OceanDarkLink,
        onTertiary = OceanDarkBg,
        tertiaryContainer = OceanDarkPrimaryContainer,
        onTertiaryContainer = OceanDarkOnPrimaryContainer,
        background = OceanDarkBg,
        onBackground = OceanDarkOnSurface,
        surface = OceanDarkSurface,
        onSurface = OceanDarkOnSurface,
        surfaceVariant = OceanDarkSurfaceVariant,
        onSurfaceVariant = OceanDarkOnSurfaceVariant,
        outline = OceanDarkOutline,
        outlineVariant = OceanDarkOutline,
        error = DarkError,
        errorContainer = DarkErrorContainer,
        surfaceTint = Color.Transparent
    )
    ThemeStyle.SUNSET_ORANGE -> darkColorScheme(
        primary = SunsetDarkLink,
        onPrimary = SunsetDarkOnPrimary,
        primaryContainer = SunsetDarkPrimaryContainer,
        onPrimaryContainer = SunsetDarkOnPrimaryContainer,
        secondary = SunsetDarkOnSurfaceVariant,
        onSecondary = SunsetDarkBg,
        secondaryContainer = SunsetDarkSurfaceVariant,
        onSecondaryContainer = SunsetDarkOnSurface,
        tertiary = SunsetDarkLink,
        onTertiary = SunsetDarkBg,
        tertiaryContainer = SunsetDarkPrimaryContainer,
        onTertiaryContainer = SunsetDarkOnPrimaryContainer,
        background = SunsetDarkBg,
        onBackground = SunsetDarkOnSurface,
        surface = SunsetDarkSurface,
        onSurface = SunsetDarkOnSurface,
        surfaceVariant = SunsetDarkSurfaceVariant,
        onSurfaceVariant = SunsetDarkOnSurfaceVariant,
        outline = SunsetDarkOutline,
        outlineVariant = SunsetDarkOutline,
        error = DarkError,
        errorContainer = DarkErrorContainer,
        surfaceTint = Color.Transparent
    )
    ThemeStyle.ROSE_PINK -> darkColorScheme(
        primary = RoseDarkLink,
        onPrimary = RoseDarkOnPrimary,
        primaryContainer = RoseDarkPrimaryContainer,
        onPrimaryContainer = RoseDarkOnPrimaryContainer,
        secondary = RoseDarkOnSurfaceVariant,
        onSecondary = RoseDarkBg,
        secondaryContainer = RoseDarkSurfaceVariant,
        onSecondaryContainer = RoseDarkOnSurface,
        tertiary = RoseDarkLink,
        onTertiary = RoseDarkBg,
        tertiaryContainer = RoseDarkPrimaryContainer,
        onTertiaryContainer = RoseDarkOnPrimaryContainer,
        background = RoseDarkBg,
        onBackground = RoseDarkOnSurface,
        surface = RoseDarkSurface,
        onSurface = RoseDarkOnSurface,
        surfaceVariant = RoseDarkSurfaceVariant,
        onSurfaceVariant = RoseDarkOnSurfaceVariant,
        outline = RoseDarkOutline,
        outlineVariant = RoseDarkOutline,
        error = DarkError,
        errorContainer = DarkErrorContainer,
        surfaceTint = Color.Transparent
    )
}

@Composable
fun KrfrTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeStyle: ThemeStyle = ThemeStyle.PAPER_GREEN,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) buildDarkScheme(themeStyle) else buildLightScheme(themeStyle)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = KrfrTypography,
        shapes = KrfrShapes,
        content = content
    )
}
