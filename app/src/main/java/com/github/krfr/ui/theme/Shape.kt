package com.github.krfr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 大圆角：选择框 / 卡片 / pill 按钮 */
val KrfrPill = RoundedCornerShape(50)
val KrfrSelectShape = RoundedCornerShape(24.dp)
val KrfrCardShape = RoundedCornerShape(18.dp)
val KrfrChipShape = RoundedCornerShape(12.dp)

val KrfrShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
