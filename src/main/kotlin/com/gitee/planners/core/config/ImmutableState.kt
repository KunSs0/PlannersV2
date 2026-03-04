package com.gitee.planners.core.config

import taboolib.library.configuration.ConfigurationSection

class ImmutableState(val config: ConfigurationSection) : State {

    override val id: String = config.name

    override val priority: Double = config.getDouble("priority")

    override val maxLayer: Int = config.getInt("max-layer").takeIf { it > 0 } ?: Int.MAX_VALUE

    override val name: String = config.getString("name", id)!!

    override val isStatic: Boolean = config.getBoolean("static", false)

    override val action: String? = config.getString("action")?.takeIf { it.isNotBlank() }
}
