package com.gitee.planners.core.config

import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.common.script.ComplexScriptPlatform
import com.gitee.planners.api.common.script.kether.KetherHelper
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.util.mapSection

class ImmutableState(val config: ConfigurationSection) : State {

    override val id: String = config.name

    override val priority: Double = config.getDouble("priority")

    override val name: String = config.getString("name", id)!!

    override val isStatic: Boolean = config.getBoolean("static", false)

    override val triggers: Map<String, State.Trigger> = config.mapSection("trigger") {
        val id: String = it.name
        val on: String = it.getString("on", it.name)!!
        val action = it.getString("action", "")!!

        State.Trigger(id, on, ScriptImpl(action))
    }

    class ScriptImpl(val experience: String) : ComplexCompiledScript {

        override val id: String = experience

        override val async: Boolean = false

        override fun source(): String {
            return experience
        }

        override fun namespaces(): List<String> {
            return listOf(KetherHelper.NAMESPACE_COMMON)
        }

        override fun platform(): ComplexScriptPlatform {
            return ComplexScriptPlatform.STATE
        }
    }
}

