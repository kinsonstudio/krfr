package com.github.krfr.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.krfr.R
import com.github.krfr.ui.glass.GlassBackdropImage
import com.github.krfr.ui.glass.LocalGlassBackdrop
import com.github.krfr.ui.glass.glassBaseColor
import com.github.krfr.ui.screens.ConfigScreen
import com.github.krfr.ui.screens.PersonalizeScreen
import com.github.krfr.ui.screens.QuickPresetScreen
import com.github.krfr.ui.screens.SettingsScreen
import com.github.krfr.ui.screens.SimStatusScreen
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.ui.theme.LiquidGlassBackground
import com.github.krfr.ui.theme.ThemeMode
import com.github.krfr.ui.theme.ThemeStyle
import com.github.krfr.ui.theme.UiStyle
import com.github.krfr.ui.theme.isDarkSurface
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

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

@Composable
fun MainScaffold(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeStyle: ThemeStyle,
    onThemeStyleChange: (ThemeStyle) -> Unit,
    uiStyle: UiStyle,
    onUiStyleChange: (UiStyle) -> Unit,
    backgroundPath: String?,
    onBackgroundPathChange: (String?) -> Unit,
    appLang: String?,
    onLangChange: (String) -> Unit,
    onShowAbout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.CONFIG) }
    val darkBg = isDarkSurface()

    // 风格切换用交叉淡入淡出，而不是硬切
    Crossfade(
        targetState = uiStyle,
        animationSpec = tween(260),
        label = "ui-style"
    ) { style ->
        val screens: @Composable (MainTab) -> Unit = { tab ->
            TabScreen(
                tab = tab,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                themeStyle = themeStyle,
                onThemeStyleChange = onThemeStyleChange,
                uiStyle = style,
                onUiStyleChange = onUiStyleChange,
                backgroundPath = backgroundPath,
                onBackgroundPathChange = onBackgroundPathChange,
                appLang = appLang,
                onLangChange = onLangChange,
                onShowAbout = onShowAbout
            )
        }
        if (style == UiStyle.NATIVE) {
            NativeMainScaffold(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it },
                darkBg = darkBg,
                backgroundPath = backgroundPath,
                content = screens
            )
        } else {
            GlassMainLayout(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it },
                darkBg = darkBg,
                themeStyle = themeStyle,
                backgroundPath = backgroundPath,
                content = screens
            )
        }
    }
}

@Composable
private fun GlassMainLayout(
    selectedTab: MainTab,
    onSelect: (MainTab) -> Unit,
    darkBg: Boolean,
    themeStyle: ThemeStyle,
    backgroundPath: String?,
    content: @Composable (MainTab) -> Unit
) {
    val base = glassBaseColor(darkBg)

    // 背景层：只录流体背景 + 壁纸，供页面卡片取样（卡片的玻璃折射的是背景，不会自我循环）
    val bgBackdrop = rememberLayerBackdrop {
        drawRect(base)
        drawContent()
    }
    // 页面层：背景 + 卡片全部内容，供底部 Tab 栏取样 —— 玻璃栏里能看到卡片被模糊弯折
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(base)
        drawContent()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(pageBackdrop)
        ) {
            CompositionLocalProvider(LocalGlassBackdrop provides bgBackdrop) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(bgBackdrop)
                ) {
                    LiquidGlassBackground(style = themeStyle, dark = darkBg)
                    GlassBackdropImage(path = backgroundPath, dark = darkBg)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                // 只做交叉淡入淡出：不缩放、不位移，
                                // 避免整屏玻璃卡片每帧重算 RenderEffect 造成切换卡顿
                                fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
                            },
                            label = "main-tab"
                        ) { tab ->
                            content(tab)
                        }
                    }
                }
            }
        }

        // 悬浮液态玻璃 Tab 栏：按深浅色重建，避免主题切换后旧 GraphicsLayer 残留
        key(darkBg) {
            LiquidBottomBar(
                selected = selectedTab,
                onSelect = onSelect,
                backdrop = pageBackdrop,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/** 原生风格：标准 Material 3 布局，实心卡片 + 标准底部导航栏 */
@Composable
private fun NativeMainScaffold(
    selectedTab: MainTab,
    onSelect: (MainTab) -> Unit,
    darkBg: Boolean,
    backgroundPath: String?,
    content: @Composable (MainTab) -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (backgroundPath != null) {
            GlassBackdropImage(path = backgroundPath, dark = darkBg, withScrim = true)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (backgroundPath != null) Color.Transparent else MaterialTheme.colorScheme.background,
            bottomBar = { NativeBottomBar(selectedTab, onSelect) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
                    },
                    label = "main-tab-native"
                ) { tab ->
                    content(tab)
                }
            }
        }
    }
}

@Composable
private fun NativeBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.padding(bottom = 0.dp)
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = selected == tab
            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 0.88f,
                animationSpec = tween(220),
                label = "tab-icon-scale"
            )
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    Box(modifier = Modifier.scale(iconScale)) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = stringResource(tab.labelRes)
                        )
                    }
                },
                label = {
                    Text(
                        stringResource(tab.labelRes),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = KrfrAdaptive.link(),
                    selectedTextColor = KrfrAdaptive.link(),
                    indicatorColor = KrfrAdaptive.linkSoft(),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun TabScreen(
    tab: MainTab,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeStyle: ThemeStyle,
    onThemeStyleChange: (ThemeStyle) -> Unit,
    uiStyle: UiStyle,
    onUiStyleChange: (UiStyle) -> Unit,
    backgroundPath: String?,
    onBackgroundPathChange: (String?) -> Unit,
    appLang: String?,
    onLangChange: (String) -> Unit,
    onShowAbout: () -> Unit
) {
    when (tab) {
        MainTab.CONFIG -> ConfigScreen()
        MainTab.STATUS -> SimStatusScreen()
        MainTab.QUICK -> QuickPresetScreen()
        MainTab.PERSONALIZE -> PersonalizeScreen(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            themeStyle = themeStyle,
            onThemeStyleChange = onThemeStyleChange,
            uiStyle = uiStyle,
            onUiStyleChange = onUiStyleChange,
            backgroundPath = backgroundPath,
            onBackgroundPathChange = onBackgroundPathChange,
            appLang = appLang,
            onLangChange = onLangChange
        )
        MainTab.SETTINGS -> SettingsScreen(onShowAbout = onShowAbout)
    }
}
