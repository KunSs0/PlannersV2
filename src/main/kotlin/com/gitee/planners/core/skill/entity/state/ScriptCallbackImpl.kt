package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.hasState
import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.script.ScriptCallback
import com.gitee.planners.core.skill.script.ScriptEventHolder
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import com.gitee.planners.module.fluxon.SingletonFluxonScript
import org.tabooproject.fluxon.parser.ParsedScript
import org.tabooproject.fluxon.runtime.Environment
import taboolib.common.platform.event.EventPriority
import java.util.concurrent.CompletableFuture

/**
 * 基于状态函数的脚本回调实现
 *
 * @param state 关联的状态
 * @param funcName 要调用的函数名
 * @param stateScript 状态脚本（包含函数定义）
 */
open class ScriptCallbackImpl<T>(
    val state: State,
    val funcName: String,
    private val stateScript: ParsedScript
) : ScriptCallback<T>(
    id = "${state.id}:$funcName",
    script = SingletonFluxonScript(), // 空脚本占位
    ignoreCancelled = true,
    priority = EventPriority.NORMAL,
    async = false
) {

    override fun call(sender: ProxyTarget<*>, event: T, holder: ScriptEventHolder<T>): CompletableFuture<Any?> {
        if (sender !is ProxyTarget.Entity<*>) {
            return CompletableFuture.completedFuture(null)
        }

        // 仅携带该状态的实体可用
        if (!sender.hasState(state)) {
            return CompletableFuture.completedFuture(null)
        }

        // 创建新环境，先执行脚本定义函数，再调用目标函数
        val env = stateScript.newEnvironment()

        // 设置运行时变量
        env.defineRootVariable("sender", sender.instance)
        env.defineRootVariable("event", event)
        env.defineRootVariable("@State", state)

        // 注入事件变量
        val options = FluxonScriptOptions.create {
            set("sender", sender.instance)
            set("event", event)
            set("@State", state)
        }
        holder.handle(event, options)
        options.applyTo(env)

        // 执行脚本定义函数
        stateScript.eval(env)

        // 调用目标函数
        return CompletableFuture.completedFuture(env.callFunction(funcName))
    }

    /**
     * 调用环境中已定义的函数
     */
    private fun Environment.callFunction(name: String): Any? {
        // 通过执行函数调用表达式来调用函数
        val callScript = FluxonScriptCache.getOrParse("$name()")
        return callScript.eval(this)
    }
}
