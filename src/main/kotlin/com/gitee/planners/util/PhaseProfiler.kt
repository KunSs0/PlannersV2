package com.gitee.planners.util

import java.util.Locale

/**
 * 一次启动或重载各阶段的耗时统计。
 *
 * 只在生命周期入口创建，按固定顺序记录阶段耗时；格式化为单行日志，
 * 避免排查性能问题时再引入临时的 System.nanoTime 输出。
 */
internal class PhaseProfiler(private val title: String) {

    private val started = System.nanoTime()
    private val phases = ArrayList<Pair<String, Long>>()

    /**
     * 执行一个阶段并记录其耗时。
     */
    fun <T> measure(name: String, action: () -> T): T {
        val phaseStarted = System.nanoTime()
        return action().also {
            phases += name to (System.nanoTime() - phaseStarted)
        }
    }

    /**
     * 生成单行统计文本；[detail] 用于追加 Nova 编译程序等非阶段信息。
     */
    fun summary(detail: String? = null): String {
        val total = System.nanoTime() - started
        val text = StringBuilder(title)
        text.append(": total=").append(millis(total)).append("ms")
        for ((name, nanos) in phases) {
            text.append(", ").append(name).append('=').append(millis(nanos)).append("ms")
        }
        if (detail != null) {
            text.append("; ").append(detail)
        }
        return text.toString()
    }

    private fun millis(nanos: Long): String {
        return String.format(Locale.ROOT, "%.1f", nanos / 1_000_000.0)
    }
}
