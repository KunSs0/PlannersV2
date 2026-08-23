package com.gitee.planners.core.config

class SkillTreeSkillNode(
    id: String,
    val skillId: String,
    maxLevel: Int,
    levels: Map<Int, Map<String, Map<String, Any>>>,
    position: SkillTreeNodePosition
) : SkillTreeNode(id, SkillTreeNodeType.SKILL, maxLevel, levels, position)
