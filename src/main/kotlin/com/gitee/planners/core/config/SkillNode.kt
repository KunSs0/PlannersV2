package com.gitee.planners.core.config

import com.gitee.planners.core.condition.ConditionRegistry
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection

/**
 * 技能节点。
 * 一个节点对应一个 ImmutableSkill，定义等级上限和每级条件。
 */
class SkillNode(
    val maxLevel: Int,
    /** 每级条件：level → { conditionKey: overrideProps } */
    val levels: Map<Int, Map<String, Map<String, Any>>>
) {
    companion object {
        fun parse(config: ConfigurationSection): SkillNode {
            val maxLevel = config.getInt("maxLevel", 1)

            val levelsSection = config.getConfigurationSection("levels") ?: config
            val levels = mutableMapOf<Int, Map<String, Map<String, Any>>>()

            for (key in levelsSection.getKeys(false)) {
                val lv = key.toIntOrNull() ?: continue
                val condSection = levelsSection.getConfigurationSection(key) ?: continue
                val conditions = mutableMapOf<String, Map<String, Any>>()
                for (condKey in condSection.getKeys(false)) {
                    val propsSection = condSection.getConfigurationSection(condKey)
                    val props = if (propsSection != null) {
                        propsSection.getValues(false).mapValues { it.value ?: "" }
                    } else {
                        emptyMap()
                    }
                    conditions[condKey] = props
                }
                levels[lv] = conditions
            }

            // condition key 存在性校验（警告不阻塞 — ConditionRegistry 可能尚未初始化）
            for ((lv, conds) in levels) {
                for (key in conds.keys) {
                    if (!ConditionRegistry.contains(key)) {
                        warning("SkillNode: 未知的 condition key '$key' (level $lv)，将在运行时校验")
                    }
                }
            }

            return SkillNode(maxLevel, levels)
        }
    }
}
