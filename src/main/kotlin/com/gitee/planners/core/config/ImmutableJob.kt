package com.gitee.planners.core.config

import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.job.Job
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.Variable
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
import taboolib.module.configuration.Configuration

class ImmutableJob(private val config: Configuration) : Job {

    override val id = config.file!!.nameWithoutExtension

    private val option = config.getOption()

    override val name = option.getString("name", id)!!

    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
    }

    private val immutableSkillKeys = option.getStringList("skill")

    override fun hasSkill(id: String): Boolean {
        return this.immutableSkillKeys.contains(id)
    }

    fun getImmutableSkillValues(): List<ImmutableSkill> {
        return RegistryBuiltin.SKILL.getValues().filter { it.id in immutableSkillKeys }
    }

    override fun getSkillOrNull(id: String): ImmutableSkill? {
        return RegistryBuiltin.SKILL.getOrNull(id)
    }

    override fun getVariableOrNull(id: String): Variable? {
        return immutableVariables[id]
    }

    override fun getVariables(): Map<String, Variable> {
        return immutableVariables
    }
}
