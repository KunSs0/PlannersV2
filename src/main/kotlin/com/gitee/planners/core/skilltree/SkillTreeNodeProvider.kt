package com.gitee.planners.core.skilltree

interface SkillTreeNodeProvider {

    val id: String

    fun provide(context: SkillTreeNodeProviderContext): List<SkillTreeNodeContribution>
}
