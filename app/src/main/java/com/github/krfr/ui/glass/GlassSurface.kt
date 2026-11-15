package com.github.krfr.ui.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.krfr.ui.theme.KrfrCardShape
import com.github.krfr.ui.theme.LocalUiStyle
import com.github.krfr.ui.theme.UiStyle
import com.github.krfr.ui.theme.isDarkSurface
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * 真玻璃面板：对背景层做 vibrancy + blur + 轻微 lens 折射，再叠高光 / 内阴影 / 半透明底。
 * 只给「首屏可见的大块卡片」用，保证一屏内实时 RenderEffect 数量可控。
 */
@Composable
fun Modifier.realGlass(
    shape: Shape = KrfrCardShape,
    tint: Color = Color.Unspecified,
    strong: Boolean = false,
    blurRadius: Dp = 8.dp,
    lensRadius: Dp = 0.dp,
    elevated: Boolean = false,
    backdrop: Backdrop? = LocalGlassBackdrop.current
): Modifier {
    if (LocalUiStyle.current == UiStyle.NATIVE) {
        // 原生风格：实心卡片 + 1dp 描边，没有任何 RenderEffect
        val fill = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.surface
        return this
            .clip(shape)
            .background(fill)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    }
    val dark = isDarkSurface()
    val fill = when {
        tint != Color.Unspecified -> tint
        dark -> Color(0xFF141719).copy(alpha = if (strong) 0.44f else 0.34f)
        else -> Color.White.copy(alpha = if (strong) 0.52f else 0.40f)
    }
    if (backdrop == null) return this.staticGlass(shape, dark, tint, strong)
    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx())
                if (lensRadius.value > 0f) lens(lensRadius.toPx(), (lensRadius * 1.7f).toPx())
            },
            highlight = {
                Highlight(
                    width = 0.5.dp,
                    blurRadius = 3.dp,
                    alpha = if (dark) 0.55f else 0.95f
                )
            },
            // 卡片不上投影/内阴影：省掉两趟额外模糊，滚动时的关键优化点
            shadow = if (elevated) {
                { Shadow(radius = 18.dp, alpha = if (dark) 0.42f else 0.16f) }
            } else null,
            innerShadow = if (elevated) {
                { InnerShadow(radius = 14.dp, alpha = if (dark) 0.30f else 0.14f) }
            } else null,
            onDrawSurface = { drawRect(fill) }
        )
        .clip(shape)
}

/**
 * 静态玻璃（零 RenderEffect）：半透明渐变 + 顶部镜面高光 + 1dp 渐变描边。
 * 列表项 / 次要卡片一律用它，滚动时零成本。
 */
fun Modifier.staticGlass(
    shape: Shape = KrfrCardShape,
    dark: Boolean,
    tint: Color = Color.Unspecified,
    strong: Boolean = false
): Modifier {
    val top: Color
    val mid: Color
    val bottom: Color
    if (tint != Color.Unspecified) {
        top = tint
        mid = tint.copy(alpha = tint.alpha * 0.94f)
        bottom = tint.copy(alpha = tint.alpha * 0.86f)
    } else if (dark) {
        top = Color.White.copy(if (strong) 0.15f else 0.10f)
        mid = Color.White.copy(if (strong) 0.09f else 0.055f)
        bottom = Color.White.copy(if (strong) 0.045f else 0.028f)
    } else {
        top = Color.White.copy(if (strong) 0.74f else 0.58f)
        mid = Color.White.copy(if (strong) 0.60f else 0.44f)
        bottom = Color.White.copy(if (strong) 0.50f else 0.34f)
    }
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                0f to top,
                0.55f to mid,
                1f to bottom
            )
        )
        .background(
            Brush.verticalGradient(
                0f to (if (dark) Color.White.copy(0.10f) else Color.White.copy(0.30f)),
                0.16f to (if (dark) Color.White.copy(0.02f) else Color.White.copy(0.08f)),
                0.30f to Color.Transparent
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                0f to (if (dark) Color.White.copy(0.26f) else Color.White.copy(0.86f)),
                0.45f to (if (dark) Color.White.copy(0.07f) else Color.White.copy(0.28f)),
                1f to (if (dark) Color.White.copy(0.14f) else Color.White.copy(0.52f))
            ),
            shape = shape
        )
}

/**
 * 与 Material Surface 同签名的玻璃卡片：把现有界面里的 Surface(shape = KrfrCardShape, ...)
 * 直接替换成 GlassSurface 即可获得真 backdrop 效果；没有 backdrop 时自动退回静态玻璃。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = KrfrCardShape,
    color: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = null,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    strong: Boolean = false,
    blurRadius: Dp = 8.dp,
    lensRadius: Dp = 0.dp,
    real: Boolean = true,
    content: @Composable () -> Unit
) {
    // real = false 时退回静态玻璃：列表里靠后的卡片用，控制同屏 RenderEffect 数量
    val panel = if (real) {
        Modifier.realGlass(
            shape = shape,
            tint = color,
            strong = strong,
            blurRadius = blurRadius,
            lensRadius = lensRadius
        )
    } else {
        Modifier.staticGlass(shape, isDarkSurface(), color, strong)
    }
    Box(modifier.then(panel)) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            content()
        }
    }
}

/**
 * 胶囊按钮的玻璃底：低 alpha 渐变填充 + vibrancy + 模糊 + 轻微透镜，
 * 再用边缘高光/内阴影表现厚度。alpha 太高就会退回「实心漆面」，所以调用方给 0.2~0.5。
 */
@Composable
fun Modifier.glassButton(
    fill: Color,
    shape: Shape = CircleShape,
    backdrop: Backdrop? = LocalGlassBackdrop.current
): Modifier {
    if (LocalUiStyle.current == UiStyle.NATIVE) {
        val solid = fill.copy(alpha = 1f)
        return if (solid.luminance() > 0.6f) {
            this
                .clip(shape)
                .background(solid)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        } else {
            this.clip(shape).background(solid)
        }
    }
    val dark = isDarkSurface()
    if (backdrop == null) {
        return this.staticGlass(shape, dark, fill)
    }
    val topAlpha = (fill.alpha * 1.55f).coerceAtMost(1f)
    val bottomAlpha = (fill.alpha * 0.72f).coerceAtMost(1f)
    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(10.dp.toPx())
                lens(7.dp.toPx(), 14.dp.toPx())
            },
            highlight = {
                Highlight(
                    width = 1.dp,
                    blurRadius = 2.dp,
                    alpha = if (dark) 0.65f else 0.95f
                )
            },
            innerShadow = { InnerShadow(radius = 10.dp, alpha = if (dark) 0.30f else 0.16f) },
            onDrawSurface = {
                drawRect(
                    Brush.verticalGradient(
                        0f to fill.copy(alpha = topAlpha),
                        0.55f to fill.copy(alpha = (bottomAlpha + topAlpha) / 2f),
                        1f to fill.copy(alpha = bottomAlpha)
                    )
                )
                // 顶部镜面反光：这一条是「玻璃」和「色块」的分界
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.16f else 0.34f),
                        0.28f to Color.White.copy(alpha = if (dark) 0.04f else 0.10f),
                        0.5f to Color.Transparent
                    )
                )
            }
        )
        .clip(shape)
}
