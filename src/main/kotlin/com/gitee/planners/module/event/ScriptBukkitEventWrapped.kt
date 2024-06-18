package com.gitee.planners.module.event

import org.bukkit.event.Event
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptFrame
import java.util.*

interface ScriptBukkitEventWrapped<T : Event> : ScriptEventWrapped<T> {

    override fun handle(event: T, ctx: ScriptContext) {
        ctx["event"] = getModifier(event) ?: event
    }

    companion object {

        fun ScriptFrame.getWrappedEvent(): Optional<Event> {
            return this.variables().get<Event>("event")
        }

    }

}
