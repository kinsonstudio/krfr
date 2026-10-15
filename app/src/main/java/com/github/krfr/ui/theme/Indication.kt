package com.github.krfr.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer

/** 不绘制任何按压/涟漪背景 */
object NoIndication : Indication {
    @Deprecated("Super method is deprecated")
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        return NoIndicationInstance
    }

    private object NoIndicationInstance : IndicationInstance {
        override fun ContentDrawScope.drawIndication() = Unit
    }
}

/** 可点击但不出现灰色/涟漪背景 */
fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

/** 可点击 + 按压时轻微缩放（0.97），松开回弹，无涟漪背景 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.clickablePressScale(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(130),
        label = "press-scale"
    )
    clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    ).graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
