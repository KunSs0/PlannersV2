package com.gitee.planners.core.config

import taboolib.library.configuration.ConfigurationSection

object SkillNode {

    fun parseLevels(config: ConfigurationSection): Map<Int, Map<String, Map<String, Any>>> {
        val levelsSection = config.getConfigurationSection("levels")
        if (levelsSection == null) {
            error("技能树节点 '${config.name}' 缺少 levels")
        }
        val levels = LinkedHashMap<Int, Map<String, Map<String, Any>>>()
        for (key in levelsSection.getKeys(false)) {
            val level = key.toIntOrNull()
            if (level == null || level <= 0) {
                error("技能树节点 '${config.name}' 存在无效等级 '$key'")
            }
            val levelSection = levelsSection.getConfigurationSection(key)
            if (levelSection == null) {
                error("技能树节点 '${config.name}' 的等级 $level 不是配置节")
            }
            val conditions = LinkedHashMap<String, Map<String, Any>>()
            for (conditionId in levelSection.getKeys(false)) {
                val conditionSection = levelSection.getConfigurationSection(conditionId)
                if (conditionSection == null) {
                    conditions[conditionId] = emptyMap()
                } else {
                    val values = LinkedHashMap<String, Any>()
                    for ((propertyId, propertyValue) in conditionSection.getValues(false)) {
                        if (propertyValue != null) {
                            values[propertyId] = propertyValue
                        }
                    }
                    conditions[conditionId] = values
                }
            }
            levels[level] = conditions
        }
        return levels
    }
}
