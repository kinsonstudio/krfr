package com.github.krfr.data

import android.content.Context
import com.github.krfr.ui.theme.ThemeMode
import com.github.krfr.ui.theme.ThemeStyle

/** 应用设置的持久化存储 */
object SettingsStore {
    private const val PREFS = "krfr_settings"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_THEME_STYLE = "theme_style"
    private const val KEY_BACKGROUND_PATH = "background_path"

    fun readThemeMode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun writeThemeMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    fun readThemeStyle(context: Context): ThemeStyle {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return runCatching {
            ThemeStyle.valueOf(prefs.getString(KEY_THEME_STYLE, ThemeStyle.PAPER_GREEN.name)!!)
        }.getOrDefault(ThemeStyle.PAPER_GREEN)
    }

    fun writeThemeStyle(context: Context, style: ThemeStyle) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_STYLE, style.name)
            .apply()
    }

    /** 自定义背景图本地路径；为空表示使用默认纯色背景 */
    fun readBackgroundPath(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BACKGROUND_PATH, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun writeBackgroundPath(context: Context, path: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BACKGROUND_PATH, path)
            .apply()
    }
}
