package com.gitee.planners.module.kether.bukkit.event

import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.module.event.ScriptBukkitEventWrapped.Companion.getWrappedEvent
import org.bukkit.event.Cancellable
import org.bukkit.event.entity.EntityDamageByEntityEvent

@CombinationKetherParser.Used
object ActionEvent : MultipleKetherParser("event") {

    val damage = KetherHelper.combinedKetherParser() {
        it.group(command("to", "set", then = double()).optional()).apply(it) { data ->
            now {
                val wrappedEvent = this.getWrappedEvent()
                if (!wrappedEvent.isPresent) {
                    error("No event found.")
                }
                val event = wrappedEvent.get()
                if (event !is EntityDamageByEntityEvent) {
                    error("Event is not damage event.")
                }
                if (data.isPresent) {
                    event.damage = data.get()
                }
                event.damage
            }
        }
    }

    val cancellable = KetherHelper.combinedKetherParser("set", "to") {
        it.group(command("set", "to", then = bool()).optional()).apply(it) { data ->
            now {
                val wrappedEvent = this.getWrappedEvent()
                if (!wrappedEvent.isPresent) {
                    error("No event found.")
                }
                val event = wrappedEvent.get()
                if (event !is Cancellable) {
                    error("Event is not cancellable.")
                }
                if (data.isPresent) {
                    event.isCancelled = data.get()
                }
                event.isCancelled
            }
        }
    }

}
