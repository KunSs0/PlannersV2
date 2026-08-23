package com.gitee.planners.core.skilltree

import com.gitee.planners.core.config.SkillTreeAttributeNode

class AttributeSkillTreeNodeProvider : SkillTreeNodeProvider {

    override val id = "attribute"

    override fun provide(context: SkillTreeNodeProviderContext): List<SkillTreeNodeContribution> {
        val node = context.node
        if (node !is SkillTreeAttributeNode || node.providerId != id || context.state.level <= 0) {
            return emptyList()
        }
        val result = ArrayList<SkillTreeNodeContribution>()
        for ((attributeId, amountPerLevel) in node.providerValues) {
            result.add(AttributeNodeContribution(attributeId, amountPerLevel * context.state.level))
        }
        return result
    }
}
