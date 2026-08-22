package com.gitee.planners.core.config

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Variable
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration

class ImmutableJob(private val config: Configuration) {

    val id = config.file!!.nameWithoutExtension

    private val option = config.getOption()

    val name = option.getString("name", id)!!

    /** 职业显示图标；未配置时为空。 */
    val icon: ItemStack?
        get() {
            val display = option.getConfigurationSection("display")
            if (display == null) {
                return null
            }
            return display.getItemStack("icon")
        }

    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
    }

    val skillIds = option.getStringList("skill")

    init {
        val seenSkills = mutableSetOf<String>()
        val duplicateSkills = mutableSetOf<String>()
        for (skillId in skillIds) {
            if (!seenSkills.add(skillId)) {
                duplicateSkills.add(skillId)
            }
        }
        if (duplicateSkills.isNotEmpty()) {
            error("Job '$id' 包含重复技能: ${duplicateSkills.joinToString(", ")}")
        }
    }

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
        return skillIds.contains(id)
    }

    fun getImmutableSkillValues(): List<ImmutableSkill> {
        return Registries.SKILL.values().filter { it.id in skillIds }
    }

    fun getSkillOrNull(id: String): ImmutableSkill? {
        if (!hasSkill(id)) {
            return null
        }
        return Registries.SKILL.getOrNull(id)
    }

    fun getVariableOrNull(id: String): Variable? {
        return immutableVariables[id]
    }

    fun getVariables(): Map<String, Variable> {
        return immutableVariables
    }
}
