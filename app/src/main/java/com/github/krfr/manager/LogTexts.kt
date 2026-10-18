package com.github.krfr.manager

import com.github.krfr.data.LanguageManager

/**
 * 运行日志文本：跟随当前界面语言生成（英文 / 简体中文 / 繁體中文）。
 * 词典为代码内三语映射，写入 LogStore 的即为最终本地化文本。
 */
object LogTexts {

    private val EN = mapOf(
        "sim_status_done" to "SIM status read done in %sms · cards=%s",
        "boot_write_failed" to "Boot re-write failed: %s",
        "badge_clear_failed" to "Failed to clear residual badge: %s",
        "sim_override_state" to "SIM%s(subId=%s) override source=%s value=%s",
        "sim_read_done" to "SIM read done in %sms · cards=%s",
        "reset_failed" to "Restore failed: %s",
        "reset_all_done" to "Reset all SIMs done in %sms · cards=%s",
        "config_set" to "Set config subId=%s country=%s carrier=%s",
        "config_saved" to "Saved subId=%s country=%s carrier=%s",
        "config_reset" to "Restored subId=%s in %sms",
        "confirm_timeout" to "Config submitted but confirmation timed out subId=%s bundle=%s; system will apply asynchronously",
        "preset_apply_failed" to "Preset apply error: %s",
        "backup_restored" to "Backup restored, %s slot(s) applied",
        "backup_restore_failed" to "Backup restore error: %s",
        "log_export_failed" to "Log export failed: %s",
        "log_copy_failed" to "Log copy failed: %s",
        "reset_all_failed" to "Reset-all error: %s",
        "restore_exception" to "Restore error: %s",
        "save_exception" to "Save error: %s",
        "sim_name_save_failed" to "SIM display name save failed subId=%s: %s",
        "sim_name_reset_failed" to "SIM display name reset failed subId=%s: %s",
        "log_title" to "krfr run log · %s",
        "no_logs" to "(no logs yet)"
    )

    private val ZH_CN = mapOf(
        "sim_status_done" to "读取 SIM 状态完成 用时 %sms 卡数=%s",
        "boot_write_failed" to "开机回写失败: %s",
        "badge_clear_failed" to "清除残留角标失败: %s",
        "sim_override_state" to "SIM%s(subId=%s) 覆盖状态来源=%s 值=%s",
        "sim_read_done" to "读取 SIM 卡完成 用时 %sms 卡数=%s",
        "reset_failed" to "还原失败: %s",
        "reset_all_done" to "一键还原全部完成 用时 %sms 卡数=%s",
        "config_set" to "设置配置 subId=%s 国家码=%s 运营商=%s",
        "config_saved" to "已保存 subId=%s 国家码=%s 运营商=%s",
        "config_reset" to "已还原 subId=%s 用时 %sms",
        "confirm_timeout" to "配置已提交但生效确认超时 subId=%s bundle=%s，系统会继续异步生效",
        "preset_apply_failed" to "快捷方案应用异常: %s",
        "backup_restored" to "恢复备份完成，已应用 %s 个卡槽",
        "backup_restore_failed" to "恢复备份异常: %s",
        "log_export_failed" to "导出日志失败: %s",
        "log_copy_failed" to "复制日志失败: %s",
        "reset_all_failed" to "全部还原异常: %s",
        "restore_exception" to "还原异常: %s",
        "save_exception" to "保存异常: %s",
        "sim_name_save_failed" to "SIM 显示名保存失败 subId=%s: %s",
        "sim_name_reset_failed" to "SIM 显示名重置失败 subId=%s: %s",
        "log_title" to "krfr 运行日志 · %s",
        "no_logs" to "（暂无日志）"
    )

    private val ZH_TW = mapOf(
        "sim_status_done" to "讀取 SIM 狀態完成 用時 %sms 卡數=%s",
        "boot_write_failed" to "開機回寫失敗: %s",
        "badge_clear_failed" to "清除殘留角標失敗: %s",
        "sim_override_state" to "SIM%s(subId=%s) 覆寫狀態來源=%s 值=%s",
        "sim_read_done" to "讀取 SIM 卡完成 用時 %sms 卡數=%s",
        "reset_failed" to "還原失敗: %s",
        "reset_all_done" to "一鍵還原全部完成 用時 %sms 卡數=%s",
        "config_set" to "設定配置 subId=%s 國家碼=%s 電訊商=%s",
        "config_saved" to "已儲存 subId=%s 國家碼=%s 電訊商=%s",
        "config_reset" to "已還原 subId=%s 用時 %sms",
        "confirm_timeout" to "配置已提交但生效確認逾時 subId=%s bundle=%s，系統會繼續非同步生效",
        "preset_apply_failed" to "快捷方案套用異常: %s",
        "backup_restored" to "還原備份完成，已套用 %s 個卡槽",
        "backup_restore_failed" to "還原備份異常: %s",
        "log_export_failed" to "匯出日誌失敗: %s",
        "log_copy_failed" to "複製日誌失敗: %s",
        "reset_all_failed" to "全部還原異常: %s",
        "restore_exception" to "還原異常: %s",
        "save_exception" to "儲存異常: %s",
        "sim_name_save_failed" to "SIM 顯示名稱儲存失敗 subId=%s: %s",
        "sim_name_reset_failed" to "SIM 顯示名稱重設失敗 subId=%s: %s",
        "log_title" to "krfr 執行日誌 · %s",
        "no_logs" to "（暫無日誌）"
    )

    /** 取当前语言下的日志文本（未初始化缓存时兜底英文） */
    fun t(key: String, vararg args: Any?): String {
        val lang = runCatching { LanguageManager.currentLangCached() }.getOrNull() ?: "en"
        val template = when (lang) {
            "zh-CN" -> ZH_CN[key]
            "zh-TW" -> ZH_TW[key]
            else -> EN[key]
        } ?: EN[key] ?: key
        return if (args.isEmpty()) template else template.format(*args)
    }
}
