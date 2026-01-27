package com.gitee.planners.core.config

import com.gitee.planners.module.fluxon.FluxonTrigger
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.util.mapSection

class ImmutableState(val config: ConfigurationSection) : State {

    override val id: String = config.name

    override val priority: Double = config.getDouble("priority")

    override val maxLayer: Int = config.getInt("max-layer").takeIf { it > 0 } ?: Int.MAX_VALUE

    override val name: String = config.getString("name", id)!!

    override val isStatic: Boolean = config.getBoolean("static", false)

    override val triggers: Map<String, State.Trigger> = config.mapSection("trigger") {
        val triggerId: String = it.name
        val listen: String = it.getString("listen", it.name)!!
        val action = it.getString("action", "")!!
        val async = it.getBoolean("async", false)

        State.Trigger(
            id = triggerId,
            listen = listen,
            action = FluxonTrigger.of(triggerId, listen, action, async)
        )
    }
}
