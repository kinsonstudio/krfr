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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Bolt
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.krfr.ui.screens.ConfigScreen
import com.github.krfr.ui.screens.QuickPresetScreen
import com.github.krfr.ui.screens.SettingsScreen
import com.github.krfr.ui.screens.SimStatusScreen
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.ui.theme.ThemeMode

/** 底部导航四个主页面 */
enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CONFIG("配置", Icons.Filled.Tune, Icons.Outlined.Tune),
    STATUS("SIM 状态", Icons.Filled.SimCard, Icons.Outlined.SimCard),
    QUICK("快捷方案", Icons.Filled.Bolt, Icons.Outlined.Bolt),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScaffold(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onShowAbout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.CONFIG) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
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
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = {
                            Text(
                                tab.label,
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
                    // 按切换方向做横向滑动 + 缩放 + 淡入淡出，比纯淡入更顺滑
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
                    MainTab.SETTINGS -> SettingsScreen(
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        onShowAbout = onShowAbout
                    )
                }
            }
        }
    }
}
