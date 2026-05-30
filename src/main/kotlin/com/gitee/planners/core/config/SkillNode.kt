package com.gitee.planners.core.config

import com.gitee.planners.Planners
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection

class SkillNode(
    val maxLevel: Int,
    val levels: Map<Int, Map<String, Map<String, Any>>>
) {
    companion object {
        fun parse(config: ConfigurationSection): SkillNode {
            val maxLevel = config.getInt("maxLevel", 1)
            val levelsSection = config.getConfigurationSection("levels") ?: config
            val levels = mutableMapOf<Int, Map<String, Map<String, Any>>>()

            for (key in levelsSection.getKeys(false)) {
                val lv = key.toIntOrNull()
                if (lv == null) {
                    continue
                }
                val condSection = levelsSection.getConfigurationSection(key)
                if (condSection == null) {
                    continue
                }
                val conditions = mutableMapOf<String, Map<String, Any>>()
                for (condKey in condSection.getKeys(false)) {
                    val propsSection = condSection.getConfigurationSection(condKey)
                    val props: Map<String, Any>
                    if (propsSection != null) {
                        props = propsSection.getValues(false).mapValues { it.value ?: "" }
                    } else {
                        props = emptyMap()
                    }
                    conditions[condKey] = props
                }
                levels[lv] = conditions
            }

            for ((lv, conds) in levels) {
                for (key in conds.keys) {
                    if (!Planners.conditions.get().containsKey(key)) {
                        warning("SkillNode: 未知的 condition key '$key' (level $lv)，将在运行时校验")
                    }
                }
            }

            return SkillNode(maxLevel, levels)
        }
    }
}
