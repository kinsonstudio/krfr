package com.github.krfr.ui.glass

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.github.krfr.ui.theme.LiquidGlassBackground
import com.github.krfr.ui.theme.LocalUiStyle
import com.github.krfr.ui.theme.UiStyle
import com.github.krfr.ui.theme.ThemeStyle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 全 App 玻璃的取样源。
 *
 * 约定：这个 backdrop 只录制「背景层」（流体渐变 + 用户壁纸），不录制卡片自身，
 * 因此任何卡片都可以安全地拿它做 drawBackdrop 而不会形成绘制循环。
 * 底部 Tab 栏用的是更高一层的 pageBackdrop（背景 + 卡片内容），由 MainScaffold 提供。
 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** 背景底色：drawBackdrop 采样区的兜底不透明底，避免内容没铺满时出现透明像素 */
@Composable
fun glassBaseColor(dark: Boolean): Color =
    if (dark) Color(0xFF070A09) else Color(0xFFF3F7F4)

/**
 * 为独立页面（关于页 / 首次引导 / Shizuku 未就绪）铺一层可被玻璃采样的场景背景。
 */
@Composable
fun GlassScene(
    style: ThemeStyle,
    dark: Boolean,
    backgroundPath: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (LocalUiStyle.current == UiStyle.NATIVE) {
        // 原生风格：实心底 + 标准组件，不做任何 backdrop 取样
        Box(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            GlassBackdropImage(path = backgroundPath, dark = dark, withScrim = true)
            content()
        }
        return
    }
    val base = glassBaseColor(dark)
    val sceneBackdrop = rememberLayerBackdrop {
        drawRect(base)
        drawContent()
    }
    CompositionLocalProvider(LocalGlassBackdrop provides sceneBackdrop) {
        Box(modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(sceneBackdrop)
            ) {
                LiquidGlassBackground(style = style, dark = dark)
                GlassBackdropImage(path = backgroundPath, dark = dark)
            }
            content()
        }
    }
}

/** 用户自定义壁纸（按屏幕尺寸采样解码，避免大图 OOM） */
@Composable
fun GlassBackdropImage(
    path: String?,
    dark: Boolean,
    modifier: Modifier = Modifier,
    withScrim: Boolean = false
) {
    if (path == null) return
    val bitmap = remember(path) { decodeSampledBitmap(path) } ?: return
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alpha = if (dark) 0.34f else 0.55f
    )
    if (withScrim) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (dark) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.62f))
        )
    } else if (dark) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.34f)))
    }
}

/** 按目标尺寸采样解码背景图 */
fun decodeSampledBitmap(path: String, reqSize: Int = 1080): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        var sample = 1
        while (opts.outWidth / sample > reqSize || opts.outHeight / sample > reqSize) {
            sample *= 2
        }
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (t: Throwable) {
        null
    }
}
