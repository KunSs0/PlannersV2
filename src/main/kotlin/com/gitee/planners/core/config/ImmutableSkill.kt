package com.gitee.planners.core.config

import com.gitee.planners.api.script.SingletonKetherScript
import com.gitee.planners.api.skill.Skill
import com.gitee.planners.api.skill.Variable
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
import taboolib.module.configuration.Configuration

class ImmutableSkill(config: Configuration) : SingletonKetherScript(), Skill {

    override val id = config.file!!.nameWithoutExtension

    private val option = config.getOption()

    override val action = config.getString("action", config.getString("run", "tell none"))!!

    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
    }

    override fun getVariableOrNull(id: String): Variable? {
        return immutableVariables[id]
    }

    override fun getVariables(): Map<String, Variable> {
        return immutableVariables
    }


}
