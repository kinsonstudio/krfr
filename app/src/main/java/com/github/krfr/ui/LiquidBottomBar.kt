package com.github.krfr.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.krfr.ui.theme.clickableNoIndication
import com.github.krfr.ui.theme.isDarkSurface
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * 液态玻璃底部栏：真实背景磨砂（Kyant0/AndroidLiquidGlass）。
 * 只有这块小面积使用 RenderEffect，滚动列表不参与模糊，保证流畅。
 */
@Composable
fun LiquidBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val dark = isDarkSurface()
    val shape = RoundedCornerShape(30.dp)
    val surfaceTint = if (dark) Color(0xFF0E1513).copy(alpha = 0.50f) else Color.White.copy(alpha = 0.34f)

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(64.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(16.dp.toPx())
                },
                highlight = { Highlight.Default },
                shadow = { Shadow.Default },
                innerShadow = { InnerShadow(radius = 4.dp, alpha = 0.10f) },
                onDrawSurface = {
                    drawRect(surfaceTint)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = selected == tab
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.04f else 0.94f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 320f),
                label = "liquid-tab-scale"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 3.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(
                                    if (dark) Color.White.copy(alpha = 0.14f)
                                    else Color.White.copy(alpha = 0.62f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (dark) Color.White.copy(alpha = 0.20f)
                                    else Color.White.copy(alpha = 0.82f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        } else Modifier
                    )
                    .clickableNoIndication { onSelect(tab) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.scale(scale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelRes),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                        },
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** 让玻璃面板覆盖在内容上的占位（无操作，供布局对齐使用） */
@Composable
internal fun BottomBarSpacer(height: Int = 84) {
    Box(Modifier.fillMaxSize().height(height.dp))
}
