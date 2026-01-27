package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.event.script.ScriptCustomTriggerEvent
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.script.ScriptCustomTrigger
import com.gitee.planners.core.skill.script.ScriptEventHolder
import java.util.concurrent.CompletableFuture

open class ScriptCustomCallbackImpl(state: State, trigger: State.Trigger) : ScriptCallbackImpl<ScriptCustomTriggerEvent>(state, trigger) {

    val name = trigger.listen.replace(ScriptCustomTrigger.name,"").trim()

    override fun call(sender: ProxyTarget<*>, event: ScriptCustomTriggerEvent, holder: ScriptEventHolder<ScriptCustomTriggerEvent>): CompletableFuture<Any?> {
        if (event.name != this.name) {
            return CompletableFuture.completedFuture(null)
        }

        return super.call(sender, event, holder)
    }

}
