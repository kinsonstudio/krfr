package com.github.krfr.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.github.krfr.ui.glass.glassBaseColor

/**
 * 主题切换的过渡：配色 / 深浅色一变，用一层「新主题底色」快速漫过再褪去。
 *
 * 不重建 Compose 树、不动 GraphicsLayer，只是多层 alpha 动画，
 * 所以既能把硬切盖掉，又不会造成玻璃卡片整屏重算。
 */
@Composable
fun ThemeTransition(
    themeKey: Any,
    dark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val wash = remember { Animatable(0f) }
    var firstPass = remember { booleanArrayOf(true) }

    LaunchedEffect(themeKey) {
        if (firstPass[0]) {
            firstPass[0] = false
            return@LaunchedEffect
        }
        wash.snapTo(1f)
        wash.animateTo(0f, tween(durationMillis = 360, easing = FastOutSlowInEasing))
    }

    Box(modifier.fillMaxSize()) {
        content()
        if (wash.value > 0.001f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(wash.value * 0.55f)
                    .background(glassBaseColor(dark))
            )
        }
    }
}
