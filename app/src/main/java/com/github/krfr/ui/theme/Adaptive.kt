package com.github.krfr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** 随主题切换的表面与强调色，全部取自 colorScheme，不依赖系统深浅判断 */
object KrfrAdaptive {
    @Composable
    fun card(): Color = MaterialTheme.colorScheme.surface

    @Composable
    fun wash(): Color = MaterialTheme.colorScheme.surfaceVariant

    @Composable
    fun link(): Color = MaterialTheme.colorScheme.primary

    /** 实心链接色块上的前景（图标/白字） */
    @Composable
    fun onLink(): Color = MaterialTheme.colorScheme.onPrimary

    @Composable
    fun linkSoft(): Color = MaterialTheme.colorScheme.primaryContainer

    @Composable
    fun okBg(): Color {
        val surface = MaterialTheme.colorScheme.surface
        return if (surface.luminance() < 0.5f) {
            Color(0xFF1B3B31)
        } else {
            KrfrOkBg
        }
    }

    @Composable
    fun codeAccent(): Color = MaterialTheme.colorScheme.tertiary

    @Composable
    fun error(): Color = MaterialTheme.colorScheme.error

    @Composable
    fun errorContainer(): Color = MaterialTheme.colorScheme.errorContainer
}
