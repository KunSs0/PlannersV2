package com.gitee.planners.core.player

/**
 * 单个真实技能树节点的持久状态，不包含虚拟选项。
 *
 * @property index 数据库记录 ID。
 * @property treeId 所属技能树。
 * @property nodeId 实际节点 ID。
 * @property level 已学习等级。
 */
class PlayerSkillTreeNodeState(
    val index: Long,
    val treeId: String,
    val nodeId: String,
    var level: Int
)
