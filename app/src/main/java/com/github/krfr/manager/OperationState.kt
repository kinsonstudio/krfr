package com.github.krfr.manager

import java.util.concurrent.atomic.AtomicBoolean

// 全局操作锁：同时只允许一个写配置任务；支持强制终止卡住的后台线程
object OperationState {
    private val applying = AtomicBoolean(false)

    @Volatile
    private var runningThread: Thread? = null

    @Volatile
    private var cancelled = false

    /** 申请开始一个写配置任务；已有任务进行中时返回 false */
    fun begin(): Boolean {
        if (!applying.compareAndSet(false, true)) {
            return false
        }
        cancelled = false
        return true
    }

    /** 在真正执行阻塞逻辑的线程（IO worker）里标记自己，供强制终止定位 */
    fun markRunning() {
        runningThread = Thread.currentThread()
    }

    fun end() {
        applying.set(false)
        runningThread = null
    }

    fun reset() {
        applying.set(false)
        runningThread = null
        cancelled = false
    }

    fun isRunning(): Boolean = applying.get()

    /** 强制终止当前后台任务：中断其线程（打断 sleep / shell 等待） */
    fun cancelAll(): Boolean {
        val target = runningThread ?: return false
        cancelled = true
        target.interrupt()
        return true
    }

    fun wasCancelled(): Boolean = cancelled
}
