package com.gitee.planners.module.fluxon.delay

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.platform.function.submit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Delay 延迟等待扩展
 *
 * 注意：由于 Fluxon 的扩展函数机制，这些函数需要通过某个对象调用
 * 建议使用方式：
 * ```fluxon
 * "wait" :: delay("0.4s")
 * "wait" :: delayTicks(8L)
 * ```
 */
object DelayExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // String 延迟扩展（用于延迟操作的辅助类）
        runtime.registerExtension(String::class.java)
            .function("delay", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val target = ctx.target ?: return@function
                if (target != "wait") return@function

                val duration = ctx.getRef(0)?.toString() ?: return@function
                val ticks = parseDuration(duration)

                // 使用 Taboolib 的 submit 函数延迟执行
                val future = CompletableFuture<Void>()
                submit(delay = ticks) {
                    future.complete(null)
                }
                // 等待延迟完成
                future.join()
            }
            .function("delayTicks", FunctionSignature.returns(Type.VOID).params(Type.J)) { ctx ->
                val target = ctx.target ?: return@function
                if (target != "wait") return@function

                val ticks = ctx.getAsLong(0)

                // 使用 Taboolib 的 submit 函数延迟执行
                val future = CompletableFuture<Void>()
                submit(delay = ticks) {
                    future.complete(null)
                }
                // 等待延迟完成
                future.join()
            }
            .function("delayMillis", FunctionSignature.returns(Type.VOID).params(Type.J)) { ctx ->
                val target = ctx.target ?: return@function
                if (target != "wait") return@function

                val millis = ctx.getAsLong(0)
                val ticks = millis / 50L  // 1 tick = 50ms

                // 使用 Taboolib 的 submit 函数延迟执行
                val future = CompletableFuture<Void>()
                submit(delay = ticks) {
                    future.complete(null)
                }
                // 等待延迟完成
                future.join()
            }
    }

    /**
     * 解析时间字符串
     * 支持格式：
     * - "0.4s" -> 8 ticks (0.4 * 20)
     * - "10t" -> 10 ticks
     * - "200ms" -> 4 ticks (200 / 50)
     * - "1s" -> 20 ticks
     */
    private fun parseDuration(duration: String): Long {
        val trimmed = duration.trim().lowercase()

        return when {
            trimmed.endsWith("ms") -> {
                val millis = trimmed.removeSuffix("ms").toDoubleOrNull() ?: 0.0
                (millis / 50.0).toLong()
            }
            trimmed.endsWith("s") -> {
                val seconds = trimmed.removeSuffix("s").toDoubleOrNull() ?: 0.0
                (seconds * 20.0).toLong()
            }
            trimmed.endsWith("t") || trimmed.endsWith("tick") || trimmed.endsWith("ticks") -> {
                trimmed.replace(Regex("[a-z]+"), "").toLongOrNull() ?: 0L
            }
            else -> {
                // 默认当作 ticks
                trimmed.toLongOrNull() ?: 0L
            }
        }
    }
}
