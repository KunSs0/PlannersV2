package com.gitee.planners.core.config

class SkillTreeAttributeNode(
    id: String,
    val providerId: String,
    val providerValues: Map<String, Double>,
    maxLevel: Int,
    levels: Map<Int, Map<String, Map<String, Any>>>,
    position: SkillTreeNodePosition
) : SkillTreeNode(id, SkillTreeNodeType.ATTRIBUTE, maxLevel, levels, position)
