package com.github.krfr.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.krfr.ui.theme.clickableNoIndication
import com.github.krfr.ui.theme.isDarkSurface
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.roundToInt

/**
 * 液态玻璃底部栏 —— 完全按 Kyant0/AndroidLiquidGlass 的官方结构实现：
 * 1. 容器：vibrancy + blur + lens 折射（真实 RenderEffect）
 * 2. 选中项：独立玻璃透镜层，带 chromaticAberration 色散、高光、投影、内阴影
 * 3. 图标文字：绘制在玻璃之上，点击命中优先
 */
@Composable
fun LiquidBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val dark = isDarkSurface()
    val capsule = RoundedCornerShape(32.dp)
    // 官方配色：浅色 0xFAFAFA/40%，深色 0x121212/40%
    val containerColor = if (dark) Color(0xFF121212).copy(alpha = 0.40f) else Color(0xFFFAFAFA).copy(alpha = 0.40f)
    val tabs = MainTab.entries
    val density = LocalDensity.current

    val animatedIndex by animateFloatAsState(
        targetValue = selected.ordinal.toFloat(),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 340f),
        label = "liquid-tab-index"
    )

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(64.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val tabWidthPx = with(density) {
            (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabs.size
        }
        val tabWidthDp = with(density) { tabWidthPx.toDp() }

        // ---------- 第 1 层：容器玻璃（vibrancy + blur + lens 折射） ----------
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { capsule },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx())
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow.Default },
                    innerShadow = { InnerShadow(radius = 4.dp, alpha = 0.14f) },
                    onDrawSurface = { drawRect(containerColor) }
                )
        )

        // ---------- 第 2 层：选中项玻璃透镜（色散折射，随选中弹性滑动） ----------
        Box(
            Modifier
                .offset {
                    IntOffset(
                        x = (4.dp.toPx() + animatedIndex * tabWidthPx).roundToInt(),
                        y = 4.dp.toPx().roundToInt()
                    )
                }
                .width(tabWidthDp)
                .height(56.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { capsule },
                    effects = {
                        lens(
                            10.dp.toPx(),
                            14.dp.toPx(),
                            chromaticAberration = true
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.9f) },
                    shadow = { Shadow(alpha = 0.45f) },
                    innerShadow = { InnerShadow(radius = 8.dp, alpha = 0.28f) },
                    onDrawSurface = {
                        // 平时几乎透明，只有极淡的一层，靠折射与高光表现体积
                        drawRect(
                            if (dark) Color.White.copy(alpha = 0.10f)
                            else Color.Black.copy(alpha = 0.06f)
                        )
                    }
                )
        )

        // ---------- 第 3 层：图标与文字 ----------
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = selected == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickableNoIndication { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelRes),
                        tint = if (isSelected) {
                            if (dark) Color.White else Color(0xFF141414)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSelected) {
                            if (dark) Color.White else Color(0xFF141414)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** 占位（历史兼容，无实际使用） */
@Composable
internal fun BottomBarSpacer(height: Int = 84) {
    Box(Modifier.fillMaxSize().height(height.dp))
}
