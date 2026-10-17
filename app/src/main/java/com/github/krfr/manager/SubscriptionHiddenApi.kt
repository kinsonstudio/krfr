package com.github.krfr.manager

import android.content.Context
import android.os.IBinder
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.TelephonyManager
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * 覆盖 CarrierConfig 后，Oppo 副卡槽可能不会把 override 名称写入 SubscriptionInfo.carrierName（状态栏 SPN）。
 * Android 16 的 ISub 已移除 setCarrierName，需走 ISubExt / ContentProvider / setOperatorBrandOverride。
 */
internal object SubscriptionHiddenApi {
    private const val TAG = "KrfrSub"
    private const val ISUB_STUB = "com.android.internal.telephony.ISub\$Stub"
    private const val ISUB_EXT_STUB = "com.oplus.telephony.ISubExt\$Stub"
    private const val OPLUS_TELEPHONY_INTERNAL_STUB =
        "com.android.internal.telephony.IOplusTelephonyInternalExt\$Stub"
    private const val ITELEPHONY = "com.android.internal.telephony.ITelephony"
    private const val OPLUS_TELEPHONY_INTERNAL = "com.android.internal.telephony.IOplusTelephonyInternalExt"
    private const val ISUB_EXT = "com.oplus.telephony.ISubExt"
    private val RETRY_DELAYS_MS = longArrayOf(0L, 400L, 1200L, 2500L, 5000L)
    // 还原/清除角标走短重试：配置本身已被确认清空，无需等满 9s
    private val RESTORE_RETRY_DELAYS_MS = longArrayOf(0L, 400L, 1200L)
    private val syncShellIdentity = ThreadLocal.withInitial { false }

    /**
     * 设置 SIM 卡显示名（系统「SIM 卡信息」里可编辑的卡名，状态栏显示）。
     * 经 Shizuku shell 身份调用；多通道兜底（ISub 普通反射 / HiddenApiBypass 直取 / OPPO ISubExt）。
     * name 传空串表示清除回默认。调用失败抛异常（不再静默吞掉）；写后回读验证。
     */
    fun setSimDisplayName(subId: Int, name: String?): Boolean {
        val display = name ?: ""
        val errors = mutableListOf<String>()

        runCatching { invokeISubSetDisplayName(subId, display) }
            .onSuccess { verifySimDisplayName(subId, display); return true }
            .onFailure { errors.add("ISub:${it.message}") }

        runCatching { invokeISubSetDisplayNameBypass(subId, display) }
            .onSuccess { verifySimDisplayName(subId, display); return true }
            .onFailure { errors.add("ISubBypass:${it.message}") }

        runCatching { invokeOplusSetDisplayName(subId, display) }
            .onSuccess { verifySimDisplayName(subId, display); return true }
            .onFailure { errors.add("ISubExt:${it.message}") }

        val available = runCatching {
            val iSub = getISub()
            iSub?.javaClass?.methods?.map { it.name }?.distinct()?.sorted()?.joinToString(",")
                ?: "ISub 不可用"
        }.getOrDefault("?")
        throw IllegalStateException(
            "所有修改显示名通道均失败：${errors.joinToString(" | ")}；ISub 现有方法：$available"
        )
    }

    /** 通道 A：普通反射遍历 ISub（代理类 + 接口类），方法名 setDisplayName / setDisplayNameUsingSrc，动态签名。 */
    private fun invokeISubSetDisplayName(subId: Int, display: String) {
        val iSub = getISub() ?: throw IllegalStateException("ISub 不可用")
        val interfaceMethods = runCatching {
            Class.forName("com.android.internal.telephony.ISub").methods
        }.getOrDefault(emptyArray())
        val candidates = (iSub.javaClass.methods + interfaceMethods)
            .distinctBy { it.name + it.parameterTypes.joinToString(",") { c -> c.name } }
            .filter { it.name == "setDisplayName" || it.name == "setDisplayNameUsingSrc" }
        for (method in candidates) {
            val args = buildDisplayNameArgs(method.parameterTypes, subId, display) ?: continue
            try {
                invoke(method, iSub, *args)
                return
            } catch (e: Throwable) {
                // 继续尝试下一签名
            }
        }
        throw NoSuchMethodException("ISub 无可用 setDisplayName 签名")
    }

    /** 通道 B：HiddenApiBypass 直取接口声明方法（绕过 hidden API 过滤）+ invoke。 */
    private fun invokeISubSetDisplayNameBypass(subId: Int, display: String) {
        val iSub = getISub() ?: throw IllegalStateException("ISub 不可用")
        val iface = Class.forName("com.android.internal.telephony.ISub")
        val methods = HiddenApiBypass.getDeclaredMethods(iface)
            .filterIsInstance<Method>()
            .filter { it.name == "setDisplayName" || it.name == "setDisplayNameUsingSrc" }
        for (method in methods) {
            val args = buildDisplayNameArgs(method.parameterTypes, subId, display) ?: continue
            try {
                method.invoke(iSub, *args)
                return
            } catch (e: Throwable) {
                // 继续尝试下一签名
            }
        }
        throw NoSuchMethodException("ISub(HiddenApiBypass) 无可用 setDisplayName 签名")
    }

    /** 通道 C：OPPO ISubExt 的显示名方法（setDisplayName / setDisplayNameUsingSrc / *DisplayName*）。 */
    private fun invokeOplusSetDisplayName(subId: Int, display: String) {
        val iSubExt = getISubExt() ?: throw IllegalStateException("ISubExt 不可用")
        val interfaceMethods = runCatching {
            HiddenApiBypass.getDeclaredMethods(Class.forName(ISUB_EXT))
                .filterIsInstance<Method>()
        }.getOrDefault(emptyList())
        val candidates = (iSubExt.javaClass.methods.toList() + interfaceMethods)
            .distinctBy { it.name + it.parameterTypes.joinToString(",") { c -> c.name } }
            .filter { it.name.contains("isplay", ignoreCase = true) }
        for (method in candidates) {
            val args = buildDisplayNameArgs(method.parameterTypes, subId, display) ?: continue
            try {
                invoke(method, iSubExt, *args)
                return
            } catch (e: Throwable) {
                // 继续尝试下一签名
            }
        }
        throw NoSuchMethodException("ISubExt 无显示名方法")
    }

    /** 按真实参数类型动态构造 ISub.setDisplayName 参数（int,String 或 String,int 开头，其余按类型填）。 */
    private fun buildDisplayNameArgs(
        types: Array<Class<*>>,
        subId: Int,
        display: String
    ): Array<Any?>? {
        if (types.size < 2) return null
        val first = types[0]
        val second = types[1]
        val result = ArrayList<Any?>(types.size)
        for (i in 2 until types.size) {
            when (types[i]) {
                Long::class.javaPrimitiveType, java.lang.Long::class.java ->
                    result.add(2L) // NAME_SOURCE_USER_INPUT
                Int::class.javaPrimitiveType, Integer::class.java -> result.add(1)
                String::class.java -> result.add("com.android.shell")
                else -> return null
            }
        }
        return when {
            first == Int::class.javaPrimitiveType && second == String::class.java -> {
                val all = ArrayList<Any?>(types.size)
                all.add(subId); all.add(display); all.addAll(result)
                all.toTypedArray()
            }
            first == String::class.java && second == Int::class.javaPrimitiveType -> {
                val all = ArrayList<Any?>(types.size)
                all.add(display); all.add(subId); all.addAll(result)
                all.toTypedArray()
            }
            else -> null
        }
    }

    /** 写后回读验证：系统可能异步生效，最多等待 ~1.2s；未生效抛异常，避免“假成功”。 */
    private fun verifySimDisplayName(subId: Int, display: String) {
        Thread.sleep(400)
        val current = com.github.krfr.manager.CarrierConfigManager.getSimDisplayName(subId)?.trim()
        if (display.isBlank()) {
            // 清除场景：回读应为空/默认
            if (!current.isNullOrEmpty()) {
                Thread.sleep(800)
                val again = com.github.krfr.manager.CarrierConfigManager.getSimDisplayName(subId)?.trim()
                if (!again.isNullOrEmpty()) {
                    throw IllegalStateException(
                        "清除未生效：当前显示名仍为“${again}”"
                    )
                }
            }
        } else if (current != display) {
            Thread.sleep(800)
            val again = com.github.krfr.manager.CarrierConfigManager.getSimDisplayName(subId)?.trim()
            if (again != display) {
                throw IllegalStateException(
                    "修改未生效：当前显示名仍为“${again.orEmpty().ifBlank { "默认名称" }}”"
                )
            }
        }
    }

    fun syncAfterOverride(context: Context, subId: Int, bundle: PersistableBundle?) {
        val carrierName = bundle?.let { readOverrideCarrierName(it) }
        scheduleSync(context, subId, carrierName, blocking = false, useShellIdentity = false)
    }

    fun syncAfterRestore(context: Context, subId: Int) {
        // 还原必须阻塞做完，否则重启后角标（品牌/SPN）常残留
        scheduleSync(context, subId, carrierName = null, blocking = true, useShellIdentity = false)
    }

    /** instrumentation 进程会在 finish 后立即退出，须在同一线程（保留 shell 身份）阻塞完成。 */
    fun syncAfterOverrideBlocking(context: Context, subId: Int, bundle: PersistableBundle?) {
        val carrierName = bundle?.let { readOverrideCarrierName(it) }
        scheduleSync(context, subId, carrierName, blocking = true, useShellIdentity = true)
    }

    fun syncAfterRestoreBlocking(context: Context, subId: Int) {
        scheduleSync(context, subId, carrierName = null, blocking = true, useShellIdentity = true)
    }

    /**
     * 强制清除状态栏运营商角标（含重试）。用于重启后 / 无 CarrierConfig 覆盖时。
     */
    fun clearBrandThoroughly(context: Context, subId: Int) {
        scheduleSync(context, subId, carrierName = null, blocking = true, useShellIdentity = false)
    }

    /**
     * 快速清一次残留角标（无长重试）。
     */
    fun clearResidualBrandWhenNoOverride(context: Context, subId: Int) {
        val appContext = context.applicationContext
        syncShellIdentity.set(false)
        try {
            runCatching { clearCarrierName(appContext, subId) }
            runCatching { notifyConfigChangedForSubId(subId) }
        } finally {
            syncShellIdentity.remove()
        }
    }

    private fun scheduleSync(
        context: Context,
        subId: Int,
        carrierName: String?,
        blocking: Boolean,
        useShellIdentity: Boolean,
        retryDelays: LongArray = if (carrierName == null) RESTORE_RETRY_DELAYS_MS else RETRY_DELAYS_MS
    ) {
        val appContext = context.applicationContext
        val worker = Runnable {
            syncShellIdentity.set(useShellIdentity)
            try {
                Log.i(TAG, "sync start subId=$subId expected=$carrierName shellIdentity=$useShellIdentity")
                retryDelays.forEachIndexed { attempt, delayMs ->
                    if (delayMs > 0) {
                        Thread.sleep(delayMs)
                    }
                    var syncSucceeded = false
                    runCatching {
                        applySync(appContext, subId, carrierName)
                        syncSucceeded = true
                    }.onFailure {
                        Log.w(TAG, "sync attempt ${attempt + 1} failed subId=$subId", it)
                    }

                    if (
                        (carrierName == null && syncSucceeded) ||
                            (carrierName != null &&
                                (verifyCarrierName(subId, carrierName) || verifyOperatorBrand(subId, carrierName)))
                    ) {
                        if (carrierName != null) {
                            Log.i(
                                TAG,
                                "carrierName synced subId=$subId name=$carrierName attempt=${attempt + 1} " +
                                    "carrierName=${readCarrierName(subId)} brand=${readOperatorBrand(subId)}"
                            )
                        }
                        return@Runnable
                    }
                }
                Log.w(
                    TAG,
                    "carrierName still mismatched after retries subId=$subId expected=$carrierName " +
                        "actual=${readCarrierName(subId)} brand=${readOperatorBrand(subId)}"
                )
            } finally {
                syncShellIdentity.remove()
            }
        }
        if (blocking) {
            worker.run()
        } else {
            Thread(worker, "KrfrSubSync-$subId").apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun applySync(
        context: Context,
        subId: Int,
        carrierName: String?
    ) {
        runCatching { notifyConfigChangedForSubId(subId) }
            .onFailure { Log.w(TAG, "notifyConfigChangedForSubId($subId) failed", it) }

        if (carrierName == null) {
            clearCarrierName(context, subId)
            return
        }

        // ITelephony + ShizukuBinderWrapper 已是 shell 权限，无需再套 delegate
        setCarrierName(context, subId, carrierName)
    }

    private fun setCarrierName(context: Context, subId: Int, carrierName: String) {
        val errors = mutableListOf<Throwable>()

        if (syncShellIdentity.get()) {
            runCatching { setOperatorBrandOverrideViaTelephonyManager(context, subId, carrierName) }
                .onSuccess {
                    Log.i(TAG, "setCarrierName via TelephonyManager subId=$subId name=$carrierName")
                    return
                }
                .onFailure { errors.add(it) }
        }

        runCatching { setOperatorBrandOverrideViaITelephony(subId, carrierName) }
            .onSuccess {
                Log.i(TAG, "setCarrierName via ITelephony subId=$subId name=$carrierName")
                return
            }
            .onFailure { errors.add(it) }

        runCatching { setCarrierNameViaOplusInternalExt(subId, carrierName) }
            .onSuccess {
                Log.i(TAG, "setCarrierName via IOplusTelephonyInternalExt subId=$subId name=$carrierName")
                return
            }
            .onFailure { errors.add(it) }

        runCatching { triggerOppoSpnRefreshViaInternalExt(subId) }
            .onSuccess {
                if (verifyCarrierName(subId, carrierName) || verifyOperatorBrand(subId, carrierName)) {
                    Log.i(TAG, "carrierName refreshed via IOplusTelephonyInternalExt.updateSpn subId=$subId")
                    return
                }
                throw IllegalStateException("IOplusTelephonyInternalExt.updateSpn did not apply name")
            }
            .onFailure { errors.add(it) }

        runCatching { triggerOplusCarrierNameOverrideViaInternalExt(subId, carrierName) }
            .onSuccess {
                if (verifyCarrierName(subId, carrierName) || verifyOperatorBrand(subId, carrierName)) {
                    Log.i(TAG, "carrierName refreshed via IOplusCarrierNameOverride subId=$subId")
                    return
                }
                throw IllegalStateException("IOplusCarrierNameOverride refresh did not apply name")
            }
            .onFailure { errors.add(it) }

        runCatching { triggerOppoSpnRefresh(context, subId) }
            .onSuccess {
                if (verifyCarrierName(subId, carrierName) || verifyOperatorBrand(subId, carrierName)) {
                    Log.i(TAG, "carrierName refreshed via OplusFeatureCache subId=$subId name=$carrierName")
                    return
                }
                throw IllegalStateException("OplusFeatureCache refresh did not apply name")
            }
            .onFailure { errors.add(it) }

        runCatching { setCarrierNameViaOplusExt(subId, carrierName) }
            .onSuccess {
                Log.i(TAG, "setCarrierName via ISubExt subId=$subId name=$carrierName")
                return
            }
            .onFailure { errors.add(it) }

        Log.w(
            TAG,
            "all carrier name paths failed subId=$subId: " +
                errors.joinToString { "${it.javaClass.simpleName}:${it.message}" }
        )
        throw errors.lastOrNull() ?: IllegalStateException("all carrier name sync paths failed")
    }

    private fun clearCarrierName(context: Context, subId: Int) {
        val errors = mutableListOf<Throwable>()

        if (syncShellIdentity.get()) {
            runCatching { setOperatorBrandOverrideViaTelephonyManager(context, subId, null) }
                .onSuccess {
                    Log.i(TAG, "cleared carrier name via TelephonyManager subId=$subId")
                    return
                }
                .onFailure { errors.add(it) }
        }

        runCatching { setOperatorBrandOverrideViaITelephony(subId, null) }
            .onSuccess {
                Log.i(TAG, "cleared carrier name via ITelephony subId=$subId")
                return
            }
            .onFailure { errors.add(it) }

        runCatching { setCarrierNameViaOplusInternalExt(subId, null) }
            .onSuccess {
                Log.i(TAG, "cleared carrier name via IOplusTelephonyInternalExt subId=$subId")
                return
            }
            .onFailure { errors.add(it) }

        runCatching { setCarrierNameViaOplusExt(subId, null) }
            .onSuccess {
                Log.i(TAG, "cleared carrier name via ISubExt subId=$subId")
                return
            }
            .onFailure { errors.add(it) }

        runCatching { triggerOppoSpnRefreshViaInternalExt(subId) }
            .onSuccess {
                Log.i(TAG, "refreshed carrier name via IOplusTelephonyInternalExt subId=$subId")
                return
            }
            .onFailure { errors.add(it) }

        runCatching { triggerOppoSpnRefresh(context, subId) }
            .onSuccess {
                Log.i(TAG, "refreshed carrier name after clear subId=$subId")
                return
            }
            .onFailure { errors.add(it) }

        throw errors.lastOrNull() ?: IllegalStateException("all carrier name clear paths failed")
    }

    private fun setOperatorBrandOverrideViaTelephonyManager(
        context: Context,
        subId: Int,
        carrierName: String?
    ) {
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
            ?.createForSubscriptionId(subId)
            ?: throw IllegalStateException("TelephonyManager unavailable")
        val method = TelephonyManager::class.java.getMethod(
            "setOperatorBrandOverride",
            String::class.java
        )
        val result = invokeBoolean(method, telephonyManager, carrierName)
        if (!result) {
            throw IllegalStateException("TelephonyManager.setOperatorBrandOverride returned false")
        }
    }

    private fun setCarrierNameViaOplusInternalExt(subId: Int, carrierName: String?) {
        val internalExt = getIOplusTelephonyInternalExt()
            ?: throw IllegalStateException("IOplusTelephonyInternalExt unavailable")
        val phoneId = getPhoneIdForSubId(subId)
        invokeFirstMatching(
            target = internalExt,
            interfaceClassNames = arrayOf(OPLUS_TELEPHONY_INTERNAL),
            methodName = "setCarrierName"
        ) { method ->
            buildCarrierNameArgs(method.parameterTypes, subId, carrierName)
                ?: buildCarrierNameArgs(method.parameterTypes, phoneId, carrierName)
        } ?: throw NoSuchMethodException("IOplusTelephonyInternalExt.setCarrierName unsupported signature")
    }

    private fun triggerOppoSpnRefreshViaInternalExt(subId: Int) {
        val internalExt = getIOplusTelephonyInternalExt()
            ?: throw IllegalStateException("IOplusTelephonyInternalExt unavailable")
        val phoneId = getPhoneIdForSubId(subId)
        val invoked = invokeFirstMatching(
            target = internalExt,
            interfaceClassNames = arrayOf(OPLUS_TELEPHONY_INTERNAL),
            methodName = "updateSpn"
        ) { method ->
            buildOplusSpnArgs(method.parameterTypes, phoneId, subId, config = null)
        }
        if (invoked == null) {
            throw NoSuchMethodException("IOplusTelephonyInternalExt.updateSpn not found")
        }
    }

    private fun triggerOplusCarrierNameOverrideViaInternalExt(subId: Int, carrierName: String) {
        val internalExt = getIOplusTelephonyInternalExt()
            ?: throw IllegalStateException("IOplusTelephonyInternalExt unavailable")
        val phoneId = getPhoneIdForSubId(subId)
        val override = invokeInterfaceMethod(
            internalExt,
            OPLUS_TELEPHONY_INTERNAL,
            "getOplusCarrierNameOverride"
        ) ?: throw IllegalStateException("getOplusCarrierNameOverride returned null")

        val config = TelephonyHiddenApi.getConfigForSubId("com.android.shell", subId)
        var invoked = false
        listOf("updateSpn", "setSpnFromConfig", "refreshSpn", "updateCarrierName").forEach { methodName ->
            invokeFirstMatching(
                target = override,
                interfaceClassNames = arrayOf(
                    "com.android.internal.telephony.IOplusCarrierNameOverride"
                ),
                methodName = methodName
            ) { method ->
                buildOplusSpnArgs(method.parameterTypes, phoneId, subId, config)
                    ?: buildCarrierNameArgs(method.parameterTypes, phoneId, carrierName)
                    ?: buildCarrierNameArgs(method.parameterTypes, subId, carrierName)
            }?.let { invoked = true }
        }
        if (!invoked) {
            throw NoSuchMethodException("IOplusCarrierNameOverride spn refresh methods not found")
        }
    }

    private fun invokeFirstMatching(
        target: Any,
        interfaceClassNames: Array<String>,
        methodName: String,
        argsBuilder: (Method) -> Array<Any?>?
    ): Method? {
        for (method in findInterfaceMethods(*interfaceClassNames, methodName = methodName)) {
            val args = argsBuilder(method) ?: continue
            runCatching {
                invoke(method, target, *args)
                return method
            }
        }
        return null
    }

    private fun invokeInterfaceMethod(
        target: Any,
        interfaceClassName: String,
        methodName: String,
        vararg args: Any?
    ): Any? {
        val method = findInterfaceMethods(interfaceClassName, methodName = methodName)
            .firstOrNull { it.parameterTypes.size == args.size }
            ?: throw NoSuchMethodException("$interfaceClassName.$methodName not found")
        return method.invoke(target, *args)
    }

    private fun findInterfaceMethods(vararg interfaceClassNames: String, methodName: String): List<Method> {
        return interfaceClassNames.flatMap { className ->
            runCatching {
                Class.forName(className).methods.filter { it.name == methodName }
            }.getOrDefault(emptyList())
        }
    }

    private fun wrapBinder(binder: IBinder): IBinder {
        return if (syncShellIdentity.get()) binder else ShizukuBinderWrapper(binder)
    }

    private fun getIOplusTelephonyInternalExt(): Any? {
        return runCatching {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "oplus_telephony_internal_ext") as IBinder
            val stubClass = Class.forName(OPLUS_TELEPHONY_INTERNAL_STUB)
            stubClass.getMethod("asInterface", IBinder::class.java)
                .invoke(null, wrapBinder(binder))
        }.getOrNull()
    }

    private fun setOperatorBrandOverrideViaITelephony(subId: Int, carrierName: String?) {
        val iTelephony = getITelephony() ?: throw IllegalStateException("ITelephony unavailable")
        val method = findInterfaceMethods(ITELEPHONY, methodName = "setOperatorBrandOverride")
            .firstOrNull { it.parameterTypes.size == 2 }
            ?: throw NoSuchMethodException("ITelephony.setOperatorBrandOverride(int,String) not found")
        val result = invokeBoolean(method, iTelephony, subId, carrierName)
        if (!result) {
            throw IllegalStateException("ITelephony.setOperatorBrandOverride returned false")
        }
    }

    private fun getITelephony(): Any? {
        return runCatching {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "phone") as IBinder
            val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            stubClass.getMethod("asInterface", IBinder::class.java)
                .invoke(null, wrapBinder(binder))
        }.getOrNull()
    }

    private fun triggerOppoSpnRefresh(context: Context, subId: Int) {
        val phoneId = getPhoneIdForSubId(subId)
        val feature = getOplusCarrierNameOverride()
            ?: throw IllegalStateException("OplusCarrierNameOverride unavailable")
        val config = TelephonyHiddenApi.getConfigForSubId(context.packageName, subId)

        var invoked = false
        feature.javaClass.methods.filter {
            it.name == "updateSpn" || it.name == "setSpnFromConfig"
        }.forEach { method ->
            buildOplusSpnArgs(method.parameterTypes, phoneId, subId, config)?.let { args ->
                runCatching {
                    invoke(method, feature, *args)
                    invoked = true
                }
            }
        }
        if (!invoked) {
            throw NoSuchMethodException("OplusCarrierNameOverride.updateSpn/setSpnFromConfig not found")
        }
    }

    private fun buildOplusSpnArgs(
        parameterTypes: Array<Class<*>>,
        phoneId: Int,
        subId: Int,
        config: PersistableBundle?
    ): Array<Any?>? {
        return when (parameterTypes.size) {
            1 -> when (parameterTypes[0]) {
                Int::class.javaPrimitiveType -> arrayOf(phoneId)
                else -> null
            }
            2 -> when {
                parameterTypes[0] == Int::class.javaPrimitiveType &&
                    parameterTypes[1] == Int::class.javaPrimitiveType ->
                    arrayOf(phoneId, subId)
                parameterTypes[0] == Int::class.javaPrimitiveType &&
                    parameterTypes[1] == PersistableBundle::class.java &&
                    config != null ->
                    arrayOf(phoneId, config)
                parameterTypes[0] == Int::class.javaPrimitiveType &&
                    parameterTypes[1] == String::class.java &&
                    config != null ->
                    arrayOf(phoneId, config.getString(CarrierConfigManager.KEY_CARRIER_NAME_STRING))
                else -> null
            }
            else -> null
        }
    }

    private fun getPhoneIdForSubId(subId: Int): Int {
        val iSub = getISub() ?: throw IllegalStateException("ISub unavailable")
        val method = iSub.javaClass.getMethod("getPhoneId", Int::class.javaPrimitiveType)
        return method.invoke(iSub, subId) as Int
    }

    private fun getOplusCarrierNameOverride(): Any? {
        return runCatching {
            Class.forName("com.oplus.internal.telephony.OplusFeatureCache")
                .getMethod("getOplusCarrierNameOverride")
                .invoke(null)
        }.getOrNull()
    }

    private fun setCarrierNameViaOplusExt(subId: Int, carrierName: String?) {
        val iSubExt = getISubExt() ?: throw IllegalStateException("ISubExt unavailable")
        invokeFirstMatching(
            target = iSubExt,
            interfaceClassNames = arrayOf(ISUB_EXT),
            methodName = "setCarrierName"
        ) { method ->
            buildCarrierNameArgs(method.parameterTypes, subId, carrierName)
        } ?: throw NoSuchMethodException("ISubExt.setCarrierName unsupported signature")
    }

    private fun buildCarrierNameArgs(
        parameterTypes: Array<Class<*>>,
        subId: Int,
        carrierName: String?
    ): Array<Any?>? {
        return when (parameterTypes.size) {
            2 -> when {
                parameterTypes[0] == Int::class.javaPrimitiveType &&
                    parameterTypes[1] == String::class.java -> arrayOf(subId, carrierName)
                parameterTypes[0] == String::class.java &&
                    parameterTypes[1] == Int::class.javaPrimitiveType -> arrayOf(carrierName, subId)
                else -> null
            }
            3 -> when {
                parameterTypes[0] == Int::class.javaPrimitiveType &&
                    parameterTypes[1] == String::class.java &&
                    parameterTypes[2] == String::class.java -> arrayOf(subId, carrierName, "com.android.shell")
                else -> null
            }
            else -> null
        }
    }

    private fun readOperatorBrand(subId: Int): String? {
        return runCatching {
            val iTelephony = getITelephony() ?: return null
            val method = findInterfaceMethods(ITELEPHONY, methodName = "getOperatorBrandOverride")
                .firstOrNull { it.parameterTypes.size == 1 }
                ?: return null
            method.invoke(iTelephony, subId)?.toString()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun verifyOperatorBrand(subId: Int, expected: String): Boolean {
        return readOperatorBrand(subId)?.equals(expected, ignoreCase = false) == true
    }

    private fun verifyCarrierName(subId: Int, expected: String): Boolean {
        return readCarrierName(subId)?.equals(expected, ignoreCase = false) == true
    }

    private fun readCarrierName(subId: Int): String? {
        val info = getActiveSubscriptionInfo(subId) ?: return null
        return runCatching {
            info.javaClass.getMethod("getCarrierName").invoke(info)?.toString()
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun getActiveSubscriptionInfo(subId: Int): Any? {
        val iSub = getISub() ?: return null
        val method = iSub.javaClass.methods.firstOrNull { candidate ->
            candidate.name == "getActiveSubscriptionInfo" &&
                candidate.parameterTypes.size == 3 &&
                candidate.parameterTypes[0] == Int::class.javaPrimitiveType
        } ?: iSub.javaClass.methods.firstOrNull { candidate ->
            candidate.name == "getActiveSubscriptionInfo" &&
                candidate.parameterTypes.size == 2 &&
                candidate.parameterTypes[0] == Int::class.javaPrimitiveType
        } ?: iSub.javaClass.methods.firstOrNull { candidate ->
            candidate.name == "getActiveSubscriptionInfo" &&
                candidate.parameterTypes.size == 1 &&
                candidate.parameterTypes[0] == Int::class.javaPrimitiveType
        } ?: return null

        return runCatching {
            when (method.parameterTypes.size) {
                3 -> method.invoke(iSub, subId, "com.android.shell", null)
                2 -> method.invoke(iSub, subId, "com.android.shell")
                else -> method.invoke(iSub, subId)
            }
        }.getOrNull()
    }

    private fun readOverrideCarrierName(bundle: PersistableBundle): String? {
        if (!bundle.getBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, false)) {
            return null
        }
        return bundle.getString(CarrierConfigManager.KEY_CARRIER_NAME_STRING)?.takeIf { it.isNotBlank() }
    }

    fun notifyConfigChangedForSubId(subId: Int) {
        val loader = TelephonyHiddenApi.getCarrierConfigLoaderOrNull()
            ?: throw IllegalStateException("CarrierConfigLoader unavailable")
        val method = loader.javaClass.getMethod(
            "notifyConfigChangedForSubId",
            Int::class.javaPrimitiveType
        )
        invoke(method, loader, subId)
    }

    private fun getISub(): Any? {
        return runCatching {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "isub") as IBinder
            val stubClass = Class.forName(ISUB_STUB)
            stubClass.getMethod("asInterface", IBinder::class.java)
                .invoke(null, wrapBinder(binder))
        }.getOrNull()
    }

    private fun getISubExt(): Any? {
        return runCatching {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "isub_ext") as IBinder
            val stubClass = Class.forName(ISUB_EXT_STUB)
            stubClass.getMethod("asInterface", IBinder::class.java)
                .invoke(null, wrapBinder(binder))
        }.getOrNull()
    }

    private fun invoke(method: java.lang.reflect.Method, target: Any, vararg args: Any?) {
        try {
            method.invoke(target, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }
    }

    private fun invokeBoolean(method: java.lang.reflect.Method, target: Any, vararg args: Any?): Boolean {
        try {
            return method.invoke(target, *args) as? Boolean ?: false
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }
    }
}
