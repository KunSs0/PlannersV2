package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.job.target.CapableState
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.script.ScriptCallback
import com.gitee.planners.core.skill.script.ScriptEventHolder
import taboolib.common.platform.event.EventPriority
import taboolib.module.kether.ScriptContext

open class ScriptCallbackImpl<T>(val state: State, val trigger: State.Trigger) :
    ScriptCallback<T>(trigger.id, trigger.action, true, EventPriority.NORMAL, null, false) {



    override fun call(sender: Target<*>, event: T, holder: ScriptEventHolder<T>) {
        if (sender !is CapableState) {
            return
        }

        // 仅生物实体可用
        if (!sender.hasState(state)) {
            return
        }

        super.call(sender, event, holder)
    }

    override fun <T> whenBegin(sender: Target<*>, event: T, holder: ScriptEventHolder<T>, ctx: ScriptContext) {
        super.whenBegin(sender, event, holder, ctx)
        ctx["@State"] = state
        ctx["@Trigger"] = trigger
    }

}
    