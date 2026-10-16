package com.github.krfr.data

import android.content.Context
import java.util.Locale

/**
 * 应用语言管理。首次启动未选择时返回 null（触发语言向导），
 * 可选值：en（英文，默认）/ zh-CN（简体中文）/ zh-TW（繁体中文）。
 */
object LanguageManager {
    private const val PREFS = "krfr_settings"
    private const val KEY_LANG = "app_lang"

    fun readLanguage(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, null)
    }

    fun writeLanguage(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, lang)
            .apply()
    }

    /** 当前语言对应的 Locale；未选择时默认英文 */
    fun resolveLocale(lang: String?): Locale {
        return when (lang) {
            "zh-CN" -> Locale("zh", "CN")
            "zh-TW" -> Locale("zh", "TW")
            else -> Locale.ENGLISH
        }
    }

    /** 把 context 的资源语言切到当前设置（attachBaseContext 用） */
    fun applyLocale(base: Context): Context {
        val lang = readLanguage(base)
        val locale = resolveLocale(lang)
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
