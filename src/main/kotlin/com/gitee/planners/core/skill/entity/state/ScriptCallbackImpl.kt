package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.hasState
import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.script.ScriptCallback
import com.gitee.planners.core.skill.script.ScriptEventHolder
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import com.gitee.planners.module.fluxon.SingletonFluxonScript
import taboolib.common.platform.event.EventPriority
import java.util.concurrent.CompletableFuture

open class ScriptCallbackImpl<T>(val state: State, val trigger: State.Trigger) :
    ScriptCallback<T>(
        trigger.id,
        SingletonFluxonScript(trigger.action.source()),
        true,
        EventPriority.NORMAL,
        trigger.action.async
    ) {

    override fun call(sender: ProxyTarget<*>, event: T, holder: ScriptEventHolder<T>): CompletableFuture<Any?> {
        if (sender !is ProxyTarget.Entity<*>) {
            return CompletableFuture.completedFuture(null)
        }

        // 仅生物实体可用
        if (!sender.hasState(state)) {
            return CompletableFuture.completedFuture(null)
        }

        // 注入状态和触发器变量
        val options = FluxonScriptOptions.create {
            set("sender", sender.instance)
            set("id", this@ScriptCallbackImpl.id)
            set("@State", state)
            set("@Trigger", trigger)
            async(this@ScriptCallbackImpl.async)
        }

        // 注入事件变量
        holder.handle(event, options)

        return script.run(options)
    }

}
