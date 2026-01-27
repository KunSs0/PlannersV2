package com.gitee.planners.module.fluxon

import org.bukkit.event.Event
import org.tabooproject.fluxon.parser.ParsedScript
import org.tabooproject.fluxon.runtime.Environment
import java.util.concurrent.CompletableFuture

/**
 * Fluxon 触发器
 * 用于事件驱动的脚本执行
 */
class FluxonTrigger(
    val id: String,
    val listen: String,
    val script: ParsedScript,
    val async: Boolean = false,
    private val source: String = ""
) {

    /**
     * 获取脚本源代码
     */
    fun source(): String = source

    /**
     * 执行触发器脚本
     * @param sender 发送者/执行者
     * @param event 触发事件
     * @param variables 额外变量
     */
    fun execute(
        sender: Any,
        event: Event,
        variables: Map<String, Any?> = emptyMap()
    ): CompletableFuture<Any?> {
        val env = FluxonScriptCache.runtime.newEnvironment().apply {
            defineRootVariable("sender", sender)
            defineRootVariable("event", event)
            variables.forEach { (k, v) -> defineRootVariable(k, v) }
        }

        return if (async) {
            CompletableFuture.supplyAsync { script.eval(env) }
        } else {
            CompletableFuture.completedFuture(script.eval(env))
        }
    }

    /**
     * 执行触发器脚本（简化版，无事件）
     * @param sender 发送者/执行者
     * @param variables 额外变量
     */
    fun execute(
        sender: Any,
        variables: Map<String, Any?> = emptyMap()
    ): CompletableFuture<Any?> {
        val env = FluxonScriptCache.runtime.newEnvironment().apply {
            defineRootVariable("sender", sender)
            variables.forEach { (k, v) -> defineRootVariable(k, v) }
        }

        return if (async) {
            CompletableFuture.supplyAsync { script.eval(env) }
        } else {
            CompletableFuture.completedFuture(script.eval(env))
        }
    }

    companion object {

        /**
         * 从源代码创建触发器
         */
        fun of(
            id: String,
            listen: String,
            source: String,
            async: Boolean = false
        ): FluxonTrigger {
            return FluxonTrigger(
                id = id,
                listen = listen,
                script = FluxonScriptCache.getOrParse(source),
                async = async,
                source = source
            )
        }
    }
}
