package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.script.ScriptCustomTrigger

open class ScriptCustomCallbackImpl(state: State, trigger: State.Trigger) : ScriptCallbackImpl(state, trigger) {

    val name = trigger.listen.replace(ScriptCustomTrigger.name,"").trim()

}