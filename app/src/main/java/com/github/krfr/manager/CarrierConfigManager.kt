package com.github.krfr.manager

// 单 APK：Shizuku 直连 CarrierConfig，受限时委托 shell 权限回退；不用 am instrument shell（会 force-stop 闪退）。

import android.content.Context
import android.os.PersistableBundle
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.CarrierConfigManager as AndroidCarrierConfigManager
import com.github.krfr.model.SimCardInfo
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import java.io.InputStream

object CarrierConfigManager {
    private val PHONE_ID_PATTERN = Regex("""^Phone Id = (\d+)""")
    private const val PREFS_SESSION = "krfr_session"
    private const val KEY_CLEARED_BOOT_COUNT = "cleared_boot_count"

    /** 本进程内对「无覆盖却有角标」再强清一次即可，避免每次刷新都阻塞。 */
    @Volatile
    private var orphanScrubDone = false


    private data class TimedSnapshot(val value: Any?, val at: Long)

    @Volatile
    private var isubSnapshotCache: TimedSnapshot? = null

    @Volatile
    private var currentConfigsCache: TimedSnapshot? = null

    private val READ_CACHE_TTL_MS = 3_000L

    private fun invalidateReadCaches() {
        isubSnapshotCache = null
        currentConfigsCache = null
    }

    init {
        HiddenApiBypass.addHiddenApiExemptions("L")
        HiddenApiBypass.addHiddenApiExemptions("I")
    }

    fun getSimCards(context: Context): List<SimCardInfo> {
        val startedAt = System.currentTimeMillis()
        // 注意：不在这里把 dumpsys 快照写进 prefs（rememberLiveOverrides）。
        // 还原后 dumpsys 短暂残留旧值会被误记，开机回写会让已还原的覆盖“复活”。
        runCatching { ensureClearedAfterReboot(context) }
            .onFailure { LogStore.w("开机回写失败: ${it.message.orEmpty()}") }
        runCatching { scrubOrphanBadges(context) }
            .onFailure { LogStore.w("清除残留角标失败: ${it.message.orEmpty()}") }

        val isubSnapshot = getIsubSnapshotByDumpsys()
        val configsByPhoneId = getCurrentConfigsByDumpsys()
        val simCards = mutableListOf<SimCardInfo>()

        for (slotIndex in 0..1) {
            val slot = slotIndex + 1
            val subId = isubSnapshot?.slotToSubId?.get(slotIndex)
                ?: getSubIdForSlotViaApi(context, slotIndex)
                ?: continue
            // 覆盖状态以 dumpsys 的 mOverrideConfigs（CarrierConfigService 真实覆盖表）为准：
            // 还原后 ICarrierConfigLoader.getConfigForSubId 的合并缓存可能残留旧值（需 notify 刷新），
            // dumpsys 才是系统真实状态；loader / public API 仅在 dumpsys 拿不到时兜底。
            var configSource = "dumpsys"
            val config = when {
                configsByPhoneId.containsKey(slotIndex) -> configsByPhoneId[slotIndex] ?: emptyMap()
                else -> {
                    getCurrentConfigByLoader(context, subId)?.let {
                        configSource = "loader"
                        it
                    } ?: runCatching { getCurrentConfigByPublicApi(context, subId) }.getOrNull()?.let {
                        configSource = "api"
                        it
                    } ?: emptyMap()
                }
            }
            val carrierName = getCarrierNameBySubId(
                context,
                subId,
                slotIndex,
                config,
                isubSnapshot
            )
            if (config.isNotEmpty()) {
                LogStore.i("SIM$slot(subId=$subId) 覆盖状态来源=$configSource 值=$config")
            }
            simCards.add(SimCardInfo(slot, subId, carrierName, config))
        }

        LogStore.i("读取 SIM 卡完成 用时 ${System.currentTimeMillis() - startedAt}ms 卡数=${simCards.size}")
        return simCards
    }

    /**
     * 每次开机后首次把已保存的卡1/卡2覆盖写回去（不清空）。
     * 同一开机周期内不重复全量回写，避免打断当前会话刚保存的设置。
     */
    fun ensureClearedAfterReboot(context: Context): Boolean {
        if (!ShizukuHelper.hasPermission()) {
            return false
        }
        val bootCount = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.BOOT_COUNT,
            -1
        )
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        val lastCleared = prefs.getInt(KEY_CLEARED_BOOT_COUNT, -2)
        if (bootCount == lastCleared) {
            return false
        }
        reapplySavedOverrides(context, prefs)
        prefs.edit().putInt(KEY_CLEARED_BOOT_COUNT, bootCount).apply()
        orphanScrubDone = true
        return true
    }

    /**
     * 对全部 SIM 执行还原。两张卡并行处理，每张卡只走一遍完整流程
     * （resetCarrierConfig 内部已含覆盖清除 + SPN 同步 + 生效确认），
     * 不再叠加第二/第三段重复的阻塞同步，避免还原卡几十秒。
     */
    fun restoreAllToUnset(context: Context) {
        if (!ShizukuHelper.hasPermission()) {
            throw IllegalStateException("Shizuku 未启动或未授权")
        }
        val startedAt = System.currentTimeMillis()
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        val futures = (0..1).mapNotNull { slotIndex ->
            val subId = getSubIdForSlotViaApi(context, slotIndex) ?: return@mapNotNull null
            executor.submit<Unit> { resetCarrierConfig(context, subId) }
        }
        var failures = 0
        try {
            futures.forEach { future ->
                runCatching { future.get() }.onFailure {
                    failures++
                    LogStore.e("还原失败: ${it.message.orEmpty()}")
                }
            }
        } finally {
            executor.shutdown()
        }
        invalidateReadCaches()
        LogStore.i("一键还原全部完成 用时 ${System.currentTimeMillis() - startedAt}ms 卡数=${futures.size}")
        if (failures > 0) {
            throw IllegalStateException("有 $failures 张卡还原失败，请查看运行日志")
        }
    }

    private fun slotIndexForSubId(context: Context, subId: Int): Int? {
        for (slotIndex in 0..1) {
            if (getSubIdForSlotViaApi(context, slotIndex) == subId) {
                return slotIndex
            }
        }
        return getIsubSnapshotByDumpsys()?.slotToSubId?.entries
            ?.firstOrNull { it.value == subId }
            ?.key
    }

    private fun rememberSavedOverride(
        context: Context,
        subId: Int,
        countryCode: String?,
        carrierName: String?
    ) {
        val slot = slotIndexForSubId(context, subId) ?: return
        context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE).edit()
            .putBoolean("slot_${slot}_set", true)
            .putString("slot_${slot}_cc", countryCode.orEmpty())
            .putString("slot_${slot}_name", carrierName.orEmpty())
            .apply()
    }

    private fun clearSavedOverride(context: Context, subId: Int) {
        val slot = slotIndexForSubId(context, subId) ?: return
        context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE).edit()
            .putBoolean("slot_${slot}_set", false)
            .remove("slot_${slot}_cc")
            .remove("slot_${slot}_name")
            .apply()
    }

    private fun rememberLiveOverrides(
        context: Context,
        configsByPhoneId: Map<Int, Map<String, String>>
    ) {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var changed = false
        for (slotIndex in 0..1) {
            val config = configsByPhoneId[slotIndex] ?: continue
            val countryCode = config["国家码"].orEmpty()
            val carrierName = config["运营商名称"].orEmpty()
            if (countryCode.isEmpty() && carrierName.isEmpty()) {
                continue
            }
            editor.putBoolean("slot_${slotIndex}_set", true)
            editor.putString("slot_${slotIndex}_cc", countryCode)
            editor.putString("slot_${slotIndex}_name", carrierName)
            changed = true
        }
        if (changed) {
            editor.apply()
        }
    }

    private fun reapplySavedOverrides(
        context: Context,
        prefs: android.content.SharedPreferences
    ) {
        for (slotIndex in 0..1) {
            if (!prefs.getBoolean("slot_${slotIndex}_set", false)) {
                continue
            }
            val subId = getSubIdForSlotViaApi(context, slotIndex) ?: continue
            val countryCode = prefs.getString("slot_${slotIndex}_cc", null)?.takeIf { it.isNotEmpty() }
            val carrierName = prefs.getString("slot_${slotIndex}_name", null)?.takeIf { it.isNotEmpty() }
            if (countryCode == null && carrierName == null) {
                continue
            }
            runCatching { setCarrierConfig(context, subId, countryCode, carrierName) }
        }
    }

    /**
     * 无 CarrierConfig 覆盖时，再强清一次残留角标（本进程最多一次）。
     */
    fun scrubOrphanBadges(context: Context) {
        if (orphanScrubDone || !ShizukuHelper.hasPermission()) {
            return
        }
        val configsByPhoneId = getCurrentConfigsByDumpsys()
        var didClear = false
        for (slotIndex in 0..1) {
            val subId = getSubIdForSlotViaApi(context, slotIndex) ?: continue
            val config = configsByPhoneId[slotIndex]
                ?: getCurrentConfigByLoader(context, subId)
                ?: getCurrentConfigByPublicApi(context, subId)
            if (config.isNotEmpty()) {
                continue
            }
            didClear = true
            runCatching {
                DelegatedCarrierConfigOverride.runWithShellPermission {
                    SubscriptionHiddenApi.syncAfterRestoreBlocking(context, subId)
                }
            }
            runCatching {
                SubscriptionHiddenApi.clearBrandThoroughly(context, subId)
            }
        }
        if (didClear) {
            orphanScrubDone = true
        }
    }

    private data class IsubSnapshot(
        val slotToSubId: Map<Int, Int>,
        val subIdToCarrierName: Map<Int, String>
    )

    private val LOGICAL_SLOT_PATTERN = Regex("""Logical SIM slot (\d+): subId=(\d+)""")

    fun setCarrierConfig(
        context: Context,
        subId: Int,
        countryCode: String?,
        carrierName: String? = null
    ): CarrierConfigOperationResult {
        val startedAt = System.currentTimeMillis()
        LogStore.i("设置配置 subId=$subId 国家码=${countryCode ?: "-"} 运营商=${carrierName ?: "-"}")
        val bundle = PersistableBundle()

        if (!countryCode.isNullOrEmpty() && countryCode.length == 2) {
            bundle.putString(
                AndroidCarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING,
                countryCode.lowercase()
            )
        }

        if (!carrierName.isNullOrEmpty()) {
            bundle.putBoolean(AndroidCarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
            bundle.putString(AndroidCarrierConfigManager.KEY_CARRIER_NAME_STRING, carrierName)
            // 与 AOSP CTS 一致，提高 SPN 覆盖成功率（尤其副卡）
            bundle.putBoolean(AndroidCarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL, true)
        }

        overrideCarrierConfig(context, subId, bundle)
        rememberSavedOverride(context, subId, countryCode, carrierName)
        invalidateReadCaches()
        LogStore.i("已保存 subId=$subId 国家码=${countryCode.orEmpty()} 运营商=${carrierName.orEmpty()}")
        return CarrierConfigOperationResult()
    }

    fun resetCarrierConfig(context: Context, subId: Int): CarrierConfigOperationResult {
        val startedAt = System.currentTimeMillis()
        overrideCarrierConfig(context, subId, null)
        clearSavedOverride(context, subId)
        invalidateReadCaches()
        LogStore.i("已还原 subId=$subId 用时 ${System.currentTimeMillis() - startedAt}ms")
        return CarrierConfigOperationResult()
    }

    /**
     * @return true 表示走了异步 instrumentation 回退（调用方会在返回前等待配置生效）
     */
    private fun overrideCarrierConfig(
        context: Context,
        subId: Int,
        bundle: PersistableBundle?
    ): Boolean {
        var usedInstrumentation = false
        try {
            TelephonyHiddenApi.overrideConfig(subId, bundle, persistent = false)
            syncSubscriptionAfterOverride(context, subId, bundle)
            // 还原（null）只等 2s，设置等 3s；避免配置生效确认把保存/还原拖慢
            if (!waitUntilConfigMatches(context, subId, bundle, timeoutMs = if (bundle == null) 2_000L else 3_000L)
                && bundle == null
            ) {
                throw IllegalStateException("还原未生效：系统仍存在覆盖配置，请查看运行日志")
            }
            return false
        } catch (error: SecurityException) {
            if (!error.message.orEmpty().contains("cannot be invoked by shell", ignoreCase = true)) {
                throw TelephonyHiddenApi.unwrapCause(error)
            }
        } catch (error: Throwable) {
            val message = TelephonyHiddenApi.unwrapCause(error).message.orEmpty()
            if (!message.contains("cannot be invoked by shell", ignoreCase = true)) {
                throw TelephonyHiddenApi.unwrapCause(error)
            }
        }

        val delegatedError = runCatching {
            DelegatedCarrierConfigOverride.overrideConfigWithFallback(
                context,
                subId,
                bundle,
                persistent = false
            )
        }
        if (delegatedError.isSuccess) {
            syncSubscriptionAfterOverride(context, subId, bundle)
            if (!waitUntilConfigMatches(context, subId, bundle, timeoutMs = if (bundle == null) 2_000L else 3_000L)
                && bundle == null
            ) {
                throw IllegalStateException("还原未生效：系统仍存在覆盖配置，请查看运行日志")
            }
            return false
        }

        if (!PrivilegedCarrierConfigRunner.overrideConfig(context, subId, bundle)) {
            throw IllegalStateException("无法启动后台服务应用配置")
        }
        usedInstrumentation = true
        // instrumentation 进程会立即退出，主进程延迟再同步一次 SPN
        if (bundle == null) {
            SubscriptionHiddenApi.syncAfterRestore(context, subId)
        } else {
            SubscriptionHiddenApi.syncAfterOverride(context, subId, bundle)
        }
        // 配置已提交成功；生效确认超时不抛错（系统会异步生效，误报会让页面停留在旧状态）
        if (!waitUntilConfigMatches(context, subId, bundle, timeoutMs = 6_000L)) {
            if (bundle == null) {
                // 还原（清空覆盖）必须确认真正生效，避免“假成功”导致页面一直显示已覆盖
                throw IllegalStateException("还原未生效：系统仍存在覆盖配置，请查看运行日志")
            }
            LogStore.w("配置已提交但生效确认超时 subId=$subId bundle=${bundle != null}，系统会继续异步生效")
        }
        return usedInstrumentation
    }

    private fun waitUntilConfigMatches(
        context: Context,
        subId: Int,
        expectedBundle: PersistableBundle?,
        timeoutMs: Long = 5_000L
    ): Boolean {
        val phoneId = getPhoneIdForSubId(context, subId) ?: return true
        val expected = expectedBundle?.let { parseConfigBundle(it) } ?: emptyMap()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            // 轮询期间强制重读 dumpsys，避免 3s 缓存让残留旧值一直命中
            if (configMatches(readOverrideConfigForPhoneId(context, phoneId, forceDumpsysRefresh = true), expected)) {
                return true
            }
            Thread.sleep(250)
        }
        return configMatches(readOverrideConfigForPhoneId(context, phoneId, forceDumpsysRefresh = true), expected)
    }

    private fun readOverrideConfigForPhoneId(
        context: Context,
        phoneId: Int,
        forceDumpsysRefresh: Boolean = false
    ): Map<String, String> {
        val subId = getIsubSnapshotByDumpsys()?.slotToSubId?.get(phoneId)
            ?: getSubIdForSlotViaApi(context, phoneId)
        // dumpsys 的 mOverrideConfigs 是 CarrierConfigService 的真实覆盖表，优先；
        // loader 合并缓存可能在还原后残留旧值（需 notify 刷新），只作兜底。
        val dumpsysConfig = getCurrentConfigsByDumpsys(forceRefresh = forceDumpsysRefresh)[phoneId]
        if (dumpsysConfig != null) {
            return dumpsysConfig
        }
        if (subId == null) {
            return emptyMap()
        }
        return getCurrentConfigByLoader(context, subId)
            ?: runCatching { getCurrentConfigByPublicApi(context, subId) }.getOrNull()
            ?: emptyMap()
    }

    private fun configMatches(current: Map<String, String>, expected: Map<String, String>): Boolean {
        if (expected.isEmpty()) {
            return current.isEmpty()
        }
        return expected.all { (key, value) -> current[key] == value }
    }

    private fun getPhoneIdForSubId(context: Context, subId: Int): Int? {
        getIsubSnapshotByDumpsys()?.slotToSubId?.entries?.firstOrNull { it.value == subId }?.key?.let {
            return it
        }
        for (slotIndex in 0..1) {
            if (getSubIdForSlotViaApi(context, slotIndex) == subId) {
                return slotIndex
            }
        }
        return null
    }

    private fun syncSubscriptionAfterOverride(context: Context, subId: Int, bundle: PersistableBundle?) {
        if (bundle == null) {
            SubscriptionHiddenApi.syncAfterRestore(context, subId)
        } else {
            SubscriptionHiddenApi.syncAfterOverride(context, subId, bundle)
        }
    }

    private fun ensureShizukuReady() {
        if (!ShizukuHelper.hasPermission()) {
            throw IllegalStateException("Shizuku 未启动或未授权")
        }
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    private fun getIsubSnapshotByDumpsys(): IsubSnapshot? {
        val now = System.currentTimeMillis()
        isubSnapshotCache?.let { cached ->
            if (now - cached.at < READ_CACHE_TTL_MS) {
                return cached.value as? IsubSnapshot
            }
        }
        val fresh = try {
            if (!ShizukuHelper.hasPermission()) {
                null
            } else {
                parseDumpsysIsub(runShellCommand("dumpsys isub"))
            }
        } catch (e: Exception) {
            null
        }
        isubSnapshotCache = TimedSnapshot(fresh, now)
        return fresh
    }

    private fun parseDumpsysIsub(output: String): IsubSnapshot {
        val slotToSubId = mutableMapOf<Int, Int>()
        LOGICAL_SLOT_PATTERN.findAll(output).forEach { match ->
            val slot = match.groupValues[1].toIntOrNull() ?: return@forEach
            val subId = match.groupValues[2].toIntOrNull() ?: return@forEach
            slotToSubId[slot] = subId
        }

        val subIdToCarrierName = mutableMapOf<Int, String>()
        output.lineSequence().forEach { line ->
            if (!line.contains("SubscriptionInfoInternal: id=")) {
                return@forEach
            }
            val subId = Regex("""\bid=(\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull()
                ?: return@forEach
            val carrierName = Regex("""\bcarrierName=([^\s]+)""")
                .find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            val displayName = Regex("""\bdisplayName=([^\s]+)""")
                .find(line)?.groupValues?.get(1)
                ?.replace(Regex("""\d+$"""), "")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            val mcc = Regex("""\bmcc=(\d+)""").find(line)?.groupValues?.get(1)
            val mnc = Regex("""\bmnc=(\d+)""").find(line)?.groupValues?.get(1)
            val name = displayName
                ?: carrierName
                ?: resolveCarrierNameByMccMnc(
                    if (mcc != null && mnc != null) mcc + mnc.padStart(2, '0') else null
                )
            if (!name.isNullOrBlank()) {
                subIdToCarrierName[subId] = name
            }
            val slotIndex = Regex("""\bsimSlotIndex=(\d+)""")
                .find(line)?.groupValues?.get(1)?.toIntOrNull()
            if (slotIndex != null && slotIndex >= 0) {
                slotToSubId.putIfAbsent(slotIndex, subId)
            }
        }

        return IsubSnapshot(slotToSubId, subIdToCarrierName)
    }

    private fun getSubIdForSlotViaApi(context: Context, slotIndex: Int): Int? {
        val manager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return null
        return runCatching {
            manager.getActiveSubscriptionInfoForSimSlotIndex(slotIndex)?.subscriptionId
                ?.takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
        }.getOrNull() ?: runCatching {
            val getSubId = SubscriptionManager::class.java.getDeclaredMethod(
                "getSubId",
                Int::class.javaPrimitiveType
            )
            val subIds = getSubId.invoke(null, slotIndex) as? IntArray ?: return null
            subIds.firstOrNull { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
        }.getOrNull()
    }

    private fun getCurrentConfigByLoader(context: Context, subId: Int): Map<String, String>? {
        return try {
            val config = TelephonyHiddenApi.getConfigForSubId(context.packageName, subId)
                ?: return null
            parseConfigBundle(config)
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentConfigByPublicApi(context: Context, subId: Int): Map<String, String> {
        return try {
            val manager = context.getSystemService(AndroidCarrierConfigManager::class.java)
                ?: return emptyMap()
            val config = manager.getConfigForSubId(subId) ?: return emptyMap()
            parseConfigBundle(config)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun parseConfigBundle(config: PersistableBundle): Map<String, String> {
        val result = mutableMapOf<String, String>()

        config.getString(AndroidCarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING)?.let {
            result["国家码"] = it
        }

        if (config.getBoolean(AndroidCarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, false)) {
            config.getString(AndroidCarrierConfigManager.KEY_CARRIER_NAME_STRING)?.let {
                result["运营商名称"] = it
            }
        }

        return result
    }

    private fun getCurrentConfigsByDumpsys(forceRefresh: Boolean = false): Map<Int, Map<String, String>> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            currentConfigsCache?.let { cached ->
                if (now - cached.at < READ_CACHE_TTL_MS) {
                    return cached.value as? Map<Int, Map<String, String>> ?: emptyMap()
                }
            }
        }
        val fresh = try {
            if (!ShizukuHelper.hasPermission()) {
                emptyMap()
            } else {
                parseDumpsysCarrierConfig(runShellCommand("dumpsys carrier_config"))
            }
        } catch (e: Exception) {
            emptyMap()
        }
        currentConfigsCache = TimedSnapshot(fresh, now)
        return fresh
    }

    private fun parseDumpsysCarrierConfig(output: String): Map<Int, Map<String, String>> {
        val result = mutableMapOf<Int, Map<String, String>>()
        var currentPhoneId: Int? = null
        var readingOverrideConfig = false
        val rawValues = mutableMapOf<String, String>()

        fun flushCurrentConfig() {
            val phoneId = currentPhoneId ?: return
            val config = mutableMapOf<String, String>()
            rawValues["sim_country_iso_override_string"]?.takeIf { it.isNotBlank() }?.let {
                config["国家码"] = it
            }
            if (rawValues["carrier_name_override_bool"] == "true") {
                rawValues["carrier_name_string"]?.takeIf { it.isNotBlank() }?.let {
                    config["运营商名称"] = it
                }
            }
            result[phoneId] = config
            rawValues.clear()
        }

        output.lineSequence().forEach { line ->
            PHONE_ID_PATTERN.find(line)?.let { match ->
                flushCurrentConfig()
                currentPhoneId = match.groupValues[1].toIntOrNull()
                readingOverrideConfig = false
                return@forEach
            }

            val trimmed = line.trim()
            if (trimmed.startsWith("mOverrideConfigs")) {
                readingOverrideConfig = !trimmed.endsWith("null")
                if (!readingOverrideConfig) {
                    rawValues.clear()
                }
                return@forEach
            }

            if (!readingOverrideConfig) {
                return@forEach
            }

            if (trimmed.startsWith("m") && trimmed.endsWith(":")) {
                readingOverrideConfig = false
                return@forEach
            }

            val separatorIndex = trimmed.indexOf(" = ")
            if (separatorIndex > 0) {
                rawValues[trimmed.substring(0, separatorIndex)] =
                    trimmed.substring(separatorIndex + 3)
            }
        }

        flushCurrentConfig()
        return result
    }

    private fun getCarrierNameBySubId(
        context: Context,
        subId: Int,
        slotIndex: Int,
        currentConfig: Map<String, String>,
        isubSnapshot: IsubSnapshot?
    ): String {
        currentConfig["运营商名称"]?.takeIf { it.isNotBlank() }?.let { return it }

        isubSnapshot?.subIdToCarrierName?.get(subId)?.takeIf { it.isNotBlank() }?.let { return it }

        val subscriptionManager =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        subscriptionManager?.let { manager ->
            val subscriptionInfo = runCatching {
                manager.getActiveSubscriptionInfo(subId)
            }.getOrNull() ?: runCatching {
                manager.getActiveSubscriptionInfoForSimSlotIndex(slotIndex)
            }.getOrNull()
            subscriptionInfo?.let { info ->
                resolveCarrierNameByMccMnc(
                    String.format("%03d%02d", info.mcc, info.mnc)
                )?.let { return it }

                info.displayName?.toString()?.trim()
                    ?.replace(Regex("""\d+$"""), "")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { return it }
            }
        }

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return ""

        return try {
            val subTelephony = telephonyManager.createForSubscriptionId(subId)
            subTelephony.simOperatorName.takeIf { it.isNotBlank() }
                ?: resolveCarrierNameByMccMnc(subTelephony.simOperator)
                ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun resolveCarrierNameByMccMnc(operatorNumeric: String?): String? {
        if (operatorNumeric.isNullOrBlank()) {
            return null
        }

        return when {
            operatorNumeric.startsWith("46015") -> "中国广电"
            operatorNumeric.startsWith("46003") ||
                operatorNumeric.startsWith("46005") ||
                operatorNumeric.startsWith("46011") -> "中国电信"
            operatorNumeric.startsWith("46001") ||
                operatorNumeric.startsWith("46006") ||
                operatorNumeric.startsWith("46009") -> "中国联通"
            operatorNumeric.startsWith("46000") ||
                operatorNumeric.startsWith("46002") ||
                operatorNumeric.startsWith("46007") ||
                operatorNumeric.startsWith("46008") -> "中国移动"
            else -> null
        }
    }

    private fun runShellCommand(command: String, timeoutMs: Long = 5_000L): String {
        val shellProcess = newShizukuProcess(arrayOf("sh", "-c", command))
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val stdoutThread = readAsync(shellProcess.inputStream, stdout)
        val stderrThread = readAsync(shellProcess.errorStream, stderr)
        if (!shellProcess.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            shellProcess.destroy()
            throw IllegalStateException("执行 shell 命令超时(>${timeoutMs}ms): $command")
        }
        stdoutThread.join()
        stderrThread.join()

        if (shellProcess.exitValue() != 0) {
            throw IllegalStateException(
                listOf(stdout.toString(), stderr.toString())
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                    .ifBlank { "执行 shell 命令失败，退出码: ${shellProcess.exitValue()}" }
            )
        }

        return stdout.toString()
    }

    private fun newShizukuProcess(command: Array<String>): Process {
        val newProcess = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        newProcess.isAccessible = true
        return newProcess.invoke(null, command, null, null) as Process
    }

    private fun readAsync(inputStream: InputStream, output: StringBuilder): Thread {
        return Thread {
            inputStream.bufferedReader().use { reader ->
                output.append(reader.readText())
            }
        }.apply { start() }
    }

    data class CarrierConfigOperationResult(
        val warnings: List<String> = emptyList()
    )
}

internal fun Throwable.toSimCardsUserMessage(): String {
    var current: Throwable = this
    while (current.cause != null) {
        current = current.cause!!
    }
    val message = current.message.orEmpty()
    if (current is SecurityException) {
        return "权限不足，请检查 Shizuku 是否已授权"
    }
    return message.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
        ?: "读取 SIM 配置失败"
}
