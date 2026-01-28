package com.gitee.planners.core.skill.script

import com.gitee.planners.api.event.script.ScriptCustomTriggerEvent
import com.gitee.planners.api.job.target.ProxyTarget

object ScriptCustomTrigger : ScriptBukkitEventHolder<ScriptCustomTriggerEvent>() {

    override val name: String = "customtrigger"

    override val bind: Class<ScriptCustomTriggerEvent> = ScriptCustomTriggerEvent::class.java

    override fun getSender(event: ScriptCustomTriggerEvent): ProxyTarget<*>? {
        return event.sender
    }
}
