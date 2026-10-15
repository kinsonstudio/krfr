package com.github.krfr.manager

import android.content.Context
import android.telephony.SubscriptionManager
import com.github.krfr.model.SimCardInfo
import org.json.JSONArray
import org.json.JSONObject

// 备份：当前各卡槽覆盖配置导出为 JSON；恢复：按卡槽写回，备份为空的卡槽执行还原。
object BackupManager {

    data class SlotConfig(
        val slot: Int,
        val countryCode: String,
        val carrierName: String
    )

    private const val FORMAT_VERSION = 1

    fun exportJson(simCards: List<SimCardInfo>): String {
        val slots = JSONArray()
        simCards.sortedBy { it.slot }.forEach { card ->
            val config = JSONObject().apply {
                put("slot", card.slot)
                put("countryCode", card.currentConfig["国家码"].orEmpty())
                put("carrierName", card.currentConfig["运营商名称"].orEmpty())
            }
            slots.put(config)
        }
        return JSONObject().apply {
            put("app", "krfr")
            put("version", FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("slots", slots)
        }.toString(2)
    }

    fun parseJson(json: String): List<SlotConfig> {
        val root = JSONObject(json)
        require(root.optString("app").equals("krfr", ignoreCase = true)) {
            "不是 krfr 的备份文件"
        }
        val slots = root.optJSONArray("slots") ?: JSONArray()
        return buildList {
            for (index in 0 until slots.length()) {
                val item = slots.getJSONObject(index)
                val slot = item.optInt("slot", -1)
                if (slot <= 0) continue
                add(
                    SlotConfig(
                        slot = slot,
                        countryCode = item.optString("countryCode").trim().uppercase(),
                        carrierName = item.optString("carrierName").trim()
                    )
                )
            }
        }
    }

    /** 恢复备份：每个卡槽写回或还原。返回应用了配置的卡槽数。 */
    fun applyBackup(context: Context, slots: List<SlotConfig>): Int {
        var applied = 0
        slots.forEach { slotConfig ->
            val subId = subIdForSlot(context, slotConfig.slot) ?: return@forEach
            runCatching {
                if (slotConfig.countryCode.isEmpty() && slotConfig.carrierName.isEmpty()) {
                    CarrierConfigManager.resetCarrierConfig(context, subId)
                } else {
                    CarrierConfigManager.setCarrierConfig(
                        context,
                        subId,
                        slotConfig.countryCode.takeIf { it.length == 2 },
                        slotConfig.carrierName.takeIf { it.isNotEmpty() }
                    )
                }
                applied += 1
            }
        }
        return applied
    }

    private fun subIdForSlot(context: Context, slot: Int): Int? {
        val manager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return null
        return runCatching {
            manager.getActiveSubscriptionInfoForSimSlotIndex(slot - 1)?.subscriptionId
                ?.takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
        }.getOrNull() ?: runCatching {
            val getSubId = SubscriptionManager::class.java.getDeclaredMethod(
                "getSubId",
                Int::class.javaPrimitiveType
            )
            val subIds = getSubId.invoke(null, slot - 1) as? IntArray ?: return null
            subIds.firstOrNull { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
        }.getOrNull()
    }
}
