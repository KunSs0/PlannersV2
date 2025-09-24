package com.gitee.planners.module.kether.bukkit.event

import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.core.skill.script.ScriptBukkitEventHolder.Companion.getWrappedEvent
import com.gitee.planners.core.skill.script.animated.AbstractCancellableEvent
import com.gitee.planners.core.skill.script.animated.DamageEventModifier

@CombinationKetherParser.Used
object ActionEvent : MultipleKetherParser("event") {

    val damage = KetherHelper.combinedKetherParser {
        it.group(command("to", "set", then = double()).optional()).apply(it) { data ->
            now {
                val wrappedEvent = this.getWrappedEvent()
                if (!wrappedEvent.isPresent) {
                    error("No event found.")
                }
                val event = wrappedEvent.get()
                if (event !is DamageEventModifier) {
                    error("Event is not damage event.")
                }
                if (data.isPresent) {
                    event.damage.set(data.get())
                }
                event.damage.asDouble()
            }
        }
    }

    val cancelled = KetherHelper.combinedKetherParser {
        it.group(command("set", "to", then = bool()).optional()).apply(it) { data ->
            now {
                val wrappedEvent = this.getWrappedEvent()
                if (!wrappedEvent.isPresent) {
                    error("No event found.")
                }
                val event = wrappedEvent.get()
                if (event !is AbstractCancellableEvent) {
                    error("Event is not cancellable.")
                }
                if (data.isPresent) {
                    event.isCancelled.setAsUpdate(data.get())
                }
                event.isCancelled.asBoolean()
            }
        }
    }

}
