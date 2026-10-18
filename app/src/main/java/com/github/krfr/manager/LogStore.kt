package com.github.krfr.manager

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 运行日志：内存环形缓冲 + 持久化到 SharedPreferences，供设置页查看
object LogStore {
    private const val PREFS = "krfr_logs"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 200

    data class Entry(
        val time: Long,
        val level: String,
        val message: String
    ) {
        fun timeText(): String =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(time))
    }

    private val entries = ArrayDeque<Entry>()

    @Synchronized
    fun add(level: String, message: String) {
        val entry = Entry(System.currentTimeMillis(), level, message)
        entries.addLast(entry)
        while (entries.size > MAX_ENTRIES) {
            entries.removeFirst()
        }
    }

    fun i(message: String) = add("I", message)
    fun w(message: String) = add("W", message)
    fun e(message: String) = add("E", message)

    @Synchronized
    fun snapshot(): List<Entry> = entries.toList()

    /** 格式化全文，用于复制 / 导出 */
    @Synchronized
    fun exportText(): String {
        if (entries.isEmpty()) {
            return LogTexts.t("no_logs")
        }
        return buildString {
            appendLine(LogTexts.t("log_title", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())))
            appendLine("----------------")
            entries.forEach { entry ->
                appendLine("${entry.timeText()} [${entry.level}] ${entry.message}")
            }
        }
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }

    @Synchronized
    fun persist(context: Context) {
        val json = JSONArray()
        entries.forEach { entry ->
            json.put(
                JSONObject()
                    .put("t", entry.time)
                    .put("l", entry.level)
                    .put("m", entry.message)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ENTRIES, json.toString()).apply()
    }

    /** 仅当内存为空时加载持久化日志；否则保留当前会话日志（避免打开日志时把新日志清掉） */
    @Synchronized
    fun restore(context: Context) {
        if (entries.isNotEmpty()) {
            return
        }
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ENTRIES, null) ?: return
        runCatching {
            val json = JSONArray(raw)
            for (index in 0 until json.length()) {
                val obj = json.getJSONObject(index)
                entries.addLast(
                    Entry(
                        obj.optLong("t", 0L),
                        obj.optString("l", "I"),
                        obj.optString("m", "")
                    )
                )
            }
            while (entries.size > MAX_ENTRIES) {
                entries.removeFirst()
            }
        }
    }
}
