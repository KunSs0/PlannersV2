package com.gitee.planners.core.player

import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.Variable
import com.gitee.planners.core.config.ImmutableSkill

class PlayerSkill(var bindingId: Long, private val skillId: String, var level: Int) : Skill {

    val immutable: ImmutableSkill
        get() = RegistryBuiltin.SKILL.getOrNull(skillId) ?: error("Couldn't find skill with id $skillId'")

    override val id: String
        get() = skillId


    override fun getVariables(): Map<String, Variable> {
        return immutable.getVariables()
    }

    override fun getVariableOrNull(id: String): Variable? {
        return immutable.getVariableOrNull(id)
    }


}
