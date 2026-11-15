package com.github.krfr.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 界面风格：
 * - [LIQUID_GLASS] 液态玻璃（默认）：悬浮玻璃 Tab 栏 + backdrop 折射 + 流动背景
 * - [NATIVE] 原生风格：Material 3 标准布局，实心卡片 + 标准底部导航栏
 */
enum class UiStyle {
    LIQUID_GLASS,
    NATIVE
}

/** 当前生效的界面风格，供各组件按需降级到原生样式 */
val LocalUiStyle = staticCompositionLocalOf { UiStyle.LIQUID_GLASS }

/**
 * 底部留白：液态玻璃是悬浮栏，需要给内容留出栏的高度；
 * 原生风格由 Scaffold 的 bottomBar 自动避让，只需常规间距。
 */
@Composable
fun uiBottomInset(glass: Dp, native: Dp = 16.dp): Dp =
    if (LocalUiStyle.current == UiStyle.NATIVE) native else glass
