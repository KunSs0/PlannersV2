package com.gitee.planners.core.skilltree

import com.gitee.planners.core.config.SkillTreeSkillNode

class SkillTreeSkillNodeProvider : SkillTreeNodeProvider {

    override val id = "skill"

    override fun provide(context: SkillTreeNodeProviderContext): List<SkillTreeNodeContribution> {
        val node = context.node
        if (node !is SkillTreeSkillNode) {
            return emptyList()
        }
        if (context.state.level <= 0) {
            return emptyList()
        }
        return listOf(SkillLevelNodeContribution(node.skillId, context.state.level))
    }
}
