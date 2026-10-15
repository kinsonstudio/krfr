package com.github.krfr.data

import android.content.Context
import com.github.krfr.ui.theme.ThemeMode

/** 应用设置的持久化存储 */
object SettingsStore {
    private const val PREFS = "krfr_settings"
    private const val KEY_THEME_MODE = "theme_mode"

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
}
