package com.gitee.planners.core.config

import taboolib.library.configuration.ConfigurationSection

class ImmutableState(val config: ConfigurationSection) : State {

    override val id: String = config.name

    override val priority: Double = config.getDouble("priority")

    override val maxLayer: Int
    get() {
        val value = config.getInt("max-layer")
        if (value > 0) {
            return value
        }
        return Int.MAX_VALUE
    }

    override val name: String = config.getString("name", id)!!

    override val isStatic: Boolean = config.getBoolean("static", false)

    override val action: String?
    get() {
        val raw = config.getString("action")
        if (raw != null && raw.isNotBlank()) {
            return raw
        }
        return null
    }
}
