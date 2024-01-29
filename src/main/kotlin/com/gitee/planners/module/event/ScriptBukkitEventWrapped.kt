package com.gitee.planners.module.event

import org.bukkit.event.Event
import taboolib.module.kether.ScriptContext

interface ScriptBukkitEventWrapped<T : Event> : ScriptEventWrapped<T> {

    override fun handle(event: T, ctx: ScriptContext) {
        ctx["event"] = getModifier(event) ?: event
    }

}
