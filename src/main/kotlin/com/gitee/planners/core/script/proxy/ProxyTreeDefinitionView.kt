package com.gitee.planners.core.script.proxy

import com.gitee.planners.core.config.ImmutableSkillTree

class ProxyTreeDefinitionView(tree: ImmutableSkillTree) : ProxyTreeDefinition {

    override val id: String = tree.id

    override val nodeIds: List<String> = tree.nodes.keys.toList()
}
