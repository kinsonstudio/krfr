package com.github.krfr.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 液态玻璃视觉基础层。
 *
 * 性能策略：背景只有一块 Canvas —— 大色块 + 缓慢漂移的光斑 + 细的「液面等高线」。
 * 等高线是给底部玻璃栏准备的折射素材：有了高频细节，lens 弯折才看得出来。
 * 页面卡片/列表项的玻璃用 backdrop 模糊或静态渐变，不逐项上 RenderEffect。
 */
@Immutable
data class GlassPalette(
    val baseTop: Color,
    val baseBottom: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
    val lineTint: Color,
    val isDark: Boolean
)

fun glassPalette(style: ThemeStyle, dark: Boolean): GlassPalette = when {
    dark -> when (style) {
        ThemeStyle.PAPER_GREEN -> GlassPalette(
            Color(0xFF05080E), Color(0xFF0B131E),
            Color(0xFF1B4F8A), Color(0xFF2C6FA8), Color(0xFF3A3F8A),
            Color(0xFF74B6F2), true
        )
        ThemeStyle.OCEAN_BLUE -> GlassPalette(
            Color(0xFF070C12), Color(0xFF0E1620),
            Color(0xFF1D4F7C), Color(0xFF2C6E8A), Color(0xFF3C3A6E),
            Color(0xFFA8D8F5), true
        )
        ThemeStyle.SUNSET_ORANGE -> GlassPalette(
            Color(0xFF120C08), Color(0xFF1B120C),
            Color(0xFF8A4A22), Color(0xFF6B3A5C), Color(0xFF4A3A1E),
            Color(0xFFF5C79E), true
        )
        ThemeStyle.ROSE_PINK -> GlassPalette(
            Color(0xFF120A0D), Color(0xFF1A1013),
            Color(0xFF7C2C44), Color(0xFF4A2C6E), Color(0xFF8A5A3A),
            Color(0xFFF0B9C8), true
        )
    }
    else -> when (style) {
        ThemeStyle.PAPER_GREEN -> GlassPalette(
            Color(0xFFF7FBFF), Color(0xFFE4EEFA),
            Color(0xFFA9D2FF), Color(0xFFC6E6FF), Color(0xFFC3C6FF),
            Color(0xFF3E86D6), false
        )
        ThemeStyle.OCEAN_BLUE -> GlassPalette(
            Color(0xFFEFF6FC), Color(0xFFE2ECF6),
            Color(0xFFB9D8F0), Color(0xFFC7E8F2), Color(0xFFD6D6F2),
            Color(0xFF5B87B5), false
        )
        ThemeStyle.SUNSET_ORANGE -> GlassPalette(
            Color(0xFFFDF4EC), Color(0xFFF3E6D9),
            Color(0xFFF2C8A2), Color(0xFFE8C0D0), Color(0xFFF0E0B4),
            Color(0xFFB5794E), false
        )
        ThemeStyle.ROSE_PINK -> GlassPalette(
            Color(0xFFFBF2F4), Color(0xFFF0E2E7),
            Color(0xFFF2C0CC), Color(0xFFE0C6E8), Color(0xFFF0DCC0),
            Color(0xFFB0697C), false
        )
    }
}

/** 流体背景：一块 Canvas，光斑缓慢漂移（约 40s 一轮），等高线始终提供折射细节 */
@Composable
fun LiquidGlassBackground(
    style: ThemeStyle,
    dark: Boolean,
    modifier: Modifier = Modifier,
    animated: Boolean = false
) {
    val palette = remember(style, dark) { glassPalette(style, dark) }
    // 静态一次性绘制：背景不参与逐帧重绘，滚动 / 交互时零成本
    Canvas(modifier.fillMaxSize()) {
        drawLiquidGlassBackdrop(palette, 0f)
    }
}

private fun DrawScope.drawLiquidGlassBackdrop(p: GlassPalette, phase: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(0f to p.baseTop, 1f to p.baseBottom))

    val angle = 2f * PI.toFloat() * phase
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
    // 三个缓慢绕行的大光斑
    blob(
        p.blobA,
        w * (0.12f + 0.05f * sin(angle)),
        h * (0.16f + 0.03f * cos(angle)),
        maxR * 0.62f,
        if (p.isDark) 0.52f else 0.82f
    )
    blob(
        p.blobB,
        w * (0.92f + 0.04f * cos(angle * 0.8f)),
        h * (0.34f + 0.04f * sin(angle * 0.8f)),
        maxR * 0.55f,
        if (p.isDark) 0.44f else 0.72f
    )
    blob(
        p.blobC,
        w * (0.55f + 0.06f * sin(angle * 0.6f + 1.2f)),
        h * (0.86f + 0.02f * cos(angle * 0.6f)),
        maxR * 0.60f,
        if (p.isDark) 0.40f else 0.66f
    )

    // 液面等高线：细弧线，专门给底部玻璃栏当折射素材
    val lineAlpha = if (p.isDark) 0.10f else 0.16f
    val lineColor = p.lineTint.copy(alpha = lineAlpha)
    val bandTop = h * 0.62f
    val bandHeight = h * 0.42f
    for (i in 0 until 7) {
        val t = (i / 6f) * 1.4f - 0.2f
        val yBase = bandTop + bandHeight * t
        val path = Path()
        val amplitude = h * (0.012f + 0.004f * i)
        path.moveTo(-w * 0.1f, yBase)
        var x = -w * 0.1f
        while (x < w * 1.1f) {
            val y = yBase + amplitude * sin((x / w) * 3.4f * PI.toFloat() + i * 0.55f + angle * 0.5f)
            path.lineTo(x, y)
            x += w / 24f
        }
        drawPath(
            path = path,
            color = lineColor.copy(alpha = lineAlpha * (1f - 0.09f * i)),
            style = Stroke(
                width = 1.1.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }


    // 底部压暗，给 Tab 栏足够对比
    drawRect(
        Brush.verticalGradient(
            0.74f to Color.Transparent,
            1f to if (p.isDark) Color.Black.copy(0.30f) else Color.Black.copy(0.05f)
        )
    )
}

/**
 * 玻璃面板（静态版，零 RenderEffect）：半透明渐变 + 顶部镜面高光 + 内描边。
 * 仅在没有 backdrop 时兜底使用。
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

/** 顶部镜面高光带：叠在任意玻璃面板上，增强「液态玻璃」光泽 */
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
