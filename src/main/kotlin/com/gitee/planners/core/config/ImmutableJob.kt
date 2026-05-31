package com.gitee.planners.core.config

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Variable
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
import taboolib.module.configuration.Configuration

class ImmutableJob(private val config: Configuration) {

    val id = config.file!!.nameWithoutExtension

    private val option = config.getOption()

    val name = option.getString("name", id)!!

    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
    }

    private val immutableSkillKeys = option.getStringList("skill")

    /**
     * 职业提供的属性。
     * key = 属性键（在 registry 中为逻辑属性，否则为物理直通）
     * value = JS 表达式字符串或数字
     */
    val attributes: Map<String, String>
        get() {
            val section = option.getConfigurationSection("hook.attributes")
            if (section == null) {
                return emptyMap()
            }
            return section.getValues(false).mapValues { it.value.toString() }
        }

    fun hasSkill(id: String): Boolean {
        return this.immutableSkillKeys.contains(id)
    }

    fun getImmutableSkillValues(): List<ImmutableSkill> {
        return Registries.SKILL.values().filter { it.id in immutableSkillKeys }
    }

    fun getSkillOrNull(id: String): ImmutableSkill? {
        return Registries.SKILL.getOrNull(id)
    }

    fun getVariableOrNull(id: String): Variable? {
        return immutableVariables[id]
    }

    fun getVariables(): Map<String, Variable> {
        return immutableVariables
    }
}
