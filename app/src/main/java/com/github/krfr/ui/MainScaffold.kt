package com.github.krfr.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.krfr.ui.theme.LiquidGlassBackground
import com.github.krfr.ui.theme.isDarkSurface
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.github.krfr.R
import com.github.krfr.ui.screens.ConfigScreen
import com.github.krfr.ui.screens.PersonalizeScreen
import com.github.krfr.ui.screens.QuickPresetScreen
import com.github.krfr.ui.screens.SettingsScreen
import com.github.krfr.ui.screens.SimStatusScreen
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.ui.theme.ThemeMode
import com.github.krfr.ui.theme.ThemeStyle

/** 底部导航五个主页面 */
enum class MainTab(
    @androidx.annotation.StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CONFIG(R.string.tab_config, Icons.Filled.Tune, Icons.Outlined.Tune),
    STATUS(R.string.tab_status, Icons.Filled.SimCard, Icons.Outlined.SimCard),
    QUICK(R.string.tab_quick, Icons.Filled.Bolt, Icons.Outlined.Bolt),
    PERSONALIZE(R.string.tab_personalize, Icons.Filled.Palette, Icons.Outlined.Palette),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScaffold(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeStyle: ThemeStyle,
    onThemeStyleChange: (ThemeStyle) -> Unit,
    backgroundPath: String?,
    onBackgroundPathChange: (String?) -> Unit,
    appLang: String?,
    onLangChange: (String) -> Unit,
    onShowAbout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.CONFIG) }
    val darkBg = isDarkSurface()

    val bgBitmap = remember(backgroundPath) {
        backgroundPath?.let { decodeSampledBitmap(it) }
    }

    // 背景 + 页面内容一起录进 backdrop，Tab 栏玻璃折射的就是背后真实内容
    val contentBackdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(contentBackdrop)
        ) {
            LiquidGlassBackground(style = themeStyle, dark = darkBg)
            if (bgBitmap != null) {
                Image(
                    bitmap = bgBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = if (darkBg) 0.28f else 0.42f
                )
                if (darkBg) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.42f))
                    )
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent
            ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = 84.dp)
            ) {
                // 顶部横向柔光：增强玻璃光泽层次（静态，零成本）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to if (darkBg) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.22f),
                                1f to Color.Transparent
                            )
                        )
                )
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (
                            slideInHorizontally(tween(280)) { direction * it / 6 } +
                                fadeIn(tween(280)) +
                                scaleIn(initialScale = 0.96f, animationSpec = tween(280))
                            )
                            .togetherWith(
                                slideOutHorizontally(tween(200)) { -direction * it / 6 } +
                                    fadeOut(tween(150)) +
                                    scaleOut(targetScale = 0.96f, animationSpec = tween(200))
                            )
                    },
                    label = "main-tab"
                ) { tab ->
                    when (tab) {
                        MainTab.CONFIG -> ConfigScreen()
                        MainTab.STATUS -> SimStatusScreen()
                        MainTab.QUICK -> QuickPresetScreen()
                        MainTab.PERSONALIZE -> PersonalizeScreen(
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            themeStyle = themeStyle,
                            onThemeStyleChange = onThemeStyleChange,
                            backgroundPath = backgroundPath,
                            onBackgroundPathChange = onBackgroundPathChange,
                            appLang = appLang,
                            onLangChange = onLangChange
                        )
                        MainTab.SETTINGS -> SettingsScreen(
                            onShowAbout = onShowAbout
                        )
                    }
                }
            }
            }
        }

        // 悬浮玻璃 Tab 栏：覆盖在内容之上，折射滚动内容
        LiquidBottomBar(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            backdrop = contentBackdrop,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** 按目标尺寸采样解码背景图，避免大图 OOM */
private fun decodeSampledBitmap(path: String, reqSize: Int = 1080): Bitmap? {
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
