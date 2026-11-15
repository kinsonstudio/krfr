package com.github.krfr.ui.theme

import androidx.compose.ui.graphics.Color

// Krfr 色板（2.1 苹果蓝）：冷白偏蓝的底 + iOS 蓝主色，深色为近黑蓝底 + 亮蓝点缀。
val KrfrInk = Color(0xFF0B1220)          // 主文字
val KrfrInkSoft = Color(0xFF2A3A50)      // 次级文字
val KrfrMute = Color(0xFF5B6B80)         // 弱化文字
val KrfrMute2 = Color(0xFF8A99AC)        // 更弱化文字
val KrfrLine = Color(0xFFDCE5F0)         // 分隔线 / 描边
val KrfrLineStrong = Color(0xFFC5D3E4)
val KrfrBg = Color(0xFFF2F6FC)           // 页面背景（冷白）
val KrfrSurface = Color(0xFFFFFFFF)      // 卡片背景
val KrfrSurface2 = Color(0xFFFFFFFF)
val KrfrWash = Color(0xFFE8EFF9)         // 填充底色


val KrfrLink = Color(0xFF0A7AFF)         // 主色：iOS 蓝
val KrfrLinkSoft = Color(0xFFDCEBFF)     // 主色浅底
val KrfrCodeAccent = Color(0xFF5E5CE6)   // 数据 / 链接高亮（靛蓝）
val KrfrVioletSoft = Color(0xFFE9E8FF)
val KrfrCodeBg = Color(0xFF0F1A2B)
val KrfrCodeFg = Color(0xFFE8F1FB)
val KrfrCodeWarn = Color(0xFFBBD8FF)

val KrfrSuccess = Color(0xFF34C759)      // 成功绿（系统色）
val KrfrOkBg = Color(0xFFE3F7E9)
val KrfrError = Color(0xFFE5484D)
val KrfrErrorContainer = Color(0xFFFFE9E9)


val KrfrPrimary = KrfrLink
val KrfrOnPrimary = Color.White
val KrfrPrimaryContainer = KrfrLinkSoft
val KrfrOnPrimaryContainer = Color(0xFF06305E)
val KrfrSecondary = KrfrMute
val KrfrOnSecondary = Color.White
val KrfrSecondaryContainer = KrfrWash
val KrfrOnSecondaryContainer = KrfrInk
val KrfrBackground = KrfrBg
val KrfrSurfaceVariant = KrfrWash
val KrfrOnSurface = KrfrInk
val KrfrOnSurfaceVariant = KrfrMute
val KrfrOutline = KrfrLine
val KrfrOutlineVariant = KrfrLine


val DarkPrimary = Color(0xFF0A84FF)
val DarkOnPrimary = Color(0xFF00264D)
val DarkPrimaryContainer = Color(0xFF123A63)
val DarkOnPrimaryContainer = Color(0xFFCFE4FF)
val DarkBackground = Color(0xFF070A0F)
val DarkSurface = Color(0xFF121821)
val DarkSurfaceVariant = Color(0xFF1E2733)
val DarkOnSurface = Color(0xFFE9EFF7)
val DarkOnSurfaceVariant = Color(0xFFA5B4C6)
val DarkOutline = Color(0xFF3B4756)
val DarkLink = Color(0xFF4FA8FF)
val DarkSuccess = Color(0xFF30D158)
val DarkError = Color(0xFFFF6961)
val DarkErrorContainer = Color(0xFF4A1F1C)

// ============ 可选配色：海蓝 OCEAN ============
val OceanLink = Color(0xFF2E5D8A)
val OceanLinkSoft = Color(0xFFE0ECF5)
val OceanBg = Color(0xFFF4F8FC)
val OceanSurface = Color(0xFFFDFEFF)
val OceanSurfaceVariant = Color(0xFFE7EEF5)
val OceanInk = Color(0xFF1E2B38)
val OceanMute = Color(0xFF5E7488)
val OceanAccent = Color(0xFF7FA6C9)
val OceanDarkLink = Color(0xFF8FC4EA)
val OceanDarkOnPrimary = Color(0xFF0B2C46)
val OceanDarkPrimaryContainer = Color(0xFF1C405E)
val OceanDarkOnPrimaryContainer = Color(0xFFC9E4F8)
val OceanDarkBg = Color(0xFF0F141A)
val OceanDarkSurface = Color(0xFF18212C)
val OceanDarkSurfaceVariant = Color(0xFF25313F)
val OceanDarkOnSurface = Color(0xFFE2EBF3)
val OceanDarkOnSurfaceVariant = Color(0xFFADBECA)
val OceanDarkOutline = Color(0xFF3E4E5C)

// ============ 可选配色：暖橙 SUNSET ============
val SunsetLink = Color(0xFFC06A3A)
val SunsetLinkSoft = Color(0xFFFBE9DF)
val SunsetBg = Color(0xFFFDF6F0)
val SunsetSurface = Color(0xFFFFFCF8)
val SunsetSurfaceVariant = Color(0xFFF3E6DB)
val SunsetInk = Color(0xFF38271E)
val SunsetMute = Color(0xFF8A7160)
val SunsetAccent = Color(0xFFD9A05F)
val SunsetDarkLink = Color(0xFFF0A878)
val SunsetDarkOnPrimary = Color(0xFF45220F)
val SunsetDarkPrimaryContainer = Color(0xFF6E3D20)
val SunsetDarkOnPrimaryContainer = Color(0xFFFCDCC7)
val SunsetDarkBg = Color(0xFF18120E)
val SunsetDarkSurface = Color(0xFF241B14)
val SunsetDarkSurfaceVariant = Color(0xFF33271E)
val SunsetDarkOnSurface = Color(0xFFF5EAE1)
val SunsetDarkOnSurfaceVariant = Color(0xFFC4B4A6)
val SunsetDarkOutline = Color(0xFF4E3F33)

// ============ 可选配色：玫红 ROSE ============
val RoseLink = Color(0xFFA64D62)
val RoseLinkSoft = Color(0xFFF8E4E9)
val RoseBg = Color(0xFFFBF5F6)
val RoseSurface = Color(0xFFFFFCFC)
val RoseSurfaceVariant = Color(0xFFF3E3E7)
val RoseInk = Color(0xFF38242A)
val RoseMute = Color(0xFF8B6A73)
val RoseAccent = Color(0xFFD08B9A)
val RoseDarkLink = Color(0xFFF2A2B5)
val RoseDarkOnPrimary = Color(0xFF4A1B27)
val RoseDarkPrimaryContainer = Color(0xFF703041)
val RoseDarkOnPrimaryContainer = Color(0xFFFCD6DF)
val RoseDarkBg = Color(0xFF181114)
val RoseDarkSurface = Color(0xFF241A1E)
val RoseDarkSurfaceVariant = Color(0xFF322429)
val RoseDarkOnSurface = Color(0xFFF5E6EA)
val RoseDarkOnSurfaceVariant = Color(0xFFC6B0B8)
val RoseDarkOutline = Color(0xFF4E3940)
