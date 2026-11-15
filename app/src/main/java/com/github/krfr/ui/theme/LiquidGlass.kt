package com.github.krfr.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃视觉基础层。
 *
 * 性能策略：背景光斑用静态 Canvas 只绘制一次；玻璃面板用半透明渐变 + 描边 + 顶部高光带，
 * 不使用逐帧 RenderEffect。只有底部 Tab 栏这类小面积、低刷新区域才用真实背景模糊。
 */
@Immutable
data class GlassPalette(
    val baseTop: Color,
    val baseBottom: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
    val isDark: Boolean
)

fun glassPalette(style: ThemeStyle, dark: Boolean): GlassPalette = when {
    dark -> when (style) {
        ThemeStyle.PAPER_GREEN -> GlassPalette(
            Color(0xFF0A1013), Color(0xFF121A17),
            Color(0xFF1F6E5C), Color(0xFF3E5C8A), Color(0xFF7A5C2E), true
        )
        ThemeStyle.OCEAN_BLUE -> GlassPalette(
            Color(0xFF070C12), Color(0xFF0E1620),
            Color(0xFF1D4F7C), Color(0xFF2C6E8A), Color(0xFF3C3A6E), true
        )
        ThemeStyle.SUNSET_ORANGE -> GlassPalette(
            Color(0xFF120C08), Color(0xFF1B120C),
            Color(0xFF8A4A22), Color(0xFF6B3A5C), Color(0xFF4A3A1E), true
        )
        ThemeStyle.ROSE_PINK -> GlassPalette(
            Color(0xFF120A0D), Color(0xFF1A1013),
            Color(0xFF7C2C44), Color(0xFF4A2C6E), Color(0xFF8A5A3A), true
        )
    }
    else -> when (style) {
        ThemeStyle.PAPER_GREEN -> GlassPalette(
            Color(0xFFF4F7F4), Color(0xFFE8EEE9),
            Color(0xFFB9E3D2), Color(0xFFC7D8F0), Color(0xFFF0E2C4), false
        )
        ThemeStyle.OCEAN_BLUE -> GlassPalette(
            Color(0xFFEFF6FC), Color(0xFFE2ECF6),
            Color(0xFFB9D8F0), Color(0xFFC7E8F2), Color(0xFFD6D6F2), false
        )
        ThemeStyle.SUNSET_ORANGE -> GlassPalette(
            Color(0xFFFDF4EC), Color(0xFFF3E6D9),
            Color(0xFFF2C8A2), Color(0xFFE8C0D0), Color(0xFFF0E0B4), false
        )
        ThemeStyle.ROSE_PINK -> GlassPalette(
            Color(0xFFFBF2F4), Color(0xFFF0E2E7),
            Color(0xFFF2C0CC), Color(0xFFE0C6E8), Color(0xFFF0DCC0), false
        )
    }
}

/** 静态流体渐变背景：一次绘制，滚动时零开销 */
@Composable
fun LiquidGlassBackground(
    style: ThemeStyle,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = remember(style, dark) { glassPalette(style, dark) }
    Canvas(modifier.fillMaxSize()) {
        drawLiquidGlassBackdrop(palette)
    }
}

private fun DrawScope.drawLiquidGlassBackdrop(p: GlassPalette) {
    val w = size.width
    val h = size.height
    drawRect(
        Brush.verticalGradient(
            0f to p.baseTop,
            1f to p.baseBottom
        )
    )
    val maxR = size.maxDimension
    fun blob(color: Color, cx: Float, cy: Float, r: Float, a: Float) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = a), color.copy(alpha = 0f)),
                center = Offset(cx, cy),
                radius = r
            ),
            radius = r,
            center = Offset(cx, cy)
        )
    }
    blob(p.blobA, w * 0.12f, h * 0.16f, maxR * 0.62f, if (p.isDark) 0.34f else 0.55f)
    blob(p.blobB, w * 0.92f, h * 0.34f, maxR * 0.55f, if (p.isDark) 0.28f else 0.48f)
    blob(p.blobC, w * 0.55f, h * 0.86f, maxR * 0.60f, if (p.isDark) 0.24f else 0.42f)
    // 底部压暗，给 Tab 栏足够对比
    drawRect(
        Brush.verticalGradient(
            0.72f to Color.Transparent,
            1f to (if (p.isDark) Color.Black.copy(0.34f) else Color.Black.copy(0.06f))
        )
    )
}

/**
 * 玻璃面板（静态版，零 RenderEffect）：
 * 半透明填充 + 顶部镜面高光 + 内描边 + 外柔和投影。
 */
fun Modifier.liquidGlassPanel(
    shape: Shape,
    dark: Boolean,
    tint: Color = Color.Unspecified,
    strong: Boolean = false
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            0f to (if (tint != Color.Unspecified) tint else if (dark) Color.White.copy(if (strong) 0.16f else 0.10f) else Color.White.copy(if (strong) 0.78f else 0.62f)),
            0.5f to (if (tint != Color.Unspecified) tint.copy(alpha = tint.alpha * 0.96f) else if (dark) Color.White.copy(if (strong) 0.10f else 0.06f) else Color.White.copy(if (strong) 0.64f else 0.48f)),
            1f to (if (tint != Color.Unspecified) tint else if (dark) Color.White.copy(if (strong) 0.05f else 0.03f) else Color.White.copy(if (strong) 0.52f else 0.38f))
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            0f to (if (dark) Color.White.copy(0.22f) else Color.White.copy(0.72f)),
            0.55f to (if (dark) Color.White.copy(0.07f) else Color.White.copy(0.30f)),
            1f to (if (dark) Color.White.copy(0.16f) else Color.White.copy(0.58f))
        ),
        shape = shape
    )

/** 顶部镜面高光带：叠在任意玻璃面板上，增强“液态玻璃”光泽 */
fun Modifier.glassSheen(shape: Shape, dark: Boolean): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            0f to (if (dark) Color.White.copy(0.10f) else Color.White.copy(0.30f)),
            0.22f to (if (dark) Color.White.copy(0.03f) else Color.White.copy(0.10f)),
            0.34f to Color.Transparent
        )
    )

/** 当前是否深色主题 */
@Composable
fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** 玻璃页面的通用容器：透明，让背景光斑透出 */
@Composable
fun GlassContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize()) { content() }
}
