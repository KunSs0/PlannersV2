package com.gitee.planners.module.event

import org.bukkit.event.Event

interface ScriptBukkitEventWrapped<T : Event> : ScriptEventWrapped<T> {
}
