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

    @Volatile
    private var cachedAppContext: Context? = null

    /** 供非 UI 层（如日志模块）取当前语言使用，应用启动时注入一次 */
    fun initApp(context: Context) {
        cachedAppContext = context.applicationContext
    }

    /** 无 context 时取当前生效语言；未初始化缓存返回 "en" 兜底 */
    fun currentLangCached(): String {
        val ctx = cachedAppContext ?: return "en"
        return currentLang(ctx)
    }

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

    /** 手机系统默认语言对应的应用语言：简体→zh-CN、繁体→zh-TW、其他→en */
    fun systemDefaultLang(context: Context): String {
        val locale = Locale.getDefault()
        return if (locale.language == "zh") {
            if (locale.country == "TW" || locale.country == "HK" || locale.country == "MO") {
                "zh-TW"
            } else {
                "zh-CN"
            }
        } else {
            "en"
        }
    }

    /** 当前生效语言：已设置用设置值，否则自动匹配系统语言 */
    fun currentLang(context: Context): String {
        return readLanguage(context) ?: systemDefaultLang(context)
    }

    /** 把 context 的资源语言切到当前生效语言（attachBaseContext 用）：
     *  已设置用设置值，未设置自动匹配系统语言 */
    fun applyLocale(base: Context): Context {
        val lang = currentLang(base)
        val locale = resolveLocale(lang)
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
