package com.gitee.planners.core.config

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.tabooproject.fluxon.parser.ParsedScript
import taboolib.library.configuration.ConfigurationSection

class ImmutableState(val config: ConfigurationSection) : State {

    override val id: String = config.name

    override val priority: Double = config.getDouble("priority")

    override val maxLayer: Int = config.getInt("max-layer").takeIf { it > 0 } ?: Int.MAX_VALUE

    override val name: String = config.getString("name", id)!!

    override val isStatic: Boolean = config.getBoolean("static", false)

    override val action: ParsedScript? = config.getString("action")?.let { actionStr ->
        if (actionStr.isBlank()) null else FluxonScriptCache.getOrParse(actionStr)
    }
}
