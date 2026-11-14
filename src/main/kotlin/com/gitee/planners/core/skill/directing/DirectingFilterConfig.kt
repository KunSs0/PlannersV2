package com.gitee.planners.core.skill.directing

import com.gitee.planners.module.action.ActionTargeterChainParser
import com.gitee.planners.module.action.ActionTargeterStep
import taboolib.library.configuration.ConfigurationSection

/**
 * 解析技能 `__option__.directing.filter` 配置段。
 *
 * 过滤按候选类别分块，当前只有实体候选可过滤；出现其他类别直接报错，
 * 避免配置写了却被静默忽略。解析结果只描述过滤链，运行期由 [DirectingFilterCompiler] 绑定施法者。
 */
object DirectingFilterConfig {

    /**
     * 读取某个技能的过滤链。
     *
     * @param directingSection `__option__.directing` 配置段。
     * @param skillId 技能标识，用于错误信息。
     * @return 已编译的过滤链步骤；未配置过滤链时为空列表。
     */
    fun parse(directingSection: ConfigurationSection?, skillId: String): List<ActionTargeterStep> {
        if (directingSection == null) {
            return emptyList()
        }
        val filterSection = directingSection.getConfigurationSection("filter")
        if (filterSection == null) {
            return emptyList()
        }
        for (key in filterSection.getKeys(false)) {
            if (!key.equals("entity", true)) {
                throw IllegalArgumentException(
                    "Skill '$skillId' directing.filter only supports 'entity', got '$key'",
                )
            }
        }
        val entitySection = filterSection.getConfigurationSection("entity")
        if (entitySection == null) {
            throw IllegalArgumentException("Skill '$skillId' directing.filter.entity must be a section")
        }
        val entries = entitySection.getStringList("targeters")
        if (entries.isEmpty()) {
            return emptyList()
        }
        return ActionTargeterChainParser.parseEntries(entries, "skill:$skillId.directing.filter.entity.targeters")
    }
}
