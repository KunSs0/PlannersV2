package com.gitee.planners.core.config

/**
 * 独立功能解锁节点，学习记录由技能树保存，业务按节点等级读取效果。
 *
 * @param id 本树内唯一节点 ID。
 * @property name 必填的展示名称。
 * @param maxLevel 节点可学习的最高等级。
 * @param levels 每一级的学习条件及既有消费配置。
 * @param position 节点布局坐标。
 */
class SkillTreeUnlockNode(
    id: String,
    val name: String,
    maxLevel: Int,
    levels: Map<Int, Map<String, Map<String, Any>>>,
    position: SkillTreeNodePosition
) : SkillTreeNode(id, SkillTreeNodeType.UNLOCK, maxLevel, levels, position)
