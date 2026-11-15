package com.github.krfr.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.github.krfr.ui.glass.DampedDragAnimation
import com.github.krfr.ui.glass.InteractiveHighlight
import com.github.krfr.ui.theme.clickableNoIndication
import com.github.krfr.ui.theme.isDarkSurface
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** 按压时底部栏内容的放大倍率（官方用 1 → 1.2），只作用于图标，避免长语言文案被裁切 */
internal val LocalLiquidTabScale = staticCompositionLocalOf<() -> Float> { { 1f } }

/**
 * 液态玻璃底部栏 —— 照 AndroidLiquidGlass 1.0.1 官方 LiquidBottomTabs 结构实现：
 *
 * 1. 容器 Row：vibrancy + blur(8dp) + lens(24,24)，按压时整条栏轻微横向拉伸；
 * 2. tab 图标层再用 rememberLayerBackdrop 录一份（alpha 0，肉眼不可见），
 *    只用主题色 tint，作为胶囊透镜的取样源；
 * 3. 选中胶囊用 rememberCombinedBackdrop(页面 backdrop, tab 层 backdrop)，
 *    于是一颗胶囊同时折射「页面内容」和「被放大的 tab 图标」，按下去才有色散与高光；
 * 4. 所有交互变换写在 drawBackdrop 的 layerBlock 里，保证折射坐标不错位。
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
    val containerColor = if (dark) {
        Color(0xFF141B26).copy(alpha = 0.55f)
    } else {
        Color.White.copy(alpha = 0.52f)
    }
    val accent = if (dark) {
        lerp(MaterialTheme.colorScheme.primary, Color.White, 0.42f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val tabs = MainTab.entries
    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(64.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabs.size
        }

        // 拖动时整条栏的横向弹性位移（官方 panelOffset）
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction =
                    (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember { mutableIntStateOf(selected.ordinal) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selected.ordinal.toFloat(),
                valueRange = 0f..(tabs.size - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStopped = {
                    val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabs.size - 1)
                    currentIndex = target
                    animateToValue(target.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabs.size - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        // 外部（点击 / 状态恢复）改选中项时，把胶囊弹过去
        LaunchedEffect(selected) {
            if (selected.ordinal != currentIndex) {
                currentIndex = selected.ordinal
                dampedDragAnimation.animateToValue(selected.ordinal.toFloat())
            }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onSelect(tabs[index])
                }
        }

        val tabScale = {
            lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
        }
        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        // ---------- 第 1 层：容器玻璃 ----------
        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { capsule },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(20.dp.toPx(), 28.dp.toPx())
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow(radius = 20.dp, alpha = if (dark) 0.34f else 0.16f) },
                    innerShadow = { InnerShadow(radius = 6.dp, alpha = 0.16f) },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(64.dp)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = {
                CompositionLocalProvider(LocalLiquidTabScale provides tabScale) {
                    TabRowContent(selected, onSelect, dark, accent)
                }
            }
        )

        // ---------- 第 2 层：不可见的 tab 图标层（录制源，仅用于折射） ----------
        CompositionLocalProvider(LocalLiquidTabScale provides tabScale) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { capsule },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(24.dp.toPx() * progress, 24.dp.toPx() * progress)
                        },
                        highlight = { Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress) },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(56.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accent)),
                verticalAlignment = Alignment.CenterVertically,
                content = { TabRowContent(selected, onSelect, dark, accent, recording = true) }
            )
        }

        // ---------- 第 3 层：选中胶囊（合成 backdrop + 透镜色散 + 速度形变） ----------
        Box(
            Modifier
                .padding(start = 4.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) {
                            dampedDragAnimation.value * tabWidth + panelOffset
                        } else {
                            val w = constraints.maxWidth.toFloat()
                            (w - (dampedDragAnimation.value + 1f) * tabWidth)
                                .coerceAtLeast(0f) + panelOffset
                        }
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { capsule },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            4.dp.toPx() + 8.dp.toPx() * progress,
                            6.dp.toPx() + 12.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = 0.35f + 0.65f * dampedDragAnimation.pressProgress)
                    },
                    shadow = {
                        Shadow(alpha = 0.35f + 0.65f * dampedDragAnimation.pressProgress)
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 6.dp + 8.dp * dampedDragAnimation.pressProgress,
                            alpha = 0.18f + 0.60f * dampedDragAnimation.pressProgress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (dark) Color.White.copy(0.10f) else Color.Black.copy(0.055f),
                            alpha = 1f - progress
                        )
                        drawRect(Color.White.copy(alpha = 0.05f * progress))
                    }
                )
                .height(56.dp)
                .width(with(density) { tabWidth.toDp() })
        )
    }
}

@Composable
private fun RowScope.TabRowContent(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    dark: Boolean,
    accent: Color,
    recording: Boolean = false
) {
    val scale = LocalLiquidTabScale.current
    MainTab.entries.forEach { tab ->
        val isSelected = tab == selected
        val contentColor = when {
            isSelected -> if (dark) Color.White else Color(0xFF141414)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (recording) Modifier
                    else Modifier.clickableNoIndication { onSelect(tab) }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = stringResource(tab.labelRes),
                tint = if (recording) accent else contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        val s = scale()
                        scaleX = s
                        scaleY = s
                    }
            )
            Text(
                text = stringResource(tab.labelRes),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (recording) accent else contentColor,
                maxLines = 1
            )
        }
    }
}

/** 占位（历史兼容，无实际使用） */
@Composable
internal fun BottomBarSpacer(height: Int = 84) {
    Box(Modifier.fillMaxHeight().height(height.dp))
}
