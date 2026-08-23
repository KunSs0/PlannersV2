package com.gitee.planners.core.config

abstract class SkillTreeNode(
    val id: String,
    val type: SkillTreeNodeType,
    val maxLevel: Int,
    val levels: Map<Int, Map<String, Map<String, Any>>>,
    val position: SkillTreeNodePosition
)
