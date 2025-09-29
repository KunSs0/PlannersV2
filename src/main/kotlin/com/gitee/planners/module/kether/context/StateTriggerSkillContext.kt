package com.gitee.planners.module.kether.context

import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.script.ScriptEventHolder
import java.util.concurrent.CompletableFuture

class StateTriggerSkillContext(sender: Target<*>, val event: Any, val wrapped: ScriptEventHolder<Any>, val state: State, val trigger: State.Trigger) : AbstractContext(sender) {


    override fun call(): CompletableFuture<Any> {
        val platform = trigger.action.platform()
        val script = trigger.action.compiledScript()

        return platform.run("${state.id}.${trigger.id}",script, KetherScriptOptions.common(sender) {
            it.context {
                this@StateTriggerSkillContext.wrapped.handle(event,this)
            }
        })
    }


}