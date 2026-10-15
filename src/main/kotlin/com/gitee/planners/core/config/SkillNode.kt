package com.gitee.planners.core.config

import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.Planners
import com.gitee.planners.core.condition.ConditionConfig
import taboolib.library.configuration.ConfigurationSection

/** 解码技能树节点等级条件，并在 Workspace 加载前登记脚本属性。 */
object SkillNode {

    /** 原生模板按字面量校验，Nova 模板才编译字符串表达式。 */
    fun parseLevels(config: ConfigurationSection, templates: Map<String, ConditionConfig> = Planners.conditions.get()): Map<Int, Map<String, Map<String, Any>>> {
        val levelsSection = config.getConfigurationSection("levels")
        if (levelsSection == null) {
            error("Skill tree node '${config.name}' is missing levels")
        }
        val levels = LinkedHashMap<Int, Map<String, Map<String, Any>>>()
        for (key in levelsSection.getKeys(false)) {
            val level = key.toIntOrNull()
            if (level == null || level <= 0) {
                error("Skill tree node '${config.name}' contains an invalid level: $key")
            }
            val levelSection = levelsSection.getConfigurationSection(key)
            if (levelSection == null) {
                error("Skill tree node '${config.name}' level $level must be a configuration section")
            }
            val conditions = LinkedHashMap<String, Map<String, Any>>()
            for (conditionId in levelSection.getKeys(false)) {
                val template = templates[conditionId]
                require(template != null) { "未知技能树条件：$conditionId" }
                val conditionSection = levelSection.getConfigurationSection(conditionId)
                if (conditionSection == null) {
                    conditions[conditionId] = emptyMap()
                } else {
                    val values = LinkedHashMap<String, Any>()
                    for ((propertyId, propertyValue) in conditionSection.getValues(false)) {
                        if (propertyValue != null) {
                            values[propertyId] = propertyValue
                            if (template.expressionProperties && propertyValue is String && propertyValue.toDoubleOrNull() == null) {
                                // 条件覆盖表达式必须在 Workspace load 前登记，运行期禁止动态编译。
                                ScriptManager.compileExpression(
                                    "skill-tree:${config.name}:level:$level:$conditionId:$propertyId",
                                    listOf("player", "profile", "router", "route", "treeId", "nodeId", "nodeLevel"),
                                    propertyValue
                                )
                            }
                        }
                    }
                    conditions[conditionId] = values
                }
                val merged = LinkedHashMap(template.props)
                merged.putAll(conditions.getValue(conditionId))
                template.validateProperties(merged)
            }
            levels[level] = conditions
        }
        return levels
    }
}
