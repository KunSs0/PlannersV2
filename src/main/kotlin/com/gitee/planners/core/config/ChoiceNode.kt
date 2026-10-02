package com.gitee.planners.core.config

class ChoiceNode(
    id: String,
    maxLevel: Int,
    levels: Map<Int, Map<String, Map<String, Any>>>,
    position: SkillTreeNodePosition,
    val options: Map<String, Option>
) : SkillTreeNode(id, SkillTreeNodeType.CHOICE, maxLevel, levels, position) {

    class Option(
        val id: String,
        val name: String,
        val providerId: String?,
        val properties: Map<String, Any>
    )
}
