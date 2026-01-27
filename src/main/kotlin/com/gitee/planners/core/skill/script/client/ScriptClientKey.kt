package com.gitee.planners.core.skill.script.client

import com.gitee.planners.api.event.ProxyClientKeyEvents
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.skill.script.ScriptBukkitEventHolder
import org.bukkit.event.Event

abstract class ScriptClientKey<T : Event> : ScriptBukkitEventHolder<T>() {

    object Up : ScriptClientKey<ProxyClientKeyEvents.Up>() {

        override val name: String = "keyup"

        override val bind: Class<ProxyClientKeyEvents.Up> = ProxyClientKeyEvents.Up::class.java

        override fun getSender(event: ProxyClientKeyEvents.Up): ProxyTarget<*>? {
            return event.sender.asTarget()
        }
    }

    object Down : ScriptClientKey<ProxyClientKeyEvents.Down>() {

        override val name: String = "keydown"

        override val bind: Class<ProxyClientKeyEvents.Down> = ProxyClientKeyEvents.Down::class.java

        override fun getSender(event: ProxyClientKeyEvents.Down): ProxyTarget<*>? {
            return event.sender.asTarget()
        }

    }

}
