package com.gitee.planners.core.skill.script

import com.gitee.planners.api.event.script.ScriptCustomTriggerEvent
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.entity.state.ScriptCallbackImpl
import com.gitee.planners.core.skill.entity.state.ScriptCustomCallbackImpl

object ScriptCustomTrigger : ScriptBukkitEventHolder<ScriptCustomTriggerEvent>() {

    override val name: String = "customtrigger"

    override val bind: Class<ScriptCustomTriggerEvent> = ScriptCustomTriggerEvent::class.java

    override fun getSender(event: ScriptCustomTriggerEvent): Target<*>? {
        return event.sender
    }

    override fun getAssignCallback(state: State, trigger: State.Trigger): ScriptCallbackImpl<ScriptCustomTriggerEvent> {
        return ScriptCustomCallbackImpl(state, trigger)
    }

}