package com.github.krfr.manager

import android.os.IBinder
import android.telephony.TelephonyManager
import com.github.krfr.model.SimCardInfo
import com.github.krfr.model.SimStatusInfo
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper

// 读取 SIM 实时状态。不申请电话权限，全部经 Shizuku shell 身份反射系统服务获取。
object SimStatusReader {

    init {
        HiddenApiBypass.addHiddenApiExemptions("L")
        HiddenApiBypass.addHiddenApiExemptions("I")
    }

    // 反射方法缓存：方法查找（methods.firstOrNull 遍历）开销大，缓存后显著提速
    private val methodCache = java.util.concurrent.ConcurrentHashMap<String, java.lang.reflect.Method>()

    // 服务实例缓存：同一进程内 binder 可复用，避免每次读取重复 getService + asInterface
    @Volatile
    private var cachedITelephony: Any? = null

    @Volatile
    private var cachedISub: Any? = null

    fun resetServiceCache() {
        cachedITelephony = null
        cachedISub = null
    }

    private fun findMethod(
        target: Any,
        name: String,
        paramCount: Int,
        paramType: Class<*>? = null
    ): java.lang.reflect.Method? {
        val key = target.javaClass.name + "#" + name + "#" + paramCount + "#" + (paramType?.name ?: "*")
        return methodCache[key] ?: run {
            val method = target.javaClass.methods.firstOrNull { m ->
                m.name == name && m.parameterTypes.size == paramCount &&
                    (paramType == null || m.parameterTypes[0] == paramType)
            }
            if (method != null) {
                methodCache[key] = method
            }
            method
        }
    }

    fun readAll(context: android.content.Context, baseCards: List<SimCardInfo>): List<SimStatusInfo> {
        val startedAt = System.currentTimeMillis()
        val result = if (baseCards.size <= 1) {
            baseCards.map { readOne(context, it) }
        } else {
            // 双卡并行读取，互不阻塞
            val executor = java.util.concurrent.Executors.newFixedThreadPool(minOf(baseCards.size, 4))
            try {
                val futures = baseCards.map { card ->
                    executor.submit<SimStatusInfo> { readOne(context, card) }
                }
                futures.map { it.get() }
            } finally {
                executor.shutdown()
            }
        }
        LogStore.i("读取 SIM 状态完成 用时 ${System.currentTimeMillis() - startedAt}ms 卡数=${result.size}")
        return result
    }

    private fun readOne(context: android.content.Context, card: SimCardInfo): SimStatusInfo {
        val info = getSubscriptionInfo(card.subId)

        val mcc = info?.let { reflectInt(it, "getMcc") }?.takeIf { it > 0 }?.toString() ?: "—"
        val mnc = info?.let { reflectInt(it, "getMnc") }?.takeIf { it > 0 }?.toString() ?: "—"
        val countryIso = info?.let { reflectString(it, "getCountryIso") }
            ?.uppercase()?.ifEmpty { "—" } ?: "—"
        val roaming = info?.let { reflectInt(it, "getDataRoaming") == 1 } == true
        val iccid = info?.let { reflectString(it, "getIccId").orEmpty() }.orEmpty()

        val networkType = readNetworkType(card.subId)
        val (signalLevel, signalDbm) = readSignal(card.subId)

        return SimStatusInfo(
            slot = card.slot,
            subId = card.subId,
            carrierName = card.carrierName.ifEmpty { "未知运营商" },
            mcc = mcc,
            mnc = mnc,
            countryIso = countryIso,
            networkType = networkType,
            signalLevel = signalLevel,
            signalDbm = signalDbm,
            dataRoaming = roaming,
            iccidMasked = maskIccId(iccid),
            simState = "就绪",
            hasOverride = card.currentConfig.isNotEmpty()
        )
    }

    /** 通过 ISub + shell 身份获取 SubscriptionInfo（权限检查在 binder 端完成） */
    private fun getSubscriptionInfo(subId: Int): Any? {
        cachedISub?.let { return readFromISub(it, subId) }
        return runCatching {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "isub") as IBinder
            val stubClass = Class.forName("com.android.internal.telephony.ISub\$Stub")
            val iSub = stubClass.getMethod("asInterface", IBinder::class.java)
                .invoke(null, ShizukuBinderWrapper(binder))
            cachedISub = iSub
            readFromISub(iSub, subId)
        }.getOrNull()
    }

    private fun readFromISub(iSub: Any, subId: Int): Any? {
        return runCatching {
            val method3 = findMethod(iSub, "getActiveSubscriptionInfo", 3)
                ?: findMethod(iSub, "getActiveSubscriptionInfo", 2)
                ?: findMethod(iSub, "getActiveSubscriptionInfo", 1)
                ?: return null
            when (method3.parameterTypes.size) {
                3 -> method3.invoke(iSub, subId, "com.android.shell", null)
                2 -> method3.invoke(iSub, subId, "com.android.shell")
                else -> method3.invoke(iSub, subId)
            }
        }.getOrNull()
    }

    private fun getITelephony(): Any? {
        cachedITelephony?.let { return it }
        return runCatching {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "phone") as IBinder
            val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            val iTelephony = stubClass.getMethod("asInterface", IBinder::class.java)
                .invoke(null, ShizukuBinderWrapper(binder))
            cachedITelephony = iTelephony
            iTelephony
        }.getOrNull()
    }

    private fun readNetworkType(subId: Int): String {
        return runCatching {
            val iTelephony = getITelephony() ?: return "—"
            // 优先按 subId 查询当前卡，回退到无参（默认卡）
            val method = findMethod(iTelephony, "getNetworkType", 1, Int::class.javaPrimitiveType)
                ?: findMethod(iTelephony, "getNetworkType", 0)
                ?: return "—"
            val invoked: Any? = if (method.parameterTypes.isEmpty()) {
                method.invoke(iTelephony)
            } else {
                method.invoke(iTelephony, subId)
            }
            val raw = invoked as? Int ?: return "—"
            mapNetworkType(raw)
        }.getOrDefault("—")
    }

    private fun readSignal(subId: Int): Pair<Int, Int> {
        return runCatching {
            val iTelephony = getITelephony() ?: return Pair(0, 0)
            // 优先 getSignalStrength(int subId) 按卡查询，回退无参（默认卡）
            val method = findMethod(iTelephony, "getSignalStrength", 1, Int::class.javaPrimitiveType)
                ?: findMethod(iTelephony, "getSignalStrength", 0)
                ?: return Pair(0, 0)
            val strength = if (method.parameterTypes.isEmpty()) {
                method.invoke(iTelephony)
            } else {
                method.invoke(iTelephony, subId)
            } ?: return Pair(0, 0)

            val level = runCatching {
                findMethod(strength, "getLevel", 0)?.invoke(strength) as? Int
            }
                .getOrDefault(0)
                ?.coerceIn(0, 4) ?: 0
            val dbm = if (android.os.Build.VERSION.SDK_INT >= 29) {
                runCatching {
                    val list = findMethod(strength, "getCellSignalStrengths", 0)?.invoke(strength)
                        as? List<*> ?: emptyList<Any?>()
                    list.firstOrNull()?.let { cell ->
                        findMethod(cell, "getDbm", 0)?.invoke(cell) as? Int
                    } ?: 0
                }.getOrDefault(0)
            } else {
                0
            }
            // 兜底：level 反射失败为 0，但 dbm 显示有信号时，至少归为“弱”
            val fixedLevel = if (level == 0 && dbm != 0 && dbm > -120) 1 else level
            Pair(fixedLevel, dbm)
        }.getOrDefault(Pair(0, 0))
    }

    private fun reflectInt(target: Any, methodName: String): Int? {
        return runCatching {
            findMethod(target, methodName, 0)?.invoke(target) as? Int
        }.getOrNull()
    }

    private fun reflectString(target: Any, methodName: String): String? {
        return runCatching {
            findMethod(target, methodName, 0)?.invoke(target)?.toString()
        }.getOrNull()
    }

    private fun maskIccId(iccid: String): String {
        if (iccid.isBlank()) return "未授权读取"
        if (iccid.length <= 8) return "****$iccid"
        return iccid.take(6) + "*".repeat(iccid.length - 10) + iccid.takeLast(4)
    }

    private fun mapNetworkType(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3.5G (HSPA+)"
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA -> "3G (HSPA)"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G (UMTS)"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G (TD-SCDMA)"
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G (EVDO)"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "2.5G (1xRTT)"
            TelephonyManager.NETWORK_TYPE_EDGE -> "2.75G (EDGE)"
            TelephonyManager.NETWORK_TYPE_GPRS -> "2.5G (GPRS)"
            TelephonyManager.NETWORK_TYPE_GSM -> "2G (GSM)"
            TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "Wi-Fi 通话"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
            else -> "—"
        }
    }

    /** 信号等级 0-4 → 描述 */
    fun signalText(level: Int): String {
        return when (level) {
            0 -> "无信号"
            1 -> "弱"
            2 -> "一般"
            3 -> "良好"
            else -> "强"
        }
    }
}
