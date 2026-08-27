package com.gitee.planners.core.config

import com.gitee.planners.module.script.NovaScriptUnit
import com.gitee.planners.module.script.ScriptManager
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

    override val attribute: List<String> = config.getStringList("attribute")

    override val action: String?
    get() {
        val raw = config.getString("action")
        if (raw != null && raw.isNotBlank()) {
            return raw
        }
        return null
    }

    override val actionModule: NovaScriptUnit?
    init {
        val source = action
        if (source == null) {
            actionModule = null
        } else {
            actionModule = ScriptManager.compileModule("state:$id:action", source)
        }
    }
}
