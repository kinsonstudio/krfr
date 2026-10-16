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
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    val darkBg = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val bgBitmap = remember(backgroundPath) {
        backgroundPath?.let { decodeSampledBitmap(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (darkBg) 0.32f else 0.55f
            )
            if (darkBg) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (bgBitmap != null) Color.Transparent else MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.padding(bottom = 0.dp)
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.08f else 0.88f,
                            animationSpec = tween(220),
                            label = "tab-icon-scale"
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Box(
                                    modifier = Modifier.scale(iconScale)
                                ) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    stringResource(tab.labelRes),
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
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
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
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
}

/** 背景图采样解码：限制到 ~1080px 宽，避免大图 OOM */
private fun decodeSampledBitmap(path: String): android.graphics.Bitmap? {
    return runCatching {
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, opts)
        val target = 1080
        var sample = 1
        while (opts.outWidth / sample > target * 2) sample *= 2
        val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        android.graphics.BitmapFactory.decodeFile(path, decodeOpts)
    }.getOrNull()
}
